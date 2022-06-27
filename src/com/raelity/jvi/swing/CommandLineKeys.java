/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2022 Ernie Rael.  All Rights Reserved.
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

package com.raelity.jvi.swing;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

/**
 *
 * @author err
 */
public interface CommandLineKeys
{
    public static String COMMAND_LINE_KEYMAP = "viCommandLine";
    /**
     *  This is not intended to match an actual keystroke, it is used
     *  to register an action that can be used externally.
     */
    public static KeyStroke EXECUTE_KEY
            = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                                     InputEvent.SHIFT_DOWN_MASK |
            InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK |
            InputEvent.CTRL_DOWN_MASK | InputEvent.BUTTON1_DOWN_MASK |
            InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK);
}
