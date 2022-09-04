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

import java.awt.EventQueue;
import java.util.logging.Logger;

import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.core.lib.Messages;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.manager.Scheduler;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.lib.RegExp;
import com.raelity.jvi.lib.MySegment;

import static java.util.logging.Level.*;

import static com.raelity.jvi.core.MarkOps.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Misc01.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.eventbus.Subscribe;

import com.raelity.jvi.options.*;

import static com.raelity.jvi.manager.ViManager.eatme;
import static com.raelity.jvi.lib.TextUtil.sf;

/**
 * Searching, regexp and substitution.
 * Everything's static, can only do one thing at a time.
 */
public class Search
{
  private Search() { }

  private static final Logger LOG = Logger.getLogger(Search.class.getName());
  static final DebugOption dbg = Options.getDebugOption(Options.dbgSearch);

  ///////////////////////////////////////////////////////////////////////
  //
  // normal searching like "/" and "?"
  //
  private static ViCmdEntry searchCommandEntry;

  /**
   * Since the search is often two parts, due to command entry widget,
   * this holds the shared state between the parts.
   * Additional info for incremental search.
   */
  static class ActiveSearchState
  {
    private ViFPOS searchPos;
    private int searchTopLine;
    private boolean didIncrSearch;
    private boolean incrSearchActive;
    private int searchCount;
    private int searchOptions;
    private char dirc;
    private RegExp lastRE;

    char dirc() { return dirc; }
    int searchOptions() { return searchOptions; }
    ViFPOS searchPos() { return searchPos; }
    String pattern() { return fetchInputPattern(); }
  }

  static final ActiveSearchState ass = new ActiveSearchState();

  // HACK
  static void startupInitializePattern(String pattern) {
    if(pattern != null) {
      RegExp re = search_regcomp(pattern, 0, 0, 0);
      lastMatchingRE = re;
    }
    eatme(ESCAPED_FLAG);
  }

  static ViCmdEntry getSearchCommandEntry() {
    if(searchCommandEntry == null) {
      searchCommandEntry = ViManager.getFactory()
                            .createCmdEntry(ViCmdEntry.Type.SEARCH);
      ViCmdEntry.getEventBus().register(new Object()
      {
        @Subscribe
        public void entryComplete(ViCmdEntry.CmdEntryComplete ev)
        {
          if(ev.getSource() == searchCommandEntry)
            searchEntryComplete(ev.getActionCommand(), ev);
        }
      });
    }
    return searchCommandEntry;
  }
  
  private static String fetchInputPattern() {
      return getSearchCommandEntry().getCommand();
  }

  static private void searchEntryComplete(String cmd, Object ev) {
    try {
      dbg.printf(INFO, () -> sf("SEARCH: searchEntryComplete: %s\n", ev.toString()));
      Hook.setJViBusy(true);
      boolean acceptIncr = false;
      boolean cancel = false;
      
      Scheduler.stopCommandEntry(getSearchCommandEntry());
      
      if(cmd.charAt(0) == '\n') {
        if(ass.incrSearchActive
           && ass.didIncrSearch
           && ! fetchInputPattern().isEmpty()) {
          acceptIncr = true;
        }
      } else
        cancel = true;
      
      if(ass.incrSearchActive) {
        stopIncrementalSearch(acceptIncr);
      }
      
      if(acceptIncr)
        GetChar.fakeGotcPickupExtraChar(K_X_INCR_SEARCH_DONE);
      else if(cancel) {
        lastMatchingRE = ass.lastRE;
        GetChar.fakeGotcPickupExtraChar(K_X_SEARCH_CANCEL);
      } else
        GetChar.fakeGotcPickupExtraChar(K_X_SEARCH_FINISH);
    } finally {
      Hook.setJViBusy(false);
    }
  }
  
