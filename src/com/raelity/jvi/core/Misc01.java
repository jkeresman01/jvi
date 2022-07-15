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

import java.awt.Toolkit;
import java.io.File;
import java.text.CharacterIterator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.google.common.eventbus.Subscribe;

import org.openide.util.Exceptions;

import com.raelity.jvi.*;
import com.raelity.jvi.ViTextView.Direction;
import com.raelity.jvi.ViTextView.Orientation;
import com.raelity.jvi.ViTextView.SIZOP;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.manager.*;
import com.raelity.text.MySegment;
import com.raelity.text.StringSegment;

import static com.raelity.jvi.core.Edit.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.CtrlChars.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;
import static com.raelity.text.TextUtil.sf;

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
  static void scrollToLine(int line) {
    if(line > G.curbuf.getLineCount())
        line = G.curbuf.getLineCount();
    int logicalLine = G.curwin.getLogicalLine(line);
    scrollToLogicalLine(logicalLine);
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
    private static void scrollToLogicalLine(int logicalLine)
    {
      int logicalLineF = logicalLine;
      G.dbgCoordSkip.println(Level.FINE, () ->
              sf("gotoLogicalLine: logicalLine %d", logicalLineF));
      if(logicalLine < 1)
        logicalLine = 1;
      if(logicalLine > G.curwin.getLogicalLineCount())
        logicalLine = G.curwin.getLogicalLineCount();

      // if target line is less than half a screen away from
      // being visible, then just let it scroll, otherwise
      // center the target line

      final int curTop = G.curwin.getVpTopLogicalLine();
      final int vpLines = G.curwin.getVpLines();
      final int so = getScrollOff();
      final int center = curTop + vpLines / 2 - 1;

      // reduce scrollMargin, the distance from center that we will scroll
      // the screen, by amount of scrolloff.
      final int scrollMargin = vpLines - so; // max dist from center to do scroll

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
      else {
          if(curTop != newTop)
              G.curwin.setVpTopLogicalLine(adjustTopLogicalLine(newTop));
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
        beep_flush();
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
          beep_flush();
          retval = FAIL;
          break;
        }

        // the following test is added because with swing there can not be
        // blank lines on the screen, so we can go no more when the cursor
        // is positioned at the last line.
        if (dir == FORWARD
                && G.curwin.getLogicalLine(G.curwin.w_cursor.getLine())
                      == G.curwin.getLogicalLineCount()) {
          beep_flush();
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
      beginline(fpos, BL_SOL | BL_FIX);
      G.curwin.w_cursor.set(fpos.getOffset()); // KEEP fpos.getOffset()
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

      // int newtopline;
      // int newbotline;
      // int newcursorline;

      final ViFPOS cursor = G.curwin.w_cursor;

      if (Prenum != 0)
        G.curwin.w_p_scr = (Prenum > G.curwin.getVpLines())
                ? G.curwin.getVpLines() : Prenum;
      n = (G.curwin.w_p_scr <= G.curwin.getVpLines())
          ?  G.curwin.w_p_scr : G.curwin.getVpLines();

      validate_botline();
      room = G.curwin.getVpBlankLines();
      int newtopline = G.curwin.getVpTopLogicalLine();
      int newbotline = G.curwin.getVpBottomLogicalLine();
      //COORD CHANGED: newcursorline = cursor.getLine();
      int newcursorline = G.curwin.getLogicalLine(cursor.getLine());
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
      beginline(fpos, BL_SOL | BL_FIX);
      G.curwin.w_cursor.set(fpos.getOffset()); // KEEP fpos.getOffset()
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
    }
    static void cursor_correct(ViFPOS fpos) {
      // NEEDSWORK: cursor_correct: handle p_so and the rest
    }

    static void validate_botline() {
      // NEEDSWORK: validate_botline: think this is a nop
      comp_botline();
    }

    private static void comp_botline() {
      // NEEDSWORK: comp_botline: think this is a nop
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
            case CTRL_Underbar:
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
            case CTRL_S:
                win_split(Orientation.UP_DOWN, Prenum, null);
                break;

            case 'v':
            case CTRL_V:
                win_split(Orientation.LEFT_RIGHT, Prenum, null);
                break;

            // close current window
            case 'c':
            case CTRL_C:
                GetChar.stuffReadbuff(":close\n");
                break;

            // close all but current window
            case 'O':
            case 'o':
                //
                GetChar.stuffReadbuff(":only\n");
                break;

            // quit current window */
            case CTRL_Q:
            case 'q':
                G.curwin.win_quit();
                break;

            // cursor to next window with wrap around
            case CTRL_W:
            case 'w':
                ok = win_jump_forw(AppViews.ALL, Prenum);
                break;

            // cursor to prev window with wrap around
            case 'W':
                ok = win_jump_back(AppViews.ALL, Prenum);
                break;

            // cursor to next nomadic editor with wrap around
            case CTRL_E:
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
            case CTRL_J:
            case 'j':
                ok = win_jump(Direction.DOWN, Prenum);
                break;

            // cursor to window above
            case K_UP:
            case CTRL_K:
            case 'k':
                ok = win_jump(Direction.UP, Prenum);
                break;

            // cursor to left window
            case K_LEFT:
            case K_BS:
            case CTRL_H:
            case 'h':
                ok = win_jump(Direction.LEFT, Prenum);
                break;

            // cursor to right window
            case K_RIGHT:
            case CTRL_L:
            case 'l':
                ok = win_jump(Direction.RIGHT, Prenum);
                break;

            // cursor to top-left window
            case 't':
            case CTRL_T:
                ok = win_jump_to(AppViews.ALL, 1); // firstwin
                break;

            // cursor to bottom-right window
            case 'b':
            case CTRL_B:
                ok = win_jump_to(AppViews.ALL, Integer.MAX_VALUE); // lastwin
                break;

            // cursor to last accessed (previous) window
            case 'p':
            case CTRL_P:
                // Handle like :e#
                ViAppView av = AppViews.getMruAppView(1);
                ok = av != null ? ViManager.getFS().edit(av, false) : false;
                break;

            default:
                ok = false;
                break;
        }
        if(!ok)
            beep_flush();
    }

    private static boolean win_jump_forw(AppViews whichViews, int n)
    {
        boolean ok;
        ok = n == 0 ? win_cycle(whichViews, FORWARD)
                    : win_jump_to(whichViews, n);
        return ok;
    }

    private static boolean win_jump_back(AppViews whichViews, int n)
    {
        boolean ok;
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
            setExpectedNewActivation(
                    G.curbuf.getFile(), G.curwin.w_cursor.getOffset());
        Direction dir = orientation == Orientation.LEFT_RIGHT
                    ? (G.p_spr ? Direction.RIGHT : Direction.LEFT)
                    : (G.p_sb ? Direction.DOWN : Direction.UP);
        G.curwin.win_split(dir, n, av);
    }

    private static void win_move(Direction direction, int n)
    {
        Options.getDebugOption(Options.dbgWindowTreeBuilder)
                .println(() -> sf("win_move(%s, %d) NIMP", direction, n));
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
        final int nF = n;
        Options.getDebugOption(Options.dbgWindowTreeBuilder)
                .println(() -> sf("win_size(%s, %s, %d)", op, o, nF));
        G.curwin.win_size(op, o, n);
    }

    static void win_clone()
    {
        setExpectedNewActivation(
                G.curbuf.getFile(), G.curwin.w_cursor.getOffset());
        G.curwin.win_clone();
    }

    /**
     * Use this when the next text view activation is for a new text view.
     * Sets the line number for the newly opened window.
     * If next activation doesn't meet the conditions, then nothing happens.
     * spit/clone.
     */

    private static void setExpectedNewActivation(File fi, int offset)
    {
        ViEvent.getBus().register(new NextNewActivationOffset(fi, offset));
    }
   
    /** Newly opened TV set the position; after split/clone.
     */
    private static class NextNewActivationOffset
    {
    private final File fi;
    private final int offset;
    
    public NextNewActivationOffset(File fi, int offset)
    {
        this.fi = fi;
        this.offset = offset;
    }
    
    @Subscribe
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void check(ViEvent.OpenTv ev) {
        if(fi.equals(G.curbuf.getFile()))
            G.curwin.w_cursor.set(offset);
        ViEvent.getBus().unregister(this);
    }

    // Get rid of this if it's still around.
    @Subscribe
    public void clear(ViEvent.SwitchToTv ev) {
        ViEvent.getBus().unregister(this);
    }
    } // END CLASS NextNewActivationOffset

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
    
    // static final int TERMCAP2KEY(int a, int b) { return a + (b << 8); }
    public static char ctrl(char x) { return (char)(x & 0x1f); }
    // static final int shift(int c) { return c | (0x1 << 24); }
    // static void stuffcharReadbuff(int c) {}
    
    /** position to end of line. */
    static void endLine() {
        ViFPOS fpos = G.curwin.w_cursor;
        int offset = G.curbuf.getLineEndOffsetFromOffset(fpos.getOffset());
        // assumes there is at least one char in line, could be a '\n'
        offset--;	// point at last char of line
        if(getCharAt(offset) != '\n') { // DONE
            offset++; // unlikely
        }
        G.curwin.w_cursor.set(offset);
    }
    
    /**
     * "ring the bell", if the G.p_vb is set then visual bell.
     * <p/>
     * Note: if there's an error and the typeahead buffer should be flushed
     * then use beep_flush().
     */
    public static void vim_beep() {
        if(Options.getDebugOption(Options.dbgBeep).getBoolean())
            Exceptions.printStackTrace(new Exception("BEEP"));
        if(!G.p_vb() || G.curwin == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        G.curwin.visual_bell();
    }
    
    /**
     * Returns the substring of c in s or null if c not part of s.
     * @param s the string to search in
     * @param c the character to search for
     * @return the substring of c in s or null if c not part of s.
     */
    public static String vim_strchr(String s, char c) {
        int index = s.indexOf(c);
        if(index < 0) {
            return null;
        }
        return s.substring(index);
    }
    
    /**
     * Returns the position of c in cs or -1 if not part of s.
     * vim_strchar(s, 0, c) < 0 can replace vim_strchr(s, c) == null
     * @param cs sequence to search
     * @param i start checking at this position
     * @param c look for this char
     * @return position if found, else -1
     */
    public static int vim_strchr(CharSequence cs, int i, char c) {
        int p = i;
        int length = cs.length();
        while(p < length) {
            if(cs.charAt(p) == c)
                return p;
            ++p;
        }
        return -1;
    }
    
    /**
     * VISUAL and OP_PENDING State are never set, they are equal to NORMAL State
     * with a condition.  This function returns the real State.
     * {jVi} PLATFORM.
     */
    public static int get_real_state()
    {
        if ((G.State & NORMAL) != 0)
        {
            if (G.VIsual_active)
                return VISUAL;
            else if (G.finish_op)
                return OP_PENDING;
            else if(G.curwin.hasSelection())
                return PLATFORM;
        }
        return G.State;
    }
    
    public static boolean isInsertMode() {
        return (G.State & BASE_STATE_MASK) == INSERT;
    }
    
    /**
     * Vim has its own isspace() function, because on some machines isspace()
     * can't handle characters above 128.
     */
    public static boolean vim_isspace(char x)
    {
        return ((x >= 9 && x <= 13) || x == ' ');
    }
    
    public static boolean isalnum(int regname) {
        return	regname >= '0' && regname <= '9'
                || regname >= 'a' && regname <= 'z'
                || regname >= 'A' && regname <= 'Z';
    }
    
    public static boolean isalpha(char c) {
        return	   c >= 'a' && c <= 'z'
                || c >= 'A' && c <= 'Z';
    }
    
    public static boolean ascii_isalpha(char c) {
        return	   c < 0x7f && isalpha(c);
    }
    
    public static boolean islower(char c) {
        return 'a' <= c && c <= 'z';
    }
    
    public static char tolower(char c) {
        if(isupper(c)) {
            c |= 0x20;
        }
        return c;
    }
    
    static boolean isupper(char c) {
        return 'A' <= c && c <= 'Z';
    }
    
    static char toupper(char c) {
        if(islower(c)) {
            c &= ~0x20;
        }
        return c;
    }
    
    public static boolean isdigit(char c) {
        return '0' <= c && c <= '9';
    }
    
    /**
     * Variant of isxdigit() that can handle characters > 0x100.
     * We don't use isxdigit() here, because on some systems it also considers
     * superscript 1 to be a digit.
     */
    public static boolean isxdigit(char c)
    {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }
    
    // #define CharOrd(x)	((x) < 'a' ? (x) - 'A' : (x) - 'a')
    // #define CharOrdLow(x)	((x) - 'a')
    // #define CharOrdUp(x)	((x) - 'A')
    // #define ROT13(c, a)	(((((c) - (a)) + 13) % 26) + (a))
    public static int CharOrd(char c) {
        return c < 'a' ? c - 'A' : c - 'a';
    }
    
    //static boolean vim_isprintc(char c) { return false; }
    
    /**
     * get a pointer to a (read-only copy of a) line.
     *
     * On failure an error message is given and IObuff is returned (to avoid
     * having to check for error everywhere).
     */
    static MySegment ml_get(int lnum) {
        return ml_get_buf(G.curbuf, lnum);
    }
    
    /**
     * get a pointer to a (read-only copy of a) line.
     *
     * On failure an error message is given and IObuff is returned (to avoid
     * having to check for error everywhere).
     */
    static MySegment ml_get_buf(Buffer buf, int lnum) {
        return buf.getLineSegment(lnum);
    }
    
    static MySegment ml_get_curline() {
        return ml_get(G.curwin.w_cursor.getLine());
    }
    
    /** get pointer to positin 'pos', the returned MySegment's CharacterIterator
     * is initialized to the character at pos.
     * <p>
     * NEEDSWORK: There are usages of CharacterIterator that depend on the
     *            the entire line being part of the iterator, in particular
     *            the column position is: ci.getIndex() - ci.getBeginIndex()
     *
     * @return MySegment for the line.
     */
    static CharacterIterator ml_get_pos(ViFPOS pos) {
        MySegment seg = G.curbuf.getLineSegment(pos.getLine());
        seg.setIndex(seg.offset + pos.getColumn()); // DONE
        return seg;
    }
    
    static CharacterIterator ml_get_cursor() {
        return ml_get_pos(G.curwin.w_cursor);
    }
    
    static void ml_replace(int lnum, CharSequence line) {
        G.curwin.replaceString(G.curbuf.getLineStartOffset(lnum),
                                     G.curbuf.getLineEndOffset(lnum) -1,
                                     line.toString());
    }
    
    public static MySegment truncateNewline(MySegment seg) {
        assert(seg.fetch(seg.count - 1) == '\n'); // DONE
        return new MySegment(seg.array, seg.offset, seg.count - 1, seg.docOffset);
    }
    
    /**
     * Get the length of a line, not including the newline
     */
    static int lineLength(int line) {
        return lineLength(G.curbuf, line);
    }
    
    public static int lineLength(Buffer buf, int line) {
        return buf.getLineLength(line);
    }
    
    public static int lineLength(MySegment seg) {
        return seg.count < 1 ? 0 : seg.count - 1;
    }
    
    /** is the indicated line empty? */
    static boolean lineempty(int lnum) {
        MySegment seg = G.curbuf.getLineSegment(lnum);
        return seg.count == 0 || seg.fetch(0) == '\n'; // DONE
    }
    
    static boolean bufempty() {
        return G.curbuf.getLineCount() == 1
                && lineempty(1);
    }
    
    static char getChar() {
        return getCharAt(G.curwin.getCaretPosition());
    }
    
    static char getCharAt(int offset) {
        MySegment seg = new MySegment();
        G.curbuf.getSegment(offset, 1, seg);
        return seg.count > 0 ? seg.fetch(0) : 0; // DONE ??
    }
    
    /** flush map and typeahead buffers and give a warning for an error */
    public static void beep_flush() {
        GetChar.flush_buffers(false);
        vim_beep();
    }
    
    /**
     * @param s1
     * @param s2
     * @param n this is ignored
     * @return
     */
    static int strncmp(String s1, String s2, int n)
    {
        if(s1.length() > n)
            s1 = s1.substring(0, n);
        if(s2.length() > n)
            s2 = s2.substring(0, n);
        return s1.compareTo(s2);
    }
    
    static int strncmp(MySegment seg, int i, String s2, int n)
    {
        String s1 = seg.subSequence(i, seg.count).toString();
        return strncmp(s1, s2, n);
    }
    
    static int strnicmp(StringSegment ss1, String s2, int n)
    {
        String s1 = ss1.length() > n ? ss1.substring(0, n) : ss1.toString();
        if(s2.length() > n)
            s2 = s2.substring(0, n);
        return s1.compareToIgnoreCase(s2);
        
    }
    
    /**
     * Convert a string into a long and/or unsigned long, taking care of
     * hexadecimal and octal numbers.  Accepts a '-' sign.
     * If "hexp" is not null, returns a flag to indicate the type of the number:
     * <pre>
     *  0	    decimal
     *  '0'	    octal
     *  'X'	    hex
     *  'x'	    hex
     * If "len" is not null, the length of the number in characters is returned.
     * If "nptr" is not null, the signed result is returned in it.
     * If "unptr" is not null, the unsigned result is returned in it.
     * If "dooct" is non-zero recognize octal numbers, when > 1 always assume
     * octal number.
     * If "dohex" is non-zero recognize hex numbers, when > 1 always assume
     * hex number.
     * </pre>
     */
//    void
//vim_str2nr(start, hexp, len, dooct, dohex, nptr, unptr)
    static void vim_str2nr(MySegment seg, int start,
                           MutableInt hexp, MutableInt len,
                           int dooct, int dohex,
                           MutableInt nptr, MutableInt unptr)
//    char_u		*start;
//    int			*hexp;	    /* return: type of number 0 = decimal, 'x'
//				       or 'X' is hex, '0' = octal */
//    int			*len;	    /* return: detected length of number */
//    int			dooct;	    /* recognize octal number */
//    int			dohex;	    /* recognize hex number */
//    long		*nptr;	    /* return: signed result */
//    unsigned long	*unptr;	    /* return: unsigned result */
    {
        int	    	    ptr = start;
        int		    hex = 0;		/* default is decimal */
        boolean	    negative = false;
        int             un = 0;
        int		    n;
        
        if (seg.charAt(ptr+0) == '-')
        {
            negative = true;
            ++ptr;
        }
        
        // Recognize hex and octal.
        if (seg.charAt(ptr+0) == '0' && seg.charAt(ptr+1) != '8' && seg.charAt(ptr+1) != '9')
        {
            hex = seg.charAt(ptr+1);
            if (dohex != 0 && (hex == 'X' || hex == 'x') && isxdigit(seg.charAt(ptr+2)))
                ptr += 2;			/* hexadecimal */
            else
            {
                hex = 0;			/* default is decimal */
                if (dooct != 0)
                {
                    /* Don't interpret "0", "08" or "0129" as octal. */
                    for (n = 1; isdigit(seg.charAt(ptr+n)); ++n)
                    {
                        if (seg.charAt(ptr+n) > '7')
                        {
                            hex = 0;	/* can't be octal */
                            break;
                        }
                        if (seg.charAt(ptr+n) > '0')
                            hex = '0';	/* assume octal */
                    }
                }
            }
        }
        
        /*
        * Do the string-to-numeric conversion "manually" to avoid sscanf quirks.
        */
        if (hex == '0' || dooct > 1)
        {
            /* octal */
            while ('0' <= seg.charAt(ptr) && seg.charAt(ptr) <= '7')
            {
                un = 8 * un + (seg.charAt(ptr) - '0');
                ++ptr;
            }
        }
        else if (hex != 0 || dohex > 1)
        {
            /* hex */
            while (isxdigit(seg.charAt(ptr)))
            {
                un = 16 * un + hex2nr(seg.charAt(ptr));
                ++ptr;
            }
        }
        else
        {
            /* decimal */
            while (isdigit(seg.charAt(ptr)))
            {
                un = 10 * un + (seg.charAt(ptr) - '0');
                ++ptr;
            }
        }
        
        if (hexp != null)
            hexp.setValue(hex); //*hexp = hex;
        if (len != null)
            len.setValue(ptr - start); //*len = (int)(ptr - start);
        if (nptr != null)
        {
            if (negative)   /* account for leading '-' for decimal numbers */
                nptr.setValue(-un); //*nptr = -(long)un;
            else
                nptr.setValue(un);//*nptr = (long)un;
        }
        if (unptr != null)
            unptr.setValue(un);//*unptr = un;
    }
    
    /**
     * Simplified version: parses a decimal number, return an int,
     * advance segment past parsed characters.
     * MIN_VALUE returned if char not a number, can also check if segments
     * index has moved.
     * @param seg
     * @return parsed integer, Integer.MIN_VALUE if chars not a number
     */
    static int vim_str2nr(StringSegment seg)
    {
        MutableInt p_len = new MutableInt();
        MutableInt p_num = new MutableInt();
        int start_parse_idx = seg.getIndex();
        int result = Integer.MIN_VALUE;
        
        char tchar = seg.current();
        vim_str2nr(seg, start_parse_idx, null, p_len, 0, 0, p_num, null);
        // if only '-', then not a number
        if(!(tchar == '-' && p_len.getValue() == 1)) {
            seg.setIndex(start_parse_idx + p_len.getValue());
            result = p_num.getValue();
        }
        
        return result;
    }
    
    //static void
    //vim_str2nr(String str, int start,
    //           MutableInt hexp, MutableInt len,
    //           int dooct, int dohex,
    //           MutableInt nptr, MutableInt unptr)
    //{
    //  StringSegment seg = new StringSegment(str);
    //  vim_str2nr(seg, start, hexp, len, dooct, dohex, nptr, unptr);
    //}
    
    /**
     * Return the value of a single hex character.
     * Only valid when the argument is '0' - '9', 'A' - 'F' or 'a' - 'f'.
     */
    public static int hex2nr(char c)
    {
        if (c >= 'a' && c <= 'f')
            return c - 'a' + 10;
        if (c >= 'A' && c <= 'F')
            return c - 'A' + 10;
        return c - '0';
    }
    
// cursor compare
    static String cur() {
        String s = G.curwin.w_cursor.toString();
        // put the virtual position in their
        return s;
    }
    
    static boolean equalpos(ViFPOS p1, ViFPOS p2) {
        return p1 == null
                                           ? p2 == null
                                           : p1.equals(p2);
    }
    static boolean lt(ViFPOS p1, ViFPOS p2) {
        return p1.compareTo(p2) < 0;
    }
    
    static void add_to_history(int histype, String new_entry)
    {
        ViCmdEntry ce = histype == HIST_SEARCH
                                                    ? Search.getSearchCommandEntry()
                                                    : ExCommands.getColonCommandEntry();
        ce.makeTop(new_entry);
    }
    
    private Misc01()
    {
    }
    
}
