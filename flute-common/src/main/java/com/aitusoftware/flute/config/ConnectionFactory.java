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
package com.aitusoftware.flute.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionFactory.class);

    private final DatabaseConfig databaseConfig;
    private boolean driverRegistered;

    public ConnectionFactory(final DatabaseConfig databaseConfig)
    {
        this.databaseConfig = databaseConfig;
        try
        {
            DriverManager.registerDriver((Driver) Class.forName(databaseConfig.getDriverClassName()).newInstance());
            driverRegistered = true;
        }
        catch(final Exception e)
        {
            LOGGER.error("Failed to register driver {}", databaseConfig.getDriverClassName());
        }
    }

    public Connection getConnection()
    {
        if(!driverRegistered)
        {
            throw new IllegalStateException("Failed to register driver, unable to get connection");
        }
        try
        {
            return DriverManager.getConnection(
                    databaseConfig.getUrl(),
                    databaseConfig.getUsername(),
                    databaseConfig.getPassword());
        }
        catch (final SQLException e)
        {
            throw new RuntimeException("Failed to get SQL connection", e);
        }
    }
}
