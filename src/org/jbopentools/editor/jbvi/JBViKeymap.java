//------------------------------------------------------------------------------
// Copyright (c) 1996-1999 Inprise Corporation.  All Rights Reserved.
//------------------------------------------------------------------------------
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

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Iterator;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;
import javax.swing.text.JTextComponent;
import javax.swing.text.Element;

import com.borland.jbuilder.*;
import com.borland.jbuilder.debugger.DebuggerActions;
import com.borland.jbuilder.build.BuildActionPool;
import com.borland.primetime.ide.*;
import com.borland.primetime.help.HelpManagerActions;
import com.borland.primetime.insight.template.TemplateActions;
import com.borland.primetime.editor.EditorManager;
import com.borland.primetime.editor.EditorAction;
import com.borland.primetime.editor.EditorActions;
import com.borland.primetime.editor.EditorPane;

import com.raelity.jvi.G;
import com.raelity.jvi.ColonCommands;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.KeyDefs;
import com.raelity.jvi.swing.KeyBinding;
import java.beans.PropertyChangeEvent;

/**
 * JBuilderKeymap holds the default key bindings for the JBuilder IDE.
 *
 * @author Mike Timbol
 */
public class JBViKeymap implements PropertyChangeListener {
  final static String VI_KEYMAP = "VI";
  final static String VI_EDIT_KEYMAP = "Insert";
  static JBViKeymap instance;
  static Keymap insertKeymap;
  static Keymap normalKeymap;
  //public static int majorVersion;
  //public static int minorVersion;

  public static void initOpenTool(byte majorVersion, byte minorVersion) {
    if (majorVersion != 4)
	 return;
    JBOT.maj = majorVersion;
    JBOT.min = minorVersion;

    instance = new JBViKeymap();

    // System.err.println("OT: " + majorVersion + "." + minorVersion);
    //JBViKeymap.minorVersion = minorVersion;
    //JBViKeymap.majorVersion = majorVersion;

    ViManager.setViFactory(new JBViFactory());

    //
    // Set up the IDE keymap
    //

    Keymap ideMap = KeymapManager.createKeymap(VI_KEYMAP, bindingsVI);
    KeymapManager.registerKeymap(ideMap);

    //
    // Set up the editor keymap
    //

    // first muck with the escape key
    //     - remove the existing bindings to the escape action
    //     - add our bindings and actions

    List bindings = KeyBinding.getBindingsList();
    Iterator iter = bindings.iterator();
    while(iter.hasNext()) {
      JTextComponent.KeyBinding b = (JTextComponent.KeyBinding)iter.next();
      if(b.actionName.equals("ViEscapeKey")) {
	iter.remove();
      }
    }

    // The escape key get usurped in jdk1.3 if there is a tool tip manager
    // on the editor pane. Catch escape with KEY TYPED
    bindings.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 (char)KeyEvent.VK_ESCAPE), // KEY TYPED
                 "ViEscapeKey"));
    /*
    bindings.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_OPEN_BRACKET, Event.CTRL_MASK),
                 "ViEscapeKey"));      // alternate
    */

    Keymap viMap = EditorManager.createKeymap("VI",
		      (JTextComponent.KeyBinding[]) bindings.toArray(
			      new JTextComponent.KeyBinding[bindings.size()]),
		      KeyBinding.getActions());
    viMap.setDefaultAction(KeyBinding.getDefaultAction());

    //
    // Add a few extra hooks for IDE special stuff
    //

    viMap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_J,
						       Event.CTRL_MASK),
                                TemplateActions.ACTION_ExpandTemplate);

    //
    // Remove from viMap keystrokes bound in ideMap
    //

    KeyBinding.removeBindings(viMap, ideMap);

    EditorManager.registerKeymap(viMap);
    if(JBOT.has41()) {
      Keymap subMap = setupInsertSubKeymap(viMap);
      EditorManager.registerKeymap(subMap);
      ViManager.setInsertModeKeymap(subMap);
      insertKeymap = subMap;
    } else {
      KeyBinding.getKeymap(); // installs default sub key maps
    }

    //
    // Listen for changes in current keymap
    //
    EditorManager.addPropertyChangeListener(
		      EditorManager.keymapAttribute, instance);
  }

  /**
   * Catch editor manager property changes.
   */
  public void propertyChange(PropertyChangeEvent evt) {
    Keymap oldKeymap = (Keymap)evt.getOldValue();
    Keymap newKeymap = (Keymap)evt.getNewValue();
    if(evt.getPropertyName().equals(EditorManager.keymapAttribute)) {
      if(JBOT.has41()) {
	if(oldKeymap != null && ((Keymap)oldKeymap).getName().equals("VI")) {
	  if(EditorManager.getCaret().getClass() == JBViCaret.class) {
	    EditorManager.setCaret(null);
	  }
	}
	if(newKeymap != null && ((Keymap)newKeymap).getName().equals("VI")) {
	  EditorManager.setCaret(JBViCaret.class);
	}
      }
    }
  }

  private static Keymap setupInsertSubKeymap(Keymap viMap) {
    Keymap subMap;
    subMap = EditorManager.createSubKeymap(VI_EDIT_KEYMAP,
                                           viMap,
					   KeyBinding.getInsertModeBindings(),
					   KeyBinding.getInsertModeActions());

    /*
    subMap = KeymapManager.createSubKeymap("Edit", viMap,
				  new KeymapManager$KeyActionBinding[0]);
    */

    /*
    subMap.addActionForKeyStroke(KeyStroke.getKeyStroke('.',
							InputEvent.CTRL_MASK),
				 new InsertModeAction("indent to next paren", 0));
    */
    return subMap;
  }

  private static KeymapManager$KeyActionBinding bind(KeyStroke stroke,
						     Action action) {
    return new KeymapManager$KeyActionBinding(stroke, action);
  }

  /**
   * These are the default key bindings for the ProjectView.
   */
  static KeymapManager$KeyActionBinding[] bindingsVI = {
    // File-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK),
	 Browser.ACTION_FileOpen),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK | Event.SHIFT_MASK),
	 Browser.ACTION_NodeSaveAll),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK),
	 GalleryDialog.ACTION_Gallery),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F4, Event.CTRL_MASK),
	 Browser.ACTION_NodeClose),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK),
	 Browser.ACTION_NodeSave),

    // Edit-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.CTRL_MASK),
	 Browser.DELEGATE_Cut),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK),
	 Browser.DELEGATE_Copy),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK),
	 Browser.DELEGATE_Paste),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK),
	 Browser.DELEGATE_Undo),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK | Event.SHIFT_MASK),
	 Browser.DELEGATE_Redo),
