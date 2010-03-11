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
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.swing.SwingFactory;
import java.awt.Container;
import javax.swing.text.JTextComponent;

/**
 * Basic Swing AppView. This is suitable when there is a 1-1
 * and invarient relationship between Container and Text.
 * Assumes that equals is '=='.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public abstract class SimpleAppView implements ViAppView
{
    private Container c;
    private JTextComponent e;
    private int wnum;

    private static int genWNum; // for the generation of the unique nums

    /** this is added as a client property to the JTextComponent
     * satisfying the requirements of {@link SwingFactory}
     * @param c platform/application handle
     * @param e text component where the action happens
     */
    public SimpleAppView(Container c, JTextComponent e)
    {
        this.c = c;
        this.e = e;
        wnum = ++genWNum;
        // The assert can be used when equals is based on '=='
        assert e.getClientProperty(SwingFactory.PROP_AV) == null;
        linkFromTextComponent();
    }

    private void linkFromTextComponent()
    {
        e.putClientProperty(SwingFactory.PROP_AV, this);
    }

    public int getWNum()
    {
        return wnum;
    }

    public boolean isShowing()
    {
        return e.isShowing();
    }

    public JTextComponent getEditor()
    {
        return e;
    }

    /** Note this is not part of the ViAppView interface,
     * override to cast in more convenient form. */
    protected Container getAppContainer() {
        return c;
    }

    @Override
    public String toString()
    {
        return ViManager.getFS().getDisplayFileName(this);
    }

}
