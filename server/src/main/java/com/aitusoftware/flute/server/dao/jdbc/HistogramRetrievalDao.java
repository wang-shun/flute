/*
 * Copyright 2016 Aitu Software Limited.
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
import com.aitusoftware.flute.server.cache.HistogramQueryFunction;
import com.aitusoftware.flute.server.dao.HistogramAggregator;
import com.aitusoftware.flute.server.query.FullHistogramHandler;
import com.aitusoftware.flute.server.query.Query;
import org.HdrHistogram.Histogram;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;

public final class HistogramRetrievalDao implements HistogramQueryFunction
{
    private static final String DETAIL_QUERY_BASE = "SELECT * FROM histogram_data ";
    private static final String DETAIL_QUERY_CLAUSE =
            "WHERE identifier IN (%s) AND start_timestamp >= ? AND start_timestamp <= ?";
    private static final ThreadLocal<ByteBuffer> BUFFER = new ThreadLocal<>();
    private final ConnectionFactory connectionFactory;
    private final int maxEncodedHistogramSize;
    private final HistogramAggregator aggregator;
    private final HistogramConfig histogramConfig;

    public HistogramRetrievalDao(final ConnectionFactory connectionFactory, final HistogramConfig histogramConfig,
                                 final int maxEncodedHistogramSize)
    {
        this.connectionFactory = connectionFactory;
        this.histogramConfig = histogramConfig;
        this.maxEncodedHistogramSize = maxEncodedHistogramSize;
        this.aggregator = new HistogramAggregator(histogramConfig);
    }

    @Override
    public List<Histogram> query(final long selectionStartTime, final long duration,
                                 final TimeUnit durationUnit, final Set<String> identifiers,
                                 final String metricKey)
    {
        return queryForHistograms(identifiers, DETAIL_QUERY_BASE + DETAIL_QUERY_CLAUSE, selectionStartTime, System.currentTimeMillis());
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

    private List<Histogram> queryForHistograms(final Set<String> identifiers, final String sql,
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
            final List<Histogram> resultList = new ArrayList<>();
            try
            {
                while (resultSet.next())
                {
                    try (final InputStream histogramDataStream = resultSet.getBinaryStream("raw_data"))
                    {
                        buffer.clear();
                        final ReadableByteChannel inputChannel = Channels.newChannel(histogramDataStream);
                        while ((inputChannel.read(buffer)) != -1)
                        {
                            // read data
                        }
                        buffer.flip();
                        final Histogram component =
                                Histogram.decodeFromCompressedByteBuffer(buffer, histogramConfig.getMaxValue());
                        component.setStartTimeStamp(resultSet.getLong("start_timestamp"));
                        component.setEndTimeStamp(resultSet.getLong("end_timestamp"));
                        resultList.add(component);
                    }
                    catch (IOException | DataFormatException | RuntimeException e)
                    {
                        throw new RuntimeException("Failed to read from stream", e);
                    }

                }

                return resultList;
            }
            catch (final SQLException e)
            {
                throw new RuntimeException("Query failed", e);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Query failed", e);
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
            return aggregator.aggregate(handler, metricKey, BUFFER.get(), resultSet, startMillis);
        }
        catch (SQLException e)
        {
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
        final PreparedStatement preparedStatement =
                connection.prepareStatement(String.format(sql, identifierList(identifiers)));
        preparedStatement.setLong(1, startMillis);
        preparedStatement.setLong(2, endMillis);
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