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

import java.util.logging.Logger;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.raelity.jvi.lib.Wrap;
import java.util.Map;
import com.raelity.text.TextUtil;
import com.raelity.jvi.options.Option;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.logging.Level;

import static com.raelity.jvi.core.Constants.*;
import static com.raelity.jvi.core.KeyDefs.*;
import static com.raelity.jvi.core.Util.*;

public class GetChar {
  private static boolean block_redo = false;
  private static boolean handle_redo = false;
  private static Option magicRedoAlgo
          = Options.getOption(Options.magicRedoAlgorithm);
  private static String currentMagicRedoAlgo = "anal";

  private GetChar()
  {
  }

  /** An input char from the user has been recieved.
   * Implement the key into 16 bit mapping defined in KeyDefs.
   * <p>
   * There is still a hole. This doesn't handle stuff where screwy
   * modifiers are applied to normal keys, e.g. Ctrl-Shft-Alt-A.
   * There is room in the reseved
   * area to map the regular keys, probably want to put them in the
   * E000-E07f range, and put the K_UP type keys in the E080 range.
   * </p>
   */
  static void gotc(char key, int modifier) {
    Options.kd.printf(Level.FINER,
        "gotc: '%s' %d\n", TextUtil.debugString(String.valueOf(key)), modifier);
    if((key & 0xF000) == VIRT) {
      key = adjustShiftedSpecial(key, modifier);
    }
    G.setModMask(modifier);

    last_recorded_len = 0;      // for the one in vgetc()
    userInput(key);
    
    assert(!handle_redo);
    handle_redo = false;

    ///// NEED API, NO SWING...
    ///// // Lock document while handling a single user character
    ///// // EXPERIMENTAL
    ///// AbstractDocument doc = null;
    ///// if(true)
    /////     doc = null; // DISABLE
    ///// else {
    /////     //JEditorPane ep = G.curwin.getEditorComponent();
    /////     //if(ep.getDocument() instanceof AbstractDocument)
    /////     //    doc = (AbstractDocument)ep.getDocument();
    ///// }
    
    try {
        ///// if(doc != null)
        /////     doc.readLock();
        
        // Normal.processInputChar(key, true);
        user_ins_typebuf(key);

        if(!handle_redo)
            pumpVi();
        else {
            //
            // Handle some type of redo command: start_redo or start_redo_ins.
            // Execute these commands atomically, with the file locked if
            // supported by the platform. We need these special brackets since
            // while executing the command, other begin/endUndo's may occur.
            // 
            // During the above processInputChar, a redo command was setup by
            // stuffing the buffer. pumpVi() delivers stuffbuf and typebuf
            // to processInputChar. It's tempting to always bracket pumpVi with
            // begin/endRedoUndo, but macro execution might end in input mode.
            //
            // NEEDSWORK: INPUT MODE FROM MACRO ????????
            try {
              Misc.runUndoable(new Runnable() {
                @Override
                public void run() {
                  pumpVi();
                }
              });
            } finally {
                handle_redo = false;
            }
        }
    } finally {
        ///// if(doc != null)
        /////     doc.readUnlock();
    }


    // returning from event
    // only do this if no pending characters
    // but can't test for pending characters, so ....

    Misc.out_flush();
  }

