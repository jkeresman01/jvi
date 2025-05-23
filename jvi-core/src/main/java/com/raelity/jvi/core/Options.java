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
package com.raelity.jvi.core;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.core.lib.Mappings;
import com.raelity.jvi.lib.CharTab;
import com.raelity.jvi.lib.Wrap;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.DebugOption;
import com.raelity.jvi.options.OptUtil;
import com.raelity.jvi.options.Option;
import com.raelity.jvi.options.SetColonCommand;
import com.raelity.jvi.lib.MySegment;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.Constants.FDO.*;
import static com.raelity.jvi.core.lib.Constants.NF.*;
import static com.raelity.jvi.lib.TextUtil.sf;

import com.google.common.eventbus.Subscribe;

import com.raelity.jvi.ViOutputStream.COLOR;
import com.raelity.jvi.ViOutputStream.FLAGS;
import com.raelity.jvi.options.*;

import static com.raelity.jvi.options.OptUtil.createBooleanOption;
import static com.raelity.jvi.options.OptUtil.createColorOption;
import static com.raelity.jvi.options.OptUtil.createDebugOption;
import static com.raelity.jvi.options.OptUtil.createEnumSetOption;
import static com.raelity.jvi.options.OptUtil.createEnumStringOption;
import static com.raelity.jvi.options.OptUtil.createIntegerOption;
import static com.raelity.jvi.options.OptUtil.createStringOption;
import static com.raelity.jvi.options.OptUtil.setupOptionDesc;
import static com.raelity.jvi.options.OptUtil.setExpertHidden;
import static com.raelity.jvi.options.Option.failedValidation;

/**
 * Option handling from external sources.
 * <br>
 * Should there be a vi command to set the options to persistent storage,
 * this is useful if want to save after several set commands.
 * <br>
 *
 */

public final class Options {
  private static final Logger LOG = Logger.getLogger(Options.class.getName());
  private Options() {
  }

