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

import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.manager.Scheduler;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.event.ChangeEvent;

import com.raelity.text.*;
import com.raelity.text.TextUtil.MySegment;

import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;

import static com.raelity.jvi.core.Constants.*;
import static com.raelity.jvi.core.KeyDefs.*;
import static com.raelity.jvi.core.MarkOps.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Misc01.*;
import static com.raelity.jvi.core.Util.*;

/**
 * Searching, regexp and substitution.
 * Everything's static, can only do one thing at a time.
 */
public class Search {
  private static final Logger LOG = Logger.getLogger(Search.class.getName());

  ///////////////////////////////////////////////////////////////////////
  //
  // normal searching like "/" and "?"
  //
  private static ViCmdEntry searchCommandEntry;
  
  // parameters for the current search, they do not change during search
  private static int searchCount;
  private static int searchFlags;
  private static int lastDir = FORWARD;
  
  // state when incremental search started
  private static ViFPOS searchPos;
  private static int searchTopLine;
  private static boolean didIncrSearch;
  private static boolean setPCMarkAfterIncrSearch;
  private static boolean incrSearchSucceed;

  // for next command and such
  private static String lastPattern;
  private static String lastSubstitution;

  private static ViCmdEntry getSearchCommandEntry() {
    if(searchCommandEntry == null) {
      searchCommandEntry = ViManager.getFactory()
                            .createCmdEntry(ViCmdEntry.SEARCH_ENTRY);
      searchCommandEntry.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            searchEntryComplete(ev);
          }   });
    }
    return searchCommandEntry;
  }
  
  private static String fetchPattern() {
      return getSearchCommandEntry().getCommand();
  }

  static private void searchEntryComplete(ActionEvent ev) {
    try {
      Hook.setJViBusy(true);
      String cmd = ev.getActionCommand();
      boolean acceptIncr = false;
      boolean cancel = false;
      
      Scheduler.stopCommandEntry();
      
      if(cmd.charAt(0) == '\n') {
        if(G.p_is.getBoolean()
           && didIncrSearch
           && ! "".equals(fetchPattern()))
          acceptIncr = true;
      } else
        cancel = true;
      
      if(G.p_is.getBoolean()) {
        stopIncrementalSearch(acceptIncr);
      }
      
      if(acceptIncr)
        GetChar.fakeGotc(K_X_INCR_SEARCH_DONE);
      else if(cancel)
        GetChar.fakeGotc(K_X_SEARCH_CANCEL);
      else
        GetChar.fakeGotc(K_X_SEARCH_FINISH);
    } finally {
      Hook.setJViBusy(false);
    }
  }
  
  /** Start the entry dialog and stash the interesting info for later use
   *  int doSearch(). */
  static void inputSearchPattern(CMDARG cap, int count, int flags) {
    String mode = "";
    int cmdchar = cap.cmdchar;
    if (cmdchar == '/') {
      mode = "/";
      lastDir = FORWARD;
    } else if (cmdchar == '?') {
      mode = "?";
      lastDir = BACKWARD;
    }
    searchCount = count;
    searchFlags = flags;
    
    ViCmdEntry ce = getSearchCommandEntry();
    if(G.p_is.getBoolean())
        startIncrementalSearch();
    Scheduler.startCommandEntry(ce, mode, G.curwin, null);
  }

  static int getIncrSearchResultCode() {
      return incrSearchSucceed ? OK : FAIL;
  }

  // This is used to grab the pattern after search complete, for redoBuffer.
  static String last_search_pat() {
    return lastPattern;
  }
  
  /** doSearch() should only be called after inputSearchPattern() */
  static int doSearch() {
    String pattern = fetchPattern();
    G.curwin.w_set_curswant = true;
    if(pattern.equals("")) {
      if(lastPattern == null) {
        Msg.emsg(Messages.e_noprevre);
        return 0;
      }
      pattern = lastPattern;
    }
    lastPattern = pattern;
    // executeSearch(pattern, lastDir, G.p_ic.getBoolean());
    ViFPOS pos = G.curwin.w_cursor.copy();
    int rc = searchit(null, pos, lastDir, pattern,
                      searchCount, searchFlags, 0, G.p_ic.getBoolean());
    if(rc != FAIL) {
      Msg.smsg((lastDir == FORWARD ? "/" : "?") + pattern);
    }
    
    if(rc == FAIL) {
      return 0;
    } else {
      return 1; // NEEDSWORK: not returning 2 ever, so not line mode.
    }
    
    /* ***************************
    if(rc == FAIL) {
      Normal.clearopInstance();
    }
    ******************************/
  }
  
  private static void laterDoIncrementalSearch() {
    EventQueue.invokeLater(new Runnable() {
      public void run()
      {
        doIncrementalSearch();
      }
    });
  }

  private static class SearchListener implements ChangeListener
  {
    @Override
    public void stateChanged(ChangeEvent e)
    {
      laterDoIncrementalSearch();
    }
  }

  private static SearchListener isListener;

  private static void startIncrementalSearch() {
      // funny aborts might leave one...
      // NEEDSWORK: Might want to use a weak listen as well
      getSearchCommandEntry().removeChangeListener(isListener);
      if(isListener == null)
        isListener = new SearchListener();
      searchPos = G.curwin.w_cursor.copy();
      searchTopLine = G.curwin.getVpTopDocumentLine();
      setPCMarkAfterIncrSearch = (searchFlags & SEARCH_MARK) != 0;
      searchFlags &= ~SEARCH_MARK;
      didIncrSearch = false;
      incrSearchSucceed = false;
      getSearchCommandEntry().addChangeListener(isListener);
  }
  
  private static void stopIncrementalSearch(boolean accept) {
      getSearchCommandEntry().removeChangeListener(isListener);

      G.curwin.clearSelection(); // since it is used by incr search
      
      if(accept) {
          lastPattern = fetchPattern();
      } else
        resetViewIncrementalSearch();

      // NEEDSWORK: setpcmark if accept == false ????????
      if(setPCMarkAfterIncrSearch && incrSearchSucceed) {
        MarkOps.setpcmark(searchPos);
      }
  }
  
  private static void resetViewIncrementalSearch() {
    G.curwin.setVpTopLine(searchTopLine);
    G.curwin.setCaretPosition(searchPos.getOffset());
  }
  
  private static void doIncrementalSearch() {
    try {
      Hook.setJViBusy(true);
      String pattern = getSearchCommandEntry().getCurrentEntry();
      
      if("".equals(pattern)) {
        resetViewIncrementalSearch();
        return;
      }
      ViFPOS pos = searchPos.copy();
      incrSearchSucceed = false;
      int rc = searchit(null, pos, lastDir, pattern,
                        searchCount, searchFlags /*& ~SEARCH_MSG*/,
                        0, G.p_ic.getBoolean());
      // for incr search, use java selection to show progress
      int new_pos = G.curwin.w_cursor.getOffset();
      G.curwin.setSelection(new_pos, new_pos + search_match_len);

      didIncrSearch = true;
      if(rc == FAIL) {
        resetViewIncrementalSearch();
        searchitErrorMessage(null);
      } else
        incrSearchSucceed = true;
    } catch(Exception ex) {
        LOG.log(Level.SEVERE, null, ex);
    } finally {
      Normal.v_updateVisualState();
      Hook.setJViBusy(false);
    }
  }

  static int doNext(CMDARG cap, int count, int flag) {
    G.curwin.w_set_curswant = true;
    int dir = ((flag & SEARCH_REV) != 0 ? - lastDir : lastDir);
    //G.curwin.repeatSearch(dir);
    int rc = FAIL;
    if(lastPattern == null) {
      Msg.emsg(Messages.e_noprevre);
    } else {
      Msg.smsg((dir == FORWARD ? "/" : "?") + lastPattern);
      // executeSearch(lastPattern, dir, G.p_ic.getBoolean());
      ViFPOS pos = G.curwin.w_cursor.copy();
      rc = searchit(null, pos, dir, lastPattern,
                        count, flag, 0, G.p_ic.getBoolean());
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

    ViFPOS fpos = G.curwin.w_cursor;
    MySegment seg = G.curbuf.getLineSegment(fpos.getLine());
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
		  col + G.curbuf
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
  private static boolean funnyCharsAsWord;

  /**
   * cls() - returns the class of character at curwin->w_cursor
   *<br>
   * The 'type' of the current search modifies the classes of characters if a
   * 'W', 'B', or 'E' motion is being done. In this case, chars. from class 2
   * are reported as class 1 since only white space boundaries are of interest.
   */
  private static int cls() {
    return cls(G.curwin.w_cursor);
  }
  private static int cls(ViFPOS fpos) {
    char    c;

    c = gchar_pos(fpos);
    if (c == ' ' || c == '\t' || c == '\n')
      return 0;

    if (Misc.vim_iswordc(c))
      return 1;

    //
    // If stype is non-zero, report these as class 1.
    //
    return ( ! funnyCharsAsWord) ? 2 : 1;
  }


  /**
   * fwd_word(count, type, eol) - move forward one word
   *<br>
   * Returns FAIL if the cursor was already at the end of the file.
   * If eol is TRUE, last word stops at end of line (for operators).
   */
  static int fwd_word(int count, boolean type, boolean eol) {
    ViFPOS fpos = G.curwin.w_cursor.copy();
    int rc = fwd_word(count, type, eol, fpos);
    G.curwin.setCaretPosition(fpos.getOffset());
    return rc;
  }
  static int fwd_word(int count, boolean type, boolean eol, ViFPOS fpos) {
    int		sclass;	    /* starting class */
    int		i;
    boolean	last_line;

    funnyCharsAsWord = type;
    while (--count >= 0) {
      sclass = cls(fpos);

      //
      // We always move at least one character, unless on the last character
      // in the buffer.
      //
      last_line = (fpos.getLine() == G.curbuf.getLineCount());
      i = inc(fpos);
      if (i == -1 || (i == 1 && last_line)) // started at last char in file
	return FAIL;
      if (i == 1 && eol && count == 0)      // started at last char in line
	return OK;

      //
      // Go one char past end of current word (if any)
      //
      if (sclass != 0)
	while (cls(fpos) == sclass) {
	  i = inc(fpos);
	  if (i == -1 || (i == 1 && eol && count == 0))
	    return OK;
	}

      //
      // go to next non-white
      //
      while (cls(fpos) == 0) {
	//
	// We'll stop if we land on a blank line
	//
	if(fpos.getColumn() == 0
	   	&& gchar_pos(fpos) == '\n') {
	  break;
	}

	i = inc(fpos);
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
    ViFPOS fpos = G.curwin.w_cursor.copy();
    int rc = bck_word(count, type, stop, fpos);
    G.curwin.setCaretPosition(fpos.getOffset());
    return rc;
  }
  static int bck_word(int count, boolean type, boolean stop, ViFPOS fpos) {
    int		sclass;	    /* starting class */

    funnyCharsAsWord = type;
    while (--count >= 0) {
      sclass = cls(fpos);
      if (dec(fpos) == -1)     /* started at start of file */
	return FAIL;

finished_block:
      do {
	if (!stop || sclass == cls(fpos) || sclass == 0) {
	  //
	  // Skip white space before the word.
	  // Stop on an empty line.
	  //
	  while (cls(fpos) == 0) {
	    if (fpos.getColumn() == 0 && Util.lineempty(fpos.getLine()))
	      break finished_block;
	    if (dec(fpos) == -1)   // hit start of file, stop here
	      return OK;
	  }

	  //
	  // Move backward to start of this word.
	  //
	  if (skip_chars(cls(fpos), BACKWARD, fpos))
	    return OK;
	}

	inc(fpos);		 // overshot - forward one
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
    ViFPOS fpos = G.curwin.w_cursor.copy();
    int rc = end_word(count, type, stop, empty, fpos);
    G.curwin.setCaretPosition(fpos.getOffset());
    return rc;
  }
  static int end_word(
          int count, boolean type, boolean stop, boolean empty, ViFPOS fpos)
  {
    int		sclass;	    /* starting class */

    funnyCharsAsWord = type;
    while (--count >= 0) {
      sclass = cls(fpos);
      if (inc(fpos) == -1)
	return FAIL;

finished_block:
      do {
	//
	// If we're in the middle of a word, we just have to move to the end
	// of it.
	//
	if (cls(fpos) == sclass && sclass != 0) {
	  //
	  // Move forward to end of the current word
	  //
	  if (skip_chars(sclass, FORWARD, fpos))
	    return FAIL;
	} else if (!stop || sclass == 0) {
	  //
	  // We were at the end of a word. Go to the end of the next word.
	  // First skip white space, if 'empty' is TRUE, stop at empty line.
	  //
	  while (cls(fpos) == 0) {
	    if (empty && fpos.getColumn() == 0
		&& Util.lineempty(fpos.getLine()))
	      break finished_block;
	    if (inc(fpos) == -1)    // hit end of file, stop here
	      return FAIL;
	  }

	  //
	  // Move forward to the end of this word.
	  //
	  if (skip_chars(cls(fpos), FORWARD, fpos))
	    return FAIL;
	}
	dec(fpos);	// overshot - one char backward
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
    ViFPOS fpos = G.curwin.w_cursor.copy();
    int rc = bckend_word(count, type, eol, fpos);
    G.curwin.setCaretPosition(fpos.getOffset());
    return rc;
  }
  static int bckend_word(int count, boolean type, boolean eol, ViFPOS fpos) {
    int		sclass;	    // starting class
    int		i;

    funnyCharsAsWord = type;
    while (--count >= 0) {
      sclass = cls(fpos);
      if ((i = dec(fpos)) == -1)
	return FAIL;
      if (eol && i == 1)
	return OK;

      //
      // Move backward to before the start of this word.
      //
      if (sclass != 0) {
	while (cls(fpos) == sclass)
	  if ((i = dec(fpos)) == -1 || (eol && i == 1))
	    return OK;
      }

      //
      // Move backward to end of the previous word
      //
      while (cls(fpos) == 0) {
	if (fpos.getColumn() == 0 && Util.lineempty(fpos.getLine()))
	  break;
	if ((i = dec(fpos)) == -1 || (eol && i == 1))
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
    ViFPOS fpos = G.curwin.w_cursor.copy();
    boolean rc = skip_chars(cclass, dir, fpos);
    G.curwin.setCaretPosition(fpos.getOffset());
    return rc;
  }
  private static boolean skip_chars(int cclass, int dir, ViFPOS fpos) {
    while (cls(fpos) == cclass)
      if ((dir == FORWARD ? inc(fpos) : dec(fpos)) == -1)
	return true;
    return false;
  }

  /////////////////////////////////////////////////////////////////////
  //
  // regualr expression handling stuff
  //

  static public RegExp getLastRegExp() {
    if(lastPattern == null)
      return null;
    return getRegExp(lastPattern, G.p_ic.getBoolean());
  }

  //
  // NEEDSWORK: need to build a structure out of a pattern that
  // tracks things like lastRegExp* and forceCase
  //
  // simple cache. Helps "n" and "N" commands
  static RegExp lastRegExp;
  static String lastRegExpPattern = "";
  static boolean lastRegExpIC;

  static int forceCase_CleanupPatternHack; // for \c and \C
  static final int FORCE_CASE_IGNORE = 1;
  static final int FORCE_CASE_EXACT = -1;
  static final int FORCE_CASE_NONE = 0;

  /** Get a compiled regular expression. Clean up the escaping as needed. */
  static RegExp getRegExp(String pattern, boolean ignoreCase) {
    String cleanPattern = cleanupPattern(pattern);

    // If the pattern has an 'ignoreCase' flag built in,
    // then apply the override
    if(forceCase_CleanupPatternHack == FORCE_CASE_EXACT) {
      ignoreCase = false;
    } else if(forceCase_CleanupPatternHack == FORCE_CASE_IGNORE) {
      ignoreCase = true;
    } // else FORCE_CASE_NONE

    // can the last re be reused?
    // NEEDSWORK: getRegExp: cache re's, LRU?
    if(cleanPattern.equals(lastRegExpPattern) && lastRegExpIC == ignoreCase) {
      return lastRegExp;
    }
    RegExp re = null;
    try {
      int flags = ignoreCase ? RegExp.IGNORE_CASE : 0;
      re = RegExpFactory.create();
      // NEEDSWORK: compilePattern vs cleanPattern
      String compilePattern = re.patternType() == RegExp.PATTERN_SIMPLE
                                ? pattern : cleanPattern;
      re.compile(cleanPattern, flags);
      // cache the information
      lastRegExpPattern = cleanPattern;
      lastRegExpIC = ignoreCase;
      lastRegExp = re;
    } catch(RegExpPatternError ex) {
      Msg.emsg(ex.getMessage() + " [" + ex.getIndex() + "]" + pattern);
      //Msg.emsg("Invalid search string: \"" + pattern + "\" " + ex.getMessage());
      re = null;
    }
    return re;
  }

  private static final int ESCAPED_FLAG = 0x10000;
  /**
   * Change metacharacter escaping of input pattern to match
   * the perl5 requirements. Do this because non-standard metachars
   * may be be escaped in patterns according to metaEscape option.
   * <p> (This should be an html table:)
   * <br/>   'c' is escaped      |   'c' is not escaped
   * <br/>        c  --> \c      |         c  --> c
   * <br/>        \c --> c       |         \c --> \c
   * </p><p> Previously, vim did not allow ? to indicate optional atom, so
   * there was some very messy code. Now, since '?' is accepted by vim,
   * just need to deal with converting '=' to '?'. Still messy, but not
   * quite so bad.
   * <br/>   '=' is escaped      |   '=' is not escaped
   * <br/>        c  --> \c      |         c  --> c
   * <br/>        \c --> c       |         \c --> \c
   *
   * </p><p>
   * And finally, either \&lt; or \&gt; are replaced by \b. If there wasn't
   * this last rule, then could just return if p_meta_escape was empty and
   * p_meta_equals was false.
   * </p>
   */
  static String cleanupPattern(String s) {
    forceCase_CleanupPatternHack = FORCE_CASE_NONE;
    String metacharacterEscapes = G.p_meta_escape.getString();
    StringBuilder sb = new StringBuilder();
    boolean isEscaped = false;
    for(int in = 0; in < s.length(); in++) {
      char c = s.charAt(in);
      if( ! isEscaped && c == '\\') {
        isEscaped = true;
        continue;
      }
      
      if((c == '=') && G.p_meta_equals.getBoolean()) {
        // Have an '=' and that char is used to specify an optional atom.
        // Set useEscape if the '=' needs to be escaped to mean optional.
        boolean useEscape = metacharacterEscapes.indexOf("?") >= 0;
        if(isEscaped && useEscape
           || !isEscaped && !useEscape) {
          // the '=' is intened to indicated an optional atom,
          // convert it to a '?'
          c = '?';
        }
      }
      if(metacharacterEscapes.indexOf(c) >= 0) { // metachar gets escaped
        // reverse of what was seen
        if( ! isEscaped) {
          sb.append("\\");
        }
        sb.append(c);
      } else if(isEscaped && (c == '<' || c == '>')) {
        sb.append("\\b");
      } else if(isEscaped && c == 'c') {
        forceCase_CleanupPatternHack = FORCE_CASE_IGNORE;
      } else if(isEscaped && c == 'C') {
        forceCase_CleanupPatternHack = FORCE_CASE_EXACT;
      } else {
        // pass through what was seen
        if(isEscaped) {
          sb.append("\\");
        }
        sb.append(c);
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
   * <p>
   * Return FAIL (zero) for failure, non-zero for success.
   * When FEAT_EVAL is defined, returns the index of the first matching
   * subpattern plus one; one if there was none.
   * </p>
   * @return FAIL (zero) for failure. 1 no subpattern else subpattern + 1
   */
   //
   // Somehow anchoring at the beginning of the line seems to work fine.
   // Not sure how. The vim code has a "at start of line" flag it passes
   // to the reg exp matcher.
   //
  static int searchit(TextView win,      // BUF,    NOT USED
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
    int submatch = 0;
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
        for ( ; lnum > 0 && lnum <= G.curbuf.getLineCount();
              lnum += dir, at_first_line = false)
        {
          //
          // Look for a match somewhere in the line.
          //
          //////////ptr = ml_get_buf(buf, lnum, FALSE);
          MySegment seg = G.curbuf.getLineSegment(lnum);
                                                // NEEDSWORK: AT_BOL == TRUE
          //System.err.println("line: " + lnum);
          if(prog.search(seg.array, seg.offset, seg.count)) {
            match = prog.start(0) - seg.offset; // column index
            matchend = prog.stop(0) - seg.offset;
            submatch = first_submatch(prog);
            int eolColumn = G.curbuf.getLineEndOffset(lnum) -
                             G.curbuf.getLineStartOffset(lnum) - 1;
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
                  submatch = first_submatch(prog);
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
                int colIdx = prog.start(0) - seg.offset;
                int matchend01 = prog.stop(0) - seg.offset;
                if (!at_first_line
                    || (((options & SEARCH_END) != 0)
                        ? (matchend01 - 1 + extra_col
                                               <= start_pos.getColumn())
                        : (colIdx + extra_col <= start_pos.getColumn())))
                {
                  match_ok = true;
                  match = colIdx;
                  matchend = matchend01;
                  submatch = first_submatch(prog);
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
            pos.set(lnum, tcol);
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
          lnum = G.curbuf.getLineCount();
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
        searchitErrorMessage(Messages.e_interr);
      else if ((options & SEARCH_MSG) == SEARCH_MSG) {
        if (G.p_ws.getBoolean())
          searchitErrorMessage(Messages.e_patnotf2 + pattern);
        else if (lnum == 0)
          searchitErrorMessage("search hit TOP without match for: " + pattern);
        else
          searchitErrorMessage("search hit BOTTOM without match for: " + pattern);
      }
      search_match_len = 0;
      return FAIL;
    }
    search_match_len = matchend - match;

    if((options & SEARCH_MARK) != 0) {
      MarkOps.setpcmark();
    }
    gotoLine(G.curbuf.getLineNumber(pos.getOffset()), 0, true);
    int new_pos = pos.getOffset();
    if(search_match_len == 0) {
        // search /$ puts cursor on end of line
        new_pos = G.curwin.validateCursorPosition(new_pos);
    }
    G.curwin.w_cursor.set(new_pos);
    G.curwin.w_set_curswant = true;
    if(wmsg != null) {
      Msg.wmsg(wmsg/*, true*/);
    }
    return submatch + 1;
  }
  private static String lastSearchitErrorMessage;
  /** null means reprint the last one */
  private static void searchitErrorMessage(String s)
  {
    if(s != null) {
      lastSearchitErrorMessage = s;
    }
    if(lastSearchitErrorMessage != null)
      Msg.emsg(lastSearchitErrorMessage);
  }

/**
 * Return the number of the first subpat that matched.
 */
    static int
first_submatch(RegExp rp)
{
    int		submatch;
    int         n = rp.nGroup();

    for (submatch = 1; ; ++submatch)
    {
        if(submatch > n || submatch > 9)
        {
	    submatch = 0;
	    break;
	}
	if (rp.start(submatch) >= 0)
	    break;
    }
    return submatch;
}

  ////////////////////////////////////////////////////////////////
  //
  // Stuff from ex_cmds.c
  //
  
  private static int nSubMatch;
  private static int nSubChanges;
  private static int nSubLine = 0;
  
  private static String lastSubstituteArg;

  private static final int SUBST_ALL      = 0x0001;
  private static final int SUBST_PRINT    = 0x0002;
  private static final int SUBST_CONFIRM  = 0x0004;
  private static final int SUBST_ESCAPE   = 0x0008;
  private static final int SUBST_QUIT     = 0x0010;
  private static final int SUBST_DID_ACK  = 0x0020;

  private static MutableInt substFlags;

  /**
   * Substitute command
   * @param cev cev's first arg is /pattern/substitution/{flags}
   */
  static void substitute(ColonCommands.ColonEvent cev) {
    // The substitute command doesn't parse arguments,
    // so it has 0 or 1 argument.
    String cmd;
    if(cev.getNArg() == 0) {
      cmd = lastSubstituteArg;
      if(cmd == null) {
	Msg.emsg("No previous substitute argument");
	return;
      }
    } else {
      cmd = cev.getArg(1);
      lastSubstituteArg = cmd;
    }
    String pattern = null;
    RegExp prog = null;
    CharSequence substitution;
    char delimiter = cmd.charAt(0);
    MySegment line;
    int cursorLine = 0; // set to line number of last change
    int sidx = 1; // after delimiter

    boolean newFlags = false;
    if(!G.global_busy || substFlags == null) {
      substFlags = new MutableInt();
      newFlags = true;
    }
    
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
    Options.newSearch();
    
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
    substitution = lastSubstitution;

    if(newFlags) {
      //
      // pick up the flags
      //
                // NEEDSWORK: || lastSubstitution.indexOf('~', sidx01) != -1;
      if(lastSubstitution.indexOf('\\') != -1
                  || lastSubstitution.indexOf('&') != -1)
        substFlags.setBits(SUBST_ESCAPE);
    
      
      ++sidx; // move past the delimiter
      for( ; sidx < cmd.length(); sidx++) {
        char c = cmd.charAt(sidx);
        switch(c) {
          case 'g':
            substFlags.setBits(SUBST_ALL);
            break;
          
          case 'p':
            substFlags.setBits(SUBST_PRINT);
            break;
          
          case 'c':
            substFlags.setBits(SUBST_CONFIRM);
            break;
          
          case ' ':
            // silently ignore blanks
            break;
            
          default:
            Msg.emsg("ignoring flag: '" + c + "'");
            break;
        }
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
    if(! G.global_busy) {
      nSubLine = 0;
      nSubMatch = 0;
      nSubChanges = 0;
    }
    for(int i = line1;
          i <= line2 && !substFlags.testAnyBits(SUBST_QUIT);
          i++) {
      int nChange = substitute_line(prog, i, substFlags, substitution);
      if(nChange > 0) {
        nSubChanges += nChange;
        cursorLine = i;  // keep track of last line changed
        nSubLine++;
        if(substFlags.testAnyBits(SUBST_PRINT)) {
          ColonCommands.outputPrint(i, 0, 0);
        }
      }
    }
    
    if(! G.global_busy) {
      if(cursorLine > 0) {
	gotoLine(cursorLine, BL_WHITE | BL_FIX, true);
      }
      if(nSubMatch == 0) {
	Msg.emsg(Messages.e_patnotf2 + pattern);
      } else {
	do_sub_msg();
      }
    }
    
  }

  /**
   * Give message for number of substitutions.
   * Can also be used after a ":global" command.
   * Return TRUE if a message was given.
   */
  private static boolean do_sub_msg() {
    if(nSubChanges >= G.p_report.getInteger()) {
      String msg = "" + nSubChanges + " substitution" + Misc.plural(nSubChanges)
		   + " on " + nSubLine + " line" + Misc.plural(nSubLine);
      G.curwin.getStatusDisplay().displayStatusMessage(msg);
      return true;
    }
    return false;
  }

  private static char modalResponse;
  /**
   * This method preforms the substitution within one line of text.
   * This is not adapted vim code.
   * <br><b>NEEDSWORK:</b><ul>
   * <li>Handle more flags, not just doAll.
   * </ul>
   * @param prog the compiled regular expression
   * @param line text to check for match and substitue
   * @param doAll if true, substitute all occurences within the line
   * @param subs the substitution string
   * @param hasEscape if true, then escape processing is needed on <i>subs</i>.
   * @return number of changes on the line
   */
  static int substitute_line(RegExp prog,
                                      int lnum,
                                      MutableInt flags,
                                      CharSequence subs)
  {
    MySegment seg = G.curbuf.getLineSegment(lnum);

    StringBuffer sb = null;
    int lookColumnOffset = 0;
    int lastMatchColumn = -1;
    int countChanges = 0;

    while(prog.search(seg.array,
                      seg.offset + lookColumnOffset,
                      seg.count - lookColumnOffset)) {
      int matchOffsetColumn = prog.start(0) - seg.offset;
      if(lastMatchColumn == matchOffsetColumn) {
        // prevent infinite loops, can happen with match of zero characters
        ++lookColumnOffset;
        // The following statement is true if the lookColumn is on or after
        // the newline. Note that there has already been a successful match.
        // So get out of the loop when looking at newline and have had a match
        if(lookColumnOffset >= seg.count - 1)
          break;
        continue;
      }
      lastMatchColumn = matchOffsetColumn;

      nSubMatch++;
      int segOffsetToDoc = G.curbuf.getLineStartOffset(lnum) - seg.offset;

      modalResponse = 0;
      if(flags.testAnyBits(SUBST_CONFIRM)
              && ! flags.testAnyBits(SUBST_DID_ACK)) {
        G.curwin.setSelection(segOffsetToDoc + prog.start(0),
                           segOffsetToDoc + prog.stop(0)
                            + (prog.length(0) == 0 ? 1 : 0));

        Msg.wmsg("replace with '" + subs + "' (y/n/a/q/l)");
        ViManager.getFactory().startModalKeyCatch(new KeyAdapter() {
                    @Override
          public void keyPressed(KeyEvent e) {
            e.consume();
            char c = e.getKeyChar();
            switch(c) {
              case 'y': case 'n': case 'a': case 'q': case 'l':
                modalResponse = c;
                break;
              case KeyEvent.VK_ESCAPE:
                modalResponse = 'q';
                break;
              default:
                Util.vim_beep();
                break;
            }
            if(modalResponse != 0) {
              ViManager.getFactory().stopModalKeyCatch();
            }
          }
        });
        Msg.clearMsg();
      }

      // advance past matched characters
      lookColumnOffset = prog.stop(0) - seg.offset;
      if(modalResponse != 'n' && modalResponse != 'q') {
        if(sb == null) { // match and do substitute, make sure there's a buffer
          sb = new StringBuffer();
        }
        CharSequence changedData = flags.testAnyBits(SUBST_ESCAPE)
                                ? translateSubstitution(prog, seg, sb, subs)
                                : subs;

        int sizeDelta = changedData.length() - prog.length(0);
        // the column may shifted, adjust by size diff of substitution
        lookColumnOffset += sizeDelta;
        if(prog.length(0) == 0)
          lastMatchColumn += sizeDelta;

        // apply the change to the document
        countChanges++;
        G.curwin.replaceString(segOffsetToDoc + prog.start(0),
                               segOffsetToDoc + prog.stop(0),
                               changedData.toString());

        // the line has changed, fetch changed line
        seg = G.curbuf.getLineSegment(lnum);
      }

      if(modalResponse == 'q' || modalResponse == 'l') {
        flags.setBits(SUBST_QUIT);
        break;
      } else if(modalResponse == 'a') {
        flags.setBits(SUBST_DID_ACK);
      }

      if( ! flags.testAnyBits(SUBST_ALL)) {
        // only do one substitute per line
        break;
      }
    }
    return countChanges;
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
  static CharSequence translateSubstitution(RegExp prog,
                                   MySegment line,
                                   StringBuffer sb,
                                   CharSequence subs)
  {
    int i = 0;
    char c;
    sb.setLength(0);

    for( ; i < subs.length(); i++) {
      c = subs.charAt(i);
      switch(c) {
        case '&':
          // copy chars that matched
          sb.append(line.array, prog.start(0), prog.length(0));
          break;

        case '\\':
          if(i+1 < subs.length()) {
            i++;
            c = subs.charAt(i);
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
    return sb;
  }

  /**
   * global command. first arg is /pattern/some_command_goes_here
   * <br>
   * Only do print for now.
   */

  static void global(ColonCommands.ColonEvent cev) {
    G.global_busy = true;
    try {
      doGlobal(cev);
    }
    finally {
      G.global_busy = false;
    }
  }
  
  static void doGlobal(ColonCommands.ColonEvent cev) {
    if(cev.getNArg() != 1) {
      Msg.emsg("global takes an argument (FOR NOW)");
      return;
    }
    int old_lcount = G.curbuf.getLineCount();
    nSubLine = 0;
    nSubMatch = 0;
    nSubChanges = 0;
    String cmd = cev.getArg(1);
    String cmdExec;
    String pattern = null;
    RegExp prog = null;
    char delimiter = cmd.charAt(0);
    MySegment line;
    int cursorLine = 0; // set to line number of last found line
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
    if(lastPattern == null) {
      Msg.emsg(Messages.e_noprevre);
      return;
    }

    //
    // pick up the command
    //

    sidx++; // first char of command
    if(sidx < cmd.length()) {
      cmdExec = cmd.substring(sidx);
    } else {
      cmdExec = "";
    }

    //
    // compile regex
    //

    prog = getRegExp(pattern, G.p_ic.getBoolean());
    if(prog == null) {
      return;
    }

    // figure out what command to execute for all the indicated lines
    
    ActionListener cmdAction = null;
    ColonCommands.ColonEvent cevAction = null;

    if(cmdExec.equals("")) {
      // if no command specified then "p" command
      cmdAction = Cc01.getActionPrint();
    } else {
      cevAction = ColonCommands.parseCommand(cmdExec);
      if(cevAction != null) {
	cmdAction = cevAction.getAction();
      }
    }

    int nLine = G.curbuf.getLineCount();
      
    // for now special case a few known commands that can be global'd
    // NEEDSWORK: make global two pass, check vim sources. There's no nice
    // way to keep track of the matched lines for the seconde pass. The only
    // generalized thing that would seem to work is to catch the document
    // events for delete and remove any lines from the global list that are
    // deleted.
    
    ViOutputStream result = null;
    if(cmdAction == Cc01.getActionPrint()) {
      result = ViManager.createOutputStream(G.curwin,
                                            ViOutputStream.SEARCH, pattern);
    } else {
      // Assume it will be handled by the command
      //result = ViManager.createOutputStream(G.curwin,
      //                                      ViOutputStream.LINES, pattern);
    }
    
    substFlags = null;

    for(int lnum = 1; lnum <= nLine; lnum++) {
      line = G.curbuf.getLineSegment(lnum);
      if(prog.search(line.array, line.offset, line.count)) {
	// if full parse each time command executed,
	// then should move cursor (or equivilent) but.....
	if(cevAction != null) {
	  cevAction.line1 = lnum;
	  cevAction.line2 = lnum;
	}
	if(cmdAction == Cc01.getActionPrint()) {
	    result.println(lnum, prog.start(0) - line.offset, prog.length(0));
	} else if(cmdAction == Cc01.getActionSubstitute()) {
	  ColonCommands.executeCommand(cevAction);
          if(substFlags != null && substFlags.testAnyBits(SUBST_QUIT))
            break;
	} else if(cmdAction == Cc01.getActionDelete()) {
	  OPARG oa = ColonCommands.setupExop(cevAction, false);
	  oa.op_type = OP_DELETE;
	  Misc.op_delete(oa);
	  // The troublesome command/situation
	  // A line has just been deleted
	  --nLine;
	  --lnum;
	} else if(cmdAction == Cc01.getActionGlobal()) {
	  Msg.emsg("Cannot do :global recursively");
	  return;
	} else {
	  // no command specified, but cursorLine is getting set above
	}
	cursorLine = lnum;  // keep track of last line matched
      }
    }

    if(cursorLine > 0) {
      gotoLine(cursorLine, BL_WHITE | BL_FIX, true);
    }
    if(result != null)
      result.close();
    
    if( ! do_sub_msg()) {
      Misc.msgmore(G.curbuf.getLineCount() - old_lcount);
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

  static boolean findsent(int dir, int count) {
    ViFPOS pos, tpos;
    char c;
    int i;
    int startlnum;
    boolean noskip = false;
    boolean cpo_J;
    boolean found_dot;

    pos = G.curwin.w_cursor.copy();

    while (count-- > 0) {

found:
      do {
        if (Misc.gchar_pos(pos) == '\n') {
          do {
            if (Misc.inclDeclV7(pos, dir) == -1)
              break;
          } while (Misc.gchar_pos(pos) == '\n');

          if (dir == FORWARD)
            break found;
        }
        else if (dir == FORWARD && pos.getColumn() == 0 &&
          startPS(pos.getLine(), NUL, false)) {
          if (pos.getLine() == G.curbuf.getLineCount())
            return false;
          pos.set(pos.getLine() + 1, 0);
          break found;
        }
        else if (dir == BACKWARD)
          Misc.decl(pos);

        // go back to previous non-blank character
        found_dot = false;
        while ((c = Misc.gchar_pos(pos)) == ' ' || c == '\t' ||
          (dir == BACKWARD && Util.vim_strchr(".!?)]\"'", c) != null)) {
          if (Util.vim_strchr(".!?", c) != null) {
            // Only skip over a '.', '!' and '?' once.
            if (found_dot)
              break;
            found_dot = true;
          }
          if (Misc.decl(pos) == -1)
            break;
          // when going forward: Stop in front of empty line
          if (Util.lineempty(pos.getLine()) && dir == FORWARD) {
            Misc.inclV7(pos);
            break found;
          }
        }

        // remember the line where the search started
        startlnum = pos.getLine();
        cpo_J = G.p_cpo_j.getBoolean();

        for (;;) {
          c = Misc.gchar_pos(pos);
          if (c == '\n' ||
            (pos.getColumn() == 0 && startPS(pos.getLine(), NUL, false))) {
            if (dir == BACKWARD && pos.getLine() != startlnum)
              pos.set(pos.getLine() + 1, 0);
            break;
          }
          if (c == '.' || c == '!' || c == '?') {
            tpos = pos.copy();
            do
              if ((i = Misc.inc(tpos)) == -1)
                break;
            while (Util.vim_strchr(")]\"'", c = Misc.gchar_pos(tpos)) != null);

            if (i == -1 || (!cpo_J && (c == ' ' || c == '\t')) || c == '\n'
                || (cpo_J && (c == ' ' && Misc.inc(tpos) >= 0
                    && Misc.gchar_pos(tpos) == ' '))) {
              pos = tpos;
              if (Misc.gchar_pos(pos) == '\n') // skip '\n' at EOL
                Misc.inc(pos);
              break;
            }
          }
          if (Misc.inclDeclV7(pos, dir) == -1) {
            if (count > 0)
              return false;
            noskip = true;
            break;
          }
        }
      } while (false);

      while (!noskip && ((c = Misc.gchar_pos(pos)) == ' ' || c== '\t'))
        if (Misc.inclV7(pos) == -1) break;
    }
    MarkOps.setpcmark();
    G.curwin.setCaretPosition(pos.getOffset());
    return true;
  }

  static boolean findpar(CMDARG oap, int dir, int count, int what,
    boolean both) {

    int curr = G.curwin.w_cursor.getLine();

    while (count-- > 0) {
      boolean did_skip = false; //TRUE after separating lines have been skipped 
      boolean first = true;     // TRUE on first line 
      do {
        if (!Util.lineempty(curr))
          did_skip = true;

        if (!first && did_skip && startPS(curr, what, both))
          break;

        if ((curr += dir) < 1 || curr > G.curbuf.getLineCount()) {
          if (count > 0)
            return false;
          curr -= dir;
          break;
        }
        first = false;
      } while (true);
    }

    MarkOps.setpcmark();

    if (both && !Util.lineempty(curr) && Util.ml_get(curr).charAt(0) == '}')
      ++curr;

    int offset = 0;

    if (curr == G.curbuf.getLineCount()) {
      offset = Util.lineLength(curr);
      if (offset > 0) {
        offset--;
        oap.oap.inclusive = true;
      }
    }

    G.curwin.w_cursor.set(curr, offset);

    return true;
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

/*
 * "Other" Searches
 *
 *****************************************************
 * NOTE: The findmatch and related are taken from vim7
 *****************************************************
 */

/**
 * findmatch - find the matching paren or brace
 *
 * Improvement over vi: Braces inside quotes are ignored.
 */
static ViFPOS findmatch(OPARG oap, char initc)
{
    return findmatchlimit(oap, initc, 0, 0);
}

/*
 * Return TRUE if the character before "linep[col]" equals "ch".
 * Return FALSE if "col" is zero.
 * Update "*prevcol" to the column of the previous character, unless "prevcol"
 * is NULL.
 * Handles multibyte string correctly.
 */
static boolean check_prevcol(MySegment linep, int col, char ch,
                             MutableInt pPrevcol)
{
    --col;
//#ifdef FEAT_MBYTE...
    if (pPrevcol != null)
	pPrevcol.setValue(col);
    return (col >= 0 && linep.charAt(col) == ch) ? true : false;
}

/**
 * findmatchlimit -- find the matching paren or brace, if it exists within
 * maxtravel lines of here.  A maxtravel of 0 means search until falling off
 * the edge of the file.
 *
 * "initc" is the character to find a match for.  NUL means to find the
 * character at or after the cursor.
 *
 * flags: FM_BACKWARD	search backwards (when initc is '/', '*' or '#')
 *	  FM_FORWARD	search forwards (when initc is '/', '*' or '#')
 *	  FM_BLOCKSTOP	stop at start/end of block ({ or } in column 0)
 *	  FM_SKIPCOMM	skip comments (not implemented yet!)
 *
 * "oap" is only used to set oap->motion_type for a linewise motion, it be
 * NULL
 */

static ViFPOS findmatchlimit(OPARG oap, char initc, int flags, int maxtravel)
{
  return findmatchlimit(G.curwin.w_cursor.copy(), oap, initc, flags, maxtravel);
}
static ViFPOS findmatchlimit(ViFPOS cursor,
        OPARG oap, char initc, int flags, int maxtravel)
{
    int         pos_lnum;
    int         pos_col;
    char	findc = 0;		// matching brace
    char 	c;
    int		count = 0;		// cumulative number of braces
    boolean	backwards = false;	// init for gcc
    boolean	inquote = false;	// TRUE when inside quotes
    MySegment 	linep;			// pointer to current line
    int 	ptr;
    int		do_quotes;		// check for quotes in current line
    int		at_start;		// do_quotes value at start position
    int		hash_dir = 0;		// Direction searched for # things
    int		comment_dir = 0;	// Direction searched for comments
    //ViFPOS	match_pos;		// Where last slash-star was found
    int 	match_pos_lnum;
    int 	match_pos_col;
    int		start_in_quotes;	// start position is in quotes
    int		traveled = 0;		// how far we've searched so far
    boolean	ignore_cend = false;    // ignore comment end
    boolean	cpo_match;		// vi compatible matching
    boolean	cpo_bsl;		// don't recognize backslashes
    int		match_escaped = 0;	// search for escaped match
    int		dir;			// Direction to search
    int		comment_col = MAXCOL;   // start of / / comment
//#ifdef FEAT_LISP...

    assert initc != 0;

    // pos_lnum = G.curwin.w_cursor.getLine();
    // pos_col = G.curwin.w_cursor.getColumn();
    pos_lnum = cursor.getLine();
    pos_col = cursor.getColumn();
    linep = Util.ml_get(pos_lnum);

    cpo_match = (Util.vim_strchr(G.p_cpo, CPO_MATCH) != null);
    cpo_bsl = (Util.vim_strchr(G.p_cpo, CPO_MATCHBSL) != null);

    /* Direction to search when initc is '/', '*' or '#' */
    if ((flags & FM_BACKWARD) != 0)
	dir = BACKWARD;
    else if ((flags & FM_FORWARD) != 0)
	dir = FORWARD;
    else
	dir = 0;

    //
    // if initc given, look in the table for the matching character
    // '/' and '*' are special cases: look for start or end of comment.
    // When '/' is used, we ignore running backwards into an star-slash, for
    // "[*" command, we just want to find any comment.
    //
    if (initc == '/' || initc == '*')
    {
	comment_dir = dir;
	if (initc == '/')
	    ignore_cend = true;
	backwards = (dir == FORWARD) ? false : true;
	initc = NUL;
    }
    else if (initc != '#' && initc != NUL)
    {
	/* 'matchpairs' is "x:y,x:y" */
	//for (ptr = curbuf->b_p_mps; *ptr; ptr += 2)
        String s = G.curbuf.b_p_mps;
	for (ptr = 0; ptr < s.length(); ptr += 2)
	{
	    if (s.charAt(ptr) == initc)
	    {
		findc = initc;
		initc = s.charAt(ptr+2);
		backwards = true;
		break;
	    }
	    ptr += 2;
	    if (s.charAt(ptr) == initc)
	    {
		findc = initc;
		initc = s.charAt(ptr-2);
		backwards = false;
		break;
	    }
	}
	if (findc == 0)		// invalid initc!
	    return null;
    }
    /*
     * Either initc is '#', or no initc was given and we need to look under the
     * cursor.
     */
    else
    {
	if (initc == '#')
	{
	    hash_dir = dir;
	}
	else
	{
	    //
	    // initc was not given, must look for something to match under
	    // or near the cursor.
	    // Only check for special things when 'cpo' doesn't have '%'.
	    //
	    if (!cpo_match)
	    {
		// Are we before or at #if, #else etc.?
		ptr = skipwhite(linep, 0);
		if (ptr < linep.count -1
                        && linep.charAt(ptr) == '#' && pos_col <= ptr)
		{
		    ptr = skipwhite(linep, ptr + 1);
		    if (   strncmp(linep, ptr, "if", 2) == 0
			|| strncmp(linep, ptr, "endif", 5) == 0
			|| strncmp(linep, ptr, "el", 2) == 0)
			hash_dir = 1;
		}

		// Are we on a comment?
		else if (linep.charAt(pos_col) == '/')
		{
		    if (linep.charAt(pos_col + 1) == '*')
		    {
			comment_dir = FORWARD;
			backwards = false;
			pos_col++;
		    }
		    else if (pos_col > 0 && linep.charAt(pos_col - 1) == '*')
		    {
			comment_dir = BACKWARD;
			backwards = true;
			pos_col--;
		    }
		}
		else if (linep.charAt(pos_col) == '*')
		{
		    if (linep.charAt(pos_col + 1) == '/')
		    {
			comment_dir = BACKWARD;
			backwards = true;
		    }
		    else if (pos_col > 0 && linep.charAt(pos_col - 1) == '/')
		    {
			comment_dir = FORWARD;
			backwards = false;
		    }
		}
	    }

	    //
	    // If we are not on a comment or the # at the start of a line, then
	    // look for brace anywhere on this line after the cursor.
	    //
	    if (hash_dir == 0 && comment_dir == 0)
	    {
		//
		// Find the brace under or after the cursor.
		// If beyond the end of the line, use the last character in
		// the line.
		//
                if(pos_col >= linep.count - 1 && pos_col != 0)
                    --pos_col;
		for (;;)
		{
		    initc = linep.charAt(pos_col);
		    if (initc == NUL)
			break;

                    ptr = 0;
                    String s = G.curbuf.b_p_mps;
		    for (; ptr < s.length(); ptr += 2)
		    {
			if (s.charAt(ptr) == initc)
			{
			    findc = s.charAt(ptr+2);
			    backwards = false;
			    break;
			}
			ptr += 2;
			if (s.charAt(ptr) == initc)
			{
			    findc = s.charAt(ptr-2);
			    backwards = true;
			    break;
			}
		    }
		    if (findc != NUL)
			break;
//#ifdef FEAT_MBYTE...
                    ++pos_col;
		}
		if (findc == 0)
		{
		    // no brace in the line, maybe use "  #if" then
                    int i00 = skipwhite(linep, 0);
		    if (!cpo_match && linep.charAt(i00) == '#')
			hash_dir = 1;
		    else
			return null;
		}
		else if (!cpo_bsl)
		{
		    int col, bslcnt = 0;

		    // Set "match_escaped" if there are an odd number of
		    // backslashes.
                    col = pos_col;
                    while(true) {
                        boolean match = check_prevcol(linep, col, '\\', null);
                        if(!match)
                          break;
                        bslcnt++;
                    }
		    match_escaped = (bslcnt & 1);
		}
	    }
	}
	if (hash_dir != 0)
	{
	    //
	    // Look for matching #if, #else, #elif, or #endif
	    //
	    if (oap != null)
		oap.motion_type = MLINE;   // Linewise for this case only
	    if (initc != '#')
	    {
		ptr = skipwhite(linep, skipwhite(linep, 0) + 1);
		if (strncmp(linep, ptr, "if", 2) == 0
                        || strncmp(linep, ptr, "el", 2) == 0)
		    hash_dir = 1;
		else if (strncmp(linep, ptr, "endif", 5) == 0)
		    hash_dir = -1;
		else
		    return null;
	    }
	    pos_col = 0;
	    while(true) // while (!got_int)
	    {
		if (hash_dir > 0)
		{
		    if (pos_lnum == G.curbuf.getLineCount())
			break;
		}
		else if (pos_lnum == 1)
		    break;
		pos_lnum += hash_dir;
		linep = ml_get(pos_lnum);
		//line_breakcheck();	/* check for CTRL-C typed */
		ptr = skipwhite(linep, 0);
		if (linep.charAt(ptr) != '#')
		    continue;
		pos_col = ptr;
		ptr = skipwhite(linep, ptr + 1);
		if (hash_dir > 0)
		{
		    if (strncmp(linep, ptr, "if", 2) == 0)
			count++;
		    else if (strncmp(linep, ptr, "el", 2) == 0)
		    {
			if (count == 0)
                            return new FPOS(pos_lnum, pos_col);
		    }
		    else if (strncmp(linep, ptr, "endif", 5) == 0)
		    {
			if (count == 0)
                            return new FPOS(pos_lnum, pos_col);
			count--;
		    }
		}
		else
		{
		    if (strncmp(linep, ptr, "if", 2) == 0)
		    {
			if (count == 0)
                            return new FPOS(pos_lnum, pos_col);
			count--;
		    }
		    else if (initc == '#' && strncmp(linep, ptr, "el", 2) == 0)
		    {
			if (count == 0)
                            return new FPOS(pos_lnum, pos_col);
		    }
		    else if (strncmp(linep, ptr, "endif", 5) == 0)
			count++;
		}
	    }
	    return null;
	}
    }

//#ifdef FEAT_RIGHTLEFT
//    /* This is just guessing: when 'rightleft' is set, search for a maching
//     * paren/brace in the other direction. */
//    if (curwin->w_p_rl && vim_strchr((char_u *)"()[]{}<>", initc) != NULL)
//	backwards = !backwards;
//#endif
//
    do_quotes = -1;
    start_in_quotes = MAYBE;
    match_pos_lnum = 0; //clearpos(&match_pos); ????????????????
    match_pos_col = 0;  //clearpos(&match_pos); ????????????????

    // backward search: Check if this line contains a single-line comment
    if ((backwards && comment_dir != 0)
//#ifdef FEAT_LISP...
	    )
	comment_col = check_linecomment(linep);
//#ifdef FEAT_LISP...
    while(true) // while (!got_int)
    {
	//
	// Go to the next position, forward or backward. We could use
	// inc() and dec() here, but that is much slower
	//
	if (backwards)
	{
//#ifdef FEAT_LISP...
	    if (pos_col == 0)		// at start of line, go to prev. one
	    {
		if (pos_lnum == 1)	// start of file
		    break;
		--pos_lnum;

		if (maxtravel > 0 && ++traveled > maxtravel)
		    break;

		linep = Util.ml_get(pos_lnum);
		pos_col = linep.length() - 1; // pos.col on trailing '\n'
		do_quotes = -1;
		//line_breakcheck();

		// Check if this line contains a single-line comment
		if (comment_dir != 0
//#ifdef FEAT_LISP...
			)
		    comment_col = check_linecomment(linep);
//#ifdef FEAT_LISP...
	    }
	    else
	    {
		--pos_col;
//#ifdef FEAT_MBYTE...
	    }
	}
	else				// forward search
	{
	    if (pos_col + 1 >= linep.length()
		    // at end of line, go to next one
//#ifdef FEAT_LISP...
		    )
	    {
		if (pos_lnum == G.curbuf.getLineCount()  // end of file
//#ifdef FEAT_LISP...
			 )
		    break;
		++pos_lnum;

		if (maxtravel != 0 && traveled++ > maxtravel)
		    break;

		linep = Util.ml_get(pos_lnum);
		pos_col = 0;
		do_quotes = -1;
		//line_breakcheck();
//#ifdef FEAT_LISP...
	    }
	    else
	    {
//#ifdef FEAT_MBYTE...
		    ++pos_col;
	    }
	}

	//
	// If FM_BLOCKSTOP given, stop at a '{' or '}' in column 0.
	//
	if (pos_col == 0 && (flags & FM_BLOCKSTOP) != 0
                 && ((c = linep.charAt(0)) == '{' || c == '}'))
	{
	    if (c == findc && count == 0) {	// match!
                return new FPOS(pos_lnum, pos_col);
            }
	    break;					/* out of scope */
	}

	if (comment_dir != 0)
	{
	    /* Note: comments do not nest, and we ignore quotes in them */
	    /* TODO: ignore comment brackets inside strings */
	    if (comment_dir == FORWARD)
	    {
		if (linep.charAt(pos_col) == '*'
                        && linep.charAt(pos_col + 1) == '/')
		{
		    pos_col++;
                    return new FPOS(pos_lnum, pos_col);
		}
	    }
	    else    /* Searching backwards */
	    {
		/*
		 * A comment may contain / * or / /, it may also start or end
		 * with / * /.	Ignore a / * after / /.
		 */
		if (pos_col == 0)
		    continue;
		else if (  linep.charAt(pos_col - 1) == '/'
			&& linep.charAt(pos_col) == '*'
			&& pos_col < comment_col)
		{
		    count++;
		    match_pos_lnum = pos_lnum;
		    match_pos_col = pos_col;
		    match_pos_col--;
		}
		else if (linep.charAt(pos_col - 1) == '*'
                        && linep.charAt(pos_col) == '/')
		{
		    if (count > 0) {
			pos_lnum = match_pos_lnum;
			pos_col = match_pos_col;
                    }else if (pos_col > 1 && linep.charAt(pos_col - 2) == '/'
					       && pos_col <= comment_col)
			pos_col -= 2;
		    else if (ignore_cend)
			continue;
		    else
			return null;
                    return new FPOS(pos_lnum, pos_col);
		}
	    }
	    continue;
	}

	//
	// If smart matching ('cpoptions' does not contain '%'), braces inside
	// of quotes are ignored, but only if there is an even number of
	// quotes in the line.
	//
	if (cpo_match)
	    do_quotes = 0;
	else if (do_quotes == -1)
	{
	    //
	    // Count the number of quotes in the line, skipping \" and '"'.
	    // Watch out for "\\".
	    //
	    at_start = do_quotes;
            // '-1' in following 'cause of newline
	    for (ptr = 0; ptr < linep.count - 1; ++ptr)
	    {
		if (ptr == pos_col + (backwards ? 1 : 0))
		    at_start = (do_quotes & 1);
		if (linep.charAt(ptr) == '"'
			&& (ptr == 0
                            || linep.charAt(ptr-1) != '\''
                            || linep.charAt(ptr + 1) != '\''))
		    ++do_quotes;
		if (linep.charAt(ptr) == '\\' && ptr + 1 < linep.count)
		    ++ptr;
	    }
	    do_quotes &= 1;	// result is 1 with even number of quotes

	    //
	    // If we find an uneven count, check current line and previous
	    // one for a '\' at the end.
	    //
	    if (do_quotes == 0)
	    {
		inquote = false;
		if (linep.charAt(ptr-1) == '\\')
		{
		    do_quotes = 1;
		    if (start_in_quotes == MAYBE)
		    {
			/* Do we need to use at_start here? */
			inquote = true;
			start_in_quotes = TRUE;
		    }
		    else if (backwards)
			inquote = true;
		}
		if (pos_lnum > 1)
		{
                    MySegment seg = ml_get(pos_lnum - 1);
                    // checking last char of previous line
		    if (seg.charAt(0) != '\n'
                            && seg.charAt(seg.count - 2) == '\\')
		    {
			do_quotes = 1;
			if (start_in_quotes == MAYBE)
			{
			    inquote = at_start != 0;
			    if (inquote)
				start_in_quotes = TRUE;
			}
			else if (!backwards)
			    inquote = true;
		    }
		}
	    }
	}
	if (start_in_quotes == MAYBE)
	    start_in_quotes = FALSE;

	//
	// If 'smartmatch' is set:
	//   Things inside quotes are ignored by setting 'inquote'.  If we
	//   find a quote without a preceding '\' invert 'inquote'.  At the
	//   end of a line not ending in '\' we reset 'inquote'.
	//
	//   In lines with an uneven number of quotes (without preceding '\')
	//   we do not know which part to ignore. Therefore we only set
	//   inquote if the number of quotes in a line is even, unless this
	//   line or the previous one ends in a '\'.  Complicated, isn't it?
	//

	switch (c = linep.charAt(pos_col))
	{
	case NUL:
	    /* at end of line without trailing backslash, reset inquote */
	    if (pos_col == 0 || linep.array[linep.offset + pos_col - 1] != '\\')
	    {
		inquote = false;
		start_in_quotes = FALSE;
	    }
	    break;

	case '"':
	    // a quote that is preceded with an odd number of backslashes is
	    // ignored
	    if (do_quotes != 0)
	    {
		int col;

		for (col = pos_col - 1; col >= 0; --col)
		    if (linep.charAt(col) != '\\')
			break;
		if (((pos_col - 1 - col) & 1) == 0)
		{
		    inquote = !inquote;
		    start_in_quotes = FALSE;
		}
	    }
	    break;

	//
	// If smart matching ('cpoptions' does not contain '%'):
	//   Skip things in single quotes: 'x' or '\x'.  Be careful for single
	//   single quotes, eg jon's.  Things like '\233' or '\x3f' are not
	//   skipped, there is never a brace in them.
	//   Ignore this when finding matches for `'.
	//
	case '\'':
	    if (!cpo_match && initc != '\'' && findc != '\'')
	    {
		if (backwards)
		{
		    if (pos_col > 1)
		    {
			if (linep.charAt(pos_col - 2) == '\'')
			{
			    pos_col -= 2;
			    break;
			}
			else if (linep.charAt(pos_col - 2) == '\\'
                                 && pos_col > 2
                                 && linep.charAt(pos_col - 3) == '\'')
			{
			    pos_col -= 3;
			    break;
			}
		    }
		}
                // jVi: FOLLOWING was +1
		else if (pos_col + 2 < linep.length())	// forward search
		{
		    if (linep.charAt(pos_col + 1) == '\\'
			    // ORIGINAL: && pos_col + 2 < linep.length()
			    && pos_col + 3 < linep.length()
                            && linep.charAt(pos_col + 3) == '\'')
		    {
			pos_col += 3;
			break;
		    }
		    else if (linep.charAt(pos_col + 2) == '\'')
		    {
			pos_col += 2;
			break;
		    }
		}
	    }
	    /* FALLTHROUGH */

	default:
//#ifdef FEAT_LISP...

	    // Check for match outside of quotes, and inside of
	    // quotes when the start is also inside of quotes.
	    if ((!inquote || start_in_quotes == TRUE)
		    && (c == initc || c == findc))
	    {
		int	col, bslcnt = 0;

		if (!cpo_bsl)
		{
                    col = pos_col;
                    while(true) {
                        boolean match = check_prevcol(linep, col, '\\', null);
                        if(!match)
                          break;
                        bslcnt++;
                        col--;
                    }
		}
		// Only accept a match when 'M' is in 'cpo' or when ecaping is
		// what we expect.
		if (cpo_bsl || (bslcnt & 1) == match_escaped)
		{
		    if (c == initc)
			count++;
		    else
		    {
			if (count == 0) {
                            return new FPOS(pos_lnum, pos_col);
                        }
			count--;
		    }
		}
	    }
	}
    }

    if (comment_dir == BACKWARD && count > 0)
    {
        return new FPOS(match_pos_lnum, match_pos_col);
    }
  return null;	// never found it
}

/**
 * Check if line[] contains a / / comment.
 * Return MAXCOL if not, otherwise return the column.
 * TODO: skip strings.
 */
static int check_linecomment(MySegment seg)
{
    int p = 0;
//#ifdef FEAT_LISP...
    while ((p = Util.vim_strchr(seg, p, '/')) > 0)
    {
	if (seg.charAt(p + 1) == '/')
	    break;
	++p;
    }

    if (p < 0)
	return MAXCOL;
    return p;
}

//===========================================================================


/**
 * Go back to the start of the word or the start of white space
 */
static void back_in_line()
{
    int		sclass;		    /* starting class */

    sclass = cls();
    for (;;)
    {
	if (G.curwin.w_cursor.getColumn() == 0)	    // stop at start of line
	    break;
	dec_cursor();
	if (cls() != sclass)		    /* stop at start of word */
	{
	    inc_cursorV7();
	    break;
	}
    }
}

static void
find_first_blank(ViFPOS posp)
{
    char    c;

    while (decl(posp) != -1)
    {
	c = gchar_pos(posp);
	if (!vim_iswhite(c))
	{
	    inclV7(posp);
	    break;
	}
    }
}

/**
 * Skip count/2 sentences and count/2 separating white spaces.
 */
static void
findsent_forward(int count, boolean at_start_sent)
    //int	    at_start_sent;	/* cursor is at start of sentence */
{
    while (count-- != 0)
    {
	findsent(FORWARD, 1);
	if (at_start_sent)
	    find_first_blank(G.curwin.w_cursor);
	if (count == 0 || at_start_sent)
	    decl(G.curwin.w_cursor);
	at_start_sent = !at_start_sent;
    }
}

/**
 * Find word under cursor, cursor at end.
 * Used while an operator is pending, and in Visual mode.
 */
static int current_word(OPARG oap, int count, boolean include, boolean bigword)
//    oparg_T	*oap;
//    long	count;
//    int		include;	/* TRUE: include word and white space */
//    int		bigword;	/* FALSE == word, TRUE == WORD */
{
    ViFPOS	start_pos;
    ViFPOS	pos;
    boolean	inclusive = true;
    boolean	include_white = false;

    funnyCharsAsWord = bigword; // cls_bigword = bigword;
    start_pos = null; // clearpos(&start_pos);

    /* Correct cursor when 'selection' is exclusive */
    if (G.VIsual_active && G.p_sel.charAt(0) == 'e'
            && G.VIsual.compareTo(G.curwin.w_cursor) < 0)
	Misc.dec_cursor();

    //
    // When Visual mode is not active, or when the VIsual area is only one
    // character, select the word and/or white space under the cursor.
    //
    if (!G.VIsual_active || G.curwin.w_cursor.equals(G.VIsual))
    {
	/*
	 * Go to start of current word or white space.
	 */
	back_in_line();
	start_pos = G.curwin.w_cursor.copy();

	//
	// If the start is on white space, and white space should be included
	// ("	word"), or start is not on white space, and white space should
	// not be included ("word"), find end of word.
	//
	if ((cls() == 0) == include)
	{
	    if (end_word(1, bigword, true, true) == FAIL)
		return FAIL;
	}
	else
	{
	    //
	    // If the start is not on white space, and white space should be
	    // included ("word	 "), or start is on white space and white
	    // space should not be included ("	 "), find start of word.
	    // If we end up in the first column of the next line (single char
	    // word) back up to end of the line.
	    //
	    fwd_word(1, bigword, true);
	    if (G.curwin.w_cursor.getColumn() == 0)
		Misc.decl(G.curwin.w_cursor);
	    else
		Edit.oneleft();

	    if (include)
		include_white = true;
	}

	if (G.VIsual_active)
	{
	    /* should do something when inclusive == FALSE ! */
	    G.VIsual = start_pos;
	    // redraw_curbuf_later(INVERTED);	/* update the inversion */
            Normal.v_updateVisualState();
	}
	else
	{
	    oap.start = start_pos;
	    oap.motion_type = MCHAR;
	}
	--count;
    }

    //
    // When count is still > 0, extend with more objects.
    //
    while (count > 0)
    {
	inclusive = true;
	if (G.VIsual_active && G.curwin.w_cursor.compareTo(G.VIsual) < 0)
	{
	    //
	    // In Visual mode, with cursor at start: move cursor back.
	    //
	    if (Misc.decl(G.curwin.w_cursor) == -1)
		return FAIL;
	    if (include != (cls() != 0))
	    {
		if (bck_word(1, bigword, true) == FAIL)
		    return FAIL;
	    }
	    else
	    {
		if (bckend_word(1, bigword, true) == FAIL)
		    return FAIL;
		Misc.inclV7(G.curwin.w_cursor);
	    }
	}
	else
	{
	    //
	    // Move cursor forward one word and/or white area.
	    //
	    if (Misc.inclV7(G.curwin.w_cursor) == -1)
		return FAIL;
	    if (include != (cls() == 0))
	    {
		if (fwd_word(1, bigword, true) == FAIL && count > 1)
		    return FAIL;
		//
		// If end is just past a new-line, we don't want to include
		// the first character on the line.
		// Put cursor on last char of white.
		//
		if (Edit.oneleft() == FAIL)
		    inclusive = false;
	    }
	    else
	    {
		if (end_word(1, bigword, true, true) == FAIL)
		    return FAIL;
	    }
	}
	--count;
    }

    if (include_white && (cls() != 0
		 || (G.curwin.w_cursor.getColumn() == 0 && !inclusive)))
    {
	//
	// If we don't include white space at the end, move the start
	// to include some white space there. This makes "daw" work
	// better on the last word in a sentence (and "2daw" on last-but-one
	// word).  Also when "2daw" deletes "word." at the end of the line
	// (cursor is at start of next line).
	// But don't delete white space at start of line (indent).
	//
	pos = G.curwin.w_cursor.copy();	// save cursor position
	G.curwin.w_cursor.set(start_pos);
	if (Edit.oneleft() == OK)
	{
	    back_in_line();
	    if (cls() == 0 && G.curwin.w_cursor.getColumn() > 0)
	    {
		if (G.VIsual_active)
		    G.VIsual = G.curwin.w_cursor.copy();
		else
		    oap.start = G.curwin.w_cursor.copy();
	    }
	}
	G.curwin.w_cursor.set(pos);	// put cursor back at end
    }

    if (G.VIsual_active)
    {
	if (G.p_sel.charAt(0) == 'e'
                && inclusive
                && G.VIsual.compareTo(G.curwin.w_cursor) <= 0)
	    inc_cursorV7();
	if (G.VIsual_mode == 'V')
	{
	    G.VIsual_mode = 'v';
	    //redraw_cmdline = TRUE;		/* show mode later */
	}
        Normal.v_updateVisualState();
    }
    else
	oap.inclusive = inclusive;

    return OK;
}

/**
 * Find sentence(s) under the cursor, cursor at end.
 * When Visual active, extend it by one or more sentences.
 */
static int
current_sent(OPARG oap, int count, boolean include)
{
    ViFPOS	start_pos;
    ViFPOS	pos;
    boolean	start_blank;
    char	c;
    boolean	at_start_sent;
    int		ncount;
    boolean	extending = false;

    start_pos = G.curwin.w_cursor.copy();
    pos = start_pos.copy();
    findsent(FORWARD, 1);	// Find start of next sentence.

    //
    // When visual area is bigger than one character: Extend it.
    //

extend: do {
    if (extending || G.VIsual_active && !start_pos.equals(G.VIsual))
    {
//extend: original target of goto
	if (start_pos.compareTo(G.VIsual) < 0)
	{
	    /*
	     * Cursor at start of Visual area.
	     * Find out where we are:
	     * - in the white space before a sentence
	     * - in a sentence or just after it
	     * - at the start of a sentence
	     */
	    at_start_sent = true;
	    decl(pos);
	    while (pos.compareTo(G.curwin.w_cursor) < 0)
	    {
		c = gchar_pos(pos);
		if (!vim_iswhite(c))
		{
		    at_start_sent = false;
		    break;
		}
		inclV7(pos);
	    }
	    if (!at_start_sent)
	    {
		findsent(BACKWARD, 1);
		if (equalpos(G.curwin.w_cursor, start_pos))
		    at_start_sent = true;  /* exactly at start of sentence */
		else
		    /* inside a sentence, go to its end (start of next) */
		    findsent(FORWARD, 1);
	    }
	    if (include)	/* "as" gets twice as much as "is" */
		count *= 2;
	    while (count-- != 0)
	    {
		if (at_start_sent)
		    find_first_blank(G.curwin.w_cursor);
		c = gchar_cursor();
		if (!at_start_sent || (!include && !vim_iswhite(c)))
		    findsent(BACKWARD, 1);
		at_start_sent = !at_start_sent;
	    }
	}
	else
	{
	    /*
	     * Cursor at end of Visual area.
	     * Find out where we are:
	     * - just before a sentence
	     * - just before or in the white space before a sentence
	     * - in a sentence
	     */
	    inclV7(pos);
	    at_start_sent = true;
	    if (!equalpos(pos, G.curwin.w_cursor)) /* not just before a sentence */
	    {
		at_start_sent = false;
		while (lt(pos, G.curwin.w_cursor))
		{
		    c = gchar_pos(pos);
		    if (!vim_iswhite(c))
		    {
			at_start_sent = true;
			break;
		    }
		    inclV7(pos);
		}
		if (at_start_sent)	/* in the sentence */
		    findsent(BACKWARD, 1);
		else		/* in/before white before a sentence */
		    G.curwin.w_cursor.set(start_pos);
	    }

	    if (include)	/* "as" gets twice as much as "is" */
		count *= 2;
	    findsent_forward(count, at_start_sent);
	    if (G.p_sel.charAt(0) == 'e')
		G.curwin.w_cursor.incColumn();
	}
	return OK;
    }

    /*
     * If cursor started on blank, check if it is just before the start of the
     * next sentence.
     */
    while (vim_iswhite(gchar_pos(pos)))	/* vim_iswhite() is a macro */
	inclV7(pos);
    if (equalpos(pos, G.curwin.w_cursor))
    {
	start_blank = true;
	find_first_blank(start_pos);	/* go back to first blank */
    }
    else
    {
	start_blank = false;
	findsent(BACKWARD, 1);
	start_pos = G.curwin.w_cursor;
    }
    if (include)
	ncount = count * 2;
    else
    {
	ncount = count;
	if (start_blank)
	    --ncount;
    }
    if (ncount > 0)
	findsent_forward(ncount, true);
    else
	decl(G.curwin.w_cursor);

    if (include)
    {
	/*
	 * If the blank in front of the sentence is included, exclude the
	 * blanks at the end of the sentence, go back to the first blank.
	 * If there are no trailing blanks, try to include leading blanks.
	 */
	if (start_blank)
	{
	    find_first_blank(G.curwin.w_cursor);
	    c = gchar_pos(G.curwin.w_cursor);	/* vim_iswhite() is a macro */
	    if (vim_iswhite(c))
		decl(G.curwin.w_cursor);
	}
	else if (!vim_iswhite(gchar_cursor()))
	    find_first_blank(start_pos);
    }

    if (G.VIsual_active)
    {
	/* avoid getting stuck with "is" on a single space before a sent. */
	if (equalpos(start_pos, G.curwin.w_cursor)) {
            extending = true; // part of goto workaround
	    continue extend;  // was goto extend
        }
	if (G.p_sel.charAt(0) == 'e')
	    G.curwin.w_cursor.incColumn();
        G.VIsual = start_pos;
	G.VIsual_mode = 'v';
	//redraw_curbuf_later(INVERTED);	/* update the inversion */
        Normal.v_updateVisualState();
    }
    else
    {
	/* include a newline after the sentence, if there is one */
	if (inclV7(G.curwin.w_cursor) == -1)
	    oap.inclusive = true;
	else
	    oap.inclusive = false;
	oap.start = start_pos;
	oap.motion_type = MCHAR;
    }
} while(false); // part of implementing goto extend
    return OK;
}

/*
 * Find block under the cursor, cursor at end.
 * "what" and "other" are two matching parenthesis/paren/etc.
 */
static int
current_block(OPARG oap, int count, boolean include, char what, char other)
    // int		include;	/* TRUE == include white space */
    // int		what;		/* '(', '{', etc. */
    // int		other;		/* ')', '}', etc. */
{
    ViFPOS	old_pos;
    ViFPOS	pos = null; //*pos = NULL;
    ViFPOS	start_pos = null;
    ViFPOS	end_pos; //*end_pos;
    ViFPOS	old_start, old_end;
    String	save_cpo;
    boolean	sol = false;		// '{' at start of line

    old_pos = G.curwin.w_cursor.copy();
    old_end = G.curwin.w_cursor.copy();		/* remember where we started */
    old_start = old_end;

    //
    // If we start on '(', '{', ')', '}', etc., use the whole block inclusive.
    //
    if (!G.VIsual_active || G.VIsual.equals(G.curwin.w_cursor))
    {
	MarkOps.setpcmark();
	if (what == '{')		/* ignore indent */
	    while (Misc.inindent(1))
		if (inc_cursorV7() != 0)
		    break;
	if (Misc.gchar_cursor() == what)
	    /* cursor on '(' or '{', move cursor just after it */
            G.curwin.w_cursor.incColumn();
    }
    else if (G.VIsual.compareTo(G.curwin.w_cursor) < 0)
    {
	old_start = G.VIsual.copy();
        G.curwin.w_cursor.set(G.VIsual);  // cursor at low end of Visual
    }
    else
	old_end = G.VIsual.copy();

    /*
     * Search backwards for unclosed '(', '{', etc..
     * Put this position in start_pos.
     * Ignore quotes here.
     */
    save_cpo = G.p_cpo;
    G.p_cpo = "%";
    while (count-- > 0)
    {
	if ((pos = findmatch(null, what)) == null)
	    break;
	G.curwin.w_cursor.set(pos);
	start_pos = pos.copy(); // the findmatch for end_pos will overwrite *pos
    }
    G.p_cpo = save_cpo;

    //
    // Search for matching ')', '}', etc.
    // Put this position in curwin->w_cursor.
    //
    if (pos == null || (end_pos = findmatch(null, other)) == null)
    {
        G.curwin.w_cursor.set(old_pos);
	return FAIL;
    }
    G.curwin.w_cursor.set(end_pos);

    //
    // Try to exclude the '(', '{', ')', '}', etc. when "include" is FALSE.
    // If the ending '}' is only preceded by indent, skip that indent.
    // But only if the resulting area is not smaller than what we started with.
    //
    while (!include)
    {
	Misc.inclV7(start_pos);
	sol = (G.curwin.w_cursor.getColumn() == 0);
	Misc.decl(G.curwin.w_cursor);
	if (what == '{')
	    while (Misc.inindent(1))
	    {
		sol = true;
		if (Misc.decl(G.curwin.w_cursor) != 0)
		    break;
	    }
	//
	// In Visual mode, when the resulting area is not bigger than what we
	// started with, extend it to the next block, and then exclude again.
	//
	if (!(start_pos.compareTo(old_start) < 0)                 // !lt
                && !(old_end.compareTo(G.curwin.w_cursor) < 0)    // !lt
		&& G.VIsual_active)
	{
	    G.curwin.w_cursor.set(old_start);
	    Misc.decl(G.curwin.w_cursor);
	    if ((pos = findmatch(null, what)) == null)
	    {
		G.curwin.w_cursor.set(old_pos);
		return FAIL;
	    }
	    start_pos.set(pos);
	    G.curwin.w_cursor.set(pos);
	    if ((end_pos = findmatch(null, other)) == null)
	    {
		G.curwin.w_cursor.set(old_pos);
		return FAIL;
	    }
	    G.curwin.w_cursor.set(end_pos);
	}
	else
	    break;
    }

    if (G.VIsual_active)
    {
	if (G.p_sel.charAt(0) == 'e')
	    G.curwin.w_cursor.incColumn();
	if (sol && Misc.gchar_cursor() != NUL)
	    Misc.inc(G.curwin.w_cursor);	// include the line break
	G.VIsual.set(start_pos);
	G.VIsual_mode = 'v';
	// redraw_curbuf_later(INVERTED);	/* update the inversion */
	// showmode();
        Normal.v_updateVisualState();
    }
    else
    {
	oap.start = start_pos;
	oap.motion_type = MCHAR;
	if (sol)
	{
	    Misc.inclV7(G.curwin.w_cursor);
	    oap.inclusive = false;
	}
	else
	    oap.inclusive = true;
    }

    return OK;
}

/**
 * Return true if the cursor is on a "<aaa>" tag.  Ignore "<aaa/>".
 * When "end_tag" is true return true if the cursor is on "</aaa>".
 */
static boolean
in_html_tag(boolean end_tag)
{
    MySegment	line = ml_get_curline();
    int		p;
    char	c;
    int		lc = NUL;
    ViFPOS	pos;

// #ifdef FEAT_MBYTE
//     if (enc_dbcs)
//     {
// 	char_u	*lp = null;
//
// 	/* We search forward until the cursor, because searching backwards is
// 	 * very slow for DBCS encodings. */
// 	for (p = line; p < line + G.curwin.w_cursor.getColumn(); mb_ptr_adv(p))
// 	    if (line.charAt(p) == '>' || line.charAt(p) == '<')
// 	    {
// 		lc = line.charAt(p);
// 		lp = p;
// 	    }
// 	if (line.charAt(p) != '<')	    /* check for '<' under cursor */
// 	{
// 	    if (lc != '<')
// 		return false;
// 	    p = lp;
// 	}
//     }
//     else
// #endif
    {
	//for (p = line + G.curwin.w_cursor.getColumn(); p > line; )
	for (p =  G.curwin.w_cursor.getColumn(); p > 0; )
	{
	    if (line.charAt(p) == '<')	/* find '<' under/before cursor */
		break;
	    --p; //mb_ptr_back(line, p);
	    if (line.charAt(p) == '>')	/* find '>' before cursor */
		break;
	}
	if (line.charAt(p) != '<')
	    return false;
    }

    pos = G.curwin.w_cursor.copy();
    pos.set(G.curwin.w_cursor.getLine(), p); // (..., p - line)

    p++; //mb_ptr_adv(p);
    if (end_tag)
	/* check that there is a '/' after the '<' */
	return line.charAt(p) == '/';

    /* check that there is no '/' after the '<' */
    if (line.charAt(p) == '/')
	return false;

    /* check that the matching '>' is not preceded by '/' */
    for (;;)
    {
	if (incV7(pos) < 0)
	    return false;
	c = ml_get_pos(pos).current();
	if (c == '>')
	    break;
	lc = c;
    }
    return lc != '/';
}

/**
 * Find tag block under the cursor, cursor at end.
 * @param include true == include white space
 */
static int
current_tagblock(OPARG oap, int count_arg, boolean include)
    /* true == include white space */
{
    int		count = count_arg;
    int		n;
    ViFPOS	old_pos;
    ViFPOS	start_pos;
    ViFPOS	end_pos;
    ViFPOS	old_start, old_end;
    String	spat = null, epat = null;
    MySegment   line;
    int		p;
    int		cp;
    int		len;
    int		r;
    boolean	do_include = include;
    boolean	save_p_ws = G.p_ws.getBoolean();
    int		retval = FAIL;

    G.p_ws.setBoolean(false);

    old_pos = G.curwin.w_cursor.copy();
    old_end = G.curwin.w_cursor.copy();		    /* remember where we started */
    old_start = old_end.copy();
    //System.err.println("html: init cursor " + old_pos);

    /*
     * If we start on "<aaa>" select that block.
     */
    if (!G.VIsual_active || equalpos(G.VIsual, G.curwin.w_cursor))
    {
	setpcmark();

	/* ignore indent */
	while (inindent(1))
	    if (inc_cursorV7() != 0)
		break;

	if (in_html_tag(false))
	{
	    /* cursor on start tag, move to just after it */
	    while (ml_get_cursor().current() != '>')
		if (inc_cursorV7() < 0)
		    break;
            //System.err.println("html: in start tag " + G.curwin.w_cursor);
	}
	else if (in_html_tag(true))
	{
	    /* cursor on end tag, move to just before it */
	    while (ml_get_cursor().current() != '<')
		if (dec_cursor() < 0)
		    break;
	    dec_cursor();
	    old_end = G.curwin.w_cursor.copy();
            //System.err.println("html: in end tag " + G.curwin.w_cursor);
	}
    }
    else if (lt(G.VIsual, G.curwin.w_cursor))
    {
	old_start = G.VIsual.copy();
	G.curwin.w_cursor.set(G.VIsual);	    /* cursor at low end of Visual */
    }
    else
	old_end = G.VIsual.copy();

    /*
     * Search backwards for unclosed "<aaa>".
     * Put this position in start_pos.
     */
again:
do {
    for (n = 0; n < count; ++n)
    {
                  //"<[^ \t>/!]\\+\\%(\\_s\\_[^>]\\{-}[^/]>\\|$\\|\\_s\\=>\\)",
                  //"<[^ \t>/!]+(?:[\\n\\s][\\n[^>]]*?[^/]>|$|[\\n\\s]?>)",
                  //"<[^ \t>/!]+(?:\\s[^>]*?[^/]>|$|\\s?>)",
	if (Eval.do_searchpair(
                    "<[^ \t>/!]+(?:[\\n\\s][\\n[^>]]*?[^/]>|$|[\\n\\s]?>)",
                    "",
		    "</[^>]*>", BACKWARD, "", 0, null, 0) <= 0)
	{
	    G.curwin.w_cursor.set(old_pos);
	    break again; //break theend;
	}
    }
    start_pos = G.curwin.w_cursor.copy();
    //System.err.println("html: find count " + G.curwin.w_cursor);

    /*
     * Search for matching "</aaa>".  First isolate the "aaa".
     */
    inc_cursorV7();
    line = (MySegment)ml_get_cursor(); // p = ml_get_cursor();
    p = line.getIndex() - line.getBeginIndex();
    //for (cp = p; *cp != NUL && *cp != '>' && !vim_iswhite(*cp); mb_ptr_adv(cp))
    for (cp = p;
         line.charAt(cp) != '\n'
              && cp < line.length() // this is defensive, not needed
              && line.charAt(cp) != '>'
              && !vim_iswhite(line.charAt(cp));
         //mb_ptr_adv(cp)
    )
	++cp;
    len = (cp - p);
    if (len == 0)
    {
	G.curwin.w_cursor.set(old_pos);
	break again; //break theend;
    }
    // NOTE: IGNORE CASE
    //sprintf((char *)spat, "<%.*s\\%%(\\_[^>]\\{-}[^/]>\\|>\\)\\c", len, p);
    //                "<%s(?:[\\n[^>]]*?[^/]>|>)\\c",
    //                   "<%s(?:[^>]*?[^/]>|>)\\c",
    spat = String.format(
                         "<%s(?:[^>]*?[^/]>|>)\\c",
                         line.subSequence(p, cp).toString());
    //sprintf((char *)epat, "</%.*s>\\c", len, p);
    epat = String.format("</%s>\\c",
                         line.subSequence(p, cp).toString());

    r = Eval.do_searchpair(spat, "", epat, FORWARD, "", 0, null, 0);
    //System.err.format("html: after searchpair r: %d\n    spat '%s' epat '%s'\n", r, spat, epat);

    if (r < 1 || lt(G.curwin.w_cursor, old_end))
    {
	/* Can't find other end or it's before the previous end.  Could be a
	 * HTML tag that doesn't have a matching end.  Search backwards for
	 * another starting tag. */
	count = 1;
	G.curwin.w_cursor.set(start_pos);
	continue again;
    }

    if (do_include || r < 1)
    {
	/* Include up to the '>'. */
	while (ml_get_cursor().current() != '>')
	    if (inc_cursorV7() < 0)
		break;
    }
    else
    {
	/* Exclude the '<' of the end tag. */
	if (ml_get_cursor().current() == '<')
	    dec_cursor();
    }
    end_pos = G.curwin.w_cursor.copy();

    if (!do_include)
    {
	/* Exclude the start tag. */
	G.curwin.w_cursor.set(start_pos);
	while (inc_cursorV7() >= 0)
	    if (ml_get_cursor().current() == '>'
                  && lt(G.curwin.w_cursor, end_pos))
	    {
		inc_cursorV7();
		start_pos = G.curwin.w_cursor.copy();
		break;
	    }
	G.curwin.w_cursor.set(end_pos);

	/* If we now have the same text as before reset "do_include" and try
	 * again. */
	if (equalpos(start_pos, old_start) && equalpos(end_pos, old_end))
	{
	    do_include = true;
	    G.curwin.w_cursor.set(old_start);
	    count = count_arg;
	    continue again;
	}
    }

    if (G.VIsual_active)
    {
	if (G.p_sel.charAt(0) == 'e')
	    G.curwin.w_cursor.incColumn();
	G.VIsual = start_pos.copy();
	G.VIsual_mode = 'v';
	// redraw_curbuf_later(INVERTED);	/* update the inversion */
        Normal.v_updateVisualState();
	// showmode();
    }
    else
    {
	oap.start = start_pos;
	oap.motion_type = MCHAR;
	oap.inclusive = true;
    }
    retval = OK;
} while(false); // theend:

    G.p_ws.setBoolean(save_p_ws);
    return retval;
}

static int
current_par(OPARG oap, int count, boolean include, char type)
    // int		include;	/* TRUE == include white space */
    // int		type;		/* 'p' for paragraph, 'S' for section */
{
    int		start_lnum;
    int		end_lnum;
    boolean	white_in_front;
    int		dir;
    int		start_is_white;
    int		prev_start_is_white;
    int		retval = OK;
    boolean	do_white = false;
    int		t;
    int		i;
    boolean	extending = false;

    if (type == 'S')	    /* not implemented yet */
	return FAIL;

    start_lnum = G.curwin.w_cursor.getLine();

    /*
     * When visual area is more than one line: extend it.
     */
extend: do {
    if (extending || G.VIsual_active && start_lnum != G.VIsual.getLine())
    {
//extend:
	if (start_lnum < G.VIsual.getLine())
	    dir = BACKWARD;
	else
	    dir = FORWARD;
	for (i = count; --i >= 0; )
	{
	    if (start_lnum ==
			   (dir == BACKWARD ? 1 : G.curbuf.getLineCount()))
	    {
		retval = FAIL;
		break;
	    }

	    prev_start_is_white = -1;
	    for (t = 0; t < 2; ++t)
	    {
		start_lnum += dir;
		start_is_white = linewhite(start_lnum) ? 1 : 0;
		if (prev_start_is_white == start_is_white)
		{
		    start_lnum -= dir;
		    break;
		}
		for (;;)
		{
		    if (start_lnum == (dir == BACKWARD
					    ? 1 : G.curbuf.getLineCount()))
			break;
		    if (start_is_white != (linewhite(start_lnum + dir) ? 1 : 0)
			    || (start_is_white == 0
				    && startPS(start_lnum
                                               + (dir > 0 ? 1 : 0), 0, false)))
			break;
		    start_lnum += dir;
		}
		if (!include)
		    break;
		if (start_lnum == (dir == BACKWARD
					    ? 1 : G.curbuf.getLineCount()))
		    break;
		prev_start_is_white = start_is_white;
	    }
	}
	G.curwin.w_cursor.setLine(start_lnum);
	G.curwin.w_cursor.setColumn(0);
	return retval;
    }

    /*
     * First move back to the start_lnum of the paragraph or white lines
     */
    white_in_front = linewhite(start_lnum);
    while (start_lnum > 1)
    {
	if (white_in_front)	    /* stop at first white line */
	{
	    if (!linewhite(start_lnum - 1))
		break;
	}
	else		/* stop at first non-white line of start of paragraph */
	{
	    if (linewhite(start_lnum - 1) || startPS(start_lnum, 0, false))
		break;
	}
	--start_lnum;
    }

    /*
     * Move past the end of any white lines.
     */
    end_lnum = start_lnum;
    while (end_lnum <= G.curbuf.getLineCount() && linewhite(end_lnum))
	++end_lnum;

    --end_lnum;
    i = count;
    if (!include && white_in_front)
	--i;
    while (i-- != 0)
    {
	if (end_lnum == G.curbuf.getLineCount())
	    return FAIL;

	if (!include)
	    do_white = linewhite(end_lnum + 1);

	if (include || !do_white)
	{
	    ++end_lnum;
	    /*
	     * skip to end of paragraph
	     */
	    while (end_lnum < G.curbuf.getLineCount()
		    && !linewhite(end_lnum + 1)
		    && !startPS(end_lnum + 1, 0, false))
		++end_lnum;
	}

	if (i == 0 && white_in_front && include)
	    break;

	/*
	 * skip to end of white lines after paragraph
	 */
	if (include || do_white)
	    while (end_lnum < G.curbuf.getLineCount()
						   && linewhite(end_lnum + 1))
		++end_lnum;
    }

    /*
     * If there are no empty lines at the end, try to find some empty lines at
     * the start (unless that has been done already).
     */
    if (!white_in_front && !linewhite(end_lnum) && include)
	while (start_lnum > 1 && linewhite(start_lnum - 1))
	    --start_lnum;

    if (G.VIsual_active)
    {
	/* Problem: when doing "Vipipip" nothing happens in a single white
	 * line, we get stuck there.  Trap this here. */
	if (G.VIsual_mode == 'V' && start_lnum == G.curwin.w_cursor.getLine())
	    continue extend;
	G.VIsual.setLine(start_lnum);
	G.VIsual_mode = 'V';
	//redraw_curbuf_later(INVERTED);	/* update the inversion */
        Normal.v_updateVisualState();
	//showmode();
    }
    else
    {
	oap.start.setLine(start_lnum);
	oap.start.setColumn(0);
	oap.motion_type = MLINE;
    }
    G.curwin.w_cursor.setLine(end_lnum);
    G.curwin.w_cursor.setColumn(0);
} while(false);
    return OK;
}


/**
 * Search quote char from string line[col].
 * Quote character escaped by one of the characters in "escape" is not counted
 * as a quote.
 * Returns column number of "quotechar" or -1 when not found.
 */
static int
find_next_quote(MySegment line, int col, char quotechar, String escape)
//    char_u	*escape;	/* escape characters, can be NULL */
{
    char	c;

    for (;;)
    {
	if (col >= line.count)
	    return -1;
	c = line.charAt(col);
	//if (escape != null && vim_strchr(escape, c) != null)
        if(escape != null && vim_strchr(escape, c) != null)
	    ++col;
	else if (c == quotechar)
	    break;
//#ifdef FEAT_MBYTE...
        ++col;
    }
    return col;
}

/**
 * Search backwards in "line" from column "col_start" to find "quotechar".
 * Quote character escaped by one of the characters in "escape" is not counted
 * as a quote.
 * Return the found column or zero.
 */
static int
find_prev_quote(MySegment line, int col_start, char quotechar, String escape)
//    char_u	*escape;	/* escape characters, can be NULL */
{
    int		n;

    while (col_start > 0)
    {
	--col_start;
//#ifdef FEAT_MBYTE...
	n = 0;
	if (escape != null)
	    while (col_start - n > 0
                   && vim_strchr(escape, line.charAt(col_start - n - 1)) != null)
                ++n;
	if ((n & 1) != 0)
	    col_start -= n;	// uneven number of escape chars, skip it
	else if (line.charAt(col_start) == quotechar)
	    break;
    }
    return col_start;
}

/**
 * Find quote under the cursor, cursor at end.
 * Returns TRUE if found, else FALSE.
 */
static int current_quote(OPARG oap, int count, boolean include, char quotechar)
//  int		include;	/* TRUE == include quote char */
//  int		quotechar;	/* Quote character */
{
    MySegment	line = Util.ml_get_curline();
    int		col_end;
    int		col_start = G.curwin.w_cursor.getColumn();
    boolean	inclusive = false;
    boolean	vis_empty = true;	// Visual selection <= 1 char
    boolean	vis_bef_curs = false;	// Visual starts before cursor
    boolean	inside_quotes = false;	// Looks like "i'" done before
    boolean	selected_quote = false;	// Has quote inside selection
    int		i;

    // Correct cursor when 'selection' is exclusive
    if (G.VIsual_active)
    {
	vis_bef_curs = G.VIsual.compareTo(G.curwin.w_cursor) < 0;
	if (G.p_sel.charAt(0) == 'e' && vis_bef_curs)
	    dec_cursor();
	vis_empty = G.VIsual.equals(G.curwin.w_cursor);
    }

    if (!vis_empty)
    {
	// Check if the existing selection exactly spans the text inside
	// quotes.
	if (vis_bef_curs)
	{
	    inside_quotes = G.VIsual.getColumn() > 0
			&& line.charAt(G.VIsual.getColumn() - 1) == quotechar
			&& line.charAt(G.curwin.w_cursor.getColumn()) != 0
			&& line.charAt(G.curwin.w_cursor.getColumn() + 1) == quotechar;
	    i = G.VIsual.getColumn();
	    col_end = G.curwin.w_cursor.getColumn();
	}
	else
	{
	    inside_quotes = G.curwin.w_cursor.getColumn() > 0
			&& line.charAt(G.curwin.w_cursor.getColumn() - 1) == quotechar
			&& line.charAt(G.VIsual.getColumn()) != 0
			&& line.charAt(G.VIsual.getColumn() + 1) == quotechar;
	    i = G.curwin.w_cursor.getColumn();
	    col_end = G.VIsual.getColumn();
	}

	// Find out if we have a quote in the selection.
	while (i <= col_end)
	    if (line.charAt(i++) == quotechar)
	    {
		selected_quote = true;
		break;
	    }
    }

    if (!vis_empty && line.charAt(col_start) == quotechar)
    {
	// Already selecting something and on a quote character.  Find the
	// next quoted string.
	if (vis_bef_curs)
	{
	    // Assume we are on a closing quote: move to after the next
	    // opening quote.
	    col_start = find_next_quote(line, col_start + 1, quotechar, null);
	    if (col_start < 0)
		return FALSE;
	    col_end = find_next_quote(line, col_start + 1, quotechar,
							      G.curbuf.b_p_qe);
	    if (col_end < 0)
	    {
		// We were on a starting quote perhaps?
		col_end = col_start;
		col_start = G.curwin.w_cursor.getColumn();
	    }
	}
	else
	{
	    col_end = find_prev_quote(line, col_start, quotechar, null);
	    if (line.charAt(col_end) != quotechar)
		return FALSE;
	    col_start = find_prev_quote(line, col_end, quotechar,
							      G.curbuf.b_p_qe);
	    if (line.charAt(col_start) != quotechar)
	    {
		// We were on an ending quote perhaps?
		col_start = col_end;
		col_end = G.curwin.w_cursor.getColumn();
	    }
	}
    }
    else

    if (line.charAt(col_start) == quotechar
	    || !vis_empty
	    )
    {
	int	first_col = col_start;

	if (!vis_empty)
	{
	    if (vis_bef_curs)
		first_col = find_next_quote(line, col_start, quotechar, null);
	    else
		first_col = find_prev_quote(line, col_start, quotechar, null);
	}
	// The cursor is on a quote, we don't know if it's the opening or
	// closing quote.  Search from the start of the line to find out.
	// Also do this when there is a Visual area, a' may leave the cursor
	// in between two strings.
	col_start = 0;
	for (;;)
	{
	    // Find open quote character.
	    col_start = find_next_quote(line, col_start, quotechar, null);
	    if (col_start < 0 || col_start > first_col)
		return FALSE;
	    // Find close quote character.
	    col_end = find_next_quote(line, col_start + 1, quotechar,
							      G.curbuf.b_p_qe);
	    if (col_end < 0)
		return FALSE;
	    // If is cursor between start and end quote character, it is
	    // target text object.
	    if (col_start <= first_col && first_col <= col_end)
		break;
	    col_start = col_end + 1;
	}
    }
    else
    {
	// Search backward for a starting quote.
	col_start = find_prev_quote(line, col_start, quotechar, G.curbuf.b_p_qe);
	if (line.charAt(col_start) != quotechar)
	{
	    // No quote before the cursor, look after the cursor.
	    col_start = find_next_quote(line, col_start, quotechar, null);
	    if (col_start < 0)
		return FALSE;
	}

	// Find close quote character.
	col_end = find_next_quote(line, col_start + 1, quotechar,
							      G.curbuf.b_p_qe);
	if (col_end < 0)
	    return FALSE;
    }

    // When "include" is TRUE, include spaces after closing quote or before
    // the starting quote.
    if (include)
    {
	if (vim_iswhite(line.charAt(col_end + 1)))
	    while (vim_iswhite(line.charAt(col_end + 1)))
		++col_end;
	else
	    while (col_start > 0 && vim_iswhite(line.charAt(col_start - 1)))
		--col_start;
    }

    /* Set start position.  After vi" another i" must include the ".
     * For v2i" include the quotes. */
    if (!include && count < 2
	    && (vis_empty || !inside_quotes)
	    )
	++col_start;
    G.curwin.w_cursor.setColumn(col_start);
    if (G.VIsual_active)
    {
	// Set the start of the Visual area when the Visual area was empty, we
	// were just inside quotes or the Visual area didn't start at a quote
	// and didn't include a quote.
	//
	if (vis_empty
		|| (vis_bef_curs
		    && !selected_quote
		    && (inside_quotes
			|| (line.charAt(G.VIsual.getColumn()) != quotechar
			    && (G.VIsual.getColumn() == 0
				|| line.charAt(G.VIsual.getColumn() - 1)
                                    != quotechar)))))
	{
	    G.VIsual = G.curwin.w_cursor.copy();
	    // redraw_curbuf_later(INVERTED);
            Normal.v_updateVisualState();
	}
    }
    else
    {
	oap.start = G.curwin.w_cursor.copy();
	oap.motion_type = MCHAR;
    }

    // Set end position.
    G.curwin.w_cursor.setColumn(col_end);
    if ((include || count > 1
		// After vi" another i" must include the ".
		|| (!vis_empty && inside_quotes)
	) && inc_cursorV7() == 2)
	inclusive = true;
    if (G.VIsual_active)
    {
	if (vis_empty || vis_bef_curs)
	{
	    // decrement cursor when 'selection' is not exclusive
	    if (G.p_sel.charAt(0) != 'e')
		dec_cursor();
	}
	else
	{
	    // Cursor is at start of Visual area.  Set the end of the Visual
	    // area when it was just inside quotes or it didn't end at a
	    // quote.
	    if (inside_quotes
		    || (!selected_quote
			&& line.charAt(G.VIsual.getColumn()) != quotechar
			&& (line.charAt(G.VIsual.getColumn()) == NUL
			    || line.charAt(G.VIsual.getColumn() + 1) != quotechar)))
	    {
		dec_cursor();
		G.VIsual = G.curwin.w_cursor.copy();
	    }
	    G.curwin.w_cursor.setColumn(col_start);
	}
	if (G.VIsual_mode == 'V')
	{
	    G.VIsual_mode = 'v';
	    //redraw_cmdline = TRUE;		// show mode later
            Normal.v_updateVisualState();
	}
    }
    else
    {
	// Set inclusive and other oap's flags.
	oap.inclusive = inclusive;
    }

    return OK;
}

  static boolean linewhite(int /*linenr_t*/ lnum) {
      MySegment seg = Util.ml_get(lnum);
      int idx = Misc.skipwhite(seg);
      return seg.array[seg.offset + idx] == '\n';
  }
    
  /*
   * startPS: return TRUE if line 'lnum' is the start of a section or paragraph.
   * If 'para' is '{' or '}' only check for sections.
   * If 'both' is TRUE also stop at '}'
   */
  static boolean startPS(int /*linenr_t*/lnum, int para, boolean both) {
    MySegment seg = Util.ml_get(lnum);
    // if seg.count == 1, then only a \n, ie empty line
    char s = seg.count > 1 ? seg.array[seg.offset] : 0;
    // '\f' is formfeed, oh well, it doesn't hurt to be here
    if (s == para || s == '\f' || (both && s == '}'))
      return true;
//    if (s == '.' && (inmacro(p_sections, s + 1) ||
//            (!para && inmacro(p_para, s + 1))))
//        return true;
    return false;
  }
}

// vi:set sw=2 ts=8:
