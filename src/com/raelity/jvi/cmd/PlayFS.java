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

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.ViBuffer;
import com.raelity.jvi.ViFS;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.lib.abstractFS;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 *  A default implementation of the {@link com.raelity.jvi.ViFS}
 *  (vi file system support).
 */
public class PlayFS extends abstractFS
{

    /**
     *  Default constructor.
     */
    public PlayFS()
    {
    }
    public String getDisplayFileName(ViAppView av)
    {
        JTextComponent ep = (JTextComponent)av.getEditor();
        ViTextView tv = ViManager.getViFactory().getTextView(ep);
        if(tv != null) {
            return getDisplayFileName(tv.getBuffer());
        }
        assert false;
        return "file-name-unkown-from-appview";
    }


    public String getDisplayFileName(ViBuffer buf)
    {
        String fname = null;
        Document doc = (Document)buf.getDocument();
        if(doc != null) {
            Object o = doc.getProperty(Document.TitleProperty);
            if(o != null)
                fname = o.toString();
        }
        return fname != null ? fname : "file-name-unknown";
    }


    public boolean isReadOnly( ViBuffer buf )
    {
        return false;
    }


    public boolean isModified( ViBuffer buf )
    {
        return true;
    }


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

        System.err.println(String.format(
                "write tv: %s%s, range: %s", (force?"! ":""), writeTarget, r));
        return true;
    }


    public boolean writeAll( boolean force )
    {
        Msg.emsg("writAll() not implemented");
        return false;
    }


    public void edit( ViTextView tv, boolean force, int n )
    {
        // get the av to open
        ViAppView av = getAppViewByNumber(n);
        Msg.emsg("edit(tv, int{" + n + "}, force) not implemented");
    }


    public void edit( ViTextView tv, boolean force, Object fileThing )
    {
        Msg.emsg("edit(tv, int{" + fileThing + "}, force) not implemented");
    }


} // end com.raelity.jvi.PlayFS
