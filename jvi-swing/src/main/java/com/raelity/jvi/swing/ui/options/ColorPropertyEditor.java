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

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.swing.ComponentFactory;
import com.l2fprod.common.swing.PercentLayout;
import com.l2fprod.common.swing.renderer.ColorCellRenderer;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.options.ColorOption;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class ColorPropertyEditor extends AbstractPropertyEditor {
    public static final int SHOW_NULL = 1;
    public static final int SHOW_DFLT = 2;
    private ColorCellRenderer label;
    private JButton button;
    private Color color;
    private Property property;

    public ColorPropertyEditor()
    {
        this(SHOW_NULL, null);
    }

    public ColorPropertyEditor(Property property)
    {
        this(SHOW_DFLT |
                (((ColorOption)Options.getOption(property.getName())).isPermitNull()
                ? SHOW_NULL : 0), property);
    }

    public ColorPropertyEditor(int flags, Property property)
    {
        this.property = property;
        editor = new JPanel(new PercentLayout(PercentLayout.HORIZONTAL, 0));
        ((JPanel)editor).add("*", label = new ColorCellRenderer());
        label.setOpaque(false);
        ((JPanel)editor).add(
                button = ComponentFactory.Helper.getFactory().createMiniButton());
        button.addActionListener((ActionEvent e) -> {
            selectColor();
        });
        if((flags & SHOW_NULL) != 0) {
            ((JPanel)editor).add(button
                    = ComponentFactory.Helper.getFactory().createMiniButton());
            button.setText("X");
            button.addActionListener((ActionEvent e) -> {
                selectNull();
            });
        }
        if((flags & SHOW_DFLT) != 0 && property != null) {
            ((JPanel)editor).add(button
                    = ComponentFactory.Helper.getFactory().createMiniButton());
            button.setText("DFLT");
            button.addActionListener((ActionEvent e) -> {
                selectDefault();
            });
        }
        ((JPanel)editor).setOpaque(false);
    }

    @Override
    public Object getValue()
    {
        return color;
    }

    @Override
    public void setValue(Object value)
    {
        color = (Color)value;
        label.setValue(color);
    }

    protected void selectColor()
    {
        // ResourceManager rm = ResourceManager.all(FilePropertyEditor.class);
        // String title = rm.getString("ColorPropertyEditor.title");
        String title = "Color section";
        Color selectedColor = JColorChooser.showDialog(editor, title, color);
        if(selectedColor != null) {
            Color oldColor = color;
            Color newColor = selectedColor;
            label.setValue(newColor);
            color = newColor;
            firePropertyChange(oldColor, newColor);
        }
    }

    protected void selectDefault()
    {
        Color oldColor = color;
        ColorOption opt = (ColorOption)Options.getOption(property.getName());
        Color newColor = opt.getDefault();
        label.setValue(newColor);
        color = newColor;
        firePropertyChange(oldColor, newColor);
    }

    protected void selectNull()
    {
        Color oldColor = color;
        Color newColor = null;
        if(property != null) {
            ColorOption opt = (ColorOption)Options.getOption(property.getName());
            newColor = opt.decode("");
        }
        label.setValue(newColor);
        color = newColor;
        firePropertyChange(oldColor, newColor);
    }
    
}
