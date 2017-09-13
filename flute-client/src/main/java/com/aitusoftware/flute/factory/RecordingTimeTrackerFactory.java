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
package com.aitusoftware.flute.factory;

import com.aitusoftware.flute.compatibility.LongSupplier;
import com.aitusoftware.flute.compatibility.Supplier;
import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.exchanger.Exchanger;
import com.aitusoftware.flute.exchanger.MultiWriterTimeBasedValueExchanger;
import com.aitusoftware.flute.exchanger.TimeBasedValueExchanger;
import com.aitusoftware.flute.record.HistogramRecorder;
import com.aitusoftware.flute.record.Stopwatch;
import com.aitusoftware.flute.record.TimeTracker;
import com.aitusoftware.flute.send.AggregatingDataSender;
import com.aitusoftware.flute.send.HistogramPublisher;
import com.aitusoftware.flute.send.NonBlockingAggregator;
import com.aitusoftware.flute.send.Sender;
import com.aitusoftware.flute.send.SocketChannelConnector;
import com.aitusoftware.flute.send.events.AggregatorEvents;
import com.aitusoftware.flute.send.events.LoggingAggregatorEvents;
import com.aitusoftware.flute.util.timing.SystemEpochMillisTimeSupplier;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory for generating a StopWatch.
 */
public class RecordingTimeTrackerFactory
{
    private static final long UNSET = Long.MIN_VALUE;
    private boolean validating = false;
    private String identifier;
    private long highestTrackableValue;
    private long publishInterval = UNSET;
    private TimeUnit publishTimeUnit;
    private LongSupplier clock = new SystemEpochMillisTimeSupplier();
    private AtomicBoolean aggregatorStarted = new AtomicBoolean(false);
    private NonBlockingAggregator nonBlockingAggregator;
    private HistogramConfig histogramConfig;
    private boolean multiThreadedAccess = true;
    private AggregatorEvents aggregatorEvents = new LoggingAggregatorEvents();
    protected Supplier<Supplier<SocketChannel>> socketConnectorFactory;

    /**
     * Indicates whether the event sink will be written to by multiple threads.
     * @param multiThreadedAccess is multi-threaded access required
     * @return the factory
     */
    public final RecordingTimeTrackerFactory withMultiThreadedAccess(final boolean multiThreadedAccess)
    {
        this.multiThreadedAccess = multiThreadedAccess;
        return this;
    }

    /**
     * Allows the clock to be overridden - defaults to system epoch millis
     * @param clock the clock
     * @return the factory
     */
    public final RecordingTimeTrackerFactory withClock(final LongSupplier clock)
    {
        this.clock = clock;
        return this;
    }

    /**
     * Defines the size and precision of the histograms
     * @param histogramConfig histogram configuration
     * @return the factory
     */
    public final RecordingTimeTrackerFactory withHistogramConfig(final HistogramConfig histogramConfig)
    {
        this.histogramConfig = histogramConfig;
        this.highestTrackableValue = histogramConfig.getMaxValue();
        return this;
    }

    /**
     * Defines the end-point of the persistor
     * @param socketAddress the target address
     * @return the factory
     * @throws IOException if a Selector cannot be opened
     */
    public final RecordingTimeTrackerFactory publishingTo(final SocketAddress socketAddress) throws IOException
    {
        this.socketConnectorFactory = new Supplier<Supplier<SocketChannel>>()
        {
            @Override
            public Supplier<SocketChannel> get()
            {
                return new SocketChannelConnector(socketAddress, aggregatorEvents);
            }
        };
        this.nonBlockingAggregator = new NonBlockingAggregator(socketConnectorFactory.get(),
                10L, TimeUnit.MILLISECONDS, aggregatorEvents);
        return this;
    }

    /**
     * Sets the identifier used for aggregating metrics
     * @param identifer the identifier
     * @return the factory
     */
    public final RecordingTimeTrackerFactory withIdentifer(final String identifer)
    {
        this.identifier = identifer;
        return this;
    }

