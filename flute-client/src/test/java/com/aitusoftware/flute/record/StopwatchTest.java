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
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class StopwatchTest
{
    private static final long START_TIME = 17L;
    private static final long END_TIME = 37L;

    private final AtomicLong durationReceiver = new AtomicLong();
    private final LongConsumer durationRecorder = new LongConsumer()
    {
        @Override
        public void accept(final long value)
        {
            durationReceiver.set(value);
        }
    };
    private final Stopwatch stopwatch = new Stopwatch(true, durationRecorder);

    @Test
    public void shouldRecordDuration() throws Exception
    {
        stopwatch.begin(START_TIME);
        stopwatch.end(END_TIME);

        assertThat(durationReceiver.get(), is(END_TIME - START_TIME));
    }
}