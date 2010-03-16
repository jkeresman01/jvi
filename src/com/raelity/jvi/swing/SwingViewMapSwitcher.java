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

import com.raelity.jvi.core.G;

/**
 * Switch between NoFolding and FontFixed ViewMaps.
 *
 * A listener that sets the current vm is more efficient.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class SwingViewMapSwitcher implements ViewMap
{
    ViewMap vmNoFolding;
    ViewMap vmFontFixed;

    public SwingViewMapSwitcher(SwingTextView tv)
    {
        vmNoFolding = new SwingViewMapNoFolding(tv);
        vmFontFixed = new SwingViewMapFontFixed(tv);
    }

    private ViewMap getMap()
    {
        return G.isCoordSkip.getBoolean() ? vmFontFixed : vmNoFolding;
    }

    public int viewLine(int docLine) throws RuntimeException
    {
        return getMap().viewLine(docLine);
    }

    public boolean isFontFixedWidth()
    {
        return getMap().isFontFixedWidth();
    }

    public boolean isFontFixedHeight()
    {
        return getMap().isFontFixedHeight();
    }

    public boolean isFontFixed()
    {
        return getMap().isFontFixed();
    }

    public boolean isFolding()
    {
        return getMap().isFolding();
    }

    public int docLineOffset(int viewLine)
    {
        return getMap().docLineOffset(viewLine);
    }

    public int docLine(int viewLine)
    {
        return getMap().docLine(viewLine);
    }

}
