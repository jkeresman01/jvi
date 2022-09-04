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
package com.raelity.jvi.core.lib;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

/**
 * The vim code uses character constants and key constants.
 * Characters are unicode characters. Keys are keystrokes that
 * do not have a character value, for example the down key is
 * not a character.
 * In this file define the keys used by vi. All keys must be representable
 * in 16 bits, so that they fit into unicode strings. Further,
 * we don't want the keys (as opposed to characters) to have
 * a valid unicode value. The unicode spec reserves
 * \\uE000 - \\uF8FF for private use according to the doc.
 * So keys (not chars) are put into this range.
 * <p>
 * So set up this magic range as follows:
 * <br>
 * \\uEXYY the second hex digit "X" encodes the four modifiers,
 * shft, ctrl, meta and alt.
 * <br>
 * The bottom byte "YY" is arbitrarily assigned below to refer to the keys.
 * </p><p>
 * There is a further complication for vi, some of the special keys have
 * separate values for the shifted variety, e.g. K_S_UP is a shifted up key.
 * The function keys also have separate defines for the shifted varieties,
 * but we will get rid of these defines and use generic techniques when
 * dealing with shifted items like in the map command.
 * Note: the special shifted varieties do not have to be kept within
 * 16 bits. These values are used primarily in switch statements and
 * for equality tests. They could be constructed by GetChar.gotc()
 * and GetChar.pumpChar() when a special shifted character is encountered.
 * <br>
 * Since there are less than 16 keys that have separate shifted defines,
 * offset them from their unshifted values.
 * <br>So the range \\uEX10 - \\uEX1F is for these shifted special keys,
 * and they are identified by being in the first 16.
 * </p>
 */
public class KeyDefs {
    private KeyDefs() { }

    public enum KeyStrokeType { CHAR, KEY }

    public static boolean isVIRT(char c)
    {
        return (c & 0xF000) == VIRT;
    }

    public static final char NO_CHAR = '\uffff';

    /**
     * Any keys as opposed to characters, must be put out of range.
     * This is used to flag keys (non-unicode characters).
     */
    public static final char VIRT      = 0xE000;      // a key not a character
    public static final int VIRT_MASK = 0xF000;
    /** modifier tags for the keys. */
    public static final char SHFT = 0x01;
    public static final char CTRL = 0x02;
    public static final char META = 0x04;
    public static final char ALT  = 0x08;
    public static final int MODIFIER_POSITION_SHIFT = 8;   // a numeric shift

    public static final int MOD_MASK = SHFT | CTRL | META | ALT;
    
    /** the special keys that have shifted versions */
    public static final char SHIFTED_VIRT_OFFSET = 0x10;
    
    public static final char K_CCIRCM	= 0x1e;	// control circumflex
    
    /*
     * the three byte codes are replaced with the following char when using vgetc()
     */
    public static final char K_ZERO	= 0xf8ff;
    
    //
    // Assign a small integer to each of the special (non character) Keys.
    // This is used to map stuff like K_UP, to key strokes
    //
    // REMEMBER to add any new map'd items to KeyBinding.initJavaKeyMap
    //
    public static final char MAP_K_UP	= 0;
    public static final char MAP_K_DOWN	= 1;
    public static final char MAP_K_LEFT	= 2;
    public static final char MAP_K_RIGHT	= 3;
    public static final char MAP_K_TAB	= 4;
    
    public static final char MAP_K_HOME	= 5;
    public static final char MAP_K_END	= 6;
    
    
    public static final char MAP_K_F1	= 0x20;
    public static final char MAP_K_F2	= 0x21;
    public static final char MAP_K_F3	= 0x22;
    public static final char MAP_K_F4	= 0x23;
    public static final char MAP_K_F5	= 0x24;
    public static final char MAP_K_F6	= 0x25;
    public static final char MAP_K_F7	= 0x26;
    public static final char MAP_K_F8	= 0x27;
    public static final char MAP_K_F9	= 0x28;
    public static final char MAP_K_F10	= 0x29;
    
