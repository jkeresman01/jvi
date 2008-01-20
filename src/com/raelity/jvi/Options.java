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

import com.raelity.jvi.ColonCommands.ColonEvent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
//import java.util.ArrayDeque; JDK1.6
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
//import java.util.Deque; JDK1.6
import java.util.Set;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import com.raelity.text.TextUtil.MySegment;
import com.raelity.jvi.Option.ColorOption;

import static com.raelity.jvi.Constants.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Option handling from external sources.
 * <br>
 * Should there be a vi command to set the options to persistent storage,
 * this is useful if want to save after several set commands.
 * <br>
 *
 */

public final class Options {
  private Options() {
  }
  private static Options options;
  private static PropertyChangeSupport pcs
                        = new PropertyChangeSupport(getOptions());
  
  static Options getOptions() {
      if(options == null) {
          options = new Options();
      }
      return options;
  }
  
  public enum Category { DEBUG, MODIFY, GENERAL, CURSOR_WRAP }
  
  public static final String commandEntryFrame = "viCommandEntryFrameOption";
  public static final String redoTrack = "viRedoTrack";
  public static final String pcmarkTrack = "viPCMarkTrack";
  public static final String autoPopupFN = "viAutoPopupFN";
  public static final String coordSkip = "viCoordSkip";

  public static final String backspaceWrapPrevious = "viBackspaceWrapPrevious";
  public static final String hWrapPrevious = "viHWrapPrevious";
  public static final String leftWrapPrevious = "viLeftWrapPrevious";
  public static final String spaceWrapNext = "viSpaceWrapNext";
  public static final String lWrapNext = "viLWrapNext";
  public static final String rightWrapNext = "viRightWrapNext";
  public static final String tildeWrapNext = "viTildeWrapNext";
  public static final String insertLeftWrapPrevious = "viInsertLeftWrapPrevious";
  public static final String insertRightWrapNext = "viInsertRightWrapNext";

  public static final String unnamedClipboard = "viUnnamedClipboard";
  public static final String joinSpaces = "viJoinSpaces";
  public static final String shiftRound = "viShiftRound";
  public static final String notStartOfLine = "viNotStartOfLine";
  public static final String changeWordBlanks = "viChangeWordBlanks";
  public static final String tildeOperator = "viTildeOperator";
  public static final String searchFromEnd = "viSearchFromEnd";
  public static final String endOfSentence = "viEndOfSentence";
  public static final String wrapScan = "viWrapScan";

  public static final String metaEquals = "viMetaEquals";
  public static final String metaEscape = "viMetaEscape";
  public static final String incrSearch = "viIncrSearch";
  public static final String highlightSearch = "viHighlightSearch";

  public static final String ignoreCase = "viIgnoreCase";
  public static final String platformBraceMatch = "viPlatformBraceMatch";
  
  public static final String expandTabs = "viExpandTabs";

  public static final String report = "viReport";
  public static final String backspace = "viBackspace";
  public static final String scrollOff = "viScrollOff";
  public static final String shiftWidth = "viShiftWidth";
  public static final String tabStop = "viTabStop";
  public static final String textWidth = "viTextWidth";
  public static final String showMode = "viShowMode";
  public static final String showCommand = "viShowCommand";
  
  public static final String modeline = "viModeline";
  public static final String modelines = "viModelines";
  
  public static final String selection = "viSelection";
  public static final String selectMode = "viSelectMode";
  public static final String selectColor = "viSelectColor";
  public static final String selectFgColor = "viSelectFgColor";

  public static final String equalProgram = "viEqualProgram";
  public static final String formatProgram = "viFormatProgram";

  public static final String shell = "viShell";
  public static final String shellCmdFlag = "viShellCmdFlag";
  public static final String shellXQuote = "viShellXQuote";
  public static final String shellSlash = "viShellSlash";
  
  public static final String readOnlyHack = "viReadOnlyHack";
  public static final String classicUndoOption = "viClassicUndo";
  
  public static final String dbgRedo = "viDbgRedo";
  public static final String dbgKeyStrokes = "viDbgKeyStrokes";
  public static final String dbgCache = "viDbgCache";
  public static final String dbgEditorActivation = "viDbgEditorActivation";
  public static final String dbgBang = "viDbgBang";
  public static final String dbgBangData = "viDbgBangData";
  public static final String dbgMouse = "viDbgMouse";
  public static final String dbgCompletion = "viDbgCompletion";
  public static final String dbgCoordSkip = "viDbgCoordSkip";

  public static final String twMagic = "#TEXT-WIDTH#";

  
  private static Map<String,Option> optionsMap = new HashMap<String,Option>();
  
