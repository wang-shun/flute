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

public final class FlywayProperties
{
    private final DatabaseConfig databaseConfig;

    public FlywayProperties(final DatabaseConfig databaseConfig)
    {
        this.databaseConfig = databaseConfig;
    }

    public Properties getProperties()
    {
        final Properties properties = new Properties();
        properties.setProperty("flyway.url", databaseConfig.getUrl());
        properties.setProperty("flyway.user", databaseConfig.getUsername());
        properties.setProperty("flyway.password", databaseConfig.getPassword());

        return properties;
    }
}
