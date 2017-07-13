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
package com.aitusoftware.flute.send;

import com.aitusoftware.flute.compatibility.BiConsumer;
import com.aitusoftware.flute.compatibility.Utf8Charset;
import com.aitusoftware.flute.exchanger.TimeWindow;
import com.aitusoftware.flute.send.events.AggregatorEvents;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public final class HistogramPublisher implements BiConsumer<Histogram, TimeWindow>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HistogramPublisher.class);
    private static final int ID_BYTES_LENGTH_INDICATOR_LENGTH = 4;
    private static final int TIMESTAMP_LENGTH = 8;

    private final Sender sender;
    private final byte[] identifierBytes;
    private final int headerLength;
    private final int payloadLengthOffset;
    private final AggregatorEvents aggregatorEvents;
    private final String identifier;
    private ByteBuffer buffer = ByteBuffer.allocateDirect(4096);

    public HistogramPublisher(final Sender sender,
                              final String identifier,
                              final AggregatorEvents aggregatorEvents)
    {
        this.sender = sender;
        this.identifier = identifier;
        this.identifierBytes = identifier.getBytes(Utf8Charset.UTF_8);
        this.aggregatorEvents = aggregatorEvents;
        headerLength = identifierBytes.length +
                ID_BYTES_LENGTH_INDICATOR_LENGTH +
                TIMESTAMP_LENGTH +
                TIMESTAMP_LENGTH;
        payloadLengthOffset = 0;
    }

    public void publish(final Histogram histogram, final TimeWindow timeWindow)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Publish histogram of size {} for window {}", histogram.getTotalCount(), timeWindow);
        }
        try
        {
            final int estimatedFootprint = histogram.getEstimatedFootprintInBytes();
            if (buffer.capacity() < estimatedFootprint + headerLength)
            {
                buffer = ByteBuffer.allocateDirect(2 * (estimatedFootprint + headerLength));
            }
            prepareBuffer(timeWindow, buffer);
            final int encodedLength = histogram.encodeIntoByteBuffer(buffer);
            buffer.putInt(payloadLengthOffset, encodedLength + headerLength);
            buffer.flip();
            if(!sender.enqueue(buffer))
            {
                aggregatorEvents.sendQueueBufferOverflow(identifier, buffer);
            }
        }
        finally
        {
            histogram.reset();
        }
    }

    private void prepareBuffer(final TimeWindow timeWindow,
                               final ByteBuffer buffer)
    {
        buffer.clear();
        buffer.putInt(Integer.MIN_VALUE);
        buffer.putInt(identifierBytes.length);
        buffer.put(identifierBytes);
        buffer.putLong(timeWindow.getWindowStart());
        buffer.putLong(timeWindow.getWindowEnd());
    }

    @Override
    public void accept(final Histogram histogram, final TimeWindow timeWindow)
    {
        publish(histogram, timeWindow);
    }
}