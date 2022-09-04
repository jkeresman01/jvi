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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class BooleanAsCheckBoxPropertyEditor extends AbstractPropertyEditor {

    public BooleanAsCheckBoxPropertyEditor()
    {
        editor = new JCheckBox();
        ((JCheckBox)editor).setOpaque(false);
        ((JCheckBox)editor).addActionListener((ActionEvent e) -> {
            firePropertyChange(((JCheckBox)editor).isSelected()
                        ? Boolean.FALSE : Boolean.TRUE,
                    ((JCheckBox)editor).isSelected()
                            ? Boolean.TRUE : Boolean.FALSE);
            ((JCheckBox)editor).transferFocus();
        });
    }

    @Override
    public Object getValue()
    {
        return ((JCheckBox)editor).isSelected() ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public void setValue(Object value)
    {
        ((JCheckBox)editor).setSelected(Boolean.TRUE.equals(value));
    }
    
}
