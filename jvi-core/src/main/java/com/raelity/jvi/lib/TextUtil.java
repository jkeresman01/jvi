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

package com.raelity.jvi.lib;

import java.text.CharacterIterator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.text.Segment;

/** Some convenient functions when working with text. This class is not
 * meant to be instantiated, its methods are all static.
 */
public class TextUtil {
    
    private TextUtil() { }

    public static String sf(String fmt, Object... args) {
        return args.length == 0 ? fmt : String.format(fmt, args);
    }

    public static String toString(Set<?> set)
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        for(Object object : set) {
            sb.append(object.toString()).append(',');
            if(++i >= 5)
                break;
        }
        sb.append(']');
        return sb.toString();
    }
    
    /** Split a string into a list of words. White space,
     * '\s+', delimets the words.
     * A blank string returns and empty list.
     * The list may be empty, but no element is empty.
     */
    public static List<String> tokens(String s) {
        return tokens(s, "\\s+");
    }
    
    /** Split a string into a list of words. using <i>separarators</i>
     * to delineate the words.
     * A blank string, or a string with only separators, returns and empty list.
     * The list may be empty, but no element is empty.
     */
    public static List<String> tokens(String s, String separators) {
        //return s.isBlank()?List.copyOf():Arrays.asList(s.trim().split("\\s+"));

        // Do it this slightly more complicated way,
        // so there's only one expression for what is a blank.
        String[] split = s.split(separators);
        if(split.length > 0 && split[0].isEmpty())
            split = Arrays.copyOfRange(split, 1, split.length);
        return Arrays.asList(split);
    }
    
    /**
     * Return a String of the characters from getBeginIndex/getEndIndex;
     * ignore a terminating newline.
     * The argument is assumed to be a single line of text, possibly with
     * a terminating newline.
     * A newline may terminate the operation, independent of length.
     * So a String with a
     * newline in the middle produces unpredictable results.
     */
    public static String toString(CharacterIterator ci) {
        int initidx = ci.getIndex();
        if(ci instanceof MySegment) {
            int endidx = ci.getEndIndex();
            if(endidx - initidx <= 0)
                return "";
            Segment seg = (Segment)ci;
            if(seg.array[endidx-1] == '\n')
                endidx--;
            return new String(seg.array, initidx, endidx - initidx);
        } else {
            StringBuilder sb = new StringBuilder();
            for(char c = ci.first(); c != '\n' && c != CharacterIterator.DONE; c = ci.next())
                sb.append(c);
            ci.setIndex(initidx);
            return sb.toString();
        }
    }
    
    /** Return a String from the characterIterator;
     * start at iterators current position, beginIndex,
     * for the specified length.
     */
    public static String toString(CharacterIterator ci, int len) {
        int initidx = ci.getIndex();
        if(ci instanceof Segment) {
            Segment seg = (Segment)ci;
            return new String(seg.array, initidx, len);
        } else {
            StringBuilder sb = new StringBuilder();
            for(char c = ci.setIndex(initidx);
                    c != CharacterIterator.DONE
                    && ci.getIndex() < initidx + len; c = ci.next())
                sb.append(c);
            ci.setIndex(initidx);
            return sb.toString();
        }
    }

    public static String debugString(CharSequence s) {
        return debugString(s, 120);
    }
    
    /** replace control characters with something visible */
    public static String debugString(CharSequence s, int max) {
        if(s == null)
            return "(null)";
        StringBuilder sb = new StringBuilder();
        int i;
        for(i = 0; i < s.length() && sb.length() < max; i++) {
            char c = s.charAt(i);
            char t = 0;
            String esc = null;
            switch(c) {
                case '\t': t = 't'; break;
                case '\b': t = 'b'; break;
                case '\n': t = 'n'; break;
                case '\r': t = 'r'; break;
                case '\f': t = 'f'; break;
                    // case '\'': esc = "\\\'"; break;
                    // case '\"': esc = "\\\""; break;
                case '\\': esc = "\\\\"; break;
                default:
            }
            if(c < ' ' && t == 0 && esc == null) {
                // use '\<upper-case>' to show the character
                // for example Ctrl-W shows as \W (this is not a regex string!)
                t =   (char)(c | 0x40);
            }
            if(c >= 0x100) {
                sb.append(String.format("\\u%04x", (int)c));
            } else if(t == 0)
                sb.append(c);
            else {
                sb.append('\\');
                sb.append(t);
            }
        }
        if(i < s.length())
            sb.append(" ...");
        return sb.toString();
    }
    

}
