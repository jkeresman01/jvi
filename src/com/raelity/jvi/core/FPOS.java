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
 * Copyright (C) 2000-2008 Ernie Rael.  All Rights Reserved.
 * 
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.core;

import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViFPOS;
import java.lang.ref.WeakReference;

/**
 * Buffer position, accessable as line number, 1 based, and column, 0 based.
 * An fpos has a very short valid lifetime, it should not be saved.
 * getLine() and getCol() do not change as the document changes.
 * The FPOS does have
 * a weak reference to a Buffer, so methods such as setLine() do the right
 * thing and the column can not be set past the end of the line.
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
    private int offset;
    private int lnum = 1;
    private int col;
    WeakReference<Buffer> rBuf;

    /**
     * after construction, referencing line 1, column 0.
     */
    FPOS()
    {
        this(G.curbuf);
    }

    FPOS(int line, int col)
    {
        this(G.curbuf);
        set(line, col);
    }

    /**
     * after construction, referencing line 1, column 0.
     */
    FPOS(Buffer buf)
    {
        rBuf = new WeakReference<Buffer>(buf);
    }

    /** Used to make a copy. */
    private void initFPOS(int o, int l, int c)
    {
        offset = o;
        lnum = l;
        col = c;
    }

    @Override
    public void set(int offset)
    {
        Buffer buf = rBuf.get();
        set(buf, offset);
    }

    public void set(Buffer buf, int offset)
    {
        verify(buf);
        if(offset > buf.getLength())
            offset = buf.getLength();
        int l = buf.getLineNumber(offset);
        initFPOS(offset, l, offset - buf.getLineStartOffset(l));
    }

    public boolean isValid() {
        return true;
    }

    public int getLine()
    {
        return lnum;
    }

    public int getColumn()
    {
        return col;
    }

    public int getOffset()
    {
        return offset;
    }

    public void set(int line, int column)
    {
        set(G.curbuf, line, column);
    }

    public void set(Buffer buf, int line, int column)
    {
        verify(buf);
        int startOffset = buf.getLineStartOffset(line);
        int endOffset = buf.getLineEndOffset(line);
        int adjustedColumn = -1;

        if (column < 0) {
            adjustedColumn = 0;
        } else if (column >= endOffset - startOffset) {
            adjustedColumn = endOffset - startOffset - 1;
        }

        if (adjustedColumn >= 0) {
            if(column != Constants.MAXCOL)
                ViManager.dumpStack("line " + line + ", column " + column
                                    + ", length " + (endOffset - startOffset));
            column = adjustedColumn;
        }

        initFPOS(startOffset + column, line, column);
    }

    final public ViFPOS copy()
    {
        FPOS fpos = new FPOS(rBuf.get());
        fpos.initFPOS(getOffset(), getLine(), getColumn());
        return fpos;
    }

    public Buffer getBuffer() {
        return rBuf.get();
    }

    public void verify(Buffer buf)
    {
        if (rBuf.get() != buf) {
            throw new IllegalStateException("fpos buffer mis-match");
        }
    }
}

// vi:set sw=4 ts=8:
