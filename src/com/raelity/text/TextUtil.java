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
import java.util.*;
import java.util.ArrayList;
import javax.swing.text.Segment;

/** Some convenient functions when working with text. This class is not
 * meant to be instantiated, its methods are all static.
 */
public class TextUtil {
    
    private TextUtil() { }
    
    /** Split a string into a vector of words. White space is used
     * for the delimeters, " \t\n\r".
     */
    public static List<String> split(String s) {
        List<String> word = new ArrayList<String>();
        StringTokenizer parse = new StringTokenizer(s);
        while(parse.hasMoreElements()) {
            word.add(parse.nextToken());
        }
        return word;
    }
    
    /** Split a string into a vector of words, using <i>separarators</i>
     * to delineate the words.
     */
    public static List<String> split(String s, String separators) {
        List<String> word = new ArrayList<String>();
        StringTokenizer parse = new StringTokenizer(s, separators);
        while(parse.hasMoreElements()) {
            word.add(parse.nextToken());
        }
        return word;
    }
    
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
    
    public static String toString(CharacterIterator ci, int index, int len) {
        int i0 = ci.getBeginIndex();
        int i1 = ci.getEndIndex();
        if(ci instanceof Segment) {
            Segment seg = (Segment)ci;
            return new String(seg.array, index, len);
        } else {
            StringBuilder sb = new StringBuilder();
            for(char c = ci.setIndex(index);
                    ci.getIndex() < index + len; c = ci.next())
                sb.append(c);
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
    
    /**
     * Also keep "docOffset" which is the offset in the document
     * of the start of the segment; -1 implies not known.
     */
    public static class MySegment extends Segment {
        public int docOffset;
        
        public MySegment() {
            super();
            docOffset = -1;
        }
        
        public MySegment(MySegment seg) {
            super(seg.array, seg.offset, seg.count);
            docOffset =  seg.docOffset;
        }
        
        public MySegment(char[] array, int offset, int count, int docOffset) {
            super(array, offset, count);
            this.docOffset = docOffset;
        }
        
        @Override
        public CharSequence subSequence(int start, int end) {
            if (start < 0) {
                throw new StringIndexOutOfBoundsException(start);
            }
            if (end > count) {
                throw new StringIndexOutOfBoundsException(end);
            }
            if (start > end) {
                throw new StringIndexOutOfBoundsException(end - start);
            }
            MySegment segment = new MySegment();
            segment.array = this.array;
            segment.offset = this.offset + start;
            segment.count = end - start;
            segment.docOffset = this.docOffset + start;
            return segment;
        }
    }

}
