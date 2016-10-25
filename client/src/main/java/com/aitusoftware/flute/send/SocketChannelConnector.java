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
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public final class SocketChannelConnector implements Supplier<SocketChannel>
{
    private final SocketAddress address;
    private final AggregatorEvents aggregatorEvents;

    public SocketChannelConnector(final SocketAddress address, final AggregatorEvents aggregatorEvents)
    {
        this.address = address;
        this.aggregatorEvents = aggregatorEvents;
    }

    @Override
    public SocketChannel get()
    {
        try
        {
            return SocketChannel.open(address);
        }
        catch (IOException e)
        {
            aggregatorEvents.failedToConnectToPersistor(e);
            return null;
        }
    }
}