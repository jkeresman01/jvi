
/**
 * Title:        VI for JBuilder<p>
 * Description:  A VI emulation mode for JBuilder3.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */
package com.raelity.tools.fixup13;

import java.util.Set;
import java.util.HashSet;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;

public class FixupInputMap {

  private FixupInputMap() {
  }

  // NEEDSWORK: memory leak!!!!!!!!!!!!!!!!
  static private Set panes = new HashSet();
  static private boolean firstTry = true;
  static private boolean hasInputMap = false;

  static public void fixupInputMap(JEditorPane editorPane) {
    if(firstTry) {
      firstTry = false;
      try {
        Class.forName("javax.swing.InputMap");
        hasInputMap = true;
      } catch(ClassNotFoundException ex) {}
    }
    if( ! hasInputMap) {
      return;
    }
    if(panes.contains(editorPane)) {
      return;
    }
    panes.add(editorPane);
    removeKey(editorPane.getInputMap(), KeyStroke.getKeyStroke((char)8));
  }

  static private void removeKey(javax.swing.InputMap map, KeyStroke key) {
    map.remove(key);
    javax.swing.InputMap parent = map.getParent();
    if(parent != null) {
      removeKey(parent, key);
    }
  }
}