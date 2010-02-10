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
 * Copyright (C) 2000-2008 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.swing;

import com.raelity.jvi.ViTextView;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.text.DefaultEditorKit;

/**
 * This provides default swing JTextComponent behavior that are
 * used by vi.
 *
 * Usually {@link #xop(int)} is overridden for the environment. 
 * When overridden, the superclass should be invoked if given an
 * unrecognized op.
 *
 * This implementation simply uses actions from the editor kit.
 * The action names are taken from the default editor kit.
 * The action map is built as needd, so if the same instance is used with
 * different editors which have different actions, curious results could
 * ensue.
 *
 * @author erra
 */
public class OpsBase implements TextOps {

    protected ViTextView textView;

    protected OpsBase.ReusableEvent event = new ReusableEvent();

    protected static Map<String,Action> actionMap
            = new HashMap<String,Action>();
    
    /** Creates a new instance of OpsBase */
    public OpsBase(ViTextView textView) {
        this.textView = textView;
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
    
    public void xact(String actionName) {
        Action action = findAction(actionName);
        xact(action);
    }

    /**
     * Try to do something useful with the op.
     */
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

    protected void xact(String actionName, String cmd) {
        event.setActionCommand(cmd);
        xact(actionName);
    }

    protected Action findAction(String actionName) {
        Action action = actionMap.get(actionName);
        if (action == null) {
            Action[] actions = ((SwingTextView)textView).getActions();
            for (int i = 0; i < actions.length; i++) {
                String name = (String)actions[i].getValue(Action.NAME);
                if (name.equals(actionName)) {
                    action = actions[i];
                    actionMap.put(name, action);
                    break;
                }
            }
        }
	if(action == null) {
	    throw new RuntimeException("Action " + actionName + "not found");
	}
        return action;
    }

    protected static class ReusableEvent extends ActionEvent {
        private String cmd;

        ReusableEvent() {
            super("", ActionEvent.ACTION_PERFORMED, "");
        }

        @Override
        public void setSource(Object source) {
          this.source = source;
        }

        void setActionCommand(String cmd) {
          this.cmd = cmd;
        }

        @Override
        public String getActionCommand() {
          return cmd;
        }
    }
}
