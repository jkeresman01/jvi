/*--- formatted by Jindent 2.1, (www.c-lab.de/~jindent) ---*/
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

import com.stevesoft.pat.*;
import com.stevesoft.pat.wrap.*;

// VERIFY NEW MATCH IS NON DESTRUCTIVE OF RESULT CLASS

/**
 * Implementation of RegExp using OROMatch from www.oroinc.com.
 */
public class RegExpStevesoft extends RegExp {
  // static String theClass = "com.stevesoft.pat.Regex";
  static String theClass = "com.stevesoft.pat.Key";
  Regex		regex;

  /**
   * Just see if the class is there. If it is enable
   * syntax error reporting.
   */
  public static boolean canInstantiate() {
    try {
      Class.forName(theClass);
      String key = System.getProperty("com.stevesoft.pat.Key");
      if(key != null) { Key.registeredTo(key); }

      // RegSyntaxError.RegSyntaxErrorEnabled = true;
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  public static String getDisplayName() {
    return "SteveSoft";
  }

  /**
   * The name of the regular expression class that this
   * class is adapting is returned.
   * @return The underlying regular expresion package.
   */
  public static String getAdaptedName() {
    return theClass;
  }

  /**
   * Create a regexp.
   */
  public RegExpStevesoft() {
    regex = new Regex();
  }

  /**
   * Prepare this regular expression to use <i>pattern</i>
   * for matching.
   */
  public void compile(String pattern, int compileFlags)
                throws RegExpPatternError
  {
    regex.esc = escape;
    try {
      regex.compile(pattern);
    } catch (RegSyntaxError e) {
      throw new RegExpPatternError(e);
    } catch (RegSyntax e) {
      throw new RegExpPatternError(e);
    }
    regex.esc = escape;
    if((compileFlags & RegExp.IGNORE_CASE) != 0) {
      regex.setIgnoreCase(true);
    }
  }

  /**
   * Search for match.
   * @return true if <i>input</i> matches this regular expression.
   */
  public boolean search(String input) {
    return matched = regex.search(input);
  }

  /**
   * Search for a match in string starting at char position <i>start</i>
   * @return true if <i>inut</i> matches this regular expression.
   */
  public boolean search(String input, int start) {
    if (start == 0) {
      return search(input);
    }

    matched = regex.searchFrom(input, start);

    return matched;
  }

  public boolean search(char[] input, int start, int length) {
    StringLike sl = new MyStringLike(input, start + length);
    matched = regex.searchFrom(sl, start);
    return matched;
  }

  /**
   * Returns a result object for the last call that used this
   * RegExp for searching an input.
   * Further calls to match do not change it.
   * @return The results of the match or null if the match failed.
   * @see AbstractRegExpResult
   */
  public RegExpResult getResult() {
    return new RegExpResultStevesoft(this);
  }

  /**
   * Return the number of backreferences.
   * @see RegExpResult#nGroup()
   */
  public int nGroup() {
    return regex.numSubs();
  }

  /**
   * Get the <i>i</i>th backreference.
   * @see RegExpResult#group(int)
   */
  public String group(int i) {
    if (i == 0) {
      return regex.stringMatched();
    } else {
      return regex.stringMatched(i);
    }
  }

  /**
   * @see RegExpResult#length(int)
   */
  public int length(int i) {
    if (i == 0) {
      return regex.charsMatched();
    } else {
      return regex.charsMatched(i);
    }
  }

  /**
   * @see RegExpResult#start(int)
   */
  public int start(int i) {
    if (i == 0) {
      return regex.matchedFrom();
    } else {
      return regex.matchedFrom(i);
    }
  }

  /**
   * @see RegExpResult#stop(int)
   */
  public int stop(int i) {
    if (i == 0) {
      return regex.matchedTo();
    } else {
      return regex.matchedTo(i);
    }
  }

  /**
   *
   */

  // abstract String[] split(String input);

  static class MyStringLike extends CharArrayWrap {
    int length;

    MyStringLike(char[] input, int length) {
      super(input);
      this.length = length;
    }

    public int length() {
      return length;
    }
  }

}

/* **************************************************************
class MyStringLike implements StringLike {
  char[] input;
  int length;

  MyStringLike(char[] input, int length) {
    this.input  = input;
    this.length = length;
  }

  public char charAt(int parm1) {
    //TODO: Implement this com.stevesoft.pat.StringLike method
  }

  public int indexOf(char parm1) {
    //TODO: Implement this com.stevesoft.pat.StringLike method
  }

  public int length() {
    //TODO: Implement this com.stevesoft.pat.StringLike method
  }

  public BasicStringBufferLike newStringBufferLike() {
    return new CharArrayBufferWrap();
  }

  public String substring(int parm1, int parm2) {
    //TODO: Implement this com.stevesoft.pat.StringLike method
  }

  public Object unwrap() {
    //TODO: Implement this com.stevesoft.pat.StringLike method
  }
}
**************************************************************/



/*--- formatting done in "Raelity Java Convention" style on 02-25-2000 ---*/

