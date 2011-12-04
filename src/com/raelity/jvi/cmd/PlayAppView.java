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

import javax.swing.JEditorPane;
import javax.swing.JFrame;

import com.raelity.jvi.swing.SwingFactory;
import com.raelity.jvi.swing.simple.SimpleAppView;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class PlayAppView extends SimpleAppView
{
    @SuppressWarnings("LeakingThisInConstructor")
    public PlayAppView(JFrame f, JEditorPane ep)
    {
        super(f, ep);
        // For convenience, in case want to get from JFrame-->AppView.
        // In this play app, a JFrame has one and only one editor.
        f.getRootPane().putClientProperty(SwingFactory.PROP_AV, this);
    }

    /** Note this is not part of the ViAppView interface */
    public JFrame getFrame() {
        return (JFrame)super.getAppContainer();
    }

    @Override
    public JEditorPane getEditor()
    {
        return (JEditorPane)super.getEditor();
    }

    @Override
    public boolean isNomad()
    {
        return false;
    }

}
