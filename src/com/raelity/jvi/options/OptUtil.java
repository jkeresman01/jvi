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

import java.awt.Color;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.Options.Category;
import com.raelity.jvi.manager.ViManager;

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

        prefs = ViManager.getFactory().getPreferences();

        prefs.addPreferenceChangeListener(new PreferenceChangeListener()
        {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt)
            {
                Option opt = Options.getOption(evt.getKey());
                if (opt != null) {
                    if (evt.getNewValue() != null) {
                        opt.preferenceChange(evt.getNewValue());
                    }
                }
                Options.optionChangeFixup(opt, evt);
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
                                                Validator<String> valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    if(valid == null)
      valid = new Option.DefaultStringValidator();
    StringOption opt = new StringOption(name, defaultValue, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  static public EnumOption<String> createEnumStringOption(String name,
                                                 String defaultValue,
                                                 String [] availableValues) {
    return createEnumStringOption(name, defaultValue, null, availableValues);
  }

  static public EnumOption<String> createEnumStringOption(String name,
                                                String defaultValue,
                                                Validator<String> valid,
                                                String [] availableValues) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    if(valid == null)
      valid = new EnumOption.DefaultEnumValidator<String>();
    EnumOption<String> opt = new EnumOption<String>(name, defaultValue,
            valid, availableValues);
    optionsMap.put(name, opt);
    return opt;
  }

  static public DebugOption createBootDebugOption(boolean v) {
    return new BootDebugOption(v);
  }

  static public DebugOption createDebugOption(String name) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    DebugOption opt = new ConcreteDebugOption(name);
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
                                                  Validator<Integer> valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    if(valid == null)
      valid = new Option.DefaultIntegerValidator();
    IntegerOption opt = new IntegerOption(name, defaultValue, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  static public EnumOption<Integer> createEnumIntegerOption(String name,
                                                 int defaultValue,
                                                 Integer [] availableValues) {
    return createEnumIntegerOption(name, defaultValue, null, availableValues);
  }

  static public EnumOption<Integer> createEnumIntegerOption(String name,
                                                int defaultValue,
                                                Validator<Integer> valid,
                                                Integer [] availableValues) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    if(valid == null)
      valid = new EnumOption.DefaultEnumValidator<Integer>();
    EnumOption<Integer> opt = new EnumOption<Integer>(name, defaultValue,
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
                                              Validator<Color> valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    if(valid == null)
      valid = new ColorOption.DefaultColorValidator();
    ColorOption opt = new ColorOption(name, defaultValue, permitNull, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  static public EnumSetOption createEnumSetOption(String name,
                                                  EnumSet defaultValue,
                                                  Class enumType,
                                                  Validator<EnumSet> valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    @SuppressWarnings("unchecked")
    EnumSetOption opt = new EnumSetOption((Class<EnumSet>)defaultValue.getClass(),
                                          enumType,
                                          name, defaultValue, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  public static Option getOption(String name) {
    return optionsMap.get(name);
  }

  public static Collection<Option> getOptions() {
    return Collections.unmodifiableCollection(optionsMap.values());
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

  public static void setupOptionDesc(String name,
                                     String displayName, String desc) {
      setupOptionDesc(null, name, displayName, desc);
  }

  public static void setupOptionDesc(Category category, String name,
                                      String displayName, String desc) {
    List<String> optionsGroup = null;
    if(category != null)
        optionsGroup = getWritableOptionList(category);
    Option opt = optionsMap.get(name);
    if(opt != null) {
      opt.setCategory(category);
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

  static void intializeGlobalOptionMemoryValue(Option opt)
  {
    VimOption vopt = VimOption.get(opt.getName());
    if(vopt == null || !vopt.isGlobal())
      return;
    try {
      Field f = G.class.getDeclaredField(vopt.getVarName());
      f.setAccessible(true);
      if(f.getType() == int.class)
        f.setInt(null, opt.getInteger());
      else if(f.getType() == boolean.class)
        f.setBoolean(null, opt.getBoolean());
      else if(f.getType() == String.class)
        f.set(null, opt.getString());
      else if(f.getType() == Color.class)
        f.set(null, opt.getColor());
      else
        throw new IllegalArgumentException("option " + opt.getName());
      if(   G.dbgOptions().getBoolean())
            G.dbgOptions().printf("Init G.%s to '%s'\n", vopt.getVarName(), opt.getValue());
    } catch(IllegalArgumentException ex) {
      Logger.getLogger(OptUtil.class.getName()).log(Level.SEVERE, null, ex);
    } catch(IllegalAccessException ex) {
      Logger.getLogger(OptUtil.class.getName()).log(Level.SEVERE, null, ex);
    } catch(NoSuchFieldException ex) {
      Logger.getLogger(OptUtil.class.getName()).log(Level.SEVERE, null, ex);
    } catch(SecurityException ex) {
      Logger.getLogger(OptUtil.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public static void verifyVimOptions() {
    for(VimOption vopt : VimOption.getAll()) {
      if(!vopt.isGlobal())
        continue;
      try {
        // The option should exist
        // G should have something with the var name
        // and the types should match
        Option opt = OptUtil.getOption(vopt.getOptName());
        Field f = G.class.getDeclaredField(vopt.getVarName());
        if(f.getType() == int.class)
          opt.getInteger();
        else if(f.getType() == boolean.class)
          opt.getBoolean();
        else if(f.getType() == String.class)
          opt.getString();
        if(     G.dbgOptions().getBoolean())
                G.dbgOptions().println("VERIFY: " + vopt.getOptName());
      } catch(NoSuchFieldException ex) {
        Logger.getLogger(VimOption.class.getName()).log(Level.SEVERE, null,
                                                                      ex);
      } catch(SecurityException ex) {
        Logger.getLogger(VimOption.class.getName()).log(Level.SEVERE, null,
                                                                      ex);
      }
    }
  }

  private OptUtil()
  {
  }
}

// vi: sw=2 et