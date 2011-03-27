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
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.swing.KeyBinding;
import com.raelity.jvi.swing.SwingFactory;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
abstract public class SimpleFactory extends SwingFactory {

    /**
     *<pre>
     * NOTE: if it is possible that different text
     *       components might have different keymaps,
     *       then this will have to be changed.
     *       Maybe a {@literal Map<OriginalKeymap, jViKeymap>}
     *</pre>
     * @param ed 
     */
    public static void installKeymap(JTextComponent ed)
    {
        Keymap keymap = JTextComponent.getKeymap("jVi");
        if(keymap == null) {
            keymap = KeyBinding.getKeymap("jVi");
            Keymap km = ed.getKeymap();
            keymap.setResolveParent(km);
        }
        ed.setKeymap(keymap);
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public ViTextView getTextView(ViAppView av)
    {
        if (av.getEditor() != null)
            return getTextView((JTextComponent)av.getEditor());
        return null;
    }

}
