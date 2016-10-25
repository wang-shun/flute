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
package com.aitusoftware.flute.acceptance.test;

import com.aitusoftware.flute.acceptance.framework.MetricServer;
import com.aitusoftware.flute.acceptance.framework.TestClient;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static com.aitusoftware.flute.acceptance.framework.Waiter.waitFor;

public final class MetricIdentifierSearchTest
{
    private final MetricServer metricServer = new MetricServer();

    @Test
    public void shouldRecordDataForDifferentMetrics() throws Exception
    {
        final String metricOne = publishMetric("metric.search.1");
        final String metricTwo = publishMetric("metric.search.2");
        final String metricThree = publishMetric("search.3");
        final String metricFour = publishMetric("foo.bar");

        waitFor(() -> metricServer.searchMetricIdentifiers(metricOne).
                containsAll(new HashSet<>(Collections.singleton(metricOne))));
        waitFor(() -> metricServer.searchMetricIdentifiers(metricThree).
                containsAll(new HashSet<>(Collections.singleton(metricThree))));
        waitFor(() -> metricServer.searchMetricIdentifiers("metric.*").
                containsAll(new HashSet<>(Arrays.asList(metricOne, metricTwo))));
        waitFor(() -> metricServer.searchMetricIdentifiers(".*search.*").
                containsAll(new HashSet<>(Arrays.asList(metricOne, metricTwo, metricThree))));
        waitFor(() -> metricServer.searchMetricIdentifiers(".*o\\.b.*").
                containsAll(new HashSet<>(Collections.singleton((metricFour)))));
    }

    private String publishMetric(final String alias) throws IOException
    {
        final TestClient otherTestClient = new TestClient(alias);

        otherTestClient.recordSample(37L);
        otherTestClient.publish();
        return otherTestClient.getMetricName();
    }
}