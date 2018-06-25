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
package com.aitusoftware.flute.server.cache;

import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.server.dao.jdbc.HistogramRetrievalDao;
import com.aitusoftware.flute.server.http.CacheConfig;

public final class HistogramSourceProvider
{
    private final HistogramQueryFunction queryFunction;
    private final HistogramConfig histogramConfig;
    private final HistogramRetrievalDao histogramRetrievalDao;
    private final CacheConfig cacheConfig;

    public HistogramSourceProvider(final HistogramQueryFunction queryFunction,
                                   final HistogramConfig histogramConfig,
                                   final HistogramRetrievalDao histogramRetrievalDao,
                                   final CacheConfig cacheConfig)
    {
        this.queryFunction = queryFunction;
        this.histogramConfig = histogramConfig;
        this.histogramRetrievalDao = histogramRetrievalDao;
        this.cacheConfig = cacheConfig;
    }

    public HistogramSource get()
    {
        if (cacheConfig.isCaching())
        {
            return new HistogramCache(cacheConfig.getCapacity(), histogramRetrievalDao,
                    System::currentTimeMillis, histogramConfig.asSupplier());
        }
        return new LiveHistogramSource(queryFunction, histogramConfig.asSupplier());
    }
}