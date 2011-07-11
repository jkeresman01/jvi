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

import java.beans.PropertyVetoException;

/**
 * @author Ernie Rael <err at raelity.com>
 */
public class EnumOption<T> extends Option<T> {
    private T[] enumValues;

    @SuppressWarnings("unchecked")
    /*package*/ EnumOption(String key, T defaultValue,
                              Validator<T> validator, T[] enumValues)
    {
        super((Class<T>)enumValues.getClass().getComponentType(),
              key, defaultValue, validator);
        this.enumValues = enumValues;
    }

    public T[] getAvailableValues()
    {
        return enumValues;
    }

    /** Notice that for the default validator, 'T' does not matter */
    public static class DefaultEnumValidator<T> extends Validator<T> {

        @Override
        public void validate(T val) throws PropertyVetoException
        {
            EnumOption eOpt = (EnumOption)opt;
            for(Object eVal : eOpt.enumValues) {
                if(eVal == null) {
                    if(val == null)
                        return;
                } else {
                    if(eVal.equals(val))
                        return;
                }
            }
            reportPropertyVetoException(
                    "Invalid option value: " + val, val);
        }

    }
}
