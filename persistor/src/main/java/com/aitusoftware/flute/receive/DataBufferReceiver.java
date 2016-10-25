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
package com.aitusoftware.flute.receive;

import com.aitusoftware.flute.protocol.HeaderInfo;
import org.HdrHistogram.Histogram;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.nio.ByteBuffer.allocateDirect;

@Deprecated
public final class DataBufferReceiver
{
    private final ByteBuffer buffer;
    private final DatagramChannel datagramChannel;
    private final Consumer<Exception> exceptionConsumer;
    private final Map<CharSequence, Histogram> trackedHistograms;
    private final Supplier<Histogram> histogramSupplier;
    private final HeaderInfo headerInfo;
    private final HistogramHandler histogramHandler;
    private final HistogramPublicationPredicate recordingPredicate;

    public DataBufferReceiver(
            final DatagramChannel datagramChannel,
            final int mtuSize,
            final Consumer<Exception> exceptionConsumer,
            final Supplier<Histogram> histogramSupplier,
            final HistogramHandler histogramHandler,
            final HistogramPublicationPredicate recordingPredicate)
    {

        this.datagramChannel = datagramChannel;
        this.exceptionConsumer = exceptionConsumer;
        this.histogramHandler = histogramHandler;
        this.recordingPredicate = recordingPredicate;
        this.trackedHistograms = new HashMap<>();
        this.histogramSupplier = histogramSupplier;
        buffer = allocateDirect(mtuSize);
        headerInfo = new HeaderInfo();
    }

    public void process()
    {
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                buffer.clear();
                final SocketAddress sender = datagramChannel.receive(buffer);
                buffer.flip();

                headerInfo.set(buffer);
                final Histogram histogram = trackedHistograms.computeIfAbsent(headerInfo.getIdentifier(),
                        k -> histogramSupplier.get());

                while(buffer.remaining() != 0)
                {
                    final long value = buffer.getLong();
                    histogram.recordValue(Math.min(value, histogram.getHighestTrackableValue()));
                }

                if(recordingPredicate.test(histogram, headerInfo.getWindowStartTimestamp()))
                {
                    histogramHandler.histogramReceived((InetSocketAddress) sender, headerInfo.getIdentifier(),
                            headerInfo.getWindowStartTimestamp(),
                            headerInfo.getWindowEndTimestamp(), histogram);

                    histogram.reset();
                }
            }
            catch (final IOException e)
            {
                exceptionConsumer.accept(e);
            }
        }
    }
}