/* vi:set ts=8 sw=2:
 *
 * VIM - Vi IMproved	by Bram Moolenaar
 *
 * Do ":help uganda"  in Vim to read copying and usage conditions.
 * Do ":help credits" in Vim to see a list of people who contributed.
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
package com.raelity.jvi.core;

import java.text.CharacterIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViFeature;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViTextView.DIR;
import com.raelity.jvi.ViTextView.EDGE;
import com.raelity.jvi.ViTextView.FOLDOP;
import com.raelity.jvi.ViTextView.HDIR;
import com.raelity.jvi.ViTextView.HSCROLL;
import com.raelity.jvi.ViTextView.TABOP;
import com.raelity.jvi.ViTextView.TAGOP;
import com.raelity.jvi.core.Misc.Yankreg;
import com.raelity.jvi.core.lib.NotSupportedException;
import com.raelity.jvi.lib.MutableBoolean;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.swing.KeyBinding;
import com.raelity.text.TextUtil;
import com.raelity.text.TextUtil.MySegment;

import static com.raelity.jvi.core.Edit.*;
import static com.raelity.jvi.core.MarkOps.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Misc01.*;
import static com.raelity.jvi.core.Search.*;
import static com.raelity.jvi.core.Search01.*;
import static com.raelity.jvi.core.Search02.*;
import static com.raelity.jvi.core.Search03.*;
import static com.raelity.jvi.core.Util.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.Constants.FDO.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;

/**
 * Contains the main routine for processing characters in command mode.
 * Communicates closely with the code in ops.c to handle the operators.
 * <p>
 * This class started with VIM's normal.c. The idea is to work this into
 * something that can be used in a JEditorPane framework to implement vi.
 * So things that are handled by swing text are taken out.
 * Here is a partial list of changes.
 * </p>
 *
 * <br/>nv_*(): functions called to handle Normal and Visual mode commands.
 * <br/>n_*(): functions called to handle Normal mode commands.
 * <br/>v_*(): functions called to handle Visual mode commands.
 */

public class Normal {
  private static final Logger LOG = Logger.getLogger(Normal.class.getName());

  // for normal_cmd() use, stuff that was declared static in the function
  static int	opnum = 0;		    /* count before an operator */
  static int   restart_VIsual_select = 0;
  static int   old_mapped_len = 0;

  private static boolean cursorShapeHACK;
  
  
  static char redo_VIsual_mode = NUL; /* 'v', 'V', or Ctrl-V */
  static int /*linenr_t*/ redo_VIsual_line_count; /* number of lines */
  static int /*colnr_t*/  redo_VIsual_col;/* number of cols or end column */
  static int /*long*/ redo_VIsual_count;/* count for Visual operator */

  static char seltab[] = {
    /* key		unshifted	shift included */
    K_S_RIGHT,		K_RIGHT,	1,
    K_S_LEFT,		K_LEFT,		1,
    K_S_UP,		K_UP,		1,
    K_S_DOWN,		K_DOWN,		1,
    K_S_HOME,		K_HOME,		1,
    K_S_END,		K_END,		1,
    // K_KHOME,		K_KHOME,	0,
    // K_XHOME,		K_XHOME,	0,
    // K_KEND,		K_KEND,		0,
    // K_XEND,		K_KEND,		0,
    K_PAGEUP,		K_PAGEUP,	0,
    // K_KPAGEUP,		K_KPAGEUP,	0,
    K_PAGEDOWN,		K_PAGEDOWN,	0,
    // K_KPAGEDOWN,	K_KPAGEDOWN,	0,
  };

  //
  // These are the info for parser state between characters.
  //

  static boolean newChunk = true;
  static boolean lookForDigit;
  static boolean pickupExtraChar;
  static boolean firstTimeHere01;
  static boolean editBusy;
  static boolean opInsertBusy;

  /**
   * Vi comands can have up to three chunks: buffer-operator-motion.
   * <br/>For example: <b>"a3y4&lt;CR&gt;</b>
   * <ul>
   * <li>has buffer: "a</li>
   * <li>and operator: 3y</li>
   * <li>and motion: 4&lt;CR&gt;</li>
   * </ul>
   * The original vim invokes normal 3 times, one for each chunk. The caller
   * had no knowledge of how many chunks there were in a single command,
   * it just called normal in a loop. normal kept control,
   * calling safe_getc as needed, and returned as each chunk was completed.
   * Accumulated information was saved in OPARG.
   * normal detected when a complete command was ready and then executed it,
   * often times in do_pending_op.
   * CMDARG has per chunk information, and OPARG is cleared after each
   * command.
   * <p>
   * To fit in the swing environment, we want to be able to parse one
   * character at a time in the event thread. This means we have to return
   * after each character. So we must maintain a bunch of "where am i" state
   * information between each character. This is messy, but not too bad.
   * (Only breiefly considered having a separate thread.)
   * </p><p>
   * Operator and motion are very similar, they have <count><char>
   * or sometimes more than one char.
   * </p>
   */

  static public void processInputChar(char c, boolean toplevel) {
    try {
      if(editBusy) {
        // NEEDSWORK: if exception from edit, turn edit off and finishupEdit
        Edit.edit(c, false, 0);
        if(!editBusy) {
          try {
            if (opInsertBusy) {
              finishOpSplit();
            }
          } finally {
            opInsertBusy = false;
            finishupEdit();
            newChunk = true;
            willStartNewChunk();
          }
        }
      } else {
        normal_cmd(c, toplevel);
        if(!editBusy && newChunk) {
          willStartNewChunk();
        }
      }
    } catch(Throwable e) {
      if(!G.curwin.isShutdown()) {
        LOG.log(Level.SEVERE, null, e);
        Util.beep_flush();
        resetCommand(false);
      }
    }
  }

  static void finishupEdit() {
    endInsertUndo();
  }

  static public void resetCommand(boolean flush) {
    oap.clearop();
    newChunk = true;
    willStartNewChunk();
    if(editBusy) {
      if (  Options.kd().getBoolean() ) {
          Options.kd().println("resetCommand: EditBusy");
      }
      // Make sure redo buf is usable if edit interrupted
      //    an alternative is to "GetChar.ResetRedobuff()",
      //    instead of "App...; editCom...;", then no redo is possible
      GetChar.AppendCharToRedobuff(ESC);
      GetChar.editComplete();
      Edit.reset();
      editBusy = false; // NEEDSWORK: what else
      finishupEdit();
    }
    clear_showcmd();
    showmode();
    ui_cursor_shape();
    G.allow_keys = 0;
    G.no_mapping = 0;
    G.no_zero_mapping = 0;
    GetChar.reset(flush);
  }

  /**
   * This has the few things that would have exectued at the beginning
   * of normal just before getting a character. Only the stuff with
   * global implications needs to be put here.
   */
  static void willStartNewChunk() {
    //
    // If there is an operator pending, then the command we take this time
    // will terminate it. Finish_op tells us to finish the operation before
    // returning this time (unless the operation was cancelled).
    //
    boolean tflag = G.finish_op;
    G.finish_op = (oap.op_type != OP_NOP);
    if (G.finish_op != tflag) {
      ui_cursor_shape();	/* may show different cursor shape */
    }

    G.State = NORMAL_BUSY;
  }

  static OPARG oap = new OPARG();
  static CMDARG	    ca; 	   /* command arguments */
  static boolean    ctrl_w;	   /* got CTRL-W command */
  static boolean    need_flushbuf; /* need to call out_flush() */
  static int	    mapped_len;

  /**
   * normal
   *
   * Parse and execute a command in Normal mode.
   *
   * This is basically a big switch with the cases arranged in rough categories
   * in the following order:
   *
   *    <ol>
   *    <li> Macros (q, @)
   *    <li> Screen positioning commands (^U, ^D, ^F, ^B, ^E, ^Y, z)
   *    <li> Control commands (:, <help>, ^L, ^G, ^^, ZZ, *, ^], ^T)
   *    <li> Cursor motions (G, H, M, L, l, K_RIGHT,  , h, K_LEFT, ^H, k, K_UP,
   *	 ^P, +, CR, LF, j, K_DOWN, ^N, _, |, B, b, W, w, E, e, $, ^, 0)
   *    <li> Searches (?, /, n, N, T, t, F, f, ,, ;, ], [, %, (, ), {, })
   *    <li> Edits (., u, K_UNDO, ^R, U, r, J, p, P, ^A, ^S)
   *    <li> Inserts (A, a, I, i, o, O, R)
   *    <li> Operators (~, d, c, y, &gt;, &lt;, !, =)
   *    <li> Abbreviations (x, X, D, C, s, S, Y, &amp;)
   *    <li> Marks (m, ', `, ^O, ^I)
   *   <li> Register name setting ('"')
   *   <li> Visual (v, V, ^V)
   *   <li> Suspend (^Z)
   *   <li> Window commands (^W)
   *   <li> extended commands (starting with 'g')
   *   <li> mouse click
   *   <li> scrollbar movement
   *   <li> The end (ESC)
   *   </ol>
   */

