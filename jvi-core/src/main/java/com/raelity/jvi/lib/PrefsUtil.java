/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
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

package com.raelity.jvi.lib;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.openide.util.Exceptions;

/**
 *
 * @author err
 */
public class PrefsUtil
{
    public static String dump(Preferences prefs)
    {
        return dump("", prefs);
    }

    public static String dump(String indent, Preferences prefs)
    {
        StringBuilder sb = new StringBuilder();
        dump(indent, prefs, sb);
        return sb.toString();
    }

    public static void dump(Preferences prefs, StringBuilder sb)
    {
        dump("", prefs, sb);
    }

    public static void dump(String indent, Preferences prefs, StringBuilder sb)
    {
        sb.append(indent).append(prefs.absolutePath()).append('\n');
        indent += "    ";
        try {
            for(String key : prefs.keys()) {
                sb.append(indent)
                  .append('\'').append(key).append('\'')
                  .append(":")
                  .append('\'').append(prefs.get(key, null)).append('\'')
                  .append('\n');
            }
            for(String n : prefs.childrenNames()) {
                dump(indent, prefs.node(n), sb);
            }
        } catch(BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private PrefsUtil(){}
}
