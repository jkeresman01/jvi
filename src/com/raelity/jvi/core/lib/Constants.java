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

public interface Constants {

  public static final int FALSE = 0;
  public static final int TRUE = 1;
  public static final int MAYBE = 2;

  // for <i>dir</i>
  public static final int FORWARD = 1;
  // for <i>dir</i>
  public static final int BACKWARD = -1;

  /** May be returned by add_new_completion(): */
    public static final int RET_ERROR	= -1;

    public static final int MAXLNUM = 0x7fffffff;
    public static final int MAXCOL = 0x7fffffff;

    public static final int MAXTYPEBUFLEN = 500000; // make huge, 500k, check out orig...
    public static final int ADJUSTTYPEBUFLEN = 10000; // 10k

  //
  // Operator IDs; The order must correspond to opchars[] in ops.c!
  //
    public static final int  OP_NOP	= 0;	// no pending operation
    public static final int  OP_DELETE	= 1;	// "d"  delete operator
    public static final int  OP_YANK	= 2;	// "y"  yank operator
    public static final int  OP_CHANGE	= 3;	// "c"  change operator
    public static final int  OP_LSHIFT	= 4;	// "<"  left shift operator
    public static final int  OP_RSHIFT	= 5;	// ">"  right shift operator
    public static final int  OP_FILTER	= 6;	// "!"  filter operator
    public static final int  OP_TILDE	= 7;	// "g~" switch case operator
    public static final int  OP_INDENT	= 8;	// "="  indent operator
    public static final int  OP_FORMAT	= 9;	// "gq" format operator
    public static final int  OP_COLON	= 10;	// ":"  colon operator
    public static final int  OP_UPPER	= 11;	// "gU" make upper case operator
    public static final int  OP_LOWER	= 12;	// "gu" make lower case operator
    public static final int  OP_JOIN	= 13;	// "J"  join operator, only Visual mode
    public static final int  OP_JOIN_NS	= 14;	// "gJ" join operator, only Visual mode
    public static final int  OP_ROT13	= 15;	// "g?" rot-13 encoding
    public static final int  OP_REPLACE	= 16;	// "r"  replace chars, only Visual mode
    public static final int  OP_INSERT	= 17;	// "I"  Insert column, only Visual mode
    public static final int  OP_APPEND	= 18;	// "A"  Append column, only Visual mode
    public static final int  OP_FOLD         = 19;	// "zf" define a fold
    public static final int  OP_FOLDOPEN     = 20;	// "zo" open folds
    public static final int  OP_FOLDOPENREC  = 21;	// "zO" open folds recursively
    public static final int  OP_FOLDCLOSE    = 22;	// "zc" close folds
    public static final int  OP_FOLDCLOSEREC = 23;	// "zC" close folds recursively
    public static final int  OP_FOLDDEL      = 24;	// "zd" delete folds
    public static final int  OP_FOLDDELREC   = 25;	// "zD" delete folds recursively

  //
  // Flags for do_put
  //
    public static final int PUT_FIXINDENT=  1;      /* make indent look nice */
    public static final int PUT_CURSEND=    2;      /* leave cursor after end of new text */
    public static final int PUT_CURSLINE=   4;      /* leave cursor on last line of new text */
    public static final int PUT_LINE=       8;      /* put register as lines */
    public static final int PUT_LINE_SPLIT= 16;     /* split line for linewise register */
    public static final int PUT_LINE_FORWARD=32;    /* put linewise register below Visual sel. */

  //
  // Motion types, used for operators and for yank/delete registers.
  //
    public static final int  MCHAR	= 0;	// character-wise movement/register
    public static final int  MLINE	= 1;	// line-wise movement/register
    public static final int  MBLOCK	= 2;	// block-wise register

    public static final int SHOWCMD_COLS = 10;	// columns needed by shown command

  //
  // values for State
  //
  // The lower byte is used to distinguish normal/visual/op_pending and cmdline/
  // insert+replace mode.  This is used for mapping.  If none of these bits are
  // set, no mapping is done.
  // The upper byte is used to distinguish between other states.
  //

    public static final int BASE_STATE_MASK = 0xff;

