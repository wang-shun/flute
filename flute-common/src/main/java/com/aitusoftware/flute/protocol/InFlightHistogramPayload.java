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
package com.aitusoftware.flute.protocol;

import org.HdrHistogram.Histogram;

import java.nio.ByteBuffer;

public final class InFlightHistogramPayload
{
    public static final int MAX_ENCODED_HISTOGRAM_SIZE = 1_048_576;
    public static final int MAX_HEADER_SIZE = HeaderInfo.MAX_IDENTIFIER_LENGTH + 16;
    public static final int MAX_IN_FLIGHT_MESSAGE_SIZE = MAX_HEADER_SIZE + MAX_ENCODED_HISTOGRAM_SIZE;
    private static final int ID_BYTES_LENGTH_INDICATOR_LENGTH = 4;
    private static final int TIMESTAMP_LENGTH = 8;

    public int requiredSpace(final Histogram histogram, final byte[] identifierBytes)
    {
        return calculateHeaderLength(identifierBytes) + histogram.getEstimatedFootprintInBytes();
    }

    public void encode(
            final Histogram histogram,
            final long startTimestamp,
            final long endTimestamp,
            final byte[] identifierBytes,
            final ByteBuffer buffer)
    {
        final int startPosition = buffer.position();
        buffer.putInt(Integer.MIN_VALUE);
        buffer.putInt(identifierBytes.length);
        buffer.put(identifierBytes);
        buffer.putLong(startTimestamp);
        buffer.putLong(endTimestamp);
        final int encodedLength = histogram.encodeIntoByteBuffer(buffer);
        buffer.putInt(startPosition, encodedLength + calculateHeaderLength(identifierBytes));
        buffer.flip();
    }

    private static int calculateHeaderLength(final byte[] identifierBytes)
    {
        return identifierBytes.length +
                ID_BYTES_LENGTH_INDICATOR_LENGTH +
                TIMESTAMP_LENGTH +
                TIMESTAMP_LENGTH;
    }
}
