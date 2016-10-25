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
package com.aitusoftware.flute.agent.intercept;

import com.aitusoftware.flute.record.TimeTracker;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public final class TimeTrackerInterceptor
{
    private final TimeTracker timeTracker;

    TimeTrackerInterceptor(final TimeTracker timeTracker)
    {
        this.timeTracker = timeTracker;
    }

    @RuntimeType
    public Object intercept(@Origin final Method m, @SuperCall final Callable<?> superCall) throws Exception
    {
        final long startTime = System.nanoTime();
        try
        {
            return superCall.call();
        }
        finally
        {
            timeTracker.begin(startTime);
            final long endTime = System.nanoTime();
            timeTracker.end(endTime);
        }
    }
}
