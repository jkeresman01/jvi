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

import com.raelity.jvi.core.Options.EditOptionsControl;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.swing.KeyBinding;
import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 *
 * @author erra
 */
public class KeyOptionsBeanBase extends SimpleBeanInfo
        implements EditOptionsControl {

    private Map<String,Boolean> changeMap = new HashMap<String,Boolean>();

    public void cancel()
    {
        for (Map.Entry<String, Boolean> entry : changeMap.entrySet()) {
            String key = entry.getKey();
            Boolean b = entry.getValue();
            prefs.putBoolean(key, b);
        }
    }

    public void clear()
    {
        // no changes so far
        changeMap.clear();
    }

    protected Preferences prefs = ViManager.getViFactory()
                                .getPreferences().node(ViManager.PREFS_KEYS);

    protected void put(String name, boolean val) {
        trackChange(name);
        prefs.putBoolean(name, val);
    }

    protected boolean get(String name) {
        return prefs.getBoolean(name, KeyBinding.getCatchKeyDefault(name));
    }

    // Called before a change is made,
    // record the previous value.
    // Do nothing if a value is already recorded for this key.
    private void trackChange(String name) {
        if(changeMap.containsKey(name))
            return;
        changeMap.put(name, get(name));
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
