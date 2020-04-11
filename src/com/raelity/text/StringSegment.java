/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2019 Ernie Rael.  All Rights Reserved.
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.text;

/**
 * A segment that is backed by a String.
 * The biggest problem is returning a zero
 * for an access that would be the null terminator
 * 
 * NOTE: if the segment's array is accessed, there is probably an NPE.
 * 
 * This should have been around in jVi forever...
 * 
 * @deprecated Use MySegment(String)
 * 
 * @author err
 */
final public class StringSegment extends MySegment
{

/**
 * The whole string which is virtually null terminated
 */
public StringSegment(String s)
{
    super(s);
}

    @Override
    public StringSegment subSequence(int start, int end) {
        return (StringSegment)super.subSequence(start, end);
    }

/**
 * Creates a shallow copy.
 *
 * @return the copy
 */
@Override
final public Object clone() {
    return super.clone();
}

}