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
package com.aitusoftware.flute.lifecycle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ExecutorServiceShutdown
{

    public static ShutdownResult shutdownNowAndWait(final ExecutorService executorService,
                                          final long duration, final TimeUnit durationUnit)
    {
        executorService.shutdownNow();
        try
        {
            return executorService.awaitTermination(duration, durationUnit) ?
                    ShutdownResult.SUCCESS :
                    ShutdownResult.FAILURE;
        }
        catch (InterruptedException e)
        {
            return ShutdownResult.FAILURE;
        }
    }
}
