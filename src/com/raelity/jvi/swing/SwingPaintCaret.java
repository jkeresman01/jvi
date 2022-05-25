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
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 * 
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position.Bias;

import com.raelity.jvi.ViCaret;
import com.raelity.jvi.ViCaretStyle;
import com.raelity.jvi.core.G;

// TABS:
//	x	x	x	x	x	x	

/**
 * This handles the VI behavior of a caret, drawing the caret is
 * the big deal.
 * // NEEDSWORK: cache the current font metric, listen to font property changes
 */
public class SwingPaintCaret
{
    ViCaret caret;        // the actual caret
    ViCaretStyle cursor;
    /** width of a block cursor */
    int blockWidth = 8;
    // -Dcom.raelity.jvi.swing.SwingPaintCaret.macRetinaIssue=true;
    //private static final boolean isMacRetinaBug = Boolean.getBoolean(
    //        "com.raelity.jvi.swing.SwingPaintCaret.macRetinaIssue");

    // -Dcom.raelity.jvi.swing.SwingPaintCaret.level=FINE;
    // private static final Logger LOG = Logger.getLogger(SwingPaintCaret.class.getName());
    //private Rectangle oldRect;

    public SwingPaintCaret(ViCaret caret)
    {
        this.caret = caret;
    }

    public void setCursor(ViCaretStyle cursor)
    {
        if (cursor == null) {
            return;
        }
        this.cursor = cursor;

        // java's caret only has one value, compromise on blink rate
        // NEEDSWORK: keep existing until this is user programable
        //caret.setBlinkRate((cursor.getBlinkon() + cursor.getBlinkoff()) / 2);
    }

    public ViCaretStyle getCursor()
    {
        return this.cursor;
    }

    public int getBlockWidth(int offset) {
        return blockWidth;
    }

    public static boolean drawCircle = false;
    public boolean isCircle() {
        return drawCircle;
    }

    public boolean damage(Rectangle cleanupArea, Rectangle cursorArea)
    {
        if (cursorArea != null) {
            cleanupArea.x = cursorArea.x;
            cleanupArea.y = cursorArea.y;
            cleanupArea.width = cursorArea.width;
            cleanupArea.height = cursorArea.height;
            //if(false) {
            //    cleanupArea.x = cursorArea.x > 0 ? cursorArea.x - 1 : cursorArea.x;
            //    cleanupArea.y = cursorArea.y;
            //    cleanupArea.width = blockWidth + 2;
            //    cleanupArea.height = cursorArea.height;
            //}
            return true;
        }
        return false;
    }

    //private void fixupHints(Graphics g)
    //{
    //    if(false)
    //        return;
    //    Graphics2D g2d = (Graphics2D)g;
    //    RenderingHints cur = g2d.getRenderingHints();
    //    RenderingHints h = new RenderingHints(RenderingHints.KEY_RENDERING,
    //                                          RenderingHints.VALUE_RENDER_QUALITY);
    //    h.put(RenderingHints.KEY_ANTIALIASING,
    //          RenderingHints.VALUE_ANTIALIAS_ON);
    //    h.put(RenderingHints.KEY_DITHERING,
    //          RenderingHints.VALUE_DITHER_ENABLE);
    //    h.put(RenderingHints.KEY_INTERPOLATION,
    //          RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    //    h.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
    //          RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    //    h.put(RenderingHints.KEY_COLOR_RENDERING,
    //          RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    //    h.put(RenderingHints.KEY_STROKE_CONTROL,
    //          RenderingHints.VALUE_STROKE_NORMALIZE);
    //    g2d.setRenderingHints(h);
    //}

