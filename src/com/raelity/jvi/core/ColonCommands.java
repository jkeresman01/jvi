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

import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.options.BooleanOption;
import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.CharacterIterator;
import java.util.List;
import java.util.ArrayList;
import java.util.TooManyListenersException;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.AbstractAction;
import javax.swing.Action;
import com.raelity.jvi.ViTextView.TAGOP;

import static com.raelity.jvi.core.Constants.*;
import static com.raelity.jvi.core.ColonCommandFlags.*;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

/**
 * This class handles registration, command input, parsing, dispatching
 * and in some instances execution of ":" commands. Some internal vi
 * commands, e.g. set, are executed here. Colon commands are added to
 * the list of avaialable commands through the
 * {@link #register} method.
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
private static Logger LOG = Logger.getLogger(ColonCommands.class.getName());
private static AbbrevLookup m_commands = new AbbrevLookup();

static String lastCommand;
static String lastBangCommand = null;

// ............


/**
    *  Default constructor.
    */
private ColonCommands()
{
}


private static ViCmdEntry colonCommandEntry;

private static ViCmdEntry getColonCommandEntry()
{
    if ( colonCommandEntry == null ) {
        try {
            colonCommandEntry = ViManager.getViFactory()
                                    .createCmdEntry(ViCmdEntry.COLON_ENTRY);
            colonCommandEntry.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        colonEntryComplete(ev);
                    }
            });
        } catch ( TooManyListenersException ex ) {
            LOG.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    return colonCommandEntry;
}


static private void colonEntryComplete( ActionEvent ev )
{
    try {
        ViManager.setJViBusy(true);

        ViManager.getViFactory().commandEntryAssist(getColonCommandEntry(),false);
        ViManager.stopCommandEntry();
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
        ViManager.setJViBusy(false);
    }
}

