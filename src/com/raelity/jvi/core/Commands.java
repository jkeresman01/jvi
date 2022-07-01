/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2022 Ernie Rael.  All Rights Reserved.
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
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.lib.AbbrevLookup;
import com.raelity.jvi.core.lib.CcFlag;
import com.raelity.jvi.core.lib.ColonCommandItem;

/**
 * Data structures for defining and registering colon commands.
 * @author err
 */
public class Commands
{
private static final AbbrevLookup m_commands = new AbbrevLookup();

// Don't want this in ColonCommands since register is used so early.
// First step, delegate to here.

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

static ColonCommandItem lookupCommand( String command )
{
    return m_commands.lookupCommand(command);
}

static boolean hasExactCommand(String command)
{
    return m_commands.hasExactCommand(command);
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

private Commands()
{
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
    } // END CLASS

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
    } // END CLASS


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
    /** the command arguments of the raw command line*/
    List<String> argsRaw;
    /** command line as expanded */
    String commandLine;
    /** index of the command args in the original string */
    int iArgString;
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
        return Collections.unmodifiableList(args);
    }

    /**
     * Fetch list of command arguments parsed from commandLineRaw.
     */
    public List<String> getArgsRaw()
    {
        if(argsRaw == null) {
            String cmdlineargs = commandLineRaw.substring(iArgString).trim();
            argsRaw = cmdlineargs.isBlank() ? Collections.emptyList()
                          : Arrays.asList(cmdlineargs.split("\\s+"));
        }
        return Collections.unmodifiableList(argsRaw);
    }

    /**
     * @return the number of arguments, not including command name.
     */
    public int getNArgRaw()
    {
        return getArgsRaw().size();
    }

    /**
     * Fetch an argument.
     * @return the nth argument, n == 0 is the expanded command name.
     */
    public String getArgRaw( int n )
    {
        return n == 0
                   ? command
                   : getArgsRaw().get(n-1) ;
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

    } // END CLASS ColonEvent

}
