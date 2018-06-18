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

import com.aitusoftware.flute.archive.ManagedDatabase;
import org.HdrHistogram.Histogram;
import org.h2.Driver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class DataMaintenanceDaoTest
{
    private static final long CLOCK_START_VALUE = System.currentTimeMillis();
    private static final long MAX_DATA_AGE_MILLIS = TimeUnit.DAYS.toMillis(30);

    private final AtomicLong clock = new AtomicLong(CLOCK_START_VALUE);

    private File databaseFile;
    private String databaseUrl;
    private Connection connection;
    private DataMaintenanceDao dataMaintenanceDao;

    @Before
    public void before() throws Exception
    {
        databaseFile = File.createTempFile("flute-db", ".db");
        final Properties properties = new Properties();
        databaseUrl = "jdbc:h2:file:" + databaseFile.getAbsolutePath();
        properties.setProperty("flyway.url", databaseUrl);
        properties.setProperty("flyway.user", "SA");
        properties.setProperty("flyway.password", "");

        new ManagedDatabase().init(properties);

        DriverManager.registerDriver(new Driver());
        connection = DriverManager.getConnection(databaseUrl, "SA", "");
        HistogramInsertDao histogramInsertDao = new HistogramInsertDao(connection, e -> {
            throw new AssertionError("Failed to insert data", e);
        }, 4096);

        dataMaintenanceDao = new DataMaintenanceDao(connection, e -> {
            throw new AssertionError("Query failed", e);
        }, MAX_DATA_AGE_MILLIS, clock::get);
        InetSocketAddress address = (InetSocketAddress) DatagramChannel.open().bind(null).getLocalAddress();
        Histogram histogram = new Histogram(1000, 2);
        for(int i = 0; i < 1000; i++)
        {
            histogram.recordValue(i);
        }

        for (int i = 0; i < 10; i++)
        {
            histogramInsertDao.histogramReceived(address, "method.one",
                    clock.get(), clock.addAndGet(TimeUnit.MINUTES.toMillis(5L)), histogram);
        }
    }

    @After
    public void after() throws Exception
    {
        connection.close();
        databaseFile.delete();
    }

    @Test
    public void shouldPersistHistograms() throws Exception
    {
        expectRecordCount(10);
        dataMaintenanceDao.deleteExpiredEntries();
        expectRecordCount(10);

        clock.set(CLOCK_START_VALUE + MAX_DATA_AGE_MILLIS + 1L);

        dataMaintenanceDao.deleteExpiredEntries();

        expectRecordCount(9);

        clock.set(CLOCK_START_VALUE + MAX_DATA_AGE_MILLIS + TimeUnit.MINUTES.toMillis(5L) + 1L);
        dataMaintenanceDao.deleteExpiredEntries();

        expectRecordCount(8);
    }

    private void expectRecordCount(final int expectedCount) throws SQLException
    {
        int count = 0;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM histogram_data"))
        {
            while (rs.next())
            {
                count++;
            }
        }
        finally
        {
            Assert.assertThat(count, is(equalTo(expectedCount)));
        }
    }
}