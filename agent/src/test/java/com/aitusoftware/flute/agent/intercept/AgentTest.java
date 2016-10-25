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
package com.aitusoftware.flute.agent.intercept;

import com.aitusoftware.flute.agent.annotation.FluteMetric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Ignore(value = "kills gradle :(")
public final class AgentTest
{
    private final Properties properties = new Properties();

    @Before
    public void before() throws Exception
    {
        properties.setProperty("flute.client.reporting.tcp.address", "localhost:51000");
        properties.setProperty("flute.histogram.maxValue", "100000");
        properties.setProperty("flute.histogram.significantDigits", "5");
        properties.setProperty("flute.client.publication.interval", "1");
        properties.setProperty("flute.client.publication.unit", TimeUnit.SECONDS.name());
    }

    @Test
    public void shouldName() throws Exception
    {
        Agent.load(properties);

        new Foo().bar(42);
        new Bar().foo(17);
        new Bar().foo2(37);
        new Bar().foo2(42);
    }

    private static final class Foo
    {
        @FluteMetric
        private String bar(final int number)
        {
            return Integer.toHexString(number);
        }
    }

    private static final class Bar
    {
        @FluteMetric
        private String foo(final int number)
        {
            return Integer.toHexString(number);
        }
        @FluteMetric
        private long foo2(final long number)
        {
            return number ^ 238764L << 34;
        }
    }
}