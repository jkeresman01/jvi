/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */

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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.awt.Point;
import javax.swing.text.Position;
import javax.swing.text.JTextComponent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.BadLocationException;
import javax.swing.plaf.TextUI;

import com.raelity.jvi.ViCursor;
import com.raelity.jvi.*;

/**
 * This extension of {@link javax.swing.text.DefaultCaret} draws the
 * caret in different ways as defined by the cursor property.
 * // NEEDSWORK: cache the current font metric, listen to font property changes
 */
public class DefaultViCaret extends DefaultCaret implements ViCaret {
  ViCaretDelegate viDelegate;

  public DefaultViCaret() {
    super();
    viDelegate = new ViCaretDelegate(this);
  }

  public void setCursor(ViCursor cursor) {
    viDelegate.setCursor(cursor);
  }

  public ViCursor getCursor() {
    return viDelegate.getCursor();
  }

  protected synchronized void damage(Rectangle r) {
    if(viDelegate.damage(this, r)) {
      repaint();
    }
  }
  
  protected void adjustVisibility(Rectangle nloc) {
    Rectangle r = new Rectangle();
    viDelegate.damage(r, nloc); // broaden to encompass whole character
    super.adjustVisibility(r);
  }

  /**
   * Render the caret as specified by the cursor.
   * <br>
   * Note: might want to check editor manager, and if not vi then
   * call super(paint), same for super(damage)
   */
  public void paint(Graphics g) {
    viDelegate.paint(g, getComponent());
  }

  public JTextComponent getTextComponent() {
    return super.getComponent();
  }

  /**
   * Tries to set the position of the caret from
   * the coordinates of a mouse event, using viewToModel().
   * Notifies vi that the most has been clicked in window
   * and give vi a chance to adjust the position.
   *
   * @param e the mouse event
   */
  /*protected void positionCaret(MouseEvent e) {
    viDelegate.positionCaret(e);
  }*/

    /* IN SWING, THIS CALLS THE FOLLOWING SET DOT
    public void setDot(int dot) {
        if(isMouseAction || mouseButtonDown)
            dot = ViManager.mouseSetDot(dot, mouseComponent);
        super.setDot(dot);
    }*/
    public void setDot(int dot, Position.Bias dotBias) {
        if(isMouseAction || mouseButtonDown)
            dot = ViManager.mouseSetDot(dot, mouseComponent, mouseEvent);
        super.setDot(dot, dotBias);
    }
    
    /* IN SWING, THIS CALLS THE FOLLOWING MOVE DOT
    public void moveDot(int dot) {
        if(mouseButtonDown)
            dot = ViManager.mouseMoveDot(dot, mouseComponent);
        super.moveDot(dot);
    }*/

    public void moveDot(int dot, Position.Bias dotBias) {
        if(mouseButtonDown)
            dot = ViManager.mouseMoveDot(dot, mouseComponent, mouseEvent);
        super.moveDot(dot, dotBias);
    }

  //
  // Following copied from NbCaret, all have to do with mouse action
  //

    boolean mouseButtonDown;

    public void mousePressed(MouseEvent mouseEvent) {
        mouseButtonDown = true;
        beginClickHack(mouseEvent);
        super.mousePressed(mouseEvent);
        endClickHack();
    }
    
    public void mouseReleased(MouseEvent mouseEvent) {
        beginClickHack(mouseEvent);
        super.mouseReleased(mouseEvent);
        ViManager.mouseRelease(mouseEvent);
        endClickHack();
        mouseButtonDown = false;
    }

    public void mouseClicked(MouseEvent mouseEvent) {
        beginClickHack(mouseEvent);
        super.mouseClicked(mouseEvent);
        endClickHack();
    }
    public void mouseDragged(MouseEvent mouseEvent) {
        beginClickHack(mouseEvent);
        super.mouseDragged(mouseEvent);
        endClickHack();
    }

    boolean isMouseAction = false;
    JTextComponent mouseComponent;
    MouseEvent mouseEvent;
    
    private void beginClickHack(MouseEvent mouseEvent) {
        isMouseAction = true;
        this.mouseEvent = mouseEvent;
        mouseComponent = (JTextComponent)mouseEvent.getComponent();
    }
    
    private void endClickHack() {
        isMouseAction = false;
    }

}

