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

import org.junit.Test;

import static com.aitusoftware.flute.agent.annotation.MetricNameSubstitution.INSTANCE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MetricNameSubstitutionTest
{
    @org.junit.Test
    public void shouldReplaceDotDelimitedProperty() throws Exception
    {
        assertThat(
                INSTANCE.getMetricName("doSomethingExpensive",
                        getMethodAnnotation("doSomethingExpensive")),
                is(String.format("system.%s.operation.somethingExpensive",
                        System.getProperty("user.name"))));
    }

    @org.junit.Test
    public void shouldReplaceCustomProperty() throws Exception
    {
        System.setProperty("customValue", "some-value");
        assertThat(
                INSTANCE.getMetricName("doSomethingElse",
                        getMethodAnnotation("doSomethingElse")),
                is("system.some-value.operation.somethingElse"));
    }

    @org.junit.Test
    public void shouldDefaultToMethodName() throws Exception
    {
        assertThat(
                INSTANCE.getMetricName("defaultName",
                        getMethodAnnotation("defaultName")),
                is("defaultName"));
    }

    @org.junit.Test
    public void shouldReturnDefinedValueIfNoPropertiesArePresent() throws Exception
    {
        assertThat(
                INSTANCE.getMetricName("noReplace",
                        getMethodAnnotation("noReplace")),
                is("selfContained"));
    }

    private FluteMetric getMethodAnnotation(final String methodName)
    {
        try
        {
            return Test.class.getDeclaredMethod(methodName).getAnnotation(FluteMetric.class);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
    }

    interface Test
    {
        @FluteMetric(metricName = "system.${user.name}.operation.somethingExpensive")
        void doSomethingExpensive();

        @FluteMetric(metricName = "system.${customValue}.operation.somethingElse")
        void doSomethingElse();

        @FluteMetric
        void defaultName();

        @FluteMetric(metricName = "selfContained")
        void noReplace();
    }
}