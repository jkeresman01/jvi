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
package com.raelity.jvi;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;

import com.raelity.jvi.ViFS;
import com.raelity.jvi.G;
import com.raelity.jvi.Normal;
import com.raelity.jvi.Misc;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.Window;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.GetChar;
import com.raelity.jvi.Options;
import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.KeyDefs;
import com.raelity.jvi.Msg;
import com.raelity.tools.fixup13.*;
import com.raelity.jvi.swing.*;

/**
 * This class coordinates things.
 * <b>NEEDSWORK:</b><ul>
 * </ul>
 */
public class ViManager implements Constants {

  static private JEditorPane currentEditorPane;
  static private ViFactory factory;

  static private Keymap editModeKeymap;
  static private Keymap normalModeKeymap;

  // HACK: to workaround JDK bug dealing with focus and JWindows
  public static ViCmdEntry activeCommandEntry;

  private static int majorVersion = 0;
  private static int minorVersion = 7;
  private static int microVersion = 1;
  private static String releaseTag = "";
  private static String release = "jVi "
                    + ViManager.majorVersion
		    + "." + ViManager.minorVersion
		    + "." + ViManager.microVersion
                    + ViManager.releaseTag;

  public static void setViFactory(ViFactory factory) {
    if(ViManager.factory != null) {
      throw new RuntimeException("ViFactory already set");
    }
    Options.init();
    ViManager.factory = factory;

    // KeyBinding.init(); // setup the default keymap
    KeyBinding.getKeymap();  // force the class loaded before the action starts
  }

  public static String getReleaseString() {
    return release;
  }

  public static int getMajorVersion() {
    return majorVersion;
  }

  public static int getMinorVersion() {
    return minorVersion;
  }

  public static int getMicroVersion() {
    return microVersion;
  }

  public static String getReleaseTag() {
    return releaseTag;
  }

  public static ViFactory getViFactory() {
    return factory;
  }

  public static ViFS getFS() {
    return factory.getFS();
  }

  public static JEditorPane getCurrentEditorPaneXXX() {
    return currentEditorPane;
  }

  public static ViTextView getViTextView(JEditorPane editorPane) {
    return factory.getViTextView(editorPane);
  }

  static public void installKeymap(JEditorPane editorPane) {
    editorPane.setKeymap(KeyBinding.getKeymap());
  }

  /**
   * Pass control to indicated ViCmdEntry widget. If there are
   * readahead or typeahead characters available, then collect
   * them up to a &lt;CR&gt; and append them to initialString.
   * If there was a CR, then signal the widget to immeadiately
   * fire its actionPerformed without displaying any UI element.
   */
  public static void startCommandEntry(ViCmdEntry commandEntry,
                                       String mode,
                                       ViTextView tv,
                                       StringBuffer initialString)
  {
    Msg.clearMsg();
    if(initialString == null) {
      initialString = new StringBuffer();
    }
    if(activeCommandEntry != null) {
      throw new RuntimeException("activeCommandEntry not null");
    }

    activeCommandEntry = commandEntry;
    boolean passThru = GetChar.getRecordedLine(initialString);
    commandEntry.activate(mode, tv, new String(initialString), passThru);
  }

  public static void stopCommandEntry() {
    activeCommandEntry = null;
  }

  // NEEDSWORK: textMRU: use a weak reference to fileObject?
  private static List textBuffers = new ArrayList();
  private static List textMRU = new LinkedList();

  /**
   * Fetch the text buffer indicated by the argument. If the argument is
   * positive, then fetch the Nth buffer, numbered 1 to N, according to
   * the order they were activated. If the argument is negative then use
   * the MRU list to get the buffer, where -1
   * means the previous buffer. An argument of 0 will return null.
   */
  public static Object getTextBuffer(int i) {
    if(i == 0) {
      return null;
    }
    List list = null;
    if(i < 0) {
      list = textMRU;
      i = -i;
    } else if(i > 0) {
      list = textBuffers;
      i = i - 1;
    }
    if(i >= list.size()) {
      return null;
    }
    return list.get(i);
  }

  /**
   * The application invokes this whenever a file becomes selected
   * in the specified container.
   */
  public static void activateFile(Object fileContainer, Object fileObject) {
    textMRU.remove(fileObject);
    textMRU.add(0, fileObject);
    if( ! textBuffers.contains(fileObject)) {
      textBuffers.add(fileObject);
    }
  }

  /**
   * The applications invokes this method when a file is completely
   * removed from a container.
   */
  public static void deactivateFile(Object fileContainer, Object fileObject) {
    textMRU.remove(fileObject);
    textBuffers.remove(fileObject);
  }

  /**
   * Set up an editor pane for use with vi.
   * This is a nop if already registered.
   */
  public static void registerEditorPane(JEditorPane editorPane) {
    FixupInputMap.fixupInputMap(editorPane);
    factory.registerEditorPane(editorPane);
  }

  /**
   * A key was typed. Handle the event.
   * <br>NEEDSWORK: catch all exceptions comming out of here?
   */
  static public void keyStroke(JEditorPane target, int key, int modifier) {
    if(false && KeyBinding.keyDebug.getBoolean()) { // DEBUG
      boolean changeIt = false;
      if(modifier == MOD_MASK_ALT) {
	switch (key) {
	  case 0x30:
	    changeIt = true;
	    key = 128; // display as a box
	    break;
	  case 0x31:
	    changeIt = true;
	    key = 231;
	    break;
	  case 0x32:
	    changeIt = true;
	    key = 232;
	    break;
	  case 0x33:
	    changeIt = true;
	    key = 233;
	    break;
	  case 0x34:
	    changeIt = true;
	    key = 234;
	    break;
	}
	if(changeIt) {
	  modifier = 0;
	  System.err.println("KeyChange '" + (char)key + "'");
	}
      }
    }
    if(target != currentEditorPane) {
      if(currentEditorPane != null) {
        Normal.resetCommand();
      }
      switchTo(target);
      Normal.resetCommand(); // dont think this is needed
    }
    if(rerouteChar(key, modifier)) {
      return;
    }
    GetChar.gotc(key, modifier);
  }