    public static final char MAP_K_F11	= 0x2a;
    public static final char MAP_K_F12	= 0x2b;
    public static final char MAP_K_F13	= 0x2c;
    public static final char MAP_K_F14	= 0x2d;
    public static final char MAP_K_F15	= 0x2e;
    public static final char MAP_K_F16	= 0x2f;
    public static final char MAP_K_F17	= 0x30;
    public static final char MAP_K_F18	= 0x31;
    public static final char MAP_K_F19	= 0x32;
    public static final char MAP_K_F20	= 0x33;
    
    public static final char MAP_K_F21	= 0x34;
    public static final char MAP_K_F22	= 0x35;
    public static final char MAP_K_F23	= 0x36;
    public static final char MAP_K_F24	= 0x37;
    
    public static final char MAP_K_HELP	= 0x38;
    public static final char MAP_K_UNDO	= 0x39;
    
    public static final char MAP_K_BS	= 0x3a;
    
    public static final char MAP_K_INS	= 0x3b;
    public static final char MAP_K_DEL	= 0x3c;
    public static final char MAP_K_PAGEUP	= 0x3d;
    public static final char MAP_K_PAGEDOWN= 0x3e;
    
    public static final char MAP_K_KPLUS	= 0x3f;
    public static final char MAP_K_KMINUS	= 0x40;
    public static final char MAP_K_KDIVIDE	= 0x41;
    public static final char MAP_K_KMULTIPLY = 0x42;
    public static final char MAP_K_KENTER	= 0x43;
    
    public static final char MAP_K_X_PERIOD = 0x44;
    public static final char MAP_K_X_COMMA = 0x45;
    
    public static final char MAP_K_X_SEARCH_FINISH = 0x46;
    public static final char MAP_K_X_INCR_SEARCH_DONE = 0x47;
    public static final char MAP_K_X_SEARCH_CANCEL = 0x48;
    
    public static final char MAP_K_X_IM_SHIFT_RIGHT = 0x49;
    public static final char MAP_K_X_IM_SHIFT_LEFT = 0x4a;
    public static final char MAP_K_X_IM_INS_REP = 0x4b;
    public static final char MAP_K_X_IM_LITERAL = 0x4c;

    public static final char MAP_K_SPACE = 0x4d;
    
    public static final int MAX_JAVA_KEY_MAP = 0x4d; //////////////////////////
    
    //public static final char MAP_K_S_UP	= MAP_K_UP + SHIFTED_VIRT_OFFSET;
    //public static final char MAP_K_S_DOWN = K_DOWN + SHIFTED_VIRT_OFFSET;
    //public static final char MAP_K_S_LEFT = K_LEFT + SHIFTED_VIRT_OFFSET;
    //public static final char MAP_K_S_RIGHT = K_RIGHT + SHIFTED_VIRT_OFFSET;
    //public static final char MAP_K_S_TAB	= K_TAB + SHIFTED_VIRT_OFFSET;
    //public static final char MAP_K_S_HOME = K_HOME + SHIFTED_VIRT_OFFSET;
    //public static final char MAP_K_S_END	= K_END + SHIFTED_VIRT_OFFSET;
    
    public static final char K_UP		= MAP_K_UP + VIRT;
    public static final char K_DOWN	= MAP_K_DOWN + VIRT;
    public static final char K_LEFT	= MAP_K_LEFT + VIRT;
    public static final char K_RIGHT	= MAP_K_RIGHT + VIRT;
    public static final char K_TAB		= MAP_K_TAB  + VIRT;
    
    public static final char K_HOME	= MAP_K_HOME + VIRT;
    // public static final char K_KHOME	= KeyEvent.VK_HOME; // XXX
    // public static final char K_XHOME	= KeyEvent.VK_HOME; // XXX
    public static final char K_END		= MAP_K_END	 + VIRT;
    // public static final char K_KEND	= KeyEvent.VK_END; // XXX
    // public static final char K_XEND	= KeyEvent.VK_END; // XXX
    
