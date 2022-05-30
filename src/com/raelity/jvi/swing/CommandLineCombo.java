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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
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


import com.raelity.jvi.core.*;
import com.raelity.jvi.core.CommandHistory.HistoryContext;
import com.raelity.jvi.core.CommandHistory.InitialHistoryItem;
import com.raelity.jvi.manager.*;

import static com.raelity.jvi.lib.LibUtil.dumpEvent;
import static com.raelity.jvi.manager.ViManager.eatme;
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
public final class CommandLineCombo extends AbstractCommandLine
{
    private static final boolean flip = true;
    static final Logger LOG
            = Logger.getLogger(CommandLineCombo.class.getName());
    private static final DebugOption dbg = Options.getDebugOption(Options.dbgSearch);
    private final JLabel modeLabel = new JLabel();
    private final JComboBox<String> combo = new JComboBox<>();
    private final GridBagLayout gridBagLayout1 = new GridBagLayout();
    private String mode;
    private Border border1;
    boolean setKeymapActive;
    private boolean inUpDown = false;
    private boolean commandLineFiringEvents = false; // MORE HACKING
    private int dot;
    private int mark;
    private static final String ACT_FINISH = "vi-command-finish";
    private static final String ACT_TAB = "vi-command-tab";
    private static final String ACT_BACK_SPACE = "vi-command-backspace";
    static final DebugOption dbgKeys
            = Options.getDebugOption(Options.dbgKeyStrokes);
    HistoryContext ctx;


    @Override
    void fireCaretEvent(CaretEvent event)
    {
        // commandlinecombo is deprecated...
    }


    // ............


