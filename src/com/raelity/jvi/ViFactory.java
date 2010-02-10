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

import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.ColonCommands;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.Action;
import com.raelity.jvi.ViTextView.TAGOP;
import java.awt.Component;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

/**
 * This provides Vi the items it needs to interface with
 * swing UI environment.
 */
public interface ViFactory
{
    public Class loadClass(String name) throws ClassNotFoundException;

    /** jVi can be disabled. This means that its keymap and cursor should
     * not be installed, which is mostly platform dependent.
     *
     * In addition, there are some listeners attached and they can check
     * this to see if there is anything for them to do.
     */
    public boolean isEnabled();

    public ViAppView getAppView(Component editor);

    /** Return a TextView, create one if it doesn't already exist */
    public ViTextView createTextView(Component editor);

    /** @return null if TextView does not exist */
    public ViTextView getTextView(Component editor);

    public ViTextView getTextView(ViAppView av);

    /** Make a best guess as to whether or not the 'ep' is a nomad.
     * Should default to false.
     */
    public boolean isNomadic(Component editor, ViAppView av);

    /** Handle changing document in text view.
     * Editor in TextView should hold new document.
     * @param tv TextView holding editor that is changing document
     * @param oldDoc previous document (a Document for swing)
     */
    public void changeBuffer(ViTextView tv, Object oldDoc);

    /** @return Set of active ViTextView, some may have retired */
    public Set<ViTextView> getViTextViewSet();

    /** @return true if the argument ViTextView is visible */
    public boolean isShowing(ViTextView tv);

    /** @return Set of active Buffer, some may have retired */
    public Set<Buffer> getBufferSet();

    /** @return mode title to display when there's a java text selection */
    public String getPlatformSelectionDisplayName();

    /**
     * This editor pane is going away, forget about it.
     */
    public void shutdown(Component editor);

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

    /** create an output stream for some kind of results.
     *  @param type Should be a constant from ViOutputStream,
     *          e.g. ViOutputStream.SEARCH.
     *  @param info qualifier for the output stream, e.g. search pattern.
     * @param priority 0 - 10 where 0 is lowest priority, 5 is normal,
     *        0-2 is low, don't raise window.
     */
    public ViOutputStream createOutputStream(ViTextView tv,
            Object type,
            Object info,
            int priority);

    public void startGlassKeyCatch(KeyListener kl);
    public void stopGlassKeyCatch();

    public void startModalKeyCatch(KeyListener kl);
    public void stopModalKeyCatch();

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
            Class clazz)
            throws IntrospectionException;

    /**
     * A command entry object will be created if needed.
     * @return a CmdEntry object to handle the specified type of user input.
     */
    public ViCmdEntry createCmdEntry(int type);

    public Preferences getPreferences();

    //
    // Just stuff all the tag stuff here for now
    //

    public void startTagPush(ViTextView tv, String ident);

    public void finishTagPush(ViTextView tv);

    public void tagStack(TAGOP op, int count);

    public void displayTags();

    public void tagDialog(ColonCommands.ColonEvent e);

    public void commandEntryAssist(ViCmdEntry cmdEntry, boolean enable);
}
