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

import com.aitusoftware.flute.server.dao.jdbc.HistogramRetrievalDao;
import com.aitusoftware.flute.server.dao.jdbc.MetricIdentifierDao;
import com.aitusoftware.flute.server.query.Query;
import com.aitusoftware.flute.server.shared.ReportingConfigSerialiser;
import org.HdrHistogram.Histogram;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;

final class TimeSeriesChartHandler extends DefaultHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesChartHandler.class);

    private final HistogramRetrievalDao histogramRetrievalDao;
    private final MetricIdentifierDao metricIdentifierDao;
    private final boolean isAggregator;

    TimeSeriesChartHandler(final HistogramRetrievalDao histogramRetrievalDao,
                                  final MetricIdentifierDao metricIdentifierDao,
                                  final boolean isAggregator)
    {
        this.histogramRetrievalDao = histogramRetrievalDao;
        this.metricIdentifierDao = metricIdentifierDao;
        this.isAggregator = isAggregator;
    }

    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException
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

        if(LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Query: {}", query);
        }

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

        final List<Histogram> histograms = histogramRetrievalDao.query(metricIdentifiers, query.getMetricKey(),
                query.getStartMillis(), query.getEndMillis());

        if(LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Retrieved {} histograms", histograms.size());
        }

        writeHistograms(histograms, response.getWriter());

        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        writer.flush();
    }

    private void writeHistograms(final List<Histogram> histograms, final PrintWriter writer)
    {
        writer.append("[");

        for (int i = 0; i < histograms.size(); i++)
        {
            final Histogram histogram = histograms.get(i);
            if (i != 0)
            {
                writer.append(',');
            }
            writer.append("{");
            ReportingConfigSerialiser.appendValue("start", Long.toString(histogram.getStartTimeStamp()),
                    false, writer);
            writer.append(',');
            ReportingConfigSerialiser.appendValue("end", Long.toString(histogram.getEndTimeStamp()),
                    false, writer);
            writer.append(',');
            ReportingConfigSerialiser.appendValue("max", Long.toString(histogram.getMaxValue()),
                    false, writer);
            writer.append(',');
            ReportingConfigSerialiser.appendValue("min", Long.toString(histogram.getMinValue()),
                    false, writer);
            writer.append(',');
            ReportingConfigSerialiser.appendValue("mean", String.format("%.2f", histogram.getMean()),
                    false, writer);
            writer.append(',');
            ReportingConfigSerialiser.appendValue("count", Long.toString(histogram.getTotalCount()),
                    false, writer);
            writer.append("}");
        }
        writer.append("]");
    }
}