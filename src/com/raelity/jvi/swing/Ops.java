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
package com.raelity.jvi.swing;

import com.raelity.jvi.*;

import javax.swing.JEditorPane;
import javax.swing.Action;
import javax.swing.text.*;
import java.awt.event.*;
import java.util.*;
import com.raelity.jvi.*;

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
    if(EditorActions.actionMap.size() > 0) {
      return;
    }
    EditorActions.setupActions(editorPane.getEditorKit().getActions());
  }

  public void xact(String actionName) {
    event.setSource(textView.getEditorComponent());
    Action action = EditorActions.getAction(actionName);
    xact(action);
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
      actionName = DefaultEditorKit.insertContentAction;
      break;

    case KEY_TYPED:
      actionName = DefaultEditorKit.defaultKeyTypedAction;
      break;

    case INSERT_NEW_LINE:
      actionName = DefaultEditorKit.insertBreakAction;
      break;

    case INSERT_TAB:
      actionName = DefaultEditorKit.insertTabAction;
      break;

    case DELETE_PREVIOUS_CHAR:
      actionName = DefaultEditorKit.deletePrevCharAction;
      break;

    default:
      throw new RuntimeException("Unknown op: " + op);
    }
    xact(actionName);
  }

  ReusableEvent event = new ReusableEvent();
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

/**
 * This is a cache of the editor actions. The actions can be looked
 * up by name. This is presumed to hold the actions as defined in
 * the {@link javax.swing.text.DefaultEditorKit}. The actions from
 * the {@link javax.swing.text.StyledEditorKit} should be considered
 * optional.
 */
class EditorActions {
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