  @SuppressWarnings("fallthrough")
  static public void normal_cmd(char c, boolean toplevel) {
                                        //NEEDSWORK: toplevel NOT USED

    update_curswant();	// in vim called just before calling normal_cmd

    if(newChunk) {
      newChunk = false;
      lookForDigit = true;
      G.no_zero_mapping++;
      pickupExtraChar = false;
      firstTimeHere01 = true;
      ca = new CMDARG();
      // vim_memset(&ca, 0, sizeof(ca)); cleared when created
      ca.oap = oap;

      ctrl_w = false;
      need_flushbuf = false;

      // look at willStartNewChunk for code that used to be here
      // like setting G.finish_op and G.State

      // following test if this chunk starting a new command.
      if (!G.finish_op && oap.regname == 0)
        opnum = 0;

      mapped_len = GetChar.typebuf_maplen();

      // c = GetChar.safe_vgetc();

      old_mapped_len = 0;

      /*
       * If a mapping was started in Visual or Select mode, remember the length
       * ...... REMOVED, put old_mapped_len = 0 above
       */

      if (c == NUL)
        c = K_ZERO;
      else if (c == K_KMULTIPLY)  /* K_KMULTIPLY is same as '*' */
        c = '*';
      else if (c == K_KMINUS)	/* K_KMINUS is same as '-' */
        c = '-';
      else if (c == K_KPLUS)	/* K_KPLUS is same as '+' */
        c = '+';
      else if (c == K_KDIVIDE)	/* K_KDIVIDE is same as '/' */
        c = '/';

      /*
       * In Visual/Select mode, a few keys are handled in a special way.
       * ........ REMOVED
       */

      // Don't set need flush for the first char of a command.
      // This will prevent single char commands from being displayed.
      // need_flushbuf = add_to_showcmd(c);
      if (G.finish_op || oap.regname != 0) {
        // in the middle of some command
        need_flushbuf = add_to_showcmd(c);
      } else {
        // first char of a new command
        add_to_showcmd(c);
      }
    } else {
      need_flushbuf = add_to_showcmd(c);
    }


    if(lookForDigit) {
      --G.no_zero_mapping;
      if (!(G.VIsual_active && G.VIsual_select)) {
        if (ctrl_w) {
          --G.no_mapping;
          --G.allow_keys;
        }
        // Pick up any leading digits and compute ca.count0
        if (    (c >= '1' && c <= '9')
                   || (ca.count0 != 0 && (/*c == K_DEL ||*/ c == '0'))) {
          /* *************************************************
             if (c == K_DEL)
             {
             ca.count0 /= 10;
          // NEEDSWORK: del_from_showcmd argument is not correct i bet.
          del_from_showcmd(4);	// delete the digit and ~@%
          }
          else
           **************************************************/
          ca.count0 = ca.count0 * 10 + (c - '0');
          if (ca.count0 < 0)	    // got too large!
            ca.count0 = 999999999;

          if (ctrl_w) {
            ++G.no_mapping;
            ++G.allow_keys;		// no mapping for nchar, but keys
          }
          G.no_zero_mapping++;
          return; // "c = safe_vgetc()" go get another char

        }

        //
        // If we got CTRL-W there may be a/another count
        //
        if (c == Util.ctrl('W') && !ctrl_w && oap.op_type == OP_NOP)
        {
          ctrl_w = true;
          opnum = ca.count0;		// remember first count
          ca.count0 = 0;
          ++G.no_mapping;
          ++G.allow_keys;		// no mapping for nchar, but keys
          ++G.no_zero_mapping;
          return; // go get another char
        }
      }
    }

    if(firstTimeHere01) {

      lookForDigit = false;
      firstTimeHere01 = false;

      ca.cmdchar = c;

      //
      // If we're in the middle of an operator (including after entering a yank
      // buffer with '"') AND we had a count before the
      // operator, then that count overrides the current value of ca.count0.
      // What * this means effectively, is that commands like "3dw" get turned
      // into "d3w" which makes things fall into place pretty neatly.
      // If you give a count before AND after the operator, they are multiplied.
      //
      if (opnum != 0) {
        if (ca.count0 != 0)
          ca.count0 *= opnum;
        else
          ca.count0 = opnum;
      }

      //
      // Always remember the count.  It will be set to zero (on the next call,
      // above) when there is no pending operator.
      // When called from main(), save the count for use by the "count" built-in
      // variable.
      //
      opnum = ca.count0;
      ca.count1 = (ca.count0 == 0 ? 1 : ca.count0);
    }

    //
    // Get an additional character if we need one.
    // For CTRL-W we already got it when looking for a count.
    //
    if (ctrl_w) {
      ca.nchar = ca.cmdchar;
      ca.cmdchar = Util.ctrl('W');
    } else {
      if(!pickupExtraChar) {
        // check if additional char needed
        if (  (oap.op_type == OP_NOP
                      && vim_strchr("@zm\"", 0, ca.cmdchar) >= 0)
                 || (oap.op_type == OP_NOP
                     && (ca.cmdchar == 'r'
                         || (!G.VIsual_active && ca.cmdchar == 'Z')))
                 || vim_strchr("tTfF[]g'`", 0, ca.cmdchar) >= 0
                 || (ca.cmdchar == 'q'
                     && oap.op_type == OP_NOP
                     && !G.Recording
                     && !G.Exec_reg)
                 || ((ca.cmdchar == 'a' || ca.cmdchar == 'i')
                     && (oap.op_type != OP_NOP || G.VIsual_active)))
        {
          ++G.no_mapping;
          ++G.allow_keys;		/* no mapping for nchar, but allow key codes */
          pickupExtraChar = true;
          if (ca.cmdchar == 'r') {
            G.State = REPLACE;	// pretend Replace mode, for cursor shape
            ui_cursor_shape();	// show different cursor shape
          }
          return; // and get the extra char
        }
      } else {
        // we've got the extra char
        if (ca.cmdchar == 'g') {
          ca.nchar = c;
        }
        if (ca.cmdchar == 'g' && ca.nchar == 'r') {
          G.State = REPLACE;	// pretend Replace mode, for cursor shape
          ui_cursor_shape();	// show different cursor shape
        }
        // For 'g' commands, already got next char above, "gr" still needs an
        // extra one though.
        //
        if (ca.cmdchar != 'g') {
          ca.nchar = c;
        } else if (ca.nchar == 'r') {
          ca.extra_char = c;
          throw new RuntimeException("need yet another char for gr");
        }
        G.State = NORMAL_BUSY;
        --G.no_mapping;
        --G.allow_keys;
      }
    }

    //
    //
    // All the characters needed for the comand have been collected
    //
    //

    /*
     * Flush the showcmd characters onto the screen so we can see them while
     * the command is being executed.  Only do this when the shown command was
     * actually displayed, otherwise this will slow down a lot when executing
     * mappings.
     */
    if (need_flushbuf)
      out_flush();

    // c not used past this point
    // ctrl_w not used past this point
    // need_flushbuf not used past this point
    // mapped_len not used past this point

    int		    intflag = 0;
    boolean	    flag = false;
    boolean	    type = false;	    /* type of operation */
    int		    dir = FORWARD;	    /* search direction */
    StringBuilder   searchbuff = new StringBuilder(); /* for search string */
    boolean	    dont_adjust_op_end = false;
    ViFPOS	    old_pos;		    /* cursor position before command */
    int		    old_col = G.curwin.w_curswant;

middle_code:
    do {
      G.State = NORMAL;
      if (ca.nchar == ESC)
      {
	clearop(oap);
	if (p_im != 0 && G.restart_edit == 0)
	  G.restart_edit = 'a';
	break middle_code;	// used to be goto normal_end
      }

      /* when 'keymodel' contains "startsel" some keys start Select/Visual mode */
      if (!G.VIsual_active && vim_strchr(G.p_km, 0, 'a') >= 0)
      {
	int	i;

	for (i = 0; i < seltab.length; i += 3)
	{
	  if (seltab[i] == ca.cmdchar
	      && (seltab[i + 2] != 0 || (G.mod_mask & MOD_MASK_SHIFT) != 0))
	  {
	    ca.cmdchar = seltab[i + 1];
	    start_selection();
	    break;
	  }
	}
      }

      msg_didout = false;    /* don't scroll screen up for normal command */
      msg_col = 0;
      old_pos = G.curwin.w_cursor.copy();/* remember cursor was */

      /*
       * Generally speaking, every command below should either clear any pending
       * operator (with *clearop*()), or set the motion type variable
       * oap.motion_type.
       *
       * When a cursor motion command is made,
       * it is marked as being a character or
       * line oriented motion.  Then, if an operator is in effect, the operation
       * becomes character or line oriented accordingly.
       */
      /*
       * Variables available here:
       * ca.cmdchar	command character
       * ca.nchar	extra command character
       * ca.count0	count before command (0 if no count given)
       * ca.count1	count before command (1 if no count given)
       * oap		Operator Arguments (same as ca.oap)
       * flag		is FALSE, use as you like.
       * dir		is FORWARD, use as you like.
       */
      try {
        notImpV(ca.cmdchar);
	switch (ca.cmdchar)
	{
	  /*
	   * 0: Macros
	   */
	  case 'q':
	    nv_q(ca);
	    break;

	  case '@':		/* execute a named register */
	    nv_at(ca);
	    break;

	    /*
	     * 1: Screen positioning commands
	     */
	  case 0x1f & (int)('D'):	// Ctrl
	  case 0x1f & (int)('U'):	// Ctrl
	    nv_halfpage(ca);
	    break;

	  case 0x1f & (int)('B'):	// Ctrl
	  case K_S_UP:
	  case K_PAGEUP:
	    // case K_KPAGEUP:
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case 0x1f & (int)('F'):	// Ctrl
	  case K_S_DOWN:
	  case K_PAGEDOWN:
	  // case K_KPAGEDOWN:
            if((G.getModMask() & CTRL) != 0
                    && (ca.cmdchar == K_PAGEUP
                        || ca.cmdchar == K_PAGEDOWN)) {
              // editor tab forward/backward
              ca.nchar = dir == BACKWARD ? 'T' : 't';
              nv_g_cmd(ca, searchbuff);
            } else {
              if (checkclearop(oap))
                break;
              onepage(dir, ca.count1);
            }
	    break;

	  case 0x1f & (int)('E'):	// Ctrl
	    flag = true;
	    // FALLTHROUGH

	  case 0x1f & (int)('Y'):	// Ctrl
	    nv_scroll_line(ca, flag);
	    break;

	  case 'z':
	    if (!checkclearop(oap))
	      nv_zet(ca);
	    break;

	    //
	    // 2: Control commands
	    //
	  case ':':
	    nv_colon(ca);
	    break;

	  case 'Q':
	    //
	    // Ignore 'Q' in Visual mode, just give a beep.
	    //
	    /*notSup("exmode");	// XXX
	    if (G.VIsual_active)
	      Util.vim_beep();
	    else if (!checkclearop(oap))
	      do_exmode();*/
            // If ever want to support exmode, then better support
            // map so that 'Q' can be mapped

            nv_operator(ca); // so both 'gq' and 'Q' map to same operator
	    break;

	  case K_HELP:
	  case K_F1:
	  // case K_XF1:
	    notImp("help");
	    if (!checkclearopq(oap))
	      do_help(null);
	    break;

	  case 0x1f & (int)('L'):	// Ctrl
	    notSup("update_screen");
	    if (!checkclearop(oap))
	    {
	      update_screen(CLEAR);
	    }
	    break;

	  case 0x1f & (int)('G'):	// Ctrl
	    nv_ctrlg(ca);
	    break;

	  case K_CCIRCM:	 // CTRL-^, short for ":e #"
	    notImp(":e #");
	    if (!checkclearopq(oap))
	      buflist_getfile(ca.count0, 0,
			      GETF_SETMARK|GETF_ALT, false);
	    break;

	  case 'Z':
	    notImp("close file");
	    //nv_zzet(ca);
	    break;

	  case 163:		// the pound sign, '#' for English keyboards
	    ca.cmdchar = '#';
	    //FALLTHROUGH

	  case 0x1f & (int)(']'):   // :ta to current identifier 	// Ctrl
	  case 'K':		    // run program for current identifier
	  case '*':		    // / to current identifier or string
	  case K_KMULTIPLY:	    // same as '*'
	  case '#':		    // ? to current identifier or string
	    if (ca.cmdchar == K_KMULTIPLY)
	      ca.cmdchar = '*';
	    nv_ident(ca, searchbuff);
	    break;

	  case 0x1f & (int)('T'):    // backwards in tag stack	// Ctrl
	    if (!checkclearopq(oap)) {
              // do_tag((char_u *)"", DT_POP, (int)ca.count1, FALSE, TRUE);
              ViManager.getFactory().tagStack(TAGOP.OLDER, ca.count1);
            }
	    break;

	    //
	    // Cursor motions
	    //
	  case 'G':
	    // ? curbuf.b_ml.ml_line_count
	    nv_goto(ca, G.curbuf.getLineCount());
	    break;

	  case 'H':
	  case 'M':
	  case 'L':
	    nv_scroll(ca);
	    break;

	  case K_RIGHT:
	    if ((G.mod_mask & MOD_MASK_CTRL) != 0)
	    {
	      oap.inclusive = false;
	      nv_wordcmd(ca, true);
	      break;
	    }
          //case K_S_SPACE:
            c = ' ';
            // FALTHROUGH

	  case 'l':
	  case ' ':
	    nv_right(ca);
	    break;

	  case K_LEFT:
	    if ((G.mod_mask & MOD_MASK_CTRL) != 0)
	    {
	      nv_bck_word(ca, true);
	      break;
	    }
            // FALLTHROUGH

	  case 'h':
	    dont_adjust_op_end = nv_left(ca);
	    break;

          //case K_S_BS:
            //c = K_BS;
            // FALTHROUGH

	  case K_BS:
	  case 0x1f & (int)('H'):	// Ctrl
	    if (G.VIsual_active && G.VIsual_select)
	    {
	      ca.cmdchar = 'x';	// BS key behaves like 'x' in Select mode
	      v_visop(ca);
	    }
	    else
	      dont_adjust_op_end = nv_left(ca);
	    break;

	  case '-':
	  case K_KMINUS:
	    flag = true;
	    // FALLTHROUGH

	  case 'k':
	  case K_UP:
	  case 0x1f & (int)('P'):	// Ctrl
	    oap.motion_type = MLINE;
	    if (Edit.cursor_up(ca.count1, oap.op_type == OP_NOP) == FAIL)
	      clearopbeep(oap);
	    else if (flag)
	      Edit.beginline(BL_WHITE | BL_FIX);
	    break;

	  case '+':
	  case K_KPLUS:
	  case CR:
	  case K_KENTER:
	    flag = true;
	    // FALLTHROUGH

	  case 'j':
	  case K_DOWN:
	  case 0x1f & (int)('N'):	// Ctrl
	  case NL:	// 0x1f & (int)('J')
	    oap.motion_type = MLINE;
	    //if (G.curwin.cursor_down(ca.count1, oap.op_type == OP_NOP) == FAIL)
	    if (Edit.cursor_down(ca.count1, oap.op_type == OP_NOP) == FAIL)
	      clearopbeep(oap);
	    else if (flag)
	      Edit.beginline(BL_WHITE | BL_FIX);
	    break;

	    //
	    // This is a strange motion command that helps make operators more
	    // logical. It is actually implemented, but not documented in the
	    // real Vi. This motion command actually refers to
	    // "the current line".
	    // Commands like "dd" and "yy" are really an alternate form of
	    // "d_" and "y_". It does accept a count, so "d3_" works to
	    // delete 3 lines.
	    //
	  case '_':
	    nv_lineop(ca);
	    break;

	  case K_HOME:
	  // case K_KHOME:
	  // case K_XHOME:
	  case K_S_HOME:
	    if ((G.mod_mask & MOD_MASK_CTRL) != 0) // CTRL-HOME = goto line 1
	    {
	      nv_goto(ca, 1);
	      break;
	    }
	    ca.count0 = 1;
	    // FALLTHROUGH

	  case '|':
	    nv_pipe(ca);
	    break;

	    //
	    // Word Motions
	    //
	  case 'B':
	    type = true;
	    // FALLTHROUGH

	  case 'b':
	  case K_S_LEFT:
	    nv_bck_word(ca, type);
	    break;

	  case 'E':
	    type = true;
	    // FALLTHROUGH

	  case 'e':
	    oap.inclusive = true;
	    nv_wordcmd(ca, type);
	    break;

	  case 'W':
	    type = true;
	    // FALLTHROUGH

	  case 'w':
	  case K_S_RIGHT:
	    oap.inclusive = false;
	    nv_wordcmd(ca, type);
	    break;

	  case K_END:
	  // case K_KEND:
	  // case K_XEND:
	  case K_S_END:
	    if ((G.mod_mask & MOD_MASK_CTRL) != 0) {
	      // CTRL-END = goto last line
	      // nv_goto(oap, curbuf.b_ml.ml_line_count);
	      nv_goto(ca, G.curbuf.getLineCount());
	    }
	    // FALLTHROUGH

	  case '$':
	    nv_dollar(ca);
	    break;

	  case '^':
	    intflag = BL_WHITE | BL_FIX;		// XXX
	    // FALLTHROUGH

	  case '0':
	    oap.motion_type = MCHAR;
	    oap.inclusive = false;
	    Edit.beginline(intflag);
            if (G.fdo_flags().contains(FDO_HOR) && KeyTyped
					       && oap.op_type == OP_NOP)
              foldOpenCursor();
	    break;

	    /*
	     * 4: Searches
	     */
	  case K_KDIVIDE:
	    ca.cmdchar = '/';
	    // FALLTHROUGH

	  case '?':
	  case '/':
            if(ca.nchar != 0)
                nv_search_finish(ca, searchbuff);
            else {
                nv_search(ca, searchbuff, false);
                return;
            }
	    break;

	  case 'N':
	    intflag = SEARCH_REV;
	    // FALLTHROUGH

	  case 'n':
	    nv_next(ca, intflag);
	    break;

	    //
	    // Character searches
	    //
	  case 'T':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case 't':
	    nv_csearch(ca, dir, true);
	    break;

	  case 'F':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case 'f':
	    nv_csearch(ca, dir, false);
	    break;

	  case ',':
	    intflag = 1;		// XXX why not dir??
	    // FALLTHROUGH

	  case ';':
	    // ca.nchar == NUL, thus repeat previous search
	    nv_csearch(ca, intflag, false);
	    break;

	    //
	    // section or C function searches
	    //
	  case '[':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case ']':
	    nv_brackets(ca, dir);
	    break;

	  case '%':
	    nv_percent(ca);
	    break;

	  case '(':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case ')':
	    nv_brace(ca, dir);
	    break;

	  case '{':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case '}':
	    nv_findpar(ca, dir);
	    break;

	    /*
	     * 5: Edits
	     */
	  case '.':    /* redo command */
	    if (!checkclearopq(oap))
	    {
	      /*
	       * if restart_edit is TRUE, the last but one command is repeated
	       * instead of the last command (inserting text). This is used for
	       * CTRL-O <.> in insert mode
	       */
	      if (GetChar.start_redo(ca.count0,
                                    G.restart_edit != 0 && !arrow_used) == FAIL)
		clearopbeep(oap);
	    }
	    break;

	  case 'u':    /* undo */
	    if (G.VIsual_active || oap.op_type == OP_LOWER)
	    {
	      /* translate "<Visual>u" to "<Visual>gu" and "guu" to "gugu" */
	      ca.cmdchar = 'g';
	      ca.nchar = 'u';
	      nv_operator(ca);
	      break;
	    }
	    /* FALLTHROUGH */

	  case K_UNDO:
	    if (!checkclearopq(oap))
	    {
	      u_undo(ca.count1);
              G.curwin.w_set_curswant = true;
	    }
	    break;

	  case 0x1f & (int)('R'):	/* undo undo */	// Ctrl
	    if (!checkclearopq(oap))
	    {
	      u_redo(ca.count1);
              G.curwin.w_set_curswant = true;
	    }
	    break;

	  case 'U':		/* Undo line */
	    nv_Undo(ca);
	    break;

	  case 'r':		/* replace character */
	    nv_replace(ca);
	    break;

	  case 'J':
	    nv_join(ca);
	    break;

	  case 'P':
	  case 'p':
	    nv_put(ca);
	    break;

	  case 0x1f & (int)('A'):	    /* add to number */	// Ctrl
	  case 0x1f & (int)('X'):	    /* subtract from number */	// Ctrl
	    if (!checkclearopq(oap)
                    && do_addsub(ca.cmdchar, ca.count1) == OK)
	      prep_redo_cmd(ca);
	    break;

	    /*
	     * 6: Inserts
	     */
	  case 'A':
	  case 'a':
	  case 'I':
	  case 'i':
	  case K_INS:
	    nv_edit(ca);
	    break;

	  case 'o':
	  case 'O':
	    if (G.VIsual_active)  /* switch start and end of visual */
	      v_swap_corners(ca);
	    else
	      n_opencmd(ca);
	    break;

	  case 'R':
	    nv_Replace(ca);
	    break;

	    /*
	     * 7: Operators
	     */
	  case '~':	    /* swap case */
	    /*
	     * if tilde is not an operator and Visual is off: swap case
	     * of a single character
	     */
	    if (	   ! G.p_top
			   && !G.VIsual_active
			   && oap.op_type != OP_TILDE)
	    {
	      n_swapchar(ca);
	      break;
	    }
	    // FALLTHROUGH

	  case 'd':
	  case 'c':
	  case 'y':
	  case '>':
	  case '<':
	  case '!':
	    // FALLTHROUGH

	  case '=':
	    nv_operator(ca);
	    break;

	    /*
	     * 8: Abbreviations
	     */

	  case 'S':
	  case 's':
	    if (G.VIsual_active)	/* "vs" and "vS" are the same as "vc" */
	    {
	      if (ca.cmdchar == 'S')
		G.VIsual_mode = 'V';
	      ca.cmdchar = 'c';
	      nv_operator(ca);
	      break;
	    }
	    /* FALLTHROUGH */
	  case K_DEL:
	  case 'Y':
	  case 'D':
	  case 'C':
	  case 'x':
	  case 'X':
	    if (ca.cmdchar == K_DEL)
	      ca.cmdchar = 'x';		/* DEL key behaves like 'x' */

	    /* with Visual these commands are operators */
	    if (G.VIsual_active)
	    {
	      v_visop(ca);
	      break;
	    }
	    /* FALLTHROUGH */

	  case '&':
	    if(ca.cmdchar == '&') notSup("&");	// XXX
	    nv_optrans(ca);
	    opnum = 0;
	    break;

	    /*
	     * 9: Marks
	     */
	  case 'm':
	    if (!checkclearop(oap))
	    {
	      if (MarkOps.setmark(ca.nchar, ca.count0) == FAIL)
		clearopbeep(oap);
	    }
	    break;

	  case '\'':
	    flag = true;
	    /* FALLTHROUGH */

	  case '`':
	    nv_gomark(ca, flag);
	    break;

	  case 0x1f & (int)('O'):	// Ctrl
	    /* switch from Select to Visual mode for one command */
	    if (G.VIsual_active && G.VIsual_select)
	    {
              notSup("ctrl-o vis mode");
	      G.VIsual_select = false;
	      showmode();
	      restart_VIsual_select = 2;
	      break;
	    }
            ca.count1 = - ca.count1;    // goto older pcmark

	  case 0x1f & (int)('I'):	// goto newer pcmark
          case K_TAB:
	    nv_pcmark(ca);
	    break;

	    /*
	     * 10. Register name setting
	     */
	  case '"':
	    MutableInt mi = new MutableInt(opnum);
	    nv_regname(ca, mi);
	    opnum = mi.getValue();
	    break;

	    /*
	     * 11. Visual
	     */
	  case 0x1f & (int)('Q'):	// Ctrl
            // Treat like Ctrl-V, so ^V is available for paste
            ca.cmdchar = Util.ctrl('V');
	  case 'v':
	  case 'V':
	  case 0x1f & (int)('V'):	// Ctrl
            if(ca.cmdchar == 'v' && !hasF(ViFeature.VisualCharMode)
               || ca.cmdchar == 'V' && !hasF(ViFeature.VisualLineMode)
               || ca.cmdchar == Util.ctrl('V')
                    && !hasF(ViFeature.VisualBlockMode)) {
              notImp(String.valueOf(ca.cmdchar));
            }
	    if (!checkclearop(oap))
                nv_visual(ca, false);
	    break;

	    /*
	     * 12. Suspend
	     */

	  case 0x1f & (int)('Z'):	// Ctrl
	    notSup("ctrl-z");
	    clearop(oap);
	    if (G.VIsual_active)
	      end_visual_mode();		    /* stop Visual */
	    GetChar.stuffReadbuff(":st\r");   /* with autowrite */
	    break;

	    /*
	     * 13. Window commands
	     */

	  case 0x1f & (int)('W'):	// Ctrl
	    if (!checkclearop(oap))
	      Misc01.do_window(ca.nchar, ca.count0); // everything is in window.c
	    break;

	    /*
	     *   14. extended commands (starting with 'g')
	     */
	  case 'g':
	    nv_g_cmd(ca, searchbuff);
	    break;

	    /*
	     * 15. mouse click
	     * == REMOVED ==
	     */

	    //
	    // 16. scrollbar movement
	    // == REMOVED ==
	    //

	    //
	    // case K_SELECT:	    /* end of Select mode mapping */
	    //     nv_select(ca);
	    //     break;

	    /*
	     * 17. The end
	     */
	    /* CTRL-\ CTRL-N goes to Normal mode: a no-op */
	  case 0x1f & (int)('\\'):	// Ctrl
	    notSup("normal");
	    nv_normal(ca);
	    break;

	  case 0x1f & (int)('C'):	// Ctrl
	    G.restart_edit = 0;
	    /*FALLTHROUGH*/

	  case ESC:
	    nv_esc(ca, opnum);
	    break;

	  default:			/* not a known command */
	    clearopbeep(oap);
	    break;

	}	/* end of switch on command character */

        // A modal ':' command may have quit/shutdown the text view.
        if(G.curwin.isShutdown())
          return;

        // if we didn't start or finish an operator, reset oap.regname;
        // unless we need it later.
        if (!G.finish_op && oap.op_type == 0 &&
            vim_strchr("\"DCYSsXx.", 0, ca.cmdchar) < 0)
          oap.regname = 0;

        // If an operation is pending, handle it...
        do_pending_operator(ca, searchbuff, null,
                            old_col, false, dont_adjust_op_end);
      } catch(NotSupportedException e) {
	clearopbeep(oap);
      }

      /*
       * Wait when a message is displayed that will be overwritten by the mode
       * ...... VISUAL MODE stuff for pause about showing message
       */
    } while(false);
// normal_end: this target replace by break from do {} while(0)

    /*
     * Finish up after executing a Normal mode command.
     */

    msg_nowait = false;

    boolean wasFinishOp = G.finish_op;
    G.finish_op = false;
    /* Redraw the cursor with another shape, if we were in Operator-pending
     * mode or did a replace command. */
    if (wasFinishOp || ca.cmdchar == 'r' || cursorShapeHACK) {
      ui_cursor_shape();		/* may show different cursor shape */
      // Without cursorShapeHACK in 'v' mode after a 'y', or a variety
      // of other operators, the cursor never get back as required,
      // need sel == exclusive to see it.
      cursorShapeHACK = false;
    }

    // NEEDSWORK: The following is kind of like what vim does in screen.c
    // (not a jVi file). Maybe make redraw_cmdline global and put this stuff
    // in the exit from keystroke area in getchar
    if(G.clear_cmdline || redraw_cmdline) {
        G.clear_cmdline = false;
        redraw_cmdline = false;
        showmode();
    }
    if (oap.op_type == OP_NOP && oap.regname == 0)
      clear_showcmd();

    // if (modified) ....

    MarkOps.checkpcmark();	/* check if we moved since setting pcmark */

    // vim_free(searchbuff);
    // SCROLLBIND stuff
    // May restart edit() ..... VISUAL MODE STUFF DELETED

    newChunk = true;
  }

  private static boolean hasF(ViFeature f)
  {
    return ViManager.hasFeature(f);
  }

