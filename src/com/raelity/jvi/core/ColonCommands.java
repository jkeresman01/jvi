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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.raelity.jvi.*;
import com.raelity.jvi.ViOutputStream.FLAGS;
import com.raelity.jvi.core.lib.AbbrevLookup;
import com.raelity.jvi.core.lib.CcFlag;
import com.raelity.jvi.core.lib.ColonCommandItem;
import com.raelity.jvi.core.lib.Messages;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.text.StringSegment;

import static com.raelity.jvi.core.CcBang.lastBangCommand;
import static com.raelity.jvi.core.Edit.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Misc01.*;
import static com.raelity.jvi.core.Util.isdigit;
import static com.raelity.jvi.core.Util.vim_str2nr;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.manager.ViManager.eatme;

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
public class ColonCommands
{
    private static final Logger LOG = Logger.getLogger(ColonCommands.class.getName());
    private static AbbrevLookup m_commands = new AbbrevLookup();

    static String lastCommand;
    static String currentCommand;

    /**
     *  Default constructor.
     */
    private ColonCommands()
    {
    }
    static { eatme(LOG); }

    private static ViCmdEntry colonCommandEntry;

    static ViCmdEntry getColonCommandEntry()
    {
        if ( colonCommandEntry == null ) {
            colonCommandEntry = ViManager.getFactory()
                    .createCmdEntry(ViCmdEntry.Type.COLON);
            colonCommandEntry.addActionListener((ActionEvent ev) -> {
                colonEntryComplete(ev);
            });
        }
        return colonCommandEntry;
    }


static private void colonEntryComplete( ActionEvent ev )
{
    try {
        Hook.setJViBusy(true);

        ViManager.getFactory().commandEntryAssist(getColonCommandEntry(),false);
        Scheduler.stopCommandEntry(getColonCommandEntry());
        String commandLine = colonCommandEntry.getCommand();
        String cmd = ev.getActionCommand();
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

static void doColonCommand(StringBuffer range)
{
    ViCmdEntry cmdEntry = getColonCommandEntry();
    ViManager.getFactory().commandEntryAssist(cmdEntry, true);
    Options.kd().println("doColonCommand --> startCommandEntry"); //REROUTE
    Scheduler.startCommandEntry(cmdEntry, ":", G.curwin, range);
}


static void executeCommand( ColonEvent cev )
{
    if(cev != null) {
        ((ActionListener)cev.commandElement.getValue()).actionPerformed(cev);
        if(cev.getEmsg() != null)
            Msg.emsg(cev.getEmsg());
    }
}


private static char modalResponse;

/** only used for parsing where we don't care about the command */
private static final ColonAction dummyColonAction = new AbstractColonAction() {
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
private static ColonEvent parseCommandGuts(String commandLine,
                                           boolean isExecuting)
{
    int sidx = 0; // index into string
    int sidx01;
    int sidx02;
    // Use the TextView as source
    ColonEvent cev = new ColonEvent(isExecuting ? G.curwin : null);
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
                            case 'y': case 'n':
                                modalResponse = c;
                                break;
                            case KeyEvent.VK_ESCAPE:
                                modalResponse = 'n';
                                break;
                            default:
                                Util.beep_flush();
                                break;
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
            if(l == 0) {
                l = 1;
            }
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
        if( ! (Util.isalpha(commandLine.charAt(sidx))
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
            if(Util.isdigit(c01)) {
                String t = commandLine.substring(sidx01, sidx+1);
                if(m_commands.hasExactCommand(t))
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
        cci = m_commands.lookupCommand(command);
        if(cci == null) {
            Msg.emsg("Not an editor command: " + command);
            Util.beep_flush();
            return null;
        }
    } else {
        // create a dummy so parse will complete ok and have useful info
        ColonCommandItem cciReal = m_commands.lookupCommand(command);
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
            // Should require that if NO_PARSE.
            cev.args = new ArrayList<>();
            cev.args.add(commandLine.substring(sidx));
        }
        if(!isExecuting || !flags.contains(CcFlag.NO_PARSE)) {
            // TODO: get rid of this
            String cmdlineargs = commandLine.substring(sidx);
            cev.args = Arrays.asList(cmdlineargs.split("\\s+"));
        }
    }

    cev.commandElement = cci;
    return cev;
}

public static ColonEvent parseCommandNoExec(String commandLine)
{
    return parseCommandGuts(commandLine, false);
}

static ColonEvent parseCommand(String commandLine)
{
    return parseCommandGuts(commandLine, true);
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
        case '!':
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
            break;
            
        case '%':
        case '#':
            if(is_escaped){
                break;
            }
            // replace %/# by the filename
            // current() is the '%' or '#'
            appendit = false;
            cmd.next(); // a possible modifier
            
            int av_idx = 0; // assume '%'
            Path path = null;
            String estr = null;
            // Handle: "#<digits>" and "#-<digit>"
            if (c == '#') {
                av_idx = -1; // assume only entered '#'
                char tchar = cmd.current();
                if(tchar == '-' || isdigit(tchar)) {
                    int number_idx = cmd.getIndex();
                    MutableInt p_len = new MutableInt();
                    MutableInt p_num = new MutableInt();
                    vim_str2nr(cmd, number_idx, null,
                               p_len, 0, 0, p_num, null);
                    // if only '-', then not a number
                    if(!(tchar == '-' && p_len.getValue() == 1)) {
                        cmd.setIndex(number_idx + p_len.getValue());
                        av_idx = p_num.getValue();
                    }
                    // grab the string now (easier debug)
                    // -1 to include the '#'
                    estr = cmd.substring(number_idx - 1, cmd.getIndex());
                }
            }
            
            ViAppView av = AppViews.getAppView(av_idx);
            if(av != null)
                path = ViManager.getFactory().getFS().getPath(av);
            if(path == null) {
                if(estr == null)
                    estr = String.valueOf(c);
                Msg.emsg("No file name to substitue for '%s'", estr);
                return null;
            }
            
            Wrap<Path> fnamep = new Wrap<>();
            FilePath.modify_fname(cmd, path, fnamep);
            cmd.previous(); // the last char handled
            sb.append(fnamep.getValue().toString());
            break;

        default:
                break;
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
        char c = s.charAt(sidx);
        switch(c) {
            case '.':         // '.' - Cursor position
                ++sidx;
                lnum.setValue(G.curwin.w_cursor.getLine());
                break;
            case '$':         // '$' - last line
                ++sidx;
                lnum.setValue(G.curbuf.getLineCount());
                break;
            case '\'':        // ''' - mark
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
                break;

            /* ******************************
            // NEEDSWORK: get_address: These are big cases....
            case '/':
            case '?':         // '/' or '?' - search

            case '\\':      // "\?", "\/" or "\&", repeat search
            *******************************/
            default:
                if(Util.isdigit(c)) {
                    sidx = getdigits(s, sidx, lnum);
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
            if (c != '-' && c != '+' && !Util.isdigit(c)) {
                break;
            }
            if (lnum.getValue() == MAXLNUM) {
                // "+1" is same as ".+1"
                lnum.setValue(G.curwin.w_cursor.getLine());
            }
            if (Util.isdigit(c)) {
                i = '+';        // "number" is same as "+number"
            } else {
                i = c;
                ++sidx;
                if(sidx >= s.length()) {
                    return -1;
                }
                c = s.charAt(sidx);
            }

            if (!Util.isdigit(c)) {  // '+' is '+1', but '+0' is not '+1'
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
 * This method returns a read-only list of the registered commands.
 * The elements of the list are {@link ColonCommandItem}.
 */
static public List<ColonCommandItem> getList()
{
    return m_commands.getList();
}

static public List<String> getNameList()
{
    return m_commands.getNameList();
}

static public List<String> getAbrevList()
{
    return m_commands.getAbrevList();
}

/**
 *
 * @param cmd a possibly abreviated command name
 * @return full command name or null if no such command
 */
static public String getFullCommandName(String cmd)
{
    ColonCommandItem cci = m_commands.lookupCommand(cmd);
    return cci == null ? null : cci.getName();
}


/**
 * Register a command; the abbrev must be unique.  The abbrev is the "key"
 * for the command, for example since "s" is the abbreviation for "substitue"
 * and "se" is the abbreviation for "set" then "substitute" sorts earlier
 * than "set" because "s" sorts earlier than "se". Consider this when
 * adding commands, since unique prefix has nothing to do with how commands
 * are recognized.
 * <pre>
 * NOTE: if the ActionListener is a ColonAction and the ColonAction
 *       has non null getFlags() then those flags are merged
 *       with the argument flags.
 * </pre>
 * @param flags may be null
 * @exception IllegalArgumentException this is thrown if the abbreviation
 * and/or the name already exist in the list or there's a null argument.
 */
public static void register( String abbrev, String name, ActionListener l,
                            Set<CcFlag> flags )
{
    EnumSet<CcFlag> newFlags = EnumSet.noneOf(CcFlag.class);
    if(flags != null)
        newFlags.addAll(flags);
    if(l instanceof ColonAction) {
        newFlags.addAll(((ColonAction)l).getFlags());
    } else {
        newFlags.add(CcFlag.NO_ARGS);
    }
    m_commands.add(abbrev, name, l, newFlags);
}

/**
    *  Deregister a command; where the abbrev is used the "key" for the command.
    *
    * @return true if the command existed and was removed.
    */
public static boolean deregister( String abbrev )
{
    return m_commands.remove(abbrev);
}

/**
 * Commands that take arguments subclass this. Some treatment of the
 * command arguments can be controled through the {@link ColonAction#getFlags}
 * method.
 */
public interface ColonAction extends ActionListener {
    /**
     * Specify some of the commands argument handling. This default
     * implementation returns zero.
     * @see CcFlag
     */
    public EnumSet<CcFlag> getFlags();

    /**
     * A string suitable for display for this action as the argument command.
     * The abrev must be an initial substring of the returned displayName.
     * The default method returns the command name with which the action
     * is registered.
     * @param cci The command this action is executing as
     * @return The string to display or null which means use the default
     */
    public String getDisplayName(ColonCommandItem cci);

    /**
     * Note that this is intended to be the same method as used in swing.
     * @return true if the action is enabled.
     */
    public boolean isEnabled();
}

public abstract static class AbstractColonAction implements ColonAction
{
    @Override
    public EnumSet<CcFlag> getFlags()
    {
        return EnumSet.noneOf(CcFlag.class);
    }

    @Override
    public String getDisplayName(ColonCommandItem cci)
    {
        return cci.getName();
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }
}


/**
    * The event passed to {@link ColonCommands.ColonAction}. It is used
    * to pass argument information. The arguments to the command are
    * white space separated. The command word finishes with the first
    * non-alpha character. So "e#" and "e #" are parsed the same.
    */
    @SuppressWarnings("serial")
    static public class ColonEvent extends ActionEvent
{
    // NEEDSWORK: ColonEvent Add stuff for getting at command arguments
    /** The expanded command word */
    String command;
    /** The command word as input */
    String inputCommand;
    /** The index of the command word on the line */
    int iInputCommand;
    /** */
    String dummyParserCommand;
    /** The command associated with this event */
    ColonCommandItem commandElement;
    /** indicates that the command word has a trailing "!" */
    boolean bang;
    /** command line as entered */
    String commandLineRaw;
    /** command line as expanded */
    String commandLine;
    /** index of the command args in the original string */
    private int iArgString;
    /** the command arguments */
    List<String> args;
    /** the text view for this event */
    ViTextView viTextView;
    /** the first line number */
    int line1;
    /** the second line number or count */
    int line2;
    /** the number of addresses given */
    int addr_count;
    /** the last addr parsed (for tab ops) */
    String lastAddr;
    /** If this gets set there's an error */
    String emsg;

    ColonEvent(ViTextView c)
    {
        super(c == null ? "" : c.getEditor(),
              ActionEvent.ACTION_PERFORMED, "");
        viTextView = c;
        args = Collections.emptyList();
    }

    /**
     * @return the first line number
     */
    public int getLine1()
    {
        return line1;
    }
    
    /**
     * @return the second line number or count
     */
    public int getLine2()
    {
        return line2;
    }
    
    /**
     * @return the number of addresses given
     */
    public int getAddrCount()
    {
        return addr_count;
    }

    public String getLastAddr()
    {
        return lastAddr.trim();
    }
    
    public ActionListener getAction()
    {
        return (ActionListener)commandElement.getValue();
    }

    public ColonCommandItem getColonCommandItem()
    {
        return commandElement;
    }
    
    /**
     * @return true if the command has a "!" appended.
     */
    public boolean isBang()
    {
        return bang;
    }
    
    /**
     * @return the textView for this command
     */
    public ViTextView getViTextView()
    {
        return viTextView;
    }
    
    /**
     * @return the number of arguments, not including command name.
     */
    public int getNArg()
    {
        return args.size();
    }
    
    /**
     * Fetch an argument.
     * @return the nth argument, n == 0 is the expanded command name.
     */
    public String getArg( int n )
    {
        return n == 0
                ? command
                : args.get(n-1) ;
    }
    
    /**
     * Fetch the list of command arguments.
     */
    public List<String> getArgs()
    {
        return new ArrayList<>(args);
    }

    /** use in dummy parse to give the command name lookup match.
     * @return lookup command, empty string if lookup failed
     */
    public String getNoExecCommandNameLookup()
    {
        return dummyParserCommand;
    }
    
    /**
     * @return the command as it was input. May be a partial command name
     */
    public String getInputCommandName()
    {
        return inputCommand;
    }

    public int getIndexInputCommandName()
    {
        return iInputCommand;
    }
    
    /**
     * @return the unparsed string of arguments
     */
    public String getArgString()
    {
        return commandLine.substring(iArgString);
    }
    
    /**
     * Fetch the command line, including commmand name
     */
    public String getCommandLine()
    {
        return commandLine;
    }

    public String getCommandLineRaw()
    {
        return commandLineRaw;
    }

    public String getEmsg() {
        return emsg;
    }

} // end inner class ColonEvent

    /**
     * This is used for several of the colon commands to translate arguments
     * into OPARG.
     * <p>
     * If we had the very complex ":" parser from ex_docmd, then this
     * would be part of it.
     * </p>
     */
    static OPARG setupExop(ColonEvent cev, boolean parseExtra)
    {
        OPARG oa = new OPARG();
        int nextArg = 1;
        if(parseExtra && cev.getNArg() >= nextArg) {
            // check for a named buffer
            String r = cev.getArg(nextArg);
            // Note a more restrictive validity test may occur later
            if(r.length() == 1
                    && valid_yank_reg(r.charAt(0), false)
                    && !Util.isdigit(r.charAt(0))) {
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
static void outputPrint(String s)
{
    makePrintStream();
    printStream.println(s);
}

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
