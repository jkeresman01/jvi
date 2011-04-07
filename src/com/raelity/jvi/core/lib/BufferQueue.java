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
package com.raelity.jvi.core.lib;

/**
 * Small queue of characters. Can't extend StringBuffer, so delegate.
 */
/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public final class BufferQueue implements CharSequence {
    private StringBuilder buf = new StringBuilder();

    public void setLength(int length)
    {
        buf.setLength(length);
    }

    @Override
    public int length()
    {
        return buf.length();
    }

    @Override
    public char charAt(int index)
    {
        return buf.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return buf.subSequence(start, end);
    }

    public boolean hasNext()
    {
        return buf.length() > 0;
    }

    public char removeFirst()
    {
        char c = buf.charAt(0);
        buf.deleteCharAt(0);
        return c;
    }

    public BufferQueue addFirst(char c)
    {
        buf.insert(0, c);
        return this;
    }

    public char removeLast()
    {
        int newLength = buf.length() - 1;
        char c = buf.charAt(newLength);
        buf.setLength(newLength);
        return c;
    }

    public char getLast()
    {
        return getLast(0);
    }

    public char getLast(int i)
    {
        return buf.charAt(buf.length() - 1 - i);
    }

    public BufferQueue insert(int ix, String s)
    {
        buf.insert(ix, s);
        return this;
    }

    public boolean hasCharAt(int idx)
    {
        return buf.length() > idx;
    }

    public char getCharAt(int idx)
    {
        return buf.charAt(idx);
    }

    public String substring(int idx)
    {
        return buf.substring(idx);
    }

    public BufferQueue append(char c)
    {
        buf.append(c);
        return this;
    }

    public BufferQueue append(int n)
    {
        buf.append(n);
        return this;
    }

    public BufferQueue append(String s)
    {
        buf.append(s);
        return this;
    }

    @Override
    public String toString()
    {
        return buf.toString();
    }

}
