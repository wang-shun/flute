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

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.locks.LockSupport;

final class AtomicWriterEpochTracker
{
    private volatile long publishedEpoch = 0L;
    private volatile long counter = 0L;

    private static final AtomicLongFieldUpdater<AtomicWriterEpochTracker> COUNTER_UPDATER =
            AtomicLongFieldUpdater.newUpdater(AtomicWriterEpochTracker.class, "counter");
    private static final AtomicLongFieldUpdater<AtomicWriterEpochTracker> PUBLISHED_EPOCH_UPDATER =
            AtomicLongFieldUpdater.newUpdater(AtomicWriterEpochTracker.class, "publishedEpoch");

    long enterWriteSection()
    {
        return COUNTER_UPDATER.incrementAndGet(this);
    }

    void exitWriteSection(final long value)
    {
        PUBLISHED_EPOCH_UPDATER.lazySet(this, value);
    }

    void awaitPendingWrite(final long yieldTimeNanos)
    {
        final long latestAcquiredWriteEpoch = COUNTER_UPDATER.get(this);
        while(getPublishedEpoch() < latestAcquiredWriteEpoch)
        {
            LockSupport.parkNanos(yieldTimeNanos);
        }
    }

    long getPublishedEpoch()
    {
        return PUBLISHED_EPOCH_UPDATER.get(this);
    }
}
