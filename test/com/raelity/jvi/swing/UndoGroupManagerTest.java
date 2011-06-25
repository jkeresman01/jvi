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
package com.raelity.jvi.swing;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoableEdit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class UndoGroupManagerTest {
    private String content = "Hello";

    public UndoGroupManagerTest()
    {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    Support support;

    @Before
    public void setUp()
    {
        support = new Support();
        enableWarning();
    }

    @After
    public void tearDown()
    {
    }

    // could install a logger "Handler" and test the warning only when
    // expected. Maybe later.
    void disableWarning()
    {
        Logger l = Logger.getLogger(UndoGroupManager.class.getName());
        l.setLevel(Level.SEVERE);
    }
    void enableWarning()
    {
        Logger l = Logger.getLogger(UndoGroupManager.class.getName());
        l.setLevel(Level.INFO);
    }

    // Use these methods with the UndoRedoGroup patch
    void beginChunk(Document d) {
        // ur().beginUndoGroup();
        sendUndoableEdit(d, UndoGroupManager.BEGIN_COMMIT_GROUP);
    }

    void endChunk(Document d) {
        // ur().endUndoGroup();
        sendUndoableEdit(d, UndoGroupManager.END_COMMIT_GROUP);
    }

    void markChunk(Document d) {
        // ur().endUndoGroup();
        sendUndoableEdit(d, UndoGroupManager.MARK_COMMIT_GROUP);
    }

    void sendUndoableEdit(Document d, UndoableEdit ue) {
        if(d instanceof AbstractDocument) {
            UndoableEditListener[] uels = ((AbstractDocument)d).getUndoableEditListeners();
            UndoableEditEvent ev = new UndoableEditEvent(d, ue);
            for(UndoableEditListener uel : uels) {
                uel.undoableEditHappened(ev);
            }
        }
    }

    // Use these methods with compound edit implementation
    // CompoundEdit beginChunk(Document d) {
    //     CompoundEdit ce = new CompoundEdit();
    //     support.getUndoRedo().undoableEditHappened
    //             (new UndoableEditEvent(d, ce));
    //     return ce;
    // }

    // void endChunk(Document d, CompoundEdit ce) {
    //     ce.end();
    // }

    UndoManager ur() {
        return support.getUndoRedo();
    }


    @Test
    public void testTrivialChunk() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();

        // same operations as testSingleChunk,
        // but don't test modified/canUndo/canRedo state

        beginChunk(d);
        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);
        endChunk(d);

        assertEquals("data", "ab", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("after undo data", "", d.getText(0, d.getLength()));

        ur().redo();
        assertEquals("after redo data", "ab", d.getText(0, d.getLength()));
    }

    @Test
    public void testSingleChunk() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        ///// assertFalse("initially: not modified", support.isModified());
        assertFalse("initially: no undo", ur().canUndo());
        assertFalse("initially: no redo", ur().canRedo());

        beginChunk(d);
        ///// assertFalse("start chunk: not modified", support.isModified());
        assertFalse("start chunk: no undo", ur().canUndo());
        assertFalse("start chunk: no redo", ur().canRedo());

        d.insertString(d.getLength(), "a", null);
        ///// assertTrue("insert: modified", support.isModified());
        assertTrue("insert: can undo", ur().canUndo());
        assertFalse("insert: no redo", ur().canRedo());

        d.insertString(d.getLength(), "b", null);
        endChunk(d);
        assertEquals("chunk: data", "ab", d.getText(0, d.getLength()));
        ///// assertTrue("endChunk: modified", support.isModified());
        assertTrue("endChunk: can undo", ur().canUndo());
        assertFalse("endChunk: no redo", ur().canRedo());

        ur().undo();
        assertEquals("after undo: data", "", d.getText(0, d.getLength()));
        ///// assertFalse("undo: not modified", support.isModified());
        assertFalse("undo: no undo", ur().canUndo());
        assertTrue("undo: can redo", ur().canRedo());

        ur().redo();
        assertEquals("after redo: data", "ab", d.getText(0, d.getLength()));
        ///// assertTrue("redo: modified", support.isModified());
        assertTrue("redo: can undo", ur().canUndo());
        assertFalse("redo: no redo", ur().canRedo());
    }

    /** this also tests mixing regular and chunks */
    @Test
    public void testExtraEndChunk() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();

        beginChunk(d);

        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);

        endChunk(d);
        assertEquals("chunk: data", "ab", d.getText(0, d.getLength()));

        disableWarning();
        endChunk(d);
        endChunk(d);

        assertEquals("extraEnd: data", "ab", d.getText(0, d.getLength()));
        ///// assertTrue("extraEnd: modified", support.isModified());
        assertTrue("extraEnd: can undo", ur().canUndo());
        assertFalse("extraEnd: no redo", ur().canRedo());

        d.insertString(d.getLength(), "c", null);
        d.insertString(d.getLength(), "d", null);
        endChunk(d);
        assertEquals("extraEnd2: data", "abcd", d.getText(0, d.getLength()));
        ur().undo();
        endChunk(d);
        assertEquals("undo1: data", "abc", d.getText(0, d.getLength()));
        ur().undo();
        assertEquals("undo2: data", "ab", d.getText(0, d.getLength()));
        ur().undo();
        endChunk(d);
        assertEquals("undo3: data", "", d.getText(0, d.getLength()));
        ur().redo();
        assertEquals("redo1: data", "ab", d.getText(0, d.getLength()));
        ur().redo();
        endChunk(d);
        assertEquals("redo2: data", "abc", d.getText(0, d.getLength()));
        ur().redo();
        assertEquals("redo3: data", "abcd", d.getText(0, d.getLength()));
        enableWarning();
    }

    @Test
    public void testUndoRedoWhileActiveChunk() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        beginChunk(d);
        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);

        assertEquals("before undo: data", "ab", d.getText(0, d.getLength()));

        ur().undo();

        // These asserts assume that an undo in the middle of a chunk
        // is an undo on the whole chunk so far.

        assertEquals("after undo: data", "", d.getText(0, d.getLength()));
        ///// assertFalse("after undo: not modified", support.isModified());
        assertFalse("after undo: no undo", ur().canUndo());
        assertTrue("after undo: can redo", ur().canRedo());

        // note still in the chunk.

        ur().redo();
        assertEquals("after redo: data", "ab", d.getText(0, d.getLength()));
        ///// assertTrue("after redo: modified", support.isModified());
        assertTrue("after redo: can undo", ur().canUndo());
        assertFalse("after redo: no redo", ur().canRedo());

        ur().undo();
        assertEquals("after undo: data", "", d.getText(0, d.getLength()));

        // note still in the chunk.

        d.insertString(d.getLength(), "c", null);
        d.insertString(d.getLength(), "d", null);
        endChunk(d);

        assertEquals("after endChunk: data", "cd", d.getText(0, d.getLength()));
        ///// assertTrue("after endChunk: modified", support.isModified());
        assertTrue("after endChunk: can undo", ur().canUndo());
        assertFalse("after endChunk: no redo", ur().canRedo());


        ur().undo();
        assertEquals("undo after endChunk: data", "", d.getText(0, d.getLength()));
        ///// assertFalse("undo after endChunk: not modified", support.isModified());
        assertFalse("undo after endChunk: no undo", ur().canUndo());
        assertTrue("undo after endChunk: can redo", ur().canRedo());
    }

    // saveDocument NOT IMPLEMENTED
    public void testSaveDocumentWhileActiveChunkCommon(boolean doFailCase) throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        beginChunk(d);
        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);

        support.saveDocument (); // creates a separate undoable chunk
        ///// assertFalse("save: not modified", support.isModified());
        assertTrue("save: can undo", ur().canUndo());
        assertFalse("save: no redo", ur().canRedo());

        d.insertString(d.getLength(), "c", null);
        d.insertString(d.getLength(), "d", null);
        endChunk(d);

        assertEquals("insert, after save: data", "abcd", d.getText(0, d.getLength()));
        ///// assertTrue("insert, after save: modified", support.isModified());
        assertTrue("insert, after save: can undo", ur().canUndo());
        assertFalse("insert, after save: no redo", ur().canRedo());

        ur().undo();
        assertEquals("undo, at save: data", "ab", d.getText(0, d.getLength()));
        ///// assertFalse("undo, at save: not modified", support.isModified());
        assertTrue("undo, at save: can undo", ur().canUndo());
        assertTrue("undo, at save: can redo", ur().canRedo());

        ur().undo();
        assertEquals("undo, before save: data", "", d.getText(0, d.getLength()));

        if(doFailCase) {
            // ****************************************************************
            // CES BUG???
            ///// assertTrue("undo, before save: modified", support.isModified());
            // ****************************************************************
        }

        assertFalse("undo, before save: can undo", ur().canUndo());
        assertTrue("undo, before save: can redo", ur().canRedo());

        ur().redo();
        assertEquals("redo, at save: data", "ab", d.getText(0, d.getLength()));
        ///// assertFalse("redo, at save: not modified", support.isModified());
        assertTrue("redo, at save: can undo", ur().canUndo());
        assertTrue("redo, at save: can redo", ur().canRedo());
    }

    // saveDocument NOT IMPLEMENTED
    public void testSaveDocumentWhileActiveChunk() throws Exception {
        testSaveDocumentWhileActiveChunkCommon(false);
    }

    // This fails, below is "testSaveDocumentErrorCase" without chunking,
    // it also fails.
    // public void testSaveDocumentWhileActiveChunkErroCase() throws Exception {
    //     testSaveDocumentWhileActiveChunkCommon(true);
    // }

    @Test
    public void testNestedChunks() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        beginChunk(d);
        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);

        beginChunk(d); // creates a separate undoable chunk

        d.insertString(d.getLength(), "c", null);
        d.insertString(d.getLength(), "d", null);

        endChunk(d);

        d.insertString(d.getLength(), "e", null);
        d.insertString(d.getLength(), "f", null);

        endChunk(d);

        assertEquals("data", "abcdef", d.getText(0, d.getLength()));

        // following fails if nesting not supported
        ur().undo();
        assertEquals("undo1", "abcd", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("undo2", "ab", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("undo3", "", d.getText(0, d.getLength()));
    }

    @Test
    public void testNestedEmpyChunks() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        beginChunk(d);
        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);

        // should have no effect
        beginChunk(d);
        endChunk(d);

        d.insertString(d.getLength(), "e", null);
        d.insertString(d.getLength(), "f", null);

        endChunk(d);

        assertEquals("data", "abef", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("undo3", "", d.getText(0, d.getLength()));
    }

    @Test
    public void testNestedEmpyChunks2() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        beginChunk(d);
        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);

        // should have no effect
        beginChunk(d);
        beginChunk(d);
        endChunk(d);
        endChunk(d);
        beginChunk(d);
        endChunk(d);

        d.insertString(d.getLength(), "e", null);
        d.insertString(d.getLength(), "f", null);

        endChunk(d);

        assertEquals("data", "abef", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("undo3", "", d.getText(0, d.getLength()));
    }

    @Test
    public void testNestedEmpyChunks3() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        beginChunk(d);
        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);

        beginChunk(d);  // NEST
        d.insertString(d.getLength(), "c", null);

        // should have no effect
        beginChunk(d);
        endChunk(d);

        d.insertString(d.getLength(), "d", null);
        endChunk(d);    // UN-NEST

        // should have no effect
        beginChunk(d);
        endChunk(d);

        d.insertString(d.getLength(), "e", null);

        // should have no effect
        beginChunk(d);
        endChunk(d);

        d.insertString(d.getLength(), "f", null);

        // should have no effect
        beginChunk(d);
        endChunk(d);

        d.insertString(d.getLength(), "g", null);

        endChunk(d);

        assertEquals("data", "abcdefg", d.getText(0, d.getLength()));

        // following fails if nesting not supported
        ur().undo();
        assertEquals("undo1", "abcd", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("undo2", "ab", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("undo3", "", d.getText(0, d.getLength()));
    }

    @Test
    public void testMarkCommitGroup() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        beginChunk(d);
        d.insertString(d.getLength(), "a", null);
        d.insertString(d.getLength(), "b", null);

        markChunk(d); // creates a separate undoable chunk

        d.insertString(d.getLength(), "c", null);
        d.insertString(d.getLength(), "d", null);

        markChunk(d);

        d.insertString(d.getLength(), "e", null);
        d.insertString(d.getLength(), "f", null);

        endChunk(d);

        assertEquals("data", "abcdef", d.getText(0, d.getLength()));

        // following fails if nesting not supported
        ur().undo();
        assertEquals("undo1", "abcd", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("undo2", "ab", d.getText(0, d.getLength()));

        ur().undo();
        assertEquals("undo3", "", d.getText(0, d.getLength()));
    }

    @Test
    public void testDefaultUndoManagerar() throws Exception {
        content = "";
        StyledDocument d = support.openDocument();
        support.changeUndoManager(new UndoManager());

        d.insertString(d.getLength(), "a", null);
        beginChunk(d);
        d.insertString(d.getLength(), "b", null);
        endChunk(d);

        assertEquals("data", "ab", d.getText(0, d.getLength()));

        ur().undo();
        ur().undo();
        assertEquals("after undo data", "", d.getText(0, d.getLength()));

        // do the same steps, this tests that the CommitGroupEdit,
        // e.g. BEGIN_COMMIT_GROUP, can be reused. The re-use is
        // accomplished through overriding canRedo/canUndo

        d.insertString(d.getLength(), "a", null);
        beginChunk(d);
        d.insertString(d.getLength(), "b", null);
        endChunk(d);

        assertEquals("data", "ab", d.getText(0, d.getLength()));

        ur().undo();
        ur().undo();
        assertEquals("after undo data", "", d.getText(0, d.getLength()));
    }

    static class Support {
        StyledDocument doc;
        UndoManager undoMan;

        Support()
        {
            doc = new DefaultStyledDocument();
            undoMan = new UndoGroupManager();
            doc.addUndoableEditListener(undoMan);
        }

        void changeUndoManager(UndoManager undoMan)
        {
            doc.removeUndoableEditListener(this.undoMan);
            doc.addUndoableEditListener(undoMan);
            this.undoMan = undoMan;
        }

        StyledDocument openDocument()
        {
            return doc;
        }

        UndoManager getUndoRedo()
        {
            return undoMan;
        }

        void saveDocument()
        {
            assert false;
        }
    }
}
