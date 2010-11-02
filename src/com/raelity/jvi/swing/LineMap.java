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

/**
 * This interface is used to translate document line numbers to logical line
 * numbers and visa-versa. Logical lines refer to the lines that get displayed;
 * for example with code folding a group of lines and/or columns may only
 * be displayed as a single line.
 * 
 * NOTE: NO SWING CODE
 *
 * Note that line wrap is a further complication. When there is line wrapping
 * a single logical line may occupy multiple screen lines.
 * 
 * {@link SwingTextView} delegates to one of these.
 * {@link LineMapFoldSwitcher} can be used to switch between two
 * implementations based on a folding-compatible option.
 *
 * The translation is essentially 1-1 when fixed width/height
 * fonts, no code folding and no line wrap.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public interface LineMap
{

    /**
     * Check if folding is supported. Any feature that may hide lines,
     * or parts of lines, is considered folding.
     * @return true if folding is supported
     */
    public boolean isFolding();

    /**
     * Map a document line number to a view line number.
     * 
     * @param docLine line number of something in the document
     * @return line number taking folding into account
     * @throws RuntimeException if docLine is not in the file
     */
    public int logicalLine(int docLine) throws RuntimeException; // NEEDSWORK:

    public int docLine(int logicalLine);

    public int docLineOffset(int logicalLine);
}