  static List<String>platformList = new ArrayList<String>();
  static List<String>generalList = new ArrayList<String>();
  static List<String>modifyList = new ArrayList<String>();
  static List<String>searchList = new ArrayList<String>();
  static List<String>cursorWrapList = new ArrayList<String>();
  static List<String>externalProcessList = new ArrayList<String>();
  static List<String>debugList = new ArrayList<String>();

  static Preferences prefs;

  private static boolean didInit = false;
  public static void init() {
    if(didInit) {
      return;
    }
    didInit = true;

    prefs = ViManager.getViFactory().getPreferences();

    prefs.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
	Option opt = optionsMap.get(evt.getKey());
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
    // Platform Options
    //
    //
    
    platformList.add("jViVersion");

    G.redoTrack = createBooleanOption(redoTrack, true);
    setupOptionDesc(platformList, redoTrack, "\".\" magic redo tracking",
                    "Track magic document changes during input"
                    + " mode for the \".\" commnad. These"
                    + " changes are often the result of IDE code completion");

    G.pcmarkTrack = createBooleanOption(pcmarkTrack, true);
    setupOptionDesc(platformList, pcmarkTrack,
                    "\"``\" magic pcmark tracking", "Track magic cursor "
                    + " movments for the \"``\" command. These movement are"
                    + " often the result of IDE actions invoked external"
                    + " to jVi.");
    
    createColorOption(selectColor, new Color(0xffe588)); // a light orange
    setupOptionDesc(platformList, selectColor, "'hl-visual' color",
            "The color used for a visual mode selection.");
    
    createColorOption(selectFgColor, new Color(0x000000)); // black
    setupOptionDesc(platformList, selectFgColor, "'hl-visual' foreground color",
            "The color used for a visual mode selection foreground.");
    // Hide this until there's a way to specify "default"
    setExpertHidden(selectFgColor, false, true);

    G.isClassicUndo = createBooleanOption(classicUndoOption, true);
    setupOptionDesc(platformList, classicUndoOption, "classic undo",
                    "When false, undo is done according to the"
                    + " underlying platform; usually tiny chunks.");
    setExpertHidden(classicUndoOption, true, false);
    
    G.useFrame  = createBooleanOption(commandEntryFrame , true);
    setupOptionDesc(platformList, commandEntryFrame, "use modal frame",
               "Use modal frame for command/search entry."
               + " Change takes affect after restart.");
    setExpertHidden(commandEntryFrame, true, false);
    
    createBooleanOption(autoPopupFN, false);
    setupOptionDesc(platformList, autoPopupFN, "\":e#\" Auto Popup",
               "When doing \":\" command line entry, if \"e#\" is"
               + " entered then automatically popup a file"
               + " name completion window. NB6 only; post 07/07/22");

    G.isCoordSkip = createBooleanOption(coordSkip, true);
    setupOptionDesc(platformList, coordSkip, "Code Folding Compatible",
            "When false revert some navigation algorithms, e.g. ^F,"
            + " to pre code folding behavior. A just in case option;"
            + " if needed, please file a bug report.");
    setExpertHidden(coordSkip, true, false);

    /////////////////////////////////////////////////////////////////////
    //
    //
    // General Options
    //
    //
    
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
            @Override
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
    
    G.p_smd = createBooleanOption(showMode, true);
    setupOptionDesc(generalList, showMode, "'showmode' 'smd'",
            "If in Insert or Replace mode display that information.");
    
    G.p_sc = createBooleanOption(showCommand, true);
    setupOptionDesc(generalList, showCommand, "'showcmd' 'sc'",
            "Show (partial) command in status line.");

    G.p_report = createIntegerOption(report, 2);
    setupOptionDesc(generalList, report, "'report'",
            "Threshold for reporting number of lines changed.  When the"
            + " number of changed lines is more than 'report' a message will"
            + " be given for most \":\" commands.  If you want it always, set"
            + " 'report' to 0.  For the \":substitute\" command the number of"
            + " substitutions is used instead of the number of lines.");
    
    G.p_ml = createBooleanOption(modeline, true);
    setupOptionDesc(generalList, modeline, "'modeline' 'ml'",
            "Enable/disable modelines option");
    
    G.p_mls = createIntegerOption(modelines, 5);
    setupOptionDesc(generalList, modelines, "'modelines' 'mls'",
	    " If 'modeline' is on 'modelines' gives the number of lines"
            + " that is checked for set commands.  If 'modeline' is off"
            + " or 'modelines' is zero no lines are checked.");

    G.p_cb = createBooleanOption(unnamedClipboard, false);
    setupOptionDesc(generalList, unnamedClipboard,
               "'clipboard' 'cb' (unnamed)",
               "use clipboard for unamed yank, delete and put");

    G.p_notsol = createBooleanOption(notStartOfLine, false);
    setupOptionDesc(generalList, notStartOfLine, "(not)'startofline' (not)'sol'",
               "After motion try to keep column position."
            + " NOTE: state is opposite of vim.");

    G.p_sel = createStringOption(selection, "inclusive",
            new StringOption.Validator() {
            @Override
              public void validate(String val) throws PropertyVetoException {
                if("old".equals(val)
                   || "inclusive".equals(val)
                   || "exclusive".equals(val))
                  return;
                throw new PropertyVetoException(
                    "Value must be one of 'old', 'inclusive' or 'exclusive'."
                                + " Not '" + val + "'.",
                            new PropertyChangeEvent(opt, opt.getName(),
                                                    opt.getString(), val));
              }
            });
    setupOptionDesc(generalList, selection, "'selection' 'sel'",
            "This option defines the behavior of the selection."
            + " It is only used in Visual and Select mode."
            + "Possible values: 'old', 'inclusive', 'exclusive'");
    setExpertHidden(selection, true, false);
    
    G.p_slm = createStringOption(selectMode, "",
            new StringOption.Validator() {
            @Override
              public void validate(String val) throws PropertyVetoException {
                  if ("mouse".equals(val)
                      || "key".equals(val)
                      || "cmd".equals(val))
                  return;
                throw new PropertyVetoException(
                    "Value must be one of 'mouse', 'key' or 'cmd'."
                                + " Not '" + val + "'.",
                            new PropertyChangeEvent(opt, opt.getName(),
                                                    opt.getString(), val));
              }
            });
    setupOptionDesc(generalList, selectMode, "'selectmode' 'slm'",
            "This is a comma separated list of words, which specifies when to"
            + " start Select mode instead of Visual mode, when a selection is"
            + " started. Possible values: 'mouse', key' or 'cmd'");
    setExpertHidden(selectMode, true, true);

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi Options that affect modifications to the document
    //
    //

    G.p_to = createBooleanOption(tildeOperator, false);
    setupOptionDesc(modifyList, tildeOperator , "'tildeop' 'top'",
               "tilde \"~\" acts like an operator, e.g. \"~w\" works");

    G.p_cpo_w = createBooleanOption(changeWordBlanks, true);
    setupOptionDesc(modifyList, changeWordBlanks, "'cpoptions' 'cpo' \"w\"",
               "\"cw\" affects sequential white space");

    G.p_js = createBooleanOption(joinSpaces, true);
    setupOptionDesc(modifyList, joinSpaces, "'joinspaces' 'js'",
               "\"J\" inserts two spaces after a \".\", \"?\" or \"!\"");

    G.p_sr = createBooleanOption(shiftRound, false);
    setupOptionDesc(modifyList, shiftRound, "'shiftround' 'sr'",
               "\"<\" and \">\" round indent to multiple of shiftwidth");

    /////////////////////////
    //
    // per buffer options are accessed through curbuf.
    //
    
    /*G.b_p_et = */createBooleanOption(expandTabs, false);
    setupOptionDesc(modifyList, expandTabs, "'expandtab' 'et'",
           "In Insert mode: Use the appropriate number of spaces to"
           + " insert a <Tab>. Spaces are used in indents with the '>' and"
           + " '<' commands.");

    /*G.b_p_sw = */createIntegerOption(shiftWidth, 8);
    setupOptionDesc(modifyList, shiftWidth, "'shiftwidth' 'sw'",
            "Number of spaces to use for each step of indent. Used for '>>',"
            + " '<<', etc.");

    /*G.b_p_ts = */createIntegerOption(tabStop, 8);
    setupOptionDesc(modifyList, tabStop, "'tabstop' 'ts'",
            "Number of spaces that a <Tab> in the file counts for.");

    /*G.b_p_ts = */createIntegerOption(textWidth, 79);
    setupOptionDesc(modifyList, textWidth, "'textwidth' 'tw'",
            "This option currently only used in conjunction with the"
            + " 'gq' and 'Q' format command. This value is substituted"
            + " for " + twMagic + " in formatprg option string.");
    
    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi searching options
    //
    //

    G.p_is = createBooleanOption(incrSearch, true);
    setupOptionDesc(searchList, incrSearch, "'incsearch' 'is'",
            "While typing a search command, show where the pattern, as it was"
            + " typed so far, matches. If invalid pattern, no match"
            + " or abort then the screen returns to its original location."
            + " You still need to finish the search with"
            + " <ENTER> or abort it with <ESC>.");
    
    G.p_hls = createBooleanOption(highlightSearch, true);
    setupOptionDesc(searchList, highlightSearch, "'hlsearch' 'hls'",
                    "When there is a previous search pattern, highlight"
                    + " all its matches");
    
    G.p_ic = createBooleanOption(ignoreCase, false);
    setupOptionDesc(searchList, ignoreCase, "'ignorecase' 'ic'",
            "Ignore case in search patterns.");

    G.p_ws = createBooleanOption(wrapScan, true);
    setupOptionDesc(searchList, wrapScan, "'wrapscan' 'ws'",
               "Searches wrap around the end of the file.");

    G.p_cpo_search = createBooleanOption(searchFromEnd, true);
    setupOptionDesc(searchList, searchFromEnd, "'cpoptions' 'cpo' \"c\"",
               "search continues at end of match");

    G.p_cpo_j = createBooleanOption(endOfSentence, false);
    setupOptionDesc(searchList, endOfSentence, "'cpoptions' 'cpo' \"j\"",
		  "A sentence has to be followed by two spaces after"
                + " the '.', '!' or '?'.  A <Tab> is not recognized as"
                + " white space.");

    G.p_pbm = createBooleanOption(platformBraceMatch, true);
    setupOptionDesc(searchList, platformBraceMatch, "Platform Brace Matching",
		  "Use the platform/IDE for brace matching"
                  + " and match highlighting. This may enable additional"
                  + " match characters, words and features.");
    
    G.p_meta_equals = createBooleanOption(metaEquals, true);
    setupOptionDesc(searchList, metaEquals, "RE Meta Equals",
            "In a regular expression allow"
            + " '=', in addition to '?', to indicate an optional atom.");
    setExpertHidden(metaEquals, true, false);

    G.p_meta_escape = createStringOption(metaEscape, G.metaEscapeDefault,
            new StringOption.Validator() {
            @Override
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
    setupOptionDesc(searchList, metaEscape, "RE Meta Escape",
            "Regular expression metacharacters requiring escape:"
            + " any of: '(', ')', '|', '+', '?', '{'."
            + " By default vim requires escape, '\\', for these characters.");
    setExpertHidden(metaEscape, true, false);

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

    G.p_ww_i_left = createBooleanOption(insertLeftWrapPrevious, false);
    setupOptionDesc(cursorWrapList, insertLeftWrapPrevious,
               "'whichwrap' 'ww'  [ - <Left>",
               "in Insert Mode <Left> wraps to previous line");

    G.p_ww_i_right = createBooleanOption(insertRightWrapNext, false);
    setupOptionDesc(cursorWrapList, insertRightWrapNext,
               "'whichwrap' 'ww'  ] - <Right>",
               "in Insert Mode <Right> wraps to next line");

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi external process options
    //
    //
    boolean inWindows = ViManager.getOsVersion().isWindows();
    String defaultShell = System.getenv("SHELL");
    String defaultXQuote = "";
    String defaultFlag = null;

    if (defaultShell == null) {
      if (inWindows)
        defaultShell = "cmd.exe";
      else
        defaultShell = "sh";
    }

    if (defaultShell.contains("sh")) {
      defaultFlag = "-c";
      if (inWindows)
        defaultXQuote = "\"";
    }
    else
      defaultFlag = "/c";

    G.p_sh = createStringOption(shell, defaultShell);
    setupOptionDesc(externalProcessList, shell, "'shell' 'sh'",
            "Name of shell to use for ! and :! commands.  (default $SHELL " +
            "or \"sh\", MS-DOS and Win32: \"command.com\" or \"cmd.exe\").  " +
            "When changing also check 'shellcmndflag'.");

    G.p_shcf = createStringOption(shellCmdFlag, defaultFlag);
    setupOptionDesc(externalProcessList, shellCmdFlag, "'shellcmdflag' 'shcf'",
            "Flag passed to shell to execute \"!\" and \":!\" commands; " +
            "e.g., \"bash.exe -c ls\" or \"command.com /c dir\" (default: " +
            "\"-c\", MS-DOS and Win32, when 'shell' does not contain \"sh\" " +
            "somewhere: \"/c\").");

    G.p_sxq = createStringOption(shellXQuote, defaultXQuote);
    setupOptionDesc(externalProcessList, shellXQuote, "'shellxquote' 'sxq'",
            "Quoting character(s), put around the commands passed to the " +
            "shell, for the \"!\" and \":!\" commands (default: \"\"; for " +
            "Win32, when 'shell' contains \"sh\" somewhere: \"\\\"\").");
    
    G.p_ssl = createBooleanOption(shellSlash, false);
    setupOptionDesc(externalProcessList, shellSlash, "'shellslash' 'ssl'",
            "When set, a forward slash is used when expanding file names." +
            "This is useful when a Unix-like shell is used instead of " +
            "command.com or cmd.exe.");
    
    G.p_ep = createStringOption(equalProgram, "");
    setupOptionDesc(externalProcessList, equalProgram, "'equalprg' 'ep'",
            "External program to use for \"=\" command (default \"\").  " +
            "When this option is empty the internal formatting functions " +
            "are used.");

    G.p_fp = createStringOption(formatProgram, "fmt -w " + twMagic);
    setupOptionDesc(externalProcessList, formatProgram, "'formatprg' 'fp'",
            "The name of an external program used to format lines selected " +
            "with 'gq' operator (default \"fmt\").  The program must take " +
            "input on stdin and produce output to stdout.  In Unix, " +
            "\"fmt\" is such a program. " + twMagic + " in the string is " +
            "substituted by the value of textwidth option. ");

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
    setExpertHidden(readOnlyHack, true, true);

    G.dbgEditorActivation = createBooleanOption(dbgEditorActivation, false);
    setupOptionDesc(debugList, dbgEditorActivation, "debug activation",
               "Output info about editor switching between files/windows");

    createBooleanOption(dbgKeyStrokes, false);
    setupOptionDesc(debugList, dbgKeyStrokes, "debug KeyStrokes",
               "Output info for each keystroke");

    G.dbgRedo = createBooleanOption(dbgRedo, false);
    setupOptionDesc(debugList, dbgRedo, "debug redo buffer",
               "Output info on magic/tracking changes to redo buffer");

    createBooleanOption(dbgCache, false);
    setupOptionDesc(debugList, dbgCache, "debug cache",
               "Output info on text/doc cache");

    createBooleanOption(dbgBang, false);
    setupOptionDesc(debugList, dbgBang, "debug \"!\" cmds",
               "Output info about external processes");

    createBooleanOption(dbgBangData, false);
    setupOptionDesc(debugList, dbgBangData, "debug \"!\" cmds data",
               "Output data tranfers external processes");

    createBooleanOption(dbgCompletion, false);
    setupOptionDesc(debugList, dbgCompletion, "debug Completion",
               "Output info on completion, eg FileName.");

    G.dbgMouse = createBooleanOption(dbgMouse, false);
    setupOptionDesc(debugList, dbgMouse, "debug mouse events",
               "Output info about mouse events");

    G.dbgCoordSkip = createBooleanOption(dbgCoordSkip, false);
    setupOptionDesc(debugList, dbgCoordSkip, "debug coordinate skip",
               "");
  }

  static Preferences getPrefs() {
      return prefs;
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
  
  static public ColorOption createColorOption(String name,
                                              Color defaultValue) {
      return createColorOption(name, defaultValue, null);
  }

  static public ColorOption createColorOption(String name,
                                              Color defaultValue,
                                              ColorOption.Validator valid) {
    if(optionsMap.get(name) != null)
        throw new IllegalArgumentException("Option " + name + "already exists");
    ColorOption opt = new ColorOption(name, defaultValue, valid);
    optionsMap.put(name, opt);
    return opt;
  }

  public static void setOptionValue(String name, String value) {
    Option option = getOption(name);
    option.setValue(value);
  }

  public static Option getOption(String name) {
    return optionsMap.get(name);
  }

  /**
   * Only used by JBuilder, should convert....
   * @return the String key names of the options.
   */
  public static List<String> getOptionNamesList() {
    List<String> l = new ArrayList<String>();
    l.addAll(generalList);
    l.addAll(modifyList);
    l.addAll(cursorWrapList);
    l.addAll(debugList);
    return Collections.unmodifiableList(l);
  }

  public static void setupOptionDesc(Category category, String name,
                                     String displayName, String desc) {
      List<String> catList = null;
      switch(category) {
          case CURSOR_WRAP: catList = cursorWrapList; break;
          case DEBUG:       catList = debugList; break;
          case MODIFY:        catList = modifyList; break;
          case GENERAL:     catList = generalList; break;
      }
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
      if(opt.desc != null) {
	  throw new Error("option: " + name + " already has a description.");
      }
      opt.desc = desc;
      opt.displayName = displayName;
    } else {
      throw new Error("Unknown option: " + name);
    }
  }
  
  public static void setExpertHidden(String optionName,
                                      boolean fExpert, boolean fHidden) {
    Option opt = optionsMap.get(optionName);
    if(opt != null) {
      opt.fExpert = fExpert;
      opt.fHidden = fHidden;
    }
  }
  
  //
  // Look like a good bean
  // But they're static!
  //
  
  public static void addPropertyChangeListener( PropertyChangeListener listener )
  {
    pcs.addPropertyChangeListener( listener );
  }

  public static void removePropertyChangeListener( PropertyChangeListener listener )
  {
    pcs.removePropertyChangeListener( listener );
  }
  
  public static void addPropertyChangeListener(String p,
                                               PropertyChangeListener l)
  {
    pcs.addPropertyChangeListener(p, l);
  }

  public static void removePropertyChangeListener(String p,
                                                  PropertyChangeListener l)
  {
    pcs.removePropertyChangeListener(p, l);
  }
  
  /** This should only be used from Option and its subclasses */
  static void firePropertyChange(String name, Object oldValue, Object newValue) {
    pcs.firePropertyChange(name, oldValue, newValue);
  }
  
  //////////////////////////////////////////////////////////////////////
  //
  // set command including modelines processing
  //
  
  /** Implement ":se[t]".
   *
   * Options are either global or indirect, see the P_ XXX below An option
   * must be one or the other. Global options are static, an indirect option
   * is an instance variable in either G.curwin or G.curbuf. When a P_IND
   * variable is set, introspection is used to do the set.
   * <p>
   * In some cases, due to platform limitation, the same variable must be
   * set in all the instances, syncAllInstances(var) does that.
   */
  public static class SetCommand extends ColonCommands.ColonAction {
    
    private static class SetCommandException extends Exception {
      SetCommandException(String msg) {
        super(msg);
      }
    }
    
    private static int P_IND = 0x01; // indirect: either curwin or curbuf
    private static int P_WIN = 0x02; // curwin
    private static int P_OPT = 0x04; // global option
    
    private static class VimOption {
      String fullname;    // option name
      String shortname;   // option name
      int flags;          // P_* above
      // name of field and/or option
      String varName;     // java variable name in curbuf or curwin
      String optName;     // the jVi Option name.
      
      VimOption(String fullname,
                String shortname,
                int flags,
                String varName,
                String optName) {
        this.fullname = fullname;
        this.shortname = shortname;
        this.flags = flags;
        this.varName = varName;
        this.optName = optName;
      }
    }
    
    // MUST NOT SET both P_IND and P_OPT
    private static VimOption vopts[] = new VimOption[] {
      new VimOption("expandtab", "et", P_IND, "b_p_et", null),
      new VimOption("ignorecase", "ic", P_OPT, null, ignoreCase),
      new VimOption("incsearch", "is", P_OPT, null, incrSearch),
      new VimOption("hlsearch", "hls", P_OPT, null, highlightSearch),
      new VimOption("number", "nu", P_IND|P_WIN, "w_p_nu", null),
      new VimOption("shiftwidth", "sw", P_IND, "b_p_sw", shiftWidth),
      new VimOption("tabstop", "ts", P_IND, "b_p_ts", tabStop),
      new VimOption("textwidth", "tw", P_IND, "b_p_tw", textWidth),
    };
    
    public void actionPerformed(ActionEvent e) {
      ColonEvent evt = (ColonEvent) e;
      parseSetOptions(evt.getArgs());
    }
    
    public void parseSetOptions(List<String> eventArgs) {
      if(eventArgs == null
         || eventArgs.size() == 1 && "all".equals(eventArgs.get(0))) {
        displayAllOptions();
        return;
      }
      //Deque<String> args = new ArrayDeque<String>(); JDK1.6
      LinkedList<String> args = new LinkedList<String>();
      // copy eventArgs into args, with possible fixup
      // ":set sw =4" is allowed, so if something starts with "="
      // then append it to the previous element
      int j = 0;
      for(int i = 0; i < eventArgs.size(); i++) {
        String arg = eventArgs.get(i);
        if(arg.startsWith("=") && args.size() > 0)
        {
          arg = args.removeLast() + arg;
        }
        args.addLast(arg);
      }
      for (String arg : args) {
        try {
          parseSetOption(arg);
        } catch (SetCommandException ex) {
          // error message given
          return;
        } catch (IllegalAccessException ex) {
          ex.printStackTrace();
          return;
        } catch (IllegalArgumentException ex) {
          ex.printStackTrace();
          return;
        }
      }
    }
    
    private static class VimOptionDescriptor {
      
      Class type;
      Object value;
      
      // used if P_IND
      Field f;
      ViOptionBag bag;
      
      // used if regular option is provided
      Option opt;
      
      
      // Following not really option state, they are the
      // parse results when settigns options
      boolean fInv;
      boolean fNo;
      boolean fShow;
      boolean fValue;
      
      String split[];
    }
    
    // This is train of thought
    public static void parseSetOption(String arg)
    throws IllegalAccessException, SetCommandException {
      VimOptionDescriptor voptDesc = new VimOptionDescriptor();
      
      voptDesc.split = arg.split("[:=]");
      
      String voptName = voptDesc.split[0];
      if(voptDesc.split.length > 1) {
        voptDesc.fValue = true;
      }
      
      if(voptName.startsWith("no")) {
        voptDesc.fNo = true;
        voptName = voptName.substring(2);
      } else if(voptName.startsWith("inv")) {
        voptDesc.fInv = true;
        voptName = voptName.substring(3);
      } else if(voptName.endsWith("!")) {
        voptDesc.fInv = true;
        voptName = voptName.substring(0, voptName.length() -1);
      } else if(voptName.endsWith("?")) {
        voptDesc.fShow = true;
        voptName = voptName.substring(0, voptName.length() -1);
      }
      VimOption vopt = null;
      for (VimOption v : vopts) {
        if(voptName.equals(v.fullname) || voptName.equals(v.shortname)) {
          vopt = v;
          break;
        }
      }
      if(vopt == null) {
        String msg = "Unknown option: " + voptName;
        Msg.emsg(msg);
        throw new SetCommandException(msg);
      }
      
      if(!determineOptionState(vopt, voptDesc)) {
        String msg = "Internal error: " + arg;
        Msg.emsg(msg);
        throw new SetCommandException(msg);
      }
      
      Object newValue = newOptionValue(arg, vopt, voptDesc);
      
      if(voptDesc.fShow)
        Msg.smsg(formatDisplayValue(vopt, voptDesc.value));
      else {
        if(voptDesc.opt != null) {
          try {
            voptDesc.opt.validate(newValue);
          } catch (PropertyVetoException ex) {
            Msg.emsg(ex.getMessage());
            throw new SetCommandException(ex.getMessage());
          }
        }
        
        if((vopt.flags & P_IND) != 0) {
          voptDesc.f.set(voptDesc.bag, newValue);
          voptDesc.bag.viOptionSet(G.curwin, vopt.varName);
        } else {
          voptDesc.opt.setValue(newValue.toString());
        }
      }
    }
    
    /**
     * Set voptDesc with information about the argument vopt.
     */
    private static boolean determineOptionState(VimOption vopt,
                                                VimOptionDescriptor voptDesc) {
      if(vopt.optName != null)
        voptDesc.opt = getOption(vopt.optName);
      
      if((vopt.flags & P_IND) != 0) {
        voptDesc.bag = (vopt.flags & P_WIN) != 0 ? G.curwin : G.curbuf;
        try {
          voptDesc.f = voptDesc.bag.getClass().getField(vopt.varName);
        } catch (SecurityException ex) {
          ex.printStackTrace();
        } catch (NoSuchFieldException ex) {
          ex.printStackTrace();
        }
        if(voptDesc.f == null) {
          return false;
        }
        voptDesc.type = voptDesc.f.getType();
        // impossible to get exceptions
        try {
          voptDesc.value = voptDesc.f.get(voptDesc.bag);
        } catch (IllegalArgumentException ex) {
          ex.printStackTrace();
        } catch (IllegalAccessException ex) {
          ex.printStackTrace();
        }
      } else if((vopt.flags & P_OPT) != 0) {
        if(voptDesc.opt instanceof BooleanOption) {
          voptDesc.type = boolean.class;
          voptDesc.value = voptDesc.opt.getBoolean();
        } else if(voptDesc.opt instanceof IntegerOption) {
          voptDesc.type = int.class;
          voptDesc.value = voptDesc.opt.getInteger();
        }
      }
      return true;
    }
    
    // Most of the argument are class members
    private static Object newOptionValue(String arg,
                                         VimOption vopt,
                                         VimOptionDescriptor voptDesc)
    throws NumberFormatException, SetCommandException {
      Object newValue = null;
      
      if(voptDesc.type == boolean.class) {
        if(voptDesc.fValue) {
          // like: ":set ic=val"
          String msg = "Unknown argument: " + arg;
          Msg.emsg(msg);
          throw new SetCommandException(msg);
        }
        if(!voptDesc.fShow) {
          if(voptDesc.fInv)
            newValue = ! ((Boolean)voptDesc.value).booleanValue();
          else if(voptDesc.fNo)
            newValue = false;
          else
            newValue = true;
        }
      } else if(voptDesc.type == int.class) {
        if(!voptDesc.fValue)
          voptDesc.fShow = true;
        if(!voptDesc.fShow) {
            try {
              newValue = Integer.parseInt(voptDesc.split[1]);
            } catch (NumberFormatException ex) {
              String msg = "Number required after =: " + arg;
              Msg.emsg(msg);
              throw new SetCommandException(msg);
            }
        }
      } else
        assert false : "Type " + voptDesc.type.getSimpleName()
                        + " not handled";
      return newValue;
    }
    
    private static String formatDisplayValue(VimOption vopt,
                                             Object value) {
      String v = "";
      if(value instanceof Boolean) {
        v = (((Boolean)value).booleanValue()
        ? "  " : "no") + vopt.fullname;
      } else if(value instanceof Integer) {
        v = vopt.fullname + "=" + value;
      } else
        assert false : value.getClass().getSimpleName() + " not handled";
      
      return v;
    }
    
    private static void displayAllOptions() {
      ViOutputStream osa = ViManager.createOutputStream(
              null, ViOutputStream.OUTPUT, null);
      for (VimOption vopt : vopts) {
        VimOptionDescriptor voptDesc = new VimOptionDescriptor();
        determineOptionState(vopt, voptDesc);
        osa.println(formatDisplayValue(vopt, voptDesc.value));
      }
      osa.close();
    }
    
    /** Note the value from the current is used to set any others */
    public static void syncAllInstances(String varName) {
      for (VimOption vopt : vopts) {
        if((vopt.flags & P_IND) != 0) {
          
          if(vopt.varName.equals(varName)) {
            VimOptionDescriptor voptDesc = new VimOptionDescriptor();
            determineOptionState(vopt, voptDesc);
            Set<? extends ViOptionBag> set =
                    (vopt.flags & P_WIN) != 0
                      ? ViManager.getViFactory().getViTextViewSet()
                      : ViManager.getViFactory().getBufferSet();
            for (ViOptionBag bag : set) {
              try {
                voptDesc.f.set(bag, voptDesc.value);
              } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
              } catch (IllegalAccessException ex) {
                ex.printStackTrace();
              }
            }
            break;
          }
          
        }
      }
    }
    
  }
  
  private static Pattern mlPat1;
  private static Pattern mlPat2;
  public static void processModelines() {
    if(mlPat1 == null) {
      // vim modelines has two patterns
      mlPat1 = Pattern.compile("\\s+(?:vi:|vim:|ex:)\\s*(.*)");
      mlPat2 = Pattern.compile("\\s+(?:vi:|vim:|ex:)\\s*set? ([^:]*):");
    }
    int mls;
    if(!G.p_ml.value || (mls = G.p_mls.value) == 0)
      return;
    int lnum;
    int lcount = G.curbuf.getLineCount();
    for(lnum = 1; lnum < lcount && lnum <= mls; lnum++) {
      if(checkModeline(lnum))
        mls = 0;
    }
    
    for(lnum = lcount; lnum > 0 && lnum > mls && lnum > lcount - mls; lnum--) {
      if(checkModeline(lnum))
        mls = 0;
    }
  }
  
  /** @return true if parsed a modeline, there may have been errors */
  private static boolean checkModeline(int lnum) {
    // use MySegment for jdk1.5 compatibility
    MySegment seg = Util.ml_get(lnum);
    
    // must check pattern 2 first, since lines that match pattern 2
    // will also match pattern 1
    
    if(parseModeline(mlPat2, seg, lnum))
      return true;
    if(parseModeline(mlPat1, seg, lnum))
      return true;
    return false;
  }
  
  /** @return true if found and parsed a modeline, there may have been errors */
  private static boolean parseModeline(Pattern p, CharSequence cs, int lnum) {
    Matcher m = p.matcher(cs);
    if(!m.find())
      return false;
    boolean parseError = false;
    String mline = m.group(1);
    StringBuilder sb = new StringBuilder();
    try {
      String[] args = mline.split("[:\\s]");
      for (String arg : args) {
        String msg = "";
        try {
          SetCommand.parseSetOption(arg);
        } catch (SetCommand.SetCommandException ex) {
          msg = ex.getMessage();
        } catch (IllegalAccessException ex) {
          ex.printStackTrace();
        }
        if(sb.length() != 0)
          sb.append('\n');
        if(!msg.equals("")) {
          sb.append("Error: ").append(msg);
          parseError = true;
        } else
          sb.append("   OK: ").append(arg);
      }
    } finally {
      String fn = G.curbuf.getDisplayFileName();
      ViOutputStream vos = ViManager.createOutputStream(G.curwin,
                    ViOutputStream.OUTPUT,
                    "In " + fn + ":" + lnum + " process modeline: " + mline,
                    parseError ? ViOutputStream.PRI_HIGH
                               : ViOutputStream.PRI_LOW);
      vos.println(sb.toString());
      if(vos != null)
        vos.close();
    }
    return true;
  }
  
  //////////////////////////////////////////////////////////////////////
  //
  // can_bs is in option in vim
  //
  
  static boolean can_bs(int what) {
    switch(G.p_bs.value) {
      case 2:     return true;
      case 1:     return what != BS_START;
      case 0:     return false;
    }
    assert(false) : "can_bs: ; p_bs bad value";
    return false;
  }
  
  
  //////////////////////////////////////////////////////////////////////
  //
  // Whether or not to highlight depends on a mix of things.
  // Handle the logic here.
  
  static boolean nohDisableHighlight;
  
  static {
    addPropertyChangeListener(highlightSearch, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if(G.curwin != null) {
          nohDisableHighlight = false;
          ViManager.updateHighlightSearchState();
        }
      }
    });

    addPropertyChangeListener(ignoreCase, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if(G.curwin != null) {
          ViManager.updateHighlightSearchState();
        }
      }
    });
  }
  
  public static boolean doHighlightSearch() {
    return G.p_hls.value && !nohDisableHighlight;
  }
  
  static void nohCommand() {
    nohDisableHighlight = true;
    ViManager.updateHighlightSearchState();
  }
  
  static void newSearch() {
    nohDisableHighlight = false;
    ViManager.updateHighlightSearchState();
  }
}

// vi:sw=2 et