    public static final int  NORMAL	= 0x01;	// Normal mode, command expected
    public static final int  VISUAL	= 0x02;	// Visual mode - get_real_state()
    public static final int  OP_PENDING	= 0x04;	// Normal mode, operator is
                                                // pending - use get_real_state()
    public static final int  CMDLINE	= 0x08;	// Editing command line
    public static final int  INSERT	= 0x10;	// Insert mode
    public static final int  PLATFORM   = 0x20; // platform-sel, get_real_state()

    public static final int  MAP_ALL_MODES = 0x3f; // all mode bits used for
						   // mapping

    public static final int  NORMAL_BUSY	= (0x100 + NORMAL); // Normal mode, busy command
    public static final int  REPLACE	= (0x200 + INSERT); // Replace mode
    public static final int  VREPLACE	= (0x300 + INSERT); // Virtual replace mode
    public static final int  HITRETURN	= (0x600 + NORMAL); // wait return or command
    public static final int  ASKMORE	= 0x700; // Asking if you want --more--
    public static final int  SETWSIZE	= 0x800; // window size has changed
    public static final int  ABBREV	= 0x900; // abbreviation instead of mapping
    public static final int  EXTERNCMD	= 0xa00; // executing an external command
    public static final int  SHOWMATCH	= (0xb00 + INSERT); // show matching paren
    public static final int  CONFIRM	= 0xc00; // ":confirm" prompt

  // Values for buflist_getfile()
    public static final int  GETF_SETMARK	= 0x01;	// set pcmark before jumping
    public static final int  GETF_ALT		= 0x02;	// jump to alternate file
  						// (not buf num)
    public static final int  GETF_SWITCH	= 0x04;	// respect 'switchbuf' set when jumping

  //
  // flags for update_screen()
  // The higher the value, the higher the priority
  //
    public static final int VALID		= 10;  // buffer not changed
    public static final int INVERTED		= 20;  // redisplay inverted part
    public static final int VALID_TO_CURSCHAR	= 30;  // line at/below cursor changed
    public static final int VALID_BEF_CURSCHAR	= 35;  // line just above cursor changed
    public static final int NOT_VALID		= 40;  // buffer changed somewhere
    public static final int CLEAR		= 50;  // screen messed up, clear it

  //
  // Definitions of various common control characters
  //

    public static final char  NUL	= '\000';
    public static final char  BS         = '\010';
    public static final char  TAB	= '\011';
    public static final char  NL         = '\012';
    public static final String  NL_STR	= "\012";
    public static final char  FF         = '\014';
    public static final char  CR         = '\015';
    public static final char  ESC	= '\033';
    public static final String ESC_STR	= "\033";
    public static final char  DEL	= 0x7f;
    public static final char  CSI	= 0x9b;

  // Bits for modifier mask
  // WAS: 0x01 cannot be used, because the modifier must be 0x02 or higher
  // not sure why 0x01 can't be used, use it, set it to match swing
    public static final int MOD_MASK_SHIFT   = 0x01;
    public static final int MOD_MASK_CTRL    = 0x02;
    public static final int MOD_MASK_ALT	    = 0x08;
    public static final int MOD_MASK_META    = 0x04;

  // Values for change_indent()
    public static final int INDENT_SET	= 1;	// set indent
    public static final int INDENT_INC	= 2;	// increase indent
    public static final int INDENT_DEC	= 3;	// decrease indent

  // flags for beginline()
  public static final int BL_WHITE = 1;// cursor on first non-white in the line
  public static final int BL_SOL = 2;  // use 'sol' option
  public static final int BL_FIX = 4;  // don't leave cursor on a NUL

  // return values for functions
    public static final int OK		= 1;
    public static final int FAIL		= 0;

  //
  // Values for do_tag().
  //
    public static final int DT_TAG	= 1;	/* jmp to newer pos or same tag again */
    public static final int DT_POP	= 2;	/* jump to older position */
    public static final int DT_NEXT	= 3;	/* jump to next match of same tag */
    public static final int DT_PREV	= 4;	/* jump to previous match of same tag */
    public static final int DT_FIRST	= 5;	/* jump to first match of same tag */
    public static final int DT_LAST	= 6;	/* jump to first match of same tag */
    public static final int DT_SELECT	= 7;	/* jump to selection from list */
    public static final int DT_HELP	= 8;	/* like DT_TAG, but no wildcards */
    public static final int DT_JUMP	= 9;	/* jmp to new tag or select from list */

