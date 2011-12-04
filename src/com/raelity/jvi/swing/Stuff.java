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

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JEditorPane;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Stuff
{
    private Stuff()
    {
    }

    static void findKeyboardFocusManagerListeners()
    {
        KeyboardFocusManager m
                = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        for (PropertyChangeListener l : m.getPropertyChangeListeners()) {
            dumpClassObject(m, l);
        }
        dumpCleanup();
    }

    static void findMouseMotionListeners(Component c)
    {
        if(c == null)
            return;
        for (MouseMotionListener mml : c.getMouseMotionListeners()) {
            dumpClassObject(c, mml);
        }
        dumpCleanup();
        findMouseMotionListeners(c.getParent());
    }

    static void trackFocusManager()
    {
        if(focusManagerListener == null) {
            focusManagerListener = new FocusManagerListener();
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener(focusManagerListener);
                //.addPropertyChangeListener("focusOwner", focusManagerListener);
        }
    }

    private static PropertyChangeListener focusManagerListener;
    private static class FocusManagerListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            Object n = evt.getNewValue();
            Object o = evt.getOldValue();
            System.err.println("FOCUS: " + evt.getPropertyName()
                    + " new: " + (n == null ? null : n.getClass().getName())
                    + " old: " + (o == null ? null : o.getClass().getName())
                    );
            if(n instanceof JEditorPane)
                System.err.println("GOT EDITOR");
        }

    }

    private static Object lastComp;
    static void dumpCleanup()
    {
        lastComp = null;
    }
    static void dumpClassObject(Object c, Object o)
    {
        boolean newComp = false;
        if(lastComp == null || lastComp != c) {
            lastComp = c;
            System.err.println(c.getClass().getName());
        }
        System.err.println("    " + o.getClass().getName());
    }
}
