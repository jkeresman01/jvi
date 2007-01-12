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
package com.raelity.jvi;

public class StringOption extends Option {
  StringOption(String key, String defaultValue) {
    super(key, defaultValue);
  }

  public final String getString() {
    return stringValue;
  }

  /**
   * Set the value of the parameter.
   * @return true if value actually changed.
   */
  public boolean setString(String newValue) {
    boolean rc = ! newValue.equals(stringValue);
    stringValue = newValue;
    propogate();
    return rc;
  }

  /**
   * Set the value as a string.
   */
  public boolean setValue(String newValue) {
    return setString(newValue);
  }
}
