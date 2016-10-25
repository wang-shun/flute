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

public final class HeaderInfo
{
    public static final int MAX_IDENTIFIER_LENGTH = 1400;
    private final byte[] tmp = new byte[MAX_IDENTIFIER_LENGTH];
    private CharSequence identifier;
    private long windowStartTimestamp;
    private long windowEndTimestamp;

    public void set(final ByteBuffer buffer)
    {
        final int identifierLength = buffer.getInt();
        buffer.get(tmp, 0, identifierLength);
        identifier = new String(tmp, 0, identifierLength);
        windowStartTimestamp = buffer.getLong();
        windowEndTimestamp = buffer.getLong();
    }

    public CharSequence getIdentifier()
    {
        return identifier;
    }

    public long getWindowStartTimestamp()
    {
        return windowStartTimestamp;
    }

    public long getWindowEndTimestamp()
    {
        return windowEndTimestamp;
    }
}