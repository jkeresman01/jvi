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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.options;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This jVi option is for debug, based on enum of logger level.
 * It supports getBoolean, with a variation that supplies a logger level.
 * It also support some print methods.
 * 
 * @author Ernie Rael <err at raelity.com>
 */
abstract public class DebugOption extends EnumOption<String>
{

    public DebugOption(String key, String defaultValue,
                              Validator<String> validator, String[] enumValues)
    {
        super(key, defaultValue, validator, enumValues);
    }

    @Override
    abstract public Boolean getBoolean();

    abstract public boolean getBoolean(Level level);

    abstract public void println(String s, Object... args);

    abstract public void println(Level level, String s, Object... args);

    abstract public void printf(String format, Object... args);

    abstract public void printf(Level level, String format, Object... args);

    public void println(Supplier<String> sup)
    {
        if(getBoolean())
            println(sup.get());
    }

    public void println(Level level, Supplier<String> sup)
    {
        if(getBoolean(level))
            println(level, sup.get());
    }

    public void printf(Supplier<String> sup)
    {
        if(getBoolean())
            printf(sup.get());
    }

    public void printf(Level level, Supplier<String> sup)
    {
        if(getBoolean(level))
            printf(level, sup.get());
    }

    abstract public Logger getLogger();

}
