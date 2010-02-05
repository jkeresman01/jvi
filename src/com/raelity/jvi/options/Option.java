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
package com.raelity.jvi.options;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;

public abstract class Option {
    protected String name;
    protected String displayName;
    protected String stringValue;
    protected String desc;
    protected String defaultValue;
    protected boolean fExpert;
    protected boolean fHidden;
    
    protected boolean fPropogate; // used in logic, not part of option type
    
    public Option(String key, String defaultValue) {
        this(key, defaultValue, true);
    }

    public Option(String key, String defaultValue, boolean doInit) {
	name = key;
	this.defaultValue = defaultValue;
	fExpert = false;
        fHidden = false;
        if(doInit)
            initialize();
    }

    protected void initialize() {
	fPropogate = false;
	String initialValue = OptUtil.getPrefs().get(name, defaultValue);
	setValue(initialValue);
	fPropogate = true;

    }

    public abstract void setValue(String value);

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
	return fExpert;
    }

    public boolean isHidden() {
	return fHidden;
    }
    
    public void setHidden(boolean f) {
        fHidden = f;
    }
    
    public void setExpert(boolean f) {
        fExpert = f;
    }

    public void setDesc(String desc)
    {
        if (this.desc != null) {
            throw new Error("option: " + name + " already has a description.");
        }
        this.desc = desc;
    }

    public void setDisplayName(String displayName)
    {
        if (this.displayName != null) {
            throw new Error("option: " + name + " already has a display name.");
        }
        this.displayName = displayName;
    }

    /**
     * The preferences data base has changed, stay in sync.
     * Do not propogate change back to data base.
     */
    void preferenceChange(String newValue) {
	fPropogate = false;
        try {
	    //System.err.println("preferenceChange " + name + ": " + newValue);
            setValue(newValue);
        } finally {
	    fPropogate = true;
        }
    }

    protected void propogate() {
	if(fPropogate) {
            OptUtil.getPrefs().put(name, stringValue);
	}
    }
    
    public int getInteger() {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not an IntegerOption");
    }
    
    public boolean getBoolean() {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a BooleanOption");
    }
    
    public String getString() {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a StringOption");
    }
    
    public Color getColor() {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a ColorOption");
    }
    
    public void validate(int val) throws PropertyVetoException {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not an IntegerOption");
    }
    
    public void validate(boolean val) throws PropertyVetoException {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a BooleanOption");
    }
    
    public void validate(String val) throws PropertyVetoException {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a StringOption");
    }
    
    public void validate(Color val) throws PropertyVetoException {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a ColorOption");
    }
    
    public void validate(Object val) throws PropertyVetoException {
        if(val instanceof String)
            validate((String)val);
        else if(val instanceof Color)
            validate((Color)val);
        else if(val instanceof Boolean)
            validate(((Boolean)val).booleanValue());
        else if(val instanceof Integer)
            validate(((Integer)val).intValue());
        else 
            throw new ClassCastException(val.getClass().getSimpleName()
                                    + " is not int, boolean, Color or String");
    }
    
    //////////////////////////////////////////////////////////////////////
    //
    // ColorOption
    //
    
    public static class ColorOption extends Option {
        private Validator validator;
        Color value;
        private boolean permitNull;
        
        public ColorOption(String key, Color defaultValue) {
            this(key, defaultValue, false, null);
        }
        
        public ColorOption(String key,
                           Color defaultValue,
                           boolean permitNull,
                           Validator validator) {
            super(key, xformToString(defaultValue), false); // don't initialize
            this.permitNull = permitNull;
            initialize(); // ok, initialize now that permitNull is set
            if(validator == null) {
                // The default validation accepts everything, checks permitNull
                validator = new Validator() {
                    @Override
                    public void validate(Color val) throws PropertyVetoException {
                        if(val == null && !ColorOption.this.permitNull)
                            throw new PropertyVetoException(
                                    "null color not permitted",
                                    new PropertyChangeEvent(opt, opt.getName(),
                                               opt.getColor(), val));

                    }
                };
            }
            this.validator = validator;
        }

        @Override
        public final Color getColor() {
            return value;
        }

        public boolean isPermitNull() {
            return permitNull;
        }

        public final Color decode(String s) {
            Color color;
            if(s.equals("")) {
               if(permitNull)
                   color = null;
               else
                   color = Color.decode(defaultValue);
            } else
                color = Color.decode(s);
            return color;
        }
        
        public static String xformToString(Color c) {
            if(c == null)
                return "";
            return String.format("0x%x", c.getRGB() & 0xffffff);
        }

        /**
        * Set the value of the parameter.
        */
        public void setColor(Color newValue) {
            Color oldValue = value;
            value = newValue;
            stringValue = xformToString(value);
            propogate();
            OptUtil.firePropertyChange(name, oldValue, newValue);
        }

        /**
        * Set the value as a string.
        */
        @Override
        public void setValue(String newValue) throws IllegalArgumentException {
            setColor(decode(newValue));
        }

        @Override
        public void validate(Color val) throws PropertyVetoException {
            validator.validate(val);
        }
        
        public static abstract class Validator {
            protected ColorOption opt;
            
            public abstract void validate(Color val) throws PropertyVetoException;
        }
    }
}

// vi: sw=4 et
