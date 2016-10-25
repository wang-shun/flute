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
package com.aitusoftware.flute.server.reporting.dao;

import com.aitusoftware.flute.server.reporting.domain.ReportSpecification;

final class ReportSpecificationValidator
{
    private static final int REPORT_NAME_MAX_LENGTH = 64;
    private static final int SELECTOR_PATTERN_MAX_LENGTH = 256;

    void validate(final ReportSpecification reportSpecification)
    {
        if(reportSpecification.getReportName().length() > REPORT_NAME_MAX_LENGTH)
        {
            throw new IllegalArgumentException("Report name must not exceed " + REPORT_NAME_MAX_LENGTH + " characters");
        }
        if(reportSpecification.getSelectorPattern().length() > SELECTOR_PATTERN_MAX_LENGTH)
        {
            throw new IllegalArgumentException("Selector pattern must not exceed " + SELECTOR_PATTERN_MAX_LENGTH + " characters");
        }

    }
}
