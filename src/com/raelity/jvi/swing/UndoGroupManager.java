package com.raelity.jvi.swing;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * <code>UndoGroupManager</code> is an <code>UndoManager</code>
 * that allows explicit control of how
 * <code>UndoableEdit</code>s are coalesced into compound edits,
 * rather than using the rules defined by the edits themselves.
 * Other than the default usage, special handling is initiated by doing
 * <code>beginUndoGroup()</code>.
 * <p>
 * Three use cases are supported.
 * </p>
 * <ol>
 * <li> Default behavior is defined by {@link javax.swing.undo.UndoManager}.</li>
 * <li> <code>UnddoableEdit</code>s issued between <code>beginUndoGroup()</code>
 * and <code>endUndoGroup()</code> are placed into a single <code>CompoundEdit</code>.
 * Thus <code>undo()</code> and <code>redo()</code> treat them atomically.</li>
 * <li> Use <code>commitUndoGroup()</code> to place any accumulated
 * <code>UndoableEdit</code>s into a <code>CompoundEdit</code>;
 * an application may use this at strategic points, such as EndOfLine
 * entry or cursor movement.</li>
 * </ol>
 * Note that certain methods, such as <code>undo()</code>, automatically issue
 * <code>commitUndoGroup()</code>.
 */
public class UndoGroupManager extends UndoManager
{

    /** signals that edits should be accumulated */
    boolean buildUndoGroup;
    /** accumulate edits here in undoGroup */
    CompoundEdit undoGroup;

    public synchronized void beginUndoGroup()
    {
        commitUndoGroup();
        buildUndoGroup = true;
    }

    public synchronized void endUndoGroup()
    {
        buildUndoGroup = false;
        commitUndoGroup();
    }

    public synchronized void commitUndoGroup()
    {
        if (undoGroup == null) {
            return;
        } // so buildUndoGroup must be false
        boolean saveInUndoGroup = buildUndoGroup;
        buildUndoGroup = false;
        undoGroup.end();
        super.addEdit(undoGroup);
        undoGroup = null;
        buildUndoGroup = saveInUndoGroup;
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit)
    {
        if (!isInProgress()) {
            return false;
        }
        if (buildUndoGroup) {
            if (undoGroup == null) {
                undoGroup = new CompoundEdit();
            }
            return undoGroup.addEdit(anEdit);
        } else {
            return super.addEdit(anEdit);
        }
    }

    @Override
    public synchronized void discardAllEdits()
    {
        commitUndoGroup();
        super.discardAllEdits();
    }

    //
    // TODO: limits
    //
    @Override
    protected UndoableEdit editToBeUndone()
    {
        if (undoGroup != null) {
            return null;
        }
        return super.editToBeUndone();
    }

    @Override
    protected UndoableEdit editToBeRedone()
    {
        if (undoGroup != null) {
            return null;
        }
        return super.editToBeRedone();
    }

    @Override
    protected void undoTo(UndoableEdit edit)
    {
        if (undoGroup != null) {
            throw new CannotUndoException();
        }
        super.undoTo(edit);
    }

    @Override
    protected void redoTo(UndoableEdit edit)
    {
        if (undoGroup != null) {
            throw new CannotRedoException();
        }
        super.redoTo(edit);
    }

    @Override
    public synchronized void undoOrRedo()
    {
        commitUndoGroup();
        super.undoOrRedo();
    }

    @Override
    public synchronized boolean canUndoOrRedo()
    {
        if (undoGroup != null) {
            return true;
        }
        return super.canUndoOrRedo();
    }

    @Override
    public synchronized void undo()
    {
        commitUndoGroup();
        super.undo();
    }

    @Override
    public synchronized boolean canUndo()
    {
        if (undoGroup != null) {
            return true;
        }
        return super.canUndo();
    }

    @Override
    public synchronized void redo()
    {
        if (undoGroup != null) {
            throw new CannotRedoException();
        }
        super.redo();
    }

    @Override
    public synchronized boolean canRedo()
    {
        if (undoGroup != null) {
            return false;
        }
        return super.canRedo();
    }

    @Override
    public synchronized void end()
    {
        commitUndoGroup();
        super.end();
    }

    @Override
    public synchronized String getUndoOrRedoPresentationName()
    {
        if (undoGroup != null) {
            return undoGroup.getUndoPresentationName();
        }
        return super.getUndoOrRedoPresentationName();
    }

    @Override
    public synchronized String getUndoPresentationName()
    {
        if (undoGroup != null) {
            return undoGroup.getUndoPresentationName();
        }
        return super.getUndoPresentationName();
    }

    @Override
    public synchronized String getRedoPresentationName()
    {
        if (undoGroup != null) {
            return undoGroup.getRedoPresentationName();
        }
        return super.getRedoPresentationName();
    }

    @Override
    public boolean isSignificant()
    {
        if (undoGroup != null && undoGroup.isSignificant()) {
            return true;
        }
        return super.isSignificant();
    }

    @Override
    public void die()
    {
        commitUndoGroup();
        super.die();
    }

    @Override
    public String getPresentationName()
    {
        if (undoGroup != null) {
            return undoGroup.getPresentationName();
        }
        return super.getPresentationName();
    }
}
