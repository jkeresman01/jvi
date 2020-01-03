/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.swing;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.Action;
import javax.swing.ComboBoxEditor;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;

/**
 * This is for a combo box that contains a JEP.
 * Taken from following where I found a bug, issue 97130 in NB.
 * debuggerjpda/ui/src/org/netbeans/modules/debugger/jpda/ui/Evaluator.java
 * 
 * Historical...
 */
final class ComboBoxEditorPane implements ComboBoxEditor
{
    private final JEditorPane editor;
    //private Component component;
    private Object oldValue;

    public ComboBoxEditorPane()
    {
        editor = new JEditorPane("text/x-vicommand", ""); //NOI18N
        editor.setBorder(null);
        new KeymapUpdater().run();
        /*component = new JScrollPane(editor,
        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        editor.addFocusListener(new FocusListener() {
        public void focusGained(FocusEvent e) {
        WatchPanel.setupContext(editor);
        }
        public void focusLost(FocusEvent e) {
        }
        });*/
        editor.addPropertyChangeListener("keymap",
                                         (PropertyChangeEvent evt) -> {
            if(!(evt.getNewValue() instanceof FilteredKeymap)) {
                // We have to do this lazily, because the property change
                // is fired *before* the keymap is actually changed!
                EventQueue.invokeLater(new KeymapUpdater());
            }
        });
    } //NOI18N
    /*component = new JScrollPane(editor,
    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    editor.addFocusListener(new FocusListener() {
    public void focusGained(FocusEvent e) {
    WatchPanel.setupContext(editor);
    }
    public void focusLost(FocusEvent e) {
    }
    });*/
    // We have to do this lazily, because the property change
    // is fired *before* the keymap is actually changed!

    @Override
    public void addActionListener(ActionListener l)
    {
        // NB that setEditor calls addActionListener
        //editor.addActionListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l)
    {
        //assert false;
        //editor.removeActionListener(l);
    }

    @Override
    public Component getEditorComponent()
    {
        return editor;
    }

    @Override
    public Object getItem()
    {
        Object newValue = editor.getText();
        if(oldValue != null && !(oldValue instanceof String)) {
            // The original value is not a string.
            // Should return the value in it's original type.
            if(newValue.equals(oldValue.toString())) {
                return oldValue;
            } else {
                // Must take the value from the editor
                // and get the value and cast it to the new type.
                Class cls = oldValue.getClass();
                try {
                    @SuppressWarnings(value = "unchecked")
                    Method method
                            = cls.getMethod("valueOf", new Class[]{String.class});
                    newValue
                            = method.invoke(oldValue,
                                            new Object[]{editor.getText()});
                } catch(IllegalAccessException | IllegalArgumentException |
                        NoSuchMethodException | SecurityException |
                        InvocationTargetException ex) {
                    // Fail silent and return the newValue (a String object)
                }
            }
        }
        return newValue;
    }

    @Override
    public void setItem(Object obj)
    {
        if(obj != null) {
            editor.setText(obj.toString());
            oldValue = obj;
        } else {
            editor.setText("");
        }
    }

    @Override
    public void selectAll()
    {
        editor.selectAll();
        editor.requestFocus();
    }

    private class KeymapUpdater implements Runnable
    {
    @Override
    public void run()
    {
        editor.setKeymap(new FilteredKeymap(editor.getKeymap()));
    }
    }

    // taken from same package as the above combo box editor
    public static class FilteredKeymap implements Keymap
    {
    private final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private final KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    private final KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
    private final Keymap keyMap; // The original keymap

    /** Creates a new instance of FilteredKeymap */
    public FilteredKeymap(Keymap keyMap)
    {
        this.keyMap = keyMap;
    }

    @Override
    public void addActionForKeyStroke(KeyStroke key, Action a)
    {
        keyMap.addActionForKeyStroke(key, a);
    }

    @Override
    public Action getAction(KeyStroke key)
    {
        if(enter.equals(key) || esc.equals(key) || tab.equals(key)) {
            return null;
        } else {
            return keyMap.getAction(key);
        }
    }

    @Override
    public Action[] getBoundActions()
    {
        return keyMap.getBoundActions();
    }

    @Override
    public KeyStroke[] getBoundKeyStrokes()
    {
        return keyMap.getBoundKeyStrokes();
    }

    @Override
    public Action getDefaultAction()
    {
        return keyMap.getDefaultAction();
    }

    @Override
    public KeyStroke[] getKeyStrokesForAction(Action a)
    {
        return keyMap.getKeyStrokesForAction(a);
    }

    @Override
    public String getName()
    {
        return keyMap.getName() + "_Filtered"; //NOI18N
    }

    @Override
    public javax.swing.text.Keymap getResolveParent()
    {
        return keyMap.getResolveParent();
    }

    @Override
    public boolean isLocallyDefined(KeyStroke key)
    {
        if(enter.equals(key) || esc.equals(key) || tab.equals(key)) {
            return false;
        } else {
            return keyMap.isLocallyDefined(key);
        }
    }

    @Override
    public void removeBindings()
    {
        keyMap.removeBindings();
    }

    @Override
    public void removeKeyStrokeBinding(KeyStroke keys)
    {
        keyMap.removeKeyStrokeBinding(keys);
    }

    @Override
    public void setDefaultAction(Action a)
    {
        keyMap.setDefaultAction(a);
    }

    @Override
    public void setResolveParent(Keymap parent)
    {
        keyMap.setResolveParent(parent);
    }
    } // end inner class FilteredKeymap
    
} // end inner class ViCommandEditor
