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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class BatchCalculator
{
    private final CandidateBatch batch;

    public BatchCalculator(final CandidateBatch batch)
    {
        this.batch = batch;


    }

    TimeUnit getTargetRollUpUnit()
    {
        return getBatchedTimeUnit(batch.getWindowUnit());
    }

    long getTargetRollUpLength()
    {
        return getBatchedTimeLength(batch);
    }

    List<CandidateBatch> rollUp()
    {
        return Collections.emptyList();
    }

    private long getBatchedTimeLength(final CandidateBatch batch)
    {
        final long batchLengthSeconds = batch.getWindowUnit().toSeconds(batch.getWindowLength());

        if (batchLengthSeconds < TimeUnit.HOURS.toSeconds(1))
        {
            return 1L;
        }

        throw new UnsupportedOperationException();
    }

    private static TimeUnit getBatchedTimeUnit(final TimeUnit input)
    {
        switch (input)
        {
            case NANOSECONDS:
            case MICROSECONDS:
            case MILLISECONDS:
                throw new IllegalArgumentException(String.format("TimeUnit %s not supported", input));
            case SECONDS:
            case MINUTES:
                return TimeUnit.HOURS;
            case HOURS:
                return TimeUnit.DAYS;
            default:
                throw new IllegalStateException(String.format("Unknown TimeUnit: %s", input));
        }
    }
}