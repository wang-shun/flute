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
package com.aitusoftware.flute.server.config;

import com.aitusoftware.flute.config.RequiredProperties;

import java.util.Properties;

import static java.lang.Integer.parseInt;

public final class ServerConfig
{
    private final int httpPort;

    public ServerConfig(final int httpPort)
    {
        this.httpPort = httpPort;
    }

    public int getHttpPort()
    {
        return httpPort;
    }

    public static ServerConfig fromFluteProperties(final Properties properties)
    {
        return new ServerConfig(parseInt(RequiredProperties.requiredProperty("flute.server.httpPort", properties)));
    }
}
