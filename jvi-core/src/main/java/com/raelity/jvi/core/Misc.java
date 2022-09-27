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
package com.raelity.jvi.core;


import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.KeyStroke;
import javax.swing.text.Keymap;

import com.google.common.eventbus.Subscribe;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.ViCaretStyle;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViTextView.DIR;
import com.raelity.jvi.ViXlateKey;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.*;

import static java.util.logging.Level.*;

import static com.raelity.jvi.core.G.dbgUndo;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.CtrlChars.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;
import static com.raelity.jvi.manager.ViManager.eatme;
import static com.raelity.jvi.lib.TextUtil.sf;

// DONE lots of \n in this file
public class Misc {
    private static final Logger LOG = Logger.getLogger(Misc.class.getName());

    private Misc() {}

    //////////////////////////////////////////////////////////////////
    //
    // "misc1.c"
    //

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
        @Override
        public void init()
        {
            Misc.init();
        }
    }

    private static void init() {
        //ColonCommands.register("reg", "registers", new DoRegisters(), null);
        ViEvent.getBus().register(new EventHandlers());
        if(Boolean.FALSE)
          eatme(get_indent_lnum(0), skiptowhite("", 0));
    }

    private static class EventHandlers
    {

    @Subscribe
    public void lateInit(ViEvent.LateInit ev) {
      javaKeyMap = initJavaKeyMap();
      ViEvent.getBus().unregister(this);
    }
    }

    static List<String> readPrefsList(String nodeName,
                                      Wrap<PreferencesImportMonitor> pImportMonitor)
    {
      List<String> l = readList(nodeName);
      pImportMonitor.setValue(PreferencesImportMonitor.getMonitor(
              ViManager.getFactory().getPreferences(), nodeName));
      return l;
    }

    static void writePrefsList(String nodeName, Iterable<String> iterable,
                               PreferencesImportMonitor importMonitor)
    {
      if(!importMonitor.isChange()) {
        writeList(nodeName, iterable);
      } else {
        LOG.info(sf("jVi %s imported", nodeName));
      }
    }

  /**
   * count the size of the indent in the current line
   */
  static int get_indent() {
    return get_indent(G.curwin.w_cursor);
  }
  static int get_indent(ViFPOS fpos) {
      MySegment seg = G.curbuf.getLineSegment(fpos.getLine());
      return get_indent_str(seg);
  }

  /**
   * count the size of the indent in line "lnum"
   */
  static int get_indent_lnum(int lnum) {
      MySegment seg = G.curbuf.getLineSegment(lnum);
      return get_indent_str(seg);
  }

  /**
   * count the size of the indent in line "ptr"
   */
  private static int get_indent_str(MySegment seg) {
      int	    count = 0;

      int ptr = seg.offset;
      OUTER:
      for (; ptr < seg.offset + seg.count; ++ptr) {
        switch (seg.r(ptr)) {
        // count a tab for what it is worth
        case TAB:
          count += G.curbuf.b_p_ts - (count % G.curbuf.b_p_ts);
          break;
        case ' ':
          ++count;		// count a space for one
          break;
        default:
          break OUTER;
        }
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
    if(lnum < 1 || lnum > G.curbuf.getLineCount()) {
      return -1;
    }
    MySegment seg = G.curbuf.getLineSegment(lnum);
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
      if (seg.r(ptr) == '(') {
	// keep track of last paren seen
	prev_paren = count;
      }
      if (seg.r(ptr) == TAB) {  // count a tab for what it is worth
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
      OUTER:
      for (; ptr < seg.offset + seg.count; ++ptr) {
        switch (seg.r(ptr)) {
        case '(':
          found = true;
          break OUTER;
        case TAB:
          // count a tab for what it is worth
          count += G.curbuf.b_p_ts - (count % G.curbuf.b_p_ts);
          break;
        default:
          ++count;
          break;
        }
      }
    return found ? count : -1;
  }
  
  static int findFirstNonBlank(int lnum, int fromIndent, int dir) {
    if(lnum < 1 || lnum > G.curbuf.getLineCount()) {
      return -1;
    }
    MySegment seg = G.curbuf.getLineSegment(lnum);
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
      if(seg.r(ptr) != ' ' && seg.r(ptr) != TAB) {
          non_blank = count;
          break;
      }
      if (seg.r(ptr) == TAB) {  // count a tab for what it is worth
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
      if(seg.r(ptr) != ' ' && seg.r(ptr) != TAB) {
	found = true;
	break;
      }
      if (seg.r(ptr) == TAB)    // count a tab for what it is worth
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
    set_indent(size, del_first, G.curwin.w_cursor);
  }
  /** NOTE: fpos must be in curwin */
  static void set_indent(int size, boolean del_first, ViFPOS fpos) {
    int		oldstate = G.State;
    //int		c;

    fpos.verify(G.curbuf);
    G.State = INSERT;		    // don't want REPLACE for State
    int col = 0;
    // NEEDSWORK: note use of curbuf. but fpos not curwin
    MySegment seg = G.curbuf.getLineSegment(fpos.getLine());
    if (del_first) {		    // delete old indent
      // vim_iswhite() is a define!
      while(vim_iswhite(seg.fetch(col))) {
	col++;
      }
      // col is char past last whitespace
    }
    StringBuilder sb = new StringBuilder();
    if (!G.curbuf.b_p_et) {	    // if 'expandtab' is set, don't use TABs
      while (size >= G.curbuf.b_p_ts) {
	// NEEDSWORK:  how did b_p_et get set, dont expand tabs for now
	sb.append(TAB);
	size -= G.curbuf.b_p_ts;
      }
    }
    while (size > 0) {
      sb.append(' ');
      --size;
    }
    int offset = G.curbuf.getLineStartOffset(fpos.getLine());
    G.curbuf.replaceString(offset, offset + col, sb.toString());
    G.State = oldstate;
  }

  /**
   * open_line: Add a new line within, below or above the current line.
   * <p>
   * VREPLACE mode not supported.
   * </p><p>
   * For insert/replace put the newline wherever the cursor is. Otherwise,
   * create an empty line either before or after the current line, according
   * to dir.
   * </p>
   *
   * @return TRUE for success, FALSE for failure
   */
  static boolean open_line(int dir, boolean redraw, boolean del_spaces,
		    int old_indent)
  {
    boolean ok = true;
    if(G.State == INSERT || G.State == REPLACE) {
      // in input/insert mode, put the new line wherever we are
      G.curwin.insertNewLine();
    } else {
      ok = G.curwin.openNewLine(dir == FORWARD
                                ? DIR.FORWARD : DIR.BACKWARD);
    }
    if(ok)
      G.did_ai = true;
    return ok;
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
    final ViFPOS cursor = G.curwin.w_cursor;
    int stopline = cursor.getLine() + nlines - 1;
    int start = G.curbuf.getLineStartOffsetFromOffset(cursor.getOffset());
    int end = G.curbuf.getLineEndOffset(stopline);
    if(stopline >= G.curbuf.getLineCount()) {
      // deleting the last line is tricky
      --end; // can not delete the final new line
      if(start > 0) {
        --start; // delete the newline before the starting line to componsate
      }
    }
    G.curwin.deleteChar(start, end);
  }

  /** @return character at the position */
  static char gchar_pos(ViFPOS pos) {
    return Misc01.getCharAt(pos.getOffset());
  }

  /** @return character at the position */
  static char gchar_cursor() {
    ViFPOS pos = G.curwin.w_cursor;
    return gchar_pos(pos);
  }

  /**
   * Write a character at the current cursor position.
   * It is directly written into the block.
   */
  static void pchar_cursor(char c) {
    G.curwin.replaceChar(c, false);
  }

  static void pchar(ViFPOS fpos, char c) {
    int offset = fpos.getOffset();
    G.curwin.replaceString(offset, offset+1, String.valueOf(c));
    // do not change cursor position
    G.curwin.w_cursor.set(offset);
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
    @SuppressWarnings("empty-statement")
  static boolean inindent(int extra) {
      int	col;

      MySegment seg = G.curbuf.getLineSegment(G.curwin.w_cursor.getLine());
      for(col = 0; vim_iswhite(seg.fetch(col)); ++col);

      return col >= G.curwin.w_cursor.getColumn() + extra;
  }

  public static String plural(int n) {
    return n == 1 ? "" : "s";
  }

  static void ins_char(char c) {
    ins_char(c, false);
  }
  static void ins_char(char c, boolean ctrlv) {
    int extra;
    char curChar = 0;

    if (G.State != REPLACE || (curChar = Misc01.getChar()) == '\n') // DONE
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
      if(ctrlv) {
        String s = mapCtrlv(c);
        // following does doc.insert
        G.curwin.insertText(G.curwin.w_cursor.getOffset(), s);
      } else
        // following goes through key typed event, but some special tab handling
        G.curwin.insertChar(c);
    }
  }

  private static String mapCtrlv(char c)
  {
      // should also do stuff like
      // change an input F1 key to "<F1>"
      // Doesn't look like you can put a '\r' into a document?
      switch (c) {
      case K_TAB:
      case K_S_TAB:
        c = '\t';
        break;
      case K_KENTER:
      case CR:
        c = 13;  // carriage return
        break;
      case NL:
        c = 10; // newline
        break;
      }

    return String.valueOf(c);
  }

  static int del_char(boolean fixpos) {

    // return del_chars(1, fixpos);
    // just implement delete a single character

    ViFPOS fpos = G.curwin.w_cursor;

    // Can't do anything when the cursor is on the NUL after the line.
    if(Misc01.getChar() == '\n') { // DONE
      return FAIL;
    }

    // NEEDSWORK: could use a deleteNextChar
    int offset = fpos.getOffset();
    G.curwin.deleteChar(offset, offset+1);
    return OK;
  }

  static int del_chars(int count, boolean fixpos) {
    final ViFPOS cursor = G.curwin.w_cursor;
    int col = cursor.getColumn();
    MySegment oldp = Misc01.ml_get(cursor.getLine());
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
        G.curwin.w_cursor.set(oldOffset -1); //--curwin->w_cursor.col;
      count = oldlen - col;
      //movelen = 1;
    }
    G.curwin.deleteChar(oldOffset, oldOffset + count);

    return OK;
  }

  static void msgmore(int n) {
    int pn;

    if(G.global_busy) {
      // vim has a few more conditions for skipping this
      return;
    }

    if(n > 0) { pn = n; }
    else { pn = -n; }

    if(pn >= G.p_report) {
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
	   && (c = txt.r(ptr)) != '\n') // DONE
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

    // no hidden thing anymore, cursor can be inside a fold
    // idx = G.curwin.getFirstHiddenColumn(txt.docOffset, idx);

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
    return coladvanceColumnIndex(G.curwin.w_curswant, txt, null);
  }

  public static ViFPOS coladvance(int wcol) {
    return coladvance(G.curwin.w_cursor, wcol);
  }

  public static ViFPOS coladvance(ViFPOS fpos, int wcol)
  {
    return coladvance(fpos, wcol, null);
  }

  /** advance the fpos, note if fpos is w_cursor, may move the cursor */
  public static ViFPOS coladvance(ViFPOS fpos, int wcol,
                                  MutableBoolean hitTarget)
  {
    MySegment txt = G.curbuf.getLineSegment(fpos.getLine());
    int startColumn = 0;
    // NEEDSWORK: the following assert fires when used
    //           used for block mode stuff like "I" command.
    //           Doesn't seem to assert standalone
    //assert fpos.getColumn() == 0;
    if(G.False) {
      // WRAP issue. (OR IS IT...)
      // NOTE that if fpos.col is ever not zero
      //      then the txt segment starts *before* the col
      //      and coladvance starts at a screwy place.
      startColumn = fpos.getColumn();
      if(startColumn != 0) {
        // This is needed because of line wrap,
        // it may actually be needed in general.
        // coladvanceColumnIndex start from "txt.offset"
        txt.offset += startColumn;
        txt.count -= startColumn;
      }
    }
    int idx = coladvanceColumnIndex(wcol, txt, null);
    fpos.setColumn(startColumn + idx);
    if(hitTarget != null)
      hitTarget.setValue(idx == wcol);
    return fpos;
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
    ViFPOS fpos = G.curwin.w_cursor.copy();
    int rc = inc(fpos);
    G.curwin.w_cursor.set(fpos.getOffset()); // KEEP fpos.getOffset()
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
   * Return 1 when going to the next line.<br/>
   * Return 2 when moving forward onto a newline at the end of the line).<br/>
   * Return -1 when at the end of file.<br/>
   * Return 0 otherwise.
   * </p>
   */
  static int incV7(ViFPOS lp) {
    int currLine = lp.getLine();
    char c0 = Misc.gchar_pos(lp);

    if(c0 != '\n')    // still within line, move to next char (may be newline) // DONE
    {
      // #ifdef FEAT_MBYTE ... #endif

      lp.set(currLine, lp.getColumn() + 1);

      /* #ifdef FEAT_VIRTUALEDIT
	         lp->coladd = 0;
         #endif */

      return Misc.gchar_pos(lp) != '\n' ? 0 : 2; // DONE
    }
    if(currLine != G.curbuf.getLineCount())  // there is a next line
    {
      lp.set(currLine + 1, 0);

      /* #ifdef FEAT_VIRTUALEDIT
	         lp->coladd = 0;
         #endif */

      return 1;
    }
    return -1;
  }

  static int inc_cursorV7() {
    return incV7(G.curwin.w_cursor);
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
    ViFPOS fpos = G.curwin.w_cursor.copy();
    int rc = dec(fpos);
    G.curwin.w_cursor.set(fpos.getOffset()); // KEEP fpos.getOffset()
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

    // #ifdef FEAT_VIRTUALEDIT ... #endif

    if (currCol > 0) {                               // still within line
      lp.set(currLine, currCol - 1);

      // #ifdef FEAT_MBYTE ... #endif

      return 0;
    }
    if (currLine > 1) {                              // there is a prior line
      int newLine = currLine - 1;
      lp.set(newLine, Misc01.lineLength(newLine));

      // #ifdef FEAT_MBYTE ... #endif

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
    final ViFPOS cursor = G.curwin.w_cursor;
    int lnum = check_cursor_lnum(cursor.getLine());
    if(lnum > 0) {
      G.curwin.w_cursor.set(lnum, 0);
    }
    Edit.beginline(flags);
  }

  /**
   * Make sure curwin->w_cursor.lnum is valid.
   * @return correct line number.
   */
  static int check_cursor_lnum(int lnum) {
    if(lnum > G.curbuf.getLineCount()) {
      lnum = G.curbuf.getLineCount();
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
    int len;
    MySegment seg = G.curbuf.getLineSegment(lnum);
    len = seg.count - 1; // don't count trailing newline

    if (len == 0) {
      col = 0;
    } else if (col >= len) {
      // DONE
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
    final ViFPOS cursor = G.curwin.w_cursor;
    int lnum = check_cursor_lnum(cursor.getLine());
    int col = check_cursor_col(lnum, cursor.getColumn());
    if(lnum != cursor.getLine() || col != cursor.getColumn()) {
      cursor.set(lnum, col);
    }
  }


  private static ViCaretStyle[] cursor_table;

  /**
   * Parse options for cursor shapes.
   * <br><b>NEEDSWORK:</b><ul>
   * <li>actually parse something rather than hardcode shapes
   * </ul>
   */
  private static void parse_guicursor() {
    // Set them up according to the defaults.
    Cursor block = new Cursor(SHAPE_BLOCK, 0, 0);
    Cursor ver25_input = new Cursor(SHAPE_VER, 25, -1);
    Cursor ver35_visual = new Cursor(SHAPE_VER, 35, 0);
    Cursor hor20 = new Cursor(SHAPE_HOR, 20, 0);
    Cursor hor50 = new Cursor(SHAPE_HOR, 50, 0);

    cursor_table = new ViCaretStyle[SHAPE_COUNT];
    cursor_table[SHAPE_N] = block;
    cursor_table[SHAPE_V] = block;
    cursor_table[SHAPE_I] = ver25_input;
    cursor_table[SHAPE_R] = hor20;
    cursor_table[SHAPE_C] = block;
    cursor_table[SHAPE_CI] = ver25_input;
    cursor_table[SHAPE_CR] = hor20;
    cursor_table[SHAPE_SM] = block;	// plus different blink rates
    cursor_table[SHAPE_O] = hor50;
    cursor_table[SHAPE_VE] = ver35_visual;
  }

  /**
   * @return the description of the cursor to draw
   */
  private static ViCaretStyle getCursor() {
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

      // If there's a java selection
      if(G.curwin.hasSelection()) {
        return SHAPE_VE;
      }

      return SHAPE_N;
  }

  /** Read an order list from prefs; no dups. */
  private static List<String> readList(String nodeName)
  {
    List<String> l = new ArrayList<>();
    Set<String> check = new HashSet<>();
    try {
      Preferences prefs = ViManager.getFactory().getPreferences().node(nodeName);
      int nKey = prefs.keys().length;
      for(int i = 1; i <= nKey; i++) {
        String s = prefs.get("" + i, "");
        if(!s.isEmpty()) {
          // avoid duplicates,
          // though they should never have gotten in there in the first place.
          if(check.add(s))
            l.add(s);
        }
      }
    } catch(BackingStoreException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return l;
    }

  /** Write an ordered list to prefs. */
  private static void writeList(String nodeName, Iterable<String> iterable)
  {
    assert iterable != null;
    try {
      Preferences prefs = ViManager.getFactory().getPreferences().node(nodeName);
      prefs.removeNode();
      prefs = ViManager.getFactory().getPreferences().node(nodeName);

      int i = 1;
      for(String val : iterable) {
        prefs.put(String.valueOf(i), val);
        i++;
      }
      prefs.flush();
    } catch(IllegalStateException ex) {
    } catch(BackingStoreException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  static int check_fname()
  {
    File f = G.curbuf.getFile();
    if(f == null || !f.isFile()) {
      Msg.emsg(Messages.e_noname);
      return FAIL;
    }
    return OK;
  }

  //////////////////////////////////////////////////////////////////
  //
  // "ui.c"
  //

  static void ui_cursor_shape() {
    G.curwin.updateCursor(getCursor());
  }

    //////////////////////////////////////////////////////////////////
    //
    // "charset.c"
    //
    
    //
    // For the current character offset in the current line,
    // calculate the virtual offset. That is the offset if
    // tabs are expanded. I *think* this is equivelent to getvcolStart(int).
    //
    
    /**
     * Get virtual column number of pos.
     *  start: on the first position of this character (TAB, ctrl)
     * cursor: where the cursor is on this character (first char, except for TAB)
     *    end: on the last position of this character (TAB, ctrl)
     *
     * This is used very often, keep it fast!
     * 
     * From vim's charset.c
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
      getvcol(((TextView)tv).w_buffer, fpos, start, cursor, end);
    }

    public static void getvcol(Buffer buf,
            ViFPOS fpos,
            MutableInt start,
            MutableInt cursor,
            MutableInt end) {
      int incr = 0;
      int vcol = 0;
      char c = 0;
      
      int ts = buf.b_p_ts;
      MySegment seg = buf.getLineSegment(fpos.getLine());
      for (int col = fpos.getColumn(), ptr = seg.offset; ; --col, ++ptr) {
        c = seg.r(ptr);
        // make sure we don't go past the end of the line
        if (c == '\n') { // DONE
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
    
    private static final MutableInt l1 = new MutableInt();
    private static final MutableInt l2 = new MutableInt();
    private static final MutableInt r1 = new MutableInt();
    private static final MutableInt r2 = new MutableInt();
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
      eatme(length);
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
          
          if (G.VIsual_active) {
            if (G.VIsual_select) mode = Edit.VI_MODE_SELECT;
            else mode = Edit.VI_MODE_VISUAL;
            
            // It may be "VISUAL BLOCK" or "VISUAl LINE"
            if (G.VIsual_mode == CTRL_V)
              mode += " " + Edit.VI_MODE_BLOCK;
            else if (G.VIsual_mode == 'V')
              mode += " " + Edit.VI_MODE_LINE;
          }
        }
        
      }
      else {
        if(G.curwin.hasSelection())
          mode = ViManager.getFactory().getPlatformSelectionDisplayName();
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
    }
    
    static void check_for_delay(boolean check_msg_scroll) {
    }
    
    //////////////////////////////////////////////////////////////////
    //
    // "undo.c"
    //
    
    static void u_undo(int count) {
      int			old_lcount = G.curbuf.getLineCount();
      
      while(count-- > 0) {
        G.curbuf.undo();
      }
      
      msgmore(G.curbuf.getLineCount() - old_lcount);
    }
    
    static void u_redo(int count) {
      int			old_lcount = G.curbuf.getLineCount();
      
      while(count-- > 0) {
        G.curbuf.redo();
      }
      
      msgmore(G.curbuf.getLineCount() - old_lcount);
    }
    
    //////////////////////////////////////////////////////////////////
    //
    // "charset.c"
    //
    
    
    /**
     * return TRUE if 'c' is a keyword character: Letters and characters from
     * 'iskeyword' option for current buffer.
     */
    static boolean vim_iswordc(char c) {
      return G.curbuf.b_chartab.iswordc(c);
    }
    
    /**
     * Catch 22: chartab[] can't be initialized before the options are
     * initialized, and initializing options may cause transchar() to be called.
     * When chartab_initialized == FALSE don't use chartab[].
     * <br>
     * NOTE: NEEDSWORK: jbvi modified to never use chartab
     */
    static String transchar(char c) {
      StringBuilder buf = new StringBuilder();
      
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
        buf.append(c);
      } else {
        transchar_nonprint(buf, c);
      }
      return buf.toString();
    }
    
    static void transchar_nonprint(StringBuilder buf, char c) {
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
    
    static int lbr_chartabsize(char c, int col) {
      if (c == TAB && (!G.curwin.w_p_list /*|| lcs_tab1*/)) {
        int ts = G.curbuf.b_p_ts;
        return (ts - (col % ts));
      } else {
        // return charsize(c);
        return 1;
      }
    }
    
/*
 * return the number of characters the string 's' will take on the screen,
 * taking into account the size of a tab
 */
    static int linetabsize(MySegment seg) {
      int col = 0;
      char ch;
      
      ch = seg.first();
      while(ch != MySegment.DONE) {
        col += lbr_chartabsize(ch, col);
        ch = seg.next();
      }
      
      return col;
    }
    
    public static boolean vim_iswhite(char c) {
      return c == ' ' || c == '\t';
    }
    
    /**
     * Skip over ' ' and '\t', return index, relative to
     * seg.offset, of next non-white.
     * 
     * NOTE: CHAR ITER
     */
    @SuppressWarnings("empty-statement")
    static void skipwhite(CharacterIterator seg) {
      if(!vim_iswhite(seg.current()))
        return;
      while(vim_iswhite(seg.next()));
    }
    
    /**
     * Skip over ' ' and '\t', return index next non-white.
     * This is only used for specialized parsing, not part of main vi engine.
     */
    static int skipwhite(CharSequence str, int idx) {
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
      
      mi.setValue(rval);
      if(sidx >= s.length())
        return sidx;

      char v = s.charAt(sidx);
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
        if( ! Misc01.isdigit(v)) {
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
      if(!s.equals(commandCharacters))
        ccDirty = true;
      commandCharacters = s;
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
    // undo/redo stuff
    //

    private static int undoNesting;
    private static int insertUndoNesting;

    static boolean isInUndo() {
        return undoNesting != 0;
    }

    static boolean isInInsertUndo() {
      return insertUndoNesting != 0;
    }

    public static boolean isInAnyUndo() {
      return isInInsertUndo() || isInUndo();
    }

    private static void debugUndo(String tag) {
      dbgUndo.printf(CONFIG, () -> sf("%s: nesting: %d, inInsert: %d\n",
                                tag, undoNesting, insertUndoNesting));
    }

    public static void runUndoable(Runnable r) {
        beginUndo();
        try {
            G.curbuf.do_runUndoable(r);
        } finally {
            endUndo();
        }
    }

    //
    // beginUndo and endUndo are used only from runUndoable
    // these methods help control interactions with insertUndo
    //
    private static void beginUndo() {
        debugUndo("{Misc:beginUndo");
        checkUndoThreading();
        if(undoNesting == 0) {
            G.curbuf.do_beginUndo();
        }
        undoNesting++;

    }
    private static void endUndo() {
        undoNesting--;
        if(undoNesting == 0) {
            try {
              G.curbuf.do_endUndo();
            } finally {
              clearUndoThreading();
            }
        }
        debugUndo("}Misc:endUndo");
    }

    static void beginInsertUndo() {
      debugUndo("{Misc:beginInsertUndo");
      if(isInInsertUndo()) LOG.log(Level.SEVERE, "inInsertUndo", new Throwable());
      if(insertUndoNesting == 0) {
          G.curbuf.do_beginInsertUndo();
      }
      insertUndoNesting++;
    }

    /** Note: no guarantee that this is not called without a begin */
    static void endInsertUndo() {
      if(!isInInsertUndo()) LOG.log(
              Level.SEVERE, "!inInsertUndo", new Throwable());
      insertUndoNesting--;
      if(insertUndoNesting == 0) {
          G.curbuf.do_endInsertUndo();
      }
      debugUndo("}Misc:endInsertUndo");
    }

    private static Thread undoThread;
    private static synchronized void checkUndoThreading() {
      // NEEDSWORK: runUndoable check same thread and/or doc
      if(undoThread == null) {
        undoThread = Thread.currentThread();
      } else if(undoThread != Thread.currentThread()) {
        throw new IllegalStateException("undoThread is " + undoThread.getName());
      }
    }

    private static synchronized void clearUndoThreading() {
      // NEEDSWORK: clearUndoThreading verify in control thread?
      undoThread = null;
    }


    
    private static int[] javaKeyMap;
    
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
    static char xlateViKey(Keymap map, char vikey, int modifiers) {
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

  /*
   * This method return an array which maps special vi keys, from
   * {@link KeyDefs}, to Java KeyEventt keys, which are turned into key strokes.
   */
  private static int[] initJavaKeyMap() {
    int[] jk = new int[MAX_JAVA_KEY_MAP + 1];

    for(int i = 0; i < jk.length; i++) {
      jk[i] = -1;
    }

    jk[MAP_K_UP] = KeyEvent.VK_UP;
    jk[MAP_K_DOWN] = KeyEvent.VK_DOWN;
    jk[MAP_K_LEFT] = KeyEvent.VK_LEFT;
    jk[MAP_K_RIGHT] = KeyEvent.VK_RIGHT;
    jk[MAP_K_TAB] = KeyEvent.VK_TAB;
    jk[MAP_K_HOME] = KeyEvent.VK_HOME;
    jk[MAP_K_END] = KeyEvent.VK_END;
    // jk[MAP_K_S_UP] = KeyEvent.VK_S_UP;
    // jk[MAP_K_S_DOWN] = KeyEvent.VK_S_DOWN;
    // jk[MAP_K_S_LEFT] = KeyEvent.VK_S_LEFT;
    // jk[MAP_K_S_RIGHT] = KeyEvent.VK_S_RIGHT;
    // jk[MAP_K_S_TAB] = KeyEvent.VK_S_TAB;
    // jk[MAP_K_S_HOME] = KeyEvent.VK_S_HOME;
    // jk[MAP_K_S_END] = KeyEvent.VK_S_END;
    jk[MAP_K_F1] = KeyEvent.VK_F1;
    jk[MAP_K_F2] = KeyEvent.VK_F2;
    jk[MAP_K_F3] = KeyEvent.VK_F3;
    jk[MAP_K_F4] = KeyEvent.VK_F4;
    jk[MAP_K_F5] = KeyEvent.VK_F5;
    jk[MAP_K_F6] = KeyEvent.VK_F6;
    jk[MAP_K_F7] = KeyEvent.VK_F7;
    jk[MAP_K_F8] = KeyEvent.VK_F8;
    jk[MAP_K_F9] = KeyEvent.VK_F9;
    jk[MAP_K_F10] = KeyEvent.VK_F10;
    jk[MAP_K_F11] = KeyEvent.VK_F11;
    jk[MAP_K_F12] = KeyEvent.VK_F12;
    jk[MAP_K_F13] = KeyEvent.VK_F13;
    jk[MAP_K_F14] = KeyEvent.VK_F14;
    jk[MAP_K_F15] = KeyEvent.VK_F15;
    jk[MAP_K_F16] = KeyEvent.VK_F16;
    jk[MAP_K_F17] = KeyEvent.VK_F17;
    jk[MAP_K_F18] = KeyEvent.VK_F18;
    jk[MAP_K_F19] = KeyEvent.VK_F19;
    jk[MAP_K_F20] = KeyEvent.VK_F20;
    jk[MAP_K_F21] = KeyEvent.VK_F21;
    jk[MAP_K_F22] = KeyEvent.VK_F22;
    jk[MAP_K_F23] = KeyEvent.VK_F23;
    jk[MAP_K_F24] = KeyEvent.VK_F24;
    jk[MAP_K_HELP] = KeyEvent.VK_HELP;
    jk[MAP_K_UNDO] = KeyEvent.VK_UNDO;
    jk[MAP_K_BS] = KeyEvent.VK_BACK_SPACE;
    jk[MAP_K_INS] = KeyEvent.VK_INSERT;
    jk[MAP_K_DEL] = KeyEvent.VK_DELETE;
    jk[MAP_K_PAGEUP] = KeyEvent.VK_PAGE_UP;
    jk[MAP_K_PAGEDOWN] = KeyEvent.VK_PAGE_DOWN;
    jk[MAP_K_KPLUS] = KeyEvent.VK_PLUS;
    jk[MAP_K_KMINUS] = KeyEvent.VK_MINUS;
    jk[MAP_K_KDIVIDE] = KeyEvent.VK_DIVIDE;
    jk[MAP_K_KMULTIPLY] = KeyEvent.VK_MULTIPLY;
    jk[MAP_K_KENTER] = KeyEvent.VK_ENTER;
    jk[MAP_K_X_PERIOD] = KeyEvent.VK_PERIOD;
    jk[MAP_K_X_COMMA] = KeyEvent.VK_COMMA;
    jk[MAP_K_SPACE] = KeyEvent.VK_SPACE;

    return jk;
  }

    static void append_spaces(StringBuilder sb, int len)
    {
      while(len-- > 0)
        sb.append(' ');
    }

    static void append_chars(StringBuilder dst, int len, char c) {
      while(len-- > 0)
        dst.append(c);
    }
    
    static void mch_memmove(StringBuilder dst, int dstIndex,
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
    
    static void copy_spaces(StringBuilder dst, int index, int len) {
      while(len-- > 0)
        dst.setCharAt(index++, ' ');
    }
    
    static int STRLEN(StringBuilder sb) {
      int len = sb.indexOf("\0");
      return len >= 0 ? len : sb.length();
    }
    
    static void copy_chars(StringBuilder dst, int index, int len, char c) {
      while(len-- > 0)
        dst.setCharAt(index++, c);
    }
}

// vi: sw=2 ts=8
