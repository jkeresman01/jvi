
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
import com.borland.primetime.editor.SearchManager$FindAllMessage;
import com.borland.primetime.editor.EditorPane;
import com.borland.primetime.ide.Browser;
import com.borland.primetime.ide.MessageView;
import com.borland.primetime.ide.Message;

import com.raelity.jvi.OutputStreamAdaptor;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.G;

/**
 * This takes jVi serach results and stuffs them into the 
 * SearchResults message tab.
 * <p>
 * In the future, want to set this up to accomodate a search in
 * path type function.
 * </p>
 */
public class SearchOutput extends OutputStreamAdaptor {
  MessageView mv;
  Message searchFileMessage;
  String pattern;
  JBTextView tv;

  /**
   * create a node in the SearchResults tab for the search.
   */
  public SearchOutput(ViTextView tv, String file, String pattern) {
    this.pattern = pattern;
    this.tv = (JBTextView)tv;
    mv = Browser.getActiveBrowser().getMessageView();
    searchFileMessage = mv.addMessage(SearchManager.CATEGORY_SEARCH,
                           new Message("Search results for '"
			               + pattern + "' in "
				       + this.tv.getDisplayFileName()));
  }

  public void println(int line, int col, int length) {
    Position pos;
    Segment seg = tv.getLineSegment(line); // line that matched
    try {
      pos = tv.getEditorComponent().getDocument()
		      .createPosition(seg.offset + col);
    }
    catch (BadLocationException ex) {
      //NEEDSWORK:
      return;
    }

    SearchManager$FindAllMessage msg = new SearchManager$FindAllMessage(
		    (EditorPane)tv.getEditorComponent(),
		    pos,
		    line,
		    col,
		    length,
		    pattern,
		    new String(seg.array, seg.offset, seg.count - 1)
		  );
    mv.addMessage(SearchManager.CATEGORY_SEARCH, searchFileMessage, msg);

	    // EditorPane editor,
	    // javax.swing.text.Position pos,
	    // int line,
	    // int column,
	    // int length,
	    // java.lang.String searchText,
	    // java.lang.String matchLine 
  }

  public void println(String s) {
    /**@todo: Implement this com.raelity.jvi.ViOutputStream method*/
    throw new java.lang.UnsupportedOperationException("Method println() not yet implemented.");
  }

  public void close() {
  }
}