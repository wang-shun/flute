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

import com.aitusoftware.flute.protocol.Version;
import com.aitusoftware.flute.protocol.VersionCodec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Supplier;

public final class VersionedSocketChannelConnector implements Supplier<WritableByteChannel>
{
    private final Supplier<SocketChannel> socketChannelConnector;
    private final ByteBuffer versionHeader = ByteBuffer.allocateDirect(VersionCodec.FORMATTED_VERSION_LENGTH);

    public VersionedSocketChannelConnector(final Supplier<SocketChannel> socketChannelConnector,
                                           final Version version)
    {
        this.socketChannelConnector = socketChannelConnector;
        VersionCodec.formatTo(version, versionHeader);
        versionHeader.flip();
        versionHeader.mark();
    }

    @Override
    public WritableByteChannel get()
    {
        final SocketChannel socketChannel = socketChannelConnector.get();
        try
        {
            socketChannel.write(versionHeader);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            versionHeader.reset();
        }
        return socketChannel;
    }
}
