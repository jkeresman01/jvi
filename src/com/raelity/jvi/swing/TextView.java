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

import java.awt.Point;

import javax.swing.text.*;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import com.raelity.jvi.FPOS;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.swing.TextOps;
import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.ViCursor;
import com.raelity.jvi.Util;
import com.raelity.jvi.MutableInt;
import com.raelity.jvi.Window;
import com.raelity.jvi.*;

/**
 * Presents a swing editor interface for use with vi. There should be
 * one of these for each visual editor display area, so multiple tabs
 * in the same pane editing different files would have one TextView;
 * there is typically one status display for each text view.
 * <p>Notice the listeners for caret changes. If the caret changes
 * to a location that is unexpected, i.e. it came from some external
 * source, then an externalChange message is sent to vi.
 * </p><p>
 * The getEditorComponent method should not be used by the primary
 * vi software. The primary vi software should only access, or make
 * changes to, the underlying JEditorPane through other methods in
 * this class.
 * </p>
 */
public class TextView implements ViTextView {
  protected JEditorPane editorPane;
  protected TextOps ops;
  protected static char[] oneCharArray = new char[1];
  protected TextViewCache cache;
  protected Window window;

  protected ViStatusDisplay statusDisplay;

  protected int expectedCaretPosition = -1;

  public TextView() {
    cache = createTextViewCache();
    statusDisplay = createStatusDisplay();
  }

  public void setWindow(Window window) {
    this.window = window;
  }

  public Window getWindow() {
    return window;
  }

  //
  // Pretend a little to be a window
  //

  /** @return the current location of the cursor in the window */
  public FPOS getWCursor() { return cache.getCursor(); }
  //public FPOS getWCursor() { return window.getWCursor(); }

  public int getWCurswant() { return window.getWCurswant(); }
  public void setWCurswant(int c) { window.setWCurswant(c); }
  public boolean getWSetCurswant() { return window.getWSetCurswant(); }
  public void setWSetCurswant(boolean f) { window.setWSetCurswant(f); }

  public ViMark getPCMark() { return window.getPCMark(); }
  public ViMark getPrevPCMark() { return window.getPrevPCMark(); }
  public void pushPCMark() { window.pushPCMark(); }
  public ViMark getMark(int i) { return window.getMark(i); }

  public int getWPScroll() { return window.getWPScroll(); }
  public void setWPScroll(int n) { window.setWPScroll(n); }
  public boolean getWPList() { return window.getWPList(); }
  public void setWPList(boolean f) { window.setWPList(f); }

  /**
   * Override this class to provide a different implementations
   * of status display.
   */
  protected ViStatusDisplay createStatusDisplay() {
    return new StatusDisplay();
  }

  /** Override this method to provide different cache implementation */
  protected TextViewCache createTextViewCache() {
    return new TextViewCache(this);
  }

  public void switchTo(JEditorPane editorPane) {
    if(ops == null) {
      createOps(editorPane);
    }
    attach(editorPane);
  }
  
  /**
   * Create methods to invoke and interact with editor pane actions.
   * Override for custom editor panes.
   */
  protected void createOps(JEditorPane editorPane) {
    ops = new Ops(this);
    ops.init(editorPane);
  }

  public void attach(JEditorPane editorPane) {
    if(this.editorPane == editorPane) {
      return;
    }
    detach();	// the old editorPane
    this.editorPane = editorPane;
    expectedCaretPosition = -1;
    cache.attach(editorPane);
  }

  public void detach() {
    cache.detach(editorPane);
    editorPane = null;
    // NEEDSWORK: more to do?
  }

  public JEditorPane getEditorComponent() {
    return editorPane;
  }

  /**
   * Use the document in default implementation.
   * @return opaque FileObject backing this EditorPane */
  public Object getFileObject() {
    return getEditorComponent().getDocument();
  }

  public void insertNewLine() {
    ops.xop(TextOps.INSERT_NEW_LINE);
  }

  public void insertTab() {
    expectedCaretPosition++;
    ops.xop(TextOps.INSERT_TAB);
  }

  public void replaceChar(int c, boolean advanceCursor) {
    if( ! editorPane.isEditable()) {
      Util.vim_beep();
      return;
    }
    oneCharArray[0] = (char)c;
    String s = new String(oneCharArray);
    int offset = editorPane.getCaretPosition();

    Document doc = getDoc();
    try {
      doc.remove(offset, 1);
      expectedCaretPosition++;
      doc.insertString(offset, s, null);
      if(advanceCursor) {
	offset++;
      }
      setCaretPosition(offset);// also clears the selection
    } catch(BadLocationException e) {
      Util.vim_beep();
    }
  }

