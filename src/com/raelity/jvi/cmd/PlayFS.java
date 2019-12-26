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

package com.raelity.jvi.cmd;

import java.io.File;

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.ViBuffer;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.swing.simple.SimpleFS;

/**
 *  A default implementation of the {@link com.raelity.jvi.ViFS}
 *  (vi file system support).
 */
public class PlayFS extends SimpleFS
{

    /**
     *  Default constructor.
     */
    public PlayFS()
    {
    }

    @Override
    public String getDisplayFileName(ViAppView av) {
        return "File:" + av.getWNum();
    }


    @Override
    public boolean isModified( ViBuffer buf )
    {
        return true;
    }


    @Override
    public boolean write(ViTextView tv,
                         boolean force,
                         Object writeTarget,
                         Integer[] range)
    {
        // Compatibility method with old interface
        //
        // if(range.length == 0) {
        //     if(writeTarget == null) {
        //         return write(tv, force);
        //     } else if(writeTarget instanceof String) {
        //         return write(tv, fName, force);
        //     } else
        //         assert false : "unsupported writeTarget";
        // }
        // assert false : "range not supported";
        //

        String r = null;
        if(range.length > 0) {
            r = "" + range[0] + "," + range[1];
        }

        Msg.emsg(String.format(
                "write tv: %s%s, range: %s not implemented",
                (force?"! ":""), writeTarget, r));
        return true;
    }


    @Override
    public boolean writeAll( boolean force )
    {
        Msg.emsg("writAll() not implemented");
        return false;
    }

    @Override
    public boolean edit(ViAppView av, boolean force)
    {
        av.getEditor().requestFocusInWindow();
        return true;
    }


    @Override
    public void edit(File f, boolean force, ViFPOS fpos )
    {
        Msg.emsg("edit(File{" + f + "}, force, fpos) not implemented");
    }


} // end com.raelity.jvi.PlayFS
