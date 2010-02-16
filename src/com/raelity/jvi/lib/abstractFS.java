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

package com.raelity.jvi.lib;

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.ViBuffer;
import com.raelity.jvi.ViFS;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.Misc;
import com.raelity.jvi.core.Window;
import java.util.Iterator;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
abstract public class abstractFS implements ViFS
{
    /**
     * Look up the appView. If i ge 0 then find app view with window that
     * matches that number. If i lt 0 then use it to index into the mru list.
     * @param i
     * @return
     */
    //
    // NEEDSWORK: this is static and app independent
    //
    protected ViAppView getAppViewByNumber(int i)
    {
        ViAppView av = null;
        if(i >= 0) {
            Iterator<ViAppView> iter = ViManager.getTextBufferIterator();
            while(iter.hasNext()) {
                ViAppView av01 = iter.next();
                if(i == av01.getWNum()) {
                    av = av01;
                    break;
                }
            }
        } else {
            av = ViManager.getMruBuffer(-i);
        }
        return av;
    }

    //
    // NEEDSWORK: this is static and app independent
    //
    public String getDisplayFileViewInfo(ViTextView tv) {
        ViAppView av = tv.getAppView();
        Buffer buf = tv.getBuffer();
        Window win = (Window)tv;
        StringBuffer sb = new StringBuffer();
        sb.append("\"" + (av != null ? getDisplayFileName(av)
                                     : getDisplayFileName(buf)) + "\"");
        if(isModified(buf))
            sb.append(" [Modified]");
        if(isReadOnly(buf))
            sb.append(" [readonly]");
        int l = buf.getLineCount();
        int percent = (win.w_cursor.getLine() * 100) / l;
        if(true) {
            sb.append(" " + l + " line" + Misc.plural(l));
            sb.append(" --" + percent + "%--");
        } else {
            sb.append(" line " + win.w_cursor.getLine());
            sb.append(" of " + buf.getLineCount());
            sb.append(" --" + percent + "%--");
            sb.append(" col " + win.w_cursor.getColumn());
        }
        if(av != null) {
            if(av.isNomad()) {
                sb.append("(nomad");
                if(!tv.isEditable())
                    sb.append(",no-edit");
                sb.append(")");
            }
        }
        return sb.toString();
    }

    public String getDisplayFileNameAndSize(ViBuffer buf) {
        StringBuffer sb = new StringBuffer();
        sb.append("\"" + buf.getDisplayFileName() + "\"");
        int l = buf.getLineCount();
        sb.append(" " + buf.getLineCount() + "L, ");
        sb.append(" " + buf.getLength() + "C");
        return sb.toString();
  }

}
