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
package com.aitusoftware.flute.acceptance.framework;

import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.factory.RecordingTimeTrackerFactory;
import com.aitusoftware.flute.record.TimeTracker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestClient
{
    private static final AtomicInteger INSTANCE_ID = new AtomicInteger(0);
    private static final InetSocketAddress PUBLISH_TARGET = new InetSocketAddress("localhost", 51000);
    private final String metricName;
    private final TimeTracker timeTracker;
    private final StubClock clock = new StubClock(System.currentTimeMillis());
    private long start;

    public TestClient(final String alias) throws IOException
    {
        SystemReadiness.waitForServer(PUBLISH_TARGET);

        this.metricName = alias + "." + INSTANCE_ID.getAndIncrement() + "." + System.nanoTime();
        this.timeTracker =
                new RecordingTimeTrackerFactory().
                        withClock(clock::getCurrentMillis).
                        publishingEvery(1L, TimeUnit.SECONDS).
                        publishingTo(PUBLISH_TARGET).
                        withHistogramConfig(new HistogramConfig(100_000, 4)).
                        withIdentifer(metricName).
                        create();
    }

    public void recordSample(final long sample)
    {
        final long end = start + sample;
        timeTracker.begin(start);
        timeTracker.end(end);
        start = end;
    }

    public void publish()
    {
        clock.move(TimeUnit.SECONDS.toMillis(1L));
    }

    public String getMetricName()
    {
        return metricName;
    }

    public long getCurrentTime()
    {
        return clock.getCurrentMillis();
    }
}