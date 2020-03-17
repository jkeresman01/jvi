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
import java.util.List;
import java.util.logging.Level;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.ViCmdEntry.HistEntry;
import com.raelity.jvi.*;
import com.raelity.jvi.core.ColonCommands.AbstractColonAction;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.*;
import com.raelity.text.StringSegment;

import static com.raelity.jvi.core.Msg.*;
import static com.raelity.jvi.core.ColonCommands.getColonCommandEntry;
import static com.raelity.jvi.core.Misc.skipwhite;
import static com.raelity.jvi.core.Search.getSearchCommandEntry;
import static com.raelity.jvi.core.Util.isalpha;
import static com.raelity.jvi.core.Util.isdigit;
import static com.raelity.jvi.core.Util.strnicmp;
import static com.raelity.jvi.core.Util.vim_str2nr;
import static com.raelity.jvi.core.Util.vim_strchr;
import static com.raelity.jvi.core.lib.Constants.NUL;
import static com.raelity.text.TextUtil.sf;

/**
 *
 * @author err
 */
public class CommandHistory
{
@ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
    @Override
    public void init()
    {
        CommandHistory.init();
    }
    }

private CommandHistory() {}

private static void init() {
    ColonCommands.register("his", "history",
                           new History(), null);
}
// History stuff
private final static String[] history_names
        = new String[] { "cmd", "search" };
private final static int HIST_CMD = 0;
private final static int HIST_SEARCH = 1;
private final static int HIST_COUNT = history_names.length;

static DebugOption dbgKeys;

/**
 * Translate a history character to the associated type number.
 */
private static int hist_char2type(char c)
{
    if(c == ':')
        return HIST_CMD;
    return HIST_SEARCH;	    // must be '?' or '/'
}

/**
 * Convert history name (from table above) to its HIST_ equivalent.
 * When "name" is empty, return "cmd" history.
 * Returns -1 for unknown history name.
 */
private static int get_histtype(StringSegment name)
{
    int len = name.length();
    if(len == 0)
        return hist_char2type(':');
    for(int i = 0; i < history_names.length; i++) {
        if(strnicmp(name, history_names[i], len) == 0)
            return i;
    }
    if(vim_strchr(":?/", name.charAt(0)) != null
            && name.charAt(1) == NUL)
        return hist_char2type(name.charAt(0));
    
    return -1;
}

/**
 * Get indices "num1,num2" that specify a range within a list (not a range of
 * text lines in a buffer!) from a string.  Used for ":history" and ":clist".
 * Returns OK if parsed successfully, otherwise FAIL.
 * NOTE: str is used as a character iterator
 */
private static boolean get_list_range(StringSegment str,
                                          MutableInt p_num1, MutableInt p_num2)
{
    MutableInt p_len = new MutableInt();
    boolean first = false;
    MutableInt p_num = new MutableInt();
    
    skipwhite(str);
    if(str.current() == '-' || isdigit(str.current())) { // parse "from" part of range
        int tidx = str.getIndex();
        vim_str2nr(str, tidx, null, p_len, 0, 0, p_num, null);
        str.setIndex(tidx + p_len.getValue());
        p_num1.setValue(p_num.getValue());
        first = true;
    }
    skipwhite(str);
    if(str.current() == ',') { // parse "to" part of range
        str.next();
        skipwhite(str);
        int tidx;
        tidx = str.getIndex();
        vim_str2nr(str, tidx, null, p_len, 0, 0, p_num, null);
        if(p_len.getValue() > 0) {
            p_num2.setValue(p_num.getValue());
            str.setIndex(tidx + p_len.getValue());
            skipwhite(str);
        } else if(! first)
            return false;
    } else if(first)
        p_num2.setValue(p_num1.getValue());
    
    return true;
}

/**
 * :history command - print a history
 * See vim/src/cmdhist.c.
 */
private static class History extends AbstractColonAction
{
@Override
public void actionPerformed(ActionEvent e)
{
    dbgKeys = Options.getDebugOption(Options.dbgKeyStrokes);
    ColonEvent cev = (ColonEvent)e;
    int histype1 = HIST_CMD;
    int histype2 = HIST_CMD;
    MutableInt p_hisidx1 = new MutableInt(1);
    MutableInt p_hisidx2 = new MutableInt(-1);
    
    if(G.p_hi == 0) {
        smsg("'history' option is zero");
        return;
    }
    // work with the whole string to handle unusual syntax
    StringSegment arg = new StringSegment(cev.getArgString());
    skipwhite(arg);
    int argStartIdx = arg.getIndex();
    if(!(isdigit(arg.current())
            || arg.current() == '-' || arg.current() == ',')) {
        while(isalpha(arg.current())
                || vim_strchr(":/?", arg.current()) != null) {  // ":=@>/?"
            arg.next();
        }
        StringSegment targ = arg.subSequence(argStartIdx, arg.getIndex());
        histype1 = get_histtype(targ);
        if(histype1 == -1) {
            if(strnicmp(targ, "all", targ.length()) == 0) {
                histype1 = 0;
                histype2 = HIST_COUNT -1;
            } else {
                emsg(Messages.e_trailing);
                return;
            }
        } else
            histype2 = histype1;
    }
    // else
    //     end = arg; but arg is untouched; jvi uses targ
    
    boolean ok = get_list_range(arg, p_hisidx1, p_hisidx2);
    
    dbgKeys.printf(Level.FINE, "history types: %d, %d\n", histype1, histype2);
    dbgKeys.printf(Level.FINE, "list_range: %b, %d,%d\n",
                                   ok, p_hisidx1.getValue(), p_hisidx2.getValue());
    if(!ok || !arg.atEnd()) {
        emsg(Messages.e_trailing);
        return;
    }
    
    try (ViOutputStream os = ViManager.createOutputStream(
            null, ViOutputStream.OUTPUT, null)) {
        for(; histype1 <= histype2; ++histype1) {
            List<HistEntry>hist = histype1 == HIST_CMD
                                      ? getColonCommandEntry().getHistEntrys()
                                      : getSearchCommandEntry().getHistEntrys();
            os.println(sf("\n      #  %s history", history_names[histype1]));
            int hislen = hist.size();
            if(hislen > 0) {
                
                // hist = history[histype1];
                int j = p_hisidx1.getValue();
                int k = p_hisidx2.getValue();
                dbgKeys.printf(Level.FINE, "before check: j = %d, k = %d\n", j, k);
                if (j < 0)
                    j = (-j > hislen) ? 0 : hist.get(hislen + j).hisnum;
                if (k < 0)
                    k = (-k > hislen) ? 0 : hist.get(hislen + k).hisnum;
                dbgKeys.printf(Level.FINE, "Hist range: j = %d, k = %d\n", j, k);
                int current = hist.get(hislen - 1).hisnum;
                if(j <= k)
                    for(HistEntry he : hist) {
                        if(he.hisnum >= j && he.hisnum <= k) {
                            String s = he.hisstr;
                            if(s.length() > 60)
                                s = s.substring(0, 60);
                            os.println(sf("%c%6d  %s",
                                              he.hisnum == current ? '>' : ' ',
                                              he.hisnum, s));
                        }
                    }
            }
        }
    }
}

}

}
