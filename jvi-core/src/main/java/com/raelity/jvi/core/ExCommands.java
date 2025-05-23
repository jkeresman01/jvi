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
 * Copyright (C) 2011 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import com.raelity.jvi.*;
import com.raelity.jvi.ViOutputStream.FLAGS;
import com.raelity.jvi.core.lib.CcFlag;
import com.raelity.jvi.core.lib.ColonCommandItem;
import com.raelity.jvi.core.lib.Messages;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.*;
import com.raelity.jvi.lib.StringSegment;
import com.raelity.jvi.lib.TextUtil;

import static java.util.logging.Level.*;

import static com.raelity.jvi.core.CcBang.lastBangCommand;
import static com.raelity.jvi.core.Edit.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Misc01.*;
import static com.raelity.jvi.core.Register.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.manager.ViManager.eatme;

import static com.raelity.jvi.lib.TextUtil.sf;

/**
 * This class handles registration, command input, parsing, dispatching
 * and in some instances execution of ":" commands. Some internal vi
 * commands, e.g. set, are executed here. Colon commands are added to
 * the list of available commands through the
 * {@link #register} method.
 * <p>
 * A command is represented by an {@link java.awt.event.ActionListener}.
 * Typically a subclass is used. Only actions that implement
 * {@link ColonCommands.ColonAction} get command line parsing for
 * arguments, ranges. The actions get the command line arguments through
 * the {@link ColonCommands.ColonEvent}. Actions that implement
 * {@link ColonCommands.ColonAction} are not invoked if 
 * their isEnabled method returns false. The source
 * of the {@link java.awt.event.ActionEvent}
 * is often a JEditorPane for swing implementations.
 */
public class ExCommands
{
    private static final Logger LOG = Logger.getLogger(ExCommands.class.getName());
    static final DebugOption dbg = Options.getDebugOption(Options.dbgSearch);

    static String lastCommand;
    static String currentCommand;

    /**
     *  Default constructor.
     */
    private ExCommands()
    {
    }
    static { eatme(LOG); }

    private static ViCmdEntry colonCommandEntry;

