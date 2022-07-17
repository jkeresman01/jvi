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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.TextAction;

import com.raelity.jvi.*;
import com.raelity.jvi.core.*;
import com.raelity.jvi.options.DebugOption;


import com.raelity.jvi.core.CommandHistory.HistoryContext;
import com.raelity.jvi.core.CommandHistory.InitialHistoryItem;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.core.lib.Constants.SICL;
import com.raelity.jvi.manager.*;

import static java.util.logging.Level.*;

import static com.raelity.jvi.core.G.p_sicl;
import static com.raelity.jvi.core.Misc01.beep_flush;
import static com.raelity.jvi.manager.ViManager.eatme;
import static com.raelity.jvi.lib.TextUtil.sf;

/**
 * This class presents a editable text field for picking up command entry
 * data. A mode, usually a single character like "/", "?", or ":" can be
 * set for label. Support history of commands traversed by UP/DOWN and
 * displayed in the text field. See {@link HistoryContext}. ^R in the
 * command line is handled.
 * This component builds a keymap through which the action events are
 * delivered.
 * <p>
 * Take steps to prevent this component from taking focus
 * through the focus manager. But ultimately need to subclass the
 * editor component to handle it.
 */
@SuppressWarnings("serial")
final class BasicCommandLine
extends JPanel
implements SwingCommandLine
{
    static final Logger LOG
            = Logger.getLogger(BasicCommandLine.class.getName());
    private static final DebugOption dbg = Options.getDebugOption(Options.dbgSearch);
    private final JLabel modeLabel = new JLabel();
    private final CommandLineTextField text = getNewTextField();
    private final GridBagLayout gridBagLayout1 = new GridBagLayout();
    private String mode;
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
    private HistoryContext ctx;
    private int gotCtrlR;

    public BasicCommandLine()
    {
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
            modeLabel.setText("");
            this.setLayout(gridBagLayout1);
            this.add(modeLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
            this.add(text, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
        } catch ( Exception ex ) {
            LOG.log(SEVERE, null, ex);
        }
        Font font = modeLabel.getFont();
        modeLabel.setFont(new Font("Monospaced",
                                   Font.BOLD,
                                   font.getSize()));
        font = text.getFont();
        text.setFont(new Font("Monospaced", font.getStyle(), font.getSize()));

        setMode(" ");
        setKeymap();
        // allow tabs to be entered into text field
        Component c = getTextComponent();
        Set<AWTKeyStroke> set = Collections.emptySet();
        c.setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set);
        c.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);
        //setupBorder();
        this.setBorder(BorderFactory.createLineBorder(Color.black, 2));

    }

    @Override
    public JComponent get()
    {
        return this;
    }

    /** this event sends the current values, should be a no-op,
     * but NB completion uses a caret event to refresh the list. */
    @Override
    public void fireCaretEvent(CaretEvent event)
    {
        JTextComponent jtc = getTextComponent();
        ((CommandLineTextField)jtc).fireCaretUpdate(event);
    }

    /** mac bug fixup */
    @Override
    public int[] getMacFixupDotMark()
    {
        return new int[] { dot, mark };
    }

    /** Label with single character: ":", "/", "?" */
    @Override
    public void setupFont(Font srcFont)
    {
        modeLabel.setFont(srcFont.deriveFont(Font.BOLD));
        text.setFont(srcFont);
    }

    @SuppressWarnings("FieldMayBeFinal")
    /**
     * This is used to initialize the text, needed so that
     * characters entered before it gets focus are not lost.
     * And then there's passthru.
     */
    @Override
    public void init( String s )
    {
        InitialHistoryItem initalState = ctx.init();
        gotCtrlR = -1;
        commandLineFiringEvents = true;
        dbg.printf(FINE, "CLINE: init: s=%s, commandLineFiringEvents true\n", s);
        dot = mark = 0;
        if ( s.length() == 0 ) {
            SICL sicl = p_sicl();
            JTextComponent tc = getTextComponent();
            String t = "";
            if(mode.equals(":") && sicl != SICL.EMPTY
                    || !initalState.isAtBeginning()) {
                // Only put previous value in text field if colon command,
                // or if selected from history;
                // this avoids an unintended search.
                t = initalState.getInitialItem();
            }
            tc.setText(t);
            if(t.length() > 0 && initalState.isAtBeginning()
                    && sicl == SICL.SELECTED) {
                mark = 0;
                dot = t.length();
                tc.setCaretPosition(mark);
                tc.moveCaretPosition(dot);
            }
        } else {
            try {
                Document doc = getTextComponent().getDocument();
                mark = dot = s.length();
                doc.remove(0,doc.getLength());
                doc.insertString(0, s, null);
            } catch ( BadLocationException ex ) { }
        }
        dbg.printf(FINE, () -> sf("CLINE: init: end: text=%s\n",
                            getTextComponent().getText()));

        setFindHistoryPrefix(s);
    }

    private void setFindHistoryPrefix(String s) {
        ctx.setFilter(s);
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

    @Override
    public void takeFocus( boolean flag )
    {
        if ( flag ) {
            text.setEnabled(true);
            // NEEDSWORK: FOCUS: use requestFocusInWindow()
            text.requestFocus();
        } else {
            text.setEnabled(false);
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
    public void setMode( String newMode )
    {
        mode = newMode;
        modeLabel.setText(" " + mode + " ");
    }

    @Override
    public String getMode()
    {
        return mode;
    }

    @Override
    final public  JTextComponent getTextComponent()
    {
        return text;
    }

    /**
     * Install the associated history.
     */
    @Override
    public void SetHistory(HistoryContext ctx)
    {
        this.ctx = ctx;
        ctx.init();
    }
    
    /**
     *  Make the argument command the top of the list.
     *  First remove it.
     */
    @Override
    public void makeTop( String command )
    {
        // TODO: Could do this from CommandLineEntry directly to history.
        //       Then get rid of makeTop in SwingCommandLine.
        //       Would have to put the command string in event.
        //       Then history would be read only from commandLine
        //       (but note search prefix informs history)

        // if the empty string was selected we're done
        if(command.isEmpty()) {
            return;
        }
        // save the command in the history
        ctx.push(command);
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
                    EXECUTE_KEY, // This is a "super keycombo", never typed
                    ACT_FINISH),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_ENTER, 0),
                    ACT_FINISH),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_ESCAPE, 0),
                    ACT_FINISH),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK),
                    ACT_FINISH),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
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
                        // other things, like ESC and Ctrl-[, are ignored
                        String actionCommand = e.getActionCommand();
                        commandLineComplete(actionCommand);
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
            LOG.log(SEVERE, null, e);
        }
        return localActions;
    }

    @Override
    public boolean isFiringEvents() {
        return commandLineFiringEvents;
    }


    /**
     * Fire event which is expected to complete the command.
     * Do some maintenance on the LRU history.
     */
    protected void commandLineComplete(String eventCommand)
    {
        // only run this once, sometimes get event from combo and keys
        dbg.printf(FINE, () -> sf("CLINE: fireCommandLineActionPerformed: %s\n",
                          eventCommand));
        if(!commandLineFiringEvents)
            return;

        String command = getTextComponent().getText();
        dbg.printf(FINE, () -> sf("CLINE: commandLineFiringEvents false cmd=%s\n", command));
        // turn off events for makeTop
        commandLineFiringEvents = false;

        // Maintain LRU history
        makeTop(command);

        // back from makeTop, events back on
        dbg.printf(FINE, "CLINE: commandLineFiringEvents true for finishing events\n");
        commandLineFiringEvents = true;

        try {
            // post commandLineComplete this, eventCommand
            ViCmdEntry.getEventBus().post( new SwingCommandLine
                    .CommandLineComplete(this, eventCommand, mode));
        } finally {
            commandLineFiringEvents = false;
        }
        dbg.printf(FINE, "CLINE: commandLineFiringEvents false until next time\n");
    }

    private CommandLineTextField getNewTextField()
    {
        CommandLineTextField ed = new CommandLineTextField("",9);
        ed.setBorder(null);
        ed.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                if(!inUpDown) {
                    setFindHistoryPrefix(ed.getText());
                }
                dbgKeys.printf(FINER, ()->sf(
                        "insert: inUpDown %b, text '%s'\n",
                                   inUpDown, ed.getText()));
            }
            
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                if(!inUpDown) {
                    setFindHistoryPrefix(ed.getText());
                }
                dbgKeys.printf(FINER, ()->sf(
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


    //////////////////////////////////////////////////////////////////////
    //
    // CLASS commandLineTextField
    //

    final static StyleContext context = StyleContext.getDefaultStyleContext();
    final static AttributeSet attrRed = context.addAttribute(
            context.getEmptySet(), StyleConstants.Foreground, Color.RED);

    private class CommandLineTextField extends JTextField // JTextPane
    {
    StringBuilder sb = new StringBuilder();

    Color fg;
    private void saveColorFg() {
        Object o = this;
        if(!(o instanceof JTextPane)) {
            fg = getForeground();
            setForeground(Color.RED);
        }
    }
    private void restorFg() {
        Object o = this;
        if(!(o instanceof JTextPane)) {
            setForeground(fg);
        }
    }

    CommandLineTextField(String value,int n) {
        // TODO: get rid of this, incorporate into createCmdEntry, builder pattern DOC
        super(((SwingFactory)ViManager.getFactory()).createCmdEntryDoc(),
              value, n);
        //super(new DefaultStyledDocument());
        //setText("");
    }

    @Override
    protected void fireCaretUpdate(CaretEvent e)
    {
        super.fireCaretUpdate(e);
    }

    /**
     * Handle KEY_TYPED Ctrl_R for yankreg insertion,
     * and KEY_PRESSED UP/DOWN for history traversal.
     */
    @Override
    protected void processKeyEvent(KeyEvent e)
    {
        if(e.getID() == KeyEvent.KEY_TYPED) {
            if(gotCtrlR >= 0) {
                Document doc = getDocument();
                if(doc != null) {
                    try {
                        restorFg();
                        doc.remove(gotCtrlR, 1);
                        String s = ctx.get_register(e.getKeyChar());
                        if(s != null && !s.isEmpty()) {
                            sb.setLength(0);
                            for(int i = 0; i < s.length(); i++) {
                                char c = s.charAt(i);
                                if(c >= ' ')
                                    sb.append(c);
                            }
                            doc.insertString(gotCtrlR, sb.toString() , null);
                        } else
                            EventQueue.invokeLater(() -> beep_flush());
                    } catch(BadLocationException ex) {
                            EventQueue.invokeLater(() -> beep_flush());
                    }
                }
                gotCtrlR = -1;
                return;
            }
            if(e.getKeyChar() == CtrlChars.CTRL_R) {
                Document doc = getDocument();
                if(doc != null) {
                    try {
                        int temp = getCaretPosition();
                        saveColorFg();
                        doc.insertString(temp, "\"", attrRed);
                        setCaretPosition(temp);
                        gotCtrlR = temp;
                    } catch(BadLocationException ex) {
                        EventQueue.invokeLater(() -> beep_flush());
                    }
                }
                return;
            }
        }
        if(gotCtrlR >= 0)
            return;
        if (e.getID() == KeyEvent.KEY_PRESSED
                && (e.getKeyCode() == KeyEvent.VK_UP
                    || e.getKeyCode() == KeyEvent.VK_DOWN)) {
            try {
                inUpDown = true;
                String val;
                if(e.getKeyCode() == KeyEvent.VK_UP) {
                    val = ctx.next();
                } else {
                    val = ctx.prev();
                }
                
                // there's only one shot at using a selection, to late now...
                setCaretPosition(getCaretPosition()); // clear any selection
                if(val != null) {
                    setText(val);
                    return;
                }
                SwingUtilities.invokeLater(() -> Misc01.beep_flush());
                return;
            } finally {
                inUpDown = false;
            }
        }
        super.processKeyEvent(e);
    }
    } // END CLASS commandLineTextField /////////////////////////////////////

}
