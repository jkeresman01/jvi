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

import com.raelity.jvi.ViAppView;
import java.awt.Container;
import javax.swing.text.JTextComponent;

/**
 * Basic Swing AppView. This is suitable when there is a 1-1
 * and invarient relationship between Container and Text.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public abstract class AppView implements ViAppView
{
    private Container c;
    private JTextComponent e;

    /** this is added as a client property to the JTextComponent
     * satisfying the requirements of {@link SwingFactory}
     * @param c platform/application handle
     * @param e text component where the action happens
     */
    public AppView(Container c, JTextComponent e)
    {
        this.c = c;
        this.e = e;
        assert e.getClientProperty(SwingFactory.PROP_AV) == null;
        e.putClientProperty(SwingFactory.PROP_AV, this);
    }

    /** Note this is not part of the ViAppView interface,
     * override to cast in more convenient form. */
    protected Container getAppContainer() {
        return c;
    }

    /** Override this to cast in more convenient form */
    protected JTextComponent getTextComponent()
    {
        return e;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AppView other = (AppView)obj;
        if (this.c != other.c && (this.c == null || !this.c.equals(other.c))) {
            return false;
        }
        if (this.e != other.e &&
                (this.e == null || !this.e.equals(other.e))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 41 * hash + (this.c != null ? this.c.hashCode() : 0);
        hash = 41 * hash + (this.e != null ? this.e.hashCode() : 0);
        return hash;
    }

}
