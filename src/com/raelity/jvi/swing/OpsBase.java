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

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.text.DefaultEditorKit;

import com.raelity.jvi.ViTextView;

import static java.awt.event.ActionEvent.ACTION_PERFORMED;

import static com.raelity.text.TextUtil.sf;

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
public class OpsBase implements TextOps
{
    private static final Logger LOG = Logger.getLogger(OpsBase.class.getName());

    protected ViTextView textView;

    protected static Map<String,Action> actionMap
            = new HashMap<String,Action>();
    
    /** Creates a new instance of OpsBase */
    public OpsBase(ViTextView textView) {
        this.textView = textView;
    }

    @Override
    public void xact(Action action) {
	xact(action, "");
    }

    @Override
    public void xact(Action action, String command) {
	action.actionPerformed(
            new ActionEvent(textView.getEditor(), ACTION_PERFORMED, command));
    }
    
    @Override
    public void xop(int op) {
        String actionName;
        switch(op) {
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
            IllegalArgumentException x = new IllegalArgumentException();
            LOG.severe(()->sf("xop(%d): \n%s", op, x));
            return;
            
        }
        xact(actionName);
    }
    
    @Override
    public void xact(String actionName) {
        xact(actionName, "");
    }

    /**
     * Try to do something useful with the op.
     * NEEDSWORK: seems like only KEY_TYPED is ever used.
     */
    @Override
    public void xop(int op, String cmd) {
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
        xact(actionName, cmd);
    }

    protected void xact(String actionName, String cmd) {
        Action action = findAction(actionName);
        xact(action, cmd);
    }

    protected Action findAction(String actionName) {
        Action action = actionMap.get(actionName);
        if (action == null) {
            Action[] actions = ((SwingTextView)textView).getActions();
            for(Action action1 : actions) {
                String name = (String)action1.getValue(Action.NAME);
                if(name.equals(actionName)) {
                    action = action1;
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
}
