/*
 * Buffer.java
 *
 * Created on March 5, 2007, 11:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi.core;

import java.awt.EventQueue;
import com.raelity.jvi.lib.CharTab;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViBuffer;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViOptionBag;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.text.RegExp;
import com.raelity.text.RegExpJava;
import com.raelity.text.TextUtil.MySegment;
import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.raelity.jvi.core.lib.Constants.*;
import static java.lang.Math.min;

/**
 * Buffer: structure that holds information about one file, primarily
 * per file options.
 *
 * Several windows can share a single Buffer.
 *
 * @author erra
 */
public abstract class Buffer implements ViBuffer, ViOptionBag {
    private static final Logger LOG = Logger.getLogger(Buffer.class.getName());

    /** Each buffer gets a unique and invariant number */
    private static int fnum;

    private boolean didFirstInit;
    
    private int share; // the number of text views sharing this buffer
    public int getShare() { return share; }
    public void addShare() { share++; }
    public void removeShare() { share--; }
    public boolean singleShare() { return share == 1; }
    
    /** Creates a new instance of Buffer, initialize values from Options.
     * NOTE: tv is not completely "constructed".
     */
    public Buffer(ViTextView tv) {
        //
        // create the well known marks
        //
        b_visual_start = createMark(null);
        b_visual_end = createMark(null);
        b_op_start = createMark(null);
        b_op_end = createMark(null);
        b_last_insert = createMark(null);
        for(int i = 0; i < b_namedm.length; i++)
            b_namedm[i] = createMark(null);
        b_fnum = ++fnum;
    }
    
    /** from switchto */
    @Override
    public void activateOptions(ViTextView tv) {
        if(!didFirstInit) {
            firstGo();
            didFirstInit = true;
        }
    }

    /**
     * Put stuff here that should run once
     * after after construction and every things is setup (curbuf, curwin).
     * <br/>initOptions
     * <br/>modeline
     */
    protected void firstGo()
    {
        //
        // init options
        //
        b_p_ts = Options.getOption(Options.tabStop).getInteger();
        b_p_sts = Options.getOption(Options.softTabStop).getInteger();
        b_p_sw = Options.getOption(Options.shiftWidth).getInteger();
        b_p_et = Options.getOption(Options.expandTabs).getBoolean();
        b_p_tw = Options.getOption(Options.textWidth).getInteger();
        b_p_nf = Options.getOption(Options.nrFormats).getString();

        b_p_isk = Options.getOption(Options.isKeyWord).getString();
        b_chartab = new CharTab();
        b_chartab.init(b_p_isk);

        //b_p_mps = Options.getOption(Options.matchPairs).getString();
        //b_p_qe = Options.getOption(Options.quoteEscape).getString();

        //
        // modeline
        //
        Options.processModelines();
    }

    @Override
    public void viOptionSet(ViTextView tv, String name) {
        if("b_p_isk".equals(name)) {
            b_chartab.init(b_p_isk);
        }
    }
    
    //////////////////////////////////////////////////////////////////////
    //
    // Declare the variables referenced as part of a ViOptionBag
    //
    
    public boolean b_p_et;      // expand tabs
    public int b_p_sw;          // shiftw width
    public int b_p_ts;          // tab stop
    public int b_p_sts;         // soft tab stop
    public int b_p_tw;          // text width
    public String b_p_isk;      // is key word pattern
    public CharTab b_chartab;   // for b_p_isk
    
    //////////////////////////////////////////////////////////////////////
    //
    // Other per buffer variables
    //

    public int b_fnum;

    // The lower case marks
    ViMark b_namedm[] = new ViMark[26];
    
    // Save the current VIsual area for '< and '> marks, and "gv"
    public final ViMark b_visual_start;
    public final ViMark b_visual_end;
    public final ViMark b_last_insert;
    public char b_visual_mode;
    public String b_p_nf;

    public String b_p_qe = "\\"; // NEEDSWORK: make an option
    // NOTE: Following meainingful only when internal findmatch is used.
    public String b_p_mps = "(:),{:},[:]"; // NEEDSWORK: make an option

    // start and end of an operator, also used for '[ and ']
    public final ViMark b_op_start;
    public final ViMark b_op_end;

    //////////////////////////////////////////////////////////////////////
    //
    //