  /**
   * Start the entry dialog and stash the interesting info for later use
   * in doSearch().
   * <p/>
   * Note that inputSearchPattern and doSearch together
   * are like vim's do_search.
   */
  static void inputSearchPattern(CMDARG cap, int count, int options) {
    int cmdchar = cap.cmdchar;

    ass.searchPos = G.curwin.w_cursor.copy();
    ass.searchCount = count;
    ass.searchOptions = options;
    ass.dirc = (char)cmdchar;
    ass.incrSearchActive = G.p_is;
    ass.lastRE = getLastMatchingRegExp();
    
    ViCmdEntry ce = getSearchCommandEntry();
    if(ass.incrSearchActive)
        startIncrementalSearch();
    Options.kd().println("inputSearchPattern --> startCommandEntry"); //REROUTE
    Scheduler.startCommandEntry(ce, String.valueOf(ass.dirc), G.curwin, null);
  }
  
  private static Object isListener;
  private static void removeIsListener()
  {
    if(isListener != null) {
      ViCmdEntry.getEventBus().unregister(isListener);
      isListener = null;
    }
  }

  private static void startIncrementalSearch() {
      // funny aborts might leave one...
      // NEEDSWORK: Might want to use a weak listen as well
      dbg.println(INFO, "ISEARCH: startIncrementalSearch");
      removeIsListener();
      ass.searchTopLine = G.curwin.getVpTopDocumentLine();
      ass.didIncrSearch = false;
      isListener = new Object() {
        @Subscribe public void incrSearchChar(ViCmdEntry.CmdEntryChange ev)
        {
          if(ev.getSource() == getSearchCommandEntry().getTextComponent())
            EventQueue.invokeLater(Search::doIncrementalSearch);
        }
      };
      ViCmdEntry.getEventBus().register(isListener);
  }
  
  private static void stopIncrementalSearch(boolean accept) {
      removeIsListener();

      G.curwin.clearSelection(); // since it is used by incr search
      
      if(!accept)
        resetViewIncrementalSearch();
  }
  
  private static void resetViewIncrementalSearch() {
    G.curwin.setVpTopLine(ass.searchTopLine);
    G.curwin.w_cursor.set(ass.searchPos.getOffset());
  }
  
