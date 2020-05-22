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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import org.openide.util.Exceptions;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.*;
import com.raelity.text.TextUtil;

import static com.raelity.jvi.manager.ViManager.getFS;
import static com.raelity.jvi.manager.ViManager.getFactory;
import static com.raelity.text.TextUtil.sf;

/** A mark that can be persisted. The mark is not fully functional if
 * the associated file is not opened in a window.
 * <p>
 * When the associated file is not opened, only getOffset() works.
 * </p>
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Filemark implements ViMark { // NEEDSWORK: extends File

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=7)
    public static class Init implements ViInitialization
    {
        private static boolean didInit;
        @Override
        public void init()
        {
            if(didInit)
                return;
            didInit = true;
            Filemark.init();
        }
    }

    private static final String PREF_FILEMARKS = "filemarks";
    private static PreferencesImportMonitor filemarksImportCheck;
    private static DebugOption dbg;
    private static Preferences prefsFM;
    private static final ValueMap<String, Filemark> map
            = new ValueHashMap<>((Filemark fm) -> fm.getMarkName(), 26);

    private static void init()
    {
        dbg = Options.getDebugOption(Options.dbgMarks);

        PropertyChangeListener pcl = (PropertyChangeEvent evt) -> {
            String pname = evt.getPropertyName();
            switch (pname) {
            case ViManager.P_OPEN_BUF:
                openBuf((Buffer)evt.getNewValue());
                break;
            case ViManager.P_CLOSE_BUF:
                closeBuf((Buffer)evt.getOldValue());
                break;
            case ViManager.P_BOOT:
                prefsFM = getFactory().getPreferences().node(PREF_FILEMARKS);
                filemarksImportCheck = PreferencesImportMonitor.getMonitor(
                        getFactory().getPreferences(), PREF_FILEMARKS);
                read_viminfo_filemarks();
                break;
            case ViManager.P_PRE_SHUTDOWN:
                if(!filemarksImportCheck.isChange()) {
                    write_viminfo_filemarks();
                } else {
                    dbg.println(Level.INFO, "FM: jVi filemarks imported");
                }
                break;
            default:
                break;
            }
        };
        ViManager.addPropertyChangeListener(ViManager.P_BOOT, pcl);
        ViManager.addPropertyChangeListener(ViManager.P_PRE_SHUTDOWN, pcl);
        ViManager.addPropertyChangeListener(ViManager.P_OPEN_BUF, pcl);
        ViManager.addPropertyChangeListener(ViManager.P_CLOSE_BUF, pcl);
    }

    final private String markName;
    private ViMark mark;
    private File f;

    private int line;
    private int col;
    private int offset;

    static Filemark create(String markName, ViMark mark)
    {
        assert isValidMarkName(markName);
        Filemark fm = new Filemark(markName, mark);
        dbg.printf(Level.INFO, () -> sf("FM: Create %s\n", dump(fm)));
        Filemark prevFM = map.put(fm);
        fm.persist();
        if(prevFM != null) {
            dbg.println(Level.FINEST, () -> sf("FM: replace %s", dump(prevFM)));
        }
        return fm;
    }

    static Filemark get(String markName) {
        return map.get(markName);
    }

    private static void read_viminfo_filemarks() {
        try {
            for(String markName : prefsFM.childrenNames()) {
                Filemark fm = read_viminfo_filemark(markName);
                if(fm != null) {
                    map.put(fm);
                }
            }
        } catch(BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static void write_viminfo_filemarks() {
        dbg.printf(Level.INFO, "FM: write filemarks\n");
        // could delete stuff that isn't in the map
        for(Filemark fm : map.values()) {
            fm.persist();
        }
    }

    static void deleteMark(String markName) {
        assert isValidMarkName(markName);
        Filemark fm = map.remove(markName);
        boolean prefsExist = false;

        boolean fmExist = fm != null;
        try {
            if (prefsFM.nodeExists(markName)) {
                prefsFM.node(markName).removeNode();
                prefsExist = true;
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
        if(fmExist || prefsExist)
            dbg.printf(Level.INFO, () -> sf("FM: deleteMark %s\n", markName));
        if(fmExist ^ prefsExist) {
            final boolean prefsExistF = prefsExist;
            dbg.printf(Level.SEVERE, () -> sf(
                    "FM: deleteMark WARNING %s fmExist=%b prefsExist=%b\n",
                    markName, fmExist, prefsExistF));
        }
    }

    /** create a valid filemark */
    private Filemark(String markName, ViMark mark) {
        this.markName = markName;
        this.mark = mark;
        f = mark.getBuffer().getFile().getAbsoluteFile();
        initStuff();
    }

    private void initStuff() {
        this.line = mark.getLine();
        this.col = mark.getColumn();
        this.offset = mark.getOffset();
    }

    /** Create an invalid filemark, no mark.
     *  Typically from preferences. */
    private Filemark(String markName, File f, int line, int col, int offset) {
        this.markName = markName;
        this.f = f;
        this.line = line;
        this.col = col;
        this.offset = offset;
    }

    /**
     * Create a mark, hooking into the file. Use line,col. */
    private void hookup(Buffer buf) {
        assert !isActiveFilemark();
        assert f.equals(buf.getFile().getAbsoluteFile());
        mark = buf.createMark(null);
        FPOS fpos = new FPOS(buf);
        fpos.set(buf, line, col, true);
        mark.setMark(fpos);
        // line,col may may have been brought in bounds
        if(mark.getLine() != line || mark.getColumn() != col || mark.getOffset() != offset) {
            dbg.printf(Level.WARNING, () -> sf("FM: hookup re-boundary %s\n", dump(this)));
            initStuff();
            persist();
        }
        dbg.printf(Level.CONFIG, () -> sf("FM: hookup buf %s\n", dump(this)));
    }

    /** closing buffer, record mark position, persist as needed, invalidate mark */
    private void unhookup(Buffer buf) {
        dbg.printf(Level.FINEST, () -> sf("FM: unhookup %s\n", dump(this)));
        assert isActiveFilemark() && mark.getBuffer() == buf;
        f = buf.getFile().getAbsoluteFile(); // maybe a rename happened
        persist();
        mark = null;
    }

    private void persist() {

        Preferences prefs = prefsFM.node(markName);
        boolean isActiveFM = isActiveFilemark();
        if(isActiveFM) {
            // only valid FMs can read the mark
            line = mark.getLine();
            col = mark.getColumn();
            offset = mark.getOffset();
        }
        dbg.printf(Level.CONFIG, () -> sf("FM: persist valid=%b %s\n",
                                          isActiveFM, dump(this)));
        if(!compare(this, prefs)) {
            // in-core vs in-prefs are different, so update the prefs.
            // If the FM is not valid, it should NOT be different
            if(!isActiveFM)
                dbg.printf(Level.SEVERE, () -> sf("FM: persist WARNING %s\n",
                                                  dump(prefs)));
            prefs.put(MarkOps.FNAME, f.getAbsolutePath());
            prefs.putInt(MarkOps.LINE, getLine());
            prefs.putInt(MarkOps.COL, getColumn());
            prefs.putInt(MarkOps.OFFSET, getOffset());
        }
        checkFM(getMarkName(), this);
    }

    public File getFile() {
        if(isActiveFilemark())
            f = mark.getBuffer().getFile().getAbsoluteFile(); // maybe a rename happened
        return f;
    }

    // public int getWnum() {
    //     return isValidFilemark() ? wnum : 0;
    // }

    public String getMarkName() {
        return markName;
    }

    private boolean isActiveFilemark() {
        return mark != null && mark.isValid();
    }

    @Override public int getOffset() {
        return ! isActiveFilemark() ? offset : mark.getOffset();
    }

    @Override public int getLine() {
        return ! isActiveFilemark() ? line : mark.getLine();
    }

    @Override public int getColumn() {
        return ! isActiveFilemark() ? col : mark.getColumn();
    }

    @Override public ViFPOS copyTo(ViFPOS target) {
        target.set(this);
        return target;
    }

    @Override public boolean isValid() {
        return true; // though in a limitted context
    }

    @Override public Buffer getBuffer() {
        return ! isActiveFilemark() ? null : mark.getBuffer();
    }

    @Override
    public String toString()
    {
        return "Filemark{" + dump(this) + '}';
    }

    private static boolean isValidMarkName(String markName) {
        return markName.length() == 1
                && markName.charAt(0) >= 'A' && markName.charAt(0) <= 'Z';
    }

    /** Spin through the filemarks and hook up this buffer if found.
     */
    private static void openBuf(Buffer buf) {
        dbg.printf(Level.FINEST, () -> sf("FM: openBuf %s\n",
                   getFS().getDisplayFileName(buf)));
        // might be some sort of nomad
        if(buf.getFile() == null)
            return;
        File file = buf.getFile().getAbsoluteFile();
        for(Filemark fm : map.values()) {
            if(!fm.isActiveFilemark() && fm.f.equals(file)) {
                fm.hookup(buf);
            }
        }
    }

    private static void closeBuf(Buffer buf) {
        dbg.printf(Level.FINEST, () -> sf("FM: closeBuf %s\n",
                   getFS().getDisplayFileName(buf)));
        for(Filemark fm : map.values()) {
            if(fm.isActiveFilemark() && fm.mark.getBuffer().equals(buf)) {
                fm.unhookup(buf);
            }
        }
    }

    private static Filemark read_viminfo_filemark(String markName)
    throws BackingStoreException {
        if (!prefsFM.nodeExists(markName))
            return null;
        assert isValidMarkName(markName);

        // set the node for the specific filemark
        Preferences prefs = prefsFM.node(markName);
        dbg.printf(Level.CONFIG, () -> sf("FM: Read viminfo prefs %s\n", dump(prefs)));

        String fName = prefs.get(MarkOps.FNAME, null);
        int l = prefs.getInt(MarkOps.LINE, -1);
        int c = prefs.getInt(MarkOps.COL, -1);
        int o = prefs.getInt(MarkOps.OFFSET, -1);
        if (fName == null || l < 0 || c < 0 || o < 0) {
            dbg.printf(Level.SEVERE, () -> sf("FM: Read viminfo malformed prefs\n"));
            return null;
        }
        File f = new File(fName);
        Filemark fm = new Filemark(markName, f, l, c, o);
        checkFM(markName, fm);
        dbg.printf(Level.CONFIG, () -> sf("FM: Read viminfo fm %s\n", dump(fm)));
        return fm;
    }

    private static boolean compare(Filemark fm, Preferences prefs) {
        String fName = prefs.get(MarkOps.FNAME, null);
        int l = prefs.getInt(MarkOps.LINE, -1);
        int c = prefs.getInt(MarkOps.COL, -1);
        int o = prefs.getInt(MarkOps.OFFSET, -1);
        return fm.f.getAbsolutePath().equals(fName)
                && fm.line == l && fm.col == c && fm.offset == o;
    }

    private static String dump(Preferences prefs)
    {
        return dump("pFM", prefs.name(),
                    prefs.get(MarkOps.FNAME, "null"),
                    prefs.getInt(MarkOps.LINE, -1),
                    prefs.getInt(MarkOps.COL, -1),
                    prefs.getInt(MarkOps.OFFSET, -1));
    }

    private static String dump(Filemark fm)
    {
        if(fm == null)
            return " FM: null";
        String fName = fm.f != null ? fm.f.getAbsolutePath() : "null";
        return dump(" FM", fm.markName, fName, fm.line, fm.col, fm.offset);
    }

    private static String dump(String tag, String markName,
                               String fName, int line, int col, int offset)
    {
        return TextUtil.sf("%s:%s (%d,%d,%d) n:%s",
                           tag, markName, line, col, offset, fName);
    }

    static void issueFM(String tag, Filemark fm, Throwable t) {
        Preferences prefs = prefsFM.node(fm.markName);
        String msg = sf("FM: issue %s\n    %s\n    %s\n",
                                          tag, dump(fm), dump(prefs));
        dbg.printf(Level.SEVERE, msg);
        JOptionPane.showMessageDialog(null, msg, "Filemark problem",
                                      JOptionPane.ERROR_MESSAGE);
    }

    /** debug ... */
    static void checkFM(String markName, Filemark fm) {
        if(!ViManager.isDebugAtHome())
            return;
        if(!markName.equals("A"))
            return;
        try {
            if(fm.line <= 1) {
                issueFM("checkFM", fm, null);
                return;
            }
            if (prefsFM.nodeExists(markName)) {
                Preferences prefs = prefsFM.node(markName);
                if(prefs.getInt(MarkOps.LINE, -1) <= 1) {
                    issueFM("checkFM", fm, null);
                }
            }
        } catch(BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    //////////////////////////////////////////////////////////////////////
    //
    // A filemark have very limited functionality, mostly unsupported

    @Override
    public int getOriginalColumnDelta()
    {
        throw new UnsupportedOperationException();
    }

    @Override public void setMark(ViFPOS fpos) {
        throw new UnsupportedOperationException();
    }

    @Override public void invalidate() {
        throw new UnsupportedOperationException();
    }

    @Override public void set(int line, int column) {
        throw new UnsupportedOperationException();
    }

    @Override public void set(int offset) {
        throw new UnsupportedOperationException();
    }

    @Override public void set(ViFPOS fpos) {
        throw new UnsupportedOperationException();
    }

    @Override public void setColumn(int col) {
        throw new UnsupportedOperationException();
    }

    @Override public void incColumn() {
        throw new UnsupportedOperationException();
    }

    @Override public void decColumn() {
        throw new UnsupportedOperationException();
    }

    @Override public void incLine() {
        throw new UnsupportedOperationException();
    }

    @Override public void decLine() {
        throw new UnsupportedOperationException();
    }

    @Override public void setLine(int line) {
        throw new UnsupportedOperationException();
    }

    @Override public ViFPOS copy() {
        throw new UnsupportedOperationException();
    }

    @Override public void verify(Buffer buf) {
        throw new UnsupportedOperationException();
    }

    @Override public int compareTo(ViFPOS o) {
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
