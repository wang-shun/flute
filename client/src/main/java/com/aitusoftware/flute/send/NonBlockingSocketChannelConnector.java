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

import com.aitusoftware.flute.compatibility.Supplier;
import com.aitusoftware.flute.send.events.AggregatorEvents;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public final class NonBlockingSocketChannelConnector
{
    private final Supplier<SocketChannel> socketChannelSupplier;
    private final AggregatorEvents aggregatorEvents;

    public NonBlockingSocketChannelConnector(final Supplier<SocketChannel> socketChannelSupplier,
                                             final AggregatorEvents aggregatorEvents)
    {
        this.socketChannelSupplier = socketChannelSupplier;
        this.aggregatorEvents = aggregatorEvents;
    }

    public SocketChannel registerSenderWithSocket()
    {
        try
        {
            final SocketChannel channel = socketChannelSupplier.get();
            if(channel == null)
            {
                return null;
            }
            channel.configureBlocking(false);
            // TODO this needs some kind of back-off
            /*
                    at java.nio.channels.SocketChannel.open(SocketChannel.java:189)
        at com.aitusoftware.flute.send.SocketChannelConnector.get(SocketChannelConnector.java:41)
        at com.aitusoftware.flute.send.SocketChannelConnector.get(SocketChannelConnector.java:25)
        at com.aitusoftware.flute.send.NonBlockingSocketChannelConnector.registerSenderWithSocket(NonBlockingSocketChannelConnector.java:40)
        at com.aitusoftware.flute.send.NonBlockingAggregator.tryConnect(NonBlockingAggregator.java:133)
        at com.aitusoftware.flute.send.NonBlockingAggregator.access$200(NonBlockingAggregator.java:33)
        at com.aitusoftware.flute.send.NonBlockingAggregator$1.accept(NonBlockingAggregator.java:73)
        at com.aitusoftware.flute.send.NonBlockingAggregator$1.accept(NonBlockingAggregator.java:63)
        at com.aitusoftware.flute.collection.LockFreeCopyOnWriteArray.forEach(LockFreeCopyOnWriteArray.java:73)
             */
            return channel;
        }
        catch (IOException e)
        {
            aggregatorEvents.failedToRegisterSenderWithSocket(e);
        }

        return null;
    }
}