  //
  // Values for 'options' argument in do_search() and searchit()
  //
    public static final int SEARCH_REV   = 0x01;  // go in reverse of previous dir.
    public static final int SEARCH_ECHO  = 0x02;  // echo search command and handle opts
    public static final int SEARCH_MSG   = 0x0c;  // give messages (yes, it's not 0x04)
    public static final int SEARCH_NFMSG = 0x08;  // give all messages except not found
    public static final int SEARCH_OPT   = 0x10;  // interpret optional flags
    public static final int SEARCH_HIS   = 0x20;  // put search pattern in history
    public static final int SEARCH_END   = 0x40;  // put cursor at end of match
    public static final int SEARCH_NOOF  = 0x80;  // don't add offset to position
    public static final int SEARCH_START =0x100;  // start search without col offset
    public static final int SEARCH_MARK  =0x200;  // set previous context mark
    public static final int SEARCH_KEEP  =0x400;  // keep previous search pattern
    public static final int SEARCH_PEEK  =0x800;  // peek for typed char, cancel search
    public static final int SEARCH_ISCLEAN  =0x1000;  // jVi only, don't clean the pattern

    // Values for sub_cmd and which_pat argument for search_regcomp()
    // Also used for which_pat argument for searchit()
    public static final int RE_SEARCH = 0; // save/use pat in/from search_pattern
    public static final int RE_SUBST = 1;  // save/use pat in/from subst_pattern
    public static final int RE_BOTH = 2;   // save pat in both patterns
    public static final int RE_LAST = 2;   // use last used pattern if "pat" NULL

    public static final int SP_NOMOVE	= 0x01; /* don't move cursor */
    public static final int SP_REPEAT	= 0x02; /* repeat to find outer pair */
    public static final int SP_RETCOUNT	= 0x04; /* return matchcount */
    public static final int SP_SETPCMARK	= 0x08; /* set previous context mark */
    public static final int SP_START	= 0x10; /* accept match at start position */
    public static final int SP_SUBPAT	= 0x20; /* return nr of matching sub-pattern */
    public static final int SP_END	= 0x40; /* leave cursor at end of match */
    // NOTE that SEARCH_NOCLEAN doesn't overlap the SP_* flags
    public static final int SP_XXX     = 0x1000;

  // Values for flags argument for findmatchlimit()
    public static final int FM_BACKWARD	= 0x01;	// search backwards
    public static final int FM_FORWARD	= 0x02;	// search forwards
    public static final int FM_BLOCKSTOP	= 0x04;	// stop at start/end of block
    public static final int FM_SKIPCOMM	= 0x08;	// skip comments

  //
  // Values for find_ident_under_cursor()
  //
    public static final int FIND_IDENT	=1;	// find identifier (word)
    public static final int FIND_STRING	=2;	// find any string (WORD)

    //
    // There are four history tables:
    //
    public static final int HIST_CMD    = 0;	// colon commands
    public static final int HIST_SEARCH	= 1;	// search commands

  //
  // characters for the p_cpo option:
  //
    public static final char CPO_ALTREAD	= 'a';	/* ":read" sets alternate file name */
    public static final char CPO_ALTWRITE	= 'A';	/* ":write" sets alternate file name */
    public static final char CPO_BAR	= 'b';	/* "\|" ends a mapping */
    public static final char CPO_BSLASH	= 'B';	/* backslash in mapping is not special*/
    public static final char CPO_SEARCH	= 'c';
    public static final char CPO_DOTTAG	= 'd';	/* "./tags" in 'tags' in current dir */
    public static final char CPO_EXECBUF	= 'e';
    public static final char CPO_EMPTYREGION = 'E';	/* operating on empty region is error */
    public static final char CPO_FNAMER	= 'f';	/* set file name for ":r file" */
    public static final char CPO_FNAMEW	= 'F';	/* set file name for ":w file" */
    public static final char CPO_JOINSP	= 'j';	/* only use two spaces for join after '.' */
    public static final char CPO_ENDOFSENT = 'J';	/* need two spaces to detect end of sentence */
    public static final char CPO_KEYCODE	= 'k';	/* don't recognize raw key code in mappings */
    public static final char CPO_KOFFSET	= 'K';	/* don't wait for key code in mappings */
    public static final char CPO_LITERAL	= 'l';	/* take char after backslash in [] literal */
    public static final char CPO_LISTWM	= 'L';	/* 'list' changes wrapmargin */
    public static final char CPO_SHOWMATCH = 'm';
    public static final char CPO_MATCHBSL  = 'M'; /* "%" ignores use of backslashes */
    public static final char CPO_LINEOFF	= 'o';
    public static final char CPO_OVERNEW	= 'O';	/* silently overwrite new file */
    public static final char CPO_LISP	= 'p';	/* 'lisp' indenting */
    public static final char CPO_REDO	= 'r';
    public static final char CPO_BUFOPT	= 's';
    public static final char CPO_BUFOPTGLOB = 'S';
    public static final char CPO_TAGPAT	= 't';
    public static final char CPO_UNDO	= 'u';	/* "u" undoes itself */
    public static final char CPO_CW	= 'w';	/* "cw" only changes one blank */
    public static final char CPO_FWRITE	= 'W';	/* "w!" doesn't overwrite readonly files */
    public static final char CPO_ESC	= 'x';
    public static final char CPO_YANK	= 'y';
    public static final char CPO_DOLLAR	= '$';
    public static final char CPO_FILTER	= '!';
    public static final char CPO_MATCH	= '%';
    public static final char CPO_STAR	= '*';	/* ":*" means ":@" */
    public static final char CPO_SPECI	= '<';	/* don't recognize <> in mappings */
    public static final String CPO_DEFAULT = "aABceFs";
    public static final String CPO_ALL	= "aAbBcdeEfFjJkKlLmoOprsStuwWxy$!%*<";
  
