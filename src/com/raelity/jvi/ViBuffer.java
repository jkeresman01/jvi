/*
 * ViBuffer.java
 *
 * Created on July 5, 2007, 12:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
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
 * Copyright (C) 2007 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi;

import java.io.File;

import com.raelity.text.MySegment;

/**
 * TODO: throw ViBadLocationException in all doc mod methods.
 * 
 * @author erra
 */
public interface ViBuffer {

    public enum BIAS { BACK, FORW }
    
    /** @return opaque FileObject backing this Buffer */
    public Object getDocument();
    
    /** Replace indicated region with string */
    public void replaceString(int start, int end, String s);
    
    /** Delete a bunch of characters */
    public void deleteChar(int start, int end);
    
    /** insert text at specified location */
    public void insertText(int offset, String s);
    
    /** get some text from the document */
    public String getText(int offset, int length) throws ViBadLocationException;

    /** should be a BIAS.BACK mark */
    public void replaceChar(ViMark pos, char c);
    
    /** undo a change */
    public void undo();
    
    /** redo a change */
    public void redo();

    /** a portion of a document may be write protected */
    public boolean isGuarded(int offset);

    /** notify that a read only violation */
    public void readOnlyError(ViTextView tv);
    
    
    

    /** platform indent algorithm */
    public void reindent(int line, int count);
    
    /** platform indent algorithm */
    public void reformat(int line, int count);
    
    /** @return the line number, 1 based, corresponding to the offset */
    public int getLineNumber(int offset);
    
    /** @return the column number, 1 based, corresponding to the offset */
    public int getColumnNumber(int offset);
    
    /** @return the starting offset of the line */
    public int getLineStartOffset(int line);
    
    /** @return the end offset of the line, char past newline */
    public int getLineEndOffset(int line);
    
    /**
     * Like getLineEndOffset, but backup the offset if points after the
     * implied '\n'. See javax.swing.text.element.getEndOffset
     * (note: probably want to migrate all uses to this method, deprecate other)
     * 
     * @return the end offset of the line, char past newline.
     */
    public int getLineEndOffset2(int line);
    
    /** @return the starting offset of the line */
    public int getLineStartOffsetFromOffset(int offset);
    
    /** @return the end offset of the line, char past newline */
    public int getLineEndOffsetFromOffset(int offset);
    
    /** @return the number of lines in the associated file */
    public int getLineCount();
    
    /** @return the length of the document */
    public int getLength();

    /** @return the length of the line, not including the '\n' */
    public int getLineLength(int line);
    
    /**
     * The associated character iterator is initialized with first().
     * @return the segment for the line.
     */
    public MySegment getLineSegment(int line);
    
    /** Fill the argument segment with the requested text. If the segment
     * is null, then create a segment.
     * The associated character iterator is initialized with first().
     * @return a segment for the requested text.
     */
    public MySegment getSegment(int offset, int length, MySegment segment);

    /**
     * Handy for searching the document.
     * If end less than zero, then goto end of document.
     * @param start document offset
     */
    public CharSequence getDocumentCharSequence(int start, int end);



    public int[] getVisualSelectBlocks(ViTextView tv,
                                       int startOffset, int endOffset);
    
    


    /** start an undo group, must be paired */
    public void do_beginUndo();
    
    /** end an undo group, must be paired */
    public void do_endUndo();
    
    /** start a insert (user typing) ungo group, must be paired */
    public void do_beginInsertUndo();
    
    /** end a insert (user typing) ungo group, must be paired */
    public void do_endInsertUndo();

    /**
     * Fetch the mark. May not be initialized.
     * Does assert if this is not G.curbuf.
     */
    public ViMark getMark(char c);
    
    /**
     * @param fpos if non-null, initialize mark to this value
     * @return a Mark attached to this Buffer
     */
    public ViMark createMark(ViFPOS fpos);
    /** following marks only support getOffset() */
    public ViMark createMark(int offset, BIAS bias);

    public ViFPOS createFPOS(int offset);

    /**
     * Uses ViFS to get the info.
     * @return
     */
    public String getDisplayPath();

    /**
     * For debug messages
     * @return
     */
    default String getDebugFileName()
    {
        return getDisplayPath();
    }

    /**
     * This method provides a {@linkplain File} for the buffer.
     * @return null or the java File for this buffer
     */
    public File getFile();
}
