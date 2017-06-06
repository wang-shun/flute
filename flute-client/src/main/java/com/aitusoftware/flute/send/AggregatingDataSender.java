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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public final class AggregatingDataSender implements Sender
{
    private final int maxBufferSize;
    private ByteBuffer buffer;

    public AggregatingDataSender(final int maxBufferSize, final int initialSize)
    {
        this.maxBufferSize = maxBufferSize;
        buffer = ByteBuffer.allocateDirect(initialSize);
    }

    @Override
    public boolean enqueue(final ByteBuffer content)
    {
        if (buffer.position() + content.remaining() > maxBufferSize)
        {
            return false;
        }
        if (content.remaining() > buffer.remaining())
        {
            int newCapacity = buffer.capacity();
            while (newCapacity < content.remaining())
            {
                newCapacity *= 2;
            }
            buffer = ByteBuffer.allocateDirect(newCapacity);
        }
        buffer.put(content);
        return true;
    }

    public void send(final WritableByteChannel channel) throws IOException
    {
        buffer.flip();
        if(!channel.isOpen())
        {
            throw new IOException("Channel closed");
        }
        if(buffer.remaining() != 0)
        {
            channel.write(buffer);
        }
        buffer.compact();
    }

    void clear()
    {
        buffer.clear();
    }
}