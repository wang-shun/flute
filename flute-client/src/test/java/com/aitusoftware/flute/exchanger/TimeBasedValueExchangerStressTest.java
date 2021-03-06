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

import com.aitusoftware.flute.compatibility.BiConsumer;
import com.aitusoftware.flute.util.timing.SystemEpochMillisTimeSupplier;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public final class TimeBasedValueExchangerStressTest
{
    private final MutableLong first = new MutableLong();
    private final MutableLong second = new MutableLong();
    private final Aggregator aggregator = new Aggregator(first, second);
    private final TimeBasedValueExchanger<MutableLong> exchanger =
            new TimeBasedValueExchanger<MutableLong>(first, second,
                    aggregator, new SystemEpochMillisTimeSupplier(), 10L, TimeUnit.MILLISECONDS);

    private static final class MutableLong
    {
        private long value;

        void increment()
        {
            value++;
        }

        long getValue()
        {
            return value;
        }

        void reset()
        {
            value = 0;
        }
    }

    @Test
    public void shouldRecordAllUpdates() throws Exception
    {
        long timeout = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
        long counter = 0;

        final Thread consumer = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    exchanger.poll();
                }
            }
        });

        consumer.start();
        long totalIncrementCount = 0;

        while(!Thread.currentThread().isInterrupted())
        {
            exchanger.acquire().increment();
            exchanger.release();
            totalIncrementCount++;

            if((counter++ & 8191) == 0)
            {
                if(System.nanoTime() > timeout)
                {
                    break;
                }
            }
        }

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50L));

        consumer.interrupt();
        consumer.join();

        assertThat(aggregator.totalIncrementCount.get(), is(totalIncrementCount));
        assertThat(aggregator.totalIncrementCount.get(), is(not(0L)));
    }

    @Test
    public void shouldSeeAllUpdatesWhenSingleThreaded() throws Exception
    {
        final long timeout = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
        long counter = 0;
        long totalIncrementCount = 0;

        while(!Thread.currentThread().isInterrupted())
        {
            exchanger.acquire().increment();
            exchanger.release();
            totalIncrementCount++;
            exchanger.poll();

            if((counter++ & 8191) == 0)
            {
                if(System.nanoTime() > timeout)
                {
                    break;
                }
            }
        }

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50L));
        exchanger.poll();

        assertThat(aggregator.totalIncrementCount.get(), is(totalIncrementCount));
        assertThat(aggregator.totalIncrementCount.get(), is(not(0L)));
    }

    private static final class Aggregator implements BiConsumer<MutableLong, TimeWindow>
    {
        private final MutableLong first;
        private final MutableLong second;
        private long firstCount;
        private long secondCount;
        private AtomicLong totalIncrementCount = new AtomicLong(0L);

        private Aggregator(final MutableLong first, final MutableLong second)
        {
            this.first = first;
            this.second = second;
        }

        @Override
        public void accept(final MutableLong mutableLong, final TimeWindow timeWindow)
        {
            if(mutableLong == first)
            {
                firstCount += mutableLong.getValue();
            }
            else if(mutableLong == second)
            {
                secondCount += mutableLong.getValue();
            }
            else
            {
                throw new IllegalArgumentException("Unknown value");
            }

            totalIncrementCount.set(firstCount + secondCount);
            mutableLong.reset();
        }
    }
}