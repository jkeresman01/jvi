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
package org.jbopentools.editor.jbvi;

import javax.swing.JEditorPane;
import javax.swing.Action;
import javax.swing.text.*;
import java.awt.event.*;
import java.util.*;

import com.borland.primetime.editor.EditorActionNames;
import com.borland.primetime.editor.EditorActions;
import com.borland.primetime.editor.EditorActions.DefaultKeyTypedAction;
import com.borland.primetime.editor.EditorManager;
import com.raelity.jvi.*;
import com.raelity.jvi.swing.*;

/**
 * This provides access to swings JEditorPane actions that are
 * used by vi.
 */
class Ops implements TextOps, Constants {
  ViTextView textView;

  Ops(ViTextView textView) {
    this.textView = textView;
  }

  public void init(JEditorPane editorPane) {
    if(ActionsCache.actionMap.size() > 0) {
      return;
    }
    ActionsCache.setupActions(EditorActions.getActions());
    // need to put in the default action manually?
    ActionsCache.actionMap.put(EditorActionNames.defaultKeyTypedAction,
			       new EditorActions.DefaultKeyTypedAction());
  }

  public void xact(String actionName) {
    event.setSource(textView.getEditorComponent());
    Action action = ActionsCache.getAction(actionName);
    xact(action);
  }

  public void xact(String actionName, String s) {
    //keyEvent.setSource(textView.getEditorComponent());
    //keyEvent.setKeyChar(c);
    event.setSource(textView.getEditorComponent());
    event.setActionCommand(s);
    Action action = ActionsCache.getAction(actionName);
    action.actionPerformed(event);
  }

  public void xact(Action action) {
    action.actionPerformed(event);
  }

  public void xop(int op, String s) {
    event.setActionCommand(s);
    xop(op);
  }

  public void xop(int op) {
    String actionName;
    switch(op) {
    case INSERT_TEXT:
      // actionName = EditorActionNames.insertContentAction;
      actionName = EditorActionNames.defaultKeyTypedAction;
      break;

    case KEY_TYPED:
      actionName = EditorActionNames.defaultKeyTypedAction;
      break;

    case INSERT_NEW_LINE:
      actionName = EditorActionNames.smartIndentAction;
      break;

    case INSERT_TAB:
      // NEEDSWORK: should be in edit module for turning tab to blanks
      if( ! EditorManager.isBooleanOptionValue(EditorManager.useTabCharAttribute)
	 || EditorManager.isBooleanOptionValue(EditorManager.smartTabsAttribute)) {
	actionName = EditorActionNames.tabKeyAction;
      } else {
	int offset = textView.getCaretPosition();
	textView.insertText(offset, "\t");
	return;
      }
      break;

    case DELETE_PREVIOUS_CHAR:
      actionName = EditorActionNames.smartBackspaceAction;
      break;

    default:
      throw new RuntimeException("Unknown op: " + op);
    }
    xact(actionName);
  }

  ReusableEvent event = new ReusableEvent();
  
  KeyEvent keyEvent = new ReusableKeyEvent();
}

class ReusableEvent extends ActionEvent {
  private String cmd;

  ReusableEvent() {
    super("", ActionEvent.ACTION_PERFORMED, "");
  }

  void setSource(Object source) {
    this.source = source;
  }

  void setActionCommand(String cmd) {
    this.cmd = cmd;
  }

  public String getActionCommand() {
    return cmd;
  }
}

/** Make this a key typed event */
class ReusableKeyEvent extends KeyEvent {
  ReusableKeyEvent() {
    super(new java.awt.Button(), KEY_TYPED, 0L, 0, VK_UNDEFINED, '}');
  }

  public void setSource(Object source) {
    this.source = source;
  }
}

/**
 * This is a cache of the editor actions. The actions can be looked
 * up by name. This is presumed to hold the actions as defined in
 * the {@link javax.swing.text.DefaultEditorKit}. The actions from
 * the {@link javax.swing.text.StyledEditorKit} should be considered
 * optional.
 */
class ActionsCache {
  static Map actionMap = new HashMap();

  static void setupActions(Action[] actions) {
    for(int i = 0; i < actions.length; i++) {
      Action action = actions[i];
      String name = (String)action.getValue(Action.NAME);
      actionMap.put(name, action);
    }
  }

  static Action getAction(String name) {
    return (Action)actionMap.get(name);
  }
}
