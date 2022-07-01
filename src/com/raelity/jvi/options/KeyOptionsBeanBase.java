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
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.options;

import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;
import java.util.prefs.Preferences;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.OptUtil.OptionChangeHandler;
import com.raelity.jvi.swing.KeyBinding;

import static com.raelity.jvi.manager.ViManager.getFactory;

/**
 *
 * @author erra
 */
public class KeyOptionsBeanBase extends SimpleBeanInfo
implements Options.EditControl
{
    private final OptionChangeHandler optionChangeHandler;
    final protected Preferences prefs;



    public KeyOptionsBeanBase()
    {
        this.prefs = getFactory()
                .getPreferences().node(ViManager.PREFS_KEYS);
        this.optionChangeHandler = new OptionChangeHandler();
    }


    @Override
    public void cancel()
    {
        optionChangeHandler.clear();
    }

    @Override
    public void ok()
    {
        // Now's the time to persist the changes
        optionChangeHandler.applyChanges();
    }

    @Override
    public void start()
    {
        optionChangeHandler.clear();
    }

    protected void put(String name, boolean val) {
        optionChangeHandler.changeOption(name, get(name), val);
    }

    protected boolean get(String name) {
        return prefs.getBoolean(name, KeyBinding.getCatchKeyDefault(name));
    }

    private static Image icon, icon32;
    @Override
    public Image getIcon (int type) {
        if (type == BeanInfo.ICON_COLOR_16x16
                || type == BeanInfo.ICON_MONO_16x16) {
            if (icon == null)
                icon = loadImage("/com/raelity/jvi/resources/jViLogo.png");
            return icon;
        } else {
            if (icon32 == null)
                icon = loadImage("/com/raelity/jvi/resources/jViLogo32.png");
            return icon32;
        }
    }

    protected static final String shortDescription =
                "Checked (enabled) means that jVi will process"
            + " this key. If clear (disabled) then the key"
            + " is available for other keybindings.";
}
