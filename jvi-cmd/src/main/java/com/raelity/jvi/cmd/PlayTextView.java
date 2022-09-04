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

import com.raelity.jvi.ViAppView;

import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;

import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.*;
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
        // Make this one so that scrollToLine is called for every line
        // change. Need this since PlayCaret overrides udpateVisual
        // and scrollToRect is never called.
        cursorLineMoveCheck = 1;
    }

    @Override
    public void viOptionSet(ViTextView tv, VimOption vopt) {
        super.viOptionSet(tv, vopt);
        if("w_p_wrap".equals(vopt.getVarName())) {
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
        PlayAppView av = (PlayAppView) ViManager.getFactory().getAppView(editorPane);
        getEditor().lineWrap = w_p_wrap;
        av.getContainer().setHorizontalScrollBarPolicy(
                w_p_wrap ? JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                         : JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    @Override
    public void win_close(boolean freeBuf)
    {
        PlayAppView av = (PlayAppView) getAppView();
        Util.winClose(av);
    }

    @Override
    public void win_quit()
    {
        win_close(false);
    }
    

    @Override
    public void win_split(Direction dir, int n, ViAppView avNew_) {
        PlayAppView av = (PlayAppView) getAppView();
        PlayAppView avNew = (PlayAppView) avNew_;
        if(avNew == null) {
            // create a new editor/buffer to put in the other side of new splitter
            PlayEditorContainer edC
                    = new PlayEditorContainer((JviFrame) av.getFrame());
            avNew = (PlayAppView) ViManager.getFactory()
                    .getAppView(edC.getEditor());
        }
        Util.winSplit(av, dir, n, avNew);
    }

    
}