    public static interface EditControl {
        /** starting edit */
        public void start();
        /** accept an edit */
        public void ok();
        /** cancel an in progress edit */
        public void cancel();
        /** current cached value, possibly from cache */
        public Object getCurrentValue(String name);
    }

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=2)
    public static class Init implements ViInitialization
    {
      @Override
      public void init()
      {

        OptUtil.init();
        Options.init(); // Create all the options
        OptUtil.go();
        ready = true;
      }
    }

  /**
   * Options are grouped into categories. Typically a UI groups the options
   * by category when presenting an options editor.
   */
  public enum Category { PLATFORM, GENERAL, VIMINFO,
                         WINDOW, COLORS, SEARCH,
                         MODIFY, CURSOR_WRAP, PROCESS, DEBUG,
                         NONE }

  public static final String perProjectSupport = "viPerProjectSupport";
  
  public static final String commandEntryFrame = "viCommandEntryFrameOption";
  public static final String redoTrack = "viRedoTrack";
  public static final String pcmarkTrack = "viPCMarkTrack";
  public static final String autoPopupFN = "viAutoPopupFN";
  public static final String autoPopupCcName = "viAutoPopupCcName";
  public static final String coordSkip = "viCoordSkip";
  public static final String platformPreferences = "viPlatformPreferences";
  public static final String platformTab = "viPlatformTab";
  public static final String magicRedoAlgorithm = "viMagicRedoAlgorithm";
  public static final String caretBlinkRate = "viCaretBlinkRate";
  public static final String disableFontError = "viDisableFontError";
  public static final String disableFontCheckSpecial = "viDisableFontCheckSpecial";
  public static final String cursorXorBug = "viCursorXorBug";

  public static final String selectColor = "viSelectColor";
  public static final String selectFgColor = "viSelectFgColor";
  public static final String searchColor = "viSearchColor";
  public static final String searchFgColor = "viSearchFgColor";
  public static final String roCursorColor = "viRoCursorColor";

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
  public static final String clipboard = "viClipboard";
  public static final String joinSpaces = "viJoinSpaces";
  public static final String shiftRound = "viShiftRound";
  public static final String startOfLine = "viStartOfLine";
  public static final String notStartOfLine = "viNotStartOfLine"; // NOT AN OPTION
  public static final String changeWordBlanks = "viChangeWordBlanks";
  public static final String tildeOperator = "viTildeOperator";
  public static final String searchFromEnd = "viSearchFromEnd";
  public static final String endOfSentence = "viEndOfSentence";
  public static final String wrapScan = "viWrapScan";
  public static final String isKeyWord = "viIsKeyWord";

  public static final String magic = "viAaSearchMagic";
  public static final String metaEquals = "viMetaEquals";
  public static final String metaEscape = "viMetaEscape"; // NOT AN OPTION
  public static final String incrSearch = "viIncrSearch";
  public static final String highlightSearch = "viHighlightSearch";

  public static final String ignoreCase = "viIgnoreCase";
  public static final String smartCase = "viSmartCase";
  public static final String platformBraceMatch = "viPlatformBraceMatch";
  
  public static final String expandTabs = "viExpandTabs";

  public static final String report = "viReport";
  public static final String backspace = "viBackspace";
  public static final String scrollOff = "viScrollOff";
  public static final String sideScroll = "viSideScroll";
  public static final String sideScrollOff = "viSideScrollOff";
  public static final String shiftWidth = "viShiftWidth";
  public static final String tabStop = "viTabStop";
  public static final String softTabStop = "viSoftTabStop";
  public static final String textWidth = "viTextWidth";
  public static final String showMode = "viShowMode";
  public static final String showCommand = "viShowCommand";
  public static final String visualBell = "viVisualBell";
  public static final String visualBellTime = "viVisualBellTime";
  public static final String visualBellColor = "viVisualBellColor";
  public static final String cursorInView = "viCursorInView";

  public static final String wrap = "viWrap";
  public static final String list = "viList";
  public static final String lineBreak = "viLineBreak";
  public static final String number = "viNumber";
  public static final String scroll = "viScroll";
  public static final String timeout = "viTimeout";
  public static final String timeoutlen = "viTimeoutLen";
  public static final String equalAlways = "viEqualAlways";
  public static final String splitBelow = "viSplitBelow";
  public static final String splitRight = "viSplitRight";
  public static final String foldOpen = "viFoldOpen";

  public static final String nrFormats = "viNrFormats";
  public static final String matchPairs = "viMatchPairs";
  public static final String quoteEscape = "viQuoteEscape";
  
  public static final String modeline = "viModeline";
  public static final String modelines = "viModelines";
  
  public static final String selection = "viSelection";
  public static final String selectMode = "viSelectMode";

  public static final String equalProgram = "viEqualProgram";
  public static final String formatProgram = "viFormatProgram";

  public static final String shell = "viShell";
  public static final String shellCmdFlag = "viShellCmdFlag";
  public static final String shellXQuote = "viShellXQuote";
  public static final String shellSlash = "viShellSlash";

  public static final String mapCommands = "viMapCommands";

  public static final String history = "viHistory";

  public static final String persistedSearch = "viPersistedSearch";
  public static final String persistedColon = "viPersistedColon";
  public static final String persistedBufMarks = "viPersistedBufMarks";
  public static final String persistedRegLines = "viPersistedRegLines";
  public static final String persistedSize = "viPersistedSize";
  public static final String persistFilemarks = "viPersistFilemarks";
  
  public static final String readOnlyHack = "viReadOnlyHack";
  public static final String classicUndoOption = "viClassicUndo";
  public static final String hideVersionOption = "viHideVersion";

  public static final String tabCompletionPrefix = "viTabCompletionPrefix";
  public static final String closedFiles = "viClosedFiles";
  public static final String selInitComLine = "viSelInitComLine";
  
  public static final String dbgRedo = "viDbgRedo";
  public static final String dbgKeyStrokes = "viDbgKeyStrokes";
  public static final String dbgCache = "viDbgCache";
  public static final String dbgEditorActivation = "viDbgEditorActivation";
  public static final String dbgBang = "viDbgBang";
  public static final String dbgBangData = "viDbgBangData";
  public static final String dbgMouse = "viDbgMouse";
  public static final String dbgCompletion = "viDbgCompletion";
  public static final String dbgCoordSkip = "viDbgCoordSkip";
  public static final String dbgUndo = "viDbgUndo";
  public static final String dbgSearch = "viDbgSearch";
  public static final String dbgOptions = "viDbgOptions";
  public static final String dbgWindowTreeBuilder = "viDbgWindowTreeBuilder";
  public static final String dbgPrefChangeMonitor = "viDbgPrefChangeMonitor";
  public static final String dbgFonts = "viDbgFonts";
  public static final String dbgMarks = "viDbgMarks";
  public static final String dbgBeep = "viDbgBeep";
  public static final String dbgOps = "viDbgOps";

  public static final String twMagic = "#TEXT-WIDTH#";

  // some enum values

  // CommandEntryFrame
  public static final String CEF_APP_MODAL = "Application Modal";
  public static final String CEF_DOC_MODAL = "Document Modal";
  public static final String CEF_GLASS_PANE = "Glass Pane";
  
  public static final String MSG_VERY_MAGIC = "Very Magic";
  public static final String MSG_MAGIC = "Magic";
  public static final String MSG_NO_MAGIC = "No Magic";
  public static final String MSG_VERY_NO_MAGIC = "Very No Magic";
  
  public static final String MESC_VERY_MAGIC = "vm";
  public static final String MESC_MAGIC = "m";
  public static final String MESC_NO_MAGIC = "nm";
  public static final String MESC_VERY_NO_MAGIC = "vnm";

  public static boolean isReady()
  {
    return ready;
  }

  private static final String space4 = "\u00a0\u00a0\u00a0\u00a0";

  private static boolean ready = false;
  private static boolean didInit = false;
  private static void init() {
    if(didInit) {
      return;
    }
    didInit = true;

    String MinMinus1 = "Only '-1 <= val' allowed. Not '%d'.";
    String ToTenK = "Only '0 <= val <= 10000' allowed. Not '%d'.";

    // Since this is used to debug options, put it first

    G.dbgOptions = createDebugOption(dbgOptions);
    setupOptionDesc(Category.DEBUG, dbgOptions, "debug options set", "");
    
    // Some options that do not appear in the dialog property sheets.

    createIntegerOption(scroll, 0);
    setupOptionDesc(null, scroll, "'scroll' 'scr'", "");

    //
    // Put this in GENERAL, but mark it hidden.
    // It is handled very specially.
    //
    createStringOption(mapCommands,
        """
        " These two mappings, which apply to PLATFORM-SELECT only,
        " get 'y' and 'p' to work with the mouse selection.
        " pnoremap y vy
        " pnoremap p vp
        
        # These mapping are handy if line wrap is turned on.
        # With them, j,k,$,0,^ behave visually as usual.
        # On wrapped lines the cursor will not change visual line.
        # The arrow keys and <Home>, <End> still behave as usual,
        # they could be mapped as well.
        # Uncomment them and try them out with wrapped lines.
        
        " map j gj
        " map k gk
        " map $ g$
        " map 0 g0
        " map ^ g^""",
            (val) -> {
                Wrap<String> emsg = new Wrap<>("");
                if(null == Mappings.parseMapCommands(val, emsg)) {
                  failedValidation(emsg.getValue());
                }});
    setupOptionDesc(Category.GENERAL, mapCommands, "Map Commands", sf("""
        map-cmd {lhs} {rhs}
        [nvop]map, [nvop]noremap and [nvop]unmap commands supported \
        (only normal mode mappings).
        pmap is jVi only, when PLATFORM-SELECT.
        Comments are on a line by themselves and start with '"' or '#'.
        In lhs or rhs a char is of the form:
        %s"c"           - except \\ and < and space
        %s"<C-X>"       - except Ctrl-\\
        %s"<special>"   - see jVi doc for valid specials
        %s"<C-special>" "<S-special>"
        Some specials: <lt>,<Space>,<Bslash>,<Up>,<Home>""",
        space4, space4, space4, space4));
    setExpertHidden(mapCommands, true, true);

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Debug Options
    //
    //

    // debug options first

    G.dbgEditorActivation = createDebugOption(dbgEditorActivation);
    setupOptionDesc(Category.DEBUG, dbgEditorActivation, "debug activation",
               "Output info about editor switching between files/windows");

    kd = createDebugOption(dbgKeyStrokes);
    setupOptionDesc(Category.DEBUG, dbgKeyStrokes, "debug KeyStrokes",
               "Output info for each keystroke");

    G.dbgRedo = createDebugOption(dbgRedo);
    setupOptionDesc(Category.DEBUG, dbgRedo, "debug redo buffer",
               "Output info on magic/tracking changes to redo buffer");

    createDebugOption(dbgCache);
    setupOptionDesc(Category.DEBUG, dbgCache, "debug cache",
               "Output info on text/doc cache");

    createDebugOption(dbgBang);
    setupOptionDesc(Category.DEBUG, dbgBang, "debug \"!\" cmds",
               "Output info about external processes");

    createDebugOption(dbgBangData);
    setupOptionDesc(Category.DEBUG, dbgBangData, "debug \"!\" cmds data",
               "Output data tranfers external processes");

    createDebugOption(dbgCompletion);
    setupOptionDesc(Category.DEBUG, dbgCompletion, "debug Completion",
               "Output info on completion, eg FileName.");

    G.dbgMouse = createDebugOption(dbgMouse);
    setupOptionDesc(Category.DEBUG, dbgMouse, "debug mouse events",
               "Output info about mouse events");

    G.dbgCoordSkip = createDebugOption(dbgCoordSkip);
    setupOptionDesc(Category.DEBUG, dbgCoordSkip,
                            "debug coordinate skip", "");

    G.dbgUndo = createDebugOption(dbgUndo);
    setupOptionDesc(Category.DEBUG, dbgUndo, "debug undo begin/end", "");

    G.dbgSearch = createDebugOption(dbgSearch);
    setupOptionDesc(Category.DEBUG, dbgSearch, "debug search", "");

    G.dbgWindowTreeBuilder = createDebugOption(dbgWindowTreeBuilder);
    setupOptionDesc(Category.DEBUG, dbgWindowTreeBuilder, "debug window tree builder", "");

    G.dbgPrefChangeMonitor = createDebugOption(dbgPrefChangeMonitor);
    setupOptionDesc(Category.DEBUG, dbgPrefChangeMonitor, "debug pref change monitor", "");

    G.dbgFonts = createDebugOption(dbgFonts);
    setupOptionDesc(Category.DEBUG, dbgFonts, "debug font issues", "");

    G.dbgMarks = createDebugOption(dbgMarks);
    setupOptionDesc(Category.DEBUG, dbgMarks, "debug marks", "");

    G.dbgBeep = createDebugOption(dbgBeep);
    setupOptionDesc(Category.DEBUG, dbgBeep, "debug beep outputs stack", "");

    G.dbgOps = createDebugOption(dbgOps);
    setupOptionDesc(Category.DEBUG, dbgOps, "debug ops/clip/registers", "");
    
    /////////////////////////////////////////////////////////////////////
    //
    //
    // Platform Options
    //
    //
    
    // platformList.add("jViVersion"); // hard coded in OptUtil.init


    createEnumStringOption(selInitComLine, SICL.EMPTY.toString(),
                new String[] {SICL.EMPTY.toString(),
                              SICL.COMMAND.toString(),
                              SICL.SELECTED.toString()});
    setupOptionDesc(Category.PLATFORM, selInitComLine,
                    "Select Initial CommandLine 'sicl'",
                    "This controls the initial content of the command line.\n"
                  + space4 + "empty - an empty command line window\n"
                  + space4 + "command - the last executed command\n"
                  + space4 + "selected - select the last executed command\n"
                  + """
                  With "command" or "selected", pressing ENTER executes \
                  the last command. With "selected" a key entry replaces \
                  the selection and starts a new command With "empty" or \
                  "command" the selection clipboard is not modified. With \
                  "empty", when the command line comes up, pressing up \
                  arrow shows the previous command.""");

    createBooleanOption(perProjectSupport, true);
    setupOptionDesc(Category.PLATFORM, perProjectSupport,
                    "Support \"Project specific options\"", """
            In NetBeans' project properties, under "Formatting", \
            there is a "Use project specific options" radio button. \
            When this jVi option is enabled and "Use project specific \
            options" is enabled, some of the per project settings \
            override the jVi settings; these are 'expandtab', \
            'shiftwidth' and 'tabstop'""");
    setExpertHidden(perProjectSupport, true, false);

    createBooleanOption(redoTrack, true);
    setupOptionDesc(Category.PLATFORM, redoTrack, "\".\" magic redo tracking", """
            Track magic document changes during input mode for the \
            "." commnad. These changes are often the result of IDE \
            code completion""");

    createBooleanOption(pcmarkTrack, true);
    setupOptionDesc(Category.PLATFORM, pcmarkTrack, """
                    "``" magic pcmark tracking""", """
                    Track magic cursor movments for use by the "``" \
                    and "''" commands. These movement are often the result \
                    of IDE actions invoked external to jVi.""");
    
    createBooleanOption(hideVersionOption, false);
    setupOptionDesc(Category.PLATFORM, hideVersionOption, "hide version", """
            When true, display of initial version information does \
            not bring up output window.""");
    setExpertHidden(hideVersionOption, true, false);
    
    createEnumStringOption(commandEntryFrame , CEF_APP_MODAL,
            new String[] {CEF_APP_MODAL, CEF_DOC_MODAL, CEF_GLASS_PANE});
    setupOptionDesc(Category.PLATFORM, commandEntryFrame,
            "command line modality", """
                Modality for command/search entry windows.
                APPLICATION MODAL is recommended.
                NOTE: change takes affect after restart.
                
                This option is provided due to problems with dialog \
                modality on some OS. The third option uses a non modal \
                command line on a glass pane.""");
    setExpertHidden(commandEntryFrame, true, false);
    
    createBooleanOption(autoPopupFN, true);
    setupOptionDesc(Category.PLATFORM, autoPopupFN,
                    "\":e [#|fname]\" Completion Auto Popup", """
              When doing ":" command line entry, if "e#" \
              or "e fNameChar" is entered then automatically \
              popup a file name completion window.

              NOTE: Otherwise use Ctrl-D, and/or platform specific \
              key sequence, to pop up the completion window.""");
    
    createBooleanOption(autoPopupCcName, true);
    setupOptionDesc(Category.PLATFORM, autoPopupCcName,
                            "\":\" Command Completion Auto Popup", """
              After doing ":" for command line entry, automatically \
              popup command name completion.

              NOTE: Otherwise use Ctrl-D, and/or platform specific \
              key sequence, to pop up the completion window.""");
    setExpertHidden(autoPopupCcName, false, false);

    createBooleanOption(coordSkip, true);
    setupOptionDesc(Category.PLATFORM, coordSkip, "Code Folding Compatible", """
            When false revert some navigation algorithms, e.g. ^F, \
            to pre code folding behavior. A just in case option; if \
            needed, please file a bug report.""");
    setExpertHidden(coordSkip, true, false);

    createBooleanOption(platformPreferences, false);
    setupOptionDesc(Category.PLATFORM, platformPreferences,
                    "Store init (\"vimrc\") with Platform", """
            Store user preferences/options in platform location. \
            Change occurs after next application startup. For \
            example, on NetBeans store in userdir. NOTE: except \
            for the first switch to platform, changes made in one \
            area are not propogated to the other.""");
    setExpertHidden(platformPreferences, true, true);

    createBooleanOption(platformTab, false);
    setupOptionDesc(Category.PLATFORM, platformTab,
            "Use the platform's TAB handling", """
            When false, jVi processes the TAB character according \
            to the expandtab and softtabstop options. Otherwise the \
            TAB is passed to the platform, e.g. IDE, for handling. \
            The only reason to set this true is if a bug is \
            discovered in the jVi tab handling.""");
    setExpertHidden(platformTab, true, false);

    createEnumStringOption(magicRedoAlgorithm, "anal",
            new String[] {"anal", "guard"});
    setupOptionDesc(Category.PLATFORM,
                            magicRedoAlgorithm, "magic redo algorithm", """
              Which algorithm to use to capture code completion changes for \
              use in a subsequent '.' (redo) command. None is perfect.
              
              The 'anal' algorithm looks at each document change, analizes \
              it and adjusts the redo buffer.
              
              The 'guard' algorithm places marks around the insertion point \
              and captures that as the change; this is currently experimental, \
              but handles some single line cases better; simpler algorithm.""");
    setExpertHidden(magicRedoAlgorithm, true, false);

    createIntegerOption(caretBlinkRate, 300);
    setupOptionDesc(Category.PLATFORM,
                            caretBlinkRate, "caret blink rate", """
            This determines if and how fast the caret blinks. If \
            this is zero the caret will not blink""");

    createBooleanOption(disableFontError, false);
    setupOptionDesc(Category.PLATFORM, disableFontError,
                    "Font Check disable Problem Dialog", """
            If a font size problem is detected, don't bring up \
            a dialog. No matter how this is set, the error is \
            reported in the output window""");

    createBooleanOption(disableFontCheckSpecial, true);
    setupOptionDesc(Category.PLATFORM,
                            disableFontCheckSpecial,
                            "Font Check ignore special chars", """
            By default all characters are used to determine font \
            width. Some fonts have special characters, unicode \
            u0000 to u001f, that are a different width from standard \
            chars. Use this option to ignore the special chars \
            when checkingfor font size problems.""");

    createBooleanOption(cursorXorBug, true);
    setupOptionDesc(Category.PLATFORM,
                            cursorXorBug,
                            "Disable Cursor Xor", """
            jVi can use graphics xor when drawing the cursor. On \
            several systems there is a problem with xor draw mode; \
            the symptom is that the cursor is not visible. Set \
            this option to false to use xor mode.""");
    setExpertHidden(cursorXorBug, true, false);

    createBooleanOption(tabCompletionPrefix, true);
    setupOptionDesc(Category.PLATFORM,
                            tabCompletionPrefix,
                            "TAB inserts common prefix", """
            This option doesn't work (yet), in the meantime add
            -J-Dorg.netbeans.modules.editor.completion.noTabCompletion=true
            in netbeans.conf

            Use the TAB character during command line completion to complete \
            the common prefix from the list. Otherwise, TAB may finish/close \
            the completion list popup.

            NOTE: change takes affect after restart.""");
    setExpertHidden(tabCompletionPrefix, false, false);

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Viminfo Options
    //
    //

    createIntegerOption(history, 200, (val) -> {
                          if(val < 0 || val > 10000) {
                            failedValidation(sf(ToTenK, val));
                          }});
    setupOptionDesc(Category.VIMINFO, history, "'history' 'hi'","""
            A history of ':' commands, and a history of previous search \
            patterns is remembered.  This option decides how many entries \
            may be stored in each of these histories (see |cmdline-editing|).
    
            The maximum value is 10000.""");

    createIntegerOption(persistedSearch, -1, (val) -> {
                          if(val < -1) {
                            failedValidation(sf(MinMinus1, val));
                          }});
    setupOptionDesc(Category.VIMINFO, persistedSearch, "max Search Patterns", """
            Maximum number of items in the search pattern history to save.
            When set to '-1', the value of 'history' is used.""");

    createIntegerOption(persistedColon, -1, (val) -> {
                          if(val < -1) {
                            failedValidation(sf(MinMinus1, val));
                          }});
    setupOptionDesc(Category.VIMINFO, persistedColon, "max CommandLine", """
            Maximum number of items in the command-line history to save.

            When set to '-1', the value of 'history' is used.""");

    createIntegerOption(closedFiles, 200, (val) -> {
                          if(val < 0 || val > 10000) {
                            failedValidation(sf(ToTenK, val));
                          }});
    setupOptionDesc(Category.VIMINFO, closedFiles, "'closedfiles'", """
            An MRU sorted list of closed files, used by the 'e*' command, \
            is remembered between sessions.  This option determines how \
            many entries to persist to/from backing store.
    
            The maximum value is 10000.""");

    createIntegerOption(persistedBufMarks, 100);
    setupOptionDesc(Category.VIMINFO, persistedBufMarks, "max buf-marks", """
            Maximum number of previously edited files for which the marks \
            are remembered. Set to 0 and no marks are persisted.""");

    createIntegerOption(persistedRegLines, 50, (val) -> {
                          if(val < -1) {
                            failedValidation(sf(MinMinus1, val));
                          }});
    setupOptionDesc(Category.VIMINFO, persistedRegLines, "max reg-lines", """
            Maximum number of lines saved for each regster. If zero then \
            registers are not saved. When -1, all lines are saved. Also \
            see the 'viminfo: max size': limit in Kbyte.""");

    createIntegerOption(persistedSize, 10);
    setupOptionDesc(Category.VIMINFO, persistedSize, "max size", """
            Maximum size of an item in Kbyte. If zero then registers are \
            not saved. Currently only applies to registers. The default, \
            '10' excludes registers with more that 10 Kbyte of text. Also \
            see 'viminfo: max reg-lines': line count limit.""");

    createBooleanOption(persistFilemarks, true);
    setupOptionDesc(Category.VIMINFO, persistFilemarks, "save Filemarks",
            "Whether to save Filemarks.");


    /////////////////////////////////////////////////////////////////////
    //
    //
    // Colors Options
    //
    //
  
  String NOTE_USE_LOOKANDFEEL = """
          
          NOTE: press 'X', then the platform LookAndFeel/component \
          provides the value as follows:""";

    createColorOption(searchColor, new Color(0xffb442), true, true); //a light orange
    setupOptionDesc(Category.COLORS, searchColor, "'hl-search' background color",
            "The background color used for search highlight."
                    + NOTE_USE_LOOKANDFEEL
    );

    createColorOption(searchFgColor, new Color(0x000000), true, true);
    setupOptionDesc(Category.COLORS, searchFgColor, "'hl-search' foreground color",
            "The character color used for search highlight."
                    + NOTE_USE_LOOKANDFEEL
    );
    setExpertHidden(searchFgColor, false, false);
    
    createColorOption(selectColor, new Color(0xffe588), true, true); //a light orange
    setupOptionDesc(Category.COLORS, selectColor, "'hl-visual' background color",
            "The background color used for a visual mode selection."
                    + NOTE_USE_LOOKANDFEEL
    );

    createColorOption(selectFgColor, new Color(0x000000), true, true);
    setupOptionDesc(Category.COLORS, selectFgColor, "'hl-visual' foreground color",
            "The character color used for a visual mode selection foreground."
                    + NOTE_USE_LOOKANDFEEL
    );
    setExpertHidden(selectFgColor, false, false);

    createColorOption(roCursorColor, Color.red, true, false);
    setupOptionDesc(Category.COLORS, roCursorColor, "'rocursorcolor' 'rocc'", """
            The cursor color in a read only editor. If 'null' then use \
            editor's default cursor color""");

    createColorOption(visualBellColor, new Color(0x41e7e7), true, false);
    setupOptionDesc(Category.COLORS, visualBellColor,
                            "'visualbellcolor' 'vbc'", """
            The color used for the visual bell, the editor's background \
            is set to this color. If null, then the editor's background \
            color is inverted""");

    /////////////////////////////////////////////////////////////////////
    //
    //
    // General Options
    //
    //

    createIntegerOption(scrollOff, 0);
    setupOptionDesc(Category.GENERAL, scrollOff, "'scrolloff' 'so'", """
            Visible context around cursor (scrolloff)\tMinimal number \
            of screen lines to keep above and below the cursor. This \
            will make some context visible around where you are working. \
            If you set it to a very large value (999) the cursor line \
            will always be in the middle of the window (except at the \
            start or end of the file)""");

    createIntegerOption(sideScroll, 0);
    setupOptionDesc(Category.GENERAL, sideScroll, "'sidescroll' 'ss'", """
            The minimal number of columns to scroll horizontally. \
            Usedonly when the 'wrap' option is off and the cursor \
            is movedoff of the screen. When it is zero the cursor \
            will be putin the middle of the screen.Not used for "zh" \
            and "zl" commands.""");
    setExpertHidden(sideScroll, false, true);

    createIntegerOption(sideScrollOff, 0);
    setupOptionDesc(Category.GENERAL, sideScrollOff, "'sidescrolloff' 'siso'", """
            The minimal number of screen columns to keep to the left \
            and to the right of the cursor if 'nowrap' is set.""");
    setExpertHidden(sideScrollOff, false, true);
    
    createBooleanOption(showMode, true);
    setupOptionDesc(Category.GENERAL, showMode, "'showmode' 'smd'",
            "If in Insert or Replace mode display that information.");
    
    createBooleanOption(showCommand, true);
    setupOptionDesc(Category.GENERAL, showCommand, "'showcmd' 'sc'",
            "Show (partial) command in status line.");

    createIntegerOption(report, 2);
    setupOptionDesc(Category.GENERAL, report, "'report'", """
            Threshold for reporting number of lines changed.  When \
            the number of changed lines is more than 'report' a \
            message will be given for most ":" commands.  If you \
            want it always, set 'report' to 0.  For the ":substitute" \
            command the number of substitutions is used instead of \
            the number of lines.""");
    
    createBooleanOption(modeline, true);
    setupOptionDesc(Category.GENERAL, modeline, "'modeline' 'ml'", sf("""
            Enable/disable modelines option.
            [text]{white}{vi:|vim:|ex:}[white]{options}
            %sexample: vi:noai:sw=3 ts=6
            [text]{white}{vi:|vim:|ex:}[white]se[t] {options}:[text]
            %sexample: /* vim: set ai tw=75: */""", space4, space4));
    
    createIntegerOption(modelines, 5);
    setupOptionDesc(Category.GENERAL, modelines, "'modelines' 'mls'", """
            If 'modeline' is on 'modelines' gives the number of lines \
            that is checked for set commands.  If 'modeline' is off or \
            'modelines' is zero no lines are checked.""");

    createEnumSetOption(clipboard, EnumSet.noneOf(CBU.class), CBU.class, null);
    setupOptionDesc(Category.GENERAL, clipboard,
               "'clipboard' 'cb'",
               "use unnamed/unnamedplus clipboard for yank, delete, put");

    createBooleanOption(startOfLine, true);
    setupOptionDesc(Category.GENERAL, startOfLine, "'startofline' 'sol'", """
            When "on" the commands listed below move the cursor to the \
            first non-blank of the line.  When off the cursor is kept \
            in the same column(if possible).  This applies to the \
            commands: CTRL-D, CTRL-U, CTRL-B, CTRL-F, "G", "H", "M", \
            "L", gg, and to the commands "d", "<<" and ">>" with a \
            linewise operator, with "%" with a count and to buffer \
            changing commands (CTRL-^, :bnext, :bNext, etc.). Also \
            for an Ex command that only has a line number, e.g., ":25" \
            or ":+".In case of buffer changing commands the cursor is \
            placed at the column where it was the last time the buffer \
            was edited.""");

    createEnumStringOption(selection, "inclusive",
            new String[] {"old", "inclusive", "exclusive"});
    setupOptionDesc(Category.GENERAL, selection, "'selection' 'sel'", """
            This option defines the behavior of the selection. It is \
            only used in Visual and Select mode. Possible values: \
            'old', 'inclusive', 'exclusive'""");
    setExpertHidden(selection, false, false);
    
    createEnumStringOption(selectMode, "",
            new String[] {"mouse", "key", "cmd"});
    setupOptionDesc(Category.GENERAL, selectMode, "'selectmode' 'slm'", """
            This is a comma separated list of words, which specifies \
            when to start Select mode instead of Visual mode, when a \
            selection is started. Possible values: 'mouse', key' or 'cmd'""");
    setExpertHidden(selectMode, true, true);

    createBooleanOption(timeout, true);
    setupOptionDesc(Category.GENERAL, timeout, "'timeout' 'to'", """
            Enables timeout when part of a mapped key sequence has \
            been received. After that the already received characters \
            are interpreted as single characters.  The waiting time \
            can be changed with the 'timeoutlen' option.""");

    createIntegerOption(timeoutlen, 1000);
    setupOptionDesc(Category.GENERAL, timeoutlen, "'timeoutlen' 'tm'", """
            The time in milliseconds that is waited for a mapped \
            key sequence to complete.""");

    createEnumSetOption(foldOpen,
            EnumSet.of(
                FDO_BLOCK, FDO_HOR, FDO_MARK, FDO_PERCENT, FDO_QUICKFIX,
                FDO_SEARCH, FDO_TAG, FDO_UNDO),
            FDO.class, null);
    setupOptionDesc(Category.GENERAL, foldOpen, "'foldopen' 'fdo'", """
            Specifies for which type of commands folds will be opened, \
            if the command moves the cursor into a closed fold.""");

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Window Options
    //
    //

    createBooleanOption(wrap, true);
    setupOptionDesc(Category.WINDOW, wrap, "'wrap'", """
            This option changes how text is displayed. When on, \
            lines longer than the width of the window will wrap \
            and displaying continues on the next line.  When off \
            lines will not wrap and only part of long lines will be displayed.
    
            The line will be broken in the middle of a word if necessary. \
            See 'linebreak' to get the break at a word boundary.""");

    createBooleanOption(lineBreak, false);
    setupOptionDesc(Category.WINDOW, lineBreak, "'linebreak' 'lbr'", """
            If on Vim will wrap long lines at a word boundary rather \
            than at the last character that fits on the screen.""");

    createBooleanOption(visualBell, true);
    setupOptionDesc(Category.WINDOW, visualBell, "'visualbell' 'vb'", """
            Use visual bell instead of beeping.  The editor window \
            background is inverted for a period of time,  see 'vbt' \
            option. When no beep or flash is wanted, set time to zero.""");

    createIntegerOption(visualBellTime, 20);
    setupOptionDesc(Category.WINDOW, visualBellTime,
                    "'visualbelltime' 'vbt'", """
            The duration, in milliseconds, of the 'visual bell'. \
            If the visual bell is enabled, see 'vb', and the 'vbt' \
            value is zero then there is no beep or flash.""");

    createBooleanOption(number, false);
    setupOptionDesc(Category.WINDOW, number, "'number' 'nu'",
            "Print the line number in front of each line.");

    createBooleanOption(list, false);
    setupOptionDesc(Category.WINDOW, list, "'list'", """
             List mode. Useful to see the difference between tabs
             and spaces and for trailing blanks.""");

    createBooleanOption(equalAlways, true);
    setupOptionDesc(Category.WINDOW, equalAlways, "'equalalways' 'ea'", """
            When on, all the windows are automatically made the same \
            size after splitting or closing a window.""");

    createBooleanOption(splitBelow, false);
    setupOptionDesc(Category.WINDOW, splitBelow, "'splitbelow' 'sb'", """
            When on, splitting a window will put the new window \
            below the current one.""");

    createBooleanOption(splitRight, false);
    setupOptionDesc(Category.WINDOW, splitRight, "'splitright' 'spr'", """
            When on, splitting a window will put the new window right \
            of the current one.""");

    createBooleanOption(cursorInView, true);
    setupOptionDesc(Category.WINDOW, cursorInView, "'cursorinview' 'civ'", """
            When on, follow the vim behavior; if the scrollbar or \
            scrollwheel change the view move the cursor to keep it \
            visible in the view. When this option is off allow the \
            cursor to scroll out of view""");


    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi Options that affect modifications to the document
    //
    //

    createBooleanOption(tildeOperator, false);
    setupOptionDesc(Category.MODIFY, tildeOperator , "'tildeop' 'top'",
               "tilde \"~\" acts like an operator, e.g. \"~w\" works");

    createBooleanOption(changeWordBlanks, true);
    setupOptionDesc(Category.MODIFY, changeWordBlanks, "'cpoptions' 'cpo' \"w\"",
               "\"cw\" affects sequential white space");

    createBooleanOption(joinSpaces, true);
    setupOptionDesc(Category.MODIFY, joinSpaces, "'joinspaces' 'js'",
               "\"J\" inserts two spaces after a \".\", \"?\" or \"!\"");

    createBooleanOption(shiftRound, false);
    setupOptionDesc(Category.MODIFY, shiftRound, "'shiftround' 'sr'",
               "\"<\" and \">\" round indent to multiple of shiftwidth");

    OptUtil.createEnumIntegerOption(backspace, 0, new Integer[] { 0, 1, 2});
    setupOptionDesc(Category.MODIFY, backspace, "'backspace' 'bs'", """
            Influences the working of <BS>, <Del> during insert.
             0 - no special handling.
             1 - allow backspace over <EOL>.
             2 - allow backspace over start of insert.""");

    /////////////////////////
    //
    // per buffer options are accessed through curbuf.
    //
    
    /*G.b_p_et = */createBooleanOption(expandTabs, false);
    setupOptionDesc(Category.MODIFY, expandTabs, "'expandtab' 'et'", """
            In Insert mode: Use the appropriate number of spaces to \
            insert a <Tab>. Spaces are used in indents with the '>' \
            and '<' commands.""");

    /*G.b_p_sw = */createIntegerOption(shiftWidth, 8);
    setupOptionDesc(Category.MODIFY, shiftWidth, "'shiftwidth' 'sw'", """
            Number of spaces to use for each step of indent. Used for \
            '>>', '<<', etc.""");

    /*G.b_p_ts = */createIntegerOption(tabStop, 8);
    setupOptionDesc(Category.MODIFY, tabStop, "'tabstop' 'ts'",
            "Number of spaces that a <Tab> in the file counts for.");

    /*G.b_p_sts = */createIntegerOption(softTabStop, 0);
    setupOptionDesc(Category.MODIFY, softTabStop, "'softtabstop' 'sts'", """
            Number of spaces that a <Tab> in the file counts for while \
            performing editing operations, like inserting a <Tab> or \
            using <BS>. It "feels" like <Tab>s are being inserted, while \
            in fact a mix of spaces and <Tab>s is used (<Tabs>s only if \
            'expandtabs' is false).  When 'sts' is zero, this feature is \
            off. If 'softtabstop' is non-zero, a <BS> will try to delete \
            as much white space to move to the previous 'softtabstop' \
            position.""");

    createIntegerOption(textWidth, 79);
    setupOptionDesc(Category.MODIFY, textWidth, "'textwidth' 'tw'",
            "This option currently only used in conjunction with the"
            + " 'gq' and 'Q' format command. This value is substituted"
            + " for " + twMagic + " in formatprg option string.");

    createEnumSetOption(nrFormats,
        EnumSet.of(NF_HEX, NF_OCTAL), NF.class, null);
    setupOptionDesc(Category.MODIFY, nrFormats, "'nrformats' 'nf'", """
            Defines bases considered for numbers with the 'CTRL-A' and \
            'CTRL-X' commands for adding to and subtracting from a number \
            respectively. Value is comma separated list; 'octal,hex,alpha' \
            is all possible values.""");
    
    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi searching options
    //
    //
    
    final String fmtMagic
            = "\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0%s - %s: %s\n";
    createEnumStringOption(magic , MESC_MAGIC,
            new String[] {MESC_VERY_MAGIC, MESC_MAGIC,
              MESC_NO_MAGIC, MESC_VERY_NO_MAGIC});
    setupOptionDesc(Category.SEARCH, magic, "Search magic", sf("""
            NOTE: Use \\v, \\m, \\M, \\V within a pattern to switch \
            handling for that pattern. This option specifies which \
            characters need to be escaped in a search (regular expression) \
            pattern by default.
            %s%s%s%s\
            See jVi pattern docs for more information.""",
            sf(fmtMagic, MESC_VERY_MAGIC, MSG_VERY_MAGIC,
                  "None - pass through to reg exp engine"),
            sf(fmtMagic, MESC_MAGIC, MSG_MAGIC,
                  Search.MAGIC),
            sf(fmtMagic, MESC_NO_MAGIC, MSG_NO_MAGIC,
                  Search.NO_MAGIC),
            sf(fmtMagic, MESC_VERY_NO_MAGIC, MSG_VERY_NO_MAGIC,
                  Search.VERY_NO_MAGIC)));

    createBooleanOption(incrSearch, true);
    setupOptionDesc(Category.SEARCH, incrSearch, "'incsearch' 'is'", """
            While typing a search command, show where the pattern, as \
            it was typed so far, matches. If invalid pattern, no match \
            or abort then the screen returns to its original location. \
            You still need to finish the search with <ENTER> or abort \
            it with <ESC>.""");
    
    createBooleanOption(highlightSearch, true);
    setupOptionDesc(Category.SEARCH, highlightSearch, "'hlsearch' 'hls'", """
            When there is a previous search pattern, highlight \
            all its matches""");

    createBooleanOption(ignoreCase, false);
    setupOptionDesc(Category.SEARCH, ignoreCase, "'ignorecase' 'ic'",
            "Ignore case in search patterns.");
    
    createBooleanOption(smartCase, false);
    setupOptionDesc(Category.SEARCH, smartCase, "'smartcase' 'scs'", """
            Override the 'ignorecase' option if the search pattern \
            contains upper case characters.""");

    createBooleanOption(wrapScan, true);
    setupOptionDesc(Category.SEARCH, wrapScan, "'wrapscan' 'ws'",
               "Searches wrap around the end of the file.");

    createBooleanOption(searchFromEnd, true);
    setupOptionDesc(Category.SEARCH, searchFromEnd, "'cpoptions' 'cpo' \"c\"",
               "search continues at end of match");

    createBooleanOption(endOfSentence, false);
    setupOptionDesc(Category.SEARCH, endOfSentence, "'cpoptions' 'cpo' \"j\"", """
            A sentence has to be followed by two spaces after the \
            '.', '!' or '?'.  A <Tab> is not recognized as white space.""");

    createBooleanOption(platformBraceMatch, true);
    setupOptionDesc(Category.SEARCH, platformBraceMatch,
                    "Platform Brace Matching", """
            Use the platform/IDE for brace matching and match \
            highlighting. This may enable additional match \
            characters, words and features.""");
    setExpertHidden(platformBraceMatch, true, false);
    
    createStringOption(isKeyWord, "@,48-57,_,192-255", (val) -> {
                         CharTab ct = new CharTab();
                         if(!ct.init(val)) {
                           failedValidation("parse of '" + val + "' failed.");
                         }});
    setupOptionDesc(Category.SEARCH, isKeyWord, "'iskeyword' 'isk'", """
            Keywords are used in searching and recognizing with many \
            commands: "w", "*", etc. See vim docs for more info. The \
            ":set iskeyword=xxx" command is per buffer and this works \
            with modelines.""");

    /////////////////////////////////////////////////////////////////////
    //
    //
    // Vi cursor wrap options
    //
    //
    createBooleanOption(backspaceWrapPrevious, true);
    setupOptionDesc(Category.CURSOR_WRAP, backspaceWrapPrevious,
               "'whichwrap' 'ww'  b - <BS>",
               "<backspace> wraps to previous line");

    createBooleanOption(hWrapPrevious, false);
    setupOptionDesc(Category.CURSOR_WRAP, hWrapPrevious,
               "'whichwrap' 'ww'  h - \"h\"",
               "\"h\" wraps to previous line (not recommended, see vim doc)");

    createBooleanOption(leftWrapPrevious, false);
    setupOptionDesc(Category.CURSOR_WRAP, leftWrapPrevious,
               "'whichwrap' 'ww'  < - <Left>",
               "<left> wraps to previous line");

    createBooleanOption(spaceWrapNext, true);
    setupOptionDesc(Category.CURSOR_WRAP, spaceWrapNext,
               "'whichwrap' 'ww'  s - <Space>",
               "<space> wraps to next line");

    createBooleanOption(lWrapNext, false);
    setupOptionDesc(Category.CURSOR_WRAP, lWrapNext,
               "'whichwrap' 'ww'  l - \"l\"",
               "\"l\" wraps to next line (not recommended, see vim doc)");

    createBooleanOption(rightWrapNext, false);
    setupOptionDesc(Category.CURSOR_WRAP, rightWrapNext,
               "'whichwrap' 'ww'  > - <Right>",
               "<right> wraps to next line");

    createBooleanOption(tildeWrapNext, false);
    setupOptionDesc(Category.CURSOR_WRAP, tildeWrapNext,
               "'whichwrap' 'ww'  ~ - \"~\"",
               "\"~\" wraps to next line");

    createBooleanOption(insertLeftWrapPrevious, false);
    setupOptionDesc(Category.CURSOR_WRAP, insertLeftWrapPrevious,
               "'whichwrap' 'ww'  [ - <Left>",
               "in Insert Mode <Left> wraps to previous line");

    createBooleanOption(insertRightWrapNext, false);
    setupOptionDesc(Category.CURSOR_WRAP, insertRightWrapNext,
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
    String defaultFlag;

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

    createStringOption(shell, defaultShell);
    setupOptionDesc(Category.PROCESS, shell, "'shell' 'sh'", """
            Name of shell to use for ! and :! commands. (default \
            $SHELL or "sh", MS-DOS and Win32: "command.com" or \
            "cmd.exe").  When changing also check 'shellcmndflag'.""");

    createStringOption(shellCmdFlag, defaultFlag);
    setupOptionDesc(Category.PROCESS, shellCmdFlag, "'shellcmdflag' 'shcf'", """
            Flag passed to shell to execute "!" and ":!" commands; \
            e.g., "bash.exe -c ls" or "command.com /c dir" (default: \
            "-c", MS-DOS and Win32, when 'shell' does not contain "sh" \
            somewhere: "/c").""");

    createStringOption(shellXQuote, defaultXQuote);
    setupOptionDesc(Category.PROCESS, shellXQuote, "'shellxquote' 'sxq'", """
            Quoting character(s), put around the commands passed \
            to the shell, for the "!" and ":!" commands (default: \
            ""; for Win32, when 'shell' contains "sh" somewhere: "\\"").""");
    
    createBooleanOption(shellSlash, false);
    setupOptionDesc(Category.PROCESS, shellSlash, "'shellslash' 'ssl'", """
            When set, a forward slash is used when expanding \
            file names. This is useful when a Unix-like shell \
            is used instead of command.com or cmd.exe.""");
    
    createStringOption(equalProgram, "");
    setupOptionDesc(Category.PROCESS, equalProgram, "'equalprg' 'ep'", """
            External program to use for "=" command (default ""). \
            When this option is empty the internal formatting \
            functions are used.""");

    createStringOption(formatProgram, "");
    setupOptionDesc(Category.PROCESS, formatProgram, "'formatprg' 'fp'", """
            External program to use for "qq" or "Q" command \
            (default ""). When this option is empty the internal \
            formatting functions are used.

            When specified, the program must take input on stdin \
            and send output to stdout. In Unix, "fmt" is such a program.
            """
            +  space4 +  space4 + twMagic + """

            in the string is substituted by the value of textwidth option. 

            Typically set to "fmt -w #TEXT-WIDTH#" to use external program.""");

    /////////////////////////////////////////////////////////////////////
    //
    createBooleanOption(readOnlyHack, true);
    setupOptionDesc(Category.DEBUG, readOnlyHack, "enable read only hack", """
            A Java implementation issue, restricts the characters \
            that jVi recieves for a read only file. Enabling this, \
            changes the file editor mode to read/write so that the \
            file can be viewed using the Normal Mode vi commands.""");
    setExpertHidden(readOnlyHack, true, true);

    OptUtil.verifyVimOptions();
    if(ViManager.isDebugAtHome())
      OptUtil.checkAllForSetCommand();
  } // END init

  /**
   * Get the named option.
   * @param name option name
   * @return option
   */
  public static Option<?> getOption(String name) {
    return OptUtil.getOption(name);
  }

  public static DebugOption getDebugOption(String name) {
    return (DebugOption)OptUtil.getOption(name);
  }

  /**
   * Get the option names in the specified option category
   * @param category of options
   * @return an immutable list of option names
   */
  public static List<String> getOptionList(Category category) {
    return OptUtil.getOptionList(category);
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
    if(!G.p_ml || (mls = G.p_mls) == 0)
      return;
    int lnum;
    int lcount = G.curbuf.getLineCount();
    for(lnum = 1; lnum <= lcount && lnum <= mls; lnum++) {
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
    MySegment seg = Misc01.ml_get(lnum);
    
    // must check pattern 2 first, since lines that match pattern 2
    // will also match pattern 1
    
    if(parseModeline(mlPat2, seg, lnum))
      return true;
    return parseModeline(mlPat1, seg, lnum);
  }
  
  /** @return true if found and parsed a modeline, there may have been errors */
  private static boolean parseModeline(Pattern p, CharSequence cs, int lnum) {
    Matcher m = p.matcher(cs);
    if(!m.find())
      return false;
    boolean parseError = false;
    String mline = m.group(1);
    List<String> results = new ArrayList<>();
    try {
      String[] args = mline.split("[:\\s]");
      for (String arg : args) {
        String msg = "";
        try {
          SetColonCommand.parseSetOption(arg);
        } catch (SetColonCommand.SetCommandException ex) {
          msg = ex.getMessage();
        } catch (IllegalAccessException ex) {
          LOG.log(Level.SEVERE, null, ex);
        }
        if(!msg.isEmpty()) {
          results.add("Error: " + msg);
          parseError = true;
        } else
          results.add("   OK: " + arg);
      }
    } finally {
      
      EnumSet<FLAGS> flags = EnumSet.noneOf(FLAGS.class);
      if(parseError)
        flags.add(FLAGS.RAISE_YES);
      String fn = G.curbuf.getDisplayPath();
      try (ViOutputStream vos = ViManager.createOutputStream(G.curwin, ViOutputStream.MAIN,
              "In " + fn + ":" + lnum + " process modeline: " + mline,
              flags)) {
        for(String s : results) {
          if(s.startsWith("Error:"))
            vos.println(s, COLOR.FAILURE);
          else
            vos.println(s);
        }
      }
    }
    return true;
  }

  //////////////////////////////////////////////////////////////////////
  //
  // can_bs is in option in vim
  // and other random option checks
  //
  
  static boolean can_bs(char what) {
    switch(G.p_bs) {
      case 2 -> { return true; }
      case 1 -> { return what != BS_START; }
      case 0 -> { return false; }
    }
    assert(false) : "can_bs: ; p_bs bad value";
    return false;
  }
  
  
  //////////////////////////////////////////////////////////////////////
  //
  // Whether or not to highlight depends on a mix of things.
  // Handle some of the logic here.
  
  private static boolean nohDisableHighlight;
  
  static {
    OptionEvent.getEventBus().register(new Object() {
      @Subscribe public void searchOptions(OptionEvent.Global ev) {
        switch(ev.getName()) {
        case Options.highlightSearch, Options.ignoreCase -> {
          if(G.curwin != null)
            ViManager.updateHighlightSearchState();
        }
        } } });
  }
  
  public static boolean doHighlightSearch() {
    return G.p_hls && !nohDisableHighlight;
  }
  
  static void nohCommand() {
    nohDisableHighlight = true;
    ViManager.updateHighlightSearchState();
  }
  
  static void newSearch() {
    nohDisableHighlight = false;
    ViManager.updateHighlightSearchState();
    Normal.v_updateVisualState();
  }

  //////////////////////////////////////////////////////////////////////
  //
  // This option is used a lot, make it fast
  //

  private static DebugOption kd;
  public static final DebugOption kd() {
    return kd;
  }
}

// vi:sw=2 et
