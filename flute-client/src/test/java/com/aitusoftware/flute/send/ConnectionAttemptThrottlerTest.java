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
package com.aitusoftware.flute.send;

import com.aitusoftware.flute.compatibility.LongSupplier;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConnectionAttemptThrottlerTest
{
    private static final long BASE_TIME = 1234567890123L;
    private static final long INITIAL_BACKOFF = 100L;
    private static final long MAX_BACKOFF = 5000L;
    private final StubClock clock = new StubClock(BASE_TIME);
    private final ConnectionAttemptThrottler throttler =
            new ConnectionAttemptThrottler(clock, INITIAL_BACKOFF, MAX_BACKOFF, TimeUnit.MILLISECONDS);

    @Test
    public void shouldNotThrottleInitialConnection() throws Exception
    {
        assertThat(throttler.shouldAttemptConnection(), is(true));
    }

    @Test
    public void shouldThrottleImmediateRetry() throws Exception
    {
        throttler.shouldAttemptConnection();
        assertThat(throttler.shouldAttemptConnection(), is(false));
    }

    @Test
    public void shouldNotThrottleRetryAfterInitialBackOffPeriod() throws Exception
    {
        throttler.shouldAttemptConnection();

        clock.advance(INITIAL_BACKOFF + 1);

        assertThat(throttler.shouldAttemptConnection(), is(true));
    }

    @Test
    public void shouldNotThrottleRetryAfterSuccessfulConnection() throws Exception
    {
        throttler.shouldAttemptConnection();
        throttler.connectionSuccessful();

        assertThat(throttler.shouldAttemptConnection(), is(true));
    }

    @Test
    public void shouldLimitBackOffPeriodToMaximum() throws Exception
    {
        for(int i = 0; i < 500; i++)
        {
            throttler.shouldAttemptConnection();
        }

        assertThat(throttler.shouldAttemptConnection(), is(false));

        clock.advance(MAX_BACKOFF + 1);

        assertThat(throttler.shouldAttemptConnection(), is(true));
    }

    @Test
    public void shouldResetAfterSuccessfulConnectionFollowingManyFailedConnections() throws Exception
    {
        for(int i = 0; i < 500; i++)
        {
            throttler.shouldAttemptConnection();
        }

        throttler.connectionSuccessful();

        assertThat(throttler.shouldAttemptConnection(), is(true));
    }

    private static final class StubClock implements LongSupplier
    {
        private long timestamp;

        StubClock(final long timestamp)
        {
            this.timestamp = timestamp;
        }

        @Override
        public long getAsLong()
        {
            return timestamp;
        }

        void setTimestamp(final long timestamp)
        {
            this.timestamp = timestamp;
        }

        void advance(final long delta)
        {
            timestamp += delta;
        }
    }
}