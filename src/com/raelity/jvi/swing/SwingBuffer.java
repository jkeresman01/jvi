/*
 * SwingBuffer.java
 * 
 * Created on Jul 5, 2007, 5:32:05 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.raelity.jvi.swing;

import com.raelity.jvi.options.BooleanOption;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.Util;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViMark.MarkException;
import com.raelity.jvi.ViTextView;
import com.raelity.text.TextUtil.MySegment;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 *
 * @author erra
 */
abstract public class SwingBuffer extends Buffer {
    private static Logger LOG = Logger.getLogger(SwingBuffer.class.getName());
    private Document doc;
    private UndoGroupManager undoMan;
    //private UndoManager undoMan;
    //private CompoundEdit compoundEdit;
    
    public SwingBuffer(ViTextView tv) {
        super(tv);
        doc = ((JTextComponent)tv.getEditorComponent()).getDocument();
        startDocumentEvents();
        if(ViManager.getViFactory().isStandalone()) {
            undoMan = new UndoGroupManager();
            //undoMan = new UndoManager();
            doc.addUndoableEditListener(undoMan);
        }
    }

    @Override
    public void removeShare() {
        super.removeShare();
        if(getShare() == 0) {
            doc.removeUndoableEditListener(undoMan);
            stopDocumentEvents();
            doc = null;
        }
    }

    //////////////////////////////////////////////////////////////////////
    //
    // ViBuffer interface
    // 
    
    public int getLineNumber(int offset) {
        return getElemIndex(offset) + 1;
    }
    
    public int getColumnNumber(int offset) {
        Element elem = getElem(offset);
        return offset - elem.getStartOffset();
    }
    
    /** @return the starting offset of the line */
    public int getLineStartOffset(int line) {
        return getLineElement(line).getStartOffset();
    }
    
    /** @return the starting offset of the line */
    public int getLineEndOffset(int line) {
        return getLineElement(line).getEndOffset();
    }
    
    public int getLineStartOffsetFromOffset(int offset) {
        Element elem = getElem(offset);
        return elem.getStartOffset();
    }
    
    public int getLineEndOffsetFromOffset(int offset) {
        Element elem = getElem(offset);
        return elem.getEndOffset();
    }
    
    public int getLineCount() {
        return getDocument().getDefaultRootElement().getElementCount();
    }

    public int getLength() {
        return getDocument().getLength();
    }

    /*final public MySegment getLineSegment(int lnum) {
        return getLineSegment(lnum); // CACHE
    }*/
    
    /*final public Element getLineElement(int lnum) {
        return getLineElement(lnum); // CACHE
    }*/
    
    final public MySegment getSegment(int offset, int length, MySegment seg) {
        if(seg == null)
            seg = new MySegment();
        try {
            getDocument().getText(offset, length, seg);
        } catch (BadLocationException ex) {
            seg.count = 0;
            LOG.log(Level.SEVERE, null, ex);
        }
        seg.docOffset = offset;
        seg.first();
        return seg;
    }

    public boolean isGuarded(int offset) {
        return false;
    }



    
    private boolean isEditable() { return true; }

    /**
     * Use the document in default implementation.
     * @return Swing Document backing this EditorPane */
    public Document getDocument() {
        return doc;
    }

    public void replaceString(int start, int end, String s) {
        if( ! isEditable()) {
            Util.vim_beep();
            return;
        }
        try {
            if(start != end) {
                getDocument().remove(start, end - start);
            }
            getDocument().insertString(start, s, null);
        } catch(BadLocationException ex) {
            processTextException(ex);
        }
    }
    
    public void deleteChar(int start, int end) {
        if( ! isEditable()) {
            Util.vim_beep();
            return;
        }
        try {
            getDocument().remove(start, end - start);
        } catch(BadLocationException ex) {
            processTextException(ex);
        }
    }

