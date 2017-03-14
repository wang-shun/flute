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

import com.aitusoftware.flute.collection.LockFreeCopyOnWriteArray;
import com.aitusoftware.flute.compatibility.Supplier;
import com.aitusoftware.flute.exchanger.Exchanger;
import com.aitusoftware.flute.protocol.Version;
import com.aitusoftware.flute.protocol.VersionCodec;
import com.aitusoftware.flute.send.events.AggregatorEvents;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class NonBlockingAggregator implements Runnable
{
    private final LockFreeCopyOnWriteArray<SocketChannelAndExchanger> exchangers =
            new LockFreeCopyOnWriteArray<>();
    private final NonBlockingSocketChannelConnector socketChannelConnector;
    private final long pollInterval;
    private final TimeUnit pollUnit;
    private final AggregatorEvents aggregatorEvents;

    public NonBlockingAggregator(
            final Supplier<SocketChannel> socketChannelSupplier,
            final long pollInterval,
            final TimeUnit pollUnit,
            final AggregatorEvents aggregatorEvents)
    {
        this.aggregatorEvents = aggregatorEvents;
        this.pollInterval = pollInterval;
        this.pollUnit = pollUnit;
        socketChannelConnector = new NonBlockingSocketChannelConnector(socketChannelSupplier, aggregatorEvents);
    }

    @Override
    public void run()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            exchangers.forEach(this::pollExchanger);
            exchangers.forEach(this::sendPendingData);

            LockSupport.parkNanos(pollUnit.toNanos(pollInterval));
        }
    }

    private void pollExchanger(final SocketChannelAndExchanger unit)
    {
        try
        {
            unit.exchanger.poll();
            if (unit.socketChannel == null)
            {
                tryConnect(unit);
            }
        }
        catch (RuntimeException e)
        {
            logExceptionInSendLoop(e);
        }
    }

    private void sendPendingData(final SocketChannelAndExchanger unit)
    {
        try
        {
            try
            {
                if (unit.socketChannel != null)
                {
                    final WritableByteChannel dataSink = unit.socketChannel;
                    if (unit.needsToSendVersion())
                    {
                        unit.writeVersion(dataSink);
                    }
                    else
                    {
                        unit.sender.send(dataSink);
                    }
                }
            }
            catch (final IOException e)
            {
                unit.socketChannel = null;
                unit.sender.clear();
                reportFailureToSendData(unit, e);
            }

        }
        catch (RuntimeException e)
        {
            logExceptionInSendLoop(e);
        }
    }

    private void reportFailureToSendData(final SocketChannelAndExchanger unit, final IOException e)
    {
        aggregatorEvents.failedToSendDataForSender(unit.identifier, e);
    }

    private void logExceptionInSendLoop(final RuntimeException e)
    {
        aggregatorEvents.exceptionInSendLoop(e);
    }

    private void tryConnect(final SocketChannelAndExchanger unit)
    {
        final SocketChannel socketChannel = socketChannelConnector.registerSenderWithSocket();
        unit.reset(socketChannel);
    }

    public void register(final Exchanger exchanger, final AggregatingDataSender sender, final String identifier)
    {
        exchangers.add(new SocketChannelAndExchanger(exchanger, sender, identifier));
    }

    private static final class SocketChannelAndExchanger
    {
        private final Exchanger exchanger;
        private final AggregatingDataSender sender;
        private final String identifier;
        private final ByteBuffer version = VersionCodec.asBuffer(Version.ONE);
        private SocketChannel socketChannel;


        private SocketChannelAndExchanger(
                final Exchanger exchanger,
                final AggregatingDataSender sender,
                final String identifier)
        {
            this.exchanger = exchanger;
            this.sender = sender;
            this.identifier = identifier;
            version.clear();
        }

        private boolean needsToSendVersion()
        {
            return version.remaining() != 0;
        }

        private void reset(final SocketChannel socketChannel)
        {
            this.socketChannel = socketChannel;
            version.clear();
        }

        private void writeVersion(final WritableByteChannel channel) throws IOException
        {
            channel.write(version);
        }
    }
}
