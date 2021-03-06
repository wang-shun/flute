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

import com.aitusoftware.flute.config.RequiredProperties;

import java.util.Properties;

import static java.lang.Boolean.parseBoolean;

public final class CacheConfig
{
    private final boolean isCaching;
    private final boolean storeCompressedHistograms;
    private final int capacity;

    private CacheConfig(
            final boolean isCaching,
            final boolean storeCompressedHistograms,
            final int capacity)
    {
        this.isCaching = isCaching;
        this.storeCompressedHistograms = storeCompressedHistograms;
        this.capacity = capacity;
    }

    static CacheConfig fromFluteProperties(final Properties properties)
    {
        return new CacheConfig(
                parseBoolean(RequiredProperties.requiredProperty("flute.server.histogram.cache.enabled", properties)),
                parseBoolean(RequiredProperties.requiredProperty("flute.server.histogram.compressed", properties)),
                Integer.parseInt(properties.getProperty("flute.server.histogram.cache.capacity", "100")));
    }

    public boolean isCaching()
    {
        return isCaching;
    }

    public int getCapacity()
    {
        return capacity;
    }

    public boolean storeCompressedHistograms()
    {
        return storeCompressedHistograms;
    }
}