    public void insertText(int offset, String s) {
        if( ! isEditable()) {
            Util.vim_beep();
            return;
        }
    /* ******************************************
    setCaretPosition(offset);
    ops.xop(TextOps.INSERT_TEXT, s);
     ******************************************/
        if(offset > getDocument().getLength()) {
            // Damn, trying to insert after the final magic newline.
            // 	(the one that gets counted in elem.getEndOffset() but
            // 	not in getLength() )
            // Take the new line from the end of the string and put it
            // at the beging, e.g. change "foo\n" to "\nfoo". Then set
            // offset to length. The adjusted string is inserted before
            // the magic newline, this gives the correct result.
            // If there is no newline at the end of the string being inserted,
            // then there will end up being a newline added to the file magically,
            // but this shouldn't really matter.
            StringBuffer new_s = new StringBuffer();
            new_s.append('\n');
            if(s.endsWith("\n")) {
                if(s.length() > 1) {
                    new_s.append(s.substring(0,s.length()-1));
                }
            } else {
                new_s.append(s);
            }
            offset = getDocument().getLength();
            s = new_s.toString();
        }
        try {
            getDocument().insertString(offset, s, null);
        } catch(BadLocationException ex) {
            processTextException(ex);
        }
    }
    
    public void replaceChar(int offset, char c) {
        if( ! isEditable()) {
            Util.vim_beep();
            return;
        }
        String s = String.valueOf(c);
        
        try {
            // This order works better with the do-again, '.', buffer
            getDocument().insertString(offset, s, null);
            getDocument().remove(offset + 1, 1);
        } catch(BadLocationException ex) {
            processTextException(ex);
        }
    }

    public String getText(int offset, int length) throws BadLocationException {
        return getDocument().getText(offset, length);
    }
    
    protected void processTextException(BadLocationException ex) {
        Util.vim_beep();
    }




    




    public void reindent(int line, int count) {
        System.err.format("reindent line %d, count %d", line, count);
        Util.vim_beep();
    }

    public void reformat(int line, int count) {
        System.err.format("reformat line %d, count %d", line, count);
        Util.vim_beep();
    }


    
    final public void undo(){
        undoOperation();
    }
    
    final public void redo() {
        redoOperation();
    }

    protected void undoOperation() {
        if(undoMan.canUndo())
            undoMan.undo();
        else
            Util.vim_beep();
    }

