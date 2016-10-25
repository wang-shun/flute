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
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.fail;

public final class ReportSpecificationValidatorTest
{
    private final ReportSpecificationValidator validator = new ReportSpecificationValidator();

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfReportNameIsTooLong() throws Exception
    {
        validator.validate(specificationNamed(generateString(65)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfSelectorPatternIsTooLong() throws Exception
    {
        validator.validate(specificationWithPattern(generateString(257)));
    }

    @Ignore
    @Test
    public void shouldBlowUpIfMarshalledTimeWindowsExceedMaximumLength() throws Exception
    {
        fail("Not yet implemented");
    }

    private static ReportSpecification specificationWithPattern(final String pattern)
    {
        return new ReportSpecification("report_0", pattern, Collections.emptyList(), Collections.emptyList());
    }

    private static ReportSpecification specificationNamed(final String reportName)
    {
        return new ReportSpecification(reportName, ".*", Collections.emptyList(), Collections.emptyList());
    }

    private static String generateString(final int length)
    {
        final StringBuilder builder = new StringBuilder(length);
        for(int i = 0; i < length; i++)
        {
            builder.append('0');
        }

        return builder.toString();
    }
}