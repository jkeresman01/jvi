/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */

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
package com.raelity.jvi;

public abstract class Option {
    protected String name;
    protected String displayName;
    protected String stringValue;
    protected String desc;
    protected String defaultValue;
    protected boolean fPropogate;
    protected boolean isExpert;
    
    public Option(String key, String defaultValue) {
	name = key;
	this.defaultValue = defaultValue;
	String initialValue = Options.getPrefs().get(key, defaultValue);
	isExpert = false;

	fPropogate = false;
	setValue(initialValue);
	fPropogate = true;
    }

    public abstract boolean setValue(String value);

    public String getValue() {
	return stringValue;
    }

    public String getName() {
	return name;
    }
    
    public String getDefault() {
	return defaultValue;
    }
    
    public String getDesc() {
	return desc;
    }

    public String getDisplayName() {
	if(displayName != null) {
	    return displayName;
	} else {
	    return name;
	}
    }

    public boolean isExpert() {
	return isExpert;
    }

    /**
     * The preferences data base has changed, stay in sync.
     * Do not propogate change back to data base.
     */
    void preferenceChange(String newValue) {
	fPropogate = false;
        System.err.println("preferenceChange " + name + ": " + newValue);
	setValue(newValue);
	fPropogate = true;
    }

    protected void propogate() {
	if(fPropogate) {
	    Options.prefs.put(name, stringValue);
	}
    }
}
