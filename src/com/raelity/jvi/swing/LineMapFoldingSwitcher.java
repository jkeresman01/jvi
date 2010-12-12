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
import com.raelity.jvi.lib.MutableInt;

/**
 * Switch between NoFolding and FontFixed ViewMaps.
 * 
 * NOTE: NO SWING CODE
 *
 * A listener that sets the current lm is more efficient.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class LineMapFoldingSwitcher implements LineMap
{
    LineMap lmNoFolding;
    LineMap lmFolding;

    public LineMapFoldingSwitcher(LineMap lmNoFolding,
                                LineMap lmFolding)
    {
        this.lmNoFolding = lmNoFolding;
        this.lmFolding = lmFolding;
    }

    private LineMap getMap()
    {
        return G.isCoordSkip.getBoolean() ? lmFolding : lmNoFolding;
    }

    @Override
    public int logicalLine(int docLine) throws RuntimeException
    {
        return getMap().logicalLine(docLine);
    }

    @Override
    public boolean hasFolding(int docLine,
                            MutableInt pDocFirst, MutableInt pDocLast)
    {
        return getMap().hasFolding(docLine, pDocFirst, pDocLast);
    }

    @Override
    public boolean isFolding()
    {
        return getMap().isFolding();
    }

    @Override
    public int docLineOffset(int viewLine)
    {
        return getMap().docLineOffset(viewLine);
    }

    @Override
    public int docLine(int viewLine)
    {
        return getMap().docLine(viewLine);
    }

    @Override
    public String toString()
    {
        return getMap().toString();
    }

}
