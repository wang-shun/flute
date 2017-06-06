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
package com.aitusoftware.flute.server.dao.jdbc;

import com.aitusoftware.flute.server.dao.HistogramIterator;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

final class ResultSetHistogramIterator implements HistogramIterator
{
    private final ResultSet resultSet;

    ResultSetHistogramIterator(final ResultSet resultSet)
    {
        this.resultSet = resultSet;
    }

    @Override
    public InputStream getHistogramData() throws IOException
    {
        try
        {
            return resultSet.getBinaryStream("raw_data");
        }
        catch (SQLException e)
        {
            throw new IOException(e);
        }
    }

    @Override
    public boolean next() throws IOException
    {
        try
        {
            return resultSet.next();
        }
        catch (SQLException e)
        {
            throw new IOException(e);
        }
    }

    @Override
    public long getStartTimestamp() throws IOException
    {
        try
        {
            return resultSet.getLong("start_timestamp");
        }
        catch (SQLException e)
        {
            throw new IOException(e);
        }
    }

    @Override
    public long getEndTimestamp() throws IOException
    {
        try
        {
            return resultSet.getLong("end_timestamp");
        }
        catch (SQLException e)
        {
            throw new IOException(e);
        }
    }
}
