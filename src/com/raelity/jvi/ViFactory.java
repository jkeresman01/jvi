/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */

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

import javax.swing.JEditorPane;
import javax.swing.Action;

import com.raelity.jvi.ViFS;
import com.raelity.jvi.Window;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.swing.TextOps;
import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.swing.*;

/**
 * This provides Vi the items it needs to interface with
 * swing UI environment.
 */
public interface ViFactory {

  public ViTextView getViTextView(JEditorPane editorPane);

  /*
   * Register editor pane for use with vi.
   * This is a nop if already registered.
   */
  public void registerEditorPane(JEditorPane editorPane);

  /**
   * File manipulation handled through this object.
   */
  public ViFS getFS();

  /**
   * The factory keeps track of which Window is active when there
   * is a switch. The argument may or may not be used depending on
   * the implementation.
   * @return the Window for the specified editor
   */
  public Window lookupWindow(JEditorPane editorPane);

  /**
   * @return action suitable for default key action
   */
  public Action createCharAction(String name);

  /**
   * @return action for picking up specified key
   */
  public Action createKeyAction(String name, int key);
  
  /**
   * @return edit mode action for picking up specified key
   */
  public Action createInsertModeKeyAction(String name, int vkey, String desc);
  
  /**
   * @return normal mode action for picking up specified key
   */
  public Action createNormalModeKeyAction(String name, int vkey, String desc);

  /**
   * A command entry object will be created if needed.
   * @return a CmdEntry object to handle the specified type of user input.
   */
  public ViCmdEntry createCmdEntry(int type);

}
