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

import java.util.logging.Logger;

import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.text.MySegment;

import static com.raelity.jvi.core.JviClipboard.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Normal.*;
import static com.raelity.jvi.core.Register.*;
import static com.raelity.jvi.core.Util.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.Constants.NF.*;
import static com.raelity.jvi.core.lib.CtrlChars.*;
import static com.raelity.jvi.manager.ViManager.eatme;

public class Ops
{
    static final Logger LOG = Logger.getLogger(Ops.class.getName());

    private Ops() {}

    //////////////////////////////////////////////////////////////////
    //
    // "ops.c"
    //
    
  static class block_def {
    int       startspaces;   /* 'extra' cols of first char */
    int       endspaces;     /* 'extra' cols of first char */
    int       textlen;       /* chars in block */
    int       textstart;     /* pointer to 1st char in block */
    int       textcol;       /* cols of chars (at least part.) in block */
    int       start_vcol;    /* start col of 1st char wholly inside block */
    int       end_vcol;      /* start col of 1st char wholly after block */
    boolean   is_short;      /* TRUE if line is too short to fit in block */
    boolean   is_MAX;        /* TRUE if curswant==MAXCOL when starting */
    boolean   is_EOL;        /* TRUE if cursor is on NUL when starting */
    boolean   is_oneChar;    /* TRUE if block within one character */
    int       pre_whitesp;   /* screen cols of ws before block */
    int       pre_whitesp_c; /* chars of ws before block */
    int    end_char_vcols;   /* number of vcols of post-block char */
    int    start_char_vcols; /* number of vcols of pre-block char */
  };

  /**
   * op_shift - handle a shift operation
   */
  static void op_shift(OPARG oap, boolean curs_top, int amount) {
    int	    i;
    char    first_char;
    //int	    block_col = 0;

    int	    line;

    /*
       if (u_save((linenr_t)(curwin.w_cursor.lnum - 1),
       (linenr_t)(curwin.w_cursor.lnum + oap.line_count)) == FAIL)
       return;
     */


    if (oap.block_mode) {
    }

    line = G.curwin.w_cursor.getLine();
    // NOTE: the optimization of using copy() is around 12%
    //       need to do single insert to get a big win
    ViMark mStart = G.curbuf.createMark(oap.start);
    ViMark mEnd = G.curbuf.createMark(oap.end);
    ViFPOS fpos = G.curwin.w_cursor.copy();
    for (i = oap.line_count; --i >= 0; ) {
      fpos.set(line, 0);
      first_char = gchar_pos(fpos);
      if (first_char != '\n') {	// empty line // DONE
	shift_line(oap.op_type == OP_LSHIFT, G.p_sr, amount, fpos);
      }
      line++;
    }

    if (oap.block_mode) {
    } else if (curs_top) {    /* put cursor on first line, for ">>" */
      G.curwin.w_cursor.set(line - oap.line_count, 0);
      Edit.beginline(BL_SOL | BL_FIX); // shift_line() may have set cursor.col
    } else {
      G.curwin.w_cursor.set(line - 1, 0);
    }
    // update_topline();
    // update_screen(NOT_VALID);

    if (oap.line_count >= G.p_report) {
      Msg.smsg("" + oap.line_count + " line" + plural(oap.line_count)
	       + " " + ((oap.op_type == OP_RSHIFT) ? ">" : "<") + "ed "
	       + amount + " time" +  plural(amount));
    }

    /*
     * Set "'[" and "']" marks.
     */
    G.curbuf.b_op_start.setMark(mStart);
    G.curbuf.b_op_end.setMark(mEnd);
  }

