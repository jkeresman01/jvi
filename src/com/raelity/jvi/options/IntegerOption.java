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

import com.raelity.jvi.core.Options;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;

public class IntegerOption extends Option {
  private Validator validator;
  protected int value;
  
  public IntegerOption(String key, int defaultValue) {
    this(key, defaultValue, null);
  }
  
  public IntegerOption(String key, int defaultValue, Validator validator) {
    super(key, "" + defaultValue);
    if(validator == null) {
      // The default validation is that the value must be >= zero.
      validator = new Validator() {
                @Override
        public void validate(int val) throws PropertyVetoException {
          if(val < 0) {
            throw new PropertyVetoException(
                        "Value must be positive: " + val,
                        new PropertyChangeEvent(opt, opt.getName(),
                                                opt.getInteger(), val));
          }
        }
      };
    }
    validator.opt = this;
    this.validator = validator;
  }

    @Override
  public final int getInteger() {
    return value;
  }

  /**
   * Set the value of the parameter.
   * @return true if value actually changed.
   */
  public void setInteger(int newValue) {
    int oldValue = value;
    value = newValue;
    stringValue = "" + value;
    propogate();
    OptUtil.firePropertyChange(name, oldValue, newValue);
  }

  /**
   * Set the value as a string.
   */
    @Override
  public void setValue(String newValue) throws IllegalArgumentException {
    int n = Integer.parseInt(newValue);
    setInteger(n);
  }
  
  /**
   * Validate the setting value.
   */
    @Override
  public void validate(int val) throws PropertyVetoException {
    validator.validate(val);
  }
  
  public static abstract class Validator {
    protected IntegerOption opt;
    
    public abstract void validate(int val) throws PropertyVetoException;
  }
}
