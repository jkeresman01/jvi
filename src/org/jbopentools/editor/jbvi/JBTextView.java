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

import java.util.List;
import java.util.ArrayList;
import java.awt.Component;
import java.awt.Container;

import javax.swing.JEditorPane;
import javax.swing.text.Caret;
import javax.swing.SwingUtilities;

import com.borland.primetime.ide.Browser;
import com.borland.primetime.ide.NodeViewer;
import com.borland.primetime.editor.EditorManager;
import com.borland.primetime.editor.EditorPane;
import com.borland.primetime.editor.EditorActions;
import com.borland.primetime.editor.SearchManager;
import com.borland.primetime.editor.SearchOptions;
import com.borland.primetime.viewer.NodeViewMap;
import com.borland.primetime.viewer.TextViewerComponent;
import com.borland.primetime.ui.Splitter;
import com.borland.primetime.node.FileNode;
import com.borland.primetime.node.Node;

import com.raelity.jvi.G;
import com.raelity.jvi.Window;
import com.raelity.jvi.Msg;
import com.raelity.jvi.Constants;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.Misc;
import com.raelity.jvi.swing.TextView;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.Util;

import com.raelity.jvi.swing.TextViewCache;
import com.raelity.jvi.swing.DefaultOutputStream;

/**
 * Pretty much the TextView used for standard swing.
 */
