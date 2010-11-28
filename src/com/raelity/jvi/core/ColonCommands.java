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
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;

import com.raelity.jvi.core.AbbrevLookup.CommandElement;
import com.raelity.jvi.manager.Scheduler;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.lib.MutableInt;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;

import static com.raelity.jvi.core.ColonCommands.Flags.*;
import static com.raelity.jvi.core.Constants.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Misc01.*;

/**
 * This class handles registration, command input, parsing, dispatching
 * and in some instances execution of ":" commands. Some internal vi
 * commands, e.g. set, are executed here. Colon commands are added to
 * the list of avaialable commands through the
 * {@link #open} method.
 * <p>
 * A command is represented by an {@link java.awt.event.ActionListener}.
 * A {@link javax.swing.Action} could be used. Only commands that subclass
 * {@link ColonCommands.ColonAction} can have arguments. These commands
 * are passed a {@link ColonCommands.ColonEvent}. Actions that subclass
 * {@link javax.swing.Action} are not invoked if they are disabled. The source
 * of the {@link java.awt.event.ActionEvent}
 * is a {@link javax.swing.JEditorPane}.
 */
public class ColonCommands
{
    private static final Logger LOG = Logger.getLogger(ColonCommands.class.getName());
    private static AbbrevLookup m_commands = new AbbrevLookup();

    static String lastCommand;

    /**
     *  Default constructor.
     */
    private ColonCommands()
    {
    }

    private static ViCmdEntry colonCommandEntry;

    static ViCmdEntry getColonCommandEntry()
    {
        if ( colonCommandEntry == null ) {
            colonCommandEntry = ViManager.getFactory()
                    .createCmdEntry(ViCmdEntry.Type.COLON);
            colonCommandEntry.addActionListener(
                    new ActionListener() {
                            public void actionPerformed(ActionEvent ev) {
                                colonEntryComplete(ev);
                            }
            });
        }
        return colonCommandEntry;
    }


static private void colonEntryComplete( ActionEvent ev )
{
    try {
        Hook.setJViBusy(true);

        ViManager.getFactory().commandEntryAssist(getColonCommandEntry(),false);
        Scheduler.stopCommandEntry();
        String commandLine = colonCommandEntry.getCommand();
        String cmd = ev.getActionCommand();
        // if not <CR> must be an escape, just ignore it
        if(cmd.charAt(0) == '\n') {
            if( ! commandLine.equals("")) {
                lastCommand = commandLine;
                executeCommand(parseCommand(commandLine));
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
    Scheduler.startCommandEntry(cmdEntry, ":", G.curwin, range);
}


static void executeCommand( ColonEvent cev )
{
    if(cev != null) {
        ((ActionListener)cev.commandElement.getValue()).actionPerformed(cev);
    }
}


private static char modalResponse;

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
static ColonEvent parseCommand( String commandLine )
{
    int sidx = 0; // index into string
    int sidx01;
    int sidx02;
    // Use the TextView as source
    ColonEvent cev = new ColonEvent(G.curwin);
    boolean bang = false;
    MutableInt lnum = new MutableInt(0);
    boolean skip = false; // NEEDSWORK: executCommmand how else is this set
    cev.commandLine = commandLine;

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
        cev.line2 = G.curwin.w_cursor.getLine();
            sidx = skipwhite(commandLine, sidx);
        sidx = get_address(commandLine, sidx, skip, lnum);
        if (sidx < 0)            // error detected
            return null; // NEEDSWORK: goto doend;
        // if(lnum.getValue() == 0) {
        //   lnum.setValue(1);    // NEEDSWORK: is this right?
        // }
        if (lnum.getValue() == MAXLNUM) {
            if (commandLine.charAt(sidx) == '%') { // '%' - all lines
                ++sidx;
                cev.line1 = 1;
                cev.line2 = G.curbuf.getLineCount();
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
            modalResponse = 0;
            Msg.wmsg("Backwards range given, OK to swap (y/n)?");
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
                            Util.vim_beep();
                            break;
                    }
                    if(modalResponse != 0) {
                        ViManager.getFactory().stopModalKeyCatch();
                    }
                }
            });
            Msg.wmsg("Backwards range given, OK to swap (y/n)? " + modalResponse);
            if(modalResponse != 'y')
                return null;
            int t = cev.line1;
            cev.line1 = cev.line2;
            cev.line2 = t;
        }
    }

    // Find the command

