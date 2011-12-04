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

package com.raelity.jvi.cmd;

import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;

import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.swing.simple.SimpleTextView;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class PlayTextView extends SimpleTextView
{
    public PlayTextView(final JTextComponent editor, ViStatusDisplay sd)
    {
        super(editor);
        this.statusDisplay = sd;
    }

    @Override
    public void viOptionSet(ViTextView tv, String name) {
        super.viOptionSet(tv, name);
        if("w_p_wrap".equals(name)) {
            updateWrapOption();
        }
    }

    @Override
    public void activateOptions( ViTextView tv )
    {
        super.activateOptions(tv);
        updateWrapOption();
    }

    @Override
    public PlayEditorPane getEditor()
    {
        return (PlayEditorPane)super.getEditor();
    }

    private void updateWrapOption()
    {
        JviFrame frame = Jvi.mapJepFrame.get(getEditor());
        getEditor().lineWrap = w_p_wrap;
        frame.scrollPane.setHorizontalScrollBarPolicy(
                w_p_wrap ? JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                         : JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

}
