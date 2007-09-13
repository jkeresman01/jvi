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

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JEditorPane;
import javax.swing.JViewport;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.raelity.jvi.FPOS;
import com.raelity.jvi.Util;
import com.raelity.jvi.BooleanOption;
import com.raelity.jvi.G;
import com.raelity.jvi.Options;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.swing.DefaultBuffer.ElemCache;

import javax.swing.text.AbstractDocument;
import javax.swing.text.Position;


/*
  ViFPOS getCursor()
  PositionSegment getLineSegment(int line)
  PositionSegment getCurrentLineSegment()
  Element getLineElement(int line)
  Document getDoc()
  int getTopLineOffset()
  int getBottomLineOffset()
  int getScreenLines()
*/

/**
 * This class caches information about the various components that make
 * up the VI model. This class listens to these components as needed to
 * keep the cache up to date. All info is derived through a JEditorPane.
 */
public class TextViewCache implements PropertyChangeListener,
				      ChangeListener
{
  final static int DIR_TOP = -1;
  final static int DIR_BOT = 1;

  private TextView tv;

  public TextViewCache(TextView textView) {
    this.tv = textView;
  }

  public static BooleanOption cacheTrace
                = (BooleanOption)Options.getOption(Options.dbgCache);

  //
  // The cursor is a magic FPOS. It reads the current caret positon
  // and writes move the caret.
  //
  private WCursor cursor = new WCursor();

  //
  // NEEDSWORK: move WCursor up to TextView, doesn't depend on cache any more
  //

  private class WCursor extends ViFPOS.abstractFPOS {
    final public int getLine() {
      return tv.getBuffer().getElemCache(tv.getCaretPosition()).line;
    }
    final public int getColumn() {
      int offset = tv.getCaretPosition();
      return offset - tv.getBuffer().getElemCache(offset).elem.getStartOffset();
    }
    final public int getOffset() {
      return tv.getCaretPosition();
    }
    
    final public void set(int line, int column) {
      //System.err.println("setPosition("+line+","+column+")");
      Element elem = tv.getBuffer().getLineElement(line);
      int startOffset = elem.getStartOffset();
      int endOffset = elem.getEndOffset();
      int adjustedColumn = -1;

      if(column < 0) {
        adjustedColumn = 0;
      } else if(column >= endOffset - startOffset) {
        column = endOffset - startOffset - 1;
      }

      if(adjustedColumn >= 0) {
        ViManager.dumpStack("line " + line + ", column " + column
                + ", length " + (endOffset - startOffset));
        column = adjustedColumn;
      }

      tv.setCaretPosition(startOffset + column);
    }

    final public ViFPOS copy() {
      int offset = tv.getCaretPosition();
      ElemCache ec = tv.getBuffer().getElemCache(offset);
      FPOS fpos = new FPOS(offset, ec.line, offset - ec.elem.getStartOffset());
      //fpos.initFPOS(getOffset(), getLine(), getColumn());
      return fpos;
    }
  };

  final public ViFPOS getCursor() {
    return cursor;
  }

  final public JViewport getViewport() {
    return viewport;
  }

  final public int getFheight() {
    return fheight;
  }

  /** current font information. */
  private FontMetrics fm;
  /** height of the font */
  private int fheight;

  // The visible part of the document, negative means not valid.
  // These values are updated whenever the viewport changes.

  private JViewport viewport;
  private Point viewportPosition;
  private Dimension viewportExtent;
  private int topLineOffset;
  private int bottomLineOffset;

  private int viewTopLine;
  private int viewBottomLine;
  private int viewLines;

  /** @return the top line number */
  public int getViewTopLine() {
    return viewTopLine;
  }

  public void setViewTopLine(int line) {
    if(line == getViewTopLine()) {
      return; // nothing to change
    }
    int offset = tv.getBuffer().getLineStartOffset(line);
    Rectangle r;
    try {
      r = tv.getEditorComponent().modelToView(offset);
    } catch(BadLocationException e) {
      Util.vim_beep();
      return;
    }
    Point p = r.getLocation();
    p.translate(-p.x, 0); // leave a few pixels to left
    viewport.setViewPosition(p);
  }

  public int getViewBottomLine() {
    return viewBottomLine + 1;	// NEEDSWORK:
  }

  public int getViewBlankLines() {
    int blank = viewLines - (viewBottomLine - viewTopLine + 1);
    return blank;
  }

  /** @return number of lines on viewport */
  public int getViewLines() {
    return viewLines;
  }
  
  protected void fillLinePositions() {
    /*
    SwingUtilities.invokeLater(
      new Runnable() { public void run() { fillLinePositionsFinally(); }});
    */
    fillLinePositionsFinally();
  }

  /** determine document indicators visible in the viewport */
  protected void fillLinePositionsFinally() {
    Point newViewportPosition;
    Dimension newViewportExtent;
    Rectangle r;
    int newViewLines = -1;
    boolean topLineChange = false;
    
    if(viewport == null) {
      newViewportPosition = null;
      newViewportExtent = null;
    } else {
      newViewportPosition = viewport.getViewPosition();
      newViewportExtent = viewport.getExtentSize();
    }
    
    int newViewTopLine;
    if(newViewportPosition == null
       || newViewportExtent == null
       || (newViewTopLine = findFullLine(newViewportPosition, DIR_TOP)) <= 0)
    {
      viewTopLine = -1;
      viewBottomLine = -1;
      newViewLines = -1;
    } else {
      if(viewTopLine != newViewTopLine) {
          topLineChange = true;
      }
      viewTopLine = newViewTopLine;
      Point pt = new Point(newViewportPosition); // top-left
      pt.translate(0, newViewportExtent.height-1); // bottom-left
      viewBottomLine = findFullLine(pt, DIR_BOT);
      //
      // Calculate number of lines on screen, some may be blank
      //
      newViewLines = newViewportExtent.height / fheight;
    }

    boolean sizeChange = false;
    if( newViewportExtent == null
			|| ! newViewportExtent.equals(viewportExtent)
                        || newViewLines != viewLines) {
      sizeChange = true;
    }
    viewLines = newViewLines;
    viewportPosition = newViewportPosition;
    viewportExtent = newViewportExtent;

    if(sizeChange) {
      ViManager.viewSizeChange(tv);
    }
    if(sizeChange || topLineChange) {
      ViManager.viewMoveChange(tv);
    }
  }

  /**
   * Determine the line number of the text that is fully displayed
   * (top or bottom not chopped off).
   * @return line number of text at point, -1 if can not be determined
   */
  private int findFullLine(Point pt, int dir) {
    Rectangle vrect = viewport.getViewRect();
    JEditorPane editor = tv.getEditorComponent();
    
    int offset = editor.viewToModel(pt);
    if(offset < 0) {
        return -1;
    }
    int line = tv.getBuffer().getLineNumber(offset);
    Rectangle lrect;
    try {
      lrect = editor.modelToView(offset);
    }
    catch (BadLocationException ex) {
      System.err.println("findFullLine: exeption 1st " + line);
      return -1; // can't happen
    }
    
    if(vrect.contains(lrect)) {
      return line;
    }
    int oline = line;
    line -= dir; // move line away from top/bottom
    if(line < 1 || line > tv.getBuffer().getLineCount()) {
      //System.err.println("findFullLine: line out of bounds " + line);
      return oline;
    }
    offset = tv.getBuffer().getLineStartOffset(line);
    try {
      lrect = editor.modelToView(offset);
    }
    catch (BadLocationException ex) {
      System.err.println("findFullLine: exeption 2nd " + line);
      return -1; // can't happen
    }
    if( ! vrect.contains(lrect)) {
      //System.err.println("findFullLine: adjusted line still out " + line);
    }
    return line;
  }

  //
  // Track changes of interest.
  //
  // There are two types of cached things we're interested in,
  //	- data from the document, especially around the cursor
  //	  Caret movement and document changes affect this
  //
  //	- visible screen: top line, bottom line
  //	  resize, viewport affects this
  //

  private void changeDocument(Document doc) {
    if(cacheTrace.getBoolean())System.err.println("doc switch: ");
    // NEEDSWORK:
    assert false;
  }

  private void changeFont(Font f) {
    int h;
    if(f == null) {
      fm = null;
    } else {
      fm = tv.getEditorComponent().getFontMetrics(f);
      fheight = fm.getHeight();
      fillLinePositions();
    }
  }

  /** The container for the editor has changed. */
  private void changeViewport(Object component) {
    if(viewport != null) {
      viewport.removeChangeListener(this);
    }
    if (component instanceof JViewport) {
      viewport = (JViewport)component;
      viewport.addChangeListener(this);
    } else {
      viewport = null;
    }
    changeView(true);
  }

  /** The defining rectangle of the viewport has changed
   *  @param init true indicates that the position should be
   *  checked immeadiately, not potentially defered with an invoke later.
   */
  private void changeView(boolean init) {
    if(init) {
      fillLinePositionsFinally();
    } else {
      fillLinePositions();
    }
  }

  /** This is called from the managing textview,
   * listen to things that affect the cache.
   */
  public void attach(JEditorPane editor) {
    if(G.dbgEditorActivation.getBoolean()) {
      System.err.println("TVCache: attach: "
              + (editor == null ? 0 : editor.hashCode()));
    }
    if(freezer != null) {
        freezer.stop();
        freezer = null;
    }
    
    if(hasListeners)
        return;
    
    hasListeners = true;
    editor.addPropertyChangeListener("font", this);
    editor.addPropertyChangeListener("document", this);
    editor.addPropertyChangeListener("ancestor", this);
    changeFont(editor.getFont());
    changeViewport(editor.getParent());
  }
  
  boolean hasListeners = false;

  /** Dissassociate from the observed components. */
  public void detach(JEditorPane ep) {
    if(G.dbgEditorActivation.getBoolean()) {
      System.err.println("TVCache: detach: "
              + (ep == null ? "" : ep.hashCode()));
    }
    if(ep == null) {
      return;
    }
    freezer = new FreezeViewport(ep);
    
    //removeListeners();
  }
  
  public void shutdown(JEditorPane ep) {
    if(freezer != null) {
        freezer.stop();
        freezer = null;
    }
    removeListeners();
  }
  
  private void removeListeners() {
    hasListeners = false;
    JEditorPane editor = tv.getEditorComponent();
    editor.removePropertyChangeListener("font", this);
    editor.removePropertyChangeListener("document", this);
    editor.removePropertyChangeListener("ancestor", this);
    changeViewport(null);
  }
  
  private FreezeViewport freezer;

  //
  // Listener events
  //

  // -- property change events --

  public void propertyChange(PropertyChangeEvent e) {
    String p = e.getPropertyName();
    Object o = e.getNewValue();
    if("font".equals(p)) {
      changeFont((Font)o);
    } else if("document".equals(p)) {
      changeDocument((Document)o); // this assert
    } else if("ancestor".equals(p)) {
      changeViewport(o);
    }
  }


  // -- viewport event --

  public void stateChanged(ChangeEvent e) {
    changeView(false);
  }
    
    /**
     * Stabilize (do not allow scrolling) the JViewport displaying
     * the indicated JEditorPane.
     * This is typically used when the underlying document may change while
     * being edited in another view. The {@link #stop} method is used to release
     * the listeners and so unfreeze the viewport.
     * <p>This is a one shot class. The editor is expected to be good to go.
     * Only document changes are listened to. The first char of the top line is
     * pinned to the upper left corner. If needed, this could be extended
     * to pin the horizontal position as well.
     */
    public static class FreezeViewport implements DocumentListener {
        private JEditorPane ep;
        private JViewport vp;
        private AbstractDocument doc;
        private Position pos;
        private int topLine;
        private int nLine;
        
        public FreezeViewport(JEditorPane ep) {
            this.ep = ep;
            Object o = ep.getDocument();
            if(!(o instanceof AbstractDocument))
                return;
            doc = (AbstractDocument)ep.getDocument();
            try {
                doc.readLock();
                vp = (JViewport)ep.getParent(); // may throw class cast, its ok
                Element root = doc.getDefaultRootElement();
                nLine = root.getElementCount();
                
                // Get the offset of the first displayed char in the top line
                Point pt = vp.getViewPosition();
                int offset = ep.viewToModel(pt);
                
                // Determine the line number of the top displayed line
                topLine = root.getElementIndex(offset);
                
                // Note. offset may not be first char, due to horiz scroll
                // make offset the first char of the line
                offset = root.getElement(topLine).getStartOffset();
                
                // Get marker to offset in the document
                pos = doc.createPosition(offset);
                doc.addDocumentListener(this);
            } catch (Exception ex) {
                // Note: did not start listener
            } finally {
                doc.readUnlock();
            }
        }
    
        public void stop() {
            if(doc != null)
                doc.removeDocumentListener(this);
        }
    
        private void adjustViewport(int offset) {
            // Might be able to use info from DocumentEvent to optimize
            try {
                Point pt = ep.modelToView(offset).getLocation();
                pt.translate(-pt.x, 0); // x <-- 0, leave a few pixels to left
                vp.setViewPosition(pt);
            } catch(Exception ex) {
                stop();
            }
            return;
        }
        
        private void handleChange(DocumentEvent e) {
            // Note while in listener document can't change, no read lock
            Element root = doc.getDefaultRootElement();
            int newNumLine = root.getElementCount();
            // return if line count unchanged or changed after our mark
            if(nLine == newNumLine || e.getOffset() > pos.getOffset())
                return;
            nLine = newNumLine;
            
            int newTopLine = root.getElementIndex(pos.getOffset());
            if(topLine == newTopLine)
                return;
            topLine = newTopLine;
            
            // make a move
            final int offset = root.getElement(topLine).getStartOffset();
            if(EventQueue.isDispatchThread()) {
                adjustViewport(offset);
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        adjustViewport(offset);
                    }
                });
            }
        }

        public void insertUpdate(DocumentEvent e) { handleChange(e); }

        public void removeUpdate(DocumentEvent e) { handleChange(e); }

        public void changedUpdate(DocumentEvent e) { }
    }
}

// vi: sw=2 ts=8
