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
package com.aitusoftware.flute.server.http;

import com.aitusoftware.flute.config.DatabaseConfig;
import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.server.config.ServerConfig;

import java.io.FileReader;
import java.util.Properties;

public final class HttpQueryServerMain
{
    public static void main(final String[] args) throws Exception
    {
        if(args.length < 1)
        {
            throw new IllegalArgumentException("Please supply properties file path");
        }

        final Properties properties = new Properties();
        properties.load(new FileReader(args[0]));

        final DatabaseConfig metricsDatabaseConfig = DatabaseConfig.fromFluteProperties(properties, "metrics");
        final ServerConfig serverConfig = ServerConfig.fromFluteProperties(properties);
        final HistogramConfig histogramConfig = HistogramConfig.fromFluteProperties(properties);
        final DatabaseConfig reportDatabaseConfig = DatabaseConfig.fromFluteProperties(properties, "reports");
        final CacheConfig cacheConfig = CacheConfig.fromFluteProperties(properties);

        final HistogramQueryServer queryServer =
                new HistogramQueryServer(serverConfig, metricsDatabaseConfig,
                        histogramConfig, reportDatabaseConfig, cacheConfig);
        queryServer.run();
    }
}