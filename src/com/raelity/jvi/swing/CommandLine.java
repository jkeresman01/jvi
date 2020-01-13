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

package com.raelity.jvi.swing;

import java.awt.AWTEvent;
import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxEditor.UIResource;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.Util;
import com.raelity.jvi.options.DebugOption;


import com.raelity.jvi.ViCmdEntry.HistEntry;
import com.raelity.jvi.core.*;
import com.raelity.jvi.manager.*;

import static com.raelity.text.TextUtil.sf;

/**
 * This class presents a editable combo box UI for picking up command entry
 * data. A mode, usually a single character like "?" or ":", can be
 * set for display. This UI supports the maintenance of a history of commands.
 * This component builds a keymap through which the action events are
 * delivered. When an event is delivered, the history list is updated.
 * The command line has a label that can be set with {@link #setMode}.
 * <p>Take steps to prevent this component from taking focus
 * through the focus manager. But ultimately need to subclass the
 * editor component to handle it.
 */
@SuppressWarnings("serial")
public final class CommandLine extends JPanel
{
    private static final boolean flip = true;
    static final Logger LOG
            = Logger.getLogger(CommandLine.class.getName());
    static public final int DEFAULT_HISTORY_SIZE = 50;
    static public final String COMMAND_LINE_KEYMAP = "viCommandLine";
    private final JLabel modeLabel = new JLabel();
    private final JComboBox<HistEntry> combo = new JComboBox<>();
    private final GridBagLayout gridBagLayout1 = new GridBagLayout();
    private String mode;
    private Border border1;
    boolean setKeymapActive;
    private String findHistoryPrefix = "";
    private boolean inUpDown = false;
    private boolean afterComboEnd = false;
    private boolean waitingToFire = false; // MORE HACKING
    private int dot;
    private int mark;
    private static final String ACT_FINISH = "vi-command-finish";
    private static final String ACT_TAB = "vi-command-tab";
    private static final String ACT_BACK_SPACE = "vi-command-backspace";
    static final DebugOption dbgKeys
            = Options.getDebugOption(Options.dbgKeyStrokes);

    /**
     *  This is not intended to match an actual keystroke, it is used
     *  to register an action that can be used externally.
     */
    public static final KeyStroke EXECUTE_KEY
            = KeyStroke.getKeyStroke( KeyEvent.VK_ENTER,
                                      InputEvent.SHIFT_DOWN_MASK
                                      | InputEvent.ALT_DOWN_MASK
                                      | InputEvent.META_DOWN_MASK
                                      | InputEvent.CTRL_DOWN_MASK
                                      | InputEvent.BUTTON1_DOWN_MASK
                                      | InputEvent.BUTTON2_DOWN_MASK
                                      | InputEvent.BUTTON3_DOWN_MASK );


    // ............


