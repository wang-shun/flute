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
import com.aitusoftware.flute.agent.annotation.FluteMetric;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

import static com.aitusoftware.flute.acceptance.framework.Waiter.waitFor;

@Ignore("Not working on JDK10")
public final class InstrumentationTest
{
    private final MetricServer metricServer = new MetricServer();

    @Test
    public void shouldRecordMetric() throws Exception
    {
        final TimedClient timedClient = new TimedClient();
        for(int i = 0; i < 100; i++)
        {
            timedClient.createSample();
        }

        final String metricName = "user.key.for." + System.getProperty("user.name");
        waitFor(() -> metricServer.getLatestCountForMetric(metricName) != 0);
    }

    private static final class TimedClient
    {
        @FluteMetric(metricName = "user.key.for.${user.name}")
        public void createSample()
        {
            LockSupport.parkNanos(15L);
        }
    }
}