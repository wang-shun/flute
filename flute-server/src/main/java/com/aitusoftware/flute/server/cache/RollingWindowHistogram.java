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
package com.aitusoftware.flute.server.cache;

import org.HdrHistogram.Histogram;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class RollingWindowHistogram
{
    private final Set<String> metricIdentifiers;
    private final long windowDuration;
    private final TimeUnit windowDurationUnit;
    private final Deque<Histogram> componentHistograms = new LinkedList<>();
    private final Histogram aggregate;
    private final String metricKey;
    private final HistogramQueryFunction updatedHistogramQuery;
    private final LongSupplier clock;
    private long expectedRollFrequencyMillis;
    private long windowEnd;

    RollingWindowHistogram(
            final Set<String> metricIdentifiers,
            final long windowDuration,
            final TimeUnit windowDurationUnit,
            final String metricKey,
            final HistogramQueryFunction updatedHistogramQuery,
            final LongSupplier clock,
            final Supplier<Histogram> histogramSupplier)
    {
        this.metricIdentifiers = metricIdentifiers;
        this.windowDuration = windowDuration;
        this.windowDurationUnit = windowDurationUnit;
        this.metricKey = metricKey;
        this.updatedHistogramQuery = updatedHistogramQuery;
        this.clock = clock;
        aggregate = histogramSupplier.get();
    }

    public synchronized Histogram getHistogram()
    {
        final boolean requiresUpdate = requiresUpdate();
        if(requiresUpdate)
        {
            if(windowEnd == 0L)
            {
                windowEnd = clock.getAsLong() - windowDurationUnit.toMillis(windowDuration);
            }
            final List<CompressedHistogram> updates = updatedHistogramQuery.query(metricIdentifiers, metricKey,
                    windowEnd, System.currentTimeMillis());
            for (final CompressedHistogram compressed : updates)
            {
                final Histogram component = compressed.unpack();
                if(component.getTotalCount() != 0L)
                {
                    aggregate.add(component);
                    windowEnd = component.getEndTimeStamp();
                    aggregate.setEndTimeStamp(windowEnd);
                    componentHistograms.add(component);
                    expectedRollFrequencyMillis = Math.min(component.getEndTimeStamp() - component.getStartTimeStamp(),
                            TimeUnit.SECONDS.toMillis(10L));
                }
            }
            final long startOfTimeWindow = windowEnd - windowDurationUnit.toMillis(windowDuration);
            while (!componentHistograms.isEmpty() && componentHistograms.getFirst().getStartTimeStamp() < startOfTimeWindow)
            {
                aggregate.subtract(componentHistograms.removeFirst());
            }
            if(!componentHistograms.isEmpty())
            {
                aggregate.setStartTimeStamp(componentHistograms.getFirst().getStartTimeStamp());
            }
        }
        return aggregate;
    }

    long getWindowEnd()
    {
        return windowEnd;
    }

    private boolean requiresUpdate()
    {
        return windowEnd == 0L || expectedRollFrequencyMillis == 0L || updatedExpectedWithinNextSecond();
    }

    private boolean updatedExpectedWithinNextSecond()
    {
        return clock.getAsLong() > (windowEnd + expectedRollFrequencyMillis) - 1000L;
    }
}