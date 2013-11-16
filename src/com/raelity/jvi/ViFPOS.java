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

import com.raelity.jvi.core.Buffer;

/**
 * A position in a file/document.
 */
public interface ViFPOS extends Comparable<ViFPOS>
{
    public boolean isValid();

    public int getLine();

    public int getColumn();

    public int getOffset();

    /**
     * Set the position. This will set the postition on the new line.
     * If the column is less than zero, or past the new line, then it will
     * be restricted.
     * This is optional, may throw an UnsupportedOperationException
     */
    public void set(int line, int column);

    /**
     * Set the position. Slam the offset.
     * This is optional, may throw an UnsupportedOperationException
     */
    public void set(int offset);

    /**
     * This does set(fpos.getLine(), fpos.getColumn());
     * May be optimized.
     */
    public void set(ViFPOS fpos);

    /**
     * convenience for chaining.
     * <br/>NOTE: this returns target, not "this",
     * so the chaining object changes.
     * @param target set target with "this"
     * @return target
     */
    public ViFPOS copyTo(ViFPOS target);

    /**
     * Set the column, leave the line unchanged.
     * <br/>
     * This is optional, may throw an UnsupportedOperationException
     */
    public void setColumn(int col);


    public void incColumn();

    public void decColumn();

    public void incLine();

    public void decLine();

    /**
     * Set the line, leave the column unchanged.
     * <br/>
     * This is optional, may throw an UnsupportedOperationException
     */
    public void setLine(int line);

    /** Make a copy */
    public ViFPOS copy();

    /** throws a run time exception if the buf not associated with this fpos */
    public void verify(Buffer buf);

    /** the associated buffer */
    public Buffer getBuffer();

    public static abstract class abstractFPOS implements ViFPOS
    {
        @Override
        public void set(ViFPOS fpos)
        {
            set(fpos.getLine(), fpos.getColumn());
        }

        @Override
        public ViFPOS copyTo(ViFPOS target)
        {
            target.set(this);
            return target;
        }



        // Should not reference instance variables lnum or col directly,
        // must use accessor functions since subclasses, in particular
        // WCursor, must validate values
        @Override
        public void setColumn(int column)
        {
            set(getLine(), column);
        }

        @Override
        public void incColumn()
        {
            set(getLine(), getColumn()+1);
        }

        @Override
        public void decColumn()
        {
            set(getLine(), getColumn()-1);
        }

        @Override
        public void incLine()
        {
            set(getLine()+1, getColumn());
        }

        @Override
        public void decLine()
        {
            set(getLine()-1, getColumn());
        }

        // Should not reference instance variables lnum or col directly,
        // must use accessor functions since subclasses, in particular
        // WCursor, must validate values
        @Override
        public void setLine(int line)
        {
            set(line, getColumn());
        }

        // This is optional, may throw an UnsupportedOperationException
        @Override
        public void set(int offset)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        final public int compareTo(ViFPOS p)
        {
            if(!getBuffer().equals(p.getBuffer()))
                throw new IllegalArgumentException("different buffers");
            if (this.getOffset() < p.getOffset()) {
                return -1;
            } else if (this.getOffset() > p.getOffset()) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public String toString()
        {
            return "offset=" + getOffset()
                    + " lnum=" + getLine() + " col=" + getColumn();
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj == null) {
                return false;
            }
            if(!(obj instanceof ViFPOS))
                return false;
            final ViFPOS other = (ViFPOS)obj;
            if(this.getBuffer() != other.getBuffer() &&
                    (this.getBuffer() == null ||
                    !this.getBuffer().equals(other.getBuffer()))) {
                return false;
            }
            return this.getOffset() == other.getOffset();
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash =
                    79 * hash +
                    (this.getBuffer() != null
                        ? this.getBuffer().hashCode() : 0);
            hash = 79 * hash + this.getOffset();
            return hash;
        }
    }
}