  public void replaceString(int start, int end, String s) {
    if( ! editorPane.isEditable()) {
      Util.vim_beep();
      return;
    }
    Document doc = getDoc();
    try {
      if(start != end) {
	doc.remove(start, end - start);
      }
      doc.insertString(start, s, null);
    } catch(BadLocationException e) {
      Util.vim_beep();
    }
  }

  public void deleteChar(int start, int end) {
    if( ! editorPane.isEditable()) {
      Util.vim_beep();
      return;
    }
    Document doc = getDoc();
    try {
      doc.remove(start, end - start);
    } catch(BadLocationException e) {
      Util.vim_beep();
    }
  }

  public void deletePreviousChar() {
    if( ! editorPane.isEditable()) {
      Util.vim_beep();
      return;
    }
    ops.xop(TextOps.DELETE_PREVIOUS_CHAR);
  }

  public void insertText(int offset, String s) {
    if( ! editorPane.isEditable()) {
      Util.vim_beep();
      return;
    }
    /* ******************************************
    setCaretPosition(offset);
    ops.xop(TextOps.INSERT_TEXT, s);
    ******************************************/
    expectedCaretPosition += s.length();
    Document doc = getDoc();
    try {
      doc.insertString(offset, s, null);
    } catch(BadLocationException e) {
      Util.vim_beep();
    }
  }

  /**
   * insert character at cursor position. For some characters
   * special actions may be taken.
   */
  public void insertChar(int c) {
    if(c == '\t') {
      insertTab();
      return;
    }
    insertTypedChar((char)c);
  }

  /**
   * Add a character verbatim to the window.
   */
  public void insertTypedChar(char c) {
    oneCharArray[0] = (char)c;
    expectedCaretPosition++;
    ops.xop(TextOps.KEY_TYPED, new String(oneCharArray));
  }

  public void undo(){
  }

  public void redo() {
  }

  public String getText(int offset, int length) throws BadLocationException {
    return cache.getDoc().getText(offset, length);
  }

  public int getCaretPosition() {
    return editorPane.getCaretPosition();
  }

  public void setCaretPosition(int offset) {
    expectedCaretPosition = offset;
    editorPane.setCaretPosition(offset);
  }

  public void setCaretPosition(int lnum, int col) {
    Element elem = cache.getLineElement(lnum);
    setCaretPosition(elem.getStartOffset() + col);
  }

  public void setSelect(int dot, int mark) {
    Caret c = editorPane.getCaret();
    c.setDot(mark);
    c.moveDot(dot);
  }

  public void findMatch() {
    Util.vim_beep();
  }

  public void computeCursorPosition(MutableInt offset,
			       MutableInt line,
			       MutableInt column)
  {
    int o = getCaretPosition();
    computeCursorPosition(o, line, column);
    offset.setValue(o);
  }

  public void computeCursorPosition(int offset,
			       MutableInt line,
			       MutableInt column)
  {
    // NEEDSWORK: computeCursorPosition use the cache

    Document doc = getDoc();
    Element root = doc.getDefaultRootElement();
    int idx =  root.getElementIndex(offset);
    Element elem =  root.getElement(idx);

    line.setValue(idx + 1);
    column.setValue(offset - elem.getStartOffset());
  }

  public int getLineNumber(int offset) {
    return getElemIndex(offset) + 1;
  }

  public int getColumnNumber(int offset) {
    Element elem = getElem(offset);
    return offset - elem.getStartOffset();
  }

  /** @return the starting offset of the line */
  public int getLineStartOffset(int line) {
    return getLineElement(line).getStartOffset();
  }

  /** @return the starting offset of the line */
  public int getLineEndOffset(int line) {
    return getLineElement(line).getEndOffset();
  }

  public int getLineStartOffsetFromOffset(int offset) {
    Element elem = getElem(offset);
    return elem.getStartOffset();
  }

  public int getLineEndOffsetFromOffset(int offset) {
    Element elem = getElem(offset);
    return elem.getEndOffset();
  }

  public int getLineCount() {
    return getDoc().getDefaultRootElement().getElementCount();
  }

  public int getViewTopLine() {
    return cache.getViewTopLine();
  }

  public int getViewBottomLine() {
    return cache.getViewBottomLine();
  }

  public void setViewTopLine(int line) {
    cache.setViewTopLine(line);
  }

