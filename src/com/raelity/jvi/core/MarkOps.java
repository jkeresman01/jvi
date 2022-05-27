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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.google.common.eventbus.Subscribe;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.ViTextView.MARKOP;
import com.raelity.jvi.core.ColonCommands.AbstractColonAction;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.text.MySegment;

import static java.util.logging.Level.*;

import static com.raelity.jvi.ViTextView.MARKOP.NEXT;
import static com.raelity.jvi.ViTextView.MARKOP.PREV;
import static com.raelity.jvi.ViTextView.MARKOP.TOGGLE;
import static com.raelity.jvi.core.Util.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.Messages.*;
import static com.raelity.jvi.manager.ViManager.eatme;
import static com.raelity.jvi.manager.ViManager.getFactory;
import static com.raelity.text.TextUtil.sf;

/**
 * Keep track of vi marks.
 */
class MarkOps
{
    private static final Logger LOG = Logger.getLogger(MarkOps.class.getName());
    //static { LOG.setLevel(ALL); }

    private static final String PREF_MARKS = "marks";

    // private static List<String> oldPersistedBufferMarks = new ArrayList<>();

    /** This constant indicates mark is in other file. o is the FM. */
    final static SpecialMark otherFile = new SpecialMark();
    final static SpecialMark errorMark = new SpecialMark();
        /** This class indicates special case situation. */
        static class SpecialMark extends FPOS implements ViMark {
        ViFPOS o; // to embed some kind of mark
        int flag; // and some other info

        @Override public int getLine() { return flag; }
        @Override public int getOffset() { return -1; } // equals fail

        @Override public void setMark(ViFPOS fpos) { }
        @Override public void invalidate() { }
        @Override public int getOriginalColumnDelta() { return 0; }
        }

    private static PreferencesImportMonitor marksImportCheck;
    private static BufferMarksPersist.EventHandlers bufferMarkPersistINSTANCE;

