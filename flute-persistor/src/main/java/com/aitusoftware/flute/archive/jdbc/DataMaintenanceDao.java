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
package com.aitusoftware.flute.archive.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class DataMaintenanceDao
{
    private final Connection connection;
    private final Consumer<SQLException> exceptionConsumer;
    private final long maxDataAgeMillis;
    private final LongSupplier clock;

    public DataMaintenanceDao(final Connection connection, final Consumer<SQLException> exceptionConsumer,
                              final long maxDataAgeMillis)
    {
        this(connection, exceptionConsumer, maxDataAgeMillis, System::currentTimeMillis);
    }

    DataMaintenanceDao(final Connection connection, final Consumer<SQLException> exceptionConsumer,
                       final long maxDataAgeMillis, final LongSupplier clock)
    {
        this.connection = connection;
        this.exceptionConsumer = exceptionConsumer;
        this.maxDataAgeMillis = maxDataAgeMillis;
        this.clock = clock;
    }

    public void deleteExpiredEntries()
    {
        try (final PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM histogram_data WHERE start_timestamp < ?"))
        {
            statement.setLong(1, clock.getAsLong() - maxDataAgeMillis);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            exceptionConsumer.accept(e);
        }
    }
}