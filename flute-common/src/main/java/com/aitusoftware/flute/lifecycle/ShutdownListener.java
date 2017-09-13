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
package com.aitusoftware.flute.lifecycle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public final class ShutdownListener
{
    private final int port;

    public ShutdownListener(final int port)
    {
        this.port = port;
    }

    public void waitForShutdownEvent()
    {
        ServerSocketChannel serverSocketChannel = null;
        try
        {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(true);
        }
        catch(final IOException e)
        {
            throw new UncheckedIOException("Unable to configure shutdown listener", e);
        }
        try
        {
            serverSocketChannel.accept();
        }
        catch(final IOException e)
        {
            // ignore
        }
    }
}