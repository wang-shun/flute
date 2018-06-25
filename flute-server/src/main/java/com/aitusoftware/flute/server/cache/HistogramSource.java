package com.aitusoftware.flute.server.cache;

import org.HdrHistogram.Histogram;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface HistogramSource {
    Histogram getCurrentHistogram(Set<String> metricIdentifiers, long windowDuration, TimeUnit durationUnit, String metricKey);
}
