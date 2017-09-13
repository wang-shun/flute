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
package com.aitusoftware.flute.archive;

import com.aitusoftware.flute.archive.jdbc.HistogramInsertDao;
import org.HdrHistogram.Histogram;
import org.h2.Driver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class HistogramInsertDaoTest
{
    // one mega-byte should be enough for anyone, etc.
    private static final int MAX_ENCODED_HISTOGRAM_SIZE = 1_048_576;

    private File databaseFile;
    private String databaseUrl;
    private Connection connection;
    private HistogramInsertDao histogramInsertDao;

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
        histogramInsertDao = new HistogramInsertDao(connection, e -> {
            throw new AssertionError("Failed to insert data", e);
        }, MAX_ENCODED_HISTOGRAM_SIZE);
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
        final InetSocketAddress address = (InetSocketAddress) DatagramChannel.open().bind(null).getLocalAddress();
        final Histogram histogram = new Histogram(1000, 2);
        for(int i = 0; i < 1000; i++)
        {
            histogram.recordValue(i);
        }
        histogramInsertDao.histogramReceived(address, "method.one",
                1234567890123L, 1234567990123L, histogram);
    }
}