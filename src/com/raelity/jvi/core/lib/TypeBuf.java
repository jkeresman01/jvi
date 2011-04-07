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

import com.raelity.jvi.core.Constants;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.GetChar;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Options;
import com.raelity.text.TextUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.raelity.jvi.core.Util.*;

/**
 * Like BufferQueue, but typebuf has some special requirements.
 * In particular, typebuf has a parallel data structure to track
 * if a given char can be re-mapped.
 * <p/>
 * NOTE: always: buf.length() == noremapbuf.length()
 * <p/>
 * NEEDSWORK: There's a performance problem with using a StringBuilder.
 * The insert and delete at the beginning are arraycopy.
 * USE A deque, but DEQUE doesn't support the insert at arbitrary spot.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public final class TypeBuf {
    //implements CharSequence {
    private Mappings mappings;
    private StringBuilder buf = new StringBuilder();
    private StringBuilder noremapbuf = new StringBuilder();
    private int nMappings;

    public TypeBuf(Mappings mappings)
    {
        this.mappings = mappings;
    }

    public void clearNMappings()
    {
        nMappings = 0;
    }

    public boolean insert(CharSequence str, int noremap, int offset,
                          boolean nottyped)
    {
        // if cap() > 3/4 of MAX then shrink the buffer
        // NEEDSWORK: have some hysteresis, so don't shrink too soon
        if(buf.capacity() >
                    Constants.MAXTYPEBUFLEN - (Constants.MAXTYPEBUFLEN >> 2)
                && buf.length() < 500) {
            buf.trimToSize();
            noremapbuf.trimToSize();
        }
        //
        // NEEDSWORK: ins_typebuf: performance can be improved
        //            For example, pass in a char array
        //
        if(length() + str.length() > Constants.MAXTYPEBUFLEN) {
            Msg.emsg(Messages.e_toocompl); // also calls flushbuff
            //setcursor();
            Logger.getLogger(GetChar.class.getName()).
                    log(Level.SEVERE, null, new Exception("MAXTYPEBUFLEN"));
            return false;
        }
        buf.insert(offset, str);
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
        for(int i = 0; i < str.length(); i++) {
            noremapbuf.insert(offset + i, (char)(noremap-- > 0 ? 1 : 0));
        }
        /* ************************************************************
         * // this is only correct for offset == 0!
         * if (nottyped)		// the inserted string is not typed
         * typemaplen += str.length();
         * if (no_abbr_cnt && offset == 0) // and not used for abbreviations
         * no_abbr_cnt += str.length();
         *************************************************************/
        return true;
    }

    public void delete(int start, int end)
    {
        buf.delete(start, end);
        noremapbuf.delete(start, end);
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
            c = buf.charAt(0);
            if(Options.isKeyDebug()) {
                if(Options.isKeyDebug(Level.FINEST)
                        || (G.no_mapping() != 0 || G.allow_keys() != 0)) {
                    System.err.printf("getChar check: '%s'"
                            + " noremap=%b, state=0x%x,"
                            + " G.no_mapping=%d, G.allow_keys=%d\n",
                            TextUtil.debugString(String.valueOf(c)),
                            noremapbuf.charAt(0) != 0,
                            get_real_state(),
                            G.no_mapping(),
                            G.allow_keys());
                }
            }
            if(noremapbuf.charAt(0) == 0
                    && G.no_mapping() == 0) { // && G.allow_keys == 0 ?????
                int state = get_real_state();
                Mapping mapping = mappings.getMapping(c, state);
                if(mapping != null) {
                    if(++loops > G.p_mmd()) {
                        Msg.emsg("recursive mapping");
                        c = (char)-1;
                        break;
                    }
                    if(nMappings++ > G.p_mmd2()) {
                        Msg.emsg("internal map-cmd error, file bug report");
                        c = (char)-1;
                        break;
                    }
                    // ok, map it. first delete old char
                    delete(0, 1);
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
                               mapping.isNoremap() ? -1
                            : mapping.getRhs().startsWith(mapping.getLhs()) ? 1
                            : 0, 0, true)) {
                        c = (char)-1;
                        break;
                    }
                    continue;
                }
            }
            break;
        }
        if(c != -1) {
            delete(0, 1);
        } else {
            setLength(0); // note, this should have already been done
        }
        assert buf.length() == noremapbuf.length();
        if(Options.isKeyDebug(Level.FINEST)) {
            System.err.println("getChar return: " +
                    TextUtil.debugString(String.valueOf(c)));
        }
        return c;
    }

    public void setLength(int newLength)
    {
        buf.setLength(newLength);
        noremapbuf.setLength(newLength);
    }

    public boolean hasNext()
    {
        return buf.length() > 0;
    }

    public int length()
    {
        return buf.length();
    }

    @Override
    public String toString()
    {
        return buf.toString();
    }

}
