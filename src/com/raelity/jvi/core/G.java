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

import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.StringOption;
import com.raelity.jvi.options.IntegerOption;
import com.raelity.jvi.options.BooleanOption;
import com.raelity.jvi.options.DebugOption;

/**
 *  A class of globals. Most taken directly from vim code.
 */
public class G
{
    static public final String metaEscapeDefault = "()|+?{";
    static public final String metaEscapeAll = "()|+?{";

    /**
     *  Set the current editor.
     */
    static void switchTo( ViTextView tv, Buffer buf )
    {
        if(curwin == tv && curbuf != buf
                && dbgEditorActivation.getBoolean()) {
            dbgEditorActivation.println("Activation: changeBuffer tv "
                    + ViManager.cid(tv) + " buf " + ViManager.cid(buf));
        }
        curwin = (TextView)tv;
        curbuf = buf;
    }

    public static void setModMask( int mod_mask )
    {
        G.mod_mask = mod_mask;
    }

    public static int getModMask()
    {
        return mod_mask;
    }

    static int mod_mask;

    public static TextView curwin;

    static Buffer curbuf; // per file options. as int,boolean
          //public static BooleanOption b_p_et;    // expandtabs, per file
          //public static IntegerOption b_p_sw;    // per file
          //public static IntegerOption b_p_ts;    // per file

    /* ******************************************************************
    static boolean asyncCaretMovement = false;

    public final static boolean getAsyncCaretMovement() {
      return asyncCaretMovement;
    }

    public final static void setAsyncCaretMovement(boolean flag) {
      asyncCaretMovement = flag;
    }
    ********************************************************************/

    public static int no_mapping() { return no_mapping; }
    public static int allow_keys() { return allow_keys; }
    public static int no_zero_mapping() { return no_zero_mapping; }
    static int no_mapping;
    static int allow_keys;
    static int no_zero_mapping;
    static boolean finish_op;
    static int State;
    static int restart_edit;
    public static boolean Recording;
    static boolean Exec_reg;      // true when executing a register

    // for the global command
    static boolean global_busy;
    ViOutputStream global_output;

    static boolean clear_cmdline = false;
    static int old_indent = 0;
    static int ai_col = 0;

    public static int p_mmd() { return p_mmd; }
    public static int p_mmd2() { return p_mmd2; }
    static int p_mmd  = 10000;
    static int p_mmd2 = 10000;  // detects an internal error

    // static String p_ww = "bshl<>[]"; // b,s
    // options for whichwrap
    // static BooleanOption p_ww_bs;
    // static BooleanOption p_ww_sp;
    // static BooleanOption p_ww_h;
    // static BooleanOption p_ww_l;
    // static BooleanOption p_ww_larrow;
    // static BooleanOption p_ww_rarrow;
    // static BooleanOption p_ww_tilde;
    // static BooleanOption p_ww_i_left;
    // static BooleanOption p_ww_i_right;

    // static BooleanOption p_cb;  // clipboard, treat as boolean for 'unnamed'
    // static BooleanOption p_sr;   // shiftround
    // static BooleanOption p_notsol;  // startofline
    // static BooleanOption p_cpo_w; // change word blanks
    // static BooleanOption p_cpo_search; // continue search from end of match
    // static BooleanOption p_cpo_j; // sentence search two spaces
    // static BooleanOption p_js;   // joinspaces, 2 spaces after .?!
    // static BooleanOption p_to;   // tildeop
    // static BooleanOption p_ws;   // wrapscan

    public static BooleanOption useFrame;    // use JFrame for command entry

    // public static BooleanOption p_ic;   // ignorecase
    // public static BooleanOption p_pbm;  // use PlatformBraceMatch
    // public static BooleanOption p_scs;  // smartcase
    public static boolean no_smartcase;

    // public static BooleanOption p_meta_equals;  // true use '=' instead of '?'
    // public static StringOption p_meta_escape;   // metacharacters escaped
    // public static BooleanOption p_is;
    // public static BooleanOption p_hls;
    public static boolean no_hlsearch;

    // public static BooleanOption p_smd; // showmode edit/command mode in display
    // public static BooleanOption p_sc; // showcmd

    // public static IntegerOption p_report;
    // public static IntegerOption p_bs ;   // backspace over start of insert, and more
    // public static IntegerOption p_so;   // scrolloff, lines before/after current

    static String p_km = "";      // keymodel

    // public static StringOption p_sel; // 'selection'
    // public static StringOption p_slm; // 'selectionmode'

    // public static BooleanOption p_ea; // 'equalalways'
    // public static BooleanOption p_sb; // 'splitbelow'
    // public static BooleanOption p_spr; // 'splitright'

    // public static StringOption p_ep; // 'equalprg'
    // public static StringOption p_fp; // formatprg
    static String p_cpo = "aABceFs"; // cpoptions

    // public static BooleanOption p_ml;     // modeline
    // public static IntegerOption p_mls;    // modelines

