
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

import java.util.Set;
import java.util.Iterator;

import com.raelity.jvi.G;
import com.raelity.jvi.Options;
import com.raelity.jvi.Option;
import com.raelity.jvi.BooleanOption;
import com.borland.primetime.editor.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This handles the options from the
 * <br>
 * TODO: the readOnlyHack is a JB only option, but
 * we have to add it to the main jVi options. I guess we
 * need a JB option that can contain a jVi option; and
 * a way to get the list of option names.
 */
public class JBViOptions implements PropertyChangeListener
{
  static JBViOptions instance;

  private JBViOptions() {};

  public static void initOpenTool(byte majorVersion, byte minorVersion) {
    if (majorVersion != 4)
	 return;
    //System.err.println("Options...");
    instance = new JBViOptions();
    establishReadOnlyOptions();
    establishOptions();
    establishMiscOptions();
    establishDebugOptions();

    // inneffective to load them here
    // loadOptions();

    EditorManager.addPropertyChangeListener(instance);
    // register an option listener
  }

  /**
   * For PropertyChangeListener interface
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if(false)System.err.println("EditorProp: " + evt.getPropertyName()
                       + " " + evt.getNewValue()
                       + " " + evt.getOldValue());
    String name = evt.getPropertyName();
    Option opt = Options.getOption(name);
    if(opt != null) {
      if(false)System.err.println("Setting Vi property " + opt.getName() + " to "
                         + evt.getNewValue());
      opt.setValue(evt.getNewValue().toString());
    } else {
      setJBOption(name);
    }
  }

  private static void setJBOption(String name) {
    if(name.equals(EditorManager.tabSizeAttribute)) {
      G.b_p_ts = EditorManager.getTabSize();
    } else if(name.equals(EditorManager.useTabCharAttribute)) {
      // G.b_p_et = ! EditorManager.isUseTabChar();
      G.b_p_et = ! EditorManager.isBooleanOptionValue(EditorManager.useTabCharAttribute);
    } else if(name.equals(EditorManager.blockIndentAttribute)) {
      G.b_p_sw = EditorManager.getBlockIndent();
    } else if(name.equals(Options.readOnlyHackOption)) {
      JBTextView.changeReadOnlyHackOption();
    }
  }

  // HACK, hook directly into JB stuff. Should be listening to option
  // at the least, or using set command....
  static class IgnoreCaseBooleanOption extends BooleanOption {

    IgnoreCaseBooleanOption() {
      super(Options.ignoreCaseOption, false);
      // Don't need following put because should already be there
      // since this hack overwrites an existing option. Ahrg!
      //options.put(Options.ignoreCaseOption, this);
    }

    public boolean getBoolean() {
      setBoolean(! SearchManager.getSavedOptions().isCaseSensitive());
      return super.getBoolean();
    }
  }

  /** Read JB's option settings, and set them into vi.  */
  static void initOptions() {
    loadOptions();
    G.p_ic = new IgnoreCaseBooleanOption(); // HACK
  }

  /**
   * Get the editor option settings from JB and send them to vi.
   */
  private static void loadOptions() {
    Set options = Options.getOptionNamesSet();
    Iterator iter = options.iterator();
    while(iter.hasNext()) {
      String name = (String)iter.next();
      EditorOption eo = EditorManager.getEditorOption(name);
      if(eo != null) {
        Options.setOptionValue(name, eo.getValue());
        // System.err.println("Setting option " + name
        //                    + " to " + eo.getValue());
      } else {
        if( ! name.equals(Options.metaEqualsOption)     // handled as globlal
            && name.equals(Options.metaEqualsOption)) { // handled as globlal
          System.err.println("no JB option for " + name);
        }
      }
    }
    setJBOption(EditorManager.tabSizeAttribute);
    setJBOption(EditorManager.blockIndentAttribute);
    setJBOption(EditorManager.useTabCharAttribute);
  }

  static EditorOptionCategory optionCategory;
  
  static void establishReadOnlyOptions() {
    optionCategory = new EditorOptionCategory("Vi hack read-only file option");

    establishBooleanOption(Options.readOnlyHackOption,
               "cheat so command mode works with read-only files");
  }

  static void establishOptions() {
    optionCategory = new EditorOptionCategory("Vi cursor wrap options");

    establishBooleanOption(Options.backspaceWrapPreviousOption,
               "<backspace> wraps to previous line");
    establishBooleanOption(Options.hWrapPreviousOption,
               "\"h\" wraps to previous line");
    establishBooleanOption(Options.leftWrapPreviousOption,
               "<left> wraps to previous line");
    establishBooleanOption(Options.spaceWrapNextOption,
               "<space> wraps to next line");
    establishBooleanOption(Options.lWrapNextOption,
               "\"l\" wraps to next line");
    establishBooleanOption(Options.rightWrapNextOption,
               "<right> wraps to next line");
    establishBooleanOption(Options.tildeWrapNextOption,
               "\"~\" wraps to next line");
    optionCategory = null;
  }

  static void establishMiscOptions() {
    optionCategory = new EditorOptionCategory("Vi miscellaneous options");
    establishBooleanOption(Options.commandEntryFrameOption,
               "use modal frame for command/search entry");
    establishBooleanOption(Options.unnamedClipboardOption,
               "use clipboard for unamed yank, delete and put");
    establishBooleanOption(Options.notStartOfLineOption,
               "after motion try to keep column position");
    establishBooleanOption(Options.wrapScanOption,
               "searches wrap around end of file");
    establishBooleanOption(Options.searchFromEndOption,
               "search continues at end of match");
    establishBooleanOption(Options.tildeOperator ,
               "tilde \"~\" acts like an operator, e.g. \"~w\" works");
    establishBooleanOption(Options.changeWordBlanksOption,
               "\"cw\" affects sequential white space");
    establishBooleanOption(Options.joinSpacesOption,
               "\"J\" inserts two spaces after a \".\", \"?\" or \"!\"");
    establishBooleanOption(Options.shiftRoundOption,
               "\"<\" and \">\" round indent to multiple of shiftwidth");
    optionCategory = null;
  }

  private static void establishBooleanOption(String name, String description) {
    EditorManager.registerEditorOption(
      new BooleanEditorOption(name,
                              optionCategory,
                              description,
                              ((BooleanOption)Options.getOption(name))
                                                        .getBoolean()));
  }

  static void establishDebugOptions() {
    optionCategory = new EditorOptionCategory("Vi debug options");
    establishBooleanOption(Options.dbgCache,
               "Output info on text/doc cache");
    establishBooleanOption(Options.dbgKeyStrokes,
               "Output info for each keystroke");
    dbgCIHack = establishJBOption("viDbgCIHack",
                         "Output info on CI changes", false);
    optionCategory = null;
  }

  static BooleanEditorOption dbgCIHack;

  /** This is only for options confined to the JB port */
  private static BooleanEditorOption establishJBOption(String name,
                                        String description,
                                        boolean defaultValue) {
    BooleanEditorOption beo =
      new BooleanEditorOption(name,
                              optionCategory,
                              description,
                              defaultValue);
    EditorManager.registerEditorOption(beo);
    return beo;
  }
}
