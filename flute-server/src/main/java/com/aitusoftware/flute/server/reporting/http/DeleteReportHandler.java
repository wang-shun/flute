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
package com.aitusoftware.flute.server.reporting.http;

import com.aitusoftware.flute.server.reporting.dao.ReportDao;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

final class DeleteReportHandler extends AbstractHandler
{
    private final ReportDao reportDao;

    DeleteReportHandler(final ReportDao reportDao)
    {
        this.reportDao = reportDao;
    }

    @Override
    public void handle(final String target, final Request baseRequest,
                       final HttpServletRequest request,
                       final HttpServletResponse response) throws IOException, ServletException
    {
        try
        {
            final String[] components = request.getPathInfo().split("/");
            final String reportName = components[1];

            reportDao.deleteReport(reportName);
            response.getWriter().append("{\"message\": \"ok\"}");
            response.setStatus(HttpServletResponse.SC_OK);
        }
        catch(RuntimeException e)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        baseRequest.setHandled(true);
    }
}
