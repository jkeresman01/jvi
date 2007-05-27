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

import java.awt.event.KeyEvent;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.Toolkit;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.text.BadLocationException;
import javax.swing.text.Keymap;
import javax.swing.KeyStroke;

import static com.raelity.jvi.Constants.*;
import static com.raelity.jvi.KeyDefs.*;

import com.raelity.text.TextUtil.MySegment;

public class Misc implements ClipboardOwner {

  static ClipboardOwner clipOwner = new Misc();

  //////////////////////////////////////////////////////////////////
  //
  // "misc1.c"
  //

  /**
   * count the size of the indent in the current line
   */
  static int get_indent() {
      MySegment seg = G.curwin.getLineSegment(G.curwin.getWCursor().getLine());
      return get_indent_str(seg);
  }

  /**
   * count the size of the indent in line "lnum"
   */
  static int get_indent_lnum(int lnum) {
      MySegment seg = G.curwin.getLineSegment(lnum);
      return get_indent_str(seg);
  }

  /**
   * count the size of the indent in line "ptr"
   */
  private static int get_indent_str(MySegment seg) {
      int	    count = 0;

      int ptr = seg.offset;
      for ( ; ptr < seg.offset + seg.count; ++ptr) {
	  if (seg.array[ptr] == TAB)    // count a tab for what it is worth
	      count += G.curbuf.b_p_ts - (count % G.curbuf.b_p_ts);
	  else if (seg.array[ptr] == ' ')
	      ++count;		// count a space for one
	  else
	      break;
      }
      return count;
  }
  
  /**
   * Return column position of parenthesis on argument line.
   * If no paren then return -1.
   * <br>
   * At some point in the future may want a version that find
   * the paren (any specified character) before/after some given
   * column positon.
   */
  static int findParen(int lnum, int fromIndent, int dir) {
    if(lnum < 1 || lnum > G.curwin.getLineCount()) {
      return -1;
    }
    MySegment seg = G.curwin.getLineSegment(lnum);
    return findParen(seg, fromIndent, dir);
  }
  
  /**
   * @param seg
   * @param fromIndent start looking from this position
   * @param dir FORWARD or BACKWARD
   */
  static int findParen(MySegment seg, int fromIndent, int dir) {
    boolean found = false;
    int	    count = 0;
    int     prev_paren = -1;
    
    if(dir == BACKWARD) {
      fromIndent -= 1;
    }

    // go forward until count is at fromIndent
    int ptr = seg.offset;
    for ( ; ptr < seg.offset + seg.count; ++ptr) {
      if(count >= fromIndent) {
	break;
      }
      if (seg.array[ptr] == '(') {
	// keep track of last paren seen
	prev_paren = count;
      }
      if (seg.array[ptr] == TAB) {  // count a tab for what it is worth
	count += G.curbuf.b_p_ts - (count % G.curbuf.b_p_ts);
      } else {
	++count;
      }
    }
    
    // if looking backward, then we're done. prev_paren is position
    if(dir == BACKWARD) {
      return prev_paren;
    }
    
    // ptr = seg.offset;
    //ptr++;
    for ( ; ptr < seg.offset + seg.count; ++ptr) {
      if (seg.array[ptr] == '(') {
	found = true;
	break;
      } else if (seg.array[ptr] == TAB)    // count a tab for what it is worth
	count += G.curbuf.b_p_ts - (count % G.curbuf.b_p_ts);
      else
	++count;
    }
    return found ? count : -1;
  }
  
  static int findFirstNonBlank(int lnum, int fromIndent, int dir) {
    if(lnum < 1 || lnum > G.curwin.getLineCount()) {
      return -1;
    }
    MySegment seg = G.curwin.getLineSegment(lnum);
    return findFirstNonBlank(seg, fromIndent, dir);
  }
  
  /**
   * @param seg
   * @param fromIndent start looking from this position
   * @param dir FORWARD or BACKWARD
   */
  static int findFirstNonBlank(MySegment seg, int fromIndent, int dir) {
    boolean found = false;
    int	    count = 0;
    int     non_blank = -1;
    
    if(dir == BACKWARD) {
      fromIndent -= 1;
    }

    // go forward until count is at fromIndent
    int ptr = seg.offset;
    for ( ; ptr < seg.offset + seg.count; ++ptr) {
      if(count >= fromIndent) {
	break;
      }
      if(seg.array[ptr] != ' ' && seg.array[ptr] != TAB) {
          non_blank = count;
          break;
      }
      if (seg.array[ptr] == TAB) {  // count a tab for what it is worth
	count += G.curbuf.b_p_ts - (count % G.curbuf.b_p_ts);
      } else {
	++count;
      }
    }
    
    // if looking backward, then we're done.
    if(dir == BACKWARD) {
      return non_blank;
    }
    
    for ( ; ptr < seg.offset + seg.count; ++ptr) {
      if(seg.array[ptr] != ' ' && seg.array[ptr] != TAB) {
	found = true;
	break;
      }
      if (seg.array[ptr] == TAB)    // count a tab for what it is worth
	count += G.curbuf.b_p_ts - (count % G.curbuf.b_p_ts);
      else
	++count;
    }
    return found ? count : -1;
    
    /*
    if(non_blank < fromIndent)
        return -1;
    return non_blank; */
  }

  /**
   * set the indent of the current line
   * leaves the cursor on the first non-blank in the line
   */
  static void set_indent(int size, boolean del_first) {
    int		oldstate = G.State;
    int		c;

    G.State = INSERT;		    // don't want REPLACE for State
    int col = 0;
    MySegment seg = G.curwin.getLineSegment(
				    G.curwin.getWCursor().getLine());
    if (del_first) {		    // delete old indent
      // vim_iswhite() is a define!
      while(vim_iswhite(seg.array[col + seg.offset])) {
	col++;
      }
      // col is char past last whitespace
    }
    StringBuffer sb = new StringBuffer();
    if (!G.curbuf.b_p_et) {	    // if 'expandtab' is set, don't use TABs
      while (size >= G.curbuf.b_p_ts) {
	// NEEDSWORK:  how did b_p_et get set, dont expand tabs for now
	sb.append((char)TAB);
	size -= G.curbuf.b_p_ts;
      }
    }
    while (size > 0) {
      sb.append(' ');
      --size;
    }
    int offset = G.curwin.getLineStartOffset(
				    G.curwin.getWCursor().getLine());
    G.curwin.replaceString(offset, offset + col, sb.toString());
    G.State = oldstate;
  }

  /**
   * open_line: Add a new line below or above the current line.
   *<br>
   * For VREPLACE mode, we only add a new line when we get to the end of the
   * file, otherwise we just start replacing the next line.
   *<p>
   * Caller must take care of undo.  Since VREPLACE may affect any number of
   * lines however, it may call u_save_cursor() again when starting to change a
   * new line.
   * </p><p>
   * NOTE: for jVI position the cursor if needed and invoke insertNewLine
   * </p>
   *
   * @return TRUE for success, FALSE for failure
   */
  static boolean open_line(int dir, boolean redraw, boolean del_spaces,
		    int old_indent)
  {
    boolean insert_mode = true;
    ViFPOS fpos = G.curwin.getWCursor();
    if(G.State != INSERT && G.State != REPLACE) {
      insert_mode = false;
      if(dir == BACKWARD && fpos.getLine() == 1) {
        // first set the caret position to 0 so that insert line on first line works as well
	// set position just before new line of first line
	G.curwin.setCaretPosition(0);
	// Special case if BACKWARD and at position zero of document.
	G.curwin.insertNewLine();
	MySegment seg = G.curwin.getLineSegment(1);
	G.curwin.setCaretPosition(
		      0 + coladvanceColumnIndex(MAXCOL, seg));
    G.did_ai = true;
	return true;
      }
      // position cursor according to dir, probably an 'O' or 'o' command
      int offset;
      if(dir == FORWARD) {
	offset = G.curwin.getLineEndOffsetFromOffset(fpos.getOffset());
      } else {
	offset = G.curwin.getLineStartOffsetFromOffset(fpos.getOffset());
      }
      // offset is after the newline where insert happens, backup the caret.
      // After the insert newline, caret is ready for input on blank line
      offset--;
      G.curwin.setCaretPosition(offset);
    }
    G.curwin.insertNewLine();
    G.did_ai = true;
    return true;
  }

  /**
   * plines_check(p) - like plines(), but return MAXCOL for invalid lnum.
   */
  static int plines_check(int p) {
      if (p < 1 || p > G.curwin.getLineCount())
	  return MAXCOL;
      return 1;
      // return plines_win(curwin, p); // IN plines_check, assume nowrap
  }

  /**
   * plines(p) - return the number of physical screen lines taken by line 'p'.
   */
  static int plines(int p) {
      return 1;
      // return plines_win(curwin, p); // IN plines_check, assume nowrap
  }

  /**
   * @param nlines number of lines to delete
   * @param dowindow if true, update the window
   * @param undow if true, prepare for undo
   */
  static void del_lines(int nlines, boolean dowindow, boolean undo) {
    if(nlines <= 0) {
      return;
    }
    ViFPOS fpos = G.curwin.getWCursor();
    int stopline = fpos.getLine() + nlines - 1;
    int start = G.curwin.getLineStartOffsetFromOffset(fpos.getOffset());
    int end = G.curwin.getLineEndOffset(stopline);
    if(stopline >= G.curwin.getLineCount()) {
      // deleting the last line is tricky
      --end; // can not delete the final new line
      if(start > 0) {
        --start; // delete the newline before the starting line to componsate
      }
    }
    G.curwin.deleteChar(start, end);
  }

  /** @return character at the position */
  static int gchar_pos(ViFPOS pos) {
    return Util.getCharAt(pos.getOffset());
  }

  /** @return character at the position */
  static int gchar_cursor() {
    ViFPOS pos = G.curwin.getWCursor();
    return gchar_pos(pos);
  }

  /**
   * Write a character at the current cursor position.
   * It is directly written into the block.
   */
  static void pchar_cursor(int c) {
    G.curwin.replaceChar(c, false);
  }

  /**
   * If the file is readonly, give a warning message with the first change.
   * Don't do this for autocommands.
   * Don't use emsg(), because it flushes the macro buffer.
   * If we have undone all changes b_changed will be FALSE, but b_did_warn
   * will be TRUE.
   */
  static void change_warning(int col) {
    // Normal.do_op("change_warning");	// NEEDSWORK: implement something
  }

  /**
   * When extra == 0: Return TRUE if the cursor is before or on the first
   *		    non-blank in the line.
   * <br>
   * When extra == 1: Return TRUE if the cursor is before the first non-blank in
   *		    the line.
   */
  static boolean inindent(int extra) {
      int	col;

      MySegment seg = G.curwin.getLineSegment(G.curwin.getWCursor().getLine());
      for(col = 0; vim_iswhite(seg.array[seg.offset + col]); ++col);

      if (col >= G.curwin.getWCursor().getColumn() + extra)
          return true;
      else
          return false;
  }

  public static String plural(int n) {
    return n == 1 ? "" : "s";
  }

  static void ins_char(int c) {
    int extra;
    int curChar = 0;

    if (G.State != REPLACE || (curChar = Util.getChar()) == '\n')
	extra = 1;
    else
	extra = 0;

    // A character has to be put on the replace stack if there is a
    // character that is replaced, so it can be put back when BS is used.
    if (G.State == REPLACE) {
      Edit.replace_push(NUL);
      if (extra == 0)
	Edit.replace_push(curChar);
    }

    if(extra == 0) {
      // overwrite current character, no extra chars put into line
      G.curwin.replaceChar(c, true);
    } else {
      G.curwin.insertChar(c);
    }
  }

  static int del_char(boolean fixpos) {

    // return del_chars(1, fixpos);
    // just implement delete a single character

    ViFPOS fpos = G.curwin.getWCursor();

    // Can't do anything when the cursor is on the NUL after the line.
    if(Util.getChar() == '\n') {
      return FAIL;
    }

    // NEEDSWORK: could use a deleteNextChar
    int offset = fpos.getOffset();
    G.curwin.deleteChar(offset, offset+1);
    return OK;
  }

  static int del_chars(int count, boolean fixpos) {
    ViFPOS cursor = G.curwin.getWCursor();
    int col = cursor.getColumn();
    MySegment oldp = Util.ml_get(cursor.getLine());
    int oldlen = oldp.count -1; // exclude the newline

    // Can't do anything when the cursor is on the NUL after the line.
    if(col >= oldlen)
      return FAIL;

    int oldOffset = cursor.getOffset();

    // When count is too big, reduce it.
    int movelen = oldlen - col - count + 1; // includes trailing NUL
    if (movelen <= 1) {
      //
      // If we just took off the last character of a non-blank line, and
      // fixpos is TRUE, we don't want to end up positioned at the NUL.
      //
      if (col > 0 && fixpos)
        G.curwin.setCaretPosition(oldOffset -1); //--curwin->w_cursor.col;
      count = oldlen - col;
      //movelen = 1;
    }
    G.curwin.deleteChar(oldOffset, oldOffset + count);

    return OK;
  }

  /** This method not in vim,
   * but we cant just set a line number in the window struct.
   * If the target line is within one half screen of being visible
   * then scroll to the line and the line will be near the top
   * or bottom as needed, otherwise center the target line on the screen.
   */
  static void gotoLine(int line, int flag) {
    if(G.p_so.getInteger() != 0) {
      gotoLine_scrolloff(line, flag);
      return;
    }
    if(line > G.curwin.getLineCount()) {
      line = G.curwin.getLineCount();
    }
    MySegment seg = G.curwin.getLineSegment(line);
    int col;
    if(flag < 0) {
      col = coladvanceColumnIndex(seg);
    } else {
      // from nv_goto
      col = Edit.beginlineColumnIndex(flag, seg);
    }

    // if target line is less than half a screen away from
    // being visible, then just let it scroll, otherwise
    // center the target line

    int center = G.curwin.getViewTopLine()
	      	   + G.curwin.getViewLines() / 2 - 1;
    if(line < center - G.curwin.getViewLines() - 1
	    || line > center + G.curwin.getViewLines()) {
      int top = line - (G.curwin.getViewLines() / 2);
      if((G.curwin.getViewLines() & 1) == 0) {
        ++top; // even num lines, put target in upper half
      }
      G.curwin.setViewTopLine(adjustTopLine(top));
    }
    // NEEDSWORK: gotoLine, respect p_so (scrolloff)
    G.curwin.setCaretPosition(line, col);
  }

