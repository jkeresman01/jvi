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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import com.raelity.jvi.core.G;

/**
 * Monitor preferences subtrees for creation and/or changes.
 * startMonitoring can be called multiple times to monitor several trees.
 * <p/>
 * Set variable DUMP to true for ViManager/System.err output.
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
    private Set<ParentChild> parentChilds = new HashSet<ParentChild>();

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
    }

    public static void setFileHack(FileHack fileHack)
        { PreferencesChangeMonitor.fileHack = fileHack; }
    public interface FileHack {
        public boolean hasKeyValue(Preferences prefs, String child,
                                   String key, String val);
    }

    /**
     * Preferences and child may both be null,
     * in which case startMonitoring should be used.
     * Calls startMonitoring if parent/child != null.
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

    /** stop monitoring all parent-child sets */
    public void stopAll()
    {
        for(ParentChild pc : parentChilds) {
            stopMonitoring(pc.parent, pc.child);
        }
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
                            if(G.dbgPrefChangeMonitor().getBoolean()) {
                                G.dbgPrefChangeMonitor().println("PREF CHANGE: "
                                        + "HACK in " + path);
                            }
                        }
                    } else {
                        Preferences p = hackBase.node(child);
                        if(!p.get(HACK_KEY, "").equals(hackValue)) {
                            change = true;
                            if(G.dbgPrefChangeMonitor().getBoolean()) {
                                G.dbgPrefChangeMonitor().println("PREF CHANGE: "
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
            if(G.dbgPrefChangeMonitor().getBoolean()) {
                G.dbgPrefChangeMonitor().println("CHANGE FROZEN:");
            }
    }

    /**
     * Start monitor the parent-child.
     * @param parent parent to monitor for the child
     * @param child  child subtree to monitor
     * @throws IllegalStateException if parent-child already monitored by this
     */
    public void startMonitoring(Preferences parent, String child)
    {
        ParentChild pc = new ParentChild(parent, child);
        if(!parentChilds.add(pc)) {
            throw new IllegalStateException(
                    "parent-child duplicate: " + parent + ":" + child);
        }

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
        ParentChild pc = new ParentChild(parent, child);
        parentChilds.remove(pc);

        parentCheck(true, parent, child);
        try {
            if(parent.nodeExists(child)) {
                recursiveCheck(true, parent.node(child));
            }
        } catch(BackingStoreException ex) {
        }
    }

    private void parentCheck(boolean remove,
                             Preferences parent,
                             String child)
    {
        String childPath = parent.absolutePath() + "/" + child;
        if(ENABLE_HACK) {
            if(hack) {
                assert childPath.startsWith(hackBase.absolutePath());
                if(!remove) {
                    Preferences p = parent.node(child);
                    p.put(HACK_KEY, hackValue);
                }
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
                    if(G.dbgPrefChangeMonitor().getBoolean()) {
                        G.dbgPrefChangeMonitor().println("PARENT START CHECKING: "
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
                    if(G.dbgPrefChangeMonitor().getBoolean()) {
                        G.dbgPrefChangeMonitor().println("START CHECKING: "
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
                if(G.dbgPrefChangeMonitor().getBoolean()) {
                    G.dbgPrefChangeMonitor().println("PARENT NODE CHANGE: childAdded: "
                            + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
                }
            } else
                if(G.dbgPrefChangeMonitor().getBoolean()) {
                    G.dbgPrefChangeMonitor().println("PARENT NODE CHANGE IGNORED: childAdded: "
                            + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
                }
        }

        @Override
        public void childRemoved(NodeChangeEvent evt)
        {
            if(evt.getChild().name().equals(child)) {
                changeDetected();
                if(G.dbgPrefChangeMonitor().getBoolean()) {
                    G.dbgPrefChangeMonitor().println("PARENT NODE CHANGE: childRemoved: "
                            + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
                }
            } else
                if(G.dbgPrefChangeMonitor().getBoolean()) {
                    G.dbgPrefChangeMonitor().println("PARENT NODE CHANGE IGNORED: childRemoved: "
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
            if(G.dbgPrefChangeMonitor().getBoolean()) {
                G.dbgPrefChangeMonitor().println("NODE CHANGE: childAdded: "
                        + evt.getChild().name()
                        + " in " + evt.getParent().absolutePath());
            }
        }

        @Override
        public void childRemoved(NodeChangeEvent evt)
        {
            changeDetected();
            if(G.dbgPrefChangeMonitor().getBoolean()) {
                G.dbgPrefChangeMonitor().println("NODE CHANGE: childRemoved: "
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
            if(G.dbgPrefChangeMonitor().getBoolean()) {
                G.dbgPrefChangeMonitor().println("PREF CHANGE: "
                        + evt.getKey()
                        + " in " + evt.getNode().absolutePath());
            }
        }
    }

    private static class ParentChild {
        Preferences parent;
        String child;

        public ParentChild(Preferences parent, String child)
        {
            this.parent = parent;
            this.child = child;
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj == null) {
                return false;
            }
            if(getClass() != obj.getClass()) {
                return false;
            }
            final ParentChild other = (ParentChild)obj;
            if(this.parent != other.parent &&
                    (this.parent == null || !this.parent.equals(other.parent))) {
                return false;
            }
            if((this.child == null) ? (other.child != null)
                    : !this.child.equals(other.child)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = 5;
            hash =
                    97 * hash +
                    (this.parent != null ? this.parent.hashCode() : 0);
            hash = 97 * hash + (this.child != null ? this.child.hashCode() : 0);
            return hash;
        }

    }
}
