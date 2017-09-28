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
package com.aitusoftware.flute.server.batch;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BatchCalculatorTest
{
    private static final CandidateBatch SECOND_RESOLUTION_CANDIDATE_BATCH =
            new CandidateBatch(1, TimeUnit.SECONDS, "metric.name",
                    LocalDateTime.of(2017, 9, 10,
                            14, 54, 37).
                            toInstant(ZoneOffset.UTC).toEpochMilli(),
                    LocalDateTime.of(2017, 9, 3,
                            14, 54, 37).
                            toInstant(ZoneOffset.UTC).toEpochMilli());

    @Test
    public void shouldCalculateTargetWindow() throws Exception
    {
        final BatchCalculator calculator = new BatchCalculator(SECOND_RESOLUTION_CANDIDATE_BATCH);
        assertThat(calculator.getTargetRollUpUnit(), is(TimeUnit.HOURS));
        assertThat(calculator.getTargetRollUpLength(), is(1L));
    }
}