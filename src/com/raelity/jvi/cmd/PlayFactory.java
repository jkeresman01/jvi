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
import com.raelity.jvi.ViFS;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.swing.KeyBinding;
import com.raelity.jvi.swing.SwingFactory;
import java.util.Map;
import javax.swing.text.JTextComponent;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
final public class PlayFactory extends SwingFactory
{
    private ViFS fs = new PlayFS();
    /** status displays for editors */
    private Map<JTextComponent, PlayStatusDisplay> mapJepSd;

    public PlayFactory(Map<JTextComponent, PlayStatusDisplay> m) {
        mapJepSd = m;
    }

    protected ViTextView newTextView( JTextComponent editor )
    {
        return new PlayTextView(editor, mapJepSd.get(editor));
    }

    // NOT PART OF FACTORY INTERFACE
    public static void installKeymap(JTextComponent ed)
    {
        ed.setKeymap(KeyBinding.getKeymap());
    }

    protected Buffer createBuffer( ViTextView tv )
    {
        return new PlayBuffer(tv);
    }

    public ViTextView getTextView(ViAppView av)
    {
        if(av.getEditor() != null)
            return getTextView((JTextComponent)av.getEditor());
        return null;
    }

    @Override
    public ViFS getFS()
    {
        return fs;
    }

    public ViOutputStream createOutputStream(
            ViTextView tv,
            Object type,
            Object info,
            int priority )
    {
        return new PlayOutputStream(
                tv, type.toString(),
                info == null ? null : info.toString() );
    }

}
