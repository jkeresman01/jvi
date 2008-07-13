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

package com.raelity.jvi;

/**
 * jVi's interactions with files are funnelled through this interface.
 */
public interface ViFS
{

    public String getDisplayFileName( ViBuffer buf );


    /**
     *  Has the associated document been modified?
     */
    public boolean isModified( ViBuffer buf );


    /**
     *  Is the associtated document read only?
     */
    public boolean isReadOnly( ViBuffer buf );


    /**
     * Write the specified text to the specified fileName.
     * If file is null, use a dialog to get the file name.
     *
     * @param tv Editor containing data to write
     * @param force If a '!' was used with the command, then force is true.
     * @param fName file name to write, null if no name given
     * @param range either empty or two elements, first/last lines
     * @return true if successful in writing the file.
     */
    public boolean write( ViTextView tv,
                         boolean force,
                         String fName,
                         Integer[] range);


    /**
     *  Write all open stuff.
     *
     * @return true if successful in writing the file.
     */
    public boolean writeAll( boolean force );


    /**
     *  Edit the nth file. If n &lt; 0 then n is MRU; n == -1 is most recent.
     */
    public void edit( ViTextView tv, boolean force, int n );


    /**
     *  Edit the named file.
     * 
     * @param tv Editor containing data to write
     * @param force If a '!' was used with the command, then force is true.
     * @param fName file name to edit, null if no name given
     */
    public void edit( ViTextView tv, boolean force, String fName );


} // end com.raelity.jvi.ViFS
