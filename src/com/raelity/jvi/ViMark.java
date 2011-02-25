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
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 * 
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi;

/**
 * A Mark represents a position within a document. A Mark is tracked
 * by the document.
 *
 * A Mark in vi specifies a row and column. The row "floats" as lines are
 * inserted and deleted earlier in the file.
 * <b>However</b> the column is set when
 * the mark is created and does not change even if characters are added on the
 * same line before the column.
 */
public interface ViMark extends ViFPOS {
    
    /** Set mark to position */
    public void setMark(ViFPOS fpos);
    
    /** Invalidate the mark. */
    public void invalidate();

    /**
     * Since the column is generally fixed when the mark is set,
     * if characters are deleted then the value returned by getColumn()
     * may be less than the original column.
     * This returns originalColumn - getColumn(); note always ge 0.
     * @return number of columns "lost".
     */
    public int getOriginalColumnDelta();
    
    public class MarkException extends RuntimeException {
        public MarkException(String msg) {
            super(msg);
        }
    }
    
    public class MarkOrphanException extends MarkException {
        public MarkOrphanException(String msg) {
            super(msg);
        }
    }
}
