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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raelity.jvi.core.G;
import com.raelity.jvi.core.GetChar;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.lib.MutableBoolean;
import com.raelity.jvi.lib.TextUtil;

import static com.raelity.jvi.core.Misc01.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;
import static com.raelity.jvi.lib.TextUtil.sf;

/**
 * Like BufferQueue, but typebuf has some special requirements.
 * In particular, typebuf has a parallel data structure to track
 * if a given char can be re-mapped.
 * <p/>
 * NOTE: always: buf.length() == noremapbuf.length()
 * <p/>
 * NEEDSWORK: I bet copying the Deque code and useing an int array
 * would be real fast.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public final class TypeBufMultiCharMapping {
    private static final Logger LOG = Logger.getLogger(TypeBufMultiCharMapping.class.getName());

    private final Mappings mappings;
    private ArrayDeque<Integer> buf;
    private int nMappings;
    private int capacity;
    private boolean fWaitMapping;
    private boolean fMappingTimeout;

    public TypeBufMultiCharMapping(Mappings mappings)
    {
        this.mappings = mappings;
        clear();
    }

    interface TypeBufPeek
    {
        char firstChar();

        boolean isMatch(String lhs, MutableBoolean maybe);
    }

    private boolean isNoremap(int i)
    {
        return (i & ~0xffff) != 0;
    }

    /*
     * startsWith and couldMatch use iterator
     * along with building a temporary string
     * string gets destroyed when deque is modified
     *
     * NEEDSWORK: noremap issues
     */
    private class MyTypeBufPeek implements TypeBufPeek
    {
        @Override
        public char firstChar()
        {
            int i = buf.getFirst(); // does not remove
            assert ! isNoremap(i); // should not be called if noremap
            return (char)(i & 0xffff);
        }

        @Override
        public boolean isMatch(String lhs, MutableBoolean fPartialMatch)
        {
            int idx = 0;
            for(Iterator<Integer> it = buf.iterator();
                    it.hasNext() && idx < lhs.length(); idx++) {
                int i = it.next();
                if(lhs.charAt(idx) != (i & 0xffff) || isNoremap(i)) {
                    return false;
                }
            }

            if(idx == buf.size()) {
                fPartialMatch.setValue(true);
                partialMatch = lhs.substring(0, idx);
            }
            return idx == lhs.length();
        }
    }
    private final TypeBufPeek peek = new MyTypeBufPeek();
    private String partialMatch = "";

    public void clearNMappings()
    {
        nMappings = 0;
    }

    public String getPartialMatch()
    {
        return fWaitMapping ? partialMatch : null;
    }

    public void mappingTimeout()
    {
        fMappingTimeout = true;
    }

    public boolean insert(CharSequence str, int noremap,
                          int offset, boolean nottyped)
    {
        // if maxSize > 3/4 of MAX then shrink the buffer
        // NEEDSWORK: have some hysteresis, so don't shrink too soon
        if(capacity > ADJUSTTYPEBUFLEN && buf.isEmpty()) {
            clear();
        }
        //
        // NEEDSWORK: ins_typebuf: performance can be improved
        //            For example, pass in a char array
        //
        if(length() + str.length() > MAXTYPEBUFLEN) {
            vim_beep();
            Msg.emsg(Messages.e_toocompl); // also calls flushbuff
            //setcursor();
            Logger.getLogger(GetChar.class.getName()).
                    log(Level.SEVERE, null, new Exception("MAXTYPEBUFLEN"));
            return false;
        }

        //
        // Adjust noremapbuf[] for the new characters:
        // If noremap  < 0: all the new characters are flagged not remappable
        // If noremap == 0: all the new characters are flagged mappable
        // If noremap  > 0: 'noremap' characters are flagged not remappable,
        //                  the rest mappable
        //
        if(noremap < 0) {
            noremap = str.length();
        }
        // NEEDSWORK: optimize this by only having one loop,
        byte[] temp = new byte[str.length()];
        for(int i = 0; i < str.length(); i++) {
            // noremapbuf.insert(offset + i, (char)(noremap-- > 0 ? 1 : 0));
            temp[i] = noremap-- > 0 ? (byte)1 : (byte)0;
        }

        if(offset == 0) {
            // now combine remap and string int integers
            // and add them to deque (in reverse order!).
            for(int i = temp.length-1; i >= 0; i--) {
                buf.addFirst((temp[i] << 16) | str.charAt(i));
            }
        } else if(offset == buf.size()) {
            // append to end of Deque
            for(int i = 0; i < temp.length; i++) {
                buf.addLast((temp[i] << 16) | str.charAt(i));
            }
        } else {
            // Make a big array, then use System.arrayCopy to
            // do the insert, then wrap the array in a list
            // and add that back to a new Deque

            // crash and burn
            throw new IllegalStateException("unhandled insert in middle");

            // Integer[] toArray = buf.toArray(new Integer[0]);
        }

        /* ************************************************************
         * // this is only correct for offset == 0!
         * if (nottyped)		// the inserted string is not typed
         * typemaplen += str.length();
         * if (no_abbr_cnt && offset == 0) // and not used for abbreviations
         * no_abbr_cnt += str.length();
         *************************************************************/

        if(buf.size() > capacity)
            capacity = buf.size();
        return true;
    }

    public void delete(int start, int end)
    {
        if(start != 0) {
            // crash and burn
            throw new IllegalStateException("unhandled delete in middle");
        }
        for(int i = end - start; i > 0; i--) {
            buf.removeFirst();
        }
    }

    /**
     * return a character, map as needed.
     * @return lower half of int is the char, upper half is modifiers
     */
    public int getChar()
    {
        fWaitMapping = false;
        boolean sawMappingTimeout = fMappingTimeout;
        fMappingTimeout = false;
        if(buf.isEmpty())
            return NO_CHAR;

        char c;
        int loops = 0;
        boolean needsRemove;
        while(true) {
            int peekData = buf.getFirst();
            needsRemove = true;
            boolean noremap = isNoremap(peekData);
            c = (char)(peekData & 0xffff);

                        if(Options.kd().getBoolean()) {
                            if(Options.kd().getBoolean(Level.FINEST)
                                    || G.no_mapping() != 0
                                    || G.allow_keys() != 0
                                    || G.no_zero_mapping() != 0) {
                                System.err.printf("getChar check: '%s'"
                                        + " noremap=%b, state=0x%x,"
                                        + " G.no_mapping=%d, G.allow_keys=%d"
                                        + " G.no_zero_mapping=%d\n",
                                        TextUtil.debugString(String.valueOf(c)),
                                        noremap,
                                        get_real_state(),
                                        G.no_mapping(),
                                        G.allow_keys(),
                                        G.no_zero_mapping());
                            }
                        }

            if(!noremap
                    && G.no_mapping() == 0
                    && (c != '0' || G.no_zero_mapping() == 0)
                    // && G.allow_keys == 0 ?????
            ) {
                int state = get_real_state();
                MutableBoolean fPartialMatch = new MutableBoolean();
                Mapping mapping = mappings.getMapping(peek, state, fPartialMatch);
                if(mapping != null) {
                    if(++loops > G.p_mmd()) {
                        vim_beep();
                        Msg.emsg("recursive mapping");
                        needsRemove = false;
                        c = NO_CHAR;
                        break;
                    }
                    if(nMappings++ > G.p_mmd2()) {
                        vim_beep();
                        Msg.emsg("map-cmd error, probably recursive mapping");
                        needsRemove = false;
                        c = NO_CHAR;
                        break;
                    }
                    //
                    // Insert the 'to' part in the typebuf.
                    // If 'from' field is the same as the start of the
                    // 'to' field, don't remap the first character.
                    // If m_noremap is set, don't remap the whole 'to'
                    // part.
                    //
                    if(Options.kd().getBoolean() && loops <= 20) {
                        System.err.println("getChar: map: " + mapping +
                                (loops == 20 ? "... ... ..." : ""));
                    }
                    // get rid of the stuff we've matched
                    for(int i = mapping.lhs.length(); i > 0; --i) {
                        buf.removeFirst();
                    }
                    needsRemove = false;
                    if(!insert(mapping.getRhs(),
                            mapping.isNoremap()
                                ? -1
                                : mapping.getRhs().startsWith(mapping.getLhs())
                                    ? 1 : 0,
                            0,
                            true)) {
                        clear();
                        c = NO_CHAR;
                        break;
                    }
                    // do it again
                    continue;
                } else if(fPartialMatch.getValue()) {
                    // matches something so far
                    if(!sawMappingTimeout) {
                        fWaitMapping = true;
                        Options.kd().println(Level.FINEST, () ->
                                "getChar partial match: " + partialMatch);
                        c = NO_CHAR;
                        needsRemove = false;
                    }
                    break;
                }
            } // end if(!noremap
            break;
        } // end while(true)
        if(needsRemove)
            buf.removeFirst();
        final char cF = c;
        Options.kd().println(Level.FINEST, () ->
                "getChar return: " + TextUtil.debugString(String.valueOf(cF)));

        int rval;
        if(isVIRT(c)) {
            int modifiers = c & (MOD_MASK << MODIFIER_POSITION_SHIFT);
            if(modifiers != 0) {
                c &= ~modifiers;
                modifiers = modifiers >> MODIFIER_POSITION_SHIFT;

                // if modifiers are only/exactly the shift key
                if(modifiers == SHFT
                        && c >= VIRT && c < VIRT + SHIFTED_VIRT_OFFSET) {
                    // only the shift key is pressed and its one of "those".
                    c += SHIFTED_VIRT_OFFSET;
                }
            }

            if(c == K_SPACE)
                c = ' ';
            rval = c | (modifiers << 16);
        } else
            rval = c;

        return rval;
    }

    public void clear()
    {
        buf = new ArrayDeque<>(250);
        capacity = 0;
        fMappingTimeout = false;
        fWaitMapping = false;
    }

    public boolean hasNext()
    {
        return buf.size() > 0;
    }

    public int length()
    {
        return buf.size();
    }

    public boolean isEmpty()
    {
        return buf.isEmpty();
    }

    @Override
    public String toString()
    {
        Iterator<Integer> i = buf.iterator();
	if (! i.hasNext())
	    return "[]";

	StringBuilder sb = new StringBuilder();
	sb.append('[');
	for (;;) {
            Integer data = i.next();
            if(isNoremap(data))
                sb.append('*');

	    sb.append((char)data.intValue());
	    if (! i.hasNext())
		return sb.append(']').toString();
	    sb.append(", ");
	}
    }

}
