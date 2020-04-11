/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2019 Ernie Rael.  All Rights Reserved.
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

import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.GetChar;
import com.raelity.jvi.core.Normal;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.lib.MyEventListenerList;
import com.raelity.jvi.manager.ViManager;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import com.raelity.jvi.options.*;

import static com.raelity.jvi.lib.LibUtil.dumpEvent;
import static com.raelity.text.TextUtil.sf;

// inner classes ...........................................................

/**
 * The CommandLine JPanel may be embedded in a Dialog or put on a glass
 * pane; CommandLineEntry is some common handling particularly for
 * the ViCmdEntry interface.
 */
public abstract class CommandLineEntry implements ViCmdEntry
{
    private static DebugOption dbg = Options.getDebugOption(Options.dbgSearch);
    /** result of last entry */
    protected String lastCommand;
    protected MyEventListenerList ell = new MyEventListenerList();
    protected ViCmdEntry.Type entryType;
    protected CommandLine commandLine;
    protected ViTextView tv;
    protected String initialText;
    private boolean forceEvents;

    CommandLineEntry(ViCmdEntry.Type type)
    {
        entryType = type;
        commandLine = new CommandLine();
        commandLine.setupBorder();
        commandLine.addActionListener(this::finishUpEntry);
        commandLine.setMode(entryType == ViCmdEntry.Type.COLON ? ":" : "/");
        if (ViManager.getOsVersion().isMac()) {
            // MAC needs a little TLC since the selection is changed
            // in JTextComponent after adding text and before focusGained
            //    init set: dot: 5 mark:5
            //    init: dot: 5 mark:5
            //    focusGained: dot: 5 mark:0
            //    focusGained set: dot: 5 mark:5
            // This class sets selection back as needed
            focusSetSelection = new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e)
                {
                    JTextComponent tc = (JTextComponent) e.getSource();
                    tc.removeFocusListener(focusSetSelection);
                    Caret c = tc.getCaret();
                    try {
                        tc.setCaretPosition(commandLine.getMark());
                        tc.moveCaretPosition(commandLine.getDot());
                    } catch (IllegalArgumentException ex) {
                        // sometimes get exception, see
                        // https://netbeans.org/bugzilla/show_bug.cgi?id=223853
                        String msg = String.format(
                                "mark=%d, dot=%d, caret=%d, text=%s",
                                commandLine.getMark(), commandLine.getDot(),
                                c.getDot(), tc.getText());
                        CommandLine.LOG.info(msg);
                        // new IllegalArgumentException(msg, ex)
                        //         .printStackTrace();
                    }
                }
            };
        }
    }
    
    /** ViCmdEntry interface */
    @Override
    public final void activate(String mode, ViTextView parent)
    {
        activate(mode, parent, "", false);
    }

    /**
     * Start taking input. As a side effect of invoking this,
     * the variable {@line #commandLineWindow} is set.
     */
    @Override
    public final void activate(String mode, ViTextView tv,
                               String initialText, boolean passThru)
    {
        this.tv = tv;
        this.initialText = initialText;
        commandLine.getTextComponent().removeFocusListener(focusSetSelection);
        lastCommand = "";
        if (passThru) {
            Options.kd().println("cmdLine: activate PASSTHRU"); //REROUTE
            this.tv = null; // typically done as part of finish up
            lastCommand = initialText;
            forceEvents = true; // The HACK goes on
            try {
                fireEvent(new ActionEvent(tv.getEditor(),
                                          ActionEvent.ACTION_PERFORMED, "\n"));
            } finally {
                forceEvents = false;
            }
            commandLine.makeTop(initialText);
            return;
        }
        Font f = tv.getEditor().getFont();
        commandLine.setupFont(f);
        commandLine.setMode(mode);
        commandLine.init(initialText);
        commandLine.getTextComponent().addFocusListener(focusSetSelection);
        finishActivate();
    }

    @Override
    public void setHistory(List<String> l)
    {
        commandLine.SetHistory(l);
    }

    @Override
    public void makeTop(String s)
    {
        commandLine.makeTop(s);
    }

    @Override
    public List<String> getHistory()
    {
        return commandLine.getHistory();
    }

    @Override
    public List<HistEntry> getHistEntrys()
    {
        return commandLine.getHistEntrys();
    }


    /**
     *  When the command line gets focus, there's some work to do
     *  to handle MAC bug.
     */
    private FocusListener focusSetSelection;

    @Override
    public void append(char c)
    {
        if (c == '\n') {
            fireEvent(new ActionEvent(tv.getEditor(),
                                      ActionEvent.ACTION_PERFORMED, "\n"));
        } else {
            commandLine.append(c);
        }
    }

    protected abstract void finishActivate();

    private void shutdownEntry(boolean isCancel)
    {
        commandLine.getTextComponent().removeFocusListener(focusSetSelection);
        prepareShutdown();
        // A cancel is the result of external changes, including focus,
        // so don't throw in additional focus stuff
        if (!isCancel) {
            tv.getEditor().requestFocus();
        }
        tv = null;
    }

    protected abstract void prepareShutdown();

    /**
     * Calculate the bounds for the entry component relative to the root
     * component when placed immediately under the tv's editor.
     * @param root the root, typically a JFrame
     * @param entry the command entry widget
     * @return bounds of the widget within the root
     */
    protected Rectangle positionCommandEntry(Component root, Component entry)
    {
        Container jc = SwingUtilities.getAncestorOfClass(
                javax.swing.JScrollPane.class, tv.getEditor());
        if (jc == null) {
            jc = (JTextComponent) tv.getEditor();
        }
        Dimension d00 = entry.getPreferredSize();
        Rectangle pos = jc.getBounds(); // become bounds for commmand entry
        pos.translate(0, jc.getHeight()); // just beneath component
        pos.height = d00.height;
        // trim the width
        pos.width = Math.min(500, jc.getWidth());
        // now translate bounds so relative to root pane
        Point p00 = SwingUtilities.convertPoint(jc.getParent(), pos.x, pos.y, root);
        pos.setLocation(p00);
        // If past bottom of root pane, shift it up
        int offset = root.getHeight() - (pos.y + pos.height);
        if (offset < 0) {
            pos.translate(0, offset);
        }
        return pos;
    }

    @Override
    public String getCommand()
    {
        return lastCommand;
    }

    @Override
    public void cancel()
    {
        if (tv == null) {
            return;
        }
        lastCommand = "";
        shutdownEntry(true);
    }

    public JLabel getModeLabel()
    {
        return commandLine.getModeLabel();
    }

    @Override
    public JTextComponent getTextComponent()
    {
        return commandLine.getTextComponent();
    }

    public void finishUpEntry(ActionEvent e)
    {
        if (tv == null) {
            // There are cases where there are both
            //    CommandLine$SimpleEvent.actionPerformed
            // and
            //    CommandLine$2.actionPerformed (JComboBox.fireActionEvent)
            return;
        }
        try {
            // VISUAL REPAINT HACK
            // Repaint before executing commands..
            // so that I can be sure the visual area didn't change yet
            // and all has been repainted
            if (G.drawSavedVisualBounds()) {
                G.drawSavedVisualBounds(false);
                Normal.v_updateVisualState(tv);
            }
            // END VISUAL REPAINT HACK
            lastCommand = commandLine.getCommand();
            if (CommandLine.dbgKeys.getBoolean()) {
                CommandLine.dbgKeys.println("CommandAction: '" + lastCommand + "'");
            }
            shutdownEntry(false);
            fireEvent(e);
        } catch (Exception ex) {
            CommandLine.LOG.log(Level.SEVERE, null, ex);
        }
    }

    /** Send the event If it is a successful entry, a CR, then
     * record the input. Note that initial Text is not part of
     * the recorded input.
     */
    protected void fireEvent(ActionEvent e)
    {
        // The action command must be present and have a character in it
        // This isn't always the case on a MAC when an ESC is entered
        String s = e.getActionCommand();
        if (s == null || s.isEmpty()) {
            // treat a poorly formed event as an ESC
            // see http://sourceforge.net/tracker/?func=detail&atid=103653&aid=3527153&group_id=3653
            //    ESC to exit search leaves editor in an unusable state - ID: 3527153
            e = new ActionEvent(e.getSource(), e.getID(),
                                "\u001b", e.getModifiers());
        }
        if (e.getActionCommand().charAt(0) == '\n') {
            StringBuffer sb = new StringBuffer();
            if (!initialText.isEmpty() && lastCommand.startsWith(initialText)) {
                sb.append(lastCommand.substring(initialText.length()));
            } else {
                sb.append(lastCommand);
            }
            sb.append('\n');
            GetChar.userInput(new String(sb));
        }
        final ActionEvent fev = e;
        dbg.printf(() -> sf("CLINE: fireEvent: %s: %s\n",
                            ActionListener.class, dumpEvent(fev)));
        fireEllEvent(ActionListener.class, fev);
    }

    private void fireEllEvent(Class<?> clazz, ActionEvent e)
    {
        if(forceEvents || commandLine.isFiringEvents())
            ell.fire(clazz, e);
    }

    private void fireEllEvent(Class<?> clazz, ChangeEvent e)
    {
        if(commandLine.isFiringEvents())
            ell.fire(clazz, e);
    }

    @Override
    public void addActionListener(ActionListener l)
    {
        ell.add(ActionListener.class, l);
    }

    @Override
    public void removeActionListener(ActionListener l)
    {
        ell.remove(ActionListener.class, l);
    }
    private DocumentListener dl;

    private class DocListener implements DocumentListener
    {

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        dbg.printf(() -> sf("CLINE: insertUpdate: %s\n", getTextComponent().getText()));
        fireEllEvent(ChangeListener.class, new ChangeEvent(getTextComponent()));
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        dbg.printf(() -> sf("CLINE: removeUpdate: %s\n", getTextComponent().getText()));
        fireEllEvent(ChangeListener.class, new ChangeEvent(getTextComponent()));
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
    }
    }

    @Override
    public void addChangeListener(ChangeListener l)
    {
        dbg.printf(() -> sf("CLINE: addChangeListener: count %d\n", ell.getListenerCount(ChangeListener.class)));
        if (ell.getListenerCount(ChangeListener.class) == 0) {
            dl = new DocListener();
            getTextComponent().getDocument().addDocumentListener(dl);
        }
        ell.add(ChangeListener.class, l);
    }

    @Override
    public void removeChangeListener(ChangeListener l)
    {
        ell.remove(ChangeListener.class, l);
        if (ell.getListenerCount(ChangeListener.class) == 0) {
            getTextComponent().getDocument().removeDocumentListener(dl);
            dl = null;
        }
    }

    @Override
    public String getCurrentEntry()
    {
        return getTextComponent().getText();
    }
    
} // end inner CommandLineEntry
