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

import javax.swing.JComponent;
import javax.swing.event.CaretEvent;
import javax.swing.text.JTextComponent;

import com.raelity.jvi.*;
import com.raelity.jvi.core.CommandHistory.HistoryContext;

/**
 * The idea is that is to allow a different command line implementation.
 * For example using a JEditorPane rather than JTextField.
 * @author err
 */
@SuppressWarnings("serial")
interface SwingCommandLine extends CommandLineKeys
{
    static SwingCommandLine getNewDefault()
    {
        return new BasicCommandLine();
    }

    // typically: get() { return this; }
    JComponent get();

    /**
     * This installs the history list.
     */
    void SetHistory(HistoryContext ctx);

    /**
     *  This is used to append characters to the the command line. It is
     *  needed so that characters entered before the command line gets focus
     *  are not lost.
     *  <p>
     *  If there is a selection, then clear the selection. Do this because
     *  typically typed characters replace the selection.
     */
    void append(char c);

    /**
     *  Retrieve the contents of the command line. This is typically
     *  done in response to a action event.
     */
    String getCommand();

    /**
     *  This is used to initialize the text of the command line, needed so that
     *  characters entered before the command line gets focus are not lost.
     */
    void init(String s);

    /**
     *  Make the argument command the top of the list.
     *  First remove it.
     */
    void makeTop(String command);

    void setMode(String mode);
    String getMode();

    /** This is part of the fireSpecialEvent handling. */
    void fireCaretEvent(CaretEvent event);

    JTextComponent getTextComponent();
    void setupFont(Font srcFont);
    boolean isFiringEvents();

    void takeFocus(boolean flag);

    // following for a mac bug fixup
    int[] getMacFixupDotMark();

        /** CommandLineComplete; post this when command line is finished.
         * Could use ViCmdEntry.Complete and depend on target
         * using source to figure things out. But that might be
         * constraining and too cute. */
        static class CommandLineComplete extends ViCmdEntry.AbstractComplete
        {

        public CommandLineComplete(SwingCommandLine source, String actionCommand, String tag)
        {
            super(source, actionCommand, tag);
        }

        @Override
        public SwingCommandLine getSource()
        {
            return (SwingCommandLine)super.getSource();
        }
        
        } // END CLASS
}
