package com.aitusoftware.flute.acceptance.test;

import com.aitusoftware.flute.acceptance.framework.MetricServer;
import com.aitusoftware.flute.acceptance.framework.ReportServer;
import com.aitusoftware.flute.acceptance.framework.Threshold;
import com.aitusoftware.flute.acceptance.framework.TimeWindow;
import com.aitusoftware.flute.acceptance.framework.Waiter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Ignore
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
