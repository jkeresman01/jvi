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

import com.oroinc.text.regex.*;

/**
 * Implementation of RegExp using OROMatch from www.oroinc.com.
 * @see RegExp#fixupEscape(char[] pattern, int offset, char escape)
 */

public class RegExpOroinc extends RegExp {
  static String theClass = "com.oroinc.text.regex.PatternCompiler";
  PatternMatcher matcher = new Perl5Matcher();
  PatternMatcherInput pmInput = new PatternMatcherInput("");
  PatternCompiler compiler = new Perl5Compiler();
  Pattern pattern;
  MatchResult result;

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
    return "OROMatcher";
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

  public RegExpOroinc() {
  }

  /**
   * Prepare this regular expression to use <i>pattern</i>
   * for matching. The string can begin with "(e?=x)" to
   * specify an escape character.
   */
  public void compile(String patternString, int compileFlags)
	    throws RegExpPatternError
  {
    int flags = 0;
    flags |= ((compileFlags & RegExp.IGNORE_CASE) != 0)
              ? Perl5Compiler.CASE_INSENSITIVE_MASK : 0;
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
      pattern = compiler.compile(patternString, flags);
    } catch(MalformedPatternException e) {
      throw new RegExpPatternError(e);
    }
    result = null;
  }

  /**
   * Search for match.
   * @return true if <i>input</i> matches this regular expression.
   */
  public boolean search(String input) {
    pmInput.setInput(input);
    return search();
  }

  /**
   * Search for a match in string starting at char position <i>start</i>.
   * @return true if <i>input</i> matches this regular expression.
   */
  public boolean search(String input, int start) {
    pmInput.setInput(input, start, input.length() - start);
    return search();
  }

  public boolean search(char input[], int start, int length) {
    pmInput.setInput(input, start, length);
    return search();
  }

  /**
   * Search for match using the instance variable pmInput.
   */
  boolean search() {
    matched = matcher.contains(pmInput, pattern);
    result = matcher.getMatch();
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
    return new RegExpResultOroinc(this);
  }

  /**
   * Return the number of backreferences.
   * @see RegExpResult#nGroup()
   */
  public int nGroup() {
    if(!matched) {
      return 0;
    }
    return result.groups() - 1;
  }

  /**
   * @see RegExpResult#group(int i)
   */
  public String group(int i) {
    if(!matched) {
      return null;
    }
    return result.group(i);
  }

  /**
   * @see RegExpResult#length(int)
   */
  public int length(int i) {
    if(!matched || i > result.groups() || result.endOffset(i) < 0) {
      return -1;
    }
    return result.endOffset(i) - result.beginOffset(i);
  }

  /**
   *
   */
  public int start(int i) {
    if(!matched) {
      return -1;
    }
    return result.beginOffset(i);
  }

  /**
   *
   */
  public int stop(int i) {
    if(!matched) {
      return -1;
    }
    return result.endOffset(i);
  }

  /**
   *
   */
  // public String[] split(String input);
}
