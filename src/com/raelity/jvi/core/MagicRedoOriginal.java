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

import com.raelity.jvi.core.GetChar.BufferQueue;
import com.raelity.jvi.manager.ViManager;
import com.raelity.text.TextUtil;

import static com.raelity.jvi.core.Constants.*;
import static com.raelity.jvi.core.KeyDefs.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
class MagicRedoOriginal implements GetChar.ViMagicRedo
{
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

    private int redoTrackPosition = -1;
    private boolean expectChar;
    private boolean disableTrackingOneEdit;
    // afterBuff, characters added after the current insert position
    private BufferQueue afterBuff = new BufferQueue();

    // See docRemove() method for details of the removeDocXxx variables
    // The variable remvoeDocAfterString also acts as a flag. It is non-null
    // very briefly, only for one operation, insert or remove.
    private String removeDocAfterString; // check when non-null
    private String removeDocBeforeInsstart;

    // redobuff is the master redobuff, MagicRedo pokes it in various ways
    private final BufferQueue redobuff;

    MagicRedoOriginal(BufferQueue redobuff)
    {
        if(G.dbgRedo.getBoolean())
            System.err.format("CONSTRUCT redo: MagicRedoOriginal\n");
        this.redobuff = redobuff;
    }

    @Override
    public void disableRedoTrackingOneEdit() {
      disableTrackingOneEdit = true;
    }

    @Override
    public String editComplete() {
      disableTrackingOneEdit = false;
      return null;
    }

    @Override
    public void initRedoTrackingPosition() {
      expectChar = false;
      redoTrackPosition = G.curwin.getCaretPosition();
      if(G.dbgRedo.getBoolean())
        System.err.format("initRedoTrackingPosition %d '%s'\n",
                          redoTrackPosition,
                          TextUtil.debugString(redobuff.toString()));
    }

    private boolean notTracking() {
       return redoTrackPosition < 0
               || !G.redoTrack.getBoolean()
               || disableTrackingOneEdit;
    }

    @Override
    public void markRedoBackspace() {
      if(redoTrackPosition >= 0) {
        int prevRedoTrackPosition = redoTrackPosition;
        redoTrackPosition = G.curwin.getCaretPosition();
        if(G.dbgRedo.getBoolean())
          System.err.format("markRedoTrackPositionBackspace %d --> %d '%s'\n",
                            prevRedoTrackPosition,
                            redoTrackPosition,
                            TextUtil.debugString(redobuff.toString()));
      }
    }