//    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F10, Event.SHIFT_MASK),
//                                       Browser.DELEGATE_ContextMenu),

    // Search-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK
					       | Event.SHIFT_MASK/*XXX*/),
	 Browser.DELEGATE_SearchFind),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK),
	 JBuilderActions.ACTION_SearchSourcePath),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK
					       | Event.SHIFT_MASK/*XXX*/),
	 Browser.DELEGATE_SearchReplace),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
	 Browser.DELEGATE_SearchAgain),
    //XXX bind(KeyStroke.getKeyStroke(KeyEvent.VK_G, Event.CTRL_MASK),
	 // JBuilderActions.ACTION_SearchGoToLine),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Event.CTRL_MASK),
         JBuilderActions.ACTION_SearchBrowseSymbol),
	 //JBuilderActions.ACTION_SearchBrowseClasses), //XXXXXXXXXXXXXXXXXXXXX

    bind(KeyStroke.getKeyStroke(KeyEvent.VK_FIND, 0),
	 Browser.DELEGATE_SearchFind),

    // Build-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F9, Event.CTRL_MASK),
	 com.borland.jbuilder.build.BuildActionPool.ACTION_ProjectMake),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F9, Event.CTRL_MASK | Event.SHIFT_MASK),
	 com.borland.jbuilder.build.BuildActionPool.ACTION_ProjectNodeMake),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
	 com.borland.jbuilder.runtime.RuntimeActionPool.ACTION_DummyRunOrResumeProject),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F9, Event.SHIFT_MASK),
	 com.borland.jbuilder.runtime.RuntimeActionPool.ACTION_ProjectDebug),

    // History-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.ALT_MASK | Event.CTRL_MASK),
	 Browser.ACTION_NavigateBack),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.ALT_MASK | Event.CTRL_MASK),
	 Browser.ACTION_NavigateForward),

    // Navigation-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F6, Event.CTRL_MASK),
	 Browser.ACTION_NextOpenNode),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F6, Event.SHIFT_MASK | Event.CTRL_MASK),
	 Browser.ACTION_PreviousOpenNode),

    // View-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
	 Browser.DELEGATE_Refresh),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK | Event.ALT_MASK),
	 Browser.STATE_ProjectPaneVisible),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK | Event.ALT_MASK),
	 Browser.STATE_StructurePaneVisible),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK | Event.ALT_MASK),
	 Browser.ACTION_ToggleCurtain),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK | Event.ALT_MASK),
	 Browser.STATE_ContentPaneVisible),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_M, Event.CTRL_MASK | Event.ALT_MASK),
	 Browser.STATE_MessagePaneVisible),

    // Debugger-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0),
	 DebuggerActions.ACTION_StepInto),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0),
	 DebuggerActions.ACTION_StepOver),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F2, Event.CTRL_MASK),
	 DebuggerActions.ACTION_Reset),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
	 DebuggerActions.ACTION_EditorToggleLineBreakpoint),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0),
	 DebuggerActions.ACTION_RunToCursor),

    // Help-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0),
	 HelpManagerActions.DELEGATE_Help),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
	 HelpManagerActions.DELEGATE_Help),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F1, Event.SHIFT_MASK),
	 HelpManagerActions.ACTION_HelpContext),

    // Message-related actions...
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK | Event.SHIFT_MASK),
	 MessageView.ACTION_NextMessage),
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK | Event.SHIFT_MASK),
	 MessageView.ACTION_PriorMessage),
  };
}

class JBOT {
  static int maj;
  static int min;

  /**
   * @return true if running under version 4.0
   */
  static boolean is40() {
    return maj == 4 && min == 0;
  }

  /**
   * @return true if running under version 4.1 or greater
   */
  static boolean has41() {
    return maj == 4 && min == 1;
  }
}