static void doColonCommand(StringBuffer range)
{
    ViCmdEntry cmdEntry = getColonCommandEntry();
    ViManager.getViFactory().commandEntryAssist(cmdEntry, true);
    ViManager.startCommandEntry(cmdEntry, ":", G.curwin, range);
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
            sidx = Misc.skipwhite(commandLine, sidx);
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
            ViManager.getViFactory().startModalKeyCatch(new KeyAdapter() {
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
                        ViManager.getViFactory().stopModalKeyCatch();
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

    sidx = Misc.skipwhite(commandLine, sidx);
    if(sidx >= commandLine.length()) {
        // no command, but if a number was entered then goto that line
        if(cev.getAddrCount() > 0) {
            int l = cev.getLine1();
            if(l == 0) {
                l = 1;
            }
            Misc.gotoLine(l, BL_SOL | BL_FIX);
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
    sidx = Misc.skipwhite(commandLine, sidx);

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
private static int get_address(
        String s,
        int sidx,
        boolean skip,
        MutableInt lnum )
{
    // note that the return sidx is set to minus one if there is an error

    sidx = Misc.skipwhite(s, sidx);
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
                    sidx = Misc.getdigits(s, sidx, lnum);
                }
        }

        int i = 0;
        int n = 0;
        for (;;) {
            sidx = Misc.skipwhite(s, sidx);
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
                sidx = Misc.getdigits(s, sidx, mi);
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
    * The elements of the list are {@link AbbrevLookup.CommandElement}.
    */
static public List getCommandList()
{
    return m_commands.getList();
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
static abstract public class ColonAction
        extends AbstractAction
        implements ColonCommandFlags
{
    /**
     * Specify some of the commands argument handling. This default
     * implementation returns zero.
     * @see ColonCommandFlags
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
    AbbrevLookup.CommandElement commandElement;
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
        if(args == null) {
            return 0;
        }
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
        return args;
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


public static class BangAction extends ColonAction
{
    FilterThreadCoordinator coord = null;
    BooleanOption dbg = (BooleanOption)Options.getOption(Options.dbgBang);

    public void actionPerformed(ActionEvent ev)
    {
        if(coord != null) {
            Msg.emsg("Only one command at a time.");
            return;
        }
        ColonEvent evt = (ColonEvent)ev;
        int nArgs = evt.getNArg();
        boolean isFilter = (evt.getAddrCount() > 0);

        if (dbg.getBoolean()) {
            System.err.println("!: Original command: '" + evt.getArgString() + "'");
        }
        StringBuilder arg = new StringBuilder(evt.getArgString());
        arg = parseBang(arg);
        if (arg == null) {
            Msg.emsg("No previous command");
            return;
        }
        if (dbg.getBoolean()) {
            System.err.println("!: Substitution '" + arg + "'");
        }

        if (nArgs >= 1) {
            String cmd = arg.toString();
            doBangCommand(evt, cmd);
            if(coord == null) {
                return;
            }
            lastBangCommand = cmd;
        } else {
            lastBangCommand = "";
        }
        if (dbg.getBoolean()) {
            System.err.println("!: Last command saved '" + lastBangCommand + "'");
        }
    }

    private void joinThread(Thread t)
    {
        if(dbg.getBoolean()) {
            System.err.println("!: joining thread " + t.getName());
        }
        if(t.isAlive()) {
            t.interrupt();
            try {
                t.join(50);
            } catch (InterruptedException ex) {
            }
            if(t.isAlive()) {
                System.err.println("Thread " + t.getName() + " won't die");
            }
        }
    }

    void finishBangCommand(boolean fOK)
    {
        assert(EventQueue.isDispatchThread());
        if(coord == null)
            return;
        //System.err.println("!: DONE :!" + (!fOK ? " ABORT" : ""));
        ViManager.getViFactory().stopGlassKeyCatch();
        Msg.wmsg("");

        if(!fOK) {
            // NEEDSWORK: set write to process thread to not bother cleaning up
            // set flag in coord so we get a rollback undo
            try {
                int exit = coord.process.exitValue();
            } catch(IllegalThreadStateException ex) {
                if(dbg.getBoolean()) {
                    System.err.println("!: destroying process");
                }
                coord.process.destroy();
            }
            if(coord.simpleExecuteThread != null) {
                coord.simpleExecuteThread.interrupt();
            } else {
                coord.documentThread.interrupt();
                coord.readFromProcessThread.interrupt();
                coord.writeToProcessThread.interrupt();
            }
        }

        if(coord.simpleExecuteThread != null) {
            joinThread(coord.simpleExecuteThread);
        } else {
            joinThread(coord.documentThread);
            joinThread(coord.readFromProcessThread);
            joinThread(coord.writeToProcessThread);
        }

        /* THESE CALLS WILL PROBABLY DEADLOCK.
            * IN ONE EXAMPLE, WE ARE IN A STUCK IN BUFFERED READER
            * READ, CALLING CANCEL ATTEMPTS TO CLOSE THE READER (FROM A
            * DIFFERENT THREAD NOLESS) THE CLOSE HANGS.
            *
            * WE'VE DONE LOTS TO TRY AND KILL THE THREAD CLEANLY,
            * IF IT WONT DIE, THEN TOO BAD.
            // These calls to cleanup don't make much sense,
            // we're in the wrong thread context.
            // But then again, they should be no-ops
            if(coord.simpleExecuteThread != null) {
            coord.simpleExecuteThread.cleanup();
            } else {
            coord.documentThread.cleanup();
            coord.readFromProcessThread.cleanup();
            coord.writeToProcessThread.cleanup();
            }
        */

        coord = null;
        if(!fOK) {
            return;
        }
    }

    private StringBuilder parseBang(StringBuilder sb)
    {
        StringBuilder newsb = new StringBuilder();
        int index = 0;
        while(index < sb.length()) {
            char c = sb.charAt(index);
            switch(c) {

                case '!':
                    if (escaped(index, sb)) {
                        // looking at "!" following a "\"
                        // means we already appended a \ to newsb
                        // so we replace it by !
                        newsb.setCharAt(newsb.length() - 1, '!');
                    } else {
                        // replace this ! by last command, if it exists
                        if (lastBangCommand != null) {
                            newsb.append(lastBangCommand);
                        } else {
                            return null; // Error, no last command
                        }
                    }
                    index++;
                    break;

                case '%':
                    if(escaped(index, sb)){
                        // Again, replace the last \ with %
                        newsb.setCharAt(newsb.length() - 1, '%');
                        index++;
                    } else {
                        // replace % by the filename
                        // watch for the filename modifiers ':x'
                        //
                        // NEESDWORK: filename-modifiers not handled in vim compatible way
                        // Not all file modifiers are supported and only one is allowed,
                        // allowing multiple will require rework here and getFileName.
                        //
                        if(index+2 < sb.length() && sb.charAt(index+1) == ':'){
                            newsb.append(G.curbuf.modifyFilename(sb.charAt(index+2)));
                            index += 3;
                        } else {
                            newsb.append(G.curbuf.modifyFilename(' '));
                            index++;
                        }
                    }
                    break;

                default:
                    newsb.append(c);
                    index++;

            } // end switch
        }
        return newsb;
    }

    private boolean escaped(int index, StringBuilder sb)
    {
        if (index == 0 || sb.charAt(index - 1) != '\\') {
            return false;
        } else {
            return true;
        }
    }

    private String commandLineToString(List<String> cl)
    {
        int nArgs = cl.size();
        StringBuilder result = new StringBuilder();

        if (nArgs > 0) {
            result.append(cl.get(0));
        }

        for (int i = 1; i < nArgs; i++) {
            result.append(' ' + cl.get(i));
        }

        return result.toString();
    }

    private boolean doBangCommand(ColonEvent evt, String commandLine)
    {
        boolean isFilter = (evt.getAddrCount() > 0);

        ArrayList<String> shellCommandLine = new ArrayList<String>(3);
        String shellXQuote = G.p_sxq.getString();

        shellCommandLine.add(G.p_sh.getString());
        shellCommandLine.add(G.p_shcf.getString());
        shellCommandLine.add(shellXQuote + commandLine + shellXQuote);

        if (dbg.getBoolean()) {
            System.err.println(
                    "!: ProcessBuilder: '" + shellCommandLine + "'");
        }

        ProcessBuilder pb = new ProcessBuilder(shellCommandLine);
        pb.redirectErrorStream(true);

        Process p;
        try {
            p = pb.start();
        } catch (IOException ex) {
            String s = ex.getMessage();
            Msg.emsg(s == null || s.equals("") ? "exec failed" : s);
            return false;
        }

        // Got a process, setup coord, start modal operation

        coord = new FilterThreadCoordinator(
                this, setupExop(evt, false), p);

        // NEEDSWORK: put this where it can be repeated
        coord.statusMessage = "Enter 'Ctrl-C' to ABORT";
        Msg.fmsg(coord.statusMessage);

        ViManager.getViFactory().startGlassKeyCatch(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    e.consume();
                    if(e.getKeyChar() == (KeyEvent.VK_C & 0x1f)
                        && (e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                        finishBangCommand(false);
                    } else {
                        Util.vim_beep();
                    }
                }
            });

        // start the thread(s)

        if (dbg.getBoolean()) {
            System.err.println("!: starting threads");
        }

        String fullCommand = commandLineToString(shellCommandLine);
        if (isFilter) {
            outputFromProcessToFile(evt, p, fullCommand);
        } else {
            outputToViOutputStream(evt, p, fullCommand);
        }

        return true;
    }

    private void outputToViOutputStream(
            ColonEvent evt,
            Process p,
            String cmd )
    {

        BufferedReader br;
        ViOutputStream vos;
        br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        vos = ViManager.createOutputStream(null, ViOutputStream.OUTPUT, cmd);

        coord.simpleExecuteThread = new SimpleExecuteThread(coord, vos, br);

        coord.simpleExecuteThread.start();
    }

    /**
        * Set up the threads for a filter command, there are three threads.
        * <ul>
        *   <li>read/write from document</li>
        *   <li>write to process</li>
        *   <li>read from process</li>
        * </ul>
        * There are two BlockingQueue.
        * The document thread puts data from document to fromDoc BlockingQueue and
        * takes data from toDoc BlockingQueue and puts it into the document. Each
        * of the other threads passes data between a BlockingQueue and
        * the process.
        * <p>
        * This could be done with a single thread if the process streams had a
        * timeout interface.
        *<p/><p>
        * NEEDSWORK: work with larger chunks at a time.
        * In particular, insert/delete large chunks to/from document;
        * this might descrease the memory requirements for undo/redo.
        */
    private void outputFromProcessToFile(
            ColonEvent evt,
            Process p,
            String cmd )
    {
        // NEEDSWORK: set up a thread group???

        BufferedReader br;
        BufferedWriter bw;
        br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

        coord.writeToProcessThread = new ProcessWriterThread(coord, bw);
        coord.readFromProcessThread = new ProcessReaderThread(coord, br);
        coord.documentThread = new DocumentThread(coord, evt.getViTextView());

        //coord.documentThread.start();
        coord.documentThread.runUnderTimer();
        coord.writeToProcessThread.start();
        coord.readFromProcessThread.start();
    }

} // end inner class BangAction


/**
    * This is used for both filtered bang commands and to execute a command.
    * There should really be two coordinator, one for each, but I'm feeling
    * lazy right now. Any of the Thread variables, eg simpleExecuteThread, can
    * be used to test for how this is being used.
    */
private static class FilterThreadCoordinator
{
    public static final int WAIT = 50; // milliseconds to wait for q data.
    public static final int QLEN = 100; // size of to/from doc q's'
    int startLine;
    int lastLine;

    BlockingQueue<String> fromDoc;
    BlockingQueue<String> toDoc;

    DocumentThread documentThread;
    ProcessWriterThread writeToProcessThread;
    ProcessReaderThread readFromProcessThread;
    SimpleExecuteThread simpleExecuteThread;

    OPARG oa;
    Process process;
    final BangAction ba;

    String statusMessage = "";

    public FilterThreadCoordinator(BangAction ba, OPARG oa, Process p)
    {
        this.oa = oa;
        this.process = p;
        this.ba = ba;

        startLine = oa.start.getLine();
        lastLine = oa.end.getLine();

        fromDoc = new ArrayBlockingQueue<String>(QLEN);
        toDoc = new ArrayBlockingQueue<String>(QLEN);
    }

    void finish(final boolean fOK)
    {
        if(EventQueue.isDispatchThread()) {
            ba.finishBangCommand(fOK);
        } else {
            EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        ba.finishBangCommand(fOK);
                    }
            });
        }
    }

    public void dumpState()
    {
        System.err.println("startLine " + startLine + ", lastLine = " + lastLine );
    }

} // end inner class


private static class SimpleExecuteThread extends FilterThread
{
    ViOutputStream vos;
    BufferedReader reader;
    boolean didCleanup;

    SimpleExecuteThread(
            FilterThreadCoordinator coord,
            ViOutputStream vos,
            BufferedReader reader)
    {
        super(SIMPLE_EXECUTE, coord);
        this.vos = vos;
        this.reader = reader;
    }

    @Override
    public void run()
    {
        super.run();
        coord.finish(true);
    }

    @Override
    public void dumpState()
    {
        super.dumpState();
    }

    @Override
    void cleanup()
    {
        if(didCleanup) {
            return;
        }
        didCleanup = true;
        if (dbg.getBoolean()) {
            dumpState();
        }
        if(vos != null) {
            vos.close();
            vos = null;
        }
        if(reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            reader = null;
        }
    }

    @Override
    void doTask()
    {
        String line;
        try {
            while(!isProblem()) {
                line = reader.readLine();
                if(line == null) {
                    break;
                }
                if (dbgData.getBoolean()) {
                    System.err.println("!: Writing '" + line + "' to ViOutputStream");
                }
                vos.println(line);
            }
            reader.close();
            reader = null;
            vos.close();
            vos = null;
        } catch (IOException ex) {
            exception = ex;
        }
    }

} // end inner class SimpleExecuteThread


private static class ProcessWriterThread extends FilterThread
{
    BufferedWriter writer;
    int currWriterLine;
    boolean wroteFirstLineToProcess;
    boolean reachedEndOfLines;
    boolean didCleanup;

    ProcessWriterThread(FilterThreadCoordinator coord,
                        BufferedWriter writer)
    {
        super(WRITE_PROCESS, coord);
        this.writer = writer;
    }

    @Override
    public void dumpState()
    {
        System.err.println("currWriterLine " + currWriterLine
                + ", wroteFirstLineToProcess " + wroteFirstLineToProcess
                + ", reachedEndOfLines " + reachedEndOfLines );
        super.dumpState();
    }

    @Override
    void cleanup()
    {
        if(didCleanup) {
            return;
        }
        didCleanup = true;
        if (dbg.getBoolean()) {
            dumpState();
        }
        if(writer != null) {
            try {
                writer.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            writer = null;
        }
    }

    @Override
    void doTask()
    {
        writeToProcess();
    }

    public void writeToProcess()
    {
        currWriterLine = coord.startLine;
        String data = null;
        try {
            while(!isProblem()) {
                data = coord.fromDoc.take();
                if(DONE.equals(data)) {
                    break;
                }
                writer.write(data);
                if (dbgData.getBoolean()) {
                    // NEEDSWORK: why trim(), use Text.formDebugDString
                    System.err.println("!: Writer #" + currWriterLine
                            + ": '" + data.trim() + "'");
                }
                currWriterLine++;
                wroteFirstLineToProcess = true;
            }
            writer.close();
            writer = null;
        } catch (InterruptedException ex) {
            exception = ex;
        } catch (IOException ex) {
            exception = ex;
        }
        if (dbg.getBoolean()) {
            System.err.println("!: Wrote all lines needed to process");
        }
        if(!isProblem()) {
            reachedEndOfLines = true;
        }
    }

} // end inner class ProcessWriterThread


private static class ProcessReaderThread extends FilterThread
{
    BufferedReader reader;
    int currReaderLine;
    boolean wroteFirstLineToFile;
    boolean reachedEndOfProcessOutput;
    boolean didCleanup;

    ProcessReaderThread(
            FilterThreadCoordinator coord,
            BufferedReader reader )
    {
        super(READ_PROCESS, coord);
        this.reader = reader;
    }

    @Override
    public void dumpState()
    {
        System.err.println("currReaderLine " + currReaderLine
                + ", wroteFirstLineToFile " + wroteFirstLineToFile
                + ", reachedEndOfProcessOutput "
                + reachedEndOfProcessOutput );
        super.dumpState();
    }

    @Override
    void cleanup()
    {
        if(didCleanup) {
            return;
        }
        didCleanup = true;
        if (dbg.getBoolean()) {
            dumpState();
        }
        if(reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            reader = null;
        }
    }

    @Override
    void doTask()
    {
        readFromProcess();
    }

    private void readFromProcess()
    {
        currReaderLine = coord.startLine;
        String data = null;
        try {
            while(!isProblem()) {
                data = reader.readLine();
                if (data == null) {
                    if (wroteFirstLineToFile && !isInterrupted()) {
                        reachedEndOfProcessOutput = true;
                    }
                    if (dbg.getBoolean()) {
                        System.err.println("!: end of process read data");
                    }
                    break;
                }
                if (dbgData.getBoolean()) {
                    // NEEDSWORK: why trim(), use Text.formDebugString
                    System.err.println("!: Reader #" + currReaderLine
                            + ": '" + data.trim() + "'");
                }
                coord.toDoc.put(data);
                currReaderLine++;
            }
            coord.toDoc.put(DONE);
            reader.close();
            reader = null;
        } catch (InterruptedException ex) {
            exception = ex;
        } catch (IOException ex) {
            exception = ex;
        }
    }

} // end inner class ProcessReaderThread


/**
 *  This thread both reads and writes to the document.
 *  EXCEPT, IT IS NOT REALLY A THREAD anymore. It extends FilterThread
 *  because it is interfaced to like one. But now it runs under a timer as
 *  part of the event dispatch thread, this avoid locking issues
 *  between read/write the document and display.
 * <p>
 * Read one line at a time from the document and write it to a Queue for the
 * write to process q. Read a line at a time from the read from process q and
 * build up a large string with the process output. When the process completes
 * and all the data is gathered do a single op to the document.
 * </p><p>
 * Note that OriginalDocumentThread below does a line at a time into or out
 * of the document. But to allow all document mods to be done in a single
 * Runnable that strategy has problems with the current threading model,
 * where all mods are done in the event dispatch thread.
 * </p>
 */
private static class DocumentThread extends FilterThread
{
    int docReadLine;
    boolean docReadDone;

    int docWriteLine;
    boolean docWriteDone;
    StringBuilder sb = new StringBuilder();

    Window win;

    int debugCounter;

    // timer is used as a flag, when null work is done
    Timer timer;
    boolean isThread;
    boolean interruptFlag;

    protected boolean didCleanup;

    DocumentThread(FilterThreadCoordinator coord, ViTextView tv)
    {
        super(RW_DOC, coord);
        win = (Window)tv;
    }

    @Override
    public void run()
    {
        assert(false);
    }

    @Override
    public void interrupt()
    {
        interruptFlag = true;
    }

    @Override
    public boolean isInterrupted()
    {
        return interruptFlag;
    }

    public void runUnderTimer()
    {
        assert(!isThread);
        timer = new Timer(coord.WAIT, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    assert(EventQueue.isDispatchThread());
                    if(timer != null) {
                        doServiceUnderTimer();
                    }
                }
        });
        timer.setRepeats(false);

        docReadLine = coord.startLine;
        docWriteLine = coord.startLine;

        doServiceUnderTimer();

        return;
    }

    private void finishUnderTimer()
    {
        if(!docReadDone) {
            cleanup(); // like "10000!!date"
        }

        doTaskCleanup();

        coord.finish(true);
    }

    private void doServiceUnderTimer()
    {
        boolean didSomething;

        try {
            do {
                didSomething = readDocument();
                didSomething |= writeDocument();
            } while(didSomething && !isProblem() && !docWriteDone);
        } catch(Throwable t) {
            exception = t;
        }

        if(!isProblem() && !docWriteDone) {
            timer.start();
        } else {
            finishUnderTimer();
            cleanup(); // write to the file
        }
    }

    @Override
    void doTask()
    {
    }

    public boolean readDocument()
    {
        boolean didSomething = false;
        String data = null;
        if(docReadLine <= coord.lastLine) {
            if (dbg.getBoolean()) {
                System.err.println("!: rwDoc: try read doc");
            }
            while(!isProblem() && docReadLine <= coord.lastLine) {
                int docLine = docReadLine;
                data =    win.w_buffer.getLineSegment(docLine).toString();
                if(!coord.fromDoc.offer(data)) {
                    break;
                }
                if (dbgData.getBoolean()) {
                    System.err.println("!: fromDoc #" + docReadLine + "," + docLine
                            + ": '" + data.trim() + "'");
                }
                docReadLine++;
                didSomething = true;
            }
        } else {
            if(!docReadDone && coord.fromDoc.offer(DONE)) {
                docReadDone = true;
                if (dbg.getBoolean()) {
                    System.err.println("!: rwDoc: docReadDONE");
                }
            }
        }
        return didSomething;
    }

    public boolean writeDocument()
    {
        boolean didSomething = false;
        String data = null;
        if(!docWriteDone) {
            if (dbg.getBoolean())
                System.err.println("!: rwDoc: try write doc " + debugCounter++);
            while(!isProblem()) {
                data = coord.toDoc.poll();
                if(data == null) {
                    break;
                }
                if(DONE.equals(data)) {
                    docWriteDone = true;
                    if (dbg.getBoolean()) {
                        System.err.println("!: rwDoc: docWriteDONE");
                    }
                    break;
                }
                sb.append(data);
                sb.append('\n');
                if (dbgData.getBoolean()) {
                    System.err.println("!: toDoc #" + docWriteLine + ": '"
                            + data.trim() + "'");
                }
                didSomething = true;
            }
        }
        return didSomething;
    }

    @Override
    public void dumpState()
    {
        System.err.println("docReadDone " + docReadDone
                + ", docReadLine " + docReadLine
                + ", docWriteDone " + docWriteDone
                + ", docWriteLine " + docWriteLine);
        super.dumpState();
    }

    @Override
    void cleanup()
    {
        if(didCleanup) {
            return;
        }
        didCleanup = true;
        if (dbg.getBoolean()) {
            dumpState();
        }

        if(docReadDone && docReadLine <= coord.lastLine) {
            throw new IllegalStateException();
        }
        if (dbg.getBoolean()) {
            System.err.println("!: checking doc CLEANUP");
        }
        // don't run the document cleanup if there are issues
        if(isProblem()) {
            return;
        }

        Misc.runUndoable(new Runnable() {
            public void run() {
                Buffer buf = win.w_buffer;
                int startOffset = buf.getLineStartOffset(coord.startLine);
                int endOffset = buf.getLineEndOffset(coord.lastLine);
                if(endOffset > buf.getLength()) {
                    // replacing last line, but the '\n' is not part of file
                    endOffset = buf.getLength();
                    // if replacement text end in '\n', then strip it.
                    if(sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n')
                        sb.setLength(sb.length()-1);
                }
                buf.replaceString(startOffset, endOffset, sb.toString());
            }
        });

    }

} // end

/////////////////////////////
//
// non-string builder version
//
//private static class OriginalDocumentThread extends FilterThread
//{
//    int docReadLine;
//    boolean docReadDone;
//
//    int docWriteLine;
//    boolean docWriteDone;
//
//    int linesDelta;
//    Window win;
//
//    int debugCounter;
//
//    // timer is used as a flag, when null work is done
//    Timer timer;
//    boolean isThread;
//    boolean interruptFlag;
//    boolean inUndo;
//
//    protected boolean didCleanup;
//
//    OriginalDocumentThread(FilterThreadCoordinator coord, ViTextView tv)
//    {
//        super(RW_DOC, coord);
//        win = (Window)tv;
//    }
//
//    @Override
//    public void run()
//    {
//        assert(false);
//    }
//
//    @Override
//    public void interrupt()
//    {
//        interruptFlag = true;
//    }
//
//    @Override
//    public boolean isInterrupted()
//    {
//        return interruptFlag;
//    }
//
//    public void runUnderTimer()
//    {
//        assert(!isThread);
//        timer = new Timer(coord.WAIT, new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    assert(EventQueue.isDispatchThread());
//                    if(timer != null) {
//                        doServiceUnderTimer();
//                    }
//                }
//        });
//        timer.setRepeats(false);
//
//        docReadLine = coord.startLine;
//        docWriteLine = coord.startLine;
//
//        inUndo = true;
//        Misc.beginUndo();
//
//        doServiceUnderTimer();
//
//        return;
//    }
//
//    private void finishUnderTimer()
//    {
//        if(!docReadDone) {
//            cleanup(); // like "10000!!date"
//        }
//
//        doTaskCleanup();
//
//        if(inUndo) {
//            Misc.endUndo();
//        }
//
//        coord.finish(true);
//    }
//
//    private void doServiceUnderTimer()
//    {
//        boolean didSomething;
//
//        try {
//            do {
//                didSomething = readDocument();
//                didSomething |= writeDocument();
//            } while(didSomething && !isProblem() && !docWriteDone);
//        } catch(Throwable t) {
//            exception = t;
//        }
//
//        if(!isProblem() && !docWriteDone) {
//            timer.start();
//        } else {
//            finishUnderTimer();
//        }
//    }
//
//    @Override
//    void doTask()
//    {
//    }
//
//    private void deleteLine(int line)
//    {
//        deleteLines(line, line);
//    }
//
//    private void deleteLines(int startLine, int endLine)
//    {
//        int startOffset = win.w_buffer.getLineStartOffset(startLine);
//        int endOffset = win.w_buffer.getLineEndOffset(endLine);
//        int docLength = win.getEditorComponent().getDocument().getLength();
//        if(endOffset > docLength) {
//            endOffset = docLength;
//        }
//        win.deleteChar(startOffset, endOffset);
//    }
//
//    public boolean readDocument()
//    {
//        boolean didSomething = false;
//        String data = null;
//        if(docReadLine <= coord.lastLine) {
//            if (dbg.value) {
//                System.err.println("!: rwDoc: try read doc");
//            }
//            while(!isProblem() && docReadLine <= coord.lastLine) {
//                int docLine = docReadLine + linesDelta;
//                data =    win.w_buffer.getLineSegment(docLine).toString();
//                if(!coord.fromDoc.offer(data)) {
//                    break;
//                }
//                deleteLine(docLine);
//                if (dbgData.value) {
//                    System.err.println("!: fromDoc #" + docReadLine + "," + docLine
//                            + ": '" + data.trim() + "'");
//                }
//                docReadLine++;
//                linesDelta--;
//                didSomething = true;
//            }
//        } else {
//            if(!docReadDone && coord.fromDoc.offer(DONE)) {
//                docReadDone = true;
//                if (dbg.value) {
//                    System.err.println("!: rwDoc: docReadDONE");
//                }
//            }
//        }
//        return didSomething;
//    }
//
//    public boolean writeDocument()
//    {
//        boolean didSomething = false;
//        String data = null;
//        if(!docWriteDone) {
//            if (dbg.value)
//                System.err.println("!: rwDoc: try write doc " + debugCounter++);
//            while(!isProblem()) {
//                data = coord.toDoc.poll();
//                if(data == null) {
//                    break;
//                }
//                if(DONE.equals(data)) {
//                    docWriteDone = true;
//                    if (dbg.value) {
//                        System.err.println("!: rwDoc: docWriteDONE");
//                    }
//                    break;
//                }
//                int offset = win.w_buffer.getLineStartOffset(docWriteLine);
//                win.insertText(offset, data);
//                offset += data.length();
//                win.insertText(offset, "\n");
//                if (dbgData.value) {
//                    System.err.println("!: toDoc #" + docWriteLine + ": '"
//                            + data.trim() + "'");
//                }
//                docWriteLine++;
//                linesDelta++;
//                didSomething = true;
//            }
//        }
//        return didSomething;
//    }
//
//    @Override
//    public void dumpState()
//    {
//        System.err.println("docReadDone " + docReadDone
//                + ", docReadLine " + docReadLine
//                + ", docWriteDone " + docWriteDone
//                + ", docWriteLine " + docWriteLine
//                + ", linesDelta " + linesDelta);
//        super.dumpState();
//    }
//
//    @Override
//    void cleanup()
//    {
//        if(didCleanup) {
//            return;
//        }
//        didCleanup = true;
//        if (dbg.value) {
//            dumpState();
//        }
//
//        if(docReadDone && docReadLine <= coord.lastLine) {
//            throw new IllegalStateException();
//        }
//        if (dbg.value) {
//            System.err.println("!: checking doc CLEANUP");
//        }
//        // don't run the document cleanup if there are issues
//        if(isProblem()) {
//            return;
//        }
//
//        // do one big delete
//        int line1, line2;
//        line1 = docReadLine + linesDelta;
//        line2 = coord.lastLine + linesDelta;
//        if (dbg.value) {
//            System.err.println("!: CLEANUP: fromDoc #"
//                    + docReadLine + ":" + coord.lastLine
//                    + ", " + line1 + ":" + line2);
//        }
//        deleteLines(line1, line2);
//    }
//
//} // end


/** Base class for Bang command threads.
    * <p>
    * The thread that signals completion
    * of the task should override run() and signal success if there was no
    * problem. If an problem occurs, failure will be signaled automatically,
    * see coord.finish()
    */
private static abstract class FilterThread extends Thread
{
    public static final String READ_PROCESS = "FilterReadProcess";
    public static final String WRITE_PROCESS = "FilterWriteProcess";
    public static final String RW_DOC = "FilterDocument";
    public static final String SIMPLE_EXECUTE = "SimpleCommandExecute";

    public static final String DONE
            = new String(new char[] {CharacterIterator.DONE});

    protected FilterThreadCoordinator coord;

    protected Throwable uncaughtException;
    protected Throwable exception;
    protected boolean error;
    protected boolean interrupted;

    protected BooleanOption dbg
            = (BooleanOption)Options.getOption(Options.dbgBang);
    protected BooleanOption dbgData
            = (BooleanOption)Options.getOption(Options.dbgBangData);

    public FilterThread(String filterType, FilterThreadCoordinator coord)
    {
        super(filterType);
        this.coord = coord;
        if(false) {
            // low priority a bit
            int pri = getPriority() -1;
            if(pri >= MIN_PRIORITY) {
                setPriority(pri);
            }
        }
    }

    protected void dumpState()
    {
        if(exception != null) {
            System.err.println("exception: " + exception.getMessage());
        }
        if(uncaughtException != null) {
            System.err.println("uncaughtException: " + uncaughtException.getMessage());
        }
        if(error) {
            System.err.println("error: " + error);
        }
        if(interrupted) {
            System.err.println("interrupted: " + interrupted);
        }
        coord.dumpState();
    }

    /** Its possible that cleanup is called multiple times */
    abstract void cleanup();

    abstract void doTask();

    boolean isProblem()
    {
        return isInterrupted() || error || exception != null;
    }

    @Override
    public void run()
    {
        setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    FilterThread thisThread = (FilterThread) t;
                    if(thisThread.uncaughtException != null) {
                        return;
                    }
                    thisThread.uncaughtException = e;
                    LOG.log(Level.SEVERE, null, e);
                    cleanup();
                }
            });

        doTask();
        doTaskCleanup();
    }

    public void doTaskCleanup()
    {
        if(exception != null) {
            if (dbg.getBoolean()) {
                LOG.log(Level.SEVERE,
                        "!: Exception in " + getName() , exception);
            }
            cleanup();
        }

        if(error) {
            if (dbg.getBoolean()) {
                System.err.println("!: error in " + getName());
            }
            cleanup();
        }

        if(isInterrupted()) {
            interrupted = true;
            if (dbg.getBoolean()) {
                System.err.println("!: cleanup in " + getName());
            }
            cleanup();
        }

        if(isProblem()) {
            coord.finish(false);
        }
    }

} // end inner class FilterThread


