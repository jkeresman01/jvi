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

import com.raelity.jvi.swing.KeyBinding;
import com.raelity.text.TextUtil;
import javax.swing.JEditorPane;
import javax.swing.text.AbstractDocument;

import static com.raelity.jvi.Constants.*;
import static com.raelity.jvi.KeyDefs.*;

public class GetChar {
  private static boolean block_redo = false;
  private static boolean handle_redo = false;

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
    if((key & 0xF000) == VIRT) {
      if((modifier & KeyBinding.MOD_MASK) == SHFT
                && key >= VIRT && key <= VIRT + 0x0f) {
        // only the shift key is pressed and its one of "those".
        key += SHIFTED_VIRT_OFFSET;
      }
    }
    G.setModMask(modifier);

    last_recorded_len = 0;      // for the one in vgetc()
    userInput(key);
    
    assert(!handle_redo);
    handle_redo = false;
    
    // Lock document while handling a single user character
    // EXPERIMENTAL
    AbstractDocument doc = null;
    if(true)
        doc = null; // DISABLE
    else {
        JEditorPane ep = G.curwin.getEditorComponent();
        if(ep.getDocument() instanceof AbstractDocument)
            doc = (AbstractDocument)ep.getDocument();
    }
    
    try {
        if(doc != null)
            doc.readLock();
        
        Normal.processInputChar(key, true);

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
                public void run() {
                  pumpVi();
                }
              });
            } finally {
                handle_redo = false;
            }
        }
    } finally {
        if(doc != null)
            doc.readUnlock();
    }


    // returning from event
    // only do this if no pending characters
    // but can't test for pending characters, so ....

    Misc.out_flush();
  }
  
  /** This is a special case for the two part search */
  static void fakeGotc(char key) {
    G.setModMask(0);
    Normal.processInputChar(key, true);
    Misc.out_flush();   // returning from event
  }

  // NEEDSWORK: old_mod_mask
  static char old_char;

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
        pumpChar(typebuf.removeFirst());
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
      return typebuf.removeFirst();
    }
    throw new RuntimeException("No character available");
  }

  private static void pumpChar(char c) {
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
  public static boolean getRecordedLine(StringBuffer sb) {
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
  public static void userInput(String s) {
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
   *		    2. from the stuffbuffer
   *		    3. from the typeahead buffer
   *		    4. from the user
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

  /**
   * The previous contents of the redo buffer is kept in old_redobuffer.
   * This is used for the CTRL-O <.> command in insert mode.
   */
  static void ResetRedobuff() {
    MagicRedo.markRedoTrackPosition(NUL);
    if (!handle_redo && !block_redo) {
      // old_redobuff = redobuff;
      redobuff.setLength(0);
    }
  }

  static void AppendToRedobuff(String s) {
    if(s.length() == 1) {
      AppendCharToRedobuff(s.charAt(0));
      return;
    }
    MagicRedo.markRedoTrackPosition(NUL);
    if (!handle_redo && !block_redo) {
      redobuff.append(s);
    }
  }

  static void AppendCharToRedobuff(char c) {
    if (!handle_redo && !block_redo) {
      MagicRedo.markRedoTrackPosition(c);
      redobuff.append(c);
    } else
      MagicRedo.markRedoTrackPosition(NUL);
  }

  static void AppendNumberToRedobuff(int n) {
    MagicRedo.markRedoTrackPosition(NUL);
    if (!handle_redo && !block_redo) {
      redobuff.append(n);
    }
  }

  static void appendBackspaceToRedobuff() {
    MagicRedo.markRedoBackspace();
    if (!handle_redo && !block_redo) {
      redobuff.append(BS);
    }
  }

  //
  // A few methods to bounce into the MagicRedo handling
  //
  static void startInputModeRedobuff() {
    // NEEDSWORK: markRedoTrackPosition(START_INPUT_MODE);
    MagicRedo.initRedoTrackingPosition();
  }

  static void disableRedoTrackingOneEdit() {
    MagicRedo.disableRedoTrackingOneEdit();
  }

  static void editComplete() {
    MagicRedo.editComplete();
  }

  static void docInsert(int pos, String s) {
    MagicRedo.docInsert(pos, s);
  }

  static void docRemove(int pos, int len, String removedText) {
    MagicRedo.docRemove(pos, len, removedText);
  }

  private static class MagicRedo {
    //
    // In input mode, want to track some platform changes to the document
    // and make them part of the redo buffer. This is used for code completion
    // cases. Want to KISS the problem.
    //
    // After "normal" characters are added to the redo buffer we expect the
    // character to show up in the document, additional changes are
    // incorporated into the redo buffer. BackSpace is considered a normal
    // character, because the case is so common it has to be handled.
    //
    // The NOT normal characters include newline. After a newline do not want
    // to track the autoindent
    //
    // Unfortunately there are times when "random" changes are made somewhere
    // else in the document. For example, an import may be added. This should
    // not be put into the redo buffer. So we have to keep track of where
    // user input is done and look only for local changes.
    //
    // These algorithms fail if things "go out of bounds". Eg, on NB, start with
    //      |)))
    // then enter ((, you see
    //      ((|)))))
    // then enter ';', you see
    //      (()))));|
    // this has gone out of bounds, from input mode.
    //
    // out of bounds means that the span of the change contains characters that
    // were there before editting started.
    //
    // NOTES:
    //    - If the can't track what's going on, then redobuff is considered
    //    - unusable and no attempt is made to keep it nice.
    //    - expectChar could be the actual char expected, else NUL
    //    - too complicated, if this needs much more tweaking,
    //      should probably go to a state machine.
    //    - these algorithms are probably not really general. They work for
    //      "extra" characters added for a single character at the same
    //      insertion point. They work when a character in entered, then moved
    //      to a point after the insertion stream.
    //    - The redobuff contains the command used to get into insert mode,
    //      for example "cw" or "1i". This makes it difficult to look at
    //      redobuff and learn about the insertion, but backspace makes that
    //      hard anyway. Using Edit.getInsstart() seems to work ok.
    //    - The algorithm to avoid backspacing over the insertion start
    //      is: detect in docRemove and adjust string to forget about it
    //      and in docInsert if string starts with the same string forget
    //      about it.
    //
    // NEEDSWORK:
    //        - The case where "str.leng|()" do i - ^Sp which ends up looking
    //          like "str.length()|", but the redobuff has "1ith" after working
    //          to handle the platforms delete: "leng()" and insert: "length()".
    //          Need to do that because just stuffing in "()" would end up
    //          with '.' giving stuff like "length()()".
    //          So a '.' command given "str.leng|()" produces "str.length|()"
    //          which isn't too bad. An this approach avoid modifying
    //          existing text, eg "()", with a '.' command.
    //          But that means doing i - ^Sp - x would look like "str.length()x"
    //          and then '.' would give you "lengthx()". To try to handle cases
    //          like this would have to embed special commands (uF000 range?)
    //          in the redobuff.
    //

    private static int redoTrackPosition = -1;
    private static boolean expectChar;
    private static boolean disableTrackingOneEdit;
    // afterBuff, characters added after the current insert position
    private static BufferQueue afterBuff = new BufferQueue();

    // See docRemove() method for details of the removeDocXxx variables
    // The variable remvoeDocAfterString also acts as a flag. It is non-null
    // very briefly, only for one operation, insert or remove.
    private static String removeDocAfterString; // check when non-null
    private static String removeDocBeforeInsstart;

    static void disableRedoTrackingOneEdit() {
      disableTrackingOneEdit = true;
    }

    static void editComplete() {
      disableTrackingOneEdit = false;
    }

    static void initRedoTrackingPosition() {
      expectChar = false;
      redoTrackPosition = G.curwin.getCaretPosition();
      if(G.dbgRedo.value)
        System.err.format("initRedoTrackingPosition %d '%s'\n",
                          redoTrackPosition,
                          TextUtil.debugString(redobuff.toString()));
    }

    private static boolean notTracking() {
       return redoTrackPosition < 0
               || !G.redoTrack.value
               || disableTrackingOneEdit;
    }

    static void markRedoBackspace() {
      if(redoTrackPosition >= 0) {
        int prevRedoTrackPosition = redoTrackPosition;
        redoTrackPosition = G.curwin.getCaretPosition();
        if(G.dbgRedo.value)
          System.err.format("markRedoTrackPositionBackspace %d --> %d '%s'\n",
                            prevRedoTrackPosition,
                            redoTrackPosition,
                            TextUtil.debugString(redobuff.toString()));
      }
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // markRedoTrackPosition

    static void markRedoTrackPosition(char c) {
      if(!G.redoTrack.value)
        return;
      removeDocAfterString = null;
      if(expectChar)
        System.err.println("markRedoPosition ERROR: expectChar");

      if(didNotInsertableChar(c))
        return;

      expectChar = true;
      int prevRedoTrackPosition = redoTrackPosition;
      redoTrackPosition = G.curwin.getCaretPosition();
      if(prevRedoTrackPosition >= 0) {
        int skip = redoTrackPosition - prevRedoTrackPosition;
        // skip is 0 when the character comming in is where it is expected
        if(skip != 0) {
          String before = null; // debug
          if(G.dbgRedo.value)
            before = afterBuff.toString();
          if(skip < 0) { // '[' --> '[|]', then NOW char, eg 'x', '[x|]'
            // One or more chars inserted before the track position,
            // take them out of redobuff and save them.
            // Assuming will leave at least one character in redobuff.
            prependRedoBuffToAfterBuf(-skip); // **** or (-skip) - 1
          } else if(skip > 0) { // like when ';' to end of ')))'
            // One or more characters skipped forward,
            // move chars from afterBuff to redoBuff.
            // Don't move if further than all of afterBuff
            appendAfterBufToRedoBuff(skip);
          }
          debugMarkRedoTrackPositionSKIP(skip, before);
        }
      }
      debugMarkRedoTrackPosition(prevRedoTrackPosition, c);
    }

    // assist for markRedoTrackPosition
    private static boolean didNotInsertableChar(char c) {
      if((G.State & BASE_STATE_MASK) != INSERT
         || c < 0x20 && (c != BS
                      && c != TAB)
         || c == DEL
         || (c & 0xF000) == VIRT) {
        if(G.dbgRedo.value)debug("markRedoPosition OFF");
        redoTrackPosition = -1;
        expectChar = false;
        //
        // dump any leftover chars onto redo buff
        //
        if(afterBuff.length() > 0) {
          if(G.dbgRedo.value)debug("markRedoPosition leftovers: " + afterBuff);
          appendAfterBufToRedoBuff(afterBuff.length());
        }
        return true;
      }
      return false;
    }
    // assist for markRedoTrackPosition
    private static void debugMarkRedoTrackPositionSKIP(int skip, String before) {
      if(G.dbgRedo.value)
        System.err.format("markRedoPosition SKIP[%d]: %s[%d] --> %s[%d]\n",
                          skip,
                          before, before.length(),
                          afterBuff, afterBuff.length());
    }
    // assist for markRedoTrackPosition
    private static void
    debugMarkRedoTrackPosition(int prevRedoTrackPosition, char c) {
      if(G.dbgRedo.value)
        System.err.format("markRedoPosition %d --> %d '%c' '%s'\n",
                          prevRedoTrackPosition,
                          redoTrackPosition,
                          c,
                          TextUtil.debugString(redobuff.toString()));
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // docInsert

    static void docInsert(int pos, String s) {
      if(G.dbgRedo.value) debugDocInsert(pos, s);
      if(doingBackspace() || notTracking())
        return;
      if(removeDocBeforeInsstart != null && s != null
              && s.startsWith(removeDocBeforeInsstart)) {
        if(G.dbgRedo.value) debugDocInsertMATCH_BEFORE();
        int len = removeDocBeforeInsstart.length();
        pos += len;
        s = s.substring(len);
      }
      removeDocBeforeInsstart = null;

      if(expectChar) {
        if(pos == redoTrackPosition) {
          if(G.dbgRedo.value)debugDocInsertMATCH_EXPECTED(pos, s);
          redoTrackPosition += 1;
        } else if(pos != redoTrackPosition)
          debugDocInsertERROR(pos, s);
        if(s.length() != 1) {
          debugDocInsertLONG(pos, s);
          // add in the extra
          redobuff.append(s.substring(1));
          redoTrackPosition += s.length() - 1;
        }
      } else {
        if(pos == redoTrackPosition && removeDocAfterString == null) {
          if(G.dbgRedo.value) debugDocInsertMATCH_EXTRA(pos, s);
          //
          // Add the mystery string to the redo buffer
          //
          redobuff.append(s);
          redoTrackPosition += s.length();
        } else {
          if(G.dbgRedo.value) debugDocInsertNO_MATCH();

          if(didCharWarpedRight(pos, s)) {
            // An insert to the right of where the next character was expected
            // and afterBuff had enough chars to cover it.
            // "(((|)))" enter ';' and get "((()));|"
            nop();
          } else if(didDeleteInsertAfterTrackPosition(pos, s)) {
            // Re-inserting characters that were deleted after Trackd' Pos
            // See comments in docRemoveInternal/didDocRemoveAfterTrackPosition
            nop();
          }
        }
      }
      removeDocAfterString = null;
      expectChar = false;
    }
    // docInsert assist
    private static boolean didCharWarpedRight(int pos, String s) {
      int warpDistance = pos - redoTrackPosition;
      // Is there enough chars, stashed from the insert,
      // in afterBuff to cover the warp
      if(s.length() == 1
              && warpDistance > 0
              && warpDistance <= afterBuff.length()) {

        // Handle character warp, like for ';' on NB. Given
        //       (((|)))
        // enter ';', and you get
        //       ((()));|
        // NetBeans add the ';' at the cursor, then removes it;
        // so at this point, there is ";\b" in redobuff, they must be removed
        // (which breaks vim's "rule" about leaving in the \b), otherwise
        // during a redo the ';' causes problems.
        //
        // Be very strict (at least for now)

        // get rid of ';\b' if its there
        if(redobuff.length() >= 2
                && redobuff.getLast(0) == BS
                && redobuff.getLast(1) == s.charAt(0)) {
          redobuff.removeLast();
          redobuff.removeLast();
        }
        int i = warpDistance;
        while(i-- > 0)
          redobuff.append(afterBuff.removeFirst());
        redobuff.append(s);
        if(G.dbgRedo.value) debugDocInsertWARP(warpDistance);
        return true;
      }
      return false;
    }
    private static boolean
    didDeleteInsertAfterTrackPosition(int pos, String s)
    {
      if(removeDocAfterString != null
                && redoTrackPosition == pos
                && s.endsWith(removeDocAfterString)
              ) {
        // This is part of the "Delete stuff *after* the insertion point".
        // See comments in docRemove

        int count = s.length() - removeDocAfterString.length();
        String t = s.substring(0, count);
        redobuff.append(t);
        // NOTE: not adjusting redoTrackPosition, so no more stuff gets put
        // in the redo buffer.
        if(G.dbgRedo.value) debugDocInsertMATCH_REMOVE_EXTRA(pos, t);
        return true;
      }
      return false;
    }
    private static void debugDocInsert(int pos, String s) {
      System.err.format("docInsert: pos %d, %d, '%s'\n",
                        pos, s.length(), TextUtil.debugString(s));
    }
    private static void debugDocInsertMATCH_BEFORE() {
      if(G.dbgRedo.value)
        System.err.format("docInsert MATCH BEFORE '%s'\n",
                          TextUtil.debugString(removeDocBeforeInsstart));
    }
    private static void debugDocInsertMATCH_EXPECTED(int pos, String s) {
      if(G.dbgRedo.value) {
        System.err.println("docInsert MATCH expected " + pos
                           + (s.length() > 1 ? " LENGTH: " + s.length() : ""));
      }
    }
    private static void debugDocInsertERROR(int pos, String s) {
      // NOTE: error condition, not based on dbgRedo
      System.err.format("docInsert ERROR: pos %d, redoPosition %d, '%s'\n",
                        pos, redoTrackPosition, s);
    }
    private static void debugDocInsertLONG(int pos, String s) {
      // NOTE: error condition, not based on dbgRedo
      System.err.format("docInsert LONG: %d, length %d\n",
                        pos, s.length());
    }
    private static void debugDocInsertMATCH_EXTRA(int pos, String s) {
      if(G.dbgRedo.value)
        System.err.format("docInsert MATCH EXTRA: %d '%s'\n",
                          pos, TextUtil.debugString(s));
    }
    private static void debugDocInsertNO_MATCH() {
      if(G.dbgRedo.value)
        System.err.format("docInsert: NO MATCH redoPosition %d, redobuff %s[%d]"
                          + " afterBuff %s[%d]\n",
                          redoTrackPosition,
                          redobuff, redobuff.length(),
                          afterBuff, afterBuff.length());
    }
    private static void debugDocInsertWARP(int warpDistance) {
      System.err.format("docInsert: WARP %d, redobuff %s[%d]"
                        + " afterBuff %s[%d]\n",
                        warpDistance,
                        redobuff, redobuff.length(),
                        afterBuff, afterBuff.length());
    }
    private static void debugDocInsertMATCH_REMOVE_EXTRA(int pos, String t) {
      System.err.format("docInsert MATCH REMOVE/EXTRA:"
                         + " %d, '%s' / '%s'\n",
                         pos,
                         TextUtil.debugString(removeDocAfterString),
                         TextUtil.debugString(t));
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // docRemove

    static void docRemove(int pos, int len, String removedText) {
      removeDocAfterString = null;
      debugDocRemove(pos, len, removedText);
      if(doingBackspace() || notTracking())
        return;

      // Don't worry about before insertion stuff if probably a backspace
      // NEEDSWORK: with current backspace handling can probably simplify here
      if(!expectChar || len != 1) {
        int nChar = didDocRemoveBeforeInsertionStart(pos, len, removedText);
        if(nChar > 0) {
          pos += nChar;
          len -= nChar;
          removedText = removedText.substring(nChar);
        }
      }

      if(pos + len == redoTrackPosition) {
        if(G.dbgRedo.value) debugDocRemoveMATCH(pos, len);
        docRemoveExtendedWithRedobuf(len);
        redoTrackPosition -= len;
      } else if(didDocRemoveAfterTrackPosition(pos, len, removedText)) {
      } else {
        if(G.dbgRedo.value) debugDocRemoveNO_MATCH();
      }
      expectChar = false;
    }
    private static int
    didDocRemoveBeforeInsertionStart(int pos, int len, String removedText) {
      int insstartOffset = Edit.getInsstart().getOffset();
      removeDocBeforeInsstart = null;
      int nChar = insstartOffset - pos;
      if(nChar > 0) {
        if(removedText != null && removedText.length() >= nChar) {
          // Do not even try to remove text before innstart
          removeDocBeforeInsstart = removedText.substring(0, nChar);
          if(G.dbgRedo.value)debugDocRemoveBEFORE_INSSTART();
        } else {
          nChar = 0;
          if(G.dbgRedo.value)debugDocRemoveBEFORE_INSSTART_NO_MATCH();
        }
      }
      return nChar;
    }
    private static void
    docRemoveExtendedWithRedobuf(int len) {
        //
        // Delete the string from the redo buffer, using BS
        //
        if(expectChar && len != 1) {
          System.err.println("docRemove ERROR: expectChar len = " + len);
        }
        // when expectChar, the BS is already in redobuff
        if(!expectChar) {
          for(int i = len; i > 0; i--)
            redobuff.append(BS);
        }
    }
    private static boolean
    didDocRemoveAfterTrackPosition(int pos, int len, String removedText) {
      if(pos + len > redoTrackPosition) {  // goes past insertion point
        int nBefore = redoTrackPosition - pos;
        int insstart = Edit.getInsstart().getOffset();
        int nInsertedBefore = redoTrackPosition - insstart;
        if(nBefore <= nInsertedBefore  // enough to remove
                  && removedText != null  // there's something to work with
                  && afterBuff.length() == 0  // don't deal with it if this not empty
                ) {
          // Deleting chars *after* the insertion point; maybe in the document
          // before insert started. If this docRemove is immeadiately followed
          // by docInsert whose last characters are the same as the ones being
          // removed, then handle it by stripping the chars from the end of
          // the insert. For example given,
          //    foo.ch|()
          // and code complete with no args, then the "ch()" are deleted
          // and then "charValue()" is inserted. Remove the "ch" here and
          // in docInsert handler strip the matched "()" from the insert
          // string before adding to redobuff. Thus we avoid
          // modifying non-inserted text.

          // stash the characters after insertion point that are deleted

          // check out removeDocAfterString != null in docInsert...
          try {
            removeDocAfterString = removedText.substring(nBefore);
            docRemoveExtendedWithRedobuf(nBefore);
            redoTrackPosition -= nBefore;

            if(G.dbgRedo.value) debugDocRemoveREMOVE_AFTER(nBefore);
            return true;
          } catch(Exception ex) {

            // HACK ALERT
            StackTraceElement[] stack = ex.getStackTrace();
            boolean skip = false;
            for (int i = 0; i < stack.length; i++) {
              String e = stack[i].toString();
              if(stack[i].toString().contains("form.JavaCodeGenerator")) {
                skip = true;
                break;
              }

            } // END HACK ALERT

            if(!skip)
              ViManager.dumpStack("redo tracking: remove after");
          }
        }
      }
      return false;
    }
    private static void debugDocRemove(int pos, int len, String removedText) {
      if(G.dbgRedo.value)
        System.err.format("docRemove: pos %d, %d, '%s', track %d, insstart %d\n",
                          pos, len, TextUtil.debugString(removedText),
                          redoTrackPosition, Edit.getInsstart().getOffset());
    }
    private static void debugDocRemoveBEFORE_INSSTART() {
      if(G.dbgRedo.value) System.err.format("docRemove BEFORE INSSTART: '%s'\n",
                          TextUtil.debugString(removeDocBeforeInsstart));
    }
    private static void debugDocRemoveBEFORE_INSSTART_NO_MATCH() {
      if(G.dbgRedo.value)
        System.err.format("docRemove BEFORE INSSTART NO MATCH\n");
    }
    private static void debugDocRemoveMATCH(int pos, int len) {
      if(G.dbgRedo.value)
        System.err.format(
                  "docRemove MATCH%s: redoPosition %d --> %d, length %d\n",
                  (expectChar ? "-expect" : ""),
                  redoTrackPosition, pos, len);
    }
    private static void debugDocRemoveREMOVE_AFTER(int nBefore) {
      if(G.dbgRedo.value)
        System.err.println("docRemove REMOVE AFTER,"
                           + " nBefore: " + nBefore
                           + " AfterString: '" + removeDocAfterString + "'");
    }
    private static void debugDocRemoveNO_MATCH() {
      if(G.dbgRedo.value)
        System.err.println("docRemove NO MATCH");
    }

    private static boolean appendAfterBufToRedoBuff(int len)
    {
      // OPTIMIZE: get rid of loop
      if(afterBuff.length() < len)
        return false;
      while(len-- > 0)
        redobuff.append(afterBuff.removeFirst());
      return true;
    }

    private static boolean prependRedoBuffToAfterBuf(int len)
    {
      // OPTIMIZE: get rid of loop
      if(redobuff.length() < len)
        return false;
      while(len-- > 0)
        afterBuff.addFirst(redobuff.removeLast());
      return true;
    }

    private static boolean doingBackspace() {
      if(Edit.doingBackspace) {
        if(G.dbgRedo.value) System.err.println("doing BACKSPACE");
        return true;
      }
      return false;
    }

    private static void debug(String s)
    {
        if(G.dbgRedo.value)
          System.err.println(s);
    }

    private static void nop() {}
  }

  /**
   * Remove the contents of the stuff buffer and the mapped characters in the
   * typeahead buffer (used in case of an error). If 'typeahead' is true,
   * flush all typeahead characters (used when interrupted by a CTRL-C).
   */
  public static void flush_buffers(boolean typeahead) {
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
    if(G.dbgRedo.value)
      System.err.println("stuffbuff = '" + stuffbuff + "'");
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
      if (Util.vim_strchr("AaIiRrOo", c) != null) {
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
   * <p>
   * If noremap is 0, new string can be mapped again.
   * <br/>If noremap is -1, new string cannot be mapped again.
   * <br/>If noremap is >0, that many characters of the new string
   * cannot be mapped.
   * </p><p>
   * If nottyped is TRUE, the string does not return KeyTyped (don't use when
   * offset is non-zero!).
   * </p>
   *
   * @return FAIL for failure, OK otherwise
   */
  static int ins_typebuf(String str, int noremap,
                         int offset, boolean nottyped)
  {
    // NEEDSWORK: ins_typebuf: performance can be improved
    if(typebuf.length() + str.length() > MAXTYPEBUFLEN) {
      Msg.emsg(Messages.e_toocompl);     // also calls flushbuff
      //setcursor();
      return FAIL;
    }

    typebuf.insert(0, str);

    //
    // Adjust noremapbuf[] for the new characters:
    // If noremap  < 0: all the new characters are flagged not remappable
    // If noremap == 0: all the new characters are flagged mappable
    // If noremap  > 0: 'noremap' characters are flagged not remappable, the
    //			rest mappable
    //

    ////// NEEDSWORK: ins_typebuf: mapping stuff

    /* ************************************************************
		    // this is only correct for offset == 0!
    if (nottyped)			// the inserted string is not typed
      typemaplen += str.length();
    if (no_abbr_cnt && offset == 0)	// and not used for abbreviations
      no_abbr_cnt += str.length();
    *************************************************************/

    return OK;
  }

  //
  // The various character queues
  //

  private static final BufferQueue stuffbuff = new BufferQueue();
  private static final BufferQueue redobuff = new BufferQueue();
  private static final BufferQueue recordbuff = new BufferQueue();
  private static final BufferQueue typebuf = new BufferQueue();

  private static int last_recorded_len = 0;  // number of last recorded chars
  private static int redobuff_idx = 0;
  // static BufferQueue old_redobuff;

  /**
  * Small queue of characters. Can't extend StringBuffer, so delegate.
  */
  static class BufferQueue {
    private StringBuilder buf = new StringBuilder();

    void setLength(int length) {
      buf.setLength(length);
    }

    int length() {
      return buf.length();
    }

    boolean hasNext() {
      return buf.length() > 0;
    }

    /** @deprecated in favor of getFirst */
    char getNext() {
      char c = buf.charAt(0);
      buf.deleteCharAt(0);
      return c;
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
}

// vi:set sw=2 ts=8:
