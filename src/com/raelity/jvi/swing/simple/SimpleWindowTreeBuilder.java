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
import com.raelity.jvi.core.lib.WindowTreeBuilder;
import com.raelity.jvi.manager.ViManager;
import java.awt.Component;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

/**
 * Swing inmplementation.
 *
 * NOTE: this should not be cached. It would be great to build an implementation
 * that tracks UI changes so the expensive recalc could be be avoided. NEEDSWORK:
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class SimpleWindowTreeBuilder extends WindowTreeBuilder
{
    public SimpleWindowTreeBuilder(List<ViAppView> avs)
    {
        super(avs);
    }

    @Override
    protected Component windowForAppView(ViAppView av)
    {
        return ((JComponent)av.getEditor()).getRootPane().getContentPane();
    }

    @Override
    protected boolean removeFromToDo(Component c)
    {
        return removeFromToDo(ViManager.getFactory().getAppView(c));
    }

    @Override
    protected boolean isEditor(Component c)
    {
        ViAppView av = null;
        if(c instanceof JTextComponent)
            av = ViManager.getFactory().getAppView(c);
        assert av == null || isToDo(av);
        return isToDo(av);
    }

    @Override
    protected ViAppView getAppView(Component c)
    {
        ViAppView av = ViManager.getFactory().getAppView(c);
        return av;
    }

}
