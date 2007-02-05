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
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.KeyStroke;

import com.raelity.jvi.KeyDefs;
import com.raelity.jvi.BooleanOption;
import com.raelity.jvi.Options;
import com.raelity.jvi.*;
import java.util.HashSet;
import java.util.Set;

import static java.awt.event.InputEvent.SHIFT_MASK;
import static java.awt.event.InputEvent.CTRL_MASK;

public class KeyBinding implements KeyDefs, Constants {
  public static final String PREF_KEYS = "KeyBindings";
  private static Preferences prefs = ViManager.getViFactory()
                                .getPreferences().node(KeyBinding.PREF_KEYS);

  private static BooleanOption keyDebugOption;
  public static final boolean isKeyDebug() {
    // NEEDSWORK: clean isKeyDebug up
    if(keyDebugOption == null) {
       keyDebugOption = (BooleanOption)Options.getOption(Options.dbgKeyStrokes);
    }
    if(keyDebugOption == null) {
      return false;
    } else {
      return keyDebugOption.getBoolean();
    }
  }
  
  public static boolean notImpDebug = false;

  public static final int MOD_MASK = SHIFT_MASK
	    				| CTRL_MASK
					| InputEvent.META_MASK
					| InputEvent.ALT_MASK;

  static final String enqueKeyAction = "enque-key";

  static Keymap sharedKeymap;
  /**
   * Return a shared keymap for a standard swing text component.
   * Also, if not already existing, construct insert and normal mode
   * keymaps only used for user
   * defined mappings.
   */
  public static Keymap getKeymap() {
      if(true) {
	  // NEEDSWORK: System.err.println("PRIVATE KEYMAP");
          return getPrivateKeymap();
      }
      if(sharedKeymap != null) {
          sharedKeymap = getPrivateKeymap();
      }
      // NEEDSWORK: System.err.println("SHARED KEYMAP");
      return sharedKeymap;
  }
  
