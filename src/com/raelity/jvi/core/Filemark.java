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

import java.io.File;
import java.util.logging.Logger;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import com.google.common.eventbus.Subscribe;

import org.openide.util.Exceptions;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.*;

import static java.util.logging.Level.*;

import static com.raelity.jvi.manager.ViManager.dialogEx;
import static com.raelity.jvi.manager.ViManager.getFS;
import static com.raelity.jvi.manager.ViManager.getFactory;
import static com.raelity.text.TextUtil.sf;

/** A mark that can be persisted. The mark is not fully functional if
 * the associated file is not opened in a window.
 * <p>
 * When the associated file is not opened, only getOffset() and such works.
 * </p>
 * This file has two parts, a bunch of static functions used to manipulate
 * marks, read/write files, checking buf opens/closes. And this class is
 * also an immutable object.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Filemark implements ViMark { // NEEDSWORK: extends File
    private static final Logger LOG = Logger.getLogger(MarkOps.class.getName());

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
            dbg = Options.getDebugOption(Options.dbgMarks);
            ViEvent.getBus().register(new EventHandlers());
        }
    }

    private static final String PREF_FILEMARKS = "filemarks";
    private static PreferencesImportMonitor filemarksImportCheck;
    private static DebugOption dbg;
    private static Preferences prefsFM;
    private static final ValueMap<String, Filemark> map
            = new ValueHashMap<>((Filemark fm) -> fm.getMarkName(), 26);
    
    static Filemark create(String markName, ViMark mark)
    {
        assert isValidMarkName(markName);
        if(!isValidMarkName(markName)) {
            LOG.severe("invalid mark name");
            return null;
        }
        Filemark fm = new Filemark(markName, mark);
        dbg.printf(INFO, () -> sf("FM: Create %s\n", dump(fm)));
        Filemark prevFM = map.put(fm);
        fm.persist();
        if(prevFM != null) {
            dbg.println(FINEST, () -> sf("FM: replace %s", dump(prevFM)));
        }
        return fm;
    }

    static Filemark get(String markName) {
        Filemark fm = map.get(markName);
        checkFM(markName, fm);
        return fm;
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
            dbg.printf(INFO, () -> sf("FM: deleteMark %s\n", markName));
        if(fmExist ^ prefsExist) {
            final boolean prefsExistF = prefsExist;
            dbg.printf(SEVERE, () -> sf(
                    "FM: deleteMark WARNING %s fmExist=%b prefsExist=%b\n",
                    markName, fmExist, prefsExistF));
        }
    }

    private static class EventHandlers
    {

    @Subscribe
    public void read_viminfo_filemarks(ViEvent.Boot ev) {
        dbg.println(INFO, () -> "FM: " + ev);
        prefsFM = getFactory().getPreferences().node(PREF_FILEMARKS);
        filemarksImportCheck = PreferencesImportMonitor.getMonitor(
                getFactory().getPreferences(), PREF_FILEMARKS);

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

    @Subscribe
    public void shutdown(ViEvent.Shutdown ev) {
        dbg.println(INFO, () -> "FM: " + ev);
        if(getFactory().hasPreShutdown())
            return;
        write_viminfo_filemarks(null);
    }

    @Subscribe
    public void write_viminfo_filemarks(ViEvent.PreShutdown ev) {
        dbg.println(INFO, () -> "FM: " + ev);
        if(filemarksImportCheck.isChange()) {
            dbg.println(INFO, "FM: jVi filemarks imported");
            return;
        }

        dbg.printf(INFO, "FM: write filemarks\n");
        // could delete stuff that isn't in the map
        for(Filemark tfm : map.values()) {
            Filemark fm = tfm;
            if(fm.isActiveFilemark() && !compareShadow(fm, fm.mark)) {
                dbg.printf(WARNING, () -> sf("FM: shutdown external change %s\n", dump(tfm)));
                fm = new Filemark(fm.markName, fm.mark);
            }
            fm.persist();
        }
    }

    /** Spin through the filemarks and hook from buffer if found.
     */
    @Subscribe
    public void openBuf(ViEvent.OpenBuf ev) {
        dbg.println(INFO, () -> "FM: " + ev);
        Buffer buf = ev.getBuf();

        dbg.printf(FINEST, () -> sf("FM: openBuf %s\n",
                   getFS().getDebugFileName(buf)));
        // might be some sort of nomad
        if(buf.getFile() == null)
            return;
        File file = buf.getFile().getAbsoluteFile();
        for(Entry<String, Filemark> entry : map.entrySet()) {
            String key = entry.getKey();
            Filemark fm = entry.getValue();
            assert key.equals(fm.markName);
            checkFM(key, fm);
            if(!fm.isActiveFilemark() && fm.f.equals(file)) {
                Filemark newFm = new Filemark(fm.markName, hookupMark(fm, buf));
                entry.setValue(newFm);
                newFm.persist();
                if(!compareShadow(fm, newFm.mark))
                    // fpos changed while file not open
                    dbg.printf(SEVERE, () -> sf("FM: hookup re-boundary %s\n",
                                                 dump(fm)));
                dbg.printf(CONFIG, () -> sf("FM: hookup buf %s\n", dump(newFm)));
            }
            checkFM(key, entry.getValue());
        }
    }

    /** Spin through the filemarks and unhook from buffer if found.
     * NOTE: the closeBuf event seems to be too late occasionally;
     * All the marks have offset 0. Use switch from tv
     */
    @Subscribe
    public void leavingTv(ViEvent.SwitchFromTv ev) {
        Buffer buf = ev.getTv().getBuffer();
        persistBuf(buf, ev);
    }

    /** Spin through the filemarks and unhook from buffer if found.
     * NOTE: In NB the closeBuf event does not have a valid doc.
     * All the marks have offset 0. Use switch from tv
     */
    @Subscribe
    public void writeBuf(ViEvent.DirtyBuf ev) {
        if(ev.isDirty())
            return;
        Buffer buf = ev.getBuf();
        persistBuf(buf, ev);
    }
    

    /** Spin through the filemarks and unhook from buffer if found.
     * NOTE: In NB the closeBuf event does not have a valid doc.
     * Occasionally all the marks have offset 0. Use switch from tv and write.
     */
    public void closeBuf(ViEvent.CloseBuf ev) {
        Buffer buf = ev.getBuf();
        persistBuf(buf, ev);
    }


    /** Spin through the filemarks and unhook from buffer if found. */
    private void persistBuf(Buffer buf, ViEvent ev)
    {
        if(filemarksImportCheck.isChange()) {
            dbg.println(INFO, "FM: jVi filemarks imported");
            return;
        }
        dbg.println(INFO, () -> sf("FM: (act:%s) %s",
                                   buf != null ? buf.isActive() : "NONE",
                                   ev));
        if(buf == null)
            return;
        if(buf.getFile() == null) // something in a zip file
            return;

        for(Entry<String, Filemark> entry : map.entrySet()) {
            String key = entry.getKey();
            Filemark fm = entry.getValue();
            assert key.equals(fm.markName);
            checkFM(key, fm);

            if(ViManager.isDebugAtHome()) {
                if(!buf.isActive() && !(ev instanceof ViEvent.CloseBuf))
                    dbg.printf(SEVERE, () -> sf(
                        "FM: close ERROR: NOT CLOSE and inactive buffer: fm.f %s\n",
                        dump(fm)));
                if(!fm.f.isAbsolute())
                    dbg.printf(SEVERE, () -> sf(
                        "FM: close ERROR: NOT ABSOLUTE: fm.f %s\n", dump(fm)));
                if(!buf.getFile().isAbsolute())
                    dbg.printf(SEVERE, () -> sf(
                        "FM: close ERROR: NOT ABSOLUTE: buf %s\n", dump(fm)));
                if(fm.isActiveFilemark() && buf.isActive()
                            && !fm.mark.getBuffer().getFile().isAbsolute())
                    dbg.printf(SEVERE, () -> sf(
                        "FM: close ERROR: NOT ABSOLUTE: fm %s\n", dump(fm)));
            }
            if(!fm.isActiveFilemark() && fm.f.equals(buf.getFile())) {
                // This is impossible, the filemark is for this buf
                // but it is not active.
                dbg.printf(SEVERE, () -> sf("FM: close ERROR: buffer not active%s\n",
                                            dump(fm)));
            }
            if(fm.isActiveFilemark() && fm.getBuffer().equals(buf)
                    || fm.getFile().equals(buf.getFile())) {
                if(ev instanceof ViEvent.CloseBuf) {
                    // persist an FM going inactive
                    // NOTE: in CloseBuf, use location in fm (not mark)
                    dbg.printf(FINER, () -> sf("FM: unhookup %s\n", dump(fm)));
                    if(fm.isActiveFilemark() || !compareShadow(fm))
                        entry.setValue(new Filemark(fm, buf.getFile()));
                } else {
                    // filemark is staying active
                    if(!compareShadow(fm, fm.mark))
                        entry.setValue(new Filemark(fm.markName, fm.mark));
                }
                entry.getValue().persist();
            }
            checkFM(key, fm);
        }
    }

    } // EventHandler

    private static Filemark read_viminfo_filemark(String markName)
    throws BackingStoreException {
        if (!prefsFM.nodeExists(markName))
            return null;
        assert isValidMarkName(markName);

        // set the node for the specific filemark
        Preferences prefs = prefsFM.node(markName);
        dbg.printf(CONFIG, () -> sf("FM: Read viminfo %s\n", dump(prefs)));

        String fName = prefs.get(MarkOps.FNAME, null);
        int l = prefs.getInt(MarkOps.LINE, -1);
        int c = prefs.getInt(MarkOps.COL, -1);
        int o = prefs.getInt(MarkOps.OFFSET, -1);
        if (fName == null || l < 0 || c < 0 || o < 0) {
            dbg.printf(SEVERE, () -> sf("FM: Read viminfo malformed prefs\n"));
            return null;
        }
        File f = new File(fName);
        Filemark fm = new Filemark(markName, f, l, c, o);
        checkFM(markName, fm);
        dbg.printf(CONFIG, () -> sf("FM: Read viminfo %s\n", dump(fm)));
        return fm;
    }

    private static boolean isValidMarkName(String markName) {
        return markName.length() == 1
                && markName.charAt(0) >= 'A' && markName.charAt(0) <= 'Z';
    }

    /**
     * Create a mark, hooking into the file. Use line,col. Persist it.
     */
    private static ViMark hookupMark(Filemark prevFm, Buffer buf) {
        ViMark mark = buf.createMark(null);
        FPOS fpos = new FPOS(buf);
        fpos.set(buf, prevFm.line, prevFm.col, true);
        mark.setMark(fpos);
        return mark;
    }

    // compare shadowed values in fm to mark values
    private static boolean compareShadow(Filemark fm, ViMark mark) {
        if(mark == null)
            return false;
        String fName = mark.getBuffer().getFile().getAbsoluteFile().toString();
        int l = mark.getLine();
        int c = mark.getColumn();
        int o = mark.getOffset();
        return compare(fm, fName, l, c, o);
    }

    // compare shadowed values in fm to prefs values
    private static boolean compareShadow(Filemark fm) {
        return compareShadow(fm, prefsFM.node(fm.markName));
    }

    // compare shadowed values in fm to prefs values
    // NOTE: if isActiveFM, probably should have checked against mark first
    private static boolean compareShadow(Filemark fm, Preferences prefs) {
        String fName = prefs.get(MarkOps.FNAME, null);
        int l = prefs.getInt(MarkOps.LINE, -1);
        int c = prefs.getInt(MarkOps.COL, -1);
        int o = prefs.getInt(MarkOps.OFFSET, -1);
        return compare(fm, fName, l, c, o);
    }

    private static boolean compare(Filemark fm, String fName, int l, int c, int o) {
        return fm.f.toString().equals(fName)
                && fm.line == l && fm.col == c && fm.offset == o;
    }

    private static String dump(Preferences prefs)
    {
        return dump("FM-prefs", prefs.name(), prefs.get(MarkOps.FNAME, "null"),
                    dump(prefs.getInt(MarkOps.LINE, -1),
                         prefs.getInt(MarkOps.COL, -1),
                         prefs.getInt(MarkOps.OFFSET, -1)));
    }

    private static String dump(Filemark fm)
    {
        if(fm == null)
            return " FM: null";
        String fName = fm.f != null ? fm.f.getAbsolutePath() : "null";
        if(fm.isActiveFilemark())
            return dump("FM-activ", fm.markName, fName,
                        dump(fm.getLine(), fm.getColumn(), fm.getOffset())
                        + " " + dump(fm.line, fm.col, fm.offset));
        else
            return dump("FM-inact", fm.markName, fName,
                        dump(fm.line, fm.col, fm.offset));
    }

    private static String dump(String tag, String markName,
                               String fName, String vals)
    {
        return sf("%s:%s %s n:%s",
                  tag, markName, vals, fName);
    }

    private static String dump(int line, int col, int offset)
    {
        return sf("(%d,%d,%d)", line, col, offset);
    }

    static void reportIssueFM(String tag, Filemark fm, Throwable t) {
        if(t != null)
            Exceptions.printStackTrace(dialogEx(t));
        Preferences prefs = prefsFM.node(fm.markName);
        String msg = sf("FM: ISSUE %s\n    %s\n    %s\n",
                                          tag, dump(fm), dump(prefs));
        dbg.printf(SEVERE, msg);
        JOptionPane.showMessageDialog(getFactory().getMainWindow(),
                                      msg, "Filemark problem",
                                      JOptionPane.ERROR_MESSAGE);
    }

    /** debug ... */
    static void checkFM(String markName, Filemark fm) {
        if(fm == null)
            return;
        if(!ViManager.isDebugAtHome() || !dbg.getBoolean())
            return;
        if(Boolean.FALSE && !markName.equals("V"))
            return;
        try {
            if(fm.line <= 1) {
                reportIssueFM("checkFM", fm, new Throwable(
                        sf("line <= 1: %d", fm.line)));
                return;
            }
            if (prefsFM.nodeExists(markName)) {
                Preferences prefs = prefsFM.node(markName);
                if(prefs.getInt(MarkOps.LINE, -1) <= 1) {
                    reportIssueFM("checkFM", fm, new Throwable(
                            sf("prefs line <= 1: %d", prefs.getInt(MarkOps.LINE, -1))));
                }
            }
        } catch(BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    // The Filemark object
    //

    final private String markName;
    final private ViMark mark;

    // These fields are only to shadow prefs while mark is null since no buf
    final private File f;
    final private int line;
    final private int col;
    final private int offset;

    @SuppressWarnings("LeakingThisInConstructor")
    private Filemark(String markName, File f, ViMark mark,
                     int line, int col, int offset) {
        this.markName = markName;
        this.f = f.getAbsoluteFile();
        this.mark = mark;
        this.line = line;
        this.col = col;
        this.offset = offset;
        checkFM(markName, this);
    }

    /** create an active filemark */
    private Filemark(String markName, ViMark mark) {
        this(markName, mark.getBuffer().getFile(), mark,
             mark.getLine(), mark.getColumn(), mark.getOffset());
    }

    /** create an inactive filemark based on the shadow values. */
    private Filemark(Filemark fm, File fi) {
        this(fm.markName, fi, null, fm.line, fm.col, fm.offset);
    }

    /** Create an inactive filemark, no mark; typically from preferences. */
    private Filemark(String markName, File f, int line, int col, int offset) {
        this(markName, f, null, line, col, offset);
    }

    /**
     * Save this fm to prefs. Update this fm's values
     * from the mark.
     */
    private void persist() {
        if(isActiveFilemark()) {
            // the fm shadow vals should be equal to the mark values
            // otherwise persist should not have been called.
            //assert compareShadow(this, this.mark);
            if(!compareShadow(this, this.mark)) {
                dbg.printf(SEVERE, () -> sf("FM: persist shadow!=mark valid=%b %s\n",
                                            isActiveFilemark(), dump(this)));
                LOG.severe(() -> sf("FM: persist shadow!=mark valid=%b %s\n",
                                    isActiveFilemark(), dump(this)));
                Exceptions.printStackTrace(new Throwable("persist"));
                return;
            }
        }
        dbg.printf(CONFIG, () -> sf("FM: persist %s\n", dump(this)));
        Preferences prefs = prefsFM.node(markName);
        if(!compareShadow(this, prefs)) {
            // in-core vs in-prefs are different, so update the prefs.
            dbg.printf(FINE, () -> sf("FM: persist prev pref %s\n",
                                        dump(prefs)));
            prefs.put(MarkOps.FNAME, f.getAbsolutePath());
            prefs.putInt(MarkOps.LINE, getLine());
            prefs.putInt(MarkOps.COL, getColumn());
            prefs.putInt(MarkOps.OFFSET, getOffset());
        }
        checkFM(getMarkName(), this);
    }

    public File getFile() {
        return ! isActiveFilemark()
               ? f : mark.getBuffer().getFile().getAbsoluteFile();
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
}
