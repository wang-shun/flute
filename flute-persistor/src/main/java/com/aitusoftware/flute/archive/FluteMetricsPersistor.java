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
package com.aitusoftware.flute.archive;

import com.aitusoftware.flute.archive.jdbc.HistogramInsertDao;
import com.aitusoftware.flute.config.ConnectionFactory;
import com.aitusoftware.flute.config.DatabaseConfig;
import com.aitusoftware.flute.config.FluteThreadFactory;
import com.aitusoftware.flute.config.FlywayProperties;
import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.config.TcpReceiverConfig;
import com.aitusoftware.flute.factory.HistogramReceiverFactory;
import com.aitusoftware.flute.receive.ReceiverProcess;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.sql.SQLException;
import java.util.function.Consumer;

import static java.util.concurrent.Executors.newCachedThreadPool;

public final class FluteMetricsPersistor
{
    private static final int ONE_MEGABYTE = 1024 * 1024;
    private final DatabaseConfig databaseConfig;
    private final TcpReceiverConfig tcpReceiverConfig;
    private final HistogramConfig histogramConfig;
    private final Consumer<Exception> receiverExceptionConsumer;
    private final Consumer<SQLException> persistenceExceptionConsumer;

    public FluteMetricsPersistor(
            final DatabaseConfig databaseConfig,
            final TcpReceiverConfig tcpReceiverConfig,
            final HistogramConfig histogramConfig,
            final Consumer<Exception> receiverExceptionConsumer,
            final Consumer<SQLException> persistenceExceptionConsumer)
    {
        this.databaseConfig = databaseConfig;
        this.tcpReceiverConfig = tcpReceiverConfig;
        this.histogramConfig = histogramConfig;
        this.receiverExceptionConsumer = receiverExceptionConsumer;
        this.persistenceExceptionConsumer = persistenceExceptionConsumer;
    }

    public void start()
    {
        new ManagedDatabase().init(new FlywayProperties(databaseConfig).getProperties());
        final ConnectionFactory connectionFactory = new ConnectionFactory(databaseConfig);
        final HistogramInsertDao insertDao = new HistogramInsertDao(connectionFactory.getConnection(),
                persistenceExceptionConsumer, ONE_MEGABYTE);


        try
        {
            final ReceiverProcess tcpReceiverProcess = new HistogramReceiverFactory().
                    listeningTo(createServerSocketChannel(tcpReceiverConfig.getSocketAddress())).
                    withExceptionConsumer(receiverExceptionConsumer).
                    withExecutor(newCachedThreadPool(new FluteThreadFactory("tcp-persistor"))).
                    highestTrackableValue(histogramConfig.getMaxValue()).
                    dispatchingTo(insertDao).
                    create();
            tcpReceiverProcess.start();
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException(e);
        }

    }

    private ServerSocketChannel createServerSocketChannel(final SocketAddress socketAddress) throws IOException
    {
        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(true);
        return serverSocketChannel.bind(socketAddress);
    }
}
