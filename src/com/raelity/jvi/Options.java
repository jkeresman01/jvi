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

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;

import com.borland.primetime.editor.SearchManager; //HACK
import com.borland.primetime.editor.SearchOptions; //HACK

/**
 * Option handling from external sources.
 * <br>
 * Should there be a vi command to set the options to persistent storage,
 * this is useful if want to save after several set commands.
 * <br>
 *
 */

public class Options {
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

  private static Map options = new HashMap();
  private static Set optionNames;

  static {
    init();
  }

  private static boolean didInit = false;
  public static void init() {
    if(didInit) {
      return;
    }
    G.p_ww_bs = setupBooleanOption(backspaceWrapPreviousOption, true);
    G.p_ww_h = setupBooleanOption(hWrapPreviousOption, false);
    G.p_ww_larrow = setupBooleanOption(leftWrapPreviousOption, false);
    G.p_ww_sp = setupBooleanOption(spaceWrapNextOption, true);
    G.p_ww_l = setupBooleanOption(lWrapNextOption, false);
    G.p_ww_rarrow = setupBooleanOption(rightWrapNextOption, false);
    G.p_ww_tilde = setupBooleanOption(tildeWrapNextOption, false);

    G.p_cb = setupBooleanOption(unnamedClipboardOption, false);
    G.p_js = setupBooleanOption(joinSpacesOption, true);
    G.p_sr = setupBooleanOption(shiftRoundOption, false);
    G.p_notsol = setupBooleanOption(notStartOfLineOption, false);
    G.p_cpo_w = setupBooleanOption(changeWordBlanksOption, true);
    G.p_to = setupBooleanOption(tildeOperator, false);
    G.p_cpo_search = setupBooleanOption(searchFromEndOption, true);
    G.p_ws = setupBooleanOption(wrapScanOption, true);

    G.p_meta_equals = setupBooleanOption(metaEqualsOption, true);
    G.p_meta_escape = setupStringOption(metaEscapeOption, G.metaEscapeDefault);

    // HACK: p_ic may be changed to a new object by embedding environment
    // this is only needed because there is not proper listening set up
    // for jVi options.
    //G.p_ic = new IgnoreCaseBooleanOption(); // HACK
    G.p_ic = setupBooleanOption(ignoreCaseOption, false);

    dbgInit();
    didInit = true;
  }

  static private StringOption setupStringOption(String name,
                                                String defaultValue)
  {
    StringOption opt = new StringOption(name, defaultValue);
    options.put(name, opt);
    return opt;
  }

  static private BooleanOption setupBooleanOption(String name,
                                                  boolean defaultValue)
  {
    BooleanOption opt = new BooleanOption(name, defaultValue);
    options.put(name, opt);
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
  public static Set getOptionNamesSet() {
    if(optionNames == null) {
      optionNames = Collections.unmodifiableSet(options.keySet());
    }
    return optionNames;
  }

  public static final String dbgKeyStrokes = "viDbgKeyStroks";
  public static final String dbgCache = "viDbgCache";
  static void dbgInit() {
    setupBooleanOption(dbgKeyStrokes, false);
    setupBooleanOption(dbgCache, false);
  }

  /* MOVED TO A JBUILDER CLASS
  // HACK, hook directly into JB stuff. Should be listening to option
  // at the least, or using set command....
  static class IgnoreCaseBooleanOption extends BooleanOption {

    IgnoreCaseBooleanOption() {
      super(Options.ignoreCaseOption, false);
      options.put(ignoreCaseOption, this);
    }

    public boolean getBoolean() {
      setBoolean(! SearchManager.getSavedOptions().isCaseSensitive());
      return super.getBoolean();
    }
  }
  */
}

