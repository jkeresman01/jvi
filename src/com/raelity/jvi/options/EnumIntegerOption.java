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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.Arrays;

public class EnumIntegerOption extends IntegerOption implements EnumOption {
    Integer [] availableValues;
  
  EnumIntegerOption(String key, int defaultValue, Integer[] availableValues) {
    this(key, defaultValue, null, availableValues);
  }
  
  public EnumIntegerOption(String key, int defaultValue,
          IntegerOption.Validator validator,
          Integer[] availableValues) {
    super(key, defaultValue, getValidator(validator));
    this.availableValues = availableValues;
  }

    private static IntegerOption.Validator
    getValidator(IntegerOption.Validator validator) {
        return validator != null ? validator : new DefaultEnumIntegerValidator();
    }

    @Override
    public Integer[] getAvailableValues() {
        // NEEDSWORK: return unModifiable list to avoid copy
        return Arrays.copyOf(availableValues, availableValues.length);
    }

    public static class DefaultEnumIntegerValidator
    extends IntegerOption.Validator {
        @Override
        public void validate(int val) throws PropertyVetoException
        {
            EnumIntegerOption eOpt = (EnumIntegerOption)this.opt;
            for(Integer eVal : eOpt.availableValues) {
                if(eVal.equals(val))
                    return;
            }
            throw new PropertyVetoException(
                    "Invalid option value: " + val,
                    new PropertyChangeEvent(opt, opt.getName(),
                            opt.getInteger(), val));
        }

    }
}
