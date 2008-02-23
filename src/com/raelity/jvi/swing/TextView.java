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
import java.util.Arrays;

import javax.swing.JEditorPane;

import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.ViCursor;
import com.raelity.jvi.Util;
import com.raelity.jvi.MutableInt;
import com.raelity.jvi.Window;
import com.raelity.jvi.*;
import com.raelity.text.TextUtil.MySegment;
import java.awt.Color;
import java.awt.Rectangle;

import static com.raelity.jvi.Constants.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

/**
 * Presents a swing editor interface for use with vi. There is
 * one of these for each JEditorPane;
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
  private static int gen;
  protected int mygen;

  private static MutableAttributeSet HIGHLIGHT = new SimpleAttributeSet();
  private static MutableAttributeSet UNHIGHLIGHT = new SimpleAttributeSet();
  static {
      StyleConstants.setBackground(HIGHLIGHT, Color.LIGHT_GRAY);
      StyleConstants.setBackground(UNHIGHLIGHT, Color.WHITE);
  }

  protected JEditorPane editorPane;
  protected DefaultBuffer buf;
  protected TextOps ops;
  protected TextViewCache cache;
  protected Window window;

  protected ViStatusDisplay statusDisplay;

  // NEEDSWORK: either get rid of this, or get it working.
  protected int expectedCaretPosition = -1;

  private CaretListener cursorSaveListener;

  private int lastDot;

  private Point point0; // NEEDSWORK: change if document changes

  public TextView(final JEditorPane editorPane) {
    this.editorPane = editorPane;
    try {
      point0 = editorPane.modelToView(0).getLocation();
    } catch (BadLocationException ex) {
      point0 = new Point(0,0);
    }
    mygen = ++gen;

    cursorSaveListener = new CaretListener() {
      public void caretUpdate(CaretEvent ce) {
        ViManager.caretUpdate(TextView.this, lastDot,
                              ce.getDot(), ce.getMark());
        lastDot = ce.getDot();
      }
    };
  }
  
  private void enableCursorSave() {
    lastDot = editorPane.getCaret().getDot();
    editorPane.removeCaretListener(cursorSaveListener);
    editorPane.addCaretListener(cursorSaveListener);
  }

  private void disableCursorSave() {
    editorPane.removeCaretListener(cursorSaveListener);
  }

  public void startup(Buffer buf) {
    this.buf = (DefaultBuffer)buf;
    if(cache == null)
      cache = createTextViewCache();
    if(statusDisplay == null)
      statusDisplay = createStatusDisplay();
    enableCursorSave();
  }
  
  public void shutdown() {
    disableCursorSave();
    if(G.dbgEditorActivation.getBoolean()) {
      assert buf == ViManager.getBuffer(getEditorComponent());
      if(buf.getShare() == 1) {
        System.err.println("TV.shutdown: LAST CLOSE");
      }
    }
    cache.shutdown(editorPane);
    ViManager.detached(editorPane);
    editorPane = null;
  }

  public boolean isShutdown() {
      return editorPane == null;
  }
  
  //
  // Declare the variables referenced as part of a ViOptionBag
  //

  /** jVi doesn't support this flag. Keep it as a per window flag like vim.
   * The platform may do something with it.
   */
  public boolean w_p_nu;
  
  public void viOptionSet(ViTextView tv, String name) {
  }

  public void activateOptions(ViTextView tv) {
    updateHighlightSearchState();
  }
  
  //
  //
  //

  public void setWindow(Window window) {
    this.window = window;
  }

  public Window getWindow() {
    return window;
  }

  //
  // Pretend a little to be a window
  //

  /** @return the current location of the cursor in the window,
      note that this cursor is dynamic as caret moves this gets
      updated.
   */
  public ViFPOS getWCursor() { return cache.getCursor(); }
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

  public final DefaultBuffer getBuffer() {
      return buf;
  }

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

  public void attach() {
    if(ops == null) {
      createOps(editorPane);
    }
    if(G.dbgEditorActivation.getBoolean()) {
      System.err.println("TV.attach: " + editorPane.hashCode());
    }
    expectedCaretPosition = -1;
    cache.attach(editorPane);
  }

  public void detach() {
    cache.detach(editorPane);
    
    ViManager.detached(editorPane); // NEEDSWORK: what's this for?
  }
  
  /**
   * Create methods to invoke and interact with editor pane actions.
   * May override for custom editor panes.
   */
  protected void createOps(JEditorPane editorPane) {
    ops = new Ops(this);
    ops.init(editorPane);
  }

  public JEditorPane getEditorComponent() {
    return editorPane;
  }
  
  /**
   * @return true if the text can be changed.
   */
  public boolean isEditable() {
    return editorPane.isEditable();
  }

  ////////////////////////////////////////////////////////////////////////
  //
  // Text modification methods.
  //
  // The text modifications are a bit jumbled. Some use actions and some go
  // to the buffer. Some use the cursor position and some use offsets.
  // All check isEditable.
  //
  // NEEDSWORK: consistent text modification methods.
  //            It will be difficult to clean this up paricularly because of the
  //            because of the dependency on actions, for example insertNewline
  //            must be used to get proper autoindent handling.
  //
  
  public void insertNewLine() {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    ops.xop(TextOps.INSERT_NEW_LINE); // NEEDSWORK: xop throws no exception
  }

  public void insertTab() {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    expectedCaretPosition++;
    ops.xop(TextOps.INSERT_TAB); // NEEDSWORK: xop throws no exception
  }

  public void replaceChar(char c, boolean advanceCursor) {
      if( ! isEditable()) {
          Util.vim_beep();
          return;
      }
      int offset = editorPane.getCaretPosition();
      getBuffer().replaceChar(offset, c);
      if(advanceCursor) {
          offset++;
      }
      setCaretPosition(offset);// also clears the selection
  }

  public void deletePreviousChar() {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    ops.xop(TextOps.DELETE_PREVIOUS_CHAR); // NEEDSWORK: xop throws no exception
  }

  /**
   * insert character at cursor position. For some characters
   * special actions may be taken.
   */
  public void insertChar(char c) {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    if(c == '\t') {
      insertTab();
      return;
    }
    insertTypedChar(c);
  }

  /**
   * Add a character verbatim to the window.
   */
  public void insertTypedChar(char c) {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    expectedCaretPosition++;
    ops.xop(TextOps.KEY_TYPED, String.valueOf(c)); // NEEDSWORK: xop throws no exception
  }

  //
  // NEEDSWORK: These three text modification methods take document offsets
  // unlike the rest of the modification methods which use the caret.
  // They should probably not be here. Currently they wrap some methods
  // in Buffer.
  //

  public void replaceString(int start, int end, String s) {
      if( ! isEditable()) {
          Util.vim_beep();
          return;
      }
      getBuffer().replaceString(start, end, s);
  }

  public void deleteChar(int start, int end) {
      if( ! isEditable()) {
          Util.vim_beep();
          return;
      }
      getBuffer().deleteChar(start, end);
  }

  public void insertText(int offset, String s) {
      if( ! isEditable()) {
          Util.vim_beep();
          return;
      }
      getBuffer().insertText(offset, s);
  }
  
  /**
   * <p>
   * Create an empty line, autoindented, either before or after current line.
   * This is simple algorithm; but it ignores guarded text issues
   * and code folding subtlties.
   * </p>
   * In this example, either the cursor was on line1 and do a NL_FORWARD,
   * or cursor on line2 and NL_BACKWARD.
   * The cursor is shown as '|' positioned where the newLine action will
   * open up a clean line with proper autoindent.
   * <pre>
   *     line1|\n
   *     line2\n
   * </pre>
   * This has problems if line1 is guarded text (write protected), since
   * it modifies that line. And issues with folding in that it will open
   * the fold since a folded line is modified.
   */
  public boolean openNewLine(NLOP op) {
      final ViFPOS cursor = getWCursor();
      if(op == NLOP.NL_BACKWARD && cursor.getLine() == 1) {
          // Special case if BACKWARD and at position zero of document.
          // set the caret position to 0 so that insert line on first line
          // works as well, set position just before new line of first line
          if(!Edit.canEdit(this, getBuffer(), 0))
              return false;
          setCaretPosition(0);
          insertNewLine();
          
          MySegment seg = getBuffer().getLineSegment(1);
          setCaretPosition(0 + Misc.coladvanceColumnIndex(MAXCOL, seg));
          return true;
      }

      int offset;
      if(op == NLOP.NL_FORWARD) {
          // after the current line,
          // handle current line might be a fold
          // int cmpOffset = getBuffer()
          //                   .getLineEndOffsetFromOffset(cursor.getOffset());
          offset = G.curwin.getBufferLineOffset(
                  G.curwin.getCoordLine(cursor.getLine()) + 1);
      } else {
          // before the current line
          offset = getBuffer().getLineStartOffsetFromOffset(cursor.getOffset());
      }
      // offset is after the newline where insert happens, backup the caret.
      // After the insert newline, caret is ready for input on blank line
      offset--;
      if(!Edit.canEdit(this, getBuffer(), 0))
          return false;
      setCaretPosition(offset);
      insertNewLine();
      return true;
  }
  
  