  static void gotoLine_scrolloff(int line, int flag) {
    if(line > G.curwin.getLineCount()) {
      line = G.curwin.getLineCount();
    }
    MySegment seg = G.curwin.getLineSegment(line);
    int col;
    if(flag < 0) {
      col = coladvanceColumnIndex(seg);
    } else {
      // from nv_goto
      col = Edit.beginlineColumnIndex(flag, seg);
    }

    // if target line is less than half a screen away from
    // being visible, then just let it scroll, otherwise
    // center the target line

    int scrollMargin = G.curwin.getViewLines(); // max distance from center
                                                // to do scroll
    int center = G.curwin.getViewTopLine() + scrollMargin / 2 - 1;
    int so = getScrollOff();
    // reduce scrollMargin, the distance from center that we will scroll
    // the screen, by amount of scrolloff.
    scrollMargin -= so;
    
    int newTop = G.curwin.getViewTopLine(); // assume the top line wont change
    if(line < center - scrollMargin - 1
	    || line > center + scrollMargin) {
      newTop = line - (G.curwin.getViewLines() / 2);
      if((G.curwin.getViewLines() & 1) == 0) {
        ++newTop; // even num lines, put target in upper half
      }
      // center the target line
    } else {
      // scroll to the line
      if(line < G.curwin.getViewTopLine()+so) {
	newTop = line-so;
      } else if(line > G.curwin.getViewBottomLine()-so-1) {
	newTop = line-G.curwin.getViewLines()+1+so;
      }
    }
    G.curwin.setCaretPosition(line, col);
    G.curwin.setViewTopLine(adjustTopLine(newTop));
  }
  
  /**
   * @return scrolloff possibly adjusted for window size
   */
  static int getScrollOff() {
    int halfLines = G.curwin.getViewLines()/2; // max distance from center
    int so = G.p_so.getInteger();
    if(so > halfLines) {
      // adjust scrolloff so that its not bigger than usable
      so = halfLines;
    }
    return so;
  }

  /**
   * This method not in vim (that i know of).
   * The argument is the target for the top line, adjust
   * it so that there is no attempt to put blanks on the screen
   */
  static int adjustTopLine(int top) {
    if(top + G.curwin.getViewLines() > G.curwin.getLineCount()) {
      top = G.curwin.getLineCount() - G.curwin.getViewLines() + 1;
    }
    if(top < 1) {
      top = 1;
    }
    return top;
  }

  static void msgmore(int n) {
    int pn;

    if(G.global_busy) {
      // vim has a few more conditions for skipping this
      return;
    }

    if(n > 0) { pn = n; }
    else { pn = -n; }

    if(pn >= G.p_report.getInteger()) {
      String msg = "" + pn + " " + (n > 0 ? "more" : "fewer")
		+ " line" + plural(pn);
      // NEEDSWORK: msgmore: conditionally append "(interrupted)" to msg

      G.curwin.getStatusDisplay().displayStatusMessage(msg);
    }
  }

  private static int	line_breakcheck_count = 0;
  /**
   * Check for CTRL-C pressed, but only once in a while.
   * Should be used instead of ui_breakcheck() for functions that check for
   * each line in the file.  Calling ui_breakcheck() each time takes too much
   * time, because it can be a system call.
   */

  static void line_breakcheck() {
    /*
	#ifndef BREAKCHECK_SKIP
	# ifdef USE_GUI	   // assume the GUI only runs on fast computers
	#  define BREAKCHECK_SKIP 200
	# else
	#  define BREAKCHECK_SKIP 32
	# endif
	#endif
     */


    if (++line_breakcheck_count == 200) {
      line_breakcheck_count = 0;
      // ui_breakcheck(); // NEEDSWORK: check for interrupt
    }
  }

  //////////////////////////////////////////////////////////////////
  //
  // "misc2.c"
  //

  /**
   * coladvance(col).
   *
   * Using cursWant as the target column, advance the cursor on
   * the line specified by argument segment.
   *
   * @return the index of where to set the cursor on the line
   */
  public static int coladvanceColumnIndex(int wcol,
					  MySegment txt,
					  MutableBoolean reached) {
    int		idx = -1;
    int		col = 0;

    // Try to advance to the specified column.
    // There are txt.count chars in the line possibly including a '\n'; and
    // idx starts at -1.
    int ptr = txt.offset;
    char c;
    while (col <= wcol
	   && idx < txt.count - 1
	   && (c = txt.array[ptr]) != '\n')
    {
      ++idx;
      /* Count a tab for what it's worth (if list mode not on) */
      col += lbr_chartabsize(c, col);
      ++ptr;
    }
    /*
     * In Insert mode it is allowed to be one char beyond the end of the line.
     * Also in Visual mode, when 'selection' is not "old".
     */
    if (((G.State & INSERT) != 0
	 	|| (G.VIsual_active && G.p_sel.charAt(0) != 'o'))
	&& col <= wcol) {
      ++idx;
    }

    if(reached != null) {
      // indicate if the column was reached or not
      if (col <= wcol) { reached.setValue(false); } /* Couldn't reach column */
      else {reached.setValue(true); }    /* Reached column */
    }

    if (idx < 0) { idx = 0; }
    return idx;
  }

  public static int coladvanceColumnIndex(int wcol, MySegment txt) {
    return coladvanceColumnIndex(wcol, txt, null);
  }

  public static int coladvanceColumnIndex(MySegment txt) {
    return coladvanceColumnIndex(G.curwin.getWCurswant(), txt, null);
  }

  public static boolean coladvance(int wcol) {
    int line = G.curwin.getWCursor().getLine();
    MySegment txt = G.curwin.getLineSegment(line);
    int idx = coladvanceColumnIndex(wcol, txt, null);

    int offset = G.curwin.getLineStartOffset(G.curwin.getWCursor().getLine());
    G.curwin.setCaretPosition(offset + idx);
    return idx == wcol;
  }

  /**
   * inc_cursor()
   *<p>
   * Increment the current window's cursor position crossing line boundaries
   * as necessary.  Return 1 when crossing a line, -1 when at end of file, 0
   * otherwise.
   * </p>
   */
  static int inc_cursor() {
    ViFPOS fpos = G.curwin.getWCursor().copy();
    int rc = inc(fpos);
    G.curwin.setCaretPosition(fpos.getOffset());
    return rc;
  }

  /**
   * inc(lp)
   *<p>
   * Increment the line pointer 'lp' crossing line boundaries as necessary.
   * Return 1 when crossing a line, -1 when at end of file, 0 otherwise.
   * </p>
   */
   static int inc(ViFPOS lp) {
    int rc = incV7(lp);
    return rc != 2 ? rc : 1;
  }

  /**
   * incV7(lp)
   *<p>
   * Increment the line pointer 'lp' crossing line boundaries as necessary.
   * Return 1 when going to the next line.
   * Return 2 when moving forward onto a newline at the end of the line).
   * Return -1 when at the end of file.
   * Return 0 otherwise.
   * </p>
   */
  static int incV7(ViFPOS lp) {
    int currLine = lp.getLine();
    int c0 = Misc.gchar_pos(lp);

    if(c0 != '\n')    // still within line, move to next char (may be newline)
    {
      /* #ifdef FEAT_MBYTE
             if (has_mbyte)
             {
       	       int l = (*mb_ptr2len)(p);

	           lp->col += l;
	           return ((p[l] != NUL) ? 0 : 2);
             }
         #endif */

      lp.setPosition(currLine, lp.getColumn() + 1);

      /* #ifdef FEAT_VIRTUALEDIT
	         lp->coladd = 0;
         #endif */

      return Misc.gchar_pos(lp) != '\n' ? 0 : 2;
    }
    if(currLine != G.curwin.getLineCount())  // there is a next line
    {
      lp.setPosition(currLine + 1, 0);

      /* #ifdef FEAT_VIRTUALEDIT
	         lp->coladd = 0;
         #endif */

      return 1;
    }
    return -1;
  }

  /**
   * inclV7(fpos)
   *<p>
   * Same as incV7(), but skip the newline at the end of non-empty lines.
   * </p>
   */
  static int inclV7(ViFPOS fpos) {
    int rc = incV7(fpos);
    if (rc >= 1 && fpos.getColumn() > 0) {
      rc = incV7(fpos);
    }
    return rc;
  }
  
  /**
   * dec_cursor()
   *<p>
   * Decrement the current window's cursor position crossing line boundaries
   * as necessary.  Return 1 when crossing a line, -1 when at start of file, 0
   * otherwise.
   * </p>
   */
  static int dec_cursor() {
    ViFPOS fpos = G.curwin.getWCursor().copy();
    int rc = dec(fpos);
    G.curwin.setCaretPosition(fpos.getOffset());
    return rc;
  }

  /**
   * dec(lp)
   *<p>
   * Decrement the line pointer 'lp' crossing line boundaries as necessary.
   * Return 1 when crossing a line, -1 when at start of file, 0 otherwise.
   * Code is taken from vim 7.0 (but also behaves as 5.6)
   * </p>
   */
  static int dec(ViFPOS lp) {
    int currLine = lp.getLine();
    int currCol = lp.getColumn();

    /* #ifdef FEAT_VIRTUALEDIT
           lp->coladd = 0;
       #endif */

    if (currCol > 0) {                               // still within line
      lp.setPosition(currLine, currCol - 1);

      /* #ifdef FEAT_MBYTE
	         if (has_mbyte)
	           {
	               p = ml_get(lp->lnum);
	               lp->col -= (*mb_head_off)(p, p + lp->col);
	           }
         #endif */

      return 0;
    }
    if (currLine > 1) {                              // there is a prior line
      int newLine = currLine - 1;
      lp.setPosition(newLine, Util.lineLength(newLine));

      /* #ifdef FEAT_MBYTE
	         if (has_mbyte)
	             lp->col -= (*mb_head_off)(p, p + lp->col);
         #endif */

      return 1;
    }
    return -1;                                      // at start of file
  }

  /**
   * decl(lp)
   *<p>
   * Same as dec(), but skip the newline at the end of non-empty lines.
   * Code is taken from vim 7.0 (but also behaves a 5.6)
   * </p>
   */
  static int decl(ViFPOS lp) {
    int rc = dec(lp);
    if (rc == 1 && lp.getColumn() > 0) {
      rc = dec(lp);
    }
    return rc;
  }

  /**
   * inclDeclV7(lp,dir)
   *<p>
   * Increment or decrement the line pointer 'lp' base on direction 'dir'
   * crossing line boundaries as necessary and skipping over newline char
   * ('\n').  Return 1 when crossing a line, -1 when at end or start of file,
   * 0 otherwise.  Code is vim7.0 biased.
   * </p>
   */
  static int inclDeclV7(ViFPOS lp, int dir) {
    if (dir == BACKWARD)
      return decl(lp);
    else
      return inclV7(lp);
  }

  /**
   * Make sure current line number is valid and do begin line according
   * to flags.
   */
  static void check_cursor_lnumBeginline(int flags) {
    ViFPOS fpos = G.curwin.getWCursor();
    int lnum = check_cursor_lnum(fpos.getLine());
    if(lnum > 0) {
      G.curwin.setCaretPosition(lnum, 0);
    }
    Edit.beginline(flags);
  }

  /**
   * Make sure curwin->w_cursor.lnum is valid.
   * @return correct line number.
   */
  static int check_cursor_lnum(int lnum) {
    Normal.do_xop("check_cursor_lnum");
    if(lnum > G.curwin.getLineCount()) {
      lnum = G.curwin.getLineCount();
    }
    if(lnum <= 0) {
      lnum = 1;
    }
    return lnum;
  }

  /**
   * Make sure curwin->w_cursor.col is valid.
   * <p>Kind of screwy, using swing component cursor is always valid,
   * but this may backup the cursor off of a newline.
   */
  public static void check_cursor_col() {
    // just call adjust cursor, lnum must be valid.
    adjust_cursor();
  }

  /**
   * Make sure curwin->w_cursor.col is valid.
   * <p>Kind of screwy, using swing component cursor is always valid,
   * but this may backup the cursor off of a newline.
   * @return correct column number.
   */
  static int check_cursor_col(int lnum, int col) {
    Normal.do_xop("check_cursor_col");
    int len;
    MySegment seg = G.curwin.getLineSegment(lnum);
    len = seg.count - 1; // don't count trailing newline

    if (len == 0) {
      col = 0;
    } else if (col >= len) {
      // adjust len to ignore trailing newline
      // MUST_HAVE_NL
      // if(seg.array[seg.offset + len - 1] == '\n') { len--; }
      // Allow cursor past end-of-line in Insert mode, restarting Insert
      // mode or when in Visual mode and 'selection' isn't "old"
      if ((G.State & INSERT) != 0 || G.restart_edit != 0
	 	|| (G.VIsual_active && G.p_sel.charAt(0) != 'o')) {
	col = len;
      } else {
	col = len - 1;
      }
    }
    return col;
  }

  /**
   * make sure curwin->w_cursor in on a valid character.
   * <p>Kind of screwy, using swing component cursor is always valid,
   * but this may backup the cursor off of a newline.
   */
  static void adjust_cursor() {
    Normal.do_xop("adjust_cursor");
    ViFPOS fpos = G.curwin.getWCursor();
    int lnum = check_cursor_lnum(fpos.getLine());
    int col = check_cursor_col(lnum, fpos.getColumn());
    if(lnum != fpos.getLine() || col != fpos.getColumn()) {
      G.curwin.setCaretPosition(lnum, col);
    }
  }


  static ViCursor[] cursor_table;

  /**
   * Parse options for cursor shapes.
   * <br><b>NEEDSWORK:</b><ul>
   * <li>actually parse something rather than hardcode shapes
   * </ul>
   */
  static void parse_guicursor() {
    // Set them up according to the defaults.
    Cursor block = new Cursor(SHAPE_BLOCK, 0, 0);
    Cursor ver25 = new Cursor(SHAPE_VER, 25, -1);
    Cursor ver35 = new Cursor(SHAPE_VER, 35, -1);
    Cursor hor20 = new Cursor(SHAPE_HOR, 20, 0);
    Cursor hor50 = new Cursor(SHAPE_HOR, 50, 0);

    cursor_table = new ViCursor[SHAPE_COUNT];
    cursor_table[SHAPE_N] = block;
    cursor_table[SHAPE_V] = block;
    cursor_table[SHAPE_I] = ver25;
    cursor_table[SHAPE_R] = hor20;
    cursor_table[SHAPE_C] = block;
    cursor_table[SHAPE_CI] = ver25;
    cursor_table[SHAPE_CR] = hor20;
    cursor_table[SHAPE_SM] = block;	// plus different blink rates
    cursor_table[SHAPE_O] = hor50;
    cursor_table[SHAPE_VE] = ver35;
  }

