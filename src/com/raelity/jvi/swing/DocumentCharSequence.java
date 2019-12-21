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
 * Copyright (C) 2000-2011 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.swing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

/**
 * Present up to an entire document as a CharSequence.
 * Note that using the CharSequence to scan an entire document
 * should need at most two Segments. This is because PartialReturn
 * is used and always ask for entire document.
 * 
 * @author Ernie Rael <err at raelity.com>
 */
public class DocumentCharSequence implements CharSequence
{
    private static final Logger LOG
            = Logger.getLogger(DocumentCharSequence.class.getName());

    private final Document doc;
    private int start;
    private int end;

    private int lo = Integer.MAX_VALUE;
    private int hi;
    private int adjust;
    private Segment seg;
    private int segDocOffset;

    /**
     * Note if 'end' is less than 0, then go to end of file.
     * @param doc Document
     * @param start  starting offset in document
     * @param end  ending document offset, exclusive
     */
    public DocumentCharSequence(Document doc, int start, int end)
    {
        LOG.log(Level.FINE, "DocumentCharSequence: start: {0}, end{1}",
                new Object[]{start, end});
        this.doc = doc;
        if(start > end || start < 0 || end > doc.getLength())
            throw new IndexOutOfBoundsException(
                    "[" + start + "," + end + ") not in [0,len)");
        this.start = start;
        if(end < 0)
            end = doc.getLength();
        this.end = end;
        seg = new Segment();
        seg.setPartialReturn(true);
    }

    @Override
    public int length()
    {
        return end - start;
    }

    @Override
    public char charAt(int index)
    {
        if(index < lo || index >= hi)
            getSegment(index);
        return seg.array[index + adjust];
    }

    private void getSegment(int idx)
    {
        int offset = start + idx;
        if(idx < 0 || idx >= length())
            throw new IndexOutOfBoundsException(
                    "" + idx + " not in [0," + length() + ")");
        try {
            doc.getText(offset, doc.getLength() - offset, seg);
        } catch(BadLocationException ex) {
            lo = Integer.MAX_VALUE;
            RuntimeException rex
                    = new IndexOutOfBoundsException("BadLocation Exception");
            rex.initCause(ex);
            throw rex;
        }
        LOG.log(Level.FINE, "getSegment: idx: {0}, seg.off: {1}, seg.count: {2}",
                new Object[]{idx, seg.offset, seg.count});
        segDocOffset = offset;
        lo = segDocOffset - start;
        hi = Math.min(lo + seg.count, end);
        adjust = seg.offset - lo;
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return new DocumentCharSequence(doc,
                start + this.start, end + this.start);
    }

    @Override
    public String toString()
    {
        String s;
        try {
            s = doc.getText(start, end - start);
        } catch(BadLocationException ex) {
            LOG.log(Level.SEVERE, null, ex);
            s = "TOSTRING DOCUMENT EXCEPTION";
        }
        return s;
    }

}
