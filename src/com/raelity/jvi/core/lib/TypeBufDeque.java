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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raelity.jvi.core.G;
import com.raelity.jvi.core.GetChar;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Options;
import com.raelity.text.TextUtil;

import static com.raelity.jvi.core.Util.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.KeyDefs.NO_CHAR;

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
public final class TypeBufDeque {
    private Mappings mappings;
    private ArrayDeque<Integer> buf;
    private int nMappings;
    private int capacity;

    public TypeBufDeque(Mappings mappings)
    {
        this.mappings = mappings;
        clear();
    }

    public void clearNMappings()
    {
        nMappings = 0;
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
            temp[offset + i] = noremap-- > 0 ? (byte)1 : (byte)0;
        }

        if(offset == 0) {
            // now combine remap and string int integers
            // and add them to deque (in reverse order!).
            for(int i = temp.length-1; i >= 0; i--) {
                buf.addFirst((temp[i] << 16) | str.charAt(i));
            }
        } else {
            // Make a big array, then use System.arrayCopy to
            // do the insert, then wrap the array in a list
            // and add that back to a new Deque

            // crash and burn
            Object o = null;
            System.err.println("Crash and burn" + o.toString());

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
            Object o = null;
            System.err.println("Crash and burn" + o.toString());
        }
        for(int i = end - start; i > 0; i--) {
            buf.removeFirst();
        }
    }

    /**
     * return a character, map as needed.
     * @return
     */
    public char getChar()
    {
        char c;
        int loops = 0;
        while(true) {
            int noremap = buf.removeFirst();
            c = (char)(noremap & 0xffff);
            noremap &= ~0xffff;

            if(Options.isKeyDebug()) {
                if(Options.isKeyDebug(Level.FINEST)
                        || G.no_mapping() != 0
                        || G.allow_keys() != 0
                        || G.no_zero_mapping() != 0) {
                    System.err.printf("getChar check: '%s'"
                            + " noremap=%b, state=0x%x,"
                            + " G.no_mapping=%d, G.allow_keys=%d"
                            + " G.no_zero_mapping=%d\n",
                            TextUtil.debugString(String.valueOf(c)),
                            noremap != 0,
                            get_real_state(),
                            G.no_mapping(),
                            G.allow_keys(),
                            G.no_zero_mapping());
                }
            }
            if(noremap == 0
                    && G.no_mapping() == 0
                    && (c != '0' || G.no_zero_mapping() == 0)
                    // && G.allow_keys == 0 ?????
            ) {
                int state = get_real_state();
                Mapping mapping = mappings.getMapping(c, state);
                if(mapping != null) {
                    if(++loops > G.p_mmd()) {
                        Msg.emsg("recursive mapping");
                        c = NO_CHAR;
                        break;
                    }
                    if(nMappings++ > G.p_mmd2()) {
                        Msg.emsg("internal map-cmd error, file bug report");
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
                    if(Options.isKeyDebug() && loops <= 20) {
                        System.err.println("getChar: map: " + mapping +
                                (loops == 20 ? "... ... ..." : ""));
                    }
                    if(!insert(mapping.getRhs(),
                            mapping.isNoremap()
                                ? -1
                                : mapping.getRhs().startsWith(mapping.getLhs())
                                    ? 1 : 0,
                            0,
                            true)) {
                        c = NO_CHAR;
                        break;
                    }
                    // do it again
                    continue;
                }
            }
            break;
        }
        if(c == NO_CHAR) {
            clear(); // note, this should have already been done
        }
        if(Options.isKeyDebug(Level.FINEST)) {
            System.err.println("getChar return: " +
                    TextUtil.debugString(String.valueOf(c)));
        }
        return c;
    }

    public void clear()
    {
        buf = new ArrayDeque<Integer>(250);
        capacity = 0;
    }

    public boolean hasNext()
    {
        return buf.size() > 0;
    }

    public int length()
    {
        return buf.size();
    }

    @Override
    public String toString()
    {
        return buf.toString();
    }

}