public class JBTextView extends TextView
			implements Constants
{
  
  JBTextView(Browser browser, EditorPane editorPane) {
    cache = createTextViewCache();
    statusDisplay = new JBStatusDisplay(browser, editorPane);
  }
  
  protected TextViewCache createTextViewCache() {
    return new JBTextViewCache(this);
  }
  
  protected void createOps(JEditorPane editorPane) {
    ops = new Ops(this);
    ops.init(editorPane);
  }
  
  /**
   * HACK: override this method so that we can turn off
   * isEditable for readonly files.
   * @param editorPane editor pane that is current
   */
  public void switchTo(JEditorPane editorPane) {
    super.switchTo(editorPane);
    ReadOnlyHack.switchTo(editorPane);
  }
  
  /**
   * This method assumes that it will only be called for the active editor.
   * @return true if the text can be changed
   */
  public boolean isEditable() {
    return ReadOnlyHack.isEditable();
  }
  
  /**
   * Notification of changing mode, command/modify.
   * Job is to make sure a read only pane is not modified
   * @param modify true if entering a modification mode
   */
  static void setModify(boolean modify) {
    ReadOnlyHack.setModify(modify);
  }
  
  /**
   * State of read only option has changed. Make any adjustments.
   */
  static void changeReadOnlyHackOption() {
    ReadOnlyHack.changeReadOnlyHackOption();
  }

  /**
   * Use the JBuilder Node.
   * @return opaque FileObject backing this EditorPane
   */
  public Object getFileObject() {
    return NodeViewMap.getNode((EditorPane)editorPane);
  }

  public ViOutputStream createOutputStream(Object type, Object info) {
    if(type == ViOutputStream.SEARCH) {
      return new SearchOutput(this, type.toString(), info.toString());
    }
    return new PrintOutput(this, type.toString(), info.toString());
  }

  public void insertChar(int c) {
    switch (c) {
      case '\t':
	insertTab();
	break;
      case '}':
	if(JBOT.has41()) {
	  ops.xact(EditorActions.ACTION_ClosingCurlyBrace, "}");
	  break;
	}
	// FALL THROUGH
      default:
	insertTypedChar((char)c);
	break;
    }
  }

  public void undo() {
    EditorPane ep = (EditorPane)editorPane;
    if(!ep.canUndo()) {
      Util.vim_beep();
      return;
    }
    if(G.isClassicUndo.getBoolean()) {
      // keep invoking undo until something in the document actually changes.
      cache.isUndoChange(); // clear the change flag
      while(ep.canUndo()) {
	ep.undo();
	if(cache.isUndoChange()) {
	  break;
	}
      }
    } else {
      ep.undo();
    }
  }

  public void redo() {
    EditorPane ep = (EditorPane)editorPane;
    if(!ep.canRedo()) {
      Util.vim_beep();
      return;
    }
    if(G.isClassicUndo.getBoolean()) {
      // keep invoking redo until something in the document actually changes.
      cache.isUndoChange(); // clear the change flag
      while(ep.canRedo()) {
	ep.redo();
	if(cache.isUndoChange()) {
	  break;
	}
      }
    } else {
      ep.redo();
    }
  }

  public void findMatch() {
    ops.xact(EditorActions.ACTION_MatchBrace);
  }

  /** use this for consistency checking */
  private static boolean inUndo;

  public void beginUndo() {
    if(inUndo) {
      ViManager.dumpStack();
    }
    inUndo = true;
    ((EditorPane)editorPane).startUndoGroup();
  }

  public void endUndo() {
    if( ! inUndo) {
      ViManager.dumpStack();
      return;
    }
    inUndo = false;
    ((EditorPane)editorPane).endUndoGroup();
  }

  public boolean isInUndo() {
    return inUndo;
  }

  /**
   * Quit tab, or close if split. Doing the optional close is a hack,
   * hope to get support from JBuilder to clean this up.
   */
  public void win_quit() {
    boolean is_split = false;
    Component c = getEditorComponent();
    while((c = c.getParent()) != null) {
      if(c instanceof TextViewerComponent) {
        break;
      }
      if(c instanceof Splitter) {
        is_split = true;
        break;
      }
    }
    if(is_split) {
      win_close(false);
    } else {
      ops.xact(com.borland.primetime.ide.BrowserFile.ACTION_NodeClose);
      focusCurrentNode();
    }
  }

  /** Split this window.
   * @param n the size of the new window.
   */
  public void win_split(int n) {
    ops.xact(com.borland.primetime.viewer.TextView.ACTION_SplitVertical);
  }

  /** Close this window
   * @param freeBuf true if the related buffer may be freed
   */
  public void win_close(boolean freeBuf) {
    ops.xact(com.borland.primetime.viewer.TextView.ACTION_CloseView);
    focusCurrentNode();
  }

  /** Close other windows
   * @param forceit true if always hide all other windows
   */
  public void win_close_others(boolean forceit) {
    ops.xact(com.borland.primetime.viewer.TextView.ACTION_CloseOtherViews);
  }
  
  /**
   * Descend into the container hierarchy and stash
   * any discovered instances of the specified class
   * into the list.
   */
  void collectComponents(Object o, Class clazz, List l) {
    if(o != null && clazz.isAssignableFrom(o.getClass())) {
      l.add(o);
    }
    if(o == null || ! (o instanceof Container)) {
      return;
    }
    Container c = (Container) o;
    for(int i = c.getComponentCount()-1; i >= 0; i--) {
      collectComponents(c.getComponent(i), clazz, l);
    }
  }

  /**
   * Bring focus to the active node in the content pane.
   */
  void focusCurrentNode() {
    focusCurrentNode(false);
    //win_cycle(1);
    /*
    Browser b = Browser.getActiveBrowser();
    Node n = b.getActiveNode();
    NodeViewer v = b.getActiveViewer(n);
    v.getViewerComponent().requestFocus();
    */
  }
  
  void focusCurrentNode(boolean later) {
    if(later) {
      SwingUtilities.invokeLater(
        new Runnable() { public void run() {
	  //System.err.println("foc1");
	  focusCurrentNode(false); }});
      return;
    }
    SwingUtilities.invokeLater(
      new Runnable() { public void run() {
	//System.err.println("foc2");
        focusCurrentNodeFinally(); }});
  }
  
  void focusCurrentNodeFinally() {
    List l = new ArrayList();
    Browser b = Browser.getActiveBrowser();
    Node n = b.getActiveNode();
    NodeViewer v = b.getActiveViewer(n);
    if(v == null) {
      return;
    }
    collectComponents(v.getViewerComponent(), EditorPane.class, l);
    if(l.size() == 0) {
      return;
    }
    // take the first editor pane and give it focus
    JEditorPane c = (JEditorPane)l.get(0);
    c.requestFocus();
    /*
    try {
      Browser b = Browser.getActiveBrowser();
      Node n = b.getActiveNode();
      System.err.println("foc2 " + n.getDisplayName());
      b.setActiveNode(n, true);
    }
    catch (Exception ex) {
      // Oh well, guess focus doesn't change.
      System.err.println("Sigh!");
    }
    */
  }

  /** Goto the indicated buffer.
   * @param n the index of the window to make current
   */
  public void win_goto(int n) {
    Msg.emsg("win_goto: not implemented");
    // Normal.notImp("win_goto");
  }

  /** Cycle to the indicated buffer.
   * @param n the positive/negative number of windows to cycle.
   */
  public void win_cycle(int n) {
    ops.xact(com.borland.primetime.viewer.TextView.ACTION_NextView);
  }

  public void displayFileInfo() {
    StringBuffer sb = new StringBuffer();
    sb.append("\"" + getDisplayFileName() + "\"");
    int l = getLineCount();
    sb.append(" " + l + " line" + Misc.plural(l));
    sb.append(" --" + (int)((cache.getCursor().getLine() * 100)
			      / getLineCount()) + "%--");
    getStatusDisplay().displayStatusMessage(sb.toString());
  }

  public String getDisplayFileName() {
    FileNode node = NodeViewMap.getNode((EditorPane)editorPane);
    return node.getDisplayName();
  }
}