    /**
     * Render the caret as specified by the cursor.
     *
     * Want to handle focus'd and read only. It's difficult, NB/jdk aren't
     * the same and when they call is not consistent with each other.
     * Need to push logic out of hear, into caller.
     */
    public void paint(Graphics g, boolean blinkVisible, char[] dotChar,
                      JTextComponent component)
    {
        // if the caret is not visible and the editor is not writable
        // then show an outline of a block caret
        boolean drawNotFocused = !component.isFocusOwner();
        boolean drawReadOnly = !component.isEditable();
        boolean drawNormal = false;
        if(!drawReadOnly && !drawNotFocused)
            drawNormal = blinkVisible;

        if (drawNotFocused || drawReadOnly || drawNormal) {
            int dot = caret.getDot();
            FontMetrics fm = g.getFontMetrics();	// NEEDSWORK: should be cached
            // NEEDSWORK: COULD USE dotChar in following
            blockWidth = fm.charWidth('.');		// assume all the same width
            try {
                Rectangle r = component.getUI().modelToView2D(
                        component, dot, Bias.Forward).getBounds();
                CurrentCaretState ccs = new CurrentCaretState(dotChar[0], r, false);
                Rectangle overwritingRectangle = null;
                char overwritingChar = 0;
                int cursorShape = ViCaretStyle.SHAPE_BLOCK;
                if (cursor != null) {
                    cursorShape = cursor.getShape();
                    overwritingChar = cursor.getEditPutchar();
                }
                if(drawNotFocused || drawReadOnly) {
                    cursorShape = ViCaretStyle.SHAPE_BLOCK;
                    overwritingChar = 0;
                }
                if(overwritingChar != 0)
                    overwritingRectangle = (Rectangle) r.clone();
                int h02;
                switch (cursorShape) {
                    case ViCaretStyle.SHAPE_BLOCK:
                        r.width = blockWidth;
                        break;
                    case ViCaretStyle.SHAPE_HOR:
                        r.width = blockWidth;
                        h02 = percent(r.height, cursor.getPercentage());
                        r.y += r.height - h02;
                        r.height = h02;
                        break;
                    case ViCaretStyle.SHAPE_VER:
                        /* back up vertical cursors by one pixel */
                        /* NEEDSWORK: following was in 0.7.x
                        if(r.x > 0) {
                        r.x -= 1;
                        }*/
                        r.width = percent(blockWidth + 1, cursor.getPercentage());
                        break;
                }
                // only allow this if SHAPE_VER
                if(overwritingChar != 0 && cursorShape == ViCaretStyle.SHAPE_VER) {
                    g.setColor(component.getBackground());
                    g.setFont(g.getFont().deriveFont(Font.BOLD));
                    assert overwritingRectangle != null;
                    g.fillRect(overwritingRectangle.x + r.width, overwritingRectangle.y,
                               blockWidth - r.width, fm.getAscent());
                    //g.setColor(component.getForeground());
                    g.setColor(Color.red);
                    g.drawString(String.valueOf(overwritingChar),
                                 overwritingRectangle.x + r.width, overwritingRectangle.y + fm.getAscent());
                }
                g.setColor(component.getCaretColor());
                if(!G.isCursorXorBug())
                    g.setXORMode(component.getBackground());
                if(drawNormal) {
                    if(!isCircle() || cursorShape != ViCaretStyle.SHAPE_BLOCK
                            || G.isCursorXorBug()) {
                        g.fillRect(r.x, r.y, r.width, r.height);
                        // span a tab with a line
                        Rectangle r1 = ccs.getBounds();
                        if(ccs.isTab() && r1.width - 1 > r.width) {
                            g.drawLine(r.x + r.width, r.y + (r.height >> 1),
                                       r.x + r1.width - 1, r.y + (r.height >> 1));
                        }
                    } else {
                        //adjustCaretBounds(r, dotChar, false, "internal");
                        Rectangle r1 = ccs.getBounds();
                        Graphics2D g2d = (Graphics2D)g;
                        g2d.setStroke(new BasicStroke(2));
                        g2d.draw(new Ellipse2D.Double(r1.x, r1.y, r1.width, r1.height));
                    }
                    // log the caret position on the canvas
                    // if(LOG.isLoggable(Level.FINE)) {
                    //     if(LOG.isLoggable(Level.FINEST) || !r.equals(oldRect)) {
                    //         LOG.log(Level.FINE, "jViCaret: {0}", r);
                    //         oldRect = r;
                    //     }
                    // }
                    // drawing the char over the cursor derived from NB
                    if(G.isCursorXorBug()) {
                        if(cursorShape == ViCaretStyle.SHAPE_BLOCK
                                && dotChar != null
                                && !Character.isWhitespace(dotChar[0])
                                ) {
                            Color textBackgroundColor = component.getBackground();
                            if (textBackgroundColor != null)
                                g.setColor(textBackgroundColor);
                            // int ascent = FontMetricsCache.getFontMetrics(afterCaretFont, c).getAscent();
                            g.drawChars(dotChar, 0, 1, r.x,
                                    r.y + fm.getAscent());
                            // r.y + editorUI.getLineAscent());
                        }
                    }
                } else if(drawNotFocused)
                    g.drawRect(r.x, r.y, r.width-1, r.height-1);
                else if(drawReadOnly) {
                    if(G.p_rocc() != null)
                        g.setColor(G.p_rocc());
                    g.drawRect(r.x, r.y, r.width-1, r.height-1);
                    // draw vertical line
                    // g.drawLine(r.x + r.width/2, r.y,
                    //            r.x + r.width/2, r.y + r.height-1);

                    // thicken the line at top
                    g.drawLine(r.x + 1        , r.y + 1,
                               r.x + r.width-2, r.y + 1);
                    // thicken the line at bottom
                    g.drawLine(r.x + 1        , r.y + r.height - 2,
                               r.x + r.width-2, r.y + r.height - 2);
                    // draw vertical line
                    // g.drawLine(r.x + r.width/2, r.y + 2,
                    //            r.x + r.width/2, r.y + r.height-3);

                    // // draw horiz line 1/2 way down
                    // g.drawLine(r.x            , r.y + r.height/2,
                    //            r.x + r.width-2, r.y + r.height/2);
                    // // draw horiz line 1/3 way down
                    // g.drawLine(r.x            , r.y + r.height/3,
                    //            r.x + r.width-2, r.y + r.height/3);
                    // // draw horiz line 1/3 way down
                    // g.drawLine(r.x            , r.y + r.height*2/3,
                    //            r.x + r.width-2, r.y + r.height*2/3);
                }
                g.setPaintMode();

            } catch (BadLocationException e) {
                //System.err.println("Can't render cursor");
            }
        }
    }

