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
package com.aitusoftware.flute.acceptance.test;

import com.aitusoftware.flute.acceptance.framework.MetricServer;
import com.aitusoftware.flute.acceptance.framework.TestClient;
import com.aitusoftware.flute.acceptance.framework.Waiter;
import org.junit.Before;
import org.junit.Test;

public final class SmokeTest
{
    private final MetricServer metricServer = new MetricServer();
    private TestClient testClient;

    @Before
    public void before() throws Exception
    {
        testClient = new TestClient("foo.bar.smoke.test", System.currentTimeMillis());
    }

    @Test
    public void shouldRecordMetric() throws Exception
    {
        testClient.recordSample(500L);
        testClient.publish();

        Waiter.waitFor(() -> metricServer.getLatestCountForMetric(testClient.getMetricName()) == 1);
    }

    @Test
    public void shouldRecordMultipleDataPointsForMetric() throws Exception
    {
        testClient.recordSample(500L);
        testClient.publish();

        testClient.recordSample(700L);
        testClient.publish();

        Waiter.waitFor(() -> metricServer.getLatestCountForMetric(testClient.getMetricName()) == 2L);
        Waiter.waitFor(() -> metricServer.getMaxValueForMetric(testClient.getMetricName()) == 700L);
    }

    @Test
    public void shouldRecordDataForDifferentMetrics() throws Exception
    {
        final TestClient otherTestClient = new TestClient("smoke.test1", System.currentTimeMillis());

        testClient.recordSample(500L);
        testClient.recordSample(700L);
        testClient.publish();

        otherTestClient.recordSample(15L);
        otherTestClient.recordSample(25L);
        otherTestClient.recordSample(35L);
        otherTestClient.recordSample(45L);
        otherTestClient.publish();

        Waiter.waitFor(() -> metricServer.getLatestCountForMetric(testClient.getMetricName()) == 2L);
        Waiter.waitFor(() -> metricServer.getLatestCountForMetric(otherTestClient.getMetricName()) == 4L);
    }
}