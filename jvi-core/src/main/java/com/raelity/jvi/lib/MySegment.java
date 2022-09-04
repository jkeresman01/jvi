/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.lib;

import javax.swing.text.Segment;


/**
 * Some "enhancements" to the standard Segment.
 * In jVi MySegment is typically represents one line of text in the document.
 * 
 * Keep "docOffset" which is the offset in the document
 * of the start of the segment; -1 implies not known.
 * 
 * There is a MySegment(String) constructor which converts the string to
 * an array. This has slightly different behavior
 * In particular, charAt(length()) returns doneChar instead of an exception.
 * Use the subclass {@link StringSegment} and doneChar is NUL instead
 * of char 0xffff;
 * 
 * Some additional methods:
 * - charIterator: atEnd()
 * 
 * Apr 1 2020
 * Copy *everything* from Segment. Well, almost everything, not partial-return;
 * Use array, offset, count.
 */
public class MySegment extends Segment //implements Cloneable
{
    public int docOffset;
    protected char doneChar = DONE;
    /** 0 or 1; when one there's a magic doneChar */
    private int charAtCheatOffset;
    private int pos;
    private boolean wOK;

    static public MySegment getWritable(char[] chars)
    {
        MySegment seg = new MySegment(chars, 0, chars.length, -1);
        seg.wOK = true;
        return seg;
    }

    public MySegment()
    {
        super();
        this.docOffset = -1;
    }

    /**
     * Copy the string to an array. Return null instead of DONE.
     * Method charAt() return doneChar for first index past end of string
     * rather than exception.
     * @param str 
     */
    public MySegment(String str)
    {
        // TODO: implementation that doesn't copy the string
        this(str.toCharArray(), 0, str.length(), -1);
        // When this constructor is used, use DONE not NUL.
        // Subclass, like StringSegment, can override with setReturnNull().
        charAtCheatOffset = 1;
    }

    public MySegment(MySegment seg)
    {
        this(seg.array, seg.offset, seg.count, seg.docOffset);
    }

    public MySegment(char[] array, int offset, int count, int docOffset)
    {
        super(array, offset, count);
        this.docOffset = docOffset;
        pos = offset;
    }

    final public char getDoneChar()
    {
        return doneChar;
    }

    /**
     * Whether to return DONE or null for characterIterator end.
     * @param returnNull if true then return null
     */
    final protected void setReturnNull(boolean returnNull)
    {
        doneChar = returnNull ? 0 : DONE;
    }

    /**
     * Converts a segment into a String.
     *
     * @return the string
     */
    @Override
    public String toString() {
        if (array != null) {
            return new String(array, offset, count);
        }
        return "";
    }

    public String substring(int start, int end) {
        if (array != null) {
            return new String(array, offset+start, end - start);
        }
        return "";
    }

    // --- CharacterIterator methods -------------------------------------

    /**
     * Sets the position to getBeginIndex() and returns the character at that
     * position.
     * @return the first character in the text, or DONE if the text is empty
     * @see #getBeginIndex
     * @since 1.3
     */
    @Override
    public char first() {
        pos = offset;
        if (count != 0) {
            return array[pos];
        }
        return doneChar;
    }

    /**
     * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty)
     * and returns the character at that position.
     * @return the last character in the text, or DONE if the text is empty
     * @see #getEndIndex
     * @since 1.3
     */
    @Override
    public char last() {
        pos = offset + count;
        if (count != 0) {
            pos -= 1;
            return array[pos];
        }
        return doneChar;
    }

    /**
     * Gets the character at the current position (as returned by getIndex()).
     * @return the character at the current position or DONE if the current
     * position is off the end of the text.
     * @see #getIndex
     * @since 1.3
     */
    @Override
    public char current() {
        if (count != 0 && pos < offset + count) {
            return array[pos];
        }
        return doneChar;
    }

