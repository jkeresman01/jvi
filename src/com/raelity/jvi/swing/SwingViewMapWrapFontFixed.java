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

import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.core.G;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class SwingViewMapWrapFontFixed implements ViewMap
{
    private static final
            Logger LOG = Logger.getLogger(SwingTextView.class.getName());
    private final SwingTextView tv;

    public SwingViewMapWrapFontFixed(SwingTextView tv)
    {
        this.tv = tv;
    }

    @Override
    public int viewLine(int logicalLine)
    {
        // USED TO BE: int logicalLine(int docLine)
        int docLine = tv.getLineMap().docLine(logicalLine);
        int viewLine = 1;
        try {
          int offset = tv.getBuffer().getLineStartOffset(docLine);
          Rectangle2D lineRect = tv.modelToView(offset);
          viewLine = tv.countViewLines(lineRect, tv.getRect0());
          if ( G.dbgCoordSkip.getBoolean(Level.FINER) ) {
                G.dbgCoordSkip.println(Level.FINER, String.format(
                      "\tviewLine(fixed): %d, line1: %d:%g, line %d:%g",
                      viewLine, 1, tv.getPoint0().getY(), docLine, lineRect.getY()));
          }
        } catch (BadLocationException ex) {
            //Logger.getLogger(SwingTextView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return viewLine;
    }

    @Override
    public int viewLine(ViFPOS fpos)
    {
        int logicalLine = tv.getLogicalLine(fpos.getLine());
        int viewLine = viewLine(fpos.getLine());
        return viewLine
                + countViewLines(tv.getBuffer().getLineStartOffset(logicalLine),
                                 fpos.getOffset()) - 1;
    }

    @Override
    public int logicalLine(int viewLine)
    {
        Rectangle2D lrect = tv.getRect0();
        tv.translateY(lrect, (viewLine - 1) * tv.getFontHeight()
                              + lrect.getCenterY());
        int offset = tv.viewToModel(tv.getLocation(lrect));
        int docLine = tv.getBuffer().getLineNumber(offset);
        return tv.getLineMap().logicalLine(docLine);
    }

    @Override
    public int countViewLines(int logicalLine)
    {
        int count = 1; // default if some exception

        try {
          int offset1 = tv.getBuffer().getLineStartOffset(logicalLine);
          // following should be offset of '\n' 
          int offset2 = tv.getBuffer().getLineEndOffset(logicalLine) - 1;
          count = tv.countViewLines(tv.modelToView(offset2),
                                 tv.modelToView(offset1));
        } catch (BadLocationException ex) {
            //Logger.getLogger(SwingTextView.class.getName()).log(Level.SEVERE, null, ex);
        }

        return count;
    }

    int countViewLines(int offset1, int offset2)
    {
        int count = 1; // default if exception
        try {
            count = tv.countViewLines(tv.modelToView(offset2),
                                      tv.modelToView(offset1));
        } catch(BadLocationException ex) {
            // Logger.getLogger(SwingViewMapWrapFontFixed.class.getName()).
            //      log(Level.SEVERE, null, ex);
        }
        return count;
    }

}