  private static void doIncrementalSearch() {
    dbg.println(FINE, "ISEARCH: doIncrementalSearch");
    Hook.setJViBusy(true);
    try {
      String pattern = getSearchCommandEntry().getCurrentEntry();
      
      if(pattern.isEmpty()) {
        lastMatchingRE = null;
        resetViewIncrementalSearch();
        return;
      }

      int rc;
      rc = do_search(ass.searchPos.copy(), null,
                     ass.dirc, pattern, ass.searchCount,
                     SEARCH_MSG
                     + SEARCH_KEEP + SEARCH_OPT + SEARCH_NOOF + SEARCH_PEEK);
      // for incr search, use java selection to show progress
      int new_pos = G.curwin.w_cursor.getOffset();
      G.curwin.setSelection(new_pos, new_pos + search_match_len);

      ass.didIncrSearch = true;
      if(rc == FAIL) {
        resetViewIncrementalSearch();
        searchitErrorMessage(null);
      }
    } catch(Exception ex) {
        LOG.log(SEVERE, null, ex);
    } finally {
      try {
        Normal.v_updateVisualState();
        ViManager.updateHighlightSearchState();
      } finally {
        Hook.setJViBusy(false);
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////
  //
  // do_search
  //

  /**
   * This is the vim interface, except there's 'tm' (yet?).
   * Most of vim's guts are not here, they are about repeating
   * the search, like for pattern followed by ';', e.g. "/foo/;?bar"
   * and probably some search offset stuff.
   * <p/>
   * Highest level string search function.
   * Search for the 'count'th occurrence of pattern 'pat' in direction 'dirc'
   *		  If 'dirc' is 0: use previous dir.
   *    If 'pat' is NULL or empty : use previous string.
   *    If 'options & SEARCH_REV' : go in reverse of previous dir.
   *    If 'options & SEARCH_ECHO': echo the search command and handle options
   *    If 'options & SEARCH_MSG' : may give error message
   *    If 'options & SEARCH_OPT' : interpret optional flags
   *    If 'options & SEARCH_HIS' : put search pattern in history
   *    If 'options & SEARCH_NOOF': don't add offset to position
   *    If 'options & SEARCH_MARK': set previous context mark
   *    If 'options & SEARCH_KEEP': keep previous search pattern
   *    If 'options & SEARCH_START': accept match at curpos itself
   *    If 'options & SEARCH_PEEK': check for typed char, cancel search
   *
   * Careful: If spats[0].off.line == TRUE and spats[0].off.off == 0 this
   * makes the movement linewise without moving the match position.
   *
   * return 0 for failure, 1 for found, 2 for found and line offset added
   */
  static int do_search(ViFPOS pos,   // not a vim arg
                       OPARG oap, char dirc, String pat,
                       int count, int options)
  {
    dbg.printf(FINE, "SEARCH: do_search: %s\n", pos);
    if(pos == null)
      pos = G.curwin.w_cursor.copy(); // start searching from here
    ViFPOS initialPos = pos.copy();   // use with setpcmark on success (!vim)

    int retval;
    String searchstr;

    Spat spat0 = spats[0];

    //
    // find direction of search
    //
    if(dirc == 0)
      dirc = spat0.off.dir;
    else {
      spat0.off.dir = dirc;
      // #if defined(FEAT_EVAL)
    }
    if((options & SEARCH_REV) != 0) {
      dirc = dirc == '/' ? '?' : '/';
    }

    // #ifdef FEAT_FOLDING
    /* If the cursor is in a closed fold, don't find another match
     * in the same fold. */
    // NEEDSWORK: folding search issue
    // #endif

end_do_search:
    {

      //
      // Repeat the search when pattern followed by ';', e.g. "/foo/;?bar".
      //
      // for (;;)
      searchstr = pat;
      if(pat == null || pat.isEmpty()) //     || *pat == dirc)
      {
        if(spats[RE_SEARCH].pat == null) {          // no previous pattern
          pat = spats[RE_SUBST].pat;
          if(pat == null) {
            Msg.emsg(Messages.e_noprevre);
            retval = 0;
            break end_do_search;
          }
          searchstr = pat;
        } else {
          // make search_regcomp() use spats[RE_SEARCH].pat
          searchstr = "";
        }
      }

      // ..... lots of offset and some SEARCH_ECHO stuff .....
      if((options & SEARCH_ECHO) != 0) {
        // simplified
        String p;
        if(searchstr == null || searchstr.isEmpty())
          p = spats[last_idx].pat;
        else
          p = searchstr;
        Msg.nmsg(String.valueOf(dirc) + p);
      }

      int rc = searchit(null, pos,
                        dirc == '/' ? FORWARD : BACKWARD,
                        searchstr,
                        count,
                        // spats[0].off.end + // this may be SEARCH_END
                        options & (SEARCH_KEEP + SEARCH_PEEK + SEARCH_HIS
                                   + SEARCH_MSG + SEARCH_START
                                   + (false ? 0 : SEARCH_NOOF)),
                        RE_LAST, 0);
      if(rc == FAIL) {
        retval = 0;
        break end_do_search;
      }
      // if(spat0.off.end && oap != null)
      //    oap.inclusive = true;  // 'e' includes last character
      retval = 1;  // pattern found

      //
      // add character and/or line offset
      //
      // if (!(options & SEARCH_NOOF) || (pat != NULL && *pat == ';'))
      //   ...
      //   retval = 2;
      //   ...
      //

      //
      // The search command can be followed by a ';' to do another search.
      // For example: "/pat/;/foo/+3;?bar"
      // This is like doing another search command, except:
      // - The remembered direction '/' or '?' is from the first search.
      // - When an error happens the cursor isn't moved at all.
      // Don't do this when called by get_address() (it handles ';' itself).
      //
      // ...

      // end of for(;;) when handling multi-part search

      if((options & SEARCH_MARK) != 0)
        setpcmark(initialPos);

      G.curwin.w_cursor.set(pos);
      G.curwin.w_set_curswant = true;
    }
// end_do_search: TARGET OF GOTO

    // if((options & SEARCH_KEEP) != 0)
    //   spat0.off = old_off;

    return retval;
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
	if (seg.fetch(col) == c)
	  break;
      }
    }
    if (typeT) {
      col -= dir;
    }
    G.curwin.w_cursor.setColumn(col);
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
    if (c == ' ' || c == '\t' || c == '\n') // DONE
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
    G.curwin.w_cursor.set(fpos);
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
	   	&& gchar_pos(fpos) == '\n') { // DONE
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
    G.curwin.w_cursor.set(fpos);
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
	    if (fpos.getColumn() == 0 && Misc01.lineempty(fpos.getLine()))
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
    G.curwin.w_cursor.set(fpos);
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
		&& Misc01.lineempty(fpos.getLine()))
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
    G.curwin.w_cursor.set(fpos);
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
	if (fpos.getColumn() == 0 && Misc01.lineempty(fpos.getLine()))
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
    G.curwin.w_cursor.set(fpos);
    return rc;
  }
  private static boolean skip_chars(int cclass, int dir, ViFPOS fpos) {
    if(Boolean.FALSE) skip_chars(0, 0); // eatme
    while (cls(fpos) == cclass)
      if ((dir == FORWARD ? inc(fpos) : dec(fpos)) == -1)
	return true;
    return false;
  }

