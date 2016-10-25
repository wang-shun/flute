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

import com.aitusoftware.flute.compatibility.LongConsumer;
import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.config.RequiredProperties;
import com.aitusoftware.flute.config.SocketAddressParser;
import com.aitusoftware.flute.factory.RecordingTimeTrackerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class HiccupClient
{
    private final String recordingSeriesIdentifier;
    private final RecordingTimeTrackerFactory timeTrackerFactory;
    private LongConsumer recordingTimeTracker;

    public static void main(String[] args) throws Exception
    {
        if(args.length < 1)
        {
            throw new IllegalArgumentException("Please supply properties file path");
        }

        final Properties properties = new Properties();
        properties.load(new FileReader(args[0]));

        final SocketAddress recordingAddress = SocketAddressParser.fromAddressSpec(RequiredProperties.requiredProperty("flute.test.acceptance.reporting.tcp.address", properties));
        final HistogramConfig histogramConfig = HistogramConfig.fromFluteProperties(properties);

        final RecordingTimeTrackerFactory timeTrackerFactory = new RecordingTimeTrackerFactory().
                publishingTo(recordingAddress).
                withValidation(true).
                withHistogramConfig(histogramConfig).
                publishingEvery(1, TimeUnit.SECONDS);

        startAndRunClient(timeTrackerFactory, "hiccup.1.");
        startAndRunClient(timeTrackerFactory, "hiccup.2.");
    }

    private static void startAndRunClient(
            final RecordingTimeTrackerFactory timeTrackerFactory, final String identifier) throws IOException
    {
        final HiccupClient hiccupClient =
                new HiccupClient(
                        identifier + System.nanoTime(),
                        timeTrackerFactory);
        hiccupClient.init();
        new Thread(hiccupClient::run).start();
    }

    public HiccupClient(
            final String recordingSeriesIdentifier,
            final RecordingTimeTrackerFactory timeTrackerFactory)
    {
        this.recordingSeriesIdentifier = recordingSeriesIdentifier;
        this.timeTrackerFactory = timeTrackerFactory;
    }

    public void init() throws IOException
    {
        recordingTimeTracker = timeTrackerFactory.
                withIdentifer(recordingSeriesIdentifier).
                create().asValueRecorder();

        final Thread allocator = new Thread(() ->
        {
            final List<String> ref = new LinkedList<>();
            while(!Thread.currentThread().isInterrupted())
            {
                final StringBuilder builder = new StringBuilder();
                for(int i = 0; i < 1000; i++)
                {
                    builder.append(Long.toHexString(System.currentTimeMillis())).append(',');
                }
                ref.add(builder.toString());

                if(ref.size() > 5000)
                {
                    ref.remove(ref.size() / 2);
                }

                if(builder.toString().equals(""))
                {
                    System.out.println("Side effect");
                }

                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(250L));
            }
        });
        allocator.setDaemon(true);
        allocator.start();
    }

    public void run()
    {
        while(!Thread.currentThread().isInterrupted())
        {
            final long startNs = System.nanoTime();
            snooze();
            final long durationNs = System.nanoTime() - startNs;
            publishDatum(TimeUnit.NANOSECONDS.toMicros(durationNs), recordingTimeTracker);
        }
    }

    private static void snooze()
    {
        try
        {
            Thread.sleep(1L);
        }
        catch (InterruptedException e)
        {
            // ignore
        }
    }

    private static void publishDatum(final long duration, final LongConsumer timeTracker)
    {
        timeTracker.accept(duration);
    }
}
