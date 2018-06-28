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

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class LiveHistogramSource implements HistogramSource
{
    private final HistogramQueryFunction queryFunction;
    private final Supplier<Histogram> histogramSupplier;

    LiveHistogramSource(final HistogramQueryFunction queryFunction,
                        final Supplier<Histogram> histogramSupplier)
    {
        this.queryFunction = queryFunction;
        this.histogramSupplier = histogramSupplier;
    }

    @Override
    public Histogram getCurrentHistogram(final Set<String> metricIdentifiers,
                                         final long windowDuration,
                                         final TimeUnit durationUnit,
                                         final String metricKey)
    {
        return new RollingWindowHistogram(metricIdentifiers, windowDuration, durationUnit, metricKey,
                queryFunction, System::currentTimeMillis, histogramSupplier).getHistogram();
    }
}
