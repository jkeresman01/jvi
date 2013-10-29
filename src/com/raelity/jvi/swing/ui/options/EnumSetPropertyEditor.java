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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private Object[] items;
    private EnumSet oldValue;
    private EnumSet newValue;
    private EnumSetOption opt;
    private JPopupMenu popup;

    private int drawAgain;

    @SuppressWarnings("unchecked")
    public EnumSetPropertyEditor(Property property, Object[] items)
    {
        opt = (EnumSetOption)Options.getOption(property.getName());
        editor = new EditorButton();
        getEditor().setText("click");
        getEditor().addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                doPopup();
            }
        });
        this.items = items;
        selected = new boolean[items.length];
    }

    class EditorButton extends JButton {

        @Override
        public void removeNotify()
        {
            super.removeNotify();
            System.err.println("REMOVE BUTTON");
        }

    }

    private JButton getEditor() {
        return (JButton)editor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EnumSet getValue() {
        EnumSet set = opt.getEmpty();
        for(int i = 0; i < selected.length; i++) {
            if(selected[i])
                set.add(items[i]);
        }
        return set;
    }

    private void fixNewValue() {
        newValue = getValue();
    }

    private class MyPopupMenu extends JPopupMenu {

        @Override
        public void removeNotify()
        {
            super.removeNotify();
            System.err.println("removeNotify " + drawAgain);
        }
    }

    @SuppressWarnings("unchecked")
    private void makePopup() {
        oldValue = EnumSet.copyOf(opt.getValue());
        drawAgain = 0;
        for(int i = 0; i < items.length; i++) {
            selected[i] = oldValue.contains(items[i]);
        }
        popup = new MyPopupMenu();
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
                drawAgain--;
                System.err.println("popupMenuWillBecomeInvisible " + drawAgain);
                if(drawAgain < 0) {
                    // fixNewValue();
                    // EnumSetPropertyEditor.this.firePropertyChange(oldValue,
                    //                                               newValue);
                    // popup = null;
                }
            }
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
        });
        popup.addKeyListener(new KeyAdapter() {
          @Override public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                fixNewValue();
                EnumSetPropertyEditor.this.firePropertyChange(oldValue,
                                                              newValue);
            }
          }
        });
    }

    private void doPopup() {
        makePopup();
        Dimension sz = popup.getPreferredSize();
        sz.width = getEditor().getWidth();
        popup.setPreferredSize(sz);
        popup.show(getEditor(), 0, 0);
    }

    class MyActionListener implements ActionListener {
        private int index;

        public MyActionListener(int index)
        {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JCheckBoxMenuItem cb = (JCheckBoxMenuItem)e.getSource();
            selected[index] = cb.isSelected();
            System.err.println("cb: "
                    + cb.getText() + " " + cb.isSelected());
            popup.show(getEditor(), 0, 0);
            drawAgain++;
        }

    }

}
