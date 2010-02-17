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
import java.util.List;

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

            // cursor to next window with wrap around
            case 'W' & 0x1f:
            case 'w':
                win_cycle(AppViews.ACTIVE, Prenum);
                break;

            // cursor to next nomadic editor with wrap around
            case 'E' & 0x1f:
            case 'e':
                win_cycle(AppViews.NOMAD, Prenum);
                break;

            default:
                Util.vim_beep();
                break;
        }
    }

    static void win_cycle(AppViews whichViews, int n)
    {
        if(n == 0)
            n = 1;
        // NEEDSWORK: win_cycle options about nomads

        Object[] o = AppViews.get(whichViews, true);
        List<ViAppView> l = (List<ViAppView>) o[0];
        int idx = (Integer)o[1];
        if(l.size() == 0) {
            Util.vim_beep();
            return;
        }

        // idx may be < 0, if the current textView is not in the list
        // that is ok for ^W^W (but not for close others)
        boolean foundInList = idx >= 0;
        if(idx < 0)
            idx = l.size() - 1;
        if(foundInList && l.size() <= 1)
            return;

        // get the window after/before the current window and activate it
        // Need to check the geometric relationship of the showing windows
        // and implement the down/left traversal algorithm.
        // For now just pick the next one in the list.
        idx += n;
        if(idx >= l.size())
            idx = 0;
        ViManager.getFS().edit(l.get(idx));
    }

}
