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

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Set;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.TextAction;
import javax.swing.text.Caret;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.borland.primetime.editor.EditorAction;
import com.borland.primetime.editor.EditorPane;
import com.borland.primetime.editor.EditorManager;
import com.borland.primetime.ide.Browser;
import com.borland.primetime.ide.BrowserListener;
import com.borland.primetime.ide.NodeViewer;
import com.borland.primetime.ide.Context;
import com.borland.primetime.node.Node;
import com.borland.primetime.node.Project;
import com.borland.primetime.node.TextFileNode;
import com.borland.primetime.viewer.NodeViewMap;

import com.borland.jbuilder.insight.java.JavaInsightSettings;
import com.borland.jbuilder.jot.JotSourceFile;
import com.borland.jbuilder.node.JBProject;
import com.borland.jbuilder.jot.JotClass;
import com.borland.jbuilder.jot.JotClassSource;
import com.borland.jbuilder.jot.JotPackages;
import com.borland.jbuilder.node.JavaFileNode;

import com.raelity.text.RegExpFactory;

import com.raelity.jvi.Window;
import com.raelity.jvi.ViFS;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.KeyDefs;
import com.raelity.jvi.swing.TextOps;
import com.raelity.jvi.Edit;
import com.raelity.jvi.ViCmdEntry;

import com.raelity.jvi.ViManager;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.swing.DefaultViCaret;
import com.raelity.jvi.swing.ViCaret;
import com.raelity.jvi.swing.KeyBinding;
import com.raelity.jvi.swing.CommandLine;
import com.raelity.jvi.swing.WindowCmdEntry;

/**
 * This provides the Vi items to interface with standard swing JEditorPane.
 * One ViTextView is allocated per EditorPane.
 * For the JB port it does a variety of management functions as well, in
 * particular in response to browser events.
 */