  public static Keymap getPrivateKeymap() {
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

    List bl = new ArrayList();

    bl.add(createKeyBinding(KeyEvent.VK_UP, 0, "ViUpKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DOWN, 0, "ViDownKey"));
    bl.add(createKeyBinding(KeyEvent.VK_LEFT, 0, "ViLeftKey"));
    bl.add(createKeyBinding(KeyEvent.VK_RIGHT, 0, "ViRightKey"));
    bl.add(createKeyBinding(KeyEvent.VK_INSERT, 0, "ViInsertKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DELETE, 0, "ViDeleteKey"));
    bl.add(createKeyBinding(KeyEvent.VK_HOME, 0, "ViHomeKey"));
    bl.add(createKeyBinding(KeyEvent.VK_END, 0, "ViEndKey"));
    bl.add(createKeyBinding(KeyEvent.VK_HELP, 0, "ViHelpKey"));
    bl.add(createKeyBinding(KeyEvent.VK_UNDO, 0, "ViUndoKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PAGE_UP, 0, "ViPage_upKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PAGE_DOWN, 0, "ViPage_downKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PLUS, 0, "ViPlusKey"));
    // bindingList.add(createKeyBinding(KeyEvent.VK_MINUS, 0, "ViMinusKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DIVIDE, 0, "ViDivideKey"));
    bl.add(createKeyBinding(KeyEvent.VK_MULTIPLY, 0, "ViMultiplyKey"));

    //
    //
    // SHIFTED KEYS
    //
    //

    bl.add(createKeyBinding(KeyEvent.VK_UP, SHIFT_MASK, "ViUpKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DOWN, SHIFT_MASK, "ViDownKey"));
    bl.add(createKeyBinding(KeyEvent.VK_LEFT, SHIFT_MASK, "ViLeftKey"));
    bl.add(createKeyBinding(KeyEvent.VK_RIGHT, SHIFT_MASK, "ViRightKey"));
    bl.add(createKeyBinding(KeyEvent.VK_INSERT, SHIFT_MASK, "ViInsertKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DELETE, SHIFT_MASK, "ViDeleteKey"));
    bl.add(createKeyBinding(KeyEvent.VK_HOME, SHIFT_MASK, "ViHomeKey"));
    bl.add(createKeyBinding(KeyEvent.VK_END, SHIFT_MASK, "ViEndKey"));
    bl.add(createKeyBinding(KeyEvent.VK_HELP, SHIFT_MASK, "ViHelpKey"));
    bl.add(createKeyBinding(KeyEvent.VK_UNDO, SHIFT_MASK, "ViUndoKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PAGE_UP, SHIFT_MASK, "ViPage_upKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PAGE_DOWN, SHIFT_MASK, "ViPage_downKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PLUS, SHIFT_MASK, "ViPlusKey"));
    // bl.add(createKeyBinding(KeyEvent.VK_MINUS, SHIFT_MASK, "ViMinusKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DIVIDE, SHIFT_MASK, "ViDivideKey"));
    bl.add(createKeyBinding(KeyEvent.VK_MULTIPLY, SHIFT_MASK, "ViMultiplyKey"));
     
    //
    //
    // CTRL KEYS
    //
    //

    bl.add(createKeyBinding(KeyEvent.VK_UP, CTRL_MASK, "ViUpKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DOWN, CTRL_MASK, "ViDownKey"));
    bl.add(createKeyBinding(KeyEvent.VK_LEFT, CTRL_MASK, "ViLeftKey"));
    bl.add(createKeyBinding(KeyEvent.VK_RIGHT, CTRL_MASK, "ViRightKey"));
    bl.add(createKeyBinding(KeyEvent.VK_INSERT, CTRL_MASK, "ViInsertKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DELETE, CTRL_MASK, "ViDeleteKey"));
    bl.add(createKeyBinding(KeyEvent.VK_HOME, CTRL_MASK, "ViHomeKey"));
    bl.add(createKeyBinding(KeyEvent.VK_END, CTRL_MASK, "ViEndKey"));
    bl.add(createKeyBinding(KeyEvent.VK_HELP, CTRL_MASK, "ViHelpKey"));
    bl.add(createKeyBinding(KeyEvent.VK_UNDO, CTRL_MASK, "ViUndoKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PAGE_UP, CTRL_MASK, "ViPage_upKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PAGE_DOWN, CTRL_MASK, "ViPage_downKey"));
    bl.add(createKeyBinding(KeyEvent.VK_PLUS, CTRL_MASK, "ViPlusKey"));
    bl.add(createKeyBinding(KeyEvent.VK_MINUS, CTRL_MASK, "ViMinusKey"));
    bl.add(createKeyBinding(KeyEvent.VK_DIVIDE, CTRL_MASK, "ViDivideKey"));
    bl.add(createKeyBinding(KeyEvent.VK_MULTIPLY, CTRL_MASK, "ViMultiplyKey"));

    //
    //
    // other bindings
    //
    //

    //bl.add(createKeyBinding(KeyEvent.VK_SPACE, 0, "ViSpaceKey"));

    bl.add(createKeyBinding(KeyEvent.VK_BACK_SPACE, 0, "ViBack_spaceKey"));

    bl.add(createKeyBinding(KeyEvent.VK_TAB, 0, "ViTabKey"));

    bl.add(createKeyBinding(KeyEvent.VK_ENTER, 0, "ViEnterKey"));

    bl.add(createKeyBinding(KeyEvent.VK_OPEN_BRACKET, Event.CTRL_MASK,
			    "ViEscapeKey"));      // alternate 
    bl.add(createKeyBinding(KeyEvent.VK_ESCAPE, 0, "ViEscapeKey"));

    bl.add(createKeyBinding(KeyEvent.VK_CLOSE_BRACKET, Event.CTRL_MASK,
			    "ViCtrl-ClosebracketKey"));
    
    // input mode bindings to shift line to align with paren in previous line
    // the ">" must be on period
    bl.add(createKeyBinding('.', CTRL_MASK, "ViPeriodCloseAngle"));
    // the "<" must be on comma
    bl.add(createKeyBinding(',', CTRL_MASK, "ViCommaOpenAngle"));

    //
    // Add the normal control characters.
    // Check the preference for each one.
    //
    for(char c = 'A'; c <= 'Z'; c++) {
      String keyName = "Ctrl-" + new String(new char[] {c});
      if(prefs.getBoolean(keyName, getCatchKeyDefault(keyName))) {
          bl.add(createKeyBinding(c, Event.CTRL_MASK, "Vi"+keyName));
      }
    }
    
    return bl;
  }

  public static List getExtraBindingsList() {
    List bl = new ArrayList();
    // NEEDSWORK: for now just stuff all the extra bindings here


    //
    //
    // other bindings
    //
    //

    // WHAT ARE THE FOLLOWING TWO FOR?

    //bl.add(createKeyBinding( KeyEvent.VK_CIRCUMFLEX, CTRL_MASK, "ViCtrl-CircumflexKey"));
    //bl.add(createKeyBinding( KeyEvent.VK_UNDERSCORE, CTRL_MASK, "ViCtrl-UnderscoreKey"));


    bl.add(createKeyBinding(KeyEvent.VK_SPACE, SHIFT_MASK, "ViSpaceKey"));

    bl.add(createKeyBinding(KeyEvent.VK_BACK_SPACE, SHIFT_MASK, "ViBack_spaceKey"));
    bl.add(createKeyBinding(KeyEvent.VK_BACK_SLASH, CTRL_MASK, "ViCtrl-BackslashKey"));
    bl.add(createKeyBinding(KeyEvent.VK_TAB, SHIFT_MASK, "ViTabKey"));
    bl.add(createKeyBinding(KeyEvent.VK_ENTER, SHIFT_MASK, "ViEnterKey"));
    
    return bl;
  }

  public static List getFunctionKeyBindingsList() {
    List bl = new ArrayList();

    //
    // Function keys
    //

    bl.add(createKeyBinding(KeyEvent.VK_F1, 0, "ViF1Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F2, 0, "ViF2Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F3, 0, "ViF3Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F4, 0, "ViF4Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F5, 0, "ViF5Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F6, 0, "ViF6Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F7, 0, "ViF7Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F8, 0, "ViF8Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F9, 0, "ViF9Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F10, 0, "ViF10Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F11, 0, "ViF11Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F12, 0, "ViF12Key"));

    //
    // Shifted Function Keys
    //

    bl.add(createKeyBinding(KeyEvent.VK_F1, SHIFT_MASK, "ViF1Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F2, SHIFT_MASK, "ViF2Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F3, SHIFT_MASK, "ViF3Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F4, SHIFT_MASK, "ViF4Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F5, SHIFT_MASK, "ViF5Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F6, SHIFT_MASK, "ViF6Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F7, SHIFT_MASK, "ViF7Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F8, SHIFT_MASK, "ViF8Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F9, SHIFT_MASK, "ViF9Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F10, SHIFT_MASK, "ViF10Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F11, SHIFT_MASK, "ViF11Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F12, SHIFT_MASK, "ViF12Key"));

    //
    // Control Function Keys
    //

    bl.add(createKeyBinding(KeyEvent.VK_F1, CTRL_MASK, "ViF1Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F2, CTRL_MASK, "ViF2Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F3, CTRL_MASK, "ViF3Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F4, CTRL_MASK, "ViF4Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F5, CTRL_MASK, "ViF5Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F6, CTRL_MASK, "ViF6Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F7, CTRL_MASK, "ViF7Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F8, CTRL_MASK, "ViF8Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F9, CTRL_MASK, "ViF9Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F10, CTRL_MASK, "ViF10Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F11, CTRL_MASK, "ViF11Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F12, CTRL_MASK, "ViF12Key"));
    
    return bl;
  }


  /**
   * initialize keymap.
   */
  public static Action[] getActions() {
    List l = getActionsList();
    return (Action[]) l.toArray(new Action[l.size()]);
  }
  
  public static List getActionsList() {
    List actionsList = new ArrayList();
    try {
      ViFactory factory = ViManager.getViFactory();
      actionsList.add(factory.createKeyAction("ViUpKey", K_UP));
      actionsList.add(factory.createKeyAction("ViDownKey", K_DOWN));
      actionsList.add(factory.createKeyAction("ViLeftKey", K_LEFT));
      actionsList.add(factory.createKeyAction("ViRightKey", K_RIGHT));
      actionsList.add(factory.createKeyAction("ViInsertKey", K_INS));
      actionsList.add(factory.createKeyAction("ViDeleteKey", K_DEL));
      actionsList.add(factory.createKeyAction("ViTabKey", K_TAB));
      actionsList.add(factory.createKeyAction("ViHomeKey", K_HOME));
      actionsList.add(factory.createKeyAction("ViEndKey", K_END));
      actionsList.add(factory.createKeyAction("ViHelpKey", K_HELP));
      actionsList.add(factory.createKeyAction("ViUndoKey", K_UNDO));
      actionsList.add(factory.createKeyAction("ViBack_spaceKey", KeyEvent.VK_BACK_SPACE));

      actionsList.add(factory.createKeyAction("ViPage_upKey", K_PAGEUP));
      actionsList.add(factory.createKeyAction("ViPage_downKey", K_PAGEDOWN));
      actionsList.add(factory.createKeyAction("ViPlusKey", K_KPLUS));
      actionsList.add(factory.createKeyAction("ViMinusKey", K_KMINUS));
      actionsList.add(factory.createKeyAction("ViDivideKey", K_KDIVIDE));
      actionsList.add(factory.createKeyAction("ViMultiplyKey", K_KMULTIPLY));
      // NEEDSWORK: bring in ViEnterKey as K_KENTER, not as KeyEvent.VK_ENTER
      actionsList.add(factory.createKeyAction("ViEnterKey", K_KENTER));

      actionsList.add(factory.createKeyAction("ViPeriodCloseAngle", K_X_PERIOD));
      actionsList.add(factory.createKeyAction("ViCommaOpenAngle", K_X_COMMA));
      
      actionsList.add(factory.createKeyAction("ViCtrl-A", 1));
      actionsList.add(factory.createKeyAction("ViCtrl-B", 2));
      actionsList.add(factory.createKeyAction("ViCtrl-C", 3));
      actionsList.add(factory.createKeyAction("ViCtrl-D", 4));
      actionsList.add(factory.createKeyAction("ViCtrl-E", 5));
      actionsList.add(factory.createKeyAction("ViCtrl-F", 6));
      actionsList.add(factory.createKeyAction("ViCtrl-G", 7));
      actionsList.add(factory.createKeyAction("ViCtrl-H", 8));
      actionsList.add(factory.createKeyAction("ViCtrl-I", 9));
      actionsList.add(factory.createKeyAction("ViCtrl-J", 10));
      actionsList.add(factory.createKeyAction("ViCtrl-K", 11));
      actionsList.add(factory.createKeyAction("ViCtrl-L", 12));
      actionsList.add(factory.createKeyAction("ViCtrl-M", 13));
      //actionsList.add(factory.createKeyAction("ViEnterKey", KeyEvent.VK_ENTER)); // 13
      actionsList.add(factory.createKeyAction("ViCtrl-N", 14));
      actionsList.add(factory.createKeyAction("ViCtrl-O", 15));
      actionsList.add(factory.createKeyAction("ViCtrl-P", 16));
      actionsList.add(factory.createKeyAction("ViCtrl-Q", 17));
      actionsList.add(factory.createKeyAction("ViCtrl-R", 18));
      actionsList.add(factory.createKeyAction("ViCtrl-S", 19));
      actionsList.add(factory.createKeyAction("ViCtrl-T", 20));
      actionsList.add(factory.createKeyAction("ViCtrl-U", 21));
      actionsList.add(factory.createKeyAction("ViCtrl-V", 22));
      actionsList.add(factory.createKeyAction("ViCtrl-W", 23));
      actionsList.add(factory.createKeyAction("ViCtrl-X", 24));
      actionsList.add(factory.createKeyAction("ViCtrl-Y", 25));
      actionsList.add(factory.createKeyAction("ViCtrl-Z", 26));
      actionsList.add(factory.createKeyAction("ViEscapeKey", KeyEvent.VK_ESCAPE)); // 27
      actionsList.add(factory.createKeyAction("ViCtrl-BackslashKey", 28));
      actionsList.add(factory.createKeyAction("ViCtrl-ClosebracketKey", 29));
      //actionsList.add(factory.createKeyAction("ViCtrl-CircumflexKey", 30));
      //actionsList.add(factory.createKeyAction("ViCtrl-UnderscoreKey", 31));
      actionsList.add(factory.createKeyAction("ViSpaceKey", KeyEvent.VK_SPACE));

      actionsList.add(factory.createKeyAction("ViF1Key", K_F1));
      actionsList.add(factory.createKeyAction("ViF2Key", K_F2));
      actionsList.add(factory.createKeyAction("ViF3Key", K_F3));
      actionsList.add(factory.createKeyAction("ViF4Key", K_F4));
      actionsList.add(factory.createKeyAction("ViF5Key", K_F5));
      actionsList.add(factory.createKeyAction("ViF6Key", K_F6));
      actionsList.add(factory.createKeyAction("ViF7Key", K_F7));
      actionsList.add(factory.createKeyAction("ViF8Key", K_F8));
      actionsList.add(factory.createKeyAction("ViF9Key", K_F9));
      actionsList.add(factory.createKeyAction("ViF10Key", K_F10));
      actionsList.add(factory.createKeyAction("ViF11Key", K_F11));
      actionsList.add(factory.createKeyAction("ViF12Key", K_F12));
    } catch(Throwable e) {
      e.printStackTrace();
    }
    return actionsList;
  }
  
  public static JTextComponent.KeyBinding[] getInsertModeBindings() {
    List l = getInsertModeBindingsList();
    return (JTextComponent.KeyBinding[]) l.toArray(
                              new JTextComponent.KeyBinding[l.size()]);
  }

  public static List getInsertModeBindingsList() {

    List bindingList = new ArrayList();

    bindingList.add(createKeyBinding(
		   KeyEvent.VK_PERIOD, CTRL_MASK,
		   "ViInsert_indentNextParen"));
    bindingList.add(createKeyBinding(
		   KeyEvent.VK_COMMA, CTRL_MASK,
		   "ViInsert_indentPrevParen"));
    bindingList.add(createKeyBinding(
		   KeyEvent.VK_T, CTRL_MASK,
		   "ViInsert_shiftRight"));
    bindingList.add(createKeyBinding(
		   KeyEvent.VK_D, CTRL_MASK,
		   "ViInsert_shiftLeft"));
    bindingList.add(createKeyBinding(
		   KeyEvent.VK_INSERT, 0,
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
  
  /*
   * This method return an array which maps special vi keys, from
   * {@link KeyDefs}, to Java KeyEventt keys, which are turned into key strokes.
   */
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
  
  private static JTextComponent.KeyBinding createKeyBinding(
                        int c, int modifiers, String bindKeyName) {
    return new JTextComponent.KeyBinding(
		   KeyStroke.getKeyStroke(c, modifiers),
		   bindKeyName);
  }
  //
  // Handle the preferences for which keys to catch
  //
  
  static public boolean getCatchKeyDefault(String keyName) {
      return keyBindingPrefs.getCatchKeyDefault(keyName);
  }
  
  static private KeyBindingPrefs keyBindingPrefs = new KeyBindingPrefs();
  static private class KeyBindingPrefs {
      private Set<String> defaultKeysFalse = new HashSet();
      KeyBindingPrefs() {
          defaultKeysFalse.add("Ctrl-A");
          defaultKeysFalse.add("Ctrl-C");
          defaultKeysFalse.add("Ctrl-I");
          defaultKeysFalse.add("Ctrl-J");
          defaultKeysFalse.add("Ctrl-K");
          defaultKeysFalse.add("Ctrl-Q");
          defaultKeysFalse.add("Ctrl-V");
          defaultKeysFalse.add("Ctrl-X");
          defaultKeysFalse.add("Ctrl-Z");
      }
      
      public Boolean getCatchKeyDefault(String keyName) {
          return ! defaultKeysFalse.contains(keyName);
      }
  }
}
