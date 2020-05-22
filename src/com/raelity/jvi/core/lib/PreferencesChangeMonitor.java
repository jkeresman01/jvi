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

import static com.raelity.text.TextUtil.sf;

/**
 * Monitor preferences subtrees for creation and/or changes.
 * startMonitoring can be called multiple times to monitor several trees.
 * <p/>
 * This detects changes made through the prefs interface. Because of the
 * async nature of prefs change notifications , this can detect changes,
 * but it is impossible to make "safe" changes, see internalChange(runnable).
 * Using prefs.flush() does not help; the changes may be sync'd
 * but the notificaitons are still async.
 * <p/>
 * I suppose it is possible to Q up all changes and resolve them with
 * incoming notifications, but what a PITA.
 * <p/>
 * NEEDSWORK:   if a change is detected, then remove listeners right away.
 * <p/>
 * NEEDSWORK:   could track all startChecking stopChecking args and
 *              have a clear/reset method does stopChecking as needed.
 *              I suppose could put that in a finalize...
 * NEEDSWORK:   or use weak listeners
 * 
 * @see PreferencesImportMonitor
 * 
 * @author Ernie Rael <err at raelity.com>
 */
public final class PreferencesChangeMonitor {
    private boolean change;
    private boolean freeze;
    private final PreferenceChangeListener prefListener;
    private final NodeChangeListener nodeListener;
    private final Map<String, ParentListener> parentListeners;
    private final Set<ParentChild> parentChilds = new HashSet<>();

    private PreferencesChangeMonitor()
    {
        prefListener = new PrefCheckListener();
        nodeListener = new NodeCheckListener();
        parentListeners = new HashMap<>(2);
    }

    /**
     * Preferences and child may both be null,
     * in which case startMonitoring should be used.
     * Calls startMonitoring if parent/child != null.
     */
    public static PreferencesChangeMonitor getMonitor(Preferences parent,
                                                      String child)
    {
        G.dbgPrefChangeMonitor().println(Level.CONFIG, () ->
                sf("PCM: getMonitor %s / %s", parent != null ? parent.name() : "null",
                                              child != null ? child : "null"));
        PreferencesChangeMonitor checker = new PreferencesChangeMonitor();

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
        return change;
    }

    /** run something that is allowed to make a change */
    public void internalChange(Runnable run)
    {
        freeze = true;
        try {
            run.run();
        } finally {
            freeze = false;
        }
    }

    private void changeDetected()
    {
        if(!freeze) {
            change = true;
            G.dbgPrefChangeMonitor().println("PCM: CHANGE:");
        } else
            G.dbgPrefChangeMonitor().println("PCM: CHANGE FROZEN:");
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
                    G.dbgPrefChangeMonitor().println(() ->
                            "PCM: PARENT START CHECKING: "
                                    + child + " in " + parent.absolutePath());
                }
            }
            catch (IllegalArgumentException | IllegalStateException ex) {}
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
                    G.dbgPrefChangeMonitor().println(() ->
                            "PCM: START CHECKING: " + prefs.absolutePath());
                }
            }
            catch(IllegalArgumentException | IllegalStateException ex) { }

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
        private final String child;

        public ParentListener(String child)
        {
            this.child = child;
        }

        @Override
        public void childAdded(NodeChangeEvent evt)
        {
            if(evt.getChild().name().equals(child)) {
                changeDetected();
                G.dbgPrefChangeMonitor().println(() ->
                        "PCM: PARENT NODE CHANGE: childAdded: "
                                + evt.getChild().name()
                                + " in " + evt.getParent().absolutePath());
            } else
                G.dbgPrefChangeMonitor().println(() ->
                        "PCM: PARENT NODE CHANGE IGNORED: childAdded: "
                                + evt.getChild().name()
                                + " in " + evt.getParent().absolutePath());
        }

        @Override
        public void childRemoved(NodeChangeEvent evt)
        {
            if(evt.getChild().name().equals(child)) {
                changeDetected();
                G.dbgPrefChangeMonitor().println(() ->
                        "PCM: PARENT NODE CHANGE: childRemoved: "
                                + evt.getChild().name()
                                + " in " + evt.getParent().absolutePath());
            } else
                G.dbgPrefChangeMonitor().println(() ->
                        "PCM: PARENT NODE CHANGE IGNORED: childRemoved: "
                                + evt.getChild().name()
                                + " in " + evt.getParent().absolutePath());
        }

    }

    private class NodeCheckListener
    implements NodeChangeListener
    {
        @Override
        public void childAdded(NodeChangeEvent evt)
        {
            changeDetected();
            G.dbgPrefChangeMonitor() .println(() ->
                    "PCM: NODE CHANGE: childAdded: " + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
        }

        @Override
        public void childRemoved(NodeChangeEvent evt)
        {
            changeDetected();
            G.dbgPrefChangeMonitor().println(() ->
                    "PCM: NODE CHANGE: childRemoved: " + evt.getChild().name()
                            + " in " + evt.getParent().absolutePath());
        }

    }

    private class PrefCheckListener
    implements PreferenceChangeListener
    {
        @Override
        public void preferenceChange(PreferenceChangeEvent evt)
        {
            changeDetected();
            G.dbgPrefChangeMonitor().println(() -> "PCM: PREF CHANGE: "
                    + evt.getKey() + " in " + evt.getNode().absolutePath());
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
