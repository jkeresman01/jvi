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

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.ArrayList;
import javax.swing.KeyStroke;
import javax.swing.Action;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.KeyStroke;

import com.raelity.jvi.KeyDefs;
import com.raelity.jvi.BooleanOption;
import com.raelity.jvi.Option;
import com.raelity.jvi.Options;
import com.raelity.jvi.*;

public class KeyBinding implements KeyDefs, Constants {

  public static BooleanOption keyDebug
                = (BooleanOption)Options.getOption(Options.dbgKeyStrokes);
  public static boolean notImpDebug = false;

  public static final int MOD_MASK = InputEvent.SHIFT_MASK
	    				| InputEvent.CTRL_MASK
					| InputEvent.META_MASK
					| InputEvent.ALT_MASK;

  static final String enqueKeyAction = "enque-key";

  /**
   * Construct and return a keymap for a standard swing text component.
   * Also, if not already existing, construct insert and normal mode
   * keymaps only used for user
   * defined mappings.
   */
  public static Keymap getKeymap() {
    Keymap keymap = JTextComponent.addKeymap(null, null);
    keymap.setDefaultAction(ViManager.getViFactory()
			    	.createCharAction(enqueKeyAction));
    JTextComponent.loadKeymap(keymap, getBindings(), getActions());
    createSubKeymaps();
    return keymap;
  }
  
  static void createSubKeymaps() {
    Keymap insertModeKeymap = JTextComponent.addKeymap(null, null);
    JTextComponent.KeyBinding[] bindings = getInsertModeBindings();
    Action[] actions = getInsertModeActions();
    JTextComponent.loadKeymap(insertModeKeymap,
			      bindings,
			      actions);
    // This is here only for convenience, it may be overridden.
    ViManager.setInsertModeKeymap(insertModeKeymap);
  }

  public static Action getDefaultAction() {
    return ViManager.getViFactory().createCharAction(enqueKeyAction);
  }

  /** Modify the keymap <code>km00</code> by removing any keystrokes
   * specified in the keymap <code>km01</code>. This is typically used
   * to remove keys that vi would normally be looking at, e.g. function
   * keys, but are otherwise bound to the environment in which vi is
   * operating.
   */
  public static void removeBindings(Keymap km00, Keymap km01) {
    int i;
    KeyStroke[] k01 = km01.getBoundKeyStrokes();
    for(i = 0; i < k01.length; i++) {
      km00.removeKeyStrokeBinding(k01[i]);
    }
  }

  /**
   * Bind the keys to actions of their own name. This is simpley a
   * way to grab all the keys. Probably want to use key events
   * directly at some point.
   * <br>
   * Only do regular and shift versiions for now.
   */
  public static JTextComponent.KeyBinding[] getBindings() {
    List l = getBindingsList();
    return (JTextComponent.KeyBinding[]) l.toArray(
                              new JTextComponent.KeyBinding[l.size()]);
  }

