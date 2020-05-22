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
package com.raelity.jvi.core.lib;

import java.util.Date;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import com.raelity.jvi.core.G;

import static com.raelity.text.TextUtil.sf;

/**
 * This looks for change from IDE import options.
 * IDE import modifies the file system
 * and doesn't go through the preferences interface.
 * See PreferencesChangeMonitor to look for changes through
 * the preferences interface.
 * <p/>
 * The driving issue for this is that when you import options, the files
 * on disk are changed <em>before</em> either the Module closing() or close()
 * are invoked. The implication is that you can not write to preferences
 * from module closing() or close(). Additionally, TopComponent close
 * is not invoked when NB's close button is clicked.
 * <p/>
 * So a timestamp is added into pref nodes that can be imported.
 * This is initialized at ide startup
 * and the value is saved. An IDE import overwrites this value.
 * 
 * @author Ernie Rael <err at raelity.com>
 */
public final class PreferencesImportMonitor {
    private String importBaseName;

    private static final boolean ENABLE_HACK = true;
    private boolean hack;
    private Preferences hackBase;
    private String hackValue;
    private static final String HACK_KEY = "IMPORT_CHECK_HACK";
    private static FileHack fileHack;

    // All very messy...

    private PreferencesImportMonitor()
    {
    }

    public static void setFileHack(FileHack fileHack)
        { PreferencesImportMonitor.fileHack = fileHack; }
    public interface FileHack {
        // Checks the file system (not through prefs), return true if as expected
        public boolean hasKeyValue(Preferences prefs, String child,
                                   String key, String val);
    }

    /**
     * Only look for external imports.
     * Do not listen to preferences events (it's flakey anyway).
     */
    public static PreferencesImportMonitor getMonitor(Preferences parent,
                                                            String child)
    {
        G.dbgPrefChangeMonitor().println(Level.CONFIG, () ->
                sf("PCM: getImportMonitor %s / %s",
                   parent != null ? parent.name() : "null",
                   child != null ? child : "null"));
        if(parent == null || child == null)
            throw new IllegalArgumentException("parent or child null");
        PreferencesImportMonitor checker = new PreferencesImportMonitor();

        if(ENABLE_HACK) {
            // Use tricks to detect nb import.
            // This has the unfortunate side effect of creating any child
            // node that we are monitoring.
            // <p/>
            assert checker.hackBase == null || checker.hackBase == parent;
            checker.hack = true;
            checker.hackBase = parent;
            checker.hackValue = new Date().toString();
        }

        checker.setupImportCheck(parent, child);
        return checker;
    }

    private void setupImportCheck(Preferences parent, String child) {
        if(ENABLE_HACK) {
            if(hack) {
                String childPath = parent.absolutePath() + "/" + child;
                importBaseName = childPath;
                assert childPath.startsWith(hackBase.absolutePath());
                Preferences p = parent.node(child);
                p.put(HACK_KEY, hackValue);
            }
        }
    }

    public boolean isChange()
    {
        boolean importChange = false;
        if(ENABLE_HACK) {
            if(hack) {
                String path = importBaseName;
                // +1 in following for '/' serarator
                String child = path.substring(
                        hackBase.absolutePath().length()+1);
                if(fileHack != null) {
                    if(!fileHack.hasKeyValue(hackBase, child,
                                                           HACK_KEY, hackValue)) {
                        importChange = true;
                        G.dbgPrefChangeMonitor().println(() ->
                                "PCM: CHANGE: " + "HACK in " + path);
                    }
                } else {
                    Preferences p = hackBase.node(child);
                    if(!p.get(HACK_KEY, "").equals(hackValue)) {
                        importChange = true;
                        G.dbgPrefChangeMonitor().println(() ->
                                "PCM: CHANGE: " + "HACK in " + p.absolutePath());
                    }
                }
            }
        }
        return importChange;
    }
}
