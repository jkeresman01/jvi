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

public class EnumStringOption extends StringOption implements EnumOption {
    String [] availableValues;
  
  EnumStringOption(String key, String defaultValue, String[] availableValues) {
    this(key, defaultValue, null, availableValues);
  }
  
  EnumStringOption(String key, String defaultValue,
          StringOption.Validator validator,
          String[] availableValues) {
    super(key, defaultValue, validator);
    this.availableValues = availableValues;
  }

    public String[] getAvailableValues() {
        return availableValues;
    }

}
