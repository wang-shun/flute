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
package com.aitusoftware.flute.exchanger;

import com.aitusoftware.flute.collection.LockFreeCopyOnWriteArray;
import com.aitusoftware.flute.compatibility.BiConsumer;
import com.aitusoftware.flute.compatibility.LongSupplier;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class to swap two instances of a type between multiple writers and a single reader thread.
 *
 * @param <T> instance type
 */
public final class MultiWriterTimeBasedValueExchanger<T> implements Exchanger
{
    private final AtomicReference<T> container;
    private final LongSupplier millisecondClock;
    private final long publicationIntervalMillis;
    private final BiConsumer<T, TimeWindow> publishedValueConsumer;
    private final TimeWindow timeWindow = new TimeWindow();
    private final AtomicWriterEpochTracker writerEpochTracker = new AtomicWriterEpochTracker();
    private final LockFreeCopyOnWriteArray<AtomicWriterEpochTracker> epochTrackers =
            new LockFreeCopyOnWriteArray<AtomicWriterEpochTracker>();

    private static final ThreadLocal<EpochState> EPOCH_TRACKER_THREAD_LOCAL =
            new ThreadLocal<EpochState>();

    private T next;
    private long lastPublicationTimestamp;

    /**
     * Constructor
     *
     * @param initial initial value
     * @param next subsequent value
     * @param publishedValueConsumer consumer for swapped-out instance
     * @param millisecondClock provider for epoch milliseconds
     * @param publicationInterval interval between attempts to swap and publish instances
     * @param timeUnit interval unit
     */
    public MultiWriterTimeBasedValueExchanger(final T initial,
                                              final T next,
                                              final BiConsumer<T, TimeWindow> publishedValueConsumer,
                                              final LongSupplier millisecondClock,
                                              final long publicationInterval,
                                              final TimeUnit timeUnit)
    {
        this.container = new AtomicReference<T>(initial);
        this.next = next;
        this.millisecondClock = millisecondClock;
        this.publishedValueConsumer = publishedValueConsumer;
        this.publicationIntervalMillis = timeUnit.toMillis(publicationInterval);
        this.lastPublicationTimestamp = millisecondClock.getAsLong();
    }

    /**
     * Acquire the 'current' instance for writing.
     *
     * MUST be followed by a call to release from the mutating thread.
     *
     * @return the 'current' instance
     */
    public T acquire()
    {
        if(EPOCH_TRACKER_THREAD_LOCAL.get() == null)
        {
            final AtomicWriterEpochTracker epochTracker = new AtomicWriterEpochTracker();
            EPOCH_TRACKER_THREAD_LOCAL.set(new EpochState(epochTracker));
            epochTrackers.add(epochTracker);
        }
        EPOCH_TRACKER_THREAD_LOCAL.get().enterWriteSection();
        return container.get();
    }

    /**
     * Notification from the mutating thread that the current write operation is complete.
     */
    public void release()
    {
        EPOCH_TRACKER_THREAD_LOCAL.get().exitWriteSection();
    }

    /**
     * Called by the reader thread. If publication interval has expired, will attempt
     * to swap the 'current' instance, and publish the most-recently-updated value.
     */
    public void poll()
    {
        final long currentMillis = millisecondClock.getAsLong();
        if(currentMillis >= lastPublicationTimestamp + publicationIntervalMillis)
        {
            final T current = container.get();
            container.compareAndSet(current, next);
            writerEpochTracker.awaitPendingWrite(1L);

            timeWindow.set(lastPublicationTimestamp, currentMillis);
            publishedValueConsumer.accept(current, timeWindow);
            next = current;
            lastPublicationTimestamp = currentMillis;
        }
    }

    private static final class EpochState
    {
        private final AtomicWriterEpochTracker epochTracker;
        private long currentEpoch;

        private EpochState(final AtomicWriterEpochTracker epochTracker)
        {
            this.epochTracker = epochTracker;
        }

        private void enterWriteSection()
        {
            currentEpoch = epochTracker.enterWriteSection();
        }

        private void exitWriteSection()
        {
            epochTracker.exitWriteSection(currentEpoch);
        }
    }
}