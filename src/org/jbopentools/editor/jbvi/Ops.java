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
  }


  public void xact(Action action) {
    event.setSource(textView.getEditorComponent());
    action.actionPerformed(event);
  }

  public void xact(Action action, String s) {
    event.setActionCommand(s);
    xact(action);
  }

  public void xop(int op, String s) {
    event.setActionCommand(s);
    xop(op);
  }

  public void xop(int op) {
    Action action;

    switch(op) {
    case INSERT_TEXT:
      action = EditorActions.ACTION_DefaultKeyTyped;
      break;

    case KEY_TYPED:
      action = EditorActions.ACTION_DefaultKeyTyped;
      break;

    case INSERT_NEW_LINE:
      action = EditorActions.ACTION_ReturnKey; /*JB7 (was SmartIndent)*/
      break;

    case INSERT_TAB:
      // NEEDSWORK: should be in edit module for turning tab to blanks
      if( ! EditorManager.isBooleanOptionValue(EditorManager.useTabCharAttribute)
	 || EditorManager.isBooleanOptionValue(EditorManager.smartTabsAttribute)) {
	action = EditorActions.ACTION_TabKey;
      } else {
	int offset = textView.getCaretPosition();
	textView.insertText(offset, "\t");
	return;
      }
      break;

    case DELETE_PREVIOUS_CHAR:
      action = EditorActions.ACTION_SmartBackspace;
      break;

    default:
      throw new RuntimeException("Unknown op: " + op);
    }
    xact(action);
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

