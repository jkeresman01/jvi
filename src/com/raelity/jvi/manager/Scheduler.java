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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raelity.jvi.*;
import com.raelity.jvi.core.*;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.core.lib.KeyDefs.KeyStrokeType;
import com.raelity.jvi.options.*;

import static java.util.logging.Level.*;

import static com.raelity.jvi.core.G.dbgEditorActivation;
import static com.raelity.jvi.manager.ViManager.*;
import static com.raelity.text.TextUtil.sf;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Scheduler
{
    private static final Logger LOG = Logger.getLogger(Scheduler.class.getName());
    private static Component currentEditorPane;
    private static boolean started = false;
    private static ViCmdEntry activeCommandEntry;
    private static boolean draggingBlockMode;
    private static boolean mouseDown;
    private static boolean hasSelection;
    private static final Queue<ActionListener> keyStrokeTodo
            = new LinkedList<>();

    private Scheduler()
    {
    }

    static void switchTo(Component editor) // NEEDSWORK: make sure appview sync
    {
        if (editor == currentEditorPane)
            return;
        motdOutputOnce();
        if (!started) {
            started = true;
            firePropertyChange(P_LATE_INIT, null, null);
        }

        AppViews.deactivateCurrent(true);

        draggingBlockMode = false;
        eatme(draggingBlockMode);
        ViTextView currentTv = null;
        if (currentEditorPane != null) {
            currentTv = mayCreateTextView(currentEditorPane);
            firePropertyChange(P_SWITCH_FROM_TV, currentTv, null);
        }

        boolean fNewTextView = fact().getTextView(editor) == null;
        boolean fNewBuffer = fact().getBuffer(editor) == null;
        ViTextView textView = mayCreateTextView(editor);
        Buffer buf = textView.getBuffer();
        fact().setupCaret(editor); // make sure has the right caret
        textView.attach();
        dbgEditorActivation().println(INFO, () ->
                "Activation: ViManager.SWITCHTO: "
                + (fNewTextView ? "NEW: " : "") + cid(editor)
                + " " + buf.getDebugFileName() + " " + ViManager.cid(buf));
        if (currentEditorPane != null) {
            assert currentTv != null;
            currentTv.detach();
        }

        currentEditorPane = editor;
        // change curwin/curbuf
        getCore().switchTo(textView, buf, false);
        getCore().resetCommand(false); // Means something first time window switched to
        buf.activateOptions(textView);
        textView.activateOptions(textView);
        setHasSelection(); // a HACK

        ViAppView av = fact().getAppView(editor);
        AppViews.activate(av);

        // all switched up, notify 
        if (fNewTextView) {
            editor.addMouseListener(mouseListener);
            editor.addMouseMotionListener(mouseMotionListener);
            firePropertyChange(P_OPEN_TV, currentTv, textView);
        }
        if (fNewBuffer)
            firePropertyChange(P_OPEN_BUF,
                               currentTv == null ? null : currentTv.getBuffer(),
                               textView.getBuffer());

        firePropertyChange(P_SWITCH_TO_TV, currentTv, textView);

        // This final switchTo does some visual mode display stuff,
        // curbuf/curwin are already the new values.
        // But maybe it should be before the event notification?
        getCore().switchTo(textView, buf, true);
    }

    private static final FocusListener focusSwitcher = new FocusAdapter()
    {
        @Override
        public void focusGained(FocusEvent e)
        {
            Component c = e.getComponent();
            if(c != null) {
                // Bug 181490 -  JEditorPane gets focus after TC is closed
                if(c.isDisplayable()) // insure component has a peer
                    switchTo(c);
            }
        }
    };

    static void changeBuffer(ViTextView tv)
    {
        if(G.curwin() == tv) {
            getCore().switchTo(tv, tv.getBuffer(), false);
            getCore().switchTo(tv, tv.getBuffer(), true);
        }
    }

    // NEEDSWORK: register should not be public. This is public because of the
    //          lazy app views. The editor can get focus before the
    //          app view is full populated.
    //          For now NbFactory.setupCaret calls this directly.
    //          The factory could be given some special hooks.
    public static void register(Component c)
    {
        if(c != null) {
            if (fact() != null) {
                dbgEditorActivation().println(CONFIG, () ->
                        "Activation: Scheduler.register: " + cid(c));
            }
            c.removeFocusListener(focusSwitcher);
            c.addFocusListener(focusSwitcher);
        }
    }

    static Component getCurrentEditor()
    {
        return currentEditorPane;
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
            dbgEditorActivation().println(INFO, () ->
                    "Activation: ViManager.detached " + cid(ed));
            currentEditorPane = null;
        }
    }

    // CHECKME use Runnable instead of action listener
    public static void putKeyStrokeTodo(ActionListener act)
    {
        keyStrokeTodo.add(act);
    }

    private static void runKeyStrokeTodo()
    {
        ActionEvent e = new ActionEvent(G.curwin(), 0, null);
        while(!keyStrokeTodo.isEmpty()) {
            keyStrokeTodo.remove().actionPerformed(e);
        }
    }

    /**
     * A key was typed. Handle the event.
     * <br>NEEDSWORK: catch all exceptions coming out of here?
     */
    public static void keyStroke(Component target, char key,
                                 int modifier, KeyStrokeType ksType)
    {
        if(activeCommandEntry == null) // don't check when reroute character
            verifyNotBusy();
        try {
            setJViBusy(true);
            switchTo(target);
            if (rerouteChar(key, modifier, ksType))
                return;
            if(!keyStrokeTodo.isEmpty() && G.curwin() != null)
                runKeyStrokeTodo();
            getCore().gotc(key, modifier, false);
        } finally {
            setJViBusy(false);
        }
        if (G.curwin() != null)
            G.curwin().getStatusDisplay().refresh();
    }

    /**
     * If chars came in between the time a dialog was initiated and
     * the time the dialog starts taking the characters, we feed the
     * chars to the dialog.
     * <p>Special characters are discarded.
     * </p>
     */
    static boolean rerouteChar(char c, int modifiers, KeyStrokeType ksType)
    {
        if (activeCommandEntry == null)
            return false;
        // Probably just checking for CHAR would be good enough
        if ((c & 0xf000) != KeyDefs.VIRT && ksType == KeyStrokeType.CHAR)
            if (c >= 32 && c != 127) {
                Options.kd().println("rerouteChar");
                activeCommandEntry.append(c);
            }
        return true;
    }

    //////////////////////////////////////////////////////////////////////
    //
    // Command Entry
    //

    /**
     * Pass control to indicated ViCmdEntry widget. If there are
     * read ahead or typeahead characters available, then collect
     * them up to a &lt;CR&gt; and append them to initialString.
     * If there was a CR, then signal the widget to immediately
     * fire its actionPerformed without displaying any UI element.
     */
    public static void startCommandEntry(ViCmdEntry commandEntry, String mode,
                                         ViTextView tv,
                                         StringBuffer initialString)
    {
        Msg.clearMsg();
        if (initialString == null)
            initialString = new StringBuffer();
        if (activeCommandEntry != null)
            throw new RuntimeException("activeCommandEntry not null");
        activeCommandEntry = commandEntry;
        StringBuffer initialStringF = initialString;
        Options.kd().printf(() ->
              sf("startCommandEntry: set ACE tv %s, '%s'\n", ViManager.cid(tv),
                                                             initialStringF)); //REROUTE
        boolean passThru;
        if (initialString.indexOf("\n") >= 0)
            passThru = true;
        else
            passThru = getCore().getRecordedLine(initialString);
        try {
            commandEntry.activate(mode, tv, new String(initialString), passThru);
        } catch (Throwable ex) {
            // NOTE: do not set the flag until the activate completes.
            // There have been cases of NPE.
            // Particularly in relationship to nomands.
            //
            // If modal, and everything went well, then activeCommandEntry is
            // already NULL. But not modal, then it isn't null.
            Options.kd().println("startCommandEntry: exception"); //REROUTE
            Util.vim_beep();
            LOG.log(Level.SEVERE, null, ex);
            activeCommandEntry = null;
            getCore().resetCommand(false);
        }
    }

    public static void stopCommandEntry(ViCmdEntry ce)
    {
        if(ce != activeCommandEntry) {
            LOG.log(Level.SEVERE, null,
                    new IllegalStateException("wrong command entry"));
            return;
        }
        if(activeCommandEntry == null)
            return;
        Options.kd().println("StopCommandEntry"); //REROUTE
        internalStopCommandEntry();
    }

    private static void internalStopCommandEntry()
    {
        activeCommandEntry.cancel();
        activeCommandEntry = null;
    }

    static void cancelCommandEntry()
    {
        if(activeCommandEntry == null)
            return;
        dbgEditorActivation().println(CONFIG, "cancelCommandEntry"); //REROUTE
        internalStopCommandEntry();
    }

    //////////////////////////////////////////////////////////////////////
    //
    // Mouse related
    //

    static void setHasSelection()
    {
        ViTextView tv = getCurrentTextView();
        if (tv != null)
            hasSelection = tv.hasSelection();
    }

    public static void cursorChange(ViCaret caret)
    {
        if (G.curwin() == null) {
            return;
        }
        boolean nowSelection = caret.getDot() != caret.getMark();
        if (hasSelection == nowSelection) {
            return;
        }
        getCore().uiCursorAndModeAdjust();
        hasSelection = nowSelection;
    }

    public static boolean isMouseDown()
    {
        return mouseDown;
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
            final DebugOption dbg = Options.getDebugOption(Options.dbgMouse);
            dbg.println(Level.FINE, () ->
                    "mousePress: " + (mouseDown ? "down " : "up ")
                            + MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(
                //                      mev.getModifiers()));
            getCore().flush_buffers(true);
            exitInputMode();
            if (currentEditorPane != null)
                getCore().abortVisualMode();
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
            TextView tv = (TextView)fact().getTextView(currentEditorPane);
            int pos = tv.getCaretPosition();
            int newPos = tv.validateCursorPosition(pos);
            if (pos != newPos)
                tv.w_cursor.set(newPos);
            final DebugOption dbg = Options.getDebugOption(Options.dbgMouse);
            dbg.println(Level.FINE, () -> "mouseClick(" + pos + ") "
                    + MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(
                //                      mev.getModifiers()));
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
            final DebugOption dbg = Options.getDebugOption(Options.dbgMouse);
            dbg.println(Level.FINE, () -> "mouseRelease: "
                    + MouseEvent.getModifiersExText(mev.getModifiersEx()));
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
            final DebugOption dbg = Options.getDebugOption(Options.dbgMouse);
            dbg.println(Level.FINE, () -> "mouseDrag "
                    + MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(mev.getModifiers()));
        } finally {
            setJViBusy(false);
        }
    }

    private static final MouseListener mouseListener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if(fact().isEnabled())
                mouseClick(e);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            if(fact().isEnabled())
                mousePress(e);
            
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if(fact().isEnabled())
                mouseRelease(e);
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
        }
    };
    private static final MouseMotionListener mouseMotionListener =
            new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    if(fact().isEnabled())
                        mouseDrag(e);
                }

                @Override
                public void mouseMoved(MouseEvent e)
                {
                }
    };

    //////////////////////////////////////////////////////////////////////
    //
    // Convenience
    //

    private static ViFactory fact()
    {
        return getFactory();
    }
}
