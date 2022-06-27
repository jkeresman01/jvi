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
import com.raelity.jvi.manager.ViManager;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import com.google.common.eventbus.Subscribe;

import com.raelity.jvi.core.CommandHistory.HistoryContext;
import com.raelity.jvi.options.*;

import static java.util.logging.Level.*;

import static com.raelity.jvi.core.lib.Constants.ESC_STR;
import static com.raelity.text.TextUtil.sf;

// inner classes ...........................................................

/**
 * The CommandLine JPanel may be embedded in a Dialog or put on a glass
 * pane; CommandLineEntry is some common handling particularly for
 * the ViCmdEntry interface.
 */
public abstract class CommandLineEntry implements ViCmdEntry
{
    static final Logger LOG
            = Logger.getLogger(CommandLineEntry.class.getName());
    private static final DebugOption dbg = Options.getDebugOption(Options.dbgSearch);
    static final DebugOption dbgKeys
            = Options.getDebugOption(Options.dbgKeyStrokes);

    final SwingCommandLine commandLine;

    /** result of last entry */
    protected String lastCommand;
    protected ViTextView tv;
    protected String initialText;
    private boolean forceEvents;

    CommandLineEntry(ViCmdEntry.Type type)
    {
        commandLine = SwingCommandLine.getNewDefault();
        commandLine.setMode(type == ViCmdEntry.Type.COLON ? ":" : "/");
        getTextComponent().getDocument().addDocumentListener(dl);

        ViCmdEntry.getEventBus().register(new Object()
        {
            @Subscribe public void commandLineDone(
                    SwingCommandLine.CommandLineComplete ev)
            {
                if(ev.getSource() == commandLine)
                    finishUpEntry(ev);
            }
        });

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
                    int[] macFixup = commandLine.getMacFixupDotMark();
                    int dot = macFixup[0];
                    int mark = macFixup[1];
                    try {
                        tc.setCaretPosition(mark);
                        tc.moveCaretPosition(dot);
                    } catch (IllegalArgumentException ex) {
                        // sometimes get exception, see
                        // https://netbeans.org/bugzilla/show_bug.cgi?id=223853
                        String msg = String.format(
                                "mark=%d, dot=%d, caret=%d, text=%s",
                                mark, dot,
                                c.getDot(), tc.getText());
                        LOG.info(msg);
                        // new IllegalArgumentException(msg, ex)
                        //         .printStackTrace();
                    }
                }
            };
        }
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
                fireActionEvent("\n");
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
    public void firePlatformEvent(Class<?> target, Object event, String msg)
    {
        // TODO: if JTextComponent
        // TODO: verify caretevent
        commandLine.fireCaretEvent((CaretEvent)event);
    }

    

    @Override
    public void setHistory(HistoryContext ctx)
    {
        commandLine.SetHistory(ctx);
    }

    @Override
    public void makeTop(String s)
    {
        commandLine.makeTop(s);
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
            fireActionEvent("\n");
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
        // should be mono font, 'w' and '.' same width
        JTextComponent t = commandLine.getTextComponent();
        int charWidth = t.getFontMetrics(t.getFont()).charWidth('w');
        // full width of editor, but at least 80 columns
        pos.width = Math.max(charWidth * 80, jc.getWidth());
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

    @Override
    public final JTextComponent getTextComponent()
    {
        return commandLine.getTextComponent();
    }

    @Override
    public String getCurrentEntry()
    {
        return getTextComponent().getText();
    }

    public void finishUpEntry(SwingCommandLine.CommandLineComplete ev)
    {
        dbg.printf(INFO, () -> sf("CMDLINE: finishUpEntry: %s\n", ev.toString()));
        finishUpEntry(ev.getActionCommand());
    }

    public void finishUpEntry(String actionCommand)
    {
        if (tv == null) {
            // protect against extra component events
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
            dbgKeys.println(() -> "CommandAction: '" + lastCommand + "'");
            shutdownEntry(false);

            if (actionCommand == null || actionCommand.isEmpty()) {
                // MAC issue; handle a poorly formed event as an ESC.
                // ESC to exit search leaves editor in an unusable state
                // https://sourceforge.net/p/jvi/bugs/168/
                actionCommand = ESC_STR;
            }
            handleGetCharRecording(actionCommand);
            fireActionEvent(actionCommand);
        } catch (Exception ex) {
            LOG.log(SEVERE, null, ex);
        }
    }

    /** If a successful entry, a CR, then
     * record the input. Note that initial Text is not part of
     * the recorded input.
     */
    private void handleGetCharRecording(String actionCommand)
    {
        if (actionCommand.charAt(0) == '\n') {
            StringBuilder sb = new StringBuilder();
            if (!initialText.isEmpty() && lastCommand.startsWith(initialText)) {
                sb.append(lastCommand.substring(initialText.length()));
            } else {
                sb.append(lastCommand);
            }
            sb.append('\n');
            GetChar.userInput(new String(sb));
        }
    }

    private void fireActionEvent(String actionCommand)
    {
        if(forceEvents || commandLine.isFiringEvents())
            ViCmdEntry.getEventBus().post(
                    new CmdEntryCompleteImpl(actionCommand, commandLine.getMode()));
    }

    private void fireTextChange()
    {
        ViCmdEntry.getEventBus().post(
                new ViCmdEntry.CmdEntryChange(getTextComponent()));
    }
    private final DocumentListener dl = new DocListener();

        private class DocListener implements DocumentListener
        {

        @Override
        public void insertUpdate(DocumentEvent e)
        {
            dbg.printf(FINE, () -> sf("CLINE: insertUpdate: %s\n", getTextComponent().getText()));
            fireTextChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e)
        {
            dbg.printf(FINE, () -> sf("CLINE: removeUpdate: %s\n", getTextComponent().getText()));
            fireTextChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e)
        {
        }
        } // END CLASS

        private class CmdEntryCompleteImpl extends CmdEntryComplete
        {
        public CmdEntryCompleteImpl(String actionCommand, String tag)
        {
            super(CommandLineEntry.this, actionCommand, tag);
        }
        } // END CLASS
} // end inner CommandLineEntry
