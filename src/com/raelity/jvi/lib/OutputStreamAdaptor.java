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

import com.raelity.jvi.ViOutputStream;

/**
 * Use this class instead of the interface, to make it easier to
 * augment the interface in a compatible way.
 */
public class OutputStreamAdaptor implements ViOutputStream {

  public OutputStreamAdaptor() {
  }

    @Override
  public void println(int line, int col, int length) {
  }

    @Override
  public void println(String s) {
  }

    @Override
  public void printlnLink(String link, String text) {
  }

    @Override
  public void close() {
  }
}