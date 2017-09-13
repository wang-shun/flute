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

import com.aitusoftware.flute.server.reporting.domain.ReportSpecification;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReportServer
{
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 15002;
    private static final String SERVER_ADDRESS = "http://" + SERVER_HOST + ":" + SERVER_PORT;
    private static final String CREATE_REPORT_URI = "/flute/report/create";
    private static final String AMEND_REPORT_URI = "/flute/report/amend";
    private static final String DELETE_REPORT_URI = "/flute/report/delete/%s";
    private static final String GET_REPORT_URI = "/flute/report/get/%s";
    private static final String LIST_REPORTS_URI = "/flute/report/list";

    public ReportServer()
    {
        SystemReadiness.waitForServer(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
    }

    public ApiCallResult createReport(final String reportName, final String metricSelectorPattern,
                             final List<TimeWindow> timeWindows, final List<Threshold> thresholds)
    {
        return HttpOps.post(SERVER_ADDRESS + CREATE_REPORT_URI,
                createReportSpecificationPostData(reportName, metricSelectorPattern, timeWindows, thresholds));
    }

    public ApiCallResult deleteReport(final String reportName)
    {
        return HttpOps.post(SERVER_ADDRESS + String.format(DELETE_REPORT_URI, reportName), new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public List<String> getReportNames()
    {
        return HttpOps.get(SERVER_ADDRESS + LIST_REPORTS_URI, List.class);
    }

    public ReportSpecification getReport(final String reportName)
    {
        return HttpOps.get(SERVER_ADDRESS + String.format(GET_REPORT_URI, reportName), ReportSpecification.class);
    }

    public ApiCallResult amendReport(final String reportName, final String metricSelectorPattern,
                            final List<TimeWindow> timeWindows, final List<Threshold> thresholds)
    {
        return HttpOps.post(SERVER_ADDRESS + AMEND_REPORT_URI,
                createReportSpecificationPostData(reportName, metricSelectorPattern, timeWindows, thresholds));
    }

    private static Map<String, Object> createReportSpecificationPostData(
            final String reportName, final String metricSelectorPattern,
            final List<TimeWindow> timeWindows, final List<Threshold> thresholds)
    {
        final Map<String, Object> postData = new HashMap<>();
        postData.put("reportName", reportName);
        postData.put("selectorPattern", metricSelectorPattern);
        postData.put("timeWindows", timeWindows);
        postData.put("thresholds", thresholds);
        return postData;
    }
}
