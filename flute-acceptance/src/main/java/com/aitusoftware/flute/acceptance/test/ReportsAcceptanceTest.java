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

import com.aitusoftware.flute.acceptance.framework.ApiCallResult;
import com.aitusoftware.flute.acceptance.framework.ReportServer;
import com.aitusoftware.flute.acceptance.framework.TestClient;
import com.aitusoftware.flute.acceptance.framework.Threshold;
import com.aitusoftware.flute.acceptance.framework.TimeWindow;
import com.aitusoftware.flute.server.config.Metric;
import com.aitusoftware.flute.server.reporting.domain.ReportSpecification;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.aitusoftware.flute.acceptance.framework.Waiter.waitFor;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class ReportsAcceptanceTest
{
    private final Random random = new Random(System.currentTimeMillis() + System.nanoTime());
    private final UUID uuid = new UUID(random.nextLong(), random.nextLong());
    private final String token = uuid.toString();
    private final ReportServer reportServer = new ReportServer();

    @Before
    public void before() throws Exception
    {
        final TestClient testClientOne = new TestClient(token + ".one", System.currentTimeMillis());
        final TestClient testClientTwo = new TestClient(token + ".two", System.currentTimeMillis());
        final TestClient testClientThree = new TestClient(token + ".three", System.currentTimeMillis());

        for(int i = 0; i < 45; i++)
        {
            recordAndPublish(testClientOne, random.nextInt(5990));
            recordAndPublish(testClientTwo, random.nextInt(5990));
            recordAndPublish(testClientThree, random.nextInt(5990));
        }
    }

    @Test
    public void shouldCreateReportFromExistingMetrics()
    {
        final String reportName = token + "_report";
        reportServer.createReport(reportName, ".*" + token + ".*",
                asList(new TimeWindow(TimeUnit.SECONDS, 1),
                       new TimeWindow(TimeUnit.SECONDS, 5),
                       new TimeWindow(TimeUnit.SECONDS, 30)),
                asList(
                        new Threshold(Metric.TWO_NINES.name(), 4500),
                        new Threshold(Metric.FOUR_NINES.name(), 6500),
                        new Threshold(Metric.MEAN.name(), 3000)));

        waitFor(() -> reportServer.getReportNames().contains(reportName));
    }

    @Test
    public void shouldFailToCreateReportWithDuplicateName() throws Exception
    {
        final String reportName = token + "_report";
        reportServer.createReport(reportName, ".*" + token + ".*",
                singletonList(new TimeWindow(TimeUnit.SECONDS, 1)),
                singletonList(new Threshold(Metric.TWO_NINES.name(), 4500)));

        waitFor(() -> reportServer.getReportNames().contains(reportName));

        final ApiCallResult duplicateNameResult = reportServer.createReport(reportName, ".*" + token + ".*",
                singletonList(new TimeWindow(TimeUnit.SECONDS, 1)),
                singletonList(new Threshold(Metric.TWO_NINES.name(), 4500)));

        assertThat(duplicateNameResult.isSuccessful(), is(false));
    }

    @Test
    public void shouldDeleteReportByName() throws Exception
    {
        final String reportName = token + "_report";
        reportServer.createReport(reportName, ".*" + token + ".*",
                singletonList(new TimeWindow(TimeUnit.SECONDS, 1)),
                singletonList(new Threshold(Metric.TWO_NINES.name(), 4500)));

        waitFor(() -> reportServer.getReportNames().contains(reportName));

        final ApiCallResult deleteResult = reportServer.deleteReport(reportName);

        assertThat(deleteResult.isSuccessful(), is(true));

        waitFor(() -> !reportServer.getReportNames().contains(reportName));
    }

    @Test
    public void shouldAmendReportByName() throws Exception
    {
        final String reportName = token + "_report";
        reportServer.createReport(reportName, ".*" + token + ".*",
                singletonList(new TimeWindow(TimeUnit.SECONDS, 1)),
                singletonList(new Threshold(Metric.TWO_NINES.name(), 4500)));

        waitFor(() -> reportServer.getReportNames().contains(reportName));

        final String amendedPattern = ".*matchNothing.*";
        reportServer.amendReport(reportName, amendedPattern,
                asList(new TimeWindow(TimeUnit.SECONDS, 5), new TimeWindow(TimeUnit.HOURS, 1)),
                asList(new Threshold(Metric.TWO_NINES.name(), 4500), new Threshold(Metric.FOUR_NINES.name(), 9500)));

        waitFor(() -> {
            final ReportSpecification reportSpecification = reportServer.getReport(reportName);
            return reportSpecification.getSelectorPattern().equals(amendedPattern) &&
                    reportSpecification.getThresholds().size() == 2 &&
                    reportSpecification.getTimeWindows().size() == 2;
        });
    }

    @Test
    public void shouldCreateHiccupReport() throws Exception
    {
        final String reportName = token + "_hiccup";
        reportServer.createReport(reportName, "hiccup.*",
                asList(new TimeWindow(TimeUnit.SECONDS, 10), new TimeWindow(TimeUnit.MINUTES, 1), new TimeWindow(TimeUnit.HOURS, 1)),
                asList(new Threshold(Metric.TWO_NINES.name(), 2500), new Threshold(Metric.THREE_NINES.name(), 10500),
                        new Threshold(Metric.FOUR_NINES.name(), 25000), new Threshold(Metric.MAX.name(), 54500)));

        waitFor(() -> reportServer.getReportNames().contains(reportName));
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