  /////////////////////////////////////////////////////////////////////
  //
  // regualr expression handling stuff
  //

  ////////// vim comments ///////////
  //
  // This file contains various searching-related routines. These fall into
  // three groups:
  // 1. string searches (for /, ?, n, and N)
  // 2. character searches within a single line (for f, F, t, T, etc)
  // 3. "other" kinds of searches like the '%' command, and 'word' searches.
  //

  //
  // String searches
  //
  // The string search functions are divided into two levels:
  // lowest:  searchit(); uses an pos_T for starting position and found match.
  // Highest: do_search(); uses curwin->w_cursor; calls searchit().
  //
  // The last search pattern is remembered for repeating the same search.
  // This pattern is shared between the :g, :s, ? and / commands.
  // This is in search_regcomp().
  //
  // The actual string matching is done using a heavily modified version of
  // Henry Spencer's regular expression library.  See regexp.c.
  //

  // The offset for a search command is store in a soff struct
  // Note: only spats[0].off is really used
  //
  ////////// end vim comments ///////////


  static class Soffset {
    char dir = '/';
  }

  // spat in vim
  static class Spat {

    public Spat()
    {
      off = new Soffset();
    }
    String pat;
    RegExp re;     // not in vim
    boolean re_ic; // re was compiled with this ic flag
    // int magic;
    boolean no_scs;
    // struct soffset off;
    int embededVimFlags; // embedded in pattern
    Soffset off;
  }

  // keep last for RE_SEARCH, RE_SUBST
  // private static Spat[] spats = new Spat[] {new Spat(), new Spat()};
  private static final Spat[] spats = { new Spat(), new Spat() };

  // This is used to grab the pattern after search complete, for redoBuffer.
  // or for use in Search01
  static String last_search_pat() {
    return spats[last_idx].pat;
  }

  static public RegExp getLastMatchingRegExp() {
    return lastMatchingRE;
  }

  private static int last_idx;
  private static boolean rc_did_emsg;
  private static String mr_pattern;


  private static final int FORCE_CASE_IGNORE = 1;
  private static final int FORCE_CASE_EXACT = 2;
  private static final int HAS_UPPER = 4;