public class JBViFactory implements ViFactory,
                                    KeyDefs,
                                    BrowserListener,
                                    ChangeListener
{
  static boolean browserMsgAllTrace = false;
  static boolean attachTrace = false;
  static boolean debugerDebug = true;

  Node currentNode;		// NOT USED
  Browser currentBrowser;	// NOT USED

  ViFS fs;

  /** map editorPane-key to textView-value structure.
   * This is not really a good case where to use weak map,
   * the value has references to the key. We manually need
   * to remove the references from the key, so we might as
   * well just remove the entries. But for now it provides
   * a builtin way to look for memory leaks.
   * NOTE: if textView had weak reference to key.....
   */
  Map textViews = new WeakHashMap(37);

  /** map node to per file info, e.g. marks. */
  Map nodeInfo = new HashMap();

  JBViFactory() {
    Browser.addStaticBrowserListener(this);
  }

  private void startupJBVi() {
    if( ! RegExpFactory.initFactory()) {
      RegExpFactory.addImplementation(
                              "org.jbopentools.editor.jbvi.RegExpJBuilder40");
      RegExpFactory.initFactory();
    }
    JBViOptions.initOptions();
    initCodeInsightHack();
  }

  /** @return the text view for the specified editorPane */
  public ViTextView getViTextView(JEditorPane editorPane) {
    ViTextView tv01 = (ViTextView)textViews.get(editorPane);
    if(tv01 == null) {
      tv01 = new JBTextView(NodeViewMap.getBrowser((EditorPane)editorPane),
                            (EditorPane)editorPane);
      tv01.setWindow(new Window(tv01));
      textViews.put(editorPane, tv01);
    }
    return tv01;
  }

  public ViFS getFS() {
    if(fs == null) {
      fs = new JBFS();
    }
    return fs;
  }

  static boolean firstTime = true;
  public void registerEditorPane(JEditorPane editorPane) {
    if(firstTime) {
      firstTime = false;
      startupJBVi();
    }
    // install cursor if neeeded
    Caret c = editorPane.getCaret();
    if( ! (c instanceof ViCaret)) {
      JBViCaret caret = new JBViCaret();
      editorPane.setCaret(caret);
      caret.setDot(c.getDot());
      caret.setBlinkRate(c.getBlinkRate());
      if(JBViKeymap.minorVersion == 0) {
	fixupCursorDisplay(editorPane, caret);
      }
      if(attachTrace)System.err.println("registerEditorPane caret: " + editorPane);
    }

  }

  /** Returns a Window for the current browser-node. The Window is
   * constructed if need be.
   */
  public Window lookupWindow(JEditorPane editorPane) {
    if(attachTrace)System.err.println("lookupWindow for: " + editorPane);

    return ViManager.getViTextView(editorPane).getWindow();
  }

  /**
   * Return an entry widget suitable for use with the indicated editorPane.
   */
  public ViCmdEntry createCmdEntry(int type) {
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

  void activateNode(Browser browser, Node node) {
    // dumpJot(node);
    //if( node instanceof TextFileNode) {
      // com.borland.jbuilder.node.java.JavaStructure
     // System.err.println("Class: " + ((TextFileNode)node).getTextStructureClass());
    //}
    /* *****************************************
    if(browser != currentBrowser) {
      if(attachTrace)System.err.println("activeNode; wrong browser: "
			 + currentBrowser + "\n" + browser + "\n" + node);
    }
    if( ! (node instanceof TextFileNode)) {
      currentNode = null;
    } else {
      currentNode = node;
    }
    return;
    ***************************************/
    if( ! (node instanceof TextFileNode)) {
      return;
    }
    ViManager.activateFile(browser, node);
  }

  void deactivateNode(Browser browser, Node node) {
    if(attachTrace)System.err.println("\n***** browserNodeClosed: " + browser + "\n" + node);
    if(currentNode == node) {
      currentNode = null;
    }
    ViManager.deactivateFile(browser, node);
    // remove node from maps
    // detach any text views
    Iterator iter = textViews.keySet().iterator();
    while(iter.hasNext()) {
      JEditorPane editorPane = (JEditorPane)iter.next();
      Browser b = NodeViewMap.getBrowser((EditorPane)editorPane);
      Node n = NodeViewMap.getNode((EditorPane)editorPane);
      if(b == browser && n == node) {
	ViTextView tv = getViTextView(editorPane);
	tv.detach();
      }
    }
    if(attachTrace)System.gc();
    if(attachTrace)System.err.println("textViews: " + textViews.size());
  }

  //
  // BrowserListener interface implementation
  //

  public void browserViewerDeactivating(Browser parm1,
					Node parm2,
					NodeViewer parm3)
	    throws com.borland.primetime.util.VetoException
  {
    if(attachTrace)System.err.println("\n***** browserViewerDeactivating: "
		       + parm1 + "\n" + parm2 + "\n" + parm3);
  }

  public void browserViewerActivated(Browser parm1,
				     Node parm2, NodeViewer parm3) {
    if(attachTrace)System.err.println("\n***** browserViewerActivated: "
		       + parm1 + "\n" + parm2 + "\n" + parm3);
  }

  public void browserNodeClosed(Browser browser, Node node) {
    deactivateNode(browser, node);
  }

  private static JavaInsightSettings javaInsightSettings;
  private static boolean ciMembersUserEnabled;
  private static boolean ciParamsUserEnabled;
  /**
   * CI auto popup needs to be turned off in commands mode so that navigation
   * commands, like " ", don't popup CI. The initial state of CI is recorded
   * at startup, and it is tracked as a property change event.
   * If it is disabled by the user, the it is never changed by this code.
   * <br>
   * NEEDSWORK: when param was done the code for member was just copied,
   *               it could be soem shared code, cleaner, easier understand.
   */
  void initCodeInsightHack() {
    javaInsightSettings = JavaInsightSettings.getInstance();
    javaInsightSettings.addChangeListener(this);
    ciMembersUserEnabled = javaInsightSettings.AUTO_MEMBERS.getBoolean();
    ciParamsUserEnabled = javaInsightSettings.AUTO_PARAMS.getBoolean();
    editorModeChange(Edit.VI_MODE_COMMAND);
  }

  /** listener for changes to CI state */
  public void stateChanged(ChangeEvent e) {
    if(JBViOptions.dbgCIHack.getBoolean())System.err.println("CI change: " + e);
    String mode = JBStatusDisplay.lastMode;
    if(mode == null) {
      mode = Edit.VI_MODE_COMMAND;
    }

    boolean mightChange = false;

    // has ciMembers Changed? is it what we expect?
    boolean ciOn = javaInsightSettings.AUTO_MEMBERS.getBoolean();
    if( ! ciMembersUserEnabled && ciOn) {
      // ci was just enabled by the user.
      ciMembersUserEnabled = true;
      mightChange = true;
    } else if(ciMembersUserEnabled && ciOn) {
      mightChange = true;
    } else if (ciMembersUserEnabled && !ciOn) {
      // it was turned off by the user
      ciMembersUserEnabled = false;
    }

    // has ciParams Changed? is it what we expect?
    ciOn = javaInsightSettings.AUTO_PARAMS.getBoolean();
    if( ! ciParamsUserEnabled && ciOn) {
      // ci was just enabled by the user.
      ciParamsUserEnabled = true;
      mightChange = true;
    } else if(ciParamsUserEnabled && ciOn) {
      mightChange = true;
    } else if (ciParamsUserEnabled && !ciOn) {
      // it was turned off by the user
      ciParamsUserEnabled = false;
    }
    if(mightChange) {
      editorModeChange(mode);
    }
  }

  static void editorModeChange(String mode) {
    if(mode.equals(Edit.VI_MODE_COMMAND)) {
      if(ciMembersUserEnabled) {
        javaInsightSettings.AUTO_MEMBERS.setBoolean(false);
        if(JBViOptions.dbgCIHack.getBoolean())System.err.println("reset memberCI");
      }
      if(ciParamsUserEnabled) {
        javaInsightSettings.AUTO_PARAMS.setBoolean(false);
        if(JBViOptions.dbgCIHack.getBoolean())System.err.println("reset paramCI");
      }
    } else {
      if(ciMembersUserEnabled) {
        javaInsightSettings.AUTO_MEMBERS.setBoolean(true);
        if(JBViOptions.dbgCIHack.getBoolean())System.err.println("set memberCI");
      }
      if(ciParamsUserEnabled) {
        javaInsightSettings.AUTO_PARAMS.setBoolean(true);
        if(JBViOptions.dbgCIHack.getBoolean())System.err.println("set paramCI");
      }
      EditorManager.setInsertMode(mode.equals(Edit.VI_MODE_INSERT));
    }
  }

  public void browserNodeActivated(Browser browser, Node node) {
    if(attachTrace)System.err.println("\n***** browserNodeActivated: "
		       + browser + "\n" + node);
    activateNode(browser, node);
  }

  public void browserProjectClosed(Browser parm1, Project parm2) {
    if(attachTrace)System.err.println("\n***** browserProjectClosed: "
		       + parm1 + "\n" + parm2);
  }

  public void browserProjectActivated(Browser parm1, Project parm2) {
    if(attachTrace)System.err.println("\n***** browserProjectActivated: "
		       + parm1 + "\n" + parm2);
  }

  public void browserClosed(Browser browser) {
    if(attachTrace)System.err.println("\n***** browserClosed: " + browser);
    if(currentBrowser == browser) {
      currentBrowser = null;
    }
    if(Browser.getBrowserCount() == 0) {
      if(ciMembersUserEnabled) {
        javaInsightSettings.AUTO_MEMBERS.setBoolean(true);
        if(JBViOptions.dbgCIHack.getBoolean())System.err.println("enabling memberCI for save");
      }
      if(ciParamsUserEnabled) {
        javaInsightSettings.AUTO_PARAMS.setBoolean(true);
        if(JBViOptions.dbgCIHack.getBoolean())System.err.println("enabling paramCI for save");
      }
    }
  }

  public void browserClosing(Browser browser)
	    throws com.borland.primetime.util.VetoException
  {
    if(browserMsgAllTrace)
      { System.err.println("\n***** browserClosing: " + browser); }
  }

  public void browserDeactivated(Browser browser) {
    if(browserMsgAllTrace)
      { System.err.println("\n***** browserDeactivated: " + browser); }
    // NEEDSWORK: can't null out current browser on deactivation
    // 			thinks its primarily a debug issue.
    // currentBrowser = null;
  }

  public void browserActivated(Browser browser) {
    if(browserMsgAllTrace)
      { System.err.println("\n***** browserActivated: " + browser); }
    currentBrowser = browser;
    activateNode(browser, browser.getActiveNode());
  }

  public void browserOpened(Browser browser) {
    if(browserMsgAllTrace)
      { System.err.println("\n***** browserOpened: " + browser); }
  }

  //
  // The action constructors for the vi keymap
  //

  /**
   * This is the default key action.
   */
  private static class EnqueCharAction extends EditorAction {
    public EnqueCharAction(String name) {
	super(name);
    }

    public void actionPerformed(ActionEvent e) {
      JEditorPane target = (JEditorPane)getEditorTarget(e);
      if(target != null && e != null) {
	String content = e.getActionCommand();
	int mod = e.getModifiers();
	if(content != null && content.length() > 0) {
	  int c = content.charAt(0);
	  if( ! KeyBinding.ignoreChar(c)) {
            if(KeyBinding.keyDebug.getBoolean()) {
              System.err.println("CharAction: " + ": " + c + " " + mod);
            }
	    ViManager.keyStroke(target, c, mod);
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
  private static class EnqueKeyAction extends EditorAction {
    int basekey;
    String name;	// NEEDSWORK: debug

    public EnqueKeyAction(String name, int key) {
	super(name);
	this.name = name;	// NEEDSWORK: debug
	this.basekey = key;
    }

    public void actionPerformed(ActionEvent e) {
      JEditorPane target = (JEditorPane)getEditorTarget(e);
      int mod = e.getModifiers();
      int key = basekey;
      if(KeyBinding.keyDebug.getBoolean()) {
        String virt = ((key & VIRT) != 0) ? "virt" : "";
        System.err.println("KeyAction: " + name + ": " + (key&~VIRT) + " " + mod + " " + virt);
      }
      ViManager.keyStroke(target, key, mod);
    }
  }

  /** workaround a lost listener problem in JB */
  static void fixupCursorDisplay(JEditorPane editorPane, Caret caret) {
    caret.addChangeListener(NodeViewMap.getView((EditorPane)editorPane)
					  .getStatus());
    // editorPane.addCaretListener(new fixupCursorDisplayListener());
  }

  private static Set jotSet = new HashSet();
  private static void dumpJot(Node node) {
    if( ! (node instanceof JavaFileNode)
        || jotSet.contains(node)) {
      return;
    }
    jotSet.add(node);
    printTopLevelClasses((JavaFileNode)node);
  }

  /**
   * Dump JOT stuff.
   */
  private static void printTopLevelClasses(JavaFileNode node) {
    JBProject jbProject = (JBProject)node.getProject();
    JotPackages jot = jbProject.getJotPackages();
    try {
       JotSourceFile jotFile = jot.getSourceFile(node.getUrl());
       JotClass[] jotClasses = jotFile.getClasses();
       System.err.println("\nClasses for file " + node.getDisplayName());
       for (int j = 0; j < jotClasses.length; j++) {
          System.err.println(jotClasses[j].getName());
       }
    }
    catch (Exception ex) {
    }
  }
}