    // public static StringOption p_sh;     // shell - used for external commands
    // public static StringOption p_shcf;   // shellcmdflag - flag for shell
    // public static StringOption p_sxq;    // shellXQuote - flag for shell
    // public static BooleanOption p_ssl;   // shellSlash - flag for filename expansion

    public static BooleanOption readOnlyHack;
    public static BooleanOption isHideVersion;
    public static DebugOption dbgEditorActivation;
    public static BooleanOption isCoordSkip;
    public static DebugOption dbgCoordSkip;
    public static DebugOption dbgUndo;
    public static DebugOption dbgSearch;
    public static BooleanOption usePlatformInsertTab;

    public static DebugOption dbgKeyStrokes;
    // public static IntegerOption dbgFlow;  // 1-switch
                                          // 3-op/exec
                                          // 5-not supported
                                          // 7-not implemented
                                          // 9-xop
    static BooleanOption redoTrack; // track redo for magic/code-completion
    static DebugOption dbgRedo;
    static DebugOption dbgMouse;
    static BooleanOption pcmarkTrack; // track NB caret motions for pcmark

    public static boolean dbgOptions = false; // NEEDSWORK:

    static IntegerOption viminfoMaxBuf;
    //static int viminfoMaxPersistedBuffersWithMarks = 25;

    public static boolean debugPrint = false;
    public static boolean debugOpPrint = false;
    public static boolean switchDebug = false;

    public static ViFPOS VIsual; // start position of active Visual selection
    public static boolean VIsual_active; // whether Visual mode is active
    public static boolean VIsual_select; // whether Select mode is active
    public static boolean VIsual_reselect;

    // VISUAL repaint hack when in colon command from visual mode
    public static boolean drawSavedVisualBounds = false;
    // end VISUAL repaint hack when in colon command from visual mode

    // whether to restart the selection after a Select mode mapping or menu
    public static char VIsual_mode; // type of Visual mode

    /* The visual area is remembered for redo */
    public static boolean redo_VIsual_busy = false;

    /**
     * editPutchar, when not zero, overwrites on screen the character
     * at the cursor.
     */
    static char editPutchar;

   /**
    * This flag is used to make auto-indent work right on lines where only a
    * <RETURN> or <ESC> is typed. It is set when an auto-indent is done, and
    * reset when any other editing is done on the line. If an <ESC> or <RETURN>
    * is received, and did_ai is TRUE, the line is truncated.
    */
    public static boolean did_ai = false;

    static void p_cpo_wOption_setBoolean(boolean f) {
        p_cpo_wOption.setBoolean(f);
    }
    static void p_cpo_searchOption_setBoolean(boolean f) {
        p_cpo_searchOption.setBoolean(f);
    }
    static void p_cpo_jOption_setBoolean(boolean f) {
        p_cpo_jOption.setBoolean(f);
    }
    static void p_wsOption_setBoolean(boolean f) {
        p_wsOption.setBoolean(f);
    }

    // backspace over start of insert, and more
    public static IntegerOption p_bsOption;
    public static int p_bs() { return p_bsOption.getInteger(); }
           static int p_bs;
    // clipboard, treat as boolean for 'unnamed'
    public static BooleanOption p_cbOption;
    public static boolean p_cb() { return p_cbOption.getBoolean(); }
           static boolean p_cb;
    // sentence search two spaces
    public static BooleanOption p_cpo_jOption;
    public static boolean p_cpo_j() { return p_cpo_jOption.getBoolean(); }
           static boolean p_cpo_j;
    // continue search from end of match
    public static BooleanOption p_cpo_searchOption;
    public static boolean p_cpo_search() { return p_cpo_searchOption.getBoolean(); }
           static boolean p_cpo_search;
    // change word blanks
    public static BooleanOption p_cpo_wOption;
    public static boolean p_cpo_w() { return p_cpo_wOption.getBoolean(); }
           static boolean p_cpo_w;
    // 'equalalways'
    public static BooleanOption p_eaOption;
    public static boolean p_ea() { return p_eaOption.getBoolean(); }
           static boolean p_ea;
    // 'equalprg'
    public static StringOption p_epOption;
    public static String p_ep() { return p_epOption.getString(); }
           static String p_ep;
    // formatprg
    public static StringOption p_fpOption;
    public static String p_fp() { return p_fpOption.getString(); }
           static String p_fp;

    public static BooleanOption p_hlsOption;
    public static boolean p_hls() { return p_hlsOption.getBoolean(); }
           static boolean p_hls;
    // ignorecase
    public static BooleanOption p_icOption;
    public static boolean p_ic() { return p_icOption.getBoolean(); }
           static boolean p_ic;