static ActionListener ACTION_quit = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;
            cev.getViTextView().win_quit();
        }};

static ActionListener ACTION_close = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;
            // NEEDSWORK: win_close: hidden, need_hide....
            cev.getViTextView().win_close(false);
        }};

static ActionListener ACTION_only = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;
            // NEEDSWORK: win_close_others: forceit....
            cev.getViTextView().win_close_others(false);
        }};

static ActionListener ACTION_wall = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ViManager.getFS().writeAll(false);
        }};

static ActionListener ACTION_file = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;
            cev.getViTextView().getBuffer()
                    .displayFileInfo(cev.getViTextView());
        }};

private static boolean do_write(ColonEvent cev)
{
    boolean ok;
    boolean isBang = cev.isBang();
    String fName = cev.getNArg() == 0 ? null : cev.getArg(1);
    Integer[] range = cev.getAddrCount() > 0
            ? new Integer[] { cev.getLine1(), cev.getLine2() }
            : new Integer[0];

    if(cev.getNArg() < 2) {
        ok = ViManager.getFS().write(
                cev.getViTextView(), isBang, fName, range);
    } else {
        Msg.emsg(":write only accepts none or one argument");
        ok = false;
    }
    return ok;
}

/**
    * Write command.
    */
static ColonAction ACTION_write = new ColonAction() {
        @Override
        public int getFlags()
        {
            return BANG;
        }

        public void actionPerformed(ActionEvent ev)
        {
            do_write((ColonEvent)ev);
        }
    };

