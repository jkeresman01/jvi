/*
 * KeypadBindingBean.java
 *
 * Created on February 12, 2007, 4:47 PM
 */

package com.raelity.jvi.swing;

import com.raelity.jvi.ViManager;
import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.Vector;
import java.util.prefs.Preferences;

/**
 * Use thsi class to programatically enable/disable jVi catching particular
 * keypad keys, or to determine if a particular key is being caught.
 *
 * @author erra
 */
public class KeypadBindingBean extends SimpleBeanInfo {
    //
    // The BeanInfo is embedded in the same class
    //
    public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(KeyBindingBean.class) {
            public String getDisplayName() {
                return "Keypad Bindings";
            }
        };
    }
    
    Vector<PropertyDescriptor> vD;
    public PropertyDescriptor[] getPropertyDescriptors() {
        vD = new Vector<PropertyDescriptor>();
        for (String key : KeyBinding.getKeypadNames()) {
            addDesc(key, "");
            addDesc(key, "Ctrl");
            addDesc(key, "Shift");
        }
        
	PropertyDescriptor[] descriptors = new PropertyDescriptor[vD.size()];
        vD.toArray(descriptors);
        vD = null;
	return descriptors;
    }
    private static final String shortDescription =
                "Checked (enabled) means that jVi will process"
            + " this key. If clear (disabled) then the key"
            + " is available for other keybindings."
            + " MUST RESTART NetBeans FOR CHANGES TO TAKE AFFECT."
            + " (For now.)";
    
    private void addDesc(String key, String mod) {
	int i = 0;

        PropertyDescriptor d;
        String displayName = mod.isEmpty() ? key : mod + "-" + key;
        // want the arrow keys grouped together so change their name,
        // use the "Ars" to insure that unmodified keys are grouped together
        // but leave the display name together.
        if(key.equals("Up") || key.equals("Down")
           || key.equals("Left") || key.equals("Right"))
            key = "Aa" + key;
        else
            key = "Ab" + key;
        String propertyName = mod.isEmpty() ? key : mod + "_" + key;
        try {
            d = new PropertyDescriptor(propertyName, KeypadBindingBean.class);
        } catch (IntrospectionException ex) {
            ex.printStackTrace();
            return;
        }
        d.setDisplayName(displayName);
        d.setExpert(false);
        d.setShortDescription(shortDescription);
        vD.add(d);
    }
    
    //
    // The bean
    //      The bean getter/setter, interface to preferences.
    //
    private Preferences prefs = ViManager.getViFactory()
                                .getPreferences().node(KeyBinding.PREF_KEYS);
    
    private void put(String name, boolean val) {
        prefs.put(name, "" + val);
    }
    
    private boolean get(String name) {
        return prefs.getBoolean(name, KeyBinding.getCatchKeyDefault(name));
    }

    public void setAaUp(boolean arg) {
        put("Up", arg);
    }

    public boolean getAaUp() {
	return get("Up");
    }

    public void setCtrl_AaUp(boolean arg) {
        put("Ctrl-Up", arg);
    }

    public boolean getCtrl_AaUp() {
	return get("Ctrl-Up");
    }

    public void setShift_AaUp(boolean arg) {
        put("Shift-Up", arg);
    }

    public boolean getShift_AaUp() {
	return get("Shift-Up");
    }

    public void setAaDown(boolean arg) {
        put("Down", arg);
    }

    public boolean getAaDown() {
	return get("Down");
    }

    public void setCtrl_AaDown(boolean arg) {
        put("Ctrl-Down", arg);
    }

    public boolean getCtrl_AaDown() {
	return get("Ctrl-Down");
    }

    public void setShift_AaDown(boolean arg) {
        put("Shift-Down", arg);
    }

    public boolean getShift_AaDown() {
	return get("Shift-Down");
    }

    public void setAaLeft(boolean arg) {
        put("Left", arg);
    }

    public boolean getAaLeft() {
	return get("Left");
    }

    public void setCtrl_AaLeft(boolean arg) {
        put("Ctrl-Left", arg);
    }

    public boolean getCtrl_AaLeft() {
	return get("Ctrl-Left");
    }

    public void setShift_AaLeft(boolean arg) {
        put("Shift-Left", arg);
    }

    public boolean getShift_AaLeft() {
	return get("Shift-Left");
    }

    public void setAaRight(boolean arg) {
        put("Right", arg);
    }

    public boolean getAaRight() {
	return get("Right");
    }

    public void setCtrl_AaRight(boolean arg) {
        put("Ctrl-Right", arg);
    }

    public boolean getCtrl_AaRight() {
	return get("Ctrl-Right");
    }

    public void setShift_AaRight(boolean arg) {
        put("Shift-Right", arg);
    }

    public boolean getShift_AaRight() {
	return get("Shift-Right");
    }

    public void setAbInsert(boolean arg) {
        put("Insert", arg);
    }

    public boolean getAbInsert() {
	return get("Insert");
    }

    public void setCtrl_AbInsert(boolean arg) {
        put("Ctrl-Insert", arg);
    }

    public boolean getCtrl_AbInsert() {
	return get("Ctrl-Insert");
    }

    public void setShift_AbInsert(boolean arg) {
        put("Shift-Insert", arg);
    }

    public boolean getShift_AbInsert() {
	return get("Shift-Insert");
    }

    public void setAbDelete(boolean arg) {
        put("Delete", arg);
    }

    public boolean getAbDelete() {
	return get("Delete");
    }

    public void setCtrl_AbDelete(boolean arg) {
        put("Ctrl-Delete", arg);
    }

    public boolean getCtrl_AbDelete() {
	return get("Ctrl-Delete");
    }

    public void setShift_AbDelete(boolean arg) {
        put("Shift-Delete", arg);
    }

    public boolean getShift_AbDelete() {
	return get("Shift-Delete");
    }

    public void setAbHome(boolean arg) {
        put("Home", arg);
    }

    public boolean getAbHome() {
	return get("Home");
    }

    public void setCtrl_AbHome(boolean arg) {
        put("Ctrl-Home", arg);
    }

    public boolean getCtrl_AbHome() {
	return get("Ctrl-Home");
    }

    public void setShift_AbHome(boolean arg) {
        put("Shift-Home", arg);
    }

    public boolean getShift_AbHome() {
	return get("Shift-Home");
    }

    public void setAbEnd(boolean arg) {
        put("End", arg);
    }

    public boolean getAbEnd() {
	return get("End");
    }

    public void setCtrl_AbEnd(boolean arg) {
        put("Ctrl-End", arg);
    }

    public boolean getCtrl_AbEnd() {
	return get("Ctrl-End");
    }

    public void setShift_AbEnd(boolean arg) {
        put("Shift-End", arg);
    }

    public boolean getShift_AbEnd() {
	return get("Shift-End");
    }

    public void setAbUndo(boolean arg) {
        put("Undo", arg);
    }

    public boolean getAbUndo() {
	return get("Undo");
    }

    public void setCtrl_AbUndo(boolean arg) {
        put("Ctrl-Undo", arg);
    }

    public boolean getCtrl_AbUndo() {
	return get("Ctrl-Undo");
    }

    public void setShift_AbUndo(boolean arg) {
        put("Shift-Undo", arg);
    }

    public boolean getShift_AbUndo() {
	return get("Shift-Undo");
    }

    public void setAbPageUp(boolean arg) {
        put("PageUp", arg);
    }

    public boolean getAbPageUp() {
	return get("PageUp");
    }

    public void setCtrl_AbPageUp(boolean arg) {
        put("Ctrl-PageUp", arg);
    }

    public boolean getCtrl_AbPageUp() {
	return get("Ctrl-PageUp");
    }

    public void setShift_AbPageUp(boolean arg) {
        put("Shift-PageUp", arg);
    }

    public boolean getShift_AbPageUp() {
	return get("Shift-PageUp");
    }

    public void setAbPageDown(boolean arg) {
        put("PageDown", arg);
    }

    public boolean getAbPageDown() {
	return get("PageDown");
    }

    public void setCtrl_AbPageDown(boolean arg) {
        put("Ctrl-PageDown", arg);
    }

    public boolean getCtrl_AbPageDown() {
	return get("Ctrl-PageDown");
    }

    public void setShift_AbPageDown(boolean arg) {
        put("Shift-PageDown", arg);
    }

    public boolean getShift_AbPageDown() {
	return get("Shift-PageDown");
    }
}
