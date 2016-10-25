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

import com.aitusoftware.flute.server.reporting.dao.ReportDao;
import com.aitusoftware.flute.server.reporting.domain.ReportSpecification;
import com.google.gson.Gson;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ModifyReportHandler extends AbstractHandler
{
    private enum Mode
    {
        CREATE,
        AMEND
    }

    private final ReportDao reportDao;
    private final Mode mode;

    private ModifyReportHandler(final ReportDao reportDao, final Mode mode)
    {
        this.reportDao = reportDao;
        this.mode = mode;
    }

    @Override
    public void handle(final String target, final Request baseRequest,
                       final HttpServletRequest request,
                       final HttpServletResponse response) throws IOException, ServletException
    {
        try
        {
            final ReportSpecification reportSpecification =
                    new Gson().fromJson(request.getReader(), ReportSpecification.class);

            switch (mode)
            {
                case CREATE:
                    reportDao.createReport(reportSpecification);
                    break;
                case AMEND:
                    reportDao.amendReport(reportSpecification);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            response.getWriter().append("{\"message\": \"ok\"}");
            response.setStatus(HttpServletResponse.SC_OK);
        }
        catch(RuntimeException e)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        baseRequest.setHandled(true);
    }

    public static ModifyReportHandler createMode(final ReportDao reportDao)
    {
        return new ModifyReportHandler(reportDao, Mode.CREATE);
    }

    public static ModifyReportHandler amendMode(final ReportDao reportDao)
    {
        return new ModifyReportHandler(reportDao, Mode.AMEND);
    }
}
