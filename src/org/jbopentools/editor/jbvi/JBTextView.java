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

import java.awt.Component;
import javax.swing.JEditorPane;
import javax.swing.text.Caret;

import com.borland.primetime.ide.Browser;
import com.borland.primetime.editor.EditorPane;
import com.borland.primetime.editor.EditorActionNames;
import com.borland.primetime.editor.SearchManager;
import com.borland.primetime.editor.SearchOptions;
import com.borland.primetime.viewer.NodeViewMap;
import com.borland.primetime.viewer.TextViewerComponent;
import com.borland.primetime.ui.Splitter;
import com.borland.primetime.node.FileNode;

import com.raelity.jvi.Window;
import com.raelity.jvi.Msg;
import com.raelity.jvi.Constants;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.Misc;
import com.raelity.jvi.swing.TextView;
import com.raelity.jvi.ViManager;

/**
 * Pretty much the TextView used for standard swing.
 */
public class JBTextView extends TextView
			implements Constants, EditorActionNames
{
  JBTextView(Browser browser, EditorPane editorPane) {
    cache = createTextViewCache();
    statusDisplay = new JBStatusDisplay(browser, editorPane);
  }
  
  protected void createOps(JEditorPane editorPane) {
    ops = new Ops(this);
    ops.init(editorPane);
  }

  /**
   * Use the JBuilder Node.
   * @return opaque FileObject backing this EditorPane
   */
  public Object getFileObject() {
    return NodeViewMap.getNode((EditorPane)editorPane);
  }

  public void undo() {
    ((EditorPane)editorPane).undo();
  }

  public void redo() {
    ((EditorPane)editorPane).redo();
  }

  public void findMatch() {
    ops.xact(matchBraceAction);
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
      ops.xact(com.borland.primetime.ide.Browser.ACTION_NodeClose);
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
  }

  /** Close other windows
   * @param forceit true if always hide all other windows
   */
  public void win_close_others(boolean forceit) {
    ops.xact(com.borland.primetime.viewer.TextView.ACTION_CloseOtherViews);
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

  String getDisplayFileName() {
    FileNode node = NodeViewMap.getNode((EditorPane)editorPane);
    return node.getDisplayName();
  }
}