static ColonAction ACTION_wq = new ColonAction() {
        @Override
        public int getFlags()
        {
            return BANG;
        }

        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            if(do_write(cev))
                cev.getViTextView().win_quit();
        }
    };

/**
    * Edit command. Only ':e#[number]' is supported.
    */
static ColonAction ACTION_edit = new ColonAction() {
        @Override
        public int getFlags()
        {
            return BANG;
        }

        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            /*
            if(cev.getNArg() == 0) {
                ViManager.getFS().write(cev.getViTextView(), cev.isBang());
            } else
            */
            if(cev.getNArg() == 1 && cev.getArg(1).charAt(0) == '#') {
                boolean error = false;
                String arg;
                int i = -1;
                arg = cev.getArg(1);
                if(arg.length() > 1) {
                    arg = arg.substring(1);
                    try {
                        i = Integer.parseInt(arg);
                    } catch(NumberFormatException ex) {
                        error = true;
                    }
                    if(error) {
                        Msg.emsg("Only 'e#[<number>]' allowed");
                    }
                }
                if( ! error) {
                    ViManager.getFS().edit(
                            cev.getViTextView(), cev.isBang(), i);
                }
            } else if ( cev.getNArg() < 2) {
                String fName = cev.getNArg() == 0 ? null : cev.getArg(1);
                ViManager.getFS().edit(
                        cev.getViTextView(), cev.isBang(), fName);
            } else {
                Msg.emsg(":edit only accepts none or one argument");
            }
        }
    };

