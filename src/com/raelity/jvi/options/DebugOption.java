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
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class DebugOption extends EnumStringOption
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
        super(key, "OFF", loggerValues);
        logger = Logger.getLogger("com.raelity.jvi.debug." + key);
        // setValue below gets invoked by the constructor,
        // so it can't set the logger level
        logger.setLevel(Level.parse(stringValue));
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
    public boolean getBoolean()
    {
        return logger.isLoggable(Level.SEVERE);
    }

    public boolean getBoolean(Level level)
    {
        return logger.isLoggable(level);
    }

}
