package com.aitusoftware.flute.server.http;

import com.aitusoftware.flute.config.RequiredProperties;

import java.util.Properties;

import static java.lang.Boolean.parseBoolean;

public final class CacheConfig
{
    private final boolean isCaching;

    public CacheConfig(final boolean isCaching)
    {
        this.isCaching = isCaching;
    }

    public static CacheConfig fromFluteProperties(final Properties properties)
    {
        return new CacheConfig(parseBoolean(RequiredProperties.requiredProperty("flute.server.histogram.cache", properties)));
    }

    public boolean isCaching()
    {
        return isCaching;
    }
}
