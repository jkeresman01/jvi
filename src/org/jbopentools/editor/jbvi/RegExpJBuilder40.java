/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */
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
package org.jbopentools.editor.jbvi;

import com.raelity.text.RegExp;
import com.raelity.text.RegExpResult;
import com.raelity.text.RegExpPatternError;

import com.borland.primetime.editor.SearchManager;
import com.borland.primetime.editor.SearchOptions;
import com.borland.primetime.util.RegularExpression$MatchResult;
import com.borland.primetime.util.RegularExpression;

/**
 * Implementation of RegExp using JB's internal pattern matching.
 */
public class RegExpJBuilder40 extends RegExp {

  String pattern;  // The "compiled" pattern
  boolean ignoreCase;  // The "compiled" pattern
  RegularExpression$MatchResult result;

  public RegExpJBuilder40() {
  }

  public static int patternType() {
    return PATTERN_SIMPLE;
  }

  static String theClass = "com.borland.primetime.editor.SearchManager";

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
    return "JBuilder";
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
   * Prepare this regular expression to use <i>pattern</i>
   * for matching. Strings beginning with "(e?=x)" are not
   * special.
   */
  public void compile(String patternString, int compileFlags)
	    throws RegExpPatternError
  {
    pattern = patternString;
    ignoreCase = (compileFlags & IGNORE_CASE) != 0;
    result = null;
  }

  /**
   * Search for match.
   * @return true if <i>input</i> matches this regular expression.
   */
  public boolean search(String input) {
    throw new UnsupportedOperationException();
  }

  /**
   * Search for a match in string starting at char position <i>start</i>.
   * @return true if <i>input</i> matches this regular expression.
   */
  public boolean search(String input, int start) {
    throw new UnsupportedOperationException();
  }

  public boolean search(char input[], int start, int length) {
    SearchOptions options = SearchManager.getSavedOptions();
    options.setCaseSensitive( ! ignoreCase);
    options.setSearchText(pattern);
    options.setForwardSearch(true);
    result = SearchManager.search(input, options, start, start + length);
    matched = result != RegularExpression.NO_MATCH;
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
    throw new IllegalArgumentException();
    // return new RegExpResultJBuilder40(this);
  }

  /**
   * Return the number of backreferences.
   * @see RegExpResult#nGroup()
   */
  public int nGroup() {
    if(!matched) {
      return 0;
    }
    return 0;
  }

  /**
   * @see RegExpResult#group(int i)
   */
  public String group(int i) {
    throw new UnsupportedOperationException();
  }

  /**
   * @see RegExpResult#length(int)
   */
  public int length(int i) {
    checkGroup(i);
    if(!matched) {
      return -1;
    }
    return result.getLength();
  }

  /**
   *
   */
  public int start(int i) {
    checkGroup(i);
    if(!matched) {
      return -1;
    }
    return result.getStartIndex();
  }

  /**
   *
   */
  public int stop(int i) {
    checkGroup(i);
    if(!matched) {
      return -1;
    }
    return result.getStartIndex() + result.getLength();
  }

  private void checkGroup(int i) {
    if(i != 0) {
      throw new IllegalArgumentException("Only group 0 allowed, not " + i);
    }
  }

  /**
   *
   */
  // public String[] split(String input);
}