    static ViCmdEntry getColonCommandEntry()
    {
        if ( colonCommandEntry == null ) {
            colonCommandEntry = ViManager.getFactory()
                    .createCmdEntry(ViCmdEntry.Type.COLON);
            ViCmdEntry.getEventBus().register(new Object()
            {
                @Subscribe
                public void entryComplete(ViCmdEntry.CmdEntryComplete ev)
                {
                    if(ev.getSource() == colonCommandEntry)
                        colonEntryComplete(ev.getActionCommand(), ev);
                }
            });
        }
        return colonCommandEntry;
    }


static private void colonEntryComplete(String cmd, Object ev)
{
    try {
        // TODO: make dbg local static after Commands.register called directly
        dbg.printf(INFO, () -> sf("COLON: colonEntryComplete: %s \n", ev.toString()));
        Hook.setJViBusy(true);

        ViManager.getFactory().commandEntryAssist(getColonCommandEntry(),false);
        Scheduler.stopCommandEntry(getColonCommandEntry());
        String commandLine = getColonCommandEntry().getCommand();
        //String cmd = ev.getActionCommand();
        // if not <CR> must be an escape, just ignore it
        if(cmd.charAt(0) == '\n') {
            if( ! commandLine.isEmpty()) {
                currentCommand = commandLine;
                executeCommand(parseCommand(commandLine));
                lastCommand = currentCommand;
            }
        }
        closePrint();
    } finally {
        Hook.setJViBusy(false);
    }
}

static void doColonCommand(StringBuilder range)
{
    ViCmdEntry cmdEntry = getColonCommandEntry();
    ViManager.getFactory().commandEntryAssist(cmdEntry, true);
    Options.kd().println("doColonCommand --> startCommandEntry"); //REROUTE
    Scheduler.startCommandEntry(cmdEntry, ":", G.curwin, range);
}


static void executeCommand( Commands.ColonEvent cev )
{
    if(cev != null) {
        ((ActionListener)cev.commandElement.getValue()).actionPerformed(cev);
        if(cev.getEmsg() != null)
            Msg.emsg(cev.getEmsg());
    }
}


private static char modalResponse;

/** only used for parsing where we don't care about the command */
private static final Commands.ColonAction dummyColonAction = new Commands.AbstractColonAction() {
        @Override public EnumSet<CcFlag> getFlags() {
            return EnumSet.of(CcFlag.BANG, CcFlag.RANGE);
        }

        @Override public boolean isEnabled() {
            return true;
        }
        
        @Override public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
/** dummy action for parse only, no exec, but merge in the real flags */
private static ColonCommandItem
createDummyColonCommandItem(ColonCommandItem cci)
{
    EnumSet<CcFlag> flags = dummyColonAction.getFlags();
    if(cci != null)
        flags.addAll(cci.getFlags());
    return new ColonCommandItem("xyzzy", "xyzzy", dummyColonAction, flags);

}

/**
    * Parse (partially) the command.
    * Return null if parse failure or otherwise can't execute,
    * otherwise return the colonEvent for the command.
    *
    * Following is the stock vim comment
    * <pre>
    * Execute one Ex command.
    *
    * If 'sourcing' is TRUE, the command will be included in the error message.
    *
    * 2. skip comment lines and leading space
    * 3. parse range
    * 4. parse command
    * 5. parse arguments
    * 6. switch on command name
    *
    * Note: "getline" can be NULL.
    *
    * This function may be called recursively!
    * </pre>
    */
private static Commands.ColonEvent parseCommandGuts(String commandLine,
                                           boolean isExecuting)
{
    int sidx = 0; // index into string
    int sidx01;
    int sidx02;
    // Use the TextView as source
        Commands.ColonEvent cev = new Commands.ColonEvent(isExecuting ? G.curwin : null);
    boolean bang = false;
    MutableInt lnum = new MutableInt(0);
    boolean skip = !isExecuting; // skip mark lookup in get_address
    cev.commandLine = commandLine;
    cev.commandLineRaw = commandLine;

    //
    // 3. parse a range specifier of the form: addr [,addr] [;addr] ..
    //
    // where 'addr' is:
    //
    // %          (entire file)
    // $  [+-NUM]
    // 'x [+-NUM] (where x denotes a currently defined mark)
    // .  [+-NUM]
    // [+-NUM]..
    // NUM
    //
    // The ea.cmd pointer is updated to point to the first character following
    // the range spec. If an initial address is found, but no second, the upper
    // bound is equal to the lower.
    //
    // cev.line1, cev.line2 are set to the range values

    // NEEDSWORK: parse a range!
    // while(parsing line number) {
    //   if(range number) {
    //     cev.line2 = range number
    //     ++cev.addr_count;
    //   }
    // }

    // repeat for all ',' or ';' separated addresses
    for (;;) {
        cev.line1 = cev.line2;
            // default is current line number
        cev.line2 = isExecuting ? G.curwin.w_cursor.getLine() : 1;
        sidx = skipwhite(commandLine, sidx);
        int idxAddr = sidx;
        sidx = get_address(commandLine,
                           sidx,
                           skip,
                           lnum);
        if (sidx < 0)            // error detected
            return null; // NEEDSWORK: goto doend;
        cev.lastAddr = commandLine.substring(idxAddr, sidx);
        // if(lnum.getValue() == 0) {
        //   lnum.setValue(1);    // NEEDSWORK: is this right?
        // }
        if (lnum.getValue() == MAXLNUM) {
            if (sidx < commandLine.length()
                    && commandLine.charAt(sidx) == '%' // '%' - all lines
            ) {
                ++sidx;
                cev.line1 = 1;
                cev.line2 = isExecuting ? G.curbuf.getLineCount() : MAXLNUM - 1;
                ++cev.addr_count;
            }
            /* ************************************************************
                NOT SURE WHAT THIS ADDRESS IS
                            // '*' - visual area
            else if (*ea.cmd == '*' && vim_strchr(p_cpo, CPO_STAR) == NULL)
            {
            FPOS        *fp;

            ++ea.cmd;
            if (!ea.skip)
            {
                fp = getmark('<', FALSE);
                if (check_mark(fp) == FAIL)
                goto doend;
                ea.line1 = fp->lnum;
                fp = getmark('>', FALSE);
                if (check_mark(fp) == FAIL)
                goto doend;
                ea.line2 = fp->lnum;
                ++ea.addr_count;
            }
            }
            ************************************************************/
        } else {
            cev.line2 = lnum.getValue();
        }
        cev.addr_count++;

        /* *****************************************************************
        if (*ea.cmd == ';')
        {
            if (!ea.skip)
            curwin->w_cursor.lnum = ea.line2;
        } else
        ********************************************************************/
        if (sidx >= commandLine.length() || commandLine.charAt(sidx) != ',')
            break;
        ++sidx;
    }

    // If one address given: set start and end lines
    if (cev.addr_count == 1) {
        cev.line1 = cev.line2;
        // ... but only implicit: really no address given
        if (lnum.getValue() == MAXLNUM) {
            cev.addr_count = 0;
        }
    }

    if(cev.addr_count > 1) {
        if(cev.line1 > cev.line2) {
            if(isExecuting) {
                modalResponse = 0;
                Msg.wmsg("Backwards range given, OK to swap (y/n)?");
                // NEEDSWORK: not exactly what vim's ask_yesno does.
                ViManager.getFactory().startModalKeyCatch(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        e.consume();
                        char c = e.getKeyChar();
                        switch(c) {
                            case 'y', 'n' -> modalResponse = c;
                            case KeyEvent.VK_ESCAPE -> modalResponse = 'n';
                            default -> beep_flush();
                        }
                        if(modalResponse != 0) {
                            ViManager.getFactory().stopModalKeyCatch();
                        }
                    }
                });
                Msg.wmsg("Backwards range given, OK to swap (y/n)? "
                         + modalResponse);
                if(modalResponse != 'y')
                    return null;
            }
            int t = cev.line1;
            cev.line1 = cev.line2;
            cev.line2 = t;
        }
    }

