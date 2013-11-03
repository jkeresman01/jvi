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
package com.raelity.jvi.swing.ui.options;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.Property;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.options.EnumSetOption;

/**
 * Start with a button and from it use a popup menu with checkbox items.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class EnumSetPropertyEditor extends AbstractPropertyEditor
{
    private boolean[] selected;
    private Enum[] items;
    private EnumSet oldValue;
    private final EnumSetOption opt;
    private JPopupMenu popup;
    private boolean popupActive;

    public EnumSetPropertyEditor(Property property)
    {
        opt = (EnumSetOption)Options.getOption(property.getName());
        editor = new EditorButton();
        getEditor().addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if(!popupActive) {
                    startPopup();
                } else {
                    finishPopup();
                }
            }
        });
    }

    private void startPopup() {
        popupActive = true;
        makePopup();
        showPopup();
    }

    private void finishPopup() {
        if(!popupActive)
            return;
        popupActive = false;
        if(popup != null)
            popup.setVisible(false);
        EnumSet newValue = getValue();
        // we want the editor to close, if the values are equal, then
        // it never sees the event, so in that case...
        EnumSetPropertyEditor.this.firePropertyChange(
                oldValue.equals(newValue) ? null : oldValue, newValue);
        // Since the editor seems to stay around, clean this up
        popup = null;
    }

    class EditorButton extends JButton {

        @Override
        public void removeNotify()
        {
            super.removeNotify();
            finishPopup();
        }

    }

    private JButton getEditor() {
        return (JButton)editor;
    }

    @Override
    public EnumSet getValue() {
        EnumSet set = opt.getEmpty();
        for(int i = 0; i < selected.length; i++) {
            if(selected[i])
                set.add(items[i]);
        }
        return set;
    }

    private void makePopup() {
        this.items = opt.getAvailableValues();
        selected = new boolean[items.length];
        oldValue = EnumSet.copyOf(opt.getEnumSet());
        for(int i = 0; i < items.length; i++) {
            selected[i] = oldValue.contains(items[i]);
        }
        popup = new JPopupMenu();
        for(int i = 0; i < items.length; i++) {
            Object item = items[i];
            JCheckBoxMenuItem cb = new JCheckBoxMenuItem(
                    item.toString(), selected[i]);
            cb.addActionListener(new MyActionListener(i));
            popup.add(cb);
        }
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if(popupActive)
                    EventQueue.invokeLater(new Runnable() {
                        @Override public void run() { showPopup(); }
                    });
            }
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
        });
    }

    private void showPopup() {
        if(popup == null)
            return;
        Dimension sz = popup.getPreferredSize();
        sz.width = getEditor().getWidth();
        popup.setPreferredSize(sz);
        getEditor().setText(opt.encode(getValue()));
        popup.show(getEditor(), 0, getEditor().getHeight());
    }

    class MyActionListener implements ActionListener {
        private final int index;

        public MyActionListener(int index)
        {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JCheckBoxMenuItem cb = (JCheckBoxMenuItem)e.getSource();
            selected[index] = cb.isSelected();
            getEditor().setText(opt.encode(getValue()));
            if(popupActive)
                showPopup();
        }

    }

}
