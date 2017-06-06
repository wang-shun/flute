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
package com.aitusoftware.flute.send;

import com.aitusoftware.flute.compatibility.LongSupplier;

import java.util.concurrent.TimeUnit;

final class ConnectionAttemptThrottler
{
    private final LongSupplier millisecondTimestampSupplier;
    private final long initialBackOffMillis;
    private final long maxBackOffMillis;

    private long lastConnectionAttempt = 0L;
    private long currentBackOffPeriod;

    ConnectionAttemptThrottler(final LongSupplier millisecondTimestampSupplier,
                               final long initialBackOffPeriod, final long maxBackOffPeriod,
                               final TimeUnit timeUnit)
    {
        this.millisecondTimestampSupplier = millisecondTimestampSupplier;
        this.initialBackOffMillis = timeUnit.toMillis(initialBackOffPeriod);
        this.maxBackOffMillis = timeUnit.toMillis(maxBackOffPeriod);
        currentBackOffPeriod = initialBackOffMillis / 2;
    }

    boolean shouldAttemptConnection()
    {
        final boolean isFirstAttemptSinceSuccessfulConnection = lastConnectionAttempt == 0L;

        final long currentTimestamp = millisecondTimestampSupplier.getAsLong();
        final boolean currentBackOffPeriodHasExpired = currentTimestamp > lastConnectionAttempt + currentBackOffPeriod;

        lastConnectionAttempt = currentTimestamp;
        currentBackOffPeriod = Math.min(currentBackOffPeriod * 2, maxBackOffMillis);

        return isFirstAttemptSinceSuccessfulConnection || currentBackOffPeriodHasExpired;
    }

    void connectionSuccessful()
    {
        lastConnectionAttempt = 0L;
        currentBackOffPeriod = initialBackOffMillis;
    }
}