    // Find the command

    sidx = skipwhite(commandLine, sidx);
    if(sidx >= commandLine.length()) {
        // no command, but if a number was entered then goto that line
        if(isExecuting && cev.getAddrCount() > 0) {
            int l = cev.getLine1();
            if(l < 1) {
                l = 1;
            } else if(l > G.curbuf.getLineCount())
                l = G.curbuf.getLineCount();
            ViFPOS fpos = fpos();
            fpos.set(l, 0);
            beginline(fpos, BL_SOL | BL_FIX).copyTo(G.curwin.w_cursor);
            return null;
        }
    }

    // if(sidx >= commandLine.length()) { return null; }
    sidx01 = sidx;
    // skip command name characters // NEEDSWORK: not just alpha
    for(; sidx < commandLine.length(); sidx++) {
        // NEEDSWORK: commands can be more than just alpha chars
        //            for now just hack in the two non-alpha commands we've got
        //if( ! Util.isalpha(commandLine.charAt(sidx)))
        //NOTE: might use vim_strchr("&<>")
        if( ! (isalpha(commandLine.charAt(sidx))
                || "&".equals(String.valueOf(commandLine.charAt(sidx)))
                || "<".equals(String.valueOf(commandLine.charAt(sidx)))
                || ">".equals(String.valueOf(commandLine.charAt(sidx)))))
        {
            // HACK. Deal with "ls2" and such, when written, digits could
            // not be in a command name. The digit became the first argument.
            // It may be that this "feature" is never used, in which case
            // it would be OK to just include digits.
            //
            // But, at this time, rather than risk a strangeness,
            // check if including a digit, produces a command name.
            // Thus a string ending in a single digit might be a command

            char c01 = commandLine.charAt(sidx);
            if(isdigit(c01)) {
                String t = commandLine.substring(sidx01, sidx+1);
                if(Commands.hasExactCommand(t))
                    sidx++;
            }
            break;
        }
    }
    sidx02 = sidx;
    if(sidx < commandLine.length() && commandLine.charAt(sidx) == '!') {
        if (sidx > sidx01) bang = true;
        else ++sidx02;
        ++sidx;
    }
    sidx = skipwhite(commandLine, sidx);