  /**
   * shift the current line one shiftwidth left (if left != 0) or right
   * leaves cursor on first blank in the line
   */
  static void shift_line(boolean left, boolean round, int amount) {
    shift_line(left, round, amount, G.curwin.w_cursor);
  }
  static void shift_line(boolean left, boolean round, int amount, ViFPOS fpos) {
    int		count;
    int		i, j;
    int		p_sw = G.curbuf.b_p_sw;

    count = get_indent(fpos);	// get current indent

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
      set_indent(count, true, fpos);
    }
  }

  /**
   * Put a string into a yank register. First used from clip_get_selection.
   */

  /**
   * op_delete - handle a delete operation
   */
  static boolean op_delete(OPARG oap) {

    boolean		did_yank = true;
    int			old_lcount = G.curbuf.getLineCount();
    ViFPOS              opStartPos = null;
    ViFPOS              opEndPos = null;

    eatme(did_yank);
    /*
    if (curbuf.b_ml.ml_flags & ML_EMPTY)	    // nothing to do
      return OK;
    */

    // Nothing to delete, return here.	Do prepare undo, for op_change().
    if (oap.empty)
    {
      Normal.u_save_cursor();
      return true;
    }

    oap.regname = adjust_clip_reg(oap.regname);

    //
    // NEEDSWORK: op_delete: Imitate strange Vi: If the delete spans ....
    // Imitate the strange Vi behaviour: If the delete spans more than one line
    // and motion_type == MCHAR and the result is a blank line, make the delete
    // linewise.  Don't do this for the change command or Visual mode.
    //
    /*
    if (       oap.motion_type == MCHAR
	       && !oap.is_VIsual
	       && !oap.block_mode
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
	    && Util.getCharAt(G.curbuf.getLineStartOffset(
                                            oap.start.getLine())) == '\n') // DONE
    {
      //
      // It's an error to operate on an empty region, when 'E' inclucded in
      // 'cpoptions' (Vi compatible).
      //
      if (vim_strchr(G.p_cpo, 0, CPO_EMPTYREGION) >= 0) {
	Util.beep_flush();
        return false;
      }
      return true;
    }

    if(!valid_op_range(oap))
      return false;

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
	  return false;
	}
	get_yank_register(oap.regname, true); // yank into specif'd reg.
	if (op_yank(oap, true, false) == OK)   // yank without message
	  did_yank = true;
      }

      //
      // Put deleted text into register 1 and shift number registers if the
      // delete contains a line break, or when a regname has been specified!
      //
      if (oap.motion_type == MLINE || oap.line_count > 1 || oap.use_reg_one)
      {
        shift_delete_registers();
        if(op_yank(oap, true, false) == OK)
          did_yank = true;
      }

      // Yank into small delete register when no register specified and the
      // delete is within one line.
      // NOTE: this clipboard check is from vim9
      if ((isUnnamed(oap.regname) || oap.regname == 0)
              && oap.motion_type != MLINE && oap.line_count == 1) {
        // yank into unnamed register
	oap.regname = '-';		// use special delete register
	get_yank_register(oap.regname, true);
        if(op_yank(oap, true, false) == OK)
          did_yank = true;
	oap.regname = 0;
      }

      eatme(did_yank);

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
      block_delete(oap);
    } else if (oap.motion_type == MLINE) {
      // OP_CHANGE stuff moved to op_change
      if(oap.op_type == OP_CHANGE) {
	boolean delete_last = false;
	int line =  G.curwin.w_cursor.getLine();
	if(line + oap.line_count - 1 >= G.curbuf.getLineCount()) {
	  delete_last = true;
	}
	del_lines(oap.line_count, true, true);
	open_line(delete_last ? FORWARD : BACKWARD, true, true, 0);
      } else {
	del_lines(oap.line_count, true, true);
	Edit.beginline(BL_WHITE | BL_FIX);
      }
      // full lines are deleted, set op end/start to current pos.
      opStartPos = G.curwin.w_cursor.copy();
      opEndPos = opStartPos;
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
      //if (Normal.u_save_cursor() == FAIL)	// save first line for undo
      //  return FAIL;

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
	check_cursor_col();
      }
    }

    // remove code for different screen update cases

    msgmore(G.curbuf.getLineCount() - old_lcount);
    //
    // set "'[" and "']" marks.
    //
    if(opEndPos == null) {
      if(oap.block_mode) {
        opEndPos = oap.end.copy();
        opEndPos.setColumn(oap.start.getColumn());
      } else
        opEndPos = oap.start;
    }

    if(opStartPos == null)
      opStartPos = oap.start;
              
    G.curbuf.b_op_end.setMark(opEndPos);
    G.curbuf.b_op_start.setMark(opStartPos);
    return true;
  }

  /**
   * block mode delete
   * 
   * This was inside an if block in op_delete.
   * Pull it out to clarify swap algorithm
   */
  private static void block_delete(OPARG oap) {
    if(!blockOpSwapText || oap.line_count < 5) {
      block_deleteInplace(oap);
      return;
    }
    //
    // Use block_deleteInplace for the first line and last line
    // so that they are separate undoable operation.
    // Then the undo/redo pointer are better placed (I hope).
    //

    // there are at least 3 lines to do

    int lnum = oap.start.getLine();
    int finishPositionColumn = block_deleteInplace(oap, lnum, lnum);
    ++lnum; // did the first line
    int endLine = oap.end.getLine() - 1; // don't do last line in big chunk

    // startOffset/endOffset of single multi-line chunk replaced in the document
    int startOffset = G.curbuf.getLineStartOffset(lnum);
    int endOffset = G.curbuf.getLineEndOffset(endLine);
    StringBuilder sb = new StringBuilder(endOffset - startOffset);

    block_def bd = new block_def();
    for (; lnum <= endLine; lnum++) {
      MySegment oldp;
      block_prep(oap, bd, lnum, true);

      oldp = Util.ml_get(lnum);
      if (bd.textlen == 0) {       /* nothing to delete */
        sb.append(oldp);
        continue;
      }

      // If we delete a TAB, it may be replaced by several characters.
      // Thus the number of characters may increase!
      //
      sb.append(oldp, 0, bd.textcol);
      // insert spaces
      append_spaces(sb, bd.startspaces + bd.endspaces);
      // copy the part after the deleted part
      int oldp_idx = bd.textcol + bd.textlen;
      sb.append(oldp, oldp_idx, oldp.length());
    }

    // delete the trailing '\n'
    sb.deleteCharAt(sb.length()-1); // DONE
    G.curbuf.replaceString(startOffset, endOffset-1, sb.toString());

    // do the final line
    ++endLine;
    block_deleteInplace(oap, endLine, endLine);

    G.curwin.w_cursor.set(oap.start.getLine(), finishPositionColumn);
    adjust_cursor();

    update_screen(VALID_TO_CURSCHAR);
    oap.line_count = 0; /* no lines deleted */
  }

  /** the original block_delete */
  private static int block_deleteInplace(OPARG oap) {
//      if (u_save((oap.start.getLine() - 1), (oap.end.getLine() + 1)) == FAIL)
//            return FAIL;

    ViFPOS fpos = oap.start;
    int finishPositionColumn = block_deleteInplace(oap,
                                                       oap.start.getLine(),
                                                       oap.end.getLine());

    G.curwin.w_cursor.set(fpos.getLine(), finishPositionColumn);
    //changed_cline_bef_curs();/* recompute cursor pos. on screen */
    //approximate_botline();/* w_botline may be wrong now */
    adjust_cursor();

    //changed();
    update_screen(VALID_TO_CURSCHAR);
    oap.line_count = 0; /* no lines deleted */
    return finishPositionColumn;
  }

  private static int block_deleteInplace(OPARG oap,
                                             int startLine,
                                             int endLine) {
    //ViFPOS fpos = G.curwin.w_cursor.copy();
    ViFPOS fpos = oap.start;
    int finishPositionColumn = fpos.getColumn();
    //int lnum = fpos.getLine();
    int lnum = startLine;
    //for (; lnum <= oap.end.getLine(); lnum++)
    for (; lnum <= endLine; lnum++) {
      MySegment oldp;
      StringBuilder newp;
      block_def bd = new block_def();
      block_prep(oap, bd, lnum, true);
      if (bd.textlen == 0)       /* nothing to delete */
        continue;

      /* Adjust cursor position for tab replaced by spaces and 'lbr'. */
      if (lnum == fpos.getLine()) {
        finishPositionColumn = bd.textcol + bd.startspaces;
      }

      // n == number of chars deleted
      // If we delete a TAB, it may be replaced by several characters.
      // Thus the number of characters may increase!
      //
      int n = bd.textlen - bd.startspaces - bd.endspaces;
      if(bd.startspaces + bd.endspaces == 0) {
        // No TAB is split, simplify
        int lineStart = G.curbuf.getLineStartOffset(lnum);
        G.curwin.deleteChar(lineStart + bd.textcol,
                            lineStart + bd.textcol + bd.textlen);
      } else {
        oldp = Util.ml_get(lnum);
        newp = new StringBuilder();
        //newp = alloc_check((unsigned)STRLEN(oldp) + 1 - n);
        newp.setLength(oldp.length() - 1 - n); // -1 '\n', no +1 for '\n' // DONE
        // copy up to deleted part
        mch_memmove(newp, 0, oldp, 0, bd.textcol);
        // insert spaces
        copy_spaces(newp, bd.textcol, (bd.startspaces + bd.endspaces));
        // copy the part after the deleted part
        int oldp_idx = bd.textcol + bd.textlen;
        mch_memmove(newp, bd.textcol + bd.startspaces + bd.endspaces,
                    oldp, oldp_idx,
                    (oldp.length()-1) - oldp_idx); // STRLEN(oldp) + 1)
        // replace the line
        newp.setLength(STRLEN(newp));
        Util.ml_replace(lnum, newp);
      }
    }
    return finishPositionColumn;
  }

  /**
   * Verify the delete can be performed. In particular,
   * check for guarded areas.
   * @param oap specifies the region to delete
   * @return true if the everything can be deleted
   */
  static boolean valid_op_range(OPARG oap) {
    ViFPOS start = oap.start;
    ViFPOS end = oap.end;
    boolean isValid = true;

    if(start.getOffset() >= end.getOffset()) {
      // don't believe this is needed
      start = oap.end;
      end = oap.start;
      // System.err.println("SWITCH END<-->START");
    }

    //
    // check for guarded regions,
    //
    int line = start.getLine();
    int endLine = Math.min(end.getLine(), G.curbuf.getLineCount());

    if(line == endLine) {
      // EXPERIMENTAL
      // the range is within a single line
      int endOffset = end.getOffset() - (oap.inclusive ? 0 : 1);
      int startOffset = start.getOffset();
      if(endOffset < startOffset)
        endOffset = startOffset;
      isValid = Edit.canEdit(G.curwin, G.curbuf, startOffset, endOffset);
    } else {
      // just check the first char position of each line
      // NEEDSWORK: this may not be 100% reliable.
      //      Consider in a gui form the code customizer dialog
      //      has lines which are parially guarded.
      //      In real life, don't think there's an issue.
      for(; line <= endLine; line++) {
        //System.err.println("CHECK LINE: " + line);
        if(!Edit.canEdit(G.curwin, G.curbuf, G.curbuf.getLineStartOffset(line))) {
          isValid = false;
          //System.err.println("INVLID LINE: " + line);
          break;
        }
      }
    }

    return isValid;
  }
    
  public static int op_replace(final OPARG oap, final char c) {
    final MutableInt rval = new MutableInt();
    runUndoable(() -> {
      rval.setValue(op_replace7(oap, c)); // from vim7
    });
    return rval.getValue();
  }
    
  public static int op_replace7(OPARG oap, char c)
  {
    // Note use an fpos instead of the cursor,
    // This should avoid jitter on the screen
    
    if(/* (curbuf.b_ml.ml_flags & ML_EMPTY ) || */ oap.empty)
      return OK;	    // nothing to do
    
    //#ifdef MULTI_BYTE ... #endif
    
    if (oap.block_mode) {
      block_replace(oap, c);
    } else {
      ViFPOS fpos = G.curwin.w_cursor.copy();

      //
      // MCHAR and MLINE motion replace.
      //
      if (oap.motion_type == MLINE) {
        oap.start.setColumn(0);
        fpos = oap.start.copy();
        int col = Util.lineLength(oap.end.getLine());
        if (col != 0)
          --col;
        oap.end.setColumn(col);
      } else if (!oap.inclusive)
        dec(oap.end);
      
      while(fpos.compareTo(oap.end) <= 0) {
        if (gchar_pos(fpos) != '\n') { // DONE
          // #ifdef FEAT_MBYTE ... #endif
          pchar(fpos, c);
        }
        
        // Advance to next character, stop at the end of the file.
        if (inc(fpos) == -1)
          break;
      }
    }
    
    G.curwin.w_cursor.set(oap.start);
    adjust_cursor();
    
    oap.line_count = 0;	    // no lines deleted
    
    // Set "'[" and "']" marks.
    G.curbuf.b_op_start.setMark(oap.start);
    G.curbuf.b_op_end.setMark(oap.end);
    
    return OK;
  }

  private static void block_replace(OPARG oap, char c)
  {
    if(!blockOpSwapText || oap.line_count < 5) {
      block_replaceInplace(oap, c);
      return;
    }

    int         numc;
    MySegment   oldBuf;
    block_def bd = new block_def();

    bd.is_MAX = G.curwin.w_curswant == MAXCOL;

    int lnum = oap.start.getLine();
    block_replaceInplace(oap, c, lnum, lnum);
    ++lnum; // first line done by "inplace"
    int endLine = oap.end.getLine() - 1; // don't do last line in this loop

    // startOffset/endOffset of single multi-line chunk replaced in the document
    int startOffset = G.curbuf.getLineStartOffset(lnum);
    int endOffset = G.curbuf.getLineEndOffset(endLine);
    StringBuilder sb = new StringBuilder(endOffset - startOffset);

    for (; lnum <= endLine; lnum++) {
      //fpos.set(lnum, 0);
      block_prep(oap, bd, lnum, true);

      oldBuf = Util.ml_get(lnum);

      if (bd.textlen == 0 /* && (!virtual_op || bd.is_MAX)*/)
      {
        sb.append(oldBuf);
        continue;                     // nothing to replace
      }

      // If we split a TAB, it may be replaced by several characters.
      // Thus the number of characters may increase!
      //
      // allow for pre spaces
      // allow for post spp
      numc = oap.end_vcol - oap.start_vcol + 1;
      if (bd.is_short /*&& (!virtual_op || bd.is_MAX)*/)
        numc -= (oap.end_vcol - bd.end_vcol) + 1;
      // oldlen includes textlen, so don't double count

      // copy up to deleted part
      sb.append(oldBuf, 0, bd.textcol);
      // insert pre-spaces
      append_spaces(sb, bd.startspaces);
      /* insert replacement chars CHECK FOR ALLOCATED SPACE */
      append_chars(sb, numc, c);
      if (!bd.is_short) {
        // insert post-spaces
        //copy_spaces(newBuf, STRLEN(newBuf), bd.endspaces);
        append_spaces(sb, bd.endspaces);
        // copy the part after the changed part
        sb.append(oldBuf, bd.textcol + bd.textlen, oldBuf.length());
      } else {
        sb.append('\n'); // DONE
      }
    }

    // All done with the big chunk.
    // delete the trailing '\n'
    sb.deleteCharAt(sb.length()-1); // DONE
    G.curbuf.replaceString(startOffset, endOffset-1, sb.toString());

    ++endLine;
    block_replaceInplace(oap, c, endLine, endLine);
  }

  private static void block_replaceInplace(OPARG oap, char c)
  {
    block_replaceInplace(oap, c, oap.start.getLine(), oap.end.getLine());
  }

  private static void block_replaceInplace(OPARG oap, char c,
                                           int startLine, int endLine)
  {
    int		n;
    int         numc;
    MySegment   oldBuf;
    int         oldlen;
    int		lnum;
    StringBuilder newBuf;
    block_def bd = new block_def();
    int oldp;

    // Note use an fpos instead of the cursor,
    // This should avoid jitter on the screen
    //ViFPOS fpos = G.curwin.w_cursor.copy();

    bd.is_MAX = G.curwin.w_curswant == MAXCOL;
    String fullReplaceString = null;

    for (lnum = startLine; lnum <= endLine; lnum++) {
      //fpos.set(lnum, 0);
      block_prep(oap, bd, lnum, true);
      if (bd.textlen == 0 /* && (!virtual_op || bd.is_MAX)*/)
        continue;                     // nothing to replace

      // n == number of extra chars required
      // If we split a TAB, it may be replaced by several characters.
      // Thus the number of characters may increase!
      //
      // allow for pre spaces
      n = (bd.startspaces != 0 ? bd.start_char_vcols - 1 : 0 );
      // allow for post spp
      n += (bd.endspaces != 0 && !bd.is_oneChar && bd.end_char_vcols > 0
              ? bd.end_char_vcols - 1 : 0 );
      numc = oap.end_vcol - oap.start_vcol + 1;
      if (bd.is_short /*&& (!virtual_op || bd.is_MAX)*/)
        numc -= (oap.end_vcol - bd.end_vcol) + 1;
      // oldlen includes textlen, so don't double count
      n += numc - bd.textlen;

      if(bd.textlen == numc
            && !bd.is_short && bd.startspaces == 0 && bd.endspaces == 0) {
        // replace inplace, there may be a better test but this must work
        if(fullReplaceString == null) {
          StringBuilder sb = new StringBuilder();
          for(int i = 0; i < numc; i++)
            sb.append(c);
          fullReplaceString = sb.toString();
        }
        int offset = G.curbuf.getLineStartOffset(lnum) + bd.textcol;
        G.curbuf.replaceString(offset, offset + numc, fullReplaceString);
      } else {
        //oldp = ml_get_curline();
        //oldBuf = Util.ml_get(fpos.getLine());
        oldBuf = Util.ml_get(lnum);
        oldp = 0;
        oldlen = oldBuf.length() - 1; //excluce the \n // DONE
        //newp = alloc_check((unsigned)STRLEN(oldp) + 1 + n);
        // -1 in setlength to ignore \n, don't need +1 since no null at end
        newBuf = new StringBuilder();
        newBuf.setLength(oldlen + n); // DONE

        // too much sometimes gets allocated with the setLength,
        // but the correct number of chars gets copied, keep track of that.
        // BTW, a few extra nulls at the end wouldn't hurt vim

        // copy up to deleted part
        mch_memmove(newBuf, 0, oldBuf, oldp, bd.textcol);
        oldp += bd.textcol + bd.textlen;
        // insert pre-spaces
        copy_spaces(newBuf, bd.textcol, bd.startspaces);
        /* insert replacement chars CHECK FOR ALLOCATED SPACE */
        copy_chars(newBuf, STRLEN(newBuf), numc, c);
        if (!bd.is_short) {
          // insert post-spaces
          copy_spaces(newBuf, STRLEN(newBuf), bd.endspaces);
          // copy the part after the changed part, -1 to exclude \n
          int tCount = (oldBuf.length() - 1) - oldp; // STRLEN(oldp) +1 // DONE
          mch_memmove(newBuf, STRLEN(newBuf), oldBuf, oldp, tCount);
        }
        // delete trailing nulls, vim alloc extra when tabs (seen with gdb)
        newBuf.setLength(STRLEN(newBuf));
        // replace the line
        Util.ml_replace(lnum, newBuf);
      }
    }
  }

  /**
   * op_tilde - handle the (non-standard vi) tilde operator
   */
  static void op_tilde(OPARG oap) {
    ViFPOS		pos;
    ViFPOS		finalPosition;

    /* ***********************************************
       if (u_save((linenr_t)(oap->start.lnum - 1),
       (linenr_t)(oap->end.lnum + 1)) == FAIL)
       return;
     *************************************************************/

    pos = oap.start.copy();

    if (oap.block_mode)		    // Visual block mode
    {
      finalPosition = pos.copy();
      block_def bd = new block_def();
      for (; pos.getLine() <= oap.end.getLine(); pos.set(pos.getLine()+1, 0)) {
        block_prep(oap, bd, pos.getLine(), false);
        pos.setColumn(bd.textcol);
        while (--bd.textlen >= 0) {
          swapchar(oap.op_type, pos);
          if (inc(pos) == -1) // at end of file
            break;
        }
      }
      
    } else {				    // not block mode
      if (oap.motion_type == MLINE) {
        pos.setColumn(0);      // this is start
        int col = Util.lineLength(oap.end.getLine());
        if (col != 0) {
          --col;
        }
        oap.end.setColumn(col);
      } else if (!oap.inclusive) {
        dec(oap.end);
      }

      finalPosition = pos.copy();
      while (pos.compareTo(oap.end) <= 0) {
        swapchar(oap.op_type, pos);
        if (inc(pos) == -1)    // at end of file
          break;
      }
    }
    G.curwin.w_cursor.set(finalPosition);

    /* **********************************************************
    if (oap.motion_type == MCHAR && oap.line_count == 1 && !oap.block_mode)
      update_screenline();
    else
    {
      update_topline();
      update_screen(NOT_VALID);
    }
    *********************************************************/

    //
    // Set '[ and '] marks.
    //
    G.curbuf.b_op_start.setMark(oap.start);
    G.curbuf.b_op_end.setMark(oap.end);

    if (oap.line_count > G.p_report) {
      Msg.smsg("" + oap.line_count + " line" + plural(oap.line_count) + " ~ed");
    }
  }

  /**
   * If op_type == OP_UPPER: make uppercase,
   * <br>if op_type == OP_LOWER: make lowercase,
   * <br>else swap case of character at 'pos'
   */
  static void swapchar(int op_type, ViFPOS fpos) {
    char    c;
    char    nc;

    c = gchar_pos(fpos);
    nc = swapchar(op_type, c);
    if(c != nc) {
      pchar(fpos, nc);
    }
  }

  static char swapchar(int op_type, char c) {
    char    nc;

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


  /** op_change is "split", save state across calls here. */
  private static class StateOpSplit {
    private final StateSplitOwner owner;
    OPARG       oap;
    block_def	bd;
    int         pre_textlen;

    /** Do not use directly, use acquire/release */
    private StateOpSplit(StateSplitOwner owner) {
      StateOpSplit prev = stateOpSplit;
      stateOpSplit = null;
      assert(prev == null);  // not asserting on stateOpSplit so it will clear

      this.owner = owner;
    }

    static void acquire(StateSplitOwner owner) {
      stateOpSplit = new StateOpSplit(owner);
    }

    static void release() {
      stateOpSplit = null;
    }
  }
  private enum StateSplitOwner {SPLIT_CHANGE, SPLIT_INSERT }

  private static StateOpSplit stateOpSplit;

  static void finishOpSplit() {
    if(stateOpSplit != null) {
      switch(stateOpSplit.owner) {
        case SPLIT_CHANGE: finishOpChange(); break;
        case SPLIT_INSERT: finishOpInsert(); break;
      }
    }
  }

  /**
   * op_change - handle a change operation
   */
  static void op_change(OPARG oap) {
    int		l;

    l = oap.start.getColumn();
    if (oap.motion_type == MLINE) {
      l = 0;
    }
    if (!op_delete(oap))
      return;
    
    final ViFPOS cursor = G.curwin.w_cursor;
    if ((l > cursor.getColumn()) && !Util.lineempty(cursor.getLine())) //&& !virtual_op
      inc_cursor();
    
    // check for still on same line (<CR> in inserted text meaningless)
    ///skip blank lines too
    if (oap.block_mode) {
      StateOpSplit.acquire(StateSplitOwner.SPLIT_CHANGE);
      stateOpSplit.oap = oap.copy();
      stateOpSplit.bd = new block_def();
                                    //(long)STRLEN(ml_get(oap.start.lnum));
      stateOpSplit.pre_textlen = Util.lineLength(oap.start.getLine());
      stateOpSplit.bd.textcol = cursor.getColumn();
    }
    
    Edit.edit(NUL, false, 1);
    if(!Normal.editBusy)
      StateOpSplit.release();
  }

  static void finishOpChange() {
    OPARG       oap = stateOpSplit.oap;
    block_def	bd = stateOpSplit.bd;
    int         pre_textlen = stateOpSplit.pre_textlen;
    StateOpSplit.release();

    int		offset;
    int		linenr;
    int		ins_len;
    MySegment	firstline;
    String      ins_text;
    StringBuilder newp;
    MySegment   oldp;

    //
    // In Visual block mode, handle copying the new text to all lines of the
    // block.
    //
    if (oap.block_mode && oap.start.getLine() != oap.end.getLine()) {
      firstline = Util.ml_get(oap.start.getLine());
        //
        // take a copy of the required bit.
        //
      if ((ins_len = Util.lineLength(oap.start.getLine()) - pre_textlen) > 0) {
        //vim_strncpy(ins_text, firstline + bd.textcol, ins_len);
        ins_text = firstline.subSequence(bd.textcol,
                                         bd.textcol + ins_len).toString();
        for (linenr = oap.start.getLine() + 1; linenr <= oap.end.getLine();
                                                                    linenr++) {
          block_prep(oap, bd, linenr, true);
          if (!bd.is_short /*|| virtual_op*/) {
            oldp = Util.ml_get(linenr);
            int oldp_idx = 0;
            // newp = alloc_check((unsigned)(STRLEN(oldp) + ins_len + 1));
            newp = new StringBuilder();
            newp.setLength(oldp.length() - 1 + ins_len); // -1 for '\n' // DONE
            // copy up to block start
            mch_memmove(newp, 0, oldp, oldp_idx, bd.textcol);
            offset = bd.textcol;
            mch_memmove(newp, offset, ins_text, 0, ins_len);
            offset += ins_len;
            oldp_idx += bd.textcol;
            mch_memmove(newp, offset, oldp, oldp_idx, 
                        oldp.length() - 1 - oldp_idx); // STRLEN(oldp) + 1);
            Util.ml_replace(linenr, newp);
          }
        }
        adjust_cursor();
      }
    }
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

    if(G.curwin.w_cursor.getLine()
                              == G.curbuf.getLineCount()) {
	return FAIL;	// can't join on last line
      }
      StringBuilder spaces = new StringBuilder();
      int nextline = G.curwin.w_cursor.getLine() + 1;
      int lastc;

      int offset00 = G.curbuf.getLineStartOffset(nextline);
      int offset01 = offset00 - 1; // points to the '\n' of current line // DONE

      MySegment seg = G.curbuf.getLineSegment(nextline);
      int offset02 = offset00 + skipwhite(seg, 0);
      int nextc = Util.getCharAt(offset02);

      if(insert_space) {
	if(offset01 > 0) {
	  // there's a char before first line's '\n'
	  lastc = Util.getCharAt(offset01 - 1); // last char of line // DONE
	} else {
	  // at begin file, say lastc is a newline
	  lastc = '\n'; // DONE
	}
	if(nextc != ')' && lastc != ' ' && lastc != TAB && lastc != '\n') { // DONE
	  spaces.append(' ');
	  if(   G.p_js && (lastc == '.' || lastc == '?'
                                     || lastc =='!')) {
	    spaces.append(' ');
	  }
	}
      }
      G.curwin.deleteChar(offset01, offset02);
      if(spaces.length() > 0) {
	G.curwin.insertText(offset01, spaces.toString());
      }
      G.curwin.w_cursor.set(offset01);

      return OK;
  }
    
/**
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
  //System.out.println("block prep: "+ oap.end_vcol +" -> "
  //+ (G.curwin.getLineEndOffset(lnum) - G.curwin.getLineStartOffset(lnum)));
  int   incr = 0;
  MySegment pend;
  MySegment pstart;
  int pend_idx;
  int pstart_idx;
  int prev_pstart_idx;
  int prev_pend_idx;

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
  bdp.end_char_vcols = 0;
  bdp.start_char_vcols = 0;

  eatme(bdp.is_EOL, bdp.pre_whitesp);
  pstart = Util.truncateNewline(Util.ml_get(lnum));

  pstart_idx = 0;
  prev_pstart_idx = 0;
  // pend_idx = 0;
  // prev_pend_idx = 0;

  while (bdp.start_vcol < oap.start_vcol && pstart_idx < pstart.length()) {
    /* Count a tab for what it's worth (if list mode not on) */
    incr = lbr_chartabsize(pstart.charAt(pstart_idx), bdp.start_vcol);
    bdp.start_vcol += incr;
    ++bdp.textcol;
    if (vim_iswhite(pstart.charAt(pstart_idx))) {
      bdp.pre_whitesp += incr;
      bdp.pre_whitesp_c++;
    } else {
      bdp.pre_whitesp = 0;
      bdp.pre_whitesp_c = 0;
    }
    prev_pstart_idx = pstart_idx;
    pstart_idx++;//++pstart;
  }
  bdp.start_char_vcols = incr;
  if (bdp.start_vcol < oap.start_vcol) // line too short
  {
    bdp.end_vcol = bdp.start_vcol;
    bdp.is_short = true;
    if (!is_del || oap.op_type == OP_APPEND)
      bdp.endspaces = oap.end_vcol - oap.start_vcol + 1;
  } else {
    bdp.startspaces = bdp.start_vcol - oap.start_vcol;
    if (is_del && bdp.startspaces != 0)
      bdp.startspaces = bdp.start_char_vcols - bdp.startspaces;
    pend = pstart;
    pend_idx = pstart_idx;
    bdp.end_vcol = bdp.start_vcol;
    if (bdp.end_vcol > oap.end_vcol) // it's all in one character
    {
      bdp.is_oneChar = true;
      switch (oap.op_type) {
      case OP_INSERT:
        bdp.endspaces = bdp.start_char_vcols - bdp.startspaces;
        break;
      case OP_APPEND:
        bdp.startspaces += oap.end_vcol - oap.start_vcol + 1;
        bdp.endspaces = bdp.start_char_vcols - bdp.startspaces;
        break;
      default:
        bdp.startspaces = oap.end_vcol - oap.start_vcol + 1;
        if (is_del && oap.op_type != OP_LSHIFT) {
          // just putting the sum of those two into
          // bdp->startspaces doesn't work for Visual replace,
          // so we have to split the tab in two
          bdp.startspaces = bdp.start_char_vcols
                  - (bdp.start_vcol - oap.start_vcol);
          bdp.endspaces = bdp.end_vcol - oap.end_vcol - 1;
        }
        break;
      }
    } else {
      prev_pend_idx = pend_idx;
      while (bdp.end_vcol <= oap.end_vcol && pend_idx < pend.length()) {
        /* Count a tab for what it's worth (if list mode not on) */
        prev_pend_idx = pend_idx;
        incr = lbr_chartabsize(pend.charAt(pend_idx), bdp.end_vcol);
        bdp.end_vcol += incr;
        pend_idx++;
      }
      if (bdp.end_vcol <= oap.end_vcol
              && (!is_del
              || oap.op_type == OP_APPEND
              || oap.op_type == OP_REPLACE)) // line too short
      {
        bdp.is_short = true;
        // Alternative: include spaces to fill up the block.
        // Disadvantage: can lead to trailing spaces when the line is
        // short where the text is put */
        // if (!is_del || oap->op_type == OP_APPEND) //
        if (oap.op_type == OP_APPEND /*|| virtual_op*/)
          bdp.endspaces = oap.end_vcol - bdp.end_vcol
                  + (oap.inclusive ? 1 : 0);
        else
          bdp.endspaces = 0; // replace doesn't add characters
      } else if (bdp.end_vcol > oap.end_vcol) {
        bdp.endspaces = bdp.end_vcol - oap.end_vcol - 1;
        if (!is_del && bdp.endspaces != 0) {
          bdp.endspaces = incr - bdp.endspaces;
          if (pend_idx != pstart_idx)
            pend_idx = prev_pend_idx;
        }
      }
    }
    bdp.end_char_vcols = incr;
    if (is_del && bdp.startspaces != 0)
      pstart_idx = prev_pstart_idx;
    bdp.textlen = pend_idx - pstart_idx;
  }
  bdp.textcol = pstart_idx;
  bdp.textstart = pstart_idx; // TODO_VIS textstart not pointer review usage carefully
}

