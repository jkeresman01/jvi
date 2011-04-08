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

import com.raelity.jvi.core.lib.Messages;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.manager.Scheduler;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;

import com.raelity.text.*;
import com.raelity.text.TextUtil.MySegment;

import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;
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

  private Search() { }

  static ViCmdEntry getSearchCommandEntry() {
    if(searchCommandEntry == null) {
      searchCommandEntry = ViManager.getFactory()
                            .createCmdEntry(ViCmdEntry.Type.SEARCH);
      searchCommandEntry.addActionListener(
        new ActionListener() {
          @Override
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
  // or for use in Search01
  static String last_search_pat() {
    return lastPattern;
  }
  static void last_search_pat(String s) {
    lastPattern = s;
  }
  
  /** doSearch() should only be called after inputSearchPattern() */
  static int doSearch() {
    String pattern = fetchPattern();
    G.curwin.w_set_curswant = true;
    if(pattern.isEmpty()) {
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
      @Override
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
        setpcmark(searchPos);
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
  static boolean funnyCharsAsWord;

  /**
   * cls() - returns the class of character at curwin->w_cursor
   *<br>
   * The 'type' of the current search modifies the classes of characters if a
   * 'W', 'B', or 'E' motion is being done. In this case, chars. from class 2
   * are reported as class 1 since only white space boundaries are of interest.
   */
  static int cls() {
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
	    if (fpos.getColumn() == 0 && lineempty(fpos.getLine()))
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
		&& lineempty(fpos.getLine()))
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
	if (fpos.getColumn() == 0 && lineempty(fpos.getLine()))
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
        boolean useEscape = metacharacterEscapes.indexOf('?') >= 0;
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
      setpcmark();
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

}

// vi:set sw=2 ts=8:
