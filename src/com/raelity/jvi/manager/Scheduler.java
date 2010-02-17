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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.manager;

import com.raelity.jvi.ViCaret;
import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Hook;
import com.raelity.jvi.core.KeyDefs;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Options;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Scheduler extends ViManager
{
    private static Logger LOG = Logger.getLogger(Scheduler.class.getName());
    private static Component currentEditorPane;
    private static boolean started = false;
    private static ViCmdEntry activeCommandEntry;
    //////////////////////////////////////////////////////////////////////
    //
    // Mouse interactions
    //
    private static boolean draggingBlockMode;
    private static boolean mouseDown;
    private static boolean hasSelection;
    private static MouseListener mouseListener = new MouseListener() {
        public void mouseClicked(MouseEvent e)
        {
            mouseClick(e);
        }

        public void mousePressed(MouseEvent e)
        {
            mousePress(e);
        }

        public void mouseReleased(MouseEvent e)
        {
            mouseRelease(e);
        }

        public void mouseEntered(MouseEvent e)
        {
        }

        public void mouseExited(MouseEvent e)
        {
        }
    };
    private static MouseMotionListener mouseMotionListener =
            new MouseMotionListener() {
        public void mouseDragged(MouseEvent e)
        {
            mouseDrag(e);
        }

        public void mouseMoved(MouseEvent e)
        {
        }
    };

    /**
     * A key was typed. Handle the event.
     * <br>NEEDSWORK: catch all exceptions comming out of here?
     */
    public static void keyStroke(Component target, char key, int modifier)
    {
        ViManager.verifyNotBusy();
        try {
            ViManager.setJViBusy(true);
            switchTo(target);
            if (rerouteChar(key, modifier))
                return;
            fact().finishTagPush(G.curwin); // NEEDSWORK: cleanup
            core().gotc(key, modifier);
            if (G.curwin != null)
                G.curwin.getStatusDisplay().refresh();
        } finally {
            ViManager.setJViBusy(false);
        }
    }

    /**
     * If chars came in between the time a dialog was initiated and
     * the time the dialog starts taking the characters, we feed the
     * chars to the dialog.
     * <p>Special characters are discarded.
     * </p>
     */
    static boolean rerouteChar(char c, int modifiers)
    {
        if (activeCommandEntry == null)
            return false;
        if ((c & 61440) != KeyDefs.VIRT && modifiers == 0)
            if (c >= 32 && c != 127) {
                if (Options.isKeyDebug())
                    System.err.println("rerouteChar");
                activeCommandEntry.append(c);
            }
        return true;
    }

    /**
     * requestSwitch can be used from platform code for situation where an
     * editor is activated. It allows things to be initialized,
     * with some visual implications, before a key is entered.
     * It should typically only be used after {@linkplain #activateAppView}.
     */
    public static void requestSwitch(Component ed)
    {
        switchTo(ed);
    }

    static Component getCurrentEditor()
    {
        return currentEditorPane;
    }

    static void switchTo(Component editor)
    {
        if (editor == currentEditorPane)
            return;
        if (!started) {
            started = true;
            firePropertyChange(P_LATE_INIT, null, null);
        }
        ViManager.motdOutputOnce();
        exitInputMode(); // if switching, make sure prev out of input mode
        draggingBlockMode = false;
        ViTextView currentTv = null;
        if (currentEditorPane != null) {
            currentTv = mayCreateTextView(currentEditorPane);
            firePropertyChange(P_SWITCH_FROM_WIN, currentTv, null);
        }
        boolean newTextView = fact().getTextView(editor) == null;
        ViTextView textView = mayCreateTextView(editor);
        Buffer buf = textView.getBuffer();
        fact().setupCaret(editor); // make sure has the right caret
        textView.attach();
        if (G.dbgEditorActivation.getBoolean()) {
            String newStr = newTextView ? "NEW: " : "";
            System.err.println("Activation: ViManager.SWITCHTO: " + newStr +
                    cid(editor) + " " + buf.getDisplayFileName());
        }
        if (currentEditorPane != null) {
            core().abortVisualMode();
            // MOVED ABOVE: currentTv = mayCreateTextView(currentEditorPane);
            // Freeze and/or detach listeners from previous active view
            currentTv.detach();
        }
        currentEditorPane = editor;
        core().switchTo(textView, buf);
        core().resetCommand(); // Means something first time window switched to
        buf.activateOptions(textView);
        textView.activateOptions(textView);
        setHasSelection(); // a HACK
        if (newTextView) {
            firePropertyChange(P_OPEN_WIN, currentTv, textView);
            editor.addMouseListener(mouseListener);
            editor.addMouseMotionListener(mouseMotionListener);
        }
        if (textView.getBuffer().singleShare())
            firePropertyChange(P_OPEN_BUF,
                               currentTv == null ? null : currentTv.getBuffer(),
                               textView.getBuffer());
        firePropertyChange(P_SWITCH_TO_WIN, currentTv, textView);
        Msg.smsg(getFS().getDisplayFileViewInfo(textView));
    }

    public static ViTextView getCurrentTextView()
    {
        return fact().getTextView(currentEditorPane);
    }

    /**
     * The arg Component is detached from its text view,
     * forget about it.
     */
    public static void detached(Component ed)
    {
        if (currentEditorPane == ed) {
            if (G.dbgEditorActivation.getBoolean())
                System.err.println("Activation: ViManager.detached " + cid(ed));
            currentEditorPane = null;
        }
    }

    /**
     * Pass control to indicated ViCmdEntry widget. If there are
     * readahead or typeahead characters available, then collect
     * them up to a &lt;CR&gt; and append them to initialString.
     * If there was a CR, then signal the widget to immeadiately
     * fire its actionPerformed without displaying any UI element.
     */
    public static void startCommandEntry(ViCmdEntry commandEntry, String mode,
                                         ViTextView tv,
                                         StringBuffer initialString)
    {
        core().clearMsg();
        if (initialString == null)
            initialString = new StringBuffer();
        if (activeCommandEntry != null)
            throw new RuntimeException("activeCommandEntry not null");
        activeCommandEntry = commandEntry;
        boolean passThru;
        if (initialString.indexOf("\n") >= 0)
            passThru = true;
        else
            passThru = core().getRecordedLine(initialString);
        try {
            commandEntry.activate(mode, tv, new String(initialString), passThru);
        } catch (Throwable ex) {
            // NOTE: do not set the flag until the activate completes.
            // There have been cases of NPE.
            // Particularly in relationship to nomands.
            //
            // If modal, and everything went well, then activeCommandEntry is
            // already NULL. But not modal, then it isn't null.
            core().vim_beep();
            LOG.log(Level.SEVERE, null, ex);
            activeCommandEntry = null;
            core().resetCommand();
        }
    }

    public static void stopCommandEntry()
    {
        activeCommandEntry = null;
    }

    public static boolean isMouseDown()
    {
        return mouseDown;
    }

    static void setHasSelection()
    {
        ViTextView tv = getCurrentTextView();
        if (tv != null)
            hasSelection = tv.hasSelection();
    }

    public static void cursorChange(ViCaret caret)
    {
        if (G.curwin == null)
            return;
        boolean nowSelection = caret.getDot() != caret.getMark();
        if (hasSelection == nowSelection)
            return;
        core().uiCursorAndModeAdjust();
        hasSelection = nowSelection;
    }

    /**
     * A mouse press; switch to the activated editor.
     */
    private static void mousePress(MouseEvent mev)
    {
        try {
            setJViBusy(true);
            int mask =
                    MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK |
                    MouseEvent.BUTTON3_DOWN_MASK;
            if ((mev.getModifiersEx() & mask) != 0)
                mouseDown = true;
            if (Options.getOption(Options.dbgMouse).getBoolean())
                System.err.println("mousePress: " + (mouseDown ? "down " : "up ") +
                        MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(
                //                      mev.getModifiers()));
            core().flush_buffers(true);
            exitInputMode();
            if (currentEditorPane != null)
                core().abortVisualMode();
            Component editorPane = mev.getComponent();
            ViTextView tv = fact().getTextView(editorPane);
            if (tv == null)
                return;
            switchTo(editorPane);
        } finally {
            setJViBusy(false);
        }
    }

    /**
     * A mouse click.
     * Pass the click on to the window and give it
     * a chance to adjust the position and whatever.
     *
     * NOTE: isMouseDown is false in swing when this method invoked.
     */
    public static void mouseClick(MouseEvent mev)
    {
        if (mev.getComponent() != currentEditorPane)
            return;
        try {
            setJViBusy(true);
            ViTextView tv = fact().getTextView(currentEditorPane);
            int pos = tv.getCaretPosition();
            int newPos = tv.validateCursorPosition(pos);
            if (pos != newPos)
                tv.setCaretPosition(newPos);
            if (Options.getOption(Options.dbgMouse).getBoolean())
                System.err.println("mouseClick(" + pos + ") " +
                        MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(
                //                      mev.getModifiers()));
            return;
        } finally {
            setJViBusy(false);
        }
    }

    public static void mouseRelease(MouseEvent mev)
    {
        try {
            setJViBusy(true);
            int mask =
                    MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK |
                    MouseEvent.BUTTON3_DOWN_MASK;
            if ((mev.getModifiersEx() & mask) == 0)
                mouseDown = false;
            if (Options.getOption(Options.dbgMouse).getBoolean())
                System.err.println("mouseRelease: " +
                        MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(
                //                      mev.getModifiers()));
        } finally {
            setJViBusy(false);
        }
    }

    public static void mouseDrag(MouseEvent mev)
    {
        if (mev.getComponent() != currentEditorPane)
            return;
        try {
            setJViBusy(true);
            //
            // Don't automatically go into visual mode on a drag,
            // vim does "SELECT" mode.
            // But when in select mode would like to extend selection on arrow keys,
            // which is also like vim.
            //
            // if(pos != G.curwin.getCaretPosition() && !G.VIsual_active) {
            //   G.VIsual_mode ='v';
            //   G.VIsual_active = true;
            //   G.VIsual = (FPOS) G.curwin.getWCursor().copy();
            //   Misc.showmode();
            // }
            if (Options.getOption(Options.dbgMouse).getBoolean())
                System.err.println("mouseDrag " + MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(mev.getModifiers()));
            return;
        } finally {
            setJViBusy(false);
        }
    }

    private static ViFactory fact()
    {
        return ViManager.getViFactory();
    }

    private static Hook core()
    {
        return ViManager.getCore();
    }
}
