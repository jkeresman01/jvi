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
 * Copyright (C) 2000-2011 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.options;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class BootDebugOption extends DebugOption {
    public boolean v;

    BootDebugOption(boolean v)
    {
        super(null, null, new DefaultEnumValidator<String>(), new String[0]);
        this.v = v;
    }

    @Override
    void initialize()
    {
    }

    @Override
    void setValue(String newValue)
    {
    }

    @Override
    public Boolean getBoolean()
    {
        return v;
    }

    @Override
    public boolean getBoolean(Level level)
    {
        return v;
    }

    @Override
    public void println(String s)
    {
        if(v)
            System.err.println(s);
    }

    @Override
    public void println(Level level, String s)
    {
        if(v)
            System.err.println(s);
    }

    @Override
    public void printf(String format, Object... args)
    {
        if(v)
            System.err.printf(format, args);
    }

    @Override
    public void printf(Level level, String format, Object... args)
    {
        if(v)
            System.err.printf(format, args);
    }

    @Override
    public Logger getLogger()
    {
        return null;
    }

}
