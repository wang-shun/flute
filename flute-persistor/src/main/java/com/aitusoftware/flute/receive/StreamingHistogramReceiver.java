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
package com.aitusoftware.flute.receive;

import com.aitusoftware.flute.protocol.HeaderInfo;
import com.aitusoftware.flute.protocol.InFlightHistogramPayload;
import com.aitusoftware.flute.protocol.Version;
import com.aitusoftware.flute.protocol.VersionCodec;
import org.HdrHistogram.Histogram;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StreamingHistogramReceiver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingHistogramReceiver.class);
    private static final int PAYLOAD_SIZE_NOT_SET = -1;
    private static final int BYTE_LENGTH_OF_INT = 4;
    private final HistogramHandler histogramHandler;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(InFlightHistogramPayload.MAX_IN_FLIGHT_MESSAGE_SIZE);
    private final HeaderInfo headerInfo = new HeaderInfo();
    private final long highestTrackableValue;
    private boolean hasValidatedVersion;
    private int expectedPayloadSize = PAYLOAD_SIZE_NOT_SET;

    public StreamingHistogramReceiver(final HistogramHandler histogramHandler, final long highestTrackableValue)
    {
        this.histogramHandler = histogramHandler;
        this.highestTrackableValue = highestTrackableValue;
    }

    public ReadResult readFrom(final ReadableByteChannel channel, final InetSocketAddress socketAddress) throws IOException
    {
        final int read = channel.read(buffer);
        if(read == -1)
        {
            return ReadResult.END_OF_STREAM;
        }

        buffer.flip();
        if(!hasValidatedVersion && buffer.remaining() >= VersionCodec.FORMATTED_VERSION_LENGTH)
        {
            final Version version = VersionCodec.parseFrom(buffer);
            if(version != Version.ONE)
            {
                throw new IllegalStateException("Unknown version: " + version);
            }
            hasValidatedVersion = true;
        }
        boolean shouldContinue = true;

        while(shouldContinue)
        {
            if(expectedPayloadSize == PAYLOAD_SIZE_NOT_SET && buffer.remaining() > BYTE_LENGTH_OF_INT)
            {
                expectedPayloadSize = buffer.getInt();
            }

            if(expectedPayloadSize != PAYLOAD_SIZE_NOT_SET && buffer.remaining() >= expectedPayloadSize)
            {
                headerInfo.set(buffer);
                final Histogram histogram = Histogram.decodeFromByteBuffer(buffer, highestTrackableValue);

                if(LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Received histogram for {}, sample count {}",
                        headerInfo.getIdentifier(), histogram.getTotalCount());
                }

                histogramHandler.histogramReceived(
                        socketAddress,
                        headerInfo.getIdentifier(),
                        headerInfo.getWindowStartTimestamp(),
                        headerInfo.getWindowEndTimestamp(),
                        histogram);

                expectedPayloadSize = PAYLOAD_SIZE_NOT_SET;
            }
            else
            {
                shouldContinue = false;
            }

        }

        buffer.compact();
        return ReadResult.OK;
    }
}