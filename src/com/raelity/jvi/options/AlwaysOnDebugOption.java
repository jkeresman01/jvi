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

package com.raelity.jvi.options;

import java.util.logging.Level;

/**
 * This debug option always does the output.
 * It has a similar API to an actual DebugOption.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class AlwaysOnDebugOption {

    final public Boolean getBoolean()
    {
        return true;
    }

    final public boolean getBoolean(Level level)
    {
        return true;
    }

    final public void println(String s, Object... args)
    {
        println(Level.SEVERE, s, args);
    }

    final public void println(Level level, String msg, Object... args)
    {
        if(getBoolean(level)) {
            String s = args.length == 0 ? msg : String.format(msg, args);
            System.err.println(s);
        }
    }

    final public void printf(String format, Object... args)
    {
        printf(Level.SEVERE, format, args);
    }

    final public void printf(Level level, String format, Object... args)
    {
        if(getBoolean(level))
            System.err.printf(format, args);
    }

}
