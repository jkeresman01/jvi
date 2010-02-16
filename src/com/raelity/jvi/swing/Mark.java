package com.raelity.jvi.swing;

import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.core.Buffer;
import com.raelity.text.TextUtil.MySegment;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

/**
 * A Mark in vi specifies a row and column. The row "floats" as lines are
 * inserted and deleted earlier in the file.
 * <b>However</b> the column is set when
 * the mark is created and does not change even if characters are added on the
 * same line before the column.
 *
 * This is a LEFT-BIAS mark. Swing is right bias, so there's a little didling.
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
                this.pos = buf.INVALID_MARK_LINE;
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
                --offset;
            }
            pos = buf.getDocument().createPosition(offset);
        } catch (BadLocationException ex) {
            pos = null;
            return;
        }
    }

    private int getDocOffset()
    {
        return atZero ? 0 : pos.getOffset() + 1;
    }

    public int getLine()
    {
        checkMarkUsable();
        if (this.pos == buf.INVALID_MARK_LINE) {
            return buf.getLineCount() + 1;
        }
        return buf.getLineNumber(getDocOffset());
    }

    public int getColumn()
    {
        checkMarkUsable();
        if (this.pos == buf.INVALID_MARK_LINE) {
            return 0;
        }
        MySegment seg = buf.getLineSegment(getLine());
        int len = seg.length() - 1;
        return seg.length() <= 0 ? 0 : Math.min(col, len);
    }

    public int getOffset()
    {
        checkMarkUsable();
        if (this.pos == buf.INVALID_MARK_LINE) {
            return Integer.MAX_VALUE;
        }
        return buf.getLineStartOffsetFromOffset(getDocOffset()) + getColumn();
    }

    public void invalidate()
    {
        pos = null;
    }

    public Buffer getBuffer()
    {
        return buf;
    }

    public void verify(Buffer buf)
    {
        if (buf != getBuffer()) {
            throw new IllegalStateException("fpos buffer mis-match");
        }
    }

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

    public final boolean isValid()
    {
        // same as checkMarkUsable
        return pos != null && buf.getDocument() != null;
    }

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
    public void set(int line, int col)
    {
        throw new UnsupportedOperationException();
    }

    public void set(ViFPOS fpos)
    {
        throw new UnsupportedOperationException();
    }

    public void set(int offset)
    {
        throw new UnsupportedOperationException();
    }

    /** This is optional, may throw an UnsupportedOperationException */
    public void setColumn(int col)
    {
        throw new UnsupportedOperationException();
    }

    /** This is optional, may throw an UnsupportedOperationException */
    public void setLine(int line)
    {
        throw new UnsupportedOperationException();
    }

    public void decColumn()
    {
        throw new UnsupportedOperationException();
    }

    public void incColumn()
    {
        throw new UnsupportedOperationException();
    }

    public void decLine()
    {
        throw new UnsupportedOperationException();
    }

    public void incLine()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        return "offset: " + getOffset() + " lnum: " + getLine() + " col: " +
                getColumn();
    }
}