    public CommandLine()
    {
        // see https://substance.dev.java.net/issues/show_bug.cgi?id=285
        //combo.putClientProperty(LafWidget.COMBO_BOX_NO_AUTOCOMPLETION, true);
        combo.putClientProperty("lafwidgets.comboboxNoAutoCompletion", true);
        combo.setEditor(new MyComboBoxEditor());
        combo.setEditable(true);
        JTextComponent text
                = (JTextComponent) combo.getEditor().getEditorComponent();
        text.addPropertyChangeListener("keymap", (PropertyChangeEvent evt) -> {
            if(setKeymapActive)
                return;
            Object newO = evt.getNewValue();
            Object oldO = evt.getOldValue();
            if(newO != null) {
                EventQueue.invokeLater(this::setKeymap);
            }
        });

        try {
            jbInit();
        } catch ( Exception ex ) {
            LOG.log(Level.SEVERE, null, ex);
        }
        Font font = modeLabel.getFont();
        modeLabel.setFont(new Font("Monospaced",
                                   Font.BOLD,
                                   font.getSize()));
        font = combo.getFont();
        combo.setFont(new Font("Monospaced",
                               font.getStyle(),
                               font.getSize()));

        setMode(" ");
        setComboDoneListener();
        setKeymap();
        // allow tabs to be entered into text field
        Component c = getTextComponent();
        Set<AWTKeyStroke> set = Collections.emptySet();
        c.setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set);
        c.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);
        Options.addPropertyChangeListenerSET(
                Options.history, (evt) -> sizeChange(evt));
    }

    public int getDot()
    {
        return dot;
    }

    public int getMark()
    {
        return mark;
    }

    void setupFont(Font srcFont)
    {
        modeLabel.setFont(srcFont.deriveFont(Font.BOLD));
        combo.setFont(srcFont);
    }

    /**
     *  This is used to initialize the text of the combo box, needed so that
     *  characters entered before the combo box gets focus are not lost.
     */
    public void init( String s )
    {
        waitingToFire = true;
        if(combo.getItemCount() > 0)
            combo.setSelectedIndex(flipComboIndex(0));
        dot = mark = 0;
        // set combo index to the end ????????? should already be there
        setFindHistoryPrefix(s);
        afterComboEnd = true;
        if ( s.length() == 0 ) {
            JTextComponent tc = getTextComponent();
            int len = tc.getText().length();
            if(len > 0) {
                tc.setCaretPosition(0);
                tc.moveCaretPosition(len);
                mark = 0;
                dot = len;
                //System.err.println("Selection length = " + len);
            }
        } else {
            try {
                Document doc = getTextComponent().getDocument();
                mark = dot = s.length();
                doc.remove(0,doc.getLength());
                doc.insertString(0, s, null);
            } catch ( BadLocationException ex ) { }
        }
    }

    private void setFindHistoryPrefix(String s) {
        findHistoryPrefix = s;
        // int curIdx = flipComboIndex(0);
        // if(curIdx != combo.getin)
        //combo.insertItemAt(findHistoryPrefix, flipComboIndex(0));
    }


    /**
     *  This is used to append characters to the the combo box. It is
     *  needed so that characters entered before the combo box gets focus
     *  are not lost.
     *  <p>
     *  If there is a selection, then clear the selection. Do this because
     *  typically typed characters replace the selection.
     */
    public void append( char c )
    {
        String s = String.valueOf(c);
        JTextComponent tc = getTextComponent();
        if(tc.getSelectionStart() != tc.getSelectionEnd()) {
            // replace the selection
            tc.setText(s);
        } else {
            int offset = tc.getDocument().getLength();
            try {
                getTextComponent().getDocument().insertString(offset, s, null);
            } catch (BadLocationException ex) { }
        }
    }


    private void jbInit() throws Exception
    {
        modeLabel.setText("");
        this.setLayout(gridBagLayout1);
        this.add(modeLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(combo, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
    }


    public void setupBorder()
    {
        if(border1 == null) {
            border1 = BorderFactory.createLineBorder(Color.black, 2);
        }
        this.setBorder(border1);
    }

    public void takeFocus( boolean flag )
    {
        if ( flag ) {
            combo.setEnabled(true);
            // NEEDSWORK: FOCUS: use requestFocusInWindow()
            getTextComponent().requestFocus();
        } else {
            combo.setEnabled(false);
        }
    }


    /**
     *  Retrieve the contents of the command line. This is typically
     *  done in response to a action event.
     */
    public String getCommand()
    {
        return getTextComponent().getText();
    }


    /**
     *  This sets the mode of the command line, e.g. ":" or "?".
     */
    void setMode( String newMode )
    {
        mode = newMode;
        modeLabel.setText(" " + mode + " ");
    }


    public String getMode()
    {
        return mode;
    }


    /**
     *  Returns the mode JLabel.
     *  This is intended for use in setting font or color to
     *  match a surrounding UI.
     */
    public JLabel getModeLabel()
    {
        return modeLabel;
    }


    final JTextComponent getTextComponent()
    {
        Component c = combo.getEditor().getEditorComponent();
        return (JTextComponent)c;
    }

    /**
     * Push the command to the head/top of the list
     * @param command 
     */
    private void flipPushComboItem(HistEntry he) {
        if(flip) {
            combo.addItem(he);
        } else {
            combo.insertItemAt(he, 0);
        }
    }

    private int flipComboIndex(int i) {
        return flip ? combo.getItemCount() -1 -i : i;
    }

    /**
     * This installs the history list.
     */
    public void SetHistory(List<String> l)
    {
        l = new ArrayList<>(l);
        if(flip)
            Collections.reverse(l);
        combo.removeAllItems();
        for(String item: l) {
            combo.addItem(new HistEntry(item, ++histEntryIndex));
        }
        combo.setSelectedIndex(flipComboIndex(0));
    }

    public List<String> getHistory()
    {
        List<String> l = new ArrayList<>(combo.getItemCount());
        for(int i = 0; i < combo.getItemCount(); i++) {
            l.add(combo.getItemAt(flipComboIndex(i)).hisstr);
        }
        return l;
    }

    // Retrieve list. Oldest first, newest at end of list
    public List<HistEntry> getHistEntrys() {
        List<HistEntry> l = new ArrayList<>(combo.getItemCount());
        for(int i = 0; i < combo.getItemCount(); i++) {
            // l.add(combo.getItemAt(flipComboIndex(i)));
            l.add(combo.getItemAt(i)); // i <-- should be flip(flip(i))
        }
        return l;
    }
    
    int histEntryIndex = 0;

    /**
     *  Make the argument command the top of the list.
     *  First remove it.
     */
    public void makeTop( String command )
    {
        if ( G.p_hi() == 0 ) {
            return;
        }
        // if the empty string was selected we're done
        if(command.isEmpty()) {
            return;
        }
        // save the command in the history
        HistEntry he = new HistEntry(command, ++histEntryIndex);
        combo.removeItem(he);
        flipPushComboItem(he);
        combo.setSelectedIndex(flipComboIndex(0));
        trimList();
    }

    private void sizeChange(PropertyChangeEvent evt)
    {
        trimList();
    }

    private void trimList()
    {
        while(combo.getItemCount() > G.p_hi()) {
            // remove the last item
            combo.removeItemAt(flipComboIndex(combo.getItemCount() - 1));
        }
    }


    private void setComboDoneListener()
    {
        combo.addActionListener((ActionEvent e) -> {
            if("comboBoxEdited".equals(e.getActionCommand())) {
                ActionEvent e01 = new ActionEvent(CommandLine.this,
                        e.getID(), "\n", e.getModifiers());
                fireCommandLineActionPerformed(e01);
            }
        });

    }


    /**
     * Return and <ESC> fire events and update the history list.
     */
    private void setKeymap()
    {
        if ( setKeymapActive ) {
            return;
        }
        setKeymapActive = true;
        Keymap keymap = JTextComponent.addKeymap(
                CommandLine.COMMAND_LINE_KEYMAP,
                getTextComponent().getKeymap());
        JTextComponent.loadKeymap(keymap, getBindings(), getActions());
        getTextComponent().setKeymap(keymap);
        setKeymapActive = false;
    }


    protected JTextComponent.KeyBinding[] getBindings()
    {
        JTextComponent.KeyBinding[] bindings = {
            new JTextComponent.KeyBinding(
                    EXECUTE_KEY,
                    ACT_FINISH),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_ESCAPE, 0),
                    ACT_FINISH),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK),
                    ACT_FINISH),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    '\t'),
                    ACT_TAB),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_BACK_SPACE, 0, false),
                    ACT_BACK_SPACE),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK, false),
                    ACT_BACK_SPACE),
        };
        return bindings;
    }


    protected Action[] getActions()
    {
        Action[] localActions = null;
        try {
            localActions = new Action[] {
                new TextAction(ACT_FINISH) {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        // Note that only '\n' as a command does anything,
                        // other things, like ESC and Ctrl-], are ignored
                        String actionCommand = e.getActionCommand();
                        int modifiers = e.getModifiers();
                        ActionEvent e01 = new ActionEvent(
                                CommandLine.this,
                                e.getID(),
                                actionCommand,
                                modifiers);
                        fireCommandLineActionPerformed(e01);
                    }
                },
                new TextAction(ACT_TAB) {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        // input the tab (instead of focus traversal)
                        getTextComponent(e).replaceSelection("\t");
                    }
                },
                new TextAction(ACT_BACK_SPACE) {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        // Treat a backspace on an empty line like <ESC>
                        JTextComponent jtc = getTextComponent(e);
                        if(jtc.getText().isEmpty()) {
                            Action act = jtc.getKeymap().getAction(
                                    KeyStroke.getKeyStroke(
                                        KeyEvent.VK_ESCAPE, 0));
                            act.actionPerformed(new ActionEvent(
                                    e.getSource(), e.getID(), "\u001b"));
                        } else {
                            Action bs= jtc.getActionMap()
                                .get(DefaultEditorKit.deletePrevCharAction);
                            if(bs != null) {
                                bs.actionPerformed(e);
                            }
                        }
                    }
                },
            };
        } catch(Throwable e) {
            LOG.log(Level.SEVERE, null, e);
        }
        return localActions;
    }


    /**
     * Deliver event to command line listener;
     * This is expected to complete the command
     * Do some maintenance on the LRU history.
     */
    protected void fireCommandLineActionPerformed( ActionEvent e )
    {
        // only run this once, sometimes get event from combo and keys
        if(!waitingToFire)
            return;
        waitingToFire = false;
        String command = getTextComponent().getText();

        // Maintain LRU history
        makeTop(command);
        combo.hidePopup();

        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }


    /**
     * Adds the specified action listener to receive
     * action events from this textfield.
     *
     * @param l the action listener
     */
    public synchronized void addActionListener( ActionListener l )
    {
        listenerList.add(ActionListener.class, l);
    }


    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this textfield.
     *
     * @param l the action listener
     */
    public synchronized void removeActionListener( ActionListener l )
    {
        listenerList.remove(ActionListener.class, l);
    }