  static void do_pending_operator(CMDARG cap, int old_col, boolean gui_yank)
        throws NotSupportedException {
    do_pending_operator(cap, null, null, old_col, gui_yank, true);
  }
  /**
   * Handle an operator after visual mode or when the movement is finished
   */
  static void do_pending_operator(final CMDARG cap,
			   StringBuilder searchbuff,
			   MutableBoolean command_busy,
			   int old_col,
			   boolean gui_yank,
			   boolean dont_adjust_op_end)
  throws NotSupportedException
  {
    //final OPARG	oap = cap.oap;
    assert oap == cap.oap;

    ViFPOS	old_cursor;
    boolean	empty_region_error;
    final ViFPOS cursor = G.curwin.w_cursor;


//#if defined(USE_CLIPBOARD) && !defined(MSWIN)
    /*
     * Yank the visual area into the GUI selection register before we operate
     * on it and lose it forever.  This could call do_pending_operator()
     * recursively, but that's OK because gui_yank will be TRUE for the
     * nested call.  Note also that we call clip_copy_selection() and not
     * clip_auto_select().  This is because even when 'autoselect' is not set,
     * if we operate on the text, eg by deleting it, then this is considered to
     * be an explicit request for it to be put in the global cut buffer, so we
     * always want to do it here. -- webb
     */
    /* MSWINDOWS: don't do this, there is no automatic copy to the clipboard */
    /* Don't do it if a specific register was specified, so that ""x"*P works */
//    if (clipboard.available
//    && oap->op_type != OP_NOP
//    && !gui_yank
//    && VIsual_active
//    && oap->regname == 0
//    && !redo_VIsual_busy)
//clip_copy_selection();
//#endif
    old_cursor = cursor.copy();

    /*
     * If an operation is pending, handle it...
     */
    if ((G.VIsual_active || G.finish_op) && oap.op_type != OP_NOP) {
      oap.is_VIsual = G.VIsual_active;

      /* only redo yank when 'y' flag is in 'cpoptions' */
      if ((vim_strchr(G.p_cpo, 0, CPO_YANK) >= 0 || oap.op_type != OP_YANK)
	  && !G.VIsual_active)
      {
	prep_redo(oap.regname, cap.count0,
		  get_op_char(oap.op_type), get_extra_op_char(oap.op_type),
		  cap.cmdchar, cap.nchar);
	if (cap.cmdchar == '/' || cap.cmdchar == '?') /* was a search */
	{
	  /*
	   * If 'cpoptions' does not contain 'r', insert the search
	   * pattern to really repeat the same command.
	   */
	  if (vim_strchr(G.p_cpo, 0, CPO_REDO) < 0) {
            GetChar.AppendToRedobuff(searchbuff.toString());
          }
	  GetChar.AppendToRedobuff(NL_STR);
	}
      }

      if (G.redo_VIsual_busy)
      {
          oap.start = cursor.copy();
          int line = oap.start.getLine()+ redo_VIsual_line_count - 1;
          if(line > G.curbuf.getLineCount())
              line = G.curbuf.getLineCount();
          G.VIsual_mode = redo_VIsual_mode;
          // TODO_VIS change this, but NEEDSWORK to step through it, REVIEW
          //        Is redo visual busy ever true?
          // take a shot at it
          int col = oap.start.getColumn();
          if (G.VIsual_mode == 'v')
          {
              if (redo_VIsual_line_count <= 1)
                  col += redo_VIsual_col - 1;
              else
                  col = redo_VIsual_col;
          }
          cursor.set(line, col);

          if (redo_VIsual_col == MAXCOL)
          {
            G.curwin.updateCurswant(null, MAXCOL);
            coladvance(MAXCOL);
          }
          cap.count0 = redo_VIsual_count;
          if (redo_VIsual_count != 0)
              cap.count1 = redo_VIsual_count;
          else
              cap.count1 = 1;
      } else if (G.VIsual_active) {
          // Save the current VIsual area for '< and '> marks, and "gv"
          // G.curbuf.b_visual_start.setMark(G.VIsual);
          // G.curbuf.b_visual_end.setMark(cursor);
          // G.curbuf.b_visual_mode = G.VIsual_mode;
          // VISUAL FOLD HANDLING
          G.curbuf.saveVisualMarks(G.curwin);

            /* In Select mode, a linewise selection is operated upon like a
             * characterwise selection. */
          if (G.VIsual_select && G.VIsual_mode == 'V') {
              if (G.VIsual.compareTo(cursor) < 0) {
                  G.VIsual.setColumn(0);
                  cursor.setColumn(Util.lineLength(cursor.getLine()));
              } else {
                  cursor.setColumn(0);
                  G.VIsual.setColumn(Util.lineLength(G.VIsual.getLine()));
              }
              G.VIsual_mode = 'v';
          }
	    /* If 'selection' is "exclusive", backup one character for
	     * charwise selections. */
          else if(G.VIsual_mode == 'v') {
//# ifdef FEAT_VIRTUALEDIT...
		    unadjust_for_sel();
          }

          oap.start = G.VIsual;
          if (G.VIsual_mode == 'V')
              oap.start.setColumn(0);
      }

      //
      // Set oap.start to the first position of the operated text, oap.end
      // to the end of the operated text.  w_cursor is equal to oap.start.
      //
      // NEEDSWORK: cursor modification if folding, use shadow cursor
      if (oap.start.compareTo(cursor) < 0) {
        // Include folded lines completely.
        if(!G.VIsual_active) {
          MutableInt mi = new MutableInt();
          if(G.curwin.hasFolding(oap.start.getLine(), mi, null))
            oap.start.set(mi.getValue(), 0);
          if(G.curwin.hasFolding(cursor.getLine(), null, mi))
            cursor.set(mi.getValue(), lineLength(mi.getValue()));
        }
        oap.end = cursor.copy();
        cursor.set(oap.start);
      }
      else
      {
        // Include folded lines completely.
        if(!G.VIsual_active && oap.motion_type == MLINE) {
          MutableInt mi = new MutableInt();
          if(G.curwin.hasFolding(cursor.getLine(), mi, null))
            cursor.set(mi.getValue(), 0);
          if(G.curwin.hasFolding(oap.start.getLine(), null, mi))
            oap.start.set(mi.getValue(), lineLength(mi.getValue()));
        }
        oap.end = oap.start.copy();
        oap.start = cursor.copy();
      }
      if(G.VIsual_active) {
        // VISUAL FOLD HANDLING
        Normal.foldAdjustVisual(G.curwin, G.curbuf, oap.start, oap.end);
      }
      oap.line_count = oap.end.getLine() - oap.start.getLine() + 1;

      if (G.VIsual_active || G.redo_VIsual_busy) {
          if (G.VIsual_mode == Util.ctrl('V'))  /* block mode */
          {
              int start, end;
              MutableInt miStart = new MutableInt(), miEnd = new MutableInt();

              oap.block_mode = true;
              getvcol(G.curwin, oap.start, miStart, null, miEnd);
              oap.start_vcol = miStart.getValue();
              oap.end_vcol = miEnd.getValue();
            if (!G.redo_VIsual_busy)
            {
                getvcol(G.curwin, oap.end, miStart, null, miEnd);
                start = miStart.getValue();
                end = miEnd.getValue();
                if (start < oap.start_vcol)
                    oap.start_vcol = start;
                if (end > oap.end_vcol)
                {
                    if (    G.p_sel.charAt(0) == 'e' && start - 1 >= oap.end_vcol)
                        oap.end_vcol = start - 1;
                    else
                        oap.end_vcol = end;
                }
            }

            /* if '$' was used, get oap->end_vcol from longest line */
            if (    G.curwin.w_curswant == MAXCOL)
            {
                //curwin.w_cursor.col = MAXCOL;
                // Can't set the cursor to MAXCOL (well you can, but...)
                // Use an fpos and set it to the \n for the each iteration
                ViFPOS fpos = cursor.copy(); // doesn't use cursor line/col
                oap.end_vcol = 0;
                for (int l = oap.start.getLine(); l <= oap.end.getLine(); l++) {
                  fpos.set(l, Util.lineLength(l));
                  getvcol(G.curwin, fpos, null, null, miEnd);
                  end = miEnd.getValue();
                  if (end > oap.end_vcol)
                    oap.end_vcol = end;
                }
            }
            else if (G.redo_VIsual_busy)
                oap.end_vcol = oap.start_vcol + redo_VIsual_col - 1;
            //
            // Correct oap->end.col and oap->start.col to be the
            // upper-left and lower-right corner of the block area.
            //
            // simpler than vim since jvi has coladvance on an fpos
            coladvance(oap.end, oap.end_vcol);
            coladvance(oap.start, oap.start_vcol);
            // the vim code leaves cursor on the start, so do that
            cursor.set(oap.start);
          }
          if (!G.redo_VIsual_busy && !gui_yank)
          {
              /*
               * Prepare to reselect and redo Visual: this is based on the
               * size of the Visual text
               */
              resel_VIsual_mode = G.VIsual_mode;
              if (  G.curwin.w_curswant == MAXCOL)
                  resel_VIsual_col = MAXCOL;
              else if (G.VIsual_mode == Util.ctrl('V'))
                  resel_VIsual_col = oap.end_vcol - oap.start_vcol + 1;
              else if (oap.line_count > 1)
                  resel_VIsual_col = oap.end.getColumn();
              else
                  resel_VIsual_col = oap.end.getColumn() - oap.start.getColumn() + 1;
              resel_VIsual_line_count = oap.line_count;
          }

          /* can't redo yank (unless 'y' is in 'cpoptions') and ":" */
          if ((vim_strchr(G.p_cpo, 0, CPO_YANK) >= 0 || oap.op_type != OP_YANK)
                  && oap.op_type != OP_COLON)
          {
              prep_redo(oap.regname, 0, NUL, 'v', get_op_char(oap.op_type),
                      get_extra_op_char(oap.op_type));
              redo_VIsual_mode = resel_VIsual_mode;
              redo_VIsual_col = resel_VIsual_col;
              redo_VIsual_line_count = resel_VIsual_line_count;
              redo_VIsual_count = cap.count0;
          }

          /*
           * oap->inclusive defaults to TRUE.
           * If oap->end is on a NUL (empty line) oap->inclusive becomes
           * FALSE.  This makes "d}P" and "v}dP" work the same.
           */
          oap.inclusive = true;
          if (G.VIsual_mode == 'V')
              oap.motion_type = MLINE;
          else {
              oap.motion_type = MCHAR;
              if (G.VIsual_mode != Util.ctrl('V')
                        && Util.ml_get_pos(oap.end).current() == '\n') {
                  oap.inclusive = false;
                  // Try to include the newline, unless it's an operator
                  // that works on lines only
                  if (  G.p_sel.charAt(0) != 'o'
                            && !op_on_lines(oap.op_type)
                            && oap.end.getLine() < G.curbuf.getLineCount())
                  {
                      oap.end.set(oap.end.getLine()+1, 0);
                      oap.line_count++;
                  }
              }
          }
	  G.redo_VIsual_busy = false;
            /*
            * Switch Visual off now, so screen updating does
            * not show inverted text when the screen is redrawn.
            * With OP_YANK and sometimes with OP_COLON and OP_FILTER there is
            * no screen redraw, so it is done here to remove the inverted
            * part.
            */
          if (!gui_yank) {
              G.VIsual_active = false;
              v_updateVisualState();
              if (  G.p_smd)
                  G.clear_cmdline = true;   /* unshow visual mode later */
              if (oap.op_type == OP_YANK || oap.op_type == OP_COLON ||
                      oap.op_type == OP_FILTER)
                  update_curbuf(NOT_VALID);
            }
      }


      G.curwin.w_set_curswant = true;

      /*
       * oap.empty is set when start and end are the same.  The inclusive
       * flag affects this too, unless yanking and the end is on a NUL.
       */
      oap.empty = (oap.motion_type == MCHAR
	       && (!oap.inclusive
		   || (oap.op_type == OP_YANK
		       && gchar_pos(oap.end) == '\n'))
	       && oap.start.equals(oap.end));
      /*
       * For delete, change and yank, it's an error to operate on an
       * empty region, when 'E' inclucded in 'cpoptions' (Vi compatible).
       */
      empty_region_error = (oap.empty
			    && vim_strchr(G.p_cpo, 0, CPO_EMPTYREGION) >= 0);

      /* Force a redraw when operating on an empty Visual region */
//       if (oap.is_VIsual && oap.empty)
// 	redraw_curbuf_later(NOT_VALID);

      /*
       * If the end of an operator is in column one while oap.motion_type
       * is MCHAR and oap.inclusive is FALSE, we put op_end after the last
       * character in the previous line. If op_start is on or before the
       * first non-blank in the line, the operator becomes linewise
       * (strange, but that's the way vi does it).
       */
      if (	   oap.motion_type == MCHAR
		   && oap.inclusive == false
		   && !dont_adjust_op_end
		   && oap.end.getColumn() == 0
		   && (!oap.is_VIsual || G.p_sel.charAt(0) == 'o')
		   && oap.line_count > 1)
      {
	oap.end_adjusted = true;	    /* remember that we did this */
	--oap.line_count;
	int new_line = oap.end.getLine() - 1;
        int new_col = 0;
	if (inindent(0))
	  oap.motion_type = MLINE;
	else
	{
          new_col = Util.lineLength(new_line);
	  if (new_col != 0)
	  {
            --new_col;
	    oap.inclusive = true;
	  }
	}
        oap.end.set(new_line, new_col);
      }
      else
	oap.end_adjusted = false;

      switch (oap.op_type)
      {
	case OP_LSHIFT:
        case OP_RSHIFT:
          runUndoable(new Runnable() {
        @Override
              public void run() {
                op_shift(oap, true, oap.is_VIsual ? cap.count1 : 1);
              }
          });
	  break;

	case OP_JOIN_NS:
	case OP_JOIN:
	  if (oap.line_count < 2) {
	    oap.line_count = 2;
	  }
	      // .... -1 > curbuf.b_ml.ml_line_count...
	  if (  G.curwin.w_cursor.getLine() + oap.line_count - 1 >
	      G.curbuf.getLineCount()) {
	    Util.beep_flush();
	  } else {
            runUndoable(new Runnable() {
          @Override
                public void run() {
                  do_do_join(oap.line_count, oap.op_type == OP_JOIN, true);
                }
            });
	  }
	  break;

	case OP_DELETE:
	  G.VIsual_reselect = false;	    /* don't reselect now */
	  if (empty_region_error)
	    Util.vim_beep();
	  else {
            runUndoable(new Runnable() {
          @Override
                public void run() {
                  op_delete(oap);
                }
            });
	  }
	  break;

	case OP_YANK:
	  if (empty_region_error)
	  {
	    if (!gui_yank)
	      Util.vim_beep();
	  }
	  else
	    op_yank(oap, false, !gui_yank);
	  check_cursor_col();
	  break;

	case OP_CHANGE:
	  G.VIsual_reselect = false;	    /* don't reselect now */
	  if (empty_region_error)
	    Util.vim_beep();
	  else
	  {
	    // don't restart edit after typing <Esc> in edit()
	    G.restart_edit = 0;
            beginInsertUndo();
            try {
              op_change(oap); // will call edit()
            } finally {
              if(editBusy) {
                opInsertBusy = true;
              } else {
                endInsertUndo();
              }
            }
	  }
	  break;

	case OP_FILTER:
	  if (vim_strchr(G.p_cpo, 0, CPO_FILTER) >= 0)
	    GetChar.AppendToRedobuff("!\r");  /* use any last used !cmd */
	  else
	    bangredo = true;    /* do_bang() will put cmd in redo buffer */

	case OP_INDENT:
	case OP_COLON:

          // If 'equalprg' is empty, do the indenting internally.
          if(oap.op_type == OP_INDENT && G.p_ep.isEmpty()) {
            runUndoable(new Runnable() {
          @Override
                public void run() {
                  G.curbuf.reindent(G.curwin.w_cursor.getLine(),
                                    oap.line_count);
                }
            });
            break;
          }
	  op_colon(oap);
	  break;

	case OP_TILDE:
	case OP_UPPER:
	case OP_LOWER:
	case OP_ROT13:
	  if (empty_region_error)
	    Util.vim_beep();
	  else {
            runUndoable(new Runnable() {
          @Override
                public void run() {
                  op_tilde(oap);
                  check_cursor_col();
                }
            });
          }
	  break;

	case OP_FORMAT:
	  if (!G.p_fp.isEmpty())
	    op_colon(oap);		/* use external command */
	  else
	    op_format(oap);		/* use internal function */
	  break;

	case OP_INSERT:
	case OP_APPEND:
	  G.VIsual_reselect = false;	/* don't reselect now */
          if (empty_region_error)
            Util.vim_beep();
          else {
            /* This is a new edit command, not a restart.  We don't edit
             * recursively. */
            G.restart_edit = 0;
            beginInsertUndo();
            try {
              op_insert(oap, cap.count1);/* handles insert & append
                                          * will call edit() */
            } finally {
              if(editBusy) {
                opInsertBusy = true;
              } else {
                endInsertUndo();
              }
            }
          }
	  break;

	case OP_REPLACE:
	  G.VIsual_reselect = false;	/* don't reselect now */
          if (empty_region_error)
            Util.vim_beep();
          else 
            op_replace(oap, cap.nchar);
	  break;

//
// there are several places in this method that might need to consider OP_FOLD*
//	case OP_FOLDOPEN:
//	case OP_FOLDOPENREC:
//	case OP_FOLDCLOSE:
//	case OP_FOLDCLOSEREC:
//	    G.VIsual_reselect = false;	/* don't reselect now */
//	    opFoldRange(oap->start.lnum, oap->end.lnum,
//		    oap->op_type == OP_FOLDOPEN
//					    || oap->op_type == OP_FOLDOPENREC,
//		    oap->op_type == OP_FOLDOPENREC
//					  || oap->op_type == OP_FOLDCLOSEREC,
//					  oap->is_VIsual);
//	    break;
//
//	case OP_FOLDDEL:
//	case OP_FOLDDELREC:
//	    VIsual_reselect = FALSE;	/* don't reselect now */
//	    deleteFold(oap->start.lnum, oap->end.lnum,
//			       oap->op_type == OP_FOLDDELREC, oap->is_VIsual);
//	    break;

	default:
	  clearopbeep(oap);
      }
      if (!gui_yank)
      {
	/*
	 * if 'sol' not set, go back to old column for some commands
	 */
	if (    G.p_notsol && oap.motion_type == MLINE
            && !oap.end_adjusted
	    && (oap.op_type == OP_LSHIFT || oap.op_type == OP_RSHIFT
		|| oap.op_type == OP_DELETE)) {
          G.curwin.updateCurswant(null, old_col);
	  coladvance(old_col);
	}
	oap.op_type = OP_NOP;
      }
      else {
        cursor.set(old_cursor);
      }
      oap.block_mode = false;
      oap.regname = 0;
      cursorShapeHACK = true;
    }
  }

  static void op_format(final OPARG oap)  {
    // NEESDWORK: hook into platform's format (if available)
    runUndoable(new Runnable() {
      @Override
        public void run() {
          G.curbuf.reformat(G.curwin.w_cursor.getLine(),
                            oap.line_count);
        }
    });
  }
  
  static private  void	op_colon (OPARG oap) {
      StringBuffer range = new StringBuffer();
      if(oap.is_VIsual) {
        range.append("'<,'>");
      } else {
	//
	// Make the range look nice, so it can be repeated.
	//
	if (oap.start.getLine() == G.curwin.w_cursor.getLine())
          range.append('.');
	else
          range.append(oap.start.getLine());
	if (oap.end.getLine() != oap.start.getLine())
	{
          range.append(',');
          if (oap.end.getLine() == G.curwin.w_cursor.getLine())
            range.append('.');
          else if (oap.end.getLine() == G.curbuf.getLineCount())
            range.append('$');
          else if (oap.start.getLine() == G.curwin.w_cursor.getLine())
          {
            range.append(".+");
            range.append(oap.line_count - 1);
          }
          else
            range.append(oap.end.getLine());
      }
    }
    if (oap.op_type != OP_COLON) {
      range.append("!");
    }
    if (oap.op_type == OP_INDENT)
    {
      String indent;
      if (  G.p_ep.isEmpty())
        indent = "indent";
      else
        indent = G.p_ep;
      range.append(indent).append("\n");
    }
    else if (oap.op_type == OP_FORMAT)
    {
        String fmt;
	if (G.p_fp.isEmpty()) {
          fmt = "fmt";
        } else {
          fmt = G.p_fp.replace(
                  Options.twMagic, "" + G.curbuf.b_p_tw);
        }
        range.append(fmt).append("\n");
    }
      
    ColonCommands.doColonCommand(range);
  }

