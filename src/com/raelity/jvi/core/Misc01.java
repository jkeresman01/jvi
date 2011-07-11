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

import com.raelity.jvi.ViTextView.Direction;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViAppView;
import com.raelity.jvi.ViTextView.FOLDOP;
import com.raelity.jvi.ViTextView.Orientation;
import com.raelity.jvi.ViTextView.SIZOP;
import com.raelity.jvi.ViWindowNavigator;
import com.raelity.jvi.manager.AppViews;
import com.raelity.jvi.manager.ViManager;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.List;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.Edit.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;
import static com.raelity.jvi.core.Misc.*;

/**
 * do_window is some stuff from window.c and related.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Misc01 
{
    /////////////////////////////////////////////////////////////////////////
    //
    // goto line, screen line up/down, viewport handling
    //

  /** This method not in vim,
   * but we cant just set a line number in the window struct.
   * If the target line is within one half screen of being visible
   * then scroll to the line and the line will be near the top
   * or bottom as needed, otherwise center the target line on the screen.
   */
  static void gotoLine(int line, int flag) {
    gotoLine(line, flag, false);
  }
  static void gotoLine(int line, int flag, boolean openFold) {
    if(line > G.curbuf.getLineCount())
        line = G.curbuf.getLineCount();
    int logicalLine = G.curwin.getLogicalLine(line);
    if(openFold) {
      int offset = G.curwin.getDocLineOffset(logicalLine);
      int bufferLine = G.curbuf.getLineNumber(offset);
      if(bufferLine != line) {
        offset = G.curbuf.getLineStartOffset(line);
        // System.err.println("LINE " + line + "-->" + bufferLine);
        G.curwin.foldOperation(FOLDOP.MAKE_VISIBLE, offset);
        // now that the fold is open, it should have moved on screen
        logicalLine = G.curwin.getLogicalLine(line);
      }
    }
    gotoLogicalLine(logicalLine, flag);
    return;
  }

  static ViFPOS fpos()
  {
      return G.curwin.w_cursor.copy();
  }

  static ViFPOS fposLogicalLine(int logicalLine)
  {
    ViFPOS fpos = G.curwin.w_cursor.copy();
    return fposLogicalLine(fpos, logicalLine);
  }

  static ViFPOS fposLogicalLine(ViFPOS fpos, int logicalLine)
  {
    fpos.set(G.curwin.getDocLine(logicalLine), 0);
    return fpos;
  }

  /**
   * @return scrolloff possibly adjusted for window size
   */
  static int getScrollOff() {
    int halfLines = G.curwin.getVpLines()/2; // max distance from center
    int so = G.p_so;
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
  static int adjustTopLogicalLine(int topLogicalLine) {
    int nLinesRequiredOnScreen = G.curwin.getRequiredVpLines();
    // nLinesAfterTop includes the top line
    // NOTE: the '+1' in the compare is not there in the previous
    //       so the case for LogicalLines == RequireddisplayLines is different.
    //       top + VL > LC  vs RL > LC - top + 1
    //                         top + RL > LC + 1
    int nLinesAfterTop = G.curwin.getLogicalLineCount() - topLogicalLine + 1;
    if(nLinesAfterTop < nLinesRequiredOnScreen)
      topLogicalLine = G.curwin.getLogicalLineCount() - nLinesRequiredOnScreen + 1;
    //if(top + G.curwin.getLogicalLines() > G.curwin.getCoordLineCount()) {
    //  top = G.curwin.getCoordLineCount() - G.curwin.getLogicalLines() + 1;
    //}
    if(topLogicalLine < 1) {
      topLogicalLine = 1;
    }
    return topLogicalLine;
  }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Coordinate Skipping
    //
    // This is all about handling code folding
    //
    // When doing relative navigation thoughout the file, eg up/downarrow ^D ^F,
    // the move needs to be relative to the Document/JViewport coordinate
    // system. Compution are about screen lines , not file lines.
    //
    // One approach is to compute number of lines to scroll up/down and the
    // number of lines from the top to put the cursor, or something like that;
    // give those numbers to the text view and let it figure out what to do.
    // Another approach is to work more in coordinate, but not sure what that
    // would look like.
    //
    // Another approach is to keep the algorithms identical to what they are
    // now, tweaked so that the the line numbers being used are coord/display
    // line numbers rather than file line numbers. So adjust methods like
    // GetLineCount and GetViewTopLine so they return values based on what's
    // displayed.
    //
    // Another approach is to do things in terms coordinates. For example,
    // ^D is half a screen down; do it all in coords.
    //
    // Some issues.
    //   - magic caret position in vim is char offset in line. May
    //     need to *also* keep x-coord position. Translation of char positon
    //     to Point is tricky, because char at offset 0 doesn't start at x-coord
    //     0. But the offset is probably fixed, like "2", for a given session
    //     so using "2 + charoffset * char-width" is a shortcut for monospace
    //     fonts.
    //   - scroll off option might be tricky
    //   - variable height fonts offer problems.
    //

    /**
     * based on gotoLine_scrolloff
     * This method not in vim,
     * but we cant just set a line number in the window struct.
     * If the target line is within one half screen of being visible
     * then scroll to the line and the line will be near the top
     * or bottom as needed, otherwise center the target line on the screen.
     */
    static void gotoLogicalLine(int logicalLine, int flag)
    {
      if ( G.dbgCoordSkip.getBoolean(Level.FINE) ) {
            G.dbgCoordSkip.println(Level.FINE, String.format(
                 "gotoLogicalLine: logicalLine %d, flag 0x%x",
                 logicalLine, flag));
      }
      if(logicalLine < 1)
        logicalLine = 1;
      if(logicalLine > G.curwin.getLogicalLineCount())
        logicalLine = G.curwin.getLogicalLineCount();

      // if target line is less than half a screen away from
      // being visible, then just let it scroll, otherwise
      // center the target line

      int curTop = G.curwin.getVpTopLogicalLine();
      int vpLines = G.curwin.getVpLines();
      int so = getScrollOff();
      int center = curTop + vpLines / 2 - 1;

      // reduce scrollMargin, the distance from center that we will scroll
      // the screen, by amount of scrolloff.
      int scrollMargin = vpLines - so; // max distance from center to do scroll

      int newTop = curTop;
      int newViewTop = -1; // by default don't use this
      if(logicalLine < center - scrollMargin - 1
              || logicalLine > center + scrollMargin) {
        newTop = logicalLine - (vpLines / 2);
        if((vpLines & 1) == 0) {
          ++newTop; // even num lines, put target in upper half
        }
        // center the target line
      } else {
        // scroll to the line
        if(logicalLine < curTop+so) {
          newTop = logicalLine-so;
        } else if(logicalLine > G.curwin.getVpBottomLogicalLine()-so-1) {
          //newTop = logicalLine-vpLines+1+so;
          newViewTop = G.curwin.getViewLineFromLogicalLine(logicalLine)
                        - vpLines + 1 + so;
        }
      }
      if(newViewTop >= 0)
          G.curwin.setVpTopViewLine(newViewTop);
      else
          G.curwin.setVpTopLogicalLine(adjustTopLogicalLine(newTop));
      ViFPOS fpos = fposLogicalLine(logicalLine);
      if(flag < 0) {
        coladvance(fpos, G.curwin.w_curswant).copyTo(G.curwin.w_cursor);
      } else {
        // from nv_goto
        beginline(fpos, flag).copyTo(G.curwin.w_cursor);
      }

    }

    /**
     * Move screen 'count' pages up or down and update screen.
     *<br/>
     * return FAIL for failure, OK otherwise
     */
    static int onepage(int dir, int count) {
      int	    lp;
      int	    n;
      int	    off;
      int	    retval = OK;
      int newtopline = -1;
      int newcursorline = -1;


      if (G.curwin.getLogicalLineCount() == 1) { // nothing to do
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
                ? ((G.curwin.getVpTopLogicalLine()
                                   >= G.curwin.getLogicalLineCount() - so)
                    && G.curwin.getVpBottomLogicalLine()
                                   > G.curwin.getLogicalLineCount())
                : (G.curwin.getVpTopLogicalLine() == 1)) {
          Util.beep_flush();
          retval = FAIL;
          break;
        }

        // the following test is added because with swing there can not be
        // blank lines on the screen, so we can go no more when the cursor
        // is positioned at the last line.
        if (dir == FORWARD
                && G.curwin.getLogicalLine(G.curwin.w_cursor.getLine())
                      == G.curwin.getLogicalLineCount()) {
          Util.beep_flush();
          retval = FAIL;
          break;
        }

        if (dir == FORWARD) {
          // at end of file
          if(G.curwin.getVpBottomLogicalLine()
                  > G.curwin.getLogicalLineCount()) {
            newtopline = G.curwin.getLogicalLineCount();
            newcursorline = G.curwin.getLogicalLineCount();
            // curwin->w_valid &= ~(VALID_WROW|VALID_CROW);
          } else {
            lp = G.curwin.getVpBottomLogicalLine();
            off = get_scroll_overlap(lp, -1);
            newtopline = lp - off;
            newcursorline = newtopline + so;
            // curwin->w_valid &= ~(VALID_WCOL|VALID_CHEIGHT|VALID_WROW|
            // VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
          }
        } else {	// dir == BACKWARDS
          lp = G.curwin.getVpTopLogicalLine() - 1;
          off = get_scroll_overlap(lp, 1);
          lp += off;
          if (lp > G.curwin.getLogicalLineCount())
            lp = G.curwin.getLogicalLineCount();
          newcursorline = lp - so;
          n = 0;
          while (n <= G.curwin.getVpLines() && lp >= 1) {
            n += plines(lp);
            --lp;
          }
          if (n <= G.curwin.getVpLines()) {	    // at begin of file
            newtopline = 1;
            // curwin->w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE);
          } else if (lp >= G.curwin.getVpTopLogicalLine() - 2) {
            // very long lines
            newtopline = G.curwin.getVpTopLogicalLine() - 1;
            comp_botline();
            newcursorline = G.curwin.getVpBottomLogicalLine() - 1;
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
        G.curwin.setVpTopLogicalLine(adjustTopLogicalLine(newtopline));
      }
      ViFPOS fpos = G.curwin.w_cursor.copy();
      if(newcursorline > 0) {
        fposLogicalLine(fpos, newcursorline);
      }

      cursor_correct(fpos);	// NEEDSWORK: implement
      beginline(fpos, BL_SOL | BL_FIX).copyTo(G.curwin.w_cursor);
      // curwin->w_valid &= ~(VALID_WCOL|VALID_WROW|VALID_VIRTCOL);

    /*
     * Avoid the screen jumping up and down when 'scrolloff' is non-zero.
     */
      //if (dir == FORWARD && G.curwin.getWCursor().getLine()
      //                            < G.curwin.getViewTopLine() + so) {
      //  // scroll_cursor_top(1, FALSE);	// NEEDSWORK: onepage ("^f") cleanup
      //}
      if (dir == FORWARD
          && G.curwin.getLogicalLine(G.curwin.w_cursor.getLine())
                                  < G.curwin.getVpTopLogicalLine() + so) {
        // scroll_cursor_top(1, FALSE);	// NEEDSWORK: onepage ("^f") cleanup
      }


      update_screen(VALID);
      return retval;
    }

    // This is identical to halfpage, except that the methods called in
    // curwin are the 'coord' variety, plus a little cursor fiddling.
    static void halfpage(boolean go_down, int Prenum) {

      int		scrolled = 0;
      int		i;
      int		n;
      int		room;

      int newtopline = -1;
      int newbotline = -1;
      int newcursorline = -1;

      final ViFPOS cursor = G.curwin.w_cursor;

      if (Prenum != 0)
        G.curwin.w_p_scr = (Prenum > G.curwin.getVpLines())
                ? G.curwin.getVpLines() : Prenum;
      n = (G.curwin.w_p_scr <= G.curwin.getVpLines())
          ?  G.curwin.w_p_scr : G.curwin.getVpLines();

      validate_botline();
      room = G.curwin.getVpBlankLines();
      newtopline = G.curwin.getVpTopLogicalLine();
      newbotline = G.curwin.getVpBottomLogicalLine();
      //COORD CHANGED: newcursorline = cursor.getLine();
      newcursorline = G.curwin.getLogicalLine(cursor.getLine());
      if (go_down) {	    // scroll down
        while (n > 0 && newbotline <= G.curwin.getLogicalLineCount()) {
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
          } while (newbotline <= G.curwin.getLogicalLineCount());

          if (newcursorline < G.curwin.getLogicalLineCount()) {
            ++newcursorline;
            // curwin->w_valid &= ~(VALID_VIRTCOL|VALID_CHEIGHT|VALID_WCOL);
          }
        }

        //
        // When hit bottom of the file: move cursor down.
        //
        if (n > 0) {
          newcursorline += n;
          if(newcursorline > G.curwin.getLogicalLineCount()) {
            newcursorline = G.curwin.getLogicalLineCount();
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
      G.curwin.setVpTopLogicalLine(newtopline);
      ViFPOS fpos = fposLogicalLine(newcursorline);
      cursor_correct(fpos);
      beginline(fpos, BL_SOL | BL_FIX).copyTo(G.curwin.w_cursor);
      update_screen(VALID);

    }

    //////////////////////////////////////////////////////////////////
    //
    // "screen.c"
    //

    static void update_curswant() {
      if (G.curwin.w_set_curswant) {
        //int vcol = getvcol();
        // G.curwin.setWCurswant(G.curwin.getWCursor().getColumn());
        MutableInt mi = new MutableInt();
        getvcol(G.curwin, G.curwin.w_cursor, null, mi, null);
        G.curwin.updateCurswant(G.curwin.w_cursor, mi.getValue());
        G.curwin.w_set_curswant = false;
      }
    }

  /**
   * Decide how much overlap to use for page-up or page-down scrolling.
   * This is symmetric, so that doing both keeps the same lines displayed.
   */
    private static int get_scroll_overlap(int lnum, int dir) {
      int		h1, h2, h3, h4;
      int		min_height = G.curwin.getVpLines() - 2;

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

    /**
     * plines_check(p) - like plines(), but return MAXCOL for invalid lnum.
     */
    static int plines_check(int logicalLine) {
        if (logicalLine < 1 || logicalLine > G.curbuf.getLineCount())
            return MAXCOL;
        return G.curwin.getCountViewLines(logicalLine);
        // return plines_win(curwin, p); // IN plines_check, assume nowrap
    }

    /**
     * plines(p) - return the number of physical screen lines taken by line 'p'.
     */
    static int plines(int logicalLine) {
        return G.curwin.getCountViewLines(logicalLine);
        // return plines_win(curwin, p); // IN plines_check, assume nowrap
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
    static void cursor_correct(ViFPOS fpos) {
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

    /////////////////////////////////////////////////////////////////////////
    //
    // Ctrl-W window splitting/moving/navigation stuff
    //
    static void do_window(char nchar, int Prenum)
    {
        boolean ok = true;
        switch (nchar) {
            case '=':
                win_size(SIZOP.SAME, null, 0, false);
                break;
            case '-':
                win_size(SIZOP.ADJUST, Orientation.UP_DOWN, Prenum, true);
                break;
            case '+':
                win_size(SIZOP.ADJUST, Orientation.UP_DOWN, Prenum, false);
                break;
            case '_':
            case '_' & 0x1f:            // Ctrl
                win_size(SIZOP.SET, Orientation.UP_DOWN, Prenum, false);
                break;
            case '<':
                win_size(SIZOP.ADJUST, Orientation.LEFT_RIGHT, Prenum, true);
                break;
            case '>':
                win_size(SIZOP.ADJUST, Orientation.LEFT_RIGHT, Prenum, false);
                break;
            case '|':
                win_size(SIZOP.SET, Orientation.LEFT_RIGHT, Prenum, false);
                break;

            // split current window in two parts
            case 'S':
            case 's':
            case 'S' & 0x1f:          // Ctrl
                win_split(Orientation.UP_DOWN, Prenum, null);
                break;

            case 'v':
            case 'V' & 0x1f:          // Ctrl
                win_split(Orientation.LEFT_RIGHT, Prenum, null);
                break;

            // close current window
            case 'c':
            case 'C' & 0x1f:
                GetChar.stuffReadbuff(":close\n");
                break;

            // close all but current window
            case 'O':
            case 'o':
                //
                GetChar.stuffReadbuff(":only\n");
                break;

            // quit current window */
            case 'Q' & 0x1f:
            case 'q':
                G.curwin.win_quit();
                break;

            // cursor to next window with wrap around
            case 'W' & 0x1f:
            case 'w':
                ok = win_jump_forw(AppViews.ALL, Prenum);
                break;

            // cursor to prev window with wrap around
            case 'W':
                ok = win_jump_back(AppViews.ALL, Prenum);
                break;

            // cursor to next nomadic editor with wrap around
            case 'E' & 0x1f:
            case 'e':
                ok = win_jump_forw(AppViews.NOMAD, Prenum);
                break;

            // make a clone
            case 'T':   win_clone(); break;

            // move the current window
            case 'J':   win_move(Direction.DOWN, Prenum);  break;
            case 'K':   win_move(Direction.UP, Prenum);    break;
            case 'H':   win_move(Direction.LEFT, Prenum);  break;
            case 'L':   win_move(Direction.RIGHT, Prenum); break;

            // cursor to window below
            case K_DOWN:
            case 'J' & 0x1f:
            case 'j':
                ok = win_jump(Direction.DOWN, Prenum);
                break;

            // cursor to window above
            case K_UP:
            case 'K' & 0x1f:
            case 'k':
                ok = win_jump(Direction.UP, Prenum);
                break;

            // cursor to left window
            case K_LEFT:
            case K_BS:
            case 'H' & 0x1f:
            case 'h':
                ok = win_jump(Direction.LEFT, Prenum);
                break;

            // cursor to right window
            case K_RIGHT:
            case 'L' & 0x1f:
            case 'l':
                ok = win_jump(Direction.RIGHT, Prenum);
                break;

            // cursor to top-left window
            case 't':
            case 'T' & 0x1f:
                ok = win_jump_to(AppViews.ALL, 1); // firstwin
                break;

            // cursor to bottom-right window
            case 'b':
            case 'B' & 0x1f:
                ok = win_jump_to(AppViews.ALL, Integer.MAX_VALUE); // lastwin
                break;

            // cursor to last accessed (previous) window
            case 'p':
            case 'P' & 0x1f:
                // Handle like :e#
                ViAppView av = AppViews.getMruAppView(1);
                ok = av != null ? ViManager.getFS().edit(av, false) : false;
                break;

            default:
                ok = false;
                break;
        }
        if(!ok)
            Util.beep_flush();
    }

    private static boolean win_jump_forw(AppViews whichViews, int n)
    {
        boolean ok = true;
        ok = n == 0 ? win_cycle(whichViews, FORWARD)
                    : win_jump_to(whichViews, n);
        return ok;
    }

    private static boolean win_jump_back(AppViews whichViews, int n)
    {
        boolean ok = true;
        ok = n == 0 ? win_cycle(whichViews, BACKWARD)
                    : win_jump_to(whichViews, n);
        return ok;
    }

    private static boolean win_cycle(AppViews whichViews, int n)
    {
        // n should be 1 or -1
        n = n < 0 ? -1 : 1;

        List<ViAppView> avs = getSortedAppViews(whichViews);
        if(avs == null)
            return false;

        int idx = AppViews.indexOfCurrentAppView(avs);

        boolean foundInList = idx >= 0;

        if(avs.isEmpty() || foundInList && avs.size() == 1) {
            // nowhere to go
            return false;
        }

        if(!foundInList) {
            // if current  wasn't in the list, then arange to go to
            // the first
            idx = 0;
        } else {
            // get the window after/before the current window in the list
            idx += n; // n is +/- 1
            if(idx >= avs.size())
                idx = 0;
            if(idx < 0)
                idx = avs.size() -1;
        }

        return ViManager.getFS().edit(avs.get(idx), false);
    }

    private static boolean win_jump_to(AppViews whichViews, int n)
    {
        List<ViAppView> avs = getSortedAppViews(whichViews);
        if(avs == null)
            return false;

        // n is in range 1..n, put into range 0..(n-1)
        --n;
        if(n >= avs.size())
            n = avs.size() -1; // last window

        return ViManager.getFS().edit(avs.get(n), false);
    }

    /**
     * Split the current window. The new window should handle n lines.
     * If av is not null, put that editor into the newly created area.
     *
     * @param orientation which way to split
     * @param n size lines or cols of new view area
     * @param av put this into the new view area
     */
    static void win_split(Orientation orientation, int n, ViAppView av)
    {
        if(av == null)
            TextView.setExpectedNewActivation(G.curbuf.getDisplayFileName(),
                                              G.curwin.w_cursor.getOffset());
        Direction dir = orientation == Orientation.LEFT_RIGHT
                    ? (G.p_spr ? Direction.RIGHT : Direction.LEFT)
                    : (G.p_sb ? Direction.DOWN : Direction.UP);
        G.curwin.win_split(dir, n, av);
    }

    private static void win_move(Direction direction, int n)
    {
        G.curwin.win_move(direction, n);
    }

    private static void win_size(SIZOP op, Orientation o,
                                 int n, boolean fSmaller)
    {
        switch(op) {
            case ADJUST:
                if(n == 0)      n = 1;
                if(fSmaller)    n = -n;
                break;
            case SET:
                if(n == 0)      n = 10000;
                break;
            case SAME:
                break;
        }
        G.curwin.win_size(op, o, n);
    }

    private static void win_clone()
    {
        TextView.setExpectedNewActivation(
                G.curbuf.getDisplayFileName(), G.curwin.w_cursor.getOffset());
        G.curwin.win_clone();
    }

    private static boolean win_jump(Direction direction, int n)
    {
        ViAppView av = G.curwin.getAppView();
        if(av == null) {
            return false;
        }

        ViWindowNavigator nav = ViManager.getFactory().getWindowNavigator();
        av = nav.getTarget(direction, av, n);

        return av != null ? ViManager.getFS().edit(av, false) : false;
    }

    static List<ViAppView> getSortedAppViews(AppViews whichViews)
    {
        List<ViAppView> avs = getVisibleAppViews(whichViews);
        if(avs == null)
            return null;

        AppViews.sortAppView(avs);
        return avs;
    }

    public static List<ViAppView> getVisibleAppViews(AppViews whichViews)
    {
        List<ViAppView> avs = AppViews.getList(whichViews);
        if(avs == null)
            return null;

        // get rid of appviews that are not showing
        for (Iterator<ViAppView> it = avs.iterator(); it.hasNext();) {
            ViAppView av = it.next();
            if(!av.isShowing())
                it.remove();
        }
        return avs;
    }

    private Misc01()
    {
    }

}