// inner classes ...........................................................

    //////////////////////////////////////////////////////////////////////
    //
    // In jdk1.8 in JComboBox::actionPerformed, the following was added
    //    ComboBoxEditor editor = getEditor();
    //    if ((editor != null) && (e != null) && (editor == e.getSource())) {
    // and
    //    editor -------- "BasicComboBoxEditor"
    //    e.getSource --- "BasicComboBoxEditor$BorderlessTextField"
    // so nothing happens on 1.8
    //
    // To work around this, override fireActionPerformed in the JTextField
    // and provide the combobox editor as the event source.
    // (I must be missing something, else how could you ever use this...)
    //
    //////////////////////////////////////////////////////////////////////
    private class MyComboBoxEditor extends BasicComboBoxEditor {
        @Override
        protected JTextField createEditorComponent() {
            // borderless text field has an issue, see below
            JTextField ed = new MyComboTextField("",9);
            ed.setBorder(null);
            ed.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if(!inUpDown)
                        setFindHistoryPrefix(ed.getText());
                    dbgKeys.printf(Level.FINER, ()->sf(
                                   "insert: inUpDown %b, text '%s'\n",
                                   inUpDown, ed.getText()));
                }

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if(!inUpDown)
                        setFindHistoryPrefix(ed.getText());
                    dbgKeys.printf(Level.FINER, ()->sf(
                                   "remove: inUpDown %b, text '%s'\n",
                                   inUpDown, ed.getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                }
            });
            return ed;
        }
    }

    // Copied from BasicComboBoxEditor::BorderlessTextField
    //          except for actionPerformed
    private class MyComboTextField extends JTextField
    {

    public MyComboTextField(String value,int n) {
        super(value,n);
    }

    @Override
    protected void processKeyEvent(KeyEvent e)
    {
        if(!ViManager.getFactory().commandEntryAssistBusy(null)
                && e.getID() == KeyEvent.KEY_PRESSED
                && (e.getKeyCode() == KeyEvent.VK_UP
                    || e.getKeyCode() == KeyEvent.VK_DOWN)) {
            try {
                dbgKeys.printf(Level.FINE, ()->sf(
                        "processKeyEvent idx %d, find %s\n",
                        combo.getSelectedIndex(), findHistoryPrefix));
                inUpDown = true;
                int dir = e.getKeyCode() == KeyEvent.VK_UP ? -1 : +1;
                boolean toNewer = flip ? dir > 0 : dir < 0;
                if(combo.isPopupVisible() == false) {
                    if(toNewer && afterComboEnd) {
                        SwingUtilities.invokeLater(() -> Util.beep_flush());
                        return;
                    }
                    SwingUtilities.invokeLater(() -> combo.showPopup());
                }
                // there's only one shot at using a selection, to late now...
                setCaretPosition(getCaretPosition()); // clear any selection
                //
                // find next command line that matches user input in direction
                //
                int curIdx = combo.getSelectedIndex();
                dbgKeys.printf(Level.FINE, "up/down idx %d, find %s\n",
                        curIdx, findHistoryPrefix);
                int lastDisplayedIdx; // remember current displayed
                                      // will restore if don't find a match
                // adjust curIdx to next item to consider
                if(afterComboEnd && !toNewer) {
                    // going into the combo values
                    // need to display last combo item, since it was pre-empted
                    setText(combo.getSelectedItem().toString());
                    lastDisplayedIdx = -1; // signal started after combo end
                } else {
                    lastDisplayedIdx = curIdx;
                    curIdx += dir;
                }
                afterComboEnd = false;
                while(curIdx >= 0 && curIdx < combo.getItemCount()) {
                    if(combo.getItemAt(curIdx).hisstr
                            .startsWith(findHistoryPrefix)) {
                        combo.setSelectedIndex(curIdx);
                        return;
                    }
                    curIdx += dir;
                }
                // hit a combo list bboundary, leave at a known spot
                if(lastDisplayedIdx < 0) {
                    SwingUtilities.invokeLater(() -> Util.beep_flush());
                    return;
                }
                if(toNewer) {
                    // to create a space for the newest after the end,
                    // show currently under edit
                    combo.setSelectedIndex(flipComboIndex(0));
                    setText(findHistoryPrefix);
                        afterComboEnd = true;
                        SwingUtilities.invokeLater(() -> combo.hidePopup());
                } else {
                    combo.setSelectedIndex(lastDisplayedIdx);
                    SwingUtilities.invokeLater(() -> Util.beep_flush());
                }
                return;
            } finally {
                inUpDown = false;
            }
        }
        super.processKeyEvent(e);
    }
        
    // workaround for 4530952
    @Override
    public void setText(String s) {
        if (getText().equals(s)) {
            return;
        }
        super.setText(s);
    }

    @Override
    public void setBorder(Border b) {
        if (!(b instanceof UIResource)) {
            super.setBorder(b);
        }
    }

    // modified to set the combobox editor as the event source
    @Override
    protected void fireActionPerformed() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent)currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent)currentEvent).getModifiers();
        }
        ActionEvent e =
            new ActionEvent(combo.getEditor(), ActionEvent.ACTION_PERFORMED,
                            (myCommand != null) ? myCommand : getText(),
                            EventQueue.getMostRecentEventTime(), modifiers);

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }

    private String myCommand;
    @Override
    public void setActionCommand(String command)
    {
        super.setActionCommand(command);
        myCommand = command;
    }
    }

    String cstat()
    {
        return cstat("");
    }
    String cstat(String tag)
    {
        int i = combo.getSelectedIndex();
        int n = combo.getItemCount();
        String item = i >= 0 && i < n ? combo.getItemAt(i).hisstr : "OOB";
        String last = n > 0 ? combo.getItemAt(combo.getItemCount()-1).hisstr : "---";

        String s = sf("%s: n %d, i %d, item '%s', last '%s'\n",
                      tag, n, i, item, last);
        System.err.printf(s);
        return s;
    }

} // end com.raelity.jvi.swing.CommandLine
