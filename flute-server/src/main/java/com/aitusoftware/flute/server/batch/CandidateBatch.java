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
package com.aitusoftware.flute.server.batch;

import java.util.concurrent.TimeUnit;

public final class CandidateBatch
{
    private final long windowLength;
    private final TimeUnit windowUnit;
    private final String metricName;
    private final long epochMillisStart;
    private final long epochMillisEnd;

    CandidateBatch(final long windowLength, final TimeUnit windowUnit,
                   final String metricName, final long epochMillisStart, final long epochMillisEnd)
    {
        this.windowLength = windowLength;
        this.windowUnit = windowUnit;
        this.metricName = metricName;
        this.epochMillisStart = epochMillisStart;
        this.epochMillisEnd = epochMillisEnd;
    }

    TimeUnit getWindowUnit()
    {
        return windowUnit;
    }

    long getWindowLength()
    {
        return windowLength;
    }
}