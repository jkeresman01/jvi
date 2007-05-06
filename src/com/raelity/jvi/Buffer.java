/*
 * Buffer.java
 *
 * Created on March 5, 2007, 11:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi;

import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * Buffer: structure that holds information about one file, primarily
 * per file options.
 *
 * Several windows can share a single Buffer.
 *
 * @author erra
 */
public class Buffer implements ViOptionBag {
    private Document doc;
    private boolean didCheckModelines;
    private UndoGroupManager undoMan;
    
    private int share; // the number of text views sharing this buffer
    public int getShare() { return share; }
    public void addShare() { share++; }
    public void removeShare() {
        share--;
        if(share == 0) {
            doc.removeUndoableEditListener(undoMan);
            doc = null;
        }
    }
    public Document getDoc() { return doc; }
    
    /** Creates a new instance of Buffer, initialize values from Options. */
    public Buffer(Document doc) {
        this.doc = doc;
        initOptions();
    }
    
    protected void initOptions() {
        b_p_ts = Options.getOption(Options.tabStop).getInteger();
        b_p_sw = Options.getOption(Options.shiftWidth).getInteger();
        b_p_et = Options.getOption(Options.expandTabs).getBoolean();
    }

    public void viOptionSet(ViTextView tv, String name) {
    }
    
    /** from switchto */
    public void activateOptions(ViTextView tv) {
    }
    
    /** from switchto, everything else has been setup */
    public void checkModeline() {
        if(didCheckModelines)
            return;
        didCheckModelines = true;
        Options.processModelines();
    }

    public void standAlone() {
        undoMan = new UndoGroupManager();
        doc.addUndoableEditListener(undoMan);
    }

    public void beginUndo() {
        undoMan.beginUndoGroup();
    }

    public void endUndo() {
        undoMan.endUndoGroup();
    }

    public void beginInsertUndo() {
        undoMan.beginUndoGroup();
    }

    public void endInsertUndo() {
        undoMan.endUndoGroup();
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
    
    //////////////////////////////////////////////////////////////////////
    //
    // Declare the variables referenced as part of a ViOptionBag
    //
    
    public int b_p_ts;     // tab stop
    public int b_p_sw;     // shiftw width
    public boolean b_p_et;     // expand tabs
    
    //////////////////////////////////////////////////////////////////////
    //
    // Other per buffer variables
    //
    
    /* Save the current VIsual area for '< and '> marks, and "gv" */
    public ViMark b_visual_start;
    public ViMark b_visual_end;
    public int b_visual_mode;
    public String b_p_mps; // used in nv_object

    /** An UndoManager that allows explicit control of how
     * <code>UndoableEdit</code>s are coalesced into compound edits,
     * rather than using the rules defined by the edits themselves.
     * Other than the default usage, special handling is initiated by doing
     * <code>beginUndoGroup</code>.
     * Three use cases are supported.
     * <ol>
     * <li> Default behavior is defined by {@link javax.swing.undo.UndoManager}.</li>
     * <li> <code>UnddoableEdit</code>s issued between <code>beginUndoGroup</code>
     * and <code>endUndoGroup</code> are placed into a single <code>CompoundEdit</code>.
     * Thus <code>undo</code> and <code>redo</code> treat them atomically.</li>
     * <li> Use <code>commitUndoGroup</code> to place any accumulated
     * <code>UndoableEdit</code>s into a <code>CompoundEdit</code>.
     * The application does this at strategic points, such as EndOfLine
     * entry or cursor movement.</li>
     * </ol>
     * Note that certain methods, such as <code>undo</code>, automatically issue
     * <code>commitUndoGroup</code>.
     */
    public static class UndoGroupManager extends UndoManager {
        /** signals that edits should be accumulated */
        boolean buildUndoGroup;
        /** accumulate edits here in undoGroup */
        CompoundEdit undoGroup;

        public synchronized void beginUndoGroup() {
            commitUndoGroup();
            buildUndoGroup = true;
        }

        public synchronized void endUndoGroup() {
            buildUndoGroup = false;
            commitUndoGroup();
        }

        public synchronized void commitUndoGroup() {
            if(undoGroup == null) {
                return;
            }
            // super.addEdit may end up in this.addEdit,
            // so buildUndoGroup must be false
            boolean saveInUndoGroup = buildUndoGroup;
            buildUndoGroup = false;

            undoGroup.end();
            super.addEdit(undoGroup);

            undoGroup = null;
            buildUndoGroup = saveInUndoGroup;
        }


        public synchronized boolean addEdit(UndoableEdit anEdit) {
            if(!isInProgress())
                return false;
            if(buildUndoGroup) {
                if(undoGroup == null)
                    undoGroup = new CompoundEdit();
                return undoGroup.addEdit(anEdit);
            } else {
                return super.addEdit(anEdit);
            }
        }

        public synchronized void discardAllEdits() {
            commitUndoGroup();
            super.discardAllEdits();
        }

        //
        // TODO: limits
        //

        protected UndoableEdit editToBeUndone() {
            if(undoGroup != null)
                return null;
            return super.editToBeUndone();
        }

        protected UndoableEdit editToBeRedone() {
            if(undoGroup != null)
                return null;
            return super.editToBeRedone();
        }

        protected void undoTo(UndoableEdit edit) {
            if(undoGroup != null)
                throw new CannotUndoException();
            super.undoTo(edit);
        }

        protected void redoTo(UndoableEdit edit) {
            if(undoGroup != null)
                throw new CannotRedoException();
            super.redoTo(edit);
        }

        public synchronized void undoOrRedo() {
            commitUndoGroup();
            super.undoOrRedo();
        }

        public synchronized boolean canUndoOrRedo() {
            if(undoGroup != null)
                return true;
            return super.canUndoOrRedo();
        }

        public synchronized void undo() {
            commitUndoGroup();
            super.undo();
        }

        public synchronized boolean canUndo() {
            if(undoGroup != null)
                return true;
            return super.canUndo();
        }

        public synchronized void redo() {
            if(undoGroup != null)
                throw new CannotRedoException();
            super.redo();
        }

        public synchronized boolean canRedo() {
            if(undoGroup != null)
                return false;
            return super.canRedo();
        }

        public synchronized void end() {
            commitUndoGroup();
            super.end();
        }

        public synchronized String getUndoOrRedoPresentationName() {
            if(undoGroup != null)
                return undoGroup.getUndoPresentationName();
            return super.getUndoOrRedoPresentationName();
        }

        public synchronized String getUndoPresentationName() {
            if(undoGroup != null)
                return undoGroup.getUndoPresentationName();
            return super.getUndoPresentationName();
        }

        public synchronized String getRedoPresentationName() {
            if(undoGroup != null)
                return undoGroup.getRedoPresentationName();
            return super.getRedoPresentationName();
        }

        public boolean isSignificant() {
            if(undoGroup != null && undoGroup.isSignificant()) {
                return true;
            }
            return super.isSignificant();
        }

        public void die() {
            commitUndoGroup();
            super.die();
        }

        public String getPresentationName() {
            if(undoGroup != null)
                return undoGroup.getPresentationName();
            return super.getPresentationName();
        }
    }
}

// vi: set sw=4 ts=8:
