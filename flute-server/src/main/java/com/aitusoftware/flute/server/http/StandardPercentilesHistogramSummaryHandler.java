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

import com.aitusoftware.flute.server.query.FullHistogramHandler;
import org.HdrHistogram.Histogram;

import java.io.PrintWriter;

class StandardPercentilesHistogramSummaryHandler implements FullHistogramHandler
{
    private enum Format
    {
        CSV,
        JSON;
    }

    private final Format format;
    private final PrintWriter writer;
    private boolean firstRecord;

    static StandardPercentilesHistogramSummaryHandler csv(final PrintWriter writer)
    {
        return new StandardPercentilesHistogramSummaryHandler(writer, Format.CSV);
    }

    static StandardPercentilesHistogramSummaryHandler json(final PrintWriter writer)
    {
        return new StandardPercentilesHistogramSummaryHandler(writer, Format.JSON);
    }

    private StandardPercentilesHistogramSummaryHandler(final PrintWriter writer, final Format format)
    {
        this.writer = writer;
        this.format = format;
        firstRecord = true;
    }

    StandardPercentilesHistogramSummaryHandler(final PrintWriter writer)
    {
        this(writer, Format.JSON);
    }

    @Override
    public void onRecord(final String identifier, final long timestamp, final Histogram histogram)
    {

        if (firstRecord)
        {
            firstRecord = false;
        }
        else
        {
            switch (format)
            {
                case CSV:
                    writer.append("\n");
                    break;
                case JSON:
                    writer.append(",");
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        switch (format)
        {
            case JSON:
                writer.append("[");
                break;
            case CSV:
                break;
            default:
                throw new IllegalArgumentException();
        }
        writer.append(Long.toString(timestamp)).append(",");
        writer.append(Long.toString(histogram.getMinValue())).append(',');
        writer.append(Double.toString(histogram.getMean())).append(',');
        writer.append(Long.toString(histogram.getValueAtPercentile(50d))).append(',');
        writer.append(Long.toString(histogram.getValueAtPercentile(90d))).append(',');
        writer.append(Long.toString(histogram.getValueAtPercentile(99d))).append(',');
        writer.append(Long.toString(histogram.getValueAtPercentile(99.9d))).append(',');
        writer.append(Long.toString(histogram.getValueAtPercentile(99.99d))).append(',');
        writer.append(Long.toString(histogram.getValueAtPercentile(99.999d))).append(',');
        writer.append(Long.toString(histogram.getMaxValue())).append(',');
        writer.append(Long.toString(histogram.getTotalCount()));
        switch (format)
        {
            case JSON:
                writer.append("]");
                break;
            case CSV:
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}