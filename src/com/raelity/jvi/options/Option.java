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
 * Copyright (C) 2011 Ernie Rael.  All Rights Reserved.
 * 
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.options;

import com.raelity.jvi.core.Options.Category;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public abstract class Option<T> {
    final Class<T> optionType;
    final protected String name;
    final protected T defaultValue;
    final private Validator<T> validator;
    protected T value;

    protected String displayName;
    protected String desc;
    protected boolean fExpert;
    protected boolean fHidden;
    Category category;
    
    protected boolean fPropogate; // used in logic, not part of option type

    // NOTE: can not deduce optionType from defaultValue since
    //       color allows null.

    @SuppressWarnings({"LeakingThisInConstructor", "OverridableMethodCallInConstructor"})
    /*package*/ Option(Class<T> optionType, String key, T defaultValue,
                          Validator<T> validator) {
        this.optionType = optionType;
	name = key;
	this.defaultValue = defaultValue;
        this.validator = validator;
        assert validator.opt == null;
        validator.opt = this;

	fExpert = false;
        fHidden = false;

        initialize();
    }

    void initialize() {
        preferenceChange(OptUtil.getPrefs().get(name, getValueAsString(defaultValue)));
    }

    // public String getValue() {
    //     return stringValue;
    // }

    final public T getValue() {
        return value;
    }

    final public String getName() {
	return name;
    }
    
    // NEEDSWORK: MAKE IT NOT PUBLIC
    final public T getDefault() {
	return defaultValue;
    }
    
    final public String getDesc() {
	return desc;
    }

    final public String getDisplayName() {
	if(displayName != null) {
	    return displayName;
	} else {
	    return name;
	}
    }

    final public boolean isExpert() {
	return fExpert;
    }

    final public boolean isHidden() {
	return fHidden;
    }
    
    final public void setHidden(boolean f) {
        fHidden = f;
    }
    
    final public void setExpert(boolean f) {
        fExpert = f;
    }

    final public Category getCategory()
    {
        return category;
    }

    final public void setDesc(String desc)
    {
        if (this.desc != null) {
            throw new Error("option: " + name + " already has a description.");
        }
        this.desc = desc;
    }

    final public void setDisplayName(String displayName)
    {
        if (this.displayName != null) {
            throw new Error("option: " + name + " already has a display name.");
        }
        this.displayName = displayName;
    }

    void setValue(T newValue)
    {
        T oldValue = value;
        value = newValue;
        propogate();
        OptUtil.firePropertyChange(name, oldValue, newValue);
    }

    /**
     * SetValue from String without propogating.
     * The preferences data base has changed, stay in sync.
     * Do not propogate change back to data base.
     */
    void preferenceChange(String newValue) {
	fPropogate = false;
        try {
	    //System.err.println("preferenceChange " + name + ": " + newValue);
            setValueFromString(newValue);
        } finally {
	    fPropogate = true;
        }
    }

    String getValueAsString(T val)
    {
        return val.toString();
    }

    final void setValueFromString(String sVal)
    {
        setValue(getValueFromString(sVal));
    }

    T getValueFromString(String sVal)
    {
        Preferences prefs = OptUtil.getPrefs();
        prefs.get("foo", "bar");
        assert isBaseType();
        Object o = sVal;
        if(optionType == Integer.class) {
            try {
                o = Integer.parseInt(sVal);
            } catch(NumberFormatException ex) {
                // NEEDSWORK: log sever
                Logger.getLogger(Option.class.getName()).log(
                        Level.SEVERE, null, ex);
                o = 0;
            }
        } else if(optionType == Boolean.class)
            o = Boolean.parseBoolean(sVal);

        return optionType.cast(o);
    }

    private boolean isBaseType()
    {
        return optionType == Integer.class
                || optionType == Boolean.class
                || optionType == String.class;
    }

    void propogate() {
	if(fPropogate) {
            OptUtil.getPrefs().put(name, getValueAsString(value));
	}
        OptUtil.intializeGlobalOptionMemoryValue(this);
    }
    
    final public Integer getInteger() {
        if(optionType != Integer.class)
            throw new ClassCastException(this.getClass().getSimpleName()
                                         + " is not an IntegerOption");
        return (Integer)value;
    }

    // NEEDSWORK: should be final, except for DebugOption
    public Boolean getBoolean() {
        if(optionType != Boolean.class)
            throw new ClassCastException(this.getClass().getSimpleName()
                                         + " is not a BooleanOption");
        return (Boolean)value;
    }
    
    final public String getString() {
        if(optionType != String.class)
            throw new ClassCastException(this.getClass().getSimpleName()
                                         + " is not a StringOption");
        return (String)value;
    }
    
    final public Color getColor() {
        if(optionType != Color.class)
            throw new ClassCastException(this.getClass().getSimpleName()
                                         + " is not a ColorOption");
        return (Color)value;
    }

    final public void validate(Object o) throws PropertyVetoException {

        T val;
        try {
            val = optionType.cast(o);
        } catch(ClassCastException ex) {
            throw new PropertyVetoException(
                      "expected type "+ optionType.getSimpleName() + ": " + o,
                      new PropertyChangeEvent(this, this.getName(),
                                          this.getValue(), o));
        }
        validator.validate(val);
    }

    /** default validator ensures that val greaterthan or equal to zero. */
    static class DefaultIntegerValidator extends Validator<Integer>
    {
        @Override
        public void validate(Integer val) throws PropertyVetoException
        {
            if(val == null || val < 0) {
                reportPropertyVetoException(
                        "Value must be >= 0: " + val, val);
          }
        }
    }

    static class DefaultStringValidator extends Validator<String>
    {
        @Override
        public void validate(String val) throws PropertyVetoException
        {
            if(val == null) {
                reportPropertyVetoException(
                        "null is not a valid string option" , val);
          }
        }
    }

    static class DefaultBooleanValidator extends Validator<Boolean>
    {
        // The default validation accepts everything
        @Override
        public void validate(Boolean val) throws PropertyVetoException
        {
        }

    }
}

// vi: sw=4 et
