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

package com.raelity.jvi.lib;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;

/**
 * This class hold the results of a regular expression pattern against
 * an input. It is useful for saving the result of <i>RegExp.search</i>
 * before performing another search. These same methods are available
 * in <i>RegExp</i>.
 * <p>This class is not a superclass of RegExp, even though
 * all the methods of this class appear in and have identical
 * function to methods in RegExp, because there could be negative
 * implications on performance depending on the regular expression
 * implementation.
 */
public class RegExpResult
{
private MatchResult result;
boolean matches;

public RegExpResult(Matcher m, boolean matches) {
    if(m == null) {
        return;
    }
    this.result = m.toMatchResult();
    this.matches = matches;
}

/**
 * Check if the associated search produced a match.
 * @return True if the input matched. Otherwise false.
 */
public boolean isMatch()
{
    return result != null && matches;
}

  /**
   * Return the number of backreferences.
   */
  public int groupCount()
  {
      return result.groupCount();
  }

/**
 * Retrive backreference (matching string in the input) for the <i>i</i>th
 * set of parenthesis in the pattern.
 * The backreference groups
 * are numbered starting with 1. If i == 0 then return the
 * part of the input string that matched the pattern.
 * @return The specified backreference or null if backreference
 * did not match.
 */
public String group(int i)
{
    if(!matches)
        return null;
    return result.group(i);
}

/**
 * The length of the of corresponding backreference. If i == 0
 * then the length of the entire match is returned.
 * @return The length of the specified backreference.
 */
public int length(int i)
{
    if(!matches || i > groupCount() || result.end(i) < 0)
        return -1;
    return result.end(i) - result.start(i);
}

/**
 * The returned value is the offset from the beginning of the
 * input to where the <i>i</i>th backreference starts.
 * If i == 0 then the value is the offset in the input where
 * entire match starts.
 */
public int start(int i) {
    if(!matches)
        return -1;
    return result.start(i);
}

/**
 * The returned value is the offset from the beginning of the
 * input to where the <i>i</i>th backreference ends.
 * If i == 0 then the value is the offset in the input where
 * entire match ends.
 */
public int end(int i) {
    if(!matches)
        return -1;
    return result.end(i);
}
}
