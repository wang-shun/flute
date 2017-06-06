/*
 * Copyright 2016 Aitu Software Limited.
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
package com.aitusoftware.flute.test.client;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public final class RecordedMetricsClient
{
    private final String serverHost;
    private final int serverPort;

    public RecordedMetricsClient(final String serverHost, final int serverPort)
    {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public boolean hasDetailForSeries(final String seriesName)
    {
        try
        {
            final String queryUrl = generateDetailQueryUrl();
            final List<Map<String, Object>> data = jsonQuery(queryUrl);

            return data.stream().filter(m -> seriesName.equals(m.get("identifier"))).findAny().isPresent();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public boolean hasSummaryForSeries(final String seriesName)
    {
        try
        {
            final String queryUrl = generateSummmaryQueryUrl();
            final List<Map<String, Object>> data = jsonQuery(queryUrl);

            return data.stream().filter(m -> seriesName.equals(m.get("identifier"))).findAny().isPresent();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Map<String, Object>> jsonQuery(final String queryUrl) throws IOException
    {
        final HttpURLConnection connection = (HttpURLConnection) new URL(queryUrl).openConnection();
        if(200 != connection.getResponseCode())
        {
            throw new IllegalStateException("Failed to retrieve data from server: " + connection.getResponseMessage());
        }

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> data = new Gson().fromJson(
                new InputStreamReader(connection.getInputStream()), List.class);
        connection.disconnect();
        return data;
    }

    private String generateSummmaryQueryUrl()
    {
        return "http://" + serverHost + ":" + serverPort + "/flute/query/summary";
    }

    private String generateDetailQueryUrl()
    {
        return "http://" + serverHost + ":" + serverPort + "/flute/query/detail";
    }
}