  private static char adjustShiftedSpecial(char key, int modifier) {
      if((modifier & KeyDefs.MOD_MASK) == SHFT
                && key >= VIRT && key <= VIRT + 0x0f) {
        // only the shift key is pressed and its one of "those".
        key += SHIFTED_VIRT_OFFSET;
      }
      return key;
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Map Command handling
  //

  private static Map<Character, Mapping> mappings
          = new HashMap<Character, Mapping>();
  private static Map<Character, Mapping> defaultMappings
          = new HashMap<Character, Mapping>();
  private static final Map<String, Character> mapCommandSpecial
          = new HashMap<String, Character>();

  private static class Mapping {
    String lhs;
    String rhs;
    int mode;
    boolean noremap;

    public Mapping(String lhs, String rhs, int mode, boolean noremap)
    {
      this.lhs = lhs;
      this.rhs = rhs;
      this.mode = mode;
      this.noremap = noremap;
    }

    @Override
    public String toString()
    {
      return "Mapping{'" + TextUtil.debugString(lhs)
              + "' --> '" + TextUtil.debugString(rhs)
              + "' mode=" + mode + ", noremap=" + noremap + '}';
    }

  }

  /**
   * Convert the match to a char.
   * @return null if problem else translated char
   */
  private static Character mapCommandChar(Matcher m, boolean is_rhs, String orig)
  {
    if(false) {
      System.err.println("region: '"
              +  orig.substring(m.start())
              + "' match: '" + m.group() + "'");
      for(int i = 1; i <= m.groupCount(); i++) {
        System.err.println("\t" + m.group(i));
      }
    }

    char c = 0;
    String s;
    if((s = m.group(g_char)) != null)
      c = s.charAt(0);
    else if((s = m.group(g_ctrl)) != null)
      c = (char)(s.charAt(0) & ~0x40);
    else if((s = m.group(g_spec)) != null) {
      c = mapCommandSpecial.get(s);
      s = m.group(g_modif);

      // TODO: adjust c if needed **********************************
      // adjust if needed

      if(s != null) {
        if(s.equals("C")) {
          c |= (KeyDefs.CTRL << KeyDefs.MODIFIER_POSITION_SHIFT);
        } else if(s.equals("S")) {
          c |= (KeyDefs.SHFT << KeyDefs.MODIFIER_POSITION_SHIFT);
        }
      }
    }

    return c;
  }

  // the groups in mapSeqPattern
  private static final int g_char = 1;
  private static final int g_ctrl = 2;
  private static final int g_modif = 3;
  private static final int g_spec = 4;

  private static Pattern createMapSeqPattern()
  {
    StringBuilder sb = new StringBuilder();

    //
    // a char is matched like:
    //          [!-~&&[^\\<]]           all printables, except \ and < and space
    //          <C-[@-\[\]-_]>          all control chars, except Ctrl-\,Ctrl-<
    //          <special>               list of special chars
    //          <C-special>             ctrl of list of special chars
    //          <S-special>             shft of list of special chars
    // NOTE: special look like: key1|key2|key3
    // char: [!-~&&[^\\<]] | <C-[@-_]> | <([SC]-)?(special)>
    // a line is like: noremap char char+
    //
    String pat = "([!-~&&[^\\\\<]])"
                  + "|<C-([@-\\[\\]-_])>"
                  + "|<(?:([SC])-)?(special)>";
    // Make a string of all the special words we match.
    for(String k : mapCommandSpecial.keySet()) {
      sb.append(k).append("|");
    }
    sb.deleteCharAt(sb.length() - 1);
    pat = pat.replace("special", sb.toString());
    return Pattern.compile(pat);
  }

  /** should already have verified that cmd is supported */
  private static int get_map_mode(Wrap<String>cmdp, boolean forceit)
  {
    String cmd = cmdp.getValue();
    int p = 0;
    int mode = 0;
    char modec = cmd.charAt(p++);
    if (modec == 'i')
	mode = INSERT;				   /* :imap */
    else if (modec == 'c')
	mode = CMDLINE;				   /* :cmap */
    else if (modec == 'n' && cmd.charAt(p) != 'o') /* avoid :noremap */
	mode = NORMAL;				   /* :nmap */
    else if (modec == 'v')
	mode = VISUAL;				   /* :vmap */
    else if (modec == 'o')
	mode = OP_PENDING;			   /* :omap */
    else
    {
	--p;
	if (forceit)
	    mode = INSERT + CMDLINE;		/* :map ! */
	else
	    mode = VISUAL + NORMAL + OP_PENDING;/* :map */
    }

    cmdp.setValue(cmd.substring(p));
    return mode;
  }

  private static boolean supportedMapCommand(String cmd)
  {
    return   false
           | "map".equals(cmd)
           | "nmap".equals(cmd)
           | "vmap".equals(cmd)
           | "omap".equals(cmd)
           | "noremap".equals(cmd)
           | "nnoremap".equals(cmd)
           | "vnoremap".equals(cmd)
           | "onoremap".equals(cmd)
           ;
  }

  /**
   * Parse the mapping command.
   * If emsgs is not changed, then no error occurred.
   * @param line
   * @param emsgs
   * @param m
   * @param rhs
   * @return null if no mapping on line or error
   */
  static Map.Entry<Character, Mapping>
  parseMapCommand(String line, int lnum,
                  StringBuilder emsgs, Matcher m, StringBuilder rhs)
  {
    if(m == null) {
      m = createMapSeqPattern().matcher("");
    }
    if(rhs == null) {
      rhs = new StringBuilder();
    }
    int initialEmsgs = emsgs.length();

    List<String> fields = TextUtil.split(line);

    if(fields.isEmpty() || fields.get(0).startsWith("\""))
      return null;
    if(fields.size() != 3) {
      emsgs.append("Map Command line ").append(lnum + 1)
              .append(": Must be exactly three fields\n");
      return null;
    }

    // NEEDSWORK: forceit/'!' handling
    String cmd = fields.get(0);
    if(!supportedMapCommand(cmd)) {
      emsgs.append("Map Command line ").append(lnum + 1)
              .append(": \"").append(cmd)
              .append("\" command not supported\n");
      return null;
    }

    Wrap<String> cmdp = new Wrap<String>(cmd);
    int mode = get_map_mode(cmdp, false);
    cmd = cmdp.getValue();
    int maptype = (cmd.charAt(0) == 'n') ? 2 : cmd.charAt(0) == 'u' ? 1 : 0;
    assert maptype != 1;

    Character lhs = null;
    rhs.setLength(0);
    boolean ok;

    m.reset(fields.get(1));
    ok = false;
    if(m.matches()) {
      ok = (lhs = mapCommandChar(m, false, fields.get(1))) != null;
    }
    if(!ok) {
      emsgs.append("Map Command line ").append(lnum + 1)
              .append(": \"")
              .append(fields.get(1))
              .append("\" left-side not recognized\n");
    }

    m.reset(fields.get(2));
    int idx = 0;
    do {
      if(false) {
        System.err.println("checking: '" + fields.get(2).substring(idx) + "'");
      }
      ok = false;
      if(m.find() && idx == m.start()) {
        Character c = mapCommandChar(m, true, fields.get(2));
        if(c != null) {
          rhs.append(c.charValue());
          ok = true;
        }
      }
      if(!ok) {
        emsgs.append("Map Command line ").append(lnum + 1)
                .append(": \"")
                .append(fields.get(2).substring(idx))
                .append("\" right-side not recognized\n");
        break;
      }
      idx = m.end();
    } while(idx < m.regionEnd());

    if(emsgs.length() == initialEmsgs) {
      Mapping mapping = new Mapping(String.valueOf(lhs), rhs.toString(),
                                    mode, maptype == 2);
      if(Options.isKeyDebug()) {
        System.err.println("parseMapCommand: " + line
                + ": " + mapping);
      }

      return new AbstractMap.SimpleEntry<Character, Mapping>(lhs, mapping);
              //lhs, rhs.toString());
    } else {
      Options.kd.printf("parseMapCommand: %s: error\n", line);
      return null;
    }
  }

  static Map<Character, Mapping>
  createMapCommandsMap(String input, Wrap<String>emsg)
  {
    initMapCommandStuff();

    Map<Character, Mapping> mapCommands = new HashMap<Character, Mapping>();

    StringBuilder emsgs = new StringBuilder();

    Pattern mapSeqPattern = createMapSeqPattern();
    Matcher m = mapSeqPattern.matcher("");
    StringBuilder rhs = new StringBuilder();

    List<String> lines = TextUtil.split(input, "\n");
    for(int lnum = 0; lnum < lines.size(); lnum++) {
      String line = lines.get(lnum);
      Entry<Character, Mapping> kv
              = parseMapCommand(line, lnum, emsgs, m, rhs);
      if(kv != null)
        mapCommands.put(kv.getKey(), kv.getValue());
    }

    if(emsgs.length() > 0) {
      emsg.setValue(emsgs.toString());
      return null;
    }

    // TODO: put this ...
    // HACK: save map as current map, should be somewhere else;
    saveMappings(mapCommands);

    return mapCommands;
  }

  private static void saveMappings(Map<Character, Mapping> newMappings)
  {
    mappings.clear();
    mappings.putAll(defaultMappings);
    mappings.putAll(newMappings);
  }

  private static void initMapCommandStuff()
  {
    if(!mapCommandSpecial.isEmpty())
      return;
    createMapCommandSpecial();

    // TODO: setup default mappings

  }


  /** used for parsing */
  private static void createMapCommandSpecial()
  {
    mapCommandSpecial.put("Nul",      '\n');
    mapCommandSpecial.put("BS",       '\b');///////////////////////////////
    mapCommandSpecial.put("Tab",      '\t');///////////////////////////////
    mapCommandSpecial.put("NL",       '\n');
    mapCommandSpecial.put("FF",       '\f');
    mapCommandSpecial.put("CR",       '\n');
    mapCommandSpecial.put("Return",   '\n');
    mapCommandSpecial.put("Enter",    '\n');
    mapCommandSpecial.put("Esc",      '\u001b');///////////////////////////////
    mapCommandSpecial.put("Space",    ' ');
    mapCommandSpecial.put("lt",       '<');
    mapCommandSpecial.put("Bslash",   '\\');
    mapCommandSpecial.put("Bar",      '|');
    mapCommandSpecial.put("Del",      '\u007f');///////////////////////////////

    mapCommandSpecial.put("EOL",      '\n');

    mapCommandSpecial.put("Up",       K_UP);
    mapCommandSpecial.put("Down",     K_DOWN);
    mapCommandSpecial.put("Left",     K_LEFT);
    mapCommandSpecial.put("Right",    K_RIGHT);

    mapCommandSpecial.put("Help",     K_HELP);
    mapCommandSpecial.put("Undo",     K_UNDO);
    mapCommandSpecial.put("Insert",   K_INS);
    mapCommandSpecial.put("Home",     K_HOME);
    mapCommandSpecial.put("End",      K_END);
    mapCommandSpecial.put("PageUp",   K_PAGEUP);
    mapCommandSpecial.put("PageDown", K_PAGEDOWN);

/*
<kHome>
<kEnd>
<kPageUp>
<kPageDown>
<kPlus>
<kMinus>
<kMultiply>
<kDivide>
<kEnter>
<kPoint>
<k0> - <k9>

<S-Up>
<S-Down>
<S-Left>
<S-Right>
<C-Left>
<C-Right>
<F1> - <F12>
<S-F1> - <S-F12>
<CSI>
<xCSI>
*/
  }

  //////////////////////////////////////////////////////////////////////////
  
  /** This is a special case for the two part search */
  static void fakeGotc(char key) {
    G.setModMask(0);
    Normal.processInputChar(key, true);
    Misc.out_flush();   // returning from event
  }

  // NEEDSWORK: old_mod_mask
  private static char old_char;

  /**
   * Pass queued up characters to vi for processing.
   * First from stuffbuf, then typebuf.
   */
  private static void pumpVi() {
    // NEEDSWORK: pumpVi: check for interupt?
      
    while(true) {
      if(old_char != NUL) {
        char c = old_char;
        old_char = NUL;
        pumpChar(c);
      }
      while(stuffbuff.hasNext()) {
        pumpChar(stuffbuff.removeFirst());
      }
      if(typebuf.hasNext()) {
        pumpChar(typebuf.getChar());
      } else {
        break;
      }
    }
    G.Exec_reg = false;
  }

  /**
   * @return a queued character from input stream.
   * @exception RuntimeException if no characters are available
   */
  private static char getOneChar() {
    if(stuffbuff.hasNext()) {
      return stuffbuff.removeFirst();
    }
    if(typebuf.hasNext()) {
      return typebuf.getChar();
    }
    throw new RuntimeException("No character available");
  }

  private static void pumpChar(char c) {
    if(c == -1)
      return;
    int modifiers = 0;
    if((c & 0xF000) == VIRT) {
      modifiers = (c >> MODIFIER_POSITION_SHIFT) & 0x0f;
      c &= ~(0x0f << MODIFIER_POSITION_SHIFT);
    }
    G.setModMask(modifiers);
    Normal.processInputChar(c, true);
  }

  /**
   * read from the typebuf until a CR, return what was read, discarding
   * the CR. If the input stream empties, just return what was available.
   * @return true when a CR was encountered, else false
   */
  static boolean getRecordedLine(StringBuffer sb) {
    boolean hasCR = false;
    while(true) {
      if( ! char_avail()) {
        break;
      }
      char c = getOneChar();
      if(c == '\n') {
        hasCR = true;
        break;
      }
      sb.append(c);
    }
    return hasCR;
  }

  /**
   * Call vpeekc() without causing anything to be mapped.
   * @return true if a character is available, false otherwise.
   */
  static boolean char_avail() {
    // NOTE: Look at description for vgetorpeek; there's more to add

    if(stuffbuff.hasNext() || typebuf.hasNext()) {
      return true;
    }

    return false; // would really like to check...

    // return keyInput.hasChar();

  }

  static void vungetc(char c) /* unget one character (can only be done once!) */
  {
      old_char = c;
      //old_mod_mask = mod_mask;
  }

  /**
   * Write typed characters to script file.
   * If recording is on put the character in the recordbuffer.
   * Put the current modifies into the character if needed,
   * {@see KeyDefs} for info on character layout with modifiers.
   */
  private static void userInput(char c /*, int len*/) {

    /* remember how many chars were last recorded */
    if (G.Recording) {
      last_recorded_len += 1;
      if((c & 0xF000) == VIRT) {
        c |= (G.getModMask() << MODIFIER_POSITION_SHIFT);
      }
      recordbuff.append(c);
    }

    /* ******************************************************************
    buf[1] = NUL;
    while (len--)
    {
      c = *s++;
      updatescript(c);

      if (Recording)
      {
        buf[0] = c;
        add_buff(&recordbuff, buf);
      }
    }
    may_sync_undo();
    *******************************************************************/
  }

  /**
   * Write a stuff to the script and/or record buffer.
   * Used to stash command entry input.
   */
  public static void userInput(String s) { //NEEDSWORK: public
    /* remember how many chars were last recorded */
    if (G.Recording) {
      last_recorded_len += s.length();
      recordbuff.append(s);
    }
  }

  /*
   * return the contents of a buffer as a single string
   */
  static String get_bufcont(BufferQueue buffer, boolean dozero)
  {
    if(buffer.length() == 0 && !dozero)
      return null;
    return buffer.toString();
  }

  /**
   * Return the contents of the record buffer as a single string
   *  and clear the record buffer.
   */
  static String get_recorded() {
    //
    // Remove the characters that were added the last time, these must be the
    // (possibly mapped) characters that stopped recording.
    //
    int len = recordbuff.length();
    if (len >= last_recorded_len) {
      len -= last_recorded_len;
      recordbuff.setLength(len);
    }

    /* *******************************************************************
    //
    // When stopping recording from Insert mode with CTRL-O q, also remove the
    // CTRL-O.
    //
    if (len > 0 && restart_edit && p[len - 1] == Ctrl('O'))
      p[len - 1] = NUL;
    *************************************************************/

    String s = recordbuff.toString();
    recordbuff.setLength(0);

    return s;
  }

  static String get_inserted()
  {
    return get_bufcont(redobuff, false);
  }

  /**
   * get a character: 1. from a previously ungotten character
   *		      2. from the stuffbuffer
   *		      3. from the typeahead buffer
   *		      4. from the user
   * <br/>
   * if "advance" is TRUE (vgetc()):
   *	really get the character.
   *	KeyTyped is set to TRUE in the case the user typed the key.
   *	KeyStuffed is TRUE if the character comes from the stuff buffer.
   * <br/>
   * if "advance" is FALSE (vpeekc()):
   *	just look whether there is a character available.
   */

  static boolean vgetorpeek(boolean advance) {
    // Just a place holder for the above comment for now
    return false;
  }

  /**
   * prepare stuff buffer for reading (if it contains something).
   */
  private static void start_stuff() {
    // NEEDSWORK: start_stuff: should this this is a nop
    // stuffbuff = new BufferQueue();
  }

  static boolean stuff_empty() {
    Normal.do_xop("stuff_empty");
    return ! stuffbuff.hasNext();
  }

  private static boolean block_redo()
  {
    return handle_redo || block_redo;
  }

  /**
   * The previous contents of the redo buffer is kept in old_redobuffer.
   * This is used for the CTRL-O <.> command in insert mode.
   */
  static void ResetRedobuff() {
    if(!magicRedoAlgo.getString().equals(currentMagicRedoAlgo)) {
      currentMagicRedoAlgo = magicRedoAlgo.getString();
      G.dbgRedo.printf("Switching redo to %s\n", currentMagicRedoAlgo);
      if(currentMagicRedoAlgo.equals("anal"))
        magicRedo = new MagicRedoOriginal(redobuff);
      else if(currentMagicRedoAlgo.equals("guard"))
        magicRedo = new MagicRedo(redobuff);
      else 
        System.err.format("WHAT?! magic redo algo: " + currentMagicRedoAlgo);
    }
    magicRedo.charTyped(NUL);
    if (!block_redo()) {
      // old_redobuff = redobuff;
      redobuff.setLength(0);
    }
  }

  static void AppendToRedobuff(String s) {
    if(s.length() == 1) {
      AppendCharToRedobuff(s.charAt(0));
      return;
    }
    magicRedo.charTyped(NUL);
    if (!block_redo()) {
      redobuff.append(s);
    }
  }

  static void AppendCharToRedobuff(char c) {
    if (!block_redo()) {
      if(c == BS) {
        magicRedo.markRedoBackspace();
      } else {
        magicRedo.charTyped(c);
      }
      redobuff.append(c);
    } else
      magicRedo.charTyped(NUL);
  }

  static void AppendNumberToRedobuff(int n) {
    magicRedo.charTyped(NUL);
    if (!block_redo()) {
      redobuff.append(n);
    }
  }

  static void changeInsstart() {
    magicRedo.changeInsstart();
  }

  //
  // A few methods to bounce into the MagicRedo handling
  //
  static void startInputModeRedobuff() {
    // NEEDSWORK: markRedoTrackPosition(START_INPUT_MODE);
    startInsertCommand = redobuff.toString();
    magicRedo.initRedoTrackingPosition();
  }

  static void disableRedoTrackingOneEdit() {
    magicRedo.disableRedoTrackingOneEdit();
  }

  static void editComplete() {
    String s = magicRedo.editComplete();
    if(s != null && !block_redo()) {
      // replaced the redobuf with the return value
      redobuff.setLength(0);
      redobuff.append(startInsertCommand).append(s).append(ESC);
    }
  }

  static void docInsert(int pos, String s) {
    magicRedo.docInsert(pos, s);
  }

  static void docRemove(int pos, int len, String removedText) {
    magicRedo.docRemove(pos, len, removedText);
  }

  /**
   * Remove the contents of the stuff buffer and the mapped characters in the
   * typeahead buffer (used in case of an error). If 'typeahead' is true,
   * flush all typeahead characters (used when interrupted by a CTRL-C).
   */
  static void flush_buffers(boolean typeahead) {
    init_typebuf();
    start_stuff();
    stuffbuff.setLength(0); // while(read_stuff(true) != NUL);

    /* *******************************************************************
    // NEEDSWORK: finish flush_buffer
    if (typeahead) {	    // remove all typeahead
      //
      // We have to get all characters, because we may delete the first part
      // of an escape sequence.
      // In an xterm we get one char at a time and we have to get them all.
      //
      while (inchar(typebuf, typebuflen - 1, 10L))
	;
      typeoff = MAXMAPLEN;
      typelen = 0;
    } else {		    // remove mapped characters only
      typeoff += typemaplen;
      typelen -= typemaplen;
    }
    typemaplen = 0;
    no_abbr_cnt = 0;
    *********************************************************************/
  }
  
  //
  // The stuff buff used from externally by the "r" and "*" (nv_ident) commands.
  //

  static void stuffcharReadbuff(char n) {
    stuffbuff.append(n);
  }

  static void stuffnumReadbuff(int n) {
    stuffbuff.append(n);
  }

  static void stuffReadbuff(String s) {
    stuffbuff.append(s);
  }

  /**
   * Read a character from the redo buffer.
   * The redo buffer is left as it is.
   * <p>NOTE: old_redo ignored</p>
   * @param init if TRUE, prepare for redo, return FAIL if nothing to redo, OK
   * otherwise
   * @param old_redo if TRUE, use old_redobuff instead of redobuff
   */
  static private char read_redo(boolean init, boolean old_redo) {
    if(init) {
      if(redobuff.hasNext() == false) {
	return FAIL;
      } else {
	redobuff_idx = 0;
	return OK;
      }
    }
    char c = NUL; // assume none left
    if(redobuff.hasCharAt(redobuff_idx)) {
      c = redobuff.getCharAt(redobuff_idx);
      redobuff_idx++;
    }
    return c;
  }

  /**
   * Copy the rest of the redo buffer into the stuff buffer (could do faster).
   * if old_redo is TRUE, use old_redobuff instead of redobuff
   * <p>NOTE: old_redo ignored</p>
   */
  static private void copy_redo(boolean old_redo) {
    stuffReadbuff(redobuff.substring(redobuff_idx));
  }

  /**
   * Stuff the redo buffer into the stuffbuff.
   * Insert the redo count into the command.
   * If 'old_redo' is TRUE, the last but one command is repeated
   * instead of the last command (inserting text). This is used for
   * CTRL-O <.> in insert mode
   * <p>NOTE: old_redo true causes and exception</p>
   *
   * @return FAIL for failure, OK otherwise
   */
  static int start_redo(int count, boolean old_redo) {
    if(old_redo) {
      throw new RuntimeException("old_redo not false");
    }
    if(read_redo(true, old_redo) == FAIL) {
      return FAIL;
    }

    char c = read_redo(false, old_redo);

    // copy the buffer name if present
    if (c == '"') {
      stuffbuff.append(c);
      c = read_redo(false, old_redo);

      // if a numbered buffer is used, increment the number
      if (c >= '1' && c < '9')
	++c;
      stuffbuff.append(c);
      c = read_redo(false, old_redo);
    }

    /* ********************************************************/
    if (c == 'v') {   // redo Visual	// VISUAL
      G.VIsual = G.curwin.w_cursor.copy();
      G.VIsual_active = true;
      G.VIsual_reselect = true;
      G.redo_VIsual_busy = true;
      c = read_redo(false, old_redo);
    }
    /*******************************************************/

    // try to enter the count (in place of a previous count)
    if (count != 0) {
      while (Util.isdigit(c)) {	// skip "old" count
	c = read_redo(false, old_redo);
      }
      stuffbuff.append(count);
    }

    // copy from the redo buffer into the stuff buffer
    stuffbuff.append(c);
    copy_redo(old_redo);
    handle_redo = true;
    if(G.dbgRedo.getBoolean())
      System.err.println("stuffbuff = '"
                         + TextUtil.debugString(stuffbuff)+ "'");
    return OK;
  }

  /**
   * Repeat the last insert (R, o, O, a, A, i or I command) by stuffing
   * the redo buffer into the stuffbuff.
   * return FAIL for failure, OK otherwise
   */
  static int start_redo_ins() {
    char    c;

    if (read_redo(true, false) == FAIL)
      return FAIL;
    start_stuff();

    /* skip the count and the command character */
    while ((c = read_redo(false, false)) != NUL) {
      if (vim_strchr("AaIiRrOo", 0, c) >= 0) {
	if (c == 'O' || c == 'o')
	  stuffReadbuff(NL_STR);
	break;
      }
    }

    /* copy the typed text from the redo buffer into the stuff buffer */
    copy_redo(false);
    block_redo = true;
    handle_redo = true;
    return OK;
  }

  static void stop_redo_ins() {
    block_redo = false;
  }

  //
  // typebuf stuff
  //

  static int typemaplen;

  static int typebuf_maplen() {
    return typemaplen;
  }

  static private void init_typebuf() {
    typebuf.setLength(0);
  }

  /**
   * insert a string in position 'offset' in the typeahead buffer (for "@r"
   * and ":normal" command, vgetorpeek() and check_termcode())
   * <pre>
   * If noremap is 0, new string can be mapped again.
   * If noremap is -1, new string cannot be mapped again.
   * If noremap is &gt; 0, that many characters of the
   *                       new string cannot be mapped.
   * If nottyped is TRUE, the string does not return KeyTyped (don't use when
   * offset is non-zero!).
   * </pre>
   *
   * @return FAIL for failure, OK otherwise
   */
  static int ins_typebuf(String str, int noremap,
                         int offset, boolean nottyped)
  {
    return typebuf.insert(str, noremap, offset, nottyped) ? OK : FAIL;
  }

  static void user_ins_typebuf(char c)
  {
    // add at end of typebuf with remap ok
    ins_typebuf(String.valueOf(c), 0, typebuf.length(), false);
  }

  static void del_typebuf(int len, int offset)
  {
    typebuf.delete(offset, offset + len);
  }

  /**
   * Like BufferQueue, but typebuf has some special requirements.
   * In particular, typebuf has a parallel data structure to track
   * if a given char can be re-mapped.
   */
  final static class TypeBuf implements CharSequence {
    private StringBuilder buf = new StringBuilder();
    private StringBuilder noremapbuf = new StringBuilder();

    public boolean insert(String str, int noremap, int offset, boolean nottyped)
    {
      // NEEDSWORK: ins_typebuf: performance can be improved
      if(length() + str.length() > MAXTYPEBUFLEN) {
        Msg.emsg(Messages.e_toocompl);     // also calls flushbuff
        //setcursor();
        Logger.getLogger(GetChar.class.getName())
                .log(Level.SEVERE, null, new Exception("MAXTYPEBUFLEN"));
        return false;
      }

      buf.insert(offset, str);

      //
      // Adjust noremapbuf[] for the new characters:
      // If noremap  < 0: all the new characters are flagged not remappable
      // If noremap == 0: all the new characters are flagged mappable
      // If noremap  > 0: 'noremap' characters are flagged not remappable, the
      //		  rest mappable
      //

      if(noremap < 0)
        noremap = str.length();
      for(int i = 0; i < str.length(); i++) {
        noremapbuf.insert(offset + i, (char)(noremap-- > 0 ? 1 : 0));
      }

      /* ************************************************************
                      // this is only correct for offset == 0!
      if (nottyped)			// the inserted string is not typed
        typemaplen += str.length();
      if (no_abbr_cnt && offset == 0)	// and not used for abbreviations
        no_abbr_cnt += str.length();
      *************************************************************/

      return true;
    }

    void delete(int start, int end)
    {
      buf.delete(start, end);
      noremapbuf.delete(start, end);
    }

    /**
     * return a character, map as needed.
     * @return
     */
    char getChar() {
      char c;
      int loops = 0;
      while(true) {
        c = buf.charAt(0);
        if(Options.isKeyDebug(Level.FINEST))
          System.err.println("getChar check: "
                             + TextUtil.debugString(String.valueOf(c)));
        if(noremapbuf.charAt(0) == 0) {
          if(++loops > G.p_mmd) {
            Msg.emsg("recursive mapping");
            c = (char)-1;
            break;
          }
          int state = get_real_state();
          // no insert mode or cmdline mode (yet?)
          if((state & (NORMAL|VISUAL|OP_PENDING)) != 0) {
            Mapping mapping = mappings.get(c);
            if(mapping != null && (mapping.mode & state) != 0)
            {
              // ok, map it. first delete old char
              delete(0, 1);
              //
              // Insert the 'to' part in the typebuf.
              // If 'from' field is the same as the start of the
              // 'to' field, don't remap the first character.
              // If m_noremap is set, don't remap the whole 'to'
              // part.
              //
              if(Options.isKeyDebug() && loops <= 20)
                System.err.println("getChar: map: " + mapping
                        + (loops == 20 ? "... ... ..." : ""));
              if(!insert(mapping.rhs,
                         mapping.noremap
                             ? -1
                             : mapping.rhs.startsWith(mapping.lhs) ? 1 : 0,
                         0,
                         true))
              {
                c = (char)-1;
                break;
              }
              continue;
            }
          }
        }
        break;
      }

      if(c != -1) {
        delete(0, 1);
      } else {
        setLength(0); // note, this should have already been done
      }

      assert buf.length() == noremapbuf.length();
      if(Options.isKeyDebug(Level.FINEST))
        System.err.println("getChar return: "
                           + TextUtil.debugString(String.valueOf(c)));
      return c;
    }

    public void setLength(int newLength)
    {
      buf.setLength(newLength);
      noremapbuf.setLength(newLength);
    }

    boolean hasNext() {
      return buf.length() > 0;
    }

    @Override
    public int length()
    {
      return buf.length();
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
      return buf.subSequence(start, end);
    }

    @Override
    public char charAt(int index)
    {
      return buf.charAt(index);
    }

    @Override
    public String toString()
    {
      return buf.toString();
    }

  }

  //
  // The various character queues
  //

  private static final BufferQueue stuffbuff = new BufferQueue();
  private static final BufferQueue redobuff = new BufferQueue();
  private static final BufferQueue recordbuff = new BufferQueue();
  private static final TypeBuf typebuf = new TypeBuf();
  private static ViMagicRedo magicRedo = new MagicRedoOriginal(redobuff);
  private static String startInsertCommand;

  private static int last_recorded_len = 0;  // number of last recorded chars
  private static int redobuff_idx = 0;
  // static BufferQueue old_redobuff;

  /**
  * Small queue of characters. Can't extend StringBuffer, so delegate.
  */
  final static class BufferQueue implements CharSequence {
    private StringBuilder buf = new StringBuilder();

    void setLength(int length) {
      buf.setLength(length);
    }

    @Override
    public int length() {
      return buf.length();
    }

    @Override
    public char charAt(int index)
    {
      return buf.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
      return buf.subSequence(start, end);
    }

    boolean hasNext() {
      return buf.length() > 0;
    }

    char removeFirst() {
      char c = buf.charAt(0);
      buf.deleteCharAt(0);
      return c;
    }

    BufferQueue addFirst(char c) {
      buf.insert(0, c);
      return this;
    }

    char removeLast() {
      int newLength = buf.length() - 1;
      char c = buf.charAt(newLength);
      buf.setLength(newLength);
      return c;
    }

    char getLast() {
      return getLast(0);
    }

    char getLast(int i) {
      return buf.charAt(buf.length() - 1 - i);
    }

    BufferQueue insert(int ix, String s) {
      buf.insert(ix, s);
      return this;
    }

    boolean hasCharAt(int idx) {
      return buf.length() > idx;
    }

    char getCharAt(int idx) {
      return buf.charAt(idx);
    }

    String substring(int idx) {
      return buf.substring(idx);
    }

    BufferQueue append(char c) {
      buf.append(c);
      return this;
    }

    BufferQueue append(int n) {
      buf.append(n);
      return this;
    }

    BufferQueue append(String s) {
      buf.append(s);
      return this;
    }

        @Override
    public String toString() {
      return buf.toString();
    }
  }

  interface ViMagicRedo {
    void charTyped(char c);
    void markRedoBackspace();
    void changeInsstart();
    void initRedoTrackingPosition();
    void disableRedoTrackingOneEdit();
    String editComplete();
    void docInsert(int pos, String s);
    void docRemove(int pos, int len, String removedText);
  }
}

// vi:set sw=2 ts=8:
