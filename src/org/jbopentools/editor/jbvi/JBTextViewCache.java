
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

import javax.swing.SwingUtilities;
import com.raelity.jvi.swing.TextViewCache;

public class JBTextViewCache extends TextViewCache {
  
  JBTextViewCache(JBTextView tv) {
    super(tv);
  }
  
  /**
   * JBuilder does layout on the EditorPane JViewport twice for every
   * keystroke and other events. They are different sizes to boot.
   * This is all insane. Doing these calculations "later" prevents
   * visuall defects (Msg.clearMsg invocations).
   */
  protected void fillLinePositions() {
    SwingUtilities.invokeLater(
      new Runnable() { public void run() { fillLinePositionsFinally(); }});
  }
}