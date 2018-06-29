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

import com.aitusoftware.flute.receive.HistogramHandler;
import com.aitusoftware.flute.receive.ReadResult;
import com.aitusoftware.flute.receive.StreamingHistogramReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class HistogramConnectionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HistogramConnectionHandler.class);
    private final ServerSocketChannel serverSocketChannel;
    private final Consumer<Exception> exceptionConsumer;
    private final long highestTrackableValue;
    private final ExecutorService executorService;
    private final HistogramHandler histogramHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger connectedSockets = new AtomicInteger(0);

    HistogramConnectionHandler(
            final ServerSocketChannel serverSocketChannel,
            final Consumer<Exception> exceptionConsumer,
            final long highestTrackableValue,
            final ExecutorService executorService,
            final HistogramHandler histogramHandler)
    {
        this.serverSocketChannel = serverSocketChannel;
        this.exceptionConsumer = exceptionConsumer;
        this.highestTrackableValue = highestTrackableValue;
        this.executorService = executorService;
        this.histogramHandler = histogramHandler;
    }

    public void receiveLoop()
    {
        if(running.compareAndSet(false, true))
        {
            LOGGER.info("Entering receive loop");
            try
            {
                final Selector selector = Selector.open();
                executorService.submit(
                        new InboundDataProcessor(running, selector, exceptionConsumer, this::closeQuietly));
                acceptIncomingConnections(selector);
            }
            catch (final IOException e)
            {
                LOGGER.error("Failed to open selector, exiting", e);
                exceptionConsumer.accept(e);
            }
            catch (final RuntimeException e)
            {
                LOGGER.error("Unexpected exception", e);
                exceptionConsumer.accept(e);
            }
        }
    }

    private void acceptIncomingConnections(final Selector selector)
    {
        while (running.get() && !Thread.currentThread().isInterrupted() && serverSocketChannel.isOpen())
        {
            try
            {
                final SocketChannel clientChannel = serverSocketChannel.accept();

                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_READ, new StreamingHistogramReceiver(histogramHandler, highestTrackableValue));
                connectedSockets.incrementAndGet();
            }
            catch (final Exception e)
            {
                exceptionConsumer.accept(e);
            }
        }
    }

    private static final class InboundDataProcessor implements Runnable
    {
        private final AtomicBoolean running;
        private final Selector selector;
        private final Consumer<Exception> exceptionConsumer;
        private final BiConsumer<SelectionKey, SelectableChannel> closeFunction;

        InboundDataProcessor(final AtomicBoolean running, final Selector selector,
                             final Consumer<Exception> exceptionConsumer,
                             final BiConsumer<SelectionKey, SelectableChannel> closeFunction)
        {
            this.running = running;
            this.selector = selector;
            this.exceptionConsumer = exceptionConsumer;
            this.closeFunction = closeFunction;
        }

        @Override
        public void run()
        {
            while (running.get() && !Thread.currentThread().isInterrupted())
            {
                try
                {
                    if (selector.select(100) != 0)
                    {
                        processSelectedKeys();
                    }
                    else
                    {
                        final Set<SelectionKey> allKeys = selector.keys();
                        for (SelectionKey key : allKeys)
                        {
                            processChannel(key, key.channel(), false);
                        }
                    }
                }
                catch (Exception e)
                {
                    exceptionConsumer.accept(e);
                }
            }
            closeActiveConnections();
        }

        private void processSelectedKeys()
        {
            final Set<SelectionKey> selectedKeys = selector.selectedKeys();
            for (Iterator<SelectionKey> iterator = selectedKeys.iterator(); iterator.hasNext(); )
            {
                final SelectionKey selectedKey = iterator.next();
                final SelectableChannel channel = selectedKey.channel();
                try
                {
                    processChannel(selectedKey, channel, true);
                }
                finally
                {
                    iterator.remove();
                }
            }
        }

        private void processChannel(final SelectionKey selectedKey, final SelectableChannel channel,
                                    final boolean wasSelected)
        {
            try
            {
                final StreamingHistogramReceiver receiver = (StreamingHistogramReceiver) selectedKey.attachment();

                final ReadResult readResult = receiver.readFrom((ReadableByteChannel) channel,
                        (InetSocketAddress) ((SocketChannel) channel).getRemoteAddress());

                if (readResult == ReadResult.END_OF_STREAM)
                {
                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("Inbound connection was closed: {}", ((SocketChannel) channel).getRemoteAddress());
                    }
                    closeFunction.accept(selectedKey, channel);
                }
                else if (readResult == ReadResult.OK && !wasSelected)
                {
                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("Connection was not selected, but data was available and read.");
                    }
                }
            }
            catch (final Exception e)
            {
                closeFunction.accept(selectedKey, channel);
                exceptionConsumer.accept(e);
            }
        }

        private void closeActiveConnections()
        {
            try
            {
                selector.keys().forEach(s ->
                {
                    closeFunction.accept(s, s.channel());
                });
                selector.close();
            }
            catch (IOException e)
            {
                // ignore for now
            }
        }
    }

    private void closeQuietly(final SelectionKey selectedKey, final SelectableChannel channel)
    {
        selectedKey.cancel();
        try
        {
            channel.close();
        }
        catch (IOException e)
        {
            // ignore
        }
        connectedSockets.decrementAndGet();
    }

    public int getConnectedSocketCount()
    {
        return connectedSockets.get();
    }

    public void stop()
    {
        this.running.set(false);
    }
}