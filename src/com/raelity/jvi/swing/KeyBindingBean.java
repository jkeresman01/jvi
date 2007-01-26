/*
 * KeyBindingBean.java
 *
 * Created on January 17, 2007, 6:02 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
 *
 * @author erra
 */
public class KeyBindingBean  extends SimpleBeanInfo {
    
    //
    // The BeanInfo is embedded in the same class
    //
    public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(KeyBindingBean.class) {
            public String getDisplayName() {
                return "Control Key Bindings";
            }
        };
    }
    
    public PropertyDescriptor[] getPropertyDescriptors() {
	Vector<PropertyDescriptor> vD = new Vector<PropertyDescriptor>();
	int i = 0;

	for(char c = 'A'; c <= 'Z'; c++) {
            PropertyDescriptor d;
            String displayName = "Ctrl_" + new String(new char[] {c});
            String shortDescription =
                      "Checked (enabled) means that jVi will process"
                    + " this key. If clear (disabled) then the key"
                    + " is available for other keybindings."
                    + " MUST RESTART NetBeans FOR CHANGES TO TAKE AFFECT."
                    + " (For now.)";
            try {
                d = new PropertyDescriptor(displayName, KeyBindingBean.class);
            } catch (IntrospectionException ex) {
                ex.printStackTrace();
                continue;
            }
	    d.setDisplayName(displayName);
	    d.setExpert(false);
	    d.setShortDescription(shortDescription);
	    vD.add(d);
	}
        
	PropertyDescriptor[] descriptors = new PropertyDescriptor[vD.size()];
        vD.toArray(descriptors);
	return descriptors;
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


    public void setCtrl_A(boolean arg) {
        put("Ctrl-A", arg);
    }

    public boolean getCtrl_A() {
	return get("Ctrl-A");
    }

    public void setCtrl_B(boolean arg) {
        put("Ctrl-B", arg);
    }

    public boolean getCtrl_B() {
	return get("Ctrl-B");
    }

    public void setCtrl_C(boolean arg) {
        put("Ctrl-C", arg);
    }

    public boolean getCtrl_C() {
	return get("Ctrl-C");
    }

    public void setCtrl_D(boolean arg) {
        put("Ctrl-D", arg);
    }

    public boolean getCtrl_D() {
	return get("Ctrl-D");
    }

    public void setCtrl_E(boolean arg) {
        put("Ctrl-E", arg);
    }

    public boolean getCtrl_E() {
	return get("Ctrl-E");
    }

    public void setCtrl_F(boolean arg) {
        put("Ctrl-F", arg);
    }

    public boolean getCtrl_F() {
	return get("Ctrl-F");
    }

    public void setCtrl_G(boolean arg) {
        put("Ctrl-G", arg);
    }

    public boolean getCtrl_G() {
	return get("Ctrl-G");
    }

    public void setCtrl_H(boolean arg) {
        put("Ctrl-H", arg);
    }

    public boolean getCtrl_H() {
	return get("Ctrl-H");
    }

    public void setCtrl_I(boolean arg) {
        put("Ctrl-I", arg);
    }

    public boolean getCtrl_I() {
	return get("Ctrl-I");
    }

    public void setCtrl_J(boolean arg) {
        put("Ctrl-J", arg);
    }

    public boolean getCtrl_J() {
	return get("Ctrl-J");
    }

    public void setCtrl_K(boolean arg) {
        put("Ctrl-K", arg);
    }

    public boolean getCtrl_K() {
	return get("Ctrl-K");
    }

    public void setCtrl_L(boolean arg) {
        put("Ctrl-L", arg);
    }

    public boolean getCtrl_L() {
	return get("Ctrl-L");
    }

    public void setCtrl_M(boolean arg) {
        put("Ctrl-M", arg);
    }

    public boolean getCtrl_M() {
	return get("Ctrl-M");
    }

    public void setCtrl_N(boolean arg) {
        put("Ctrl-N", arg);
    }

    public boolean getCtrl_N() {
	return get("Ctrl-N");
    }

    public void setCtrl_O(boolean arg) {
        put("Ctrl-O", arg);
    }

    public boolean getCtrl_O() {
	return get("Ctrl-O");
    }

    public void setCtrl_P(boolean arg) {
        put("Ctrl-P", arg);
    }

    public boolean getCtrl_P() {
	return get("Ctrl-P");
    }

    public void setCtrl_Q(boolean arg) {
        put("Ctrl-Q", arg);
    }

    public boolean getCtrl_Q() {
	return get("Ctrl-Q");
    }

    public void setCtrl_R(boolean arg) {
        put("Ctrl-R", arg);
    }

    public boolean getCtrl_R() {
	return get("Ctrl-R");
    }

    public void setCtrl_S(boolean arg) {
        put("Ctrl-S", arg);
    }

    public boolean getCtrl_S() {
	return get("Ctrl-S");
    }

    public void setCtrl_T(boolean arg) {
        put("Ctrl-T", arg);
    }

    public boolean getCtrl_T() {
	return get("Ctrl-T");
    }

    public void setCtrl_U(boolean arg) {
        put("Ctrl-U", arg);
    }

    public boolean getCtrl_U() {
	return get("Ctrl-U");
    }

    public void setCtrl_V(boolean arg) {
        put("Ctrl-V", arg);
    }

    public boolean getCtrl_V() {
	return get("Ctrl-V");
    }

    public void setCtrl_W(boolean arg) {
        put("Ctrl-W", arg);
    }

    public boolean getCtrl_W() {
	return get("Ctrl-W");
    }

    public void setCtrl_X(boolean arg) {
        put("Ctrl-X", arg);
    }

    public boolean getCtrl_X() {
	return get("Ctrl-X");
    }

    public void setCtrl_Y(boolean arg) {
        put("Ctrl-Y", arg);
    }

    public boolean getCtrl_Y() {
	return get("Ctrl-Y");
    }

    public void setCtrl_Z(boolean arg) {
        put("Ctrl-Z", arg);
    }

    public boolean getCtrl_Z() {
	return get("Ctrl-Z");
    }
}
