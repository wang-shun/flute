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
package com.aitusoftware.flute.server.dao.jdbc;

import com.aitusoftware.flute.config.ConnectionFactory;
import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.server.cache.ByteBufferHistogram;
import com.aitusoftware.flute.server.cache.CompressedHistogram;
import com.aitusoftware.flute.server.cache.HistogramQueryFunction;
import com.aitusoftware.flute.server.cache.UncompressedHistogram;
import com.aitusoftware.flute.server.dao.HistogramAggregator;
import com.aitusoftware.flute.server.query.FullHistogramHandler;
import com.aitusoftware.flute.server.query.Query;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.DataFormatException;

public final class HistogramRetrievalDao implements HistogramQueryFunction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HistogramRetrievalDao.class);
    private static final String DETAIL_QUERY_BASE = "SELECT * FROM histogram_data ";
    private static final String DETAIL_QUERY_CLAUSE =
            "WHERE identifier IN (%s) AND start_timestamp >= ? AND start_timestamp <= ?";
    private static final String DEBUG_QUERY = "SELECT * FROM histogram_data WHERE identifier IN (%s)";
    private static final ThreadLocal<ByteBuffer> BUFFER = new ThreadLocal<>();
    private final ConnectionFactory connectionFactory;
    private final int maxEncodedHistogramSize;
    private final HistogramAggregator aggregator;
    private final boolean storeCompressedHistograms;
    private final HistogramConfig histogramConfig;

    public HistogramRetrievalDao(final ConnectionFactory connectionFactory, final HistogramConfig histogramConfig,
                                 final int maxEncodedHistogramSize, final boolean storeCompressedHistograms)
    {
        this.connectionFactory = connectionFactory;
        this.histogramConfig = histogramConfig;
        this.maxEncodedHistogramSize = maxEncodedHistogramSize;
        this.aggregator = new HistogramAggregator(histogramConfig);
        this.storeCompressedHistograms = storeCompressedHistograms;
    }

    @Override
    public List<CompressedHistogram> query(final Set<String> identifiers, final String metricKey, final long selectionStartTime,
                                 final long selectionEndTime)
    {
        return queryForHistograms(identifiers, DETAIL_QUERY_BASE + DETAIL_QUERY_CLAUSE, selectionStartTime, selectionEndTime);
    }

    public void selectCompositeHistogramSummary(final Set<String> identifiers, final Query query, final FullHistogramHandler handler)
    {
        selectCompositeHistogram(identifiers, query, new FullHistogramHandler()
        {
            @Override
            public void onRecord(final String identifier, final long timestamp, final Histogram histogram)
            {
                handler.onRecord(
                        identifier,
                        timestamp,
                        histogram);
            }
        });
    }

    public Histogram selectCompositeHistogram(final Set<String> identifiers, final Query query, final FullHistogramHandler handler)
    {
        final String sql = DETAIL_QUERY_BASE + DETAIL_QUERY_CLAUSE;
        if(query == null)
        {
            throw new IllegalArgumentException("Query cannot be null");
        }
        return queryForHistogram(identifiers, handler, sql, query.getStartMillis(), query.getEndMillis(), query.getMetricKey());
    }

    private List<CompressedHistogram> queryForHistograms(final Set<String> identifiers, final String sql,
                                               final long startMillis, final long endMillis)
    {
        try (
                final Connection connection = connectionFactory.getConnection();
                final PreparedStatement statement =
                        createStatementWithQueryParameters(sql, identifiers, connection, startMillis, endMillis);
                final ResultSet resultSet = statement.executeQuery())
        {
            if (BUFFER.get() == null)
            {
                BUFFER.set(ByteBuffer.allocate(maxEncodedHistogramSize));
            }
            final ByteBuffer buffer = BUFFER.get();
            final List<CompressedHistogram> resultList = new ArrayList<>();
            boolean noResults = true;
            try
            {
                while (resultSet.next())
                {
                    noResults = false;
                    try (final InputStream histogramDataStream = resultSet.getBinaryStream("raw_data"))
                    {
                        buffer.clear();
                        final ReadableByteChannel inputChannel = Channels.newChannel(histogramDataStream);
                        while ((inputChannel.read(buffer)) != -1)
                        {
                            // read data
                        }
                        buffer.flip();
                        final long startTimestamp = resultSet.getLong("start_timestamp");
                        final long endTimestamp = resultSet.getLong("end_timestamp");
                        final CompressedHistogram histogram = createCompressedHistogram(startTimestamp, endTimestamp, buffer);
                        resultList.add(histogram);
                    }
                    catch (IOException | DataFormatException | RuntimeException e)
                    {
                        throw new RuntimeException("Failed to read from stream", e);
                    }
                }

                if (noResults && LOGGER.isDebugEnabled())
                {
                    logDataTimestamps(DEBUG_QUERY, identifiers, connection);
                }

                return resultList;
            }
            catch (final SQLException e)
            {
                LOGGER.warn("Query failed", e);
                throw new RuntimeException("Query failed", e);
            }
        }
        catch (SQLException e)
        {
            LOGGER.warn("Query failed", e);
            throw new RuntimeException("Query failed", e);
        }
    }

    private CompressedHistogram createCompressedHistogram(final long startTimestamp, final long endTimestamp, final ByteBuffer buffer) throws DataFormatException
    {
        if (storeCompressedHistograms)
        {
            return new ByteBufferHistogram(buffer, startTimestamp, endTimestamp, histogramConfig);
        }
        else
        {
            final Histogram component =
                    Histogram.decodeFromCompressedByteBuffer(buffer, histogramConfig.getMaxValue());
            component.setStartTimeStamp(startTimestamp);
            component.setEndTimeStamp(endTimestamp);
            return new UncompressedHistogram(component);
        }
    }

    private void logDataTimestamps(final String sql, final Set<String> identifiers, final Connection connection)
            throws SQLException
    {
        try (final Statement statement = connection.createStatement();
                final ResultSet resultSet = statement.executeQuery(String.format(sql, identifierList(identifiers))))
        {
            long first = Long.MAX_VALUE;
            long last = Long.MIN_VALUE;
            int totalHistograms = 0;
            while (resultSet.next())
            {
                final long startTimestamp = resultSet.getLong("start_timestamp");
                if (startTimestamp < first)
                {
                    first = startTimestamp;
                }
                if (startTimestamp > last)
                {
                    last = startTimestamp;
                }
                totalHistograms++;
            }
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Found data for {}, from {} to {}, total {} histograms",
                        identifiers,
                        Instant.ofEpochMilli(first),
                        Instant.ofEpochMilli(last),
                        totalHistograms);
            }
        }
    }

    private Histogram queryForHistogram(final Set<String> identifiers, final FullHistogramHandler handler,
                                        final String sql, final long startMillis, final long endMillis,
                                        final String metricKey)
    {
        try(
                final Connection connection = connectionFactory.getConnection();
                final PreparedStatement statement =
                        createStatementWithQueryParameters(sql, identifiers, connection, startMillis, endMillis);
                final ResultSet resultSet = statement.executeQuery())
        {
            if(BUFFER.get() == null)
            {
                BUFFER.set(ByteBuffer.allocate(maxEncodedHistogramSize));
            }
            return aggregator.aggregate(handler, metricKey, BUFFER.get(),
                    new ResultSetHistogramIterator(resultSet), startMillis);
        }
        catch (SQLException e)
        {
            LOGGER.warn("Query failed", e);
            throw new RuntimeException("Query failed", e);
        }
    }

    private PreparedStatement createStatementWithQueryParameters(
            final String sql,
            final Set<String> identifiers,
            final Connection connection,
            final long startMillis,
            final long endMillis) throws SQLException
    {
        final String format = String.format(sql, identifierList(identifiers));
        final PreparedStatement preparedStatement =
                connection.prepareStatement(format);
        preparedStatement.setLong(1, startMillis);
        preparedStatement.setLong(2, endMillis);

        if(LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Query: {}, start {}, end {}, delta {}", format,
                    Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis), endMillis - startMillis);
        }

        return preparedStatement;
    }

    private static String identifierList(final Set<String> identifiers)
    {
        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String identifier : identifiers)
        {
            if(!first)
            {
                builder.append(',');
            }
            builder.append('\'').append(identifier).append('\'');
            first = false;
        }
        return builder.toString();
    }
}