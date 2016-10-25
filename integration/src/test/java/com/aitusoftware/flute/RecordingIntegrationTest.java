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
package com.aitusoftware.flute;

import com.aitusoftware.flute.config.FluteThreadFactory;
import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.factory.HistogramReceiverFactory;
import com.aitusoftware.flute.factory.RecordingTimeTrackerFactory;
import com.aitusoftware.flute.receive.ReceiverProcess;
import com.aitusoftware.flute.record.TimeTracker;
import com.aitusoftware.flute.send.events.AggregatorEvents;
import org.HdrHistogram.Histogram;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RecordingIntegrationTest
{
    private static final long MAX_VALUE = 1000L;
    private static final long PUBLICATION_INTERVAL = 100L;
    private static final TimeUnit INTERVAL_UNITS = TimeUnit.MILLISECONDS;
    private final ScheduledExecutorService scheduler =
            newScheduledThreadPool(10, DaemonThreadFactory.DAEMON_THREAD_FACTORY);
    private InetSocketAddress socketAddress;
    private final List<Exception> sendExceptions = new CopyOnWriteArrayList<>();
    private final List<Exception> receiveExceptions = new CopyOnWriteArrayList<>();
    private final List<Histogram> receivedList = new CopyOnWriteArrayList<>();
    private ServerSocketChannel serverSocketChannel;
    private String identifier;

    @Before
    public void before() throws Exception
    {
        socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 41200);
        checkNoReceiverIsCurrentlyListening();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverSocketChannel.bind(socketAddress);
        identifier = "foo.bar_" + UUID.randomUUID().toString();
    }

    @After
    public void after() throws Exception
    {
        scheduler.shutdownNow();
        assertTrue(scheduler.awaitTermination(1L, TimeUnit.SECONDS));
        serverSocketChannel.close();
    }

    @Test
    public void shouldPublishRecordedDataOnSchedule() throws Exception
    {
        final TimeTracker timeTracker = createTimeTracker(identifier);
        final ReceiverProcess receiverProcess = createReceiverProcess(identifier);
        receiverProcess.start();

        long startTime = 17L;
        long bailAt = System.currentTimeMillis() + 15000L;
        for (int i = 0; i < 2000000; i++)
        {
            timeTracker.begin(startTime);
            timeTracker.end(startTime + i);

            startTime += i * 2;

            if (receivedList.size() >= 2 || System.currentTimeMillis() > bailAt)
            {
                break;
            }
            if (i % 4 == 0)
            {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(30L));
            }
        }

        receiverProcess.stop(1L, TimeUnit.SECONDS);

        assertTrue(errorMessage(receiveExceptions), receivedList.size() >= 2);
        assertTrue(getTotalValueCount(receivedList) > 1);
        assertThat(sendExceptions.isEmpty(), is(true));
    }

    @Test
    public void shouldPublishDataWhenReceiverIsUnavailableAtBeginningOfRecording() throws Exception
    {
        final TimeTracker timeTracker = createTimeTracker(identifier);

        final ReceiverProcess receiverProcess = createReceiverProcess(identifier);

        final long fourPublicationIntervalsInMillis = INTERVAL_UNITS.toMillis(PUBLICATION_INTERVAL * 4);
        final long initialSamplePeriodEnd = System.currentTimeMillis() + fourPublicationIntervalsInMillis;
        long totalValueCount = 0L;
        long lastNanos = System.nanoTime();
        while (System.currentTimeMillis() < initialSamplePeriodEnd)
        {
            timeTracker.begin(lastNanos);
            lastNanos = System.nanoTime();
            timeTracker.end(lastNanos);
            totalValueCount++;
            LockSupport.parkNanos(125L);
        }

        receiverProcess.start();

        final long subsequentSamplePeriodEnd = System.currentTimeMillis() + fourPublicationIntervalsInMillis;
        while (System.currentTimeMillis() < subsequentSamplePeriodEnd)
        {
            timeTracker.begin(lastNanos);
            lastNanos = System.nanoTime();
            timeTracker.end(lastNanos);
            totalValueCount++;
            LockSupport.parkNanos(125L);
        }

        final long bailAt = getWaitTimeout();
        while ((System.currentTimeMillis() < bailAt))
        {
            if (getTotalValueCount(receivedList) == totalValueCount)
            {
                break;
            }
        }

        receiverProcess.stop(1L, TimeUnit.SECONDS);

        assertFalse(errorMessage(receiveExceptions), receivedList.isEmpty());
        assertThat(getTotalValueCount(receivedList), is(totalValueCount));
        assertThat(sendExceptions.isEmpty(), is(true));
    }

    @Test
    public void shouldPublishDataWhenReceiverIsUnavailableDuringRecording() throws Exception
    {
        final TimeTracker timeTracker = createTimeTracker(identifier);

        final ReceiverProcess firstReceiver = createReceiverProcess(identifier);
        firstReceiver.start();

        final long fourPublicationIntervalsInMillis = INTERVAL_UNITS.toMillis(PUBLICATION_INTERVAL * 4);
        final long initialSamplePeriodEnd = System.currentTimeMillis() + fourPublicationIntervalsInMillis;
        long lastNanos = System.nanoTime();
        while (System.currentTimeMillis() < initialSamplePeriodEnd && getTotalValueCount(receivedList) == 0)
        {
            timeTracker.begin(lastNanos);
            lastNanos = System.nanoTime();
            timeTracker.end(lastNanos);
            LockSupport.parkNanos(125L);
        }

        long bailAt = getWaitTimeout();
        while ((System.currentTimeMillis() < bailAt))
        {
            if (getTotalValueCount(receivedList) > 0L)
            {
                break;
            }
        }

        assertTrue(getTotalValueCount(receivedList) > 0L);

        firstReceiver.stop(1L, TimeUnit.SECONDS);
        final long receivedByFirstReceiverCount = getTotalValueCount(receivedList);

        serverSocketChannel.close();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverSocketChannel.bind(socketAddress);

        final ReceiverProcess secondReceiver = createReceiverProcess(identifier);

        final long subsequentSamplePeriodEnd = System.currentTimeMillis() + fourPublicationIntervalsInMillis;
        while (System.currentTimeMillis() < subsequentSamplePeriodEnd)
        {
            timeTracker.begin(lastNanos);
            lastNanos = System.nanoTime();
            timeTracker.end(lastNanos);
            LockSupport.parkNanos(125L);
        }
        secondReceiver.start();

        bailAt = getWaitTimeout();
        while ((System.currentTimeMillis() < bailAt))
        {
            if (getTotalValueCount(receivedList) > receivedByFirstReceiverCount)
            {
                break;
            }
        }

        secondReceiver.stop(1L, TimeUnit.SECONDS);

        assertFalse(errorMessage(receiveExceptions), receivedList.isEmpty());
        assertTrue(errorMessage(sendExceptions), getTotalValueCount(receivedList) > receivedByFirstReceiverCount);
        assertThat(sendExceptions.isEmpty(), is(true));
    }

    private static final class ExceptionTrackingAggregatorEvents implements AggregatorEvents
    {
        private final List<Exception> sendExceptions;

        private ExceptionTrackingAggregatorEvents(final List<Exception> sendExceptions)
        {
            this.sendExceptions = sendExceptions;
        }

        @Override
        public void failedToRegisterSenderWithSocket(final IOException e)
        {
            sendExceptions.add(e);
        }

        @Override
        public void failedToSendDataForSender(final String identifier, final IOException e)
        {
            sendExceptions.add(e);
        }

        @Override
        public void selectFailed(final IOException e)
        {
            sendExceptions.add(e);
        }

        @Override
        public void exceptionInSendLoop(final RuntimeException e)
        {
            sendExceptions.add(e);
        }

        @Override
        public void failedToConnectToPersistor(final IOException e)
        {
            sendExceptions.add(e);
        }

        @Override
        public void sendQueueBufferOverflow(final String identifier, final ByteBuffer buffer)
        {
            // no-op
        }
    }

    private TimeTracker createTimeTracker(final String identifer) throws IOException
    {
        return new RecordingTimeTrackerFactory().
                publishingTo(socketAddress).
                withSenderEvents(new ExceptionTrackingAggregatorEvents(sendExceptions)).
                withValidation(true).
                withIdentifer(identifer).
                publishingEvery(PUBLICATION_INTERVAL, INTERVAL_UNITS).
                withHistogramConfig(new HistogramConfig(MAX_VALUE, 2)).
                create();
    }

    private ReceiverProcess createReceiverProcess(final String idFilter) throws IOException
    {
        final Consumer<Exception> ec = e ->
        {
            e.printStackTrace();
            if (ClosedByInterruptException.class.equals(e.getClass()) ||
                    ClosedSelectorException.class.equals(e.getClass()))
            {
                return;
            }
            receiveExceptions.add(e);
        };
        return new HistogramReceiverFactory().
                listeningTo(serverSocketChannel).
                withExecutor(newCachedThreadPool(new FluteThreadFactory("test-handler"))).
                highestTrackableValue(MAX_VALUE).
                withExceptionConsumer(ec).
                dispatchingTo((sender, id, startTimestamp, endTimestamp, histogram) ->
                {
                    if (idFilter.equals(id))
                    {
                        copyHistogramInto(histogram, receivedList);
                    }
                }).
                create();
    }

    private long getTotalValueCount(final List<Histogram> histograms)
    {
        return histograms.stream().mapToLong(Histogram::getTotalCount).sum();
    }

    private static String errorMessage(final List<? extends Exception> errors)
    {
        if (errors.isEmpty())
        {
            return "No errors collected";
        }
        String message = "Failed due to " + errors.size() + " errors:\n";
        for (Exception error : errors)
        {
            message += error.getMessage() + "\n";
        }
        return message;
    }

    private static void copyHistogramInto(final Histogram input, final List<Histogram> receivedList)
    {
        receivedList.add(input.copy());
    }

    private enum DaemonThreadFactory implements ThreadFactory
    {
        DAEMON_THREAD_FACTORY;

        @Override
        public Thread newThread(final Runnable r)
        {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    }

    private void checkNoReceiverIsCurrentlyListening()
    {
        try
        {
            if (SocketChannel.open(socketAddress).isConnected())
            {
                Assert.fail("Something already listening");
            }
        }
        catch (IOException e)
        {
            // ignore
        }
    }

    private static long getWaitTimeout()
    {
        return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30L);
    }
}