  /**
   * @return the description of the cursor to draw
   */
  private static ViCursor getCursor() {
    if(cursor_table == null) {
      parse_guicursor();
    }
    return cursor_table[get_cursor_idx()];
  }

  /**
   * Return the index into cursor_table[] for the current mode.
   */
  static private int get_cursor_idx() {
      if (G.State == SHOWMATCH)	{ return SHAPE_SM; }
      if (G.State == INSERT)	{ return SHAPE_I; }
      if (G.State == REPLACE)	{ return SHAPE_R; }
      if (G.State == VREPLACE)	{ return SHAPE_R; }
      if (G.State == CMDLINE) {
	  if (Misc.cmdline_at_end())	 { return SHAPE_C; }
	  if (Misc.cmdline_overstrike()) { return SHAPE_CR; }
	  return SHAPE_CI;
      }
      if (G.finish_op)		{ return SHAPE_O; }
      if (G.VIsual_active) {
	  if (G.p_sel.charAt(0) == 'e') {
              return SHAPE_VE;
          } else {
              return SHAPE_V;
          }
      }

      return SHAPE_N;
  }

  //////////////////////////////////////////////////////////////////
  //
  // "ops.c"
  //


  /**
   * op_shift - handle a shift operation
   */
  static void op_shift(OPARG oap, boolean curs_top, int amount) {
    int	    i;
    int	    first_char;
    int	    block_col = 0;

    int	    line;

    /*
       if (u_save((linenr_t)(curwin.w_cursor.lnum - 1),
       (linenr_t)(curwin.w_cursor.lnum + oap.line_count)) == FAIL)
       return;
     */


    if (oap.block_mode) {
    }

    line = G.curwin.getWCursor().getLine();
    for (i = oap.line_count; --i >= 0; ) {
      G.curwin.setCaretPosition(line, 0);
      first_char = gchar_cursor();
      if (first_char != '\n') {	// empty line
	shift_line(oap.op_type == OP_LSHIFT, G.p_sr.getBoolean(), amount);
      }
      line++;
    }

    if (oap.block_mode) {
    } else if (curs_top) {    /* put cursor on first line, for ">>" */
      G.curwin.setCaretPosition(line - oap.line_count, 0);
      Edit.beginline(BL_SOL | BL_FIX); // shift_line() may have set cursor.col
    } else {
      G.curwin.setCaretPosition(line - 1, 0);
    }
    // update_topline();
    // update_screen(NOT_VALID);

    if (oap.line_count >= G.p_report.getInteger()) {
      Msg.smsg("" + oap.line_count + " line" + plural(oap.line_count)
	       + " " + ((oap.op_type == OP_RSHIFT) ? ">" : "<") + "ed "
	       + amount + " time" +  plural(amount));
    }

    /*
     * Set "'[" and "']" marks.
     */
    /* ****************************************************************
       curbuf.b_op_start = oap.start;
       curbuf.b_op_end = oap.end;
     *******************************************************************/
  }

  /**
   * shift the current line one shiftwidth left (if left != 0) or right
   * leaves cursor on first blank in the line
   */
  static void shift_line(boolean left, boolean round, int amount) {
    int		count;
    int		i, j;
    int		p_sw = G.curbuf.b_p_sw;

    count = get_indent();	// get current indent

    if (round) {		// round off indent
      i = count / p_sw;	// number of p_sw rounded down
      j = count % p_sw;	// extra spaces
      if (j != 0 && left)	// first remove extra spaces
	--amount;
      if (left) {
	i -= amount;
	if (i < 0)
	  i = 0;
      } else {
	i += amount;
      }
      count = i * p_sw;
    } else {		// original vi indent
      if (left) {
	count -= p_sw * amount;
	if (count < 0)
	  count = 0;
      } else {
	count += p_sw * amount;
      }
    }

    /* Set new indent */
    if (G.State == VREPLACE) {
      // change_indent(INDENT_SET, count, false, NUL);
    } else {
      set_indent(count, true);
    }
  }

  //
  // YANK STUFF
  //

  static class Yankreg {
    StringBuffer[] y_array = new StringBuffer[1];
    int y_size;
    int y_type;
    void setData(String s) {
      int offset = 0;
      int lines = 0;
      while((offset = s.indexOf('\n', offset)) >= 0) {
	offset++;	// for next time through the loop
	lines++;
      }
      if(lines > 0) {
	y_type = MLINE;
	y_size = lines;
      } else {
	y_type = MCHAR;
	y_size = 1;
      }

      y_array[0] = new StringBuffer(s);
    }

