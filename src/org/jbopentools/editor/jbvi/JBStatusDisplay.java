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

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.borland.primetime.ide.Browser;
import com.borland.primetime.ide.StatusView;
import com.borland.primetime.editor.EditorPane;
import com.borland.primetime.editor.EditorManager;

import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.ViCursor;
import com.raelity.jvi.Edit;
import com.raelity.jvi.G;

/**
 * Display vi status in jbuilder. The mode is put into the
 * general status area.
 * <p>
 * In the future when a message is posted, may want to record
 * the cursor and line number and then use this in conjunction
 * with the scroll request and only clear the message if there
 * has been scrolling.... Could also listen to start/end of
 * full commands to help in some algorithms to keep messages up correctly.
 * </p>
 */
public class JBStatusDisplay implements ViStatusDisplay {
  static String lastMode = null;
  static String lastCmd = "";
  static String lastMsg = "";
  static Color lastMsgColor = null;
  String mode = "";
  StatusView statusView;
  // EditorPane editorPane;

  public JBStatusDisplay(Browser browser, EditorPane editorPane) {
    statusView = browser.getStatusView();
    // this.editorPane = editorPane;
  }

  String modeString() {
    return mode + (G.Recording  ? "recording" : "");
  }

  public void displayMode(String mode) {
    JBTextView.setModify(mode == Edit.VI_MODE_COMMAND ? false : true);
    if( ! mode.equals(lastMode)) {
      lastMode = mode;
      JBViFactory.editorModeChange(mode);
    }
    if( ! mode.equals("")) {
      this.mode = "-- " + mode + " -- ";
    } else {
      this.mode = "";
    }
    lastMsg = "";       // clear lastMsg when mode is set
    // dit(this.mode, null);
    dit(modeString(), null);
    // statusView.setText(msg);
  }

  public void displayCommand(String cmd) {
    if(cmd.equals(lastCmd)) {
      return;
    }
    lastCmd = cmd;
    Color color = null;
    if(lastCmd.equals("")) {
      color = lastMsgColor;
    } else {
      cmd = " [ " + cmd + " ]";
    }
    dit(modeString() + lastMsg + " " + cmd, color);
    // setText(strokeStatus, cmd);
  }

  public void displayStatusMessage(String msg) {
    lastMsg = msg;
    lastMsgColor = null;
    dit(modeString() + msg, null);
    // statusView.setText(msg);
  }

  public void displayErrorMessage(String msg) {
    lastMsg = msg;
    lastMsgColor = Color.red;
    dit(modeString() + msg, Color.red);
    // statusView.setText(msg, Color.red);
  }
  
  public void clearMessage() {
    displayStatusMessage("");
  }

  // defer execution of message display in JBuidler and hope that the
  // screen scrolling settles before it gets displayed
  void dit(String msg, Color c) {
    SwingUtilities.invokeLater(new ditQ(msg, c));
  }
  
  class ditQ implements Runnable {
    String msg;
    Color c;
    boolean doit;
    ditQ(String msg, Color c) {
      this.msg = msg;
      this.c = c;
    }
    
    public void run() {
      if(doit) {
	ditFinally(msg, c);
      } else {
	doit = true;
	SwingUtilities.invokeLater(this);
      }
    }
  }
  
  void ditFinally(String msg, Color c) {
    if(c == null) {
      statusView.setText(" " + msg);
    } else {
      statusView.setText(" " + msg, c);
    }
  }
  /*
  void ditFinally(final String msg, final Color c) {
    if(SwingUtilities.isEventDispatchThread()) {
      if(c == null) {
        statusView.setText(" " + msg);
      } else {
        statusView.setText(" " + msg, c);
      }
      // EditorManager.showStatusMessage((EditorPane)editorPane, msg,
      //                                  false, false);
    } else {
      try {
	SwingUtilities.invokeAndWait(
	  new Runnable() {
	    public void run() {
              if(c == null) {
                statusView.setText(msg);
              } else {
                statusView.setText(msg, c);
              }
              // EditorManager.showStatusMessage((EditorPane)editorPane, msg,
              //                         false, false);
	    }});
      } catch(Exception e) {}
    }
  }
  */
}