    @Override
    public ViMark getMark(char c) {
        // NEEDSWORK: buf.getMark, handle all per buf marks
        if (Util.islower(c)) {
            int i = c - 'a';
            return b_namedm[i];
        }
        //assert this == G.curbuf; // NEEDSWORK: getMark assuming correct curwin
        if(this != G.curbuf) {
            ViManager.dumpStack("this != curbuf");
            return null;
        }
        return MarkOps.getmark(c, false);
    }


    @Override
    final public ViFPOS createFPOS(int offset)
    {
        FPOS fpos = new FPOS(this);
        fpos.set(this, offset);
        return fpos;
    }


    @Override
    final public String getDisplayFileName() {
        return ViManager.getFactory().getFS().getDisplayFileName(this);
    }

    
    /**
     * In the future, to support multiple file modifiers, could take a File
     * as an argument, and return a File. Or take a String which is the list
     * of options.
     *
     * VIM: ":help filename-modifiers"
     * 
     * NEEDSWORK: missing options, only one option handled
     */
    @SuppressWarnings("fallthrough")
    @Override
    public String modifyFilename(char option) {
        File fi = getFile();
        String filename = "";
        if(fi != null) {
            switch (option) {
            case 'p':
                filename = fi.getAbsolutePath();
                break;
            case 'e':
            {
                String s = fi.getName(); // last component of name
                int idx = s.lastIndexOf('.');
                filename = idx > 0 && idx != s.length() - 1
                            ? s.substring(idx + 1)
                            : s;
                break;
            }
            case 'r':
            {
                String parent = fi.getParent();
                String s = fi.getName(); // last component of name
                int idx = s.lastIndexOf('.');
                // if has a '.' and its not the first character
                if(idx > 0)
                    s = s.substring(0,idx);
                filename = parent == null
                            ? s
                            : parent + File.separator + s;
                break;
            }
            case 't':
                filename = fi.getName();
                break;
            case 'h':
                if(fi.isAbsolute()) {
                    filename = fi.getParent();
                    break;
                }
                // FALLTHROUGH
            case ' ':
                filename = fi.getPath();
                break;
            default:
                filename = fi.getPath()
                        + ":" + new String(new char[] {option});
                break;
            }
            
            if(G.p_ssl.getBoolean()){
                // Shellslash is on, replace \ with /
                filename = filename.replace('\\','/');
            }
        }
        return filename;
    }
    
    //////////////////////////////////////////////////////////////////////
    //
    //

    protected void docInsert(int offset, String s) {
        GetChar.docInsert(offset, s);
    }

    protected void docRemove(int offset, int length, String s) {
        GetChar.docRemove(offset, length, s);

    }

    //////////////////////////////////////////////////////////////////////
    //
    // undo/redo
    //
    // The jVi undo/redo API provide for two undo/redo situations;
    // one for insert mode and one for programmatic/command/redo/macro.
    // This is painful if you need to use it...
    //
    // This default implementation for
    //          do_beginUndo, do_endUndo, do_beginInsertUndo, do_endInsertUndo
    // provide an interface that ignores the insert/programmatic differences.
    // They channel everything into beginAnyUndo/endAnyUndo which can
    // be overriden to use the simpler interface.
    //
    protected void do_runUndoable(Runnable r) {
        G.dbgUndo.printf("{Buffer:RunUndoable: \n");
        r.run();
    }

