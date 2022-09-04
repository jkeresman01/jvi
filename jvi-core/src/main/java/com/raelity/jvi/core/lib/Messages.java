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

public interface Messages {
  public String e_abort             ="Command aborted";
  public String e_argreq	    ="Argument required";
  public String e_backslash	    ="\\ should be followed by /, ? or &";
  public String e_curdir	    ="Command not allowed from exrc/vimrc in current dir or tag search";
  final public String e_exists      ="File exists (use ! to override)";
  final public String e_failed      ="Command failed";
  final public String e_internal    ="Internal error";
  final public String e_interr      ="Interrupted";
  final public String e_invaddr     ="Invalid address";
  final public String e_invarg      ="Invalid argument";
  final public String e_invarg2     ="Invalid argument: ";
  final public String e_invrange    ="Invalid range";
  final public String e_invcmd      ="Invalid command";
  final public String e_markinval   ="Mark has invalid line number";
  final public String e_marknotset  ="Mark not set";
  final public String e_nesting     ="Scripts nested too deep";
  final public String e_noalt       ="No alternate file";
  final public String e_noabbr      ="No such abbreviation";
  final public String e_nobang      ="No ! allowed";
  final public String e_noinstext   ="No inserted text yet";
  final public String e_nolastcmd   ="No previous command line";
  final public String e_nomap       ="No such mapping";
  final public String e_nomatch     ="No match";
  final public String e_nomatch2    ="No match: ";
  final public String e_noname      ="No file name";
  final public String e_nopresub    ="No previous substitute regular expression";
  final public String e_noprev      ="No previous command";
  final public String e_noprevre    ="No previous regular expression";
  final public String e_norange     ="No range allowed";
  final public String e_noroom      ="Not enough room";
  final public String e_notcreate   ="Can't create file ";
  final public String e_notmp       ="Can't get temp file name";
  final public String e_notopen     ="Can't open file ";
  final public String e_notread     ="Can't read file ";
  final public String e_nowrtmsg    ="No write since last change (use ! to override)";
  final public String e_null	    ="Null argument";
  final public String e_outofmem    ="Out of memory!";
  final public String e_patnotf2    ="Pattern not found: ";
  final public String e_positive    ="Argument must be positive";
  final public String e_re_damg     ="Damaged match string";
  final public String e_re_corr     ="Corrupted regexp program";
  final public String e_readonly    ="'readonly' option is set (use ! to override)";
  final public String e_scroll      ="Invalid scroll size";
  final public String e_tagformat   ="Format error in tags file \"%s\"";
  final public String e_tagstack    ="tag stack empty";
  final public String e_toocompl    ="Command too complex";
  final public String e_toombra     ="Too many \\(";
  final public String e_toomket     ="Too many \\)";
  final public String e_toomsbra    ="Too many [";
  final public String e_toomany     ="Too many file names";
  final public String e_trailing    ="Trailing characters";
  final public String e_umark       ="Unknown mark";
  final public String e_unknown     ="Unknown";
  final public String e_write       ="Error while writing";
  final public String e_zerocount   ="Zero count";
  final public String e_lasttab     ="Can not close last tab page";
}
