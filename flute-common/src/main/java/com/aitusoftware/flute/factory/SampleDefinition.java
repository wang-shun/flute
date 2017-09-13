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
package com.aitusoftware.flute.factory;

@Deprecated
public final class SampleDefinition
{
    private final Type sample;
    private final long spec;

    enum Type
    {
        SAMPLE,
        TIME
    }

    private SampleDefinition(final Type sample, final long spec)
    {
        this.sample = sample;
        this.spec = spec;
    }

    Type getSample()
    {
        return sample;
    }

    long getSpec()
    {
        return spec;
    }

    public static SampleDefinition invocations(final long invocationCount)
    {
        return new SampleDefinition(Type.SAMPLE, invocationCount);
    }
}
