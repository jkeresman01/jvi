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

import java.awt.event.MouseEvent;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

import com.raelity.jvi.*;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position.Bias;

/**
 * This extension of {@link javax.swing.text.DefaultCaret} draws the
 * caret in different ways as defined by the cursor property.
 * // NEEDSWORK: cache the current font metric, listen to font property changes
 */
public class DefaultViCaret extends DefaultCaret implements ViCaret
{
    ViCaretDelegate viDelegate;
    static Method super_setDot;
    static Method super_moveDot;

    public DefaultViCaret()
    {
        super();
        viDelegate = new ViCaretDelegate(this);
    }

    @Override
    public void install(JTextComponent c)
    {
        c.setNavigationFilter(new NavFilter());
        super.install(c);
    }

    @Override
    public void deinstall(JTextComponent c)
    {
        c.setNavigationFilter(null);
        super.deinstall(c);
    }

    public void setCursor(ViCursor cursor)
    {
        viDelegate.setCursor(cursor);
    }

    public ViCursor getCursor()
    {
        return viDelegate.getCursor();
    }

    @Override
    protected synchronized void damage(Rectangle r)
    {
        if (viDelegate.damage(this, r)) {
            repaint();
        }
    }

    @Override
    protected void adjustVisibility(Rectangle nloc)
    {
        Rectangle r = new Rectangle();
        viDelegate.damage(r, nloc); // broaden to encompass whole character
        super.adjustVisibility(r);
    }

    /**
     * Render the caret as specified by the cursor.
     * <br/>
     * Note: might want to check editor manager, and if not vi then
     * call super(paint), same for super(damage)
     */
    @Override
    public void paint(Graphics g)
    {
        viDelegate.paint(g, getComponent());
    }

    public JTextComponent getTextComponent()
    {
        return super.getComponent();
    }

    private class NavFilter extends NavigationFilter {

        @Override
        public void setDot(FilterBypass fb, int dot, Bias bias)
        {
            if (isMouseAction || mouseButtonDown) {
                dot = ViManager.mouseSetDot(dot, mouseComponent, mouseEvent);
            }
            fb.setDot(dot, bias);
        }

        @Override
        public void moveDot(FilterBypass fb, int dot, Bias bias)
        {
            if (mouseButtonDown) {
                dot = ViManager.mouseMoveDot(dot, mouseComponent, mouseEvent);
            }
            fb.moveDot(dot, bias);
        }
    }

    //
    // Following copied from NbCaret, all have to do with mouse action
    //
    boolean mouseButtonDown;

    @Override
    public void mousePressed(MouseEvent mouseEvent)
    {
        mouseButtonDown = true;
        beginClickHack(mouseEvent);
        super.mousePressed(mouseEvent);
        endClickHack();
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent)
    {
        beginClickHack(mouseEvent);
        super.mouseReleased(mouseEvent);
        ViManager.mouseRelease(mouseEvent);
        endClickHack();
        mouseButtonDown = false;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent)
    {
        beginClickHack(mouseEvent);
        super.mouseClicked(mouseEvent);
        endClickHack();
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent)
    {
        beginClickHack(mouseEvent);
        super.mouseDragged(mouseEvent);
        endClickHack();
    }
    boolean isMouseAction = false;
    JTextComponent mouseComponent;
    MouseEvent mouseEvent;

    private void beginClickHack(MouseEvent mouseEvent)
    {
        isMouseAction = true;
        this.mouseEvent = mouseEvent;
        mouseComponent = (JTextComponent) mouseEvent.getComponent();
    }

    private void endClickHack()
    {
        isMouseAction = false;
    }
}