static ColonAction ACTION_substitute = new ColonAction() {
        @Override
        public int getFlags()
        {
            return NOPARSE;
        }

        public void actionPerformed(final ActionEvent ev)
        {
            Misc.runUndoable(new Runnable() {
                public void run() {
                    Search.substitute((ColonEvent)ev);
                }
            });
        }
    };

static ColonAction ACTION_global = new ColonAction() {
        @Override
        public int getFlags()
        {
            return NOPARSE;
        }

        public void actionPerformed(final ActionEvent ev) {
            Misc.runUndoable(new Runnable() {
                public void run() {
                    Search.global((ColonEvent)ev);
                    Options.newSearch();
                }
            });
        }
    };

/** This is the default buffers,files,ls command. The platform specific code
    * may chose to register this, or implement their own, possibly using
    * popup gui components.
    */
public static ActionListener ACTION_BUFFERS = new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
            ViOutputStream osa = ViManager.createOutputStream(
                    null, ViOutputStream.OUTPUT,
                    "=== MRU (:n :N :e#-<digit>) ===                "
                        + "=== activation (:e#<digit> ===");
            int i = 0;
            Object cur = ViManager.relativeMruBuffer(0);
            Object prev = ViManager.getMruBuffer(1);

            List<String> l = new ArrayList<String>();
            ViFactory factory = ViManager.getViFactory();
            while(true) {
                Object o1 = ViManager.getMruBuffer(i);
                Object o2 = ViManager.getTextBuffer(i+1);
                if(o1 == null && o2 == null) {
                    break;
                }
                int w2 = ViManager.getViFactory().getWNum(o2);
                l.add(String.format(
                        " %2d %c %-40s %3d %c %s",
                        i,
                        cur == o1 ? '%' : prev == o1 ? '#' : ' ',
                        o1 != null ? factory.getDisplayFilename(o1) : "",
                        w2,
                        cur == o2 ? '%' : prev == o2 ? '#' : ' ',
                        o2 != null ? factory.getDisplayFilename(o2) : ""));
                i++;
            }
            // print in reverse order, MRU visible if scrolling
            for(int i01 = l.size() -1; i01 >= 0; i01--) {
                osa.println(l.get(i01));
            }
            osa.close();
        }
    };

