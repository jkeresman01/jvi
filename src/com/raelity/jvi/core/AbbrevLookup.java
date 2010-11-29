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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * This is a lookup table where the key can be abbreviated. It is convenient
 * for words that are entered by users.
 */
public class AbbrevLookup
{
    private List<ColonCommandItem> list = new ArrayList<ColonCommandItem>();

    /**
     * This method returns a read-only copy of the list.
     */
    public List<ColonCommandItem> getList()
    {
        return Collections.unmodifiableList(list);
    }

    //
    // NEEDSWORK: keep these lists around
    //

    public List<String> getNameList()
    {
        List<String> l = new ArrayList<String>(list.size());
        for(ColonCommandItem ce : list) {
            if(Character.isLetter(ce.getName().charAt(0)))
                l.add(ce.getName());
        }
        return Collections.unmodifiableList(l);
    }

    public List<String> getAbrevList()
    {
        List<String> l = new ArrayList<String>(list.size());
        for(ColonCommandItem ce : list) {
            if(Character.isLetter(ce.getName().charAt(0)))
                l.add(ce.getAbbrev());
        }
        return Collections.unmodifiableList(l);
    }


    /**
     * Register a command; the abbrev must be unique.  The abbrev is the "key"
     * for the command, for example since "s" is the abbreviation for "substitue"
     * and "se" is the abbreviation for "set" then "substitute" sorts earlier
     * than "set" because "s" sorts earlier than "se". Consider this when
     * adding commands, since unique prefix has nothing to do with how commands
     * are recognized.
     *
     * @exception IllegalArgumentException this is thrown if the abbreviation
     *     and/or the name already exist in the list or there's a null argument;
     *     except flags may be null.
     */
    public void add(String abbrev, String name, Object value,
                    EnumSet<ColonCommandItem.Flag> flags)
    {
        if ( abbrev == null || name == null || value == null ) {
            throw new IllegalArgumentException("All arguments must be non-null");
        }
        int idx = Collections.binarySearch(list, getSearchKey(abbrev));
        if ( idx >= 0 ) {
            throw new IllegalArgumentException("The abbreviation '" + abbrev
                    + "' is already registered");
        }
        // spin through the list insuring command name not in use
        Iterator iter = list.iterator();
        while( iter.hasNext() ) {
            if  ( ((ColonCommandItem)iter.next()).getName().equals(name) ) {
                throw new IllegalArgumentException("The name '" + abbrev
                        + "' is already registered");
            }
        }
        if ( !name.startsWith(abbrev) ) {
            throw new IllegalArgumentException("'" + name + "' does not start with '"
                    + abbrev + "'");
        }
        // turn idx into something that can be used for insertion into list
        idx = -idx - 1;
        list.add(idx, new ColonCommandItem(abbrev, name, value, flags));
    }


    /**
     * Deregister (remove) a command; the abbrev is used as the "key" for
     * the command.
     *
     * @return true if the command was removed, false if not (i.e.,
     *         it did not previously exist).
     * @exception IllegalArgumentException this is thrown if passed a
     *         null argument.
     */
    protected boolean remove( String abbrev )
    {
        if ( abbrev == null ) {
            throw new IllegalArgumentException("Null argument.");
        }
        int idx = Collections.binarySearch(list, getSearchKey(abbrev));
        return idx >= 0
                ? ( list.remove(idx) != null )
                : false ;
    }


    /**
     * Search the list for the command. Sequentially search a list starting
     * with the list that start with the first letter of the input command.
     */
    public ColonCommandItem lookupCommand( String command )
    {
        ColonCommandItem ce = null;
        if ( command.length() == 0 ) {
            return ce;
        }
        String firstChar = command.substring(0, 1);
        String nextChar = String.valueOf((char)(command.charAt(0) + 1));
        int cidx = Collections.binarySearch(list, getSearchKey(firstChar));
        if ( cidx < 0 ) {
            cidx = -cidx - 1;
        }
        Iterator iter = list.listIterator(cidx);
        while ( iter.hasNext() ) {
            ColonCommandItem ce01 = (ColonCommandItem)iter.next();
            if ( ce01.getAbbrev().compareTo(nextChar) >= 0 ) {
                break; // not in list
            }
            if ( ce01.match(command) ) {
                ce = ce01;
                break;
            }
        }
        return ce;
    }

    /**
     *  Return a command element that can be used for searching the
     *  command list. The returned element is reusable.
     */
    private ColonCommandItem getSearchKey(String abbrev)
    {
        return new ColonCommandItem(abbrev);
    }

} 