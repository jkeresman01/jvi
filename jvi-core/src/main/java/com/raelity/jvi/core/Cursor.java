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
package com.raelity.jvi.core;

import com.raelity.jvi.ViCaretStyle;

class Cursor implements ViCaretStyle {
  int shape;
  int percentage;
  int blinkwait;
  int blinkon;
  int blinkoff;
  int highlightId;
  int matchBraceOffset;

  Cursor(int shape, int percentage, int matchBraceOffset) {
    // this(shape, percentage, 700, 400, 250, 0);
    this(shape, percentage, matchBraceOffset, 700, 500, 500, 0);
  }

  private Cursor(int shape, int percentage, int matchBraceOffset,
                 int blinkwait, int blinkon, int blinkoff, int highlightId)
  {
    this.shape = shape;
    this.percentage = percentage;
    this.matchBraceOffset = matchBraceOffset;
    this.blinkwait = blinkwait;
    this.blinkon = blinkon;
    this.blinkoff = blinkoff;
    this.highlightId = highlightId;
  }

  /** one of the SHAPE_ defined */
    @Override
  public int getShape() {
    return shape;
  }

    @Override
  public char getEditPutchar() {
      return G.editPutchar;
  }

  /** percentage of cell for bar */
    @Override
  public int getPercentage() {
    return percentage;
  }
  
  /** match brace offset */
    @Override
  public int getMatchBraceOffset() {
      return matchBraceOffset;
  }

  /** blinking, wait time before blinking starts */
    @Override
  public int getBlinkwait() {
    return blinkwait;
  }

  /** blinking, on time */
    @Override
  public int getBlinkon() {
    return blinkon;
  }

  /** blinking, off time */
    @Override
  public int getBlinkoff() {
    return blinkoff;
  }

  /** highlight group ID */
    @Override
  public int getHighlightId() {
    return highlightId;
  }

    @Override
  public boolean equals(Object o) {
    if( ! (o instanceof Cursor)) {
      return false;
    }
    Cursor c = (Cursor)o;
    return       shape == c.shape
	      && percentage == c.percentage
	      && blinkwait == c.blinkwait
	      && blinkon == c.blinkon
	      && blinkoff == c.blinkoff
	      && highlightId == c.highlightId;
  }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.shape;
        hash = 37 * hash + this.percentage;
        hash = 37 * hash + this.blinkwait;
        hash = 37 * hash + this.blinkon;
        hash = 37 * hash + this.blinkoff;
        hash = 37 * hash + this.highlightId;
        return hash;
    }
}
