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
package com.aitusoftware.flute.acceptance.framework;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class MetricServer
{
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 15002;
    private static final String SERVER_ADDRESS = "http://" + SERVER_HOST + ":" + SERVER_PORT;
    private static final long WINDOW_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(10L);
    private static final String SLA_QUERY_URI = "/flute/app/query/slaReport/%s/%d/%s/%d";
    private static final String METRIC_SEARCH_URI = "/flute/app/query/metricSearch/%s";
    private static final String SLA_PERCENTILES_URI = "/flute/app/query/slaPercentiles/%s/%d/%s/%d";
    private static final String LATEST_SLA_PERCENTILES_URI = "/flute/app/query/slaPercentiles/%s/%d/%s";
    private static final String LIST_REPORTS_URI = "/flute/app/query/listReports";

    public MetricServer()
    {
        SystemReadiness.waitForServer(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
    }

    public long getLatestCountForMetric(final String metricName)
    {
        final List data = querySlaReport(metricName);
        return ((Double)((List) data.get(0)).get(10)).longValue();
    }

    public long getMaxValueForMetric(final String metricName)
    {
        final List data = querySlaReport(metricName);
        return ((Double)((List) data.get(0)).get(9)).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPercentilesForMetric(
            final String metricName, final long duration, final TimeUnit durationUnit)
    {
        final String url = String.format(SERVER_ADDRESS + LATEST_SLA_PERCENTILES_URI, metricName,
                duration, durationUnit);
        return HttpOps.get(url, List.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPercentilesForMetric(final String metricName, final long startTime, final long endTime)
    {
        final String url = String.format(SERVER_ADDRESS + SLA_PERCENTILES_URI, metricName,
                endTime - startTime, TimeUnit.MILLISECONDS.name(), endTime);
        return HttpOps.get(url, List.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> searchMetricIdentifiers(final String searchSpec)
    {
        final String url = String.format(SERVER_ADDRESS + METRIC_SEARCH_URI, searchSpec);
        final List<String> matchingMetricIdentifiers = new Gson().fromJson(new InputStreamReader(get(url)), List.class);
        return new HashSet<>(matchingMetricIdentifiers);
    }

    @SuppressWarnings("unchecked")
    public List<String> waitForReport()
    {
        return new Gson().fromJson(new InputStreamReader(
                get(SERVER_ADDRESS + LIST_REPORTS_URI)), List.class);
    }

    private List querySlaReport(final String metricName)
    {
        final long now = System.currentTimeMillis();
        final String url = String.format(SERVER_ADDRESS + SLA_QUERY_URI, queryArgs(metricName, now));
        return new Gson().fromJson(new InputStreamReader(get(url)), List.class);
    }

    private static Object[] queryArgs(final String metricName, final long now)
    {
        return new Object[] {metricName, WINDOW_DURATION_MILLIS, TimeUnit.MILLISECONDS.name(), now};
    }

    private static InputStream get(final String url)
    {
        try
        {
            final HttpURLConnection connection = (HttpURLConnection)
                    new URL(url).openConnection();

            connection.connect();
            final int responseCode = connection.getResponseCode();
            if (responseCode != 200)
            {
                throw new IOException("Received response code: " + responseCode);
            }

            return connection.getInputStream();
        }
        catch(IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
