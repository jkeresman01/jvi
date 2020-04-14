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

package com.raelity.jvi.swing;

import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import com.raelity.jvi.core.CommandHistory.HistoryContext;

/**
 *
 * @author err
 */
@SuppressWarnings("serial")
public abstract class AbstractCommandLine
extends JPanel
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

    /**
     * This installs the history list.
     */
    abstract void SetHistory(HistoryContext ctx);

    /**
     *  This is used to append characters to the the combo box. It is
     *  needed so that characters entered before the combo box gets focus
     *  are not lost.
     *  <p>
     *  If there is a selection, then clear the selection. Do this because
     *  typically typed characters replace the selection.
     */
    abstract void append(char c);

    /**
     *  Retrieve the contents of the command line. This is typically
     *  done in response to a action event.
     */
    abstract String getCommand();

    /**
     *  This is used to initialize the text of the combo box, needed so that
     *  characters entered before the combo box gets focus are not lost.
     */
    abstract void init(String s);

    /**
     *  Make the argument command the top of the list.
     *  First remove it.
     */
    abstract void makeTop(String command);

    abstract void setMode(String mode);

    /**
     * Adds the specified action listener to receive
     * action events from this textfield.
     *
     * @param l the action listener
     */
    abstract void addActionListener(ActionListener l);

    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this textfield.
     *
     * @param l the action listener
     */
    abstract void removeActionListener(ActionListener l);

    abstract JTextComponent getTextComponent();
    abstract void setupFont(Font srcFont);
    abstract boolean isFiringEvents();

    abstract void takeFocus(boolean flag);

    // following for a mac bug fixup
    abstract int[] getMacFixupDotMark();
    
}
