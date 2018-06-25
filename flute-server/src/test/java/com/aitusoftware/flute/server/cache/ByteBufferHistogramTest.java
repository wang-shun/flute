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
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ByteBufferHistogramTest
{
    private final HistogramConfig config = new HistogramConfig(100, 3);
    private final ByteBuffer data = ByteBuffer.allocate(1024);
    private ByteBufferHistogram bufferHistogram;

    @Before
    public void setUp() throws Exception
    {
        final Histogram histogram = config.asSupplier().get();
        histogram.recordValue(5L);
        histogram.recordValue(7L);
        histogram.recordValue(11L);
        histogram.recordValue(13L);
        histogram.recordValue(17L);
        histogram.recordValue(23L);

        histogram.encodeIntoCompressedByteBuffer(data);
        data.flip();

        bufferHistogram = new ByteBufferHistogram(data, 1L, 37L, config);
    }

    @Test
    public void shouldCreateNewHistogram()
    {
        assertThat(bufferHistogram.unpack().getTotalCount(), is(6L));
        assertThat(bufferHistogram.unpack().getTotalCount(), is(6L));
    }
}