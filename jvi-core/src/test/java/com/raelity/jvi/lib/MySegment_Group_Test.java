/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2022 Ernie Rael.  All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is jvi - vi editor clone.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package com.raelity.jvi.lib;

import org.junit.jupiter.api.Nested;

/**
 * Here's one way to get around parameterized classes.
 * Note that the parent class has no fields, methods
 * and no superclass (ignoring Object).
 * @author err
 */
public class MySegment_Group_Test
{
    @Nested
    public class MySegment_Normal_Test extends MySegmentTest {
        public MySegment_Normal_Test()
        {
            super(MY_SEG, "normal");
        }
    }

    @Nested
    public class MySegment_Subseq_Test extends MySegmentTest
    {
        public MySegment_Subseq_Test()
        {
            super(MY_SEG, "subseq");
        }
    }

    @Nested
    public class StringSegment_Normal_Test extends MySegmentTest
    {
        public StringSegment_Normal_Test()
        {
            super(STR_SEG, "normal");
        }
    }

    @Nested
    public class StringSegment_Subseq_Test extends MySegmentTest
    {
        public StringSegment_Subseq_Test()
        {
            super(STR_SEG, "subseq");
        }
    }
    
}
