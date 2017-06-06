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
package com.aitusoftware.flute.config;

import java.util.Properties;

public final class RequiredProperties
{
    private RequiredProperties() {}

    public static String requiredProperty(final String key, final Properties properties)
    {
        final String suppliedValue = properties.getProperty(key);
        final String resolvedValue = System.getProperty(key, suppliedValue);

        if(resolvedValue == null)
        {
            throw new IllegalStateException(String.format("Required key %s not found in properties, or overridden in system properties", key));
        }

        return resolvedValue;
    }
}
