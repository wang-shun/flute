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
import com.aitusoftware.flute.receive.ReceiverProcess;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public final class HistogramReceiverFactory
{
    private ServerSocketChannel serverSocketChannel;
    private Consumer<Exception> exceptionConsumer;
    private HistogramHandler histogramHandler;
    private long highestTrackableValue;
    private ExecutorService executorService;
    private HistogramConnectionHandler connectionHandler;

    public HistogramReceiverFactory withExecutor(final ExecutorService executorService)
    {
        this.executorService = executorService;
        return this;
    }

    public HistogramReceiverFactory listeningTo(final ServerSocketChannel serverSocketChannel)
    {
        this.serverSocketChannel = serverSocketChannel;
        return this;
    }

    public HistogramReceiverFactory withExceptionConsumer(final Consumer<Exception> exceptionConsumer)
    {
        this.exceptionConsumer = exceptionConsumer;
        return this;
    }

    public HistogramReceiverFactory dispatchingTo(final HistogramHandler histogramHandler)
    {
        this.histogramHandler = histogramHandler;
        return this;
    }

    public HistogramReceiverFactory highestTrackableValue(final long highestTrackableValue)
    {
        this.highestTrackableValue = highestTrackableValue;
        return this;
    }

    public HistogramConnectionHandler connectionHandler()
    {
        return connectionHandler;
    }

    public ReceiverProcess create() throws IOException
    {
        serverSocketChannel.configureBlocking(true);
        connectionHandler =
                new HistogramConnectionHandler(serverSocketChannel, exceptionConsumer, highestTrackableValue,
                        executorService, histogramHandler);
        return new ReceiverProcess(connectionHandler, executorService);
    }
}