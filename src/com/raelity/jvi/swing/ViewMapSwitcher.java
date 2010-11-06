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

package com.raelity.jvi.swing;

import com.raelity.jvi.swing.ViewMap;

/**
 * Switch between view maps depending on jVi wrap option
 * @author Ernie Rael <err at raelity.com>
 */
public class ViewMapSwitcher implements ViewMap {
    SwingTextView tv;
    ViewMap vmNoWrap;
    ViewMap vmWrap;

    public ViewMapSwitcher(SwingTextView tv)
    {
        this.tv = tv;
        vmNoWrap = new ViewMapNoWrap();
        vmWrap = new SwingViewMapWrapFontFixed(tv);
    }

    private ViewMap getMap()
    {
        return tv.w_p_wrap ? vmWrap : vmNoWrap;
    }

    public int viewLine(int logicalLine)
    {
        return getMap().viewLine(logicalLine);
    }

    public int logicalLine(int viewLine)
    {
        return getMap().logicalLine(viewLine);
    }

    public int countViewLines(int logicalLine)
    {
        return getMap().countViewLines(logicalLine);
    }

}
