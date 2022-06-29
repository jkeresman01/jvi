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
package com.raelity.jvi;

import java.awt.Component;

import com.google.common.eventbus.EventBus;

import com.raelity.jvi.core.CommandHistory.HistoryContext;
import com.raelity.jvi.manager.*;
import com.raelity.text.TextUtil;

import static com.raelity.text.TextUtil.sf;

/** This is used by vi to get command line input.
 * An LRU history of commands should be maintained, though this history
 * is transparent. When multiple objects are constructed, each has
 * its own history. When active, a focus listener may be set up on the
 * parent to keep grabing focus until the entry is stopped.
 * <p>The command
 * entry widget is responsible for adding user keystrokes to the recorded
 * input by calling {@link com.raelity.tools.vi.GetChar#userInput(String)}.
 * The user input should not include any initialText. If the user changes
 * the initialText during entry, and recording is going on, results are
 * undefined.
 * </p>
 */
public interface ViCmdEntry {
    enum Type { SEARCH, COLON }
    static public final int SEARCH_ENTRY = 1;
    static public final int COLON_ENTRY = 2;

    /** Start command entry.
     * The mode can be used to label the entry field.
     * @param parent component associated with the entry
     * @param initialText text widget starts out with this text;
     * @param passThru when true, fire action with initialText as the command
     */
    public void activate(String mode, ViTextView parent,
                         String initialText, boolean passThru);

    /**
     * This is used to retrieve the command that was entered.
     */
    public String getCommand();

    /**
     * Stop command entry.
     */
    public void cancel();

    /**
     * This method is used to append characters to the command line.
     * It is used to prevent lost characters that may occur between
     * the character that initiates command entry and the time the
     * entry field is ready to take characters.
     */
    public void append(char c);

    /**
     * Retrieve the component used for the data entry.
     */
    public Component getTextComponent();

    /**
     * Poke listeners, platform dependent; the idea is to let the
     * entry widget alert platform to do something.
     */
    public void firePlatformEvent(Class<?> target, Object event, String msg);

    /**
     * Make the string the most recent in the history
     */
    public void makeTop(String s);

    /**
     * Set the history.
     */
    public void setHistory(HistoryContext ctx);

    /** the current entry */
    public String getCurrentEntry();

    /**
     * Command lines share an event bus.
     * @return event bus for command lines
     */

    static final EventBus eventBus
            = new EventBus(new ViEvent.ExHandler("ViCmdEntry.Event: handleException:"));
    static EventBus getEventBus()
    {
        return eventBus;
    }

        /**
         * Post this when CmdEntry is finished.
         */
        public abstract static class CmdEntryComplete extends AbstractComplete
        {
        public CmdEntryComplete(ViCmdEntry source, String actionCommand, String tag)
        {
            super(source, actionCommand, tag);
        }

        @Override
        public ViCmdEntry getSource()
        {
            return (ViCmdEntry)super.getSource();
        }
        } // END CLASS

        /** Post when text of the command line changes;
         *  event source is {@link #getTextComponent() }.
         */
        public static class CmdEntryChange extends Event
        {
        public CmdEntryChange(Object source)
        {
            super(source);
        }
        } // END CLASS

        public abstract static class AbstractComplete extends Event
        {
        private final String actionCommand;
        private final String tag;
        public AbstractComplete(Object source, String actionCommand, String tag)
        {
            super(source);
            this.actionCommand = actionCommand;
            this.tag = tag;
        }

        /** typically a "\n" or {@literal "<ESC>"} */
        public String getActionCommand()
        {
            return actionCommand;
        }

        @Override
        public String toString()
        {
            return sf("Complete{%s:%s %s}",
                      getSource().getClass().getSimpleName(),
                      tag != null ? " '" + tag + "':" : "",
                      TextUtil.debugString(actionCommand));
        }

        } // END CLASS

        public static class Event
        {
        private final Object source;
        
        public Event(Object source)
        {
            this.source = source;
        }
    
        public Object getSource()
        {
            return source;
        }
        } // END CLASS
}
