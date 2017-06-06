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
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.aitusoftware.flute.acceptance.framework.Waiter.waitFor;

public final class SlaPercentilesFromAggregatedMetricsTest
{
    private final Random random = new Random(System.currentTimeMillis() + System.nanoTime());
    private final UUID uuid = new UUID(random.nextLong(), random.nextLong());
    private final String token = uuid.toString();
    private final MetricServer metricServer = new MetricServer();
    private long recordingStartTime;
    private TestClient testClient;

    @Before
    public void before() throws Exception
    {
        testClient = new TestClient(token);
        recordingStartTime = testClient.getCurrentTime() - TimeUnit.SECONDS.toMillis(5L);
        for(int i = 0; i < 45; i++)
        {
            recordAndPublish(testClient, random.nextInt(5990));
        }
    }

    @Test
    public void shouldSerialisePercentiles() throws Exception
    {
        final long recordingEndTime = testClient.getCurrentTime() - TimeUnit.SECONDS.toMillis(5L);
        waitFor(() -> {
            final List<Map<String, Object>> percentilesForMetric = metricServer.getPercentilesForMetric(testClient.getMetricName(), recordingStartTime, recordingEndTime);
            return percentilesForMetric.size() > 1;
        });
    }

    private void recordAndPublish(final TestClient testClient, final int maxEntries)
    {
        for(int i = 0; i < maxEntries + 1100; i++)
        {
            testClient.recordSample(random.nextInt(65536) + 256);
        }
        testClient.publish();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20L));
    }
}
