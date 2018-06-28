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
package com.aitusoftware.flute.server.dao.jdbc;

import com.aitusoftware.flute.config.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class MetricIdentifierDao
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricIdentifierDao.class);
    private final ConnectionFactory connectionFactory;

    public MetricIdentifierDao(final ConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    public Set<String> getIdentifiersMatching(final String specification)
    {
        final Pattern pattern = Pattern.compile(specification);
        final Set<String> matchingIdentifiers = new HashSet<>();
        try(
                final Connection connection = connectionFactory.getConnection();
                final PreparedStatement statement = connection.prepareStatement("SELECT DISTINCT identifier FROM histogram_data");
                final ResultSet resultSet = statement.executeQuery())
        {
            while(resultSet.next())
            {
                final String identifier = resultSet.getString("identifier");
                if(specification.equals(identifier) || pattern.matcher(identifier).matches())
                {
                    matchingIdentifiers.add(identifier);
                }
            }
        }
        catch(final SQLException e)
        {
            LOGGER.warn("Query failed", e);
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
        return matchingIdentifiers;
    }
}
