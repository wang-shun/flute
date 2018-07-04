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
package com.aitusoftware.flute.integration.client;

import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.config.RequiredProperties;

import java.io.FileReader;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.flute.config.SocketAddressParser.fromAddressSpec;

public final class TimingClientMain
{
    private static final long RUNTIME_MINUTES = Long.getLong("flute.test.client.runtime", 4);
    private static final int SERIES_COUNT = Integer.getInteger("flute.test.client.seriesCount", 6);

    public static void main(final String[] args) throws Exception
    {
        if(args.length < 1)
        {
            throw new IllegalArgumentException("Please supply properties file path");
        }

        final Properties properties = new Properties();
        properties.load(new FileReader(args[0]));

        final SocketAddress recordingAddress = fromAddressSpec(RequiredProperties.requiredProperty("flute.client.reporting.tcp.address", properties));
        final HistogramConfig histogramConfig = HistogramConfig.fromFluteProperties(properties);

        final TimingClient timingClient =
                new TimingClient(
                        SERIES_COUNT,
                        histogramConfig, recordingAddress);
        timingClient.init();

        timingClient.generateTestData(RUNTIME_MINUTES, TimeUnit.MINUTES);
    }
}