    protected void redoOperation() {
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
    
    //////////////////////////////////////////////////////////////////////
    //
    // Marks
    //
    
    public ViMark createMark() {
        return new Mark();
    }
    
    /**
     * A Mark in vi specifies a row and column. The row "floats" as lines are
     * inserted and deleted earlier in the file.
     * <b>However</b> the column is set when
     * the mark is created and does not change if characters are added on the
     * same line before the column.
     */
    class Mark implements ViMark {
        // The pos tracks the line number,
        // note that it's offset is one less that the actual offset
        // since there is no way to specify a left-bias for position
        private Position pos;
        private int col;
        private boolean atZero;
        
        /**
         * If the mark offset is not valid then this mark is converted into
         * a null mark.
         * // NEEDSWORK: deprecate setMark, just call it set ?????
         */
        public void setMark(ViFPOS fpos) {
            fpos.verify(SwingBuffer.this);
            if(fpos instanceof ViMark) {
                Mark mark = (Mark)fpos;
                this.pos = mark.pos;
                this.col = mark.col;
                this.atZero = mark.atZero;
            } else {
                // adapted from FPOS.set
                if(fpos.getLine() > getLineCount()) {
                    this.pos = INVALID_MARK_LINE;
                } else {
                    int column = fpos.getColumn();
                    int startOffset = getLineStartOffset(fpos.getLine());
                    int endOffset = getLineEndOffset(fpos.getLine());
                    
                    // NEEDSWORK: if the column is past the end of the line,
                    //            should it be preserved?
                    if(column >= endOffset - startOffset) {
                        ViManager.dumpStack("column " + column
                                + ", limit " + (endOffset - startOffset - 1));
                        column = endOffset - startOffset - 1;
                    }
                    setDocOffset(startOffset + column);
                }
            }
        }

        /**
        * If the mark offset is not valid then this mark is converted into
        * a null mark.
        */
        private void setDocOffset(int offset) {
            try {
                col = getColumnNumber(offset);
                if(offset == 0)
                    atZero = true;
                else {
                    atZero = false;
                    --offset;
                }
                pos = getDocument().createPosition(offset);
            } catch(BadLocationException ex) {
                pos = null;
                return;
            }
        }

        private int getDocOffset() {
            return atZero ? 0 : pos.getOffset() + 1;
        }
        
        public int getLine() {
            checkMarkUsable();
            if (this.pos == INVALID_MARK_LINE)
                return getLineCount() + 1;
            return getLineNumber(getDocOffset());
        }
        
        /**
         * NOTE: may return column position of newline
         */
        public int getColumn() {
            checkMarkUsable();
            if (this.pos == INVALID_MARK_LINE)
                return 0;
            MySegment seg = getLineSegment(getLine());
            int len = seg.length() - 1;
            return seg.length() <= 0 ? 0 : Math.min(col, len);
        }
        
        public int getOffset() {
            checkMarkUsable();
            if (this.pos == INVALID_MARK_LINE)
                return Integer.MAX_VALUE;
            return getLineStartOffsetFromOffset(getDocOffset()) + getColumn();
        }
        
        public void invalidate() {
            pos = null;
        }

        public Buffer getBuffer() {
            return SwingBuffer.this;
        }

        public void verify(Buffer buf) {
            if(buf != getBuffer())
                throw new IllegalStateException("fpos buffer mis-match");
        }
        
        public ViFPOS copy() {
            Mark m = new Mark();
            m.setMark(this);
            return m;
        }
        
        final void checkMarkUsable() {
            // same as isValid
            if(pos == null) throw new MarkException("Uninitialized Mark");
            if(getDocument() == null) {
                throw new MarkOrphanException("Mark Document null");
            }
        }

        final public boolean isValid() {
            // same as checkMarkUsable
            return pos != null && getDocument() != null;
        }
        
        final public int compareTo(ViFPOS p) {
            if(this.getOffset() < p.getOffset()) {
                return -1;
            } else if(this.getOffset() > p.getOffset()) {
                return 1;
            } else {
                return 0;
            }
        }
        
        @Override
        public boolean equals(Object o) {
            if(o instanceof ViFPOS) {
                ViFPOS fpos = (ViFPOS)o;
                return getOffset() == fpos.getOffset();
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 23 * hash + (this.pos != null ? this.pos.hashCode() : 0);
            hash = 23 * hash + this.col;
            return hash;
        }
        
        /** This is optional, may throw an UnsupportedOperationException */
        public void set(int line, int col) {
            throw new UnsupportedOperationException();
        }

        public void set(ViFPOS fpos) {
            throw new UnsupportedOperationException();
        }

        public void set(int offset)
        {
            throw new UnsupportedOperationException();
        }
        
        /** This is optional, may throw an UnsupportedOperationException */
        public void setColumn(int col) {
            throw new UnsupportedOperationException();
        }
        /** This is optional, may throw an UnsupportedOperationException */
        public void setLine(int line) {
            throw new UnsupportedOperationException();
        }

        public void decColumn()
        {
            throw new UnsupportedOperationException();
        }

        public void incColumn()
        {
            throw new UnsupportedOperationException();
        }

        public void decLine()
        {
            throw new UnsupportedOperationException();
        }

        public void incLine()
        {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public String toString() {
            return	  "offset: " + getOffset()
            + " lnum: " + getLine()
            + " col: " + getColumn()
            ;
        }
    } // class Mark
    
    private static final Position INVALID_MARK_LINE = new Position() {
        public int getOffset() {
            return 0;
        }
    };
    
    //////////////////////////////////////////////////////////////////////
    //
    // The "cache"
    //
    
    final static private boolean cacheDisabled = false;
    
    public static BooleanOption cacheTrace
            = (BooleanOption)Options.getOption(Options.dbgCache);
    
    /** @return the element index from root which contains the offset */
    protected int getElemIndex(int offset) {
        Element root = getDocument().getDefaultRootElement();
        return root.getElementIndex(offset);
    }
    
    /** @return the element which contains the offset */
    protected Element getElem(int offset) {
        Element elem = getCurrentLineElement(); // CACHE
        if(elem != null
                && elem.getStartOffset() <= offset
                && offset < elem.getEndOffset()) {
            return elem;
        }
        //Element root = getDocument().getDefaultRootElement();
        //return root.getElement(root.getElementIndex(offset));
        int line = getDocument().getDefaultRootElement().getElementIndex(offset) + 1;
        return getLineElement(line);
    }

    ElemCache getElemCache(int offset) {
        getElem(offset);
        return elemCache;
    }
    
    private class PositionSegment extends MySegment {
        /** The line number of the segment. The first line is numbered at 1.
         * This is negative if the
         * segment does not correspond to a single line.
         */
        public int line;
    }
    
    /** the segment cache */
    private PositionSegment segment = new PositionSegment();
    // private Segment tempSegment = new Segment();
    
    /** @return the positionsegment for the indicated line */
    final public MySegment getLineSegment(int line) {
        if(cacheDisabled || segment.count == 0 || segment.line != line) {
            if(cacheTrace.getBoolean())System.err.println("Miss seg: " + line);
            try {
                Element elem = getLineElement(line);
                getDocument().getText(elem.getStartOffset(),
                        elem.getEndOffset() - elem.getStartOffset(),
                        segment);
                segment.docOffset = elem.getStartOffset();
                segment.line = line;
        /* **************************************************
        int len = Math.max(80, tempSegment.count + 10);
        if(segment.array == null || segment.array.length < len) {
          segment.array = new char[len];
        }
        System.arraycopy(tempSegment.array, tempSegment.offset,
                         segment.array, 0, tempSegment.count);
        segment.count = tempSegment.count;
         **************************************************/
                // segment.offset is always zero
            } catch(BadLocationException ex) {
                segment.count = 0;
                LOG.log(Level.SEVERE, null, ex);
            }
        } else {
            if(cacheTrace.getBoolean())System.err.println("Hit seg: " + line);
        }
        //return segment;
        MySegment s = new MySegment(segment);
        s.first();
        return s;
    }
    
    final private void invalidateLineSegment() {
        if(cacheTrace.getBoolean())System.err.println("Inval seg:");
        segment.count = 0;
    }

    static class ElemCache {
        int line;
        Element elem;
    }
    
    private ElemCache elemCache = new ElemCache();
    /** the element cache */
    private Element element;
    /** the line number corresponding to the element cache (0 based). */
    private int elementLine;
    /** @return the element for the indicated line */
    final public Element getLineElement(int line) {
        line--;
        if(cacheDisabled || element == null || elementLine != line) {
            if(cacheTrace.getBoolean())System.err.println("Miss elem: " + (line+1));
            element = getDocument().getDefaultRootElement().getElement(line);
            elementLine = line;
            elemCache.line = line+1;
            elemCache.elem = element;
        } else {
            if(cacheTrace.getBoolean())System.err.println("Hit elem: " + (line+1));
        }
        return element;
    }
    
    final public Element getCurrentLineElement() {
        return element;
    }
    
    final private void invalidateElement() {
        if(cacheTrace.getBoolean())System.err.println("Inval elem:");
        element = null;
    }

    //////////////////////////////////////////////////////////////////////
    //
    // Document listeners
    // 
    // Track changes in document for possible inclusion into redo buf
    //

    private DocumentListener documentListener;

    // This listener is used for two purposes.
    //      1) Invalidate cached information about the document
    //      2) Provide information for magic redo tracking
    class DocListen implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            if(cacheTrace.getBoolean()) {
                System.err.println("doc changed: " + e.getOffset()
                                   + ":" + e.getLength() + " " + e);
                // System.err.println("element" + e.getChange());
            }
            // insertUpdate/removeUpdate fire as well so skip this one
            // invalidateData();
        }

        public void insertUpdate(DocumentEvent e) {
            if(cacheTrace.getBoolean())
                System.err.println("doc insert: " + e.getOffset()
                                   + ":" + e.getLength() + " " + e);
            invalidateData();
            undoOffset = e.getOffset();
            undoLength = e.getLength();
            undoChange = true;
            
            // If not in insert mode, then no magic redo tracking
            if(!isInsertMode())
                return;
            String s = "";

            if(ViManager.getViFactory().isEnabled()) {
                // magic redo tracking
                // things can get wierd in there...
                try {
                    s = getDocument().getText(e.getOffset(), e.getLength());
                    docInsert(e.getOffset(), s);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }
        
        public void removeUpdate(DocumentEvent e) {
            if(cacheTrace.getBoolean())
                System.err.println("doc remove: " + e.getOffset()
                                   + ":" + e.getLength() + " " + e);
            invalidateData();
            undoOffset = e.getOffset();
            undoLength = e.getLength();
            undoChange = true;
            
            // If not in insert mode, then no magic redo tracking
            if(!isInsertMode())
                return;

            if(ViManager.getViFactory().isEnabled()) {
                // magic redo tracking
                // things can get wierd in there...
                try {
                    docRemove(e.getOffset(), e.getLength(), getRemovedText(e));
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void stopDocumentEvents() {
        getDocument().removeDocumentListener(documentListener);
    }
    
    private void startDocumentEvents() {
        documentListener = new DocListen();
        getDocument().addDocumentListener(documentListener);
    }
  
    private void invalidateData() {
        // NEEDSWORK: invalidateCursor(-1);
        invalidateLineSegment();
        invalidateElement();
    }
    
    /**
     * This method can be used to determine if some action(s)
     * cause a change. The method itself has nothing to do with undo. It
     * is called from an optimized undo.
     *
     * @return true if there has be a change to the document since this method
     * was last called.
     */
    protected boolean isUndoChange() {
        boolean rval;
        rval = undoChange;
        undoChange = false;
        return rval;
    }
    
    // These variables track last insert/remove to document.
    // They are usually used for undo/redo.
    private int undoOffset;
    private int undoLength;
    private boolean undoChange;
    
    public int getUndoOffset() {
        return undoOffset;
    }
    
    public int getUndoLength() {
        return undoLength;
    }

    //////////////////////////////////////////////////////////////////////
    //
    // Default Undo/redo manager
    //

    /** <code>UndoGroupManager</code> is an <code>UndoManager</code>
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


        @Override
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

        @Override
        public synchronized void discardAllEdits() {
            commitUndoGroup();
            super.discardAllEdits();
        }

        //
        // TODO: limits
        //

        @Override
        protected UndoableEdit editToBeUndone() {
            if(undoGroup != null)
                return null;
            return super.editToBeUndone();
        }

        @Override
        protected UndoableEdit editToBeRedone() {
            if(undoGroup != null)
                return null;
            return super.editToBeRedone();
        }

        @Override
        protected void undoTo(UndoableEdit edit) {
            if(undoGroup != null)
                throw new CannotUndoException();
            super.undoTo(edit);
        }

        @Override
        protected void redoTo(UndoableEdit edit) {
            if(undoGroup != null)
                throw new CannotRedoException();
            super.redoTo(edit);
        }

        @Override
        public synchronized void undoOrRedo() {
            commitUndoGroup();
            super.undoOrRedo();
        }

        @Override
        public synchronized boolean canUndoOrRedo() {
            if(undoGroup != null)
                return true;
            return super.canUndoOrRedo();
        }

        @Override
        public synchronized void undo() {
            commitUndoGroup();
            super.undo();
        }

        @Override
        public synchronized boolean canUndo() {
            if(undoGroup != null)
                return true;
            return super.canUndo();
        }

        @Override
        public synchronized void redo() {
            if(undoGroup != null)
                throw new CannotRedoException();
            super.redo();
        }

        @Override
        public synchronized boolean canRedo() {
            if(undoGroup != null)
                return false;
            return super.canRedo();
        }

        @Override
        public synchronized void end() {
            commitUndoGroup();
            super.end();
        }

        @Override
        public synchronized String getUndoOrRedoPresentationName() {
            if(undoGroup != null)
                return undoGroup.getUndoPresentationName();
            return super.getUndoOrRedoPresentationName();
        }

        @Override
        public synchronized String getUndoPresentationName() {
            if(undoGroup != null)
                return undoGroup.getUndoPresentationName();
            return super.getUndoPresentationName();
        }

        @Override
        public synchronized String getRedoPresentationName() {
            if(undoGroup != null)
                return undoGroup.getRedoPresentationName();
            return super.getRedoPresentationName();
        }

        @Override
        public boolean isSignificant() {
            if(undoGroup != null && undoGroup.isSignificant()) {
                return true;
            }
            return super.isSignificant();
        }

        @Override
        public void die() {
            commitUndoGroup();
            super.die();
        }

        @Override
        public String getPresentationName() {
            if(undoGroup != null)
                return undoGroup.getPresentationName();
            return super.getPresentationName();
        }
    }
}
