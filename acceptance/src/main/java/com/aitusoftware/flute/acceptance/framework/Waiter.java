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
package com.aitusoftware.flute.acceptance.framework;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public final class Waiter
{
    private static final long TIMEOUT_MILLIS = 10000L;

    public static void waitFor(final Supplier<Boolean> condition)
    {
        try
        {
            final long start = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted() &&
                    System.currentTimeMillis() < start + TIMEOUT_MILLIS &&
                    !condition.get())
            {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
            }

            if (!condition.get())
            {
                Assert.fail("Condition not met.");
            }
        }
        catch(Exception e)
        {
            Assert.fail("Condition threw exception: " + e.getMessage());
        }
    }
}