    public CommandLineCombo()
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
            eatme(oldO);
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
        setupBorder();
    }

    @Override
    public int[] getMacFixupDotMark()
    {
        return new int[] { dot, mark };
    }

    @Override
    public void setupFont(Font srcFont)
    {
        modeLabel.setFont(srcFont.deriveFont(Font.BOLD));
        combo.setFont(srcFont);
    }

    /**
     *  This is used to initialize the text of the combo box, needed so that
     *  characters entered before the combo box gets focus are not lost.
     */
    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void init( String s )
    {
        InitialHistoryItem initalState = ctx.init();
        commandLineFiringEvents = true;
        dbg.printf("CLINE: init: commandLineFiringEvents true\n");
        if(combo.getItemCount() > 0)
            combo.setSelectedIndex(flipComboIndex(0));
        dot = mark = 0;
        // set combo index to the end ????????? should already be there
        setFindHistoryPrefix(s);
        dbg.printf("CLINE: init: middle: s=%s\n", s);
        if ( s.length() == 0 ) {
            JTextComponent tc = getTextComponent();
            String t = initalState.getInitialItem();
            if(!tc.getText().equals(t))
                System.err.println("COMBO NOT EQUAL");
            tc.setText(t);
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
        dbg.printf(() -> sf("CLINE: init: end: text=%s\n",
                            getTextComponent().getText()));
    }

    private void setFindHistoryPrefix(String s) {
        ctx.setFilter(s);
        //findHistoryPrefix = s;
    }


    /**
     *  This is used to append characters to the the combo box. It is
     *  needed so that characters entered before the combo box gets focus
     *  are not lost.
     *  <p>
     *  If there is a selection, then clear the selection. Do this because
     *  typically typed characters replace the selection.
     */
    @Override
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


    private void setupBorder()
    {
        if(border1 == null) {
            border1 = BorderFactory.createLineBorder(Color.black, 2);
        }
        this.setBorder(border1);
    }

    @Override
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
    @Override
    public String getCommand()
    {
        return getTextComponent().getText();
    }


    /**
     *  This sets the mode of the command line, e.g. ":" or "?".
     */
    @Override
    void setMode( String newMode )
    {
        mode = newMode;
        modeLabel.setText(" " + mode + " ");
    }

    @Override
    final JTextComponent getTextComponent()
    {
        Component c = combo.getEditor().getEditorComponent();
        return (JTextComponent)c;
    }

    /**
     * Push the command to the head/top of the list
     * @param command 
     */
    private void flipPushComboItem(String command) {
        if(flip) {
            combo.addItem(command);
        } else {
            combo.insertItemAt(command, 0);
        }
    }

    private int flipComboIndex(int i) {
        return flip ? combo.getItemCount() -1 -i : i;
    }

    /**
     * This installs the history list.
     */
    @Override
    public void SetHistory(HistoryContext ctx)
    {
        this.ctx = ctx;
        ctx.init();
        ArrayList<String> l = new ArrayList<>();
        String s;
        while((s = ctx.next()) != null)
            l.add(s);
        if(flip)
            Collections.reverse(l);
        combo.removeAllItems();
        for(String item: l) {
            combo.addItem(item);
        }
        combo.setSelectedIndex(flipComboIndex(0));
    }
    
    int histEntryIndex = 0;

    /**
     *  Make the argument command the top of the list.
     *  First remove it.
     */
    @Override
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
        //HistEntry he = new HistEntry(command, ++histEntryIndex);
        combo.removeItem(command);
        flipPushComboItem(command);
        combo.setSelectedIndex(flipComboIndex(0));
        ctx.push(command);
        trimList();
    }

    private void sizeChange(PropertyChangeEvent evt)
    {
        eatme(evt);
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
                ActionEvent e01 = new ActionEvent(CommandLineCombo.this,
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
                COMMAND_LINE_KEYMAP,
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
                                CommandLineCombo.this,
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

    @Override
    boolean isFiringEvents() {
        return commandLineFiringEvents;
    }

    /**
     * Deliver event to command line listener;
     * This is expected to complete the command
     * Do some maintenance on the LRU history.
     */
    protected void fireCommandLineActionPerformed( ActionEvent e )
    {
        // only run this once, sometimes get event from combo and keys
        dbg.printf(() -> sf("CLINE: fireCommandLineActionPerformed: %s\n",
                          dumpEvent(e)));
        if(!commandLineFiringEvents)
            return;

        commandLineFiringEvents = false;
        String command = getTextComponent().getText();
        dbg.printf("CLINE: commandLineFiringEvents false cmd=%s\n", command);

        // Maintain LRU history
        makeTop(command);
        combo.hidePopup();

        // turned off events for makeTop...
        dbg.printf("CLINE: commandLineFiringEvents true for finishing events\n");
        commandLineFiringEvents = true;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
        commandLineFiringEvents = false;
        dbg.printf("CLINE: commandLineFiringEvents false until next time\n");
    }


    /**
     * Adds the specified action listener to receive
     * action events from this textfield.
     *
     * @param l the action listener
     */
    @Override
    public synchronized void addActionListener( ActionListener l )
    {
        dbg.println(() -> sf("CLINE: addAddActionListener: %s", l));
        listenerList.add(ActionListener.class, l);
    }


    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this textfield.
     *
     * @param l the action listener
     */
    @Override
    public synchronized void removeActionListener( ActionListener l )
    {
        dbg.println(() -> sf("CLINE: removeAddActionListener: %s", l));
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
                    if(!inUpDown) {
                        setFindHistoryPrefix(ed.getText());
                    }
                    dbgKeys.printf(Level.FINER, ()->sf(
                                   "insert: inUpDown %b, text '%s'\n",
                                   inUpDown, ed.getText()));
                }

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if(!inUpDown) {
                        setFindHistoryPrefix(ed.getText());
                    }
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
                        combo.getSelectedIndex(), ctx.getFilter()));
                if(combo.getItemCount() <= 0)
                    return;
                inUpDown = true;
                String val;
                if(e.getKeyCode() == KeyEvent.VK_UP) {
                    val = ctx.next();
                } else {
                    val = ctx.prev();
                }

                if(combo.isPopupVisible() == false) {
                    if(val == null) {
                        SwingUtilities.invokeLater(() -> Util.beep_flush());
                        return;
                    }
                    SwingUtilities.invokeLater(() -> combo.showPopup());
                }
                // there's only one shot at using a selection, to late now...
                setCaretPosition(getCaretPosition()); // clear any selection
                if(val != null) {
                    if(ctx.atTop()) {
                        setText(val);
                        SwingUtilities.invokeLater(() -> combo.hidePopup());
                    }else
                        combo.setSelectedItem(val);
                    return;
                }
                SwingUtilities.invokeLater(() -> Util.beep_flush());
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
        dbg.printf(() -> sf("CLINE: setText %s -->%s\n", getText(), s));
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
            InputEvent iev = (InputEvent)currentEvent;
            if(iev.isAltDown())     modifiers |= ActionEvent.ALT_MASK;
            if(iev.isControlDown()) modifiers |= ActionEvent.CTRL_MASK;
            if(iev.isShiftDown())   modifiers |= ActionEvent.SHIFT_MASK;
            if(iev.isMetaDown())    modifiers |= ActionEvent.META_MASK;
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent)currentEvent).getModifiers();
        }
        ActionEvent e =
            new ActionEvent(combo.getEditor(), ActionEvent.ACTION_PERFORMED,
                            (myCommand != null) ? myCommand : getText(),
                            EventQueue.getMostRecentEventTime(), modifiers);

        dbg.printf(() -> sf("CLINE: fireActionPerformed: %s\n", dumpEvent(e)));
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
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    String cstat(String tag)
    {
        int i = combo.getSelectedIndex();
        int n = combo.getItemCount();
        String item = i >= 0 && i < n ? combo.getItemAt(i) : "OOB";
        String last = n > 0 ? combo.getItemAt(combo.getItemCount()-1) : "---";

        String s = sf("%s: n %d, i %d, item '%s', last '%s'\n",
                      tag, n, i, item, last);
        System.err.printf(s);
        return s;
    }

} // end com.raelity.jvi.swing.CommandLine
