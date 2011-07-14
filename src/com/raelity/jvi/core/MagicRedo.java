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

import com.raelity.jvi.ViBadLocationException;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViBuffer.BIAS;
import com.raelity.jvi.core.lib.BufferQueue;
import com.raelity.jvi.manager.ViManager;
import com.raelity.text.TextUtil;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * See MagicRedoOriginal for introductory comments and discussion of issues.
 * This approach sets guards around the original insertion point. It returns
 * the data between the guards from editComplete (assuming certain criteria
 * are met). This approach has problems with multi-line insertions because
 * the auto indent get included on the 2nd... lines.
 *
 * Note that when editComplete returns null, the normal redobuff should
 * be used by the caller. One of the issues with this algorithm is when
 * to return null. If there was no code completion, then returning null is
 * the best thing to do, keeping the captured keystrokes in the redo buf.
 *
 * During the edit, if State.s_abort is entered, then a null will be returned.
 * A null is also returned if there was no "magic" edits. This is detected
 * by receiving a docInsert/Remove event without there being a user keystroke.
 *
 * This algorithm could probably operate one line at a time to avoid the
 * indent issue. Some considerations: when moving to a new line provide
 * info for that line and only move the pre guard;
 *
 * @author Ernie Rael <err at raelity.com>
 */
