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

import com.borland.primetime.properties.PropertyManager;
import com.borland.primetime.properties.PropertyPageFactory;
import com.borland.primetime.properties.PropertyPage;
import com.borland.primetime.properties.PropertyGroup;
import com.borland.primetime.properties.GlobalBooleanProperty;
import com.borland.primetime.properties.GlobalIntegerProperty;
import com.borland.primetime.properties.GlobalProperty;
import com.borland.primetime.editor.EditorManager;

import com.raelity.jvi.G;
import com.raelity.jvi.Options;
import com.raelity.jvi.BooleanOption;
import com.raelity.jvi.StringOption;

public class ViNumericPropertyGroup implements PropertyGroup {

  public static void initOpenTool(byte majorVersion, byte minorVersion) {
    PropertyManager.registerPropertyGroup(new ViNumericPropertyGroup());
  }

  public static final String PAGE_NAME = "  Vi . . .  ";
  public static final String CATEGORY = "Vi.Editor";

  /**
   * The properties for this group as seen by the user. Note
   * that they are munged a bit to get the vi internal properties.
   */
  // NEEDSWORK: the default values should come from vi proper
  public static final GlobalBooleanProperty BACKSPACE1 =
    new GlobalBooleanProperty(CATEGORY, "backspace1", false);
  public static final GlobalBooleanProperty BACKSPACE2 =
    new GlobalBooleanProperty(CATEGORY, "backspace2", false);

  public static final GlobalBooleanProperty REPORT_ENBL =
    new GlobalBooleanProperty(CATEGORY, "report_enbl", true);
  public static final GlobalIntegerProperty REPORT =
    new GlobalIntegerProperty(CATEGORY, "report", 3);

  public static final GlobalBooleanProperty SCROLLOFF_ENBL =
    new GlobalBooleanProperty(CATEGORY, "scrolloff_enbl", false);
  public static final GlobalIntegerProperty SCROLLOFF =
    new GlobalIntegerProperty(CATEGORY, "scrolloff", 3);

  // The initial value is the default value
  public static final GlobalBooleanProperty META_EQUALS =
    new GlobalBooleanProperty(CATEGORY, Options.metaEqualsOption,
                              G.p_meta_equals.getBoolean());
  public static final GlobalProperty META_ESCAPE =
    new GlobalProperty(CATEGORY, Options.metaEscapeOption,
                       G.p_meta_escape.getString());

  static ViNumericPropertyGroup instance;

  private ViNumericPropertyGroup() {
    instance = this;
  }

  static ViNumericPropertyGroup getInstance() {
    return instance;
  }

  public void initializeProperties() {
    if(BACKSPACE2.getBoolean()) {
      G.p_bs = 2;
    } else if(BACKSPACE1.getBoolean()) {
      G.p_bs = 1;
    } else {
      G.p_bs = 0;
    }

    if(REPORT_ENBL.getBoolean()) {
      G.p_report = REPORT.getInteger();
    } else {
      G.p_report = 30000;
    }

    if(SCROLLOFF_ENBL.getBoolean()) {
      G.p_so = SCROLLOFF.getInteger();
    } else {
      G.p_so = 0;
    }

    G.p_meta_equals.setBoolean(META_EQUALS.getBoolean());
    G.p_meta_escape.setString(META_ESCAPE.getValue());
  }

  public PropertyPageFactory getPageFactory(Object topic) {
    Object editor_topic;
    if(JBOT.is40()) {
      editor_topic = "Editor";
    } else {
      editor_topic = EditorManager.EDITOR_TOPIC;
    }
    if(JBOT.is40() && topic == null || topic == editor_topic) {
      // System.err.println("getPageFactory: " + topic);
      return new PropertyPageFactory(PAGE_NAME,
                               "Vi numeric configuration settings") {
        public PropertyPage createPropertyPage() {
          return new NumericOptions();
        }
      };
    }
    return null;
  }
}