  static void end_visual_mode() {
// #ifdef USE_CLIPBOARD
    /*
     * If we are using the clipboard, then remember what was selected in case
     * we need to paste it somewhere while we still own the selection.
     * Only do this when the clipboard is already owned.  Don't want to grab
     * the selection when hitting ESC.
     */
//    if (clipboard.available && clipboard.owned)
//        clip_auto_select();
//#endif

//#ifdef USE_MOUSE
//    setmouse();
//    mouse_dragging = 0;
//#endif

    /* Save the current VIsual area for '< and '> marks, and "gv" */
    // G.curbuf.b_visual_start.setMark(G.VIsual);
    // G.curbuf.b_visual_end.setMark(G.curwin.w_cursor);
    // G.curbuf.b_visual_mode = G.VIsual_mode;
    // VISUAL FOLD HANDLING
    G.curbuf.saveVisualMarks(G.curwin);

    G.VIsual_active = false; // was above, but saveVisualMarks needs it active

    if (G.p_smd)
        G.clear_cmdline = true;/* unshow visual mode later */
    v_updateVisualState();

    /* Don't leave the cursor past the end of the line */
    if (G.curwin.w_cursor.getColumn() > 0 && Util.getChar() == '\n')
        G.curwin.setCaretPosition(G.curwin.getCaretPosition() -1);
    ui_cursor_shape();
  }

  /**
   * 
   * <p>
   * This is the vim comment.
   * Find the identifier under or to the right of the cursor.  If none is
   * found and find_type has FIND_STRING, then find any non-white string.  The
   * length of the string is returned, or zero if no string is found.  If a
   * string is found, a pointer to the string is put in *string, but note that
   * the caller must use the length returned as this string may not be NUL
   * terminated.
   * <p>
   * @param mi use this to return the length of the match, 0 if none
   * @param find_type mask of FIND_IDENT, FIND_STRING
   * @return a segment with its CharacterIterator initialized to found string,
   * or null if no match.
   */
  static CharacterIterator find_ident_under_cursor(MutableInt mi, int find_type) {
    int	    col = 0;	    // init to shut up GCC
    int	    i;

    //
    // if i == 0: try to find an identifier
    // if i == 1: try to find any string
    //
    MySegment seg = Util.ml_get_curline();
    for (i = ((find_type & FIND_IDENT) != 0) ? 0 : 1;	i < 2; ++i)
    {
      //
      // skip to start of identifier/string
      ///
      col = G.curwin.w_cursor.getColumn();
      while (seg.array[col + seg.offset] != '\n'
             && (i == 0
                 ? !vim_iswordc(seg.array[col + seg.offset])
                 : vim_iswhite(seg.array[col + seg.offset])))
        ++col;

      //
      // Back up to start of identifier/string. This doesn't match the
      // real vi but I like it a little better and it shouldn't bother
      // anyone.
      // When FIND_IDENT isn't defined, we backup until a blank.
      ///
      while (col > 0
             && (i == 0
                ? vim_iswordc(seg.array[col - 1 + seg.offset])
                : (!vim_iswhite(seg.array[col - 1 + seg.offset])
                   && (!((find_type & FIND_IDENT) != 0)
                       || !vim_iswordc(seg.array[col - 1 + seg.offset])))))
        --col;

      //
      // if we don't want just any old string, or we've found an identifier,
      // stop searching.
      ///
      if (!((find_type & FIND_STRING) != 0)
          || vim_iswordc(seg.array[col + seg.offset]))
        break;
    }
    //
    // didn't find an identifier or string
    ///
    if (seg.array[col + seg.offset] == '\n'
        || (!vim_iswordc(seg.array[col + seg.offset]) && i == 0))
    {
      if ((find_type & FIND_STRING) != 0)
        Msg.emsg("No string under cursor");
      else
        Msg.emsg("No identifier under cursor");
      mi.setValue(0); // length of match
      return null;
    }
    // ptr += col;
    // *string = ptr;
    //mi.setValue(G.curwin.getLineStartOffset(G.curwin.getWCursor().getLine()) + col);
    seg.setIndex(seg.offset + col);

    int len = 0;
    while (i == 0
           ? vim_iswordc(seg.array[len + col + seg.offset])
           : (seg.array[len + col + seg.offset] != '\n'
              && !vim_iswhite(seg.array[len + col + seg.offset])))
    {
      ++len;
    }
    mi.setValue(len);
    return seg;
  }

  /**
   * Prepare for redo of a normal command.
   */
  static private void prep_redo_cmd(CMDARG cap) {
    prep_redo(cap.oap.regname, cap.count0,
	      NUL, cap.cmdchar, NUL, cap.nchar);
  }

  /**
   * Prepare for redo of any command.
   */
  static private void prep_redo(char regname, int num,
			 char cmd1, char cmd2, char cmd3, char cmd4) {
    GetChar.ResetRedobuff();
    if (regname != 0) {	/* yank from specified buffer */
      GetChar.AppendCharToRedobuff('"');
      GetChar.AppendCharToRedobuff(regname);
    }
    if (num != 0) {
      GetChar.AppendNumberToRedobuff(num);
    }

    if (cmd1 != NUL) { GetChar.AppendCharToRedobuff(cmd1); }
    if (cmd2 != NUL) { GetChar.AppendCharToRedobuff(cmd2); }
    if (cmd3 != NUL) { GetChar.AppendCharToRedobuff(cmd3); }
    if (cmd4 != NUL) { GetChar.AppendCharToRedobuff(cmd4); }
  }

  /**
   * check for operator active and clear it
   *
   * @return TRUE if operator was active
   */
  static private  boolean checkclearop(OPARG oap) {
    if (oap.op_type == OP_NOP)
      return false;
    clearopbeep(oap);
    return true;
  }

  /**
   * check for operator or Visual active and clear it
   *
   * @return TRUE if operator was active
   */
  static private boolean checkclearopq(OPARG oap) {
    if (oap.op_type == OP_NOP && !G.VIsual_active)
      return false;
    clearopbeep(oap);
    return true;
  }

  static private void clearop(OPARG oap) {
    oap.clearop();
  }

  /* *******************************
  static void clearopInstance() {
    oap.clearop();
  }
  ********************************/

  static private void clearopbeep(OPARG oap) {
    clearop(oap);
    Util.beep_flush();
  }

  //
  // Routines for displaying a partly typed command
  //
  // Also used to display visual select limits
  //
  // Kind of messy.
  //

  static StringBuffer showcmd_buf = new StringBuffer();
  static StringBuffer old_showcmd_buf = new StringBuffer();
  //static boolean showcmd_is_clear = true;

  static void clear_showcmd() {

    if (!G.p_sc)
      return;
    if(G.VIsual_active)
      return;

    showcmd_buf.setLength(0);

    /*
     * Don't actually display something if there is nothing to clear.
     */
    //if (showcmd_is_clear) return;

    display_showcmd();
  }

  /**
   * Add 'c' to string of shown command chars.
   * Return TRUE if output has been written (and setcursor() has been called).
   */
  static boolean add_to_showcmd(char c) {


    if (!G.p_sc)
      return false;
    // NEEDSWORK: proper display of virtual chars
    if((c & 0xf000) == VIRT)
        return false;

    //add_one_to_showcmd_buffer(c);
    String p = transchar(c);
    showcmd_buf.append(p);

    // NEEDSWORK: typebuf MutilChar issues
    // if (GetChar.char_avail())
    //   return false;

    display_showcmd();

    return true;
  }

  static void push_add_to_showcmd(String s) {
    push_showcmd();
    // NEEDSWORK: proper display of virtual chars
    for(int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      showcmd_buf.append(transchar(c));
    }
    display_showcmd();
  }

  static private void display_showcmd() {
    setCommandCharacters(showcmd_buf.toString());
  }

  static void push_showcmd() {
    old_showcmd_buf.setLength(0);
    old_showcmd_buf.append(showcmd_buf);
  }

  static void pop_showcmd() {
    showcmd_buf.setLength(0);
    showcmd_buf.append(old_showcmd_buf);
    old_showcmd_buf.setLength(0);
  }

  public static void displaySelectState(String s) {
    showcmd_buf.setLength(0);
    showcmd_buf.append(s).append(" ");
    setCommandCharacters(showcmd_buf.toString());
    out_flush();
  }

  /**
   * Move 'dist' lines in direction 'dir', counting lines by *screen*
   * lines rather than lines in the file.
   * 'dist' must be positive.
   *
   * Return OK if able to move cursor, FAIL otherwise.
   */
  static private int nv_screengo(OPARG oap, DIR dir, int dist)
  {
    if(dist == 0)
      dist = 1;
    ViFPOS fpos = G.curwin.w_cursor.copy();
    boolean ok = G.curwin.viewLineUpDown(dir, dist, fpos);
    // get out of any fold
    ViFPOS viewLineStartFpos = fpos.copy();
    G.curwin.viewLineEdge(EDGE.LEFT, viewLineStartFpos);
    int col = G.curwin.getFirstHiddenColumn(
            viewLineStartFpos.getOffset(),
            fpos.getOffset() - viewLineStartFpos.getOffset());
    G.curwin.w_cursor.set(viewLineStartFpos.getOffset() + col);
    adjust_cursor();
    return ok ? OK : FAIL;
  }
  
  static private void nv_scroll_line(CMDARG cap, boolean is_ctrl_e) {
    if(checkclearop(cap.oap))
      return;
    scroll_redraw(is_ctrl_e, cap.count1);
  }
  
  static private void scroll_redraw(boolean up, int count) {
    if(G.curwin.getLogicalLineCount() <= G.curwin.getVpLines())
      return;
    
    int prev_topline = G.curwin.getVpTopLogicalLine();
    int prev_lnum = G.curwin.getLogicalLine(G.curwin.w_cursor.getLine());
    
    int new_topline = prev_topline + (up ? count : -count);
    
    new_topline = adjustTopLogicalLine(new_topline);
    int new_bottomline = new_topline + G.curwin.getVpLines() -1;
    
    int new_lnum = prev_lnum;
    int so = getScrollOff();
    if(new_lnum < new_topline + so)
      new_lnum = new_topline + so;
    else if(new_lnum > new_bottomline - so)
      new_lnum = new_bottomline - so;
    
    G.curwin.setVpTopLogicalLine(new_topline);
    if(new_lnum != prev_lnum) {
      ViFPOS fpos = fposLogicalLine(new_lnum);
      coladvance(fpos, G.curwin.w_curswant).copyTo(G.curwin.w_cursor);
    }
  }

  /** nv_zet is simplified */
  static private  void	nv_zet (CMDARG cap) {
    FOLDOP foldop = null;
    HSCROLL hscroll = null;
    HDIR hdir = null;
    boolean makeVisible = false;
    switch(cap.nchar) {
      case NL:		// put curwin->w_cursor at top of screen
                        // and set cursor at the first character of that line
      case 't':		// put curwin->w_cursor at top of screen
      case K_KENTER:
      case CR:
      case '.':		// put curwin->w_cursor in middle of screen
                        // and set cursor at the first character of that line
      case 'z':		// put curwin->w_cursor in middle of screen
      case '-':		// put curwin->w_cursor at bottom of screen
                        // and set cursor at the first character of that line
      case 'b':		// put curwin->w_cursor at bottom of screen
        // Scroll screen so current line at top, middle or bottom
	// the scrolloff version doesn't really change anything about how stuff
	// is displayed, so just use it.
	nv_zet_scrolloff(cap);
        return;

      case 'c':
        foldop = FOLDOP.CLOSE;
        break;
      case 'C':
        foldop = FOLDOP.CLOSE_R;
        break;
      case 'o':
        foldop = FOLDOP.OPEN;
        break;
      case 'O':
        foldop = FOLDOP.OPEN_R;
        break;
      case 'a':
        if(G.VIsual_active)
          end_visual_mode(); // OUCH, BUG should do it without end visual
        if(G.curwin.hasFolding(G.curwin.w_cursor.getLine(), null, null))
          foldop = FOLDOP.OPEN;
        else
          foldop = FOLDOP.CLOSE;
        break;
      case 'A':
        if(G.VIsual_active)
          end_visual_mode(); // OUCH, BUG should do it without end visual
        if(G.curwin.hasFolding(G.curwin.w_cursor.getLine(), null, null))
          foldop = FOLDOP.OPEN_R;
        else
          foldop = FOLDOP.CLOSE_R;
        break;
      case 'M':
        foldop = FOLDOP.CLOSE_ALL;
        break;
      case 'R':
        foldop = FOLDOP.OPEN_ALL;
        break;
      case 'v':
        makeVisible = true;
        break;

      case 'h':
      case K_LEFT:
        hscroll = HSCROLL.COUNT;
        hdir = HDIR.LEFT;
        break;

      case 'l':
      case K_RIGHT:
        hscroll = HSCROLL.COUNT;
        hdir = HDIR.RIGHT;
        break;

      case 'H':
        hscroll = HSCROLL.HALF;
        hdir = HDIR.LEFT;
        break;

      case 'L':
        hscroll = HSCROLL.HALF;
        hdir = HDIR.RIGHT;
        break;

      case 's':
        hscroll = HSCROLL.CURSOR;
        hdir = HDIR.RIGHT;
        break;

      case 'e':
        hscroll = HSCROLL.CURSOR;
        hdir = HDIR.LEFT;
        break;
          
      default:
	clearopbeep(oap);
        break;
    }
    if(makeVisible)
      foldOpenCursor();
    else if(foldop != null) {
      // NOTE: in vim visual mode fold ops goes through pending operator
      int start, end;
      boolean visual_active = G.VIsual_active;
      if(G.VIsual_active) {
        start = G.VIsual.getLine();
        end = G.curwin.w_cursor.getLine();
        if(end < start) {
          int t = start;
          start = end;
          end = t;
        }
      } else {
        start = cap.count1;
        end = cap.count1;
      }
      // endvisualmode be doing the fold to avoid bad state
      if(G.VIsual_active)
        end_visual_mode(); // should be using operator pending
      G.curwin.foldOperation(foldop, start, end, visual_active);
      clearop(ca.oap);
    } else if(hscroll != null) {
      G.curwin.hscroll(hscroll, hdir, cap.count0 != 0 ? cap.count0 : 1);
      G.curwin.w_set_curswant = true;
    }
  }

  static private  void	nv_zet_scrolloff (CMDARG cap) {

    int so = getScrollOff();
    int nchar = cap.nchar;
    int target_view_line = G.curwin.getViewLine(G.curwin.w_cursor);
    boolean change_line = false;
    int top_view_line = 0;

    if(cap.count0 != 0 && cap.count0 != target_view_line) {
      MarkOps.setpcmark();
      if(cap.count0 > G.curbuf.getLineCount()) {
	target_view_line = G.curbuf.getLineCount();
      } else {
	target_view_line = cap.count0;
      }
    }
    //int target = G.curwin.getLogicalLine(target_view_line);

    switch(nchar) {
      case NL:		    // put curwin->w_cursor at top of screen
      case K_KENTER:
      case CR:
      case 't':
	top_view_line = target_view_line - so;
	break;

      case '.':		// put curwin->w_cursor in middle of screen
      case 'z':		// put curwin->w_cursor in middle of screen
        top_view_line = target_view_line - G.curwin.getVpLines() / 2;
	break;

      case '-':		// put curwin->w_cursor at bottom of screen
      case 'b':
	top_view_line = target_view_line - G.curwin.getVpLines() + 1 + so;
	break;

      default:
        clearopbeep(cap.oap);
	return;
    }

    int top_logical_line = G.curwin.getLogicalLineFromViewLine(top_view_line);
    int vlStart = G.curwin.getViewLineFromLogicalLine(top_logical_line);
    if(top_view_line != vlStart) {
      // want th etop line to be the start of a line
      if(nchar == '-' || nchar == 'b') {
        if(top_logical_line < G.curwin.getLogicalLineCount())
          top_logical_line++;
      }
    }

    G.curwin.setVpTopLogicalLine(top_logical_line);

    ViFPOS fpos = fposLogicalLine(
                    G.curwin.getLogicalLineFromViewLine(target_view_line));
    int target_column;
    boolean keepColumn = (nchar == 't') || (nchar == 'z') || (nchar == 'b');
    if(keepColumn)
      target_column = G.curwin.w_curswant;
    else {
      // The cursor is set a few lines above, so the segment is for the line
      // that is the fold.
      MySegment seg = G.curbuf.getLineSegment(fpos.getLine());
      target_column = Edit.beginlineColumnIndex(seg, BL_WHITE | BL_FIX);
    }
    coladvance(fpos, target_column).copyTo(G.curwin.w_cursor);
  }
  
  static private void nv_colon (CMDARG cap) {
    if (G.VIsual_active) {
       // VISUAL REPAINT HACK
       G.drawSavedVisualBounds = true;
       // END VISUAL REPAINT HACK
       nv_operator(cap);
    } else if( ! checkclearop(cap.oap)) {
      // translate "count:" into ":.,.+(count - 1)"
      StringBuffer range = new StringBuffer();
      if(cap.count0 != 0) {
        range.append(".");
        // since count1 != 0, then probably > 0. Oh well, just like vim
        if(cap.count0 > 1) {
          range.append(",.+");
          range.append(cap.count0-1);
        }
      }
      ColonCommands.doColonCommand(range);
    }
  }

  static private  void	nv_ctrlg (CMDARG cap) {
    if (G.VIsual_active)	// toggle Selection/Visual mode
    {
      // Convert the visual mode bounds to a java text selection.
      // This is kind of the idea of the command.
      // VIM: G.VIsual_select = !G.VIsual_select;

      // The java selection does not include the caret.
      // This converts directly when sel is exclusive
      int textMark = G.VIsual.getOffset();
      int textDot = G.curwin.w_cursor.getOffset();
      boolean isLineMode = G.VIsual_mode == 'V';
      end_visual_mode();	// stop Visual
      check_cursor_col();	// make sure cursor is not beyond EOL
      G.curwin.w_set_curswant = true;
      update_curbuf(NOT_VALID);
      showmode();
      if(isLineMode) {
        // adjust dot/mark for full lines
        if(textDot < textMark) {
          textDot = G.curbuf.getLineStartOffsetFromOffset(textDot);
          textMark = G.curbuf.getLineEndOffsetFromOffset(textMark);
        } else {
          textDot = G.curbuf.getLineEndOffsetFromOffset(textDot);
          textMark = G.curbuf.getLineStartOffsetFromOffset(textMark);
        }
      } else {
        if(     G.p_sel.charAt(0) != 'e') {
          if(textDot < textMark)
            textMark++;
          else
            textDot++;
        }
      }
      G.curwin.setSelection(textDot, textMark);
    }
    else if (!checkclearop(cap.oap))
      Msg.smsg(ViManager.getFS().getDisplayFileViewInfo(G.curwin));
  }

