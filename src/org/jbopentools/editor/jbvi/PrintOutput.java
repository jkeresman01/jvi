
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

import javax.swing.text.Segment;
import javax.swing.text.Position;
import javax.swing.text.BadLocationException;

import com.borland.primetime.editor.SearchManager;
import com.borland.primetime.editor.SearchManager.FindAllMessage;
import com.borland.primetime.editor.EditorPane;
import com.borland.primetime.ide.Browser;
import com.borland.primetime.ide.MessageCategory;
import com.borland.primetime.ide.MessageView;
import com.borland.primetime.ide.Message;

import com.raelity.jvi.OutputStreamAdaptor;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.G;

/**
 * This takes jVi print output and puts them into a
 * Print message tab.
 * The first output message is queued until either another
 * message comes in, or a close occurs.
 * When a close occurs, if only one message has been received,
 * a single line is added to the messages (not nested).
 * </p>
 */
public class PrintOutput extends OutputStreamAdaptor {
  //final static MessageCategory CATEGORY_PRINT = new MessageCategory("Print");
  final static MessageCategory CATEGORY_PRINT = SearchManager.CATEGORY_SEARCH;
  /** true when messages are flowing through, nothing is queued up */
  boolean active;
  MessageView mv;
  /* top node of group of messages */
  Message commandPrintMessage;
  /* queued message */
  Message printMessage;
  String label;
  JBTextView tv;

  /**
   * create a node in the SearchResults tab for the search.
   */
  public PrintOutput(ViTextView tv, String file, String label) {
    this.label = label;
    this.tv = (JBTextView)tv;
    mv = Browser.getActiveBrowser().getMessageView();
    commandPrintMessage = new Message("Print results for '"
			               + label + "' in "
				       + this.tv.getDisplayFileName());
  }
  
  /**
   * A message just came in, do the lazy dance.
   */
  private void addMessage(Message msg) {
    if(!active) {
      if(printMessage == null) {
	// first message, queue it up
	printMessage = msg;
	return;
      } else {
	// something has come in and there is something queued, there are
	// at least two messages so set up the node for multiple children msg
	commandPrintMessage = mv.addMessage(CATEGORY_PRINT,
	                                    commandPrintMessage);
	mv.addMessage(CATEGORY_PRINT, commandPrintMessage, printMessage);
	active = true; // no more queueing
      }
    }
    mv.addMessage(CATEGORY_PRINT, commandPrintMessage, msg);
  }

  /**
   * Add a message that references a line in the current file. If
   * length is zero, then the whole line is selected.
   */
  public void println(int line, int col, int length) {
    Position pos;
    Segment seg = tv.getLineSegment(line); // line that matched
    int offset = tv.getLineStartOffset(line);
    if(length == 0) {
      col = 0;
      length = seg.count - 1;
    }
    try {
      pos = tv.getEditorComponent().getDocument()
		      .createPosition(offset + col);
    }
    catch (BadLocationException ex) {
      //NEEDSWORK:
      return;
    }
    SearchManager.FindAllMessage msg =
		new SearchManager.FindAllMessage(
		    (EditorPane)tv.getEditorComponent(),
		    pos,
		    line,
		    col,
		    length,
		    label,
		    new String(seg.array, seg.offset, seg.count - 1)
		  );
    addMessage(msg);

	    // EditorPane editor,
	    // javax.swing.text.Position pos,
	    // int line,
	    // int column,
	    // int length,
	    // java.lang.String searchText,
	    // java.lang.String matchLine 
  }

  public void println(String s) {
    addMessage(new Message(s));
  }

  /**
   * flush any pending output.
   */
  public void close() {
    if( ! active) {
      if(printMessage != null) {
	mv.addMessage(CATEGORY_PRINT, printMessage);
      }
    }
    mv = null;
  }
}