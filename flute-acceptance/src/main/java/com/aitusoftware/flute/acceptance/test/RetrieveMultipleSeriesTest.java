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
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.aitusoftware.flute.acceptance.framework.Waiter.waitFor;

public final class RetrieveMultipleSeriesTest
{
    private final Random random = new Random(System.currentTimeMillis() + System.nanoTime());
    private final UUID uuid = new UUID(random.nextLong(), random.nextLong());
    private final String token = uuid.toString();

    private final MetricServer metricServer = new MetricServer();
    private TestClient testClient;

    @Before
    public void before() throws Exception
    {
        testClient = new TestClient(token, System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(60L));
        for(int i = 0; i < 45; i++)
        {
            recordAndPublish(testClient, random.nextInt(5990));
        }
    }

    @Test
    public void shouldSerialisePercentiles() throws Exception
    {
        waitFor(() -> {
            final List<Map<String, Object>> recentHistory = metricServer.getPercentilesForMetric(testClient.getMetricName(), 40, TimeUnit.SECONDS);
            final List<Map<String, Object>> olderHistory = metricServer.getPercentilesForMetric(testClient.getMetricName(), 60, TimeUnit.SECONDS);
            final List<Map<String, Object>> ancientHistory = metricServer.getPercentilesForMetric(testClient.getMetricName(), 80, TimeUnit.SECONDS);

            return histogramsArePopulatedAndDifferent(recentHistory, olderHistory, ancientHistory);
        });
    }

    @SafeVarargs
    private static boolean histogramsArePopulatedAndDifferent(
            final List<Map<String, Object>>... histogramData)
    {
        boolean allDistinct = true;
        for (int i = 0; i < histogramData.length && allDistinct; i++)
        {
            for (int j = 0; j < histogramData.length; j++)
            {
                if (i != j && histogramData[i].equals(histogramData[j]))
                {
                    allDistinct = false;
                    break;
                }
            }
        }
        return allDistinct;
    }

    private void recordAndPublish(final TestClient testClient, final int maxEntries)
    {
        for(int i = 0; i < maxEntries + 1100; i++)
        {
            testClient.recordSample(random.nextInt(65536) + 256);
        }
        testClient.publish();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(120L));
    }
}
