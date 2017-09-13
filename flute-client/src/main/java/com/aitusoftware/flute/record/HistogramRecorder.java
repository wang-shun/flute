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
package com.aitusoftware.flute.record;

import com.aitusoftware.flute.compatibility.LongConsumer;
import com.aitusoftware.flute.compatibility.Supplier;
import org.HdrHistogram.Histogram;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Records values in a Histogram
 */
public final class HistogramRecorder implements LongConsumer
{
    private final Supplier<Histogram> histogramProvider;
    private final Runnable recordCompleteNotifier;
    private final long highestRecordableValue;

    public HistogramRecorder(
            final Supplier<Histogram> histogramProvider,
            final Runnable recordCompleteNotifier,
            final long highestRecordableValue)
    {
        this.histogramProvider = histogramProvider;
        this.recordCompleteNotifier = recordCompleteNotifier;
        this.highestRecordableValue = highestRecordableValue;
    }

    private void recordValue(final long value)
    {
        try
        {
            histogramProvider.get().recordValue(max(0, min(value, highestRecordableValue)));
        }
        finally
        {
            recordCompleteNotifier.run();
        }
    }

    /**
     * Record a single value in the associated histogram
     * @param value the value
     */
    @Override
    public void accept(final long value)
    {
        recordValue(value);
    }
}