static boolean	hexupper = false;	/* 0xABC */
/**
 * add or subtract 'Prenum1' from a number in a line
 * 'command' is CTRL-A for add, CTRL-X for subtract
 *
 * return FAIL for failure, OK otherwise
 *
 * from vim7 ops.c
 */
static int
do_addsub(final char command, final int Prenum1)
{
    final MutableInt rval = new MutableInt();
    runUndoable(() -> {
      rval.setValue(op_do_addsub(command, Prenum1));
    });
    return rval.getValue();
}

private static int
op_do_addsub(char command, int Prenum1)
{
    int		col;
    StringBuilder	buf1 = new StringBuilder();
    String	buf2;
    char	hex;		/* 'X' or 'x': hex; '0': octal */
    int         n;
    int		oldn;
    MySegment	ptr;
    char	c;
    int		length;		/* character length of the number */
    int		todel;
    boolean	dohex;
    boolean	dooct;
    boolean	doalp;
    char	firstdigit;
    boolean	negative;
    boolean	subtract;

    dohex = G.curbuf.b_p_nf.contains(NF_HEX);
    dooct = G.curbuf.b_p_nf.contains(NF_OCTAL);
    doalp = G.curbuf.b_p_nf.contains(NF_ALPHA);

    ptr = ml_get_curline();
    //RLADDSUBFIX(ptr);

    /*
     * First check if we are on a hexadecimal number, after the "0x".
     */
    col = G.curwin.w_cursor.getColumn();
    if (dohex)
	while (col > 0 && isxdigit(ptr.charAt(col)))
	    --col;
    if (       dohex
	    && col > 0
	    && (ptr.charAt(col) == 'X'
		|| ptr.charAt(col) == 'x')
	    && ptr.charAt(col - 1) == '0'
	    && isxdigit(ptr.charAt(col + 1)))
    {
	/*
	 * Found hexadecimal number, move to its start.
	 */
	--col;
    }
    else
    {
	/*
	 * Search forward and then backward to find the start of number.
	 */
	col = G.curwin.w_cursor.getColumn();

	while (col < ptr.length()
                && ptr.charAt(col) != '\n' // DONE
		&& !isdigit(ptr.charAt(col))
		&& !(doalp && ascii_isalpha(ptr.charAt(col))))
	    ++col;

	while (col > 0
		&& isdigit(ptr.charAt(col - 1))
		&& !(doalp && ascii_isalpha(ptr.charAt(col))))
	    --col;
    }

    /*
     * If a number was found, and saving for undo works, replace the number.
     */
    firstdigit = ptr.charAt(col);
    //RLADDSUBFIX(ptr);
    if ((!isdigit(firstdigit) && !(doalp && ascii_isalpha(firstdigit)))
	    || u_save_cursor() != OK)
    {
	beep_flush();
	return FAIL;
    }

    /* get ptr again, because u_save() may have changed it */
    ptr = ml_get_curline();
    //RLADDSUBFIX(ptr);

    if (doalp && ascii_isalpha(firstdigit))
    {
	/* decrement or increment alphabetic character */
	if (command == CTRL_X)
	{
	    if (CharOrd(firstdigit) < Prenum1)
	    {
		if (isupper(firstdigit))
		    firstdigit = 'A';
		else
		    firstdigit = 'a';
	    }
	    else
		firstdigit -= Prenum1;
	}
	else
	{
	    if (26 - CharOrd(firstdigit) - 1 < Prenum1)
	    {
		if (isupper(firstdigit))
		    firstdigit = 'Z';
		else
		    firstdigit = 'z';
	    }
	    else
		firstdigit += Prenum1;
	}
	G.curwin.w_cursor.setColumn(col);
	del_char(false);
	ins_char(firstdigit);
    }
    else
    {
	negative = false;
	if (col > 0 && ptr.charAt(col - 1) == '-')	    /* negative number */
	{
	    --col;
	    negative = true;
	}

	/* get the number value (unsigned) */
        MutableInt pHex = new MutableInt();
        MutableInt pLength = new MutableInt();
        MutableInt pN = new MutableInt();

	vim_str2nr(ptr, col, pHex, pLength,
                   dooct ? TRUE : FALSE, dohex ? TRUE : FALSE, null, pN);
        hex = (char)pHex.getValue();
        length = pLength.getValue();
        n = pN.getValue();

	/* ignore leading '-' for hex and octal numbers */
	if (hex != 0 && negative)
	{
	    ++col;
	    --length;
	    negative = false;
	}

	/* add or subtract */
	subtract = false;
	if (command == CTRL_X)
	    subtract ^= true;
	if (negative)
	    subtract ^= true;

	oldn = n;
	if (subtract)
	    n -= Prenum1;
	else
	    n += Prenum1;

	/* handle wraparound for decimal numbers */
	if (hex == 0)
	{
	    if (subtract)
	    {
		if (n > oldn)
		{
		    n = 1 + (n ^ -1);
		    negative ^= true;
		}
	    }
	    else /* add */
	    {
		if (n < oldn)
		{
		    n = (n ^ -1);
		    negative ^= true;
		}
	    }
	    if (n == 0)
		negative = false;
	}

	/*
	 * Delete the old number.
	 */
	G.curwin.w_cursor.setColumn(col);
	todel = length;
	c = gchar_cursor();
	/*
	 * Don't include the '-' in the length, only the length of the part
	 * after it is kept the same.
	 */
	if (c == '-')
	    --length;
	while (todel-- > 0)
	{
	    if (c < 0x100 && isalpha(c))
	    {
              hexupper = isupper(c);
	    }
	    /* del_char() will mark line needing displaying */
	    del_char(false);
	    c = gchar_cursor();
	}

	/*
	 * Prepare the leading characters in buf1[].
	 * When there are many leading zeros it could be very long.  Allocate
	 * a bit too much.
	 */
//	buf1 = alloc((unsigned)length + NUMBUFLEN);
//	if (buf1 == null)
//	    return FAIL;
//	ptr = buf1;
	if (negative)
	{
            buf1.append('-');
	}
	if (hex != 0)
	{
            buf1.append('0');
	    --length;
	}
	if (hex == 'x' || hex == 'X')
	{
            buf1.append(hex);
	    --length;
	}

	/*
	 * Put the number characters in buf2[].
	 */
	if (hex == 0)
            buf2 = String.format("%d", n); //sprintf(buf2, "%lu", n);
	else if (hex == '0')
	    buf2 = String.format("%o", n); //sprintf(buf2, "%lo", n);
	else if (hex != 0 && hexupper)
	    buf2 = String.format("%X", n); //sprintf(buf2, "%lX", n);
	else
	    buf2 = String.format("%x", n); //sprintf(buf2, "%lx", n);
	length -= buf2.length(); //STRLEN(buf2);

	/*
	 * Adjust number of zeros to the new number of digits, so the
	 * total length of the number remains the same.
	 * Don't do this when
	 * the result may look like an octal number.
	 */
	if (firstdigit == '0' && !(dooct && hex == 0))
	    while (length-- > 0)
		buf1.append('0'); //*ptr++ = '0';
	//*ptr = NUL;
	buf1.append(buf2); //STRCAT(buf1, buf2);
	//ins_str(buf1.toString());		/* insert the new number */
        G.curbuf.insertText(G.curwin.w_cursor.getOffset(), buf1.toString());
	//vim_free(buf1);
    }
    G.curwin.w_cursor.decColumn();
    G.curwin.w_set_curswant = true;
//#ifdef FEAT_RIGHTLEFT...
    return OK;
}
    
    static void op_insert(OPARG oap, int count1) {
      if(!valid_op_range(oap))
        return;
      int pre_textlen;
      block_def   bd = new block_def();
      final ViFPOS cursor = G.curwin.w_cursor;
      int i;
      
      /* edit() changes this - record it for OP_APPEND */
      bd.is_MAX = (G.curwin.w_curswant == MAXCOL);
      
      /* vis block is still marked. Get rid of it now. */
      cursor.setLine(oap.start.getLine());
      update_screen(INVERTED);
      
      if (oap.block_mode) {
        /* Get the info about the block before entering the text */
        block_prep(oap, bd, oap.start.getLine(), true);
        //(long)STRLEN(ml_get(oap.start.lnum));
        CharSequence firstline = Util.ml_get(oap.start.getLine())
                .subSequence(bd.textcol, Util.lineLength(oap.start.getLine()));
        
        if (oap.op_type == OP_APPEND)
          firstline = firstline.subSequence(bd.textlen, firstline.length());
        pre_textlen = firstline.length();
        
        StateOpSplit.acquire(StateSplitOwner.SPLIT_INSERT);
        stateOpSplit.oap = oap.copy();
        stateOpSplit.bd = bd;
        stateOpSplit.pre_textlen = pre_textlen;
      }
      
      if (oap.op_type == OP_APPEND) {
        if (oap.block_mode) {
          /* Move the cursor to the character right of the block. */
          G.curwin.w_set_curswant = true;
          int tcol = cursor.getColumn();
          MySegment seg = Util.ml_get_curline();
          while (seg.fetch(tcol) != '\n' // DONE
                  && (tcol < bd.textcol + bd.textlen))
            tcol++;
          cursor.setColumn(tcol);
          if (bd.is_short && !bd.is_MAX) {
                    /* First line was too short, make it longer and adjust the
                     * values in "bd". */
            if (Normal.u_save_cursor() == FAIL)
              return;
            for (i = 0; i < bd.endspaces; ++i)
              ins_char(' ');
            bd.textlen += bd.endspaces;
          }
        } else {
          cursor.set(oap.end);//.w_cursor = oap.end;
          check_cursor_col();
          /* Works just like an 'i'nsert on the next character. */
          if (!Util.lineempty(cursor.getLine())
                  && oap.start_vcol != oap.end_vcol)
            inc_cursor();
        }
      }
      
      Edit.edit(NUL, false, count1);
      if(!Normal.editBusy)
        StateOpSplit.release();
    }

    static void finishOpInsert() {
      OPARG       oap = stateOpSplit.oap;
      block_def   bd = stateOpSplit.bd;
      int         pre_textlen = stateOpSplit.pre_textlen;
      StateOpSplit.release();
      
      //int		offset;
      //int		linenr;
      int		ins_len;
      CharSequence	firstline;
      String      ins_text;
      //StringBuilder newp;
      //MySegment   oldp;
      
      
  /* if user has moved off this line, we don't know what to do, so do
  nothing */
      if (G.curwin.w_cursor.getLine() != oap.start.getLine())
        return;
      
      if (oap.block_mode) {
        block_def bd2 = new block_def();
        
    /*
     * Spaces and tabs in the indent may have changed to other spaces and
     * tabs.  Get the starting column again and correct the lenght.
     * Don't do this when "$" used, end-of-line will have changed.
     */
        block_prep(oap, bd2, oap.start.getLine(), true);
        if (!bd.is_MAX || bd2.textlen < bd.textlen) {
          if (oap.op_type == OP_APPEND) {
            pre_textlen += bd2.textlen - bd.textlen;
            if (bd2.endspaces != 0)
              --bd2.textlen;
          }
          bd.textcol = bd2.textcol;
          bd.textlen = bd2.textlen;
        }
        
    /*
     * Subsequent calls to ml_get() flush the firstline data - take a
     * copy of the required string.
     */
        firstline = Util.ml_get(oap.start.getLine())
                .subSequence(bd.textcol, Util.lineLength(oap.start.getLine()));
        
        if (oap.op_type == OP_APPEND)
          firstline = firstline.subSequence(bd.textlen, firstline.length());
        if ((ins_len = firstline.length() - pre_textlen) > 0) {
          //vim_strnsave(firstline, (int)ins_len);
          ins_text = firstline.subSequence(0, ins_len).toString();
          if (ins_text != null) {
            /* block handled here */
            //if (u_save(oap.start.getLine(), (oap.end.getLine() + 1)) == OK)
            block_insert(oap, ins_text, (oap.op_type == OP_INSERT), bd);
            
            G.curwin.w_cursor.setColumn(oap.start.getColumn());
            adjust_cursor();
            //check_cursor();
            //vim_free(ins_text);
          }
        }
      }
    }
    
    static boolean blockOpSwapText = true;
