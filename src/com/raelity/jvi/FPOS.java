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

import java.lang.ref.WeakReference;

/**
 * Buffer position, accessable as line number, 1 based, and column, 0 based.
 * An fpos has a very short valid lifetime, it should not be saved.
 * getLine() and getCol() do not change as the document changes.
 * The FPOS does have
 * a weak reference to a Buffer, so methods such as setLine() do the right
 * thing.
 * See ViMark for that type of behavior that tracks document changes.
 * <p>
 * Developer notes
 * <ul>
 * <li>Any changes or additions here must be considered for WCursor.</li>
 * <li>Methods should not reference offset,lnum,col directly. Must use the
 * accessor to insure that the data is valid.</li>
 * </ul>
 * </p>
 */

class FPOS extends ViFPOS.abstractFPOS
{
  private MutableInt offset = new MutableInt();
  private MutableInt lnum = new MutableInt();
  private MutableInt col = new MutableInt();
  WeakReference<Buffer> rBuf;

  /**
   *
   */
  FPOS() {
    this(G.curbuf);
  }

  FPOS(Buffer buf)
  {
    rBuf = new WeakReference<Buffer>(buf);
  }

  /** Used to make a copy. */
  private void initFPOS(int o, int l, int c) {
    offset.setValue(o);
    lnum.setValue(l);
    col.setValue(c);
  }

  void initFPOS(int offset) {
    Buffer buf = rBuf.get();
    int l = buf.getLineNumber(offset);
    initFPOS(offset, l, offset - buf.getLineStartOffset(l));
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
    verify(G.curbuf);
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
    FPOS fpos = new FPOS(rBuf.get());
    fpos.initFPOS(getOffset(), getLine(), getColumn());
    return fpos;
  }

  public void verify(Buffer buf)
  {
      if(rBuf.get() != buf)
          throw new IllegalStateException("fpos buffer mis-match");
  }
}

// vi:set sw=2 ts=8:
