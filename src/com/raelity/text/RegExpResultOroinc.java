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

import com.oroinc.text.regex.*;

/**
 * This class hold the results of a regular expression pattern against
 * an input.
 */
public class RegExpResultOroinc extends RegExpResult {
  MatchResult result;
  boolean     matched;

  /**
   * Constructor declaration
   *
   *
   * @param regexp
   *
   * @see
   */
  public RegExpResultOroinc(RegExpOroinc regexp) {
    this.result = regexp.result;
    this.matched = regexp.matched;
  }

  /**
   *
   */
  public boolean isMatch() {
    return matched;
  }

  /**
   * Return the number of backreferences.
   */
  public int nGroup() {
    if (!matched) {
      return 0;
    }

    return result.groups() - 1;
  }

  /**
   * Retrive backreference (matching string in the input) for the <i>i</i>th
   * set of parenthesis in the pattern.
   * When i == 0 the entire matched string is returned.
   *
   * @return A string containing the indicated input substring
   * or null if the indicated backreference did not match. Note
   * that null is different from the empty string.
   */
  public String group(int i) {
    if (!matched) {
      return null;
    }

    return result.group(i);
  }

  /**
   *
   */
  public int length(int i) {
    String s = group(i);

    if (s == null) {
      return -1;
    } else {
      return s.length();
    }
  }

  /**
   *
   */
  public int start(int i) {
    if (!matched) {
      return -1;
    }

    return result.beginOffset(i);
  }

  /**
   *
   */
  public int stop(int i) {
    if (!matched) {
      return -1;
    }

    return result.endOffset(i);
  }

}



/*--- formatting done in "Raelity Java Convention" style on 02-25-2000 ---*/

