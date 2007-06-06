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

/**
 * File position, accessable as line number, 1 based, and column, 0 based.
 * <p>
 * Any changes or additions here must be considered for WCursor.
 * </p>
 * NOTE: line and column here do not change as the document changes. See Mark
 * for that type of behavior. <br/>
 * NEEDSWORK: This should have a weak reference to Buffer.
 */

public class FPOS implements ViFPOS, Comparable, Cloneable
{
  private MutableInt offset = new MutableInt();
  private MutableInt lnum = new MutableInt();
  private MutableInt col = new MutableInt();

  /** Used to make a copy. */
  private void initFPOS(int o, int l, int c) {
    offset.setValue(o);
    lnum.setValue(l);
    col.setValue(c);
  }

  /** use the current caret location to set the offset,line,col */
  protected void setCursor(ViTextView tv) {
    tv.computeCursorPosition(offset, lnum, col);
  }

  public int getLine() {
    return lnum.getValue();
  }

  public int getColumn() {
    return col.getValue();
  }

  public int getOffset() {
    return offset.getValue();
  }

  public void set(int line, int column) {
    int startOffset = G.curwin.getLineStartOffset(line);
    int endOffset = G.curwin.getLineEndOffset(line);
    if(column < 0) {
      ViManager.dumpStack("line " + line + ", column " + column
              + ", length " + (endOffset - startOffset));
      column = 0;
    } else if(column >= endOffset - startOffset) {
      ViManager.dumpStack("line " + line + ", column " + column
              + ", length " + (endOffset - startOffset));
      column = endOffset - startOffset - 1;
    }
    
    offset.setValue(startOffset + column);
    lnum.setValue(line);
    col.setValue(column);
  }

  public void set(ViFPOS fpos) {
    set(fpos.getLine(), fpos.getColumn());
  }

  public void setColumn(int column) {
    set(lnum.getValue(), column);
  }

  public void setLine(int line) {
    set(line, col.getValue());
  }

  final public boolean equals(Object o) {
    // NEEDSWORK: equals FPOS, should doc be checked as same?
    if(o instanceof ViFPOS) {
      ViFPOS fpos = (ViFPOS)o;
      return this.getOffset() == fpos.getOffset();
    }
    return false;
  }

  final public int compareTo(Object o) {
    ViFPOS p = (ViFPOS)o;
    if(this.getOffset() < p.getOffset()) {
      return -1;
    } else if(this.getOffset() > p.getOffset()) {
      return 1;
    } else {
      return 0;
    }
  }

  final public ViFPOS copy() {
    FPOS fpos = new FPOS();
    fpos.initFPOS(offset.getValue(), lnum.getValue(), col.getValue());
    return fpos;
  }

  public String toString() {
    return	  "offset: " + offset.getValue()
	      	+ " lnum: " + lnum.getValue()
	      	+ " col: " + col.getValue()
		;
  }
}

// vi:set sw=2 ts=8:
