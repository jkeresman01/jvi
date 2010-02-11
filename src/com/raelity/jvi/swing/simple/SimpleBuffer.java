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

import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Util;
import com.raelity.jvi.swing.SwingBuffer;
import com.raelity.jvi.swing.UndoGroupManager;
import javax.swing.event.DocumentEvent;
import javax.swing.undo.UndoableEdit;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
abstract public class SimpleBuffer extends SwingBuffer
{
    private UndoGroupManager undoMan;

    public SimpleBuffer(ViTextView tv)
    {
        super(tv);

        undoMan = new UndoGroupManager() {
            @Override
            public synchronized boolean addEdit(UndoableEdit anEdit) {
                // FOR USE WITH StyledDocument, see SimpleTextView
                // only accepts insert/remove
                // quietly throw away CHANGE events
                if(anEdit instanceof DocumentEvent
                    &&((DocumentEvent)anEdit).getType()
                            == DocumentEvent.EventType.CHANGE)
                        return isInProgress();
                return super.addEdit(anEdit);
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

    public void undo() {
        if(undoMan.canUndo())
            undoMan.undo();
        else
            Util.vim_beep();
    }

    public void redo() {
        if(undoMan.canRedo())
            undoMan.redo();
        else
            Util.vim_beep();
    }

    public void do_beginUndo() {
        // NEEDSWORK: standalone like: ((AbstractDocument)doc).writeLock();
        do_beginInsertUndo();
    }

    public void do_endUndo() {
        do_endInsertUndo();
        // NEEDSWORK: standalone like: ((AbstractDocument)doc).writeUnlock();
    }

    public void do_beginInsertUndo() {
        undoMan.beginUndoGroup();
    }

    public void do_endInsertUndo() {
        undoMan.endUndoGroup();
    }
}
