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

public class KeyBinding implements KeyDefs {

  public static BooleanOption keyDebug
                = (BooleanOption)Options.getOption(Options.dbgKeyStrokes);
  public static boolean notImpDebug = false;

  public static final int MOD_MASK = InputEvent.SHIFT_MASK
	    				| InputEvent.CTRL_MASK
					| InputEvent.META_MASK
					| InputEvent.ALT_MASK;

  static final String enqueKeyAction = "enque-key";

  public static Keymap getKeymap() {
    Keymap keymap = JTextComponent.addKeymap(null, null);
    keymap.setDefaultAction(ViManager.getViFactory()
			    	.createCharAction(enqueKeyAction));
    JTextComponent.loadKeymap(keymap, getBindings(), getActions());
    return keymap;
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
    JTextComponent.KeyBinding[] bindings = {
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_UP, 0),
		       "upKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DOWN, 0),
		       "downKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_LEFT, 0),
		       "leftKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_RIGHT, 0),
		       "rightKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_INSERT, 0),
		       "insertKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DELETE, 0),
		       "deleteKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_HOME, 0),
		       "homeKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_END, 0),
		       "endKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_HELP, 0),
		       "helpKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_UNDO, 0),
		       "undoKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PAGE_UP, 0),
		       "page_upKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PAGE_DOWN, 0),
		       "page_downKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PLUS, 0),
		       "plusKey"),
	// new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  // 	     KeyEvent.VK_MINUS, 0),
	  // 	     "minusKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DIVIDE, 0),
		       "divideKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_MULTIPLY, 0),
		       "multiplyKey"),

	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F1, 0),
		       "f1Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F2, 0),
		       "f2Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F3, 0),
		       "f3Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F4, 0),
		       "f4Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F5, 0),
		       "f5Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F6, 0),
		       "f6Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F7, 0),
		       "f7Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F8, 0),
		       "f8Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F9, 0),
		       "f9Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F10, 0),
		       "f10Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F11, 0),
		       "f11Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F12, 0),
		       "f12Key"),
	//
	//
        // SHIFTED KEYS
	//
	//

	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_UP, InputEvent.SHIFT_MASK),
		       "upKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK),
		       "downKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK),
		       "leftKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK),
		       "rightKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK),
		       "insertKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK),
		       "deleteKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_HOME, InputEvent.SHIFT_MASK),
		       "homeKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_END, InputEvent.SHIFT_MASK),
		       "endKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_HELP, InputEvent.SHIFT_MASK),
		       "helpKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_UNDO, InputEvent.SHIFT_MASK),
		       "undoKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PAGE_UP, InputEvent.SHIFT_MASK),
		       "page_upKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PAGE_DOWN, InputEvent.SHIFT_MASK),
		       "page_downKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PLUS, InputEvent.SHIFT_MASK),
		       "plusKey"),
	// new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       // KeyEvent.VK_MINUS, InputEvent.SHIFT_MASK),
		       // "minusKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DIVIDE, InputEvent.SHIFT_MASK),
		       "divideKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_MULTIPLY, InputEvent.SHIFT_MASK),
		       "multiplyKey"),

	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F1, InputEvent.SHIFT_MASK),
		       "f1Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F2, InputEvent.SHIFT_MASK),
		       "f2Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F3, InputEvent.SHIFT_MASK),
		       "f3Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F4, InputEvent.SHIFT_MASK),
		       "f4Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F5, InputEvent.SHIFT_MASK),
		       "f5Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F6, InputEvent.SHIFT_MASK),
		       "f6Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F7, InputEvent.SHIFT_MASK),
		       "f7Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F8, InputEvent.SHIFT_MASK),
		       "f8Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F9, InputEvent.SHIFT_MASK),
		       "f9Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F10, InputEvent.SHIFT_MASK),
		       "f10Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F11, InputEvent.SHIFT_MASK),
		       "f11Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F12, InputEvent.SHIFT_MASK),
		       "f12Key"),
         
	//
	//
        // CTRL KEYS
	//
	//
 
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_UP, InputEvent.CTRL_MASK),
		       "upKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DOWN, InputEvent.CTRL_MASK),
		       "downKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_LEFT, InputEvent.CTRL_MASK),
		       "leftKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK),
		       "rightKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_INSERT, InputEvent.CTRL_MASK),
		       "insertKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DELETE, InputEvent.CTRL_MASK),
		       "deleteKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_HOME, InputEvent.CTRL_MASK),
		       "homeKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_END, InputEvent.CTRL_MASK),
		       "endKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_HELP, InputEvent.CTRL_MASK),
		       "helpKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_UNDO, InputEvent.CTRL_MASK),
		       "undoKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK),
		       "page_upKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK),
		       "page_downKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_PLUS, InputEvent.CTRL_MASK),
		       "plusKey"),
	// new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       // KeyEvent.VK_MINUS, InputEvent.CTRL_MASK),
		       // "minusKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_DIVIDE, InputEvent.CTRL_MASK),
		       "divideKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_MULTIPLY, InputEvent.CTRL_MASK),
		       "multiplyKey"),

	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F1, InputEvent.CTRL_MASK),
		       "f1Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F2, InputEvent.CTRL_MASK),
		       "f2Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F3, InputEvent.CTRL_MASK),
		       "f3Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F4, InputEvent.CTRL_MASK),
		       "f4Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F5, InputEvent.CTRL_MASK),
		       "f5Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F6, InputEvent.CTRL_MASK),
		       "f6Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F7, InputEvent.CTRL_MASK),
		       "f7Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F8, InputEvent.CTRL_MASK),
		       "f8Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F9, InputEvent.CTRL_MASK),
		       "f9Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F10, InputEvent.CTRL_MASK),
		       "f10Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F11, InputEvent.CTRL_MASK),
		       "f11Key"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_F12, InputEvent.CTRL_MASK),
		       "f12Key"),
 
	//
	//
	// other bindings
	//
	//

	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_SPACE, 0),
	  	     "spaceKey"),

	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_F, Event.CTRL_MASK),
	  	     "Ctrl-F"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_G, Event.CTRL_MASK),
	  	     "Ctrl-G"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_BACK_SPACE, 0),
	  	     "back_spaceKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_TAB, 0),
		       "tabKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_ENTER, 0),
	  	     "enterKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_N, Event.CTRL_MASK),
	  	     "Ctrl-N"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_P, Event.CTRL_MASK),
	  	     "Ctrl-P"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_ESCAPE, 0),
	  	     "escapeKey"),


	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_SPACE, InputEvent.SHIFT_MASK),
	  	     "spaceKey"),

	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_BACK_SPACE, InputEvent.SHIFT_MASK),
	  	     "back_spaceKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_TAB, InputEvent.SHIFT_MASK),
		       "tabKey"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
	  	     KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK),
	  	     "enterKey")
    };
    return bindings;
  }
  /**
   * initialize keymap.
   */
  public static Action[] getActions() {
    Action[] localActions = null;
    try {
      ViFactory factory = ViManager.getViFactory();
      localActions = new Action[] {
	  factory.createKeyAction("upKey", K_UP),
	  factory.createKeyAction("downKey", K_DOWN),
	  factory.createKeyAction("leftKey", K_LEFT),
	  factory.createKeyAction("rightKey", K_RIGHT),
	  factory.createKeyAction("insertKey", K_INS),
	  factory.createKeyAction("deleteKey", K_DEL),
	  factory.createKeyAction("tabKey", K_TAB),
	  factory.createKeyAction("homeKey", K_HOME),
	  factory.createKeyAction("endKey", K_END),
	  factory.createKeyAction("helpKey", K_HELP),
	  factory.createKeyAction("undoKey", K_UNDO),
	  factory.createKeyAction("back_spaceKey", KeyEvent.VK_BACK_SPACE),

	  factory.createKeyAction("page_upKey", K_PAGEUP),
	  factory.createKeyAction("page_downKey", K_PAGEDOWN),
	  factory.createKeyAction("plusKey", K_KPLUS),
	  factory.createKeyAction("minusKey", K_KMINUS),
	  factory.createKeyAction("divideKey", K_KDIVIDE),
	  factory.createKeyAction("multiplyKey", K_KMULTIPLY),
	  // factory.createKeyAction("enterKey", K_ENTER),

	  factory.createKeyAction("Ctrl-F", 6),
	  factory.createKeyAction("Ctrl-G", 7),
	  factory.createKeyAction("enterKey", KeyEvent.VK_ENTER), // 13
	  factory.createKeyAction("Ctrl-N", 14),
	  factory.createKeyAction("Ctrl-P", 16),
	  factory.createKeyAction("escapeKey", KeyEvent.VK_ESCAPE), // 27
	  factory.createKeyAction("spaceKey", KeyEvent.VK_SPACE),

	  factory.createKeyAction("f1Key", K_F1),
	  factory.createKeyAction("f2Key", K_F2),
	  factory.createKeyAction("f3Key", K_F3),
	  factory.createKeyAction("f4Key", K_F4),
	  factory.createKeyAction("f5Key", K_F5),
	  factory.createKeyAction("f6Key", K_F6),
	  factory.createKeyAction("f7Key", K_F7),
	  factory.createKeyAction("f8Key", K_F8),
	  factory.createKeyAction("f9Key", K_F9),
	  factory.createKeyAction("f10Key", K_F10),
	  factory.createKeyAction("f11Key", K_F11),
	  factory.createKeyAction("f12Key", K_F12)
      };
    } catch(Throwable e) {
      e.printStackTrace();
    }
    return localActions;
  }

  /** Read these as keys, not chars. */
  final private static boolean ignoreCtrlChars[] = {
    	false,		// 0
	false,
	false,
	false,
	false,		// 4
    	false,		// 5
	true,		// 6	Ctrl-F
	true,		// 7	Ctrl-G
	true,		// 8	backspace
	true,		// 9	tab
    	true,		// 10	return/enter
	false,
	false,
	false,
	true,		// 14	Ctrl-N
    	false,		// 15
	true,		// 16	Ctrl-P
	false,
	false,
	false,		// 19
    	false,
	false,
	false,
	false,
	false,		// 24
    	false,
	false,
	true,		// 27	escape
	false,
	false,		// 29
	false,		// 30
	false,		// 31
	true		// 32	space is special case
  };

  /** Test if the argument char should be ignored. Note that when
   * ignored as a char, it is generally is queued up as a key.
   */
  final static public boolean ignoreChar(int c) {
    return	c < KeyBinding.ignoreCtrlChars.length
	  	&& KeyBinding.ignoreCtrlChars[c];
  }
}