    public static final char K_S_UP	= K_UP + SHIFTED_VIRT_OFFSET;
    public static final char K_S_DOWN	= K_DOWN + SHIFTED_VIRT_OFFSET;
    public static final char K_S_LEFT	= K_LEFT + SHIFTED_VIRT_OFFSET;
    public static final char K_S_RIGHT	= K_RIGHT + SHIFTED_VIRT_OFFSET;
    public static final char K_S_TAB	= K_TAB + SHIFTED_VIRT_OFFSET;
    public static final char K_S_HOME	= K_HOME + SHIFTED_VIRT_OFFSET;
    public static final char K_S_END	= K_END + SHIFTED_VIRT_OFFSET;
    
    /* extra set of function keys F1-F4, for vt100 compatible xterm */
    // public static final char K_XF1		= KeyEvent.VK_F1 + VIRT;
    // public static final char K_XF2		= KeyEvent.VK_F2 + VIRT;
    // public static final char K_XF3		= KeyEvent.VK_F3 + VIRT;
    // public static final char K_XF4		= KeyEvent.VK_F4 + VIRT;
    
    public static final char K_SPACE		= MAP_K_SPACE + VIRT;

    public static final char K_F1		= MAP_K_F1 + VIRT;
    public static final char K_F2		= MAP_K_F2 + VIRT;
    public static final char K_F3		= MAP_K_F3 + VIRT;
    public static final char K_F4		= MAP_K_F4 + VIRT;
    public static final char K_F5		= MAP_K_F5 + VIRT;
    public static final char K_F6		= MAP_K_F6 + VIRT;
    public static final char K_F7		= MAP_K_F7 + VIRT;
    public static final char K_F8		= MAP_K_F8 + VIRT;
    public static final char K_F9		= MAP_K_F9 + VIRT;
    public static final char K_F10		= MAP_K_F10 + VIRT;
    
    public static final char K_F11		= MAP_K_F11 + VIRT;
    public static final char K_F12		= MAP_K_F12 + VIRT;
    public static final char K_F13		= MAP_K_F13 + VIRT;
    public static final char K_F14		= MAP_K_F14 + VIRT;
    public static final char K_F15		= MAP_K_F15 + VIRT;
    public static final char K_F16		= MAP_K_F16 + VIRT;
    public static final char K_F17		= MAP_K_F17 + VIRT;
    public static final char K_F18		= MAP_K_F18 + VIRT;
    public static final char K_F19		= MAP_K_F19 + VIRT;
    public static final char K_F20		= MAP_K_F20 + VIRT;
    
    public static final char K_F21		= MAP_K_F21 + VIRT;
    public static final char K_F22		= MAP_K_F22 + VIRT;
    public static final char K_F23		= MAP_K_F23 + VIRT;
    public static final char K_F24		= MAP_K_F24 + VIRT;
    
    public static final char K_HELP	= MAP_K_HELP + VIRT;
    public static final char K_UNDO	= MAP_K_UNDO + VIRT;
    
    public static final char K_BS		= MAP_K_BS + VIRT;
    
    public static final char K_INS		= MAP_K_INS + VIRT;
    public static final char K_DEL		= MAP_K_DEL + VIRT;
    public static final char K_PAGEUP	= MAP_K_PAGEUP + VIRT;
    public static final char K_PAGEDOWN	= MAP_K_PAGEDOWN + VIRT;
    
    public static final char K_KPLUS	= MAP_K_KPLUS + VIRT;
    public static final char K_KMINUS	= MAP_K_KMINUS + VIRT;
    public static final char K_KDIVIDE	= MAP_K_KDIVIDE + VIRT;
    public static final char K_KMULTIPLY	= MAP_K_KMULTIPLY + VIRT;
    public static final char K_KENTER	= MAP_K_KENTER + VIRT;
    