  /** If chars came in between the time a dialog was initiated and
   * the time the dialog starts taking the characters, we feed the
   * chars to the dialog.
   * <p>Special characters are discarded.
   * </p>
   */
  static boolean rerouteChar(int c, int modifiers) {
    if(activeCommandEntry == null) {
      return false;
    }
    if((c & 0xF000) != KeyDefs.VIRT
              && modifiers == 0) {
      if(c >= 0x20 && c < 0x7f) {
        String content = new String(new char[] {(char)c});
        activeCommandEntry.append(content);
      }
    }
    // System.err.println("rerouteChar " + (char)c);
    return true;
  }

  private static boolean started = false;
  static void switchTo(JEditorPane editorPane) {
    if( ! started) {
      started = true;
      startup();
    }
    registerEditorPane(editorPane); // make sure its registered

    // first do lookup, in case new window/editorPane
    Window window = factory.lookupWindow(editorPane);
    ViTextView textView = getViTextView(editorPane);
    textView.switchTo(editorPane);
    G.switchTo(textView);
    currentEditorPane = editorPane;
  }

  private static boolean inStartup;
  /** invoked once when vi is first used */
  private static void startup() {
    setupStartupList();
    inStartup = true;
    Iterator iter = startupList.iterator();
    while(iter.hasNext()) {
      ((ActionListener)iter.next()).actionPerformed(null);
    }
    Misc.javaKeyMap = KeyBinding.initJavaKeyMap();
    inStartup = false;
    startupList = null;
  }

  static List startupList;
  static void setupStartupList() {
    if(startupList == null) {
      startupList = new ArrayList();
    }
  }

  /**
   * Add listener to invoke when editor is starting up.
   * A null argument can be used to test if startup has
   * already occured.
   * @return true if listener add, otherwise false indicates
   * that startup has already occured.
   */
  public static boolean addStartupListener(ActionListener l) {
    if(started) {
      return false;
    }
    if(l != null) {
      setupStartupList();
      startupList.add(l);
    }
    return true;
  }

  public static void removeStartupListener(ActionListener l) {
    if(inStartup) {
      return;
    }
    startupList.remove(l);
  }

  /**
   * A mouse click; switch to the activated editor.
   * Pass the click on to the window and give it
   * a chance to adjust the position and whatever.
   */
  public static int mouseClickedPosition(int pos, JTextComponent c) {
    if( ! (c instanceof JEditorPane)) {
      return pos;
    }

    JEditorPane editorPane = (JEditorPane)c;

    // NEEDSWORK: mouse click: if( ! isRegistered(editorPane)) {}

    GetChar.flush_buffers(true);
    if(currentEditorPane != null) {
      Normal.resetCommand();
    }
    if(editorPane != currentEditorPane) {
      switchTo(editorPane);
    }

    ViTextView textView = getViTextView(editorPane);
    Window window = factory.lookupWindow(editorPane);
    pos = window.mouseClickedPosition(pos);
    return pos;
  }

  /** A mouse click may have moved the caret. */
  public static void unexpectedCaretChange(int dot) {
    // XXX verify mouse is at an acceptable location
  }

  /** The viewport has changed, so number of screen lines have changed */
  public static void viewSizeChange(ViTextView textView) {
    try {
      Window window = factory.lookupWindow(textView.getEditorComponent());
      window.viewSizeChange();
    }
    catch (NonExistentWindowException ex) {
    }
  }

  /** The viewport has changed or scrolled, clear messages*/
  public static void viewMoveChange(ViTextView textView) {
    if(G.curwin == null) {
      // this case is because switchto, does attach, does viewport init
      // but G.curwin is not set yet. See switchTo(JEditorPane editorPane)
      return;
    }
    Msg.clearMsg();
  }

  /** set the previous context to the indicated offset */
  public static void previousContextHack(ViTextView textView, ViMark mark) {
    Window window = factory.lookupWindow(textView.getEditorComponent());
    window.previousContextHack(mark);
  }

  /**
   * Listen to carets events for newly registered JEditorPanes.
   * If an event comes in, assign the editorPane to a TextView
   * and give it the oportunity to re-adjust the cursor.
   * <br><b>NEEDSWORK:</b><ul>
   * <li> work this out with textview, where this should be......
   * <ul>
   */
  static void fixupCaret() {
  }

  static public void dumpStack(String msg) {
    try {
      throw new IllegalStateException(msg);
    } catch(IllegalStateException ex) {
      ex.printStackTrace();
    }
  }

  static public void dumpStack() {
    try {
      throw new IllegalStateException();
    } catch(IllegalStateException ex) {
      ex.printStackTrace();
    }
  }

  static public void setInsertModeKeymap(Keymap newInsertModeKeymap) {
    editModeKeymap = newInsertModeKeymap;
  }

  static public Keymap getInsertModeKeymap() {
    return editModeKeymap;
  }

  static public void setNormalModeKeymap(Keymap newNormalModeKeymap) {
    normalModeKeymap = newNormalModeKeymap;
  }

  static public Keymap getNormalModeKeymap() {
    return normalModeKeymap;
  }

  static public ActionListener xlateKeymapAction(ActionListener act) {
    return factory.xlateKeymapAction(act);
  }
}

