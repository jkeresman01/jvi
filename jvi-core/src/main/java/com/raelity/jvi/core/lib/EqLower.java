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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.core.lib;

import java.util.Locale;

/**
 * This immutable class wraps a string and implements equals and hashcode
 * based on lower case.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class EqLower {
    private final String s;

    public EqLower(String s)
    {
        this.s = s;
    }

    @Override
    public String toString()
    {
        return s;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final EqLower other = (EqLower)obj;
        return s.toLowerCase(Locale.ENGLISH).
                equals(other.s.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public int hashCode()
    {
        return s.toLowerCase(Locale.ENGLISH).hashCode();
    }

}
