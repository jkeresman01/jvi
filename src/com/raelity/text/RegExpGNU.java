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

import gnu.regexp.*;

/**
 * Implementation of RegExp using OROMatch from www.oroinc.com.
 * @see RegExp#fixupEscape(char[] pattern, int offset, char escape)
 */

public class RegExpGNU extends RegExp {
  static String theClass = "gnu.regexp.RE";
  RE matcher;
  int offset; // added to result positions, since may not use original start

  REMatch result;
  int nGroup;   // the number of sub expressions.

  /**
   * @return True if the adapted reg ex package is available,
   * otherwise false.
   */
  public static boolean canInstantiate() {
    try {
      Class.forName(theClass);
    } catch(Exception e) {
      return false;
    }
    return true;
  }

  public static String getDisplayName() {
    return "GNURegexp";
  }

  static public int patternType() {
    return PATTERN_PERL5;
  }

  /**
   * The name of the regular expression class that this
   * class is adapting is returned.
   * @return The underlying regular expresion package.
   */
  public static String getAdaptedName() {
    return theClass;
  }

  public RegExpGNU() {
  }

  /**
   * Prepare this regular expression to use <i>pattern</i>
   * for matching. The string can begin with "(e?=x)" to
   * specify an escape character.
   */
  public void compile(String patternString, int compileFlags)
	    throws RegExpPatternError
  {
    int flags = RE.REG_MULTILINE; // also treats end of string as newline
    flags |= ((compileFlags & RegExp.IGNORE_CASE) != 0)
              ? RE.REG_ICASE : 0;
    char fixupBuffer[];
    if(patternString.startsWith("(?e=")
       && patternString.length() >= 6
       && patternString.charAt(5) == ')') {
      // change the escape character
      fixupBuffer = patternString.toCharArray();
      patternString = fixupEscape(fixupBuffer, 6, fixupBuffer[4]);
    } else if(escape != '\\') {
      // change the escape character
      patternString = fixupEscape(patternString.toCharArray(), 0, escape);
    }
    try {
      // replace \b with (?:[a-zA-Z_0-9]), i think they're equivelent
      patternString = fixupBreak(patternString);
      matcher = new RE(patternString, flags);
    } catch(REException e) {
      throw new RegExpPatternError(e);
    } catch(ArrayIndexOutOfBoundsException e) {
      throw new RegExpPatternError("ArrayIndexOutOfBoundsException");
    }
    result = null;
  }
  
  /**
   * replace \b with (?:[a-zA-Z_0-9]), i think they're equivelent
   * <p>
   * Damn, they are not really equivelent, it is not zero width.
   * What i'd like to use is (?=...), but that's not supported.
   * So throw an error if "\b" is encountered.
   */
  String fixupBreak(String pat) {
    if(pat.indexOf('\\') < 0) {
      return pat;
    }
    
    StringBuffer buf = new StringBuffer(pat.length()+1);
    int len = pat.length();
    for(int i = 0; i < len; i++) {
      char c = pat.charAt(i);
      if(c == '\\' && i < len - 1) {
	char c2 = pat.charAt(i+1);
	if(c2 == 'b' || c2 == 'B') {
	  // buf.append("(?:\\W)"); // NEEDSWORK: use a char class
	  // buf.append("(?:\\W)"); DO WHAT FOR 'B'
	  throw new RegExpPatternError("'\\b' not support by "
	                               + getDisplayName());
	} else {
	  buf.append(c);
	  buf.append(c2);
	}
	i++; // skip quoted char
      } else {
	buf.append(c);
      }
    }
    return buf.toString();
  }

  /**
   * Search for match.
   * @return true if <i>input</i> matches this regular expression.
   */
  public boolean search(String input) {
    return search(input, 0, input.length());
  }

  /**
   * Search for a match in string starting at char position <i>start</i>.
   * @return true if <i>input</i> matches this regular expression.
   */
  public boolean search(String input, int start) {
    return search(input, start, input.length());
  }

  public boolean search(char[] input, int start, int length) {
    return search((Object)input, start, length);
  }

  /**
   * Search for match using the instance variable pmInput.
   */
  boolean search(Object input, int start, int length) {
    offset = 0;   // 
    int flags = 0;
    if(input instanceof char[]) {
      // if specified length doesn't match length of array then
      // copy it. NEEDSWORK: the copy is too bad.
      char[] in = (char[])input;
      if(in.length != length) {
	offset = start;
	start = 0;
	if(offset > 0) {
	  // include a character before the first char
	  // so that begin of line tests can work.
	  offset--;
	  start++;
	  length++; // for new array and arraycopy
	}
        in = new char[length];
	System.arraycopy(input, offset, in, 0, length);
	input = in;
      }
      if(start > 0 && in[start - 1] == '\n') {
	flags = RE.REG_ANCHORINDEX;
      }
    } else {
      // a String
      String s = (String)input;
      if(start > 0 && s.charAt(start-1) == '\n') {
	flags = RE.REG_ANCHORINDEX;
      }
    }
    result = matcher.getMatch(input, start, flags);
    matched = result != null;
    nGroup = 0;
    if(matched) {
      // figure out how many subexpressions
      for(int i = matcher.getNumSubs(); i > 0; i--) {
        if(result.getSubStartIndex(i) != -1) {
          nGroup = i;
          break;
        }
      }
    }
    return matched;
  }

  /**
   * Returns a result object for the last call that used this
   * RegExp for searching an input, for example <i>match</i>.
   * Further calls to match do not change the RegExpResult
   * that is returned.
   * @return The results of the match or null if the match failed.
   * @see RegExpResult
   */
  public RegExpResult getResult() {
    return new RegExpResultGNU(this);
  }

  /**
   * Return the number of backreferences.
   * @see RegExpResult#nGroup()
   */
  public int nGroup() {
    if(!matched) {
      return 0;
    }
    return nGroup;
  }

  /**
   * @see RegExpResult#group(int i)
   */
  public String group(int i) {
    if(!matched) {
      return null;
    }
    return result.toString(i);
  }

  /**
   * @see RegExpResult#length(int)
   */
  public int length(int i) {
    if(!matched || i > nGroup || result.getSubEndIndex(i) < 0) {
      return -1;
    }
    return result.getSubEndIndex(i) - result.getSubStartIndex(i);
  }

  /**
   *
   */
  public int start(int i) {
    if(!matched) {
      return -1;
    }
    return offset + result.getSubStartIndex(i);
  }

  /**
   *
   */
  public int stop(int i) {
    if(!matched) {
      return -1;
    }
    return offset + result.getSubEndIndex(i);
  }

  /**
   *
   */
  // public String[] split(String input);
}
