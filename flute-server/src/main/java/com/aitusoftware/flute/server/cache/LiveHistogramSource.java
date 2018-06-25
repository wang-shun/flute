package com.aitusoftware.flute.server.cache;

import org.HdrHistogram.Histogram;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class LiveHistogramSource implements HistogramSource
{
    private final HistogramQueryFunction queryFunction;
    private final Supplier<Histogram> histogramSupplier;

    public LiveHistogramSource(final HistogramQueryFunction queryFunction,
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