  /**
   * Handle the commands that use the word under the cursor.
   *
   * @return TRUE for "*" and "#" commands, indicating that the next search
   * should not set the pcmark.
   */
  static private void nv_ident (CMDARG cap, StringBuilder searchp)
                               throws NotSupportedException
  {
    CharacterIterator     ptrSeg = null;
    int		n = 0;		// init for GCC
    char	cmdchar;
    boolean	g_cmd;		// "g" command
    //char_u	*aux_offset;
    //int	isman;
    //int	isman_s;
    
    MutableInt  mi = new MutableInt(0);
    boolean     fDoingSearch = false;
    StringBuilder sb = new StringBuilder();


    if (cap.cmdchar == 'g')	// "g*", "g#", "g]" and "gCTRL-]"
    {
      cmdchar = cap.nchar;
      g_cmd = true;
    }
    else
    {
      cmdchar = cap.cmdchar;
      g_cmd = false;
    }

    //
    // The "]", "CTRL-]" and "K" commands accept an argument in Visual mode.
    //
    if (cmdchar == ']' || cmdchar == Util.ctrl(']') || cmdchar == 'K')
    {
      // ################ notImp("nv_ident subset");  
      if (G.VIsual_active)	// :ta to visual highlighted text
      {
        if (G.VIsual_mode != 'V')
          unadjust_for_sel();
        if (G.VIsual.getLine() != G.curwin.w_cursor.getLine())
        {
          clearopbeep(cap.oap);
          return;
        }
        if( G.curwin.w_cursor.compareTo(G.VIsual) < 0)
        {
          ptrSeg = Util.ml_get_pos(G.curwin.w_cursor);
          n = G.VIsual.getColumn() - G.curwin.w_cursor.getColumn() + 1;
        }
        else
        {
          ptrSeg = Util.ml_get_pos(G.VIsual);
          n = G.curwin.w_cursor.getColumn() - G.VIsual.getColumn() + 1;
        }
        end_visual_mode();
        G.VIsual_reselect = false;
        redraw_curbuf_later(NOT_VALID);    // update the inversion later
      }
      if (checkclearopq(cap.oap))
        return;
    }

    if (ptrSeg == null) {
      if((ptrSeg = find_ident_under_cursor(mi,
                              (cmdchar == '*' || cmdchar == '#')
                              ? FIND_IDENT|FIND_STRING : FIND_IDENT)) == null) {
        clearop(cap.oap);
        return;
      }
      // found something, get the length
      n = mi.getValue();
    }

    switch (cmdchar)
    {
      case '*':
      case '#':
        //
        // Put cursor at start of word, makes search skip the word
        // under the cursor.
        // Call setpcmark() first, so "*``" puts the cursor back where
        // it was.
        ///
        MarkOps.setpcmark();
        G.curwin.w_cursor.set(G.curwin.w_cursor.getLine(),
                              ptrSeg.getIndex() - ptrSeg.getBeginIndex());

        if (!g_cmd && vim_iswordc(ptrSeg.current()))
          sb.append("\\<");
        G.no_smartcase = true;
        break;
        
      case 0x1f & (int)(']'):   // Ctrl-]
        // give the environment a chance at it
        // pass in the extracted identifier,
        // though system probably looks under cursor
        G.curwin.w_cursor.set(G.curwin.w_cursor.getLine(),
                              ptrSeg.getIndex() - ptrSeg.getBeginIndex());
	G.curwin.jumpDefinition(TextUtil.toString(ptrSeg, ptrSeg.getIndex(), n));
        return;

      //case 'K':
      //case ']':
      default:
        notImp("nv_ident subset");
    }

    //
    // Now grab the chars in the identifier
    ///
    String escapeMe = "/?.*~[^$\\";
    while(n-- != 0) {
      char c = ptrSeg.current();
      if(vim_strchr(escapeMe, 0, c) >= 0) {
        sb.append('\\');
      }
      // don't quote control characters, shouldn't be any....
      sb.append(c);
      ptrSeg.next();
    }

    //
    // The search commands may be given after an operator.  Therefore they
    // must be executed right now.
    ///
    if (cmdchar == '*' || cmdchar == '#')
    {
      if (       !g_cmd
              && vim_iswordc(ptrSeg.previous()))
        sb.append("\\>");

      // use SEARCH_HIS rather than vim's calling add_to_history directly
      normal_search(null, cap, cmdchar == '*' ? '/' : '?',
                    sb.toString(), SEARCH_HIS);
    }
    else {
      //do_cmdline_cmd(buf); ///// probably stuffbuf append
      throw new IllegalStateException("do_cmdline_cmd(buf)");
    }
    return;
  }

  /**
   * Handle scrolling command 'H', 'L' and 'M'.
   */
  static private void nv_scroll(CMDARG cap) {
    // NOTE: always using nv_scroll_scrolloff
    nv_scroll_scrolloff(cap);
    return;
  }

  static private void nv_scroll_scrolloff(CMDARG cap) {
    int	    used = 0;
    int    n;

    cap.oap.motion_type = MLINE;
    MarkOps.setpcmark();
    int newcursorline = -1;

    int halfway = (G.curwin.getVpLines() - G.curwin.getVpBlankLines() + 1) / 2;
    if(cap.cmdchar == 'M') {
      int topline = G.curwin.getVpTopLogicalLine();
      int line_count = G.curwin.getLogicalLineCount();
      validate_botline();	    // make sure w_empty_rows is valid
      for (n = 0; topline + n < line_count; ++n)
	if ((used += plines(topline + n)) >= halfway)
	  break;
      if (n != 0 && used > G.curwin.getVpLines())
	--n;
      newcursorline = topline + n;
      if (newcursorline > line_count)
	newcursorline = line_count;
    } else {
      // cap.cmdchar == 'H' or 'L'
      //validate_botline(); 'L' // make sure curwin.w_botline is valid
      int so = getScrollOff();
      int adjust = so;
      if(cap.count1 -1 > so) {
	adjust = cap.count1 -1;
      }
      if(adjust > halfway)
        adjust = halfway;
      if (cap.cmdchar == 'L') {
	newcursorline = G.curwin.getVpBottomViewLine() - 1 - adjust;
	if(newcursorline < G.curwin.getVpTopViewLine() + so) {
	  newcursorline = G.curwin.getVpTopViewLine() + so;
	}
      } else {
	// 'H'
	newcursorline = G.curwin.getVpTopViewLine() + adjust;
	if(newcursorline > G.curwin.getVpBottomViewLine() - 1 - so) {
	  newcursorline = G.curwin.getVpBottomViewLine() - 1 - so;
	}
      }
      newcursorline = G.curwin.getLogicalLineFromViewLine(newcursorline);
    }

    ViFPOS fpos = G.curwin.w_cursor.copy();
    if(newcursorline > 0) {
      fposLogicalLine(fpos, newcursorline);
    }
    cursor_correct(fpos);	// correct for 'so' !!DONE ELSEWHERE!!
    beginline(fpos, BL_SOL | BL_FIX).copyTo(G.curwin.w_cursor);
  }

  /**
   * Cursor right commands.
   */
  static private  void	nv_right (CMDARG cap) {
    long	n;
    boolean	past_line;

    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    past_line = (G.VIsual_active && G.p_sel.charAt(0) != 'o');
    for (n = cap.count1; n > 0; --n) {
      final ViFPOS cursor = G.curwin.w_cursor;
      MySegment seg = G.curbuf.getLineSegment(cursor.getLine());
      if ((!past_line && Edit.oneright() == FAIL)
	    || (past_line && seg.array[cursor.getColumn()+seg.offset] == '\n')
	  )
      {

	//
	//	  <Space> wraps to next line if 'whichwrap' bit 1 set.
	//	      'l' wraps to next line if 'whichwrap' bit 2 set.
	// CURS_RIGHT wraps to next line if 'whichwrap' bit 3 set
	//
	if (       ((cap.cmdchar == ' ' && G.p_ww_sp)
		    || (cap.cmdchar == 'l' && G.p_ww_l)
		    || (cap.cmdchar == K_RIGHT && G.p_ww_rarrow))
		   // && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count
		   && cursor.getLine() < G.curbuf.getLineCount())
	{
	  // When deleting we also count the NL as a character.
	  // Set cap.oap.inclusive when last char in the line is
	  // included, move to next line after that
	  if (	   (cap.oap.op_type == OP_DELETE
		    || cap.oap.op_type == OP_CHANGE)
		   && !cap.oap.inclusive
		   && !Util.lineempty(cursor.getLine())) {
	    cap.oap.inclusive = true;
	  } else {
            G.curwin.w_cursor.set(cursor.getLine() + 1, 0);
            G.curwin.w_set_curswant = true;
	    cap.oap.inclusive = false;
	  }
	  continue;
	}
	if (cap.oap.op_type == OP_NOP) {
	  // Only beep and flush if not moved at all
	  if (n == cap.count1)
	    Util.beep_flush();
	} else {
	  if (!Util.lineempty(cursor.getLine())) {
	    cap.oap.inclusive = true;
	  }
	}
	break;
      } else if (past_line) {
	// NEEDSWORK: (maybe no work) pastline always false since no select
        G.curwin.w_set_curswant = true;
        G.curwin.setCaretPosition(cursor.getOffset()+1);
      }
    }
    if (n != cap.count1 && G.fdo_flags().contains(FDO_HOR) && KeyTyped
					       && cap.oap.op_type == OP_NOP)
	foldOpenCursor();
  }

  /**
   * Cursor left commands.
   * @return TRUE when operator end should not be adjusted.
   */
  static private  boolean nv_left (CMDARG cap) {
    long	n;
    boolean	retval = false;

    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    for (n = cap.count1; n > 0; --n) {
      if (Edit.oneleft() == FAIL) {
	final ViFPOS cursor = G.curwin.w_cursor;
	// <BS> and <Del> wrap to previous line if 'whichwrap' has 'b'.
	//		 'h' wraps to previous line if 'whichwrap' has 'h'.
	//	   CURS_LEFT wraps to previous line if 'whichwrap' has '<'.
	//
	if (       (((cap.cmdchar == K_BS
		      || cap.cmdchar == Util.ctrl('H'))
		     && G.p_ww_bs)
		    || (cap.cmdchar == 'h'
			&& G.p_ww_h)
		    || (cap.cmdchar == K_LEFT
			&& G.p_ww_larrow))
		   && cursor.getLine() > 1)
	{
	  /* **********************************
	  int offset = G.curwin.getLineOffset(cursor.getLine() - 1);
	  Segment seg = G.curwin.getLineSegment(cursor.getLine() - 1);
	  int idx = coladvanceColumnIndex(MAXCOL, seg);
	  G.curwin.setCaretPosition(offset + idx);
	  ************************************/

	  // use a different algorithm with swing document
	  G.curwin.setCaretPosition(cursor.getOffset() - 1);
	  check_cursor_col();
          G.curwin.w_set_curswant = true;

	  // When the NL before the first char has to be deleted we
	  // put the cursor on the NUL after the previous line.
	  // This is a very special case, be careful!
	  // don't adjust op_end now, otherwise it won't work
	  if (	   (cap.oap.op_type == OP_DELETE
		    || cap.oap.op_type == OP_CHANGE)
		   && !Util.lineempty(cursor.getLine()))
	  {
	    G.curwin.setCaretPosition(cursor.getOffset() + 1);
	    retval = true;
	  }
	  continue;
	}
	// Only beep and flush if not moved at all
	else if (cap.oap.op_type == OP_NOP && n == cap.count1) {
	  Util.beep_flush();
	}
	break;
      }
    }
    if (n != cap.count1 && G.fdo_flags().contains(FDO_HOR) && KeyTyped
					       && cap.oap.op_type == OP_NOP)
	foldOpenCursor();

    return retval;
  }

  /**
   * Handle the "$" command.
   */
  static private void nv_dollar(CMDARG cap) {
    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = true;
    G.curwin.updateCurswant(null, MAXCOL);	// so we stay at the end
    if (Edit.cursor_down(cap.count1 - 1, cap.oap.op_type == OP_NOP) == FAIL) {
      clearopbeep(cap.oap);
    }
    else if (G.fdo_flags().contains(FDO_HOR) && KeyTyped
					       && cap.oap.op_type == OP_NOP)
      foldOpenCursor();
  }

  /**
   * Implementation of '?' and '/' commands.
   * <p>
   * This starts the search by bringing up a dialog to enter the search pattern.
   * When the dialog finishes Normal.processInputChar to normal_cmd to
   * nv_search_finish() which should do all the regular things that finish
   * up a command. So any invocation of nv_search() should return from normal_cmd
   * directly without doing the finishing steps.
   */
  static private void nv_search(CMDARG cap,
                                StringBuilder searchp,
                                boolean dont_set_mark) {
    // NOTE: in newer vim, nv_search does a small dance,
    //       then calls normal_search.
    
    inputSearchPattern(cap,
                       cap.count1,
                       (dont_set_mark ? 0 : SEARCH_MARK)
                        | normal_search_standard_options);
  }
  static private void nv_search_finish(CMDARG cap, StringBuilder searchp) {
                        assert cap.nchar == K_X_INCR_SEARCH_DONE
                               || cap.nchar == K_X_SEARCH_CANCEL
                               || cap.nchar == K_X_SEARCH_FINISH;

    if(cap.nchar == K_X_SEARCH_CANCEL) {
      Msg.clearMsg();
      clearop(oap);
      return;
    }

    normal_search(ass.searchPos(), cap,
                  ass.dirc(), ass.pattern(), ass.searchOptions());

    // now that we've used/abused this field, clear it so prep_redo happy
    cap.nchar = 0;
    return;
  }

  /**
   * Handle "N" and "n" commands.
   * (not using cap->arg in jVi (yet?), its in options)
   * cap->arg is SEARCH_REV for "N", 0 for "n".
   */
  static private  void	nv_next (CMDARG cap, int options) {
    normal_search(null, cap, '\000', null, SEARCH_MARK | options);
  }

  private static final int normal_search_standard_options
          = SEARCH_OPT | SEARCH_ECHO | SEARCH_MSG;

  /**
   * Search for "pat" in direction "dir" ('/' or '?', 0 for repeat).
   * Uses only cap->count1 and cap->oap from "cap".
   */
  static private void
  normal_search(ViFPOS pos,     // not a vim arg, null means from cursor
                CMDARG cap, char dir, String pat,
                int opt		// extra flags for do_search()
  ) {
      cap.oap.motion_type = MCHAR;
      cap.oap.inclusive = false;
      // cap.oap.use_reg_one = true;
      G.curwin.w_set_curswant = true;

      int i = do_search(pos, cap.oap, dir, pat, cap.count1,
                        opt | normal_search_standard_options);

      if (i == 0)
          clearop(cap.oap);
      else
      {
          if (i == 2)
              cap.oap.motion_type = MLINE;
          // #ifdef FEAT_FOLDING
          // if (cap->oap->op_type == OP_NOP
          //            && (fdo_flags & FDO_SEARCH) && KeyTyped)
          // 	    foldOpenCursor();
          // #endif
      }
      // always do this since might turn off highlighting if no match found
      Options.newSearch();

      // "/$" will put the cursor after the end of the line, may need to
      // correct that here
      //check_cursor();
  }

  static private void nv_csearch(CMDARG cap, int dir, boolean type) {
    cap.oap.motion_type = MCHAR;
    if (dir == BACKWARD)
      cap.oap.inclusive = false;
    else
      cap.oap.inclusive = true;
    if (cap.nchar >= 0x100
		|| !searchc(cap.nchar, dir, type, cap.count1)) {
      clearopbeep(cap.oap);
    } else {
      G.curwin.w_set_curswant = true;
      // NEEDSWORK: visual mode: adjust_for_sel(cap);
    }
    if (G.fdo_flags().contains(FDO_HOR) && KeyTyped
					       && cap.oap.op_type == OP_NOP)
      foldOpenCursor();
  }