    private MarkOps()
    {
    }

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=6)
    public static class Init implements ViInitialization
    {
        @Override
        public void init()
        {
            MarkOps.init();
        }
    }

    private static void init() {

        ColonCommands.register("marks", "marks", new DoMarks(), null);
        ColonCommands.register("delm", "delmarks", new ExDelmarks(), null);

        bufferMarkPersistINSTANCE = new BufferMarksPersist.EventHandlers();
        ViEvent.getBus().register(bufferMarkPersistINSTANCE);
    }

    private static void startImportCheck()
    {
        marksImportCheck = PreferencesImportMonitor.getMonitor(
                getFactory().getPreferences(), PREF_MARKS);
        try {
            getFactory().getPreferences().flush();
        } catch(BackingStoreException ex) {
            LOG.log(SEVERE, null, ex);
        }
    }

    /** Set the indicated mark to the current cursor position;
     * if anonymous mark characters, then handle that.
     */
    static int setmark(char c, int count)
    {
        {
            // Handle anonymous mark operations here

            MARKOP op = null;
            switch (c) {
                case '.': op = TOGGLE; break;
                case '<': op = PREV;   break; // Nothing to do with visual mode
                case '>': op = NEXT;   break; // Nothing to do with visual mode
            }
            if (op != null) {
                if (op != TOGGLE)
                    setpcmark();
                G.curwin.anonymousMark(op, count);

                return OK;
            }
        }

        if (c == '\'' || c == '`') {
            setpcmark();
            /* keep it even when the cursor doesn't move */
            G.curwin.w_prev_pcmark.setMark(G.curwin.w_pcmark);
            return OK;
        }

        if (Util.islower(c)) {
            int i;
            i = c - 'a';
            G.curbuf.b_namedm[i].setMark(G.curwin.w_cursor);
            BufferMarksPersist.persistMark(c, G.curbuf);
            return OK;
        }

        if (Util.isupper(c)) {
            if(G.curwin.getAppView().isNomad()) {
                Msg.emsg("Can not 'mark' a nomadic editor");
                return FAIL;
            }
            ViMark mark = G.curbuf.createMark(null);
            mark.setMark(G.curwin.w_cursor);
            Filemark.create(String.valueOf(c), mark);
            return OK;
        }
        return FAIL;
    }

    private static boolean JUMPLIST_ROTATE = false;
    private static final int JUMPLISTSIZE = 50;

    static void setpcmark()
    {
        setpcmark(G.curwin.w_cursor);
    }

    static void setpcmark(ViFPOS fpos)
    {
        setpcmark(G.curwin, fpos);
    }

    static void setpcmark(ViTextView tv, ViFPOS fpos)
    {
        if (G.global_busy || G.keepingCursorInView) {
            return;
        }

        TextView win = (TextView)tv;

        win.w_prev_pcmark.setMark(win.w_pcmark);
        win.w_pcmark.setMark(fpos);

        //
        // If last used entry is not at the top, put it at the top by rotating
        // the stack until it is (the newer entries will be at the bottom).
        // Keep one entry (the last used one) at the top.
        //
        // NOTE: the jdk1.6 Deque would be nice for this.
        //
        if(JUMPLIST_ROTATE) {
            if (win.w_jumplistidx < win.w_jumplist.size()) {
                ++win.w_jumplistidx;
            }
            while (win.w_jumplistidx < win.w_jumplist.size()) {
                // rotate. take the one at the end and put it at the beginning
                ViMark tempmark = win.w_jumplist
                                .remove(win.w_jumplist.size() - 1);
                win.w_jumplist.add(0, tempmark);
                ++win.w_jumplistidx;
            }
        }

        // Put the pcmark at the end of the list.
        // (switch around the order of things from vim, since we're working
        // with a more flexible data structure)
        win.w_jumplist.add((ViMark)win.w_pcmark.copy());
        if (win.w_jumplist.size() > JUMPLISTSIZE) {
            win.w_jumplist.remove(0);
        }
        // The idx points past the end
        win.w_jumplistidx = win.w_jumplist.size();
    }

    /**
     * checkpcmark()
     *             To change context, call setpcmark(), then move the current
     *		   position to where ever, then call checkpcmark().  This
     *		   ensures that the previous context will only be changed if
     *		   the cursor moved to a different line. -- webb.
     *		   If pcmark was deleted (with "dG") the previous mark is
     *		   restored.
     */
    static void checkpcmark()
    {
        if (check_mark(G.curwin.w_prev_pcmark, false) == OK
                && (G.curwin.w_pcmark.equals(G.curwin.w_cursor)
                    || check_mark(G.curwin.w_pcmark, false) == FAIL)) {
            G.curwin.w_pcmark.setMark(G.curwin.w_prev_pcmark);
            G.curwin.w_prev_pcmark.invalidate();
        }
    }

    /**
     * move "count" positions in the jump list (count may be negative)
     */
    static ViMark movemark(int count)
    {
        cleanup_jumplist();
        // NOTE: return otherFile if...
        if(G.curwin.w_jumplist.isEmpty())     // nothing to jump to
            return null;
        for(;;) {
            if(G.curwin.w_jumplistidx + count < 0
                    || G.curwin.w_jumplistidx + count
                                >= G.curwin.w_jumplist.size())
                return null;
            if(G.curwin.w_jumplistidx == G.curwin.w_jumplist.size()) {
                MarkOps.setpcmark();
                G.curwin.w_jumplistidx--;
                if(G.curwin.w_jumplistidx + count < 0)
                    return null;
            }

            G.curwin.w_jumplistidx += count;

            ViMark mark = G.curwin.w_jumplist.get(G.curwin.w_jumplistidx);
            // if(false /*mark.isSomeOtherFile()*/) {
            //     // ...
            //     return null; // return otherFile;
            // }
            return mark;
        }
    }

    /*
     * getmark(c) - find mark for char 'c'
     *
     * Return pointer to FPOS if found (caller needs to check lnum!)
     *	  NULL if there is no mark called 'c'.
     *	  -1 if mark is in other file (only if changefile is TRUE)
     */
    static ViMark getmark(char c, boolean changefileOK)
    {
        ViMark m = null;
        if (c == '\'' || c == '`') {
            // make a copy since it might change soon
            m = (ViMark) G.curwin.w_pcmark.copy();
        } else if (c == '[') {
            m = G.curbuf.b_op_start;
        } else if (c == ']') {
            m = G.curbuf.b_op_end;
        } else if (c == '<' || c == '>') {
            ViMark startp = G.curbuf.b_visual_start;
            ViMark endp = G.curbuf.b_visual_end;
            // If either mark's no good, return it and let error handling report it
            if (check_mark(startp, false) == FAIL)
                return startp;
            if (check_mark(endp, false) == FAIL)
                return endp;
            boolean isLtName = c == '<';
            boolean isLtValue = startp.compareTo(endp) < 0;
            m = isLtName == isLtValue ? startp : endp;
            /*
             * For Visual line mode, set mark at begin or end of line
             */
            if (G.curbuf.b_visual_mode == 'V') {
                // the questions is whether column 0 or last column
                int col = 0;
                if (c == '>')
                    col = Misc.check_cursor_col(m.getLine(), MAXCOL);
                int lineoff = G.curbuf
                               .getLineStartOffsetFromOffset(m.getOffset());
                m = G.curbuf.createMark(null);
                ViFPOS fpos = G.curbuf.createFPOS(lineoff + col);
                m.setMark(fpos);
            }
        } else if (Util.islower(c)) {
            int i = c - 'a';
            m = G.curbuf.b_namedm[i];
        } else if(Util.isupper(c) /* || Util.isdigit(c) */) { // named file mark
            Filemark fm = Filemark.get(String.valueOf(c));
            File f = G.curbuf.getFile();
            if(fm != null && f != null) {
                ViMark result;
                if(!f.equals(fm.getFile())) {
                    if(changefileOK) {
                        boolean ok = getFactory().getFS()
                                .edit(fm.getFile().toPath(), false, null);
                        if(ok) {
                            otherFile.o = fm; // started transition to fm
                            result = otherFile;
                        } else {
                            errorMark.flag = -1; // error, can't open file
                            result = errorMark;
                        }
                    } else {
                        errorMark.flag = 0; // fm in different file, !changefileOK
                        result = errorMark;
                    }
                } else
                    // in same file, treat fm like a regular mark
                    result = fm;

                // NOTE: after getFS.edit() in progress switch to file.
                //m = fm;
                m = result;
            }
        }
        return m;
    }

    static boolean isValidMark(char c, ViBuffer buf)
    {
        if(buf == null || (buf = G.curbuf) == null)
                return false;
        ViMark mark = buf.getMark(c);
        return mark != null ? mark.isValid() : false;
    }

    /**
     * Check a if a position from a mark is valid.
     * Give and error message and return FAIL if not.
     */
    static int check_mark(ViMark mark)
    {
        return check_mark(mark, true);
    }

    static int check_mark(ViMark mark, boolean messageOK)
    {
        //  if(!mesasgeOK) return mark.isValid() ? OK : FAIL;
        String msg = null;
        if (mark == null) {
            msg = e_umark;
        } else if(!mark.isValid()) {
            msg = e_marknotset;
        } else if(mark.getLine() <= 0) {
            // lnum is negative if mark is in another file can can't get that
            // file, error message already give then.
            if(mark.getLine() < 0)
                return FAIL;
            msg = e_marknotset;
        } else {
            try {
                if (mark.getLine() > G.curbuf.getLineCount()) {
                    msg = e_markinval;
                }
            } catch (ViMark.MarkOrphanException e) {
                msg = e_marknotset;
            } catch (ViMark.MarkException e) {
                msg = e_marknotset;
            }
        }
        if (msg != null) {
            if (messageOK) {
                Msg.emsg(msg);
            }
            return FAIL;
        }
        return OK;
    }

    static void do_jumps()
    {
        cleanup_jumplist();
        try (ViOutputStream vios = ViManager.createOutputStream("Jump List")) {
            vios.println(" jump line  col file/text");
            for(int i = 0; i < G.curwin.w_jumplist.size(); i++) {
                ViMark mark = G.curwin.w_jumplist.get(i);
                if(check_mark(mark, false) == OK) {
                    MySegment seg = G.curbuf.getLineSegment(mark.getLine());
                    String lineText = seg.subSequence(0, seg.length()-1).toString();
                    vios.println(String.format("%c %2d %5d %4d %s",
                            i == G.curwin.w_jumplistidx ? '>' : ' ',
                            i > G.curwin.w_jumplistidx ? i - G.curwin.w_jumplistidx
                                    :  G.curwin.w_jumplistidx -i,
                            mark.getLine(),
                            mark.getColumn(),
                            lineText.trim()));
                }
            }
            if(G.curwin.w_jumplistidx == G.curwin.w_jumplist.size())
                vios.println(">");
        }
    }

    /**
     * When deleting lines, this may create duplicate marks in the
     * jumplist. They will be removed here for the current window.
     * <p>
     * Remove an element if an element later in the list is on the same line.
     * </p>
     */
    private static void cleanup_jumplist()
    {
        TextView win = G.curwin;

        ViMark indexedMark = null;
        if(win.w_jumplistidx < win.w_jumplist.size())
            indexedMark = win.w_jumplist.get(win.w_jumplistidx);
        boolean grabNextIndexedMark = false;

        ListIterator<ViMark> it = win.w_jumplist.listIterator();
        while(it.hasNext()) {
            ViMark mark = it.next();
            if(grabNextIndexedMark) {
                indexedMark = mark;
                grabNextIndexedMark = false;
            }
            if(!it.hasNext())
                break;
            int checkLine = mark.getLine(); // fnum = mark.getFnum();
            for(ViMark i : win.w_jumplist.subList(it.nextIndex(),
                                                  win.w_jumplist.size())) {
                if(checkLine == i.getLine() /* fnum == i.fnum */) {
                    // found the same line later in the list
                    if(mark == indexedMark) {
                        // removing indexed item, index the item following it
                        grabNextIndexedMark = true;
                    }
                    it.remove();
                    break;
                }
            }
        }

        win.w_jumplistidx = indexedMark == null
                ? win.w_jumplist.size()
                : win.w_jumplist.indexOf(indexedMark);
    }

    /**
     * print the marks
     */
    private static class DoMarks extends AbstractColonAction
    {
        @Override
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;

            String arg = null;
            if(cev.getNArg() > 0)
                arg = cev.getArgString();
            show_one_mark('\'', arg, G.curwin.w_pcmark, null, true);
            for(int i = 0; i < G.curbuf.b_namedm.length; i++)
                show_one_mark((char)(i+'a'), arg, G.curbuf.b_namedm[i],
                              null, true);
            for (char markName = 'A'; markName <= 'Z'; markName++) {
                Filemark fm = Filemark.get(String.valueOf(markName));
                String name;
                if(fm == null) {
                    name = null;
                } else if(fm.getBuffer() != null)
                    name = fm_getname(fm, 15);
                else
                    name = fm.getFile().getPath();
                if(name != null) {
                    assert fm != null;
                    show_one_mark(markName, arg, fm, name,
                            fm.getBuffer() == G.curbuf);
                }
            }
            //show_one_mark('"', arg, G.curbuf.b_last_cursor, null, true);
            show_one_mark('[', arg, G.curbuf.b_op_start, null, true);
            show_one_mark(']', arg, G.curbuf.b_op_end, null, true);
            show_one_mark('^', arg, G.curbuf.b_last_insert, null, true);
            //show_one_mark('.', arg, G.curbuf.b_last_change, null, true);
            show_one_mark('<', arg, G.curbuf.b_visual_start, null, true);
            show_one_mark('>', arg, G.curbuf.b_visual_end, null, true);
            show_one_mark(MySegment.DONE, arg, null, null, false);
        }
    }

    /**
     * Get name of file from a filemark.
     * When it's in the current buffer, return the text at the mark.
     * Returns an allocated string.
     */
    private static String fm_getname(Filemark fm, int lead_len) {
        if(fm.getBuffer() == G.curbuf)
            return mark_line(fm, lead_len);
        return fm.getFile().getPath();
    }

    /**
     * Return the line at mark "mp".  Truncate to fit in window.
     * The returned string has been allocated.
     */
    private static String mark_line(ViFPOS mp, int lead_len) {
        String s;
        eatme(lead_len);
        if(mp instanceof Filemark && mp.getBuffer() == null
                || mp.getOffset() > mp.getBuffer().getLength())
            return "-invalid-";

        // Forget the lead_len stuff

        s = mp.getBuffer().getLineSegment(mp.getLine()).toString();
        s = s.substring(Misc.skipwhite(s, 0));
        // get rid of the newline
        // and arbitrarily truncate to 60 chars
        final int truncateTo = 60;
        if(s.length() > truncateTo)
            s = s.substring(0, truncateTo);
        else if(s.length() > 0)
            s = s.substring(0, s.length() -1);
        return s;
    }
    private static boolean did_title;
    private static ViOutputStream svios;
    private static void show_one_mark(
            char c, String arg, ViMark p, String name, boolean current) {
        if(c == MySegment.DONE) {
            if(did_title) {
                did_title = false;
                svios.close();
                svios = null;
            } else {
                if(arg == null)
                    Msg.smsg("No marks set");
                else
                    Msg.emsg("No marks mastching \"" + arg + "\"");
            }
        }
        else if((arg == null || vim_strchr(arg, 0, c) >= 0)
                && p != null
                        // filemark always usable for this simple case
                && (p instanceof Filemark || p.isValid())) {
            if(!did_title) {
                /* Highlight title */
                String heading = "\nmark line  col file/text";
                svios = ViManager.createOutputStream(heading);
                did_title = true;
            }
            if (true /*!got_int*/)
            {
                String s = String.format(" %c %6d %4d ",
                        c, p.getLine(), p.getColumn());
                if (name == null && current)
                {
                    name = mark_line(p, 15);
                }
                if (name != null)
                {
                    s += name;
                }
                svios.println(s);
            }
        }
    }

    /**
     * clrallmarks() - clear all marks in the buffer 'buf'
     *
     * Used mainly when trashing the entire buffer during ":e" type commands
     */
    static void clrallmarks(Buffer buf) {
        for (ViMark b_namedm : buf.b_namedm) {
            b_namedm.invalidate();
        }
        buf.b_op_start.invalidate();		/* start/end op mark cleared */
        buf.b_op_end.invalidate();
        // buf.b_last_cursor.lnum = 1;	/* '" mark cleared */
        // buf.b_last_cursor.col = 0;
        buf.b_last_insert.invalidate();	/* '^ mark cleared */
        // buf.b_last_change.lnum = 0;	/* '. mark cleared */
    // #ifdef FEAT_JUMPLIST
    //     buf->b_changelistlen = 0;
    // #endif
    }

    private static class ExDelmarks extends AbstractColonAction
    {
        @Override public EnumSet<CcFlag> getFlags() {
            return EnumSet.of(CcFlag.BANG);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ex_delmarks((ColonEvent)e);
        }
    }

    /**
     * ":delmarks[!] [marks]"
     */
    private static void ex_delmarks(ColonEvent cev) {

        if (cev.getNArg() == 0 && cev.isBang())
            // clear all marks
            clrallmarks(G.curbuf);
        else if (cev.isBang())
            Msg.emsg(e_invarg);
        else if (cev.getNArg() == 0)
            Msg.emsg(e_argreq);
        else
        {
            // clear specified marks only
            String arg = cev.getArgString();
            for (int p = 0; p < arg.length(); p++) {
                boolean lower = Util.islower(arg.charAt(p));
                boolean digit = Util.isdigit(arg.charAt(p));
                if (lower || digit || Util.isupper(arg.charAt(p)))
                {
                    char from;
                    char to = 0;
                    if (p + 1 < arg.length() && arg.charAt(p+1) == '-')
                    {
                        // clear range of marks
                        from = arg.charAt(p);
                        if(p + 2 < arg.length())
                            to = arg.charAt(p + 2);
                        if (!(lower ? Util.islower(to)
                                    : (digit ? Util.isdigit(to)
                                        : Util.isupper(to)))
                                || to < from)
                        {
                            Msg.emsg(e_invarg2 + arg.substring(p));
                            return;
                        }
                        p += 2;
                    }
                    else
                        // clear one lower case mark
                        from = to = arg.charAt(p);

                    for (int i = from; i <= to; ++i)
                    {
                        if (lower)
                            G.curbuf.b_namedm[i - 'a'].invalidate();
                        else
                        {
                            int n;
                            //if (digit)
                            //    n = i - '0' + NMARKS;
                            //else
                                n = i - 'A';
                            eatme(n);
                            Filemark.deleteMark(String.valueOf((char)i));
                        }
                    }
                }
                else
                    switch (arg.charAt(p))
                    {
                        //case '"': G.curbuf.b_last_cursor.invalidate(); break;
                        case '^': G.curbuf.b_last_insert.invalidate(); break;
                        //case '.': G.curbuf.b_last_change.invalidate(); break;
                        case '[': G.curbuf.b_op_start.invalidate(); break;
                        case ']': G.curbuf.b_op_end.invalidate(); break;
                        case '<': G.curbuf.b_visual_start.invalidate(); break;
                        case '>': G.curbuf.b_visual_end.invalidate(); break;
                        case ' ': break;
                        default:  Msg.emsg(e_invarg2 + arg.substring(p));
                                  return;
                    }
            }
        }
    }

    static final String FNAME = "fName";
    //static final String BUFTAG = "buftag";
    static final String BUF = "buf";
    static final String OFFSET = "offset";
    static final String LINE = "line";
    static final String COL = "col";

    /**
     * The named marks, a-z plus a few, for a buffer are persisted between
     * sessions. There is an option for how many buffers to perist, but in
     * all cases any buffers open in a session are persisted, so there may
     * be more than specified by the option. When the number of buffers
     * in a session is less that the option, then marks from previous sessions
     * are persisted in an MRU fashion.
     */
    private static class BufferMarksPersist {
        // NEEDSWORK: BufferMarksPersist easier using google collections
        // Data structure, on/off disk, notes
        // The pref node for a buffer is an arbitrary name starting with "BUF"
        // and the MRU index is a property of that node.

        private static final String INDEX = "index";

        /** All the BufferMarks being tracked. Key'd by file name. */
        static Map<String, BufferMarksHeader> all = new HashMap<>();
        /** The BufferMarks read during startup in MRU order */
        static List<String> prev = new LinkedList<>();
        /** The BufferMarks to persist from this session in MRU order */
        static List<String> next = new LinkedList<>();
        static List<String> cleanup = new ArrayList<>();

        static Preferences prefs = getFactory().getPreferences().node(PREF_MARKS);

        private BufferMarksPersist()
        {
        }

        static Random random = new Random();

        static class MarkInfo {
            int offset, line, col;

            MarkInfo(int offset, int line, int col) {
                this.offset = offset;
                this.line = line;
                this.col = col;
            }
        }

        /** info about each buffer with persisted marks */
        private static class BufferMarksHeader {
            /** file name */
            private String fname;
            /** index, starting at 1, for MRU list. 
             * A new header is assigned 1, there may be multiple 1's
             */
            private int index;
            /** the random name of the persisted set of marks */
            private String tag;

            public BufferMarksHeader(String tag, String fname, int index) {
                this.tag = tag;
                this.fname = fname;
                this.index = index;
            }

            public BufferMarksHeader(String tag, String name) {
                this(tag, name, 1);
            }

            public String getFname() {
                return fname;
            }

            public void setIndex(int index) {
                this.index = index;
            }

            public int getIndex() {
                return index;
            }

            public String getBufTag() {
                return tag;
            }
        }

        static void persistMark(char markName, Buffer buf)
        {
            bufferMarkPersistINSTANCE.persist(buf, null, markName);
        }

        /** EventBus listeners */
        private static class EventHandlers
        {

        @Subscribe
        public void startup_read_marks(ViEvent.Boot ev) {
            read_viminfo();
            startImportCheck();
        }

        /** Persist the marks for the buffer. */
        @Subscribe
        public void leaveTv(ViEvent.SwitchFromTv ev)
        {
            Buffer buf = ev.getTv().getBuffer();
            if(buf != null)
                persist(buf, ev, null);
        }

        /** Persist the marks for the buffer. */
        @Subscribe
        public void wroteBuf(ViEvent.DirtyBuf ev)
        {
            if(ev.isDirty())
                return;
            persist(ev.getBuf(), ev, null);
        }

        /** Persist the marks for the buffer. */
        @Subscribe
        public void closeBuf(ViEvent.CloseBuf ev)
        {
            persist(ev.getBuf(), ev, null);
        }

        private void persist(Buffer buf, ViEvent ev, Character markName)
        {
        if(marksImportCheck.isChange()) {
            LOG.info(() -> sf("jVi marks imported: %s", ev));
                return;
            }

            if(buf.getFile() == null || !buf.isActive())
                return;
            LOG.log(FINE, "persist {0}", buf.getFile().getAbsolutePath());
            try {
                BufferMarksHeader bmh;
                String fname = buf.getFile().getAbsolutePath();

                hasMarks:
                if(markName == null || !isValidMark(markName, buf)) {
                    for(char c = 'a'; c <= 'z'; c++) {
                        if(buf.getMark(c).isValid())
                            break hasMarks;
                    }
                    // no marks to persist
                    bmh = all.get(fname);
                    if(bmh != null) {
                        Preferences bufData = prefs.node(bmh.getBufTag());
                        bufData.removeNode();
                        all.remove(fname);
                    }
                    return;
                } // hasMarks
                bmh = all.get(fname);
                if(bmh == null) {
                    String bufTag = createBufTag();
                    bmh = new BufferMarksHeader(bufTag, fname);
                    all.put(fname, bmh);
                }
                // make this buf's marks the most recently used
                next.remove(fname);
                next.add(0, fname);
                prev.remove(fname); // shouldn't be in prev list anymore
                writeBufferMarks(bmh, buf, markName);
            } catch(BackingStoreException ex) {
                LOG.log(SEVERE, null, ex);
            }
            try {
                prefs.flush();
            } catch(BackingStoreException ex) {
                LOG.log(SEVERE, null, ex);
            }
        }

        /** put the previously saved marks into the buffer */
        @Subscribe
        public void restore(ViEvent.OpenBuf ev)
        {
            Buffer buf = ev.getBuf();
            if(buf.getFile() == null)
                return;
            try {
                String name = buf.getFile().getAbsolutePath();
                BufferMarksHeader bmh = all.get(name);
                LOG.log(FINE, "restore {0}{1}", new Object[]{bmh == null
                        ? "NOT " : "", buf.getFile().getAbsolutePath()});
                if (bmh == null) {
                    return;
                }
                Preferences bufData = prefs.node(bmh.getBufTag());
                String[] marks = bufData.childrenNames();
                LOG.log(FINER, "restoring {0} marks from {1}",
                        new Object[]{marks.length, bufData.absolutePath()});
                for (String mName : marks) {
                    MarkInfo mi = readMark(bufData.node(mName));
                    if (mi == null) {
                        // following was warning, but it fires too much
                        // wonder why...., but its ignored.
                        LOG.config(String.format("restore: "
                                + "bad mark: %s, name: %s", mName, name));
                        continue;
                    }
                    char c = mName.charAt(0);
                    if (!Util.islower(c)) {
                        continue;
                    }
                    FPOS fpos = new FPOS(buf);
                    fpos.set(buf, mi.line, mi.col, true);
                    buf.getMark(c).setMark(fpos);
                }
            } catch (BackingStoreException ex) {
                LOG.log(SEVERE, null, ex);
            }
        }

        /** read all the buffer mark stuff */
        private void read_viminfo()
        {
            if(!prev.isEmpty())
                return; // only do it once
            // If all is correct, then the indexes for the bm are in the
            // range 1-n. If not then MRU info is lost.
            // Simply toss out any bufs that cause issues.
            try {
                String[] bufTags = prefs.childrenNames(); // existing bufs
                Set<String> fnames = new HashSet<>();
                // following holds bufs in MRU order
                BufferMarksHeader[] bmHeaders = new BufferMarksHeader[bufTags.length];
                LOG.log(FINER, "persisted file count: {0}",
                        bufTags.length);
                for (String bufTag : bufTags) {
                    if (!bufTag.startsWith(BUF)) {
                        LOG.log(WARNING, "read_viminfo: "
                                + "invalid buffer tag ''{0}''", bufTag);
                        cleanup.add(bufTag);
                        continue;
                    }
                    Preferences bufData = prefs.node(bufTag);
                    String fname = bufData.get(FNAME, null);
                    int index = bufData.getInt(INDEX, -1);
                    if (fname == null || !fnames.add(fname)) {
                        bufData.removeNode();
                        LOG.log(WARNING, "read_viminfo: "
                                + "ignoring duplicate ''{0}''", fname);
                        cleanup.add(bufTag);
                        continue;
                    }
                    LOG.log(FINER, "Node: {0} name: {1} index: {2}",
                            new Object[]{bufTag, fname, index});
                    if (index <= 0 || index > bmHeaders.length) {
                        LOG.warning(String.format("read_viminfo: "
                                + "(expect 1-%d) bad index: %d, name: %s",
                                bmHeaders.length, index, fname));
                        // Shouldn't cleanup if not way too big, maybe shutdown issues,
                        // but if not, then what? Maybe add to all and continue?
                        // if(index > G.viminfoMaxBuf)
                        cleanup.add(bufTag);
                        continue;
                    }
                    if (bmHeaders[index - 1] != null) {
                        if(index != 1) {
                            LOG.warning(String.format("read_viminfo: "
                                    + "duplicate index: %d, name: %s", index, fname));
                            cleanup.add(bufTag);
                            continue;
                        }
                        // Duplicates of index 1 are allowed.
                    }
                    // Duplicates of index 1 are allowed.
                    // They can arise in this scenario:
                    // - First mark created in buffer persists
                    //   and gives buf index 1
                    // - shutdown/crash without marks being persisted
                    // So treat something with index == 1 as most MRU
                    // And make sure not to replace and existing entry
                    // in bmHeaders.
                    BufferMarksHeader bmh = new BufferMarksHeader(bufTag, fname, index);
                    if(bmHeaders[index - 1] == null)
                        bmHeaders[index - 1] = bmh;
                    else
                        prev.add(fname);
                    all.put(fname, bmh);
                }
                // establish the MRU list as read
                for (BufferMarksHeader bmh : bmHeaders) {
                    if (bmh == null) {
                        continue;
                    }
                    prev.add(bmh.getFname());
                }
            } catch (BackingStoreException ex) {
                LOG.log(SEVERE, null, ex);
            }
        }

        /**
         * Update all the indexes for any buffers that have been persisted.
         * First persist buffers that are still be open.
         */
        @Subscribe
        public void write_viminfo(ViEvent.Shutdown ev)
        {
            try {
                LOG.fine("write_viminfo entry");
                for (Buffer buf : getFactory().getBufferSet()) {
                    persist(buf, ev, null);
                }
                int max = G.viminfoMaxBuf;
                int index = 1;
                // first handle all buffers that were open this session.
                LOG.log(FINE, "write_viminfo next count {0}", next.size());
                for (String name : next) {
                    writeBufferMarksHeader(name, index);
                    index++;
                }
                // now put out buffers from previous sessions
                LOG.log(FINE, "write_viminfo prev count {0}", prev.size());
                for (String name : prev) {
                    // 2nd arg of 0 will cause removal
                    writeBufferMarksHeader(name, index <= max ? index : 0);
                    index++;
                }
                for (String x : cleanup) {
                    Preferences p = prefs.node(x);
                    p.removeNode();
                    LOG.log(FINE, "cleanup {0}", x);
                }

                prefs.flush();
            } catch(BackingStoreException ex) {
                LOG.log(SEVERE, null, ex);
            }
        }

        } // EventHandlers

        // NOTE: index <= 0 means remove the associated data.
        /** write the header for this buffer's marks. */
        private static void writeBufferMarksHeader(String name, int index)
                throws BackingStoreException
        {
                LOG.log(FINER, "write next {0} index {1}",
                        new Object[]{name, index});
                BufferMarksHeader bmh = all.get(name);
                if(bmh == null) {
                    LOG.warning(String.format(
                            "writeBufferMarksHeader: "
                            + "prev name: %s but no Buffermarks", index, name));
                } else {
                    Preferences bufData = prefs.node(bmh.getBufTag());
                    if(index > 0) {
                        bufData.putInt(INDEX, index);
                    } else {
                        bufData.removeNode();
                    }
                }
        }

        private static MarkInfo readMark(Preferences p)
        {
            int o, l, c;
            o = p.getInt(OFFSET, -1);
            l = p.getInt(LINE, -1);
            c = p.getInt(COL, -1);
            LOG.log(FINER, "{3}: offset: {0} line: {1} col: {2}",
                    new Object[]{o, l, c, p.name()});
            if(o < 0 || l < 0 || c < 0) {
                return null;
            }

            return new MarkInfo(o, l, c);
        }

        private static void writeMark(Preferences p, ViMark m) {
            if(m == null || !m.isValid())
                return;
            p.putInt(OFFSET, m.getOffset());
            p.putInt(LINE, m.getLine());
            p.putInt(COL, m.getColumn());
        }

        private static void writeBufferMarks(BufferMarksHeader bmh,
                                             Buffer buf,
                                             Character markName)
                throws BackingStoreException
        {
            Preferences bufData = prefs.node(bmh.getBufTag());
            bufData.put(FNAME, bmh.getFname());
            bufData.putInt(INDEX, bmh.getIndex());
            if(markName != null)
                writeMark(bufData.node(String.valueOf(markName)),
                          buf.getMark(markName));
            else
                for(char c = 'a'; c <= 'z'; c++) {
                    ViMark m = buf.getMark(c);
                    writeMark(bufData.node(String.valueOf(c)), m);
                }
        }

        /** Create a buf name that does not currently exist. */
        private static String createBufTag() {

            // So I guess 400 is the max persistable files
            // assuming tryId does not repeat (which is not the case)
            for(int i = 0; i < 400; i++) {
                int tryId = random.nextInt(1_000_000_000);
                String tag = BUF + tryId;
                try {
                    if (!prefs.nodeExists(tag)) {
                        return tag;
                    }
                } catch (BackingStoreException ex) {
                    LOG.log(SEVERE, null, ex);
                    break;
                }
            }

            return null;
        }
    }

}

// vi:set sw=4:
