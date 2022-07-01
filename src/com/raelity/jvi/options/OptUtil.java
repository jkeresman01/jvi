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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionHandler;

import com.raelity.jvi.core.*;
import com.raelity.jvi.core.Options.Category;
import com.raelity.jvi.manager.*;

import static java.util.logging.Level.*;

import static com.raelity.jvi.manager.ViManager.runInDispatch;
import static com.raelity.text.TextUtil.sf;

/**
 * static methods to create options and populate option categories.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class OptUtil {
    private static boolean didInit = false;
    private static boolean started = false;
    private static Preferences prefs;
    private static final Map<String,Option<?>> optionsMap = new HashMap<>();
    /** use this to avoid firing event while holding Option synchro lock. */
    private static final Queue<Runnable> toRun = new ConcurrentLinkedQueue<>();

    private static final Map<Category, List<String>> categoryLists
            = new EnumMap<>(Category.class);

    public static void init()
    {
        if (didInit) {
            return;
        }
        didInit = true;

        // HACK - just doit
        getWritableOptionList(Category.PLATFORM).add("jViVersion");

        prefs = ViManager.getFactory().getPreferences();

        // Pref change updates options; do it on EDT.
        prefs.addPreferenceChangeListener((PreferenceChangeEvent evt) -> {
          Option<?> opt = Options.getOption(evt.getKey());
          if (opt != null) {
            if (evt.getNewValue() != null) {
              G.dbgOptions().printf(FINE, () ->
                      sf("Option: pref change: %s %s\n", evt.getKey(), evt.getNewValue()));
              opt.preferenceChange(evt.getNewValue());
            }
          }
        });
    }

    public static void go()
    {
      started = true;
    }

    static Preferences getPrefs()
    {
        return prefs;
    }

  public static void firePropertyChange(OptionChangeEvent ev)
  {
    getEventBus().post(ev);
  }

  static void qPut(Runnable r)
  {
    if(started && r != null)
      toRun.offer(r);
  }

  static void qPut(OptionChangeEvent ev)
  {
    if(started && ev != null)
      qPut(() -> firePropertyChange(ev));
  }

  static void qRun()
  {
    Runnable r;
    while(toRun.peek() != null) {
      synchronized(toRun) {
        r = toRun.poll();
        if(r != null)
          runInDispatch(false, r);
      }
    }
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
      valid = new EnumOption.DefaultEnumValidator<>();
    EnumOption<String> opt = new EnumOption<>(name, defaultValue,
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

  static public BooleanOption createBooleanOption(String name, boolean defaultValue)
  {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    BooleanOption opt = new BooleanOption(name, defaultValue);
    optionsMap.put(name, opt);
    return opt;
  }

  static public IntegerOption createIntegerOption(String name, int defaultValue) {
      return createIntegerOption(name, defaultValue, null);
  }

  static public IntegerOption createIntegerOption(
          String name, int defaultValue, Validator<Integer> valid)
  {
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
      valid = new EnumOption.DefaultEnumValidator<>();
    EnumOption<Integer> opt = new EnumOption<>(name, defaultValue,
            valid, availableValues);
    optionsMap.put(name, opt);
    return opt;
  }

  static public ColorOption createColorOption(String name,
                                              Color defaultValue,
                                              boolean permitNull,
                                              boolean initNull) {
      ColorOption opt = createColorOption(
              name, defaultValue, permitNull, initNull, null);
      // only initNull if hasn't been set in preferences
      // if(initNull && getPrefs().get(name, "xyzzy").equals("xyzzy"))
      return opt;
  }

  static public ColorOption createColorOption(String name,
                                              Color defaultValue,
                                              boolean permitNull,
                                              boolean initNull,
                                              Validator<Color> valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    if(initNull && OptUtil.getPrefs().get(name, "xyzzy").equals("xyzzy")) {
      OptUtil.getPrefs().put(name, "null");
    }
    if(valid == null)
      valid = new ColorOption.DefaultColorValidator();
    ColorOption opt = new ColorOption(name, defaultValue, permitNull, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  static public EnumSetOption<?> createEnumSetOption(String name,
                                                  EnumSet<?> defaultValue,
                                                  Class<?> enumType,
                                                  Validator<EnumSet<?>> valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    @SuppressWarnings("unchecked")
    EnumSetOption<?> opt = new EnumSetOption((Class<EnumSet>)defaultValue.getClass(),
                                          enumType,
                                          name, defaultValue, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  public static Option<?> getOption(String name) {
    return optionsMap.get(name);
  }

  public static Collection<Option<?>> getOptions() {
    return Collections.unmodifiableCollection(optionsMap.values());
  }

  public static List<String> getOptionList(Category category) {
    return Collections.unmodifiableList(getWritableOptionList(category));
  }

  private static List<String> getWritableOptionList(Category category) {
    List<String> catList = categoryLists.get(category);
    if(catList == null) {
      catList = category != Category.NONE
                ? new ArrayList<>() : Collections.emptyList();
      categoryLists.put(category, catList);
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
    Option<?> opt = optionsMap.get(name);
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

  public static void setupPlatformDesc(String name, String desc) {
    Option<?> opt = optionsMap.get(name);
    if(opt != null) {
      opt.setDescPlatform(desc);
    } else {
      throw new Error("Unknown option: " + name);
    }
  }

  public static void setExpertHidden(String optionName,
                                      boolean fExpert, boolean fHidden) {
    Option<?> opt = optionsMap.get(optionName);
    if(opt != null) {
      opt.setExpert(fExpert);
      opt.setHidden(fHidden);
    }
  }

  static OptionChangeEvent intializeGlobalOptionMemoryValue(Option<?> opt)
  {
    VimOption vopt = VimOption.get(opt.getName());
    if(vopt == null || !vopt.isGlobal())
      return null;
    try {
      Field f = G.class.getDeclaredField(vopt.getVarName());
      f.setAccessible(true);
      Object oldValue = f.get(null);
      if(f.getType() == int.class
              || f.getType() == boolean.class
              || f.getType() == String.class
              || f.getType() == Color.class
              || f.getType() == EnumSet.class)
        f.set(null, opt.getValue());
      else
        throw new IllegalArgumentException("option " + opt.getName());

      G.dbgOptions().printf(() ->
              sf("Init G.%s to '%s'\n", vopt.getVarName(), opt.getValue()));
      return new OptionChangeGlobalEvent(opt.getName(), oldValue, opt.getValue());
    } catch(IllegalArgumentException | IllegalAccessException
            | NoSuchFieldException | SecurityException ex) {
      Logger.getLogger(OptUtil.class.getName()).log(SEVERE, null, ex);
    }
    return null;
  }

  public static void verifyVimOptions() {
    for(VimOption vopt : VimOption.getAll()) {
      if(!vopt.isGlobal())
        continue;
      try {
        // The option should exist
        // G should have something with the var name
        // and the types should match
        Option<?> opt = OptUtil.getOption(vopt.getOptName());
        G.dbgOptions().println(() -> "VERIFY: " + vopt.getOptName());
        Field f = G.class.getDeclaredField(vopt.getVarName());
        if(f.getType() == int.class)
          opt.getInteger();
        else if(f.getType() == boolean.class)
          opt.getBoolean();
        else if(f.getType() == String.class)
          opt.getString();
      } catch(NoSuchFieldException | SecurityException ex) {
        Logger.getLogger(VimOption.class.getName()).log(SEVERE, null, ex);
      }
    }
  }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static void checkAllForSetCommand() {
    Set<String> hasSetCommand = new HashSet<>();
    for(VimOption vopt : VimOption.getAll()) {
      hasSetCommand.add(vopt.getOptName());
    }
    System.err.println("Checking for options without set command");
    for(Option<?> opt : getOptions()) {
      if(opt.getName().startsWith("viDbg"))
        continue;
      if(!hasSetCommand.contains(opt.getName())) {
        System.err.println("    Option " + opt.getName() + ": NO SET COMMAND"
                           + (opt.isHidden() ? " (HIDDEN)" : ""));
      }
    }
  }

  private OptUtil()
  {
  }

    /** This class collects potential changes from the options dialog;
     * the OK/Apply buttons typically commit the changes.
     */
    static public class OptionChangeHandler
    {

      private static class Change
      {
      private Change(Object oldVal)
      {
        this.oldVal = oldVal;
      }
      
      private final Object oldVal;
      private Object newVal;
      }

    private final Map<String, Change> map = new HashMap<>();
    
    OptionChangeHandler()
    {
    }
    
    /** Forget/cancel pending changes. */
    void clear()
    {
      map.clear();
    }
    
    /** Proposed change from dialog. */
    void changeOption(String name, Object oldVal, Object newVal)
    {
      Change ch = map.computeIfAbsent(name, (k) -> new Change(oldVal));
      ch.newVal = newVal;
    }

    /**
     * The pending changes are accepted, change the options,
     * propagate to preferences. Clear pending changes.
     */
    void applyChanges()
    {
      for(Entry<String, Change> entry : map.entrySet()) {
        String key = entry.getKey();
        Change ch = entry.getValue();

        Option<?> opt = Options.getOption(key);
        opt.castSetValue(ch.newVal);
        
        // This is probably not used
        getEventBus().post(new OptionChangeDialogEvent(key, ch.oldVal, ch.newVal));
      }
      clear();
    }
    } // END CLASS OptionChangeHandler

  ////////////////////////////////////////////////////////////////////////
  //
  // Events about Options
  //

    private static class ReportPostEventBus extends EventBus
    {
    private final Consumer<Object> reportPost;
    
    public ReportPostEventBus(SubscriberExceptionHandler exceptionHandler,
                              Consumer<Object> reportPost)
    {
      super(exceptionHandler);
      this.reportPost = reportPost;
    }
    
    @Override
    public void post(Object ev)
    {
      if(!started)
        return;
      reportPost.accept(ev);
      runInDispatch(false, () -> super.post(ev));
    }
    } // END CLASS ReportPostEventBus

  private static EventBus bus;
  /** All events on this bus are dispatched on the EDT. */
  public static EventBus getEventBus()
  {
    if(bus == null)
      bus = new ReportPostEventBus(
              new ViEvent.ExHandler("OptionEvent: handleException:"),
              (Object ev) -> G.dbgOptions().printf(CONFIG, () ->
                      sf("FIRE:Option: %s\n", ev.toString())));
    return bus;
  }

  // TODO: event for buf/win local? See SetColonCommand

    /** not used if pcs.fire not used.
     * Maybe should call it apply/Change/prefs or something; see applyChanges. */
    static public class OptionChangeDialogEvent extends AbstractOptionChangeEvent
    {
    public OptionChangeDialogEvent(String name, Object oldValue, Object newValue)
    {
      super(name, oldValue, newValue);
    }
    } // END CLASS OptionChangeDialogEvent

    /** option is changed in memory, G.xxx.
     * <p>
     * ?????????????????????
     * What about non global options, win/buf, see SetColonCommand.
     * 
     */
    static public class OptionChangeGlobalEvent extends AbstractOptionChangeEvent
    {
    public OptionChangeGlobalEvent(String name, Object oldValue, Object newValue)
    {
      super(name, oldValue, newValue);
    }
    } // END CLASS OptionChangeSetEvent

    /** {@literal Option<>} changed value. */
    static public class OptionChangeOptionEvent extends AbstractOptionChangeEvent
    {
    public OptionChangeOptionEvent(String name, Object oldValue, Object newValue)
    {
      super(name, oldValue, newValue);
    }
    } // END CLASS OptionChangeSetEvent

    static public class OptionsInitializedEvent implements OptionChangeEvent
    {
    @Override
    public String toString()
    {
      return sf("Option{%s:}", this.getClass().getSimpleName());
    }
    } // END CLASS OptionsInitializedEvent 

    /** Tag for option related events. */
    static public interface OptionChangeEvent
    {
    }

    /** base class for option change events */
    public static class AbstractOptionChangeEvent implements OptionChangeEvent
    {
    private final String name;
    private final Object oldValue;
    private final Object newValue;
    
    public AbstractOptionChangeEvent(String name, Object oldValue, Object newValue)
    {
      this.name = name;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }
    
    public String getName()
    {
      return name;
    }
    
    public Object getOldValue()
    {
      return oldValue;
    }
    
    public Object getNewValue()
    {
      return newValue;
    }
    
    @Override
    public String toString()
    {
      //return sf("Option{%s: %s: new=%s}",
      //          this.getClass().getSimpleName(),
      //          name, newValue);
      return sf("Option{%s: %s: old=%s, new=%s}",
                this.getClass().getSimpleName(),
                name, oldValue, newValue);
    }
    
    } // END CLASS AbstractOptionChangeEvent
}

// vi: sw=2 et
