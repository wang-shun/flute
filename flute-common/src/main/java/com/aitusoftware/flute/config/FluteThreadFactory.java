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
package com.aitusoftware.flute.config;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class FluteThreadFactory implements ThreadFactory
{
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final String prefix;

    public FluteThreadFactory(final String prefix)
    {
        this.prefix = "flute-" + prefix;
    }

    @Override
    public Thread newThread(final Runnable task)
    {
        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.setName(prefix + "-" + COUNTER.getAndIncrement());
        return thread;
    }
}
