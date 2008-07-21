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

import com.raelity.text.TextUtil.MySegment;
import java.io.File;
import javax.swing.text.BadLocationException;

/**
 *
 * @author erra
 */
public interface ViBuffer {
    
    /** @return opaque FileObject backing this Buffer */
    public Object getDocument();
    
    /** Replace indicated region with string */
    public void replaceString(int start, int end, String s);
    
    /** Delete a bunch of characters */
    public void deleteChar(int start, int end);
    
    /** insert text at specified location */
    public void insertText(int offset, String s);
    
    /** get some text from the document */
    public String getText(int offset, int length) throws BadLocationException;
    
    public void replaceChar(int offset, char c);
    
    /** undo a change */
    public void undo();
    
    /** redo a change */
    public void redo();

    /** a portion of a document may be write protected */
    public boolean isGuarded(int offset);
    
    
    
    
    /** platform indent algorithm */
    public void reindent(int line, int count);
    
    /** @return the line number, 1 based, corresponding to the offset */
    public int getLineNumber(int offset);
    
    /** @return the column number, 1 based, corresponding to the offset */
    public int getColumnNumber(int offset);
    
    /** @return the starting offset of the line */
    public int getLineStartOffset(int line);
    
    /** @return the starting offset of the line */
    public int getLineEndOffset(int line);
    
    /** @return the starting offset of the line */
    public int getLineStartOffsetFromOffset(int offset);
    
    /** @return the end offset of the line, char past newline */
    public int getLineEndOffsetFromOffset(int offset);
    
    /** @return the number of lines in the associated file */
    public int getLineCount();
    
    /** @return the length of the document */
    public int getLength();
    
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



    public int[] getVisualSelectBlocks(ViTextView tv,
                                       int startOffset, int endOffset);
    
    
    
    
    /** start an undo group, must be paired */
    public void beginUndo();
    
    /** end an undo group, must be paired */
    public void endUndo();
    
    /** between a begin and an end undo? */
    public boolean isInUndo();
    
    /** start a insert (user typing) ungo group, must be paired */
    public void beginInsertUndo();
    
    /** end a insert (user typing) ungo group, must be paired */
    public void endInsertUndo();
    
    /** between a insert begin and end undo? */
    public boolean isInInsertUndo();
    
    /** Fetch the lower case mark. May not be initialized. */
    public ViMark getMark(int i);
    
    /** associate the indicated mark with a particular offset
     * @deprecated use setMarkPos
     */
    public void setMarkOffset(ViMark mark, int offset, boolean global_mark);

    /** NEEDSWORK: createMark: attached to this text view, should be in ViBuffer
     * @return a null Mark */
    public ViMark createMark();



    public void displayFileInfo(ViTextView tv);

    /**
     * Uses ViFS to get the info.
     * @return
     */
    public String getDisplayFileName();

    public String getDisplayFileNameAndSize();

    /**
     * In the future, to support multiple file modifiers, could take a File
     * as an argument, and return a File. Or take a String which is the list
     * of options.
     *
     * VIM: ":help filename-modifiers"
     *
     * NEEDSWORK: missing options, only one option handled
     */
    public String modifyFilename(char option);

    /**
     * This method provides a file only so that the path can be examined.
     * @return null or the path for this file
     */
    public File getJavaFile();
}