  public static List getBindingsList() {

    List bindingList = new ArrayList();

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UP, 0),
                   "ViUpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DOWN, 0),
                   "ViDownKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_LEFT, 0),
                   "ViLeftKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_RIGHT, 0),
                   "ViRightKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_INSERT, 0),
                   "ViInsertKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DELETE, 0),
                   "ViDeleteKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HOME, 0),
                   "ViHomeKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_END, 0),
                   "ViEndKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HELP, 0),
                   "ViHelpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UNDO, 0),
                   "ViUndoKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_UP, 0),
                   "ViPage_upKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_DOWN, 0),
                   "ViPage_downKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PLUS, 0),
                   "ViPlusKey"));
    // bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
      // 	     KeyEvent.VK_MINUS, 0),
      // 	     "ViMinusKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DIVIDE, 0),
                   "ViDivideKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_MULTIPLY, 0),
                   "ViMultiplyKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F1, 0),
                   "ViF1Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F2, 0),
                   "ViF2Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F3, 0),
                   "ViF3Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F4, 0),
                   "ViF4Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F5, 0),
                   "ViF5Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F6, 0),
                   "ViF6Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F7, 0),
                   "ViF7Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F8, 0),
                   "ViF8Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F9, 0),
                   "ViF9Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F10, 0),
                   "ViF10Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F11, 0),
                   "ViF11Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F12, 0),
                   "ViF12Key"));
    //
    //
    // SHIFTED KEYS
    //
    //

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UP, InputEvent.SHIFT_MASK),
                   "ViUpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK),
                   "ViDownKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK),
                   "ViLeftKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK),
                   "ViRightKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK),
                   "ViInsertKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK),
                   "ViDeleteKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HOME, InputEvent.SHIFT_MASK),
                   "ViHomeKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_END, InputEvent.SHIFT_MASK),
                   "ViEndKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HELP, InputEvent.SHIFT_MASK),
                   "ViHelpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UNDO, InputEvent.SHIFT_MASK),
                   "ViUndoKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_UP, InputEvent.SHIFT_MASK),
                   "ViPage_upKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_DOWN, InputEvent.SHIFT_MASK),
                   "ViPage_downKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PLUS, InputEvent.SHIFT_MASK),
                   "ViPlusKey"));
    // bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   // KeyEvent.VK_MINUS, InputEvent.SHIFT_MASK),
                   // "ViMinusKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DIVIDE, InputEvent.SHIFT_MASK),
                   "ViDivideKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_MULTIPLY, InputEvent.SHIFT_MASK),
                   "ViMultiplyKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F1, InputEvent.SHIFT_MASK),
                   "ViF1Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F2, InputEvent.SHIFT_MASK),
                   "ViF2Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F3, InputEvent.SHIFT_MASK),
                   "ViF3Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F4, InputEvent.SHIFT_MASK),
                   "ViF4Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F5, InputEvent.SHIFT_MASK),
                   "ViF5Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F6, InputEvent.SHIFT_MASK),
                   "ViF6Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F7, InputEvent.SHIFT_MASK),
                   "ViF7Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F8, InputEvent.SHIFT_MASK),
                   "ViF8Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F9, InputEvent.SHIFT_MASK),
                   "ViF9Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F10, InputEvent.SHIFT_MASK),
                   "ViF10Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F11, InputEvent.SHIFT_MASK),
                   "ViF11Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F12, InputEvent.SHIFT_MASK),
                   "ViF12Key"));
     
    //
    //
    // CTRL KEYS
    //
    //

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UP, InputEvent.CTRL_MASK),
                   "ViUpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DOWN, InputEvent.CTRL_MASK),
                   "ViDownKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_LEFT, InputEvent.CTRL_MASK),
                   "ViLeftKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK),
                   "ViRightKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_INSERT, InputEvent.CTRL_MASK),
                   "ViInsertKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DELETE, InputEvent.CTRL_MASK),
                   "ViDeleteKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HOME, InputEvent.CTRL_MASK),
                   "ViHomeKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_END, InputEvent.CTRL_MASK),
                   "ViEndKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HELP, InputEvent.CTRL_MASK),
                   "ViHelpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UNDO, InputEvent.CTRL_MASK),
                   "ViUndoKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK),
                   "ViPage_upKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK),
                   "ViPage_downKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PLUS, InputEvent.CTRL_MASK),
                   "ViPlusKey"));
    // bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   // KeyEvent.VK_MINUS, InputEvent.CTRL_MASK),
                   // "ViMinusKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DIVIDE, InputEvent.CTRL_MASK),
                   "ViDivideKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_MULTIPLY, InputEvent.CTRL_MASK),
                   "ViMultiplyKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F1, InputEvent.CTRL_MASK),
                   "ViF1Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F2, InputEvent.CTRL_MASK),
                   "ViF2Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F3, InputEvent.CTRL_MASK),
                   "ViF3Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F4, InputEvent.CTRL_MASK),
                   "ViF4Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F5, InputEvent.CTRL_MASK),
                   "ViF5Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F6, InputEvent.CTRL_MASK),
                   "ViF6Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F7, InputEvent.CTRL_MASK),
                   "ViF7Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F8, InputEvent.CTRL_MASK),
                   "ViF8Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F9, InputEvent.CTRL_MASK),
                   "ViF9Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F10, InputEvent.CTRL_MASK),
                   "ViF10Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F11, InputEvent.CTRL_MASK),
                   "ViF11Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F12, InputEvent.CTRL_MASK),
                   "ViF12Key"));

    //
    //
    // other bindings
    //
    //
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		 '.', InputEvent.CTRL_MASK),
		 "ViPeriodCloseAngle")); // the ">" must be on period
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		 ',', InputEvent.CTRL_MASK),
		 "ViCommaOpenAngle")); // the "<" must be on comma

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_SPACE, 0),
                 "ViSpaceKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_A, Event.CTRL_MASK),
                 "ViCtrl-A"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_B, Event.CTRL_MASK),
                 "ViCtrl-B"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_C, Event.CTRL_MASK),
                 "ViCtrl-C"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_D, Event.CTRL_MASK),
                 "ViCtrl-D"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_E, Event.CTRL_MASK),
                 "ViCtrl-E"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_F, Event.CTRL_MASK),
                 "ViCtrl-F"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_G, Event.CTRL_MASK),
                 "ViCtrl-G"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_H, Event.CTRL_MASK),
                 "ViCtrl-H"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_BACK_SPACE, 0),
                 "ViBack_spaceKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_I, Event.CTRL_MASK),
                 "ViCtrl-I"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_TAB, 0),
                   "ViTabKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_J, Event.CTRL_MASK),
                 "ViCtrl-J"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_ENTER, 0),
                 "ViEnterKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_K, Event.CTRL_MASK),
                 "ViCtrl-K"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_L, Event.CTRL_MASK),
                 "ViCtrl-L"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_M, Event.CTRL_MASK),
                 "ViCtrl-M"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_N, Event.CTRL_MASK),
                 "ViCtrl-N"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_O, Event.CTRL_MASK),
                 "ViCtrl-O"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_P, Event.CTRL_MASK),
                 "ViCtrl-P"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_Q, Event.CTRL_MASK),
                 "ViCtrl-Q"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_R, Event.CTRL_MASK),
                 "ViCtrl-R"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_S, Event.CTRL_MASK),
                 "ViCtrl-S"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_T, Event.CTRL_MASK),
                 "ViCtrl-T"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_U, Event.CTRL_MASK),
                 "ViCtrl-U"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_V, Event.CTRL_MASK),
                 "ViCtrl-V"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_W, Event.CTRL_MASK),
                 "ViCtrl-W"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_X, Event.CTRL_MASK),
                 "ViCtrl-X"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_Y, Event.CTRL_MASK),
                 "ViCtrl-Y"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_Z, Event.CTRL_MASK),
                 "ViCtrl-Z"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_OPEN_BRACKET, Event.CTRL_MASK),
                 "ViEscapeKey"));      // alternate 
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_ESCAPE, 0),
                 "ViEscapeKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_BACK_SLASH, Event.CTRL_MASK),
                 "ViCtrl-BackslashKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_CLOSE_BRACKET, Event.CTRL_MASK),
                 "ViCtrl-ClosebracketKey"));
    //bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
    //             KeyEvent.VK_CIRCUMFLEX, Event.CTRL_MASK),
    //             "ViCtrl-CircumflexKey"));
    //bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
    //             KeyEvent.VK_UNDERSCORE, Event.CTRL_MASK),
    //             "ViCtrl-UnderscoreKey"));


    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_SPACE, InputEvent.SHIFT_MASK),
                 "ViSpaceKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_BACK_SPACE, InputEvent.SHIFT_MASK),
                 "ViBack_spaceKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_TAB, InputEvent.SHIFT_MASK),
                   "ViTabKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK),
                 "ViEnterKey"));
    return bindingList;
  }

  /**
   * initialize keymap.
   */
  public static Action[] getActions() {
    Action[] localActions = null;
    try {
      ViFactory factory = ViManager.getViFactory();
      localActions = new Action[] {
	  factory.createKeyAction("ViUpKey", K_UP),
	  factory.createKeyAction("ViDownKey", K_DOWN),
	  factory.createKeyAction("ViLeftKey", K_LEFT),
	  factory.createKeyAction("ViRightKey", K_RIGHT),
	  factory.createKeyAction("ViInsertKey", K_INS),
	  factory.createKeyAction("ViDeleteKey", K_DEL),
	  factory.createKeyAction("ViTabKey", K_TAB),
	  factory.createKeyAction("ViHomeKey", K_HOME),
	  factory.createKeyAction("ViEndKey", K_END),
	  factory.createKeyAction("ViHelpKey", K_HELP),
	  factory.createKeyAction("ViUndoKey", K_UNDO),
	  factory.createKeyAction("ViBack_spaceKey", KeyEvent.VK_BACK_SPACE),

	  factory.createKeyAction("ViPage_upKey", K_PAGEUP),
	  factory.createKeyAction("ViPage_downKey", K_PAGEDOWN),
	  factory.createKeyAction("ViPlusKey", K_KPLUS),
	  factory.createKeyAction("ViMinusKey", K_KMINUS),
	  factory.createKeyAction("ViDivideKey", K_KDIVIDE),
	  factory.createKeyAction("ViMultiplyKey", K_KMULTIPLY),
	  // factory.createKeyAction("ViEnterKey", K_ENTER),

	  factory.createKeyAction("ViPeriodCloseAngle", K_X_PERIOD),
	  factory.createKeyAction("ViCommaOpenAngle", K_X_COMMA),
	  
	  factory.createKeyAction("ViCtrl-A", 1),
	  factory.createKeyAction("ViCtrl-B", 2),
	  factory.createKeyAction("ViCtrl-C", 3),
	  factory.createKeyAction("ViCtrl-D", 4),
	  factory.createKeyAction("ViCtrl-E", 5),
	  factory.createKeyAction("ViCtrl-F", 6),
	  factory.createKeyAction("ViCtrl-G", 7),
	  factory.createKeyAction("ViCtrl-H", 8),
	  factory.createKeyAction("ViCtrl-I", 9),
	  factory.createKeyAction("ViCtrl-J", 10),
	  factory.createKeyAction("ViCtrl-K", 11),
	  factory.createKeyAction("ViCtrl-L", 12),
	  factory.createKeyAction("ViCtrl-M", 13),
	  factory.createKeyAction("ViEnterKey", KeyEvent.VK_ENTER), // 13
	  factory.createKeyAction("ViCtrl-N", 14),
	  factory.createKeyAction("ViCtrl-O", 15),
	  factory.createKeyAction("ViCtrl-P", 16),
	  factory.createKeyAction("ViCtrl-Q", 17),
	  factory.createKeyAction("ViCtrl-R", 18),
	  factory.createKeyAction("ViCtrl-S", 19),
	  factory.createKeyAction("ViCtrl-T", 20),
	  factory.createKeyAction("ViCtrl-U", 21),
	  factory.createKeyAction("ViCtrl-V", 22),
	  factory.createKeyAction("ViCtrl-W", 23),
	  factory.createKeyAction("ViCtrl-X", 24),
	  factory.createKeyAction("ViCtrl-Y", 25),
	  factory.createKeyAction("ViCtrl-Z", 26),
	  factory.createKeyAction("ViEscapeKey", KeyEvent.VK_ESCAPE), // 27
	  factory.createKeyAction("ViCtrl-BackslashKey", 28),
	  factory.createKeyAction("ViCtrl-ClosebracketKey", 29),
	  //factory.createKeyAction("ViCtrl-CircumflexKey", 30),
	  //factory.createKeyAction("ViCtrl-UnderscoreKey", 31),
	  factory.createKeyAction("ViSpaceKey", KeyEvent.VK_SPACE),

	  factory.createKeyAction("ViF1Key", K_F1),
	  factory.createKeyAction("ViF2Key", K_F2),
	  factory.createKeyAction("ViF3Key", K_F3),
	  factory.createKeyAction("ViF4Key", K_F4),
	  factory.createKeyAction("ViF5Key", K_F5),
	  factory.createKeyAction("ViF6Key", K_F6),
	  factory.createKeyAction("ViF7Key", K_F7),
	  factory.createKeyAction("ViF8Key", K_F8),
	  factory.createKeyAction("ViF9Key", K_F9),
	  factory.createKeyAction("ViF10Key", K_F10),
	  factory.createKeyAction("ViF11Key", K_F11),
	  factory.createKeyAction("ViF12Key", K_F12)
      };
    } catch(Throwable e) {
      e.printStackTrace();
    }
    return localActions;
  }

  /** Read these as keys, not chars. */
  final private static boolean ignoreCtrlChars[] = {
    	false,		// 0
	true,
	true,
	true,
	true,		// 4
    	true,		// 5
	true,		// 6	Ctrl-F
	true,		// 7	Ctrl-G
	true,		// 8	backspace
	true,		// 9	tab
    	true,		// 10	return/enter
	true,
	true,
	true,
	true,		// 14	Ctrl-N
    	true,		// 15
	true,		// 16	Ctrl-P
	true,
	true,
	true,		// 19
    	true,
	true,
	true,
	true,
	true,		// 24
    	true,
	true,
	true,		// 27	escape
	true,
	true,		// 29
	true,		// 30
	true,		// 31
	true		// 32	space is special case
  };

  /** Test if the argument char should be ignored. Note that when
   * ignored as a char, it is generally is queued up as a key.
   */
  final static public boolean ignoreChar(int c) {
    return	c < KeyBinding.ignoreCtrlChars.length
	  	&& KeyBinding.ignoreCtrlChars[c];
  }
  
  public static JTextComponent.KeyBinding[] getInsertModeBindings() {
    List l = getInsertModeBindingsList();
    return (JTextComponent.KeyBinding[]) l.toArray(
                              new JTextComponent.KeyBinding[l.size()]);
  }

  public static List getInsertModeBindingsList() {

    List bindingList = new ArrayList();

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		   KeyEvent.VK_PERIOD, InputEvent.CTRL_MASK),
		   "ViInsert_indentNextParen"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		   KeyEvent.VK_COMMA, InputEvent.CTRL_MASK),
		   "ViInsert_indentPrevParen"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		   KeyEvent.VK_T, InputEvent.CTRL_MASK),
		   "ViInsert_shiftRight"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		   KeyEvent.VK_D, InputEvent.CTRL_MASK),
		   "ViInsert_shiftLeft"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		   KeyEvent.VK_INSERT, 0),
		   "ViInsert_insertReplace"));
    return bindingList;
  }
  
  public static Action[] getInsertModeActions() {
    Action[] localActions = null;
    try {
      ViFactory factory = ViManager.getViFactory();
      localActions = new Action[] {
	  factory.createInsertModeKeyAction("ViInsert_shiftRight",
		     IM_SHIFT_RIGHT,
		     "Insert one shiftwidth of indent at the"
		     + " start of the current line."
		     + " Only key press events are valid."),
	  factory.createInsertModeKeyAction("ViInsert_shiftLeft",
		     IM_SHIFT_LEFT,
		     "Delete one shiftwidth of indent at the"
		     + " start of the current line."
		     + " Only key press events are valid."),
	  factory.createInsertModeKeyAction("ViInsert_indentNextParen",
		     IM_SHIFT_RIGHT_TO_PAREN,
		     "Indent current line to start under next"
		     + " parenthesis on previous line."
		     + " Only key press events are valid."),
	  factory.createInsertModeKeyAction("ViInsert_indentPrevParen",
		     IM_SHIFT_LEFT_TO_PAREN,
		     "Indent current line to start under previous"
		     + " parenthesis on previous line."
		     + " Only key press events are valid."),
	  factory.createInsertModeKeyAction("ViInsert_insertReplace",
		     IM_INS_REP,
		     "Toggle between insert and replace mode")
      };
    } catch(Throwable e) {
      e.printStackTrace();
    }
    return localActions;
  }
  
  public static int[] initJavaKeyMap() {
    int[] jk = new int[KeyDefs.MAX_JAVA_KEY_MAP + 1];
    
    for(int i = 0; i < jk.length; i++) {
      jk[i] = -1;
    }
    
    jk[MAP_K_UP] = KeyEvent.VK_UP;
    jk[MAP_K_DOWN] = KeyEvent.VK_DOWN;
    jk[MAP_K_LEFT] = KeyEvent.VK_LEFT;
    jk[MAP_K_RIGHT] = KeyEvent.VK_RIGHT;
    jk[MAP_K_TAB] = KeyEvent.VK_TAB;
    jk[MAP_K_HOME] = KeyEvent.VK_HOME;
    jk[MAP_K_END] = KeyEvent.VK_END;
    // jk[MAP_K_S_UP] = KeyEvent.VK_S_UP;
    // jk[MAP_K_S_DOWN] = KeyEvent.VK_S_DOWN;
    // jk[MAP_K_S_LEFT] = KeyEvent.VK_S_LEFT;
    // jk[MAP_K_S_RIGHT] = KeyEvent.VK_S_RIGHT;
    // jk[MAP_K_S_TAB] = KeyEvent.VK_S_TAB;
    // jk[MAP_K_S_HOME] = KeyEvent.VK_S_HOME;
    // jk[MAP_K_S_END] = KeyEvent.VK_S_END;
    jk[MAP_K_F1] = KeyEvent.VK_F1;
    jk[MAP_K_F2] = KeyEvent.VK_F2;
    jk[MAP_K_F3] = KeyEvent.VK_F3;
    jk[MAP_K_F4] = KeyEvent.VK_F4;
    jk[MAP_K_F5] = KeyEvent.VK_F5;
    jk[MAP_K_F6] = KeyEvent.VK_F6;
    jk[MAP_K_F7] = KeyEvent.VK_F7;
    jk[MAP_K_F8] = KeyEvent.VK_F8;
    jk[MAP_K_F9] = KeyEvent.VK_F9;
    jk[MAP_K_F10] = KeyEvent.VK_F10;
    jk[MAP_K_F11] = KeyEvent.VK_F11;
    jk[MAP_K_F12] = KeyEvent.VK_F12;
    jk[MAP_K_F13] = KeyEvent.VK_F13;
    jk[MAP_K_F14] = KeyEvent.VK_F14;
    jk[MAP_K_F15] = KeyEvent.VK_F15;
    jk[MAP_K_F16] = KeyEvent.VK_F16;
    jk[MAP_K_F17] = KeyEvent.VK_F17;
    jk[MAP_K_F18] = KeyEvent.VK_F18;
    jk[MAP_K_F19] = KeyEvent.VK_F19;
    jk[MAP_K_F20] = KeyEvent.VK_F20;
    jk[MAP_K_F21] = KeyEvent.VK_F21;
    jk[MAP_K_F22] = KeyEvent.VK_F22;
    jk[MAP_K_F23] = KeyEvent.VK_F23;
    jk[MAP_K_F24] = KeyEvent.VK_F24;
    jk[MAP_K_HELP] = KeyEvent.VK_HELP;
    jk[MAP_K_UNDO] = KeyEvent.VK_UNDO;
    jk[MAP_K_BS] = KeyEvent.VK_BACK_SPACE;
    jk[MAP_K_INS] = KeyEvent.VK_INSERT;
    jk[MAP_K_DEL] = KeyEvent.VK_DELETE;
    jk[MAP_K_PAGEUP] = KeyEvent.VK_PAGE_UP;
    jk[MAP_K_PAGEDOWN] = KeyEvent.VK_PAGE_DOWN;
    jk[MAP_K_KPLUS] = KeyEvent.VK_PLUS;
    jk[MAP_K_KMINUS] = KeyEvent.VK_MINUS;
    jk[MAP_K_KDIVIDE] = KeyEvent.VK_DIVIDE;
    jk[MAP_K_KMULTIPLY] = KeyEvent.VK_MULTIPLY;
    jk[MAP_K_KENTER] = KeyEvent.VK_ENTER;
    jk[MAP_K_X_PERIOD] = KeyEvent.VK_PERIOD;
    jk[MAP_K_X_COMMA] = KeyEvent.VK_COMMA;
    
    return jk;
  }
}
