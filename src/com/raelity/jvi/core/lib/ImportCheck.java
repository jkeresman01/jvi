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

import com.raelity.jvi.core.Misc;
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
 * Monitor a preferences subtree for changes.
 * If preferences in a subtree are only saved at shutdown,
 * then this will detect external changes, such as importing preferences.
 * <p/>
 * NEEDSWORK: could track all startChecking stopChecking args and
 *            have a clear/reset method does stopChecking as needed.
 *            I suppose could put that in a finalize...
 * @author Ernie Rael <err at raelity.com>
 */
public final class ImportCheck {
    private boolean imports;
    private PreferenceChangeListener prefListener;
    private NodeChangeListener nodeListener;
    private Map<String, ParentListener> parentListeners;
    private static final boolean DUMP = true;

    public ImportCheck()
    {
        prefListener = new PrefImportCheckListener();
        nodeListener = new NodeImportCheckListener();
        parentListeners = new HashMap<String, ParentListener>(2);
    }

    public ImportCheck(Preferences parent, String child)
    {
        this();
        startChecking(parent, child);
    }

    public boolean isImports()
    {
        return imports;
    }

    public void startChecking(Preferences parent, String child)
    {

        parentImportCheck(false, parent, child);
        try {
            if(parent.nodeExists(child)) {
                recursiveImportCheck(false, parent.node(child));
            }
        } catch(BackingStoreException ex) {
        }
    }

    public void stopChecking(Preferences parent, String child)
    {
        parent = null; parent.toString();
        // parentImportCheck(true, prefs);
        // recursiveImportCheck(true, prefs);
    }

    private void parentImportCheck(boolean remove,
                                   Preferences parent,
                                   String child)
    {
        // Preferences parent = prefs.parent();
        if(parent != null) {
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
    }

    private void recursiveImportCheck(boolean remove, Preferences prefs)
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
                recursiveImportCheck(remove, prefs.node(name));
            }
        } catch(BackingStoreException ex) {
            Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, ex);
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
                imports = true;
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
                imports = true;
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

    private class NodeImportCheckListener
    implements NodeChangeListener
    {
        @Override
        public void childAdded(NodeChangeEvent evt)
        {
            imports = true;
            if(DUMP) {
                System.err.println("NODE CHANGE: childAdded: "
                        + evt.getChild().name()
                        + " in " + evt.getParent().absolutePath());
            }
        }

        @Override
        public void childRemoved(NodeChangeEvent evt)
        {
            imports = true;
            if(DUMP) {
                System.err.println("NODE CHANGE: childRemoved: "
                        + evt.getChild().name()
                        + " in " + evt.getParent().absolutePath());
            }
        }

    }

    private class PrefImportCheckListener
    implements PreferenceChangeListener
    {
        @Override
        public void preferenceChange(PreferenceChangeEvent evt)
        {
            imports = true;
            if(DUMP) {
                System.err.println("PREF CHANGE: "
                        + evt.getKey()
                        + " in " + evt.getNode().absolutePath());
            }
        }
    }
}
