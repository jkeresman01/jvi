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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;

import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.text.TextUtil.MySegment;
import java.text.CharacterIterator;

/**
 * This class has all/most of the methods commonly referenced throughout
 * vim. This class can be extended and then the methods can be invoked
 * without knowing which class they are in.
 *
 * This simplifies porting vim code.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class CoreMethodHooks {
  //
  // These bounce routines to make porting easier
  //

//////////////////////////////////////////////////////////////////////
//
//Misc
//
static int dec(ViFPOS fpos) { return Misc.dec(fpos); }
static int dec_cursor() { return Misc.dec_cursor(); }
static int decl(ViFPOS pos) { return Misc.decl(pos); }
static int del_char(boolean f) { return Misc.del_char(f); }
static int do_join(boolean insert_space, boolean redraw)
    { return Misc.do_join(insert_space, redraw); }
static void do_put(int regname_, int dir, int count, int flags)
    { Misc.do_put(regname_, dir, count, flags);}
static char gchar_pos(ViFPOS pos) { return Misc.gchar_pos(pos); }
static char gchar_cursor() { return Misc.gchar_cursor(); }
static void getvcol(ViTextView tv, ViFPOS fpos, MutableInt start,
                            MutableInt cursor, MutableInt end)
                    { Misc.getvcol(tv, fpos, start, cursor, end); }
static int inc(ViFPOS lp) { return Misc.inc(lp); }
static int inc_cursor() { return Misc.inc_cursor(); }
static int inc_cursorV7() { return Misc.inc_cursorV7(); }
static int inclV7(ViFPOS pos) { return Misc.inclV7(pos); }
static int incV7(ViFPOS pos) { return Misc.incV7(pos); }
static boolean inindent(int i) { return Misc.inindent(i); }
static void ins_char(char c) { Misc.ins_char(c); }
static int skipwhite(MySegment seg, int idx) { return Misc.skipwhite(seg, idx); }
static boolean vim_iswhite(char c) { return Misc.vim_iswhite(c); }
static boolean vim_iswordc(char c) { return Misc.vim_iswordc(c); }

//////////////////////////////////////////////////////////////////////
//
// Util
//
static boolean ascii_isalpha(char c) { return Util.ascii_isalpha(c); }
static void beep_flush() { Util.beep_flush(); }
static boolean bufempty() { return Util.bufempty(); }
static int CharOrd(char c) { return Util.CharOrd(c); }
static char ctrl(char x) { return Util.ctrl(x); }
static int hex2nr(char c) { return Util.hex2nr(c); }
static boolean isalpha(char c) { return Util.isalpha(c); }
static boolean isdigit(char c) {return Util.isdigit(c); }
static boolean isupper(char c) { return Util.isupper(c); }
static MySegment ml_get(int lnum) { return Util.ml_get(lnum); }
static MySegment ml_get_curline() { return Util.ml_get_curline(); }
static CharacterIterator ml_get_cursor() { return Util.ml_get_cursor();}
static CharacterIterator ml_get_pos(ViFPOS pos) { return Util.ml_get_pos(pos);}
static int strncmp(String s1, String s2, int n) { return Util.strncmp(s1, s2, n); }
static int strncmp(MySegment seg, int i, String s2, int n)
    { return Util.strncmp(seg, i, s2, n); }
static void vim_beep() { Util.vim_beep(); }
static boolean vim_isdigit(char c) {return Util.isdigit(c); }
public static boolean vim_isspace(char x) { return Util.vim_isspace(x); }
static boolean vim_isxdigit(char c) { return Util.isxdigit(c); }
static String vim_strchr(String s, char c) { return Util.vim_strchr(s, c); }

static void vim_str2nr(MySegment seg, int start,
                               MutableInt pHex, MutableInt pLength,
                               int dooct, int dohex,
                               MutableInt pN, MutableInt pUn)
    { Util.vim_str2nr(seg, start, pHex, pLength, dooct, dohex, pN, pUn); }

// cursor compare
static boolean equalpos(ViFPOS p1, ViFPOS p2) { return Util.equalpos(p1, p2); }
static boolean lt(ViFPOS p1, ViFPOS p2) { return Util.lt(p1, p2); }

//////////////////////////////////////////////////////////////////////
//
// GetChar
//
static void AppendCharToRedobuff(char c) { GetChar.AppendCharToRedobuff(c); }
static int ins_typebuf(String str, int noremap, int offset, boolean nottyped)
    {return GetChar.ins_typebuf(str, noremap, offset, nottyped); }
static void stuffReadbuff(String s) { GetChar.stuffReadbuff(s); }
static void stuffcharReadbuff(char c) { GetChar.stuffcharReadbuff(c); }
static void vungetc(char c) { GetChar.vungetc(c); }

//////////////////////////////////////////////////////////////////////
//
// Normal
//
static boolean add_to_showcmd(char c) { return Normal.add_to_showcmd(c); }
static void clear_showcmd() { Normal.clear_showcmd(); }
static CharacterIterator find_ident_under_cursor(MutableInt mi, int find_type)
    {return Normal.find_ident_under_cursor(mi, find_type);}
static int u_save_cursor() { return Normal.u_save_cursor(); }

//////////////////////////////////////////////////////////////////////
//
// Options
//
static boolean can_bs(char what) { return Options.can_bs(what); }

//////////////////////////////////////////////////////////////////////
//
// MarkOps
//
static void setpcmark() {MarkOps.setpcmark();}
static void setpcmark(ViFPOS pos) {MarkOps.setpcmark(pos);}
//////////////////////////////////////////////////////////////////////
//
// Edit
//
static int stuff_inserted(char c, int count, boolean no_esc)
    throws NotSupportedException { return Edit.stuff_inserted(c, count, no_esc); }
static String get_last_insert_save() {return Edit.get_last_insert_save();}
static String last_search_pat() {return Search.getLastPattern();}


}
