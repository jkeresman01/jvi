/*
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
 * The Initial Developer of the Original Code is Ernie Rael.
 * Portions created by Ernie Rael are
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi;

/**
 * Essentially a tag for an Option with enum values.
 *
 * Would like to have EnumOption<E>
 * but that's a lot more work. Since can't do
 * EnumOption<Integer> extends IntegerOption, everything would have to change.
 * Would need Option<E> as well.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public interface EnumOption {
    public Object[] getAvailableValues();
}