    sidx = skipwhite(commandLine, sidx);
    if(sidx >= commandLine.length()) {
        // no command, but if a number was entered then goto that line
        if(cev.getAddrCount() > 0) {
            int l = cev.getLine1();
            if(l == 0) {
                l = 1;
            }
            gotoLine(l, BL_SOL | BL_FIX);
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
        if( ! (Util.isalpha(commandLine.charAt(sidx))
                || "<".equals(String.valueOf(commandLine.charAt(sidx)))
                || ">".equals(String.valueOf(commandLine.charAt(sidx)))))
        {
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
    cev.iArgString = sidx02;
    AbbrevLookup.CommandElement ce = m_commands.lookupCommand(command);
    if(ce == null) {
        Msg.emsg("Not an editor command: " + command);
        Util.vim_beep();
        return null;
    }

    //
    // Invoke the command
    //

    if(ce.getValue() instanceof Action) {
        if( ! ((Action)ce.getValue()).isEnabled()) {
            Msg.emsg(ce.getName() + " is not enabled");
            return null;
        }
    }
    if( ! (ce.getValue() instanceof ColonAction)) {
        // no arguments allowed
        if(sidx < commandLine.length()) {
            Msg.emsg(Messages.e_trailing);
            return null;
        }
        if(cev.getAddrCount() > 0) {
            Msg.emsg(Messages.e_norange);
            return null;
        }
    }
    if(bang
            && ( ! (ce.getValue() instanceof ColonAction)
                || (((ColonAction)ce.getValue()).getFlags() & BANG) == 0)) {
        Msg.emsg("No ! allowed");
        return null;
    }
    cev.command = ce.getName();
    cev.bang = bang;

    if(sidx < commandLine.length()) {
        cev.args = new ArrayList<String>();
        if((((ColonAction)ce.getValue()).getFlags() & NOPARSE) != 0) {
            // put the line (without command name) as the argument
            cev.args.add(commandLine.substring(sidx));
        } else {
            // parse the command into tokens
            StringTokenizer toks = new StringTokenizer(commandLine.substring(sidx));
            while(toks.hasMoreTokens()) {
                cev.args.add(toks.nextToken());
            }
        }
    }

    cev.commandElement = ce;
    return cev;
    // ((ActionListener)ce.getValue()).actionPerformed(cev);
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

        int i = 0;
        int n = 0;
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
    * The elements of the list are {@link CommandElement}.
    */
static public List<CommandElement> getList()
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
 * Register a command; the abbrev must be unique.  The abbrev is the "key"
 * for the command, for example since "s" is the abbreviation for "substitue"
 * and "se" is the abbreviation for "set" then "substitute" sorts earlier
 * than "set" because "s" sorts earlier than "se". Consider this when
 * adding commands, since unique prefix has nothing to do with how commands
 * are recognized.
 * @exception IllegalArgumentException this is thrown if the abbreviation
 * and/or the name already exist in the list or there's a null argument.
 */
public static void register( String abbrev, String name, ActionListener l )
{
    m_commands.add(abbrev, name, l);
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
public abstract static class ColonAction
        extends AbstractAction
        implements Flags
{
    /**
     * Specify some of the commands argument handling. This default
     * implementation returns zero.
     * @see Flags
     */
    public int getFlags()
    {
        return 0;
    }
}


/**
    * The event passed to {@link ColonCommands.ColonAction}. It is used
    * to pass argument information. The arguments to the command are
    * white space separated. The command word finishes with the first
    * non-alpha character. So "e#" and "e #" are parsed the same.
    */
static public class ColonEvent extends ActionEvent
{
    // NEEDSWORK: ColonEvent Add stuff for getting at command arguments
    /** The expanded command word */
    String command;
    /** The command word as input */
    String inputCommand;
    /** The command associated with this event */
    CommandElement commandElement;
    /** indicates that the command word has a trailing "!" */
    boolean bang;
    /** command line as entered */
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

    ColonEvent(ViTextView c)
    {
        super(c.getEditorComponent(), ActionEvent.ACTION_PERFORMED, "");
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

    public ActionListener getAction()
    {
        return (ActionListener)commandElement.getValue();
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
        return new ArrayList<String>(args);
    }

    /**
        * @return the expanded commandName
        */
    public String getComandName()
    {
        return command;
    }

    /**
     * @return the command as it was input. May be a partial command name
     */
    public String getInputCommandName()
    {
        return inputCommand;
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
    public String XXXgetCommandLine()
    {
        return commandLine;
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
            } catch (Exception ex) {
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
            G.curwin, ViOutputStream.LINES, lastCommand);
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

/**
 * Flags used to direct ":" command parsing. Only some of these
 * are used.
 */
public interface Flags {
  /** allow a linespecs */
  public static final int RANGE   = 0x01;
  /** allow a ! after the command name (--USED--) */
  public static final int BANG	   = 0x02;
  /** allow extra args after command name */
  public static final int EXTRA   = 0x04;       // -- XXX
  /** expand wildcards in extra part */
  public static final int XFILE   = 0x08;
  /** no spaces allowed in the extra part */
  public static final int NOSPC   = 0x10;
  /** default file range is 1,$ */
  public static final int DFLALL  = 0x20;
  /** dont default to the current file name */
  public static final int NODFL   = 0x40;
  /** argument required */
  public static final int NEEDARG = 0x80;
  /** check for trailing vertical bar */
  public static final int TRLBAR  = 0x100;
  /** allow "x for register designation */
  public static final int REGSTR  = 0x200;
  /** allow count in argument, after command */
  public static final int COUNT   = 0x400;
  /** no trailing comment allowed */
  public static final int NOTRLCOM  = 0x800;
  /** zero line number allowed */
  public static final int ZEROR   = 0x1000;
  /** do not remove CTRL-V from argument */
  public static final int USECTRLV = 0x2000;
  /** num before command is not an address */
  public static final int NOTADR = 0x4000;
  /** has "+command" argument */
  public static final int EDITCMD = 0x8000;
  /** accepts buffer name */
  public static final int BUFNAME = 0x10000;
  /** multiple extra files allowed */
  public static final int FILES   = (XFILE | EXTRA);
  /** one extra word allowed */
  public static final int WORD1   = (EXTRA | NOSPC);
  /** 1 file allowed, defaults to current file */
  public static final int FILE1   = (FILES | NOSPC);
  /** 1 file allowed, defaults to "" */
  public static final int NAMEDF  = (FILE1 | NODFL);
  /** multiple files allowed, default is "" */
  public static final int NAMEDFS = (FILES | NODFL);


  //
  // Additional stuff
  //
  /** don't parse command into words, arg1 is one big line. */
  public static final int NOPARSE = 0x00020000;
}

} // end com.raelity.jvi.ColonCommand

// vi:set sw=4 ts=8 et:
