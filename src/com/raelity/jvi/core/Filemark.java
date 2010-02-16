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
package com.raelity.jvi.core;

import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViTextView;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** A mark that can be persisted. The mark is not fully functional if
 * the associated file is not opened in a window.
 * <p>
 * When the associated file is not opened, only getOffset() works.
 * </p>
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Filemark implements ViMark { // NEEDSWORK: extends File

    private ViMark mark;
    private File f;

    private int fnum;
    private int wnum;
    //String fname;
    // offset,line,col valid if fnum is 0
    private int offset;
    private int line;
    private int col;

    public Filemark(ViMark mark, ViTextView tv) {
        this.mark = mark;
        initStuff(tv);
        f = mark.getBuffer().getFile().getAbsoluteFile();
    }

    private void initStuff(ViTextView tv) {
        this.fnum = tv.getBuffer().b_fnum;
        this.wnum = ViManager.getViFactory()
                .getAppView(tv.getEditorComponent()).getWNum();
        this.line = mark.getLine();
        this.col = mark.getColumn();
    }

    private Filemark(File f, int offset, int line, int col) {
        this.f = f;
        this.offset = offset;
        this.line = line;
        this.col = col;
    }

    public File getFile() {
        if(isValidFilemark())
            f = mark.getBuffer().getFile(); // maybe a rename happened
        return f;
    }

    public int getWnum() {
        return isValidFilemark() ? wnum : 0;
    }

    /** If this filemark corresponds to file and the mark is not hooked
     * into the file then create a real mark */
    void startup(File f, ViTextView tv) {
        if(!isValidFilemark() && this.f.equals(f)) {
            mark = tv.getBuffer().createMark();
            mark.setMark(tv.getBuffer().createFPOS(offset));
            initStuff(tv);
        }
    }

    /** record offset */
    void shutdown(ViTextView tv) {
        if(isValidFilemark() && mark.getBuffer() == tv.getBuffer()) {
            f = mark.getBuffer().getFile(); // maybe a rename happened
            offset = mark.getOffset();
        }
    }

    private boolean isValidFilemark() {
        return mark != null && mark.isValid() && fnum != 0;
    }

    public int getOffset() {
        return ! isValidFilemark() ? offset : mark.getOffset();
    }

    public int getLine() {
        return ! isValidFilemark() ? line : mark.getLine();
    }

    public int getColumn() {
        return ! isValidFilemark() ? col : mark.getColumn();
    }

    public boolean isValid() {
        return true; // though in a limitted context
    }

    public Buffer getBuffer() {
        return ! isValidFilemark() ? null : mark.getBuffer();
    }

    static Filemark read_viminfo_filemark(Preferences prefs, String markName) {
        try {
            if (!prefs.nodeExists(markName))
                return null;
        } catch (BackingStoreException ex) {
            return null;
        }

        // set the node for the specific filemark
        prefs = prefs.node(markName);

        String fName = prefs.get(MarkOps.FNAME, null);
        int offset = prefs.getInt(MarkOps.OFFSET, -1);
        int line = prefs.getInt(MarkOps.LINE, -1);
        int col = prefs.getInt(MarkOps.COL, -1);
        if (fName == null || offset < 0 || line < 0 || col < 0) {
            return null;
        }
        File f = new File(fName);
        return new Filemark(f, offset, line, col);
    }

    static void write_viminfo_filemark(Preferences prefs, String markName,
                                       Filemark fm) {
        if(fm == null) {
            try {
                if (prefs.nodeExists(markName)) {
                    prefs.node(markName).removeNode();
                }
            } catch (BackingStoreException ex) {
            }
        } else {
            // set the node for the specific filemark
            prefs = prefs.node(markName);
            prefs.put(MarkOps.FNAME, fm.f.getAbsolutePath());
            prefs.putInt(MarkOps.OFFSET, fm.getOffset());
            prefs.putInt(MarkOps.LINE, fm.getLine());
            prefs.putInt(MarkOps.COL, fm.getColumn());
        }
    }

    //////////////////////////////////////////////////////////////////////
    //
    // A filemark have very limited functionality, mostly unsupported

    public void setMark(ViFPOS fpos) {
        throw new UnsupportedOperationException();
    }

    public void invalidate() {
        throw new UnsupportedOperationException();
    }

    public void set(int line, int column) {
        throw new UnsupportedOperationException();
    }

    public void set(int offset) {
        throw new UnsupportedOperationException();
    }

    public void set(ViFPOS fpos) {
        throw new UnsupportedOperationException();
    }

    public void setColumn(int col) {
        throw new UnsupportedOperationException();
    }

    public void incColumn() {
        throw new UnsupportedOperationException();
    }

    public void decColumn() {
        throw new UnsupportedOperationException();
    }

    public void incLine() {
        throw new UnsupportedOperationException();
    }

    public void decLine() {
        throw new UnsupportedOperationException();
    }

    public void setLine(int line) {
        throw new UnsupportedOperationException();
    }

    public ViFPOS copy() {
        throw new UnsupportedOperationException();
    }

    public void verify(Buffer buf) {
        throw new UnsupportedOperationException();
    }

    public int compareTo(ViFPOS o) {
        throw new UnsupportedOperationException();
    }

    //////////////////////////////////////////////////////////////////////
    //
    // DELEGATE to mark

    /*
    public int compareTo(ViFPOS o) {
        return mark.compareTo(o);
    }

    public void verify(Buffer buf) {
        mark.verify(buf);
    }

    public void setLine(int line) {
        mark.setLine(line);
    }

    public void setColumn(int col) {
        mark.setColumn(col);
    }

    public void set(ViFPOS fpos) {
        mark.set(fpos);
    }

    public void set(int offset) {
        mark.set(offset);
    }

    public void set(int line, int column) {
        mark.set(line, column);
    }

    public void incLine() {
        mark.incLine();
    }

    public void incColumn() {
        mark.incColumn();
    }

    public void decLine() {
        mark.decLine();
    }

    public void decColumn() {
        mark.decColumn();
    }

    public ViFPOS copy() {
        return mark.copy();
    }

    public void setMark(ViFPOS fpos) {
        mark.setMark(fpos);
    }

    public boolean isUsable() {
        return mark.isUsable();
    }

    public void invalidate() {
        mark.invalidate();
    }
     */
}