    public static final char BS_INDENT   = 'i';
    public static final char BS_EOL      = 'o';
    public static final char BS_START    = 's';

  // struct to store values from 'guicursor'
    public static final int SHAPE_N	= 0; 	// Normal mode
    public static final int SHAPE_V	= 1; 	// Visual mode
    public static final int SHAPE_I	= 2; 	// Insert mode
    public static final int SHAPE_R	= 3; 	// Replace mode
    public static final int SHAPE_C	= 4; 	// Command line Normal mode
    public static final int SHAPE_CI	= 5; 	// Command line Insert mode
    public static final int SHAPE_CR	= 6; 	// Command line Replace mode
    public static final int SHAPE_SM	= 7; 	// showing matching paren
    public static final int SHAPE_O	= 8; 	// Operator-pending mode
    public static final int SHAPE_VE	= 9; 	// Visual mode, 'seleciton' exclusive
    public static final int SHAPE_COUNT	= 10;

    public static final int SHAPE_BLOCK	= 0; 	// block cursor
    public static final int SHAPE_HOR	= 1; 	// horizontal bar cursor
    public static final int SHAPE_VER	= 2; 	// vertical bar cursor
  
  // edit mode operations that can be specified through a keymap
    public static final char IM_SHIFT_RIGHT = KeyDefs.K_X_IM_SHIFT_RIGHT;
    public static final char IM_SHIFT_LEFT = KeyDefs.K_X_IM_SHIFT_LEFT;
    public static final char IM_SHIFT_RIGHT_TO_PAREN = KeyDefs.K_X_PERIOD;
    public static final char IM_SHIFT_LEFT_TO_PAREN = KeyDefs.K_X_COMMA;
    public static final char IM_INS_REP = KeyDefs.K_X_IM_INS_REP;
    public static final char IM_LITERAL = KeyDefs.K_X_IM_LITERAL;

    public enum FDO {
        FDO_ALL("all"),
        FDO_BLOCK("block"),
        FDO_HOR("hor"),
        FDO_MARK("mark"),
        FDO_PERCENT("percent"),
        FDO_QUICKFIX("quickfix"),
        FDO_SEARCH("search"),
        FDO_TAG("tag"),
        FDO_INSERT("insert"),
        FDO_UNDO("undo"),
        FDO_JUMP("jump");

        private final String opt;
        private FDO(String opt) {
            this.opt = opt;
        }

        public String getOpt() { return opt; }
    }
}

/*
:1,$s/\<no_mapping\>/VIG.&/g
:1,$s/\<allow_keys\>/VIG.&/g
:1,$s/\<finish_op\>/VIG.&/g
:1,$s/\<State\>/VIG.&/g
:1,$s/\<curwin\>/VIG.&/g
:1,$s/\<curwin.w_cursor\>/curwin.getWCursor()/
:1,$s/\<Ctrl(/0x1f \& (int)(/g
	The Ctrl thing is pretty ugly
 */
