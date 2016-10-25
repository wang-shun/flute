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
package com.aitusoftware.flute.config;

import org.HdrHistogram.Histogram;

import java.util.Properties;
import java.util.function.Supplier;

import static com.aitusoftware.flute.config.RequiredProperties.requiredProperty;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

public final class HistogramConfig
{
    private final long maxValue;
    private final int significantDigits;

    public HistogramConfig(final long maxValue, final int significantDigits)
    {
        this.maxValue = maxValue;
        this.significantDigits = significantDigits;
    }

    public long getMaxValue()
    {
        return maxValue;
    }

    public int getSignificantDigits()
    {
        return significantDigits;
    }

    public Supplier<Histogram> asSupplier()
    {
        return () -> new Histogram(maxValue, significantDigits);
    }

    public static HistogramConfig fromFluteProperties(final Properties properties)
    {
        final long maxValue = parseLong(requiredProperty("flute.histogram.maxValue", properties));
        final int significantDigits = parseInt(requiredProperty("flute.histogram.significantDigits", properties));

        return new HistogramConfig(maxValue, significantDigits);
    }
}
