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
package com.aitusoftware.flute.server.http;

import com.aitusoftware.flute.server.dao.jdbc.MetricIdentifierDao;
import com.aitusoftware.flute.server.reporting.domain.Threshold;
import com.aitusoftware.flute.server.reporting.domain.TimeWindow;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class ReportingConfigSerialiser
{
    public static void serialiseReportingConfig(
            final HttpServletResponse response,
            final Map<String, List<Threshold>> metricThresholds,
            final TimeUnit unit,
            final List<TimeWindow> reportWindows,
            final MetricIdentifierDao metricIdentifierDao) throws IOException
    {
        final PrintWriter writer = response.getWriter();
        writer.append("[{");
        appendValue("unit", unit.name(), true, writer);
        writer.append(',');

        writer.append("\"reportWindows\": [");

        boolean firstReportWindow = true;
        for(TimeWindow reportWindow : reportWindows)
        {
            if(!firstReportWindow)
            {
                writer.append(',');
            }
            firstReportWindow = false;
            writer.append("{");
            appendValue("unit", reportWindow.getUnit().name(), true, writer);
            writer.append(",");
            appendValue("duration", Long.toString(reportWindow.getDuration()), true, writer);
            writer.append("}");
        }

        writer.append("],");

        writer.append("\"metricThresholds\": [");
        boolean firstEntry = true;
        final Set<String> matchingMetrics = new HashSet<>();
        for (Map.Entry<String, List<Threshold>> thresholdSet : metricThresholds.entrySet())
        {
            final Set<String> matchingIdentifiers = metricIdentifierDao.getIdentifiersMatching(thresholdSet.getKey());
            for(final String existingIdentifier : matchingIdentifiers)
            {
                if(!matchingMetrics.contains(existingIdentifier))
                {
                    matchingMetrics.add(existingIdentifier);

                    if (!firstEntry)
                    {
                        writer.append(",");
                    }
                    firstEntry = false;

                    writer.append("{");
                    appendValue("metricKey", existingIdentifier, true, writer);
                    writer.append(", \"metrics\":[");
                    boolean firstMetric = true;
                    for (Threshold threshold : thresholdSet.getValue())
                    {
                        if (!firstMetric)
                        {
                            writer.append(",");
                        }
                        firstMetric = false;
                        writer.append("{");
                        appendValue("name", threshold.getPercentile(), true, writer);
                        writer.append(",");
                        appendValue("value", Long.toString(threshold.getValue()), false, writer);
                        writer.append("}");
                    }

                    writer.append("]}");
                }
            }
        }
        writer.append("]}]");
        writer.flush();
    }

    public static void appendValue(final CharSequence key,
                                   final CharSequence value,
                                   final boolean quoteValue,
                                   final PrintWriter writer)
    {
        writer.append("\"").append(key).append("\": ");
        if(quoteValue)
        {
            writer.append("\"");
        }
        writer.append(value);
        if(quoteValue)
        {
            writer.append("\"");
        }
    }
}