    private static int percent(int v, int percentage)
    {
        // NEEDSWORK: percent use int only
        v = (int) (((v * (float) percentage) / 100) + .5);
        return v;
    }

    public void adjustCaretBounds(Rectangle bounds, char[] dotChar, boolean pad, String tag)
    {
        adjustCaretBounds(bounds, dotChar, pad);
    }

    public Rectangle adjustCaretBounds(Rectangle bounds, char[] dotChar, boolean pad)
    {
        CurrentCaretState ccs = new CurrentCaretState(dotChar[0], bounds, pad);
        Rectangle newBounds = ccs.getBounds();
        if(newBounds == null)
            return bounds; // can't do anything

        if(bounds != null) {
            bounds.x = newBounds.x;
            bounds.y = newBounds.y;
            bounds.width = newBounds.width;
            bounds.height = newBounds.height;
        }
        return newBounds;
    }

    private class CurrentCaretState
    {
        private char c;
        private Rectangle bounds; // a clone of modelToView results
        private final JTextComponent jtc;
        
        CurrentCaretState(char c, Rectangle _bounds, boolean pad)
        {
            this.jtc = (JTextComponent)caret.getTextComponent();
            if(this.jtc == null)
                return; // too eary, probably from NbCaret.install
            this.c = c;
            bounds = (Rectangle)(_bounds != null ? _bounds
                          : findBounds(caret.getDot())).clone();
            if(bounds != null)
                bounds.width = model2ViewWidth();
            if(isCircle())
                adjustCircleBounds(pad);
        }

        private Rectangle findBounds(int offset) {
            Rectangle r = null;
            try {
                r = jtc.getUI().modelToView2D(jtc, offset, Bias.Forward).getBounds();
            } catch(BadLocationException ex) {
            }
            return r;
        }
        
        boolean isTab() {
            return c == '\t';
        }

        Rectangle getBounds() {
            return bounds;
        }

        private int model2ViewWidth()
        {
            int offset = caret.getDot();
            int minWidth = getBlockWidth(offset);
            if(c == '\t') {
                Rectangle c2 = findBounds(offset+1);
                if(c2 != null) {
                    if(bounds.x < c2.x) {
                        // 2nd char follows first on the same line
                        int viewWidth = c2.x - bounds.x;
                        minWidth = Math.max(minWidth, viewWidth);
                    }
                }
            }
            return Math.max(bounds.width, minWidth);
        }

        /**
         * There is a rectangular bounds that may have been widened,
         * this make it an oval that includes the rectangle.
         */
        private void adjustCircleBounds(boolean pad)
        {
            // NEEDSWORK:
            // If pad is false, then use font WxH as basis

            // Just a circle for now
            // Get a square, based on the length of the hypotenuse,
            // then a circle includes the corners
            int hypo = hypo(bounds.height, bounds.width);
            int xExpand = hypo - bounds.width + (pad ? 4 : 0);
            int yExpand = hypo - bounds.height + (pad ? 4 : 0);
            // expand direction by given number of pixels, make them even
            xExpand = (xExpand + 1) & ~1;
            yExpand = (yExpand + 1) & ~1;
            bounds.x -= xExpand >> 1;
            bounds.y -= yExpand >> 1;
            bounds.width += xExpand;
            bounds.height += yExpand;
        }

        private int hypo(int x, int y) {
            return (int)Math.round(Math.ceil(Math.sqrt((double)x * x + y * y)));
        }
    }
}

