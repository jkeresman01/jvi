/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;

import java.awt.event.ActionEvent;
import java.util.EnumSet;
import java.util.logging.Level;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.ColonCommands.AbstractColonAction;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.lib.*;

import static com.raelity.jvi.core.Util.beep_flush;

/**
 *
 * @author err
 */
public class TabPages
{
@ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
public static class Init implements ViInitialization
{
@Override
public void init()
{
    TabPages.init();
}
}

private TabPages() {}

private static void init()
{
    // tab handling
    ColonCommands.register("tabc", "tabclose",
                           new Tabclose(), null);
    ColonCommands.register("tabo", "tabonly",
                           new Tabonly(), null);
    ColonCommands.register("tabm", "tabmove",
                           new Tabmove(), null);
    ColonCommands.register("tabn", "tabnext",
                           new Tabnext(), null);
    ColonCommands.register("tabp", "tabprevious",
                           new Tabnext(), null);
    ColonCommands.register("tabN", "tabNext",
                           new Tabnext(), null);
    ColonCommands.register("tabfir", "tabfirst",
                           new Tabnext(), null);
    ColonCommands.register("tabr", "tabrewind",
                           new Tabnext(), null);
    ColonCommands.register("tabl", "tablast",
                           new Tabnext(), null);
}
/**
 * This is derived from get_tabpage_arg in ex_docmd.c
 * in recent vim releases (vim8.x). The main difference is that
 * the string range is handled here. The parsing code prefers
 * an argument, but uses the range otherwise.
 * @param cev
 * @param ti
 * @return
 */
private static int getTabpageArg(ColonEvent cev, ViTabInfo ti) {
    int tab_number = 0;
    theend: {
        if(cev.getNArg() > 1) {
            cev.emsg = Messages.e_invarg;
            break theend;
        }
        String tabCmd = cev.getArg(0);
        boolean unaccept_arg0 = ! "tabmove".equals(tabCmd);
        boolean usingRange = cev.getNArg() == 0;
        String s = usingRange
                   ? cev.getLastAddr() : cev.getArg(1);
        if(ti == null) {
            ti = G.curwin.getTabInfo();
        }
        // There was an "else if(eap->addr_count > 0) { // SEE NOTED BELOW
        // In modern vim, get_address is updated to be aware of the type
        // of command being executed, eg ADDR_TABS; jvi doesn't do that.
        // So we parse the range string in the !s.isEmpty().
        // The only thing not the same is the '.' in ":.tabmove" (a no-op)
        // so special case
        if(usingRange && ".".equals(s) &&
                ("tabmove".equals(tabCmd)
                || "tabnext".equals(tabCmd)
                || "tabclose".equals(tabCmd))) {
            tab_number = ti.getCurNr(); // move to same location
        } else if(!s.isEmpty()) {
            // something to parse
            int relative = 0;
            int sidx = 0;
            if(s.charAt(sidx) == '-') {
                relative = -1;
                sidx++;
            } else if(s.charAt(sidx) == '+') {
                relative = 1;
                sidx++;
            }
            int sidx_save = sidx;
            MutableInt mi = new MutableInt();
            sidx = Misc.getdigits(s, sidx, mi);
            tab_number = mi.getValue();
            if(relative == 0) {
                if("$".equals(s.substring(sidx)))
                    tab_number = ti.getLastNr();
                else if(sidx == sidx_save || s.charAt(sidx_save) == '-'
                        || sidx < s.length() || tab_number > ti.getLastNr()) {
                    // No numbers as argument.
                    cev.emsg = Messages.e_invarg;
                    break theend;
                }
            } else {
                if(sidx_save >= s.length()) // actuall sidx_save == s.length
                    tab_number = 1;
                else if(sidx == sidx_save || s.charAt(sidx_save) == '-'
                        || sidx < s.length() || tab_number == 0) {
                    // No numbers as argument.
                    cev.emsg = Messages.e_invarg;
                    break theend;
                }
                tab_number = tab_number * relative + ti.getCurNr();
                if(!unaccept_arg0 && relative == -1)
                    --tab_number;
            }
            if(tab_number < (unaccept_arg0 ? 1 : 0)
                    || tab_number > ti.getLastNr()) {
                cev.emsg = Messages.e_invarg;
                break theend;
            }
            // else if(eap->addr_count > 0) {
        } else {
            // no argument
            switch(tabCmd) {
            case "tabnext":
                tab_number = ti.getCurNr() + 1;
                if(tab_number > ti.getLastNr())
                    tab_number = 1;
                break;
            case "tabmove":
                tab_number = ti.getLastNr();
                break;
            default:
                tab_number = ti.getCurNr();
            }
        }
    }
    return tab_number;
}

private static class Tabclose extends AbstractColonAction {
@Override
public EnumSet<CcFlag> getFlags()
{
    return EnumSet.of(CcFlag.RANGE);
}

@Override
public void actionPerformed(final ActionEvent ev)
{
    ColonEvent cev = (ColonEvent)ev;
    ViTabInfo ti = G.curwin.getTabInfo();
    int tab_number = getTabpageArg(cev, ti);
    
    if(Options.getDebugOption(Options.dbgEditorActivation)
            .getBoolean(Level.INFO))
        Msg.smsg("tabclose(%d)", tab_number);
    if(cev.getEmsg() == null) {
        if(ti.getLastNr() <= 1) {
            cev.emsg = Messages.e_lasttab;
        } else {
            ti.getAppView(tab_number).close(false);
        }
    }
}
}

private static class Tabonly extends AbstractColonAction {
@Override
public EnumSet<CcFlag> getFlags()
{
    return EnumSet.of(CcFlag.RANGE);
}

@Override
public void actionPerformed(final ActionEvent ev)
{
    ColonEvent cev = (ColonEvent)ev;
    ViTabInfo ti = G.curwin.getTabInfo();
    int tab_number = getTabpageArg(cev, ti);
    
    if(Options.getDebugOption(Options.dbgEditorActivation)
            .getBoolean(Level.INFO))
        Msg.smsg("tabonly(%d)", tab_number);
    if(cev.getEmsg() == null) {
        if(ti.getLastNr() <= 1) {
            cev.emsg = Messages.e_lasttab;
        } else {
            for (int i = 1; i <= ti.getLastNr(); i++) {
                if(i != tab_number)
                    ti.getAppView(i).close(false);
            }
        }
    }
}
}

private static class Tabmove extends AbstractColonAction {
@Override
public EnumSet<CcFlag> getFlags()
{
    return EnumSet.of(CcFlag.RANGE);
}

@Override
public void actionPerformed(final ActionEvent ev)
{
    ColonEvent cev = (ColonEvent)ev;
    ViTabInfo ti = G.curwin.getTabInfo();
    int idx = getTabpageArg(cev, ti);
    if(cev.getEmsg() == null)
        G.curwin.tab_move(idx, ti);
}
}

private static class Tabnext extends AbstractColonAction {
@Override
public EnumSet<CcFlag> getFlags()
{
    return EnumSet.of(CcFlag.RANGE);
}

@Override
public void actionPerformed(final ActionEvent ev)
{
    int tab_number;
    ColonEvent cev = (ColonEvent)ev;
    String tabCmd = cev.getArg(0);
    switch(tabCmd) {
    case "tabfirst":
    case "tabrewind":
        gotoTabpage(1, null);
        break;
    case "tablast":
        gotoTabpage(ViTabInfo.LAST_TAB, null);
        break;
    case "tabprevious":
    case "tabNext":
    {
        if(cev.getNArg() > 0) {
            String s = cev.getArg(1);
            MutableInt mi = new MutableInt();
            int sidx = Misc.getdigits(s, 0, mi);
            tab_number = mi.getValue();
            if(sidx == 0 || s.charAt(0) == '-'
                    || cev.getNArg() > 1 || tab_number == 0) {
                // No numbers as argument.
                cev.emsg = Messages.e_invarg;
                return;
            }
        } else {
            // using range
            if(cev.getAddrCount() == 0)
                tab_number = 1;
            else {
                tab_number = cev.getLine2();
                if(tab_number < 1) {
                    cev.emsg = Messages.e_invrange;
                    return;
                }
            }
        }
        gotoTabpage(-tab_number, null);
        break;
    }
    default: // "tabnext":
        tab_number = getTabpageArg(cev, null);
        if(cev.getEmsg() == null)
            gotoTabpage(tab_number, null);
    }
}
}

/**
 * @param n 1 is first tab
 * @param ti
 */
static void gotoTabpage(int n, ViTabInfo ti) {
    int tab_number;
    if(ti == null)
        ti = G.curwin.getTabInfo();
    // from vim: return; not allowed when editing the command line
    if(ti.getLastNr() == 1) {
        if(n > 1)
            beep_flush();
        return;
    }
    if(n == 0) {
        // No count, go to next tab page, wrap around end
        n = ti.getCurNr();
        if(n >= ti.getLastNr())
            tab_number = 1;
        else
            tab_number = n + 1;
    } else if (n < 0) {
        // "gT": to to previous tab page, wrap around end.
        // "N gT" repeats this N times
        n = -(-n % ti.getLastNr());
        tab_number = ti.getCurNr() + n;
        if(tab_number <= 0)
            tab_number += ti.getLastNr();
    } else if(n == ViTabInfo.LAST_TAB)
        // Go to last tab page
        tab_number = ti.getLastNr();
    else {
        tab_number = findTabpage(n, ti);
        if(tab_number < 1) {
            beep_flush();
            return;
        }
    }
    G.curwin.tab_goto(tab_number, ti);
}

/**
 * Find tab page "n" (first one is 1).  Returns 0 when not found.
 */
static private int findTabpage(int n, ViTabInfo ti) {
    if(n == 0)
        return ti.getCurNr();
    if(n < 1 || n > ti.getLastNr())
        return 0;
    return n;
}
}