  /**
   * Handle Normal mode "%" command.
   */
  static private void nv_percent(CMDARG cap) {

    cap.oap.inclusive = true;
    if (cap.count0 != 0) {	    // {cnt}% : goto {cnt} percentage in file
      if (cap.count0 > 100) {
	clearopbeep(cap.oap);
      } else {
	cap.oap.motion_type = MLINE;
	MarkOps.setpcmark();
	// round up, so CTRL-G will give same value
        int line = (G.curbuf.getLineCount()
                   * cap.count0 + 99) / 100;
        G.curwin.w_cursor.set(line, 0);
	Edit.beginline(BL_SOL | BL_FIX);
      }
    } else {	    // "%" : go to matching paren
      cap.oap.motion_type = MCHAR;
      boolean usePlatform = G.p_pbm & ViManager.getPlatformFindMatch();
      if(usePlatform) {
        ViFPOS fpos = G.curwin.w_cursor.copy();
        int endingOffset = fpos.getOffset(); // this assumes failture
        G.curwin.findMatch();
        endingOffset = G.curwin.getCaretPosition();

        if(endingOffset == fpos.getOffset()) {
          clearopbeep(cap.oap);
        } else {
          MarkOps.setpcmark(fpos);
          G.curwin.w_set_curswant = true;
          adjust_for_sel(cap);
        }
      } else {
        // Use the internal findmatch!
        ViFPOS fpos = findmatch(cap.oap, NUL);
        if (fpos == null) {
          clearopbeep(cap.oap);
        } else {
          MarkOps.setpcmark();
          G.curwin.w_cursor.set(fpos);
          //G.curwin.setCaretPosition(fpos.getOffset());
          G.curwin.w_set_curswant = true;
          adjust_for_sel(cap);
        }
      }
    }
  }

/**
 * "[" and "]" commands.
 * cap.arg is BACKWARD for "[" and FORWARD for "]".
 */
    static void
nv_brackets(CMDARG cap, int dir)
{
    ViFPOS	new_pos;
    ViFPOS	prev_pos;
    ViFPOS	pos = null;	    /* init for GCC */
    ViFPOS	old_pos;	    /* cursor position before command */
    int		flag;
    long	n;
    char	findc;
    int		c;

    final ViFPOS cursor = G.curwin.w_cursor.copy();
    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    old_pos = cursor.copy();
//#ifdef FEAT_VIRTUALEDIT ...

// #ifdef FEAT_SEARCHPATH
//     /*
//      * "[f" or "]f" : Edit file under the cursor (same as "gf")
//      */
//     if (cap.nchar == 'f')
// 	nv_gotofile(cap);
//     else
// #endif
//
// #ifdef FEAT_FIND_ID
//     /*
//      * Find the occurence(s) of the identifier or define under cursor
//      * in current and included files or jump to the first occurence.
//      *
//      *			search	     list	    jump
//      *		      fwd   bwd    fwd	 bwd	 fwd	bwd
//      * identifier     "]i"  "[i"   "]I"  "[I"	"]^I"  "[^I"
//      * define	 "]d"  "[d"   "]D"  "[D"	"]^D"  "[^D"
//      */
//     if (vim_strchr((char_u *)
// #ifdef EBCDIC
// 		"iI\005dD\067",
// #else
// 		"iI\011dD\004",
// #endif
// 		cap.nchar) != null)
//     {
// 	char_u	*ptr;
// 	int	len;
//
// 	if ((len = find_ident_under_cursor(&ptr, FIND_IDENT)) == 0)
// 	    clearop(cap.oap);
// 	else
// 	{
// 	    find_pattern_in_path(ptr, 0, len, true,
// 		cap.count0 == 0 ? !isupper(cap.nchar) : false,
// 		((cap.nchar & 0xf) == ('d' & 0xf)) ?  FIND_DEFINE : FIND_ANY,
// 		cap.count1,
// 		isupper(cap.nchar) ? ACTION_SHOW_ALL :
// 			    islower(cap.nchar) ? ACTION_SHOW : ACTION_GOTO,
// 		cap.cmdchar == ']' ? G.curwin.w_cursor.getLine() + 1 : 1,
// 		MAXLNUM);
// 	    G.curwin.w_set_curswant = true;
// 	}
//     }
//     else
// #endif

    /*
     * "[{", "[(", "]}" or "])": go to Nth unclosed '{', '(', '}' or ')'
     * "[#", "]#": go to start/end of Nth innermost #if..#endif construct.
     * "[/", "[*", "]/", "]*": go to Nth comment start/end.
     * "[m" or "]m" search for prev/next start of (Java) method.
     * "[M" or "]M" search for prev/next end of (Java) method.
     */
    if (  (cap.cmdchar == '['
		&& vim_strchr("{(*/#mM", 0, cap.nchar) >= 0)
	    || (cap.cmdchar == ']'
		&& vim_strchr("})*/#mM", 0, cap.nchar) >= 0))
    {
	if (cap.nchar == '*')
	    cap.nchar = '/';
	new_pos = null; // new_pos.setLine(0);
	prev_pos = null; // prev_pos.setLine(0);
	if (cap.nchar == 'm' || cap.nchar == 'M')
	{
	    if (cap.cmdchar == '[')
		findc = '{';
	    else
		findc = '}';
	    n = 9999;
	}
	else
	{
	    findc = cap.nchar;
	    n = cap.count1;
	}
	for ( ; n > 0; --n)
	{
	    if ((pos = findmatchlimit(cursor, cap.oap, findc,
		(cap.cmdchar == '[') ? FM_BACKWARD : FM_FORWARD, 0)) == null)
	    {
		if (new_pos == null)	/* nothing found */
		{
		    if (cap.nchar != 'm' && cap.nchar != 'M')
			clearopbeep(cap.oap);
		}
		else
		    pos = new_pos;	/* use last one found */
		break;
	    }
            prev_pos = new_pos == null ? null : new_pos.copy();
	    cursor.set(pos);// G.curwin.w_cursor = *pos;
	    new_pos = pos;
            //LOG.log(FINER, "prev {0}, new {1}", new Object[] {prev_pos, new_pos});
	}
	cursor.set(old_pos);

	/*
	 * Handle "[m", "]m", "[M" and "[M".  The findmatchlimit() only
	 * brought us to the match for "[m" and "]M" when inside a method.
	 * Try finding the '{' or '}' we want to be at.
	 * Also repeat for the given count.
	 */
	if (cap.nchar == 'm' || cap.nchar == 'M')
	{
	    /* norm is true for "]M" and "[m" */
	    boolean norm = ((findc == '{') == (cap.nchar == 'm'));

	    n = cap.count1;
	    /* found a match: we were inside a method */
	    if (prev_pos != null)
	    {
		pos = prev_pos;
		cursor.set(prev_pos);
		if (norm)
		    --n;
	    }
	    else
		pos = null;
	    while (n > 0)
	    {
		for (;;)
		{
		    if ((findc == '{' ? dec(cursor) : incV7(cursor)) < 0)
		    {
			/* if not found anything, that's an error */
			if (pos == null)
			    clearopbeep(cap.oap);
			n = 0;
			break;
		    }
		    c = gchar_pos(cursor);
		    if (c == '{' || c == '}')
		    {
			/* Must have found end/start of class: use it.
			 * Or found the place to be at. */
			if ((c == findc && norm) || (n == 1 && !norm))
			{
			    new_pos = cursor.copy();
			    pos = new_pos;
			    n = 0;
			}
			/* if no match found at all, we started outside of the
			 * class and we're inside now.  Just go on. */
			else if (new_pos == null)
			{
			    new_pos = cursor.copy();
			    pos = new_pos;
			}
			/* found start/end of other method: go to match */
			else if ((pos = findmatchlimit(cursor,
                                cap.oap, findc,
			    (cap.cmdchar == '[') ? FM_BACKWARD : FM_FORWARD,
								  0)) == null)
			    n = 0;
			else
			    cursor.set(pos); // G.curwin.w_cursor = *pos;
			break;
		    }
		}
		--n;
	    }
	    cursor.set(old_pos);
	    if (pos == null && new_pos != null)
		clearopbeep(cap.oap);
	}
	if (pos != null)
	{
            // up to this point, w_cursor should no have been changed.
	    setpcmark();
	    G.curwin.w_cursor.set(pos); // G.curwin.w_cursor = *pos;
	    G.curwin.w_set_curswant = true;
// #ifdef FEAT_FOLDING
// 	    if ((fdo_flags & FDO_BLOCK) && KeyTyped
// 					       && cap.oap.op_type == OP_NOP)
// 		foldOpenCursor();
// #endif
	}
    }

    /*
     * "[[", "[]", "]]" and "][": move to start or end of function
     */
    else if (cap.nchar == '[' || cap.nchar == ']')
    {
	if (cap.nchar == cap.cmdchar)		    /* "]]" or "[[" */
	    flag = '{';
	else
	    flag = '}';		    /* "][" or "[]" */

	G.curwin.w_set_curswant = true;
	/*
	 * Imitate strange Vi behaviour: When using "]]" with an operator
	 * we also stop at '}'.
	 */
	if (!findpar(cap, dir, cap.count1, flag,
	      (cap.oap.op_type != OP_NOP && dir == FORWARD && flag == '{')))
	    clearopbeep(cap.oap);
	else
	{
	    if (cap.oap.op_type == OP_NOP)
		Edit.beginline(BL_WHITE | BL_FIX);
// #ifdef FEAT_FOLDING
// 	    if ((fdo_flags & FDO_BLOCK) && KeyTyped && cap.oap.op_type == OP_NOP)
// 		foldOpenCursor();
// #endif
	}
    }

//    /*
//     * "[p", "[P", "]P" and "]p": put with indent adjustment
//     */
//    else if (cap.nchar == 'p' || cap.nchar == 'P')
//    {
//	if (!checkclearopq(cap.oap))
//	{
//	    prep_redo_cmd(cap);
//	    do_put(cap.oap.regname,
//	      (cap.cmdchar == ']' && cap.nchar == 'p') ? FORWARD : BACKWARD,
//						  cap.count1, PUT_FIXINDENT);
//	}
//    }
//
//    /*
//     * "['", "[`", "]'" and "]`": jump to next mark
//     */
//    else if (cap.nchar == '\'' || cap.nchar == '`')
//    {
//	pos = &G.curwin.w_cursor;
//	for (n = cap.count1; n > 0; --n)
//	{
//	    prev_pos = *pos;
//	    pos = getnextmark(pos, cap.cmdchar == '[' ? BACKWARD : FORWARD,
//							  cap.nchar == '\'');
//	    if (pos == null)
//		break;
//	}
//	if (pos == null)
//	    pos = &prev_pos;
//	nv_cursormark(cap, cap.nchar == '\'', pos);
//    }
//
//#ifdef FEAT_MOUSE
//    /*
//     * [ or ] followed by a middle mouse click: put selected text with
//     * indent adjustment.  Any other button just does as usual.
//     */
//    else if (cap.nchar >= K_LEFTMOUSE && cap.nchar <= K_RIGHTRELEASE)
//    {
//	(void)do_mouse(cap.oap, cap.nchar,
//		       (cap.cmdchar == ']') ? FORWARD : BACKWARD,
//		       cap.count1, PUT_FIXINDENT);
//    }
//#endif /* FEAT_MOUSE */
//
//#ifdef FEAT_FOLDING
//    /*
//     * "[z" and "]z": move to start or end of open fold.
//     */
//    else if (cap.nchar == 'z')
//    {
//	if (foldMoveTo(false, cap.cmdchar == ']' ? FORWARD : BACKWARD,
//							 cap.count1) == FAIL)
//	    clearopbeep(cap.oap);
//    }
//#endif
//
//#ifdef FEAT_DIFF
//    /*
//     * "[c" and "]c": move to next or previous diff-change.
//     */
//    else if (cap.nchar == 'c')
//    {
//	if (diff_move_to(cap.cmdchar == ']' ? FORWARD : BACKWARD,
//							 cap.count1) == FAIL)
//	    clearopbeep(cap.oap);
//    }
//#endif
//
//#ifdef FEAT_SPELL
//    /*
//     * "[s", "[S", "]s" and "]S": move to next spell error.
//     */
//    else if (cap.nchar == 's' || cap.nchar == 'S')
//    {
//	setpcmark();
//	for (n = 0; n < cap.count1; ++n)
//	    if (spell_move_to(curwin, cap.cmdchar == ']' ? FORWARD : BACKWARD,
//			  cap.nchar == 's' ? true : false, false, null) == 0)
//	    {
//		clearopbeep(cap.oap);
//		break;
//	    }
//# ifdef FEAT_FOLDING
//	if (cap.oap.op_type == OP_NOP && (fdo_flags & FDO_SEARCH) && KeyTyped)
//	    foldOpenCursor();
//# endif
//    }
//#endif

    /* Not a valid cap.nchar. */
    else
	clearopbeep(cap.oap);
}
  
  /*
 * Handle "(" and ")" commands.
 */
 
  static private void nv_brace(CMDARG cap, int dir) {

    cap.oap.motion_type = MCHAR;

    if (cap.cmdchar == ')')
      cap.oap.inclusive = false;
    else
      cap.oap.inclusive = true;

    G.curwin.w_set_curswant = true;

    if (!findsent(dir, cap.count1))
      clearopbeep(cap.oap);
  }
  
/*
 * Handle the "{" and "}" commands.
 */

static private void nv_findpar(CMDARG cap, int dir)
{
  cap.oap.motion_type = MCHAR;
  cap.oap.inclusive = false;
  G.curwin.w_set_curswant = true;
  if (!findpar(cap, dir, cap.count1, NUL, false))
    clearopbeep(cap.oap);
}

  /**
   * Handle the "r" command.
   */
  static private void nv_replace(CMDARG cap) {

    if (checkclearop(cap.oap))
      return;

    if (G.VIsual_active) {
      nv_operator(cap);
      return;
    }

    // NEEDSWORK: be very lazy, use Edit.edit for the 'r' command

    //
    // Replacing with a TAB is done by edit(), because it is complicated when
    // 'expandtab', 'smarttab' or 'softtabstop' is set.
    // Other characters are done below to avoid problems with things like
    // CTRL-V 048 (for edit() this would be R CTRL-V 0 ESC).
    //

    // if (cap.nchar == '\t' && (curbuf.b_p_et || p_sta)) ....
    GetChar.stuffnumReadbuff(cap.count1);
    GetChar.stuffcharReadbuff('R');
    GetChar.stuffcharReadbuff(cap.nchar);
    GetChar.stuffcharReadbuff(ESC);
    GetChar.disableRedoTrackingOneEdit();
    return;
  }
  
  /**
   * 'o': Exchange start and end of Visual area.
   * 'O': same, but in block mode exchange left and right corners.
   */
  static private  void	v_swap_corners (CMDARG cap) {

    final ViFPOS cursor = G.curwin.w_cursor;
    if ((cap.cmdchar == 'O') && G.VIsual_mode == Util.ctrl('V'))
    {
        MutableInt left = new MutableInt();
        MutableInt right = new MutableInt();
        ViFPOS old_cursor = cursor.copy();

        getvcols(old_cursor, G.VIsual, left, right);
        cursor.set(G.VIsual.getLine(), 0);
        coladvance(left.getValue());
        G.VIsual.set(cursor);
        cursor.set(old_cursor.getLine(), 0);
        coladvance(right.getValue());
        G.curwin.updateCurswant(cursor, right.getValue());
        if (cursor.getColumn() == old_cursor.getColumn())
        {
            cursor.set(G.VIsual.getLine(), 0);
            coladvance(right.getValue());
            G.VIsual.set(cursor);
            cursor.set(old_cursor.getLine(), 0);
            coladvance(left.getValue());
            G.curwin.updateCurswant(cursor, left.getValue());
        }
    }
    if (cap.cmdchar != 'O' || G.VIsual_mode != Util.ctrl('V'))
    {
        ViFPOS old_cursor = cursor.copy();
        cursor.set(G.VIsual);
        G.VIsual =  old_cursor;
        G.curwin.w_set_curswant = true;
    }
    v_updateVisualState();
  }
 
  static public void v_updateVisualState(ViTextView tv) {
    assert G.curwin == tv;
    v_updateVisualState();
  }
  static void v_updateVisualState() {
    if(!G.VIsual_active) {
      G.curbuf.clearVisualState();
    }
    
    displaySelectState(G.curbuf.getVisualSelectStateString());
    
    G.curwin.updateVisualState();
  }
  
  /**
   * "R".
   */
  static private void nv_Replace(CMDARG cap)
  {
    if (G.VIsual_active)		/* "R" is replace lines */
    {
      cap.cmdchar = 'c';
      G.VIsual_mode = 'V';
      nv_operator(cap);
    } else if (!checkclearopq(cap.oap)) {
      if (u_save_cursor() == OK) {
        beginInsertUndo();
        try {
          Edit.edit('R', false, cap.count1);
        } finally {
            if(!editBusy)
              endInsertUndo();
        }
      }
    }
  }

  /**
   * Swap case for "~" command, when it does not work like an operator.
   */
  static private  void	n_swapchar (final CMDARG cap) {

    if (checkclearopq(cap.oap))
      return;

    if (Util.lineempty(G.curwin.w_cursor.getLine())
                && G.p_ww_tilde) {
      clearopbeep(cap.oap);
      return;
    }

    prep_redo_cmd(cap);

    if (u_save_cursor() == FAIL)
      return;

    runUndoable(new Runnable() {
      @Override
        public void run() {
          for (int n = cap.count1; n > 0; --n) {
            swapchar(cap.oap.op_type,G.curwin.w_cursor);
            inc_cursor();
            if (gchar_cursor() == '\n') {
              if (      G.p_ww_tilde
                  && G.curwin.w_cursor.getLine() < G.curbuf.getLineCount())
              {
                G.curwin.w_cursor.set(G.curwin.w_cursor.getLine() + 1, 0);
                // redraw_curbuf_later(NOT_VALID);
              }
              else
                break;
            }
          }

          adjust_cursor();
          G.curwin.w_set_curswant = true;
        }
    });
    // changed();

    /* assume that the length of the line doesn't change, so w_botline
     * remains valid */
    // update_screenline();
  }

  static private  void	nv_cursormark (CMDARG cap, boolean flag, ViMark mark) {
    if (MarkOps.check_mark(mark) == FAIL)
      clearop(cap.oap);
    else
    {
      if (cap.cmdchar == '\'' || cap.cmdchar == '`')
	MarkOps.setpcmark();
      G.curwin.w_cursor.set(mark);
      if(flag)
        Edit.beginline(BL_WHITE | BL_FIX);
      else
        adjust_cursor();
    }
    cap.oap.motion_type = flag ? MLINE : MCHAR;
    cap.oap.inclusive = false;		/* ignored if not MCHAR */
    G.curwin.w_set_curswant = true;
  }
  
  /**
   * Handle commands that are operators in Visual mode.
   */
  static private  void	v_visop (CMDARG cap) {
      String trans = "YyDdCcxdXdAAIIrr";

    /* Uppercase means linewise, except in block mode, then "D" deletes till
     * the end of the line, and "C" replaces til EOL */
    if (Character.isUpperCase(cap.cmdchar))
    {
       if (G.VIsual_mode != Util.ctrl('V'))
           G.VIsual_mode = 'V';
       else if (cap.cmdchar == 'C' || cap.cmdchar == 'D')
            G.curwin.updateCurswant(null, MAXCOL);
    }
    cap.cmdchar = Util.vim_strchr(trans, cap.cmdchar).charAt(1);
    nv_operator(cap);
  }

  static String[] nv_optrans_ar = new String[] { "dl", "dh", "d$", "c$",
    					         "cl", "cc", "yy", ":s\r",
                                                 };
  /**
   * Translate a command into another command.
   */
  static private  void	nv_optrans (CMDARG cap) {

    String str = "xXDCsSY&";
    if (!checkclearopq(cap.oap))
    {
      if (cap.count0 != 0) {
	GetChar.stuffnumReadbuff(cap.count0);
      }

      GetChar.stuffReadbuff(nv_optrans_ar[str.indexOf(cap.cmdchar)]);
    }
  }

  /**
   * Handle "'" and "`" commands.
   */
  static private void nv_gomark(CMDARG cap, boolean flag) {
    ViMark	pos;

    pos = MarkOps.getmark(cap.nchar, (cap.oap.op_type == OP_NOP));
    //if (pos == MarkOps.otherFile)
    if (pos instanceof Filemark)
    {	    /* jumped to other file */
      if (flag) {
	check_cursor_lnumBeginline(BL_WHITE | BL_FIX);
	// Edit.beginline(BL_WHITE | BL_FIX);
      } else {
	adjust_cursor();
      }
    } else {
      nv_cursormark(cap, flag, pos);
    }
  }

  /**
   * Handle CTRL-O and CTRL-I commands.
   */
  static private void nv_pcmark(CMDARG cap)
  throws NotSupportedException {
    //G.curwin.jumpList(op, cap.count1);
    ViMark	fpos;

    if (!checkclearopq(cap.oap)) {
      fpos = MarkOps.movemark(cap.count1);
      if (fpos == MarkOps.otherFile) {      /* jump to other file */
        notSup("nv_pcmark other file");
	G.curwin.w_set_curswant = true;
	adjust_cursor();
      } else if (fpos != null) {	    /* can jump */
	nv_cursormark(cap, false, fpos);
      } else {
	clearopbeep(cap.oap);
      }
    }
  }


  /**
   * Handle '"' command.
   */
  static private  void	nv_regname (CMDARG cap, MutableInt opnump) {
    if (checkclearop(cap.oap))
      return;
    if (cap.nchar != NUL && valid_yank_reg(cap.nchar, false)) {
      cap.oap.regname = cap.nchar;
      opnump.setValue(cap.count0);    // remember count before '"'
    } else {
      clearopbeep(cap.oap);
    }
  }
  
  /**
   * Handle "v", "V" and "CTRL-V" commands.
   * Also for "gh", "gH" and "g^H" commands.
   */
  