  /*
   * Difference from vim. The regular expression is compiled here.
   * success returns RegExp, fail returns null. Otherwise should be the same.
   * Some features, like magic, not supported.
   * NEEDSWORK: defer compilation of re; and rebuild re if wrong ign case
   *
   * === vim comment ===
   * translate search pattern for vim_regcomp()
   *
   * pat_save == RE_SEARCH: save pat in spats[RE_SEARCH].pat (normal search cmd)
   * pat_save == RE_SUBST: save pat in spats[RE_SUBST].pat (:substitute command)
   * pat_save == RE_BOTH: save pat in both patterns (:global command)
   * pat_use  == RE_SEARCH: use previous search pattern if "pat" is NULL
   * pat_use  == RE_SUBST: use previous substitute pattern if "pat" is NULL
   * pat_use  == RE_LAST: use last used pattern if "pat" is NULL
   * options & SEARCH_HIS: put search string in history
   * options & SEARCH_KEEP: keep previous search pattern
   *
   * returns FAIL if failed, OK otherwise.
   */
  static RegExp search_regcomp(String pattern,
                               int pat_save, int pat_use, int options)
  {
    Spat spat = null;
    int i;
    String cleanPattern;
    MutableInt embeddedVimFlags = new MutableInt();
    rc_did_emsg = false;
    if(pattern == null || pattern.isEmpty()) {
      if(pat_use == RE_LAST)
        i = last_idx;
      else
        i = pat_use;
      spat = spats[i];
      if(spat.pat == null) { // pattern was never defined
        if(pat_use == RE_SUBST)
          Msg.emsg(Messages.e_nopresub);
        else
          Msg.emsg(Messages.e_noprevre);
        rc_did_emsg = true;
        return null;
      }
      cleanPattern = spat.pat;
      embeddedVimFlags.setValue(spat.embededVimFlags);
      // magic = spat.magic;
      G.no_smartcase = spat.no_scs;
    } else {
      if((options & SEARCH_HIS) != 0)	// put new pattern in history
          add_to_history(HIST_SEARCH, pattern);
      cleanPattern = cleanupPattern(pattern, embeddedVimFlags,
                                    (options & SEARCH_ISCLEAN) != 0);
    }

    // jVi saves more state in spat
    // (has to do with compiled re having ignore case state)
    boolean last_no_smartcase = G.no_smartcase;
    boolean ic = ignorecase(embeddedVimFlags);

    // If the pattern has an embedded 'ignoreCase' flag, \c or \C,
    // then apply the override
    if(embeddedVimFlags.testAnyBits(FORCE_CASE_EXACT)) {
      ic = false;
    } else if(embeddedVimFlags.testAnyBits(FORCE_CASE_IGNORE)) {
      ic = true;
    } // else FORCE_CASE_NONE

    RegExp re;
    // if using saved, can the re be reused?
    if(spat != null && spat.re_ic == ic) {
      re = spat.re;
    } else {
      re = get_spat_cache(cleanPattern, ic);
      if(re == null) {
        try {
          int flags = ic ? Pattern.CASE_INSENSITIVE : 0;
          re = RegExp.create();
          re.compile(cleanPattern, flags);
        } catch(PatternSyntaxException ex) {
          Msg.emsg(ex.getMessage() + " [" + ex.getIndex() + "]" + pattern);
          re = null;
        }
      }
    }
    mr_pattern = cleanPattern;

    if(re != null) {
      if((options & SEARCH_KEEP) == 0) {
	// search or global command
	if (pat_save == RE_SEARCH || pat_save == RE_BOTH)
          save_re_pat(RE_SEARCH, cleanPattern, re,
                      embeddedVimFlags.getValue(),
                      ic, last_no_smartcase);
	// substitute or global command
	if (pat_save == RE_SUBST || pat_save == RE_BOTH)
          save_re_pat(RE_SUBST, cleanPattern, re,
                      embeddedVimFlags.getValue(),
                      ic, last_no_smartcase);
      }
    }
    return re;
  }

  /** used only to print error messages */
  static String get_search_pat()
  {
    return mr_pattern;
  }

  private static RegExp get_spat_cache(String pat, boolean ic)
  {
    for(Spat spat : spats) {
      if(spat.re_ic == ic && pat.equals(spat.pat))
        return spat.re;
    }
    return null;
  }

  private static void save_re_pat(int idx, String pat, RegExp re,
                                  int embeddedVimFlags,
                                  boolean re_ic, boolean no_smartcase)
  {
    // There's so many fields, rather than trying to figure out if its
    // safe to bypass the save if pat == spat.pat
    Spat spat = spats[idx];
    spat.pat = pat;
    spat.re = re;
    spat.embededVimFlags = embeddedVimFlags;
    spat.re_ic = re_ic;
    spat.no_scs = no_smartcase;
    last_idx = idx;
    // in vim, there's code to redraw hl search if pattern changed.
    // but inc search goes through here...
    // if(G.p_hls.getBoolean())
    //   Options.newSearch();
    // G.no_hlsearch = false;
  }