    // Extensions for jVi
    public static final char K_X_PERIOD    = MAP_K_X_PERIOD + VIRT;
    public static final char K_X_COMMA     = MAP_K_X_COMMA + VIRT;
    
    public static final char K_X_SEARCH_FINISH = MAP_K_X_SEARCH_FINISH + VIRT;
    public static final char K_X_INCR_SEARCH_DONE = MAP_K_X_INCR_SEARCH_DONE + VIRT;
    public static final char K_X_SEARCH_CANCEL = MAP_K_X_SEARCH_CANCEL + VIRT;
    
    public static final char K_X_IM_SHIFT_RIGHT = MAP_K_X_IM_SHIFT_RIGHT + VIRT;
    public static final char K_X_IM_SHIFT_LEFT = MAP_K_X_IM_SHIFT_LEFT + VIRT;
    public static final char K_X_IM_INS_REP = MAP_K_X_IM_INS_REP + VIRT;
    public static final char K_X_IM_LITERAL = MAP_K_X_IM_LITERAL + VIRT;

    //
    // KeyBindingPrefs
    //

    private static final Map<String,Integer> keypadNameKeyMap = Map.ofEntries(
            entry("Enter",      KeyEvent.VK_ENTER),
            entry("Escape",     KeyEvent.VK_ESCAPE),
            entry("Back_space", KeyEvent.VK_BACK_SPACE),
            entry("Tab",        KeyEvent.VK_TAB),
            
            entry("Up",         KeyEvent.VK_UP),
            entry("Down",       KeyEvent.VK_DOWN),
            entry("Left",       KeyEvent.VK_LEFT),
            entry("Right",      KeyEvent.VK_RIGHT),
            
            entry("Insert",     KeyEvent.VK_INSERT),
            entry("Delete",     KeyEvent.VK_DELETE),
            entry("Home",       KeyEvent.VK_HOME),
            entry("End",        KeyEvent.VK_END),
            entry("Undo",       KeyEvent.VK_UNDO),
            entry("PageUp",     KeyEvent.VK_PAGE_UP),
            entry("PageDown",   KeyEvent.VK_PAGE_DOWN)
    );

    // NOTE: DO NOT CHANGE THESE.
    // These are the Preferences defaults,
    // if they are changed, then they might change the user's bindings.

    /** These keys are not caught by jVi by default */
    private static final Set<String> defaultKeysFalse = Set.of(
            "Ctrl-[",
            
            "Ctrl-@",
            "Ctrl-A",
            "Ctrl-C",
            "Ctrl-I",
            "Ctrl-J",
            "Ctrl-K",
            "Ctrl-Q",
            "Ctrl-V",
            "Ctrl-X",
            "Ctrl-Z",
            
            "Shift-Enter",
            "Ctrl-Enter",
            "Shift-Escape",
            "Ctrl-Escape",
            "Shift-Back_space",
            "Ctrl-Back_space",
            "Shift-Tab",
            "Ctrl-Tab",

            "Shift-Space",
            "Ctrl-Space",
            
            "Shift-Undo",
            "Ctrl-Undo",
            "Shift-Insert",
            "Ctrl-Insert",
            "Shift-Delete",
            "Ctrl-Delete"
    );

