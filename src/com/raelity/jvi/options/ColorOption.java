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
import java.beans.PropertyVetoException;

public class ColorOption extends Option<Color>
{
    private boolean permitNull;

    public ColorOption(String key, Color defaultValue)
    {
        this(key, defaultValue, false, null);
    }

    public ColorOption(String key, Color defaultValue, boolean permitNull,
                       Validator<Color> validator)
    {
        super(Color.class, key, defaultValue, validator);
        this.permitNull = permitNull;

        // must do this after permitNull is set
        super.initialize();
    }

    @Override
    void initialize()
    {
        // can't initialize until after permitNull is set
    }

    public boolean isPermitNull()
    {
        return permitNull;
    }

    @Override
    final Color getValueFromString(String sVal)
    {
        return decode(sVal);
    }

    @Override
    final String getValueAsString(Color val)
    {
        return encode(val);
    }

    /**
     * If param is "null" and nulls are permitted, then null is returned,
     * if "default" then return the default.
     * @param s
     * @return
     */
    public final Color decode(String s)
    {
        Color color;
        if (s.isEmpty()) {
            if (permitNull) {
                color = null;
            } else {
                color = defaultValue;
            }
        } else {
            if(permitNull && "null".equals(s))
                color = null;
            else if("default".equals(s))
                color = defaultValue;
            else
                color = Color.decode(s);
        }
        return color;
    }



    public static String encode(Color c)
    {
        if (c == null) {
            return "null";
        }
        return String.format("0x%06x", c.getRGB() & 0xffffff);
    }

    static class DefaultColorValidator extends Validator<Color>
    {
        @Override
        public void validate(Color val) throws PropertyVetoException
        {
            if (val == null && !((ColorOption)opt).isPermitNull()) {
                reportPropertyVetoException("null color not permitted", val);
            }
        }
    }
}
