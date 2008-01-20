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
package com.raelity.jvi;

import javax.swing.JEditorPane;


/**
 * The information needed by vim when running on
 * a GUI. We abstract this, rather than using
 * native swing, since different environments, e.g. JBuilder,
 * have their own abstractions. Some abstraction that presents
 * a line oriented interface is useful for vim.
 * <p>This has had a few "window" methods added to it, and it is
 * now the primary class referenced by most of the vi code. This
 * allows it to be G.curwin.
 * </p>
 * <p>
 * NEEDSWORK: get rid of JEditorPane reference, should not refer to swing.
 * </p>
 */

public interface ViTextView extends ViOptionBag {
  // text fold operations
  public static final int FOLDOP_CLOSE = 'c';
  public static final int FOLDOP_OPEN = 'o';
  public static final int FOLDOP_CLOSE_ALL = 'M';
  public static final int FOLDOP_OPEN_ALL = 'R';
  
  /** annonymous mark operations */
  public enum MARKOP { TOGGLE, NEXT, PREV }
  
  /** jump list operations */
  public enum JLOP { NEXT_JUMP, PREV_JUMP, NEXT_CHANGE, PREV_CHANGE }
  
  /** tags and tagstack operations */
  public enum TAGOP { OLDER, NEWER }

  /** move to other tab */
  public enum TABOP { NEXT_TAB, PREV_TAB }
  
  //
  // First the methods that make this look like a window
  // (maybe implement at some point)
  //
  /** @return the associated Window */
  public Window getWindow();
  public void setWindow(Window window);

  /** @return the current location of the cursor in the window */
  public ViFPOS getWCursor();

  public int getWCurswant();
  public void setWCurswant(int c);
  public boolean getWSetCurswant();
  public void setWSetCurswant(boolean f);

  public ViMark getPCMark();
  public ViMark getPrevPCMark();
  public void pushPCMark();
  public ViMark getMark(int i);

  public int getWPScroll();
  public void setWPScroll(int n);
  public boolean getWPList();
  public void setWPList(boolean f);

  //
  // and now the text view proper
  //

  /** @return the associated Buffer */
  public Buffer getBuffer();

  /** @return the underlying text component */
  public JEditorPane getEditorComponent();

  /** A text view is nomadic if it not attached to a main window.
   * It is commonly in a dialog. The editor has restricted capabilities.
   * For example, ":w" is not allowed.
   * 
   * This is a simple approach.
   */
  //public boolean isNomadic();

  //////////////////////////////////////////////////////////////////////
  //
  // Text Modification methods
  // They all fail if !isEditable
  //
  
  /** Can the editor text be changed */
  public boolean isEditable();

  /** Insert new line at current position */
  public void insertNewLine();

  /** Insert tab at current position */
  public void insertTab();

  /** Replace character at current cursor location with argument character */
  public void replaceChar(char c, boolean advanceCursor);

  /** Delete previous character (backspace). */
  public void deletePreviousChar();

  /** insert text at current cursor location.
   *  For some characters special actions may be taken
   */
  public void insertChar(char c);

  /** insert the char verbatim, no special actions */
  public void insertTypedChar(char c);

  // NEEDSWORK: get rid of the following three, if offset this must be Buffer.

  /** Replace indicated region with string */
  public void replaceString(int start, int end, String s);

  /** Delete a bunch of characters */
  public void deleteChar(int start, int end);

  /** insert text at specified location */
  public void insertText(int offset, String s);

  //
  //

  /** @return the offset of the text insertion caret */
  public int getCaretPosition();

  /** @return the offset of the text insertion caret */
  public int getMarkPosition();

  /** Determine cursor position, all args get set (call by reference). */
  public void computeCursorPosition(MutableInt offset,
				    MutableInt line,
				    MutableInt column);

  /** set the caret to the indicated position. */
  public void setCaretPosition(int offset);

  /** set the caret to the indicated position. */
  public void setCaretPosition(int lnum, int col);

  /** select a region of the screen */
  public void setSelect(int dot, int mark);

  /** clear the select, if any, on the screen, don't move the caret */
  public void clearSelect();



  /** undo a change */
  public void undo();

  /** redo a change */
  public void redo();


  
  /** Anonymous mark handling.
   * Count is the Nth mark forward, back. It is ignored by TOGGLE.
   */
  public void anonymousMark(MARKOP op, int count);
  
  /** find matching character for character under the cursor.
   * This is the '%' command. It is here to take advantage of
   * existing functionality in target environments.
   */
  public void findMatch();
  
  /** Jump to the definition of the identifier under the cursor. */
  public void jumpDefinition(String ident);
  
  /** Jump list handling */
  public void jumpList(JLOP op, int count);
  
  /** Perform the fold operation.  */
  public void foldOperation(int op);

  /** move to other file tab.<br/>
   * For NEXT,PREV if count == 0 then neighboring tab;
   * if count != 0 then countTh tab, where first is one
   */
  public void tabOperation(TABOP op, int count);



  /** @return the line number of first visible line in window */
  public int getViewTopLine();

  /** cause the idndicated line to be displayed as top line in view. */
  public void setViewTopLine(int line);

  /** @return the line number of line *after* end of window */
  public int getViewBottomLine();

  /** @return the number of unused lines on the display */
  public int getViewBlankLines();

  /** @return the number of lines in window */
  public int getViewLines();

  /** Scroll down (n_lines positive) or up (n_lines negative) the
   * specified number of lines.
   */
  public void scroll(int n_lines);

  /** skip cursor number of display lines, takes folding into account.
   * @return true if some vertical movement took place.
   */
  public boolean skipCoordLines(int n);

  /** @return the line number of first visible line in window */
  public int getViewCoordTopLine();

  /** cause the idndicated line to be displayed as top line in view. */
  public void setViewCoordTopLine(int line);

  /** @return the line number of line *after* end of window */
  public int getViewCoordBottomLine();

  /** */
  public int getCoordLineCount();

  /** */
  public int getCoordLine(int line);

  /** */
  public void setCursorCoordLine(int line, int col);

  /** */
  public int coladvanceCoord(int lineOffset, int col);



  /** establish all the listeners */
  public void attach();

  /** tear down all the listeners */
  public void detach();
  
  /** called after TV and Buffer are constructed */
  public void startup(Buffer buf);
  
  /** going away, do any remaining cleanup */
  public void shutdown();

  /** @return true if this text view is shutdown */
  public boolean isShutdown();

  /** Change the cursor shape */
  public void updateCursor(ViCursor cursor);

  /** Quit editing window. Can close last view.
   */
  public void win_quit();

  /** Split this window.
   * @param n the size of the new window.
   */
  public void win_split(int n);

  /** Close this window. Does not close last view.
   * @param freeBuf true if the related buffer may be freed
   */
  public void win_close(boolean freeBuf);

  /** Close other windows
   * @param forceit true if always hide all other windows
   */
  public void win_close_others(boolean forceit);

  /** Goto the indicated buffer.
   * @param n the index of the window to make current
   */
  public void win_goto(int n);

  /** Cycle to the indicated buffer.
   * @param n the positive/negative number of windows to cycle.
   */
  public void win_cycle(int n);

  /** Handle displayable editor state changes */
  public ViStatusDisplay getStatusDisplay();

  /**
   * Update the visual state (selection) of the window.
   */
  public void updateVisualState();
  
  /**
   * Update the hightlight search state
   */
  public void updateHighlightSearchState();
}