    StringSelection getStringSelection() {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < y_array.length; i++) {
        sb.append(y_array[i]);
        if(y_type == MBLOCK)
          sb.append('\n');
      }
      return new StringSelection(sb.toString());
    }
  }

  //
  // Number of registers.
  //	0 = unnamed register, for normal yanks and puts
  //   1..9 = number registers, for deletes
  // 10..35 = named registers
  //     36 = delete register (-)
  //     37 = Clipboard register (*).
  //
  static final int DELETION_REGISTER = 36;
  static final int CLIPBOARD_REGISTER = 37;
  static Yankreg y_current = null;
  static Yankreg y_previous = null;
  static boolean y_append;
  static Yankreg[] y_regs = new Yankreg[38];

  /**
   * Check if 'regname' is a valid name of a yank register.
   * Note: There is no check for 0 (default register), caller should do this
   * @param writing if true check for writable registers
   */
  public static boolean valid_yank_reg(int regname, boolean writing) {
    if (regname > '~')
      return false;
    if (       Util.isalnum(regname)
	       || (!writing && Util.vim_strchr("/.%#:" , regname) != null)
	       || regname == '"'
	       || regname == '-'
	       || regname == '_'
	       || regname == '*') {
      return true;
    }
    return false;
  }

  /**
   * Set y_current and y_append, according to the value of "regname".
   * Cannot handle the '_' register.
   *
   * If regname is 0 and writing, use register 0
   * If regname is 0 and reading, use previous register
   */
  static void get_yank_register(int regname, boolean writing) {
    int	    i;

    y_append = false;
    if (((regname == 0 && !writing) || regname == '"') && y_previous != null) {
      y_current = y_previous;
      return;
    }
    i = regname;
    if (Util.isdigit(i)) {
      i -= '0';
    } else if (Util.islower(i)) {
      i -= 'a' - 10;
    } else if (Util.isupper(i)) {
      i -= 'A' - 10;
      y_append = true;
    } else if (regname == '-') {
      i = DELETION_REGISTER;
      /* When clipboard is not available, use register 0 instead of '*' */
    } else if (clipboard_available && regname == '*') {
      i = CLIPBOARD_REGISTER;
    } else {		/* not 0-9, a-z, A-Z or '-': use register 0 */
      i = 0;
    }
    y_current = y_regs[i];
    if(y_current == null) {
      y_current = new Yankreg();
      y_regs[i] = y_current;
    }
    if (writing)	/* remember the register we write into for do_put() */
      y_previous = y_current;
  }

  static int	do_record_regname;
  /**
   * start or stop recording into a yank register
   *
   * return FAIL for failure, OK otherwise
   */
  static int do_record(int c) {
    Yankreg old_y_previous, old_y_current;
    int		retval;

    if ( ! G.Recording) {	    // start recording
                        // registers 0-9, a-z and " are allowed
      if (c > '~' || (!Util.isalnum(c) && c != '"'))
        retval = FAIL;
      else {
        G.Recording = true;
        showmode();
        do_record_regname = c;
        retval = OK;
      }
    } else {			    /* stop recording */
      String s;
      G.Recording = false;
      showmode(); // Msg.smsg("");
      s = GetChar.get_recorded();
      if (s.length() == 0)
        retval = FAIL;
      else {
        //
        // We don't want to change the default register here, so save and
        // restore the current register name.
        //
        old_y_previous = y_previous;
        old_y_current = y_current;

        retval = stuff_yank(do_record_regname, s);
        // System.err.println("Recorded: '" + s + "'");

        y_previous = old_y_previous;
        y_current = old_y_current;
      }
    }
    return retval;
  }

  /**
   * Stuff string 'p' into yank register 'regname' as a single line (append if
   * uppercase).	'p' must have been alloced.
   *
   * @return FAIL for failure, OK otherwise
   */
  static private int stuff_yank(int regname, String s) {
    // check for read-only register
    if (regname != 0 && !valid_yank_reg(regname, true)) {
      return FAIL;
    }
    if (regname == '_') {		    // black hole: don't do anything
      return OK;
    }
    get_yank_register(regname, true);
    if (y_append) {
      y_current.y_array[0].append(s);
    } else {
      free_yank_all();
      y_current.y_array[0].append(s);
      y_current.y_size = 1;
      y_current.y_type = MCHAR;  // (orig comment)used to be MLINE, why?
    }
    return OK;
  }

  static private int	lastc_do_execreg = NUL;
  /**
   * execute a yank register: copy it into the stuff buffer
   * @return FAIL for failure, OK otherwise
   * @param regname     get commands from this register
   * @param colon	insert ':' before each line
   * @param addcr	always add '\n' to end of line
   */
  static int do_execreg(int regname, boolean colon, boolean addcr) {
    int		retval = OK;

    // NEEDSWORK: do_execreg: colon always false
    if (regname == '@')			// repeat previous one
      regname = lastc_do_execreg;
    // check for valid regname
    if (regname == '%' || regname == '#' || !valid_yank_reg(regname, false))
      return FAIL;
    lastc_do_execreg = regname;

    if (regname == '_')			// black hole: don't stuff anything
      return OK;

    if (regname == ':')			// use last command line
    {
      return FAIL; // NEEDSWORK: do_execreg ':'
      /* ****************************************************************
      if (last_cmdline == NULL)
      {
        EMSG(e_nolastcmd);
        return FAIL;
      }
      vim_free(new_last_cmdline); // don't keep the cmdline containing @:
      new_last_cmdline = NULL;
      retval = put_in_typebuf(last_cmdline, TRUE);
      ****************************************************************/
    }
    else if (regname == '.')		// use last inserted text
    {
      /* ****************************************************************
      return FAIL; // NEEDSWORK: do_execreg '.'
      p = get_last_insert_save();
      if (p == NULL)
      {
        EMSG(e_noinstext);
        return FAIL;
      }
      retval = put_in_typebuf(p, colon);
      vim_free(p);
      ****************************************************************/
    }
    else
    {
      int remap;
      get_yank_register(regname, false);
      if (y_current.y_array.length == 0 || y_current.y_array[0].length() == 0)
        return FAIL;

      /* Disallow remaping for ":@r". */
      remap = colon ? -1 : 0;

      //
      // Insert lines into typeahead buffer, from last one to first one.
      //
      /* ****************************************************************
      for (i = y_current.y_size; --i >= 0; )
      {
        // insert newline between lines and after last line if type is MLINE
        if (y_current.y_type == MLINE || i < y_current.y_size - 1
            || addcr)
        {
          if (ins_typebuf("\n", remap, 0, true) == FAIL)
            return FAIL;
        }
        if (ins_typebuf(y_current.y_array[i], remap, 0, true) == FAIL)
          return FAIL;
        if (colon && ins_typebuf(":", remap, 0, true) == FAIL)
          return FAIL;
      }
      ****************************************************************/
      // Just roll our own for jvi
      String s;
        s = new String(y_current.y_array[0]);
      if(y_current.y_type == MLINE || addcr) {
        if( ! s.endsWith("\n")) {
          s += "\n";
        }
      }
      if(GetChar.ins_typebuf(s, remap, 0, true) == FAIL) {
        return FAIL;
      }
      G.Exec_reg = true;	// disable the 'q' command
    }
    return retval;
  }

  /**
   * Free up storage associated with current yank buffer.
   */
  static void free_yank_all() {
    if(y_current.y_array.length != 0) {
      y_current.y_array = new StringBuffer[1];
      y_current.y_array[0] = new StringBuffer();
    }
  }

  /**
   * Shift yank registers for a delete; make register 1 current.
   */
  static void shiftYank() {
    int n;
    // GC: y_current = y_regs[9];
    // GC: free_yank_all();			// free register nine
    for (n = 9; n > 1; --n)
      y_regs[n] = y_regs[n - 1];

    //y_regs[1].y_array = NULL;		// set register one to empty
    y_regs[1] = null;
    // y_previous = y_current = y_regs[1];
    get_yank_register('1', true);
  }

  /**
   * Put a string into a yank register. First used from clip_get_selection.
   */

  /**
   * op_delete - handle a delete operation
   *
   * @return FAIL if undo failed, OK otherwise.
   */
  static int op_delete(OPARG oap) {
    Normal.do_xop("op_delete");

    boolean		did_yank = true;
    int			old_lcount = G.curwin.getLineCount();

    /*
    if (curbuf.b_ml.ml_flags & ML_EMPTY)	    // nothing to do
      return OK;
    */

    // Nothing to delete, return here.	Do prepare undo, for op_change().
    if (oap.empty)
    {
      return Normal.u_save_cursor();
    }

    // If no register specified, and "unnamed" in 'clipboard', use * register
    if (oap.regname == 0 && G.p_cb.getBoolean())
      oap.regname = '*';
    if (!clipboard_available && oap.regname == '*')
      oap.regname = 0;

    //
    // NEEDSWORK: op_delete: Imitate strange Vi: If the delete spans ....
    // Imitate the strange Vi behaviour: If the delete spans more than one line
    // and motion_type == MCHAR and the result is a blank line, make the delete
    // linewise.  Don't do this for the change command or Visual mode.
    //
    /*
    if (       oap.motion_type == MCHAR
	       && !oap.is_VIsual
	       && oap.line_count > 1
	       && oap.op_type == OP_DELETE)
    {
      ptr = ml_get(oap.end.lnum) + oap.end.col + oap.inclusive;
      ptr = skipwhite(ptr);
      if (*ptr == NUL && inindent(0))
	oap.motion_type = MLINE;
    }
    */

    //
    // Check for trying to delete (e.g. "D") in an empty line.
    // Note: For the change operator it is ok.
    //
    if (       oap.motion_type == MCHAR
	       && oap.line_count == 1
	       && oap.op_type == OP_DELETE
			 //&& *ml_get(oap.start.lnum) == NUL
	       && Util.getCharAt(G.curwin
		      .getLineStartOffsetFromOffset(G.curwin
						    .getCaretPosition()))
	       		== '\n')
    {
      //
      // It's an error to operate on an empty region, when 'E' inclucded in
      // 'cpoptions' (Vi compatible).
      //
      if (Util.vim_strchr(G.p_cpo, CPO_EMPTYREGION) != null)
	Util.beep_flush();
      return OK;
    }

    //
    // Do a yank of whatever we're about to delete.
    // If a yank register was specified, put deleted text into that register.
    // For the black hole register '_' don't yank anything.
    //
    if (oap.regname != '_') {
      if (oap.regname != 0) {
	// check for read-only register
	if (!valid_yank_reg(oap.regname, true)) {
	  Util.beep_flush();
	  return OK;
	}
	get_yank_register(oap.regname, true); // yank into specif'd reg.
	if (op_yank(oap, true, false) == OK)   // yank without message
	  did_yank = true;
      }

      //
      // Put deleted text into register 1 and shift number registers if the
      // delete contains a line break, or when a regname has been specified!
      //
      if (oap.regname != 0 || oap.motion_type == MLINE
	  || oap.line_count > 1)
      {
	shiftYank();
	oap.regname = 0;
      } else if (oap.regname == 0) {		// yank into unnamed register
	oap.regname = '-';		// use special delete register
	get_yank_register(oap.regname, true);
	oap.regname = 0;
      }

      if (oap.regname == 0 && op_yank(oap, true, false) == OK) {
	did_yank = true;
      }

      /* **********************************
       NEEDSWORK: op_delete, yank failed, ask continue anyway
      //
      // If there's too much stuff to fit in the yank register, then get a
      // confirmation before doing the delete. This is crude, but simple.
      // And it avoids doing a delete of something we can't put back if we
      // want.
      //
      if (!did_yank) {
	if (ask_yesno((char_u *)"cannot yank; delete anyway", TRUE) != 'y') {
	  emsg(e_abort);
	  return FAIL;
	}
      }
      *****************************************/
    }

    //
    // block mode delete
    //
    if (oap.block_mode) {
//      if (u_save((oap.start.getLine() - 1), (oap.end.getLine() + 1)) == FAIL)
//            return FAIL;

//#ifdef SYNTAX_HL
//­       /* recompute syntax hl., starting with this line */
//­       syn_changed(curwin->w_cursor.lnum);
//#endif
          int lnum = G.curwin.getWCursor().getLine(); // is this still required?
          for (int lineNr = lnum; lineNr <= oap.end.getLine(); lineNr++) {
                StringBuffer newp, oldp;
                block_def bd = new block_def();
                block_prep(oap, bd, lineNr, true);
                if (bd.textlen == 0)       /* nothing to delete */
                    continue;

                /* n == number of chars deleted
                * If we delete a TAB, it may be replaced by several characters.
                * Thus the number of characters may increase!
                */
                int n = bd.textlen - bd.startspaces - bd.endspaces;
                oldp = new StringBuffer(Util.ml_get_curline().toString());
                newp = new StringBuffer();//newp = alloc_check(oldp.length() + 1 - n);
                //if (newp == null)
                //    continue;
                /* copy up to deleted part */
                //mch_memmove(newp, oldp, bd.textcol);
                /* insert spaces */
                //copy_spaces(newp + bd.textcol, (bd.startspaces + bd.endspaces));
                /* copy the part after the deleted part */
                //oldp += bd.textcol + bd.textlen;
                //mch_memmove(newp + bd.textcol + bd.startspaces + bd.endspaces, oldp, STRLEN(oldp) + 1);
                /* replace the line */
                //ml_replace(lineNr, newp, false);
                // hack
                //int lineoffset = G.curwin.getLineStartOffset(G.curwin.getLineNumber(G.curwin.getCaretPosition()));
                int lineoffset = G.curwin.getLineStartOffset(lineNr);
                G.curwin.deleteChar(lineoffset + bd.start_vcol, lineoffset + bd.end_vcol);
                // end hack
        }

        setLineNum(lnum); // line number isn't changing anymore.. still required???
        //changed_cline_bef_curs();/* recompute cursor pos. on screen */
        //approximate_botline();/* w_botline may be wrong now */
        adjust_cursor();

        //changed();
        update_screen(VALID_TO_CURSCHAR);
        oap.line_count = 0; /* no lines deleted */
    } else if (oap.motion_type == MLINE) {
      // OP_CHANGE stuff moved to op_change
      if(oap.op_type == OP_CHANGE) {
	boolean delete_last = false;
	int line = G.curwin.getWCursor().getLine();
	if(line + oap.line_count - 1 >= G.curwin.getLineCount()) {
	  delete_last = true;
	}
	del_lines(oap.line_count, true, true);
	open_line(delete_last ? FORWARD : BACKWARD, true, true, 0);
      } else {
	del_lines(oap.line_count, true, true);
	Edit.beginline(BL_WHITE | BL_FIX);
      }
      // u_clearline();	// "U" command should not be possible after "dd"
    }
    else if(oap.line_count == 1) {
      // delete characters within one line
      int start = oap.start.getOffset();
      int end = oap.end.getOffset();
      int n = end - start + 1 - (oap.inclusive ? 0 : 1);
      del_chars(n, G.restart_edit == 0);
    }
    else {
      // delete characters between lines
      if (Normal.u_save_cursor() == FAIL)	// save first line for undo
	return FAIL;

      int start = oap.start.getOffset();
      int end = oap.end.getOffset();
      // NEEDSWORK: inclusive: instead of decrementing, increment
      if(!oap.inclusive) {
	// NEEDSWORK: op_delete: handle oap.inclusive.
	// end--;
      } else {
	// System.err.println("*****\n***** delete oap.inclusive\n*****");
	end++;
      }
      G.curwin.deleteChar(start, end);
      if(G.restart_edit == 0) {
	Misc.check_cursor_col();
      }
    }

    // remove code for different screen update cases

    msgmore(G.curwin.getLineCount() - old_lcount);
    //
    // NEEDSWORK: op_delete: set "'[" and "']" marks.
    //

    return OK;
  }

  /**
   * op_tilde - handle the (non-standard vi) tilde operator
   */
  static void op_tilde(OPARG oap) {
    ViFPOS		pos;

    /* ***********************************************
       if (u_save((linenr_t)(oap->start.lnum - 1),
       (linenr_t)(oap->end.lnum + 1)) == FAIL)
       return;

    //
    // Set '[ and '] marks.
    //
    curbuf->b_op_start = oap->start;
    curbuf->b_op_end = oap->end;
     *************************************************************/

    //
    // Be very lazy. Just set the real cursor to oap.start
    // where its actually used,
    // then this can use the swap code that requires that
    // the cursor be used. The performance of this can be
    // improved considerable by not using the cursor, but
    // there are better things to do (like profiling).
    //

    pos = oap.start;

    if (oap.block_mode)		    // Visual block mode
    {
        for (; pos.getLine() <= oap.end.getLine(); pos.setPosition(pos.getLine()+1, pos.getColumn()))
         {
            block_def bd = new block_def();
            block_prep(oap, bd, pos.getLine(), false);
            G.curwin.setCaretPosition(pos.getLine(), bd.textcol);
            while (--bd.textlen >= 0) {
                ViFPOS tmppos = G.curwin.getWCursor(); // as stated above, don't need to do this
                swapchar(oap.op_type, tmppos);
                if (inc_cursor() == -1) // at end of file
         break;
         }
         }

    } else {				    // not block mode
      if (oap.motion_type == MLINE) {
        pos.setPosition(pos.getLine(), 0);      // this is start
        int col = Util.lineLength(oap.end.getLine());
        if (col != 0) {
          --col;
        }
        oap.end.setPosition(oap.end.getLine(), col);
      } else if (!oap.inclusive) {
        dec(oap.end);
      }

      G.curwin.getWindow().setWCursor(oap.start);
      pos = G.curwin.getWCursor(); // as stated above, don't need to do this
      int startCursor = pos.getOffset();
      while (pos.compareTo(oap.end) <= 0) {
        swapchar(oap.op_type, pos);
        if (inc_cursor() == -1)    // at end of file // was inc(pos)
          break;
      }
      G.curwin.setCaretPosition(startCursor);
    }

    /* **********************************************************
    if (oap.motion_type == MCHAR && oap.line_count == 1 && !oap.block_mode)
      update_screenline();
    else
    {
      update_topline();
      update_screen(NOT_VALID);
    }
    *********************************************************/

    if (oap.line_count > G.p_report.getInteger()) {
      Msg.smsg("" + oap.line_count + " line" + plural(oap.line_count) + " ~ed");
    }
  }

  /**
   * If op_type == OP_UPPER: make uppercase,
   * <br>if op_type == OP_LOWER: make lowercase,
   * <br>else swap case of character at 'pos'
   */
  static void swapchar(int op_type, ViFPOS fpos) {
    int	    c;
    int	    nc;

    c = gchar_pos(fpos);
    nc = swapchar(op_type, c);
    if(c != nc) {
      if(fpos == G.curwin.getWCursor()) {
        pchar_cursor(nc);
      } else {
        throw new RuntimeException("swapchar called with WCursor");
      }
    }
  }

  static int swapchar(int op_type, int c) {
    int	    nc;

    nc = c;
    if (Util.islower(c)) {
      if (op_type != OP_LOWER)
        nc = Util.toupper(c);
    }
    else if (Util.isupper(c))
    {
      if (op_type != OP_UPPER)
        nc = Util.tolower(c);
    }
    return nc;
  }

  /**
   * op_change - handle a change operation
   *
   * @return TRUE if edit() returns because of a CTRL-O command
   */
  static void op_change(OPARG oap)
  {
    // HACK so that finishOpInsert can be recalled
  op_insert_oap = oap.copy();
  op_insert_bd = new block_def();
    // END: HACK so that finishOpInsert can be recalled
    int		col;
    boolean	retval=true;

    col = oap.start.getColumn();
    if (oap.motion_type == MLINE) {
	col = 0;
    }

    // First delete the text in the region.  In an empty buffer only need to
    // save for undo
    /* *************************************************************
    if (curbuf->b_ml.ml_flags & ML_EMPTY) {
	if (u_save_cursor() == FAIL)
	    return FALSE;
    }
    else
      ***********************************************************/
      if (op_delete(oap) == FAIL)
	return;

      ViFPOS fpos = G.curwin.getWCursor();
    if ((col > fpos.getColumn()) && !Util.lineempty(fpos.getLine()))
	inc_cursor();

    // HACK so that finishOpInsert can be recalled
        MySegment s = Util.ml_get(oap.start.getLine());
        String firstline = s.subSequence(op_insert_bd.textcol,
                                         s.length()).toString();
        if (oap.op_type == OP_APPEND)
            firstline += op_insert_bd.textlen;
        op_insert_pre_textlen = firstline.length();
    // END HACK so that finishOpInsert can be recalled
    Edit.edit(NUL, false, 1);
  }

  /**
   * Yank the text between curwin->w_cursor and startpos into a yank register.
   * If we are to append (uppercase register), we first yank into a new yank
   * register and then concatenate the old and the new one (so we keep the old
   * one in case of out-of-memory).
   * <br><b>NEEDSWORK:</b><ul>
   * <li>An unfaithful and lazy port of yanking off.
   * <li>No block mode.
   * </ul>
   *
   * @return FAIL for failure, OK otherwise
   */
  public static int op_yank(OPARG oap, boolean deleting, boolean mess) {
    int		y_idx = 0;;		// index in y_array[]
    Yankreg		curr;		// copy of y_current
    // char_u		**new_ptr;
    int			lnum;		// current line number
    // int			j;
    // int			len;
    int			yanktype = oap.motion_type;
    int			yanklines = oap.line_count;
    int			yankendlnum = oap.end.getLine();
    // char_u		*p;
    StringBuffer        pnew;
    block_def		bd;

				    // check for read-only register
    if (oap.regname != 0 && !valid_yank_reg(oap.regname, true))
    {
	Util.beep_flush();
	return FAIL;
    }
    if (oap.regname == '_')	    // black hole: nothing to do
	return OK;

    // If no register specified, and "unnamed" in 'clipboard', use * register
    if (!deleting && oap.regname == 0 && G.p_cb.getBoolean())
        oap.regname = '*';
    if (!clipboard_available && oap.regname == '*')
	oap.regname = 0;

    if (!deleting)		    // op_delete() already set y_current
	get_yank_register(oap.regname, true);

    curr = y_current;
				    // append to existing contents
    if (y_append && y_current.y_array != null) {
	// y_current = new Yankreg(); // NEEDSWORK: just append to y_current
    } else {
	free_yank_all();	    // free previously yanked lines
    }

    //
    // If the cursor was in column 1 before and after the movement, and the
    // operator is not inclusive, the yank is always linewise.
    //
    if (       oap.motion_type == MCHAR
	    && oap.start.getColumn() == 0
	    && !oap.inclusive
	    && (!oap.is_VIsual || G.p_sel.charAt(0) == 'o')
	    && oap.end.getColumn() == 0
	    && yanklines > 1)
    {
	yanktype = MLINE;
	--yankendlnum;
	--yanklines;
    }

    y_current.y_size = yanklines;
    y_current.y_type = yanktype;   // set the yank register type
    // y_current.y_array = (char_u **)lalloc_clear.......
    // if (y_current.y_array == NULL) { y_current = curr; return FAIL; }

    lnum = oap.start.getLine();

    //
    // Visual block mode
    //
    if (oap.block_mode) {
      // block mode deleted
      y_current.y_type = MBLOCK;        /* set the yank register type */
      y_current.y_array = new StringBuffer[yanklines];
      for ( ; lnum <= yankendlnum; ++lnum)
      {
          //y_current.y_array[lnum - oap.start.getLine()] = new StringBuffer();
          bd = new block_def();
          block_prep(oap, bd, lnum, false);
          pnew = new StringBuffer();
          /*System.out.println("alloc :"+ (bd.startspaces + bd.endspaces + bd.textlen + 1));
          try {
            System.out.println("-->"+G.curwin.getText(G.curwin.getLineStartOffset(lnum), bd.startspaces + bd.endspaces + bd.textlen + 1));
          } catch (BadLocationException ex) {
            ex.printStackTrace();
          }*/
//          if ((pnew = alloc(bd.startspaces + bd.endspaces + bd.textlen + 1)) == NULL)
//            goto fail;
          y_current.y_array[y_idx++] = pnew;

          copy_spaces(pnew, bd.startspaces);
          //pnew += bd.startspaces;
          //pnew.append(block_text(lnum, bd.textstart, bd.textlen));

          mch_memmove(pnew, lnum, bd.textstart, bd.textlen);
          //pnew += bd.textlen;

          copy_spaces(pnew, bd.endspaces);
          //pnew += bd.endspaces;

          //pnew = null;
      }
    } else {
      int start;
      int end;
      if(yanktype == MLINE) {
        start = G.curwin.getLineStartOffset(lnum);
        end = G.curwin.getLineEndOffset(yankendlnum);
      } else {
        start = oap.start.getOffset();
        end = oap.end.getOffset() + (oap.inclusive ? 1 : 0);
      }
      int length = end - start;
      StringBuffer reg = y_current.y_array[0];
      MySegment seg = G.curwin.getSegment(start, length, null);
      reg.append(seg.array, seg.offset, seg.count);
      // bug #1724053 visual mode not capture \n after '$'
      // I guess the oap.inclusive should be trusted.
      // if(yanktype == MCHAR && length > 0
      //    	&& reg.charAt(reg.length()-1) == '\n') {
      //   reg.deleteCharAt(reg.length()-1);
      // }
    }

    // NEEDSWORK: if lines are made an array in the yank buffer
    //            then in some cases must append the current
    //            yank to the previous contents of yank buffer

    if (mess)			// Display message about yank?
    {
	if (yanktype == MCHAR && !oap.block_mode && yanklines == 1)
	    yanklines = 0;
	// Some versions of Vi use ">=" here, some don't...
	if (yanklines >= G.p_report.getInteger())
	{
	    // redisplay now, so message is not deleted
	    // NEEDSWORK: update_topline_redraw();
	    Msg.smsg("" + yanklines + " line" + plural(yanklines) + " yanked");
	}
    }

    //
    // NEEDSWORK: op_yank: set "'[" and "']" marks.
    //
    // curbuf.b_op_start = oap.start;
    // curbuf.b_op_end = oap.end;

    //
    // If we were yanking to the clipboard register, send result to clipboard.
    //
    if (curr == y_regs[CLIPBOARD_REGISTER] && clipboard_available)
    {
	// clip_own_selection();
	clip_gen_set_selection();
    }

    return OK;
  }

  /**
   * put contents of register "regname" into the text.
   * For ":put" command count == -1.
   * flags: PUT_FIXINDENT	make indent look nice
   *	  PUT_CURSEND	leave cursor after end of new text
   *
   * FIX_INDENT not supported, used by mouse and bracket print, [p
   */
  public static void do_put(int regname, int dir, long count, int flags) {

    int			old_lcount = G.curwin.getLineCount();
    int /*colnr_t*/     col;
    int /*colnr_t*/     vcol;
    int                 delcount;
    int                 oldlen;
    long                totlen = 0;
    int                 yanklen;
    StringBuffer        oldp, newp;
    StringBuffer        ptr;
    block_def           bd;
    int                 incr = 0;

    /* If no register specified, and "unnamed" in 'clipboard', use * register */
    if (regname == 0 && G.p_cb.getBoolean())
        regname = '*';
    if (regname == '*') {
	if (!clipboard_available)
	    regname = 0;
	else
	    clip_get_selection();
    }

    int y_type = 0;
    int y_size = 0;
    StringBuffer[] y_array = null;

    // NEEDSWORK: do_put: there are special registers like: '%', '#', ':',...
    // if (get_spec_reg(regname, &insert_string, &allocated, TRUE))

    if(false) {
      // This is the case where insert_string from get_spec_reg is non null
    } else {
	get_yank_register(regname, false);

	y_type = y_current.y_type;
	y_size = y_current.y_size;
	y_array = y_current.y_array;
    }

    if (y_size == 0 || y_array == null) {
	Msg.emsg("Nothing in register "
		  + (regname == 0 ? "\"" : transchar(regname)));
	return;
    }


    FPOS fpos = G.curwin.getWCursor();
    int offset = fpos.getOffset();
    int length;
    int new_offset;
    if(y_type == MCHAR) {
      if(dir == FORWARD) {
	// increment position, unless pointing at new line
	if(Util.getCharAt(offset) != '\n') {
	  offset++;
	}
      }
    } else if(y_type == MLINE) {
      if(dir == FORWARD) {
	offset = G.curwin.getLineEndOffsetFromOffset(offset);
      } else {
	offset = G.curwin.getLineStartOffsetFromOffset(offset);
      }
    } else {
      // block mode not supported
      //return;
    }

    // block mode
    if (y_type == MBLOCK) {
        if (dir == FORWARD && gchar_cursor() != NUL)
        {
            col = getvcol();

//#ifdef MULTI_BYTE
//            if (is_dbcs)
//            {
//                /* put it on the next of the multi-byte character. */
//                col += bMultiByteCode + 1;
//                curwin->w_cursor.col += bMultiByteCode + 1;
//            }
//            else
//#endif
            {
                ++col;
                G.curwin.setCaretPosition(G.curwin.getCaretPosition()+1);//++curwin.w_cursor.col;
    }
    }
        else
            col = getvcol(); //getvcol(curwin, curwin.w_cursor, col, NULL, NULL);
//#ifdef SYNTAX_HL
//        /* recompute syntax hl., starting with current line */
//        syn_changed(curwin->w_cursor.lnum);
//#endif
       
        bd = new block_def();
        for (int i = 0; i < y_size; ++i) {
            bd.startspaces = 0;
            bd.endspaces = 0;
            bd.textcol = 0;
            vcol = 0;
            delcount = 0;

            /* add a new line */
//            if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count) {
//                ml_append(curbuf->b_ml.ml_line_count, (char_u *)"",
//                        (colnr_t)1, FALSE);
//                ++nr_lines;
//            }
            /* get the old line and advance to the position to insert at */
            oldp = new StringBuffer(Util.ml_get_curline().toString());
            oldlen = oldp.length(); //STRLEN(oldp);
//            for (ptr = oldp; vcol < col && *ptr; ++ptr)
//            {
//                /* Count a tab for what it's worth (if list mode not on) */
//                incr = lbr_chartabsize(ptr, (colnr_t)vcol);
//                vcol += incr;
//                ++bd.textcol;
//            }
            if (vcol < col) /* line too short, padd with spaces */
            {
                bd.startspaces = col - vcol;
            }
            else if (vcol > col)
            {
                bd.endspaces = vcol - col;
                bd.startspaces = incr - bd.endspaces;
                --bd.textcol;
                delcount = 1;
            }

            /* insert the new text */
            yanklen = y_array[i].length();//STRLEN(y_array[i]);
            totlen = count * yanklen + bd.startspaces + bd.endspaces;
            newp = new StringBuffer();//newp = alloc_check(totlen + oldlen + 1);
//            if (newp == null)
//                break;
            /* copy part up to cursor to new line */
            ptr = newp;
            //System.out.println("mch_memmove(" + ptr +", "+ oldp+","+ bd.textcol+")");
            //mch_memmove(ptr, G.curwin.getWCursor().getLine(), oldp, bd.textcol);
            G.curwin.insertText(G.curwin.getCaretPosition(), y_array[i].toString());

//            mch_memmove(ptr, oldp, (size_t)bd.textcol);
//            ptr.setIndex(ptr.getIndex() + bd.textcol);// ptr += bd.textcol;
            /* may insert some spaces before the new text */
            copy_spaces(ptr, bd.startspaces);
//            ptr.setIndex(ptr.getIndex() + bd.startspaces); //ptr += bd.startspaces;
            /* insert the new text */
//            for (int j = 0; j < count; ++j)
//            {
//                System.out.println("mch_memmove: "+ptr +","+ y_array[i] +","+yanklen+")");//mch_memmove(ptr, y_array[i], yanklen);
//                ptr.setIndex(ptr.getIndex() + yanklen);
//            }
            /* may insert some spaces after the new text */
            copy_spaces(ptr, bd.endspaces);
//            ptr.setIndex(ptr.getIndex() | bd.endspaces);
            /* move the text after the cursor to the end of the line. */
//            mch_memmove(ptr, oldp + bd.textcol + delcount,
//                    (size_t)(oldlen - bd.textcol - delcount + 1));
//            ml_replace(curwin->w_cursor.lnum, newp, FALSE);

            //setLineNum(G.curwin.getLineNumber(G.curwin.getCaretPosition())+1);
            G.curwin.setCaretPosition(G.curwin.getLineNumber(G.curwin.getCaretPosition()) + 1, col);//++curwin.w_cursor.lnum;
//            if (i == 0) {
//                G.curwin.setCaretPosition(G.curwin.getLineNumber(G.curwin.getCaretPosition()), G.curwin.getColumnNumber(G.curwin.getCaretPosition()+ bd.startspaces));
//                //curwin.w_cursor.col += bd.startspaces;
//            }
            
        }
        /* adjust '] mark */
//        curbuf.b_op_end.lnum = curwin.w_cursor.lnum - 1;
//        curbuf.b_op_end.col = bd.textcol + totlen - 1;
//        if (flags & PUT_CURSEND)
//        {
//            curwin.w_cursor = curbuf.b_op_end;
//            curwin.w_cursor.col++;
//        }
//        else
//            curwin.w_cursor.lnum = lnum;
//        update_topline();
//        if (flags & PUT_CURSEND)
//            update_screen(NOT_VALID);
//        else
//            update_screen(VALID_TO_CURSCHAR);

    } else { // not block mode
        String s = y_array[0].toString();
    length = s.length();
    G.curwin.insertText(offset, s);

    // now figure out cursor position
    if(y_type == MCHAR && y_size == 1) {
      G.curwin.setCaretPosition(offset + length - 1);
    } else {
      if((flags & PUT_CURSEND) != 0) {
	offset += length;
	G.curwin.setCaretPosition(offset);
	if(y_type == MLINE) {
	} else {
	}
      } else if (y_type == MLINE) {
	offset = G.curwin.getLineStartOffsetFromOffset(offset);
	G.curwin.setCaretPosition(offset);
	Edit.beginline(BL_WHITE | BL_FIX);
      } else {
	G.curwin.setCaretPosition(offset);
      }
    }
    }

    msgmore(G.curwin.getLineCount() - old_lcount);
    G.curwin.setWSetCurswant(true);

    /* NEEDSWORK: if ((flags & PUT_CURSEND)
			&& gchar_cursor() == NUL
			&& curwin->w_cursor.col
			&& !(restart_edit || (State & INSERT))) {
      --curwin->w_cursor.col;
    }
    */
  }

  /**
   * join 'count' lines (minimal 2), including u_save()
   */
  static void do_do_join(int count, boolean insert_space, boolean redraw) {
    // if (u_save((linenr_t)(curwin->w_cursor.lnum - 1),
    // (linenr_t)(curwin->w_cursor.lnum + count)) == FAIL)
    // return;

    if (count > 10)
      redraw = false;		    /* don't redraw each small change */
    while (--count > 0) {
      line_breakcheck();
      if (/*got_int ||*/ do_join(insert_space, redraw) == FAIL) {
	Util.beep_flush();
	break;
      }
    }
    // redraw_later(VALID_TO_CURSCHAR);

    /*
     * Need to update the screen if the line where the cursor is became too
     * long to fit on the screen.
     */
    // update_topline_redraw();
  }

  /*
   * Join two lines at the cursor position.
   * "redraw" is TRUE when the screen should be updated.
   *
   * return FAIL for failure, OK ohterwise
   */
  static int do_join(boolean insert_space, boolean redraw) {

      if(G.curwin.getWCursor().getLine()
                              == G.curwin.getLineCount()) {
	return FAIL;	// can't join on last line
      }
      StringBuffer spaces = new StringBuffer();
      int nextline = G.curwin.getWCursor().getLine() + 1;
      int lastc = 0;

      int offset00 = G.curwin.getLineStartOffset(nextline);
      int offset01 = offset00 - 1; // points to the '\n' of current line

      MySegment seg = G.curwin.getLineSegment(nextline);
      int offset02 = offset00 + skipwhite(seg);
      int nextc = Util.getCharAt(offset02);

      if(insert_space) {
	if(offset01 > 0) {
	  // there's a char before first line's '\n'
	  lastc = Util.getCharAt(offset01 - 1); // last char of line
	} else {
	  // at begin file, say lastc is a newline
	  lastc = '\n';
	}
	if(nextc != ')' && lastc != ' ' && lastc != TAB && lastc != '\n') {
	  spaces.append(' ');
	  if(G.p_js.getBoolean() && (lastc == '.' || lastc == '?'
                                     || lastc =='!')) {
	    spaces.append(' ');
	  }
	}
      }
      G.curwin.deleteChar(offset01, offset02);
      if(spaces.length() > 0) {
	G.curwin.insertText(offset01, spaces.toString());
      }
      G.curwin.setCaretPosition(offset01);

      return OK;
  }

  //////////////////////////////////////////////////////////////////
  //
  // "ui.c"
  //

  static void ui_cursor_shape() {
    Normal.do_xop("ui_cursor_shape");
    G.curwin.updateCursor(getCursor());
  }

  //
  // Clipboard stuff
  //

  /** When true, allow the clipboard to be used. */
  static boolean clipboard_available = true;
  static boolean clipboard_owned = false;

  static void clip_gen_set_selection() {
    StringSelection ss = y_regs[CLIPBOARD_REGISTER].getStringSelection();
    Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
    synchronized(clipOwner) {
      clipboard_owned = true;
      cb.setContents(ss, clipOwner);
    }
  }

  private static boolean debugClip = false;
  static void clip_get_selection() {
    if(clipboard_owned) {
      // whatever is in the clipboard, we put there. So just return.
      // NEEDSWORK: clip_get_selection, code about clipboard.start/end...
      return;
    } else {
      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
      if(debugClip) {
        if(cb.isDataFlavorAvailable(ViManager.VimClipboard2))
            System.err.println("VimClip available");
        DataFlavor dfa[] = cb.getAvailableDataFlavors();
        Arrays.sort(dfa, new Comparator<DataFlavor>() {
            public int compare(DataFlavor df1, DataFlavor df2) {
              return df1.getMimeType().compareTo(df2.getMimeType());
            }
          }
        );
        for (DataFlavor df : dfa) {
          System.err.println(df.getMimeType());
        }
      }
      
      Transferable trans = cb.getContents(null);
      if(debugClip && trans.isDataFlavorSupported(ViManager.VimClipboard2)) {
        System.err.println("VimClip supported");
      }
      String s = "";
      try {
	s = (String)trans.getTransferData(DataFlavor.stringFlavor);
      } catch(IOException e) {
        Util.vim_beep();
      } catch(UnsupportedFlavorException e) {
        Util.vim_beep();
      }
      // NEEDSWORK: use a string reader and transfer to StringBuffer
      get_yank_register('*', false);
      // y_regs[CLIPBOARD_REGISTER].y_array = new StringBuffer(s);
      y_regs[CLIPBOARD_REGISTER].setData(s);
    }
  }

  /**
   * Lost clipboard ownership, implementation of ClibboardOwner.
   */
  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    synchronized(clipOwner) {
      clipboard_owned = false;
    }
  }

  //////////////////////////////////////////////////////////////////
  //
  // "screen.c"
  //

  /**
   * Move screen 'count' pages up or down and update screen.
   *<br>
   * return FAIL for failure, OK otherwise
   */
  static int onepage(int dir, int count) {
    Normal.do_xop("onepage");
    int	    lp;
    int	    n;
    int	    off;
    int	    retval = OK;
    int newtopline = -1;
    int newcursorline = -1;


    if (G.curwin.getLineCount() == 1) { // nothing to do
      Util.beep_flush();
      return FAIL;
    }

    // NEEDSWORK: disable count for onepage (^F, ^B)
    // 		need to only use variables, not real position
    // 		inside for loop. Don't want to actually move
    // 		the viewport each time through the loop.

    count = 1;

    int so = getScrollOff();
    for ( ; count > 0; --count) {
      validate_botline();
      //
      // It's an error to move a page up when the first line is already on
      // the screen. It's an error to move a page down when the last line
      // is on the screen and the topline is 'scrolloff' lines from the
      // last line.
      //
      if (dir == FORWARD
	      ? ((G.curwin.getViewTopLine() >= G.curwin.getLineCount() - so)
		  && G.curwin.getViewBottomLine() > G.curwin.getLineCount())
	      : (G.curwin.getViewTopLine() == 1))
      {
	Util.beep_flush();
	retval = FAIL;
	break;
      }
      
      // the following test is added because with swing there can not be
      // blank lines on the screen, so we can go no more when the cursor
      // is positioned at the last line.
      if (dir == FORWARD
             && G.curwin.getWCursor().getLine() == G.curwin.getLineCount()) {
	Util.beep_flush();
	retval = FAIL;
	break;
      }

      if (dir == FORWARD) {
	// at end of file
	if(G.curwin.getViewBottomLine() > G.curwin.getLineCount()) {
	  newtopline = G.curwin.getLineCount();
	  newcursorline = G.curwin.getLineCount();
	  // curwin->w_valid &= ~(VALID_WROW|VALID_CROW);
	} else {
	  lp = G.curwin.getViewBottomLine();
	  off = get_scroll_overlap(lp, -1);
	  newtopline = lp - off;
	  newcursorline = newtopline + so;
	  // curwin->w_valid &= ~(VALID_WCOL|VALID_CHEIGHT|VALID_WROW|
			       // VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
	}
      } else {	// dir == BACKWARDS
	lp = G.curwin.getViewTopLine() - 1;
	off = get_scroll_overlap(lp, 1);
	lp += off;
	if (lp > G.curwin.getLineCount())
	  lp = G.curwin.getLineCount();
	newcursorline = lp - so;
	n = 0;
	while (n <= G.curwin.getViewLines() && lp >= 1) {
	  n += plines(lp);
	  --lp;
	}
	if (n <= G.curwin.getViewLines()) {	    // at begin of file
	  newtopline = 1;
	  // curwin->w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE);
	} else if (lp >= G.curwin.getViewTopLine() - 2) {   // very long lines
	  newtopline = G.curwin.getViewTopLine() - 1;
	  comp_botline();
	  newcursorline = G.curwin.getViewBottomLine() - 1;
	  // curwin->w_valid &= ~(VALID_WCOL|VALID_CHEIGHT|
			       // VALID_WROW|VALID_CROW);
	} else {
	  newtopline = lp + 2;
	  // curwin->w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE);
	}
      }
    }

    // now adjust cursor locations
    if(newtopline > 0) {
      G.curwin.setViewTopLine(adjustTopLine(newtopline));
    }
    if(newcursorline > 0) {
      G.curwin.setCaretPosition(
		    G.curwin.getLineStartOffset(newcursorline));
    }

    cursor_correct();	// NEEDSWORK: implement
    Edit.beginline(BL_SOL | BL_FIX);
    // curwin->w_valid &= ~(VALID_WCOL|VALID_WROW|VALID_VIRTCOL);

    /*
     * Avoid the screen jumping up and down when 'scrolloff' is non-zero.
     */
    if (dir == FORWARD && G.curwin.getWCursor().getLine()
				< G.curwin.getViewTopLine() + so) {
      // scroll_cursor_top(1, FALSE);	// NEEDSWORK: onepage ("^f") cleanup
    }

    update_screen(VALID);
    return retval;
  }

  /*
   * Decide how much overlap to use for page-up or page-down scrolling.
   * This is symmetric, so that doing both keeps the same lines displayed.
   */
  private static int get_scroll_overlap(int lnum, int dir) {
    int		h1, h2, h3, h4;
    int		min_height = G.curwin.getViewLines() - 2;

    h1 = plines_check(lnum);
    if (h1 > min_height) {
      return 0;
    } else {
      h2 = plines_check(lnum + dir);
      if (h2 + h1 > min_height) {
	return 0;
      } else {
	h3 = plines_check(lnum + dir * 2);
	if (h3 + h2 > min_height) {
	  return 0;
	 } else {
	  h4 = plines_check(lnum + dir * 3);
	  if (h4 + h3 + h2 > min_height || h3 + h2 + h1 > min_height) {
	    return 1;
	  } else {
	    return 2;
	  }
	}
      }
    }
  }

  static void halfpage(boolean go_down, int Prenum) {
    Normal.do_xop("halfpage");

    int		scrolled = 0;
    int		i;
    int		n;
    int		room;

    int newtopline = -1;
    int newbotline = -1;
    int newcursorline = -1;

    if (Prenum != 0)
      G.curwin.setWPScroll((Prenum > G.curwin.getViewLines())
			   ?  G.curwin.getViewLines() : Prenum);
    n = (G.curwin.getWPScroll() <= G.curwin.getViewLines())
	      ?  G.curwin.getWPScroll() : G.curwin.getViewLines();

    validate_botline();
    room = G.curwin.getViewBlankLines();
    newtopline = G.curwin.getViewTopLine();
    newbotline = G.curwin.getViewBottomLine();
    newcursorline = G.curwin.getWCursor().getLine();
    if (go_down) {	    // scroll down
      while (n > 0 && newbotline <= G.curwin.getLineCount()) {
	i = plines(newtopline);
	n -= i;
	if (n < 0 && scrolled != 0)
	  break;
	++newtopline;
	// curwin->w_valid &= ~(VALID_CROW|VALID_WROW);
	scrolled += i;

	//
	// Correct w_botline for changed w_topline.
	//
	room += i;
	do {
	  i = plines(newbotline);
	  if (i > room)
	    break;
	  ++newbotline;
	  room -= i;
	} while (newbotline <= G.curwin.getLineCount());

	if (newcursorline < G.curwin.getLineCount()) {
	  ++newcursorline;
	  // curwin->w_valid &= ~(VALID_VIRTCOL|VALID_CHEIGHT|VALID_WCOL);
	}
      }

      //
      // When hit bottom of the file: move cursor down.
      //
      if (n > 0) {
	newcursorline += n;
	if(newcursorline > G.curwin.getLineCount()) {
	  newcursorline = G.curwin.getLineCount();
	}
      }
    } else {	    // scroll up
      while (n > 0 && newtopline > 1) {
	i = plines(newtopline - 1);
	n -= i;
	if (n < 0 && scrolled != 0)
	  break;
	scrolled += i;
	--newtopline;
	// curwin->w_valid &= ~(VALID_CROW|VALID_WROW|
			     // VALID_BOTLINE|VALID_BOTLINE_AP);
	if (newcursorline > 1) {
	  --newcursorline;
	  // curwin->w_valid &= ~(VALID_VIRTCOL|VALID_CHEIGHT|VALID_WCOL);
	}
      }
      //
      // When hit top of the file: move cursor up.
      //
      if (n > 0) {
	if (newcursorline > n)
	  newcursorline -= n;
	else
	  newcursorline = 1;
      }
    }
    G.curwin.setViewTopLine(newtopline);
    G.curwin.setCaretPosition(
		    G.curwin.getLineStartOffset(newcursorline));
    cursor_correct();
    Edit.beginline(BL_SOL | BL_FIX);
    update_screen(VALID);

  }

  static void update_curswant() {
    if (G.curwin.getWSetCurswant()) {
      //int vcol = getvcol();
      // G.curwin.setWCurswant(G.curwin.getWCursor().getColumn());
      MutableInt mi = new MutableInt();
      getvcol(G.curwin, G.curwin.getWCursor(), null, mi, null);
      G.curwin.setWCurswant(mi.getValue());
      G.curwin.setWSetCurswant(false);
    }
  }

  /**
   * For the current character offset in the current line,
   * calculate the virtual offset. That is the offset if
   * tabs are expanded. I *think* this is equivelent to getvcolStart(int).
   *
   * @deprecated
   * use getvcol(ViTextView, ViFPOS, MutableInt, MutableInt, MutableInt)
   */
  static int getvcol() {
    return getvcol(G.curwin.getWCursor().getColumn());
  }

  /**
   * This method returns the start vcol of param for current line
   * @deprecated
   * use getvcol(ViTextView, ViFPOS, MutableInt, MutableInt, MutableInt)
   */
  static int getvcol(int endCol) {
    int vcol = 0;
    MySegment seg = G.curwin.getLineSegment(G.curwin.getWCursor().getLine());
    int ptr = seg.offset;
    int idx = -1;
    char c;
    while (idx < endCol - 1
	   && idx < seg.count - 1
	   && (c = seg.array[ptr]) != '\n')
    {
      ++idx;
      /* Count a tab for what it's worth (if list mode not on) */
      vcol += lbr_chartabsize(c, vcol);
      ++ptr;
    }
    return vcol;
  }

  /*static void getvcol(ViFPOS fpos,
                      MutableInt start,
                      MutableInt cursor,
                      MutableInt end) {
    getvcol(G.curwin, fpos, start, cursor, end);
  }*/
  
  /**
   * Get virtual column number of pos.
   *  start: on the first position of this character (TAB, ctrl)
   * cursor: where the cursor is on this character (first char, except for TAB)
   *    end: on the last position of this character (TAB, ctrl)
   *
   * This is used very often, keep it fast!
   *
   * I think cursor is wrong for swing.
   *
   * Determine the virtual column positions of the begin and end
   * of the character at the given position. The begin and end may
   * be different when the character is a TAB. The values are returned
   * through the start, end parameters.
   *
   * @param tv the textview to use for fpos.
   * @param fpos the row and column/index of char to determin vcol for
   * @param start store the start vcol here, may be null
   * @param cursor store the cursor vcol here, may be null
   * @param end store the end vcol here, may be null
   */
  public static void getvcol(ViTextView tv,
                             ViFPOS fpos,
                             MutableInt start,
                             MutableInt cursor,
                             MutableInt end) {
    int incr = 0;
    int vcol = 0;
    char c = 0;
    
    int ts = tv.getBuffer().b_p_ts;
    MySegment seg = tv.getLineSegment(fpos.getLine());
    for (int col = fpos.getColumn(), ptr = seg.offset; ; --col, ++ptr) {
      c = seg.array[ptr];
      // make sure we don't go past the end of the line
      if (c == '\n') {
        incr = 1;	// NUL at end of line only takes one column
        break;
      }
      // A tab gets expanded, depending on the current column
      if (c == TAB)
        incr = ts - (vcol % ts);
      else {
        //incr = CHARSIZE(c);
        incr = 1; // assuming all chars take up one space.
      }
// #ifdef MULTI_BYTE ... #endif
      
      if (col == 0)	// character at pos.col
        break;
      
      vcol += incr;
    }
    if(start != null)
      start.setValue(vcol);
    if(end != null)
      end.setValue(vcol + incr - 1);
    if (cursor != null) {
      if (c == TAB && ((G.State & NORMAL) != 0) // && !wp->w_p_list
              && !(G.VIsual_active && G.p_sel.charAt(0) == 'e'))
        cursor.setValue(vcol + incr - 1);	    // cursor at end
      else
        cursor.setValue(vcol);	    // cursor at start
    }
  }

  private static MutableInt l1 = new MutableInt();
  private static MutableInt l2 = new MutableInt();
  private static MutableInt r1 = new MutableInt();
  private static MutableInt r2 = new MutableInt();
  /**
   * Get the most left and most right virtual column of pos1 and pos2.
   * Used for Visual block mode.
   */
  static void getvcols(ViFPOS pos1,
                       ViFPOS pos2,
                       MutableInt left,
                       MutableInt right) {
    getvcol(G.curwin, pos1, l1, null, r1);
    getvcol(G.curwin, pos2, l2, null, r2);

    left.setValue(l1.compareTo(l2) < 0 ? l1.getValue() : l2.getValue());
    right.setValue(r1.compareTo(r2) > 0 ? r1.getValue() : r2.getValue());
  }
  
  /**
   * show the current mode and ruler.
   * <br>
   * If clear_cmdline is TRUE, clear the rest of the cmdline.
   * If clear_cmdline is FALSE there may be a message there that needs to be
   * cleared only if a mode is shown.
   * @return the length of the message (0 if no message).
   */
  static int showmode()       {
    String mode = Edit.VI_MODE_COMMAND;
    int length = 0;
    boolean do_mode = (true/*G.p_smd*/
	  && ((G.State & INSERT) != 0 || G.restart_edit != 0
	      	|| G.VIsual_active));
    if(do_mode /*|| Recording*/) {
      // wait a bit before overwriting an important message
      check_for_delay(false);

      if(do_mode) {
	if (G.State == INSERT) mode = Edit.VI_MODE_INSERT;
	else if (G.State == REPLACE) mode = Edit.VI_MODE_REPLACE;
	else if (G.State == VREPLACE) mode = Edit.VI_MODE_VREPLACE;
	else if (G.restart_edit == 'I') mode = Edit.VI_MODE_RESTART_I;
	else if (G.restart_edit == 'R') mode = Edit.VI_MODE_RESTART_R;
	else if (G.restart_edit == 'V') mode = Edit.VI_MODE_RESTART_V;

        if (G.VIsual_active)
        {
            if (G.VIsual_select) mode = Edit.VI_MODE_SELECT;
            else mode = Edit.VI_MODE_VISUAL;
            
            // It may be "VISUAL BLOCK" or "VISUAl LINE"
            if (G.VIsual_mode == (0x1f & (int)('V'))) // Ctrl('V')
                mode += " " + Edit.VI_MODE_BLOCK;
            else if (G.VIsual_mode == 'V')
                mode += " " + Edit.VI_MODE_LINE;
        }
      }

    }

    // Any "recording" string is handled by the disply function
    // if(G.Recording) {
    //   mode += "recording";
    // }
    
    G.curwin.getStatusDisplay().displayMode(mode);
    G.clear_cmdline = false;
    return 0;
  }

  static void update_screen(int type) {
    // NEEDSWORK: update_screen: think this is a nop
    return;
  }

  /**
   * Correct the cursor position so that it is in a part of the screen at least
   * 'so' lines from the top and bottom, if possible.
   * If not possible, put it at the same position as scroll_cursor_halfway().
   * When called topline must be valid!
   */
  static void cursor_correct() {
    // NEEDSWORK: cursor_correct: handle p_so and the rest
    return;
  }

  static void validate_botline() {
    // NEEDSWORK: validate_botline: think this is a nop
    comp_botline();
    return;
  }

  private static void comp_botline() {
    // NEEDSWORK: comp_botline: think this is a nop
    return;
  }

  static void check_for_delay(boolean check_msg_scroll) {
  }

  //////////////////////////////////////////////////////////////////
  //
  // "undo.c"
  //

  static void u_undo(int count) {
    int			old_lcount = G.curwin.getLineCount();

    while(count-- > 0) {
      G.curwin.undo();
    }

    msgmore(G.curwin.getLineCount() - old_lcount);
  }

  static void u_redo(int count) {
    int			old_lcount = G.curwin.getLineCount();

    while(count-- > 0) {
      G.curwin.redo();
    }

    msgmore(G.curwin.getLineCount() - old_lcount);
  }

  //////////////////////////////////////////////////////////////////
  //
  // "charset.c"
  //


  /**
   * return TRUE if 'c' is a keyword character: Letters and characters from
   * 'iskeyword' option for current buffer.
   * <p>NOTE: hardcode for now to typical program identifier settings
   * </p>
   */
  static boolean vim_iswordc(int c) {
    return
	      	'a' <= c && c <= 'z'
	      	|| 'A' <= c && c <= 'Z'
		|| '0' <= c && c <= '9'
	      	|| '_' == c
		;
  }

  /**
   * Catch 22: chartab[] can't be initialized before the options are
   * initialized, and initializing options may cause transchar() to be called.
   * When chartab_initialized == FALSE don't use chartab[].
   * <br>
   * NOTE: NEEDSWORK: jbvi modified to never use chartab
   */
  static String transchar(int c) {
    StringBuffer buf = new StringBuffer();

    /* ***************************************************************
    i = 0;
    if (IS_SPECIAL(c))	    // special key code, display as ~@ char
    {
      buf[0] = '~';
      buf[1] = '@';
      i = 2;
      c = K_SECOND(c);
    }
    ******************************************************************/

    if ((! false /*chartab_initialized*/ && ((c >= ' ' && c <= '~')))
		/*|| (chartab[c] & CHAR_IP)*/)    // printable character
    {
      buf.append((char)c);
    } else {
      transchar_nonprint(buf, c);
    }
    return buf.toString();
  }

  static void transchar_nonprint(StringBuffer buf, int c) {
    if (c <= 0x7f) {				    // 0x00 - 0x1f and 0x7f
      /* *****************************************************************
      if (c == NL) {
	c = NUL;			// we use newline in place of a NUL
      } else if (c == CR && get_fileformat(curbuf) == EOL_MAC) {
	c = NL;		// we use CR in place of  NL in this case
      }
      ********************************************************************/
      buf.append('^').append((char)(c ^ 0x40)); // DEL displayed as ^?
    } else if (c >= ' ' + 0x80 && c <= '~' + 0x80) {    // 0xa0 - 0xfe
      buf.append('|').append((char)(c - 0x80));
    } else {					    // 0x80 - 0x9f and 0xff
      buf.append('~').append((char)((c-0x80) ^ 0x40)); // 0xff displayed as ~?
    }
  }

  /**
   * Return the number of characters 'c' will take on the screen, taking
   * into account the size of a tab.
   * Use a define to make it fast, this is used very often!!!
   * Also see getvcol() below.
   */

  static int lbr_chartabsize(int c, int col) {
    if (c == TAB && (!G.curwin.getWPList() /*|| lcs_tab1*/)) {
      int ts = G.curbuf.b_p_ts;
      return (int)(ts - (col % ts));
    } else {
      // return charsize(c);
      return 1;
    }
  }

