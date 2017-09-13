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
package com.aitusoftware.flute.config;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.flute.config.RequiredProperties.requiredProperty;
import static java.lang.Long.parseLong;

/**
 * Defines the frequency at which data should be published to the persistor.
 */
public final class PublicationIntervalConfig
{
    private final long interval;
    private final TimeUnit unit;

    /**
     * Constructor
     * @param interval interval between publications
     * @param unit unit of interval
     */
    public PublicationIntervalConfig(final long interval, final TimeUnit unit)
    {
        this.interval = interval;
        this.unit = unit;
    }

    public long getInterval()
    {
        return interval;
    }

    public TimeUnit getUnit()
    {
        return unit;
    }

    public static PublicationIntervalConfig fromFluteProperties(final Properties properties)
    {
        return new PublicationIntervalConfig(parseLong(requiredProperty("flute.client.publication.interval", properties)),
                TimeUnit.valueOf(requiredProperty("flute.client.publication.unit", properties)));
    }
}
