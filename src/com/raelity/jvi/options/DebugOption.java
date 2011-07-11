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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This jVi option is for debug, based on enum of logger level.
 * It supports getBoolean, with a variation that supplies a logger level.
 * It also support some print methods.
 * 
 * @author Ernie Rael <err at raelity.com>
 */
public class DebugOption extends EnumOption<String>
{
    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;
    static String[] loggerValues = {
        "OFF",
        "ALL",
        "SEVERE",
        "WARNING",
        "INFO",
        "CONFIG",
        "FINE",
        "FINER",
        "FINEST",
    };

    public DebugOption(String key)
    {
        super(key, "OFF", new DefaultEnumValidator<String>(),
              new String[] {
                  "OFF",
                  "ALL",
                  "SEVERE",
                  "WARNING",
                  "INFO",
                  "CONFIG",
                  "FINE",
                  "FINER",
                  "FINEST",
              }
                      );
        logger = Logger.getLogger("com.raelity.jvi.debug." + key);
        // setValue below gets invoked by the constructor,
        // so it can't set the logger level
        logger.setLevel(Level.parse(value));
    }

    @Override
    void setValue(String newValue)
    {
        // do conversion of boolean
        if("true".equalsIgnoreCase(newValue))
            newValue = "ALL";
        else if("false".equalsIgnoreCase(newValue))
            newValue = "OFF";

        super.setValue(newValue);
        if(logger != null)
            logger.setLevel(Level.parse(newValue));
    }

    @Override
    // NEEDSWORK: change getBoolean to something like: fLog(),
    //            then make getBoolean final.
    final public Boolean getBoolean()
    {
        return logger.isLoggable(Level.SEVERE);
    }

    final public boolean getBoolean(Level level)
    {
        return logger.isLoggable(level);
    }

    final public void println(String s)
    {
        println(Level.SEVERE, s);
    }

    final public void println(Level level, String s)
    {
        if(getBoolean(level))
            System.err.println(s);
    }

    final public void printf(String format, Object... args)
    {
        printf(Level.SEVERE, format, args);
    }

    final public void printf(Level level, String format, Object... args)
    {
        if(getBoolean(level))
            System.err.printf(format, args);
    }

    final public Logger getLogger()
    {
        return logger;
    }

}
