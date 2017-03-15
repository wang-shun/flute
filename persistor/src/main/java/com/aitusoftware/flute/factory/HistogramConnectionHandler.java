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

    public HistogramConnectionHandler(
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

    // TODO refactor this mess
    public void receiveLoop()
    {
        if(running.compareAndSet(false, true))
        {
            LOGGER.info("Entering receive loop");
            try
            {
                final Selector selector = Selector.open();
                executorService.submit(() ->
                {
                    while (running.get() && !Thread.currentThread().isInterrupted())
                    {
                        try
                        {
                            final int selected = selector.select(100);
                            if (selected != 0)
                            {
                                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                                for (Iterator<SelectionKey> iterator = selectedKeys.iterator(); iterator.hasNext(); )
                                {
                                    final SelectionKey selectedKey = iterator.next();
                                    final SelectableChannel channel = selectedKey.channel();
                                    try
                                    {
                                        final StreamingHistogramReceiver receiver = (StreamingHistogramReceiver) selectedKey.attachment();

                                        final ReadResult readResult = receiver.readFrom((ReadableByteChannel) channel,
                                                (InetSocketAddress) ((SocketChannel) channel).getRemoteAddress());

                                        if(readResult == ReadResult.END_OF_STREAM)
                                        {
                                            if(LOGGER.isDebugEnabled())
                                            {
                                                LOGGER.debug("Inbound connection was closed: {}", ((SocketChannel) channel).getRemoteAddress());
                                            }
                                            closeQuietly(selectedKey, channel);
                                        }
                                    }
                                    catch (final Exception e)
                                    {
                                        closeQuietly(selectedKey, channel);
                                        exceptionConsumer.accept(e);
                                    }
                                    finally
                                    {
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            exceptionConsumer.accept(e);
                        }
                    }
                    try
                    {
                        selector.keys().forEach(s ->
                        {
                            try
                            {
                                s.channel().close();
                            }
                            catch (IOException e)
                            {
                                // ignore
                            }
                        });
                        selector.close();
                    }
                    catch (IOException e)
                    {
                        // ignore for now
                    }
                });

                while (running.get() && !Thread.currentThread().isInterrupted() && serverSocketChannel.isOpen())
                {
                    try
                    {
                        final SocketChannel clientChannel = serverSocketChannel.accept();

                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ, new StreamingHistogramReceiver(histogramHandler, highestTrackableValue));
                    }
                    catch (final Exception e)
                    {
                        exceptionConsumer.accept(e);
                    }
                }
            }
            catch (final IOException e)
            {
                LOGGER.error("Failed to open selector, exiting", e);
                exceptionConsumer.accept(e);
            }
        }
    }

    private static void closeQuietly(final SelectionKey selectedKey, final SelectableChannel channel)
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
    }

    public void stop()
    {
        this.running.set(false);
    }
}