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
package com.aitusoftware.flute.server.reporting.dao;

import com.aitusoftware.flute.server.config.Metric;
import com.aitusoftware.flute.server.reporting.domain.Threshold;
import com.aitusoftware.flute.server.reporting.domain.TimeWindow;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

final class ReportDataMarshalling
{
    static List<Threshold> parseThresholds(final String encoded)
    {
        final String[] tokens = encoded.split("\\|");
        return Arrays.stream(tokens).filter(s -> s.contains("@")).map(s -> {
            final String[] spec = s.split("@");
            return new Threshold(Long.parseLong(spec[0]), Metric.valueOf(spec[1]));
        }).collect(Collectors.toList());
    }

    static String encodeThresholds(final List<Threshold> thresholds)
    {
        final StringBuilder builder = new StringBuilder();
        thresholds.forEach(t -> builder.append(t.getValue()).append('@').append(t.getPercentile()).append('|'));
        return builder.toString();
    }

    static List<TimeWindow> parseTimeWindows(final String encoded)
    {
        final String[] tokens = encoded.split("\\|");
        return Arrays.stream(tokens).filter(s -> s.contains("/")).map(s -> {
            final String[] spec = s.split("/");
            return new TimeWindow(TimeUnit.valueOf(spec[1]), Long.parseLong(spec[0]));
        }).collect(Collectors.toList());
    }

    static String encodeTimeWindows(final List<TimeWindow> timeWindows)
    {
        final StringBuilder builder = new StringBuilder();
        timeWindows.forEach(t -> builder.append(t.getDuration()).append('/').append(t.getUnit().name()).append('|'));
        return builder.toString();
    }
}