  private static boolean ignorecase(MutableInt embeddedVimFlags)
  {
    boolean ic = G.p_ic;
    if(ic && !G.no_smartcase && G.p_scs)
      ic = !embeddedVimFlags.testAnyBits(HAS_UPPER);
    G.no_smartcase = false;
    return ic;
  }

  private static final int ESCAPED_FLAG = 0x10000;
  final public static String VERY_MAGIC = "";
  final public static String MAGIC         = "()|+?{";
  final public static String NO_MAGIC      = MAGIC + ".*[";
  final public static String VERY_NO_MAGIC = NO_MAGIC + "^$";

  /** Chars that are escaped for a particular magic mode.
   * For example, VERY_MAGIC passes everything to java's RE engine;
   * MAGIC escapes {@literal '(', '|'} and such, so these characterss, often
   * found in programming languages, are
   * not used in regex pattern syntax.
   */
  final private static Map<String, String> magicMap = Map.of(
    "vm", VERY_MAGIC,
    "m", MAGIC,
    "nm", NO_MAGIC,
    "vnm", VERY_NO_MAGIC
  );
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
   * <p>isClean means that there is no vim style escaping;
   * used internally by jVi since it uses clean patterns.
   * </p>
   */
  private static String cleanupPattern(String s, MutableInt flags,
                                       boolean isClean) {
    flags.setValue(0);
    StringBuilder sb = new StringBuilder();
    boolean isEscaped = false; // set when '\' found, applies to next char

    String metacharacterEscapes;
    if(isClean)
      metacharacterEscapes = "";
    else
      metacharacterEscapes = magicMap.get(G.p_magic);

    // boolean escapeQuestion = metacharacterEscapes.indexOf('?') >= 0;

    for(int in = 0; in < s.length(); in++) {
      char c = s.charAt(in);
      if( ! isEscaped && c == '\\') {
        isEscaped = true;
        continue;
      }
      CHAR: {
        // if((c == '=') && req)
        //   c = possiblyConvertEqualToQuestion(isEscaped, escapeQuestion);
        if(metacharacterEscapes.indexOf(c) >= 0) { // metachar gets escaped
          // reverse of what was seen
          if( ! isEscaped) {
            sb.append("\\");
          }
          sb.append(c);
          break CHAR;
        }
        if(isEscaped) { // can/should probably add ' && !isClean'. \<,\> ???
          // check if it's special for pattern handling
          switch(c) {
          case '<':
          case '>':
            sb.append("\\b");
            break CHAR;
          case 'c':
            flags.setBits(FORCE_CASE_IGNORE);
            break CHAR;
          case 'C':
            flags.setBits(FORCE_CASE_EXACT);
            break CHAR;
          case 'v':
            metacharacterEscapes = magicMap.get(Options.MESC_VERY_MAGIC);
            break CHAR;
          case 'm':
            metacharacterEscapes = magicMap.get(Options.MESC_MAGIC);
            break CHAR;
          case 'M':
            metacharacterEscapes = magicMap.get(Options.MESC_NO_MAGIC);
            break CHAR;
          case 'V':
            metacharacterEscapes = magicMap.get(Options.MESC_VERY_NO_MAGIC);
            break CHAR;
          }
          // FALL THROUGH
        }
        // pass through what was seen, possibly escaped
        if(isEscaped) {
          sb.append("\\");
        } else {
          // do clean patterns take advantage of this?
          if(Character.isUpperCase(c))
            flags.setBits(HAS_UPPER);
        }
        sb.append(c);
      }
      isEscaped = false;
    }
    dbg.printf(FINE, "PATTERN: magic: %s, in %s, out %s\n", G.p_magic, s, sb);
    return sb.toString();
  }