/**
 * This class tracks the readonly state of the currently active JEditorPane.
 * While jVi is in command mode, the isEditable flag must be true so that it
 * can recieve KEY_TYPED events.
 */
class ReadOnlyHack
{
  static EditorPane currentEditor; // current editor pane
  /** If fileNode.isReadOnly or fileNode null then it is read only */
  static FileNode fileNode; // cache file node for currentEditor
  static boolean flagReadOnly; // current editor is readonly
  static boolean doReadOnlyHack; // use results of this mess
  
  static void switchTo(JEditorPane editorPane) {
    changeReadOnlyHackOption(); // in case the first time
    // restore isEditable from previously active editor
    ReadOnlyHack.restoreIsEditable();
    // get current editor and its node information
    currentEditor = (EditorPane)editorPane;
    Node node = NodeViewMap.getNode(currentEditor);
    if(node instanceof FileNode) {
      fileNode = (FileNode)node;
    }
    currentEditor.setEditable(true); // let all the characters through
    // set isEditable to activating editor
    ReadOnlyHack.checkReadOnly();
  }
  
  /** Save fileNodes readonly state.
   *  Expect this to be called on each keystroke.
   */
  static void checkReadOnly() {
    if(fileNode != null) {
      flagReadOnly = fileNode.isReadOnly();
      currentEditor.setEditable(true);
    }
  }
  
  static boolean isEditable() {
    if(doReadOnlyHack) {
      return ! flagReadOnly;
    }
    if(currentEditor != null) {
      return currentEditor.isEditable();
    }
    return true; // hope this doesn't happen much
  }
  
  static void restoreIsEditable() {
    if(fileNode != null) {
      currentEditor.setEditable(!fileNode.isReadOnly());
    }
    currentEditor = null;
    fileNode = null;
    flagReadOnly = false;
  }
  
  /**
   * This method is used to signal that the active editor is entering/exiting
   * modify mode.
   * 
   * @param modify true if entering modify mode
   */
  static void setModify(boolean modify) {
    /*
    if(currentEditor != null) {
      // if entering modify, 
      currentEditor.setEditable(modify ? false : true);
    }
    */
  }
  
  static void changeReadOnlyHackOption() {
    doReadOnlyHack = G.readOnlyHack.getBoolean();
  }
}
