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

import com.aitusoftware.flute.config.DatabaseConfig;
import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.config.TcpReceiverConfig;
import com.aitusoftware.flute.lifecycle.ShutdownListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Consumer;

public final class FluteMetricsPersistorMain
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FluteMetricsPersistorMain.class);

    public static void main(final String[] args) throws Exception
    {
        if(args.length < 1)
        {
            throw new IllegalArgumentException("Please supply properties file path");
        }

        final Properties properties = new Properties();
        properties.load(new FileReader(args[0]));

        final Consumer<SQLException> persistenceExceptionConsumer = FluteMetricsPersistorMain::logException;
        final Consumer<Exception> receiverExceptionConsumer = FluteMetricsPersistorMain::logException;
        final DatabaseConfig databaseConfig = DatabaseConfig.fromFluteProperties(properties, "metrics");
        final TcpReceiverConfig tcpReceiverConfig = TcpReceiverConfig.fromFluteProperties(properties);
        final HistogramConfig histogramConfig = HistogramConfig.fromFluteProperties(properties);

        final FluteMetricsPersistor persistor =
                new FluteMetricsPersistor(databaseConfig, tcpReceiverConfig, histogramConfig,
                        receiverExceptionConsumer, persistenceExceptionConsumer);

        persistor.start();
        new ShutdownListener(14001).waitForShutdownEvent();
    }

    private static void logException(final Throwable t)
    {
        LOGGER.error("Exception caught in persistor", t);
    }
}