    @Override
    public void readOnlyError(final ViTextView tv)
    {
        Util.beep_flush();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                tv.getStatusDisplay().displayErrorMessage(
                        "Can not modify write protected area or file."
                        );
            }
        });
    }

    private boolean fCommandUndo;
    private boolean fInsertUndo;

    @Override
    public void do_beginUndo() {
        G.dbgUndo.printf("{Buffer:do_beginUndo: \n");
        // NEEDSWORK: standalone like: ((AbstractDocument)doc).writeLock();
        assert !fCommandUndo;
        fCommandUndo = true;
        checkBeginAnyUndo();
    }

    @Override
    public void do_endUndo() {
        // NEEDSWORK: standalone like: ((AbstractDocument)doc).writeUnlock();
        assert fCommandUndo;
        checkEndAnyUndo();
        fCommandUndo = false;
        G.dbgUndo.printf("}Buffer:do_endUndo: \n");
    }

    @Override
    public void do_beginInsertUndo() {
        G.dbgUndo.printf("{Buffer:do_beginInsertUndo: \n");
        assert !fInsertUndo;
        fInsertUndo = true;
        checkBeginAnyUndo();
    }

    @Override
    public void do_endInsertUndo() {
        assert fInsertUndo;
        checkEndAnyUndo();
        fInsertUndo = false;
        G.dbgUndo.printf("}Buffer:do_endInsertUndo: \n");
    }

    private void checkBeginAnyUndo() {
        if(fCommandUndo && fInsertUndo)
            return;
        beginAnyUndo();
    }

    private void checkEndAnyUndo()
    {
        if(fCommandUndo && fInsertUndo)
            return;
        endAnyUndo();
    }

    protected void beginAnyUndo()
    {
    }

    protected void endAnyUndo()
    {
    }

    //////////////////////////////////////////////////////////////////////
    //
    // Visual Mode and Highlight Search document algorithms
    //

    ////////////////////
    //
    // Visual Mode
    //

    /**
     * Save visual marks, and adjust the line numbers for folding.
     */
    void saveVisualMarks(TextView win) {
        // VISUAL FOLD HANDLING
        ViFPOS visual = G.VIsual.copy();
        ViFPOS fpos = win.w_cursor.copy();

        int visual_col = visual.getColumn();
        int fpos_col = fpos.getColumn();
        Normal.foldAdjustVisual(win, this, visual, fpos);
        // restore the column position; one got didled by p_sel, sigh.
        visual.setColumn(visual_col);
        fpos.setColumn(fpos_col); 

        b_visual_start.setMark(visual);
        b_visual_end.setMark(fpos);
        b_visual_mode = G.VIsual_mode;
    }

    private final VisualBounds visualBounds = new VisualBounds();

    private VisualBounds getVisualBounds() {
        return visualBounds;
    }
    
    /** Calculate the 4 boundary points for a visual selection.
     * NOTE: in block mode, startOffset or endOffset may be off by one,
     *       but they should not be used, left/right are correct.
     * <p>
     * NEEDSWORK: cache this by listening to all document/caret changes OR
     *            if only called when update is needed, then no problem
     * </p><p>
     * NEEDSWORK: revisit to include TAB logic (screen.c:768 wish found sooner)
     */
    public static class VisualBounds {
        private char visMode;
        private int startOffset, endOffset;
        // following are line and column information
        private int startLine, endLine;
        private int left, right; // column numbers (not line offset, consider TAB)
        private int wantRight; // either MAXCOL or same as right
        private boolean valid; // the class may not hold valid info

        public char getVisMode() {
            return visMode;
        }

        public int getStartLine() {
            return startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public int getLeft() {
            return left;
        }

        public int getRight() {
            return right;
        }

        /*VisualBounds(boolean init) {
            if(init && G.VIsual_active) {
                init(G.VIsual_mode, G.VIsual, getWCursor().copy());
            }
        }
        
        /*VisualBounds(int mode, ViFPOS startPos, ViFPOS cursorPos) {
            init(mode, startPos, cursorPos);
        }
        
        void init() {
            valid = false;
            if(G.VIsual_active) {
                init(G.VIsual_mode, G.VIsual, getWCursor().copy());
            }
        }*/

        public void clear() {
            valid = false;
        }

        //void init(char visMode, ViFPOS startPos, ViFPOS cursorPos) {
        public void init(char visMode, ViFPOS startPos, ViFPOS cursorPos,
                         boolean wantMax, Buffer buf, ViTextView win) {
            ViFPOS start, end; // start.offset less than end.offset
            
            this.visMode = visMode;
            
            if(startPos.compareTo(cursorPos) < 0) {
                start = startPos;
                end = cursorPos;
            } else {
                start = cursorPos;
                end = startPos;
            }
            // VISUAL FOLD HANDLING
            Normal.foldAdjustVisual(win, buf, start, end);
            startOffset = start.getOffset();
            endOffset = end.getOffset();
            
            startLine = start.getLine();
            endLine = end.getLine();
            
            //
            // set left/right columns
            //
            if(visMode == (0x1f & (int)('V'))) { // block mode
                // comparing this to screen.c,
                // this.start is from1,to1
                // this.end   is from2,to2
                // this is pretty much verbatim from screen.c:782
                MutableInt from = new MutableInt();
                MutableInt to = new MutableInt();
                
                int from1,to1,from2,to2;
                Misc.getvcol(buf, start, from, null, to);
                from1 = from.getValue();
                to1 = to.getValue();
                Misc.getvcol(buf, end, from, null, to);
                from2 = from.getValue();
                to2 = to.getValue();
                
                if(from2 < from1)
                    from1 = from2;
                if(to2 > to1) {
                    if(G.p_sel.charAt(0) == 'e' && from2 - 1 >= to1)
                        to1 = from2 - 1;
                    else
                        to1 = to2;
                }
                to1++;
                left = from1;
                right = to1;
                wantRight = wantMax ? MAXCOL : right;
            } else {
                left = start.getColumn();
                right = end.getColumn();
                if(left > right) {
                    int t = left;
                    left = right;
                    right = t;
                }
                
                
                if(G.p_sel.charAt(0) == 'i' // if inclusive, include the end
                        || left == right    // always display at lest one char
                ) {
                    endOffset++;
                    right++;
                }
                wantRight = right;
            }
            
            this.valid = true;
        }
    }

    void clearVisualState() {
        getVisualBounds().clear();
    }

    VisualBounds calcCurrentVisualBounds()
    {
        // VISUAL FOLD HANDLING
        assert this == G.curbuf;
        if(!G.VIsual_active) {
            return null;
        }
        VisualBounds vb = getVisualBounds();
        
        vb.init(G.VIsual_mode, G.VIsual, G.curwin.w_cursor.copy(),
                G.curwin.w_curswant == MAXCOL, G.curbuf, G.curwin);
        return vb;
    }

    String getVisualSelectStateString() {
        assert this == G.curbuf;
        VisualBounds vb = getVisualBounds();
        if(!G.VIsual_active) {
            vb.clear();
            return "";
        }
        
        vb.init(G.VIsual_mode, G.VIsual, G.curwin.w_cursor.copy(),
                G.curwin.w_curswant == MAXCOL, G.curbuf, G.curwin);
        
        int nLine = vb.getEndLine() - vb.getStartLine() + 1;
        int nCol = vb.getRight() - vb.getLeft();
        String s = null;
        char visMode = vb.getVisMode();
        if (visMode == 'v') { // char mode
            s = "" + (nLine == 1 ? nCol : nLine);
        } else if (visMode == 'V') { // line mode
            s = "" + nLine;
        } else if (visMode == (0x1f & (int)('V'))) { // block mode
            s = "" + nLine + "x" + nCol;
        }

        return s;
    }

    @Override
    public int[] getVisualSelectBlocks(ViTextView tv,
                                       int startOffset, int endOffset) {
        TextView win = (TextView) tv;
        VisualBounds vb = getVisualBounds();
        if (G.drawSavedVisualBounds) {
            vb.init(b_visual_mode, b_visual_start, b_visual_end,
                    false, this, win);
        } else if(G.VIsual_active) {
            vb.init(G.VIsual_mode, G.VIsual, win.w_cursor.copy(),
                    ((TextView)tv).w_curswant == MAXCOL, this, win);
        } else {
            vb.clear();
        }
        return calculateVisualBlocks(vb, startOffset, endOffset);
  }
    
    // NEEDSWORK: OPTIMIZE: re-use blocks array
    public int[] calculateVisualBlocks(VisualBounds vb,
            int startOffset,
            int endOffset) {
        if(!vb.valid)
            return new int[] { -1, -1};
        
        int[] newHighlight = null;
        if (vb.visMode == 'V') { // line selection mode
            // make sure the entire lines are selected
            newHighlight = new int[] {
                getLineStartOffset(vb.startLine), getLineEndOffset(vb.endLine),
                -1, -1};
        } else if (vb.visMode == 'v') {
            newHighlight = new int[] { vb.startOffset, vb.endOffset, -1, -1 };
        } else if (vb.visMode == (0x1f & 'V')) { // visual block mode
            int startLine = getLineNumber(startOffset);
            int endLine = getLineNumber(endOffset -1);
            
            if(vb.startLine > endLine || vb.endLine < startLine)
                newHighlight = new int[] { -1, -1};
            else {
                startLine = Math.max(startLine, vb.startLine);
                endLine = min(endLine, vb.endLine);
                newHighlight = new int[(((endLine - startLine)+1)*2) + 2];
                
                MutableInt left = new MutableInt();
                MutableInt right = new MutableInt();
                int i = 0;
                for (int line = startLine; line <= endLine; line++) {
                    int offset = getLineStartOffset(line);
                    int len = getLineEndOffset(line) - offset;
                    if(getcols(line, vb.left, vb.wantRight, left, right)) {
                        newHighlight[i++] = offset + min(len, left.getValue());
                        newHighlight[i++] = offset + min(len, right.getValue());
                    } else {
                        newHighlight[i++] = offset + min(len, vb.left);
                        newHighlight[i++] = offset + min(len, vb.wantRight);
                    }
                }
                newHighlight[i++] = -1;
                newHighlight[i++] = -1;
            }
        } else {
            throw new IllegalStateException("Visual mode: "+ G.VIsual_mode +" is not supported");
        }
        return newHighlight;
    }
    
    /** This is the inverse of getvcols, given startVCol, endVCol determine
     * the cols of the corresponding chars so they can be highlighted. This means
     * that things can look screwy when there are tabs in lines between the first
     *and last lines, but that's the way it is in swing.
     * NEEDSWORK: come up with some fancy painting for half tab highlights.
     */
    private boolean getcols(int lnum,
            int vcol1, int vcol2,
            MutableInt start, MutableInt end) {
        int incr = 0;
        int vcol = 0;
        int c1 = -1, c2 = -1;
        
        int ts = b_p_ts;
        MySegment seg = getLineSegment(lnum);
        int col = 0;
        for (int ptr = seg.offset; ; ++ptr, ++col) {
            char c = seg.array[ptr];
            // A tab gets expanded, depending on the current column
            if (c == TAB)
                incr = ts - (vcol % ts);
            else {
                //incr = CHARSIZE(c);
                incr = 1; // assuming all chars take up one space except tab
            }
            vcol += incr;
            if(c1 < 0 && vcol1 < vcol)
                c1 = col;
            if(c2 < 0 && (vcol2 -1) < vcol)
                c2 = col + 1;
            if(c1 >= 0 && c2 >= 0 || c == '\n')
                break;
        }
        if(start != null)
            start.setValue(c1 >= 0 ? c1 : col);
        if(end != null)
            end.setValue(c2 >= 0 ? c2 : col);
        return true;
    }

    ////////////////////
    //
    // Highlight Search
    //
    
    MySegment highlightSearchSegment = new MySegment();
    int[] highlightSearchBlocks = new int[2];
    MutableInt highlightSearchIndex = new MutableInt();
    
    public int[] getHighlightSearchBlocks(int startOffset, int endOffset) {
        Pattern highlightSearchPattern = null;
        highlightSearchBlocks = new int[20];
        RegExp re = Search.getLastRegExp();
        if(re instanceof RegExpJava) {
            // NEEDSWORK: speed the following up
            highlightSearchPattern = ((RegExpJava)re).getPattern();
        }

        highlightSearchIndex.setValue(0);
        if(highlightSearchPattern != null && Options.doHighlightSearch()) {
            int len = getLength();
            if(startOffset > len)
                startOffset = len;
            if(endOffset > len)
                endOffset = len;
            getSegment(startOffset, endOffset - startOffset, highlightSearchSegment);
            Matcher m = highlightSearchPattern.matcher(highlightSearchSegment);
            while(m.find()) {
                highlightSearchBlocks = addBlock(highlightSearchIndex,
                                                 highlightSearchBlocks,
                                                 m.start() + startOffset,
                                                 m.end() + startOffset);
            }
        }
        return addBlock(highlightSearchIndex, highlightSearchBlocks, -1, -1);
    }
    
    protected final int[] addBlock(MutableInt idx, int[] blocks,
            int start, int end) {
        int i = idx.getValue();
        if(i + 2 > blocks.length) {
            // Arrays.copyOf introduced in 1.6
            // blocks = Arrays.copyOf(blocks, blocks.length +20);
            int[] t = new int[blocks.length + 20];
            System.arraycopy(blocks, 0, t, 0, blocks.length);
            blocks = t;
        }
        blocks[i] = start;
        blocks[i+1] = end;
        idx.setValue(i + 2);
        return blocks;
    }
    
    public static void dumpBlocks(String tag, int[] b) {
        System.err.print(tag + ":");
        for(int i = 0; i < b.length; i += 2)
            System.err.print(String.format(" {%d,%d}", b[i], b[i+1]));
        System.err.println("");
    }

    @Override
    public String toString()
    {
        return "Buffer{" + getDisplayFileName() + '}';
    }
    
}

// vi: set sw=4 ts=8:
