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


import static com.raelity.jvi.ViTextView.MARKOP;
import static com.raelity.jvi.ViTextView.MARKOP.NEXT;
import static com.raelity.jvi.ViTextView.MARKOP.PREV;
import static com.raelity.jvi.ViTextView.MARKOP.TOGGLE;

import static com.raelity.jvi.Constants.*;
import static com.raelity.jvi.Messages.*;

/**
 * Keep track of vi marks.
 */
class MarkOps {

  // NEEDSWORK: put each markset in Buffer
  //static Map<Object,ViMark[]> marks = new WeakHashMap<Object,ViMark[]>(5);

  /** This constant indicates mark is in other file. */
  final static FPOS otherFile = new FPOS();

  /** Set the indicated mark to the current cursor position;
   * if anonymous mark characters, then handle that.
   * <b>NEEDSWORK:</b><ul>
   * <li>Only handle lower case marks.
   * </ul>
   */
  static int setmark(char c, int count) {
    Normal.do_xop("setmark");
    
    {
        // Handle anonymous mark operations here
      
        MARKOP op = null;
        switch(c) {
            case '.': op = TOGGLE; break;
            case '<': op = PREV; break; // Nothing to do with visual mode
            case '>': op = NEXT; break; // Nothing to do with visual mode
        }
        if(op != null) {
            if(op != TOGGLE)
                setpcmark();
            G.curwin.anonymousMark(op, count);
            
            return OK;
        }
    }

    if (c == '\'' || c == '`') {
      setpcmark();
      /* keep it even when the cursor doesn't move */
      G.curwin.w_prev_pcmark = G.curwin.w_pcmark;
      return OK;
    }

    if (Util.islower(c)) {
      int		i;
      i = c - 'a';
      G.curbuf.getMark(i).setMark(G.curwin.getWCursor());
      // curbuf.b_namedm[i] = curwin.w_cursor;
      return OK;
    }

    if (false && Util.isupper(c)) {	// NEEDSWORK: upper case marks
      // i = c - 'A';
      // namedfm[i].mark = curwin.w_cursor;
      // namedfm[i].fnum = curbuf.b_fnum;
      // return OK;
    }
    return FAIL;
  }

  /*
   * getmark(c) - find mark for char 'c'
   *
   * Return pointer to FPOS if found (caller needs to check lnum!)
   *	  NULL if there is no mark called 'c'.
   *	  -1 if mark is in other file (only if changefile is TRUE)
   */
  static ViMark getmark(char c, boolean changefile) {
    ViMark m = null;
    if(c == '\'' || c == '`') {
      // make a copy since it might change soon
      m = (ViMark) G.curwin.w_pcmark.copy();
    } else if(c == '[') {
      m = G.curbuf.b_op_start;
    } else if(c == ']') {
      m = G.curbuf.b_op_end;
    } else if(c == '<' || c == '>') {
      ViMark startp = G.curbuf.b_visual_start;
      ViMark endp = G.curbuf.b_visual_end;
      // If either mark's no good, return it and let error handling report it
      if(check_mark(startp, false) == FAIL)
        return startp;
      if(check_mark(endp, false) == FAIL)
        return endp;
      boolean isLtName = c == '<' ? true : false;
      boolean isLtValue = startp.compareTo(endp) < 0 ? true : false;
      m = isLtName == isLtValue ? startp : endp;
      /*
       * For Visual line mode, set mark at begin or end of line
       */
      if(G.curbuf.b_visual_mode == 'V') {
        // the questions is whether column 0 or last column
        int col = 0;
        if(c == '>')
          col = Misc.check_cursor_col(m.getLine(), MAXCOL);
        int lineoff = G.curbuf.getLineStartOffsetFromOffset(m.getOffset());
        m = (ViMark)m.copy();
        G.curbuf.setMarkOffset(m, lineoff + col, false);
      }
    } else if (Util.islower(c)) {
      int i = c - 'a';
      m = G.curbuf.getMark(i);
    }
    return m;
  }

  /**
   * Check a if a position from a mark is valid.
   * Give and error message and return FAIL if not.
   */
  static int check_mark(ViMark mark) {
    return check_mark(mark, true);
  }

  static int check_mark(ViMark mark, boolean messageOK) {
    String msg = null;
    if(mark == null) {
      msg = e_umark;
    } else {
      try {
        if(mark.getLine() > G.curbuf.getLineCount()) {
	  msg = e_markinval;
	}
      } catch(ViMark.MarkOrphanException e) {
	msg = e_marknotset;
      } catch(ViMark.MarkException e) {
	msg = e_marknotset;
      }
    }
    if(msg != null) {
      if(messageOK) {
	Msg.emsg(msg);
      }
      return FAIL;
    }
    return OK;
  }


  /**
   * checkpcmark() - To change context, call setpcmark(), then move the current
   *		   position to where ever, then call checkpcmark().  This
   *		   ensures that the previous context will only be changed if
   *		   the cursor moved to a different line. -- webb.
   *		   If pcmark was deleted (with "dG") the previous mark is
   *		   restored.
   */
  static void checkpcmark() {
    if(check_mark(G.curwin.w_prev_pcmark, false) == OK
       		&& (G.curwin.w_pcmark.equals(G.curwin.getWCursor())
		    || check_mark(G.curwin.w_pcmark, false) == FAIL))
    {
      G.curwin.w_pcmark.setData(G.curwin.w_prev_pcmark);
      G.curwin.w_prev_pcmark.invalidate();
    }
  }

  static void setpcmark(ViFPOS fpos) {
    if(G.global_busy) {
      return;
    }
    G.curwin.w_prev_pcmark = G.curwin.w_pcmark;
    G.curwin.w_pcmark.setMark(fpos);
    // NEEDSWORK: pcmark and jump list stuff...
  }

  /** @deprecated */
  static void setpcmark(int offset) {
    setpcmark(G.curwin, offset);
  }

  /** @deprecated */
  static void setpcmark(ViTextView tv, int offset) {
    if(G.global_busy) {
      return;
    }
    ((Window)tv).w_prev_pcmark = ((Window)tv).w_pcmark;
    tv.getBuffer().setMarkOffset(((Window)tv).w_pcmark, offset, false);
    // NEEDSWORK: pcmark and jump list stuff...
  }

  static void setpcmark() {
    setpcmark(G.curwin.getWCursor());
  }
}

// vi:set sw=2:
