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
package com.raelity.jvi.options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raelity.jvi.core.*;

import static com.raelity.jvi.core.Options.*;

/**
 * TODO: I see no evidence that the array actually should be sorted.
 *       Get rid of the must be sorted notes.
 * @author Ernie Rael <err at raelity.com>
 */
public final class VimOption {
    final String fullName; // option name
    final String shortName; // option name
    // name of field and/or option
    final String varName; // java variable name in G, curbuf or curwin
    final String optName; // the jVi Option name.
    final S scope;
    final Set<F> flags;

    public static VimOption get(String optionName)
    {
        return mapOptionName.get(optionName);
    }

    static VimOption lookupVarName(String varName)
    {
        return mapVarName.get(varName);
    }

    static VimOption lookupUserName(String userName)
    {
        VimOption vopt = mapUserName.get(userName);
        return vopt == null || vopt.isHidden() ? null : vopt;
    }

    static List<VimOption> getAll()
    {
        return Collections.unmodifiableList(Arrays.asList(vopts));
    }

    static List<VimOption> getAllUser()
    {
        List<VimOption> l = new ArrayList<>(vopts.length);
        for(VimOption vopt : vopts) {
            if(vopt.f(F.HIDE) || Options.getOption(vopt.getOptName()).isHidden())
                continue;
            l.add(vopt);
        }
        return l;
    }

    /**
     * NOTE: varName is constructed from shortName and scope;
     *       fullName is used if no shortName.
     */
    private VimOption(String fullName, String shortName,
                      String optName, S scope, Set<F> flags)
    {
        this.fullName = fullName;
        this.shortName = shortName;
        this.optName = optName;
        this.scope = scope;
        this.flags = flags;
        StringBuilder sb = new StringBuilder();
        // construct varName
        if(flags.contains(F.VERBATIM)) {
            sb.append(fullName);
        } else {
            if(isWin())
                sb.append("w_");
            else if(isBuf())
                sb.append("b_");
            sb.append("p_");
            sb.append(shortName.isEmpty() ? fullName : shortName);
        }
        this.varName = sb.toString();
    }