  public int getViewBlankLines() {
    return cache.getViewBlankLines();
  }

  public int getViewLines() {
    return cache.getViewLines();
  }

  /** Scroll down (n_lines positive) or up (n_lines negative) the
   * specified number of lines.
   */
  public void scroll(int n_lines) {
    Point pt = cache.getViewport().getViewPosition();
    pt.translate(0, n_lines * cache.getFheight());
    cache.getViewport().setViewPosition(pt);
  }

  final public Segment getLineSegment(int lnum) {
    return cache.getLineSegment(lnum);
  }

  final public Element getLineElement(int lnum) {
    return cache.getLineElement(lnum);
  }

  /**
   * Set a mark at the specified offset.
   * @param global_mark if false then it is a mark within a file, otherwise
   * it is a file mark and is valid between files.
   */
  public void setMarkOffset(ViMark mark_arg, int offset, boolean global_mark) {
    Mark mark = (Mark) mark_arg;
    mark.setOffset(offset, this);
  }

  public ViMark[] createMarks(int n_mark) {
    ViMark[] mark = new ViMark[n_mark];
    for(int i = 0; i < n_mark; i++) {
      mark[i] = new Mark();
    }
    return mark;
  }

  public ViOutputStream createOutputStream(Object type, Object info) {
    return new DefaultOutputStream(this, type, info);
  }

  public void updateCursor(ViCursor cursor) {
    if(editorPane == null) {
      return; // NEEDSWORK: was getting some null pointer stuff here
    }
    Caret caret = editorPane.getCaret();
    if(caret instanceof ViCaret) {
      ((ViCaret)caret).setCursor(cursor);
    }
  }

  private static boolean inUndo;
  public void beginUndo() {
    if(inUndo) {
      ViManager.dumpStack();
    }
    inUndo = true;
    // NEEDSWORK: standalone
  }

  public void endUndo() {
    if( ! inUndo) {
      ViManager.dumpStack();
    }
    inUndo = false;
    // NEEDSWORK: standalone
  }

  public boolean isInUndo() {
    return inUndo;
  }

  /** Quit editing window. Can close last view.
   */
  public void win_quit() {
    // NEEDSWORK: standalone
  }

  /** Split this window.
   * @param n the size of the new window.
   */
  public void win_split(int n) {
    // NEEDSWORK: standalone
  }

  /** Close this window
   * @param freeBuf true if the related buffer may be freed
   */
  public void win_close(boolean freeBuf) {
    // NEEDSWORK: standalone
  }

  /** Close other windows
   * @param forceit true if always hide all other windows
   */
  public void win_close_others(boolean forceit) {
    // NEEDSWORK: standalone
  }

  /** Goto the indicated buffer.
   * @param n the index of the window to make current
   */
  public void win_goto(int n) {
    // NEEDSWORK: standalone
  }

  /** Cycle to the indicated buffer.
   * @param n the positive/negative number of windows to cycle.
   */
  public void win_cycle(int n) {
    // NEEDSWORK: standalone
  }

  public ViStatusDisplay getStatusDisplay() {
    return statusDisplay;
  }

  public void displayFileInfo() {
    StringBuffer sb = new StringBuffer();
    sb.append("\"" + getDisplayFileName() + "\"");
    int l = getLineCount();
    //sb.append(" " + l + " line" + Misc.plural(l));
    sb.append(" line " + cache.getCursor().getLine());
    sb.append(" of " + getLineCount());
    sb.append(" --" + (int)((cache.getCursor().getLine() * 100)
			      / getLineCount()) + "%--");
    sb.append(" col " + cache.getCursor().getColumn());
    getStatusDisplay().displayStatusMessage(sb.toString());
  }

  public TextOps getOp() {
    return ops;
  }

  public String getDisplayFileName() {
    return "xxx";
  }

  protected final Document getDoc() {
    // return editorPane.getDocument();
    return cache.getDoc();
  }

  /** @return the element index from root which contains the offset */
  protected int getElemIndex(int offset) {
    Document doc = getDoc();
    Element root = doc.getDefaultRootElement();
    return root.getElementIndex(offset);
  }

  /** @return the element which contains the offset */
  protected Element getElem(int offset) {
    Element elem = cache.getCurrentLineElement();
    if(elem != null
       	&& elem.getStartOffset() <= offset && offset < elem.getEndOffset()) {
      return elem;
    }
    Element root = getDoc().getDefaultRootElement();
    return root.getElement(root.getElementIndex(offset));
  }
}
