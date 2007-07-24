/*
 * ViBuffer.java
 *
 * Created on July 5, 2007, 12:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi;

import com.raelity.text.TextUtil.MySegment;
import javax.swing.text.BadLocationException;

/**
 *
 * @author erra
 */
public interface ViBuffer {
  /** annonymous mark operations */
  public enum MARKOP { TOGGLE, NEXT, PREV }




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




  /** Anonymous mark handling.
   * Count is the Nth mark forward, back. It is ignored by TOGGLE.
   */
  public void anonymousMark(MARKOP op, int count);

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



}
