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
 * </p><p>
 * NOTE: Methods should not reference offset,lnum,col directly. Must use the
 * accessor to insure that the data is valid.
 * </p>
 * NOTE: line and column here do not change as the document changes. See Mark
 * for that type of behavior. <br/>
 * NEEDSWORK: This should have a weak reference to Buffer.
 */

public class FPOS extends ViFPOS.abstractFPOS
{
  private MutableInt offset = new MutableInt();
  private MutableInt lnum = new MutableInt();
  private MutableInt col = new MutableInt();

  public FPOS() {
  }

  /** the values are slammed, no validity checking. */
  public FPOS(int offset, int line, int column) {
    initFPOS(offset, line, column);
  }

  /** Used to make a copy. */
  protected void initFPOS(int o, int l, int c) {
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
    int startOffset = G.curbuf.getLineStartOffset(line);
    int endOffset = G.curbuf.getLineEndOffset(line);
    int adjustedColumn = -1;

    if(column < 0) {
      adjustedColumn = 0;
    } else if(column >= endOffset - startOffset) {
      adjustedColumn = endOffset - startOffset - 1;
    }

    if(adjustedColumn >= 0) {
      ViManager.dumpStack("line " + line + ", column " + column
              + ", length " + (endOffset - startOffset));
      column = adjustedColumn;
    }
    
    initFPOS(startOffset + column, line, column);
  }

  final public ViFPOS copy() {
    FPOS fpos = new FPOS();
    fpos.initFPOS(getOffset(), getLine(), getColumn());
    return fpos;
  }
}

// vi:set sw=2 ts=8:
