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

// NEEDSWORK: could allow comparable to Integer as well

public class MutableInt implements Comparable<MutableInt> {
  private int value;

  public MutableInt() { setValue(0); }
  public MutableInt(int value) { setValue(value); }

  final public int getValue() { return value; }
  final public void setValue(int value) { this.value = value; }

  final public void setBits(int mask) { value |= mask; }
  final public void clearBits(int mask) { value &= ~mask; }
  final public boolean testAnyBits(int mask) { return (value & mask) != 0; }
  final public boolean testAllBits(int mask) { return (value & mask) == mask; }

    @Override
  public int compareTo(MutableInt o) {
    return value - o.value;
  }
    @Override
  public String toString() {
      return "" + value;
  }
}
