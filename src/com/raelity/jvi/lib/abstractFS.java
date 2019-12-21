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
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.Misc;
import com.raelity.jvi.core.TextView;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
abstract public class abstractFS implements ViFS
{

    //
    // NEEDSWORK: this is static and app independent
    //
    @Override
    public String getDisplayFileViewInfo(ViTextView tv) {
        ViAppView av = tv.getAppView();
        Buffer buf = tv.getBuffer();
        TextView win = (TextView)tv;
        StringBuilder sb = new StringBuilder();
        sb.append("\"")
          .append(av != null ? getDisplayFileName(av) : getDisplayFileName(buf))
          .append("\"");
        if(isModified(buf))
            sb.append(" [Modified]");
        if(isReadOnly(buf))
            sb.append(" [readonly]");
        int l = buf.getLineCount();
        int percent = (win.w_cursor.getLine() * 100) / l;
        if(true) {
            sb.append(" ").append(l).append(" line").append(Misc.plural(l));
            sb.append(" --").append(percent).append("%--");
        }
        // else {
        //     sb.append(" line ").append(win.w_cursor.getLine());
        //     sb.append(" of ").append(buf.getLineCount());
        //     sb.append(" --").append(percent).append("%--");
        //     sb.append(" col ").append(win.w_cursor.getColumn());
        // }
        if(av != null && av.isNomad())
            sb.append(" [nomad]");
        if(!tv.isEditable())
            sb.append(" [not-editable]");
        return sb.toString();
    }

    @Override
    public String getDisplayFileNameAndSize(ViBuffer buf) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(buf.getDisplayFileName()).append("\"");
        int l = buf.getLineCount();
        sb.append(" ").append(buf.getLineCount()).append("L, ");
        sb.append(" ").append(buf.getLength()).append("C");
        return sb.toString();
  }

}