  // private static char possiblyConvertEqualToQuestion(
  //         boolean isEscaped, boolean escapeQuestion) {
  //   // Have an '=' and that char is used to specify an optional atom.
  //   // Set useEscape if the '=' needs to be escaped to mean optional.
  //   //boolean useEscape = metacharacterEscapes.indexOf('?') >= 0;
  //   if(isEscaped && escapeQuestion
  //           || !isEscaped && !escapeQuestion) {
  //     // the '=' is intened to indicated an optional atom,
  //     // convert it to a '?'
  //     return '?';
  //   }
  //   return '='; // don't change it
  // }

  static String top_bot_msg = "search hit TOP, continuing at BOTTOM";
  static String bot_top_msg = "search hit BOTTOM, continuing at TOP";

  // HACK: who uses this?
  private static int search_match_len;
  // HACK: used by incr search
  private static RegExp lastMatchingRE;

  /**
   * lowest level search function.
   * Search for 'count'th occurrence of 'str' in direction 'dir'.
   * Start at position 'pos' and return the found position in 'pos'.
   *
   * <br/>if (options & SEARCH_MSG) == 0 don't give any messages
   * <br/>if (options & SEARCH_MSG) == SEARCH_NFMSG dont give 'notfound' messages
   * <br/>if (options & SEARCH_MSG) == SEARCH_MSG give all messages
   * <br/>if (options & SEARCH_HIS) put search pattern in history
   * <br/>if (options & SEARCH_END) return position at end of match
   * <br/>if (options & SEARCH_START) accept match at pos itself
   * <br/>if (options & SEARCH_KEEP) keep previous search pattern
   * <br/>if (options & SEARCH_FOLD) match only once in a closed fold
   * <br/>if (options & SEARCH_PEEK) check for typed char, cancel search
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
  static int searchit(TextView win,
                      // BUF,    NOT USED
                      ViFPOS pos,      // FPOS,
                      int dir,
                      String pattern,
                      int count,
                      int options,
                      int pat_use,
                      int stop_lnum   // NOT USED (yet?)
          )
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


    RegExp prog = search_regcomp(pattern, RE_SEARCH, pat_use,
                     (options & (SEARCH_HIS+SEARCH_KEEP+SEARCH_ISCLEAN)));
    if(prog == null) {
      if((options & SEARCH_MSG) != 0 && !rc_did_emsg)
        Msg.emsg("Invalid search string: " + mr_pattern);
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
          lastMatchingRE = null;
          if(prog.search(seg.array, seg.offset, seg.count)) {
            lastMatchingRE = prog;
            match = prog.start(0) - seg.offset; // column index
            matchend = prog.end(0) - seg.offset;
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
                if(             G.p_cpo_search) {
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
                  matchend = prog.end(0) - seg.offset;
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
                int matchend01 = prog.end(0) - seg.offset;
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
                if(             G.p_cpo_search) {
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
          if (G.False/*got_int*/)
            break;

          if (loop != 0 && lnum == start_pos.getLine())
            break;	    // if second loop, stop where started
        }
        at_first_line = false;

        //
        // stop the search if wrapscan isn't set, after an interrupt and
        // after a match
        ///
        if (!   G.p_ws /*|| got_int*/ || found)
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
      if (G.False/*got_int*/)
        break;
    } while (--count > 0 && found);   // stop after count matches or no match

    if (!found) {	    // did not find it
      if (G.False/*got_int*/)
        searchitErrorMessage(Messages.e_interr);
      else if ((options & SEARCH_MSG) == SEARCH_MSG) {
        if (    G.p_ws)
          searchitErrorMessage(Messages.e_patnotf2 + mr_pattern);
        else if (lnum == 0)
          searchitErrorMessage("search hit TOP without match for: " + mr_pattern);
        else
          searchitErrorMessage("search hit BOTTOM without match for: " + mr_pattern);
      }
      search_match_len = 0;
      return FAIL;
    }
    search_match_len = matchend - match;

    int new_pos = pos.getOffset();
    if(search_match_len == 0) {
        // search /$ puts cursor on end of line
        new_pos = G.curwin.validateCursorPosition(new_pos);
        if(new_pos != pos.getOffset())
          pos.set(new_pos);
    }
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