/**
    * :print command. If not busy global then output to "print" stream.
    * For now, its just a no-op so the word "print" can be found.
    */
static ColonAction ACTION_print = new ColonAction() {
        public void actionPerformed(ActionEvent ev) {

        }
    };

/**
    * :tag command.
    */
static ColonAction ACTION_tag = new ColonAction() {
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent evt = (ColonEvent)ev;
            if(evt.getNArg() > 1) {
                Msg.emsg(Messages.e_trailing);
                return;
            }
            if(evt.getNArg() == 1) {
                ViManager.getViFactory().tagDialog(evt);
            } else {
                int count = 1;
                if(evt.getAddrCount() == 1)
                    count = evt.getLine2();
                ViManager.getViFactory().tagStack(TAGOP.NEWER, count);
            }
        }
    };

/**
    * :pop command.
    */
static ColonAction ACTION_pop = new ColonAction() {
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent evt = (ColonEvent)ev;
            if(evt.getNArg() > 0) {
                Msg.emsg(Messages.e_trailing);
                return;
            }
            int count = 1;
            if(evt.getAddrCount() == 1)
                count = evt.getLine2();
            ViManager.getViFactory().tagStack(TAGOP.OLDER, count);
        }
    };

static ColonAction ACTION_tselect = new ColonAction() {
        public void actionPerformed(ActionEvent ev) {
            ColonEvent ce = (ColonEvent)ev;
            if(ce.getNArg() > 1) {
                Msg.emsg(Messages.e_trailing);
                return;
            }
            int count = 1;
            if(ce.getAddrCount() == 1)
                count = ce.getLine2();
            ViManager.getViFactory().tagDialog(ce);
        }
    };

