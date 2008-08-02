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

import com.raelity.text.TextUtil.MySegment;
import java.util.ListIterator;
import static com.raelity.jvi.ViTextView.MARKOP;
import static com.raelity.jvi.ViTextView.MARKOP.NEXT;
import static com.raelity.jvi.ViTextView.MARKOP.PREV;
import static com.raelity.jvi.ViTextView.MARKOP.TOGGLE;

import static com.raelity.jvi.Constants.*;
import static com.raelity.jvi.Messages.*;

/**
 * Keep track of vi marks.
 */
class MarkOps
{
    /** This constant indicates mark is in other file. */
    final static FPOS otherFile = new FPOS();

    /** Set the indicated mark to the current cursor position;
     * if anonymous mark characters, then handle that.
     * <b>NEEDSWORK:</b><ul>
     * <li>Only handle lower case marks.
     * </ul>
     */
    static int setmark(char c, int count)
    {
        Normal.do_xop("setmark");

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
            return OK;
        }

        if (false && Util.isupper(c)) {	// NEEDSWORK: upper case marks
            // i = c - 'A';
            // namedfm[i].mark = curwin.w_cursor;
            // namedfm[i].fnum = curbuf.b_fnum;
            // return OK;
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
        if (G.global_busy) {
            return;
        }

        Window win = (Window)tv;

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
    static ViFPOS movemark(int count)
    {
        cleanup_jumplist();
        // NOTE: return otherFile if...
        if(G.curwin.w_jumplist.size() == 0)     // nothing to jump to
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
            if(false /*mark.isSomeOtherFile()*/) {
                // ...
                return otherFile;
            }
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
    static ViMark getmark(char c, boolean changefile)
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
            boolean isLtName = c == '<' ? true : false;
            boolean isLtValue = startp.compareTo(endp) < 0 ? true : false;
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
                m = G.curbuf.createMark();
                ViFPOS fpos = G.curbuf.createFPOS(lineoff + col);
                m.setMark(fpos);
            }
        } else if (Util.islower(c)) {
            int i = c - 'a';
            m = G.curbuf.b_namedm[i];
        }
        // NEEDSWORK: else if isupper(c) || vim_isdigit(c) // named file mark
        return m;
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
        String msg = null;
        if (mark == null) {
            msg = e_umark;
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
        ViOutputStream vios = ViManager.createOutputStream(
                null, ViOutputStream.OUTPUT, "Jump List");

        vios.println(" jump line  col file/text");
        for(int i = 0; i < G.curwin.w_jumplist.size(); i++) {
            ViMark mark = G.curwin.w_jumplist.get(i);
            if(check_mark(mark, false) == OK) {
                String name;
                // name = fm_getname(&curwin->w_jumplist[i], 16);
                name = "filename";
                if(name == null)
                    continue;
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
        vios.close();
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
        Window win = G.curwin;

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

}

// vi:set sw=4:
