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

package com.raelity.text;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;


/**
 * This is essentially a wrapped StringCharacterIterator, but that's final...
 * It's extended, to either return Null or DONE.
 * The "C" is for the language; return (char)0 instead of DONE.
 * @deprecated Use StringSegment
 * @author err
 */

@Deprecated
public class StringC implements CharacterIterator
{
    final private StringCharacterIterator si;
    private boolean returnNull;


    public StringC(String s)
    {
        this.si = new StringCharacterIterator(s);
        this.returnNull = true;
    }

    public void setReturnNull(boolean returnNull)
    {
        this.returnNull = returnNull;
    }

    public boolean isReturnNull()
    {
        return returnNull;
    }

    @Override
    final public char first()
    {
        char c = si.first();
        return returnNull && c == DONE ? 0 : c;
    }

    @Override
    final public char last()
    {
        char c = si.last();
        return returnNull && c == DONE ? 0 : c;
    }

    @Override
    final public char current()
    {
        char c = si.current();
        return returnNull && c == DONE ? 0 : c;
    }

    @Override
    final public char next()
    {
        char c = si.next();
        return returnNull && c == DONE ? 0 : c;
    }

    @Override
    final public char previous()
    {
        char c = si.previous();
        return returnNull && c == DONE ? 0 : c;
    }

    @Override
    final public char setIndex(int position)
    {
        char c = si.setIndex(position);
        return returnNull && c == DONE ? 0 : c;
    }

    @Override
    final public int getBeginIndex()
    {
        return si.getBeginIndex();
    }

    @Override
    final public int getEndIndex()
    {
        return si.getEndIndex();
    }

    @Override
    final public int getIndex()
    {
        return si.getIndex();
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    final public Object clone()
    {
        try {
            return super.clone();
        } catch(CloneNotSupportedException ex) {
            return null;
        }
    }
    
}