static ActionListener ACTION_jumps = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            MarkOps.do_jumps();
        }
    };

static ActionListener ACTION_tags = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ViManager.getViFactory().displayTags();
        }
    };

static ActionListener ACTION_version = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ViManager.motd.output();
        }
    };

static ActionListener ACTION_debugMotd = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ViManager.debugMotd();
        }
    };

static ActionListener ACTION_nohlsearch = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            Options.nohCommand();
        }
    };

static ColonAction ACTION_bang = new BangAction();

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
                && Misc.valid_yank_reg(r.charAt(0), false)
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
        if(cev.getAction() != ACTION_yank) {
            MarkOps.setpcmark();
            G.curwin.w_cursor.set(oa.start);
            Edit.beginline(BL_SOL|BL_FIX);
        }
    }
    return oa;
}

/**
 * :delete command.
 */
static ColonAction ACTION_delete = new ColonAction() {
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            OPARG oa = setupExop(cev, true);
            if(!oa.error) {
                oa.op_type = OP_DELETE;
                Misc.op_delete(oa);
            }
        }
    };

/**
 * :yank command.
 */
static ColonAction ACTION_yank = new ColonAction() {
        public void actionPerformed(ActionEvent ev) {
            OPARG oa = setupExop((ColonEvent)ev, true);
            if(!oa.error) {
                oa.op_type = OP_YANK;
                Misc.op_yank(oa, false, true);
            }
        }
    };

private static ColonAction ACTION_lshift = new ShiftAction(OP_LSHIFT);
private static ColonAction ACTION_rshift = new ShiftAction(OP_RSHIFT);

private static class ShiftAction extends ColonAction
{
    final int op;

    public ShiftAction(int op)
    {
        this.op = op;
    }

    public void actionPerformed(ActionEvent ev)
    {
        ColonEvent cev = (ColonEvent)ev;
        final OPARG oa = setupExop(cev, true);
        final int amount = cev.getInputCommandName().length();
        if(!oa.error) {
            oa.op_type = op;
            Misc.runUndoable(new Runnable() {
                    public void run()
                    {
                        Misc.op_shift(oa, false, amount);
                    }
                });
        }
    }
}

private static class moveCopy extends ColonAction
{
    private boolean doMove;
    moveCopy(boolean doMove)
    {
        this.doMove = doMove;
    }

