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
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Objects;

import javax.swing.event.ChangeListener;

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

    final public static class HistEntry
    {
    public String hisstr;
    public int hisnum;
    
    public HistEntry(String hisstr, int hisnum)
    {
        this.hisstr = hisstr;
        this.hisnum = hisnum;
    }
    
    public HistEntry(String hisstr)
    {
        this(hisstr, -1);
    }
    
    @Override
    public String toString()
    {
        return hisstr;
    }
    
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.hisstr);
        return hash;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        final HistEntry other = (HistEntry)obj;
        return Objects.equals(this.hisstr, other.hisstr);
    }
    
    }

    /** Start command entry.
     * The mode can be used to label the entry field.
     */
    public void activate(String mode, ViTextView parent);

    /** Start command entry.
     * The mode can be used to label the entry field.
     * @param parent component associated with the entry
     * @param initialText text widget starts out with this text
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
     * Retrieve the history.
     */
    public List<String> getHistory();

    public List<HistEntry> getHistEntrys();

    /**
     * Make the string the most recent in the history
     */
    public void makeTop(String s);

    /**
     * Set the history.
     */
    public void setHistory(List<String> l);

    /** When command entry is complete, this listener is invoked.
     * The event is the key event that stopped entry, either a
     * &lt;CR&gt; or an &lt;ESC&gt;.
     * Only a single listener is needed.
     */
    public void addActionListener(ActionListener l);

    public void removeActionListener(ActionListener l);

    //
    // The following provide monitoring entry input per character
    //

    public void addChangeListener(ChangeListener l);
    public void removeChangeListener(ChangeListener l);
    public String getCurrentEntry();
}
