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

// Add starting and ending offsets of matches
// Is this from beginning of input or beginning of match
// Add isMatch() ?????

/**
 * This class hold the results of a regular expression pattern against
 * an input.
 */
public class RegExpResultStevesoft extends RegExpResult {
  RegRes result;

  RegExpResultStevesoft(RegExpStevesoft regexp) {
    this.result = regexp.regex.result();
  }

  /**
   *
   */
  public boolean isMatch() {
    return result.didMatch();
  }

  /**
   * Return the number of backreferences.
   */
  public int nGroup() {
    return result.numSubs();
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
    if(i == 0) {
      return result.stringMatched();
    } else {
      return result.stringMatched(i);
    }
  }

  /**
   * @see RegExpResult#length(int)
   */
  public int length(int i) {
    if(i == 0) {
      return result.charsMatched();
    } else {
      return result.charsMatched(i);
    }
  }

  /**
   *
   */
  public int start(int i) {
    if(i == 0) {
      return result.matchedFrom();
    } else {
      return result.matchedFrom(i);
    }
  }

  /**
   *
   */
  public int stop(int i) {
    if(i == 0) {
      return result.matchedTo();
    } else {
      return result.matchedTo(i);
    }
  }
}
