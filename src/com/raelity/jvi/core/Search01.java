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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;

import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.raelity.jvi.ViBuffer.BIAS;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViOutputStream.FLAGS;
import com.raelity.jvi.core.Commands.ColonEvent;
import com.raelity.jvi.core.lib.Messages;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.manager.ViManager;
import com.raelity.text.RegExp;
import com.raelity.text.MySegment;

import static com.raelity.jvi.core.Edit.*;
import static com.raelity.jvi.core.JviClipboard.end_global_changes;
import static com.raelity.jvi.core.JviClipboard.start_global_changes;
import static com.raelity.jvi.core.MarkOps.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Misc01.*;
import static com.raelity.jvi.core.Ops.op_delete;
import static com.raelity.jvi.core.Search.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.text.TextUtil.sf;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Search01 {

  ////////////////////////////////////////////////////////////////
  //
  // Stuff from ex_cmds.c
  //
  
  private static int nSubMatch;
  private static int nSubChanges;
  private static int nSubLine = 0;
  
  private static String old_sub;	/* previous substitute pattern */
  private static String lastSubstituteArg;

  private static final int SUBST_ALL      = 0x0001;
  private static final int SUBST_PRINT    = 0x0002;
  private static final int SUBST_CONFIRM  = 0x0004;
  private static final int SUBST_ESCAPE   = 0x0008;
  private static final int SUBST_QUIT     = 0x0010;
  private static final int SUBST_DID_ACK  = 0x0020;

  private static MutableInt substFlags;

  private Search01() { }

  /** do_sub()
   *
   * Perform a substitution from line eap->line1 to line eap->line2 using the
   * command pointed to by eap->arg which should be of the form:
   *
   * /pattern/substitution/{flags}
   *
   * The usual escapes are supported as described in the regexp docs.
   */
  static void substitute(ColonEvent cev)
  {
    boolean newFlags = false;
    if(!G.global_busy || substFlags == null) {
      substFlags = new MutableInt();
      newFlags = true;
    }

    // which_pat = eap->cmdidx == CMD_tilde ? RE_LAST : RE_SUBST;
    int which_pat = RE_SUBST;
    boolean do_error = true;

    String pattern = null;
    char delimiter;
    CharSequence substitution;
    RegExp prog;

    // The sub command is a little weird
    // "s c" or "sub c" means use previous pattern/sub with 'c' as flags
    // need to see " c" (not the space before the 'c')
    // So grab the whole thing.
    String cmd = cev.getArgString();

    int sidx = 0;
    int sidx01;
    if(cev.getArg(0).charAt(0) == 's' && cmd != null && cmd.length() > 0
            && !vim_iswhite(cmd.charAt(0))
            && vim_strchr("0123456789cegriIp|\"", 0, cmd.charAt(0)) < 0) {
      if(isalpha(cmd.charAt(sidx))) {
        Msg.emsg("Regular expressions can't be delimited by letters");
        return;
      }
      //
      // undocumented vi feature:
      //  "\/sub/" and "\?sub?" use last used search pattern (almost like
      //  //sub/r).  "\&sub&" use last substitute pattern (like //sub/).
      //
      if (cmd.charAt(sidx) == '\\') {
        ++sidx;
        if (vim_strchr("/?&", 0, cmd.charAt(sidx)) < 0)
        {
          Msg.emsg(Messages.e_backslash);
          return;
        }
        if (cmd.charAt(sidx) != '&')
          which_pat = RE_SEARCH;	    // use last '/' pattern
        pattern = "";	        	    // empty search pattern
        delimiter = cmd.charAt(sidx);	    // remember delimiter character
        ++sidx;
      }
      else		// find the end of the regexp
      {
        which_pat = RE_LAST;    	    // use last used regexp
        delimiter = cmd.charAt(sidx);	    // remember delimiter character
        ++sidx;
        sidx01 = sidx;		    // remember start of search pat
        sidx = skip_regexp(cmd, sidx, delimiter, true);
        if(sidx01 != sidx)
          pattern = cmd.substring(sidx01, sidx);
      }

      //
      // HUH?
      // Small incompatibility: vi sees '\n' as end of the command, but in
      // Vim we want to use '\n' to find/substitute a NUL.
      //
      sidx++;               // first char of substitution
      sidx01 = sidx;	    // remember the start of the substitution
      for( ; sidx < cmd.length(); sidx++) {
        char c = cmd.charAt(sidx);
        if(c == delimiter) {		// end delimiter found
          break;
        }
        if(c == '\\' && sidx+1 < cmd.length()) {  // skip escaped characters
          ++sidx;
        }
      }
      if(sidx01 == sidx) {
        old_sub = "";
      } else {
        old_sub = cmd.substring(sidx01, sidx);
      }
      substitution = old_sub;
    } else {
      // use previous pattern and substitution
      //cmd = lastSubstituteArg;
      if(old_sub == null) {	// there is no previous command
        substFlags.setBits(SUBST_QUIT);
	Msg.emsg(Messages.e_nopresub);
	return;
      }
      pattern = null;	// search_regcomp() will use previous pattern
      substitution = old_sub;
    }

    //RegExp prog = null;
    //CharSequence substitution;
    //char delimiter = cmd.charAt(0);

    int cursorLine = 0; // set to line number of last change

    if(cmd != null && newFlags) {
      //
      // pick up the flags
      //
                // NEEDSWORK: || lastSubstitution.indexOf('~', sidx01) != -1;
      if(old_sub.indexOf('\\') != -1
                  || old_sub.indexOf('&') != -1)
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
    
    prog = search_regcomp(pattern, RE_SUBST, which_pat, SEARCH_HIS);
    if(prog == null) {
      if(do_error)
        Msg.emsg(Messages.e_invcmd);
      substFlags.setBits(SUBST_QUIT);
      return;
    }
    if(newFlags)
      Options.newSearch();

    int line1 = cev.getLine1();
    int line2 = cev.getLine2();
    StringBuilder sb;
    if(! G.global_busy) {
      nSubLine = 0;
      nSubMatch = 0;
      nSubChanges = 0;
    }

    // Mark the beginning of each line in case extra lines are added
    // (unlike the 'g' command, doGlobal, not checking if lines are deleted)

    List<ViMark> marks = new ArrayList<>();
    for(int lnum = line1; lnum <= line2; lnum++) {
      marks.add(G.curbuf.createMark(G.curbuf.getLineStartOffset(lnum),
                                    BIAS.FORW));
    }

    for(ViMark m : marks) {
      if(substFlags.testAnyBits(SUBST_QUIT))
        break;
      int i = G.curbuf.getLineNumber(m.getOffset());
      int nChange = substitute_line(prog, i, substFlags, substitution);
      if(nChange > 0) {
        nSubChanges += nChange;
        cursorLine = i;  // keep track of last line changed
        nSubLine++;
        if(substFlags.testAnyBits(SUBST_PRINT)) {
          ExCommands.outputPrint(i, 0, 0);
        }
      }

    }

    if(! G.global_busy) {
      if(cursorLine > 0) {
        ViFPOS fpos = fpos();
        fpos.set(cursorLine, 0);
        beginline(fpos, BL_SOL | BL_FIX).copyTo(G.curwin.w_cursor);
      }
      if(nSubMatch == 0) {
	Msg.emsg(Messages.e_patnotf2 + get_search_pat());
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
    if(nSubChanges >= G.p_report) {
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
  private static int substitute_line(RegExp prog,
                             int lnum,
                             MutableInt flags,
                             CharSequence subs)
  {
    MySegment seg = G.curbuf.getLineSegment(lnum);

    StringBuilder sb = null;
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
        // NEEDSWORK: LOOP? not exactly what vim's ask_yesno type thing does.
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
                beep_flush();
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
          sb = new StringBuilder();
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
  private static CharSequence translateSubstitution(RegExp prog,
                                   MySegment line,
                                   StringBuilder sb,
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
                // as vim, impossible backref or backref didn't match are empty
                if(group <= prog.nGroup() && prog.group(group) != null)
                  sb.append(line.array, prog.start(group), prog.length(group));
                break;
              case 'n':
                // Treat \n like \r, add a newline to the file.
                // So don't put NULL into the file for \n
                // sb.append('\000');
                // break;
              case 'r':
                sb.append('\n');
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
  static void global(ColonEvent cev) {
    G.global_busy = true;
    // v7.4.396
    start_global_changes();
    try {
      doGlobal(cev);
    }
    finally {
      G.global_busy = false;
      end_global_changes();
    }
  }
  
  static void doGlobal(ColonEvent cev) {
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
    RegExp prog;
    char delim;
    char c;
    MySegment line;
    int cursorLine = 0; // set to line number of last found line
    int sidx = 0;

    char type = cev.getArg(0).charAt(0);
    if(cev.isBang()) // must be g!
      type = 'v';
    assert type == 'g' || type == 'v';
    int which_pat = RE_LAST;            // default: use last used regexp

    //
    // pick up the pattern
    //

    //
    // undocumented vi feature:
    //	"\/" and "\?": use previous search pattern.
    //		 "\&": use previous substitute pattern.
    //

    if(cmd.isEmpty()) {
      Msg.emsg("Regular expression missing from global");
      return;
    }
    if(cmd.charAt(sidx) == '\\') {
      ++sidx;
      c = cmd.charAt(sidx);
      if(vim_strchr("/?&", 0,c) < 0) {
        Msg.emsg(Messages.e_backslash);
        return;
      }
      which_pat = c == '&' ? RE_SUBST : RE_SEARCH;
      ++sidx;
      pattern = "";
    } else {
      delim = cmd.charAt(sidx);         // get the delimiter
      ++sidx;
      int sidx01 = sidx;
      sidx = skip_regexp(cmd, sidx, delim, true);
      if(sidx01 != sidx) {
        pattern = cmd.substring(sidx01, sidx);
      }
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

    prog = search_regcomp(pattern, RE_BOTH, which_pat, SEARCH_HIS);
    if(prog == null) {
      Msg.emsg(Messages.e_invcmd);
      return;
    }

    // figure out what command to execute for all the indicated lines
    
    ActionListener cmdAction = null;
    ColonEvent cevAction = null;

    if(cmdExec.isEmpty()) {
      // if no command specified then "p" command
      cmdAction = Cc01.getActionPrint();
    } else {
      cevAction = ExCommands.parseCommand(cmdExec);
      if(cevAction != null) {
	cmdAction = cevAction.getAction();
      }
    }

    // if no address range then entire file
    if(cev.getAddrCount() == 0) {
      cev.line1 = 1;
      cev.line2 = G.curbuf.getLineCount();
    }
      
    // for now special case a few known commands that can be global'd
    // 
    // As in vim, do this in two passes. First pass build list of line marks
    // that match the pattern. Second pass operate on those lines.
    //
    // Potential future problem is that multiple lines could be deleted and
    // a line, other than the current line, with a mark is deleted.
    // This situation is not detected currently.
    // The vim doc says: If a line is deleted its mark disappears.
    //
    
    ViOutputStream result = null;
    if(cmdAction == Cc01.getActionPrint()) {
      result = ViManager.createOutputStream(G.curwin, ViOutputStream.SEARCH,
              get_search_pat(),
              EnumSet.of(FLAGS.NEW_YES, FLAGS.RAISE_YES, FLAGS.CLEAR_NO));
    } else {
      // Assume it will be handled by the command
      //result = ViManager.createOutputStream(G.curwin,
      //                                      ViOutputStream.LINES,
      //                                      get_search_pat());
    }
    
    substFlags = null;

    List<ViMark> marks = new ArrayList<>();
    List<String> debugInfo = null;
    if(G.dbgSearch.getBoolean())
      debugInfo = new ArrayList<>();

    // Set up two marks for each line, first char and after newline.
    // Use two marks/line to detect if a line is deleted.
    for(int lnum = cev.getLine1(); lnum <= cev.getLine2(); lnum++) {
      line = G.curbuf.getLineSegment(lnum);
      boolean match = prog.search(line.array, line.offset, line.count);
      if(type == 'g' && match || type == 'v' && !match) {
        marks.add(G.curbuf.createMark(line.docOffset, BIAS.FORW));
        marks.add(G.curbuf.createMark(line.docOffset + line.count, BIAS.FORW));
        if(debugInfo != null) {
          debugInfo.add(new String(line.array, line.offset, lineLength(line)));
        }
      }
    }

    G.dbgSearch.printf(":global: %d lines match\n", marks.size() >> 1);
    Iterator<String> debugIt = null;
    if(debugInfo != null)
      debugIt = debugInfo.iterator();

    // second pass, perform the actions on the matching lines
    for(Iterator<ViMark> it = marks.iterator(); it.hasNext();) {
      ViMark m = it.next();
      ViMark mEnd = it.next();
      String debugText = null;
      if(debugIt != null)
        debugText = debugIt.next();

      // skip lines deleted by ":global" actions so far
      if(m.getOffset() == mEnd.getOffset()) {
        String debugTextF = debugText;
        G.dbgSearch.printf(() -> sf(":global: line deleted after %d: %s\n",
                     G.curbuf.createFPOS(m.getOffset()).getLine(), debugTextF));
        continue;
      }

      int lnum = G.curbuf.getLineNumber(m.getOffset());
      // if full parse each time command executed,
      // then should move cursor (or equivilent) but.....
      if(cevAction != null) {
        cevAction.line1 = lnum;
        cevAction.line2 = lnum;
      }
      if(cmdAction == Cc01.getActionPrint()) {
        result.println(lnum, 0, 0);
      } else if(cmdAction == Cc01.getActionSubstitute()) {
        ExCommands.executeCommand(cevAction);
        if(substFlags != null && substFlags.testAnyBits(SUBST_QUIT))
          break;
      } else if(cmdAction == Cc01.getActionDelete()) {
        OPARG oa = ExCommands.setupExop(cevAction, true);
        oa.op_type = OP_DELETE;
        op_delete(oa);
      } else if(cmdAction == Cc01.getActionGlobal()) {
        Msg.emsg("Cannot do :global recursively");
        return;
      } else {
        // no command specified, but cursorLine is getting set above
      }
      cursorLine = lnum;  // keep track of last line matched
    }


    if(cursorLine > 0) {
      ViFPOS fpos = fpos();
      fpos.set(cursorLine, 0);
      beginline(fpos, BL_SOL | BL_FIX).copyTo(G.curwin.w_cursor);
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
  private static final String REGEXP_INRANGE = "]^-\\";
  /**
   * REGEXP_ABBR contains all characters which act as abbreviations after '\'.
   * These are:
   *  <ul>
   * <li> \r	- New line (CR).
   * <li> \t	- Tab (TAB).
   * <li> \e	- Escape (ESC).
   * <li> \b	- Backspace (Ctrl('H')).
   */
  private static final String REGEXP_ABBR = "rteb";

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
    char c;
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
      switch (c) {
      case '-':
        ++sidx;
        if(sidx < s.length() && (c = s.charAt(sidx)) != ']') {
          ++sidx;
        }
        break;
      case '\\':
        if(sidx+1 < s.length()) {
          char c2 = s.charAt(sidx+1);
          if(vim_strchr(REGEXP_INRANGE, 0, c2) >= 0
                  || (!cpo_lit && vim_strchr(REGEXP_ABBR, 0, c2) >= 0)) {
            sidx += 2;
          }
        }
        break;
      case '[':
        MutableInt mi = new MutableInt(sidx);
        if (skip_class_name(s, mi) == null) {
          ++sidx; /* It was not a class name */
        } else {
          sidx = mi.getValue();
        }
        break;
      default:
        ++sidx;
        break;
      }
    }

    return sidx;
  }

  /*
   * findsent(dir, count) - Find the start of the next sentence in direction
   * "dir" Sentences are supposed to end in ".", "!" or "?" followed by white
   * space or a line break. Also stop at an empty line.
   * Return OK if the next sentence was found.
   */
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

found: {
        if (Misc.gchar_pos(pos) == '\n') { // DONE
          do {
            if (Misc.inclDeclV7(pos, dir) == -1)
              break;
          } while (Misc.gchar_pos(pos) == '\n'); // DONE

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
          (dir == BACKWARD && vim_strchr(".!?)]\"'", 0, c) >= 0)) {
          if (vim_strchr(".!?", 0, c) >= 0) {
            // Only skip over a '.', '!' and '?' once.
            if (found_dot)
              break;
            found_dot = true;
          }
          if (Misc.decl(pos) == -1)
            break;
          // when going forward: Stop in front of empty line
          if (lineempty(pos.getLine()) && dir == FORWARD) {
            Misc.inclV7(pos);
            break found;
          }
        }

        // remember the line where the search started
        startlnum = pos.getLine();
        cpo_J = G.p_cpo_j;

        for (;;) {
          c = Misc.gchar_pos(pos);
          if (c == '\n' || // DONE
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
            while (vim_strchr(")]\"'", 0, (c = Misc.gchar_pos(tpos))) >= 0);

            if (i == -1 || (!cpo_J && (c == ' ' || c == '\t')) || c == '\n' // DONE
                || (cpo_J && (c == ' ' && Misc.inc(tpos) >= 0
                    && Misc.gchar_pos(tpos) == ' '))) {
              pos = tpos;
              if (Misc.gchar_pos(pos) == '\n') // skip '\n' at EOL // DONE
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
      }

      while (!noskip && ((c = Misc.gchar_pos(pos)) == ' ' || c== '\t'))
        if (Misc.inclV7(pos) == -1) break;
    }
    setpcmark();
    G.curwin.w_cursor.set(pos);
    return true;
  }

  static boolean findpar(CMDARG oap, int dir, int count, int what,
    boolean both) {

    int curr = G.curwin.w_cursor.getLine();

    while (count-- > 0) {
      boolean did_skip = false; //TRUE after separating lines have been skipped 
      boolean first = true;     // TRUE on first line 
      do {
        if (!lineempty(curr))
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

    setpcmark();

    if (both && !lineempty(curr) && ml_get(curr).charAt(0) == '}')
      ++curr;

    int offset = 0;

    if (curr == G.curbuf.getLineCount()) {
      offset = lineLength(curr);
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
   * startPS: return TRUE if line 'lnum' is the start of a section or paragraph.
   * If 'para' is '{' or '}' only check for sections.
   * If 'both' is TRUE also stop at '}'
   */
  static boolean startPS(int /*linenr_t*/lnum, int para, boolean both) {
    MySegment seg = ml_get(lnum);
    // if seg.count == 1, then only a \n, ie empty line
    char s = seg.count > 1 ? seg.fetch(0) : 0; // DONE
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