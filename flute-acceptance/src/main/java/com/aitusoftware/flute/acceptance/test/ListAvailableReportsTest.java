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
import com.aitusoftware.flute.acceptance.framework.ReportServer;
import com.aitusoftware.flute.acceptance.framework.Threshold;
import com.aitusoftware.flute.acceptance.framework.TimeWindow;
import com.aitusoftware.flute.acceptance.framework.Waiter;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public final class ListAvailableReportsTest
{
    private static final String REPORT_NAME = "test-report";
    private final MetricServer metricServer = new MetricServer();
    private final ReportServer reportServer = new ReportServer();

    @Before
    public void createReport()
    {
        reportServer.createReport(REPORT_NAME, ".*",
                Collections.singletonList(new TimeWindow(TimeUnit.MINUTES, 5)),
                Collections.singletonList(new Threshold("99.99", 5000)));
    }

    @Test
    public void metricsServerShouldListAvailableReports()
    {
        Waiter.waitFor(() -> metricServer.waitForReport().contains(REPORT_NAME));
    }
}
