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
package com.aitusoftware.flute.server.reporting.domain;

import java.util.List;

public final class ReportSpecification
{
    private String reportName;
    private String selectorPattern;
    private List<TimeWindow> timeWindows;
    private List<Threshold> thresholds;

    public ReportSpecification(
            final String reportName,
            final String selectorPattern,
            final List<TimeWindow> timeWindows,
            final List<Threshold> thresholds)
    {
        this.reportName = reportName;
        this.selectorPattern = selectorPattern;
        this.timeWindows = timeWindows;
        this.thresholds = thresholds;
    }

    @Override
    public String toString()
    {
        return "ReportSpecification{" +
                "reportName='" + reportName + '\'' +
                ", selectorPattern='" + selectorPattern + '\'' +
                ", timeWindows=" + timeWindows +
                ", thresholds=" + thresholds +
                '}';
    }

    public String getReportName()
    {
        return reportName;
    }

    public String getSelectorPattern()
    {
        return selectorPattern;
    }

    public List<TimeWindow> getTimeWindows()
    {
        return timeWindows;
    }

    public List<Threshold> getThresholds()
    {
        return thresholds;
    }
}
