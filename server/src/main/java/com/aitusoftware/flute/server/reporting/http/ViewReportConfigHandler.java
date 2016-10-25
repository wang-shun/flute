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
package com.aitusoftware.flute.server.reporting.http;

import com.aitusoftware.flute.server.dao.MetricIdentifierDao;
import com.aitusoftware.flute.server.http.ReportingConfigSerialiser;
import com.aitusoftware.flute.server.reporting.dao.ReportDao;
import com.aitusoftware.flute.server.reporting.domain.ReportSpecification;
import com.aitusoftware.flute.server.reporting.domain.Threshold;
import com.aitusoftware.flute.server.reporting.domain.TimeWindow;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class ViewReportConfigHandler extends AbstractHandler
{
    private final ReportDao reportDao;
    private final MetricIdentifierDao metricIdentifierDao;

    public ViewReportConfigHandler(final ReportDao reportDao, final MetricIdentifierDao metricIdentifierDao)
    {
        this.reportDao = reportDao;
        this.metricIdentifierDao = metricIdentifierDao;
    }

    @Override
    public void handle(final String target, final Request baseRequest,
                       final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException
    {
        final String[] components = request.getPathInfo().split("/");
        if(components.length < 2)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        final String reportName = components[1];
        final ReportSpecification reportSpecification = reportDao.getReportSpecification(reportName);

        final Map<String, List<Threshold>> metricThresholds =
                Collections.singletonMap(reportSpecification.getSelectorPattern(), reportSpecification.getThresholds());
        response.setContentType("application/javascript; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        // TODO time unit for report thresholds should be specified
//        final TimeUnit unit = reportSpecification.getUnit();
        final TimeUnit unit = TimeUnit.MICROSECONDS;
        final List<TimeWindow> reportWindows = reportSpecification.getTimeWindows();


        ReportingConfigSerialiser.serialiseReportingConfig(response, metricThresholds, unit, reportWindows, metricIdentifierDao);
    }
}
