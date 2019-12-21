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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.swing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.manager.ViManager;
import com.raelity.text.TextUtil.MySegment;

/**
 * A Mark in vi specifies a row and column. The row "floats" as lines are
 * inserted and deleted earlier in the file.
 * <b>However</b> the column is set when
 * the mark is created and does not change even if characters are added on the
 * same line before the column.
 *
 * This is a LEFT-BIAS mark. Swing is right bias, so there's a little didling.
 * The primary implication is that the with a mark insertions at the mark
 * location occur after the mark, whereas in swing they are after
 * and move the position.
 */
class Mark implements ViMark
{

    // The pos tracks the line number,
    // note that it's offset is one less that the actual offset
    // since there is no way to specify a left-bias for position
    private Position pos;
    private int col;
    private boolean atZero;
    SwingBuffer buf;

    Mark(SwingBuffer outer)
    {
        this.buf = outer;
    }

    /**
     * If the mark offset is not valid then this mark is converted into
     * a null mark.
     * // NEEDSWORK: deprecate setMark, just call it set ?????
     */
    @Override
    public void setMark(ViFPOS fpos)
    {
        fpos.verify(buf);
        if (fpos instanceof ViMark) {
            Mark mark = (Mark)fpos;
            this.pos = mark.pos;
            this.col = mark.col;
            this.atZero = mark.atZero;
        } else {
            // adapted from FPOS.set
            if (fpos.getLine() > buf.getLineCount()) {
                this.pos = SwingBuffer.INVALID_MARK_LINE;
            } else {
                int column = fpos.getColumn();
                int startOffset = buf.getLineStartOffset(fpos.getLine());
                int endOffset = buf.getLineEndOffset(fpos.getLine());
                // NEEDSWORK: if the column is past the end of the line,
                //            should it be preserved?
                if (column >= endOffset - startOffset) {
                    ViManager.dumpStack("column " + column + ", limit " +
                            (endOffset - startOffset - 1));
                    column = endOffset - startOffset - 1;
                }
                setDocOffset(startOffset + column);
            }
        }
    }

    @Override
    public ViFPOS copyTo(ViFPOS target)
    {
        target.set(this);
        return target;
    }

    /**
     * If the mark offset is not valid then this mark is converted into
     * a null mark.
     */
    private void setDocOffset(int offset)
    {
        try {
            col = buf.getColumnNumber(offset);
            if (offset == 0) {
                atZero = true;
            } else {
                atZero = false;
                // adjust offset for LEFT-BIAS, but don't change the line
                if(getChar(offset - 1) != '\n')
                    --offset;
            }
            pos = buf.getDocument().createPosition(offset);
        } catch (BadLocationException ex) {
            pos = null;
        }
    }

    private int getDocOffset()
    {
        int offset = 0;
        if(!atZero) {
            // We're doing LEFT-BIAS so add one to the offset
            // (notice that we subtracted one when we set it)
            // UNLESS pointing at a newline; do not change lines.
            offset = pos.getOffset();
            if(getChar(offset) != '\n')
                offset += 1;
        }
        return offset;
    }

    private char getChar(int offset)
    {
        // It is "impossible" to get the exception since we're based on pos
        // but default to '\n'; that will avoid adjusting the offset
        char c = '\n';
        try {
            c = buf.getDocument().getText(offset, 1).charAt(0);
        } catch(BadLocationException ex) {
            Logger.getLogger(Mark.class.getName()).log(Level.SEVERE, null, ex);
        }
        return c;
    }

    @Override
    public int getLine()
    {
        checkMarkUsable();
        if (this.pos == SwingBuffer.INVALID_MARK_LINE) {
            return buf.getLineCount() + 1;
        }
        return buf.getLineNumber(getDocOffset());
    }

    @Override
    public int getColumn()
    {
        checkMarkUsable();
        if (this.pos == SwingBuffer.INVALID_MARK_LINE) {
            return 0;
        }
        MySegment seg = buf.getLineSegment(getLine());
        int len = seg.length() - 1;
        return seg.length() <= 0 ? 0 : Math.min(col, len);
    }

    @Override
    public int getOffset()
    {
        checkMarkUsable();
        if (this.pos == SwingBuffer.INVALID_MARK_LINE) {
            return Integer.MAX_VALUE;
        }
        return buf.getLineStartOffsetFromOffset(getDocOffset()) + getColumn();
    }

    @Override
    public int getOriginalColumnDelta()
    {
        return col - getColumn();
    }

    @Override
    public void invalidate()
    {
        pos = null;
    }

    @Override
    public Buffer getBuffer()
    {
        return buf;
    }

    @Override
    public void verify(Buffer buf)
    {
        if (buf != getBuffer()) {
            throw new IllegalStateException("fpos buffer mis-match");
        }
    }

    @Override
    public ViFPOS copy()
    {
        Mark m = new Mark(buf);
        m.setMark(this);
        return m;
    }

    final void checkMarkUsable()
    {
        // same as isValid
        if (pos == null) {
            throw new MarkException("Uninitialized Mark");
        }
        if (buf.getDocument() == null) {
            throw new MarkOrphanException("Mark Document null");
        }
    }

    @Override
    public final boolean isValid()
    {
        // same as checkMarkUsable
        return pos != null && buf.getDocument() != null;
    }

    @Override
    public final int compareTo(ViFPOS p)
    {
        if (this.getOffset() < p.getOffset()) {
            return -1;
        } else if (this.getOffset() > p.getOffset()) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof ViFPOS) {
            ViFPOS fpos = (ViFPOS)o;
            return getOffset() == fpos.getOffset();
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 23 * hash + (this.pos != null ? this.pos.hashCode() : 0);
        hash = 23 * hash + this.col;
        return hash;
    }

    /** This is optional, may throw an UnsupportedOperationException */
    @Override
    public void set(int line, int col)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(ViFPOS fpos)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int offset)
    {
        throw new UnsupportedOperationException();
    }

    /** This is optional, may throw an UnsupportedOperationException */
    @Override
    public void setColumn(int col)
    {
        throw new UnsupportedOperationException();
    }

    /** This is optional, may throw an UnsupportedOperationException */
    @Override
    public void setLine(int line)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void decColumn()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void incColumn()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void decLine()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void incLine()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        int reportCol = getColumn();
        String realCol = reportCol < col ? "(" + col + ")" : "";
        return "offset: " + getOffset() + " lnum: " + getLine() + " col: " +
                reportCol + realCol;
    }
}
