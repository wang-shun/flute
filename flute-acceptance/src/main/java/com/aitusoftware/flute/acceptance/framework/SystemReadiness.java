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
package com.aitusoftware.flute.acceptance.framework;

import org.junit.Assert;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

final class SystemReadiness
{
    private SystemReadiness()
    {
    }

    static void waitForServer(final InetSocketAddress endpoint)
    {
        final long timeoutAt = System.currentTimeMillis() + 10_000L;
        boolean serverAvailable = false;
        while (System.currentTimeMillis() < timeoutAt && !serverAvailable)
        {
            try
            {
                new Socket().connect(endpoint);
                serverAvailable = true;
            }
            catch (IOException e)
            {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100L));
            }
        }

        if(!serverAvailable)
        {
            Assert.fail("Server did not become available at " + endpoint);
        }
    }
}
