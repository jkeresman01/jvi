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

package com.raelity.jvi.options;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.core.Options.Category;
import java.awt.Color;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * static methods to create options and populate option categories.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class OptUtil {

    private static boolean didInit = false;
    private static Preferences prefs;
    private static PropertyChangeSupport pcs;
    private static Map<String,Option> optionsMap = new HashMap<String,Option>();

    private static List<String> platformList = new ArrayList<String>();
    private static List<String> generalList = new ArrayList<String>();
    private static List<String> modifyList = new ArrayList<String>();
    private static List<String> searchList = new ArrayList<String>();
    private static List<String> cursorWrapList = new ArrayList<String>();
    private static List<String> processList = new ArrayList<String>();
    private static List<String> debugList = new ArrayList<String>();

    public static void init(PropertyChangeSupport pcs)
    {
        if (didInit) {
            return;
        }
        didInit = true;

        OptUtil.pcs = pcs;

        platformList.add("jViVersion"); // HACK - just doit

        prefs = ViManager.getViFactory().getPreferences();

        prefs.addPreferenceChangeListener(new PreferenceChangeListener() {

            public void preferenceChange(PreferenceChangeEvent evt)
            {
                Option opt = Options.getOption(evt.getKey());
                if (opt != null) {
                    if (evt.getNewValue() != null) {
                        opt.preferenceChange(evt.getNewValue());
                    }
                }
            }
        });
    }

    static Preferences getPrefs()
    {
        return prefs;
    }

  /** This should only be used from Option and its subclasses */
  static void firePropertyChange(String name, Object oldValue, Object newValue) {
    pcs.firePropertyChange(name, oldValue, newValue);
  }


  static public StringOption createStringOption(String name,
                                                 String defaultValue) {
    return createStringOption(name, defaultValue, null);
  }

  static public StringOption createStringOption(String name,
                                                String defaultValue,
                                                StringOption.Validator valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    StringOption opt = new StringOption(name, defaultValue, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  static public EnumStringOption createEnumStringOption(String name,
                                                 String defaultValue,
                                                 String [] availableValues) {
    return createEnumStringOption(name, defaultValue, null, availableValues);
  }

  static public EnumStringOption createEnumStringOption(String name,
                                                String defaultValue,
                                                StringOption.Validator valid,
                                                String [] availableValues) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    EnumStringOption opt = new EnumStringOption(name, defaultValue,
            valid, availableValues);
    optionsMap.put(name, opt);
    return opt;
  }

  static public BooleanOption createBooleanOption(String name,
                                                  boolean defaultValue) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    BooleanOption opt = new BooleanOption(name, defaultValue);
    optionsMap.put(name, opt);
    return opt;
  }

  static public IntegerOption createIntegerOption(String name,
                                                  int defaultValue) {
      return createIntegerOption(name, defaultValue, null);
  }

  static public IntegerOption createIntegerOption(String name,
                                                  int defaultValue,
                                                  IntegerOption.Validator valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    IntegerOption opt = new IntegerOption(name, defaultValue, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  static public EnumIntegerOption createEnumIntegerOption(String name,
                                                 int defaultValue,
                                                 Integer [] availableValues) {
    return createEnumIntegerOption(name, defaultValue, null, availableValues);
  }

  static public EnumIntegerOption createEnumIntegerOption(String name,
                                                int defaultValue,
                                                IntegerOption.Validator valid,
                                                Integer [] availableValues) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    EnumIntegerOption opt = new EnumIntegerOption(name, defaultValue,
            valid, availableValues);
    optionsMap.put(name, opt);
    return opt;
  }

  static public ColorOption createColorOption(String name,
                                              Color defaultValue,
                                              boolean permitNull) {
      return createColorOption(name, defaultValue, permitNull, null);
  }

  static public ColorOption createColorOption(String name,
                                              Color defaultValue,
                                              boolean permitNull,
                                              ColorOption.Validator valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    ColorOption opt = new ColorOption(name, defaultValue, permitNull, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  public static Option getOption(String name) {
    return optionsMap.get(name);
  }

  public static List<String> getOptionList(Category category) {
    List<String> catList = null;
    switch(category) {
      case PLATFORM:    catList = platformList; break;
      case GENERAL:     catList = generalList; break;
      case MODIFY:      catList = modifyList; break;
      case SEARCH:      catList = searchList; break;
      case CURSOR_WRAP: catList = cursorWrapList; break;
      case PROCESS:     catList = processList; break;
      case DEBUG:       catList = debugList; break;
    }
    return Collections.unmodifiableList(catList);
  }

  private static List<String> getWritableOptionList(Category category) {
    List<String> catList = null;
    switch(category) {
      case PLATFORM:    catList = platformList; break;
      case GENERAL:     catList = generalList; break;
      case MODIFY:      catList = modifyList; break;
      case SEARCH:      catList = searchList; break;
      case CURSOR_WRAP: catList = cursorWrapList; break;
      case PROCESS:     catList = processList; break;
      case DEBUG:       catList = debugList; break;
    }
    return catList;
  }

  public static void setupOptionDesc(Category category, String name,
                                     String displayName, String desc) {
      List<String> catList = getWritableOptionList(category);
      setupOptionDesc(catList, name, displayName, desc);
  }

  public static void setupOptionDesc(String name,
                                     String displayName, String desc) {
      setupOptionDesc((List<String>)null, name, displayName, desc);
  }

  private static void setupOptionDesc(List<String> optionsGroup, String name,
                                      String displayName, String desc) {
    Option opt = optionsMap.get(name);
    if(opt != null) {
      if(optionsGroup != null) {
          optionsGroup.add(name);
      }
      opt.setDesc(desc);
      opt.setDisplayName(displayName);
    } else {
      throw new Error("Unknown option: " + name);
    }
  }

  public static void setExpertHidden(String optionName,
                                      boolean fExpert, boolean fHidden) {
    Option opt = optionsMap.get(optionName);
    if(opt != null) {
      opt.setExpert(fExpert);
      opt.setHidden(fHidden);
    }
  }
}
