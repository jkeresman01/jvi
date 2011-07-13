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

package com.raelity.jvi.swing.simple;

import com.raelity.jvi.core.TextView;
import com.raelity.jvi.ViBadLocationException;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Edit;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Misc;
import com.raelity.jvi.core.Util;
import com.raelity.jvi.manager.Scheduler;
import com.raelity.jvi.swing.SwingBuffer;
import com.raelity.jvi.swing.UndoGroupManager;
import java.util.logging.Level;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import static com.raelity.jvi.core.lib.Constants.*;

/**
 * Add undo group handling to basic swing buffer.
 *
 * @author Ernie Rael <err at raelity.com>
 */
abstract public class SimpleBuffer extends SwingBuffer
{
    private UndoManager undoMan;

    public SimpleBuffer(ViTextView tv)
    {
        super(tv);

        undoMan = new UndoGroupManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent ue)
            {
                UndoableEdit edit = ue.getEdit();
                // FOR USE WITH StyledDocument, see SimpleTextView
                // only accepts insert/remove
                // quietly throw away CHANGE events
                if(edit instanceof DocumentEvent
                    &&((DocumentEvent)edit).getType()
                            == DocumentEvent.EventType.CHANGE)
                    return;

                super.undoableEditHappened(ue);
            }
        };

        getDocument().addUndoableEditListener(undoMan);
    }

    @Override
    public void removeShare()
    {
        if (singleShare()) {
            // last share for the doc
            getDocument().removeUndoableEditListener(undoMan);
        }
        super.removeShare();
    }

    @Override
    public void undo() {
        if(undoMan.canUndo()) {
            doUndoRedo(true);
        } else
            Util.beep_flush();
    }

    @Override
    public void redo() {
        if(undoMan.canRedo()) {
            doUndoRedo(false);
        } else
            Util.beep_flush();
    }

    private void doUndoRedo(boolean isUndo)
    {
        isInUndoRedoCommand = true;
        try {
            URData stuff = beforeUndoRedo(false);
            if(isUndo)
                undoMan.undo();
            else
                undoMan.redo();
            afterUndoRedo(stuff);
        } finally {
            isInUndoRedoCommand = false;
            undoRemovedText = null;
        }
    }

    private boolean isInUndoRedoCommand;
    private String undoRemovedText;

    @Override
    protected void filterRemove(int offset, int length)
    {
        if(isInUndoRedoCommand) {
            try {
                undoRemovedText = getDocument().getText(offset, length);
            } catch(BadLocationException ex) {
            }
        }
    }

    @Override
    protected String getRemovedText(DocumentEvent _e)
    {
        String s = null;
        //
        // With reflection, could get into the
        // Vector of edits and then into
        // GapContent$RemoveUndo which has the deleted text
        //
        ////////////////////////////////////////////////////
        // BUT, use a document filter to grab the removed
        // text, then can check the text
        //

        ///// if(_e instanceof DefaultDocumentEvent) {
        /////     DefaultDocumentEvent e = (DefaultDocumentEvent)_e;
        /////     System.err.println(""+e);
        ///// }
        ///// ElementChange change
        /////         = _e.getChange(_e.getDocument().getDefaultRootElement());

        return s;
    }



    private static class URData {
        boolean isUndo;
        int nLine;

        public URData(boolean isUndo)
        {
            this.isUndo = isUndo;
        }

    }

    private URData beforeUndoRedo(boolean isUndo)
    {
        createDocChangeInfo();
        URData stuff = new URData(isUndo);
        stuff.nLine = getLineCount();
        return stuff;
    }


    //
    // Here are some observed behavior of vim,
    // without digging into implementation
    //
    // LINEMODE OPERATIONS
    // === after removing some lines
    //
    //     undo & more-lines
    //             1st line of new lines (first non-blank char)
    //             (jvi - same)
    //
    //     redo & less-lines
    //             1st line after removed lines,
    //                 same column as 1st non-blank column of removed text
    //             (jvi - correct line, but jvi is going to first non-blank)
    //
    // === after adding some lines with put
    //
    //     undo & less-lines
    //             1st line before removed lines
    //             (** jvi - after removed lines)
    //
    //     redo & more-lines
    //             1st line before new lines
    //             (** jvi - on 1st line of new lines)
    //
    // CHARACTER OPERATION
    // === removing stuff
    //     at the beginning of the sequence of chars
    // === adding stuff
    //     looks like the cursor position is remembered
    //

    private void afterUndoRedo(URData stuff)
    {
        DocChangeInfo info = getDocChangeInfo();
        if(info.isChange) {
            try {
                int nLine = stuff.nLine;

                TextView tv = (TextView)Scheduler.getCurrentTextView();
                if(info.offset != tv.w_cursor.getOffset()) {
                    tv.w_cursor.set(info.offset);
                }
                if(G.dbgUndo().getBoolean()) {
                    String text = "";
                    try {
                        if(info.isInsert)
                            text = getDocument().getText(info.offset, info.length);
                    } catch(BadLocationException ex) {
                    }
                    G.dbgUndo().printf(Level.FINEST,
                      "afterUR: after  off=%d, col=%d\n"
                    + "         change off=%d, len=%d, insert=%b\n"
                    + "         text='%s'\n",
                            tv.w_cursor.getOffset(), tv.w_cursor.getColumn(),
                            info.offset, info.length, info.isInsert,
                            text
                            );
                }
                // Only adjust the cursor if undo/redo left the cursor on col 0;
                // not entirely correct, but...
                if(tv.w_cursor.getColumn() == 0) {
                    //NEEDSWORK could try following the rules outlined above
                    if(nLine != getLineCount())
                        Edit.beginline(BL_WHITE);
                    else if ("\n".equals(getText(tv.getCaretPosition(), 1))) {
                        Misc.check_cursor_col();
                    }
                }
            } catch (ViBadLocationException ex) { }
        }
    }

    @Override
    protected void beginAnyUndo() {
        sendUndoableEdit(UndoGroupManager.BEGIN_COMMIT_GROUP);
    }

    @Override
    protected void endAnyUndo()
    {
        sendUndoableEdit(UndoGroupManager.END_COMMIT_GROUP);
    }

    void sendUndoableEdit(UndoableEdit ue) {
        Document d = getDocument();
        if(d instanceof AbstractDocument) {
            UndoableEditListener[] uels = ((AbstractDocument)d).getUndoableEditListeners();
            UndoableEditEvent ev = new UndoableEditEvent(d, ue);
            for(UndoableEditListener uel : uels) {
                uel.undoableEditHappened(ev);
            }
        }
    }
}