    public void actionPerformed(ActionEvent e)
    {
        ColonEvent cev = (ColonEvent) e;
        ViTextView tv = cev.getViTextView();
        final Buffer buf = tv.getBuffer();
        if(cev.getLine1() > buf.getLineCount()
                || cev.getLine2() > buf.getLineCount()) {
            Msg.emsg(Messages.e_invrange);
            return; // BAIL
        }
        final int offset1 = buf.getLineStartOffset(cev.getLine1());
        final int offset2 = buf.getLineEndOffset(cev.getLine2());
        // get the destination line number
        MutableInt dst = new MutableInt();
        if(cev.getNArg() < 1
                || get_address(cev.getArg(1), 0, false, dst) < 0
                || dst.getValue() > buf.getLineCount()) {
            Msg.emsg(Messages.e_invaddr);
            return; // BAIL
        }
        if(doMove && dst.getValue() == cev.getLine2())
            return; // 2,4 mo 4 does nothing

        final int dstOffset = buf.getLineEndOffset(dst.getValue());
        if(doMove && dstOffset >= offset1 && dstOffset < offset2) {
            Msg.emsg("Move lines into themselves");
            return; // BAIL
        }

        // If at the end of the file, then can't delete the final
        // '\n' on a move, so back up the range by a character
        final int atEndAdjust = cev.getLine2() == buf.getLineCount() ? 1 : 0;

        // track postions for later delete
        Misc.runUndoable(new Runnable() {
            public void run() {
                try {
                    Position pos1 = doMove
                            ? ((Document)buf.getDocument())
                                        .createPosition(offset1)
                            : null;
                    Position pos2 = doMove
                            ? ((Document)buf.getDocument())
                                        .createPosition(offset2)
                            : null;
                    String s = buf.getText(offset1, offset2 - offset1);
                    buf.insertText(dstOffset, s);
                    if(doMove) {
                        buf.deleteChar(pos1.getOffset() - atEndAdjust,
                                        pos2.getOffset() - atEndAdjust);
                    }
                } catch (BadLocationException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        });
    }

}

static ColonAction ACTION_move = new moveCopy(true);
static ColonAction ACTION_copy = new moveCopy(false);

static ActionListener ACTION_testGlassKeys = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            ViManager.getViFactory().startGlassKeyCatch(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e)
                    {
                        e.consume();
                        if(e.getKeyCode() == KeyEvent.VK_Y) {
                            ViManager.getViFactory().stopGlassKeyCatch();
                            Msg.clearMsg();
                        } else {
                            Util.vim_beep();
                        }
                    }
            });
            Msg.smsg("Enter 'y' to proceed");
            }
        };

static ActionListener ACTION_testModalKeys = new ActionListener() {
        public void actionPerformed(ActionEvent ev)
        {
            Msg.smsg("Enter 'y' to proceed");
            ViManager.getViFactory().startModalKeyCatch(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e)
                    {
                        e.consume();
                        if(e.getKeyCode() == KeyEvent.VK_Y) {
                            ViManager.getViFactory().stopModalKeyCatch();
                        } else {
                            Util.vim_beep();
                        }
                    }
            });
            Msg.clearMsg();
        }
    };

static void registerBuiltinCommands()
{
    register("ve", "version", ACTION_version);
    register("debugMotd", "debugMotd", ACTION_debugMotd);

    register("clo", "close", ACTION_close);
    register("on", "only", ACTION_only);

    register("q", "quit", ACTION_quit);
    register("w", "write", ACTION_write);
    register("wq", "wq", ACTION_wq);
    register("wa", "wall", ACTION_wall);

    register("files","files", ACTION_BUFFERS);
    register("buffers","buffers", ACTION_BUFFERS);
    register("ls","ls", ACTION_BUFFERS);

    register("f", "file", ACTION_file);
    register("e", "edit", ACTION_edit);
    register("s", "substitute", ACTION_substitute);
    register("g", "global", ACTION_global);
    register("d", "delete", ACTION_delete);
    register("p", "print", ACTION_print);

    register("se", "set", new Options.SetCommand());

    register("ju", "jumps", ACTION_jumps);

    register("ta", "tag", ACTION_tag);
    register("tags", "tags", ACTION_tags);
    register("ts", "tselect", ACTION_tselect);
    register("po", "pop", ACTION_pop);

    register("noh", "nohlsearch", ACTION_nohlsearch);

    register("testGlassKeys", "testGlassKeys", ACTION_testGlassKeys);
    register("testModalKeys", "testModalKeys", ACTION_testModalKeys);

    ColonCommands.register("jviDump", "jviDump", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ViManager.dump(System.err);
            }
            });

    register("y", "yank", ACTION_yank);
    // not pretty, limit the number of shifts...
    register(">", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>", ACTION_rshift);
    register("<", "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<", ACTION_lshift);

    register("m", "move", ACTION_move);
    register("co", "copy", ACTION_copy);
    register("t", "t", ACTION_copy);

    // register("n", "next", ACTION_next);
    // register("N", "Next", ACTION_Next);

    register("!", "!", ACTION_bang);

    register("marks", "marks", MarkOps.ACTION_do_marks);
    register("delm", "delmarks", MarkOps.ACTION_ex_delmarks);

} // end registerBuiltinCommands


private static void addDebugColonCommands()
{
    //
    // Some debug commands
    //
    ColonCommands.register("optionsDump", "optionsDump", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ViManager.getViFactory().getPreferences().exportSubtree(os);
            ViOutputStream vios = ViManager.createOutputStream(
                    null, ViOutputStream.OUTPUT, "Preferences");
            vios.println(os.toString());
            vios.close();

        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        }
    });
    ColonCommands.register("optionsDelete", "optionsDelete", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        try {
            Preferences prefs = ViManager.getViFactory().getPreferences();
            String keys[] = prefs.keys();
            for (String key : keys) {
            prefs.remove(key);
            }
            prefs = prefs.node(ViManager.PREFS_KEYS);
            keys = prefs.keys();
            for (String key : keys) {
            prefs.remove(key);
            }
        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        }
    });
    ColonCommands.register("optionDelete", "optionDelete",
            new ColonCommands.ColonAction() {
                public void actionPerformed(ActionEvent ev) {
                    ColonEvent cev = (ColonEvent) ev;

                    if(cev.getNArg() == 1) {
                    String key = cev.getArg(1);
                    Preferences prefs = ViManager.getViFactory().getPreferences();
                    prefs.remove(key);
                    } else
                    Msg.emsg("optionDelete takes exactly one argument");
                }
            });

} // end

static
{
    registerBuiltinCommands();
    addDebugColonCommands();
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

} // end com.raelity.jvi.ColonCommand

// vi:set sw=4 ts=8 et:
