package com.aitusoftware.flute.server.cache;

import java.util.Set;
import java.util.concurrent.TimeUnit;

final class MapKey
{
    final Set<String> metricIdentifiers;
    final long windowDuration;
    final TimeUnit durationUnit;
    final transient String metricKey;

    MapKey(final Set<String> metricIdentifiers, final long windowDuration,
           final TimeUnit durationUnit, final String metricKey)
    {
        this.metricIdentifiers = metricIdentifiers;
        this.windowDuration = windowDuration;
        this.durationUnit = durationUnit;
        this.metricKey = metricKey;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final MapKey mapKey = (MapKey) o;

        if (windowDuration != mapKey.windowDuration)
        {
            return false;
        }
        if (metricIdentifiers != null ? !metricIdentifiers.equals(mapKey.metricIdentifiers) : mapKey.metricIdentifiers != null)
        {
            return false;
        }
        return durationUnit == mapKey.durationUnit;

    }

    @Override
    public int hashCode()
    {
        int result = metricIdentifiers != null ? metricIdentifiers.hashCode() : 0;
        result = 31 * result + (int) (windowDuration ^ (windowDuration >>> 32));
        result = 31 * result + (durationUnit != null ? durationUnit.hashCode() : 0);
        return result;
    }
}