    /**
     * Increments the iterator's index by one and returns the character
     * at the new index.  If the resulting index is greater or equal
     * to getEndIndex(), the current index is reset to getEndIndex() and
     * a value of DONE is returned.
     * @return the character at the new position or DONE if the new
     * position is off the end of the text range.
     * @since 1.3
     */
    @Override
    public char next() {
        pos += 1;
        int end = offset + count;
        if (pos >= end) {
            pos = end;
            return doneChar;
        }
        return current();
    }

    /**
     * Decrements the iterator's index by one and returns the character
     * at the new index. If the current index is getBeginIndex(), the index
     * remains at getBeginIndex() and a value of DONE is returned.
     * @return the character at the new position or DONE if the current
     * position is equal to getBeginIndex().
     * @since 1.3
     */
    @Override
    public char previous() {
        if (pos == offset) {
            return doneChar;
        }
        pos -= 1;
        return current();
    }

    /**
     * Sets the position to the specified position in the text and returns that
     * character.
     * @param position the position within the text.  Valid values range from
     * getBeginIndex() to getEndIndex().  An IllegalArgumentException is thrown
     * if an invalid value is supplied.
     * @return the character at the specified position or DONE if the specified position is equal to getEndIndex()
     * @since 1.3
     */
    @Override
    public char setIndex(int position) {
        int end = offset + count;
        if ((position < offset) || (position > end)) {
            throw new IllegalArgumentException("bad position: " + position);
        }
        pos = position;
        if ((pos != end) && (count != 0)) {
            return array[pos];
        }
        return doneChar;
    }

    /**
     * Returns the start index of the text.
     * @return the index at which the text begins.
     * @since 1.3
     */
    @Override
    public int getBeginIndex() {
        return offset;
    }

    /**
     * Returns the end index of the text.  This index is the index of the first
     * character following the end of the text.
     * @return the index after the last character in the text
     * @since 1.3
     */
    @Override
    public int getEndIndex() {
        return offset + count;
    }

    /**
     * Returns the current index.
     * @return the current index.
     * @since 1.3
     */
    @Override
    public int getIndex() {
        return pos;
    }

    public boolean atEnd() {
        // return doneChar == current();
        return count == 0 || pos >= offset + count;
    }

    /**
     * Direct access relative to array start, put bounds check in an assert.
     * Replaces seg.array[ptr].
     * <br/><br/>
     * NOTE: doneChar NOT HANDLED
     */
    final public char r(int position) {
        assert !((position < offset) || (position >= offset + count));
        return array[position];
    }

    /**
     * Direct access relative to array start, put bounds check in an assert.
     * Replaces seg.array[ptr].
     * <br/><br/>
     * NOTE: doneChar NOT HANDLED
     */
    final public void w(int position, char c) {
        assert wOK && !((position < offset) || (position >= offset + count));
        if(!wOK)
            return;
        array[position] = c;
    }

    // --- CharSequence methods -------------------------------------

    /**
     * Direct access relative to offset, put bounds check in an assert.
     * Replaces seg.array[index + seg.offset].
     * <br/><br/>
     * NOTE: doneChar NOT HANDLED
     */
    final public char fetch(int index) {
        assert !(index < 0 || index >= count);
        return array[offset + index];
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    @Override
    public char charAt(int index) {
        if(index < 0 || index >= count + charAtCheatOffset)
            throw new StringIndexOutOfBoundsException(index);
        return index != count ? array[offset + index] : doneChar;
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    @Override
    public int length() {
        return count;
    }

    @Override
    public MySegment subSequence(int start, int end)
    {
        if(start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if(end > count) {
            throw new StringIndexOutOfBoundsException(end);
        }
        if(start > end) {
            throw new StringIndexOutOfBoundsException(end - start);
        }

        MySegment newSeg = (MySegment)clone();
        subSequenceFixup(newSeg, start, end);
        return newSeg;
    }

    protected void subSequenceFixup(MySegment newSeg, int start, int end)
    {
        newSeg.offset = this.offset + start;
        newSeg.count = end - start;
        newSeg.docOffset = this.docOffset + start;
        newSeg.pos = newSeg.offset;
    }

    /**
     * Creates a shallow copy.
     *
     * @return the copy
     */
    @Override
    public Object clone() {
        return super.clone();
    }
    
}
