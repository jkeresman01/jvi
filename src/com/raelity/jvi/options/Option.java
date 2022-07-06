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
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import com.raelity.jvi.core.*;
import com.raelity.jvi.core.Options.Category;

import static java.util.logging.Level.FINE;

import static com.raelity.jvi.manager.ViManager.getFactory;
import static com.raelity.text.TextUtil.sf;

//
// NEEDSWORK: If returning a reference, should be readonly
//

/**
 * Base class for a jVi option. Currently supported, include gui display, types
 * are int, boolean, string, color, enumSet, enumString. An option may
 * have a validator attached which can throw PropertyVetoException.
 * An option has additional information, desc, display name, ...
 * See {@link Options}, {@link OptUtil}.
 * <p>
 * Note that this method has concrete implementations of get for each type,
 * they produce class cast exception. Typically use:
 * <pre>
 * {@code 
 * Option<?> opt = Options.getOption(name);
 * opt.getColor(); // cast exception if not a color
 * }
 * </pre>
 * 
 * @author err
 * @param <T> option type
 */
//
// Sealed class to limit who can extend?
//
public abstract class Option<T> {
    final Class<T> optionType;
    final protected String name;
    final protected T defaultValue;
    final protected Validator<T> validator;
    private T value;

    private String displayName;
    private String desc;
    private String descPlatform;
    private boolean fExpert;
    private boolean fHidden;
    private Category category;
    
    private boolean fPropogateToPref; // used in logic, not part of option type

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

    synchronized final public T getValue() {
        return value;
    }

    /** If overridden, should invoke super.setValue */
    void setValue(T newValue)
    {
        syncSetValue(newValue);
        OptionEvent.qRun();
    }

    private synchronized void syncSetValue(T newValue)
    {
        G.dbgOptions().println(FINE, () -> sf("setValue: " + name));
        T oldValue = value;
        if(Objects.equals(newValue, oldValue)) {
            G.dbgOptions().println(FINE, () -> sf("Option setValue: no change"));
            return;
        }
        value = newValue;
        OptionEvent.qPut(new OptionEvent.Option(name, oldValue, newValue));
        propogate();
    }

    // NOTE: the synchro lock should be held before invoking this method.
    private void propogate() {
        OptionEvent.qPut(OptUtil.intializeGlobalOptionMemoryValue(this));
	if(fPropogateToPref) {
            String sval = getValueAsString(value); // capture value
            OptionEvent.qPut(() -> OptUtil.getPrefs().put(name, sval));
        }
    }

    /**
     * Cast and setValue; used when property is set from dialog.
     * When the pref change fires, the object should be equal,
     * so nothing happens.
     * @throws ClassCastException
     */

    final void castSetValue(Object val)
    {
        setValue(optionType.cast(val));
    }

    /**
     * SetValue from String without propagating.
     * The preferences data base has changed, stay in sync.
     * Do not propagate change back to preferences data base.
     * <p/>
     * This is invoked by a preferences change listener (OptUtil)
     * as well as the initialize method.
     */
    final void preferenceChange(String newValue) {
        syncPreferenceChange(newValue);
    }

    synchronized final void syncPreferenceChange(String newValue) {
	fPropogateToPref = false;
        try {
            setValue(getValueFromString(newValue));
        } finally {
	    fPropogateToPref = true;
        }
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

    public boolean isNull()
    {
        return value == null;
    }

    public boolean isDefault()
    {
        if(value == null)
            return defaultValue == null;
        return value.equals(defaultValue);
    }

    //////////////////////////////////////////////////////////////////////

    private boolean isBaseType()
    {
        return optionType == Integer.class
                || optionType == Boolean.class
                || optionType == String.class;
    }
    
    public Integer getInteger() {
        return (Integer)value;
    }

    // NEEDSWORK: should be final, except for DebugOption
    // sealed?
    public Boolean getBoolean() {
        return (Boolean)value;
    }
    
    public String getString() {
        return (String)value;
    }

    public Color getColor() {
        return (Color)value;
    }
    
    public EnumSet<?> getEnumSet() {
        return (EnumSet)value;
    }

    // allow override because EnumSet allows validation of string value
    public void validate(Object o) throws PropertyVetoException {

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
        getFactory().validate(name, val);
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

    static class NullValidator<T> extends Validator<T>
    {
        // The default validation accepts everything
        @Override
        public void validate(T val) throws PropertyVetoException
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
        if(descPlatform == null)
            return desc;
        return desc + "\n\n" + descPlatform;
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

    final public void setDescPlatform(String descPlatform)
    {
        if (this.descPlatform != null) {
            throw new Error("option: " + name + " already has a description.");
        }
        this.descPlatform = descPlatform;
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

    @Override
    public String toString()
    {
        return name + ":" + value;
    }

}

// vi: sw=4 et
