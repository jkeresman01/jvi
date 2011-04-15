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

import com.raelity.jvi.manager.ViManager;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * Monitor a preferences subtree for creation and/or changes.
 * <p/>
 * Set variable DUMP to true for System.err output.
 * <p/>
 * NEEDSWORK:   if a change is detected, then remove listeners right away.
 * <p/>
 * NEEDSWORK:   could track all startChecking stopChecking args and
 *              have a clear/reset method does stopChecking as needed.
 *              I suppose could put that in a finalize...
 * @author Ernie Rael <err at raelity.com>
 */
public final class PreferencesChangeMonitor {
    private boolean change;
    private boolean freeze;
    private PreferenceChangeListener prefListener;
    private NodeChangeListener nodeListener;
    private Map<String, ParentListener> parentListeners;

    private static boolean DUMP = false;

    private static final boolean ENABLE_HACK = true;
    private boolean hack;
    private Preferences hackBase;
    private String hackValue;
    private static final String HACK_KEY = "IMPORT_CHECK_HACK";
    private static FileHack fileHack;

    private PreferencesChangeMonitor()
    {
        prefListener = new PrefCheckListener();
        nodeListener = new NodeCheckListener();
        parentListeners = new HashMap<String, ParentListener>(2);
        DUMP = ViManager.isDebugAtHome();
    }

    public static void setFileHack(FileHack fileHack)
        { PreferencesChangeMonitor.fileHack = fileHack; }
    public interface FileHack {
        public boolean hasKeyValue(Preferences prefs, String child,
                                   String key, String val);
    }

    /**
     * Preferences may be null, in which case startMonitoring should be used.
     */
    public static PreferencesChangeMonitor getMonitor(Preferences parent,
                                                      String child)
    {
        PreferencesChangeMonitor checker = new PreferencesChangeMonitor();

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

        if(parent != null)
            checker.startMonitoring(parent, child);
        else if(child != null)
            throw new IllegalArgumentException("parent == null, child != null");
        return checker;
    }

    public boolean isChange()
    {
        if(ENABLE_HACK) {
            if(hack) {
                for(String path : parentListeners.keySet()) {
                    // +1 in following for '/' serarator
                    String child = path.substring(
                            hackBase.absolutePath().length()+1);
                    if(fileHack != null) {
                        if(!fileHack.hasKeyValue(hackBase, child,
                                                 HACK_KEY, hackValue)) {
                            change = true;
                            if(DUMP) {
                                System.err.println("PREF CHANGE: "
                                        + "HACK in " + path);
                            }
                        }
                    } else {
                        Preferences p = hackBase.node(child);
                        if(!p.get(HACK_KEY, "").equals(hackValue)) {
                            change = true;
                            if(DUMP) {
                                System.err.println("PREF CHANGE: "
                                        + "HACK in " + p.absolutePath());
                            }
                        }
                    }
                }
            }
        }
        return change;
    }

    public void setFreeze(boolean freeze)
    {
        this.freeze = freeze;
    }

    private void changeDetected()
    {
        if(!freeze)
            change = true;
        else
            if(DUMP) {
                System.err.println("CHANGE FROZEN:");
            }
    }

    public void startMonitoring(Preferences parent, String child)
    {
        parentCheck(false, parent, child);
        try {
            if(parent.nodeExists(child)) {
                recursiveCheck(false, parent.node(child));
            }
        } catch(BackingStoreException ex) {
        }
    }

    public void stopMonitoring(Preferences parent, String child)
    {
        parent = null; parent.toString();
        // parentCheck(true, prefs);
        // recursiveCheck(true, prefs);
    }

    private void parentCheck(boolean remove,
                             Preferences parent,
                             String child)
    {
        String childPath = parent.absolutePath() + "/" + child;
        if(ENABLE_HACK) {
            if(hack) {
                assert childPath.startsWith(hackBase.absolutePath());
                Preferences p = parent.node(child);
                p.put(HACK_KEY, hackValue);
            }
        }
        ParentListener pl = parentListeners.get(childPath);
        if(pl == null && !remove) {
            pl = new ParentListener(child);
            parentListeners.put(childPath, pl);
        }
        if(pl != null) {
            try {
                if(remove) {
                    parent.removeNodeChangeListener(pl);
                } else {
                    parent.addNodeChangeListener(pl);
                    if(DUMP) {
                        System.err.println("PARENT START CHECKING: "
                                + child + " in "
                                + parent.absolutePath());
                    }
                }
            }
            catch (IllegalArgumentException ex) {}
            catch (IllegalStateException ex) {}
        }
    }

    private void recursiveCheck(boolean remove, Preferences prefs)
    {
        try {
            try {
                if(remove) {
                    prefs.removePreferenceChangeListener(prefListener);
                    prefs.removeNodeChangeListener(nodeListener);
                } else {
                    prefs.addPreferenceChangeListener(prefListener);
                    prefs.addNodeChangeListener(nodeListener);
                    if(DUMP) {
                        System.err.println("START CHECKING: "
                                + prefs.absolutePath());
                    }
                }
            }
            catch(IllegalArgumentException ex) { }
            catch(IllegalStateException ex) { }

            for(String name : prefs.childrenNames()) {
                recursiveCheck(remove, prefs.node(name));
            }
        } catch(BackingStoreException ex) {
            Logger.getLogger(PreferencesChangeMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class ParentListener
    implements NodeChangeListener
    {
        private String child;

        public ParentListener(String child)
        {
            this.child = child;
        }

        @Override
        public void childAdded(NodeChangeEvent evt)
        {
            if(evt.getChild().name().equals(child)) {
                changeDetected();
                if(DUMP) {
                    System.err.println("PARENT NODE CHANGE: childAdded: "
                            + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
                }
            } else
                if(DUMP) {
                    System.err.println("PARENT NODE CHANGE IGNORED: childAdded: "
                            + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
                }
        }

        @Override
        public void childRemoved(NodeChangeEvent evt)
        {
            if(evt.getChild().name().equals(child)) {
                changeDetected();
                if(DUMP) {
                    System.err.println("PARENT NODE CHANGE: childRemoved: "
                            + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
                }
            } else
                if(DUMP) {
                    System.err.println("PARENT NODE CHANGE IGNORED: childRemoved: "
                            + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
                }
        }

    }

    private class NodeCheckListener
    implements NodeChangeListener
    {
        @Override
        public void childAdded(NodeChangeEvent evt)
        {
            changeDetected();
            if(DUMP) {
                System.err.println("NODE CHANGE: childAdded: "
                        + evt.getChild().name()
                        + " in " + evt.getParent().absolutePath());
            }
        }

        @Override
        public void childRemoved(NodeChangeEvent evt)
        {
            changeDetected();
            if(DUMP) {
                System.err.println("NODE CHANGE: childRemoved: "
                        + evt.getChild().name()
                        + " in " + evt.getParent().absolutePath());
            }
        }

    }

    private class PrefCheckListener
    implements PreferenceChangeListener
    {
        @Override
        public void preferenceChange(PreferenceChangeEvent evt)
        {
            changeDetected();
            if(DUMP) {
                System.err.println("PREF CHANGE: "
                        + evt.getKey()
                        + " in " + evt.getNode().absolutePath());
            }
        }
    }
}
