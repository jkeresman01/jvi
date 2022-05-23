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

import java.awt.Component;
import java.nio.file.Path;

import com.raelity.jvi.core.*;

/**
 * jVi's interactions with files are funnelled through this interface.
 */
public interface ViFS
{

public String getDisplayFileName( ViAppView av );

// Have this here, rather than having caller handle it,
// is so that 'shortmess' can be implemented more easily in the future.
default String getDisplayFileName(ViAppView av, boolean shortname)
{
    String fn = getDisplayFileName(av);
    return shortname ? FilePath.getName(fn) : fn;
}

// the buf version is like a backup
public String getDisplayFileName( ViBuffer buf );

public String getDisplayFileName( Component c );

public String getDisplayFileNameAndSize(ViBuffer buf);

public String getDisplayFileViewInfo(ViTextView tv);

// May return null
public Path getPath(ViAppView av);


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
 * @param range either null/empty or two elements, first/last lines
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
 * Focus on this editor.
 * @param av the editor to start working on
 * @return false if some problem encountered
 */
public boolean edit(ViAppView av, boolean force);

/**
 * Edit the specified file. The platform may need to create an edtior view
 * and read the file into the editor.
 *
 * The fpos may be null in which case typically the cursor is positioned
 * at the first position in the file. The fpos may be out of bounds, in
 * which case something in the neighborhood can be selected.
 *
 * @param file
 * @param force If a '!' was used with the command, then force is true.
 * @param fpos initial cursor position, may be null.
 */
public void edit(Path file, boolean force, ViFPOS fpos);


} // end com.raelity.jvi.ViFS