///////////////////////////////////////////////////////////////////////
//
  
  //
  // START BUFFER
  //
public void undo(){
      getBuffer().undo();
  }

  public void redo() {
      getBuffer().redo();
  }

  public String getText(int offset, int length) throws BadLocationException {
    return getBuffer().getText(offset, length);
  }
  //
  // END BUFFER
  //

  public int getCaretPosition() {
    return editorPane.getCaretPosition();
  }

  public int getMarkPosition() {
    return editorPane.getCaret().getMark();
  }

  public void setCaretPosition(int offset) {
    expectedCaretPosition = offset;
    editorPane.setCaretPosition(offset);
  }

  public void setCaretPosition(int lnum, int col) {
    Element elem = getLineElement(lnum);
    setCaretPosition(elem.getStartOffset() + col);
  }

  public void setSelect(int dot, int mark) {
    Caret c = editorPane.getCaret();
    c.setDot(mark);
    c.moveDot(dot);
  }

  public void clearSelect() {
    Caret c = editorPane.getCaret();
    c.setDot(c.getDot());
  }

  public void findMatch() {
    Util.vim_beep();
  }

  public void jumpDefinition(String ident) {
    Util.vim_beep();
  }
  
  public void anonymousMark(MARKOP op, int count) {
    Util.vim_beep();
  }

  public void jumpList(JLOP op, int count) {
    Util.vim_beep();
  }

  public void foldOperation(FOLDOP op) {
    Util.vim_beep();
  }

  public void foldOperation(FOLDOP op, int offset) {
    Util.vim_beep();
  }
  
  public void wordMatchOperation(WMOP op) {
    Util.vim_beep();
  }

  public void tabOperation(TABOP op, int count) {
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

    /*Document doc = getDoc();
    Element root = doc.getDefaultRootElement();
    int idx =  root.getElementIndex(offset);
    Element elem =  root.getElement(idx);*/

    //line.setValue(idx + 1);
    //column.setValue(offset - elem.getStartOffset());

    int lnum = getBuffer().getLineNumber(offset);
    line.setValue(lnum);
    column.setValue(offset - getBuffer().getLineStartOffset(lnum));
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

  public int getViewCoordTopLine() {
    if(!G.isCoordSkip.getBoolean())
      return getViewTopLine();
    int coordLine = getInternalCoordLine(getViewTopLine());
    if(G.dbgCoordSkip.getBoolean())
      System.err.println("getViewCoordTopLine: " + coordLine);
    return coordLine;
  }

  public void setViewCoordTopLine(int coordLine) {
    if(!G.isCoordSkip.getBoolean()) {
      setViewTopLine(coordLine);
      return;
    }
    //cache.setViewCoordTopLine(coordLine);
    
    //Point p = new Point(point0);
    //p.translate(0, (coordLine - 1) * cache.getFheight());
    Point p = new Point(0, (coordLine - 1) * cache.getFheight());
    // If point is after viewport top && within one char of top
    // then just return, to avoid wiggle
    int topDiff = p.y - cache.getViewport().getViewPosition().y;
    if(topDiff > 0 && topDiff < cache.getFheight())
        return;
    cache.getViewport().setViewPosition(p);
  }

  public int getViewCoordBlankLines() {
    int n;
    if(!G.isCoordSkip.getBoolean()) {
        n = getViewBlankLines();
    } else {
      n = getViewLines()
              - ((getViewCoordBottomLine()-1) - getViewCoordTopLine());
    }
    return n;
  }
  
  //
  // NOTE:
  //     in TextViewCache, viewBottomLine is not the same as getViewBottomLine
  //
  public int getViewCoordBottomLine() {
    int coordLine;
    if(!G.isCoordSkip.getBoolean())
      coordLine = getViewBottomLine();
    else {
      // NEEDSWORK: COORD consider blank lines on screen, coordLine past EOF
      coordLine = getInternalCoordLine(getViewTopLine()) + getViewLines();
      if(G.dbgCoordSkip.getBoolean())
        System.err.println("getViewCoordBottomLine: " + coordLine);
    }
    return coordLine; // NEEDSWORK: line past full line, see getViewBottomLine
  }

  public int getCoordLineCount() {
    if(!G.isCoordSkip.getBoolean())
      return getBuffer().getLineCount();
    int coordLine = 1;
    coordLine = getInternalCoordLine(getBuffer().getLineCount());
    if(G.dbgCoordSkip.getBoolean())
      System.err.println("getCoordLineCount: " + coordLine);
    return coordLine;
  }

  public int getCoordLine(int line) {
    if (!G.isCoordSkip.getBoolean())
      return line;
    int coordLine = getInternalCoordLine(line);
    if(G.dbgCoordSkip.getBoolean())
      System.err.println("getCoordLine: " + coordLine);
    return coordLine;
  }
  
  public int getBufferLineOffset(int coordLine) {
      if (!G.isCoordSkip.getBoolean()) {
          if(coordLine > getBuffer().getLineCount()) {
            return getBuffer().getLength() + 1;
          }
          return getBuffer().getLineStartOffset(coordLine);
      }
      Point p = new Point(point0);
      p.translate(0, (coordLine - 1) * cache.getFheight());
      int offset = getEditorComponent().viewToModel(p);
      Rectangle r1 = null;
      try {
          r1 = getEditorComponent().modelToView(offset);
      } catch (BadLocationException ex) {
          // should be impossible since viewtomodel return should be valid
          ex.printStackTrace();
      }
      if(p.y > r1.y) {
          // past end of file
          // System.err.println("past EOF: p " + p + ", r1 " + r1);
          offset = getBuffer().getLength() + 1;
      }
      return offset;
  }

  private int getInternalCoordLine(int line) {
    int coordLine = 1;
    try {
      int offset = getBuffer().getLineStartOffset(line);
      Rectangle lineRect = getEditorComponent().modelToView(offset);
      int yDiff = lineRect.y - point0.y;
      coordLine = yDiff / cache.getFheight() + 1;
      if(G.dbgCoordSkip.getBoolean())
        System.err.println(String.format(
                "\tgetInternalCoordLine: %d, line1: %d:%d, line %d:%d",
                coordLine, 1, point0.y, line, lineRect.y));;
    } catch (BadLocationException ex) {
      //Logger.getLogger(TextView.class.getName()).log(Level.SEVERE, null, ex);
    }
    return coordLine;
  }

  public void setCursorCoordLine(int coordLine, int col) {
    if (!G.isCoordSkip.getBoolean()) {
      setCaretPosition(coordLine, col);
      return;
    }
    Point p = new Point(point0);
    p.translate(0, (coordLine - 1) * cache.getFheight());
    int newOffset = getEditorComponent().viewToModel(p);
    //assert col == 0;
    setCaretPosition(newOffset + col);
  }

  public int coladvanceCoord(int lineOffset, int colIdx) {
    if (lineOffset < 0) {
      ViManager.dumpStack("invalid lineOffset");
      return 0;
    }

    // insure col is displayable (e.g. visual position not in fold)
    // check 'visual position after prev position is position we want'
    // in other words, getNextVisualPositionFrom(here - 1) == here
    // if not, we're in nether regions, return here - 1
    try {
      JEditorPane c = getEditorComponent();
      for (int i = 1; i <= colIdx; i++) {
        int thisVisualPosition = lineOffset + i;
        int nextVisualPosition = c.getUI().getNextVisualPositionFrom(
                c,
                thisVisualPosition - 1,
                javax.swing.text.Position.Bias.Forward,
                javax.swing.SwingConstants.EAST,
                new javax.swing.text.Position.Bias[1]); // used in jdk1.5
                //null); // may be null in jdk1.6
        if(thisVisualPosition != nextVisualPosition) {
          colIdx = i - 1;
          break;
        }
      }
    } catch (BadLocationException ex) {
      //Logger.getLogger(TextView.class.getName()).log(Level.SEVERE, null, ex);
      ex.printStackTrace();
    }
    return colIdx;
  }



  private Element getLineElement(int lnum) {
    return getBuffer().getLineElement(lnum);
  }

  public void updateCursor(ViCursor cursor) {
    if(isShutdown()) {
      return; // NEEDSWORK: was getting some null pointer stuff here
    }
    Caret caret = editorPane.getCaret();
    if(caret instanceof ViCaret) {
      ((ViCaret)caret).setCursor(cursor);
    }
  }

  /** Quit editing window. Can close last view.
   */
  public void win_quit() {
    Msg.emsg("win_quit not implemented");
  }

  /** Split this window.
   * @param n the size of the new window.
   */
  public void win_split(int n) {
    Msg.emsg("win_split not implemented");
  }

  /** Close this window
   * @param freeBuf true if the related buffer may be freed
   */
  public void win_close(boolean freeBuf) {
    Msg.emsg("win_close not implemented");
  }

  /** Close other windows
   * @param forceit true if always hide all other windows
   */
  public void win_close_others(boolean forceit) {
    Msg.emsg("win_close_others not implemented");
  }

  /** Goto the indicated buffer.
   * @param n the index of the window to make current
   */
  public void win_goto(int n) {
    Msg.emsg("win_goto not implemented");
  }

  /** Cycle to the indicated buffer.
   * @param n the positive/negative number of windows to cycle.
   */
  public void win_cycle(int n) {
    Msg.emsg("win_cycle not implemented");
  }

  public ViStatusDisplay getStatusDisplay() {
    return statusDisplay;
  }
  
  public TextOps getOps() {
    return ops;
  }
  
  //////////////////////////////////////////////////////////////////////
  //
  // Visual Select
  //
 
  /**
   * Update the selection highlight.
   */

  public void updateVisualState() {
    // Following stuff standalone, only for debugging
    // (should really be a seperate method)
    if(ViManager.getViFactory().isStandalone()) {
      if (!G.VIsual_active) {
        try {
          unhighlight(new int[]{getMark('<').getOffset(),
                                getMark('>').getOffset(), -1, -1});
        } catch(Exception e) {
          unhighlight(new int[]{0, editorPane.getText().length(),
                                -1, -1});
        }
      }
      int[] b = getBuffer().getVisualSelectBlocks(this, 0, Integer.MAX_VALUE);
      //dumpBlocks("blocks", b);
      highlight(b);
    }
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Highlight Search
  //
  
  public void updateHighlightSearchState() {
    applyBackground(new int[] {0, getBuffer().getLength(), -1, -1},
                    UNHIGHLIGHT);
    
    if(!Options.doHighlightSearch())
      return;
    
    int[] b = getBuffer().getHighlightSearchBlocks(0, getBuffer().getLength());
    applyBackground(b, HIGHLIGHT);
  }
  
  //////////////////////////////////////////////////////////////////////
  //
  // StyledDocument highlight methods
  //

  private void unhighlight(int[] blocks) {
      applyBackground(blocks, UNHIGHLIGHT);
  }

  private int[] previousAppliedHighlight = null;
  private void highlight(int[] blocks) {
      if (previousAppliedHighlight != null && !Arrays.equals(previousAppliedHighlight, blocks)) {
          unhighlight(previousAppliedHighlight);
      }
      applyBackground(blocks, HIGHLIGHT);
      previousAppliedHighlight = blocks;
  }

  protected void applyBackground(int[] blocks, MutableAttributeSet mas) {
      StyledDocument document  = (StyledDocument) editorPane.getDocument();
      for (int i = 0; i < blocks.length; i+=2) {
          int start = blocks[i];
          int end = blocks[i+1];
          if (start == -1 && end == -1) { // break
              return;
          }
          if (start > end) {
              int tmp = start;
              start = end;
              end = tmp;
        }
        document.setCharacterAttributes(start, end - start, mas, false);
          // update styled editor kit with new attributes to overcome paint errors
          StyledEditorKit k = (StyledEditorKit) editorPane.getEditorKit();
          MutableAttributeSet inputAttrs = k.getInputAttributes();
          inputAttrs.addAttributes(mas);
      }
  }
}
