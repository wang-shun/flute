package com.aitusoftware.flute.server.cache;

import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.server.dao.jdbc.HistogramRetrievalDao;

public final class HistogramSourceProvider
{
    private final int maxCacheEntries;
    private final HistogramQueryFunction queryFunction;
    private final HistogramConfig histogramConfig;
    private final HistogramRetrievalDao histogramRetrievalDao;
    private final boolean isCaching;

    public HistogramSourceProvider(final int maxCacheEntries, final HistogramQueryFunction queryFunction,
                                   final HistogramConfig histogramConfig, final HistogramRetrievalDao histogramRetrievalDao,
                                   final boolean isCaching)
    {
        this.maxCacheEntries = maxCacheEntries;
        this.queryFunction = queryFunction;
        this.histogramConfig = histogramConfig;
        this.histogramRetrievalDao = histogramRetrievalDao;
        this.isCaching = isCaching;
    }

    public HistogramSource get()
    {
        if (isCaching)
        {
            return new HistogramCache(maxCacheEntries, histogramRetrievalDao,
                    System::currentTimeMillis, histogramConfig.asSupplier());
        }
        return new LiveHistogramSource(queryFunction, histogramConfig.asSupplier());
    }
}