    String command = commandLine.substring(sidx01, sidx02);
    cev.inputCommand = command;
    cev.iInputCommand = sidx01;

    cev.iArgString = sidx02;
    ColonCommandItem cci;
    if(isExecuting) {
        cci = Commands.lookupCommand(command);
        if(cci == null) {
            Msg.emsg("Not an editor command: " + command);
            beep_flush();
            return null;
        }
    } else {
        // create a dummy so parse will complete ok and have useful info
        ColonCommandItem cciReal = Commands.lookupCommand(command);
        cev.dummyParserCommand = cciReal != null ? cciReal.getName() : "";
        cci = createDummyColonCommandItem(cciReal);
    }
    Set<CcFlag> flags = cci.getFlags();

    //
    // Invoke the command.
    // Actually, check for command usage issues
    // then parse the arguments.
    //

    if( ! cci.isEnabled()) {
        Msg.emsg(cci.getName() + " is not enabled");
        return null;
    }
    if(sidx < commandLine.length() && flags.contains(CcFlag.NO_ARGS)) {
        Msg.emsg(Messages.e_trailing);
        return null;
    }
    if(cev.getAddrCount() > 0 && !flags.contains(CcFlag.RANGE)) {
        Msg.emsg(Messages.e_norange);
        return null;
    }
    if(bang && !flags.contains(CcFlag.BANG)) {
        Msg.emsg("No ! allowed");
        return null;
    }

    cev.command = cci.getName();
    cev.bang = bang;

    if(isExecuting && flags.contains(CcFlag.XFILE)) {
        // expand starting at first char after command
        // may point to "!" (insane). If XFILE, don't use CcFlags.BANG I guess.
        StringSegment cmd = new StringSegment(commandLine);
        cmd.setIndex(cev.iArgString);
        StringBuilder sb = new StringBuilder(commandLine.substring(0, cev.iArgString));
        sb = expand_filename(cmd, cci, sb);
        if(sb == null)
            return null;
        commandLine = sb.toString();
        cev.commandLine = commandLine;
    }

    // When isExecuting is false, should always parse since don't know what
    // the completion code might be looking for.

    if(sidx < commandLine.length()) {
        if(flags.contains(CcFlag.NO_PARSE)) {
            // Put the line (without command name) as the argument.
            // But probably will get picked up with getArgString.
            // TODO: Should require that if NO_PARSE.
            cev.args = new ArrayList<>();
            cev.args.add(commandLine.substring(sidx));
        }
        if(!isExecuting || !flags.contains(CcFlag.NO_PARSE)) {
            // TODO: get rid of this
            cev.args = TextUtil.tokens(commandLine.substring(sidx));
        } else {
        }
    }

    cev.commandElement = cci;
    return cev;
}

public static Commands.ColonEvent parseCommandNoExec(String commandLine)
{
    return parseCommandGuts(commandLine, false);
}

static Commands.ColonEvent parseCommand(String commandLine)
{
    return parseCommandGuts(commandLine, true);
}

static String getaltfname(boolean errmsg)
{
    ViAppView av = AppViews.getAppView(-1);
    String fname = "";
    if(av != null) {
        Path path = av.getPath();
        if(path != null)
            fname = VimPath.getVimDisplayPath(path);
    }
    return fname;
}

