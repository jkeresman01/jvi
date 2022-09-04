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

package com.raelity.jvi.swing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class PlainDocumentWithTestFeatures extends PlainDocument
{
    private boolean chunkit = true;

    public PlainDocumentWithTestFeatures()
    {
        try {
            this.insertString(0, "@abcdefghijklmnopqrstuvwxyz", null);
        } catch(BadLocationException ex) {
            Logger.getLogger(PlainDocumentWithTestFeatures.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    public void setChunkit(boolean f)
    {
        chunkit = f;
    }

    @Override
    public void getText(int offset, int length, Segment txt)
            throws BadLocationException
    {
        if(! chunkit) {
            super.getText(offset, length, txt);
            return;
        }

        // return segment no bigger than 10, on a 10 boundary
        int segoff = offset % 10;
        int chunkoff = (offset / 10) * 10;
        length += segoff;
        if(length > 10)
            length = 10;
        // int end = chunkoff + 10;
        // since partial is set, long length should be ok?
        // if(end > getLength())
        //     end = getLength();
        super.getText(chunkoff, length, txt);

        txt.offset += segoff;
        txt.count -= segoff;
        //if(txt.count >= 10)
        //    txt.count = 10 - segoff;
    }
}
