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
package com.aitusoftware.flute.integration.service;

import org.h2.server.TcpServer;
import org.h2.tools.Server;

public final class DatabaseServerMain
{
    public static void main(final String[] args) throws Exception
    {
        final Server server = new Server(new TcpServer(), "-tcp", "-tcpAllowOthers", "-baseDir", args[0]);
        server.start();
        server.shutdown();
    }
}
