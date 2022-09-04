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

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

import com.google.common.eventbus.Subscribe;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.*;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;
import static com.raelity.jvi.manager.ViManager.getFactory;

public class KeyBinding {
  private static final Logger LOG = Logger.getLogger(KeyBinding.class.getName());
  public static final String KEY_BINDINGS = "KeyBinding";
  private static final Preferences prefs = getFactory()
                                .getPreferences().node(ViManager.PREFS_KEYS);

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=7)
    public static class Init implements ViInitialization
    {
      @Override
      public void init()
      {
        KeyBinding.init();
      }
    }

  private KeyBinding() {
  }

private static Action createKeyAction( String name, char key ) {
    return ((SwingFactory)getFactory())
            .createKeyAction(name, key);

}

  //static final String enqueKeyAction = "enque-key";

  private static void init() {
      createSubKeymaps();
      OptionEvent.getEventBus().register(new Object() {
          @Subscribe public void keys(OptionEvent.KeyOptions  evt) {
              // assert EventQueus.isEventDispatch();
              // some key options have changed
              bindingList = null; // force recalculation
              OptionEvent.fireOptionEvent(new OptionEvent.KeyBindings());
          }
      });

      // make sure "knownKeys" is filled in
      getBindingsListInternal();
      // TODO: FIX: The knownKeys is a HACK.
      setKnownKeys(tmpKnownKeys);
      tmpKnownKeys = null;
  }
  
  /**
   * Return a keymap for a standard swing text component.
   */

   public static Keymap getKeymap(String nm) {
    Keymap keymap = JTextComponent.addKeymap(nm, null);
    keymap.setDefaultAction(((SwingFactory)getFactory()).getDefaultAction());
    JTextComponent.loadKeymap(keymap, getBindings(), getActions());
    createSubKeymaps();
    return keymap;
  }
  
   public static Keymap getKeymap() {
     return getKeymap(null);
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
   * Bind the keys to actions of their own name. This is simply a
   * way to grab all the keys. May want to use key events
   * directly at some point.
   */
  public static JTextComponent.KeyBinding[] getBindings() {
    List<JTextComponent.KeyBinding> l = getBindingsListInternal();
    return l.toArray(JTextComponent.KeyBinding[]::new);
  }
  
  // NEEDSWORK: when bindings change, need to NULL this list
  // catch preference change then null list, then updateKeymap
  private static List<JTextComponent.KeyBinding> bindingList;
            

  /**
   * Return an ArrayList of bindings.
   */
  public static List<JTextComponent.KeyBinding> getBindingsList() {
      return Collections.unmodifiableList(getBindingsListInternal());
  }

  private static synchronized
  List<JTextComponent.KeyBinding> getBindingsListInternal() {

    if(bindingList != null) {
        return bindingList;
    }
    bindingList = new ArrayList<>();
    List<JTextComponent.KeyBinding> bl = bindingList;

    //
    // Add the normal control characters.
    // Check the preference for each one.
    //
    for(char c = 'A'; c <= 'Z'; c++)
    {
        // like: "Ctrl-A"
        String name = "Ctrl-" + String.valueOf(c);
        checkUseKey(bl, name, name, c, CTRL_DOWN_MASK);
    }
    
      //
      // Add the keypad characters,
      // Check the preference for each one.
      //
    Map<String, Integer> keypadNameMap = getKeypadNameKeyMap();
    for (String key : keypadNameMap.keySet()) {
        checkUseKey(bl, key, keypadNameMap.get(key), 0);
        checkUseKey(bl, key, keypadNameMap.get(key), CTRL_DOWN_MASK);
        checkUseKey(bl, key, keypadNameMap.get(key), SHIFT_DOWN_MASK);
    }
    
    //
    // There's always some wierdo's
    //
    checkUseKey(bl, "Ctrl-[", "Escape", KeyEvent.VK_OPEN_BRACKET, CTRL_DOWN_MASK);

    checkUseKey(bl, "Ctrl-]", "CloseBracket",
                KeyEvent.VK_CLOSE_BRACKET, CTRL_DOWN_MASK);
    
    // input mode bindings to shift line to align with paren in previous line
    // the ">" must be on period
    checkUseKey(bl, "Ctrl->", "PeriodCloseAngle", '.', CTRL_DOWN_MASK);
    // the "<" must be on comma
    checkUseKey(bl, "Ctrl-<", "CommaOpenAngle", ',', CTRL_DOWN_MASK);

    checkUseKey(bl, "Ctrl-@",  "Ctrl-@", KeyEvent.VK_2, CTRL_DOWN_MASK);
    checkUseKey(bl, "Ctrl-@",  "Ctrl-@", KeyEvent.VK_2, CTRL_DOWN_MASK|SHIFT_DOWN_MASK);

    // Allow the various flavors of Space
    checkUseKey(bl, "Space", KeyEvent.VK_SPACE, 0);
    checkUseKey(bl, "Space", KeyEvent.VK_SPACE, CTRL_DOWN_MASK);
    checkUseKey(bl, "Space", KeyEvent.VK_SPACE, SHIFT_DOWN_MASK);
    
    return bl;
  }
  
  private static void checkUseKey(List<JTextComponent.KeyBinding> bl,
                                  String key, int code, int mod) {
      String modTag = "";
      switch(mod) {
          case 0:           modTag = "";        break;
          case CTRL_DOWN_MASK:   modTag = "Ctrl-";   break;
          case SHIFT_DOWN_MASK:  modTag = "Shift-";  break;
          default: assert(false) : "mod = " + mod + ", not jVi modifier.";
      }
      String prefName = modTag + key; // like: "Ctrl-PageUp"
      checkUseKey(bl, prefName, key, code, mod);
 }
  
  private static Set<String> tmpKnownKeys = new HashSet<>();
  private static void checkUseKey(List<JTextComponent.KeyBinding> bl,
                                  String prefName,
                                  String actionName,
                                  int code,
                                  int mod) {
      if(tmpKnownKeys != null)
          tmpKnownKeys.add(prefName);
      if(prefs.getBoolean(prefName, getCatchKeyDefault(prefName))) {
          bl.add(createKeyBinding(code, mod, "Vi"+actionName+"Key"));
      }
  }

  static { if(Boolean.FALSE) NOT_USED_getExtraBindingsList(); }
  private static List<JTextComponent.KeyBinding> NOT_USED_getExtraBindingsList() {
    List<JTextComponent.KeyBinding> bl = new ArrayList<>();
    // NEEDSWORK: for now just stuff all the extra bindings here


    //
    //
    // other bindings
    //
    //

    // WHAT ARE THE FOLLOWING TWO FOR?

    //bl.add(createKeyBinding( KeyEvent.VK_CIRCUMFLEX, CTRL_MDOWN_ASK, "ViCtrl-CircumflexKey"));
    //bl.add(createKeyBinding( KeyEvent.VK_UNDERSCORE, CTRL_DOWN_MASK, "ViCtrl-UnderscoreKey"));


    bl.add(createKeyBinding(KeyEvent.VK_SPACE, SHIFT_DOWN_MASK, "ViSpaceKey"));

    bl.add(createKeyBinding(KeyEvent.VK_BACK_SPACE, SHIFT_DOWN_MASK, "ViBack_spaceKey"));
    bl.add(createKeyBinding(KeyEvent.VK_BACK_SLASH, CTRL_DOWN_MASK, "ViCtrl-BackslashKey"));
    bl.add(createKeyBinding(KeyEvent.VK_TAB, SHIFT_DOWN_MASK, "ViTabKey"));
    bl.add(createKeyBinding(KeyEvent.VK_ENTER, SHIFT_DOWN_MASK, "ViEnterKey"));
    
    return bl;
  }

  static { if(Boolean.FALSE) NOT_USED_getFunctionKeyBindingsList(); }
  private static List<JTextComponent.KeyBinding> NOT_USED_getFunctionKeyBindingsList() {
    List<JTextComponent.KeyBinding> bl = new ArrayList<>();

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

    bl.add(createKeyBinding(KeyEvent.VK_F1, SHIFT_DOWN_MASK, "ViF1Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F2, SHIFT_DOWN_MASK, "ViF2Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F3, SHIFT_DOWN_MASK, "ViF3Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F4, SHIFT_DOWN_MASK, "ViF4Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F5, SHIFT_DOWN_MASK, "ViF5Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F6, SHIFT_DOWN_MASK, "ViF6Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F7, SHIFT_DOWN_MASK, "ViF7Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F8, SHIFT_DOWN_MASK, "ViF8Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F9, SHIFT_DOWN_MASK, "ViF9Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F10, SHIFT_DOWN_MASK, "ViF10Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F11, SHIFT_DOWN_MASK, "ViF11Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F12, SHIFT_DOWN_MASK, "ViF12Key"));

    //
    // Control Function Keys
    //

    bl.add(createKeyBinding(KeyEvent.VK_F1, CTRL_DOWN_MASK, "ViF1Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F2, CTRL_DOWN_MASK, "ViF2Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F3, CTRL_DOWN_MASK, "ViF3Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F4, CTRL_DOWN_MASK, "ViF4Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F5, CTRL_DOWN_MASK, "ViF5Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F6, CTRL_DOWN_MASK, "ViF6Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F7, CTRL_DOWN_MASK, "ViF7Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F8, CTRL_DOWN_MASK, "ViF8Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F9, CTRL_DOWN_MASK, "ViF9Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F10, CTRL_DOWN_MASK, "ViF10Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F11, CTRL_DOWN_MASK, "ViF11Key"));
    bl.add(createKeyBinding(KeyEvent.VK_F12, CTRL_DOWN_MASK, "ViF12Key"));
    
    return bl;
  }


    /**
     * Return the available actions.
     * 
     * NEEDSWORK: only need actionsMap, use values to get all the actions.
     */
    private static Action[] getActions() {
      List<Action> l = getActionsListInternal();
      return l.toArray(Action[]::new);
    }
  
    private static List<Action> actionList;
    private static Map<String, Action> actionsMap;

    public static Action getAction(String key) {
        if(actionsMap == null)
            getActionsListInternal();
        return actionsMap.get(key);
    }
  
    public static List<Action> getActionsList() {
      return Collections.unmodifiableList(getActionsListInternal());
    }

    private static synchronized 
    List<Action> getActionsListInternal() {
        if(actionList == null)
            actionList = createActionList();
        return actionList;
    }
    
    private static List<Action> createActionList() {
        List<Action> actionsList = new ArrayList<>();
        try {
            for(Entry<String, Character> entry : KeyDefs.getNameKeyMap().entrySet()) {
                String keyName = entry.getKey();
                char keyVal = entry.getValue();
                actionsList.add(createKeyAction("Vi" + keyName + "Key", keyVal));
            }
        } catch(Throwable e) {
            LOG.log(Level.SEVERE, null, e);
        }

        actionsMap = new HashMap<>();
        for(Action a : actionsList) {
            TextAction ta = (TextAction) a;
            actionsMap.put((String) ta.getValue(Action.NAME), a);
        }

        return actionsList;
    }
  
  public static JTextComponent.KeyBinding[] getInsertModeBindings() {
    List<JTextComponent.KeyBinding> l = getInsertModeBindingsList();
    return l.toArray(JTextComponent.KeyBinding[]::new);
  }

  public static List<JTextComponent.KeyBinding> getInsertModeBindingsList() {

    List<JTextComponent.KeyBinding> bl = new ArrayList<>();

    bl.add(createKeyBinding(
		   KeyEvent.VK_PERIOD, CTRL_DOWN_MASK,
		   "ViInsert_indentNextParen"));
    bl.add(createKeyBinding(
		   KeyEvent.VK_COMMA, CTRL_DOWN_MASK,
		   "ViInsert_indentPrevParen"));
    bl.add(createKeyBinding(
		   KeyEvent.VK_T, CTRL_DOWN_MASK,
		   "ViInsert_shiftRight"));
    bl.add(createKeyBinding(
		   KeyEvent.VK_D, CTRL_DOWN_MASK,
		   "ViInsert_shiftLeft"));
    bl.add(createKeyBinding(
		   KeyEvent.VK_INSERT, 0,
		   "ViInsert_insertReplace"));
    return bl;
  }
  
  public static Action[] getInsertModeActions() {
    Action[] localActions = null;
    try {
      ViFactory factory = getFactory();
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
      LOG.log(Level.SEVERE, null, e);
    }
    return localActions;
  }
  
  private static JTextComponent.KeyBinding createKeyBinding(
                        int c, int modifiers, String bindKeyName) {
    JTextComponent.KeyBinding kb
            = new JTextComponent.KeyBinding(
		   KeyStroke.getKeyStroke(c, modifiers),
		   bindKeyName);
    return kb;
  }
  
}
