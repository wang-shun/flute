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
package com.aitusoftware.flute.server.cache;

import org.HdrHistogram.Histogram;

public final class UncompressedHistogram implements CompressedHistogram
{
    private final long startTimestamp;
    private final long endTimestamp;
    private final Histogram histogram;

    public UncompressedHistogram(final Histogram histogram)
    {
        this.startTimestamp = histogram.getStartTimeStamp();
        this.endTimestamp = histogram.getEndTimeStamp();
        this.histogram = histogram;
    }

    @Override
    public long getStartTimestamp()
    {
        return startTimestamp;
    }

    @Override
    public long getEndTimestamp()
    {
        return endTimestamp;
    }

    @Override
    public Histogram unpack()
    {
        return histogram;
    }
}