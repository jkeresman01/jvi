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

import java.awt.Component;
import java.awt.geom.Rectangle2D;

import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.lib.MutableInt;


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
 */

public interface ViTextView extends ViOptionBag {
  // text fold operations
  public enum FOLDOP { CLOSE,           // zc
                       CLOSE_R,         // zC
                       OPEN,            // zo
                       OPEN_R,          // zO
                       CLOSE_ALL,       // zM
                       OPEN_ALL,        // zR
  }

  /** anonymous mark operations */
  public enum MARKOP { TOGGLE, NEXT, PREV }

  /** jump list operations */
  public enum JLOP { NEXT_JUMP, PREV_JUMP, NEXT_CHANGE, PREV_CHANGE }

  /** tags and tagstack operations */
  public enum TAGOP { OLDER, NEWER }

  /** word match operations */
  public enum WMOP { NEXT_WORD_MATCH, PREV_WORD_MATCH }

  /** */
  public enum DIR { FORWARD, BACKWARD }

  public enum EDGE { LEFT, RIGHT, MIDDLE }

  public enum HSCROLL { COUNT, HALF, CURSOR }

  public enum HDIR { LEFT, RIGHT }

  public enum Direction {
      LEFT, RIGHT, UP, DOWN;

      public Orientation getOrientation() {
          switch(this) {
              case LEFT:
              case RIGHT:     return Orientation.LEFT_RIGHT;
              case UP:
              case DOWN:
              default:        return Orientation.UP_DOWN;
          }
      }

      public String getSplitSide() {
          // From JSplitPane constant for TOP/BOTTOM/LEFT/RIGHT
          switch(this) {
              case LEFT:      return "left";
              case RIGHT:     return "right";
              case UP:        return "top";
              case DOWN:
              default:        return "bottom";
          }
      }

      public Direction getOpposite()
      {
          switch(this) {
              case LEFT:      return RIGHT;
              case RIGHT:     return LEFT;
              case UP:        return DOWN;
              case DOWN:
              default:        return UP;
          }
      }

      /**
       * @return clockwise 90 degree direction
       */
      public Direction getClockwise()
      {
          switch(this) {
              case LEFT:      return UP;
              case UP:        return RIGHT;
              case RIGHT:     return DOWN;
              case DOWN:
              default:        return LEFT;
          }
      }
  }

  public enum Orientation { LEFT_RIGHT, UP_DOWN }

  /** Used in conjunction with Orientation. */
  public enum SIZOP { SET, ADJUST, SAME }

  /**
   * NOTE: this works only once.
   * @return create the ViFPOS that corresponds to the screens caret.
   */
  public ViFPOS createWCursor();

  /**
   * Something external, like a mouse click, wants to set the cursor
   * at the specified position. It may be off the end of the line,
   * adjust if needed to nearest last char of line.
   * @param offset target position of the cursor.
   * @return valid position
   */
  public int validateCursorPosition(int offset);

  /** convenience method */
  public ViAppView getAppView();

  /** @return the associated Buffer */
  public Buffer getBuffer();

  /** @return the underlying text component */
  public Component getEditor();

  /** ring the bell, take visual bell options into account */
  public void bell();

  /** @return tab info for container holding this text editor */
  default public ViTabInfo getTabInfo() {
    return ViTabInfo.getDebugTabInfo();
  };

  default public void tab_move(int tabNumber, ViTabInfo ti) {
        Msg.emsg("tab_move(%d) NIMP", tabNumber);
  }

