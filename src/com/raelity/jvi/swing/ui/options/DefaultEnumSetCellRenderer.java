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

package com.raelity.jvi.swing.ui.options;

import java.util.EnumSet;

import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

/**
 *
 * @author err
 */
public class DefaultEnumSetCellRenderer extends DefaultCellRenderer
{
    @Override
    protected String convertToString(Object value)
    {
        // be paranoid and check for the [ and ]
        String s = value.toString();
        if(value instanceof EnumSet && s.startsWith("[") && s.endsWith("]")) {
            // strip off the '[' and ']'
            return s.substring(1, s.length() - 1);
        } else {
            // impossible since must be enum set
            return super.convertToString(value);
        }
    }
    
}
