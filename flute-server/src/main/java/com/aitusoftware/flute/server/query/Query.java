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
package com.aitusoftware.flute.server.query;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class Query
{
    private static final long NO_EXPLICIT_END = Long.MIN_VALUE;
    private final String metricKey;
    private final long endMillis;
    private final long duration;
    private final TimeUnit durationUnit;

    private Query(
            final String metricKey,
            final long duration,
            final TimeUnit durationUnit,
            final long endMillis)
    {
        this.metricKey = metricKey;
        this.endMillis = endMillis;
        this.duration = duration;
        this.durationUnit = durationUnit;
    }

    public static Query withEndTime(final String metricKey,
                                    final long duration,
                                    final TimeUnit durationUnit,
                                    final long endMillis)
    {
        return new Query(metricKey, duration, durationUnit, endMillis);
    }

    public static Query latest(final String metricKey,
                               final long duration,
                               final TimeUnit durationUnit)
    {
        return new Query(metricKey, duration, durationUnit, NO_EXPLICIT_END);
    }

    public String getMetricKey()
    {
        return metricKey;
    }

    public long getStartMillis()
    {
        return getEndMillis() - durationUnit.toMillis(duration);
    }

    public long getEndMillis()
    {
        return endMillis == NO_EXPLICIT_END ? System.currentTimeMillis() : endMillis;
    }

    public TimeUnit getDurationUnit()
    {
        return durationUnit;
    }

    public long getDuration()
    {
        return duration;
    }

    public boolean hasExplicitEndTime()
    {
        return endMillis != NO_EXPLICIT_END;
    }

    @Override
    public String toString()
    {
        return "Query{" +
                "metricKey='" + metricKey + '\'' +
                ", startMillis=" + Instant.ofEpochMilli(getStartMillis()) +
                ", endMillis=" + Instant.ofEpochMilli(endMillis) +
                ", duration=" + duration +
                ", durationUnit=" + durationUnit +
                '}';
    }
}
