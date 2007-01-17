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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * Option handling from external sources.
 * <br>
 * Should there be a vi command to set the options to persistent storage,
 * this is useful if want to save after several set commands.
 * <br>
 *
 */

public class Options {
  public static final String commandEntryFrameOption =
  						"viCommandEntryFrameOption";
  public static final String backspaceWrapPreviousOption =
                                                "viBackspaceWrapPrevious";
  public static final String hWrapPreviousOption = "viHWrapPrevious";
  public static final String leftWrapPreviousOption = "viLeftWrapPrevious";
  public static final String spaceWrapNextOption = "viSpaceWrapNext";
  public static final String lWrapNextOption = "viLWrapNext";
  public static final String rightWrapNextOption = "viRightWrapNext";
  public static final String tildeWrapNextOption = "viTildeWrapNext";

  public static final String unnamedClipboardOption = "viUnnamedClipboard";
  public static final String joinSpacesOption = "viJoinSpaces";
  public static final String shiftRoundOption = "viShiftRound";
  public static final String notStartOfLineOption = "viNotStartOfLine";
  public static final String changeWordBlanksOption = "viChangeWordBlanks";
  public static final String tildeOperator = "viTildeOperator";
  public static final String searchFromEndOption = "viSearchFromEnd";
  public static final String wrapScanOption = "viWrapScan";

  public static final String metaEqualsOption = "viMetaEquals";
  public static final String metaEscapeOption = "viMetaEscape";

  public static final String ignoreCaseOption = "viIgnoreCase";
  
  public static final String expandTabs = "viExpandTabs";

  public static final String report = "viReport";
  public static final String backspace = "viBackspace";
  public static final String scrollOff = "viScrollOff";
  public static final String shiftWidth = "viShiftWidth";
  public static final String tabStop = "viTabStop";

  
  public static final String readOnlyHackOption = "viReadOnlyHack";
  public static final String classicUndoOption = "viClassicUndo";
  public static final String dbgEditorActivation = "viDbgEditorActivation";

  
  private static Map options = new HashMap(); //<string, Option>
  private static List<String> optionsList = new ArrayList<String>();
  /*private static Set optionNames; //<String>
  private static Map optionDesc = new HashMap(); //<String,String>
  private static Map defaults = new HashMap(); //<String,String>
  */
  

  static {
    // init(); invoked from ViManager.setViFactory
  }

  static Preferences prefs;