    /**
     * Specifies the publish rate
     * @param interval the interval
     * @param timeUnit the units of the publish interval
     * @return the factory
     */
    public final RecordingTimeTrackerFactory publishingEvery(final long interval, final TimeUnit timeUnit)
    {
        this.publishInterval = interval;
        this.publishTimeUnit = timeUnit;
        return this;
    }

    public final RecordingTimeTrackerFactory withValidation(final boolean validating)
    {
        this.validating = validating;
        return this;
    }

    /**
     * User code can provide a handler for exceptional events. The default will log the events to log4j.
     * @param aggregatorEvents the event handler
     * @return the factory
     */
    public RecordingTimeTrackerFactory withSenderEvents(final AggregatorEvents aggregatorEvents)
    {
        this.aggregatorEvents = aggregatorEvents;
        return this;
    }

    /**
     * Destroys all time tracker resources (sockets, histograms).
     *
     * Should be called when tracking is no longer required, but JVM will continue running.
     */
    public void shutdown()
    {
        if (nonBlockingAggregator != null)
        {
            nonBlockingAggregator.shutdown();
        }
    }

    /**
     * Creates, using the provided properties a TimeTracker instance
     * @return the TimeTracker
     */
    public final TimeTracker create()
    {
        if (validating)
        {
            if (highestTrackableValue < 1L)
            {
                throw new IllegalArgumentException(
                        String.format("Highest trackable value must be a positive integer, but was %d", highestTrackableValue));
            }
        }

        final Sender sender = new AggregatingDataSender(4194304, 524288);
        final HistogramPublisher histogramPublisher = new HistogramPublisher(sender, identifier, aggregatorEvents);
        final Supplier<Histogram> histogramSupplier;
        final Runnable recordCompleteNotifier;
        final Supplier<Histogram> histogramFactory = new Supplier<Histogram>()
        {
            @Override
            public Histogram get()
            {
                return multiThreadedAccess ?
                        new AtomicHistogram(histogramConfig.getMaxValue(), histogramConfig.getSignificantDigits()) :
                        new Histogram(histogramConfig.getMaxValue(), histogramConfig.getSignificantDigits());
            }
        };

        final Exchanger exchanger;
        if(multiThreadedAccess)
        {
            final MultiWriterTimeBasedValueExchanger<Histogram> multiWriterExchanger =
                    new MultiWriterTimeBasedValueExchanger<Histogram>(
                            histogramFactory.get(), histogramFactory.get(), histogramPublisher,
                            clock, publishInterval, publishTimeUnit);
            histogramSupplier = new Supplier<Histogram>()
            {
                @Override
                public Histogram get()
                {
                    return multiWriterExchanger.acquire();
                }
            };
            recordCompleteNotifier = new Runnable()
            {
                @Override
                public void run()
                {
                    multiWriterExchanger.release();
                }
            };
            exchanger = multiWriterExchanger;

        }
        else
        {
            final TimeBasedValueExchanger<Histogram> singleWriterExchanger =
                    new TimeBasedValueExchanger<Histogram>(
                            histogramFactory.get(), histogramFactory.get(), histogramPublisher,
                            clock, publishInterval, publishTimeUnit);
            histogramSupplier = new Supplier<Histogram>()
            {
                @Override
                public Histogram get()
                {
                    return singleWriterExchanger.acquire();
                }
            };
            recordCompleteNotifier = new Runnable()
            {
                @Override
                public void run()
                {
                    singleWriterExchanger.release();
                }
            };
            exchanger = singleWriterExchanger;
        }

        if(aggregatorStarted.compareAndSet(false, true))
        {
            final Thread sendingThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    nonBlockingAggregator.run();
                }
            });
            sendingThread.setDaemon(true);
            sendingThread.setName("flute-sending-daemon");
            sendingThread.start();
        }
        nonBlockingAggregator.register(exchanger, (AggregatingDataSender) sender, identifier);

        final HistogramRecorder histogramRecorder =
                new HistogramRecorder(histogramSupplier, recordCompleteNotifier, highestTrackableValue);

        return new Stopwatch(validating, histogramRecorder);
    }

}