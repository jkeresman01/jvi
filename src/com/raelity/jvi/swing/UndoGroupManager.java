package com.raelity.jvi.swing;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * <tt>UndoGroupManager</tt> extends {@link UndoManager}
 * and allows explicit control of what
 * <tt>UndoableEdit</tt>s are coalesced into compound edits,
 * rather than using the rules defined by the edits themselves.
 * Groups are defined with {@link BEGIN_COMIT_GROUP} and {@link END_COMIT_GROUP}.
 * Send these to UndoableEditListener. These must always be paired.
 * Undo or Redo while coalescing edits delimit edits, there is an implicit
 * END/BEGIN.
 * <p>
 * These use cases are supported.
 * </p>
 * <ol>
 * <li> Default behavior is defined by {@link UndoManager}.</li>
 * <li> <tt>UnddoableEdit</tt>s issued between BEGIN_COMIT_GROUP
 * and END_COMIT_GROUP are placed into a single
 * {@link CompoundEdit}.
 * Thus <tt>undo()</tt> and <tt>redo()</tt> treat them 
 * as a single undo/redo.</li>
 * <li>BEGIN/END nest.</li>
 * </ol>
 * @see UndoManager
 */
public class UndoGroupManager extends UndoManager {
    private static final Logger LOG = Logger.getLogger(UndoGroupManager.class.getName());
    /** signals that edits are being accumulated */
    private int buildUndoGroup;
    /** accumulate edits here in undoGroup */
    private CompoundEdit undoGroup;

    /**
     * Start a group of edits which will be committed as a single edit
     * for purpose of undo/redo.
     * Nesting semantics are that any BEGIN_COMIT_GROUP and
     * END_COMIT_GROUP delimits a comit-group.
     * While coalescing edits, any undo/redo/save implicitly delimits
     * a comit-group.
     */
    public static final UndoableEdit BEGIN_COMIT_GROUP = new ComitGroupEdit();
    /** End a group of edits. */
    public static final UndoableEdit END_COMIT_GROUP = new ComitGroupEdit();

    /** SeparateEdit tags an UndoableEdit so the
     * UndoGroupManager does not coalesce it.
     */
    public interface SeparateEdit {
    }

    private static class ComitGroupEdit extends AbstractUndoableEdit {
        @Override
        public boolean isSignificant() {
            return false;
        }
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent ue)
    {
        if(ue.getEdit() == BEGIN_COMIT_GROUP) {
            beginUndoGroup();
        } else if(ue.getEdit() == END_COMIT_GROUP) {
            endUndoGroup();
        } else {
            super.undoableEditHappened(ue);
        }
    }

    /**
     * Direct this <tt>UndoGroupManager</tt> to begin coalescing any
     * <tt>UndoableEdit</tt>s that are added into a <tt>CompoundEdit</tt>.
     * <p>If edits are already being coalesced and some have been 
     * accumulated, they are committed as an atomic group and a new
     * group is started.
     * @see #addEdit
     * @see #endUndoGroup
     */
    private synchronized void beginUndoGroup() {
        commitUndoGroup();
        LOG.log(Level.FINE, "beginUndoGroup: nesting {0}", buildUndoGroup);
        buildUndoGroup++;
    }

    /**
     * Direct this <tt>UndoGroupManager</tt> to stop coalescing edits.
     * Until <tt>beginUndoGroupManager</tt> is invoked,
     * any received <tt>UndoableEdit</tt>s are added singly.
     * <p>
     * This has no effect if edits are not being coalesced, for example
     * if <tt>beginUndoGroup</tt> has not been called.
     */
    private synchronized void endUndoGroup() {
        buildUndoGroup--;
        LOG.log(Level.FINE, "endUndoGroup: nesting {0}", buildUndoGroup);
        if(buildUndoGroup < 0) {
            LOG.log(Level.WARNING, null,
                    new Exception("endUndoGroup without beginUndoGroup"));
            buildUndoGroup = 0;
        }
        // slam buildUndoGroup to 0 to disable nesting
        commitUndoGroup();
    }

