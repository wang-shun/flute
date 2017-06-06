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
package com.aitusoftware.flute.protocol;

import java.nio.ByteBuffer;

public final class VersionCodec
{
    public static final int FORMATTED_VERSION_LENGTH = 5;

    private VersionCodec()
    {
    }

    public static ByteBuffer asBuffer(final Version version)
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(FORMATTED_VERSION_LENGTH);
        formatTo(version, buffer);
        return buffer;
    }

    public static void formatTo(final Version version, final ByteBuffer target)
    {
        target.putInt(Version.MAGIC);
        target.put(version.getId());
    }

    public static Version parseFrom(final ByteBuffer source)
    {
        final int magic = source.getInt();

        if(magic != Version.MAGIC)
        {
            throw new IllegalArgumentException("Invalid magic number");
        }

        final byte id = source.get();
        return Version.fromId(id);
    }
}
