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

package com.raelity.jvi.swing;

import java.awt.FontMetrics;
import java.awt.Graphics;
import static java.lang.Math.round;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.core.Window;
import com.raelity.jvi.core.Util;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.Misc;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.Edit;
import com.raelity.jvi.core.G;
import com.raelity.jvi.options.BooleanOption;
import  static com.raelity.jvi.core.Constants.*;


import com.raelity.jvi.*;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.manager.Scheduler;
import com.raelity.text.TextUtil.MySegment;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JViewport;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import org.openide.util.lookup.ServiceProvider;

/**
 *  Presents a swing editor interface for use with vi. There is
 *  one of these for each JTextComponent.
 *  <p>
 *  Notice the listeners for caret changes. If the caret changes
 *  to a location that is unexpected, i.e. it came from some external
 *  source, then an externalChange message is sent to vi.
 *  </p>
 */
public class SwingTextView extends Window
        implements ViTextView, PropertyChangeListener, ChangeListener
{
    protected static final
            Logger LOG = Logger.getLogger(SwingTextView.class.getName());
    private static int genNum; // unique/invariant window id;

    protected int w_num;

    protected LineMap vm;

    protected JTextComponent editorPane;
    protected TextOps ops;
    protected Window window;

    protected ViStatusDisplay statusDisplay;

    private Point point0;
    private CaretListener cursorSaveListener;
    private int lastDot;
    private static int gen;

    private static boolean didInit;
    @ServiceProvider(service=ViInitialization.class, path="jVi/init")
    public static class Init implements ViInitialization
    {
        @Override
        public void init()
        {
            if(didInit)
                return;
            ColonCommands.register("dumpLineMap", "dumpLineMap",
                    new DumpLineMap());
            didInit = true;
        }
    }

    // ............

    public SwingTextView( final JTextComponent editorPane)
    {
        super();
        this.editorPane = editorPane;
        w_num = ++genNum;

        cursorSaveListener = new CaretListener() {
            public void caretUpdate(CaretEvent ce) {
                if(!ViManager.getFactory().isEnabled())
                    return;
                cursorMoveDetected(lastDot, ce.getDot(), ce.getMark());
                lastDot = ce.getDot();
            }
        };
    }

    public void setLineMap(LineMap vm)
    {
        assert this.vm == null;
        this.vm = vm;
    }

    private static class DumpLineMap extends ColonCommands.ColonAction
    {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent) ev;
            SwingTextView tv = (SwingTextView)cev.getViTextView();
            System.err.println(tv.vm.toString());
        }

    }

    //////////////////////////////////////////////////////////////////////
    //
    // Some swing specific things that an implementation may want to override
    //

    public Action[] getActions() {
        if(ViManager.isDebugAtHome() && editorPane instanceof JEditorPane) {
            List<Action> l1 = Arrays.asList(((
                    JEditorPane)editorPane).getEditorKit().getActions());
            List<Action> l2 = Arrays.asList((editorPane).getActions());
            if(!l1.equals(l2))
                LOG.log(Level.SEVERE, null, new Throwable(
                        "different actions: " + getBuffer().getDisplayFileName()));
        }

        return editorPane.getActions();
    }

    public EditorKit getEditorKit() {
        if(ViManager.isDebugAtHome() && editorPane instanceof JEditorPane) {
            EditorKit kit1 = ((JEditorPane)editorPane).getEditorKit();
            EditorKit kit2 = editorPane.getUI().getEditorKit(editorPane);
            if(!kit1.equals(kit2))
                LOG.log(Level.SEVERE, null, new Throwable(
                        "different kit" + getBuffer().getDisplayFileName()));
        }

        return editorPane.getUI().getEditorKit(editorPane);
    }


    public ViFPOS createWCursor()
    {
        return w_cursor == null ? new WCursor() : null;
    }

    public ViAppView getAppView()
    {
        return ViManager.getFactory().getAppView(editorPane);
    }

    private void enableCursorSave()
    {
        lastDot = editorPane.getCaret().getDot();
        editorPane.removeCaretListener(cursorSaveListener);
        editorPane.addCaretListener(cursorSaveListener);
    }

    private void disableCursorSave()
    {
        editorPane.removeCaretListener(cursorSaveListener);
    }


    public void startup()
    {
        enableCursorSave();
    }


    @Override
    public void shutdown()
    {
        disableCursorSave();

        super.shutdown();

        shutdownMore();
        Scheduler.detached(editorPane);
        editorPane = null;
    }


    public boolean isShutdown()
    {
        return editorPane == null;
    }


    //////////////////////////////////////////////////////////////////////
    //
    // Declare the variables referenced as part of a ViOptionBag
    //

    /** jVi doesn't support these operations.
     * Keep it as a per window flag like vim.
     * The platform may do something with it.
     */
    public boolean w_p_nu;
    public boolean w_p_wrap;
    public boolean w_p_lbr;
    public boolean w_p_list;


    public void viOptionSet( ViTextView tv, String name )
    {
    }


    @Override
    public void activateOptions( ViTextView tv )
    {
        super.activateOptions(tv);
        updateHighlightSearchState();
    }


    public final SwingBuffer getBuffer()
    {
        return (SwingBuffer) w_buffer;
    }


    /**
     *  Override this class to provide a different implementations
     *  of status display.
     * @return
     */

    public void attach()
    {
        if ( ops == null ) {
            createOps();
        }
        if ( G.dbgEditorActivation.getBoolean() ) {
          System.err.println("TV.attach: " + editorPane.hashCode());
        }
        attachMore();
    }


    public void detach()
    {
        detachMore();

        Scheduler.detached(editorPane);
    }


    /**
     *  Create methods to invoke and interact with editor pane actions.
     *  May override for custom editor panes.
     */
    protected void createOps()
    {
        ops = new Ops(this);
    }


    public JTextComponent getEditorComponent()
    {
        return editorPane;
    }


    /**
     *  @return true if the text can be changed.
     */
    public boolean isEditable()
    {
        return editorPane.isEditable();
    }


    ////////////////////////////////////////////////////////////////////////
    //
    // Text modification methods.
    //
    // The text modifications are a bit jumbled. Some use actions and some go
    // to the buffer. Some use the cursor position and some use offsets.
    // All check isEditable.
    //
    // NEEDSWORK: consistent text modification methods.
    //            It will be difficult to clean this up paricularly because of the
    //            because of the dependency on actions, for example insertNewline
    //            must be used to get proper autoindent handling.
    //

    public void insertNewLine()
    {
        if ( ! isEditable() ) {
            Util.vim_beep();
            return;
        }
        ops.xop(TextOps.INSERT_NEW_LINE); // NEEDSWORK: xop throws no exception
    }

    public void insertTab()
    {
        if ( ! isEditable() ) {
            Util.vim_beep();
            return;
        }
        ops.xop(TextOps.INSERT_TAB); // NEEDSWORK: xop throws no exception
    }


    public void replaceChar( char c, boolean advanceCursor )
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return;
        }
        int offset = w_cursor.getOffset();
        getBuffer().replaceChar(offset, c);
        if ( advanceCursor ) {
            offset++;
        }
        setCaretPosition(offset);// also clears the selection
    }

    public void deletePreviousChar()
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return;
        }
        ops.xop(TextOps.DELETE_PREVIOUS_CHAR); // NEEDSWORK: xop throws no exception
    }


    /**
     *  Insert character at cursor position. For some characters
     *  special actions may be taken.
     * @param c
     */
    public void insertChar( char c )
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return;
        }
        if ( c == '\t' ) {
            if(G.usePlatformInsertTab.getBoolean())
                insertTab();
            else
                insertText(w_cursor.getOffset(), "\t");
            return;
        }
        insertTypedChar(c);
    }


    /**
     *  Add a character verbatim to the window.
     * @param c
     */
    public void insertTypedChar( char c )
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return;
        }
        ops.xop(TextOps.KEY_TYPED, String.valueOf(c)); // NEEDSWORK: xop throws no exception
    }


    //
    // NEEDSWORK: These three text modification methods take document offsets
    // unlike the rest of the modification methods which use the caret.
    // They should probably not be here. Currently they wrap some methods
    // in Buffer.
    //

    public void replaceString( int start, int end, String s )
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return;
        }
        getBuffer().replaceString(start, end, s);
    }


    public void deleteChar( int start, int end )
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return;
        }
        getBuffer().deleteChar(start, end);
    }


    public void insertText( int offset, String s )
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return;
        }
        getBuffer().insertText(offset, s);
    }


    /**
     *  Create an empty line, autoindented, either before or after current line.
     *  This is simple algorithm; but it ignores guarded text issues
     *  and code folding subtlties.
     *  <p>
     *  In this example, either the cursor was on line1 and do a NL_FORWARD,
     *  or cursor on line2 and NL_BACKWARD.
     *  The cursor is shown as '|' positioned where the newLine action will
     *  open up a clean line with proper autoindent.
     *  <pre>
     *      line1|\n
     *      line2\n
     *  </pre>
     *  This has problems if line1 is guarded text (write protected), since
     *  it modifies that line. And issues with folding in that it will open
     *  the fold since a folded line is modified.
     *  </p>
     * @param op
     * @return
     */
    public boolean openNewLine( DIR op )
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return false;
        }
        if ( op == DIR.BACKWARD && w_cursor.getLine() == 1 ) {
            // Special case if BACKWARD and at position zero of document.
            // set the caret position to 0 so that insert line on first line
            // works as well, set position just before new line of first line
            if(!Edit.canEdit(this, getBuffer(), 0))
                return false;
            w_cursor.set(0);
            insertNewLine();

            MySegment seg = getBuffer().getLineSegment(1);
            w_cursor.set(0 + Misc.coladvanceColumnIndex(MAXCOL, seg));
            return true;
        }

        int offset;
        if ( op == DIR.FORWARD ) {
            // after the current line,
            // handle current line might be a fold
            // int cmpOffset = getBuffer()
            //                .getLineEndOffsetFromOffset(w_cursor.getOffset());
            offset = getDocLineOffset(
                    getLogicalLine(w_cursor.getLine()) + 1);
        } else {
            // before the current line
            offset = getBuffer()
                    .getLineStartOffsetFromOffset(w_cursor.getOffset());
        }
        // offset is after the newline where insert happens, backup the caret.
        // After the insert newline, caret is ready for input on blank line
        offset--;
        if(!Edit.canEdit(this, getBuffer(), 0))
            return false;
        w_cursor.set(offset);
        insertNewLine();
        return true;
    }


    ///////////////////////////////////////////////////////////////////////



    public int getCaretPosition()
    {
        // NEEDSWORK: what is using this?
        return w_cursor.getOffset();
    }


    public int getMarkPosition()
    {
        return editorPane.getCaret().getMark();
    }


    public void setCaretPosition( int offset )
    {
        // NEEDSWORK: what is using this?
        editorPane.setCaretPosition(offset);
    }


    public void setSelection( int dot, int mark )
    {
        Caret c = editorPane.getCaret();
        c.setDot(mark);
        c.moveDot(dot);
    }

    public boolean hasSelection() {
        return editorPane.getSelectionStart() != editorPane.getSelectionEnd();
    }


    public void clearSelection()
    {
        Caret c = editorPane.getCaret();
        c.setDot(c.getDot());
    }


    public void findMatch()
    {
        Util.vim_beep();
    }


    public void jumpDefinition( String ident )
    {
        Util.vim_beep();
    }


    public void anonymousMark( MARKOP op, int count )
    {
        Util.vim_beep();
    }


    public void jumpList( JLOP op, int count )
    {
        Util.vim_beep();
    }


    public void foldOperation( FOLDOP op )
    {
        Util.vim_beep();
    }


    public void foldOperation( FOLDOP op, int offset )
    {
        Util.vim_beep();
    }


    public void wordMatchOperation( WMOP op )
    {
        Util.vim_beep();
    }


    public void tabOperation( TABOP op, int count )
    {
        Util.vim_beep();
    }


    public int getVpTopLogicalLine()
    {
        int logicalLine = vm.logicalLine(getVpTopLine());
        if ( G.dbgCoordSkip.getBoolean() ) {
            System.err.println("getVpViewTopLine: " + logicalLine);
        }
        return logicalLine;
    }


    public void setVpTopLogicalLine( int logicalLine )
    {
        Point2D p;
        int docLine = 1;
        int offset = 0;
        docLine = vm.docLine(logicalLine);
        offset = getBuffer().getLineStartOffset(docLine);

        try {
            p = getLocation(modelToView(offset));
        } catch (BadLocationException ex) {
            LOG.log(Level.SEVERE, null, ex);
            p = new Point(0,0);
        }

        // NEEDSWORK: may want previous line??
        double height = getLineHeight(docLine, offset);

        // If point is after viewport top && within one char of top
        // then just return, to avoid wiggle
        double topDiff = p.getY() - getViewport().getViewPosition().getY();
        if ( topDiff > 0 && topDiff < height ) {
            return;
        }
        Point p01 = makePointTruncY(p);
        p01.x = 0;
        getViewport().setViewPosition(p01);
    }


    public int getVpBottomLogicalLine()
    {
        int logicalLine = getVpTopLogicalLine() + getVpLines();
        int ll01 = getLogicalLineCount();
        if(logicalLine > ll01)
            logicalLine = ll01 + 1; // past last line
        if(G.dbgCoordSkip.getBoolean()) {
            System.err.println("getViewCoordBottomLine: " + logicalLine);
        }
        return logicalLine; // NEEDSWORK: line past full line, see getViewBottomLine
    }


    public int getLogicalLineCount()
    {
        int logicalLine = vm.logicalLine(getBuffer().getLineCount());
        if ( G.dbgCoordSkip.getBoolean() ) {
            System.err.println("getLogicalLineCount: " + logicalLine);
        }
        return logicalLine;
    }


    public int getLogicalLine(int docLine)
    {
        if(docLine > getBuffer().getLineCount()) {
            ViManager.dumpStack("line "+docLine
                                +" past "+getBuffer().getLineCount());
            docLine = getBuffer().getLineCount();
        }
        int logicalLine = vm.logicalLine(docLine);
        if ( G.dbgCoordSkip.getBoolean() ) {
            System.err.println("getLogicalLine: " + logicalLine);
        }
        return logicalLine;
    }

    public boolean cursorScreenRowEdge(EDGE edge, ViFPOS fpos)
    {
        fpos.verify(getBuffer());
        boolean ok = true;
        try {
            int offset = fpos.getOffset();
            int lineOff;
            int col;
            switch (edge) {
                case LEFT:
                    offset = Utilities.getRowStart(getEditorComponent(), offset);
                    break;

                case RIGHT:
                    lineOff = Utilities.getRowStart(getEditorComponent(), offset);
                    offset = Utilities.getRowEnd(getEditorComponent(), offset);
                    // stay out of fold
                    col = offset - lineOff; // col from begin of screen line
                    col = getFirstHiddenColumn(lineOff, col);
                    offset = lineOff + col;
                    break;

                case MIDDLE:
                    lineOff = Utilities.getRowStart(getEditorComponent(), offset);
                    Rectangle2D left = modelToView(lineOff);
                    offset = Utilities.getRowEnd(getEditorComponent(), offset);
                    col = offset - lineOff;
                    int col00 = getFirstHiddenColumn(lineOff, col);
                    // go through the "col00 != col" dance because
                    // in fold right.getMaxX() returns fold display width
                    // rather than a char width
                    if(col00 != col // into folded territory
                            && col00 > 0) //
                        --col00;
                    offset = lineOff + col00;
                    Rectangle2D right = modelToView(offset);

                    offset = viewToModel(
                            new Point2D.Double(
                                round((left.getX() + right.getMaxX()) / 2),
                                left.getCenterY()));
                    break;

                default:
                    throw new AssertionError();
            }
            if(offset >= 0)
                fpos.set(offset);
            else
                ok = false;
        } catch (BadLocationException ex) {
            LOG.log(Level.SEVERE, null, ex);
            ok = false;
        }
        return ok;
    }

    public boolean cursorScreenUpDown(DIR dir, int distance, ViFPOS fpos)
    {
        fpos.verify(getBuffer());
        boolean ok = true;
        int offset = fpos.getOffset();
        //int xx;
        //try {
        //    xx = (int)round(modelToView(offset).getX());
        //} catch (BadLocationException ex) {
        //    LOG.log(Level.SEVERE, null, ex);
        //    return false;
        //}
        int x = (int)round(w_curswant * getMaxCharWidth());

        while(distance-- > 0) {
            try {
                if(dir == DIR.BACKWARD) {
                    offset = Utilities.getPositionAbove(
                            getEditorComponent(), offset, x);
                } else { // DIR.FORWARD
                    offset = Utilities.getPositionBelow(
                            getEditorComponent(), offset, x);
                }
            } catch (BadLocationException ex) {
                LOG.log(Level.SEVERE, null, ex);
                offset = -1;
            }
            if(offset < 0) {
                ok = false;
                break;
            } else {
                fpos.set(offset);
            }
        }
        return ok;
    }

    @Override
    public int getDocLine(int logicalLine)
    {
        return vm.docLine(logicalLine);
    }

    @Override
    public int getDocLineOffset( int logicalLine )
    {
        int docLine = vm.docLine(logicalLine);
        if(docLine > getBuffer().getLineCount()) {
            return getBuffer().getLength() + 1; // just past EOF
        }
        return getBuffer().getLineStartOffset(docLine);
    }


    @Override
    public void setCursorLogicalLine( int logicalLine, int col )
    {
        //assert col == 0;
        setCaretPosition(vm.docLineOffset(logicalLine) + col);
    }


    @Override
    public int getFirstHiddenColumn( int lineOffset, int colIdx )
    {
        if(!vm.isFolding())
            return colIdx;
        JTextComponent c = getEditorComponent();
        int col = 0;
        {
            try {
                double x = modelToView(lineOffset).getX();
                for (col = 0; col <= colIdx - 1; col++) {
                    double xNext = modelToView(lineOffset + col + 1).getX();
                    if(x == xNext)
                        break; // no change, don't advance to next position
                    x = xNext;
                }
            } catch (BadLocationException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        return col;
    }


    private Element getLineElement( int lnum )
    {
        return getBuffer().getLineElement(lnum);
    }


    public void updateCursor( ViCaretStyle cursor )
    {
        if ( isShutdown() ) {
            return; // NEEDSWORK: was getting some null pointer stuff here
        }
        Caret caret = editorPane.getCaret();
        if ( caret instanceof ViCaret ) {
            ((ViCaret)caret).setCursor(cursor);
        }
    }

    //
    // NEEDSWORK: the win_* should really be somewhere else?
    // Maybe some kind of platform and/or window-manager interface?
    //

    /** Quit editing window. Can close last view.
     */
    public void win_quit()
    {
        Msg.emsg("win_quit not implemented");
    }


    /** Split this window.
     * @param n the size of the new window.
     */
    public void win_split( int n )
    {
        Msg.emsg("win_split not implemented");
    }


    /** Close this window
     * @param freeBuf true if the related buffer may be freed
     */
    public void win_close( boolean freeBuf )
    {
        Msg.emsg("win_close not implemented");
    }


    /** Close other windows
     * @param forceit true if always hide all other windows
     */
    public void win_close_others(boolean forceit)
    {
        Msg.emsg("win_close_others not implemented");
    }

    public ViStatusDisplay getStatusDisplay()
    {
        return statusDisplay;
    }

    public TextOps getOps()
    {
        return ops;
    }

    private class WCursor extends ViFPOS.abstractFPOS
    {

        public boolean isValid() {
            return true;
        }

        final public int getLine()
        {
            return getBuffer().getElemCache(getOffset()).line;
        }

        final public int getColumn()
        {
            int offset = getOffset();
            return offset - getBuffer().getElemCache(offset)
                                       .elem.getStartOffset();
        }

        final public int getOffset()
        {
            return editorPane.getCaretPosition();
        }

        @Override
        final public void set(int offset)
        {
            editorPane.setCaretPosition(offset);
        }

        final public void set(int line, int column)
        {
            //System.err.println("setPosition("+line+","+column+")");
            Element elem = getBuffer().getLineElement(line);
            int startOffset = elem.getStartOffset();
            int endOffset = elem.getEndOffset();
            int adjustedColumn = -1;

            if (column < 0) {
                adjustedColumn = 0;
            } else if (column >= endOffset - startOffset) {
                column = endOffset - startOffset - 1;
            }

            if (adjustedColumn >= 0) {
                ViManager.dumpStack("line " + line
                        + ", column " + column
                        + ", length " + (endOffset - startOffset));
                column = adjustedColumn;
            }

            editorPane.setCaretPosition(startOffset + column);
        }

        final public ViFPOS copy()
        {
            return w_buffer.createFPOS(getOffset());
        }

        final public SwingBuffer getBuffer() {
            return (SwingBuffer)w_buffer;
        }

        public void verify(Buffer buf) {
            if(w_buffer != buf)
                throw new IllegalStateException("fpos buffer mis-match");
        }

    };

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //
    // Following is the historical "TextViewCache"
    //
    // It monitors the EditorPane's Viewport
    // along with other event changes.
    //
    // NEEDSWORK: There may not be a viewport, consider a single line.
    //            May want to represent the viewport with an interface,
    //            and provide a "null" viewport when no real viewport,
    //            rather than the special case code.
    // NEEDSWORK: Without a viewport, currently assuming one line.
    //

    protected void changeDocument(PropertyChangeEvent e) {
        if (cacheTrace.getBoolean()) {
            System.err.println("doc switch: ");
        }
        point0 = null;
        super.detachBuffer();
        ViManager.getFactory().changeBuffer(this, e.getOldValue());
    }

    final static int DIR_TOP = -1;
    final static int DIR_BOT = 1;

    private static BooleanOption cacheTrace
            = (BooleanOption) Options.getOption(Options.dbgCache);

    protected JViewport getViewport()
    {
        return viewport;
    }

    /** offset should be beginning of docLine */
    private double getLineHeight(int docLine, int offset)
    {
        return getCharRect(docLine, offset).getHeight();
    }

    private Rectangle2D getCharRect(int docLine, int offset)
    {
        Rectangle2D r = new Rectangle2D.Double(0, 0, 8, 15); // arbitrary
        try {
            r = modelToView(offset);
        } catch (BadLocationException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return r;
    }
    private double getMaxCharWidth() {
        return getMaxCharBounds().getMaxX();
    }
    private Rectangle2D getMaxCharBounds() {
        Rectangle2D r = new Rectangle2D.Double(0, 0, 8, 15); // arbitrary
        Graphics g = getEditorComponent().getGraphics();
        try {
            FontMetrics fm = g.getFontMetrics();
            //r = fm.getMaxCharBounds(g);
            r = fm.getStringBounds("W", g);
        } finally {
            g.dispose();
        }
        return r;
    }

    // The visible part of the document, negative means not valid.
    // These values are updated whenever the viewport changes.
    private JViewport viewport;
    private Point viewportPosition;
    private Dimension viewportExtent;
    private int vpTopLine;
    private int vpBottomLine;
    private int vpLines;

    /** @return the top line number */
    public int getVpTopLine()
    {
        if(viewport == null)
            return 1;
        return vpTopLine;
    }

    public void setVpTopLine(int line)
    {
        if(viewport == null)
            return;
        if (line == getVpTopLine()) {
            return; // nothing to change
        }
        int offset = getBuffer().getLineStartOffset(line);
        Rectangle2D r;
        try {
            r = modelToView(offset);
        } catch (BadLocationException e) {
            Util.vim_beep();
            return;
        }
        Point p = makePointTruncY(getLocation(r));
        p.x = 0;
        viewport.setViewPosition(p);
    }

    public int getVpBlankLines()
    {
        if(viewport == null)
            return 0;

        int n;
        n = vpLines - (vm.logicalLine(vpBottomLine) - vm.logicalLine(vpTopLine) + 1);
        return n;
    }

    /** @return number of lines on viewport */
    public int getVpLines() // NEEDSWORK: variable font height
    {
        if(viewport == null)
            return 1;
        return vpLines;
    }

    public int getRequiredVpLines()
    {
        return getVpLines();
    }

    protected void fillLinePositions()
    {
        /*
        SwingUtilities.invokeLater(
        new Runnable() { public void run() { fillLinePositionsFinally(); }});
         */
        fillLinePositionsFinally();
    }

    /** determine document indicators visible in the viewport */
    protected void fillLinePositionsFinally()
    {
        Point newViewportPosition;
        Dimension newViewportExtent;
        Rectangle r;
        int newVpLines = -1;
        boolean topLineChange = false;

        if (viewport == null) {
            newViewportPosition = null;
            newViewportExtent = null;
        } else {
            newViewportPosition = viewport.getViewPosition();
            newViewportExtent = viewport.getExtentSize();
        }

        int newVpTopLine;
        if (newViewportPosition == null
                || newViewportExtent == null
                || (newVpTopLine = findFullLine(
                                        newViewportPosition, DIR_TOP)) <= 0) {
            vpTopLine = -1;
            vpBottomLine = -1;
            newVpLines = -1;
        } else {
            if (vpTopLine != newVpTopLine) {
                topLineChange = true;
            }
            vpTopLine = newVpTopLine;
            Point pt = new Point(newViewportPosition); // top-left
            pt.translate(0, newViewportExtent.height - 1); // bottom-left
            vpBottomLine = findFullLine(pt, DIR_BOT);
            //
            // Calculate number of lines on screen, some may be blank
            //
            // NEEDSWORK: WHAT TO DO????????????
            // ALLOW TO CHANGE WITH EACH SCREEN considering variable font

            // truncate intended
            newVpLines = (int)(newViewportExtent.height / getLineHeight(1, 0));
        }

        boolean sizeChange = false;
        if (newViewportExtent == null
                || !newViewportExtent.equals(viewportExtent)
                || newVpLines != vpLines) {
            sizeChange = true;
        }
        vpLines = newVpLines;
        viewportPosition = newViewportPosition;
        viewportExtent = newViewportExtent;

        if (sizeChange) {
            viewSizeChange();
        }
        if (sizeChange || topLineChange) {
            ViManager.viewMoveChange(this);
        }
    }

    public Point makePointTruncY(Point2D p)
    {
        Point p01 = new Point();
        p01.setLocation(p.getX(), (int)p.getY());
        return p01;
    }

    public Point2D getLocation(Rectangle2D r)
    {
        Point2D p = new Point2D.Double(r.getX(), r.getY());
        return p;
    }

    public Rectangle2D modelToView(int offset) throws BadLocationException
    {
        Shape s = modelToView(
                getEditorComponent(), offset, Position.Bias.Forward);
        Rectangle2D r = s.getBounds2D();
        // (0,3,300,300).contains(3,3,0,17) because of the 0 width (jdk1.5 at least)
        // so... make sure there is some width
        if (r.getWidth() == 0) {
            r.setRect(r.getX(), r.getY(), 1, r.getHeight());
        }
        return r;
    }

    public static Shape modelToView(JTextComponent jtc, int pos)
            throws BadLocationException
    {
        return modelToView(jtc, pos, Position.Bias.Forward);
    }

    // adapted from BasicTextUI
    public static Shape modelToView(
            JTextComponent jtc, int pos, Position.Bias bias)
            throws BadLocationException
    {
	Document doc = jtc.getDocument();
	if (doc instanceof AbstractDocument) {
	    ((AbstractDocument)doc).readLock();
	}
	try {
            View rootView = jtc.getUI().getRootView(jtc);
	    Rectangle alloc = getVisibleEditorRect(jtc);
	    if (alloc != null) {
		//rootView.setSize(alloc.width, alloc.height);
		Shape s = rootView.modelToView(pos, alloc, bias);
		if (s != null) {
		  //return s.getBounds();
		  return s;
		}
	    }
	} finally {
	    if (doc instanceof AbstractDocument) {
		((AbstractDocument)doc).readUnlock();
	    }
	}
	return null;
    }
    static Position.Bias[] biasReturnBitBucket = new Position.Bias[1];
    public int viewToModel(Point2D pt)
    {
        return viewToModel(getEditorComponent(), pt, biasReturnBitBucket);
    }
    public static int viewToModel(JTextComponent jtc, Point2D pt)
    {
        return viewToModel(jtc, pt, biasReturnBitBucket);
    }
    public static int viewToModel(JTextComponent jtc, Point2D pt,
			   Position.Bias[] biasReturn) {
	int offs = -1;
	Document doc = jtc.getDocument();
	if (doc instanceof AbstractDocument) {
	    ((AbstractDocument)doc).readLock();
	}
	try {
            View rootView = jtc.getUI().getRootView(jtc);
	    Rectangle alloc = getVisibleEditorRect(jtc);
	    if (alloc != null) {
		rootView.setSize(alloc.width, alloc.height);
		offs = rootView.viewToModel((float)pt.getX(), (float)pt.getY(),
                        alloc, biasReturn);
	    }
	} finally {
	    if (doc instanceof AbstractDocument) {
		((AbstractDocument)doc).readUnlock();
	    }
	}
        return offs;
    }
    private static Rectangle getVisibleEditorRect(JTextComponent jtc) {
	Rectangle alloc = jtc.getBounds();
	if ((alloc.width > 0) && (alloc.height > 0)) {
	    alloc.x = alloc.y = 0;
	    Insets insets = jtc.getInsets();
	    alloc.x += insets.left;
	    alloc.y += insets.top;
	    alloc.width -= insets.left + insets.right;
	    alloc.height -= insets.top + insets.bottom;
	    return alloc;
	}
	return null;
    }

    /**
     * Determine the line number of the text that is fully displayed
     * (top or bottom not chopped off).
     * @return line number of text at point, -1 if can not be determined
     */
    private int findFullLine(Point pt, int dir)
    {
        try {
            return findFullLineThrow(pt, dir);
        } catch (BadLocationException ex) {
            LOG.log(Level.SEVERE, null, ex);
            System.err.println("findFullLine: ");
            return -1;
        }
    }

    private int findFullLineThrow(Point pt, int dir)
            throws BadLocationException
    {
        Rectangle vrect = viewport.getViewRect();

        int offset = viewToModel(pt);
        if (offset < 0) {
            return -1;
        }

        int line = getBuffer().getLineNumber(offset);
        Rectangle2D lrect = modelToView(offset);
        if (vrect.contains(lrect)) {
            return line;
        }
        int oline = line;
        line -= dir; // move line away from top/bottom
        if (line < 1 || line > getBuffer().getLineCount()) {
            //System.err.println("findFullLine: line out of bounds " + line);
            return oline;
        }

        offset = getBuffer().getLineStartOffset(line);
        lrect = modelToView(offset);
        if (!vrect.contains(lrect)) {
            //System.err.println("findFullLine: adjusted line still out " + line);
        }
        return line;
    }

    //
    // Track changes of interest.
    //
    // There are two types of cached things we're interested in,
    //	- data from the document, especially around the cursor
    //	  Caret movement and document changes affect this
    //
    //	- visible screen: top line, bottom line
    //	  resize, viewport affects this
    //

    private void changeFont(Font f)
    {
        fillLinePositions();
    }

    /** The container for the editor has changed. */
    private void changeViewport(Object component)
    {
        if (viewport != null) {
            viewport.removeChangeListener(this);
        }
        if (component instanceof JViewport) {
            viewport = (JViewport) component;
            viewport.addChangeListener(this);
        } else {
            viewport = null;
        }
        changeVp(true);
    }

    /** The defining rectangle of the viewport has changed
     *  @param init true indicates that the position should be
     *  checked immeadiately, not potentially defered with an invoke later.
     */
    private void changeVp(boolean init)
    {
        if (init) {
            fillLinePositionsFinally();
        } else {
            fillLinePositions();
        }
    }

    /** This is called from the managing textview,
     * listen to things that affect the cache.
     */
    private void attachMore()
    {
        if (G.dbgEditorActivation.getBoolean()) {
            System.err.println("TVCache: attach: "
                               + (editorPane == null ? 0 : editorPane.hashCode()));
        }
        if (freezer != null) {
            freezer.stop();
            freezer = null;
        }

        if (hasListeners) {
            return;
        }

        hasListeners = true;
        editorPane.addPropertyChangeListener("font", this);
        editorPane.addPropertyChangeListener("document", this);
        editorPane.addPropertyChangeListener("ancestor", this);
        changeFont(editorPane.getFont());
        changeViewport(editorPane.getParent());
    }
    boolean hasListeners = false;

    /** Dissassociate from the observed components. */
    private void detachMore()
    {
        if (G.dbgEditorActivation.getBoolean()) {
            System.err.println("TVCache: detach: "
                               + (editorPane == null ? "" : editorPane.hashCode()));
        }
        if (editorPane == null) {
            return;
        }
        freezer = new FreezeViewport(editorPane);

    //removeListeners();
    }

    private void shutdownMore()
    {
        if (freezer != null) {
            freezer.stop();
            freezer = null;
        }
        removeListeners();
    }

    private void removeListeners()
    {
        hasListeners = false;
        editorPane.removePropertyChangeListener("font", this);
        editorPane.removePropertyChangeListener("document", this);
        editorPane.removePropertyChangeListener("ancestor", this);
        changeViewport(null);
    }
    private FreezeViewport freezer;

    //
    // Listener events
    //

    // -- property change events --
    public void propertyChange(PropertyChangeEvent e)
    {
        String p = e.getPropertyName();
        Object o = e.getNewValue();
        if ("font".equals(p)) {
            changeFont((Font) o);
        } else if ("document".equals(p)) {
            changeDocument(e); // this assert
        } else if ("ancestor".equals(p)) {
            changeViewport(o);
        }
    }


    // -- viewport event --
    public void stateChanged(ChangeEvent e)
    {
        changeVp(false);
    }

    //////////////////////////////////////////////////////////////////////
    //
    // Visual Select
    //

    /**
     * Update the selection highlight.
     *
     * see PlayTextView for a simple implementation.
     */
    public void updateVisualState() {}

    //////////////////////////////////////////////////////////////////////
    //
    // Highlight Search
    //

    /**
     * Update the highlight search.
     *
     * see PlayTextView for a simple implementation.
     */
    public void updateHighlightSearchState() {}

} // end com.raelity.jvi.swing.SwingTextView
