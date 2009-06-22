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
            try {
                Misc.beginRedoUndo();
                pumpVi();
            } finally {
                Misc.endRedoUndo();
                handle_redo = false;
            }
        }
    } finally {
        if(doc != null)
            doc.readUnlock();
    }
    Misc.out_flush();   // returning from event
                        // only do this if no pending characters
                        // but can't test for pending characters, so ....
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
   * <br>
   * if "advance" is TRUE (vgetc()):
   *	really get the character.
   *	KeyTyped is set to TRUE in the case the user typed the key.
   *	KeyStuffed is TRUE if the character comes from the stuff buffer.
   * <br>
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
    markRedoTrackPosition(NUL);
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
    markRedoTrackPosition(NUL);
    if (!handle_redo && !block_redo) {
      redobuff.append(s);
    }
  }

  static void AppendCharToRedobuff(char c) {
    if (!handle_redo && !block_redo) {
      markRedoTrackPosition(c);
      redobuff.append(c);
    } else
      markRedoTrackPosition(NUL);
  }

  static void AppendNumberToRedobuff(int n) {
    markRedoTrackPosition(NUL);
    if (!handle_redo && !block_redo) {
      redobuff.append(n);
    }
  }

  static void startInputModeRedobuff() {
    // NEEDSWORK: markRedoTrackPosition(START_INPUT_MODE);
    initRedoTrackingPosition();
  }

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
  //    - expectChar could be the actual char expected, else NUL
  //    - too complicated, if this needs much more tweaking,
  //      should probably go to a state machine.
  //    - these algorithms are probably not really general. They work for
  //      "extra" characters added for a single character at the same
  //      insertion point. They work when a character in entered, then moved
  //      to a point after the insertion stream.
  //
  // NEEDSWORK:
  //  - Need bug fix
  //    If the file has
  //        string.le()
  //    and the caret is on the '(', and you enter input mode and enter "ng"
  //    then code complete to
  //        string.length()
  //    This generates a remove of 'leng()' and an insert of length().
  //    Want the redobuff to have '\b\bngth' or even 'th', but we end
  //    up with '\b\b\b\blength' which backspaces over the insertion point.
  //
  //    Probably want to keep the '1i' separate, and only consider the
  //    'ng'. We can see the leng in the document and the ng in the redobuf
  //    and go from there. Need to add more structure to this process and
  //    work with high level concepts,
  //    e.g. substr(0,2) matches in document at pos
  //    or substr(0,4) match doc/redo and returns position/count in redo
  //    or somesuch...
  //
  //    Following shows the problem situation. BeforeLen is 4, but this goes
  //    beyond the original insert point. Part of the problem is that the '1i'
  //    is interpreted as part of what can be backspaced over
  //        markRedoPosition OFF
  //        initRedoTrackingPosition 1876 '1i'
  //        CharAction: 'n' 6e(110) 0
  //        markRedoPosition 1876 --> 1876 'n' '1i'
  //        docInsert: pos 1876, 1, 'n'
  //        docInsert MATCH expected 1876
  //        CharAction: 'g' 67(103) 0
  //        markRedoPosition 1877 --> 1877 'g' '1in'
  //        docInsert: pos 1877, 1, 'g'
  //        docInsert MATCH expected 1877
  //        CharAction: REJECT: ' ' 20(32) 2
  //        docRemove: pos 1874, 6, 'leng()'
  //        docRemove NO MATCH, BeforeLen: 4 AfterString: '()'
  //        docInsert: pos 1874, 8, 'length()'
  //        docInsert: NO MATCH redoPosition 1878, redobuff 1ing[4] afterBuff [0]
  //        docRemove: pos 1874, 4, '(null)'
  //        docRemove MATCH: redoPosition 1878 --> 1874, length 4
  //        docInsert MATCH REMOVE/EXTRA: 1874, beforeLen 4, '()'/'length'
  //        KeyAction: ViEscapeKey: 1b(27) 0
  //        markRedoPosition OFF
  //        ...
  //        CharAction: '.' 2e(46) 0
  //        stuffbuff = '1inglength'
  //
  // NEEDSWORK:
  //  - The idea behind initRedoTrackingPosition(), currently disabled,
  //    If the file has
  //        string.le()
  //    and the caret is on the '(', and you enter input mode and start code
  //    completion, this doesn't work because the redoTrackingPos is -1, but
  //    if you enter 'n', then it works. It would probably be safe to record
  //    the position when input mode is establish, rather than counting of the
  //    first character input.

  private static int redoTrackPosition = -1;
  private static boolean expectChar;
  private static boolean disableTrackingOneEdit;
  // afterBuff, characters added after the current insert position
  private static BufferQueue afterBuff = new BufferQueue();

  // See docRemove() method for details of the removeDocXxx variables
  // The variable remvoeDocAfterString also acts as a flag. It is non-null
  // very briefly, only for one operation, insert or remove.
  private static String removeDocAfterString; // check when non-null
  private static int removeDocBeforeLength;

  static void disableRedoTrackingOneEdit() {
    disableTrackingOneEdit = true;
  }

  static void editComplete() {
    disableTrackingOneEdit = false;
  }

  private static void initRedoTrackingPosition() {
    // disable this method for now, it doesn't do anything. see above
    if(true)
      return;

    expectChar = false;
    redoTrackPosition = G.curwin.getCaretPosition();
    if(G.dbgRedo.value)
      System.err.println(String.format("initRedoTrackingPosition %d '%s'",
                                       redoTrackPosition,
                                       TextUtil.debugString(redobuff.toString())));
  }

  private static void markRedoTrackPosition(char c) {
    if(!G.redoTrack.value)
      return;
    removeDocAfterString = null;
    if(expectChar)
      System.err.println("markRedoPosition ERROR: expectChar");
    if((G.State & BASE_STATE_MASK) != INSERT
       || c < 0x20 && (c != BS
                    && c != TAB)
       || c == DEL
       || (c & 0xF000) == VIRT) {
      if(G.dbgRedo.value)
        System.err.println("markRedoPosition OFF");
      redoTrackPosition = -1;
      expectChar = false;
      // dump any leftover chars onto redo buff
      if(afterBuff.length() > 0) {
        if(G.dbgRedo.value)
          System.err.println("markRedoPosition leftovers: " + afterBuff);
        redobuff.append(afterBuff.toString());
        afterBuff.setLength(0);
      }
      return;
    }
    expectChar = true;
    int prevRedoTrackPosition = redoTrackPosition;
    redoTrackPosition = G.curwin.getCaretPosition();
    if(prevRedoTrackPosition >= 0) {
      int skip = redoTrackPosition - prevRedoTrackPosition;
      if(skip != 0) {
        String before = null; // debug
        String skipString = null; // debug
        if(G.dbgRedo.value)
          before = afterBuff.toString();
        if(skip < 0) {
          // One or more chars skipped back before the end position,
          // take them out of redobuff and save them.
          // Typically entered something like '[' and '[|]' put into document
          // with insertion point as indicated by '|'.
          // Assuming will leave at least one character in redobuff.
          if(redobuff.length() + skip > 0) { // recall skip is negative
            int i = skip;
            while(i++ < 0)
              afterBuff.addFirst(redobuff.removeLast());
          }
        } else if(skip > 0) {
          // One or more characters skipped forward,
          // move chars from afterBuff to redoBuff.
          // Don't move if further than all of afterBuff
          if(afterBuff.length() - skip >= 0) {
            int i = skip;
            while(i-- > 0)
              redobuff.append(afterBuff.removeFirst());
          }
        }
        if(G.dbgRedo.value)
          System.err.println(String.format(
                        "markRedoPosition SKIP[%d]: %s[%d] --> %s[%d]",
                        skip,
                        before, before.length(),
                        afterBuff, afterBuff.length()));
      }
    }
    if(G.dbgRedo.value)
      System.err.println(String.format("markRedoPosition %d --> %d '%c' '%s'",
                                       prevRedoTrackPosition,
                                       redoTrackPosition,
                                       c,
                                       TextUtil.debugString(redobuff.toString())));
      //System.err.println("markRedoPosition " + redoTrackPosition + " '" + c + "'");
  }

  static void docInsert(int pos, String s) {
    if(G.dbgRedo.value)
      System.err.println(String.format("docInsert: pos %d, %d, '%s'",
                                     pos, s.length(), TextUtil.debugString(s)));
    if(redoTrackPosition < 0 || !G.redoTrack.value || disableTrackingOneEdit)
      return;
    if(expectChar) {
      if(pos == redoTrackPosition) {
        if(G.dbgRedo.value) {
          System.err.println("docInsert MATCH expected " + pos
                             + (s.length() > 1 ? " LENGTH: " + s.length() : ""));
        }
        redoTrackPosition += 1;
      } else if(pos != redoTrackPosition)
        System.err.println(String.format(
                      "docInsert ERROR: pos %d, redoPosition %d, '%s'",
                      pos, redoTrackPosition, s));
      if(s.length() != 1) {
        System.err.println(String.format("docInsert LONG: %d, length %d",
                                          pos, s.length()));
        // add in the extra
        redobuff.append(s.substring(1));
        redoTrackPosition += s.length() - 1;
      }
    } else {
      if(pos == redoTrackPosition) {
        if(G.dbgRedo.value)
          System.err.println(String.format("docInsert MATCH EXTRA: %d '%s'",
                                         pos, TextUtil.debugString(s)));
        //
        // Add the mystery string to the redo buffer
        //
        redobuff.append(s);
        redoTrackPosition += s.length();
      } else {
        if(G.dbgRedo.value)
          System.err.println(String.format(
                      "docInsert: NO MATCH redoPosition %d, redobuff %s[%d]"
                      + " afterBuff %s[%d]",
                      redoTrackPosition,
                      redobuff, redobuff.length(),
                      afterBuff, afterBuff.length()));

        // Is there enough chars, stashed from the insert,
        // in afterBuff to cover the warp
        int warpDistance = pos - redoTrackPosition;
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
          if(G.dbgRedo.value)
            System.err.println(String.format(
                        "docInsert: WARP %d, redobuff %s[%d]"
                        + " afterBuff %s[%d]",
                        warpDistance,
                        redobuff, redobuff.length(),
                        afterBuff, afterBuff.length()));
        } else if(removeDocAfterString != null
                  && redoTrackPosition - removeDocBeforeLength == pos
                  && s.endsWith(removeDocAfterString)
                ) {
          // This is part of the "Delete char *after* the insertion point".
          // See comments in docRemoveInternal
          docRemoveInternal(pos, removeDocBeforeLength, null); // MATCH

          int count = s.length() - removeDocAfterString.length();
          String t = s.substring(0, count);
          redobuff.append(t);
          // NOTE: not adjusting redoTrackPosition, so no more stuff gets put
          // in the redo buffer. For that to work would have to actually do
          // the delete in the document. (there are ways..., put a virt char
          // in the document that means delete, may also need forward backward
          // commands, too messy)
          if(G.dbgRedo.value)
            System.err.println(String.format("docInsert MATCH REMOVE/EXTRA:"
                               + " %d, beforeLen %d, '%s'/'%s'",
                               pos,
                               removeDocBeforeLength,
                               TextUtil.debugString(removeDocAfterString),
                               TextUtil.debugString(t)));
        }
      }
    }
    removeDocAfterString = null;
    expectChar = false;
  }

  static void docRemove(int pos, int len, String removedText) {
    removeDocAfterString = null;
    docRemoveInternal(pos, len, removedText);
  }

  private static void docRemoveInternal(int pos, int len, String removedText) {
    if(G.dbgRedo.value)
      System.err.println(String.format("docRemove: pos %d, %d, '%s'",
                                  pos, len, TextUtil.debugString(removedText)));
    if(redoTrackPosition < 0 || !G.redoTrack.value || disableTrackingOneEdit)
      return;
    if(pos + len == redoTrackPosition) {
      if(G.dbgRedo.value)
        System.err.println(String.format(
                  "docRemove MATCH%s: redoPosition %d --> %d, length %d",
                  (expectChar ? "-expect" : ""),
                  redoTrackPosition, pos, len));
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
      redoTrackPosition -= len;
    } else if(pos + len > redoTrackPosition  // goes past insertion point
              && (removeDocBeforeLength = redoTrackPosition - pos)
                            <= redobuff.length()  // enought to remove
              && removedText != null  // there's something to work with
              && afterBuff.length() == 0  // don't deal with it if this not empty
            ) {
      // Deleting chars *after* the insertion point; they were in the document
      // before the insert started. If this docRemove is immeadiately followed
      // by a docInsert whose last characters are the same as the ones being
      // removed, then we'll handle it by stripping the chars from the end of
      // the insert. For example given,
      //    foo.ch|()
      // and code complete to method with no args, then the "ch()" are deleted
      // and then "charValue()" is inserted. In docInsert set up the
      // redo buffer to remove the "ch" snd insert "charValue". Thus we avoid
      // modifying non-inserted text.

      // stash the characters after insertion point that are deleted

      removeDocAfterString = removedText.substring(removeDocBeforeLength);

      if(G.dbgRedo.value)
        System.err.println("docRemove NO MATCH,"
                           + " BeforeLen: " + removeDocBeforeLength
                           + " AfterString: '" + removeDocAfterString + "'");
    } else {
      if(G.dbgRedo.value)
        System.err.println("docRemove NO MATCH");
    }
    expectChar = false;
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
    stuffbuff.append((char)n);
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
      stuffbuff.append((char)c);
      c = read_redo(false, old_redo);

      // if a numbered buffer is used, increment the number
      if (c >= '1' && c < '9')
	++c;
      stuffbuff.append((char)c);
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
    stuffbuff.append((char)c);
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
   * <br>If noremap is -1, new string cannot be mapped again.
   * <br>If noremap is >0, that many characters of the new string
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