class MagicRedo implements GetChar.ViMagicRedo
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

    private static final Logger LOG = Logger.getLogger(MagicRedo.class.getName());

    private int redoTrackPosition = -1;
    private boolean expectChar;
    private boolean disableTrackingOneEdit;
    private boolean localDisable;
    // afterBuff, characters added after the current insert position
    private BufferQueue afterBuff = new BufferQueue();

    // See docRemove() method for details of the removeDocXxx variables
    // The variable remvoeDocAfterString also acts as a flag. It is non-null
    // very briefly, only for one operation, insert or remove.
    private String removeDocAfterString; // check when non-null
    private String removeDocBeforeInsstart;

    // redobuff is the master redobuff, MagicRedo pokes it in various ways
    private final BufferQueue redobuff;



    private enum State {
        s_off,          // waiting for a start event
        s_abort,        // waiting for start, no magic available last input
        s_wait_typed,   // collecting info
        s_wait_doc,
    }

    private enum Event {
        e_begin,
        e_abort,
        e_end,
        e_gotc,
        e_backspace,
        e_doc_insert,
        e_doc_remove,
    }

    private State state = State.s_off;
    private Event currentEvent;
    private boolean didMagic; // more than just user typing happened
    private DocString preString;
    private DocString postString;
    private int trackOffset = -1;

    ViMark initPos;
    ViMark endPos;

    private MagicRedo INSTANCE;


    /**
     * regular user typing flips between s_wait_typed and s_wait_doc.
     * @param e
     * @param params
     * @param tag
     */
    private void stateMachine(Event e, Params params, String tag)
    {
        currentEvent = e;

        State newState = null;

        // some events operate the same no matter what
        switch(e) {
            case e_begin:
            case e_abort:
            case e_end:
                newState = defaultProcessEvent(e, params);
                break;
        }
        if(state == State.s_abort && e != Event.e_begin)
            newState = State.s_abort; // there's only one way out of abort

        if(newState == null) {
            switch(state) {
                    // for off,abort only e_begin matters, handled above
                case s_off:     break;
                case s_abort:   break;

                case s_wait_typed:
                    switch(e) {
                        case e_gotc:
                        case e_backspace:
                            newState = defaultProcessEvent(e, params);
                            if(newState == null)
                                newState = State.s_wait_doc;
                            break;
                        case e_doc_insert:
                        case e_doc_remove:
                            newState = defaultProcessEvent(e, params);
                            LOG.warning("MagicRedo: UNEXPECTED DOC EVENT");
                            if(newState == null) {
                                didMagic = true;
                                newState = State.s_wait_typed;
                            }
                            break;

                        default:
                            break;
                    }
                    break;

                case s_wait_doc:
                    switch(e) {
                        case e_gotc:
                        case e_backspace:
                            newState = defaultProcessEvent(e, params);
                            // two keys in a row without a doc event,
                            // how can that happen?
                            LOG.warning("MagicRedo: TWO CHARS IN A ROW");
                            if(newState == null)
                                newState = State.s_abort;
                            break;
                        case e_doc_insert:
                        case e_doc_remove:
                            newState = defaultProcessEvent(e, params);
                            if(newState == null)
                                newState = State.s_wait_typed;
                            break;

                        default:
                            break;
                    }
                    break;
                default:
                    throw new AssertionError();
            }
        }

        if(G.dbgRedo.getBoolean()) {
          StringBuilder sb = new StringBuilder("MagicRedo stateMachine: " );
          sb.append(e.name()).append(": ").append(state.name());

          if(newState != null) {
              if(newState != state)
                  sb.append(" --> ").append(newState.name());
              state = newState;
          }
          if(tag != null)
              sb.append(" [").append(tag).append("]");
          G.dbgRedo.println(sb.toString());
        }

        params = null;
    }

    private State defaultProcessEvent(Event e, Params params)
    {
        State newState = null;
        switch(e) {
            case e_begin:
                newState = processBegin(params);
                break;

            case e_abort:
                newState = State.s_abort;
                break;

            case e_end:
                newState = State.s_off;
                break;

            case e_gotc:
                newState = processCharTyped(params);
                break;

            case e_backspace:
                newState = processBackspace(params);
                break;

            case e_doc_insert:
                newState = processDocInsert(params);
                break;

            case e_doc_remove:
                newState = processDocRemove(params);
                break;

            default:
                throw new AssertionError();
        }
        return newState;
    }

    MagicRedo(BufferQueue redobuff)
    {
        if(G.dbgRedo.getBoolean())
            G.dbgRedo.printf("CONSTRUCT redo: MagicRedo (guard)\n");
        this.redobuff = redobuff;
    }

    @Override
    public void initRedoTrackingPosition()
    {
        stateMachine(Event.e_begin, null, "initRedoTrackingPosition");
        initRedoTrackingPositionXXX();
    }

    private State processBegin(Params params)
    {
        trackOffset = G.curwin.getCaretPosition();

        //initPos = G.curbuf.createMark(G.curwin.w_cursor.getOffset(), BIAS.BACK);
        ViFPOS fpos = G.curbuf.createFPOS(G.curwin.w_cursor.getOffset());
        initPos = G.curbuf.createMark(fpos);
        endPos = G.curbuf.createMark(G.curwin.w_cursor.getOffset(), BIAS.FORW);
        didMagic = false;

        preString = null;
        postString = null;
        try {
            String s = null;

            // capture chars before the initial pos
            int off = initPos.getOffset() - 50;
            if(off <= 0)
                off = 1;
            s = G.curbuf.getText(off, initPos.getOffset() - off);
            // only take the end of the string, back to the first newline
            int i = s.lastIndexOf('\n') + 1; // +1 to skip the newline
            if(i > 0 && i < s.length())
                s = s.substring(i);
            preString = new DocString(initPos.getOffset() - s.length(), s);

            // capture chars after the initial pos
            s = G.curbuf.getText(initPos.getOffset(), 10);
            off = initPos.getOffset() + 11;
            if(off >= G.curbuf.getLength())
                off = G.curbuf.getLength();
            postString = new DocString(off, s, true);

        } catch(ViBadLocationException ex) {
            Logger.getLogger(MagicRedo.class.getName()).log(Level.SEVERE, null,
                                                            ex);
        }

        if(preString == null || postString == null) {
            return State.s_abort;
        } else {
            return State.s_wait_typed;
        }
    }

    @Override
    public String editComplete()
    {
        stateMachine(Event.e_end, nullParams, "editComplete");
        editCompleteXXX();

        dumpState("COMPLETE");
        dumpFinalState();

        if(haveInsertedChars())
            return getInsertedChars();
        else
            return null;
    }

    private boolean haveInsertedChars()
    {
        if(didMagic && state == State.s_off
                && preString.match() && postString.match())
            return true;
        else
            return false;
    }

    private String getInsertedChars()
    {
        int p1 = preString.getInnerOffset();
        int p2 = postString.getInnerOffset();
        try {
            return G.curbuf.getText(p1, p2 - p1);
        } catch(ViBadLocationException ex) {
            Logger.getLogger(MagicRedo.class.getName()).log(Level.SEVERE, null,
                                                            ex);
        }
        return null;
    }

    @Override
    public void disableRedoTrackingOneEdit()
    {
        stateMachine(Event.e_abort, nullParams, "disableRedoTrackingOneEdit");
        disableRedoTrackingOneEditXXX();
    }

    @Override
    public void charTyped(char c)
    {
        Params params = new Params(c);

        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        trackOffset = G.curwin.getCaretPosition();

        stateMachine(Event.e_gotc, params, "charTyped");
    }
    private State processCharTyped(Params params)
    {
        userInputCharXXX(params.typed);
        dumpState("charTyped '" + String.valueOf(params.typed) + "'");
        return null;
    }

    @Override
    public void markRedoBackspace()
    {
        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        trackOffset = G.curwin.getCaretPosition();

        stateMachine(Event.e_backspace, nullParams, "markRedoBackspace");
    }
    private State processBackspace(Params params)
    {
        markRedoBackspaceXXX();
        return null;
    }

    @Override
    public void changeInsstart()
    {
        stateMachine(Event.e_abort, nullParams, "changeInsstart");
    }

    @Override
    public void docInsert(int pos, String s)
    {
        Params params = new Params(pos, s);
        stateMachine(Event.e_doc_insert, params, debugDocInsert(params));
    }
    private State processDocInsert(Params params)
    {
        docInsertXXX(params.pos, params.text);
        updateEndPos();
        dumpState("docInsert...");
        return null;
    }
    private String debugDocInsert(Params params) {
        return String.format("pos %d, %d, '%s'",
                params.pos, params.text.length(),
                TextUtil.debugString(params.text));
    }

    @Override
    public void docRemove(int pos, int len, String removedText)
    {
        Params params = new Params(pos, len, removedText);
        stateMachine(Event.e_doc_remove, params, debugDocRemove(params));
    }
    private State processDocRemove(Params params)
    {
        docRemoveXXX(params.pos, params.len, params.text);
        dumpState("docRemove...");
        return null;
    }
    private String debugDocRemove(Params params) {
        return String.format("pos %d, %d, '%s', track %d, initPos %d",
                          params.pos, params.len,
                          TextUtil.debugString(params.text),
                          redoTrackPosition,
                          initPos != null ? initPos.getOffset() : -1);
    }



    void updateEndPos()
    {
        if(redoTrackPosition > endPos.getOffset()) {
            endPos = G.curbuf.createMark(redoTrackPosition + 1, BIAS.FORW);
            LOG.warning("****************udpateEndPos****************");
            ViManager.dumpStack();
        }
    }

    private static class Params {
        char typed;
        int pos;
        int len;
        String text;

        public Params() { }
        public Params(char typed) { this.typed = typed; }
        public Params(int pos, String text)
                { this.pos = pos; this.text = text; }
        public Params(int pos, int len, String text)
                { this.pos = pos; this.len = len; this.text = text; }

    }
    Params nullParams = new Params();

    private static class DocString {
        ViMark pos;
        String text;
        boolean anchorEnd;

        DocString(int pos, String text)
        {
            this.pos = G.curbuf.createMark(pos, BIAS.BACK);
            this.text = text;
        }

        DocString(int pos, String text, boolean anchorEnd)
        {
            this(pos, text);
            this.anchorEnd = anchorEnd;
        }

        boolean match()
        {
            return getCurrent().equals(text);
        }

        /** return the "other" side to cut off the modified string */
        int getInnerOffset()
        {
            int off = pos.getOffset();
            if(anchorEnd)
                off -= text.length() + 1;
            else
                off += text.length();
            return off;
        }

        /** using Mark, grab a string of that size */
        String getCurrent()
        {
            int off = pos.getOffset();
            if(anchorEnd)
                off -= text.length() + 1;
            String s = "OOPS-DONT'T-MATCH-THIS";
            try {
                s = G.curbuf.getText(off, text.length());
            } catch(ViBadLocationException ex) {
                Logger.getLogger(MagicRedo.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
            return s;
        }

        @Override
        public String toString()
        {
            return "DocString{"
                    + "text=" + TextUtil.debugString(text)
                    + ",pos=" + pos.getOffset()
                    + ",anchorEnd=" + anchorEnd + '}'
                    + "\n              '"
                    + TextUtil.debugString(getCurrent()) + "'"
                    + " match: " + match();
        }

    }

    private void dumpState(String tag)
    {
        try {
            ViFPOS p0 = initPos;
            //ViFPOS p1 = G.curwin.w_cursor.copy();
            ViFPOS p1 = endPos;
            int len;
            if(p0 == null || p1 == null)
                ViManager.printf("MagicRedo: %15s [%s,%s]\n", tag, p0, p1);
            else {
                len = p1.getOffset() - p0.getOffset();
                ViManager.printf("MagicRedo: %15s [%s,%s] '%s'\n", tag, p0, p1,
                          len >= 0 ? TextUtil.debugString(G.curbuf.getText(
                                p0.getOffset(), len)) : "<0");
            }

            if(false) {
                p0 = Edit.getInsstart();
                if(p0 == null || p1 == null)
                    ViManager.printf("      %15s [%s,%s]\n", "", p0, p1);
                else {
                    len = p1.getOffset() - p0.getOffset();
                    ViManager.printf("      %15s [%s,%s] '%s'\n", "", p0, p1,
                              len >= 0 ? TextUtil.debugString(G.curbuf.getText(
                                    p0.getOffset(), len)) : "<0");
                }
            }
        } catch(ViBadLocationException ex) {
            Logger.getLogger(MagicRedo.class.getName()).log(Level.SEVERE, null,
                                                            ex);
        }
    }

    private void dumpFinalState()
    {
        ViManager.println("MagicRedo: didMagic: " + didMagic
                + ", State: " + state);
        ViManager.println("MagicRedo: preString:\n" + preString);
        ViManager.println("MagicRedo: postString:\n" + postString);
        String s = getInsertedChars();
        if(haveInsertedChars()) {
            ViManager.println("MagicRedo: inserted: '" + s + "'");
        } else {
            ViManager.println("MagicRedo: inserted: NO MATCH: '" + s + "'");
        }
    }




//////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////

    public void disableRedoTrackingOneEditXXX() {
        disableTrackingOneEdit = true;
    }

    public void editCompleteXXX() {
        disableTrackingOneEdit = false;
    }

    public void initRedoTrackingPositionXXX() {
        redoTrackPosition = G.curwin.getCaretPosition();
        expectChar = false;
        if(G.dbgRedo.getBoolean())
            G.dbgRedo.printf("initRedoTrackingPosition %d '%s'\n",
                              redoTrackPosition,
                              TextUtil.debugString(redobuff.toString()));
    }

    private boolean notTracking() {
       return redoTrackPosition < 0
               || !G.redoTrack
               || disableTrackingOneEdit
               //|| localDisable
       ;
    }

    public void markRedoBackspaceXXX() {
      if(redoTrackPosition >= 0) {
        int prevRedoTrackPosition = redoTrackPosition;
        redoTrackPosition = G.curwin.getCaretPosition();
        if(G.dbgRedo.getBoolean())
            G.dbgRedo.printf("markRedoTrackPositionBackspace %d --> %d '%s'\n",
                            prevRedoTrackPosition,
                            redoTrackPosition,
                            TextUtil.debugString(redobuff.toString()));
      }
      dumpState("Backspace...");
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // userInputChar

    public void userInputCharXXX(char c) {
      if(!G.redoTrack)
        return;
      removeDocAfterString = null;
      if(expectChar)
        LOG.warning("markRedoPosition ERROR: expectChar");

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
        G.dbgRedo.printf("markRedoPosition SKIP[%d]: %s[%d] --> %s[%d]\n",
                          skip,
                          before, before.length(),
                          afterBuff, afterBuff.length());
    }
    // assist for markRedoTrackPosition
    private void
    debugMarkRedoTrackPosition(int prevRedoTrackPosition, char c) {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf("markRedoPosition %d --> %d '%c' '%s'\n",
                          prevRedoTrackPosition,
                          redoTrackPosition,
                          c,
                          TextUtil.debugString(redobuff.toString()));
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // docInsert

    public void docInsertXXX(int pos, String s) {
      debugDocInsertXXX(pos, s);
      if(doingBackspace() || notTracking())
        return;
      if(removeDocBeforeInsstart != null && s != null
              && s.startsWith(removeDocBeforeInsstart)) {
        debugDocInsertMATCH_BEFORE();
        int len = removeDocBeforeInsstart.length();
        pos += len;
        s = s.substring(len);
      }
      removeDocBeforeInsstart = null;

      if(expectChar) {
        if(pos == redoTrackPosition) {
          debugDocInsertMATCH_EXPECTED(pos, s);
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
          debugDocInsertMATCH_EXTRA(pos, s);
          //
          // Add the mystery string to the redo buffer
          //
          redobuff.append(s);
          redoTrackPosition += s.length();
        } else {
          debugDocInsertNO_MATCH();

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
        debugDocInsertWARP(warpDistance);
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
        debugDocInsertMATCH_REMOVE_EXTRA(pos, t);
        return true;
      }
      return false;
    }
    private void debugDocInsertXXX(int pos, String s) {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf("docInsert: pos %d, %d, '%s'\n",
                          pos, s.length(), TextUtil.debugString(s));
    }
    private void debugDocInsertMATCH_BEFORE() {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf("docInsert MATCH BEFORE '%s'\n",
                          TextUtil.debugString(removeDocBeforeInsstart));
    }
    private void debugDocInsertMATCH_EXPECTED(int pos, String s) {
      if(G.dbgRedo.getBoolean()) {
            G.dbgRedo.println("docInsert MATCH expected " + pos
                           + (s.length() > 1 ? " LENGTH: " + s.length() : ""));
      }
    }
    private void debugDocInsertERROR(int pos, String s) {
      LOG.log(Level.WARNING, "docInsert ERROR: pos {0}, redoPosition {1}, '{2}'",
                        new Object[]{pos, redoTrackPosition, s});
    }
    private void debugDocInsertLONG(int pos, String s) {
      LOG.log(Level.WARNING, "docInsert LONG: {0}, length {1}",
                        new Object[]{pos, s.length()});
    }
    private void debugDocInsertMATCH_EXTRA(int pos, String s) {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf("docInsert MATCH EXTRA: %d '%s'\n",
                          pos, TextUtil.debugString(s));
    }
    private void debugDocInsertNO_MATCH() {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf("docInsert: NO MATCH redoPosition %d, redobuff %s[%d]"
                          + " afterBuff %s[%d]\n",
                          redoTrackPosition,
                          redobuff, redobuff.length(),
                          afterBuff, afterBuff.length());
    }
    private void debugDocInsertWARP(int warpDistance) {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf("docInsert: WARP %d, redobuff %s[%d]"
                          + " afterBuff %s[%d]\n",
                          warpDistance,
                          redobuff, redobuff.length(),
                          afterBuff, afterBuff.length());
    }
    private void debugDocInsertMATCH_REMOVE_EXTRA(int pos, String t) {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf("docInsert MATCH REMOVE/EXTRA:"
                           + " %d, '%s' / '%s'\n",
                           pos,
                           TextUtil.debugString(removeDocAfterString),
                           TextUtil.debugString(t));
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // docRemove

    public void docRemoveXXX(int pos, int len, String removedText) {
      removeDocAfterString = null;
      debugDocRemoveXXX(pos, len, removedText);
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
      int insstartOffset = Edit.getInsstart().getOffset();
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
          LOG.log(Level.WARNING, "docRemove ERROR: expectChar len = {0}", len);
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
                LOG.log(Level.WARNING, "redo track: remove after: {0}", ex.getMessage());
              }
              if(G.dbgRedo.getBoolean())
                ViManager.dumpStack("redo track: remove after: " + ex.getMessage());
            }
          }
        }
      }
      return false;
    }
    private void debugDocRemoveXXX(int pos, int len, String removedText) {
      if(G.dbgRedo.getBoolean()) {
        int insstart = Edit.getInsstart() != null
                ? Edit.getInsstart().getOffset() : -1;
            G.dbgRedo.printf("docRemove: pos %d, %d, '%s', track %d, insstart %d\n",
                          pos, len, TextUtil.debugString(removedText),
                          redoTrackPosition, insstart);
      }
    }
    private void debugDocRemoveBEFORE_INSSTART() {
      if(G.dbgRedo.getBoolean()) G.dbgRedo.printf("docRemove BEFORE INSSTART: '%s'\n",
                          TextUtil.debugString(removeDocBeforeInsstart));
    }
    private void debugDocRemoveBEFORE_INSSTART_NO_MATCH() {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf("docRemove BEFORE INSSTART NO MATCH\n");
    }
    private void debugDocRemoveMATCH(int pos, int len) {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.printf(
                  "docRemove MATCH%s: redoPosition %d --> %d, length %d\n",
                  (expectChar ? "-expect" : ""),
                  redoTrackPosition, pos, len);
    }
    private void debugDocRemoveREMOVE_AFTER(int nBefore) {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.println("docRemove REMOVE AFTER,"
                           + " nBefore: " + nBefore
                           + " AfterString: '" + removeDocAfterString + "'");
    }
    private void debugDocRemoveNO_MATCH() {
      if(G.dbgRedo.getBoolean())
        G.dbgRedo.println("docRemove NO MATCH");
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
        if(G.dbgRedo.getBoolean()) G.dbgRedo.println("doing BACKSPACE");
        return true;
      }
      return false;
    }

    private void debug(String s)
    {
        if(G.dbgRedo.getBoolean())
          G.dbgRedo.println(s);
    }

    private void nop() {}
}