  default public void tab_goto(int tabNumber, ViTabInfo ti) {
        Msg.emsg("tab_goto(%d) NIMP", tabNumber);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Text Modification methods
  // They all fail if !isEditable
  //

  /** convenience method */
  public void xact(String actionName);

  /** Can the editor text be changed */
  public boolean isEditable();

  /** Insert new line at current position */
  public void insertNewLine();

  /** Open line is like insertNewLine, but is does autoindent dance */
  public boolean openNewLine(DIR op);

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


  //////// DEPRECATED SHOULD BE IN BUFFER ////////
  // NEEDSWORK: get rid of the following three, if offset this must be Buffer.

  /** Replace indicated region with string */
  public void replaceString(int start, int end, String s);

  /** Delete a bunch of characters */
  public void deleteChar(int start, int end);

  /** insert text at specified location */
  public void insertText(int offset, String s);

  //////////////////////////////////////////////////////////////////////
  //
  //

  /** @return the offset of the text insertion caret */
  public int getCaretPosition();

  /** @return the offset of the text insertion caret */
  public int getMarkPosition();

  /** do a platform select a region of the screen */
  public void setSelection(int dot, int mark);

  /** is there a platform selection on the screen */
  public boolean hasSelection();

  /** clear a platform select, if any, on the screen, don't move the caret */
  public void clearSelection();


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
  public void foldOperation(FOLDOP op, int start, int end, boolean isVisual);
  public void foldOpenCursor(int line);
  public boolean hasAnyFolding();

  public void wordMatchOperation(WMOP op);

  //
  // Without other indiation, any Vp*Line refers to lines on the screen.
  // A viewLine is the linenumber when the document is layed-out given
  // the consideration of linewrapping and folding.
  // Not logical or document lines. This is an issue when line wrap is possible
  //

  /** @return the document line number of first visible line in viewport */
  public int getVpTopDocumentLine();

  /** @return the number of unused lines on the display */
  public int getVpBlankLines(); // NEEDSWORK: what about variable font

  /** @return the number of lines in window/viewport */
  public int getVpLines(); // NEEDSWORK: what about variable font

  /** cause the indicated document line to be displayed as top line in viewport. */
  public void setVpTopLine(int docLine);

  /** @return the line number of first visible line in window */
  public int getVpTopViewLine();

  /** cause the indicated line to be displayed as top line in viewport. */
  public void setVpTopViewLine(int viewLine);

  /** @return the line number of line *after* end of window */
  public int getVpBottomViewLine();

  /**
   * Assumes that if logical line is in a fold, then doesn't wrap.
   *
   * @return number of screen lines used by the logical line (vim's plines)
   */
  public int getCountViewLines(int logicalLine);

  /**
   * When this returns the same value as getVpLines() then no non-existent
   * lines can be displayed. If this returns getVpLines()/2 then the last
   * line of the file can be scrolled to the center of the screen.
   *
   * @return the number of lines that must be displayed in the window.
   */
  public int getRequiredVpLines();


  /** @return the line number of first visible line in window */
  public int getVpTopLogicalLine();

  /** cause the indicated line to be displayed as top line in viewport. */
  public void setVpTopLogicalLine(int logicalLine);

  /** @return the line number of line *after* end of window */
  public int getVpBottomLogicalLine();

  /**
   * If fpos out of sight, move rectangle into viewport.
   * Note the fpos may be changed by this method.
   * @return bounding box for specified position in the view port co-ords.
   */
  public Rectangle2D getVpLocation(ViFPOS fpos);



  /**
   * If there is no code folding, then the number of logical lines is equal
   * to the number of lines in the document. When some lines are folded,
   * the number of logical lines is smaller.
   * @return number of logical lines in the document
   */
  public int getLogicalLineCount();

  /**
   * Translate a document line number to a logical line number.
   * Logical line number takes folding into account.
   * @param docLine line number in the document
   * @return corresponding line number in the view
   */
  public int getLogicalLine(int docLine);

  /**
   * logical line considers folding, view line considers wrapping.
   *
   * @param viewLine line number when rendered
   * @return line number that takes folding into account
   */
  public int getLogicalLineFromViewLine(int viewLine);

  /**
   * logical line considers folding, view line considers wrapping.
   *
   * @param logicalLine line number that takes folding into account
   * @return viewLine line number if rendered
   */
  public int getViewLineFromLogicalLine(int logicalLine);

  /** @return viewLine of fpos */
  public int getViewLine(ViFPOS fpos);

  /**
   * Return TRUE if line "lnum" in the current window is part of a closed
   * fold.
   * When returning TRUE, *firstp and *lastp are set to the first and last
   * lnum of the sequence of folded lines (skipped when NULL).
   * 
   * This is verbatim the vim interface.
   */
  public boolean hasFolding(int docLine,
                            MutableInt pDocFirst, MutableInt pDocLast);

  /**
   * Do a cursor up/down, according to dir, based on screen
   * coordinates.
   * If start position is on a wrapped line then the new position may be
   * in the same logical line but a different screen line.
   * The fpos specifies the
   * starting position and returns the final position; the actual screen cursor
   * is not moved directly.
   * @param direction to move the cursor
   * @param distance number of lines to move
   * @param fpos input/output
   * @return true if the cursor completed all requested moves
   */
  public boolean viewLineUpDown(DIR dir, int distance, ViFPOS fpos);

  /**
   * For the line with fpos, calculate the character position in the screen row.
   * The resulting position stays on the same scren line. This is
   * relevant when the fpos is on a wrapped line.
   * Note that upon return fpos may point to a '\n'. // DONE
   * The fpos specifies the
   * starting position and is modified to return the final position;
   * the actual screen cursor is not moved directly.
   * @param fpos input/output
   * @return true if the fpos-cursor moved.
   */
  public boolean viewLineEdge(EDGE edge, ViFPOS fpos);

  /**
   * Horizontal scroll. move the viewport as specified.
   * If needed, move the cursor to keep it on screen.
   *
   * @param op
   * @param dir direction the viewport moves
   * @param count number of characters, only used for op == COUNT
   */
  public void hscroll(HSCROLL op, HDIR dir, int count);

  /**
   * Find the first character in the line, less than or equal to col,
   * which occupies the same x-position in the line. This situation may occur
   * when there is code folding; if code folding is not possible or the line
   * has no hidden characters, then col is returned. Typically a line with
   * folding is a run of visible characters followed by a bunch of
   * invisible characters that occupy the same position, there is some graphic
   * that represents the invisible characters; the column of the first
   * invisible character is commonly returned, then the cursor appears at the
   * beginning of the graphic. It is possible that an implementation might
   * return the last visible rather than the first invisible depending on
   * where the cursor can be positioned. Note that upon return fpos
   * map point to a '\n'
   *
   * @param lineOffset offset in document of first char of line
   * @param col target column for caret
   * @return first column offset followed by an invisible character
   */
  public int getFirstHiddenColumn(int lineOffset, int col);

  /** Reverse of getlogicalLine, convert logical line to document line */
  public int getDocLineOffset(int logicalLine);

  /** Reverse of getlogicalLine, convert logical line to document line */
  public int getDocLine(int logicalLine);



  /** establish all the listeners */
  public void attach();

  /** tear down all the listeners */
  public void detach();

  /** called after TV and Buffer are constructed */
  public void startup();

  /** attach Buffer to the text view */
  public void attachBuffer(Buffer buf);

  /** going away, do any remaining cleanup */
  public void shutdown();

  /** @return true if this text view is shutdown */
  public boolean isShutdown();

  /**
   * Possible to invoke jvi core from "outside",
   * must check if text view is ready.
   */
  public boolean isReady();

  /** Change the cursor shape */
  public void updateCursor(ViCaretStyle cursor);

  /** Quit editing window. Can close last view.
   */
  public void win_quit();

  /**
   * Split this window, creating new area in the indicated direction.
   * If av is null, then a clone of the current text view is in there.
   * window is
   * @param dir create new area in this direction
   * @param n the size of the new window, lines/cols.
   * @param av editor to put into new window, may be null
   */
  public void win_split(Direction dir, int n, ViAppView av);

  /**
   * Move this window, create a new one if no existing target
   * in that direction. When new window is created, use the
   * specified size as appropriate.
   * @param n size of a new window
   */
  public void win_move(Direction dir, int n);

  /**
   * Make another view of the editor as a tab.
   * Map this from ^WT
   * Much like ":tabnew %"
   * This could be first step in ^W^S type behavior.
   */
  public void win_clone();

  /**
   * Change the size of a window.
   * @param op how to change size, ADJUST incr/decr by n
   * @param orientation indicates changing number of rows or columns.
   * @param n set or adjust count, not used with SAME
   */
  public void win_size(SIZOP op, Orientation orientation, int n);

  /** Close this window. Does not close last view.
   * NOTE: freeBuf not used, no jVi concept of hidden buffers
   * @param freeBuf true if the related buffer may be freed
   */
  public void win_close(boolean freeBuf);

  /** Handle displayable editor state changes */
  public ViStatusDisplay getStatusDisplay();

  /**
   * Update the visual state (selection) of the window.
   */
  public void updateVisualState();

  /**
   * Update the highlight search state
   */
  public void updateHighlightSearchState();
}
// vi:sw=2 et
