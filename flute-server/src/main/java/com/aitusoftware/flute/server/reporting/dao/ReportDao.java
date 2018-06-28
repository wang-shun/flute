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
package com.aitusoftware.flute.server.reporting.dao;

import com.aitusoftware.flute.config.ConnectionFactory;
import com.aitusoftware.flute.server.reporting.domain.ReportSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ReportDao
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportDao.class);
    private static final String INSERT_SQL = "INSERT INTO aggregate_report " +
            "(name, selectorPattern, timeWindows, thresholds) VALUES (?,?,?,?)";
    private static final String AMEND_SQL = "UPDATE aggregate_report " +
            "SET selectorPattern = ?, timeWindows = ?, thresholds = ? WHERE name = ?";
    private static final String DELETE_SQL = "DELETE FROM aggregate_report " +
            "WHERE name = ?";
    private static final String SELECT_BY_NAME_SQL = "SELECT * FROM aggregate_report WHERE name = ?";

    private final ConnectionFactory connectionFactory;

    public ReportDao(final ConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    public void createReport(final ReportSpecification reportSpecification)
    {
        final String metricPattern = Pattern.compile(reportSpecification.getSelectorPattern()).pattern();
        try(
                final Connection connection = connectionFactory.getConnection();
                final PreparedStatement statement = connection.prepareStatement(INSERT_SQL))
        {
            statement.setString(1, reportSpecification.getReportName());
            statement.setString(2, metricPattern);
            statement.setString(3, ReportDataMarshalling.encodeTimeWindows(reportSpecification.getTimeWindows()));
            statement.setString(4, ReportDataMarshalling.encodeThresholds(reportSpecification.getThresholds()));

            if(statement.executeUpdate() != 1)
            {
                throw new RuntimeException("Failed to insert report record");
            }
        }
        catch(final SQLException e)
        {
            LOGGER.warn("Query failed", e);
            throw new RuntimeException("Query failed", e);
        }
    }

    public void amendReport(final ReportSpecification reportSpecification)
    {
        final String metricPattern = Pattern.compile(reportSpecification.getSelectorPattern()).pattern();
        try(
                final Connection connection = connectionFactory.getConnection();
                final PreparedStatement statement = connection.prepareStatement(AMEND_SQL))
        {
            statement.setString(1, metricPattern);
            statement.setString(2, ReportDataMarshalling.encodeTimeWindows(reportSpecification.getTimeWindows()));
            statement.setString(3, ReportDataMarshalling.encodeThresholds(reportSpecification.getThresholds()));
            statement.setString(4, reportSpecification.getReportName());

            if(statement.executeUpdate() != 1)
            {
                throw new RuntimeException("Failed to insert report record");
            }
        }
        catch(final SQLException e)
        {
            LOGGER.warn("Query failed", e);
            throw new RuntimeException("Query failed", e);
        }
    }

    public void deleteReport(final String reportName)
    {
        try(
                final Connection connection = connectionFactory.getConnection();
                final PreparedStatement statement = connection.prepareStatement(DELETE_SQL))
        {
            statement.setString(1, reportName);

            if(statement.executeUpdate() != 1)
            {
                throw new RuntimeException("Failed to insert report record");
            }
        }
        catch(final SQLException e)
        {
            LOGGER.warn("Query failed", e);
            throw new RuntimeException("Query failed", e);
        }
    }

    public List<String> getReportNames()
    {
        final List<String> reportNames = new ArrayList<>();
        try(
                final Connection connection = connectionFactory.getConnection();
                final PreparedStatement statement = connection.prepareStatement("SELECT name FROM aggregate_report");
                final ResultSet resultSet = statement.executeQuery())
        {
            while(resultSet.next())
            {
                reportNames.add(resultSet.getString("name"));
            }
        }
        catch(final SQLException e)
        {
            LOGGER.warn("Query failed", e);
            throw new RuntimeException("Query failed", e);
        }
        return reportNames;
    }

    public ReportSpecification getReportSpecification(final String reportName)
    {
        try(
                final Connection connection = connectionFactory.getConnection();
                final PreparedStatement statement = getSelectByNameStatement(connection, reportName);
                final ResultSet resultSet = statement.executeQuery())
        {
            if(resultSet.next())
            {
                return new ReportSpecification(
                        resultSet.getString("name"),
                        resultSet.getString("selectorPattern"),
                        ReportDataMarshalling.parseTimeWindows(resultSet.getString("timeWindows")),
                        ReportDataMarshalling.parseThresholds(resultSet.getString("thresholds")));
            }
            else
            {
                throw new IllegalArgumentException("Report " + reportName + " not found");
            }
        }
        catch(final SQLException e)
        {
            LOGGER.warn("Query failed", e);
            throw new RuntimeException("Query failed", e);
        }
    }

    private static PreparedStatement getSelectByNameStatement(final Connection connection, final String reportName) throws SQLException
    {
        final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_NAME_SQL);
        preparedStatement.setString(1, reportName);
        return preparedStatement;
    }
}