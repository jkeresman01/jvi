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

import java.io.File;
import java.awt.event.ActionEvent;
import javax.swing.text.Document;

import com.borland.primetime.ide.Browser;
import com.borland.primetime.node.FileNode;
import com.borland.primetime.node.TextFileNode;
import com.borland.primetime.editor.EditorPane;
import com.borland.primetime.viewer.NodeViewMap;

import com.raelity.jvi.ViFS;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.Normal;
import com.raelity.jvi.Msg;
import com.raelity.jvi.ViManager;

public class JBFS implements ViFS {

  public JBFS() {
  }

  public void write(ViTextView tv, boolean force) {
    try {
      ((FileNode)tv.getFileObject()).save();
    } catch(Exception ex) {
      Msg.emsg(ex.getMessage());
    }
  }

  public void writeAll(boolean force) {
    Browser.ACTION_NodeSaveAll.actionPerformed(Browser.getActiveBrowser());
  }

  public void write(ViTextView tv, File file, boolean force) {
    if(file == null) {
      Browser.ACTION_NodeSaveAs.actionPerformed(Browser.getActiveBrowser());
      return;
    }
    Msg.emsg("write(tv, file) not implemented");
  }

  public void edit(ViTextView tv, int n, boolean force) {

    FileNode node = (FileNode)ViManager.getTextBuffer(n);
    if(node == null) {
      StringBuffer s = new StringBuffer(
                        "No alternate file name to substitute for '#");
      s.append(n);
      s.append("'");
      Msg.emsg(s.toString());
      return;
    }
    try {
      NodeViewMap.getBrowser((EditorPane)tv.getEditorComponent())
                                  .setActiveNode(node, true);
    } catch(Exception ex) {
      Msg.emsg(ex.getMessage());
    }
  }
}