/**
 * Insert string "s" (b_insert ? before : after) block :AKelly
 * Caller must prepare for undo.
 */
  static void block_insert(OPARG oap, String s, boolean b_insert, block_def bdp) {
    if(!blockOpSwapText || oap.line_count < 5) {
      block_insertInplace(oap, s, b_insert, bdp);
      return;
    }

    //
    // Profiling shows that almost all the time is spent in Document.<op>.
    // This modified algorithm build a new string result consisting of all
    // the modified lines and replaces them in a single document op.
    //
    // This cuts the time in half (still around 90% in Document),
    // still long but...
    //

    int		p_ts;
    int		count = 0;	// extra spaces to replace a cut TAB
    int		spaces = 0;	// non-zero if cutting a TAB
    int 	offset;		// pointer along new line
    int 	s_len;		// STRLEN(s)
    MySegment   oldp;           // new, old lines
    int		oldstate = G.State;

    eatme(count);
    int lnum = oap.start.getLine() + 1; // frist line done by edit
    int endLine = oap.end.getLine() - 1; // don't do last line in this loop

    G.State = INSERT;		// don't want REPLACE for State
    s_len = s.length();

    int startOffset = G.curbuf.getLineStartOffset(lnum);
    int endOffset = G.curbuf.getLineEndOffset(endLine);
    StringBuilder sb = new StringBuilder(endOffset - startOffset
            + s_len * (endLine - lnum + 1));

    for (; lnum <= endLine; lnum++) {
      block_prep(oap, bdp, lnum, true);

      oldp = Util.ml_get(lnum);

      if (bdp.is_short && b_insert) {
        sb.append(oldp);
        continue;	// OP_INSERT, line ends before block start
      }

      count = 0;
      if (b_insert) {
        p_ts = bdp.start_char_vcols;
        spaces = bdp.startspaces;
        if (spaces != 0)
          count = p_ts - 1; // we're cutting a TAB
        offset = bdp.textcol;
      } else // append
      {
        p_ts = bdp.end_char_vcols;
        if (!bdp.is_short) // spaces = padding after block
        {
          spaces = (bdp.endspaces != 0 ? p_ts - bdp.endspaces : 0);
          if (spaces != 0)
            count = p_ts - 1; // we're cutting a TAB
          offset = bdp.textcol + bdp.textlen - (spaces != 0 ? 1 : 0);
        } else // spaces = padding to block edge
        {
          // if $ used, just append to EOL (ie spaces==0)
          if (!bdp.is_MAX)
            spaces = (oap.end_vcol - bdp.end_vcol) + 1;
          count = spaces;
          offset = bdp.textcol + bdp.textlen;
        }
      }
      eatme(count);

      //if(spaces == 0) {
      //  // No splitting or padding going on, can simply insert the string.
      //  // Profiling shows that 45% of time spent in Document.remove
      //  // and 50% in Document.insertString, with this case don't need remove
      //  // Should almost double the performance
      //  G.curwin.insertText(G.curbuf.getLineStartOffset(lnum) + offset, s);
      //} else

      /* copy up to shifted part */
      sb.append(oldp, 0, offset);
      int oldp_idx = offset;

      // insert pre-padding
      append_spaces(sb, spaces);

      // copy the new text
      sb.append(s);

      if (spaces != 0 && !bdp.is_short) {
        // insert post-padding
        append_spaces(sb, p_ts - spaces);
        // We're splitting a TAB, don't copy it.
        oldp_idx++;
        // We allowed for that TAB, remember this now
      }

      // Copy the rest of the line
      sb.append(oldp, oldp_idx, oldp.length());

    } // for all lnum except first and last

    // All done with the big chunk.
    // delete the trailing '\n'
    sb.deleteCharAt(sb.length()-1); // DONE
    G.curbuf.replaceString(startOffset, endOffset-1, sb.toString());

    // do the last line
    ++endLine;
    block_insertInplace(oap, s, b_insert, bdp, endLine, endLine);

    // changed_lines(oap.start.lnum + 1, 0, oap.end.lnum + 1, 0L);

    G.State = oldstate;
  }

  private static void block_insertInplace(
          OPARG oap, String s, boolean b_insert, block_def bdp) {
    // The first line has already been handled by edit
    block_insertInplace(oap, s, b_insert, bdp,
                        oap.start.getLine() + 1, oap.end.getLine());
  }

  private static void block_insertInplace(
          OPARG oap, String s, boolean b_insert, block_def bdp,
          int startLine, int endLine) {
    int		p_ts;
    int		count;		// extra spaces to replace a cut TAB
    int		spaces = 0;	// non-zero if cutting a TAB
    int 	offset;		// pointer along new line
    int 	s_len;		// STRLEN(s)
    MySegment   oldp;           // new, old lines
    StringBuilder newp;
    int 	lnum;		// loop var
    int		oldstate = G.State;
    
    G.State = INSERT;		// don't want REPLACE for State
    s_len = s.length();
    
    for (lnum = startLine; lnum <= endLine; lnum++) {
      block_prep(oap, bdp, lnum, true);
      if (bdp.is_short && b_insert)
        continue;	// OP_INSERT, line ends before block start
      
      oldp = Util.ml_get(lnum);
      
      count = 0;
      if (b_insert) {
        p_ts = bdp.start_char_vcols;
        spaces = bdp.startspaces;
        if (spaces != 0)
          count = p_ts - 1; // we're cutting a TAB
        offset = bdp.textcol;
      } else // append
      {
        p_ts = bdp.end_char_vcols;
        if (!bdp.is_short) // spaces = padding after block
        {
          spaces = (bdp.endspaces != 0 ? p_ts - bdp.endspaces : 0);
          if (spaces != 0)
            count = p_ts - 1; // we're cutting a TAB
          offset = bdp.textcol + bdp.textlen - (spaces != 0 ? 1 : 0);
        } else // spaces = padding to block edge
        {
          // if $ used, just append to EOL (ie spaces==0)
          if (!bdp.is_MAX)
            spaces = (oap.end_vcol - bdp.end_vcol) + 1;
          count = spaces;
          offset = bdp.textcol + bdp.textlen;
        }
      }
      
      if(spaces == 0) {
        // No splitting or padding going on, can simply insert the string.
        // Profiling shows that 45% of time spent in Document.remove
        // and 50% in Document.insertString, with this case don't need remove
        // Should almost double the performance
        G.curwin.insertText(G.curbuf.getLineStartOffset(lnum) + offset, s);
        offset += s_len;
      } else {
        newp = new StringBuilder();
        //newp = alloc_check((unsigned)(STRLEN(oldp)) + s_len + count + 1);
        newp.setLength(oldp.length() - 1 + s_len + count);
        
        /* copy up to shifted part */
        mch_memmove(newp, 0, oldp, 0, offset);
        //oldp += offset;
        int oldp_idx = 0;
        oldp_idx += offset;
        
        //int newp_idx = 0;
        //newp_idx += offset;
        
        // insert pre-padding
        copy_spaces(newp, offset, spaces);
        
        // copy the new text
        mch_memmove(newp, offset + spaces, s, 0, s_len);
        offset += s_len;
        
        if (spaces != 0 && !bdp.is_short) {
          // insert post-padding
          copy_spaces(newp, offset + spaces, p_ts - spaces);
          // We're splitting a TAB, don't copy it.
          oldp_idx++;
          // We allowed for that TAB, remember this now
          count++;
        }
        
        if (spaces > 0)
          offset += count;
        mch_memmove(newp, offset,
                    oldp, oldp_idx,
                    (oldp.length() - 1) - oldp_idx);
        
        newp.setLength(STRLEN(newp));
        Util.ml_replace(lnum, newp);
      }
      
      if (lnum == oap.end.getLine()) {
          // Set "']" mark to the end of the block instead of the end of
          // the insert in the first line.
        ViFPOS op_end = oap.end.copy();
        op_end.setColumn(offset);
        G.curbuf.b_op_end.setMark(op_end);
      }
    } // for all lnum
    
    // changed_lines(oap.start.lnum + 1, 0, oap.end.lnum + 1, 0L);
    
    G.State = oldstate;
  }

  //////////////////////////////////////////////////////////////////
  //
  // Clipboard stuff
  //
}

// vi: sw=2 ts=8

