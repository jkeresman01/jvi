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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TooManyListenersException;

import javax.swing.text.Segment;

import com.raelity.jvi.ViManager;
import com.raelity.text.*;

/**
 * Searching, regexp and substitution.
 */
class Search implements Constants {

  ///////////////////////////////////////////////////////////////////////
  //
  // normal searching like "/" and "?"
  //

  private static int lastDir = FORWARD;
  private static ViCmdEntry searchCommandEntry;
  private static String lastPattern;
  private static String lastSubstitution;
  private static int searchCount;
  private static int searchFlags;

  private static ViCmdEntry getSearchCommandEntry() {
    if(searchCommandEntry == null) {
      try {
        searchCommandEntry = ViManager.getViFactory()
                              .createCmdEntry(ViCmdEntry.SEARCH_ENTRY);
        searchCommandEntry.addActionListener(
          new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              searchEntryComplete(ev);
            }});
      }
      catch (TooManyListenersException ex) {
        ex.printStackTrace();
        throw new RuntimeException();
      }
    }
    return searchCommandEntry;
  }

  static private void searchEntryComplete(ActionEvent ev) {
    ViManager.stopCommandEntry();
    String pattern = searchCommandEntry.getCommand();
    String cmd = ev.getActionCommand();
    // if not <CR> must be an escape, just ignore it
    if(cmd.charAt(0) == '\n') {
      runSearch(pattern);
    }
  }

  static private void runSearch(String pattern) {
    G.curwin.setWSetCurswant(true);
    if(pattern.equals("")) {
      if(lastPattern == null) {
        Msg.emsg(Messages.e_noprevre);
        return;
      }
      pattern = lastPattern;
    }
    lastPattern = pattern;
    // executeSearch(pattern, lastDir, G.p_ic.getBoolean());
    ViFPOS pos = G.curwin.getWCursor().copy();
    int rc = searchit(null, pos, lastDir, pattern,
                      searchCount, searchFlags, 0, G.p_ic.getBoolean());
    if(rc != FAIL) {
      Msg.smsg((lastDir == FORWARD ? "/" : "?") + pattern);
    }
    searchResult = rc;
    /* ***************************
    if(rc == 0) {
      Normal.clearopInstance();
    }
    ******************************/
  }

  private static int searchResult;
  
  static int doSearch(CMDARG cap, int count, int flags) {
    String mode = "";
    if (cap.cmdchar == '/') {
      lastDir = FORWARD;
      mode = "/";
    } else if (cap.cmdchar == '?') {
      lastDir = BACKWARD;
      mode = "?";
    }
    searchCount = count;
    searchFlags = flags;
    ViManager.startCommandEntry(getSearchCommandEntry(),
                                mode, G.curwin, null);
    // HACK
    // Insist that command entry happens in a dialog so that
    // for now, we get here after the command entry is complete and the
    // search has been run. Use a few globals to communicate the result
    // of the search. This can all be revisited when there is a more
    // understandable input handling mechanism.
    
    if(searchResult == FAIL) {
      return 0;
    } else {
      return 1; // NEEDSWORK: not returning 2 ever, so not line mode.
    }
  }

  static int doNext(CMDARG cap, int count, int flag) {
    G.curwin.setWSetCurswant(true);
    int dir = ((flag & SEARCH_REV) != 0 ? - lastDir : lastDir);
    //G.curwin.repeatSearch(dir);
    int rc = FAIL;
    if(lastPattern == null) {
      Msg.emsg(Messages.e_noprevre);
    } else {
      // executeSearch(lastPattern, dir, G.p_ic.getBoolean());
      ViFPOS pos = G.curwin.getWCursor().copy();
      rc = searchit(null, pos, dir, lastPattern,
                        count, flag, 0, G.p_ic.getBoolean());
      if(rc != FAIL) {
	Msg.smsg((dir == FORWARD ? "/" : "?") + lastPattern);
      }
    }
    return rc;
  }

  ////////////////////////////////////////////////////////////////
  //
  // searchc
  //

  /** for searchc, last character searched for. */
  static int	    lastc = 0;
  /** for searchc, last direction of character search. */
  static int	    lastcdir;
  /** for searchc, last type of search ("find" or "to"). */
  static boolean    lastctypeT;

  /**
   * searchc(c, dir, typeT, count);
   *
   * Search for character 'c', in direction 'dir'. If 'typeT' is 0, move to the
   * position of the character, otherwise move to just before the char.
   * Repeat this 'count' times.
   */
  static boolean searchc(int c, int dir, boolean typeT, int count) {
    int		    col;
    int		    len;

    if (c != 0)	/* normal search: remember args for repeat */
    {
      // if (!KeyStuffed)    // don't remember when redoing
      {
	lastc = c;
	lastcdir = dir;
	lastctypeT = typeT;
      }
    } else {		// repeat previous search
      if (lastc == 0)
	return false;
      if (dir != 0)	// repeat in opposite direction
	dir = -lastcdir;
      else
	dir = lastcdir;
      typeT = lastctypeT;
      c = lastc;
    }

    ViFPOS fpos = G.curwin.getWCursor();
    Segment seg = G.curwin.getLineSegment(fpos.getLine());
    col = fpos.getColumn();
    len = seg.count - 1; // don't count the newline, MUST_HAVE_NL

    while (count-- != 0)
    {
      for (;;)
      {
	if ((col += dir) < 0 || col >= len)
	  return false;
	if (seg.array[seg.offset + col] == c)
	  break;
      }
    }
    if (typeT) {
      col -= dir;
    }
    // curwin->w_cursor.col = col;
    G.curwin.setCaretPosition(
		  col + G.curwin
			  .getLineStartOffsetFromOffset(fpos.getOffset()));
    return true;
  }

  ////////////////////////////////////////////////////////////////////
  //
  // The following routines do the word searches performed by the 'w', 'W',
  // 'b', 'B', 'e', and 'E' commands.
  //

  //
  // To perform these searches, characters are placed into one of three
  // classes, and transitions between classes determine word boundaries.
  //
  // The classes are:
  //
  // 0 - white space
  // 1 - keyword charactes (letters, digits and underscore)
  // 2 - everything else
  //

  /* type of the word motion being performed,
   * true implies capital motion (W, E) vs (w, e).
   */
  private static boolean stype;

  /**
   * cls() - returns the class of character at curwin->w_cursor
   *<br>
   * The 'type' of the current search modifies the classes of characters if a
   * 'W', 'B', or 'E' motion is being done. In this case, chars. from class 2
   * are reported as class 1 since only white space boundaries are of interest.
   */
  private static int cls() {
    int	    c;

    c = Misc.gchar_cursor();
    if (c == ' ' || c == '\t' || c == '\n')
      return 0;

    if (Misc.vim_iswordc(c))
      return 1;

    //
    // If stype is non-zero, report these as class 1.
    //
    return ( ! stype) ? 2 : 1;
  }


  /**
   * fwd_word(count, type, eol) - move forward one word
   *<br>
   * Returns FAIL if the cursor was already at the end of the file.
   * If eol is TRUE, last word stops at end of line (for operators).
   */
  static int fwd_word(int count, boolean type, boolean eol) {
    int		sclass;	    /* starting class */
    int		i;
    boolean	last_line;

    stype = type;
    while (--count >= 0) {
      sclass = cls();

      //
      // We always move at least one character, unless on the last character
      // in the buffer.
      //
      last_line = (G.curwin.getWCursor().getLine()
		   	== G.curwin.getLineCount());
      i = Misc.inc_cursor();
      if (i == -1 || (i == 1 && last_line)) // started at last char in file
	return FAIL;
      if (i == 1 && eol && count == 0)      // started at last char in line
	return OK;

      //
      // Go one char past end of current word (if any)
      //
      if (sclass != 0)
	while (cls() == sclass) {
	  i = Misc.inc_cursor();
	  if (i == -1 || (i == 1 && eol && count == 0))
	    return OK;
	}

      //
      // go to next non-white
      //
      while (cls() == 0) {
	//
	// We'll stop if we land on a blank line
	//
	if(G.curwin.getWCursor().getColumn() == 0
	   	&& Misc.gchar_cursor() == '\n') {
	  break;
	}

	i = Misc.inc_cursor();
	if (i == -1 || (i == 1 && eol && count == 0))
	  return OK;
      }
    }
    return OK;
  }

  /**
   * bck_word() - move backward 'count' words
   *<br>
   * If stop is TRUE and we are already on the start of a word, move one less.
   *
   * @return FAIL if top of the file was reached.
   */
  static int bck_word(int count, boolean type, boolean stop) {
    int		sclass;	    /* starting class */

    stype = type;
    while (--count >= 0) {
      sclass = cls();
      if (Misc.dec_cursor() == -1)     /* started at start of file */
	return FAIL;

finished_block:
      do {
	if (!stop || sclass == cls() || sclass == 0) {
	  //
	  // Skip white space before the word.
	  // Stop on an empty line.
	  //
	  while (cls() == 0) {
	    ViFPOS fpos = G.curwin.getWCursor();
	    if (fpos.getColumn() == 0 && Util.lineempty(fpos.getLine()))
	      break finished_block;
	    if (Misc.dec_cursor() == -1)   // hit start of file, stop here
	      return OK;
	  }

	  //
	  // Move backward to start of this word.
	  //
	  if (skip_chars(cls(), BACKWARD))
	    return OK;
	}

	Misc.inc_cursor();		 // overshot - forward one
      } while(false); // was a label here - finished:
      stop = false;
    }
    return OK;
  }

  /**
   * end_word() - move to the end of the word
   *<br>
   * There is an apparent bug in the 'e' motion of the real vi. At least on the
   * System V Release 3 version for the 80386. Unlike 'b' and 'w', the 'e'
   * motion crosses blank lines. When the real vi crosses a blank line in an
   * 'e' motion, the cursor is placed on the FIRST character of the next
   * non-blank line. The 'E' command, however, works correctly. Since this
   * appears to be a bug, I have not duplicated it here.
   *<br>
   * If stop is TRUE and we are already on the end of a word, move one less.
   * If empty is TRUE stop on an empty line.
   *
   * @return FAIL if end of the file was reached.
   */
  static int end_word(int count, boolean type, boolean stop, boolean empty) {
    int		sclass;	    /* starting class */

    stype = type;
    while (--count >= 0) {
      sclass = cls();
      if (Misc.inc_cursor() == -1)
	return FAIL;

finished_block:
      do {
	//
	// If we're in the middle of a word, we just have to move to the end
	// of it.
	//
	if (cls() == sclass && sclass != 0) {
	  //
	  // Move forward to end of the current word
	  //
	  if (skip_chars(sclass, FORWARD))
	    return FAIL;
	} else if (!stop || sclass == 0) {
	  //
	  // We were at the end of a word. Go to the end of the next word.
	  // First skip white space, if 'empty' is TRUE, stop at empty line.
	  //
	  while (cls() == 0) {
	    ViFPOS fpos = G.curwin.getWCursor();
	    if (empty && fpos.getColumn() == 0
		&& Util.lineempty(fpos.getLine()))
	      break finished_block;
	    if (Misc.inc_cursor() == -1)    // hit end of file, stop here
	      return FAIL;
	  }

	  //
	  // Move forward to the end of this word.
	  //
	  if (skip_chars(cls(), FORWARD))
	    return FAIL;
	}
	Misc.dec_cursor();	// overshot - one char backward
      } while(false); // was a label here - finished:
finished:
      stop = false;		// we move only one word less
    }
    return OK;
  }

  /**
   * bckend_word(count, type) - move back to the end of the word
   *<br>
   * If 'eol' is TRUE, stop at end of line.
   *
   * @return FAIL if start of the file was reached.
   */
  static int bckend_word(int count, boolean type, boolean eol) {
    int		sclass;	    // starting class
    int		i;

    stype = type;
    while (--count >= 0) {
      sclass = cls();
      if ((i = Misc.dec_cursor()) == -1)
	return FAIL;
      if (eol && i == 1)
	return OK;

      //
      // Move backward to before the start of this word.
      //
      if (sclass != 0) {
	while (cls() == sclass)
	  if ((i = Misc.dec_cursor()) == -1 || (eol && i == 1))
	    return OK;
      }

      //
      // Move backward to end of the previous word
      //
      while (cls() == 0) {
	ViFPOS fpos = G.curwin.getWCursor();
	if (fpos.getColumn() == 0 && Util.lineempty(fpos.getLine()))
	  break;
	if ((i = Misc.dec_cursor()) == -1 || (eol && i == 1))
	  return OK;
      }
    }
    return OK;
  }

  /**
   * Skip a row of characters of the same class.
   * @return TRUE when end-of-file reached, FALSE otherwise.
   */
  private static boolean skip_chars(int cclass, int dir) {
    while (cls() == cclass)
      if ((dir == FORWARD ? Misc.inc_cursor() : Misc.dec_cursor()) == -1)
	return true;
    return false;
  }

  /////////////////////////////////////////////////////////////////////
  //
  // regualr expression handling stuff
  //


  // simple cache. Helps "n" and "N" commands
  static RegExp lastRegExp;
  static String lastRegExpPattern = "";
  static boolean lastRegExpIC;

  /** Get a compiled regular expression. Clean up the escaping as needed. */
  static RegExp getRegExp(String pattern, boolean ignoreCase) {
    String cleanPattern = cleanupPattern(pattern);
    // NEEDSWORK: getRegExp: cache re's, LRU?
    if(cleanPattern.equals(lastRegExpPattern) && lastRegExpIC == ignoreCase) {
      return lastRegExp;
    }
    RegExp re = null;
    try {
      int flags = ignoreCase ? RegExp.IGNORE_CASE : 0;
      re = RegExpFactory.create();
      String compilePattern = re.patternType() == RegExp.PATTERN_SIMPLE
                                ? pattern : cleanPattern;
      re.compile(cleanPattern, flags);
      // cache the information
      lastRegExpPattern = cleanPattern;
      lastRegExpIC = ignoreCase;
      lastRegExp = re;
    } catch(RegExpPatternError ex) {
      Msg.emsg("Invalid search string: \"" + pattern + "\" " + ex.getMessage());
      re = null;
    }
    return re;
  }

  private static final int ESCAPED_FLAG = 0x10000;
  /**
   * Change metacharacter escaping of input pattern to match
   * the perl5 requirements. Do this because non-standard metachars
   * may be be escaped in patterns according to metaEscape option.
   * <p> This should be a table:
   *    'c' is escaped          'c' is not escaped
   *         c  --> \c                c  --> c
   *         \c --> c                 \c --> \c
   * </p><p>The metaEquals (use '=' instead of '?') makes things a real mess.
   *    '?' is escaped          '?' is not escaped
   *         =  --> =                =  --> ?
   *         \= --> ?                \= --> =
   *         ?  --> \?               ?  --> \?
   *         \? --> \?               \? --> \?  (nonsense)
   *
   * </p><p>
   * And finally, either \&lt; or \&gt; are replaced by \b.
   * </p>
   */
  static String cleanupPattern(String s) {
    StringBuffer sb = new StringBuffer();
    String metacharacterEscapes = G.p_meta_escape.getString();
    boolean isEscaped = false;
    for(int in = 0; in < s.length(); in++) {
      char c = s.charAt(in);
      if( ! isEscaped && c == '\\') {
        isEscaped = true;
        continue;
      }
      if((c == '=' || c == '?') && G.p_meta_equals.getBoolean()) {
        int c_state = c;
        if(isEscaped) {
          c_state |= ESCAPED_FLAG;      // indicate escaped in pattern
        }
        boolean optionalOptionEscaped = metacharacterEscapes.indexOf("?") >= 0;
        switch(c_state) {
          case '=':
            if(optionalOptionEscaped) { sb.append("="); }
            else { sb.append("?"); }
            break;
          case '=' | ESCAPED_FLAG:
            if(optionalOptionEscaped) { sb.append("?"); }
            else { sb.append("="); }
            break;
          case '?':
          case '?' | ESCAPED_FLAG:
            sb.append("\\?");
            break;
        }
      } else {
        if(metacharacterEscapes.indexOf(c) >= 0) { // metachar gets escaped
          // reverse of what was seen
          if( ! isEscaped) {
            sb.append("\\");
          }
          sb.append(c);
        } else if((c == '<' || c == '>') && isEscaped) {
          sb.append("\\b");
        } else {
          // pass through what was seen
          if(isEscaped) {
            sb.append("\\");
          }
          sb.append(c);
        }
      }
      isEscaped = false;
    }
    return sb.toString();
  }

  static String top_bot_msg = "search hit TOP, continuing at BOTTOM";
  static String bot_top_msg = "search hit BOTTOM, continuing at TOP";

  // HACK: who uses this?
  private static int search_match_len;

  /**
   * lowest level search function.
   * Search for 'count'th occurrence of 'str' in direction 'dir'.
   * Start at position 'pos' and return the found position in 'pos'.
   *
   * <br>if (options & SEARCH_MSG) == 0 don't give any messages
   * <br>if (options & SEARCH_MSG) == SEARCH_NFMSG dont give 'notfound' messages
   * <br>if (options & SEARCH_MSG) == SEARCH_MSG give all messages
   * <br>if (options & SEARCH_HIS) put search pattern in history
   * <br>if (options & SEARCH_END) return position at end of match
   * <br>if (options & SEARCH_START) accept match at pos itself
   * <br>if (options & SEARCH_KEEP) keep previous search pattern
   *
   * @return OK for success, FAIL for failure.
   */
   //
   // Somehow anchoring at the beginning of the line seems to work fine.
   // Not sure how. The vim code has a "at start of line" flag it passes
   // to the reg exp matcher.
   //
  static int searchit(Window win,      // BUF,    NOT USED
                      ViFPOS pos,      // FPOS,
                      int dir,
                      String pattern,
                      int count,
                      int options,
                      int pat_use,
                      boolean ignoreCase)
  {
    ViFPOS start_pos;
    boolean found;
    boolean at_first_line;
    boolean match_ok;

    int extra_col;
    int lnum;
    int match = 0;
    int matchend = 0;
    int p;
    String wmsg = null;


    RegExp prog = getRegExp(pattern, ignoreCase); // various arguments in vim
    if(prog == null) {
        //Msg.emsg("Invalid search string: " + pattern);
        return FAIL;
    }

    if ((options & SEARCH_START) != 0)
      extra_col = 0;
    else
      extra_col = 1;


    /*
     * find the string
     */
    do	// loop for count
    {
      start_pos = pos.copy(); // remember start pos for detecting no match
      found = false;		// default: not found
      at_first_line = true;	// default: start in first line
      // if (pos.getLine() == 0) // correct lnum for when starting in line 0

      //
      // Start searching in current line, unless searching backwards and
      // we're in column 0.
      //
      if (dir == BACKWARD && start_pos.getColumn() == 0) {
        lnum = pos.getLine() - 1;
        at_first_line = false;
      } else {
        lnum = pos.getLine();
      }

      for (int loop = 0; loop <= 1; ++loop) {   // loop twice if 'wrapscan' set
        for ( ; lnum > 0 && lnum <= G.curwin.getLineCount();
              lnum += dir, at_first_line = false)
        {
          //
          // Look for a match somewhere in the line.
          //
          //////////ptr = ml_get_buf(buf, lnum, FALSE);
          Segment seg = G.curwin.getLineSegment(lnum);
                                                // NEEDSWORK: AT_BOL == TRUE
          if(prog.search(seg.array, seg.offset, seg.count)) {
            match = prog.start(0) - seg.offset; // column index
            matchend = prog.stop(0) - seg.offset;
            int eolColumn = G.curwin.getLineEndOffset(lnum) -
                             G.curwin.getLineStartOffset(lnum) - 1;
            //
            // Forward search in the first line: match should be after
            // the start position. If not, continue at the end of the
            // match (this is vi compatible).
            //
            if (dir == FORWARD && at_first_line) {
              match_ok = true;
              //
              // When *match == NUL the cursor will be put one back
              // afterwards, compare with that position, otherwise
              // "/$" will get stuck on end of line.
              //
              while (((options & SEARCH_END) != 0) ?
                     (matchend - 1  < start_pos.getColumn() + extra_col)
                     : (match - (match == eolColumn ? 1 : 0)
                                    < start_pos.getColumn() + extra_col))
              {
                //
                // If vi-compatible searching, continue at the end
                // of the match, otherwise continue one position
                // forward.
                //
                if(G.p_cpo_search.getBoolean()) {
                  p = matchend;
                  if (match == p && p != eolColumn)
                    ++p;
                } else {
                  p = match;
                  if (p != eolColumn)
                    ++p;
                }
                if (p != eolColumn
                                                // NEEDSWORK: AT_BOL == FALSE
                    && prog.search(seg.array, seg.offset + p, seg.count - p)) {
                  match = prog.start(0) - seg.offset; // column index
                  matchend = prog.stop(0) - seg.offset;
                } else {
                  match_ok = false;
                  break;
                }
              }
              if (!match_ok)
                continue;
            }
            if (dir == BACKWARD)
            {
              //
              // Now, if there are multiple matches on this line,
              // we have to get the last one. Or the last one before
              // the cursor, if we're on that line.
              // When putting the new cursor at the end, compare
              // relative to the end of the match.
              //
              match_ok = false;
              for (;;)
              {
                int match01 = prog.start(0) - seg.offset; // column index
                int matchend01 = prog.stop(0) - seg.offset;
                if (!at_first_line
                    || (((options & SEARCH_END) != 0)
                        ? (matchend01 - 1 + extra_col
                                               <= start_pos.getColumn())
                        : (match01 + extra_col <= start_pos.getColumn())))
                {
                  match_ok = true;
                  match = match01;
                  matchend = matchend01;
                }
                else
                  break;
                //
                // If vi-compatible searching, continue at the end
                // of the match, otherwise continue one position
                // forward.
                //
                if(G.p_cpo_search.getBoolean()) {
                  p = matchend;
                  if (p == match && p != eolColumn)
                    ++p;
                } else {
                  p = match;
                  if (p != eolColumn)
                    ++p;
                }
                if (p == eolColumn
                                                // NEEDSWORK: AT_BOL == FALSE
                    || !prog.search(seg.array, seg.offset + p, seg.count - p)) {
                  break;
                }
              }

              //
              // If there is only a match after the cursor, skip
              // this match.
              //
              if (!match_ok)
                continue;
            }

            int tcol;
            if (((options & SEARCH_END) != 0) && (options & SEARCH_NOOF) == 0)
              tcol = matchend - 1;
            else
              tcol = match;
            pos.setPosition(lnum, tcol);
            found = true;
            break;
          }
          Misc.line_breakcheck();	// stop if ctrl-C typed
          if (false/*got_int*/)
            break;

          if (loop != 0 && lnum == start_pos.getLine())
            break;	    // if second loop, stop where started
        }
        at_first_line = false;

        //
        // stop the search if wrapscan isn't set, after an interrupt and
        // after a match
        ///
        if (!G.p_ws.getBoolean() /*|| got_int*/ || found)
          break;

        //
        // If 'wrapscan' is set we continue at the other end of the file.
        // If 'shortmess' does not contain 's', we give a message.
        // This message is also remembered in keep_msg for when the screen
        // is redrawn. The keep_msg is cleared whenever another message is
        // written.
        //
        if (dir == BACKWARD) {    // start second loop at the other end
          lnum = G.curwin.getLineCount();
          if ((options & SEARCH_MSG) != 0)
	    // defer message until after things are positioned.
            // Msg.wmsg(top_bot_msg/*, true*/);
	    wmsg = top_bot_msg;
        } else {
          lnum = 1;
          if ((options & SEARCH_MSG) != 0)
            // Msg.wmsg(bot_top_msg/*, true*/);
	    wmsg = bot_top_msg;
        }
      }
      if (false/*got_int*/)
        break;
    } while (--count > 0 && found);   // stop after count matches or no match

    if (!found) {	    // did not find it
      if (false/*got_int*/)
        Msg.emsg(Messages.e_interr);
      else if ((options & SEARCH_MSG) == SEARCH_MSG) {
        if (G.p_ws.getBoolean())
          Msg.emsg(Messages.e_patnotf2 + pattern);
        else if (lnum == 0)
          Msg.emsg("search hit TOP without match for: " + pattern);
        else
          Msg.emsg("search hit BOTTOM without match for: " + pattern);
      }
      return FAIL;
    }
    search_match_len = matchend - match;

    if((options & SEARCH_MARK) != 0) {
      MarkOps.setpcmark();
    }
    Misc.gotoLine(G.curwin.getLineNumber(pos.getOffset()), 0);
    G.curwin.setSelect(pos.getOffset(), pos.getOffset() + search_match_len);
    G.curwin.setWSetCurswant(true);
    if(wmsg != null) {
      Msg.wmsg(wmsg/*, true*/);
    }
    return OK;
  }

  ////////////////////////////////////////////////////////////////
  //
  // Stuff from ex_cmds.c
  //
  
  private static int nMatch;

  /**
   * substitute command. first arg is /pattern/substitution/{flags}
   */

  static void substitute(ColonCommands.ColonEvent cev) {
    if(cev.getNArg() != 1) {
      Msg.emsg("substitue takes an argument (FOR NOW)");
      return;
    }
    String cmd = cev.getArg(1);
    String pattern = null;
    RegExp prog = null;
    char[] substitution = null;
    boolean doAll = false; // should be bit flag???
    boolean hasEscape = false;
    char delimiter = cmd.charAt(0);
    Segment line;
    int cursorLine = 0; // set to line number of last change
    int sidx = 1; // after delimiter
    
    //
    // pick up the pattern
    //

    int sidx01 = sidx;
    sidx = skip_regexp(cmd, sidx, delimiter, true);
    if(sidx01 == sidx) {
      pattern = lastPattern;
    } else {
      pattern = cmd.substring(sidx01, sidx);
      lastPattern = pattern;
    }
    
    //
    // pick up the substitution string
    //

    sidx++; // first char of substitution
    sidx01 = sidx;
    for( ; sidx < cmd.length(); sidx++) {
      char c = cmd.charAt(sidx);
      if(c == delimiter) {
        break;
      }
      if(c == '\\' && sidx+1 < cmd.length()) {
        ++sidx;
      }
    }
    if(sidx01 == sidx) {
      lastSubstitution = "";
    } else {
      lastSubstitution = cmd.substring(sidx01, sidx);
    }
    substitution = lastSubstitution.toCharArray();
    hasEscape = lastSubstitution.indexOf('\\') != -1
                || lastSubstitution.indexOf('&') != -1;
                // NEEDSWORK: || lastSubstitution.indexOf('~', sidx01) != -1;
    
    //
    // pick up the flags
    //
    
    ++sidx; // move past the delimiter
    for( ; sidx < cmd.length(); sidx++) {
      char c = cmd.charAt(sidx);
      switch(c) {
        case 'g':
          doAll = true;
          break;
        
        case ' ':
          // silently ignore blanks
          break;
          
        default:
          Msg.emsg("ignoring flag: '" + c + "'");
          break;
      }
    }
    
    //
    // compile regex
    //
    
    prog = getRegExp(pattern, G.p_ic.getBoolean());
    if(prog == null) {
      return;
    }

    int line1 = cev.getLine1();
    int line2 = cev.getLine2();
    StringBuffer sb;
    int nLine = 0;
    nMatch = 0;
    for(int i = line1; i <= line2; i++) {
      line = G.curwin.getLineSegment(i);
      sb = substitute_line(prog, line, doAll, substitution, hasEscape);
      if(sb != null) {
        G.curwin.replaceString(G.curwin.getLineStartOffset(i),
                               G.curwin.getLineEndOffset(i)-1,
                               sb.toString());
        cursorLine = i;  // keep track of last line changed
	nLine++;
        // System.err.println("sub: " + sb);
      }
    }
    
    if(nMatch == 0) {
      Msg.emsg(Messages.e_patnotf2 + pattern);
    } else {
      if(nMatch >= G.p_report) {
	String msg = "" + nMatch + " substitution" + Misc.plural(nMatch)
	             + " on " + nLine + " line" + Misc.plural(nLine);
	G.curwin.getStatusDisplay().displayStatusMessage(msg);
      }
    }
    
    if(cursorLine > 0) {
      // move cursor
      G.curwin.setCaretPosition(cursorLine, 0);
      Edit.beginline(BL_WHITE | BL_FIX);
    }
  }

  /**
   * This method preforms the substitution within one line of text.
   * null is returned if there was no match on the line.
   * This is not adapted vim code.
   * <br><b>NEEDSWORK:</b><ul>
   * <li>Handle more flags, not just doAll.
   * </ul>
   * @param prog the compiled regular expression
   * @param line text to check for match and substitue
   * @param doAll if true, substitute all occurences within the line
   * @param subs the substitution string
   * @param hasEscape if true, then escape processing is needed on <i>subs</i>.
   * @return a string buffer with the new line, null is
   * returned if there was no match on the line.
   */
  static StringBuffer substitute_line(RegExp prog,
                                      Segment line,
                                      boolean doAll,
                                      char[] subs,
                                      boolean hasEscape)
  {
    StringBuffer sb = null;
    int offset = line.offset;
    int lookHere = offset;
    int count = line.count -1; // don't count trailing newline
    int endOffset = offset + count;
    int lastMatch = -1;

    while(prog.search(line.array, lookHere, count)) {
      nMatch++;
      if(sb == null) { // got a match, make sure there's a buffer
        sb = new StringBuffer();
      }
      // copy characters skipped finding match from input string to output.
      // If no match, then copy to end of line.
      int matchOffset = prog.start(0);
      if(lastMatch == matchOffset) {
        // prevent infinite loops, can happen with match of zero characters
        ++lookHere;
        count = endOffset - offset;
        if(count <= 0) {
          break;
        }
        continue;
      }
      lastMatch = matchOffset;
      sb.append(line.array, offset, matchOffset - offset);
      // copy substitution string, do escaping if needed
      if(hasEscape) {
        translateSubstitution(prog, line, sb, subs);
      } else {
        sb.append(subs);
      }
      // advance past matched characters
      offset = prog.stop(0);
      lookHere = offset;
      count = endOffset - offset;
      if( ! doAll) {
        // only do one substitute
        break;
      }
    }
    if(sb != null && endOffset > offset) {
      // if there was any substitution, copy chars after last match
      sb.append(line.array, offset, endOffset - offset);
    }
    return sb;
  }

  /**
   * Handle substitution where there is escape handling.
   * Append the substitution to string buffer argument.
   * This is not adapted vim code.
   * <br><b>NEEDSWORK:</b><ul>
   * <li>append multiple non-escaped chars at once, see jpython's RegexObject
   * </ul>
   * @param prog the compiled regular expression with match result
   * @param line the line that has the match
   * @param sb append substitution to here
   * @param subs substitution string, contains escape characters
   */
  static void translateSubstitution(RegExp prog,
                                   Segment line,
                                   StringBuffer sb,
                                   char[] subs)
  {
    int i = 0;
    char c;

    for( ; i < subs.length; i++) {
      c = subs[i];
      switch(c) {
        case '&':
          // copy chars that matched
          sb.append(line.array, prog.start(0), prog.length(0));
          break;

        case '\\':
          if(i+1 < subs.length) {
            i++;
            c = subs[i];
            switch(c) {
              case '&':
                sb.append('&');
                break;

              case '0':
              case '1':
              case '2':
              case '3':
              case '4':
              case '5':
              case '6':
              case '7':
              case '8':
              case '9':
                int group = c - '0';
                sb.append(line.array, prog.start(group), prog.length(group));
                break;

              default:
                // escaped a regular character, just append it
                sb.append(c);
                break;
            }
            break; // after handling the escape character
          }

          // last char of string was backslash
          // fall through to append the '\\'

        default:
          sb.append(c);
          break;
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  //
  // Stuff from regexp.c
  //

  /**
   * REGEXP_INRANGE contains all characters which are always special in a []
   * range after '\'.
   */
  static private String REGEXP_INRANGE = "]^-\\";
  /**
   * REGEXP_ABBR contains all characters which act as abbreviations after '\'.
   * These are:
   *  <ul>
   * <li> \r	- New line (CR).
   * <li> \t	- Tab (TAB).
   * <li> \e	- Escape (ESC).
   * <li> \b	- Backspace (Ctrl('H')).
   */
  static private String REGEXP_ABBR = "rteb";

  /**
   * Skip past regular expression.
   * Stop at end of 'p' of where 'dirc' is found ('/', '?', etc).
   * Take care of characters with a backslash in front of it.
   * Skip strings inside [ and ].
   * @param s string containing regular expression
   * @param sidx first char of regular expression
   * @param dirc char that terminates (and started) regular expression
   * @param magic 
   * @return index of char that terminated regexp, may be past end of string
   */
  static int skip_regexp(String s, int sidx, char dirc, boolean magic) {
    char c = (char)0;
    while(sidx < s.length()) {
      c = s.charAt(sidx);
      if (c == dirc)	// found end of regexp
        break;
      if (c == '[' && magic
          || c == '\\'
             && sidx+1 < s.length()
             && s.charAt(sidx+1) == '['
             && ! magic) {
        sidx = skip_range(s, sidx + 1);
        if(sidx >= s.length()) {
          break;
        }
      }
      else if (c == '\\' && sidx+1 < s.length())
        ++sidx;    // skip next character
      ++sidx;
    }
    return sidx;
  }

  /**
   * Skip over a "[]" range.
   * "p" must point to the character after the '['.
   * The returned pointer is on the matching ']', or the terminating NUL.
   */
  static private int skip_range(String s, int sidx) {
    boolean    cpo_lit;	    /* 'cpoptions' contains 'l' flag */
    char c;

    if(sidx >= s.length()) {
      return sidx;
    }

    //cpo_lit = (!reg_syn && vim_strchr(p_cpo, CPO_LITERAL) != NULL);
    cpo_lit = false;

    c = s.charAt(sidx);

    if (c == '^')	/* Complement of range. */
      ++sidx;
    if (c == ']' || c == '-')
      ++sidx;
    while( sidx < s.length()) {
      c = s.charAt(sidx);
      if(c == ']') {
        break;
      }
      if (c == '-') {
        ++sidx;
        if(sidx < s.length() && (c = s.charAt(sidx)) != ']') {
          ++sidx;
        }
      }
      else if (c == '\\') {
        if(sidx+1 < s.length()) {
          char c2 = s.charAt(sidx+1);
          if(Util.vim_strchr(REGEXP_INRANGE, c2) != null
             || (!cpo_lit && Util.vim_strchr(REGEXP_ABBR, c2) != null)) {
            sidx += 2;
          }
        }
      }
      else if (c == '[')
      {
        MutableInt mi = new MutableInt(sidx);
        if (skip_class_name(s, mi) == null) {
          ++sidx; /* It was not a class name */
        } else {
          sidx = mi.getValue();
        }
      }
      else
        ++sidx;
    }

    return sidx;
  }

  //
  // Character class stuff from vim's regexp.c
  //

  /**
   * character class namedata.
   */
  static class CCNameData {
    String name;
    CCCheck checkFunc;
    CCNameData(String name, CCCheck checkFunc) {
      this.name = name;
      this.checkFunc = checkFunc;
    }
  }

  /**
   * character class check function
   */
  interface CCCheck {
    boolean doCheck(int c);
  }

  /* *****************************************************************
#define t(n, func) { sizeof(n) - 1, func, n }
      static const namedata_t class_names[] =
      {
          t("alnum:]", isalnum),		t("alpha:]", isalpha),
          t("blank:]", my_isblank),     	t("cntrl:]", iscntrl),
          t("digit:]", isdigit),		t("graph:]", isgraph),
          t("lower:]", islower),		t("print:]", vim_isprintc),
          t("punct:]", ispunct),		t("space:]", vim_isspace),
          t("upper:]", isupper),		t("xdigit:]", isxdigit),
          t("tab:]",   my_istab),		t("return:]", my_isreturn),
          t("backspace:]", my_isbspace),	t("escape:]", my_isesc)
      };
#undef t
  ***********************************************************************/

  /**
   * Check for a character class name.  "pp" is at the '['.
   * If not: NULL is returned; If so, a function of the sort "is*"
   * is returned and
   * the name is skipped.
   * @param s
   * @param mi on entry hold start string index;
   * on return string index to skip name (if any)
   * @return function used to check, null if not a char class
   */
  static CCCheck skip_class_name(String s, MutableInt mi) {
    int sidx = mi.getValue();
    // Assume there is not a class name
    return null;

    /* ******************************************************************
    const namedata_t *np;

    if ((*pp)[1] != ':')
        return NULL;
    for (   np = class_names;
            np < class_names + sizeof(class_names) / sizeof(*class_names);
            np++)
        if (STRNCMP(*pp + 2, np.name, np.len) == 0)
        {
            *pp += np.len + 2;
            return np.func;
        }
    return NULL;
    *********************************************************************/
  }
}
