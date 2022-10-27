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
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
/*
 * KeypadBindingBean.java
 *
 * Created on February 12, 2007, 4:47 PM
 */

package com.raelity.jvi.options;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raelity.jvi.core.lib.*;

/**
 * Use this class to programmatically enable/disable jVi catching particular
 * keypad keys, or to determine if a particular key is being caught.
 *
 * @author erra
 */
public class KeypadBindingBean extends KeyOptionsBeanBase
{
    private static final
            Logger LOG = Logger.getLogger(KeypadBindingBean.class.getName());

    //
    // The BeanInfo is embedded in the same class
    //
    @Override
public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(KeyBindingBean.class) {
            @Override
            public String getDisplayName() {
                return "Keypad Bindings";
            }
        };
    }
    
    @Override
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        final List<PropertyDescriptor> vD = new ArrayList<>();
        for (String key : KeyDefs.getKeypadNames()) {
            addDesc(vD, key, "");
            addDesc(vD, key, "Ctrl");
            addDesc(vD, key, "Shift");
        }
        
        // JDK-8
        //return vD.toArray(new PropertyDescriptor[vD.size()]);
        // JDK-11
        return vD.toArray(PropertyDescriptor[]::new);
    }
    
    private void addDesc(List<PropertyDescriptor> vD, String key, String mod)
    {
        PropertyDescriptor d;
        String displayName = mod.length() == 0 ? key : mod + "-" + key;
        // Want things to display nicely ordered, unmodifed keys followed by
        // Ctrl-, then by Shift-. And want the arrow keys to be together.
        // So preface names with something to control sorting
        switch (key) {
        case "Up":
        case "Down":
        case "Left":
        case "Right":
            key = "AAf" + key;
            break;
        case "Enter":
        case "Escape":
        case "Back_space":
        case "Tab":
            key = "AAb" + key;
            break;
        default:
            key = "AAj" + key;
            break;
        }
        String propertyName = mod.length() == 0 ? key : mod + "_" + key;
        try {
            d = new PropertyDescriptor(propertyName, KeypadBindingBean.class);
        } catch (IntrospectionException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return;
        }
        d.setDisplayName(displayName);
        d.setExpert(false);
        d.setShortDescription(shortDescription);
        vD.add(d);
    }
    
    //
    // The bean
    //      The bean getter/setter
    //
    public void setAAfUp(boolean arg) {
        put("Up", arg);
    }

    public boolean getAAfUp() {
	return get("Up");
    }

    public void setCtrl_AAfUp(boolean arg) {
        put("Ctrl-Up", arg);
    }

    public boolean getCtrl_AAfUp() {
	return get("Ctrl-Up");
    }

    public void setShift_AAfUp(boolean arg) {
        put("Shift-Up", arg);
    }

    public boolean getShift_AAfUp() {
	return get("Shift-Up");
    }

    public void setAAfDown(boolean arg) {
        put("Down", arg);
    }

    public boolean getAAfDown() {
	return get("Down");
    }

    public void setCtrl_AAfDown(boolean arg) {
        put("Ctrl-Down", arg);
    }

    public boolean getCtrl_AAfDown() {
	return get("Ctrl-Down");
    }

    public void setShift_AAfDown(boolean arg) {
        put("Shift-Down", arg);
    }

    public boolean getShift_AAfDown() {
	return get("Shift-Down");
    }

    public void setAAfLeft(boolean arg) {
        put("Left", arg);
    }

    public boolean getAAfLeft() {
	return get("Left");
    }

    public void setCtrl_AAfLeft(boolean arg) {
        put("Ctrl-Left", arg);
    }

    public boolean getCtrl_AAfLeft() {
	return get("Ctrl-Left");
    }

    public void setShift_AAfLeft(boolean arg) {
        put("Shift-Left", arg);
    }

    public boolean getShift_AAfLeft() {
	return get("Shift-Left");
    }

    public void setAAfRight(boolean arg) {
        put("Right", arg);
    }

    public boolean getAAfRight() {
	return get("Right");
    }

    public void setCtrl_AAfRight(boolean arg) {
        put("Ctrl-Right", arg);
    }

    public boolean getCtrl_AAfRight() {
	return get("Ctrl-Right");
    }

    public void setShift_AAfRight(boolean arg) {
        put("Shift-Right", arg);
    }

    public boolean getShift_AAfRight() {
	return get("Shift-Right");
    }

    public void setAAjInsert(boolean arg) {
        put("Insert", arg);
    }

    public boolean getAAjInsert() {
	return get("Insert");
    }

    public void setCtrl_AAjInsert(boolean arg) {
        put("Ctrl-Insert", arg);
    }

    public boolean getCtrl_AAjInsert() {
	return get("Ctrl-Insert");
    }

    public void setShift_AAjInsert(boolean arg) {
        put("Shift-Insert", arg);
    }

    public boolean getShift_AAjInsert() {
	return get("Shift-Insert");
    }

    public void setAAjDelete(boolean arg) {
        put("Delete", arg);
    }

    public boolean getAAjDelete() {
	return get("Delete");
    }

    public void setCtrl_AAjDelete(boolean arg) {
        put("Ctrl-Delete", arg);
    }

    public boolean getCtrl_AAjDelete() {
	return get("Ctrl-Delete");
    }

    public void setShift_AAjDelete(boolean arg) {
        put("Shift-Delete", arg);
    }

    public boolean getShift_AAjDelete() {
	return get("Shift-Delete");
    }

    public void setAAjHome(boolean arg) {
        put("Home", arg);
    }

    public boolean getAAjHome() {
	return get("Home");
    }

    public void setCtrl_AAjHome(boolean arg) {
        put("Ctrl-Home", arg);
    }

    public boolean getCtrl_AAjHome() {
	return get("Ctrl-Home");
    }

    public void setShift_AAjHome(boolean arg) {
        put("Shift-Home", arg);
    }

    public boolean getShift_AAjHome() {
	return get("Shift-Home");
    }

    public void setAAjEnd(boolean arg) {
        put("End", arg);
    }

    public boolean getAAjEnd() {
	return get("End");
    }

    public void setCtrl_AAjEnd(boolean arg) {
        put("Ctrl-End", arg);
    }

    public boolean getCtrl_AAjEnd() {
	return get("Ctrl-End");
    }

    public void setShift_AAjEnd(boolean arg) {
        put("Shift-End", arg);
    }

    public boolean getShift_AAjEnd() {
	return get("Shift-End");
    }

    public void setAAjUndo(boolean arg) {
        put("Undo", arg);
    }

    public boolean getAAjUndo() {
	return get("Undo");
    }

    public void setCtrl_AAjUndo(boolean arg) {
        put("Ctrl-Undo", arg);
    }

    public boolean getCtrl_AAjUndo() {
	return get("Ctrl-Undo");
    }

    public void setShift_AAjUndo(boolean arg) {
        put("Shift-Undo", arg);
    }

    public boolean getShift_AAjUndo() {
	return get("Shift-Undo");
    }

    // Enter
    public void setAAbEnter(boolean arg) {
        put("Enter", arg);
    }

    public boolean getAAbEnter() {
	return get("Enter");
    }

    public void setCtrl_AAbEnter(boolean arg) {
        put("Ctrl-Enter", arg);
    }

    public boolean getCtrl_AAbEnter() {
	return get("Ctrl-Enter");
    }

    public void setShift_AAbEnter(boolean arg) {
        put("Shift-Enter", arg);
    }

    public boolean getShift_AAbEnter() {
	return get("Shift-Enter");
    }

    // Escape
    public void setAAbEscape(boolean arg) {
        put("Escape", arg);
    }

    public boolean getAAbEscape() {
	return get("Escape");
    }

    public void setCtrl_AAbEscape(boolean arg) {
        put("Ctrl-Escape", arg);
    }

    public boolean getCtrl_AAbEscape() {
	return get("Ctrl-Escape");
    }

    public void setShift_AAbEscape(boolean arg) {
        put("Shift-Escape", arg);
    }

    public boolean getShift_AAbEscape() {
	return get("Shift-Escape");
    }

    // Back_space
    public void setAAbBack_space(boolean arg) {
        put("Back_space", arg);
    }

    public boolean getAAbBack_space() {
	return get("Back_space");
    }

    public void setCtrl_AAbBack_space(boolean arg) {
        put("Ctrl-Back_space", arg);
    }

    public boolean getCtrl_AAbBack_space() {
	return get("Ctrl-Back_space");
    }

    public void setShift_AAbBack_space(boolean arg) {
        put("Shift-Back_space", arg);
    }

    public boolean getShift_AAbBack_space() {
	return get("Shift-Back_space");
    }

    // Tab
    public void setAAbTab(boolean arg) {
        put("Tab", arg);
    }

    public boolean getAAbTab() {
	return get("Tab");
    }

    public void setCtrl_AAbTab(boolean arg) {
        put("Ctrl-Tab", arg);
    }

    public boolean getCtrl_AAbTab() {
	return get("Ctrl-Tab");
    }

    public void setShift_AAbTab(boolean arg) {
        put("Shift-Tab", arg);
    }

    public boolean getShift_AAbTab() {
	return get("Shift-Tab");
    }

    public void setAAjPageUp(boolean arg) {
        put("PageUp", arg);
    }

    public boolean getAAjPageUp() {
	return get("PageUp");
    }

    public void setCtrl_AAjPageUp(boolean arg) {
        put("Ctrl-PageUp", arg);
    }

    public boolean getCtrl_AAjPageUp() {
	return get("Ctrl-PageUp");
    }

    public void setShift_AAjPageUp(boolean arg) {
        put("Shift-PageUp", arg);
    }

    public boolean getShift_AAjPageUp() {
	return get("Shift-PageUp");
    }

    public void setAAjPageDown(boolean arg) {
        put("PageDown", arg);
    }

    public boolean getAAjPageDown() {
	return get("PageDown");
    }

    public void setCtrl_AAjPageDown(boolean arg) {
        put("Ctrl-PageDown", arg);
    }

    public boolean getCtrl_AAjPageDown() {
	return get("Ctrl-PageDown");
    }

    public void setShift_AAjPageDown(boolean arg) {
        put("Shift-PageDown", arg);
    }

    public boolean getShift_AAjPageDown() {
	return get("Shift-PageDown");
    }
}
