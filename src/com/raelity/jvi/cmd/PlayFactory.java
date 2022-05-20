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

import java.awt.Component;
import java.awt.Rectangle;
import java.util.EnumSet;

import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import com.raelity.jvi.ViCaret;
import com.raelity.jvi.ViFS;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.swing.LineMap;
import com.raelity.jvi.swing.LineMapNoFolding;
import com.raelity.jvi.swing.SwingCaret;
import com.raelity.jvi.swing.SwingTextView;
import com.raelity.jvi.swing.ViewMap;
import com.raelity.jvi.swing.ViewMapSwitcher;
import com.raelity.jvi.swing.simple.SimpleFactory;

import javax.swing.text.Document;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
final public class PlayFactory extends SimpleFactory
{
    private final ViFS fs = new PlayFS();
    /** status displays for editors */

    public PlayFactory() {
    }

    @Override
    public Component getMainWindow()
    {
        return Jvi.mainWindow;
    }

    @Override
    protected ViTextView newTextView( JTextComponent editor )
    {
        JviFrame frame = (JviFrame)((PlayAppView)getAppView(editor)).getFrame();
        @SuppressWarnings("element-type-mismatch")
        SwingTextView tv = new PlayTextView(
                editor, frame.getStatusDisplay());

        LineMap lm = new LineMapNoFolding(tv);
        //ViewMap vm = new SwingViewMapWrapFontFixed(tv);
        ViewMap vm = new ViewMapSwitcher(tv);
        tv.setMaps(lm, vm);
        return tv;
    }

    /**
     * For debug, don't want the cursor moving on its own
     * or scrolling the view.
     */

    class PlayCaret extends SwingCaret {

        public PlayCaret()
        {
            // The following is good for testing that jVi keeps the caret
            // visible. However it screws wiht isertion caret update.
            // setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }

        @Override
        protected void adjustVisibility(Rectangle nloc)
        {
        }

    }

    @Override
    @SuppressWarnings("null")
    public void setupCaret(Component editor)
    {
        JTextComponent ed = (JTextComponent)editor;
        // install cursor if neeeded
        Caret c = ed.getCaret();
        if ( !(c instanceof ViCaret) ) {
            SwingCaret caret = new PlayCaret();
            ed.setCaret(caret);
            caret.setDot(c.getDot());
            int n = Options.getOption(Options.caretBlinkRate).getInteger();
            caret.setBlinkRate(n);
            caret.setVisible(c.isVisible());
        }
    }

    @Override
    protected Buffer createBuffer( ViTextView tv )
    {
        PlayBuffer buf = new PlayBuffer(tv);
        ViAppView av = getAppView(tv.getEditor());
        buf.getDocument().putProperty(Document.TitleProperty,
                "File:" + av.getWinID());
        return buf;
    }

    @Override
    public ViFS getFS()
    {
        return fs;
    }

    @Override
    public ViOutputStream createOutputStream(
            ViTextView tv, Object type, Object info,
            EnumSet<ViOutputStream.FLAGS> flags)
    {
        return new PlayOutputStream(
                tv, type.toString(),
                info == null ? null : info.toString(), flags);
    }

}
