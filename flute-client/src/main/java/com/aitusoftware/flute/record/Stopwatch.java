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
package com.aitusoftware.flute.record;


import com.aitusoftware.flute.compatibility.LongConsumer;

/**
 * An instance of TimeTracker that models a stop-watch. Useful for wrapping invocations for timing.
 */
public final class Stopwatch implements TimeTracker
{
    private static final long UNSET_TIME = Long.MIN_VALUE;

    private final boolean validating;
    private final LongConsumer durationRecorder;

    private long lastStartTime = UNSET_TIME;

    public Stopwatch(final boolean validating, final LongConsumer durationRecorder)
    {
        this.validating = validating;
        this.durationRecorder = durationRecorder;
    }

    /**
     * Call this before an invocation
     * @param startTime the time, in nanoseconds, before the invocation
     */
    public void begin(final long startTime)
    {
        if(validating && lastStartTime != UNSET_TIME)
        {
            throw new IllegalStateException("Expected lastStartTime to be unset!");
        }

        lastStartTime = startTime;
    }

    /**
     * Call this after an invocation
     * @param endTime the time, in nanoseconds, after the invocation
     */
    public void end(final long endTime)
    {
        if(validating && lastStartTime == UNSET_TIME)
        {
            throw new IllegalStateException("Expected lastStartTime to be set!");
        }

        final long startTime = lastStartTime;
        lastStartTime = UNSET_TIME;
        durationRecorder.accept(endTime - startTime);
    }

    /**
     * Return this Stopwatch as a single-value recorder. Useful for recording durations that have already been calculated.
     * @return the single-value recorder
     */
    @Override
    public LongConsumer asValueRecorder()
    {
        return durationRecorder;
    }
}
