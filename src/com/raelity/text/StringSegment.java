/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2019 Ernie Rael.  All Rights Reserved.
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

package com.raelity.text;

import com.raelity.text.TextUtil.MySegment;

/**
 * A segment that is backed by a String.
 * The biggest problem is returning a zero
 * for an access that would be the null terminator
 * 
 * NOTE: if the segment's array is accessed, there is probably an NPE.
 * 
 * This should have been around in jVi forever...
 * 
 * @author err
 */
public class StringSegment extends MySegment
{
private String s;
private boolean returnNull = true;

private StringSegment() {
}

/**
 * The whole string which is virtually null terminated
 */
public StringSegment(String s)
{
    this.s = s;
    this.offset = 0;
    this.count = s.length();
    this.docOffset = -1;
    this.pos = offset;
}

public boolean isReturnNull()
{
    return returnNull;
}

public void setReturnNull(boolean returnNull)
{
    this.returnNull = returnNull;
}

/**
 * Converts a segment into a String.
 *
 * @return the string
 */
@Override
public String toString() {
    return s.substring(offset, offset + count);
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
final public char first() {
    pos = offset;
    if (count != 0) {
        return s.charAt(pos);
    }
    return returnNull ? 0 : DONE;
}

/**
 * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty)
 * and returns the character at that position.
 * @return the last character in the text, or DONE if the text is empty
 * @see #getEndIndex
 * @since 1.3
 */
@Override
final public char last() {
    pos = offset + count;
    if (count != 0) {
        pos -= 1;
        return s.charAt(pos);
    }
    return returnNull ? 0 : DONE;
}

/**
 * Gets the character at the current position (as returned by getIndex()).
 * @return the character at the current position or DONE if the current
 * position is off the end of the text.
 * @see #getIndex
 * @since 1.3
 */
@Override
final public char current() {
    if (count != 0 && pos < offset + count) {
        return s.charAt(pos);
    }
    return returnNull ? 0 : DONE;
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
final public char next() {
    pos += 1;
    int end = offset + count;
    if (pos >= end) {
        pos = end;
        return returnNull ? 0 : DONE;
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
final public char previous() {
    if (pos == offset) {
        return returnNull ? 0 : DONE;
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
final public char setIndex(int position) {
    int end = offset + count;
    if ((position < offset) || (position > end)) {
        throw new IllegalArgumentException("bad position: " + position);
    }
    pos = position;
    if ((pos != end) && (count != 0)) {
        return s.charAt(pos);
    }
    return returnNull ? 0 : DONE;
}

/**
 * Returns the start index of the text.
 * @return the index at which the text begins.
 * @since 1.3
 */
@Override
final public int getBeginIndex() {
    return offset;
}

/**
 * Returns the end index of the text.  This index is the index of the first
 * character following the end of the text.
 * @return the index after the last character in the text
 * @since 1.3
 */
@Override
final public int getEndIndex() {
    return offset + count;
}

/**
 * Returns the current index.
 * @return the current index.
 * @since 1.3
 */
@Override
final public int getIndex() {
    return pos;
}

final public boolean atEnd() {
    return count == 0 || pos >= offset + count;
}

    // --- CharSequence methods -------------------------------------

/**
 * Cheat. Allow charAt which goes 1 beyond count, return null or DONE
 * {@inheritDoc}
 * @since 1.6
 */
@Override
final public char charAt(int index) {
    if (index < 0
            || index > count) {
        throw new StringIndexOutOfBoundsException(index);
    }
    if(index == count)
        return returnNull ? 0 : DONE;
    return s.charAt(offset+index);
}

/**
 * {@inheritDoc}
 * @since 1.6
 */
@Override
final public int length() {
    return count;
}

final public String substring(int start, int end) {
    return s.substring(offset+start, offset+end);
}

/**
 * {@inheritDoc}
 * @since 1.6
 */
@Override
final public StringSegment subSequence(int start, int end) {
    if (start < 0) {
        throw new StringIndexOutOfBoundsException(start);
    }
    if (end > count) {
        throw new StringIndexOutOfBoundsException(end);
    }
    if (start > end) {
        throw new StringIndexOutOfBoundsException(end - start);
    }
    StringSegment segment = new StringSegment();
    segment.s = this.s;
    segment.offset = this.offset + start;
    segment.count = end - start;
    segment.pos = segment.offset;
    return segment;
}

/**
 * Creates a shallow copy.
 *
 * @return the copy
 */
@Override
final public Object clone() {
    return super.clone();
}

private int pos;


//}

}