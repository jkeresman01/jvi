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

import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

/**
 * The vim code uses character constants and key constants.
 * Characters are unicode characters. Keys are keystrokes that
 * do not have a character value, for example the down key is
 * not a character.
 * Define the keys used by vi. All keys must be represented
 * in 16 bits, so that they fit into unicode strings. Further,
 * we don't want the keys to have a valid unicode value.
 * Both characters and
 * keystrokes are kept in char buffers. The keys are put into the range
 * \uE000 - \uF8FF, this range is for private use according to the doc.
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
 * <br>
 * Since there are less than 16 keys that have separate shifted defines,
 * offset them from their unshifted values.
 * <br>So the range \\uEX10 - \\uEX1F is for these shifted special keys,
 * and they are identified by being in the first 16.
 * </p>
 */
public interface KeyDefs {

  /**
   * Any keys as opposed to characters, must be put out of range.
   * This is used to flag keys (non-ascii characters).
   */
  public static final int VIRT = 0xE000;      // a key not a character
  /** modifier tags for the keys. */
  public static final int SHFT = 0x01;
  public static final int CTRL = 0x02;
  public static final int META = 0x04;
  public static final int ALT  = 0x08;
  public static final int MODIFIER_POSITION_SHIFT = 8;   // a numeric shift

  /** the special keys that have shifted versions */
  public static final int SHIFTED_VIRT_OFFSET = 0x10;

  public static final int K_CCIRCM	= 0x1e;	// control circumflex

  /*
   * the three byte codes are replaced with the following int when using vgetc()
   */
  public static final int K_ZERO	= 0xf8ff;

  public static final int K_UP		= 0 + VIRT;
  public static final int K_DOWN	= 1 + VIRT;
  public static final int K_LEFT	= 2 + VIRT;
  public static final int K_RIGHT	= 3 + VIRT;
  public static final int K_S_UP	= K_UP + SHIFTED_VIRT_OFFSET;
  public static final int K_S_DOWN	= K_DOWN + SHIFTED_VIRT_OFFSET;
  public static final int K_S_LEFT	= K_LEFT + SHIFTED_VIRT_OFFSET;
  public static final int K_S_RIGHT	= K_RIGHT + SHIFTED_VIRT_OFFSET;
  public static final int K_TAB		= 4 + VIRT;
  public static final int K_S_TAB	= K_TAB + SHIFTED_VIRT_OFFSET;

  public static final int K_HOME	= 5 + VIRT;
  // public static final int K_KHOME	= KeyEvent.VK_HOME; // XXX
  // public static final int K_XHOME	= KeyEvent.VK_HOME; // XXX
  public static final int K_END		= 6 + VIRT;
  // public static final int K_KEND	= KeyEvent.VK_END; // XXX
  // public static final int K_XEND	= KeyEvent.VK_END; // XXX

  public static final int K_S_HOME	= K_HOME + SHIFTED_VIRT_OFFSET;
  public static final int K_S_END	= K_END + SHIFTED_VIRT_OFFSET;

/* extra set of function keys F1-F4, for vt100 compatible xterm */
  // public static final int K_XF1		= KeyEvent.VK_F1 + VIRT;
  // public static final int K_XF2		= KeyEvent.VK_F2 + VIRT;
  // public static final int K_XF3		= KeyEvent.VK_F3 + VIRT;
  // public static final int K_XF4		= KeyEvent.VK_F4 + VIRT;

  public static final int K_F1		= 0x20 + VIRT;
  public static final int K_F2		= 0x21 + VIRT;
  public static final int K_F3		= 0x22 + VIRT;
  public static final int K_F4		= 0x23 + VIRT;
  public static final int K_F5		= 0x24 + VIRT;
  public static final int K_F6		= 0x25 + VIRT;
  public static final int K_F7		= 0x26 + VIRT;
  public static final int K_F8		= 0x27 + VIRT;
  public static final int K_F9		= 0x28 + VIRT;
  public static final int K_F10		= 0x29 + VIRT;

  public static final int K_F11		= 0x2a + VIRT;
  public static final int K_F12		= 0x2b + VIRT;
  public static final int K_F13		= 0x2c + VIRT;
  public static final int K_F14		= 0x2d + VIRT;
  public static final int K_F15		= 0x2e + VIRT;
  public static final int K_F16		= 0x2f + VIRT;
  public static final int K_F17		= 0x30 + VIRT;
  public static final int K_F18		= 0x31 + VIRT;
  public static final int K_F19		= 0x32 + VIRT;
  public static final int K_F20		= 0x33 + VIRT;

  public static final int K_F21		= 0x34 + VIRT;
  public static final int K_F22		= 0x35 + VIRT;
  public static final int K_F23		= 0x36 + VIRT;
  public static final int K_F24		= 0x37 + VIRT;

  public static final int K_HELP	= 0x38 + VIRT;
  public static final int K_UNDO	= 0x39 + VIRT;

  public static final int K_BS		= 0x3a + VIRT;

  public static final int K_INS		= 0x3b + VIRT;
  public static final int K_DEL		= 0x3c + VIRT;
  public static final int K_PAGEUP	= 0x3d + VIRT;
  public static final int K_PAGEDOWN	= 0x3e + VIRT;

  public static final int K_KPLUS	= 0x3f + VIRT;
  public static final int K_KMINUS	= 0x40 + VIRT;
  public static final int K_KDIVIDE	= 0x41 + VIRT;
  public static final int K_KMULTIPLY	= 0x42 + VIRT;
  public static final int K_KENTER	= 0x43 + VIRT;

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
}
