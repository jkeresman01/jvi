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
import java.awt.event.KeyListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.Action;

import com.raelity.jvi.ViTextView.TAGOP;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.Commands.ColonEvent;

/**
 * This provides Vi the items it needs to interface with
 * UI environment (like swing).
 */
public interface ViFactory
{
    public Class<?> loadClass(String name) throws ClassNotFoundException;

    /** jVi can be disabled. This means that its keymap and cursor should
     * not be installed, which is mostly platform dependent.
     *
     * In addition, there are some listeners attached and they can check
     * this to see if there is anything for them to do.
     */
    public boolean isEnabled();

    public Component getMainWindow();

    public ViAppView getAppView(Component editor);

    /** Return a TextView, create one if it doesn't already exist */
    public ViTextView createTextView(Component editor);

    /** @return null if TextView does not exist */
    public ViTextView getTextView(Component editor);

    /** Obj may be editor, or document.
     * @return null if Buffer does not exist for doc */
    public ViBuffer getBuffer(Object obj);

    public ViTextView getTextView(ViAppView av);

    /** Handle changing document in text view.
     * Editor in TextView should hold new document.
     * @param tv TextView holding editor that is changing document
     * @param oldDoc previous document (a Document for swing)
     */
    public void changeBuffer(ViTextView tv, Object oldDoc);

    /** @return Set of active ViTextView, some may have retired */
    public Set<ViTextView> getViTextViewSet();

    /** @return Set of active Buffer, some may have retired */
    public Set<Buffer> getBufferSet();

    /** @return mode title to display when there's a java text selection */
    public String getPlatformSelectionDisplayName();

    /**
     * This editor pane is going away, forget about it.
     */
    public void shutdown(Component editor);

    /**
     * Construct a window navigator for the specified list of ViAppView.
     * If avs is null, then select all visible appViews.
     * @param avs
     * @return
     */
    public ViWindowNavigator getWindowNavigator(List<ViAppView> avs);

    /**
     * Get a "stable" component that contains the editor.
     * @param ed
     * @return return a fix bounded component
     */
    public Component getViewport(Component ed);

    /**
     * Construct a window navigator for all visible AppViews.
     * @return 
     */
    public ViWindowNavigator getWindowNavigator();

  /*
   * Setup editor pane caret for use with vi.
   * This is a nop if already handled.
   */
    public void setupCaret(Component editor);

    /**
     * File manipulation handled through this object.
     */
    public ViFS getFS();

    public void setShutdownHook(Runnable hook);

    default public boolean hasPreShutdown() { return false; }

    /**
     * create an output stream for some kind of results.
     * @param tv
     * @param type Should be a constant from ViOutputStream,
     *         e.g. ViOutputStream.SEARCH.
     * @param info qualifier for the output stream, e.g. search pattern.
     * @param flags 
     */
    public ViOutputStream createOutputStream(ViTextView tv,
            Object type, Object info, EnumSet<ViOutputStream.FLAGS> flags);

    public Component findDialogParent();

    public void startGlassKeyCatch(KeyListener kl);
    public void stopGlassKeyCatch();

    public void startModalKeyCatch(KeyListener kl);
    public void stopModalKeyCatch();

    /** Assumed only one active timeout at a time.
     * Suppose could key by listener and support multiple timers.
     */
    public void startTimeout(int timeoutlen, ActionListener l);
    public void stopTimeout(ActionListener l);

    /**
     * fetch the keymap for insert mode operations
     */
    //public Keymap getInsertModeKeymap();

    /**
     * fetch the keymap for normal mode operations
     */
    //public Keymap getNormalModeKeymap();

    /**
     * @return edit mode action for specific operation
     */
    public Action createInsertModeKeyAction(String name, char vkey, String desc);

    /**
     * @return normal mode action for specific operation
     */
    public Action createNormalModeKeyAction(String name, int vkey, String desc);

    /**
     * The actions used for keymap translation may be munged by the environment
     * so we need a way to get back the original action.
     */
    public ActionListener xlateKeymapAction(ActionListener act);

    /**
     * Create a property descriptor. Typically for an option bean.
     * @param optName
     * @param methodName
     * @param clazz
     * @return
     * @throws java.beans.IntrospectionException
     */
    public PropertyDescriptor createPropertyDescriptor(String optName,
            String methodName,
            Class<?> clazz)
            throws IntrospectionException;

    /**
     * Give the platform a last chance to deny an option change.
     * NEEDSWORK: might be nice to provide for registration of validators.
     * @param optName the name Options.xxx
     * @param val the new val
     * @throws PropertyVetoException 
     */
    default public void validate(String optName, Object val)
    throws PropertyVetoException { }

    /**
     * A command entry object will be created if needed.
     * @return a CmdEntry object to handle the specified type of user input.
     */
    public ViCmdEntry createCmdEntry(ViCmdEntry.Type type);

    public Preferences getPreferences();

    //
    // Just stuff all the tag stuff here for now
    //

    public void startTagPush(ViTextView tv, String ident);

    public void tagStack(TAGOP op, int count);

    public void displayTags();

    public void tagDialog(ColonEvent e);

    public void commandEntryAssist(ViCmdEntry cmdEntry, boolean enable);

    default public boolean commandEntryAssistBusy(ViCmdEntry cmdEntry) {
        return false;
    }

    /**
     * platform context wants to execute a jvi command.
     * After activating the text view with the platform,
     * execute the runnable.
     * 
     * @param tv If null, use curwin
     * @param r  execute this
     */
    default void platformRequestsCommand(ViTextView tv, Runnable r) {
        r.run();
    }
}
