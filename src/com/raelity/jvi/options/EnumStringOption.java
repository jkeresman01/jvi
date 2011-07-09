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

public class EnumStringOption extends StringOption implements EnumOption {
    String [] availableValues;

    public EnumStringOption(String key, String defaultValue,
                            String[] availableValues) {
        this(key, defaultValue, null, availableValues);
    }

    public EnumStringOption(String key, String defaultValue,
                            StringOption.Validator validator,
                            String[] availableValues) {
        super(key, defaultValue, getValidator(validator));
        this.availableValues = availableValues;
    }

    private static StringOption.Validator
    getValidator(StringOption.Validator validator) {
        return validator != null ? validator : new DefaultEnumStringValidator();
    }

    @Override
    public String[] getAvailableValues() {
        // NEEDSWORK: return unModifiable list to avoid copy
        return Arrays.copyOf(availableValues, availableValues.length);
    }

    public static class DefaultEnumStringValidator extends StringOption.Validator {

        @Override
        public void validate(String val) throws PropertyVetoException
        {
            EnumStringOption eOpt = (EnumStringOption)this.opt;
            for(String eVal : eOpt.availableValues) {
                if(val.equals(eVal))
                    return;
            }
            throw new PropertyVetoException(
                        "Invalid option value: " + val,
                        new PropertyChangeEvent(eOpt, eOpt.getName(),
                                                eOpt.getString(), val));
        }

    }

    public static class EnumStringStartsWithValidator
    extends StringOption.Validator {
        @Override
        public void validate(String val) throws PropertyVetoException
        {
            EnumStringOption eOpt = (EnumStringOption)this.opt;
            for(String eVal : eOpt.availableValues) {
                if(val.isEmpty()) {
                    if(eVal.isEmpty())
                        return;
                } else {
                    if(eVal.startsWith(val))
                        return;
                }
            }
            throw new PropertyVetoException(
                        "No valid option value starts with: " + val,
                        new PropertyChangeEvent(eOpt, eOpt.getName(),
                                                eOpt.getString(), val));
        }

    }

}