  private static boolean didInit = false;
  public static void init() {
    if(didInit) {
      return;
    }

    prefs = ViManager.getViFactory().getPreferences();

    prefs.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
	Option opt = (Option)options.get(evt.getKey());
	if(opt != null) {
          if(evt.getNewValue() != null) {
              opt.preferenceChange(evt.getNewValue());
          }
	}
      }
    });

    G.p_ww_bs = createBooleanOption(backspaceWrapPreviousOption, true);
    G.p_ww_h = createBooleanOption(hWrapPreviousOption, false);
    G.p_ww_larrow = createBooleanOption(leftWrapPreviousOption, false);
    G.p_ww_sp = createBooleanOption(spaceWrapNextOption, true);
    G.p_ww_l = createBooleanOption(lWrapNextOption, false);
    G.p_ww_rarrow = createBooleanOption(rightWrapNextOption, false);
    G.p_ww_tilde = createBooleanOption(tildeWrapNextOption, false);

    G.p_cb = createBooleanOption(unnamedClipboardOption, false);
    G.p_js = createBooleanOption(joinSpacesOption, true);
    G.p_sr = createBooleanOption(shiftRoundOption, false);
    G.p_notsol = createBooleanOption(notStartOfLineOption, false);
    G.p_cpo_w = createBooleanOption(changeWordBlanksOption, true);
    G.p_to = createBooleanOption(tildeOperator, false);
    G.p_cpo_search = createBooleanOption(searchFromEndOption, true);
    G.p_ws = createBooleanOption(wrapScanOption, true);

    G.p_meta_equals = createBooleanOption(metaEqualsOption, true);
    G.p_meta_escape = createStringOption(metaEscapeOption, G.metaEscapeDefault);

    G.p_ic = createBooleanOption(ignoreCaseOption, false);
    
    G.b_p_et = createBooleanOption(expandTabs, false);
    
    G.p_report = createIntegerOption(report, 2);
    G.p_bs = createIntegerOption(backspace, 0);
    G.p_so = createIntegerOption(scrollOff, 2);
    G.b_p_sw = createIntegerOption(shiftWidth, 4);
    G.b_p_ts = createIntegerOption(tabStop, 8);
    
    G.useFrame  = createBooleanOption(commandEntryFrameOption , false);
    
    G.readOnlyHack = createBooleanOption(readOnlyHackOption, true);
    G.isClassicUndo = createBooleanOption(classicUndoOption, true);
    G.dbgEditorActivation = createBooleanOption(dbgEditorActivation, false);

    dbgInit();
    setupOptionDescs();
    didInit = true;
  }
  
  static Preferences getPrefs() {
      return prefs;
  }

  // NEEDSWORK: FOR NOW PROPOGATE DEFAULTS TO DATA BASE
  static private boolean fAddDefaultToDB = false;
  static private StringOption createStringOption(String name,
                                                String defaultValue)
  {
    StringOption opt = new StringOption(name, defaultValue);
    options.put(name, opt);
    optionsList.add(name);
    if(fAddDefaultToDB) {
      opt.setValue(defaultValue);
    }
    return opt;
  }

  static private BooleanOption createBooleanOption(String name,
                                                  boolean defaultValue)
  {
    BooleanOption opt = new BooleanOption(name, defaultValue);
    options.put(name, opt);
    optionsList.add(name);
    if(fAddDefaultToDB) {
      opt.setBoolean(defaultValue);
    }
    return opt;
  }

  static private IntegerOption createIntegerOption(String name,
                                                  int defaultValue)
  {
    IntegerOption opt = new IntegerOption(name, defaultValue);
    options.put(name, opt);
    optionsList.add(name);
    if(fAddDefaultToDB) {
      opt.setInteger(defaultValue);
    }
    return opt;
  }

  public static void setOptionValue(String name, String value) {
    Option option = getOption(name);
    option.setValue(value);
  }

  public static Option getOption(String name) {
    return (Option)options.get(name);
  }

  /** @return the String key names of the options. */
  public static List<String> getOptionNamesList() {
    return Collections.unmodifiableList(optionsList);
  }

  public static final String dbgKeyStrokes = "viDbgKeyStrokes";
  public static final String dbgCache = "viDbgCache";
  static void dbgInit() {
    createBooleanOption(dbgKeyStrokes, false);
    createBooleanOption(dbgCache, false);
  }

  private static void setupOptionDesc(String name, String desc) {
    Option opt = (Option)options.get(name);
    if(opt != null) {
      opt.desc = desc;
    } else {
      throw new Error("Unknown option: " + name);
    }
  }

  private static void setupOptionDescs() {
    //
    // optionCategory = new EditorOptionCategory("Vi cursor wrap options");
    //
    // 		boolean
    //
    setupOptionDesc(Options.backspaceWrapPreviousOption,
               "<backspace> wraps to previous line");
    setupOptionDesc(Options.hWrapPreviousOption,
               "\"h\" wraps to previous line");
    setupOptionDesc(Options.leftWrapPreviousOption,
               "<left> wraps to previous line");
    setupOptionDesc(Options.spaceWrapNextOption,
               "<space> wraps to next line");
    setupOptionDesc(Options.lWrapNextOption,
               "\"l\" wraps to next line");
    setupOptionDesc(Options.rightWrapNextOption,
               "<right> wraps to next line");
    setupOptionDesc(Options.tildeWrapNextOption,
               "\"~\" wraps to next line");
    //
    // optionCategory = new EditorOptionCategory("Vi miscellaneous options");
    //
    // 		boolean
    //
    setupOptionDesc(Options.commandEntryFrameOption,
               "use modal frame for command/search entry");
    setupOptionDesc(Options.unnamedClipboardOption,
               "use clipboard for unamed yank, delete and put");
    setupOptionDesc(Options.notStartOfLineOption,
               "after motion try to keep column position");
    setupOptionDesc(Options.wrapScanOption,
               "searches wrap around end of file");
    setupOptionDesc(Options.searchFromEndOption,
               "search continues at end of match");
    setupOptionDesc(Options.tildeOperator ,
               "tilde \"~\" acts like an operator, e.g. \"~w\" works");
    setupOptionDesc(Options.changeWordBlanksOption,
               "\"cw\" affects sequential white space");
    setupOptionDesc(Options.joinSpacesOption,
               "\"J\" inserts two spaces after a \".\", \"?\" or \"!\"");
    setupOptionDesc(Options.shiftRoundOption,
               "\"<\" and \">\" round indent to multiple of shiftwidth");
    //
    // optionCategory = new EditorOptionCategory("Vi debug options");
    //
    // 		boolean
    //
    setupOptionDesc(Options.dbgCache,
               "Output info on text/doc cache");
    setupOptionDesc(Options.dbgKeyStrokes,
               "Output info for each keystroke");
  }
}

