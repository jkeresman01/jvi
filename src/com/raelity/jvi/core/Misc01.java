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

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.manager.AppViews;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.core.WindowTreeBuilder.Direction;
import java.util.Iterator;
import java.util.List;

import static com.raelity.jvi.core.Constants.*;
import static com.raelity.jvi.core.KeyDefs.*;

/**
 * do_window is some stuff from window.c and related.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Misc01
{
    static void do_window(char nchar, int Prenum)
    {
        Normal.do_xop("do_window");
        switch (nchar) {
            // split current window in two parts
            case 'S':
            case 's':
            case 'S' & 0x1f:          // Ctrl
                G.curwin.win_split(Prenum);
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
                win_move_forw(AppViews.ACTIVE, Prenum);
                break;

            // cursor to prev window with wrap around
            case 'W':
                win_move_back(AppViews.ACTIVE, Prenum);
                break;

            // cursor to next nomadic editor with wrap around
            case 'E' & 0x1f:
            case 'e':
                win_move_forw(AppViews.NOMAD, Prenum);
                break;

            // cursor to window below
            case K_DOWN:
            case 'J' & 0x1f:
            case 'j':
                win_jump(Direction.DOWN, Prenum);
                break;

            // cursor to window above
            case K_UP:
            case 'K' & 0x1f:
            case 'k':
                win_jump(Direction.UP, Prenum);
                break;

            // cursor to left window
            case K_LEFT:
            case K_BS:
            case 'H' & 0x1f:
            case 'h':
                win_jump(Direction.LEFT, Prenum);
                break;

            // cursor to right window
            case K_RIGHT:
            case 'L' & 0x1f:
            case 'l':
                win_jump(Direction.RIGHT, Prenum);
                break;

            // cursor to top-left window
            case 't':
            case 'T' & 0x1f:
                win_moveto(AppViews.ACTIVE, 1); // firstwin
                break;

            // cursor to bottom-right window
            case 'b':
            case 'B' & 0x1f:
                win_moveto(AppViews.ACTIVE, Integer.MAX_VALUE); // lastwin
                break;

            // cursor to last accessed (previous) window
            case 'p':
            case 'P' & 0x1f:
                // Handle like :e#
                ViAppView av = AppViews.getMruAppView(1);
                if(av != null)
                    ViManager.getFS().edit(av, false);
                break;

            default:
                Util.vim_beep();
                break;
        }
    }

    private static void win_move_forw(AppViews whichViews, int n)
    {
        if(n == 0)
            win_cycle(whichViews, FORWARD);
        else
            win_moveto(whichViews, n);
    }

    private static void win_move_back(AppViews whichViews, int n)
    {
        if(n == 0)
            win_cycle(whichViews, BACKWARD);
        else
            win_moveto(whichViews, n);
    }

    private static void win_cycle(AppViews whichViews, int n)
    {
        // n should be 1 or -1
        if(n < 0)
            n = -1;
        else
            n = 1;

        List<ViAppView> avs = getSortedAppViews(whichViews);
        if(avs == null)
            return;

        int idx = AppViews.indexOfCurrentAppView(avs);

        boolean foundInList = idx >= 0;

        if(avs.isEmpty() || foundInList && avs.size() == 1) {
            // nowhere to go
            Util.vim_beep();
            return;
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

        ViManager.getFS().edit(avs.get(idx), false);
    }

    private static void win_moveto(AppViews whichViews, int n)
    {
        List<ViAppView> avs = getSortedAppViews(whichViews);
        if(avs == null)
            return;

        // n is in range 1..n, put into range 0..(n-1)
        --n;
        if(n >= avs.size())
            n = avs.size() -1; // last window

        ViManager.getFS().edit(avs.get(n), false);
    }

    private static void win_jump(Direction direction, int n)
    {
        List<ViAppView> avs = getVisibleAppViews(AppViews.ACTIVE);
        if(avs == null)
            return;

        WindowTreeBuilder tree
                = ViManager.getFactory().getWindowTreeBuilder(avs);
        tree.processAppViews();

        ViAppView av = AppViews.currentAppView(avs);
        if(av == null) {
            Util.vim_beep();
            return;
        }

        av = tree.jump(direction, av, n);

        if(av != null)
            ViManager.getFS().edit(av, false);
    }

    private static List<ViAppView> getSortedAppViews(AppViews whichViews)
    {
        List<ViAppView> avs = getVisibleAppViews(whichViews);
        if(avs == null)
            return null;

        AppViews.sortAppView(avs);
        return avs;
    }
    private static List<ViAppView> getVisibleAppViews(AppViews whichViews)
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

}
