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
package com.aitusoftware.flute.server.cache;

import com.aitusoftware.flute.config.HistogramConfig;
import org.HdrHistogram.Histogram;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

public final class ByteBufferHistogram implements CompressedHistogram
{
    private final ByteBuffer buffer;
    private final long startTimestamp;
    private final long endTimestamp;
    private final HistogramConfig histogramConfig;

    public ByteBufferHistogram(
            final ByteBuffer buffer,
            final long startTimestamp,
            final long endTimestamp,
            final HistogramConfig histogramConfig)
    {
        this.buffer = buffer;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.histogramConfig = histogramConfig;
    }

    @Override
    public long getStartTimestamp()
    {
        return startTimestamp;
    }

    @Override
    public long getEndTimestamp()
    {
        return endTimestamp;
    }

    @Override
    public Histogram unpack()
    {
        final Histogram decompressed = decompress();
        decompressed.setStartTimeStamp(startTimestamp);
        decompressed.setEndTimeStamp(endTimestamp);

        return decompressed;
    }

    private Histogram decompress()
    {
        buffer.mark();
        try
        {
            return Histogram.decodeFromCompressedByteBuffer(buffer, histogramConfig.getMaxValue());
        }
        catch (DataFormatException e)
        {
            throw new IllegalArgumentException("Could not decode from buffer", e);
        }
        finally
        {
            buffer.reset();
        }
    }
}