    /**
     * Commit any accumulated <tt>UndoableEdit</tt>s as an atomic
     * <tt>undo</tt>/<tt>redo</tt> group. {@link CompoundEdit#end}
     * is invoked on the <tt>CompoundEdit</tt> and it is added as a single
     * <tt>UndoableEdit</tt> to this <tt>UndoManager</tt>.
     * <p>
     * If edits are currently being coalesced, a new undo group is started.
     * This has no effect if edits are not being coalesced, for example
     * <tt>beginUndoGroup</tt> has not been called.
     */
    private synchronized void commitUndoGroup() {
        if(undoGroup == null) {
            return;
        }
        // super.addEdit may end up in this.addEdit,
        // so buildUndoGroup must be false
        int saveBuildUndoGroup = buildUndoGroup;
        buildUndoGroup = 0;

        undoGroup.end();
        super.addEdit(undoGroup);
        undoGroup = null;

        buildUndoGroup = saveBuildUndoGroup;
    }

    /** Add this edit separately, not part of a group.
     * @return super.addEdit
     */
    private boolean commitAddEdit(UndoableEdit anEdit) {
        commitUndoGroup();

        int saveBuildUndoGroup = buildUndoGroup;
        buildUndoGroup = 0;
        boolean f = super.addEdit(anEdit);
        //boolean f = addEdit(undoGroup);
        buildUndoGroup = saveBuildUndoGroup;
        return f;
    }

    /**
     * If this <tt>UndoManager</tt> is coalescing edits then add
     * <tt>anEdit</tt> to the accumulating <tt>CompoundEdit</tt>.
     * Otherwise, add it to this UndoManager. In either case the
     * edit is saved for later <tt>undo</tt> or <tt>redo</tt>.
     * @return {@inheritDoc}
     * @see #BEGIN_COMIT_GROUP
     * @see #END_COMIT_GROUP
     */
    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        if(!isInProgress())
            return false;

        if(buildUndoGroup > 0) {
            if(anEdit instanceof SeparateEdit)
                return commitAddEdit(anEdit);
            if(undoGroup == null)
                undoGroup = new CompoundEdit();
            return undoGroup.addEdit(anEdit);
        } else {
            return super.addEdit(anEdit);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void discardAllEdits() {
        commitUndoGroup();
        super.discardAllEdits();
    }

    //
    // TODO: limits
    //

    /** {@inheritDoc} */
    @Override
    public synchronized void undoOrRedo() {
        commitUndoGroup();
        super.undoOrRedo();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean canUndoOrRedo() {
        if(undoGroup != null)
            return true;
        return super.canUndoOrRedo();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void undo() {
        commitUndoGroup();
        super.undo();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean canUndo() {
        if(undoGroup != null)
            return true;
        return super.canUndo();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void redo() {
        if(undoGroup != null)
            throw new CannotRedoException();
        super.redo();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean canRedo() {
        if(undoGroup != null)
            return false;
        return super.canRedo();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void end() {
        commitUndoGroup();
        super.end();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized String getUndoOrRedoPresentationName() {
        if(undoGroup != null)
            return undoGroup.getUndoPresentationName();
        return super.getUndoOrRedoPresentationName();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized String getUndoPresentationName() {
        if(undoGroup != null)
            return undoGroup.getUndoPresentationName();
        return super.getUndoPresentationName();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized String getRedoPresentationName() {
        if(undoGroup != null)
            return undoGroup.getRedoPresentationName();
        return super.getRedoPresentationName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSignificant() {
        if(undoGroup != null && undoGroup.isSignificant()) {
            return true;
        }
        return super.isSignificant();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void die() {
        commitUndoGroup();
        super.die();
    }

    /** {@inheritDoc} */
    @Override
    public String getPresentationName() {
        if(undoGroup != null)
            return undoGroup.getPresentationName();
        return super.getPresentationName();
    }

    // The protected methods are only accessed from
    // synchronized methods that do commitUndoGroup
    // so they do not need to be overridden in this class
}
