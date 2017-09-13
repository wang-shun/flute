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
package com.aitusoftware.flute.client.benchmark;

import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.factory.RecordingTimeTrackerFactory;
import com.aitusoftware.flute.record.TimeTracker;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class RecordingBenchmark
{

    private final BusinessObject obj = new BusinessObject();
    private TimeTracker timeTracker;
    private long input = 0L;
    private InetSocketAddress socketAddress;
    private ServerSocket serverSocket;

    @Setup
    public void setup() throws Exception
    {
        socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 41200);
        serverSocket = new ServerSocket(41200);
        final Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    try
                    {
                        serverSocket.accept();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        timeTracker = new RecordingTimeTrackerFactory().
                publishingTo(socketAddress).publishingEvery(1L, TimeUnit.SECONDS).
                withHistogramConfig(new HistogramConfig(100000, 3)).
                withMultiThreadedAccess(false).withIdentifer("bench").
                create();
    }

    @Benchmark
    public long baseline()
    {
        return obj.invoke(input++);
    }

    @Benchmark
    public long timed()
    {
        timeTracker.begin(System.nanoTime());
        final long result = obj.invoke(input++);
        timeTracker.end(System.nanoTime());

        return result;
    }

    private static final class BusinessObject
    {
        public long invoke(final long input)
        {
            return (long) Math.pow((input / 37L) * 12345678L, 7) / 17L;
        }
    }
}
