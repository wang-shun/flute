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
package com.aitusoftware.flute.server.cache;

import org.HdrHistogram.Histogram;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class HistogramCache
{
    private final int maxEntries;
    private final ConcurrentMap<MapKey, RollingWindowHistogram> cache;
    private final HistogramQueryFunction queryFunction;
    private final LongSupplier clock;
    private final Supplier<Histogram> histogramSupplier;


    public HistogramCache(
            final int maxEntries,
            final HistogramQueryFunction queryFunction,
            final LongSupplier clock,
            final Supplier<Histogram> histogramSupplier)
    {
        this.maxEntries = maxEntries;
        cache = new ConcurrentHashMap<>(maxEntries);
        this.queryFunction = queryFunction;
        this.clock = clock;
        this.histogramSupplier = histogramSupplier;
    }

    public Histogram getCurrentHistogram(final Set<String> metricIdentifiers, final long windowDuration, final TimeUnit durationUnit, final String metricKey)
    {
        final Histogram histogram = cache.computeIfAbsent(new MapKey(metricIdentifiers, windowDuration, durationUnit, metricKey), this::create).getHistogram();
        trimCache();
        return histogram;
    }

    private void trimCache()
    {
        if(cache.size() > maxEntries)
        {
            MapKey keyForLruEntry = null;
            long oldestAccessTime = Long.MAX_VALUE;
            for (Map.Entry<MapKey, RollingWindowHistogram> entry : cache.entrySet())
            {
                if(entry.getValue().getWindowEnd() < oldestAccessTime)
                {
                    oldestAccessTime = entry.getValue().getWindowEnd();
                    keyForLruEntry = entry.getKey();
                }
            }

            cache.remove(keyForLruEntry);
        }
    }

    private RollingWindowHistogram create(final MapKey mapKey)
    {
        return new RollingWindowHistogram(mapKey.metricIdentifiers, mapKey.windowDuration,
                mapKey.durationUnit, mapKey.metricKey, queryFunction, clock, histogramSupplier);
    }


    private static final class MapKey
    {
        private final Set<String> metricIdentifiers;
        private final long windowDuration;
        private final TimeUnit durationUnit;
        private final transient String metricKey;

        private MapKey(final Set<String> metricIdentifiers, final long windowDuration,
                       final TimeUnit durationUnit, final String metricKey)
        {
            this.metricIdentifiers = metricIdentifiers;
            this.windowDuration = windowDuration;
            this.durationUnit = durationUnit;
            this.metricKey = metricKey;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final MapKey mapKey = (MapKey) o;

            if (windowDuration != mapKey.windowDuration)
            {
                return false;
            }
            if (metricIdentifiers != null ? !metricIdentifiers.equals(mapKey.metricIdentifiers) : mapKey.metricIdentifiers != null)
            {
                return false;
            }
            return durationUnit == mapKey.durationUnit;

        }

        @Override
        public int hashCode()
        {
            int result = metricIdentifiers != null ? metricIdentifiers.hashCode() : 0;
            result = 31 * result + (int) (windowDuration ^ (windowDuration >>> 32));
            result = 31 * result + (durationUnit != null ? durationUnit.hashCode() : 0);
            return result;
        }
    }
}