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
package com.aitusoftware.flute.server.http;

import com.aitusoftware.flute.server.dao.jdbc.MetricIdentifierDao;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.regex.Pattern;

final class MetricIdentifierSearchHandler extends DefaultHandler
{
    private final MetricIdentifierDao metricIdentifierDao;

    MetricIdentifierSearchHandler(final MetricIdentifierDao metricIdentifierDao)
    {
        this.metricIdentifierDao = metricIdentifierDao;
    }

    @Override
    public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, ServletException
    {
        response.setContentType("application/javascript; charset=utf-8");

        final Pattern querySpecification;
        try
        {
            querySpecification = getQuerySpecification(request);
        }
        catch(RuntimeException e)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            baseRequest.setHandled(true);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        final Set<String> matching = metricIdentifierDao.getIdentifiersMatching(querySpecification.pattern());
        final PrintWriter writer = response.getWriter();
        writer.append("[");
        boolean first = true;
        for (String identifier : matching)
        {
            if(!first)
            {
                writer.append(',');
            }
            first = false;
            writer.append('"').append(identifier).append('"');
        }
        writer.append("]");
        writer.flush();
    }

    private Pattern getQuerySpecification(final HttpServletRequest request)
    {
        final String[] components = request.getPathInfo().split("/");
        return Pattern.compile(components[1], Pattern.CASE_INSENSITIVE);
    }
}
