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
  public String e_abort	="Command aborted";
  public String e_argreq	="Argument required";
  public String e_backslash	="\\ should be followed by /, ? or &";
  public String e_curdir	="Command not allowed from exrc/vimrc in current dir or tag search";
  public String e_exists	="File exists (use ! to override)";
  public String e_failed	="Command failed";
  public String e_internal	="Internal error";
  public String e_interr	="Interrupted";
  public String e_invaddr	="Invalid address";
  public String e_invarg	="Invalid argument";
  public String e_invarg2	="Invalid argument: ";
  public String e_invrange	="Invalid range";
  public String e_invcmd	="Invalid command";
  public String e_markinval	="Mark has invalid line number";
  public String e_marknotset	="Mark not set";
  public String e_nesting	="Scripts nested too deep";
  public String e_noalt	="No alternate file";
  public String e_noabbr	="No such abbreviation";
  public String e_nobang	="No ! allowed";
  public String e_noinstext	="No inserted text yet";
  public String e_nolastcmd	="No previous command line";
  public String e_nomap	="No such mapping";
  public String e_nomatch	="No match";
  public String e_nomatch2	="No match: ";
  public String e_noname	="No file name";
  public String e_nopresub	="No previous substitute regular expression";
  public String e_noprev	="No previous command";
  public String e_noprevre	="No previous regular expression";
  public String e_norange	="No range allowed";
  public String e_noroom	="Not enough room";
  public String e_notcreate	="Can't create file ";
  public String e_notmp	="Can't get temp file name";
  public String e_notopen	="Can't open file ";
  public String e_notread	="Can't read file ";
  public String e_nowrtmsg	="No write since last change (use ! to override)";
  public String e_null		="Null argument";
  public String e_outofmem	="Out of memory!";
  public String e_patnotf2	="Pattern not found: ";
  public String e_positive	="Argument must be positive";
  public String e_re_damg	="Damaged match string";
  public String e_re_corr	="Corrupted regexp program";
  public String e_readonly	="'readonly' option is set (use ! to override)";
  public String e_scroll	="Invalid scroll size";
  public String e_tagformat	="Format error in tags file \"%s\"";
  public String e_tagstack	="tag stack empty";
  public String e_toocompl	="Command too complex";
  public String e_toombra	="Too many \\(";
  public String e_toomket	="Too many \\)";
  public String e_toomsbra	="Too many [";
  public String e_toomany	="Too many file names";
  public String e_trailing	="Trailing characters";
  public String e_umark	="Unknown mark";
  public String e_unknown	="Unknown";
  public String e_write	="Error while writing";
  public String e_zerocount	="Zero count";
}
