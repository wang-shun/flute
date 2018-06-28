/*
 * Copyright 2016 - 2017 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.flute.archive.jdbc;

import com.aitusoftware.flute.receive.HistogramHandler;
import com.aitusoftware.flute.util.lang.StringTrimmer;
import org.HdrHistogram.Histogram;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HistogramInsertDao implements HistogramHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HistogramInsertDao.class);
    private static final String INSERT_SQL = "INSERT INTO histogram_data (sender, identifier, start_timestamp, end_timestamp, " +
            "min_value, mean, fifty, ninety, two_nines, three_nines, four_nines, five_nines, max_value, total_count, raw_data) " +
            "VALUES " +
            "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final int HISTOGRAM_SENDER_IDENTIFIER_MAX_LENGTH = 256;
    private static final int HISTOGRAM_DATA_SENDER_MAX_LENGTH = 64;

    private final StringTrimmer trimmer = new StringTrimmer();
    private final Connection connection;
    private final Consumer<SQLException> exceptionConsumer;
    private final ByteBuffer encodedData;

    public HistogramInsertDao(final Connection connection, final Consumer<SQLException> exceptionConsumer,
                              final int maxEncodedHistogramSize)
    {
        this.connection = connection;
        this.exceptionConsumer = exceptionConsumer;
        encodedData = ByteBuffer.allocate(maxEncodedHistogramSize);
    }

    @Override
    public void histogramReceived(final InetSocketAddress sender, final CharSequence identifier,
                                  final long startTimestamp,
                                  final long endTimestamp, final Histogram histogram)
    {
        try(final PreparedStatement statement = connection.prepareStatement(INSERT_SQL))
        {
            statement.setString(1, trimmer.trimToLength(sender.getHostString(), HISTOGRAM_DATA_SENDER_MAX_LENGTH));
            statement.setString(2, trimmer.trimToLength(identifier.toString(), HISTOGRAM_SENDER_IDENTIFIER_MAX_LENGTH));
            statement.setLong(3, startTimestamp);
            statement.setLong(4, endTimestamp);
            statement.setLong(5, histogram.getMinValue());
            statement.setDouble(6, histogram.getMean());
            statement.setLong(7, histogram.getValueAtPercentile(50));
            statement.setLong(8, histogram.getValueAtPercentile(90));
            statement.setLong(9, histogram.getValueAtPercentile(99));
            statement.setLong(10, histogram.getValueAtPercentile(99.9));
            statement.setLong(11, histogram.getValueAtPercentile(99.99));
            statement.setLong(12, histogram.getValueAtPercentile(99.999));
            statement.setLong(13, histogram.getMaxValue());
            statement.setLong(14, histogram.getTotalCount());
            encodedData.clear();
            histogram.encodeIntoCompressedByteBuffer(encodedData, Deflater.BEST_COMPRESSION);
            encodedData.flip();
            statement.setBlob(15, new ByteArrayInputStream(encodedData.array(), 0, encodedData.remaining()));


            if(statement.executeUpdate() == 0)
            {
                throw new SQLException("Failed to insert data", statement.getWarnings());
            }
            connection.commit();

            if(LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Successfully recorded histogram for {}", identifier.toString());
            }
        }
        catch(final SQLException e)
        {
            exceptionConsumer.accept(e);
        }
    }
}