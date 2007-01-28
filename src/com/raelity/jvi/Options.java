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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
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
  public static final String commandEntryFrame =
  						"viCommandEntryFrameOption";
  public static final String backspaceWrapPrevious =
                                                "viBackspaceWrapPrevious";
  public static final String hWrapPrevious = "viHWrapPrevious";
  public static final String leftWrapPrevious = "viLeftWrapPrevious";
  public static final String spaceWrapNext = "viSpaceWrapNext";
  public static final String lWrapNext = "viLWrapNext";
  public static final String rightWrapNext = "viRightWrapNext";
  public static final String tildeWrapNext = "viTildeWrapNext";

  public static final String unnamedClipboard = "viUnnamedClipboard";
  public static final String joinSpaces = "viJoinSpaces";
  public static final String shiftRound = "viShiftRound";
  public static final String notStartOfLine = "viNotStartOfLine";
  public static final String changeWordBlanks = "viChangeWordBlanks";
  public static final String tildeOperator = "viTildeOperator";
  public static final String searchFromEnd = "viSearchFromEnd";
  public static final String wrapScan = "viWrapScan";

  public static final String metaEquals = "viMetaEquals";
  public static final String metaEscape = "viMetaEscape";

  public static final String ignoreCase = "viIgnoreCase";
  
  public static final String expandTabs = "viExpandTabs";

  public static final String report = "viReport";
  public static final String backspace = "viBackspace";
  public static final String scrollOff = "viScrollOff";
  public static final String shiftWidth = "viShiftWidth";
  public static final String tabStop = "viTabStop";

  
  public static final String readOnlyHack = "viReadOnlyHack";
  public static final String classicUndoOption = "viClassicUndo";
  
  public static final String dbgKeyStrokes = "viDbgKeyStrokes";
  public static final String dbgCache = "viDbgCache";
  public static final String dbgEditorActivation = "viDbgEditorActivation";

  
  private static Map<String,Option> options = new HashMap<String,Option>();
  
  static List<String>generalList = new ArrayList<String>();
  static List<String>miscList = new ArrayList<String>();
  static List<String>cursorWrapList = new ArrayList<String>();
  static List<String>debugList = new ArrayList<String>();

  static Preferences prefs;

  private static boolean didInit = false;
  public static void init() {
    if(didInit) {
      return;
    }

    prefs = ViManager.getViFactory().getPreferences();

    prefs.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
	Option opt = options.get(evt.getKey());
	if(opt != null) {
          if(evt.getNewValue() != null) {
              opt.preferenceChange(evt.getNewValue());
          }
	}
      }
    });
    
    /////////////////////////////////////////////////////////////////////
    //
    //
    // General Options
    //
    //
    G.p_meta_equals = createBooleanOption(metaEquals, true);
    setupOptionDesc(generalList, metaEquals, "RE Meta Equals",
            "In a regular expression"
            + " use '=', not '?', to indicate an optional atom."
            + " vim default is '='.");
    setExpertHidden(metaEquals, true, false);

    G.p_meta_escape = createStringOption(metaEscape, G.metaEscapeDefault,
            new StringOption.Validator() {
              public void validate(String val) throws PropertyVetoException {
		for(int i = 0; i < val.length(); i++) {
		  if(G.metaEscapeAll.indexOf(val.charAt(i)) < 0) {
		     throw new PropertyVetoException(
		         "Only characters from '" + G.metaEscapeAll
                         + "' are RE metacharacters."
                         + " Not '" + val.substring(i,i+1) + "'.",
                       new PropertyChangeEvent(opt, opt.getName(),
                                               opt.getString(), val));
		  }
		}
              }
            });
    setupOptionDesc(generalList, metaEscape, "RE Meta Escape",
            "Regular expression metacharacters requiring escape:"
            + " any of: '(', ')', '|', '+', '?', '{'."
            + " By default vim requires escape, '\\', for these characters.");
    setExpertHidden(metaEscape, true, false);

    G.p_so = createIntegerOption(scrollOff, 0);
    setupOptionDesc(generalList, scrollOff, "'scrolloff' 'so'",
           "visible context around cursor (scrolloff)" 
            + "	Minimal number of screen lines to keep above and below the"
            + " cursor. This will make some context visible around where you"
            + " are working.  If you set it to a very large value (999) the"
            + " cursor line will always be in the middle of the window"
            + " (except at the start or end of the file)");

    G.p_bs = createIntegerOption(backspace, 0,
            new IntegerOption.Validator() {
              public void validate(int val) throws PropertyVetoException {
                  if(val < 0 || val > 2) {
		     throw new PropertyVetoException(
		         "Only 0, 1, or 2 are allowed."
                         + " Not '" + val + "'.",
                       new PropertyChangeEvent(opt, opt.getName(),
                                               opt.getInteger(), val));
                  }
              }
            });
    setupOptionDesc(generalList, backspace, "'backspace' 'bs'",
            "Influences the working of <BS>, <Del> during insert."
            + "\n  0 - no special handling."
            + "\n  1 - allow backspace over <EOL>."
            + "\n  2 - allow backspace over start of insert.");

    G.p_report = createIntegerOption(report, 2);
    setupOptionDesc(generalList, report, "'report'",
            "Threshold for reporting number of lines changed.  When the"
            + " number of changed lines is more than 'report' a message will"
            + " be given for most \":\" commands.  If you want it always, set"
            + " 'report' to 0.  For the \":substitute\" command the number of"
            + " substitutions is used instead of the number of lines.");

    G.p_ic = createBooleanOption(ignoreCase, false);
    setupOptionDesc(generalList, ignoreCase, "'ignorecase' 'ic'",
            "Ignore case in search patterns.");

    G.b_p_et = createBooleanOption(expandTabs, false);
    setupOptionDesc(generalList, expandTabs, "'expandtab' 'et'",
           "In Insert mode: Use the appropriate number of spaces to"
           + " insert a <Tab>. Spaces are used in indents with the '>' and"
           + " '<' commands.");

    G.b_p_sw = createIntegerOption(shiftWidth, 8);
    setupOptionDesc(generalList, shiftWidth, "'shiftwidth' 'sw'",
            "Number of spaces to use for each step of indent. Used for '>>',"
            + " '<<', etc.");

    G.b_p_ts = createIntegerOption(tabStop, 8);
    setupOptionDesc(generalList, tabStop, "'tabstop' 'ts'",
            "Number of spaces that a <Tab> in the file counts for.");

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi cursor wrap options
    //
    //
    G.p_ww_bs = createBooleanOption(backspaceWrapPrevious, true);
    setupOptionDesc(cursorWrapList, backspaceWrapPrevious,
               "'whichwrap' 'ww'  b - <BS>",
               "<backspace> wraps to previous line");

    G.p_ww_h = createBooleanOption(hWrapPrevious, false);
    setupOptionDesc(cursorWrapList, hWrapPrevious,
               "'whichwrap' 'ww'  h - \"h\"",
               "\"h\" wraps to previous line (not recommended, see vim doc)");

    G.p_ww_larrow = createBooleanOption(leftWrapPrevious, false);
    setupOptionDesc(cursorWrapList, leftWrapPrevious,
               "'whichwrap' 'ww'  < - <Left>",
               "<left> wraps to previous line");

    G.p_ww_sp = createBooleanOption(spaceWrapNext, true);
    setupOptionDesc(cursorWrapList, spaceWrapNext,
               "'whichwrap' 'ww'  s - <Space>",
               "<space> wraps to next line");

    G.p_ww_l = createBooleanOption(lWrapNext, false);
    setupOptionDesc(cursorWrapList, lWrapNext,
               "'whichwrap' 'ww'  l - \"l\"",
               "\"l\" wraps to next line (not recommended, see vim doc)");

    G.p_ww_rarrow = createBooleanOption(rightWrapNext, false);
    setupOptionDesc(cursorWrapList, rightWrapNext,
               "'whichwrap' 'ww'  > - <Right>",
               "<right> wraps to next line");

    G.p_ww_tilde = createBooleanOption(tildeWrapNext, false);
    setupOptionDesc(cursorWrapList, tildeWrapNext,
               "'whichwrap' 'ww'  ~ - \"~\"",
               "\"~\" wraps to next line");

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi miscellaneous options
    //
    //
    G.p_cb = createBooleanOption(unnamedClipboard, false);
    setupOptionDesc(miscList, unnamedClipboard,
               "'clipboard' 'cb' (unnamed)",
               "use clipboard for unamed yank, delete and put");

    G.p_notsol = createBooleanOption(notStartOfLine, false);
    setupOptionDesc(miscList, notStartOfLine, "(not)'startofline' (not)'sol'",
               "After motion try to keep column position."
            + " NOTE: state is opposite of vim.");

    G.p_ws = createBooleanOption(wrapScan, true);
    setupOptionDesc(miscList, wrapScan, "'wrapscan' 'ws'",
               "Searches wrap around the end of the file.");

    G.p_cpo_search = createBooleanOption(searchFromEnd, true);
    setupOptionDesc(miscList, searchFromEnd, "'cpoptions' 'cpo' \"c\"",
               "search continues at end of match");

    G.p_to = createBooleanOption(tildeOperator, false);
    setupOptionDesc(miscList, tildeOperator , "'tildeop' 'top'",
               "tilde \"~\" acts like an operator, e.g. \"~w\" works");

    G.p_cpo_w = createBooleanOption(changeWordBlanks, true);
    setupOptionDesc(miscList, changeWordBlanks, "'cpoptions' 'cpo' \"w\"",
               "\"cw\" affects sequential white space");

    G.p_js = createBooleanOption(joinSpaces, true);
    setupOptionDesc(miscList, joinSpaces, "'joinspaces' 'js'",
               "\"J\" inserts two spaces after a \".\", \"?\" or \"!\"");

    G.p_sr = createBooleanOption(shiftRound, false);
    setupOptionDesc(miscList, shiftRound, "'shiftround' 'sr'",
               "\"<\" and \">\" round indent to multiple of shiftwidth");
    
    G.useFrame  = createBooleanOption(commandEntryFrame , false);
    setupOptionDesc(miscList, commandEntryFrame, "use modal frame",
               "use modal frame for command/search entry");
    setExpertHidden(commandEntryFrame, true, true);

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi debug options
    //
    //
    G.readOnlyHack = createBooleanOption(readOnlyHack, true);
    setupOptionDesc(debugList, readOnlyHack, "enable read only hack",
            "A Java implementation issue, restricts the characters that jVi"
            + " recieves for a read only file. Enabling this, changes the file"
            + " editor mode to read/write so that the file can be viewed"
            + " using the Normal Mode vi commands.");
    setExpertHidden(readOnlyHack, true, false);

    // Want to turn following on, but NB problems
    G.isClassicUndo = createBooleanOption(classicUndoOption, false);
    setupOptionDesc(debugList, classicUndoOption, "classic undo", "yyy");
    setExpertHidden(classicUndoOption, true, true);

    G.dbgEditorActivation = createBooleanOption(dbgEditorActivation, false);
    setupOptionDesc(debugList, dbgEditorActivation, "debug activation",
               "Output info about editor switching between files/windows");

    createBooleanOption(dbgKeyStrokes, false);
    setupOptionDesc(debugList, dbgKeyStrokes, "debug KeyStrokes",
               "Output info for each keystroke");

    createBooleanOption(dbgCache, false);
    setupOptionDesc(debugList, dbgCache, "debug cache",
               "Output info on text/doc cache");

    didInit = true;
  }

  static Preferences getPrefs() {
      return prefs;
  }

  // NEEDSWORK: FOR NOW PROPOGATE DEFAULTS TO DATA BASE
  static private boolean fAddDefaultToDB = false;
  static private StringOption createStringOption(String name,
                                                 String defaultValue) {
    return createStringOption(name, defaultValue, null);
  }
  
  static private StringOption createStringOption(String name,
                                                String defaultValue,
                                                StringOption.Validator valid) {
    StringOption opt = new StringOption(name, defaultValue, valid);
    options.put(name, opt);
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
    if(fAddDefaultToDB) {
      opt.setBoolean(defaultValue);
    }
    return opt;
  }
  
  static private IntegerOption createIntegerOption(String name,
                                                  int defaultValue)
  {
      return createIntegerOption(name, defaultValue, null);
  }

  static private IntegerOption createIntegerOption(String name,
                                                  int defaultValue,
                                                  IntegerOption.Validator valid)
  {
    IntegerOption opt = new IntegerOption(name, defaultValue, valid);
    options.put(name, opt);
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
    return options.get(name);
  }

  /**
   * Only used by JBuilder, should convert....
   * @return the String key names of the options.
   */
  public static List<String> getOptionNamesList() {
    List<String> l = new ArrayList();
    l.addAll(generalList);
    l.addAll(miscList);
    l.addAll(cursorWrapList);
    l.addAll(debugList);
    return Collections.unmodifiableList(l);
  }

  private static void setupOptionDesc(List<String> optionsGroup, String name,
                                      String displayName, String desc) {
    Option opt = options.get(name);
    if(opt != null) {
      if(optionsGroup != null) {
          optionsGroup.add(name);
      }
      if(opt.desc != null) {
	  throw new Error("option: " + name + " already has a description.");
      }
      opt.desc = desc;
      opt.displayName = displayName;
    } else {
      throw new Error("Unknown option: " + name);
    }
  }
  
  private static void setExpertHidden(String optionName,
                                      boolean fExpert, boolean fHidden) {
    Option opt = options.get(optionName);
    if(opt != null) {
      opt.fExpert = fExpert;
      opt.fHidden = fHidden;
    }
  }
}

