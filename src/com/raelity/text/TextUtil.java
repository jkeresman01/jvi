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
import javax.swing.text.Segment;

/** Some convenient functions when working with text. This class is not
 * meant to be instantiated, its methods are all static.
 */
public class TextUtil {

  /** Split a string into a vector of words. White space is used
   * for the delimeters, " \t\n\r".
   */
  public static Vector split(String s) {
    Vector<String> word = new Vector<String>();
    StringTokenizer parse = new StringTokenizer(s);
    while(parse.hasMoreElements()) {
      word.addElement(parse.nextToken());
    }
    return word;
  }

  /** Split a string into a vector of words, using <i>separarators</i>
   * to delineate the words.
   */
  public static Vector split(String s, String separators) {
    Vector<String> word = new Vector<String>();
    StringTokenizer parse = new StringTokenizer(s, separators);
    while(parse.hasMoreElements()) {
      word.addElement(parse.nextToken());
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
      for(char c = ci.setIndex(index); ci.getIndex() < index + len; c = ci.next())
        sb.append(c);
      return sb.toString();
    }
  }
  
  /** replace control characters with something visible,
   * only handle \n for now. */
  public static String debugString(String s) {
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if(c != '\n')
        sb.append(c);
      else {
        sb.append('\\');
        sb.append('n');
      }
    }
    return sb.toString();
  }
    
  /** To support jdk1.5, need a segment that isa CharSequence */
  public static class MySegment extends Segment implements CharSequence {
    public MySegment() {
      super();
    }
    
    public MySegment(Segment seg) {
      super(seg.array, seg.offset, seg.count);
    }
    
    public MySegment(char[] array, int offset, int count) {
      super(array, offset, count);
    }
    
    public char charAt(int index) {
      if (index < 0 || index >= count) {
        throw new StringIndexOutOfBoundsException(index);
      }
      return array[offset + index];
    }
    
    public int length() {
      return count;
    }
    
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
      return segment;
    }
  }
  
}
