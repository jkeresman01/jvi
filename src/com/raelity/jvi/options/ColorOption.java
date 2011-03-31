package com.raelity.jvi.options;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;

public class ColorOption extends Option
{

    private Validator validator;
    Color value;
    private boolean permitNull;

    public ColorOption(String key, Color defaultValue)
    {
        this(key, defaultValue, false, null);
    }

    public ColorOption(String key, Color defaultValue, boolean permitNull,
                       Validator validator)
    {
        super(key, xformToString(defaultValue), false); // don't initialize
        this.permitNull = permitNull;
        initialize(); // ok, initialize now that permitNull is set
        if (validator == null) {
            // The default validation accepts everything, checks permitNull
            validator = new Validator() {

                @Override
                public void validate(Color val) throws PropertyVetoException
                {
                    if (val == null && !ColorOption.this.permitNull) {
                        throw new PropertyVetoException(
                                "null color not permitted",
                                new PropertyChangeEvent(opt,
                                                        opt.getName(),
                                                        opt.getColor(),
                                                        val));
                    }
                }
            };
        }
        linkUpValidator(validator);
    }

    private void linkUpValidator(Validator validator)
    {
        assert validator.opt == null;
        validator.opt = this;
        this.validator = validator;
    }

    @Override
    public final Color getColor()
    {
        return value;
    }

    public boolean isPermitNull()
    {
        return permitNull;
    }

    public final Color decode(String s)
    {
        Color color;
        if (s.isEmpty()) {
            if (permitNull) {
                color = null;
            } else {
                color = Color.decode(defaultValue);
            }
        } else {
            color = Color.decode(s);
        }
        return color;
    }

    public static String xformToString(Color c)
    {
        if (c == null) {
            return "";
        }
        return String.format("0x%x", c.getRGB() & 16777215);
    }

    /**
     * Set the value of the parameter.
     */
    void setColor(Color newValue)
    {
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
    void setValue(String newValue) throws IllegalArgumentException
    {
        setColor(decode(newValue));
    }

    @Override
    public void validate(Color val) throws PropertyVetoException
    {
        validator.validate(val);
    }

    public static abstract class Validator
    {

        protected ColorOption opt;

        public abstract void validate(Color val) throws PropertyVetoException;
    }
}
