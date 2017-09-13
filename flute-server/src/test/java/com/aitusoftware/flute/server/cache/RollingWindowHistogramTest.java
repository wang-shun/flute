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
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class RollingWindowHistogramTest
{
    private static final long START_TIME = 1234567890000L;
    private static final TimeUnit DURATION_UNIT = TimeUnit.MINUTES;
    private static final long WINDOW_DURATION = 10L;
    private final List<Histogram> queryResponse = new LinkedList<>();
    private final Set<String> metricIdentifiers = new HashSet<>(Arrays.asList("one", "two"));
    private long currentTime = START_TIME;
    private final RollingWindowHistogram rollingWindowHistogram =
            new RollingWindowHistogram(metricIdentifiers, WINDOW_DURATION, DURATION_UNIT,
                    ".*o.*", this::queryFunction,
                    this::clock, () -> new Histogram(1000, 2));
    private long capturedQueryRequestTimestamp;
    private int queryCount;

    @Test
    public void shouldBuildInitialHistogram() throws Exception
    {
        final long earliestDataPointStartTime = 1234567800000L;
        final long latestDataPointStartTime = earliestDataPointStartTime + 50000L;
        setQueryResults(
                createHistogram(5, earliestDataPointStartTime, earliestDataPointStartTime + 10000L),
                createHistogram(5, earliestDataPointStartTime + 10000L, latestDataPointStartTime)
        );

        final Histogram aggregate = rollingWindowHistogram.getHistogram();

        assertThat(capturedQueryRequestTimestamp, is(START_TIME - DURATION_UNIT.toMillis(WINDOW_DURATION)));
        assertThat(aggregate.getTotalCount(), is(WINDOW_DURATION));
        assertThat(aggregate.getStartTimeStamp(), is(earliestDataPointStartTime));
        assertThat(aggregate.getEndTimeStamp(), is(latestDataPointStartTime));
    }

    @Test
    public void shouldAppendNewComponents() throws Exception
    {
        final long dataStart = 1234567800000L;
        setQueryResults(createHistogram(5, dataStart, dataStart + 10000L));

        Histogram aggregate = rollingWindowHistogram.getHistogram();

        assertThat(capturedQueryRequestTimestamp, is(START_TIME - DURATION_UNIT.toMillis(WINDOW_DURATION)));
        assertThat(aggregate.getTotalCount(), is(5L));
        assertThat(aggregate.getStartTimeStamp(), is(dataStart));
        assertThat(aggregate.getEndTimeStamp(), is(dataStart + 10000L));

        setQueryResults(createHistogram(7, dataStart + 20000L, dataStart + 30000L));

        aggregate = rollingWindowHistogram.getHistogram();

        assertThat(capturedQueryRequestTimestamp, is(dataStart + 10000L));
        assertThat(aggregate.getTotalCount(), is(12L));
        assertThat(aggregate.getStartTimeStamp(), is(dataStart));
        assertThat(aggregate.getEndTimeStamp(), is(dataStart + 30000L));
    }

    @Test
    public void shouldTrimComponentsThatFallOutsideOfTheTimeWindow() throws Exception
    {
        final long dataStart = 1234567800000L;
        setQueryResults(
                createHistogram(5, dataStart, dataStart + 10000L),
                createHistogram(5, dataStart + 10000L, dataStart + 20000L)
        );

        Histogram aggregate = rollingWindowHistogram.getHistogram();

        assertThat(aggregate.getTotalCount(), is(WINDOW_DURATION));

        final long endTimeAfterEndOfInitialTimeWindow = dataStart + DURATION_UNIT.toMillis(WINDOW_DURATION) + 1;
        setQueryResults(
                createHistogram(7, dataStart + 50000L, endTimeAfterEndOfInitialTimeWindow)
        );

        aggregate = rollingWindowHistogram.getHistogram();

        assertThat(aggregate.getTotalCount(), is(12L));
        assertThat(aggregate.getStartTimeStamp(), is(dataStart + 10000L));
        assertThat(aggregate.getEndTimeStamp(), is(endTimeAfterEndOfInitialTimeWindow));
    }

    @Test
    public void shouldNotPollForNewDataBeforeExpectedUpdateIntervalHasExpired() throws Exception
    {
        final long dataStart = 1234567800000L;
        setQueryResults(createHistogram(5, dataStart, dataStart + 10000L));

        Histogram aggregate = rollingWindowHistogram.getHistogram();

        assertThat(capturedQueryRequestTimestamp, is(START_TIME - DURATION_UNIT.toMillis(WINDOW_DURATION)));
        assertThat(queryCount, is(1));
        assertThat(aggregate.getTotalCount(), is(5L));

        setQueryResults(createHistogram(7, dataStart + 20000L, dataStart + 30000L));

        currentTime = dataStart + 18999L;

        aggregate = rollingWindowHistogram.getHistogram();

        assertThat(capturedQueryRequestTimestamp, is(START_TIME - DURATION_UNIT.toMillis(WINDOW_DURATION)));
        assertThat(queryCount, is(1));
        assertThat(aggregate.getTotalCount(), is(5L));

        currentTime = dataStart + 19001L;

        aggregate = rollingWindowHistogram.getHistogram();

        assertThat(capturedQueryRequestTimestamp, is(dataStart + 10000L));
        assertThat(queryCount, is(2));
        assertThat(aggregate.getTotalCount(), is(12L));
        assertThat(aggregate.getStartTimeStamp(), is(dataStart));
        assertThat(aggregate.getEndTimeStamp(), is(dataStart + 30000L));
    }

    private void setQueryResults(final Histogram... elements)
    {
        queryResponse.clear();
        queryResponse.addAll(Arrays.asList(elements));
    }

    private Histogram createHistogram(final int numberOfDataPoints, final long startTime, final long endTime)
    {
        final Histogram histogram = new Histogram(1000, 2);
        for(int i = 0; i < numberOfDataPoints; i++)
        {
            histogram.recordValue(i);
        }
        histogram.setStartTimeStamp(startTime);
        histogram.setEndTimeStamp(endTime);

        return histogram;
    }

    private long clock()
    {
        return currentTime;
    }

    private List<Histogram> queryFunction(final Set<String> metricIdentifiers, final String metricKey,
                                          final long startTimestamp, final long endTimestamp)
    {
        capturedQueryRequestTimestamp = startTimestamp;
        queryCount++;
        return queryResponse;
    }
}