    boolean f(F f)
    {
        return flags.contains(f);
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getShortName()
    {
        return shortName;
    }

    public String getOptName()
    {
        return optName;
    }

    public String getVarName()
    {
        return varName;
    }

    public boolean isGlobal()
    {
        return scope.isGlobal();
    }

    public boolean isLocal()
    {
        return scope.isLocal();
    }

    public boolean isWin()
    {
        return scope.isWin();
    }

    public boolean isBuf()
    {
        return scope.isBuf();
    }

    public boolean isHidden()
    {
        return f(F.HIDE) || Options.getOption(getOptName()).isHidden();
    }

    // Scope of the option
    private enum S {
        P_GBL, // a global option
        P_WIN, // a per window option
        P_BUF; // a per buffer option

        boolean isLocal() {
            return this == P_WIN || this == P_BUF;
        }

        boolean isGlobal() {
            return this == P_GBL;
        }

        boolean isWin() {
            return this == P_WIN;
        }

        boolean isBuf() {
            return this == P_BUF;
        }
    }

    // option flags
    enum F {
        COMMA,          // comma separated list
        NODUP,          // don't allow duplicate strings
        FLAGLIST,       // list of single-char flags
        HIDE,           // don't allow set command
        VERBATIM,       // use fullName verbatim as varName
    }

    private static final Set<F> nullF = EnumSet.noneOf(F.class);


final private static VimOption[] vopts = new VimOption[]{
    // SORTED ALPHABETICALLY, BEFORE F.HIDE
new VimOption("backspace",   "bs",   backspace,       S.P_GBL, nullF),
new VimOption("clipboard",   "cb",   unnamedClipboard,S.P_GBL, nullF),
new VimOption("closedfiles", "",     closedFiles, S.P_GBL, nullF),
//new VimOption("comboCommandLine","", comboCommandLine,S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("cpo_j",       "",     endOfSentence,   S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("cpo_search",  "",     searchFromEnd,   S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("cpo_w",       "",     changeWordBlanks,S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("cursorinview","civ",  cursorInView,    S.P_GBL, nullF),
new VimOption("equalalways", "ea",   equalAlways,     S.P_GBL, nullF),
new VimOption("equalprg",    "ep",   equalProgram,    S.P_GBL, nullF),
new VimOption("expandtab",   "et",   expandTabs,      S.P_BUF, nullF),
new VimOption("foldopen",    "fdo",  foldOpen,        S.P_GBL, EnumSet.of(F.COMMA, F.NODUP)),
new VimOption("formatprg",   "fp",   formatProgram,   S.P_GBL, nullF),
new VimOption("history",     "hi",   history,         S.P_GBL, nullF),
new VimOption("hlsearch",    "hls",  highlightSearch, S.P_GBL, nullF),
new VimOption("ignorecase",  "ic",   ignoreCase,      S.P_GBL, nullF),
new VimOption("incsearch",   "is",   incrSearch,      S.P_GBL, nullF),
new VimOption("iskeyword",   "isk",  isKeyWord,       S.P_BUF, EnumSet.of(F.COMMA, F.NODUP)),
new VimOption("joinspaces",  "js",   joinSpaces,      S.P_GBL, nullF),
new VimOption("linebreak",   "lbr",  lineBreak,       S.P_WIN, nullF),
new VimOption("list",        "",     list,            S.P_WIN, nullF),
new VimOption("magic",       "magic",magic,           S.P_GBL, nullF),
new VimOption("modeline",    "ml",   modeline,        S.P_GBL, nullF),
new VimOption("modelines",   "mls",  modelines,       S.P_GBL, nullF),
new VimOption("number",      "nu",   number,          S.P_WIN, nullF),
new VimOption("nrformats",   "nf",   nrFormats,       S.P_BUF, EnumSet.of(F.COMMA, F.NODUP)),
new VimOption("platformbrace","pbm", platformBraceMatch,S.P_GBL,EnumSet.of(F.HIDE)),
new VimOption("report",      "",     report,          S.P_GBL, nullF),
new VimOption("rocursorcolor","rocc",roCursorColor,   S.P_GBL, nullF),
new VimOption("scroll",      "scr",  scroll,          S.P_WIN, nullF),
new VimOption("scrolloff",   "so",   scrollOff,       S.P_GBL, nullF),
new VimOption("selection",   "sel",  selection,       S.P_GBL, nullF),
new VimOption("selectmode",  "slm",  selectMode,      S.P_GBL, nullF),
new VimOption("shell",       "sh",   shell,           S.P_GBL, nullF),
new VimOption("shellcmdflag","shcf", shellCmdFlag,    S.P_GBL, nullF),
new VimOption("shellslash",  "ssl",  shellSlash,      S.P_GBL, nullF),
new VimOption("shellxquote", "sxq",  shellXQuote,     S.P_GBL, nullF),
new VimOption("shiftround",  "sr",   shiftRound,      S.P_GBL, nullF),
new VimOption("shiftwidth",  "sw",   shiftWidth,      S.P_BUF, nullF),
new VimOption("showcmd",     "sc",   showCommand,     S.P_GBL, nullF),
new VimOption("showmode",    "smd",  showMode,        S.P_GBL, nullF),
new VimOption("sidescroll",  "ss",   sideScroll,      S.P_GBL, nullF),
new VimOption("sidescrolloff","siso",sideScrollOff,   S.P_GBL, nullF),
new VimOption("smartcase",   "scs",  smartCase,       S.P_GBL, nullF),
new VimOption("softtabstop", "sts",  softTabStop,     S.P_BUF, nullF),
new VimOption("splitbelow",  "sb",   splitBelow,      S.P_GBL, nullF),
new VimOption("splitright",  "spr",  splitRight,      S.P_GBL, nullF),
new VimOption("startofline", "sol",  startOfLine,     S.P_GBL, nullF),
new VimOption("tabCompletionPrefix","",tabCompletionPrefix, S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("tabstop",     "ts",   tabStop,         S.P_BUF, nullF),
new VimOption("textwidth",   "tw",   textWidth,       S.P_BUF, nullF),
new VimOption("tildeop",     "top",  tildeOperator,   S.P_GBL, nullF),
new VimOption("timeout",     "to",   timeout,         S.P_GBL, nullF),
new VimOption("timeoutlen",  "tm",   timeoutlen,      S.P_GBL, nullF),
new VimOption("visualbell",  "vb",   visualBell,      S.P_GBL, nullF),
new VimOption("visualbellcolor","vbc",visualBellColor,S.P_GBL, nullF),
new VimOption("visualbelltime","vbt",visualBellTime,  S.P_GBL, nullF),
new VimOption("wrap",        "",     wrap,            S.P_WIN, nullF),
new VimOption("wrapscan",    "ws",   wrapScan,        S.P_GBL, nullF),
    // SORTED ALPHABETICALLY, BEFORE F.HIDE
new VimOption("ww_bs",       "",     backspaceWrapPrevious,S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("ww_h",        "",     hWrapPrevious,   S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("ww_i_left",   "",     insertLeftWrapPrevious,S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("ww_i_right",  "",     insertRightWrapNext,S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("ww_l",        "",     lWrapNext,       S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("ww_larrow",   "",     leftWrapPrevious,S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("ww_rarrow",   "",     rightWrapNext,   S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("ww_sp",       "",     spaceWrapNext,   S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("ww_tilde",    "",     tildeWrapNext,   S.P_GBL, EnumSet.of(F.HIDE)),
new VimOption("disableFontError","", disableFontError,S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("disableFontCheckSpecial","", disableFontCheckSpecial, S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("isCursorXorBug","",   cursorXorBug,    S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("isHideVersion","",    hideVersionOption,S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("isCoordSkip", "",     coordSkip,       S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("pcmarkTrack", "",     pcmarkTrack,     S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("readOnlyHack","",     readOnlyHack,    S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("redoTrack",   "",     redoTrack,       S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("useFrame",    "",     commandEntryFrame,S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
new VimOption("viminfoMaxBuf","",    persistedBufMarks,S.P_GBL, EnumSet.of(F.HIDE, F.VERBATIM)),
    // SORTED ALPHABETICALLY, BEFORE F.HIDE
};


    private static int capacity(int c) { return (int)(c / .75) + 3; }
    final private static Map<String, VimOption> mapOptionName
            = new HashMap<>(capacity(vopts.length));
    final private static Map<String, VimOption> mapVarName
            = new HashMap<>(capacity(vopts.length));
    
    // both long/short name are in here
    final private static Map<String, VimOption> mapUserName
            = new HashMap<>(capacity(2 * vopts.length));

    // Set the maps up, verify things are as expected.
    static {
        for(VimOption vopt : vopts) {
            assert !mapOptionName.containsKey(vopt.getOptName());
            mapOptionName.put(vopt.getOptName(), vopt);
            assert !mapVarName.containsKey(vopt.getVarName());
            mapVarName.put(vopt.getVarName(), vopt);

            assert !mapUserName.containsKey(vopt.getFullName());
            mapUserName.put(vopt.getFullName(), vopt);
            if(!vopt.getShortName().isEmpty()
                    && !vopt.getFullName().equals(vopt.getShortName())) {
                if(mapUserName.containsKey(vopt.getShortName())) {
                    G.dbg.println("VimOption VERIFY: " + vopt.getShortName());
                }
                assert !mapUserName.containsKey(vopt.getShortName());
                mapUserName.put(vopt.getShortName(), vopt);
            }
        }
    }
}