  static private  void	nv_visual(CMDARG cap, boolean selectmode) {
      if (G.VIsual_active) {/* change Visual mode */
          if (G.VIsual_mode == cap.cmdchar) {    /* stop visual mode */
              end_visual_mode();
          } else {/* toggle char/block mode */
              /*or char/line mode */
              G.VIsual_mode = cap.cmdchar;
              showmode();
              /* update the screen cursor position */
              v_updateVisualState();
              //G.curwin.setWCurswant(G.curwin.getCaretPosition());
          }
          update_curbuf(NOT_VALID);/* update the inversion */
      } else { /* start Visual mode */
          if (cap.count0 > 0) { /*use previously selected part */
              if (resel_VIsual_mode == NUL)   /* there is none */
              {
                  Util.beep_flush();
                  return;
              }
              G.VIsual = G.curwin.w_cursor.copy();
              G.VIsual_active = true;
              G.VIsual_reselect = true;
              if (!selectmode)
              /* start Select mode when 'selectmode' contains "cmd" */
                  may_start_select('c');
              if (G.p_smd)
                  redraw_cmdline = true;  /* show visual mode later */
              /*
               * For V and ^V, we multiply the number of lines even if there
               * was only one -- webb
               */
              if (resel_VIsual_mode != 'v' || resel_VIsual_line_count > 1)
              {
                  int line = G.curwin.w_cursor.getLine()
                                    + resel_VIsual_line_count * cap.count0 - 1;
                  if(line > G.curbuf.getLineCount())
                      line = G.curbuf.getLineCount();
                  // Not sure about how column should be set, but at least
                  // make sure it stays in the correct line
                  int col = check_cursor_col(line,
                                            G.curwin.w_cursor.getColumn());
                  G.curwin.w_cursor.set(line, col);
              }
              G.VIsual_mode = resel_VIsual_mode;
              if (G.VIsual_mode == 'v')
              {
                  if (resel_VIsual_line_count <= 1)
                      // NEEDSWORK: ? can column overflow here?
                    G.curwin.w_cursor.set(G.curwin.w_cursor.getLine(),
                              G.curwin.w_cursor.getColumn()
                                + resel_VIsual_col * cap.count0 - 1);
                  else
                    G.curwin.w_cursor.set(G.curwin.w_cursor.getLine(),
                                          resel_VIsual_col);
              }
              if (resel_VIsual_col == MAXCOL)
              {
                  G.curwin.updateCurswant(null, MAXCOL);
                  coladvance(MAXCOL);
              }
              else if (G.VIsual_mode == Util.ctrl('V'))
              {
                  //TODO: FIXME_VISUAL BLOCK MODE
                  //validate_virtcol();
                          /*G.curwin.w_virtcol*/
                  G.curwin.updateCurswant(null,
                                          G.curwin.w_cursor.getColumn()
                                          + resel_VIsual_col * cap.count0 - 1);
                  coladvance(G.curwin.w_curswant);
              }
              else
                  G.curwin.w_set_curswant = true;
              update_curbuf(NOT_VALID); /* show the inversion */
              /* update the screen cursor position */
              v_updateVisualState();
          } else {
              if (!selectmode)
                  // start Select mode when 'selectmode' contains "cmd"
                  may_start_select('c');
              n_start_visual_mode(cap.cmdchar);
              /* update the screen cursor position */
              convertSelectionToVisual();
              v_updateVisualState();
          }
      }
      ui_cursor_shape();
  }

  /**
   * This is used when initiating visual mode. If there is a
   * JavaTextSelection then use it to set the visual mode boundaries.
   */
  static private void convertSelectionToVisual() {
    if(G.VIsual_active && G.curwin.hasSelection()) {
      // convert a selection into a visual mode thing,
      // set visual start, G.VIsual, to the offset
      
      // convert a selection into a visual mode thing,
      // set visual start, G.VIsual, to the textMark
      ViFPOS fpos = G.curwin.w_cursor.copy();

      int textMark = G.curwin.getMarkPosition();
      int textDot = fpos.getOffset();

      if(   G.p_sel.charAt(0) != 'e') {
        if(textMark < textDot)
          G.curwin.w_cursor.set(fpos.getOffset()-1);
        else
          --textMark;
      }

      fpos.set(G.curbuf.getLineNumber(textMark),
              G.curbuf.getColumnNumber(textMark));

      G.VIsual = fpos;
      
      // converting a selection into visual mode, clear the selection
      G.curwin.clearSelection();
    }
  }
  
  /*
  * Start Select mode, if "c" is in 'selectmode' and not in a mapping or menu.
  */
  static void may_start_select(char c) {
      G.VIsual_select = (GetChar.stuff_empty() && typebuf_typed()
        && (vim_strchr(G.p_slm, 0, c) >= 0));
  }
  
  /*
   * Start Visual mode "c".
   * Should set VIsual_select before calling this.
   */
  static private  void	n_start_visual_mode(char c) {
      G.VIsual = G.curwin.w_cursor.copy();
      G.VIsual_mode = c;
      G.VIsual_active = true;
      G.VIsual_reselect = true;

      //
      // VISUAL FOLD HANDLING
      // vim changes the actual bounds here, opens folds....
      // foldAdjustVisual();
      //

      if (G.p_smd)
          redraw_cmdline = true; /* show visual mode later */
//#ifdef USE_CLIPBOARD
    /* Make sure the clipboard gets updated.  Needed because start and
     * end may still be the same, and the selection needs to be owned */
//    clipboard.vmode = NUL;
//#endif
      //update_screenline();/* start the inversion */
  }

  // VISUAL FOLD HANDLING
  /**
   * Adjust the Visual area to include any fold at the start or end completely.
   */
  static void foldAdjustVisual()
  {
    if (!G.VIsual_active) // || !hasAnyFolding(curwin)
      return;

    foldAdjustVisual(G.curwin, G.curbuf, G.VIsual, G.curwin.w_cursor);
  }

  /** does not depend on G */
  static void foldAdjustVisual(ViTextView win, Buffer buf,
                               ViFPOS start, ViFPOS end)
  {
    // ViFPOS start, end;
    
    if (!G.VIsual_active) // || !hasAnyFolding(curwin)
      return;

    if(start.compareTo(end) > 0) {
      ViFPOS t = start;
      start = end;
      end = t;
    }

    MutableInt mi = new MutableInt();
    if(win.hasFolding(start.getLine(), mi, null)) {
      start.set(mi.getValue(), 0);
    }
    if(win.hasFolding(end.getLine(), null, mi)) {
      int col = lineLength(buf, mi.getValue());
      if(col > 0 && G.p_sel.charAt(0) != 'o')
        --col;
      end.set(mi.getValue(), col);
    }
  }

  static private void nv_g_cmd(CMDARG cap, StringBuilder searchbuff)
  throws NotSupportedException {
    ViFPOS tpos;
    int t = 0;
    boolean flag = false;
    switch (cap.nchar) {
      /*case 'x':
        Yankreg r1 = Misc.get_register('a', true);
        Yankreg r2 = Misc.get_register('a', false);
        Yankreg r3 = Misc.get_register('a', false);
        get_yank_register('z', false);
        y_current.set(r2);
        break;*/

//    case ',':
//      nv_pcmark(JLOP.NEXT_CHANGE, cap);
//      break;
//    case ';':
//      nv_pcmark(JLOP.PREV_CHANGE, cap);
//      break;

      //
      // "gv": Reselect the previous Visual area.  If Visual already active,
      // exchange previous and current Visual area.
      //
      case 'v':
        if (checkclearop(oap))
            break;
        if (     MarkOps.check_mark(G.curbuf.b_visual_start, true) == FAIL
              || MarkOps.check_mark(G.curbuf.b_visual_end, true) == FAIL
              // || ! G.curbuf.b_visual_start.isValid()
              || G.curbuf.b_visual_start.getLine() > G.curbuf.getLineCount()
              // || ! G.curbuf.b_visual_end.isValid()
            )
            Util.beep_flush();
        else
        {
            final ViFPOS cursor = G.curwin.w_cursor;
            /* set w_cursor to the start of the Visual area, tpos to the end */
            if (G.VIsual_active)
            {
                char c = G.VIsual_mode;
                G.VIsual_mode = G.curbuf.b_visual_mode;
                G.curbuf.b_visual_mode = c;
                tpos = G.curbuf.b_visual_end.copy();
                G.curbuf.b_visual_end.setMark(cursor);
                cursor.set(G.curbuf.b_visual_start);
                G.curbuf.b_visual_start.setMark(G.VIsual);
            }
            else
            {
                G.VIsual_mode = G.curbuf.b_visual_mode;
                tpos = G.curbuf.b_visual_end.copy();
                cursor.set(G.curbuf.b_visual_start);
            }

            G.VIsual_active = true;
            G.VIsual_reselect = true;

            /* Set Visual to the start and w_cursor to the end of the Visual
             * area.  Make sure they are on an existing character. */
            adjust_cursor();
            G.VIsual =  cursor.copy();
            cursor.set(tpos);
            adjust_cursor();
            //update_topline();
            /*
             * When called from normal "g" command: start Select mode when
             * 'selectmode' contains "cmd".  When called for K_SELECT, always
             * start Select mode
             */
//TODO: FIXME_VISUAL SELECT MODE
            if (searchbuff == null)
                G.VIsual_select = true;
            else
                may_start_select('c');
            //#ifdef USE_MOUSE
            //setmouse();
            //#endif
            //#ifdef USE_CLIPBOARD
            ///* Make sure the clipboard gets updated.  Needed because start and
            //* end are still the same, and the selection needs to be owned */
            //clipboard.vmode = NUL;
            //#endif
            update_curbuf(NOT_VALID);
            showmode();
            ui_cursor_shape();
        }
        break;

      //
      // "gj" and "gk" two new funny movement keys -- up and down
      // movement based on *screen* line rather than *file* line.
      //
      case 'j':
      case K_DOWN:
	// with 'nowrap' it works just like the normal "j" command
        if(!G.curwin.w_p_wrap
              || G.curwin.hasFolding(G.curwin.w_cursor.getLine(), null, null)) {
          oap.motion_type = MLINE;
          t = cursor_down(cap.count1, oap.op_type == OP_NOP);
        } else
          t = nv_screengo(oap, DIR.FORWARD, cap.count1);
        if(t == FAIL)
          clearopbeep(oap);
        break;

      case 'k':
      case K_UP:
	// with 'nowrap' it works just like the normal "k" command
        if(!G.curwin.w_p_wrap
              || G.curwin.hasFolding(G.curwin.w_cursor.getLine(), null, null)) {
          oap.motion_type = MLINE;
          t = cursor_up(cap.count1, oap.op_type == OP_NOP);
        } else
          t = nv_screengo(oap, DIR.BACKWARD, cap.count1);
        if(t == FAIL)
          clearopbeep(oap);
        break;

      //
      // "gJ": join two lines without inserting a space.
      //
      case 'J':
        nv_join(cap);
        break;
      //
      // "g0", "g^" and "g$": Like "0", "^" and "$" but for screen lines.
      // "gm": middle of "g0" and "g$".
      //
      case '^':
        flag = true;
        /*FALLTHROUGH*/
      case '0':
      case K_HOME:
        {
          cap.oap.motion_type = MCHAR;
          cap.oap.inclusive = false;
          ViFPOS fpos = G.curwin.w_cursor.copy();
          G.curwin.viewLineEdge(EDGE.LEFT, fpos);

          if(flag) {
            char c;
            do {
              c = gchar_pos(fpos);
            } while(vim_iswhite(c) && oneright(fpos) == OK);
          }
          G.curwin.w_cursor.set(fpos);
          G.curwin.w_set_curswant = true;
          break;
        }

      case 'm':
        {
          cap.oap.motion_type = MCHAR;
          cap.oap.inclusive = false;
          ViFPOS fpos = G.curwin.w_cursor.copy();
          G.curwin.viewLineEdge(EDGE.MIDDLE, fpos);
          G.curwin.w_cursor.set(fpos);
          G.curwin.w_set_curswant = true;
          break;
        }

      case '$':
      case K_END:
        {
          cap.oap.motion_type = MCHAR;
          cap.oap.inclusive = true;
          int n = cap.count1;
          boolean ok = true;
          ViFPOS fpos = G.curwin.w_cursor.copy();
          if(n > 1) {
            ok = G.curwin.viewLineUpDown(DIR.FORWARD, n - 1, fpos);
          }
          G.curwin.viewLineEdge(EDGE.RIGHT, fpos);

          //
          // Back up the cursor off of a newline unless one of
          // - empty line
          // - visual mode and 'selection' is not "old"
          if(gchar_pos(fpos) == '\n'
                  && !(fpos.getColumn() == 0
                       || (G.VIsual_active && G.p_sel.charAt(0) != 'o')))
            fpos.decColumn();
          G.curwin.w_cursor.set(fpos);

          if(G.curwin.w_p_wrap) {
            G.curwin.updateCurswant(null, MAXCOL);
          } else {
            G.curwin.w_set_curswant = true;
          }

          if(!ok)
            clearopbeep(oap);
        }
        break;
      //
      // "g*" and "g#", like "*" and "#" but without using "\<" and "\>"
      //
      case '*':
      case '#':
          nv_ident(cap, searchbuff);
          break;

      //
      // ge and gE: go back to end of word
      //
      case 'e':
      case 'E':
          oap.motion_type = MCHAR;
          G.curwin.w_set_curswant = true;
          oap.inclusive = true;
          if (bckend_word(cap.count1, cap.nchar == 'E', false) == FAIL)
              clearopbeep(oap);
          break;
      //
      // "gg": Goto the first line in file.  With a count it goes to
      // that line number like for "G". -- webb
      //
      case 'g':
        nv_goto(cap, 1);
        break;

      //
      //   Two-character operators:
      //   "gq"	    Format text
      //   "gw"	    Format text and keep cursor position
      //   "g~"	    Toggle the case of the text.
      //   "gu"	    Change text to lower case.
      //   "gU"	    Change text to upper case.
      //   "g?"	    rot13 encoding
      //   "g@"	    call 'operatorfunc'
      //
      case 'q':
      case '~':
      case 'u':
      case 'U':
        nv_operator(cap);
        break;

      // "gP" and "gp": same as "P" and "p" but leave cursor just after new text
      case 'p':
      case 'P':
        nv_put(cap);
        break;

    case 't':
        G.curwin.tabOperation(TABOP.NEXT_TAB, cap.count0);
        break;

    case 'T':
        G.curwin.tabOperation(TABOP.PREV_TAB, cap.count0);
        break;

      default:
        notSup("g" + String.valueOf(cap.nchar));
        break;
    }
  }

  /**
   * Handle "o" and "O" commands.
   */
  static private void n_opencmd(CMDARG cap)
  {

    ViFPOS fpos = G.curwin.w_cursor;
    if (!checkclearopq(cap.oap)) {
      // if (u_save((curwin.w_cursor.lnum - (cap.cmdchar == 'O' ? 1 : 0)) .....
      // NEEDSWORK: would like the beginUndo only if actually making changes
      boolean startEdit = false;  
      beginInsertUndo();
      try {
        startEdit = open_line(cap.cmdchar == 'O' ? BACKWARD : FORWARD,
                                   true, false, 0);
        if(startEdit)
	      Edit.edit(cap.cmdchar, true, cap.count1);
      } finally {
          if(!editBusy)
            endInsertUndo();
      }
    }
    return;
  }
  
  /*
   * Handle "U" command.
   */
  static private  void	nv_Undo (CMDARG cap) throws NotSupportedException {
         // In Visual mode and typing "gUU" triggers an operator
    if (G.VIsual_active || cap.oap.op_type == OP_UPPER)
    {
         // translate "gUU" to "gUgU"
          cap.cmdchar = 'g';
          cap.nchar = 'U';
          nv_operator(cap);
    } else if (!checkclearopq(cap.oap)) {
          //u_undoline();
          notSup("nv_Undo");
          G.curwin.w_set_curswant = true;
    }

  }

  /**
   * Handle an operator command.
   */
  static private void nv_operator(CMDARG cap) {
    int	    op_type;

    op_type = get_op_type(cap.cmdchar, cap.nchar);

    if (op_type == cap.oap.op_type)	    // double operator works on lines
      nv_lineop(cap);
    else if (!checkclearop(cap.oap))
    {
      cap.oap.start = G.curwin.w_cursor.copy();
      cap.oap.op_type = op_type;
    }
  }

  /**
   * Handle linewise operator "dd", "yy", etc.
   */
  static private void nv_lineop(CMDARG cap) {
    cap.oap.motion_type = MLINE;
    if (Edit.cursor_down(cap.count1 - 1, cap.oap.op_type == OP_NOP) == FAIL)
      clearopbeep(cap.oap);
    else if (  cap.oap.op_type == OP_DELETE
	       || cap.oap.op_type == OP_LSHIFT
	       || cap.oap.op_type == OP_RSHIFT)
      Edit.beginline(BL_SOL | BL_FIX);
    else if (cap.oap.op_type != OP_YANK)	/* 'Y' does not move cursor */
      Edit.beginline(BL_WHITE | BL_FIX);
  }

  /**
   * Handle "|" command.
   */
  static private  void	nv_pipe (CMDARG cap) {

    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    Edit.beginline(0);
    if (cap.count0 > 0) {
      coladvance(cap.count0 - 1);
      G.curwin.updateCurswant(null, cap.count0 - 1);
    }
    else
      G.curwin.updateCurswant(null, 0);
    // keep curswant at the column where we wanted to go, not where
    // we ended; differs is line is too short
    G.curwin.w_set_curswant = false;
  }

  static private void nv_goto (CMDARG cap, int lnum) {
    cap.oap.motion_type = MLINE;
    MarkOps.setpcmark();
    
    // When a count is given, use it instead of the default lnum
    if(cap.count0 != 0)
        lnum = cap.count0;
    lnum = lnum < 1 ? 1
            : lnum > G.curbuf.getLineCount() ? G.curbuf.getLineCount() : lnum;
    gotoLine(lnum, BL_SOL | BL_FIX);
  }

  /**
   * Handle back-word command "b".
   */
  static void nv_bck_word(CMDARG cap, boolean type) {
    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    G.curwin.w_set_curswant = true;
    if (bck_word(cap.count1, type, false) == FAIL)
      clearopbeep(cap.oap);
    else if (G.fdo_flags().contains(FDO_HOR) && KeyTyped
					       && cap.oap.op_type == OP_NOP)
      foldOpenCursor();
  }

  /**
   * Handle word motion commands "e", "E", "w" and "W".
   */
  static private void nv_wordcmd(CMDARG cap, boolean type) {
    char       c;
    boolean    word_end;
    boolean    flag = false;

    //
    // Inclusive has been set for the "E" and "e" command.
    //
    word_end = cap.oap.inclusive;

    //
    // "cw" and "cW" are a special case.
    //
    if (!word_end && cap.oap.op_type == OP_CHANGE) {
      c = gchar_cursor();
      if (c != '\n') {			/* not an empty line */
	if (vim_iswhite(c)) {
	  //
	  // Reproduce a funny Vi behaviour: "cw" on a blank only
	  // changes one character, not all blanks until the start of
	  // the next word.  Only do this when the 'w' flag is included
	  // in 'cpoptions'.
          //
          // Note: the p_cpo_w flag is false for vi behavour.
	  //
	  if (cap.count1 == 1 && ! G.p_cpo_w) {
	    cap.oap.inclusive = true;
	    cap.oap.motion_type = MCHAR;
	    return;
	  }
	} else {
	  //
	  // This is a little strange. To match what the real Vi does,
	  // we effectively map 'cw' to 'ce', and 'cW' to 'cE', provided
	  // that we are not on a space or a TAB.  This seems impolite
	  // at first, but it's really more what we mean when we say
	  // 'cw'.
	  // Another strangeness: When standing on the end of a word
	  // "ce" will change until the end of the next wordt, but "cw"
	  // will change only one character! This is done by setting
	  // flag.
	  //
	  cap.oap.inclusive = true;
	  word_end = true;
	  flag = true;
	}
      }
    }

    cap.oap.motion_type = MCHAR;
    G.curwin.w_set_curswant = true;
    int rc;
    if (word_end) {
      rc = end_word(cap.count1, type, flag, false);
    } else {
      rc = fwd_word(cap.count1, type, cap.oap.op_type != OP_NOP);
    }

    // Don't leave the cursor on the NUL past a line
    if (G.curwin.w_cursor.getColumn() != 0
		&& gchar_cursor() == '\n') {
      dec_cursor();
      cap.oap.inclusive = true;
    }

    if (rc == FAIL && cap.oap.op_type == OP_NOP) {
      clearopbeep(cap.oap);
    } else {
      adjust_for_sel(cap);
      if (G.fdo_flags().contains(FDO_HOR) && KeyTyped && cap.oap.op_type == OP_NOP)
        foldOpenCursor();
    }
  }