    private static final Map<String,Character> nameKeyMap = Map.ofEntries(
            entry("Up",         K_UP),
            entry("Down",       K_DOWN),
            entry("Left",       K_LEFT),
            entry("Right",      K_RIGHT),
            entry("Insert",     K_INS),
            entry("Delete",     K_DEL),
            entry("Tab",        K_TAB),
            entry("Home",       K_HOME),
            entry("End",        K_END),
            entry("Help",       K_HELP),
            entry("Undo",       K_UNDO),
            entry("Back_space", (char)KeyEvent.VK_BACK_SPACE),

            entry("PageUp",   K_PAGEUP),
            entry("PageDown", K_PAGEDOWN),
            entry("Plus",     K_KPLUS),
            entry("Minus",    K_KMINUS),
            entry("Divide",   K_KDIVIDE),
            entry("Multiply", K_KMULTIPLY),
            entry("Enter",    K_KENTER),

            entry("PeriodCloseAngle", K_X_PERIOD),
            entry("CommaOpenAngle",   K_X_COMMA),
            
            entry("Ctrl-@", (char)0),
            entry("Ctrl-A", (char)1),
            entry("Ctrl-B", (char)2),
            entry("Ctrl-C", (char)3),
            entry("Ctrl-D", (char)4),
            entry("Ctrl-E", (char)5),
            entry("Ctrl-F", (char)6),
            entry("Ctrl-G", (char)7),
            entry("Ctrl-H", (char)8),
            entry("Ctrl-I", (char)9),
            entry("Ctrl-J", (char)10),
            entry("Ctrl-K", (char)11),
            entry("Ctrl-L", (char)12),
            entry("Ctrl-M", (char)13),
            entry("Ctrl-N", (char)14),
            entry("Ctrl-O", (char)15),
            entry("Ctrl-P", (char)16),
            entry("Ctrl-Q", (char)17),
            entry("Ctrl-R", (char)18),
            entry("Ctrl-S", (char)19),
            entry("Ctrl-T", (char)20),
            entry("Ctrl-U", (char)21),
            entry("Ctrl-V", (char)22),
            entry("Ctrl-W", (char)23),
            entry("Ctrl-X", (char)24),
            entry("Ctrl-Y", (char)25),
            entry("Ctrl-Z", (char)26),
            entry("Escape", (char)KeyEvent.VK_ESCAPE), // 27
            entry("Ctrl-Backslash", (char)28),
            entry("CloseBracket",   (char)29),
            //actionsList.add(createKeyAction("Ctrl-Circumflex", (char)30));
            //actionsList.add(createKeyAction("Ctrl-Underscore", (char)31));
            entry("Space", K_SPACE),

            entry("F1",  K_F1),
            entry("F2",  K_F2),
            entry("F3",  K_F3),
            entry("F4",  K_F4),
            entry("F5",  K_F5),
            entry("F6",  K_F6),
            entry("F7",  K_F7),
            entry("F8",  K_F8),
            entry("F9",  K_F9),
            entry("F10", K_F10),
            entry("F11", K_F11),
            entry("F12", K_F12)
    );

    private static Set<String> knownKeys;

    public static Map<String, Character> getNameKeyMap() {
        return nameKeyMap;
    }

    public static Map<String, Integer> getKeypadNameKeyMap() {
        return keypadNameKeyMap;
    }
    public static Set<String> getKeypadNames() {
        return keypadNameKeyMap.keySet();
    }

    public static boolean getCatchKeyDefault(String keyName) {
        return ! defaultKeysFalse.contains(keyName);
    }

    // TODO: HMM, maybe it is needed, see OptUtil use of isKnownKey.
    // This whole known keys thing isn't really needed anymore.
    // It comes from CopyPreferences.java which really shouldn't
    // be needed at all any more.
    static public boolean isKnownKey(String prefName) {
        return knownKeys.contains(prefName);
    }

    /**
     * HACK tet the known keys.
     * TODO: get rid of this. The known keys should come from here,
     * not the swing specific code. Maybe createKeyBinding is in factory.
     */
    public static void setKnownKeys(Set<String> keyNames) {
        if(knownKeys != null)
            throw new IllegalStateException("known keys already set");
        knownKeys = new HashSet<>(keyNames);
    }
}


  // public static final int K_F25		;
  // public static final int K_F26		;
  // public static final int K_F27		;
  // public static final int K_F28		;
  // public static final int K_F29		;
  // public static final int K_F30		;

  // public static final int K_F31		;
  // public static final int K_F32		;
  // public static final int K_F33		;
  // public static final int K_F34		;
  // public static final int K_F35		;