static StringBuilder expand_filename(StringSegment cmd,
                                     ColonCommandItem cci, StringBuilder sb)
{
    //StringBuilder cmd = new StringBuilder();
    //StringSegment cmd = new StringSegment(command_line);
    if(sb == null)
        sb = new StringBuilder();
    boolean appendit;
    boolean is_escaped;
    for(char c = cmd.current(); !cmd.atEnd(); c = cmd.next()) {
        appendit = true;
        is_escaped = false;
        if(c == '\\') {
            if(cmd.next() == NUL)
                cmd.previous();
            else {
                c = cmd.current();
                is_escaped = true;
            }
        }
        switch(c) {
        case '!' -> {
            if (is_escaped) {
                break;
            } else {
                // replace this ! by last command, if it exists
                if (lastBangCommand != null) {
                    sb.append(lastBangCommand);
                    appendit = false;
                } else {
                    Msg.emsg("No previous command");
                    return null;
                }
            }
        }
        case '%', '#' -> {
            if(is_escaped){
                break;
            }
            // replace %/# by the filename
            // current() is the '%' or '#'
            appendit = false;
            cmd.next(); // a possible modifier
            
            int parse_idx = cmd.getIndex();
            int av_idx = 0; // assume '%'
            // Handle: "#<digits>" and "#-<digit>"
            if (c == '#') {
                av_idx = -1; // assume only entered '#'
                char tchar = cmd.current();
                if(tchar == '-' || isdigit(tchar)) {
                    int tint = vim_str2nr(cmd);
                    if(cmd.getIndex() != parse_idx)
                        av_idx = tint;
                }
            }
            
            ViAppView av = AppViews.getAppView(av_idx);
            Path path = null;
            String estr = null;
            if(av != null)
                path = av.getPath();
            if(path == null) {
                // -1 to include the '%'/'#'
                String fnShortcut = cmd.substring(parse_idx - 1, cmd.getIndex());
                if(av != null) {
                    // An av without a path, pass the shortcut on
                    sb.append(fnShortcut);
                    break;
                }
                if(estr == null)
                    estr = fnShortcut;
                Msg.emsg("No file name to substitue for '%s'", estr);
                return null;
            }
            
            Wrap<Path> fnamep = new Wrap<>();
            VimPath.modify_fname(cmd, path, fnamep);
            cmd.previous(); // the last char handled
            sb.append(fnamep.getValue().toString());
        }
        default -> { }
        } // end switch
        if(appendit)
            sb.append(c);
    }
    return sb;
}

/**
 * get a single EX address
 *
 * Set ptr to the next character after the part that was interpreted.
 * Set ptr to NULL when an error is encountered.
 *
 * Return MAXLNUM when no Ex address was found.
 *
 * @param skip only skip the address, don't use it
 * @return the index of next character after the part that was interpreted
 * else minus one if there was an error;
 */
static int get_address(
        String s,
        int sidx,
        boolean skip,
        MutableInt lnum )
{
    // note that the return sidx is set to minus one if there is an error

    sidx = skipwhite(s, sidx);
    lnum.setValue(MAXLNUM);
    if(sidx == s.length())
        return sidx;
    do {
        /* ******************************
        // NEEDSWORK: get_address: These are big cases....
        case '/':
        case '?':         // '/' or '?' - search

        case '\\':      // "\?", "\/" or "\&", repeat search
        *******************************/
        char c = s.charAt(sidx);
        switch(c) {
        case '.' -> { // '.' - Cursor position
            ++sidx;
            lnum.setValue(G.curwin.w_cursor.getLine());
        }
        case '$' -> { // '$' - last line
            ++sidx;
            lnum.setValue(G.curbuf.getLineCount());
        }
        case '\'' -> { // ''' - mark
            ++sidx;
            if(sidx >= s.length()) {
                return -1;
            }
            c = s.charAt(sidx);
            if(skip) {
                ++sidx;
            } else {
                ViMark fp = MarkOps.getmark(c, false);
                ++sidx;
                if(MarkOps.check_mark(fp) == FAIL) {
                    return -1;
                }
                lnum.setValue(fp.getLine());
            }
        }
        default -> {
            if(isdigit(c)) {
                sidx = getdigits(s, sidx, lnum);
            }
        }
        }

        int i;
        int n;
        for (;;) {
            sidx = skipwhite(s, sidx);
            if(sidx >= s.length()) {
                break;
            }
            c = s.charAt(sidx);
            if (c != '-' && c != '+' && !isdigit(c)) {
                break;
            }
            if (lnum.getValue() == MAXLNUM) {
                // "+1" is same as ".+1"
                lnum.setValue(G.curwin.w_cursor.getLine());
            }
            if (isdigit(c)) {
                i = '+';        // "number" is same as "+number"
            } else {
                i = c;
                ++sidx;
                if(sidx >= s.length()) {
                    return -1;
                }
                c = s.charAt(sidx);
            }

            if (!isdigit(c)) {  // '+' is '+1', but '+0' is not '+1'
                n = 1;
            } else {
                MutableInt mi = new MutableInt(0);
                sidx = getdigits(s, sidx, mi);
                n = mi.getValue();
            }

            if (i == '-') {
                lnum.setValue(lnum.getValue() - n);
            } else {
                lnum.setValue(lnum.getValue() + n);
            }
        }
    } while(false); //NEEDSWORK:  while (*cmd == '/' || *cmd == '?');

    return sidx;
}