/*
 * return the number of characters the string 's' will take on the screen,
 * taking into account the size of a tab
 */
  static int linetabsize(MySegment seg)
  {
    int col = 0;
    char ch;
    
    ch = seg.first();
    while(ch != seg.DONE) {
      col += lbr_chartabsize(ch, col);
      ch = seg.next();
    }

    return col;
  }

  public static boolean vim_iswhite(int c) {
    return c == ' ' || c == '\t';
  }

  /**
   * Skip over ' ' and '\t', return index, relative to
   * seg.offset, of next non-white.
   */
  static int skipwhite(MySegment seg) {
      return skipwhite(seg, 0);
  }
  static int skipwhite(MySegment seg, int idx) {
    for(; idx < seg.count; idx++) {
      if(!vim_iswhite(seg.array[seg.offset + idx]))
        return idx;
    }
    /*NOTREACHED*/
    throw new RuntimeException("no newline?");
  }

  /**
   * Skip over ' ' and '\t', return index next non-white.
   * This is only used for specialized parsing, not part of main vi engine.
   */
  static int skipwhite(String str, int idx) {
    for(; idx < str.length(); idx++) {
      if( ! vim_iswhite(str.charAt(idx))) {
        return idx;
      }
    }
    return idx;
  }

  /**
   * Skip over not ' ' and '\t', return index next non-white.
   */
  static int skiptowhite(String str, int idx) {
    for(; idx < str.length(); idx++) {
      if(vim_iswhite(str.charAt(idx))) {
        return idx;
      }
    }
    return idx;
  }

  /**
   * Getdigits: Get a number from a string and skip over it.
   * Note: the argument is a pointer to a char_u pointer!
   */

  static int getdigits(String s, int sidx, MutableInt mi) {
      int rval = 0;
      boolean isneg = false;

      int v = s.charAt(sidx);
      if(v == '-' || v == '+') {
        if(v == '-') {
          isneg = true;
        }
        ++sidx;
      }
      
      while(true) {
        if(sidx >= s.length()) {
          break;
        }
        v = s.charAt(sidx);
        if( ! Util.isdigit(v)) {
          break;
        }
        rval = 10 * rval + v - '0';
        ++sidx;
      }
      
      if(isneg) {
        rval = -rval;
      }
      mi.setValue(rval);
      return sidx;
  }

  //////////////////////////////////////////////////////////////////
  //
  // "ex_getln.c"
  //

  static boolean cmdline_at_end() {
    return false;
  }

  static boolean cmdline_overstrike() {
    return false;
  }

  //////////////////////////////////////////////////////////////////
  //
  // term.c
  //

  // NEEDSWORK: commandCharacters should be per edit window
  private static String commandCharacters;
  private static boolean ccDirty = false;

  /** update the command characters */
  static void setCommandCharacters(String s) {
    commandCharacters = s;
    ccDirty = true;
  }

  /** This is used to do showcommand stuff. */
  public static void out_flush() {
    if(ccDirty) {
      G.curwin.getStatusDisplay().displayCommand(commandCharacters);
    }
    ccDirty = false;
  }

  //////////////////////////////////////////////////////////////////
  //
  // window.c
  //

  static void do_window(int nchar, int Prenum) {
    Normal.do_xop("do_window");
    switch(nchar) {
      // split current window in two parts
      case 'S':
      case 's':
      case 'S' & 0x1f:          // Ctrl
        G.curwin.win_split(Prenum);
        break;

      // close current window
      case 'c':
      case 'C' & 0x1f:
        GetChar.stuffReadbuff(":close\n");
        break;

      // close all but current window
      case 'O':
      case 'o':
        GetChar.stuffReadbuff(":only\n");
        break;

      // cursor to next window with wrap around
      case 'W' & 0x1f:
      case 'w':
        G.curwin.win_cycle(Prenum);
        break;
    }
  }

  //////////////////////////////////////////////////////////////////
  //
  // other stuff
  //
  
  // inUndoCount is relatd to beginRedoUndo. During a redoUndo can get
  // regular undo's. Would still like to check for proper undo state stuff,
  // although the checking is what seems flakey.
  private static int inUndoCount;
  static void beginUndo() {
    if(G.global_busy) {
      return;
    }
    if(inUndoCount > 0)
        ViManager.dumpStack("inUndoCount = " + inUndoCount);
    inUndoCount++;
    if(!inRedo)
        G.curwin.beginUndo();
  }
  
  static void endUndo() {
    if(G.global_busy) {
      return;
    }
    inUndoCount--;
    if(inUndoCount > 0)
        ViManager.dumpStack("inUndoCount = " + inUndoCount);
    if(!inRedo)
        G.curwin.endUndo();
  }
  
  // There are interactions between insert and redo undo.
  // And they they do not nest.
  // However the last thing is always endRedoUndo.
  //
  // redo is atomic, it does not involve user interactions, and should
  // allow locking the file for any modifications. So when in a "redo"
  // trump beginInsertUndo with beginUndo.
  //
  
  private static boolean inRedo = false;
  
  /** Currently in insertUndo? Since using inRedo directly, there will be
   * some false positives. This method is used for some consistency checking
   * and the false positives won't cause the assert.
   */
  static boolean isInInsertUndo() {
      return G.curwin.isInInsertUndo() || inRedo;
  }
  
  static void beginInsertUndo() {
    if(G.global_busy || inRedo) {
      return;
    }
    G.curwin.beginInsertUndo();
  }
  
  static void endInsertUndo() {
    if(G.global_busy || inRedo) {
      return;
    }
    G.curwin.endInsertUndo();
  }
  
  //
  // begin/endRedoUndo indicate changes that can be treated atomically,
  // but they might use the "Edit" command. So need to handle interactions
  // the other begin/endUndo.
  //
  static void beginRedoUndo() {
    if(G.global_busy) {
      return;
    }
    if(G.curwin.isInInsertUndo()) {
        endInsertUndo();
    }
    inRedo = true;
    G.curwin.beginUndo();
  }
  
  static void endRedoUndo() {
    if(G.global_busy) {
      return;
    }
    inRedo = false;
    G.curwin.endUndo();
  }
  
  static int[] javaKeyMap;
  
  /**
   * decode the vi key character embedded
   * in a unicode character and
   * return a kestroke.
   */
  static KeyStroke getKeyStroke(int vikey, int modifiers) {
    //int modifiers = (vikey >> MODIFIER_POSITION_SHIFT) & 0x0f;
    int vikeyIdx = vikey & 0xff;
    if(vikeyIdx < javaKeyMap.length) {
      int k = javaKeyMap[vikeyIdx];
      if(k != 0) {
	return KeyStroke.getKeyStroke(k, modifiers);
      }
    }
    return null;
  }
  
  /**
   * Translate the vi internal key, see {@link KeyDefs}, into a swing keystroke.
   * @param vikey 
   */
  static int xlateViKey(Keymap map, int vikey, int modifiers) {
    if(map == null) {
      return vikey;
    }
    if((vikey & 0xf000) != VIRT && vikey >= 0x20) {
      return vikey;
    }
    KeyStroke ks;
    if(vikey < 0x20) {
      ks = KeyStroke.getKeyStroke(controlKey[vikey], CTRL);
    } else {
      ks = getKeyStroke(vikey, modifiers);
    }
    if(ks == null) {
      return vikey;
    }
    ActionListener al = map.getAction(ks);
    if(al == null) {
      return vikey;
    }
    ViXlateKey xk = (ViXlateKey)ViManager.xlateKeymapAction(al);
    return xk.getXlateKey();
  }
  
  final private static int[] controlKey = new int[] {
    KeyEvent.VK_AT,
    KeyEvent.VK_A,
    KeyEvent.VK_B,
    KeyEvent.VK_C,
    KeyEvent.VK_D,
    KeyEvent.VK_E,
    KeyEvent.VK_F,
    KeyEvent.VK_G,
    KeyEvent.VK_H,
    KeyEvent.VK_I,
    KeyEvent.VK_J,
    KeyEvent.VK_K,
    KeyEvent.VK_L,
    KeyEvent.VK_M,
    KeyEvent.VK_N,
    KeyEvent.VK_O,
    KeyEvent.VK_P,
    KeyEvent.VK_Q,
    KeyEvent.VK_R,
    KeyEvent.VK_S,
    KeyEvent.VK_T,
    KeyEvent.VK_U,
    KeyEvent.VK_V,
    KeyEvent.VK_W,
    KeyEvent.VK_X,
    KeyEvent.VK_Y,
    KeyEvent.VK_Z,
    KeyEvent.VK_OPEN_BRACKET,
    KeyEvent.VK_BACK_SLASH,
    KeyEvent.VK_CLOSE_BRACKET,
    KeyEvent.VK_CIRCUMFLEX,
    KeyEvent.VK_UNDERSCORE
  };
  
    /* may insert some spaces before the new text */
    private static void copy_spaces(StringBuffer ptr, int startspaces) {
        //System.out.println("Copy spaces: "+ startspaces +" / "+ ptr.length());
      while(startspaces-- > 0) {
        ptr.append(' ');
        }
      //System.out.println("-->copy_spaces:'"+ptr+"'");
    }
  
  static class block_def
{
    int       startspaces;   /* 'extra' cols of first char */
    int       endspaces;     /* 'extra' cols of first char */
    int       textlen;       /* chars in block */
    int    textstart;    /* pointer to 1st char in block */
    int    textcol;       /* cols of chars (at least part.) in block */
    int    start_vcol;    /* start col of 1st char wholly inside block */
    int    end_vcol;      /* start col of 1st char wholly after block */
    boolean       is_short;      /* TRUE if line is too short to fit in block */
    boolean       is_MAX;       /* TRUE if curswant==MAXCOL when starting */
    boolean       is_EOL;       /* TRUE if cursor is on NUL when starting */
    boolean       is_oneChar;    /* TRUE if block within one character */
    int       pre_whitesp;   /* screen cols of ws before block */
    int       pre_whitesp_c; /* chars of ws before block */
    int    end_char_vcols;/* number of vcols of post-block char */
    int    start_char_vcols; /* number of vcols of pre-block char */
};

