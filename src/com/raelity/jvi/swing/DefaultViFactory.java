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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import javax.swing.JComponent;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.TextAction;
import javax.swing.text.Caret;

import com.raelity.jvi.Window;
import com.raelity.jvi.ViFS;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.KeyDefs;
import com.raelity.jvi.swing.TextOps;
import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.DefaultViFS;
import com.raelity.jvi.*;

/**
 * This provides the Vi items to interface with standard swing JEditorPane.
 * <b>NEEDSWORK:</b><ul>
 * <li> only one text view supported for now
 * </ul>
 */
public class DefaultViFactory implements ViFactory, KeyDefs {

  Window window;
  ViTextView textView;
  CommandLine cmdLine;
  ViFS fs;

  public DefaultViFactory(CommandLine cmdLine) {
    this.cmdLine = cmdLine;
  }

  public ViTextView getViTextView(JEditorPane editorPane) {
    if(textView == null) {
      textView = new TextView();
      Window window = new Window(textView);
      textView.setWindow(window);
    }
    return textView;
  }

  public ViFS getFS() {
    if(fs == null) {
      fs = new DefaultViFS();
    }
    return fs;
  }

  /**
   * Register editor pane for use with vi. Install a
   * vi cursor. This is a nop
   * if already registered.
   */
  public void registerEditorPane(JEditorPane editorPane) {
    // install cursor if neeeded
    Caret c = editorPane.getCaret();
    if( ! (c instanceof ViCaret)) {
      DefaultViCaret caret = new DefaultViCaret();
      editorPane.setCaret(caret);
      caret.setDot(c.getDot());
      caret.setBlinkRate(c.getBlinkRate());
    }
  }

  public Window lookupWindow(JEditorPane editorPane) {
    ViTextView tv = ViManager.getViTextView(editorPane);
    return tv.getWindow();
  }

  public ViCmdEntry createCmdEntry(int type) {
    // ViCmdEntry cmdEntry = new DefaultCmdEntry(cmdLine);
    // return cmdEntry;
    
    // use this instead so that input is grabbed. When we have a
    // cleaner and more understandable key input state machine revisit
    // this.
    return new WindowCmdEntry(type);
  }

  /**
   * @return action suitable for default key action
   */
  public Action createCharAction(String name) {
    return new EnqueCharAction(name);
  }

  /**
   * @return action for picking up specified key
   */
  public Action createKeyAction(String name, int key) {
    return new EnqueKeyAction(name, key);
  }


  /**
   * This is the default key action.
   */
  private static class EnqueCharAction extends TextAction {
    public EnqueCharAction(String name) {
	super(name);
    }

    public void actionPerformed(ActionEvent e) {
      JEditorPane target = (JEditorPane)getTextComponent(e);
      if(target != null && e != null) {
	String content = e.getActionCommand();
	int mod = e.getModifiers();
	if(content != null && content.length() > 0) {
	  int c = content.charAt(0);
	  if( ! KeyBinding.ignoreChar(c)) {
            if(KeyBinding.keyDebug.getBoolean()) {
              System.err.println("CharAction: " + ": " + c + " " + mod);
            }
	    ViManager.keyStroke(target, content.charAt(0), mod);
	  }
	}
	else {
          if(KeyBinding.keyDebug.getBoolean()) {
            System.err.println("CharAction: " + e);
          }
	}
      }
    }
  }

  /**
   * Catch non-printing keys with this class. The constructor
   * specifies which key. The modifiers are handled by examining
   * the event and added to the key. Recieved characters are placed
   * on the vi input Q.
   */
  private static class EnqueKeyAction extends TextAction {
    int basekey;
    String name;

    public EnqueKeyAction(String name, int key) {
	super(name);
	this.basekey = key;
        this.name = name;
    }

    public void actionPerformed(ActionEvent e) {
      JEditorPane target = (JEditorPane)getTextComponent(e);
      int mod = e.getModifiers();
      int key = basekey;
      if(KeyBinding.keyDebug.getBoolean()) {
        String virt = ((key & 0xF000) == VIRT) ? "virt" : "";
        System.err.println("KeyAction: " + name + ": "
                           + (key&~VIRT) + " " + mod + " " + virt);
      }
      ViManager.keyStroke(target, key, mod);
    }
  }
}
