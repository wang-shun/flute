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
package com.aitusoftware.flute.config;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class SocketAddressParser
{
    private SocketAddressParser() {}

    public static SocketAddress fromAddressSpec(final String socketAddress)
    {
        if(socketAddress.indexOf(':') < 0)
        {
            throw new IllegalArgumentException("address specification should be in the format 'address:port'");
        }

        final String[] addressParts = socketAddress.split(":");

        return new InetSocketAddress(addressParts[0], parsePort(addressParts[1]));
    }

    private static int parsePort(final String portSpecification)
    {
        try
        {
            return Integer.parseInt(portSpecification);
        }
        catch(final RuntimeException e)
        {
            throw new IllegalArgumentException("Failed to parse port from string: " + portSpecification);
        }
    }
}