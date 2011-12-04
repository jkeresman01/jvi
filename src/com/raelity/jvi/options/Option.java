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

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import com.raelity.jvi.core.Options.Category;

public abstract class Option<T> {
    final Class<T> optionType;
    final protected String name;
    final protected T defaultValue;
    final private Validator<T> validator;
    private T value;

    private String displayName;
    private String desc;
    private boolean fExpert;
    private boolean fHidden;
    private Category category;
    
    private boolean fPropogate; // used in logic, not part of option type

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

        initialize(); // overridableMethodCallInConstructor
    }

    void initialize() {
        preferenceChange(OptUtil.getPrefs().get(name, getValueAsString(defaultValue)));
    }

    final public T getValue() {
        return value;
    }

    /** If overridden, should invoke super.setValue */
    void setValue(T newValue)
    {
        T oldValue = value;
        value = newValue;
        propogate();
        OptUtil.firePropertyChange(name, oldValue, newValue);
    }

    private void propogate() {
	if(fPropogate) {
            OptUtil.getPrefs().put(name, getValueAsString(value));
	}
        OptUtil.intializeGlobalOptionMemoryValue(this);
    }

    /**
     * SetValue from String without propagating.
     * The preferences data base has changed, stay in sync.
     * Do not propagate change back to data base.
     * <p/>
     * This is invoked by a preferences change listener
     * as well as the initialize method.
     */
    final void preferenceChange(String newValue) {
	fPropogate = false;
        try {
	    //System.err.println("preferenceChange " + name + ": " + newValue);
            setValueFromString(newValue);
        } finally {
	    fPropogate = true;
        }
    }

    private void setValueFromString(String sVal)
    {
        setValue(getValueFromString(sVal));
    }

    String getValueAsString(T val)
    {
        return val.toString();
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

    //////////////////////////////////////////////////////////////////////

    private boolean isBaseType()
    {
        return optionType == Integer.class
                || optionType == Boolean.class
                || optionType == String.class;
    }
    
    public Integer getInteger() {
        // if(optionType != Integer.class)
        //     throw new ClassCastException(this.getClass().getSimpleName()
        //                                  + " is not an IntegerOption");
        return (Integer)value;
    }

    // NEEDSWORK: should be final, except for DebugOption
    public Boolean getBoolean() {
        // if(optionType != Boolean.class)
        //     throw new ClassCastException(this.getClass().getSimpleName()
        //                                  + " is not a BooleanOption");
        return (Boolean)value;
    }
    
    public String getString() {
        // if(optionType != String.class)
        //     throw new ClassCastException(this.getClass().getSimpleName()
        //                                  + " is not a StringOption");
        return (String)value;
    }
    
    public Color getColor() {
        // if(optionType != Color.class)
        //     throw new ClassCastException(this.getClass().getSimpleName()
        //                                  + " is not a ColorOption");
        return (Color)value;
    }

    final public void validate(Object o) throws PropertyVetoException {

        T val;
        try {
            val = optionType.cast(o);
        } catch(ClassCastException ex) {
            throw new PropertyVetoException(
                      "expected type "+ optionType.getSimpleName()
                        + " not " + o.getClass().getSimpleName() + ": " + o,
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

    final public String getName() {
	return name;
    }

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

    final public void setCategory(Category category)
    {
        if (this.category != null) {
            throw new Error("option: " + name + " already has a category.");
        }
        this.category = category;
    }
}

// vi: sw=4 et
