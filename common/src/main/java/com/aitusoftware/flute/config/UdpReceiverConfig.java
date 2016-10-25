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
package com.aitusoftware.flute.config;

import java.net.SocketAddress;
import java.util.Properties;

import static com.aitusoftware.flute.config.RequiredProperties.requiredProperty;
import static com.aitusoftware.flute.config.SocketAddressParser.fromAddressSpec;
import static java.lang.Integer.parseInt;

public final class UdpReceiverConfig
{
    private final SocketAddress socketAddress;
    private final int mtuSize;

    public UdpReceiverConfig(final SocketAddress socketAddress, final int mtuSize)
    {
        this.socketAddress = socketAddress;
        this.mtuSize = mtuSize;
    }

    public SocketAddress getSocketAddress()
    {
        return socketAddress;
    }

    public int getMtuSize()
    {
        return mtuSize;
    }

    public static UdpReceiverConfig fromFluteProperties(final Properties properties)
    {
        return new UdpReceiverConfig(
                fromAddressSpec(requiredProperty("flute.server.udp.listenAddress", properties)),
                parseInt(requiredProperty("flute.mtuSize", properties)));
    }
}