    @Override
    public void changeInsstart()
    {
        System.err.println("CHANGE INSSTART");
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // markRedoTrackPosition

    @Override
    public void charTyped(char c) {
      if(!G.redoTrack.getBoolean())
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
          if(G.dbgRedo.getBoolean())
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
    private boolean didNotInsertableChar(char c) {
      if((G.State & BASE_STATE_MASK) != INSERT
         || c < 0x20 && (c != BS
                      && c != TAB)
         || c == DEL
         || (c & 0xF000) == VIRT) {
        if(G.dbgRedo.getBoolean())debug("markRedoPosition OFF");
        redoTrackPosition = -1;
        expectChar = false;
        //
        // dump any leftover chars onto redo buff
        //
        if(afterBuff.length() > 0) {
          if(G.dbgRedo.getBoolean())debug("markRedoPosition leftovers: " + afterBuff);
          appendAfterBufToRedoBuff(afterBuff.length());
        }
        return true;
      }
      return false;
    }
    // assist for markRedoTrackPosition
    private void debugMarkRedoTrackPositionSKIP(int skip, String before) {
      if(G.dbgRedo.getBoolean())
        System.err.format("markRedoPosition SKIP[%d]: %s[%d] --> %s[%d]\n",
                          skip,
                          before, before.length(),
                          afterBuff, afterBuff.length());
    }
    // assist for markRedoTrackPosition
    private void
    debugMarkRedoTrackPosition(int prevRedoTrackPosition, char c) {
      if(G.dbgRedo.getBoolean())
        System.err.format("markRedoPosition %d --> %d '%c' '%s'\n",
                          prevRedoTrackPosition,
                          redoTrackPosition,
                          c,
                          TextUtil.debugString(redobuff.toString()));
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // docInsert

    @Override
    public void docInsert(int pos, String s) {
      if(G.dbgRedo.getBoolean()) debugDocInsert(pos, s);
      if(doingBackspace() || notTracking())
        return;
      if(removeDocBeforeInsstart != null && s != null
              && s.startsWith(removeDocBeforeInsstart)) {
        if(G.dbgRedo.getBoolean()) debugDocInsertMATCH_BEFORE();
        int len = removeDocBeforeInsstart.length();
        pos += len;
        s = s.substring(len);
      }
      removeDocBeforeInsstart = null;

      if(expectChar) {
        if(pos == redoTrackPosition) {
          if(G.dbgRedo.getBoolean())debugDocInsertMATCH_EXPECTED(pos, s);
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
          if(G.dbgRedo.getBoolean()) debugDocInsertMATCH_EXTRA(pos, s);
          //
          // Add the mystery string to the redo buffer
          //
          redobuff.append(s);
          redoTrackPosition += s.length();
        } else {
          if(G.dbgRedo.getBoolean()) debugDocInsertNO_MATCH();

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
    private boolean didCharWarpedRight(int pos, String s) {
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
        if(G.dbgRedo.getBoolean()) debugDocInsertWARP(warpDistance);
        return true;
      }
      return false;
    }
    private boolean
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
        if(G.dbgRedo.getBoolean()) debugDocInsertMATCH_REMOVE_EXTRA(pos, t);
        return true;
      }
      return false;
    }
    private void debugDocInsert(int pos, String s) {
      System.err.format("docInsert: pos %d, %d, '%s'\n",
                        pos, s.length(), TextUtil.debugString(s));
    }
    private void debugDocInsertMATCH_BEFORE() {
      if(G.dbgRedo.getBoolean())
        System.err.format("docInsert MATCH BEFORE '%s'\n",
                          TextUtil.debugString(removeDocBeforeInsstart));
    }
    private void debugDocInsertMATCH_EXPECTED(int pos, String s) {
      if(G.dbgRedo.getBoolean()) {
        System.err.println("docInsert MATCH expected " + pos
                           + (s.length() > 1 ? " LENGTH: " + s.length() : ""));
      }
    }
    private void debugDocInsertERROR(int pos, String s) {
      // NOTE: error condition, not based on dbgRedo
      System.err.format("docInsert ERROR: pos %d, redoPosition %d, '%s'\n",
                        pos, redoTrackPosition, s);
    }
    private void debugDocInsertLONG(int pos, String s) {
      // NOTE: error condition, not based on dbgRedo
      System.err.format("docInsert LONG: %d, length %d\n",
                        pos, s.length());
    }
    private void debugDocInsertMATCH_EXTRA(int pos, String s) {
      if(G.dbgRedo.getBoolean())
        System.err.format("docInsert MATCH EXTRA: %d '%s'\n",
                          pos, TextUtil.debugString(s));
    }
    private void debugDocInsertNO_MATCH() {
      if(G.dbgRedo.getBoolean())
        System.err.format("docInsert: NO MATCH redoPosition %d, redobuff %s[%d]"
                          + " afterBuff %s[%d]\n",
                          redoTrackPosition,
                          redobuff, redobuff.length(),
                          afterBuff, afterBuff.length());
    }
    private void debugDocInsertWARP(int warpDistance) {
      System.err.format("docInsert: WARP %d, redobuff %s[%d]"
                        + " afterBuff %s[%d]\n",
                        warpDistance,
                        redobuff, redobuff.length(),
                        afterBuff, afterBuff.length());
    }
    private void debugDocInsertMATCH_REMOVE_EXTRA(int pos, String t) {
      System.err.format("docInsert MATCH REMOVE/EXTRA:"
                         + " %d, '%s' / '%s'\n",
                         pos,
                         TextUtil.debugString(removeDocAfterString),
                         TextUtil.debugString(t));
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // docRemove

    @Override
    public void docRemove(int pos, int len, String removedText) {
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
        if(G.dbgRedo.getBoolean()) debugDocRemoveMATCH(pos, len);
        docRemoveExtendedWithRedobuf(len);
        redoTrackPosition -= len;
      } else if(didDocRemoveAfterTrackPosition(pos, len, removedText)) {
      } else {
        if(G.dbgRedo.getBoolean()) debugDocRemoveNO_MATCH();
      }
      expectChar = false;
    }
    private int
    didDocRemoveBeforeInsertionStart(int pos, int len, String removedText) {
      int insstartOffset = Edit.getInsstartOriginalOffset();
      removeDocBeforeInsstart = null;
      int nChar = insstartOffset - pos;
      if(nChar > 0) {
        if(removedText != null && removedText.length() >= nChar) {
          // Do not even try to remove text before innstart
          removeDocBeforeInsstart = removedText.substring(0, nChar);
          if(G.dbgRedo.getBoolean())debugDocRemoveBEFORE_INSSTART();
        } else {
          nChar = 0;
          if(G.dbgRedo.getBoolean())debugDocRemoveBEFORE_INSSTART_NO_MATCH();
        }
      }
      return nChar;
    }
    private void
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
    private boolean
    didDocRemoveAfterTrackPosition(int pos, int len, String removedText) {
      if(pos + len > redoTrackPosition) {  // goes past insertion point
        int nBefore = redoTrackPosition - pos;
        int insstartOffset = Edit.getInsstartOriginalOffset();
        int nInsertedBefore = redoTrackPosition - insstartOffset;
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

            if(G.dbgRedo.getBoolean()) debugDocRemoveREMOVE_AFTER(nBefore);
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

            if(!skip) {
              if(ViManager.isDebugAtHome()) {
                System.err.println("redo track: remove after: " + ex.getMessage());
              }
              if(G.dbgRedo.getBoolean())
                ViManager.dumpStack("redo track: remove after: " + ex.getMessage());
            }
          }
        }
      }
      return false;
    }
    private void debugDocRemove(int pos, int len, String removedText) {
      if(G.dbgRedo.getBoolean()) {
        int insstart = Edit.getInsstart() != null
                ? Edit.getInsstart().getOffset() : -1;
        System.err.format("docRemove: pos %d, %d, '%s', track %d, insstart %d\n",
                          pos, len, TextUtil.debugString(removedText),
                          redoTrackPosition, insstart);
      }
    }
    private void debugDocRemoveBEFORE_INSSTART() {
      if(G.dbgRedo.getBoolean()) System.err.format("docRemove BEFORE INSSTART: '%s'\n",
                          TextUtil.debugString(removeDocBeforeInsstart));
    }
    private void debugDocRemoveBEFORE_INSSTART_NO_MATCH() {
      if(G.dbgRedo.getBoolean())
        System.err.format("docRemove BEFORE INSSTART NO MATCH\n");
    }
    private void debugDocRemoveMATCH(int pos, int len) {
      if(G.dbgRedo.getBoolean())
        System.err.format(
                  "docRemove MATCH%s: redoPosition %d --> %d, length %d\n",
                  (expectChar ? "-expect" : ""),
                  redoTrackPosition, pos, len);
    }
    private void debugDocRemoveREMOVE_AFTER(int nBefore) {
      if(G.dbgRedo.getBoolean())
        System.err.println("docRemove REMOVE AFTER,"
                           + " nBefore: " + nBefore
                           + " AfterString: '" + removeDocAfterString + "'");
    }
    private void debugDocRemoveNO_MATCH() {
      if(G.dbgRedo.getBoolean())
        System.err.println("docRemove NO MATCH");
    }

    private boolean appendAfterBufToRedoBuff(int len)
    {
      // OPTIMIZE: get rid of loop
      if(afterBuff.length() < len)
        return false;
      while(len-- > 0)
        redobuff.append(afterBuff.removeFirst());
      return true;
    }

    private boolean prependRedoBuffToAfterBuf(int len)
    {
      // OPTIMIZE: get rid of loop
      if(redobuff.length() < len)
        return false;
      while(len-- > 0)
        afterBuff.addFirst(redobuff.removeLast());
      return true;
    }

    private boolean doingBackspace() {
      if(Edit.doingBackspace) {
        if(G.dbgRedo.getBoolean()) System.err.println("doing BACKSPACE");
        return true;
      }
      return false;
    }

    private void debug(String s)
    {
        if(G.dbgRedo.getBoolean())
          System.err.println(s);
    }

    private void nop() {}
}
