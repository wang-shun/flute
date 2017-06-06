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
package com.aitusoftware.flute.server.dao;

import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.server.query.FullHistogramHandler;
import org.HdrHistogram.Histogram;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.SQLException;
import java.util.zip.DataFormatException;

public final class HistogramAggregator
{
    private final HistogramConfig histogramConfig;

    public HistogramAggregator(final HistogramConfig histogramConfig)
    {
        this.histogramConfig = histogramConfig;
    }

    public Histogram aggregate(
            final FullHistogramHandler handler,
            final String identifierDescription,
            final ByteBuffer buffer,
            final HistogramIterator histogramIterator,
            final long startMillis) throws SQLException
    {
        final Histogram composite = new Histogram(histogramConfig.getMaxValue(), histogramConfig.getSignificantDigits());

        try
        {
            while (histogramIterator.next())
            {
                try (final InputStream histogramDataStream = histogramIterator.getHistogramData())
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
                    if(composite.getTotalCount() == 0L)
                    {
                        composite.setStartTimeStamp(histogramIterator.getStartTimestamp());
                    }
                    composite.add(component);
                    composite.setEndTimeStamp(histogramIterator.getEndTimestamp());
                }
                catch (IOException | DataFormatException | RuntimeException e)
                {
                    throw new RuntimeException("Failed to read from stream", e);
                }

            }

            handler.onRecord(
                    identifierDescription,
                    startMillis,
                    composite);

            return composite;
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Query failed", e);
        }
    }
}
