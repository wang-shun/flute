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

import com.aitusoftware.flute.server.cache.HistogramSource;
import com.aitusoftware.flute.server.dao.jdbc.HistogramRetrievalDao;
import com.aitusoftware.flute.server.dao.jdbc.MetricIdentifierDao;
import com.aitusoftware.flute.server.query.Query;
import org.HdrHistogram.Histogram;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;

final class SlaReportHandler extends DefaultHandler
{
    private final HistogramRetrievalDao histogramRetrievalDao;
    private final MetricIdentifierDao metricIdentifierDao;
    private final boolean isAggregator;
    private final HistogramSource histogramSource;

    SlaReportHandler(final HistogramRetrievalDao histogramRetrievalDao,
                     final MetricIdentifierDao metricIdentifierDao,
                     final boolean isAggregator, final HistogramSource histogramSource)
    {
        this.histogramRetrievalDao = histogramRetrievalDao;
        this.metricIdentifierDao = metricIdentifierDao;
        this.isAggregator = isAggregator;
        this.histogramSource = histogramSource;
    }

    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
                       final HttpServletResponse response) throws IOException, ServletException
    {
        response.setContentType("application/javascript; charset=utf-8");
        final Optional<Query> parseResult = QueryParser.parseQuery(request.getPathInfo());

        if(!parseResult.isPresent())
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            baseRequest.setHandled(true);
            return;
        }
        final Query query = parseResult.get();

        final PrintWriter writer = response.getWriter();

        final Set<String> metricIdentifiers;
        if(isAggregator)
        {
            metricIdentifiers = metricIdentifierDao.getIdentifiersMatching(query.getMetricKey());
        }
        else
        {
            metricIdentifiers = singleton(query.getMetricKey());
        }

        writer.append("[");
        if(!query.hasExplicitEndTime())
        {
            loadFromCache(query, writer, metricIdentifiers, query.getMetricKey());
        }
        else
        {
            histogramRetrievalDao.selectCompositeHistogramSummary(metricIdentifiers, query, new StandardPercentilesHistogramSummaryHandler(writer));
        }
        writer.append("]");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        writer.flush();
    }

    private void loadFromCache(
            final Query query, final PrintWriter writer,
            final Set<String> metricIdentifiers,
            final String metricKey)
    {
        final Histogram histogram = histogramSource.getCurrentHistogram(metricIdentifiers,
                query.getDuration(), query.getDurationUnit(), metricKey);
        new StandardPercentilesHistogramSummaryHandler(writer).onRecord(query.getMetricKey(), histogram.getStartTimeStamp(), histogram);
    }
}