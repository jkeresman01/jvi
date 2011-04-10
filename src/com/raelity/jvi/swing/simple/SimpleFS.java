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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.swing.simple;

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.ViBuffer;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.lib.abstractFS;
import java.io.File;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * Filename determination uses {@linkplain Document.TitleProperty}, the
 * app should set this.
 *
 * @author Ernie Rael <err at raelity.com>
 */
abstract public class SimpleFS extends abstractFS
{
    @Override
    public boolean isReadOnly(ViBuffer buf) {
        File f = buf.getFile();
        return f != null && !f.canWrite();
    }

    @Override
    public String getDisplayFileName(ViAppView av)
    {
        JTextComponent ep = (JTextComponent)av.getEditor();
        if (ep != null) {
            ViTextView tv = ViManager.getFactory().getTextView(ep);
            if (tv != null)
                return getDisplayFileName(tv.getBuffer());
        }
        // NEEDSWORK: triggered by com.raelity.jvi.cmd.Jvi.setupFrame debug

        // assert false;
        return "file_name_unkown_from_appview";
    }

    @Override
    public String getDisplayFileName(ViBuffer buf)
    {
        String fname = null;
        Document doc = (Document)buf.getDocument();
        if (doc != null) {
            Object o = doc.getProperty(Document.TitleProperty);
            if (o != null)
                fname = o.toString();
        }
        assert fname != null;
        return fname != null ? fname : "file_name_unknown_from_buf";
    }

}