    public static BooleanOption p_isOption;
    public static boolean p_is() { return p_isOption.getBoolean(); }
           static boolean p_is;
    // joinspaces, 2 spaces after .?!
    public static BooleanOption p_jsOption;
    public static boolean p_js() { return p_jsOption.getBoolean(); }
           static boolean p_js;
    // true use '=' instead of '?'
    public static BooleanOption p_meta_equalsOption;
    public static boolean p_meta_equals() { return p_meta_equalsOption.getBoolean(); }
           static boolean p_meta_equals;
    // metacharacters escaped
    public static StringOption p_meta_escapeOption;
    public static String p_meta_escape() { return p_meta_escapeOption.getString(); }
           static String p_meta_escape;
    // modeline
    public static BooleanOption p_mlOption;
    public static boolean p_ml() { return p_mlOption.getBoolean(); }
           static boolean p_ml;
    // modelines
    public static IntegerOption p_mlsOption;
    public static int p_mls() { return p_mlsOption.getInteger(); }
           static int p_mls;
    // startofline
    public static BooleanOption p_notsolOption;
    public static boolean p_notsol() { return p_notsolOption.getBoolean(); }
           static boolean p_notsol;
    // use PlatformBraceMatch
    public static BooleanOption p_pbmOption;
    public static boolean p_pbm() { return p_pbmOption.getBoolean(); }
           static boolean p_pbm;

    public static IntegerOption p_reportOption;
    public static int p_report() { return p_reportOption.getInteger(); }
           static int p_report;
    // 'splitbelow'
    public static BooleanOption p_sbOption;
    public static boolean p_sb() { return p_sbOption.getBoolean(); }
           static boolean p_sb;
    // showcmd
    public static BooleanOption p_scOption;
    public static boolean p_sc() { return p_scOption.getBoolean(); }
           static boolean p_sc;
    // smartcase
    public static BooleanOption p_scsOption;
    public static boolean p_scs() { return p_scsOption.getBoolean(); }
           static boolean p_scs;
    // 'selection'
    public static StringOption p_selOption;
    public static String p_sel() { return p_selOption.getString(); }
           static String p_sel;
    // shell - used for external commands
    public static StringOption p_shOption;
    public static String p_sh() { return p_shOption.getString(); }
           static String p_sh;
    // shellcmdflag - flag for shell
    public static StringOption p_shcfOption;
    public static String p_shcf() { return p_shcfOption.getString(); }
           static String p_shcf;
    // 'selectionmode'
    public static StringOption p_slmOption;
    public static String p_slm() { return p_slmOption.getString(); }
           static String p_slm;
    // showmode edit/command mode in display
    public static BooleanOption p_smdOption;
    public static boolean p_smd() { return p_smdOption.getBoolean(); }
           static boolean p_smd;
    // scrolloff, lines before/after current
    public static IntegerOption p_soOption;
    public static int p_so() { return p_soOption.getInteger(); }
           static int p_so;
    // 'splitright'
    public static BooleanOption p_sprOption;
    public static boolean p_spr() { return p_sprOption.getBoolean(); }
           static boolean p_spr;
    // shiftround
    public static BooleanOption p_srOption;
    public static boolean p_sr() { return p_srOption.getBoolean(); }
           static boolean p_sr;
    // shellSlash - flag for filename expansion
    public static BooleanOption p_sslOption;
    public static boolean p_ssl() { return p_sslOption.getBoolean(); }
           static boolean p_ssl;
    // shellXQuote - flag for shell
    public static StringOption p_sxqOption;
    public static String p_sxq() { return p_sxqOption.getString(); }
           static String p_sxq;
    // tildeop
    public static BooleanOption p_toOption;
    public static boolean p_to() { return p_toOption.getBoolean(); }
           static boolean p_to;
    // wrapscan
    public static BooleanOption p_wsOption;
    public static boolean p_ws() { return p_wsOption.getBoolean(); }
           static boolean p_ws;

    public static BooleanOption p_ww_bsOption;
    public static boolean p_ww_bs() { return p_ww_bsOption.getBoolean(); }
           static boolean p_ww_bs;

    public static BooleanOption p_ww_hOption;
    public static boolean p_ww_h() { return p_ww_hOption.getBoolean(); }
           static boolean p_ww_h;

    public static BooleanOption p_ww_i_leftOption;
    public static boolean p_ww_i_left() { return p_ww_i_leftOption.getBoolean(); }
           static boolean p_ww_i_left;

    public static BooleanOption p_ww_i_rightOption;
    public static boolean p_ww_i_right() { return p_ww_i_rightOption.getBoolean(); }
           static boolean p_ww_i_right;

    public static BooleanOption p_ww_lOption;
    public static boolean p_ww_l() { return p_ww_lOption.getBoolean(); }
           static boolean p_ww_l;

    public static BooleanOption p_ww_larrowOption;
    public static boolean p_ww_larrow() { return p_ww_larrowOption.getBoolean(); }
           static boolean p_ww_larrow;

    public static BooleanOption p_ww_rarrowOption;
    public static boolean p_ww_rarrow() { return p_ww_rarrowOption.getBoolean(); }
           static boolean p_ww_rarrow;

    public static BooleanOption p_ww_spOption;
    public static boolean p_ww_sp() { return p_ww_spOption.getBoolean(); }
           static boolean p_ww_sp;

    public static BooleanOption p_ww_tildeOption;
    public static boolean p_ww_tilde() { return p_ww_tildeOption.getBoolean(); }
           static boolean p_ww_tilde;

} // end com.raelity.jvi.G
