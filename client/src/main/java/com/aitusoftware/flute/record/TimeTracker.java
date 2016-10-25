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
package com.aitusoftware.flute.record;

import com.aitusoftware.flute.compatibility.LongConsumer;

/**
 * Basic interface for recording invocation start/end times.
 */
public interface TimeTracker
{
    /**
     * Call this before an invocation
     * @param startTime the time, in nanoseconds, before the invocation
     */
    void begin(final long startTime);

    /**
     * Call this after an invocation
     * @param endTime the time, in nanoseconds, after the invocation
     */
    void end(final long endTime);

    /**
     * Return this Stopwatch as a single-value recorder. Useful for recording durations that have already been calculated.
     * @return the single-value recorder
     */
    LongConsumer asValueRecorder();
}
