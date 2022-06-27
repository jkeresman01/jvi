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

import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.raelity.jvi.core.ColonCommands.ColonAction;
import com.raelity.jvi.core.lib.*;

/**
 *
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
}
