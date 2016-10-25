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
package com.aitusoftware.flute.integration.client;

import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.factory.RecordingTimeTrackerFactory;
import com.aitusoftware.flute.record.TimeTracker;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class TimingClient
{
    static final String RECORDING_SERIES_IDENTIFIER = "foo.bar.record";

    private final String recordingSeriesIdentifier;
    private final HistogramConfig histogramConfig;
    private final SocketAddress socketAddress;
    private TimeTracker recordingTimeTracker;

    public TimingClient(
            final String recordingSeriesIdentifier,
            final HistogramConfig histogramConfig,
            final SocketAddress socketAddress)
    {
        this.socketAddress = socketAddress;
        this.recordingSeriesIdentifier = recordingSeriesIdentifier;
        this.histogramConfig = histogramConfig;
    }

    public void init() throws IOException
    {
        recordingTimeTracker = new RecordingTimeTrackerFactory().
                publishingTo(socketAddress).
                withValidation(true).
                withHistogramConfig(histogramConfig).
                publishingEvery(1, TimeUnit.SECONDS).
                withIdentifer(recordingSeriesIdentifier).
                create();
    }

    public void generateTestData(final long duration, final TimeUnit unit)
    {
        final Random random = new Random(System.currentTimeMillis());

        final long endAt = System.currentTimeMillis() + unit.toMillis(duration);
        while(System.currentTimeMillis() < endAt && !Thread.currentThread().isInterrupted())
        {
            if(recordingTimeTracker != null)
            {
                publishDatum(sampleDuration(random), recordingTimeTracker);
            }
            LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(sampleDuration(random) * 50));
        }
    }

    private static int sampleDuration(final Random random)
    {
        return random.nextInt(100) * random.nextInt(10) + 50;
    }

    private static void publishDatum(final long duration, final TimeTracker timeTracker)
    {
        final long startTime = System.nanoTime();
        timeTracker.begin(startTime);
        timeTracker.end(startTime + duration);
    }
}
