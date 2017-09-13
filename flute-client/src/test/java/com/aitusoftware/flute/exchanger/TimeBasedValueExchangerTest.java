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
import com.aitusoftware.flute.compatibility.LongSupplier;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public final class TimeBasedValueExchangerTest
{
    private static final String FIRST = "FIRST";
    private static final String SECOND = "SECOND";
    private static final long INITIAL_VALUE = -1L;
    private static final long BASE_TIME = 1234567890000L;
    private static final long PUBLICATION_INTERVAL = 1L;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final long INTERVAL_MILLIS = TIME_UNIT.toMillis(PUBLICATION_INTERVAL);

    private long currentTestTimestamp = BASE_TIME;
    private long lastTimeWindowStartTime = INITIAL_VALUE;
    private long lastTimeWindowEndTime = INITIAL_VALUE;
    private String lastValue;

    private final TimeBasedValueExchanger<String> exchanger =
            new TimeBasedValueExchanger<String>(FIRST, SECOND,
                    new BiConsumer<String, TimeWindow>()
                    {
                        @Override
                        public void accept(final String s, final TimeWindow timeWindow)
                        {
                            valuePublished(s, timeWindow);
                        }
                    }, new LongSupplier()
            {
                @Override
                public long getAsLong()
                {
                    return getCurrentTestTimestamp();
                }
            },
                    PUBLICATION_INTERVAL, TIME_UNIT);
    private final CountDownLatch latch = new CountDownLatch(1);


    @Before
    public void before() throws Exception
    {
        final Thread writer = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                exchanger.acquire();
                exchanger.release();
                latch.countDown();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));
            }
        });
        writer.setDaemon(true);
        writer.start();

        latch.await();
    }

    @Test
    public void shouldPublishCurrentValueAfterPublicationIntervalHasExpired() throws Exception
    {
        exchanger.poll();

        currentTestTimestamp = currentTestTimestamp + INTERVAL_MILLIS - 1;

        exchanger.poll();

        assertThat(lastValue, nullValue());

        currentTestTimestamp += 1;

        exchanger.poll();

        assertThat(lastValue, is(FIRST));
        assertThat(lastTimeWindowStartTime, is(BASE_TIME));
        assertThat(lastTimeWindowEndTime, is(BASE_TIME + INTERVAL_MILLIS));
    }

    @Test
    public void shouldNotPublishSubsequentValueUntilPublicationIntervalHasExpired() throws Exception
    {
        currentTestTimestamp += INTERVAL_MILLIS - 1;

        exchanger.poll();

        assertThat(lastValue, nullValue());

        currentTestTimestamp += 1;

        exchanger.poll();

        assertThat(lastValue, is(FIRST));
        assertThat(lastTimeWindowStartTime, is(BASE_TIME));
        assertThat(lastTimeWindowEndTime, is(BASE_TIME + INTERVAL_MILLIS));

        currentTestTimestamp += INTERVAL_MILLIS - 1;
        assertThat(lastValue, is(FIRST));
        assertThat(lastTimeWindowStartTime, is(BASE_TIME));
        assertThat(lastTimeWindowEndTime, is(BASE_TIME + INTERVAL_MILLIS));
    }

    @Test
    public void shouldPublishAlternatingValues() throws Exception
    {
        exchanger.poll();

        currentTestTimestamp += INTERVAL_MILLIS;
        exchanger.poll();

        assertThat(lastValue, is(FIRST));
        assertThat(lastTimeWindowStartTime, is(BASE_TIME));
        assertThat(lastTimeWindowEndTime, is(BASE_TIME + INTERVAL_MILLIS));

        currentTestTimestamp += INTERVAL_MILLIS;
        exchanger.poll();

        assertThat(lastValue, is(SECOND));
        assertThat(lastTimeWindowStartTime, is(BASE_TIME + INTERVAL_MILLIS));
        assertThat(lastTimeWindowEndTime, is(BASE_TIME + (2 * INTERVAL_MILLIS)));

        currentTestTimestamp += INTERVAL_MILLIS;
        exchanger.poll();

        assertThat(lastValue, is(FIRST));
        assertThat(lastTimeWindowStartTime, is(BASE_TIME + (2 * INTERVAL_MILLIS)));
        assertThat(lastTimeWindowEndTime, is(BASE_TIME + (3 * INTERVAL_MILLIS)));

        currentTestTimestamp += INTERVAL_MILLIS;
        exchanger.poll();

        assertThat(lastValue, is(SECOND));
        assertThat(lastTimeWindowStartTime, is(BASE_TIME + (3 * INTERVAL_MILLIS)));
        assertThat(lastTimeWindowEndTime, is(BASE_TIME + (4 * INTERVAL_MILLIS)));
    }

    private long getCurrentTestTimestamp()
    {
        return currentTestTimestamp;
    }

    private void valuePublished(final String value, final TimeWindow timeWindow)
    {
        this.lastValue = value;
        this.lastTimeWindowStartTime = timeWindow.getWindowStart();
        this.lastTimeWindowEndTime = timeWindow.getWindowEnd();
    }
}