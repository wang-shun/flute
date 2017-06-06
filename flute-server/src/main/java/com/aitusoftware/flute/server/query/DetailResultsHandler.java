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
package com.aitusoftware.flute.server.query;

@FunctionalInterface
public interface DetailResultsHandler
{
    void onRecord(
            final String identifier,
            final long timestamp,
            final long minValue,
            final double meanValue,
            final long fifty,
            final long ninety,
            final long twoNines,
            final long threeNines,
            final long fourNines,
            final long fiveNines,
            final long maxValue,
            final long totalCount,
            final boolean firstRecord);
}
