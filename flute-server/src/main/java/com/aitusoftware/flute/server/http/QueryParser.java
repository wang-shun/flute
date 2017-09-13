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
package com.aitusoftware.flute.server.http;

import com.aitusoftware.flute.server.query.Query;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

final class QueryParser
{
    static Optional<Query> parseQuery(final String pathInfo)
    {
        try
        {
            final String[] components = pathInfo.split("/");
            if(components.length > 4)
            {
                return Optional.of(Query.withEndTime(components[1],
                        Long.parseLong(components[2]), TimeUnit.valueOf(components[3]), Long.parseLong(components[4])
                ));
            }
            else
            {
                return Optional.of(Query.latest(components[1],
                        Long.parseLong(components[2]), TimeUnit.valueOf(components[3])));
            }
        }
        catch(RuntimeException e)
        {
            return Optional.empty();
        }
    }
}