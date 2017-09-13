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
package com.aitusoftware.flute.protocol;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertThat;

public class VersionCodecTest
{
    private final ByteBuffer buffer = ByteBuffer.allocate(8);

    @Test
    public void shouldEncodeAndDecode() throws Exception
    {
        VersionCodec.formatTo(Version.ONE, buffer);
        buffer.flip();
        assertThat(VersionCodec.parseFrom(buffer), CoreMatchers.is(Version.ONE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfBufferDoesNotStartWithMagicNumber() throws Exception
    {
        VersionCodec.parseFrom(buffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfVersionDoesNotExist() throws Exception
    {
        buffer.putInt(Version.MAGIC);
        buffer.put((byte) 255);
        buffer.flip();

        VersionCodec.parseFrom(buffer);
    }
}