/* extra set of shifted function keys F1-F4, for vt100 compatible xterm */
  // public static final int K_S_XF1	= VIUtil.shift(KeyEvent.VK_F1);
  // public static final int K_S_XF2	= VIUtil.shift(KeyEvent.VK_F2);
  // public static final int K_S_XF3	= VIUtil.shift(KeyEvent.VK_F3);
  // public static final int K_S_XF4	= VIUtil.shift(KeyEvent.VK_F4);

  // public static final int K_S_F1	= K_F1 + SHFT;
  // public static final int K_S_F2	= K_F2 + SHFT;
  // public static final int K_S_F3	= K_F3 + SHFT;
  // public static final int K_S_F4	= K_F4 + SHFT;
  // public static final int K_S_F5	= K_F5 + SHFT;
  // public static final int K_S_F6	= K_F6 + SHFT;
  // public static final int K_S_F7	= K_F7 + SHFT;
  // public static final int K_S_F8	= K_F8 + SHFT;
  // public static final int K_S_F9	= K_F9 + SHFT;
  // public static final int K_S_F10	= K_F10 + SHFT;

  // public static final int K_S_F11	= K_F11 + SHFT;
  // public static final int K_S_F12	= K_F12 + SHFT;
  // public static final int K_S_F13	= K_F13 + SHFT;
  // public static final int K_S_F14	= K_F14 + SHFT;
  // public static final int K_S_F15	= K_F15 + SHFT;
  // public static final int K_S_F16	= K_F16 + SHFT;
  // public static final int K_S_F17	= K_F17 + SHFT;
  // public static final int K_S_F18	= K_F18 + SHFT;
  // public static final int K_S_F19	= K_F19 + SHFT;
  // public static final int K_S_F20	= K_F20 + SHFT;

  // public static final int K_S_F21	= K_F21 + SHFT;
  // public static final int K_S_F22	= K_F22 + SHFT;
  // public static final int K_S_F23	= K_F23 + SHFT;
  // public static final int K_S_F24	= K_F24 + SHFT;
  // public static final int K_S_F25	;
  // public static final int K_S_F26	;
  // public static final int K_S_F27	;
  // public static final int K_S_F28	;
  // public static final int K_S_F29	;
  // public static final int K_S_F30	;

  // public static final int K_S_F31	;
  // public static final int K_S_F32	;
  // public static final int K_S_F33	;
  // public static final int K_S_F34	;
  // public static final int K_S_F35	;


  /**********************************************************************
  public static final int K_MOUSE	= KeyEvent.VK_HELP; // XXX
  public static final int K_MENU	= KeyEvent.VK_HELP; // XXX
  public static final int K_SCROLLBAR	= KeyEvent.VK_HELP; // XXX
  public static final int K_HORIZ_SCROLLBAR   = KeyEvent.VK_HELP; // XXX

  public static final int K_SELECT	= KeyEvent.VK_HELP; // XXX
  public static final int K_TEAROFF	= KeyEvent.VK_HELP; // XXX
  **********************************************************************/

  /*
   * Symbols for pseudo keys which are translated from the real key symbols
   * above.
   */
  /*************************************************************************
  public static final int K_LEFTMOUSE	  ;
  public static final int K_LEFTMOUSE_NM  ;
  public static final int K_LEFTDRAG	  ;
  public static final int K_LEFTRELEASE	  ;
  public static final int K_LEFTRELEASE_NM ;
  public static final int K_MIDDLEMOUSE	  ;
  public static final int K_MIDDLEDRAG	  ;
  public static final int K_MIDDLERELEASE ;
  public static final int K_RIGHTMOUSE	  ;
  public static final int K_RIGHTDRAG	  ;
  public static final int K_RIGHTRELEASE  ;

  public static final int K_IGNORE	  ;

  public static final int K_SNIFF	  ;
  *************************************************************************/

  /*
   * Codes for keys that do not have a termcap name.
   *
   * K_SPECIAL KS_EXTRA KE_xxx
   */
  /************************************************************************
  static final int KE_NAME = 3;	// name of this terminal entry

  static final int KE_S_UP = 0;	// shift-up
  static final int KE_S_DOWN = 0;	// shift-down

  static final int KE_S_F1 = 0;	// shifted function keys
  static final int KE_S_F2 = 0;
  static final int KE_S_F3 = 0;
  static final int KE_S_F4 = 0;
  static final int KE_S_F5 = 0;
  static final int KE_S_F6 = 0;
  static final int KE_S_F7 = 0;
  static final int KE_S_F8 = 0;
  static final int KE_S_F9 = 0;
  static final int KE_S_F10 = 0;

  static final int KE_S_F11 = 0;
  static final int KE_S_F12 = 0;
  static final int KE_S_F13 = 0;
  static final int KE_S_F14 = 0;
  static final int KE_S_F15 = 0;
  static final int KE_S_F16 = 0;
  static final int KE_S_F17 = 0;
  static final int KE_S_F18 = 0;
  static final int KE_S_F19 = 0;
  static final int KE_S_F20 = 0;

  static final int KE_S_F21 = 0;
  static final int KE_S_F22 = 0;
  static final int KE_S_F23 = 0;
  static final int KE_S_F24 = 0;
  static final int KE_S_F25 = 0;
  static final int KE_S_F26 = 0;
  static final int KE_S_F27 = 0;
  static final int KE_S_F28 = 0;
  static final int KE_S_F29 = 0;
  static final int KE_S_F30 = 0;

  static final int KE_S_F31 = 0;
  static final int KE_S_F32 = 0;
  static final int KE_S_F33 = 0;
  static final int KE_S_F34 = 0;
  static final int KE_S_F35 = 0;

  static final int KE_MOUSE = 0;	// mouse event start

  //
  // Symbols for pseudo keys which are translated from the real key symbols
  // above.
  //
  static final int KE_LEFTMOUSE = 0;	// Left mouse button click
  static final int KE_LEFTDRAG = 0;	// Drag with left mouse button down
  static final int KE_LEFTRELEASE = 0;	// Left mouse button release
  static final int KE_MIDDLEMOUSE = 0;	// Middle mouse button click
  static final int KE_MIDDLEDRAG = 0;	// Drag with middle mouse button down
  static final int KE_MIDDLERELEASE = 0;// Middle mouse button release
  static final int KE_RIGHTMOUSE = 0;	// Right mouse button click
  static final int KE_RIGHTDRAG = 0;	// Drag with right mouse button down
  static final int KE_RIGHTRELEASE = 0;// Right mouse button release

  static final int KE_IGNORE = 0;	// Ignored mouse drag/release

  static final int KE_TAB = 0;		// unshifted TAB key
  static final int KE_S_TAB = 0;	// shifted TAB key

  static final int KE_SNIFF = 0;	// SNiFF+ input waiting

  static final int KE_XF1 = 0;		// extra vt100 function keys for xterm
  static final int KE_XF2 = 0;
  static final int KE_XF3 = 0;
  static final int KE_XF4 = 0;
  static final int KE_XEND = 0;	// extra (vt100) end key for xterm
  static final int KE_XHOME = 0;	// extra (vt100) home key for xterm

  static final int KE_LEFTMOUSE_NM = 0;// non-mappable Left mouse button click
  static final int KE_LEFTRELEASE_NM = 0;// non-mappable left mouse button release

  static final int KE_S_XF1 = 0;	// extra vt100 shifted function keys for xterm
  static final int KE_S_XF2 = 0;
  static final int KE_S_XF3 = 0;
  static final int KE_S_XF4 = 0;
  *****************************************************************/
