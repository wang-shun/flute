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
package com.aitusoftware.flute.send;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AggregatingDataSenderTest
{
    private final AggregatingDataSender sender = new AggregatingDataSender(32, 16);

    @Test
    public void shouldEnqueueData() throws Exception
    {
        final ByteBuffer item = ByteBuffer.wrap("foobar".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.enqueue(item), is(true));

        final byte[] expected = item.array();

        final ByteBuffer target = ByteBuffer.allocate(1024);
        sender.send(new CapturingWritableByteChannel(target));

        assertThat(arraysAreEqualAtBeginning(expected, target.array()), is(true));
    }

    @Test
    public void shouldEnqueueMoreData() throws Exception
    {
        final ByteBuffer item = ByteBuffer.wrap("foobar".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.enqueue(item), is(true));
        final ByteBuffer itemTwo = ByteBuffer.wrap("0x1dea".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.enqueue(itemTwo), is(true));

        final byte[] expected = "foobar0x1dea".getBytes(StandardCharsets.UTF_8);

        final ByteBuffer target = ByteBuffer.allocate(1024);
        sender.send(new CapturingWritableByteChannel(target));

        assertThat(arraysAreEqualAtBeginning(expected, target.array()), is(true));
    }

    @Test
    public void shouldSendPartialData() throws Exception
    {
        final ByteBuffer item = ByteBuffer.wrap("foobar".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.enqueue(item), is(true));
        final ByteBuffer itemTwo = ByteBuffer.wrap("0x1dea".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.enqueue(itemTwo), is(true));

        byte[] expected = "foobar0x1dea".getBytes(StandardCharsets.UTF_8);

        ByteBuffer target = ByteBuffer.allocate(3);
        sender.send(new CapturingWritableByteChannel(target));

        assertThat(arraysAreEqualAtBeginning(expected, target.array()), is(true));

        expected = "bar0x1dea".getBytes(StandardCharsets.UTF_8);
        target = ByteBuffer.allocate(32);
        sender.send(new CapturingWritableByteChannel(target));

        assertThat(arraysAreEqualAtBeginning(expected, target.array()), is(true));
    }

    @Test
    public void shouldGrowInternalBufferAsRequired() throws Exception
    {
        final ByteBuffer item = ByteBuffer.wrap("01234567890123456789".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.enqueue(item), is(true));
    }

    @Test
    public void shouldNotGrowInternalBufferPastMaximumValue() throws Exception
    {
        final ByteBuffer item = ByteBuffer.wrap("01234567890123456789".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.enqueue(item), is(true));
        final ByteBuffer itemTwo = ByteBuffer.wrap("0123456789012".getBytes(StandardCharsets.UTF_8));
        assertThat(sender.enqueue(itemTwo), is(false));
    }

    private static boolean arraysAreEqualAtBeginning(final byte[] expected, final byte[] actual)
    {
        for(int i = 0; i < Math.min(expected.length, actual.length); i++)
        {
            if(expected[i] != actual[i])
            {
                return false;
            }
        }
        return true;
    }

    private static final class CapturingWritableByteChannel implements WritableByteChannel
    {
        private final ByteBuffer target;

        private CapturingWritableByteChannel(final ByteBuffer target)
        {
            this.target = target;
        }

        @Override
        public int write(final ByteBuffer src) throws IOException
        {
            final int previousLimit = src.limit();
            src.limit(Math.min(target.remaining(), src.limit()));
            target.put(src);
            src.limit(previousLimit);
            return target.position();
        }

        @Override
        public boolean isOpen()
        {
            return true;
        }

        @Override
        public void close() throws IOException
        {

        }
    }
}