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

import com.raelity.jvi.core.Filemark;
import java.io.File;

/**
 * jVi's interactions with files are funnelled through this interface.
 */
public interface ViFS
{

    public String getDisplayFileName( ViBuffer buf );

    public String getDisplayFileName( ViAppView av );

    public String getDisplayFileNameAndSize(ViBuffer buf);

    public String getDisplayFileViewInfo(ViTextView tv);


    /**
     *  Has the associated document been modified?
     */
    public boolean isModified( ViBuffer buf );


    /**
     *  Is the associtated document read only?
     */
    public boolean isReadOnly( ViBuffer buf );


    /**
     * Write the specified text to writeTarget.
     * If writeTarget is null overwrite the original source of the TextView.
     * writeTarget may be instanceof String, this is a file name.
     * writeTarget may be a File.
     * writeTarget may be extended by platforms to be an
     * OutputStream, Buffer or whatever.
     *
     * @param tv Editor containing data to write
     * @param force If a '!' was used with the command, then force is true.
     * @param writeTarget write the data to here
     * @param range either empty or two elements, first/last lines
     * @return true if successful in writing the file.
     */
    public boolean write( ViTextView tv,
                         boolean force,
                         Object writeTarget,
                         Integer[] range);


    /**
     *  Write all open stuff.
     *
     * @return true if successful in writing the file.
     */
    public boolean writeAll( boolean force );


    /**
     *  Edit the nth file. If n &lt; 0 then n is MRU,
     * for example n == -1 is previous file and n == -2 is the
     * file before that. Display MRU list with ":ls" command.
     */
    public void edit( ViTextView tv, boolean force, int n );

    /**
     * Focus on this editor.
     * @param av the editor to start working on
     */
    public void edit(ViAppView av);


    /**
     *  Edit the specified thing. fileThing can be a String which is a path,
     * a {@link File} or a {@link Filemark}.
     * 
     * @param tv Editor containing data to write
     * @param force If a '!' was used with the command, then force is true.
     * @param fName the thing to open in the editor
     */
    public abstract void edit( ViTextView tv, boolean force, Object fileThing);


} // end com.raelity.jvi.ViFS
