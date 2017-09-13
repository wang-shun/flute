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
package com.aitusoftware.flute.agent.annotation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MetricNameSubstitution
{
    INSTANCE;

    private static final Pattern PROPERTY_KEY_PATTERN =
            Pattern.compile("\\$\\{[^\\}]+\\}");

    public String getMetricName(final String methodName, final FluteMetric annotation)
    {
        if (FluteMetric.USE_METHOD_NAME.equals(annotation.metricName()))
        {
            return methodName;
        }

        final Matcher matcher = PROPERTY_KEY_PATTERN.matcher(annotation.metricName());
        String metricName = annotation.metricName();
        while (matcher.find())
        {
            final String pattern = matcher.group();

            final String propertyName = pattern.substring(2, pattern.length() - 1);
            metricName = metricName.replace("${" + propertyName + "}",
                    System.getProperty(propertyName));
        }

        return metricName;
    }
}
