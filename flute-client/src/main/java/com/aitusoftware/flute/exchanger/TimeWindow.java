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
package com.aitusoftware.flute.exchanger;

import java.util.Date;

/**
 * Describes a start and end time in epoch milliseconds.
 */
public final class TimeWindow
{
    private long windowStart;
    private long windowEnd;

    void set(final long windowStart, final long windowEnd)
    {
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }

    public long getWindowStart()
    {
        return windowStart;
    }

    public long getWindowEnd()
    {
        return windowEnd;
    }

    @Override
    public String toString()
    {
        return "TimeWindow{" +
                "windowStart=" + new Date(windowStart) +
                ", windowEnd=" + new Date(windowEnd) +
                '}';
    }
}
