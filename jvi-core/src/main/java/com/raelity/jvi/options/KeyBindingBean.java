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
 * KeyBindingBean.java
 *
 * Created on January 17, 2007, 6:02 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi.options;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Use this class to programmatically enable/disable jVi catching particular
 * control keys or to determine if a particular key is being caught.
 *
 * @author erra
 */
public class KeyBindingBean  extends KeyOptionsBeanBase {
    private static final
            Logger LOG = Logger.getLogger(KeyBindingBean.class.getName());
    
    //
    // The BeanInfo is embedded in the same class
    //
    @Override
    public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(KeyBindingBean.class) {
            @Override
            public String getDisplayName() {
                return "Control-Key Bindings";
            }
        };
    }
    
    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        final List<PropertyDescriptor> vD = new ArrayList<>();
        
	for(char c = 'A'; c <= 'Z'; c++) {
            String keyChar = String.valueOf(c);
            String propertyName = "Ctrl_" + keyChar;
            String displayName = "Ctrl-" + keyChar;
            addDesc(vD, propertyName, displayName);
        }
        addDesc(vD, "AaShiftSpace", "Shift-Space");
        addDesc(vD, "AaCtrlSpace", "Ctrl-Space");
        addDesc(vD, "BaOpenBracket", "Ctrl-[    the \"real\" <ESC>");
        addDesc(vD, "BbCloseBracket", "Ctrl-]");
        addDesc(vD, "CcCommaOpenAngle", "Ctrl-< or Ctrl-,");
        addDesc(vD, "CdPeriodCloseAngle", "Ctrl-> or Ctrl-.");
        addDesc(vD, "DeCtrl_AT", "Ctrl-@");
        
	return vD.toArray(PropertyDescriptor[]::new);
    }
    
    private void addDesc(List<PropertyDescriptor> vD,
                         String propertyName, String displayName)
    {
        PropertyDescriptor d;
        try {
            d = new PropertyDescriptor(propertyName, KeyBindingBean.class);
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

    public void setAaShiftSpace(boolean arg) {
        put("Shift-Space", arg);
    }

    public boolean getAaShiftSpace() {
	return get("Shift-Space");
    }

    public void setAaCtrlSpace(boolean arg) {
        put("Ctrl-Space", arg);
    }

    public boolean getAaCtrlSpace() {
	return get("Ctrl-Space");
    }

    public void setBaOpenBracket(boolean arg) {
        put("Ctrl-[", arg);
    }

    public boolean getBaOpenBracket() {
	return get("Ctrl-[");
    }

    public void setBbCloseBracket(boolean arg) {
        put("Ctrl-]", arg);
    }

    public boolean getBbCloseBracket() {
	return get("Ctrl-]");
    }

    public void setCcCommaOpenAngle(boolean arg) {
        put("Ctrl-<", arg);
    }

    public boolean getCcCommaOpenAngle() {
	return get("Ctrl-<");
    }

    public void setCdPeriodCloseAngle(boolean arg) {
        put("Ctrl->", arg);
    }

    public boolean getCdPeriodCloseAngle() {
	return get("Ctrl->");
    }

    public void setDeCtrl_AT(boolean arg) {
        put("Ctrl-@", arg);
    }

    public boolean getDeCtrl_AT() {
	return get("Ctrl-@");
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
