/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.lib;

import java.awt.event.ActionEvent;

import javax.swing.event.ChangeEvent;

import com.raelity.jvi.core.*;

import static com.raelity.text.TextUtil.sf;

/**
 *
 * @author err
 */
public class LibUtil
{
private LibUtil() {}
static LibUtil INSTANCE;
private boolean aTrue;
private boolean aFalse;

public static LibUtil getDefault() {
    if(INSTANCE == null) {
        INSTANCE = new LibUtil();
        INSTANCE.aFalse = false;
        INSTANCE.aTrue = true;
    }
    return INSTANCE;
}

public static String dumpEvent(ActionEvent e)
{
    return sf("cmd=%s on %s",
              e.getActionCommand(), e.getSource().getClass().getSimpleName());
    
}

public static String dumpChangeEvent(ChangeEvent e) {
    return sf("on %s", e.getSource().getClass().getSimpleName());
}

public static boolean getTrue() { return getDefault().aTrue; }

public static boolean getFalse() { return getDefault().aFalse; }

}
