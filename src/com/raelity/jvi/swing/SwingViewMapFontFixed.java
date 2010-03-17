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

import static java.lang.Math.round;
import com.raelity.jvi.core.G;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;

/**
 * Handle a view that has fixed with/height font.
 *
 * Font changes in the document are handled.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class SwingViewMapFontFixed implements ViewMap {
    private static final
            Logger LOG = Logger.getLogger(SwingTextView.class.getName());
    private SwingTextView tv;

    public SwingViewMapFontFixed(SwingTextView tv)
    {
        this.tv = tv;
        // tv.getEditorComponent().addPropertyChangeListener("font",
        //     new PropertyChangeListener()
        //     {
        //         public void propertyChange(PropertyChangeEvent evt)
        //         {
        //             if(evt.getPropagationId().equals("font"))
        //                 changeFont();
        //         }
        // });
    }

    public boolean isFontFixed()
    {
        return true;
    }

    public boolean isFontFixedHeight()
    {
        return true;
    }

    public boolean isFontFixedWidth()
    {
        return true;
    }

    public boolean isFolding()
    {
        return true;
    }

    // NEEDSWORK: getViewLine CALLED A LOT
    public int viewLine(int docLine) throws RuntimeException
    {
        int viewLine = 1;
        try {
          int offset = tv.getBuffer().getLineStartOffset(docLine);
          Rectangle2D lineRect = tv.modelToView(offset);
          double yDiff = lineRect.getCenterY() - getRect0().getCenterY();
          viewLine = (int)round(yDiff / getFontHeight()) + 1;
          if ( G.dbgCoordSkip.getBoolean() ) {
              System.err.println(String.format(
                      "\tviewLine(fixed): %d, line1: %d:%g, line %d:%g",
                      viewLine, 1, getPoint0().getY(), docLine, lineRect.getY()));
           }
        } catch (BadLocationException ex) {
            //Logger.getLogger(SwingTextView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return viewLine;
    }

    public int docLineOffset(int viewLine)
    {
        Point2D p = getPoint0();
        p.setLocation(p.getX(),
                      (viewLine - 1) * getFontHeight() + getFontHeight()/2);
        int docLineOffset = tv.viewToModel(p);
        return docLineOffset;
    }

    public int docLine(int viewLine)
    {
        int docLine;
        docLine = tv.getBuffer().getLineNumber(docLineOffset(viewLine));
        return docLine;
    }

    //private int fontHeight;

    //public void changeFont(Font f)
    //{
    //    point0 = null;
    //    if(f != null) {
    //        FontMetrics fm = tv.getEditorComponent().getFontMetrics(f);
    //        fontHeight = fm.getHeight();
    //    }
    //}

    //public int getFontHeight()
    //{
    //    return fontHeight;
    //}

    public double getFontHeight()
    {
        return getRect0().getHeight();
    }

    // CAN'T CACHE IF DON'T MONITOR FONT CHANGES
    //private Rectangle rect0;
    //private Rectangle getRect0()
    //{
    //    if ( rect0 != null ) {
    //        return rect0;
    //    }
    //    try {
    //      Rectangle r = tv.modelToView(0);
    //        if ( r != null ) {
    //            rect0 = r;
    //            return rect0;
    //        }
    //    } catch (BadLocationException ex) { }
    //    return new Rectangle(0,0,8,15);
    //}
    private Rectangle2D getRect0()
    {
        try {
          Rectangle2D r = tv.modelToView(0);
            if ( r != null ) {
                return r;
            }
        } catch (BadLocationException ex) { }
        return new Rectangle2D.Double(0,0,8,15); // arbitrary
    }
    private Point2D getPoint0()
    {
        return tv.getLocation(getRect0());
    }

    private static int adjustedY(Rectangle r)
    {
        // return r.y + (r.height >> 2); // center of y in rectangle
        return r.y;
    }

}