/*
 * prepare a few things for block mode yank/delete/tilde
 *
 * for delete:
 * - textlen includes the first/last char to be (partly) deleted
 * - start/endspaces is the number of columns that are taken by the
 *   first/last deleted char minus the number of columns that have to be
 *   deleted.  for yank and tilde:
 * - textlen includes the first/last char to be wholly yanked
 * - start/endspaces is the number of columns of the first/last yanked char
 *   that are to be yanked.
 */
    static void block_prep(OPARG oap, block_def bdp, int lnum, boolean is_del) {
      System.out.println("block prep: "+ oap.end_vcol +" -> "+ (G.curwin.getLineEndOffset(lnum) - G.curwin.getLineStartOffset(lnum)));
    int   incr = 0;
    StringBuffer pend;
    StringBuffer pstart;

    bdp.startspaces = 0;
    bdp.endspaces = 0;
    bdp.textlen = 0;
    bdp.textcol = 0;
    bdp.start_vcol = 0;
    bdp.end_vcol = 0;
    bdp.is_short = false;
    bdp.is_oneChar = false;
    bdp.pre_whitesp = 0;
    bdp.pre_whitesp_c = 0;

    pstart = new StringBuffer(Util.ml_get(lnum).toString().substring(0, Util.lineLength(lnum)));//Util.ml_get(lnum).toString());
    int pstart_idx = 0;
    int pend_idx = 0;
    while (bdp.start_vcol < oap.start_vcol && pstart_idx < pstart.length())
    {
        /* Count a tab for what it's worth (if list mode not on) */
        incr = lbr_chartabsize(pstart.charAt(pstart_idx), bdp.start_vcol);
        bdp.start_vcol += incr;
        ++bdp.textcol;
        if (vim_iswhite(pstart.charAt(pstart_idx)))
        {
            bdp.pre_whitesp += incr;
            bdp.pre_whitesp_c++;
        }
        else
        {
            bdp.pre_whitesp = 0;
            bdp.pre_whitesp_c = 0;
        }
        pstart_idx++;//++pstart;
    }
    bdp.start_char_vcols = incr;
    if (bdp.start_vcol < oap.start_vcol) /* line too short */
    {
        bdp.is_short = true;
        if (!is_del || oap.op_type == OP_APPEND)
            bdp.endspaces = oap.end_vcol - oap.start_vcol + 1;
    }
    else
    {
        bdp.startspaces = bdp.start_vcol - oap.start_vcol;
        if (is_del && bdp.startspaces > 0)
            bdp.startspaces = bdp.start_char_vcols - bdp.startspaces;
        pend = pstart;
        bdp.end_vcol = bdp.start_vcol;
        if (bdp.end_vcol > oap.end_vcol) /* it's all in one character */
        {
            bdp.is_oneChar = true;
            if (oap.op_type == OP_INSERT)
                bdp.endspaces = bdp.start_char_vcols - bdp.startspaces;
            else if (oap.op_type == OP_APPEND)
            {
                bdp.startspaces += oap.end_vcol - oap.start_vcol + 1;
                bdp.endspaces = bdp.start_char_vcols - bdp.startspaces;
            }
            else
            {
                bdp.startspaces = oap.end_vcol - oap.start_vcol + 1;
                if (is_del && oap.op_type != OP_LSHIFT)
                    bdp.startspaces = bdp.start_char_vcols - bdp.startspaces;
            }
        }
        else
        {
            while (bdp.end_vcol <= oap.end_vcol && pend_idx < pend.length())
            {
                /* Count a tab for what it's worth (if list mode not on) */
                incr = lbr_chartabsize(pend.charAt(pend_idx), bdp.end_vcol);
                bdp.end_vcol += incr;
                pend_idx++;
            }
            if (bdp.end_vcol < oap.end_vcol
                    && (!is_del
                        || oap.op_type == OP_APPEND
                        || oap.op_type == OP_REPLACE)) /* line too short */
            {
                bdp.is_short = true;
                if (oap.op_type == OP_APPEND)
                    bdp.endspaces = oap.end_vcol - bdp.end_vcol;
                else {
                    // TODO_VIS vim is bdp.endspaces = 0
                    bdp.endspaces = oap.end_vcol+1
                            - (G.curwin.getLineEndOffset(lnum) 
                               - G.curwin.getLineStartOffset(lnum)); // was 0
                }
            }
            else if (bdp.end_vcol > oap.end_vcol)
            {
                bdp.endspaces = bdp.end_vcol - oap.end_vcol - 1;
                if (!is_del && pend != pstart && bdp.endspaces > 0)
                    --pend_idx;//--pend;
            }
        }
//#ifdef VISUALEXTRA
//        bdp->end_char_vcols = incr;
//#endif
        if (is_del && bdp.startspaces > 0)
        {
            --pstart_idx;//--pstart;
            --bdp.textcol;
        }
        bdp.textlen = pend_idx;// - pstart_idx;//(int)(pend - pstart);
    }
    bdp.textstart = pstart_idx;
}

    
  static void op_insert(OPARG oap, int count1) {
      op_insert_oap = oap.copy();
      
    String firstline, ins_text;
    op_insert_bd = new block_def();
    int i;

//    if (u_save((linenr_t)(oap->start.lnum - 1),
//                (linenr_t)(oap->end.lnum + 1)) == FAIL)
//        return;

    /* edit() changes this - record it for OP_APPEND */
    op_insert_bd.is_MAX = (G.curwin.getWCurswant() == MAXCOL);
    /* beyond EOL allowed in VIsual mode */
    op_insert_bd.is_EOL = (linetabsize(Util.ml_get_curline()) == (int)oap.end_vcol);

    /* vis block is still marked. Get rid of it now. */
//    curwin->w_cursor.lnum = oap->start.lnum;
    update_screen(VALID_TO_CURSCHAR);

    if (oap.block_mode) {
        /* Get the info about the block before entering the text */
        block_prep(oap, op_insert_bd, oap.start.getLine(), true);
        MySegment s = Util.ml_get(oap.start.getLine());
        firstline = s.subSequence(op_insert_bd.textcol, s.length()).toString();//Util.ml_get(oap.start.getLine()).subSequence(bd.textcol, );// + bd.textcol;
        if (oap.op_type == OP_APPEND)
            firstline += op_insert_bd.textlen;
        op_insert_pre_textlen = firstline.length();
    }
    
    if (oap.op_type == OP_APPEND)
    {
        if (oap.block_mode)
        {
            /* this lil bit if code adapted from nv_append() */
//            curwin->w_set_curswant = TRUE;
//            while (inc_cursor() == 0
//                    && (curwin->w_cursor.col < bd.textcol + bd.textlen))
//                ;
//            if (bd.is_short && !bd.is_MAX)
//            {
//                /* First line was too short, make it longer and adjust the
//                 * values in "bd". */
//                for (i = 0; i < bd.endspaces; ++i)
//                    ins_char(' ');
//                bd.textlen += bd.endspaces;
//            }
        }
        else
        {
//            curwin->w_cursor = oap->end;
//
//            /* Works just like an 'i'nsert on the next character. */
//            if (!lineempty(curwin->w_cursor.lnum)
//                    && oap->start_vcol != oap->end_vcol)
//                inc_cursor();
        }
    }
    beginInsertUndo(); // don't know where this should belong
    Edit.edit(NUL, false, count1);
  }
  
  static OPARG op_insert_oap;
  static block_def op_insert_bd;
  static int op_insert_pre_textlen;
  static void finishOpInsert() {
    long ins_len = 0;
    /* if user has moved off this line, we don't know what to do, so do
     * nothing */
    if (G.curwin.getWCursor().getLine() != op_insert_oap.start.getLine())
        return;


    if (op_insert_oap.block_mode)
    {
        block_def bd2 = new block_def();

        /*
         * Spaces and tabs in the indent may have changed to other spaces and
         * tabs.  Get the starting column again and correct the lenght.
         * Don't do this when "$" used, end-of-line will have changed.
         */
        block_prep(op_insert_oap, bd2, op_insert_oap.start.getLine(), true);
        if (!op_insert_bd.is_MAX || bd2.textlen < op_insert_bd.textlen)
        {
            if (op_insert_oap.op_type == OP_APPEND)
            {
                op_insert_pre_textlen += bd2.textlen - op_insert_bd.textlen;
                if (bd2.endspaces > 0)
                    --bd2.textlen;
            }
            op_insert_bd.textcol = bd2.textcol;
            op_insert_bd.textlen = bd2.textlen;
        }

        /*
         * Subsequent calls to ml_get() flush the firstline data - take a
         * copy of the required string.
         */
        //firstline = Util.ml_get(oap.start.lnum) + bd.textcol;
        MySegment s = Util.ml_get(op_insert_oap.start.getLine());
        String firstline = s.subSequence(op_insert_bd.textcol, s.length()).toString();//Util.ml_get(oap.start.getLine()).subSequence(bd.textcol, );// + bd.textcol;
        if (op_insert_oap.op_type == OP_APPEND)
            firstline += op_insert_bd.textlen;
        //System.out.println("First line : "+ firstline +" "+ ins_len +" = "+ firstline.length() +" - "+ (op_insert_pre_textlen - op_insert_bd.textcol));
        if ((ins_len = firstline.length() - (op_insert_pre_textlen-op_insert_bd.textcol)) > 0)
        {
            //System.out.println("First line : "+ ins_len);
            String ins_text = firstline.substring(0, (int)ins_len);
            block_insert(op_insert_oap, ins_text, (op_insert_oap.op_type == OP_INSERT) , bd2);
        }
    }
  }
    private static void mch_memmove(StringBuffer pnew, int lnum, int textstart, int textlen) {
        try {
            int linestart = G.curwin.getLineStartOffset(lnum);
            String tmp = G.curwin.getText(linestart + textstart, textlen);
            pnew.append(tmp);
            //pnew.append(G.curwin.getText(linestart + textstart, textlen));
        } catch(Exception e) {
        }
    }

    private static void mch_memmove(StringBuffer dst, int dstIndex,
                                    CharSequence src, int srcIndex,
                                    int len) {
                           // overlap, copy backwards
      if(src == dst && dstIndex > srcIndex && dstIndex < srcIndex + len) {
        dstIndex += len;
        srcIndex += len;
        while(len-- > 0)
          dst.setCharAt(--dstIndex, src.charAt(--srcIndex));
      } else                // copy forwards
        while(len-- > 0)
          dst.setCharAt(dstIndex++, src.charAt(srcIndex++));
    }

    private static void copy_spaces(StringBuffer dst, int index, int len) {
      while(len-- > 0)
        dst.setCharAt(index++, ' ');
    }

    private static void setLineNum(int line) {
        //int line = G.curwin.getLineNumber(G.curwin.getCaretPosition());
        int col = G.curwin.getColumnNumber(G.curwin.getCaretPosition());
        G.curwin.setCaretPosition(G.curwin.getLineStartOffset(line)+col);
    }
    
    static void block_insert(OPARG oap, String s, boolean b_insert, block_def bdp) {
        //int­­       p_ts;
        //int­­       count = 0;­     /* extra spaces to replace a cut TAB */
        //int­­       spaces = 0;­    /* non-zero if cutting a TAB */
        //colnr_t­    offset;­­       /* pointer along new line */
        int   s_len;    /* STRLEN(s) */
        //char_u­     *newp, *oldp;­  /* new, old lines */
        int   lnum;/* loop var */
        //int­­       oldstate = State;

        //State = INSERT;/* don't want REPLACE for State */
        s_len = s.length();
        //System.out.println("Block insert: "+ oap.start.getLine() +" .. "+ oap.end.getLine());
        for (lnum = oap.start.getLine() + 1; lnum <= oap.end.getLine(); lnum++) {
          int offset = G.curwin.getLineStartOffset(lnum) + bdp.start_vcol; // TODO plus column
          G.curwin.insertText(offset, s);
        }
    }

    /**
     * Replace a whole area with one character.
     * @param oap
     * @param c
     */
  public static final int op_replace(OPARG oap, int c) {
    // NOTE this method is absolutely not like vim
    // don't know how to match code below to vims implementation
    if (oap.block_mode) {
      char[] chars = new char[(oap.end_vcol - oap.start_vcol) + 1]; // +1 --> inclusive
      for (int i = 0; i < chars.length; i++) {
        chars[i] = (char)c;
      }
      for (int line = oap.start.getLine(); line <= oap.end.getLine(); line++) {
        block_def bd = new block_def();
        block_prep(oap, bd, line, true);
        int lineOffset = G.curwin.getLineStartOffset(line);
        G.curwin.replaceString(lineOffset + bd.start_vcol, lineOffset + bd.end_vcol, new String(chars));
      }
      
    } else {
      char[] chars = new char[oap.end.getOffset()-oap.start.getOffset()];
      for (int i = 0; i < chars.length; i++) {
        chars[i] = (char)c;
      }
      G.curwin.replaceString(oap.start.getOffset(), oap.end.getOffset(), new String(chars));
    }
    return OK;
  }

}

// vi: sw=2 ts=8
