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

package com.raelity.text;

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
     * '\s+', delimets the words. A blank string return
     * and empty list.
     */
    public static List<String> tokens(String s) {
        //return s.isBlank()?List.copyOf():Arrays.asList(s.trim().split("\\s+"));

        // Do it this slightly more complicated way,
        // so there's only one expression for what is a blank.
        String[] split = s.split("\\s+");
        if(split.length > 0 && split[0].isEmpty())
            split = Arrays.copyOfRange(split, 1, split.length);
        return Arrays.asList(split);
    }
    
    // /** Split a string into a vector of words, using <i>separarators</i>
    //  * to delineate the words.
    //  */
    // public static List<String> tokens(String s, String separators) {
    //     List<String> word = new ArrayList<>();
    //     StringTokenizer parse = new StringTokenizer(s, separators);
    //     while(parse.hasMoreElements()) {
    //         word.add(parse.nextToken());
    //     }
    //     return word;
    // }
    
    /** Return a String of the characters from getBeginIndex/getEndIndex */
    public static String toString(CharacterIterator ci) {
        int i0 = ci.getBeginIndex();
        int i1 = ci.getEndIndex();
        if(ci instanceof Segment) {
            Segment seg = (Segment)ci;
            return new String(seg.array, i0, i1 - i0);
        } else {
            StringBuilder sb = new StringBuilder();
            for(char c = ci.first(); c != CharacterIterator.DONE; c = ci.next())
                sb.append(c);
            return sb.toString();
        }
    }
    
    /** Return a String of the characters of index for len */
    public static String toString(CharacterIterator ci, int index, int len) {
        int i0 = ci.getBeginIndex();
        int i1 = ci.getEndIndex();
        int initidx = ci.getIndex();
        if(ci instanceof Segment) {
            Segment seg = (Segment)ci;
            return new String(seg.array, index, len);
        } else {
            StringBuilder sb = new StringBuilder();
            for(char c = ci.setIndex(index);
                    c != CharacterIterator.DONE
                    && ci.getIndex() < index + len; c = ci.next())
                sb.append(c);
            ci.setIndex(initidx);
            return sb.toString();
        }
    }
    
    /** replace control characters with something visible */
    public static String debugString(CharSequence s) {
        if(s == null)
            return "(null)";
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length(); i++) {
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
        return sb.toString();
    }
    

}