  /**
   * In exclusive Visual mode, may include the last character.
   */
  static private void adjust_for_sel(CMDARG cap) {
      if (G.VIsual_active
          && cap.oap.inclusive
          && G.p_sel.charAt(0) == 'e'
          && gchar_cursor() != '\n')
       {
          G.curwin.w_cursor.incColumn();
          cap.oap.inclusive = false;
       }
  }

  /**
   * Exclude last character at end of Visual area for 'selection' == "exclusive".
   * Should check VIsual_mode before calling this.
   */
  static private void unadjust_for_sel()
  {
      ViFPOS	pp;
      final ViFPOS cursor = G.curwin.w_cursor;

      if (G.p_sel.charAt(0) == 'e' && G.VIsual.compareTo(cursor) != 0)
      {
          if(G.VIsual.compareTo(cursor) < 0)
              pp = cursor;
          else
              pp = G.VIsual;

          if(pp.getColumn() > 0)
              pp.decColumn();
          else if(pp.getLine() > 1)
          {
            int line = pp.getLine() - 1;
            pp.set(line, Util.lineLength(line));
          }
      }
  }

  /**
   * ESC in Normal mode: beep, but don't flush buffers.
   * Don't even beep if we are canceling a command.
   */
  static private void nv_esc(CMDARG cap, int opnum) {
    if (G.VIsual_active) {
      end_visual_mode();	// stop Visual
      check_cursor_col();	// make sure cursor is not beyond EOL
      G.curwin.w_set_curswant = true;
      update_curbuf(NOT_VALID);
    } else if (G.curwin.hasSelection()) {
      G.curwin.clearSelection();
    } else if (cap.oap.op_type == OP_NOP && opnum == 0
	     && cap.count0 == 0 && cap.oap.regname == 0 && p_im == 0) {
      Util.vim_beep();
    }
    clearop(cap.oap);
    if (p_im != 0 && G.restart_edit == 0) {
      G.restart_edit = 'a';
    }
  }
  
  /** force exit from visual mode.
   * Used by ViManager when switching out and EditorPane
   * This is taken from nv_esc above, except that end_visual_mode
   * has a comment that says it checks EOL so took that out.
   */
  static void abortVisualMode() {
    if (G.VIsual_active) {
      end_visual_mode();	// stop Visual
      G.curwin.w_set_curswant = true;
      update_curbuf(NOT_VALID);
      showmode();
    }
  }

  /**
   * Handle "A", "a", "I", "i" and <Insert> commands.
   */
  static private void nv_edit (CMDARG cap)
  {
    /* in Visual mode "A" and "I" are an operator */
    if ((cap.cmdchar == 'A' || cap.cmdchar == 'I')
	&& G.VIsual_active)
    {
      v_visop(cap);
    }
    /* in Visual mode and after an operator "a" and "i" are for text objects */
    else if ((cap.cmdchar == 'a' || cap.cmdchar == 'i')
	     && (cap.oap.op_type != OP_NOP || G.VIsual_active))
    {
        nv_object(cap);
      //clearopbeep(cap.oap);
    } else if (!checkclearopq(cap.oap) && u_save_cursor() == OK) {
      beginInsertUndo();
      try {
        nv_edit_dispatch(cap);
      } finally {
        if(!editBusy)
          endInsertUndo();
      }
      return;
    }
    return;
  }

  /**
   * Enter regualar edit mode, moved here so could be invoked
   * as an atomic operation.
   */
  // can't be private, if want to dispatch
  static private void nv_edit_dispatch (CMDARG cap)
  {
    switch (cap.cmdchar) {
      case 'A':	/* "A"ppend after the line */
        G.curwin.w_set_curswant = true;
        Util.endLine();
        // G.op.xop(ViOp.THIS_END_LINE, this);
        break;

      case 'I':	/* "I"nsert before the first non-blank */
        Edit.beginline(BL_WHITE);
        break;

      case 'a':	/* "a"ppend is like "i"nsert on the next character. */
        int offset = G.curwin.w_cursor.getOffset();
        int c = Util.getCharAt(offset);
        if(c != '\n') {
          G.curwin.setCaretPosition(offset+1);
        }
        break;
    }
    Edit.edit(cap.cmdchar, false, cap.count1);
    return;
  }

  static private  void	nv_object (CMDARG cap) {
      int flag;
      boolean include;
      //char_u *mps_save;
      String mps_save;

      if (cap.cmdchar == 'i')
          include = false;    /* "ix" = inner object: exclude white space */
      else
          include = true;    /* "ax" = an object: include white space */

      /* Make sure (), [], {} and <> are in 'matchpairs' */
      mps_save = G.curbuf.b_p_mps;
      G.curbuf.b_p_mps = "(:),{:},[:],<:>";

      switch (cap.nchar)
      {
        case 'w': /* "aw" = a word */
            flag = current_word(cap.oap, cap.count1, include, false);
            break;
        case 'W': /* "aW" = a WORD */
            flag = current_word(cap.oap, cap.count1, include, true);
            break;
        case 'b': /* "ab" = a braces block */
        case '(':
        case ')':
            flag = current_block(cap.oap, cap.count1, include, '(', ')');
            break;
        case 'B': /* "aB" = a Brackets block */
        case '{':
        case '}':
            flag = current_block(cap.oap, cap.count1, include, '{', '}');
            break;
        case '[': /* "a[" = a [] block */
        case ']':
            flag = current_block(cap.oap, cap.count1, include, '[', ']');
            break;
        case '<': /* "a<" = a <> block */
        case '>':
            flag = current_block(cap.oap, cap.count1, include, '<', '>');
            break;
	case 't': /* "at" = a tag block (xml and html) */
		flag = current_tagblock(cap.oap, cap.count1, include);
		break;
        case 'p': /* "ap" = a paragraph */
            flag = current_par(cap.oap, cap.count1, include, 'p');
            break;
         case 's': /* "as" = a sentence */
             flag = current_sent(cap.oap, cap.count1, include);
             break;
	case '"': /* "a"" = a double quoted string */
	case '\'': /* "a'" = a single quoted string */
	case '`': /* "a`" = a backtick quoted string */
		flag = current_quote(cap.oap, cap.count1, include,
								  cap.nchar);
		break;
//             //#if 0­  /* TODO */
//             //­       case 'S': /* "aS" = a section */
//             //­       case 'f': /* "af" = a filename */
//             //­       case 'u': /* "au" = a URL */
//             //#endif
          default:
              flag = FAIL;
              break;
      }

      G.curbuf.b_p_mps = mps_save;
      if (flag == FAIL)
          clearopbeep(cap.oap);
      //adjust_cursor_col();
      G.curwin.w_set_curswant = true;

  }

  /**
   * Handle the "q" key.
   */
  static private void nv_q(CMDARG cap) throws NotSupportedException {
    if (cap.oap.op_type == OP_FORMAT) {
      /* "gqq" is the same as "gqgq": format line */
      cap.cmdchar = 'g';
      cap.nchar = 'q';
      nv_operator(cap);
    }
    else if (!checkclearop(cap.oap))
    {
      /* (stop) recording into a named register */
      /* command is ignored while executing a register */
      if (!G.Exec_reg && do_record(cap.nchar) == FAIL)
	clearopbeep(cap.oap);
    }
  }

  /**
   * Handle the "@r" command.
   */
  static private void nv_at(CMDARG cap) {
    if (checkclearop(cap.oap))
      return;
    while (cap.count1-- > 0)
    {
      if (do_execreg(cap.nchar, false, false) == FAIL)
      {
	clearopbeep(cap.oap);
	break;
      }
    }
  }

  /**
   * Handle the CTRL-U and CTRL-D commands.
   */
  static private void nv_halfpage(CMDARG cap) {
    final ViFPOS cursor = G.curwin.w_cursor;
    if ((cap.cmdchar == Util.ctrl('U') && cursor.getLine() == 1)
	|| (cap.cmdchar == Util.ctrl('D')
	    && cursor.getLine() == G.curbuf.getLineCount()))
      clearopbeep(cap.oap);
    else if (!checkclearop(cap.oap)) {
      halfpage(cap.cmdchar == Util.ctrl('D'), cap.count0);
    }
  }

  /**
   * Handle "J" or "gJ" command.
   */
  static private void nv_join(final CMDARG cap) {
    if (G.VIsual_active)	/* join the visual lines */
      nv_operator(cap);
    else if (!checkclearop(cap.oap)) {
      if (cap.count0 <= 1)
	cap.count0 = 2;	    /* default for join is two lines! */
      if (G.curwin.w_cursor.getLine() + cap.count0 - 1 >
	  		G.curbuf.getLineCount())
	clearopbeep(cap.oap);  /* beyond last line */
      else
      {
	prep_redo(cap.oap.regname, cap.count0,
		  NUL, cap.cmdchar, NUL, cap.nchar);
        runUndoable(new Runnable() {
          @Override
            public void run() {
              do_do_join(cap.count0, cap.nchar == NUL, true);
            }
        });
      }
    }
  }

  /**
   * "P", "gP", "p" and "gp" commands.
   */
  static private void nv_put(final CMDARG cap) {
    
    if (cap.oap.op_type != OP_NOP) {
      //#ifdef FEAT_DIFF ... #endif
      clearopbeep(cap.oap);
    } else {
      runUndoable(new Runnable() {
        @Override
          public void run() {
            char regname = 0;
            Yankreg reg1 = null, reg2 = null;
            boolean empty = false;
            boolean was_visual = false;
            int dir;
            int flags = 0;
            final ViFPOS cursor = G.curwin.w_cursor;

            dir = (cap.cmdchar == 'P'
                    || (cap.cmdchar == 'g' && cap.nchar == 'P'))
                    ? BACKWARD : FORWARD;
            prep_redo_cmd(cap);
            if (cap.cmdchar == 'g')
              flags |= PUT_CURSEND;

            try { // this is so that yank regsiter is lost
              if (G.VIsual_active) {
                // Putting in Visual mode: The put text replaces the selected
                // text.  First delete the selected text, then put the new text.
                // Need to save and restore the registers that the delete
                // overwrites if the old contents is being put.
                was_visual = true;
                regname = cap.oap.regname;
                regname = adjust_clip_reg(regname);
                if (regname == 0 || Util.isdigit(regname)
                    || (    G.p_cb && (regname == '*' || regname == '+'))) {
                  // the delete is going to overwrite the register we want to
                  // put, save it first.
                  reg1 = get_register(regname, true);
                }

                /* Now delete the selected text. */
                cap.cmdchar = 'd';
                cap.nchar = NUL;
                cap.oap.regname = NUL;
                nv_operator(cap);
                try {
                  do_pending_operator(cap, 0, false);
                } catch(Exception e) {
                  LOG.log(Level.SEVERE, "do_pending_operator", e);
                }
                //empty = (G.curbuf.b_ml.ml_flags.empty);

                /* delete PUT_LINE_BACKWARD; */
                cap.oap.regname = regname;

                if (reg1 != null) {
                  // Delete probably changed the register we want to put, save
                  // it first. Then put back what was there before the delete.
                  reg2 = get_register(regname, false);
                  put_register(regname, reg1);
                }

                // When deleted a linewise Visual area, put the register as
                // lines to avoid it joined with the next line.  When deletion
                // was characterwise, split a line when putting lines. */
                if (G.VIsual_mode == 'V')
                  flags |= PUT_LINE;
                else if (G.VIsual_mode == 'v')
                  flags |= PUT_LINE_SPLIT;
                if (G.VIsual_mode == Util.ctrl('V') && dir == FORWARD)
                  flags |= PUT_LINE_FORWARD;
                dir = BACKWARD;
                if (   (G.VIsual_mode != 'V'
                        && cursor.getColumn() < G.curbuf.b_op_start.getColumn())
                    || (G.VIsual_mode == 'V'
                        && cursor.getLine() < G.curbuf.b_op_start.getLine()))
                  // cursor is at the end of the line or end of file, put
                  // forward.
                  dir = FORWARD;
              }
              do_put(cap.oap.regname, dir, cap.count1, flags);

            } finally {
              // If a register was saved, put it back now.
              if (reg2 != null)
                put_register(regname, reg2);
            }

            // What to reselect with "gv"?  Selecting the just put text seems to
            // be the most useful, since the original text was removed.
            if (was_visual) {
              G.curbuf.b_visual_start.setMark(G.curbuf.b_op_start);
              G.curbuf.b_visual_end.setMark(G.curbuf.b_op_end);
            }

            // When all lines were selected and deleted do_put() leaves an empty
            // line that needs to be deleted now.
    //	if (empty && Util.ml_get(G.curbuf.b_ml.ml_line_count).charAt(0) == NUL)
    //	{
    //	    ml_delete(G.curbuf.b_ml.ml_line_count, true);
    //
    //	    /* If the cursor was in that line, move it to the end of the last
    //	     * line. */
    //	    if (G.curwin.getWCursor().getLine() > G.curbuf.b_ml.ml_line_count)
    //	    {
    //		G.curwin.getWCursor().setLine(G.curbuf.b_ml.ml_line_count);
    //		coladvance(MAXCOL);
    //	    }
    //	}
            //auto_format(false, true);
          }
      });
    }
  }

  static void foldOpenCursor() {
        G.curwin.foldOpenCursor(G.curwin.w_cursor.getLine());
  }

  static int u_save_cursor() { return OK; }

  static void start_selection() {}
  static void do_help(String s) {}
  static boolean buflist_getfile(int n, int lnum, int options, boolean forceit) {
    return true;
  }
  static void update_curbuf(int type) {}
  static void redraw_curbuf_later(int type) {}
  static void do_tag(String tag, int type, int count,
	      boolean forceit, boolean verbose) {}
  static boolean typebuf_typed() { return true; }
  static boolean msg_attr(String s, int attr) { return true; }
  static void setcursor() {}
  static void cursor_on() {}
  static void ui_delay(int msec, boolean ignoreinput) {}
  static void update_other_win() {}

  //
  //
  //
  //
  //
  //

  //static boolean Recording;
  //static boolean Exec_reg;
  static int p_im;
  static boolean msg_didout;
  static int msg_col;
  static boolean arrow_used;
  static boolean redraw_cmdline;
  static boolean msg_didany;
  static boolean msg_nowait;
  static boolean KeyTyped = true;
  static boolean msg_scroll;
  static boolean emsg_on_display;
  static boolean must_redraw;
  static String keep_msg;
  static int keep_msg_attr;
  static boolean modified;

  static boolean bangredo;



  /**
   * The Visual area is remembered for reselection.
   */
  static char	resel_VIsual_mode = NUL;	/* 'v', 'V', or Ctrl-V */
  static int	resel_VIsual_line_count;/* number of lines */
  static int	resel_VIsual_col;	/* nr of cols or end col */


  static private  void	nv_normal (CMDARG cap) {}
  static void do_exmode() {}
  static void update_screen(boolean flag) {}
  static void update_screen(int flag) {}



  static class Opchar {
    char c1, c2;
    boolean lines;

    Opchar(char c1, char c2, boolean lines) {
      this.c1 = c1;
      this.c2 = c2;
      this.lines = lines;
    }
  }

  /*
   * The names of operators.  Index must correspond with defines in vim.h!!!
   * The third field indicates whether the operator always works on lines.
   */
  static Opchar[] opchars = new Opchar[]
  {
      new Opchar('\000', '\000', false),/* OP_NOP */
      new Opchar('d', '\000', false),	/* OP_DELETE */
      new Opchar('y', '\000', false),	/* OP_YANK */
      new Opchar('c', '\000', false),	/* OP_CHANGE */
      new Opchar('<', '\000', true),	/* OP_LSHIFT */
      new Opchar('>', '\000', true),	/* OP_RSHIFT */
      new Opchar('!', '\000', true),	/* OP_FILTER */
      new Opchar('g', '~', false),	/* OP_TILDE */
      new Opchar('=', '\000', true),	/* OP_INDENT */
      new Opchar('g', 'q', true),	/* OP_FORMAT */
      new Opchar(':', '\000', true),	/* OP_COLON */
      new Opchar('g', 'U', false),	/* OP_UPPER */
      new Opchar('g', 'u', false),	/* OP_LOWER */
      new Opchar('J', '\000', true),	/* DO_JOIN */
      new Opchar('g', 'J', true),	/* DO_JOIN_NS */
      new Opchar('g', '?', false),	/* OP_ROT13 */
      new Opchar('r', '\000', false),	/* OP_REPLACE */
      new Opchar('I', '\000', false),	/* OP_INSERT */
      new Opchar('A', '\000', false)	/* OP_APPEND */
  };

  /**
   * Translate a command name into an operator type.
   * Must only be called with a valid operator name!
   */
  static int get_op_type(int char1, int char2) {
      int		i;

      if (char1 == 'r')		/* ignore second character */
	  return OP_REPLACE;
      if (char1 == '~')		/* when tilde is an operator */
	  return OP_TILDE;
      if (char1 == 'Q')		/* also in map as 'g', 'q' */
          return OP_FORMAT;
      for (i = 0; ; ++i)
	  if (opchars[i].c1 == char1 && opchars[i].c2 == char2)
	      break;
      return i;
  }

  /**
   * Return TRUE if operator "op" always works on whole lines.
   */
  static boolean op_on_lines(int op) {
      return opchars[op].lines;
  }

  /**
   * Get first operator command character.
   * Returns 'g' if there is another command character.
   */
  static char get_op_char(int optype) {
      return opchars[optype].c1;
  }

  /**
   * Get second operator command character.
   */
  static char get_extra_op_char(int optype) {
      return opchars[optype].c2;
  }


  /**
   * An operation that is not yet implemented has been selected.
   * Output the op and the characters that got us here.
   */
  static void notImp(String op) throws NotSupportedException {
    // setGeneralStatus
    if(KeyBinding.notImpDebug) ViManager.println("Not Implemented: "
                      + op + ": " + "\"" + getCmdChars() + "\"");
    throw new NotSupportedException(op, getCmdChars());
  }

  static void notImpV(char op) throws NotSupportedException {
    final boolean debug = false;
    if(!debug) {
      if(G.VIsual_active) {
        String category = null;
        if(G.VIsual_mode == Util.ctrl('V')
            && vim_strchr("<>", 0, op) >= 0) {
          category = "block";
        }
        if(category != null) {
          String s = String.valueOf(op);
          Msg.smsg("Visual " + category + " Mode: '" + s + "' not implemented");
          throw new NotSupportedException(s, getCmdChars());
        }
      }
    }
  }

  /**
   * An operation that is not planned support has been selected.
   * Output the op and the characters that got us here.
   */
  static void notSup(String op) throws NotSupportedException {
    // setGeneralStatus
    if(KeyBinding.notImpDebug) ViManager.println("Not supported: "
                       + op + ": " + "\"" + getCmdChars() + "\"");
    throw new NotSupportedException(op, getCmdChars());
  }

  /**
   * @return the command characters for the current command.
   */
  static String getCmdChars() {
    return showcmd_buf.toString();
  }

  private Normal()
  {
  }

}