/**
 *
 * @param cmd a possibly abreviated command name
 * @return full command name or null if no such command
 */
static public String getFullCommandName(String cmd)
{
    ColonCommandItem cci = Commands.lookupCommand(cmd);
    return cci == null ? null : cci.getName();
}

    /**
     * This is used for several of the colon commands to translate arguments
     * into OPARG.
     * <p>
     * If we had the very complex ":" parser from ex_docmd, then this
     * would be part of it.
     * </p>
     */
    static OPARG setupExop(Commands.ColonEvent cev, boolean parseExtra)
    {
        OPARG oa = new OPARG();
        int nextArg = 1;
        if(parseExtra && cev.getNArg() >= nextArg) {
            // check for a named buffer
            String r = cev.getArg(nextArg);
            // Note a more restrictive validity test may occur later
            if(r.length() == 1
                    && valid_yank_reg(r.charAt(0), false)
                    && !isdigit(r.charAt(0))) {
                oa.regname = r.charAt(0);
                nextArg++;
            }
        }
        oa.start = new FPOS();
        oa.start.set(cev.getLine1(), 0);
        oa.end = new FPOS();
        oa.end.set(cev.getLine2(), 0);
        oa.line_count = cev.getLine2() - cev.getLine1() + 1;
        if(parseExtra && cev.getNArg() == nextArg) {
            try {
                oa.line_count = Integer.parseInt(cev.getArg(nextArg));
                // shuffle/recalc the range
                oa.start = oa.end;
                oa.end = new FPOS();
                oa.end.set(oa.start.getLine() + oa.line_count - 1, 0);
                nextArg++;
            } catch (NumberFormatException ex) {
                Msg.emsg(Messages.e_trailing);
                oa.error = true;
            }
        }
        if(!oa.error) {
            if(parseExtra && cev.getNArg() >= nextArg) {
                Msg.emsg(Messages.e_trailing);
                oa.error = true;
            }
        }
        if(!oa.error) {
            oa.motion_type = MLINE;
            if(cev.getAction() != Cc01.getActionYank()) {
                MarkOps.setpcmark();
                G.curwin.w_cursor.set(oa.start);
                Edit.beginline(BL_SOL|BL_FIX);
            }
        }
        return oa;
    }

//
// handle the print stream
//

/**
    * The current print stream (text window) is automatically created if there
    * is no current one.
    *
    * It is expected that it will be closed at the end of
    * the current command.
    */
private static ViOutputStream printStream;

private static void makePrintStream()
{
    if(printStream != null) {
        return;
    }
    printStream = ViManager.createOutputStream(
            G.curwin, ViOutputStream.LINES, currentCommand,
            EnumSet.of(FLAGS.NEW_YES, FLAGS.RAISE_YES, FLAGS.CLEAR_NO));
}

/**
 * Output a string to the current text window.
 */
//static void outputPrint(String s)
//{
//    makePrintStream();
//    printStream.println(s);
//}

/**
    * Output the specified document line to the current text window.
    */
static void outputPrint(int line, int offset, int length)
{
    makePrintStream();
    printStream.println(line, offset, length);
}

/**
    * Close the current print output stream.
    */
static void closePrint()
{
    if(printStream == null) {
        return;
    }
    printStream.close();
    printStream = null;
}

} // end com.raelity.jvi.ColonCommand

// vi:set sw=4 ts=8 et:
