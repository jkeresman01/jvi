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

import com.raelity.jvi.options.BooleanOption;
import  static com.raelity.jvi.Constants.*;


import com.raelity.jvi.*;
import com.raelity.text.TextUtil.MySegment;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JEditorPane;
import javax.swing.JViewport;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

/**
 *  Presents a swing editor interface for use with vi. There is
 *  one of these for each JEditorPane.
 *  <p>
 *  Notice the listeners for caret changes. If the caret changes
 *  to a location that is unexpected, i.e. it came from some external
 *  source, then an externalChange message is sent to vi.
 *  </p><p>
 *  The getEditorComponent method should not be used by the primary
 *  vi software. The primary vi software should only access, or make
 *  changes to, the underlying JEditorPane through other methods in
 *  this class.
 *  </p>
 */
public class TextView extends Window
        implements ViTextView, PropertyChangeListener, ChangeListener
{
    private static Logger LOG = Logger.getLogger(TextView.class.getName());
    private static int genNum; // unique/invariant window id;

    protected int w_num;


    protected JEditorPane editorPane;
    protected TextOps ops;
    protected Window window;

    protected ViStatusDisplay statusDisplay;

    private Point point0;
    private CaretListener cursorSaveListener;
    private int lastDot;
    private static int gen;

    // ............


    public TextView(final JEditorPane editorPane, ViStatusDisplay statusDisplay)
    {
        //HACK for cmd.Jvi
        this(editorPane);
        this.statusDisplay = statusDisplay;
    }
    public TextView( final JEditorPane editorPane)
    {
        super();
        this.editorPane = editorPane;
        w_num = ++genNum;

        cursorSaveListener = new CaretListener() {
            public void caretUpdate(CaretEvent ce) {
                if(!ViManager.getViFactory().isEnabled())
                    return;
                cursorMoveDetected(lastDot, ce.getDot(), ce.getMark());
                lastDot = ce.getDot();
            }
        };
    }


    public ViFPOS createWCursor()
    {
        return w_cursor == null ? new WCursor() : null;
    }


    public int getNum() {
        return w_num;
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

        shutdown(editorPane); // CACHE
        ViManager.detached(editorPane);
        editorPane = null;
    }


    public boolean isShutdown()
    {
        return editorPane == null;
    }


    //
    // Declare the variables referenced as part of a ViOptionBag
    //

    /** jVi doesn't support this flag. Keep it as a per window flag like vim.
     * The platform may do something with it.
     */
    public boolean w_p_nu;


    public void viOptionSet( ViTextView tv, String name )
    {
    }


    public void activateOptions( ViTextView tv )
    {
        updateHighlightSearchState();
    }


    public final DefaultBuffer getBuffer()
    {
        return (DefaultBuffer) w_buffer;
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
        attach(editorPane); // CACHE
    }


    public void detach()
    {
        detach(editorPane); // CACHE

        ViManager.detached(editorPane);
    }


    /**
     *  Create methods to invoke and interact with editor pane actions.
     *  May override for custom editor panes.
     */
    protected void createOps()
    {
        ops = new Ops(this);
    }


    public JEditorPane getEditorComponent()
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
    public boolean openNewLine( NLOP op )
    {
        if ( !isEditable() ) {
            Util.vim_beep();
            return false;
        }
        if ( op == NLOP.NL_BACKWARD && w_cursor.getLine() == 1 ) {
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
        if ( op == NLOP.NL_FORWARD ) {
            // after the current line,
            // handle current line might be a fold
            // int cmpOffset = getBuffer()
            //                .getLineEndOffsetFromOffset(w_cursor.getOffset());
            offset = getBufferLineOffset(
                    getCoordLine(w_cursor.getLine()) + 1);
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


    /** Scroll down (n_lines positive) or up (n_lines negative) the
     * specified number of lines.
     * @param n_lines
     */
    public void scroll( int n_lines )
    {
        Point pt = getViewport().getViewPosition();
        pt.translate(0, n_lines * getFheight());
        getViewport().setViewPosition(pt);
    }


    private Point getPoint0()
    {
        if ( point0 != null ) {
            return point0;
        }
        try {
          Rectangle r = editorPane.modelToView(0);
            if ( r != null ) {
                point0 = r.getLocation();
                return point0;
            }
        } catch (BadLocationException ex) { }
        return new Point(0,0);
    }


    public int getViewCoordTopLine()
    {
        if ( !G.isCoordSkip.getBoolean() ) {
            return getViewTopLine();
        }
        int coordLine = getInternalCoordLine(getViewTopLine());
        if ( G.dbgCoordSkip.getBoolean() ) {
            System.err.println("getViewCoordTopLine: " + coordLine);
        }
        return coordLine;
    }


    public void setViewCoordTopLine( int coordLine )
    {
        if ( !G.isCoordSkip.getBoolean() ) {
            setViewTopLine(coordLine);
            return;
        }
        //cache.setViewCoordTopLine(coordLine);

        Point p = new Point(0, (coordLine - 1) * getFheight());
        // If point is after viewport top && within one char of top
        // then just return, to avoid wiggle
        int topDiff = p.y - getViewport().getViewPosition().y;
        if ( topDiff > 0 && topDiff < getFheight() ) {
            return;
        }
        getViewport().setViewPosition(p);
    }


    public int getViewCoordBlankLines()
    {
        int n;
        if(!G.isCoordSkip.getBoolean()) {
            n = getViewBlankLines();
        } else {
          n = getViewLines()
                  - (viewBottomLine - getViewCoordTopLine() + 1);
               // - ((getViewCoordBottomLine()-1) - getViewCoordTopLine());
        }
        return n;
    }


    //
    // NOTE:
    //     in TextViewCache, viewBottomLine is not the same as getViewBottomLine
    //
    public int getViewCoordBottomLine()
    {
        int coordLine;
        if ( !G.isCoordSkip.getBoolean() ) {
            coordLine = getViewBottomLine();
        } else {
            // NEEDSWORK: COORD consider blank lines on screen, coordLine past EOF
            coordLine = getInternalCoordLine(getViewTopLine()) + getViewLines();
            coordLine = viewBottomLine + 1;
            if(G.dbgCoordSkip.getBoolean()) {
                System.err.println("getViewCoordBottomLine: " + coordLine);
            }
        }
        return coordLine; // NEEDSWORK: line past full line, see getViewBottomLine
    }


    public int getCoordLineCount()
    {
        if ( !G.isCoordSkip.getBoolean() ) {
            return getBuffer().getLineCount();
        }
        int coordLine = 1;
        coordLine = getInternalCoordLine(getBuffer().getLineCount());
        if ( G.dbgCoordSkip.getBoolean() ) {
            System.err.println("getCoordLineCount: " + coordLine);
        }
        return coordLine;
    }


    public int getCoordLine(int line)
    {
        if ( !G.isCoordSkip.getBoolean() ) {
            return line;
        }
        if(line > getBuffer().getLineCount()) {
            ViManager.dumpStack("line "+line
                                +" past "+getBuffer().getLineCount());
            line = getBuffer().getLineCount();
        }
        int coordLine = getInternalCoordLine(line);
        if ( G.dbgCoordSkip.getBoolean() ) {
            System.err.println("getCoordLine: " + coordLine);
        }
        return coordLine;
    }


    public int getBufferLineOffset( int coordLine )
    {
        if ( !G.isCoordSkip.getBoolean() ) {
            if ( coordLine > getBuffer().getLineCount() ) {
                return getBuffer().getLength() + 1;
            }
            return getBuffer().getLineStartOffset(coordLine);
        }
        Point p = new Point(getPoint0());
        p.translate(0, (coordLine - 1) * getFheight());
        int offset = getEditorComponent().viewToModel(p);
        Rectangle r1 = null;
        try {
            r1 = getEditorComponent().modelToView(offset);
        } catch ( BadLocationException ex ) {
            // should be impossible since viewtomodel return should be valid
            LOG.log(Level.SEVERE, null, ex);
        }
        if ( p.y > r1.y ) {
            // past end of file
            // System.err.println("past EOF: p " + p + ", r1 " + r1);
            offset = getBuffer().getLength() + 1;
        }
        return offset;
    }


    // NEEDSWORK: This gets CALLED A LOT
    private int getInternalCoordLine( int line )
    {
        int coordLine = 1;
        try {
          int offset = getBuffer().getLineStartOffset(line);
          Rectangle lineRect = getEditorComponent().modelToView(offset);
          int yDiff = lineRect.y - getPoint0().y;
          coordLine = yDiff / getFheight() + 1;
          if ( G.dbgCoordSkip.getBoolean() ) {
              System.err.println(String.format(
                      "\tgetInternalCoordLine: %d, line1: %d:%d, line %d:%d",
                      coordLine, 1, getPoint0().y, line, lineRect.y));
           }
        } catch (BadLocationException ex) {
            //Logger.getLogger(TextView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return coordLine;
    }


    public void setCursorCoordLine( int coordLine, int col )
    {
        if ( !G.isCoordSkip.getBoolean() ) {
            w_cursor.set(coordLine, col);
            return;
        }
        Point p = new Point(getPoint0());
        p.translate(0, (coordLine - 1) * getFheight());
        int newOffset = getEditorComponent().viewToModel(p);
        //assert col == 0;
        setCaretPosition(newOffset + col);
    }


    public int coladvanceCoord( int lineOffset, int colIdx )
    {
      if ( lineOffset < 0 ) {
          ViManager.dumpStack("invalid lineOffset");
          return 0;
      }

      // insure col is displayable (e.g. visual position not in fold)
      // check 'visual position after prev position is position we want'
      // in other words, getNextVisualPositionFrom(here - 1) == here
      // if not, we're in nether regions, return here - 1
      try {
          JEditorPane c = getEditorComponent();
          for (int i = 1; i <= colIdx; i++) {
              int thisVisualPosition = lineOffset + i;
              int nextVisualPosition = c.getUI().getNextVisualPositionFrom(
                      c,
                      thisVisualPosition - 1,
                      javax.swing.text.Position.Bias.Forward,
                      javax.swing.SwingConstants.EAST,
                      new javax.swing.text.Position.Bias[1]); // used in jdk1.5
                      //null); // may be null in jdk1.6
              if ( thisVisualPosition != nextVisualPosition ) {
                  colIdx = i - 1;
                  break;
              }
          }
      } catch ( BadLocationException ex ) {
          LOG.log(Level.SEVERE, null, ex);
      }
      return colIdx;
    }


    private Element getLineElement( int lnum )
    {
        return getBuffer().getLineElement(lnum);
    }


    public void updateCursor( ViCursor cursor )
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


    /** Goto the indicated buffer.
     * @param n the index of the window to make current
     */
    public void win_goto( int n )
    {
        Msg.emsg("win_goto not implemented");
    }

    /** Cycle to the indicated buffer.
     * @param n the positive/negative number of windows to cycle.
     */
    public void win_cycle( int n )
    {
        Msg.emsg("win_cycle not implemented");
    }

    /** Cycle to the indicated buffer.
     * @param n the positive/negative number of windows to cycle.
     */
    public void win_cycle_nomad( int n )
    {
        Msg.emsg("win_cycle_nomad not implemented");
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

        final public DefaultBuffer getBuffer() {
            return (DefaultBuffer)w_buffer;
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
        ViManager.getViFactory().changeBuffer(this, e.getOldValue());
    }

    final static int DIR_TOP = -1;
    final static int DIR_BOT = 1;

    private static BooleanOption cacheTrace
            = (BooleanOption) Options.getOption(Options.dbgCache);

    final private JViewport getViewport()
    {
        return viewport;
    }

    final protected int getFheight()
    {
        return fheight;
    }
    /** current font information. */
    private FontMetrics fm;
    /** height of the font */
    private int fheight;

    // The visible part of the document, negative means not valid.
    // These values are updated whenever the viewport changes.
    private JViewport viewport;
    private Point viewportPosition;
    private Dimension viewportExtent;
    private int topLineOffset;
    private int bottomLineOffset;
    private int viewTopLine;
    private int viewBottomLine;
    private int viewLines;

    /** @return the top line number */
    public int getViewTopLine()
    {
        if(viewport == null)
            return 1;
        return viewTopLine;
    }

    public void setViewTopLine(int line)
    {
        if(viewport == null)
            return;
        if (line == getViewTopLine()) {
            return; // nothing to change
        }
        int offset = getBuffer().getLineStartOffset(line);
        Rectangle r;
        try {
            r = getEditorComponent().modelToView(offset);
        } catch (BadLocationException e) {
            Util.vim_beep();
            return;
        }
        Point p = r.getLocation();
        p.translate(-p.x, 0); // leave a few pixels to left
        viewport.setViewPosition(p);
    }

    public int getViewBottomLine()
    {
        if(viewport == null)
            return 1;
        return viewBottomLine + 1;  // NEEDSWORK: returning line past full line
    }

    public int getViewBlankLines()
    {
        if(viewport == null)
            return 0;
        int blank = viewLines - (viewBottomLine - viewTopLine + 1);
        return blank;
    }

    /** @return number of lines on viewport */
    public int getViewLines()
    {
        if(viewport == null)
            return 1;
        return viewLines;
    }

    public int getRequiredDisplayLines()
    {
        return getViewLines();
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
        int newViewLines = -1;
        boolean topLineChange = false;

        if (viewport == null) {
            newViewportPosition = null;
            newViewportExtent = null;
        } else {
            newViewportPosition = viewport.getViewPosition();
            newViewportExtent = viewport.getExtentSize();
        }

        int newViewTopLine;
        if (newViewportPosition == null
                || newViewportExtent == null
                || (newViewTopLine = findFullLine(
                                        newViewportPosition, DIR_TOP)) <= 0) {
            viewTopLine = -1;
            viewBottomLine = -1;
            newViewLines = -1;
        } else {
            if (viewTopLine != newViewTopLine) {
                topLineChange = true;
            }
            viewTopLine = newViewTopLine;
            Point pt = new Point(newViewportPosition); // top-left
            pt.translate(0, newViewportExtent.height - 1); // bottom-left
            viewBottomLine = findFullLine(pt, DIR_BOT);
            //
            // Calculate number of lines on screen, some may be blank
            //
            newViewLines = newViewportExtent.height / fheight;
        }

        boolean sizeChange = false;
        if (newViewportExtent == null || !newViewportExtent.equals(viewportExtent) || newViewLines != viewLines) {
            sizeChange = true;
        }
        viewLines = newViewLines;
        viewportPosition = newViewportPosition;
        viewportExtent = newViewportExtent;

        if (sizeChange) {
            viewSizeChange();
        }
        if (sizeChange || topLineChange) {
            ViManager.viewMoveChange(this);
        }
    }

    protected Rectangle modelToView(int offset) throws BadLocationException
    {
        Rectangle r = getEditorComponent().modelToView(offset);
        // (0,3,300,300).contains(3,3,0,17) because of the 0 width (jdk1.5 at least)
        // so...
        if (r.width == 0) {
            r.width = 1;
        }
        return r;
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
            // Logger.getLogger(TextViewCache.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("findFullLine: ");
            return -1;
        }
    }

    private int findFullLineThrow(Point pt, int dir) throws BadLocationException
    {
        Rectangle vrect = viewport.getViewRect();

        int offset = getEditorComponent().viewToModel(pt);
        if (offset < 0) {
            return -1;
        }

        int line = getBuffer().getLineNumber(offset);
        Rectangle lrect = modelToView(offset);
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
        int h;
        if (f == null) {
            fm = null;
        } else {
            fm = getEditorComponent().getFontMetrics(f);
            fheight = fm.getHeight();
            fillLinePositions();
        }
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
        changeView(true);
    }

    /** The defining rectangle of the viewport has changed
     *  @param init true indicates that the position should be
     *  checked immeadiately, not potentially defered with an invoke later.
     */
    private void changeView(boolean init)
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
    private void attach(JEditorPane editor)
    {
        if (G.dbgEditorActivation.getBoolean()) {
            System.err.println("TVCache: attach: "
                               + (editor == null ? 0 : editor.hashCode()));
        }
        if (freezer != null) {
            freezer.stop();
            freezer = null;
        }

        if (hasListeners) {
            return;
        }

        hasListeners = true;
        editor.addPropertyChangeListener("font", this);
        editor.addPropertyChangeListener("document", this);
        editor.addPropertyChangeListener("ancestor", this);
        changeFont(editor.getFont());
        changeViewport(editor.getParent());
    }
    boolean hasListeners = false;

    /** Dissassociate from the observed components. */
    private void detach(JEditorPane ep)
    {
        if (G.dbgEditorActivation.getBoolean()) {
            System.err.println("TVCache: detach: "
                               + (ep == null ? "" : ep.hashCode()));
        }
        if (ep == null) {
            return;
        }
        freezer = new FreezeViewport(ep);

    //removeListeners();
    }

    private void shutdown(JEditorPane ep)
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
        JEditorPane editor = getEditorComponent();
        editor.removePropertyChangeListener("font", this);
        editor.removePropertyChangeListener("document", this);
        editor.removePropertyChangeListener("ancestor", this);
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
        changeView(false);
    }

    /**
     * Stabilize (do not allow scrolling) the JViewport displaying
     * the indicated JEditorPane.
     * This is typically used when the underlying document may change while
     * being edited in another view. The {@link #stop} method is used to release
     * the listeners and so unfreeze the viewport.
     * <p>This is a one shot class. The editor is expected to be good to go.
     * Only document changes are listened to. The first char of the top line is
     * pinned to the upper left corner. If needed, this could be extended
     * to pin the horizontal position as well.
     */
    public static class FreezeViewport
    implements DocumentListener, ChangeListener
    {
        private JEditorPane ep;
        private JViewport vp;
        private AbstractDocument doc;
        private Position pos;
        private int topLine;
        private int nLine;

        public FreezeViewport(JEditorPane ep)
        {
            this.ep = ep;
            Object o = ep.getDocument();
            if (!(o instanceof AbstractDocument)) {
                return;
            }
            doc = (AbstractDocument) ep.getDocument();
            try {
                doc.readLock();
                vp = (JViewport) ep.getParent(); // may throw class cast, its ok
                Element root = doc.getDefaultRootElement();
                nLine = root.getElementCount();

                // Get the offset of the first displayed char in the top line
                Point pt = vp.getViewPosition();
                int offset = ep.viewToModel(pt);

                // Determine the line number of the top displayed line
                topLine = root.getElementIndex(offset);
                //System.err.format("FreezeViewport: top %d\n", topLine);

                // Note. offset may not be first char, due to horiz scroll
                // make offset the first char of the line
                offset = root.getElement(topLine).getStartOffset();

                // Get marker to offset in the document
                pos = doc.createPosition(offset);
                doc.addDocumentListener(this);
                //vp.addChangeListener(this); // debug info
            } catch (Exception ex) {
                // Note: did not start listener
            } finally {
                doc.readUnlock();
            }
        }

        public void stop()
        {
            if (doc != null) {
                doc.removeDocumentListener(this);
            }
            if(vp != null) {
                vp.removeChangeListener(this);
            }
        }

        private void adjustViewport(int offset)
        {
            // Might be able to use info from DocumentEvent to optimize
            try {
                Point pt = ep.modelToView(offset).getLocation();
                pt.translate(-pt.x, 0); // x <-- 0, leave a few pixels to left
                vp.setViewPosition(pt);
            } catch (Exception ex) {
                stop();
            }
            return;
        }

        private void handleChange(DocumentEvent e)
        {
            // Note while in listener document can't change, no read lock
            Element root = doc.getDefaultRootElement();
            int newNumLine = root.getElementCount();
            // return if line count unchanged or changed after our mark
            if (nLine == newNumLine || e.getOffset() > pos.getOffset()) {
                return;
            }
            nLine = newNumLine;

            int newTopLine = root.getElementIndex(pos.getOffset());
            //System.err.format("handleChange: old %d new %d\n", topLine, newTopLine);
            if (topLine == newTopLine) {
                return;
            }
            topLine = newTopLine;

            // make a move
            final int offset = root.getElement(topLine).getStartOffset();
            if (false && EventQueue.isDispatchThread()) { // false needed NB6.8
                adjustViewport(offset);
                //System.err.println("handleChange: adjust in dispatch");
            } else {
                EventQueue.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                adjustViewport(offset);
                                //System.err.println("handleChange: adjust later");
                            }
                        });
            }
        }

        public void insertUpdate(DocumentEvent e)
        {
            handleChange(e);
        }

        public void removeUpdate(DocumentEvent e)
        {
            handleChange(e);
        }

        public void changedUpdate(DocumentEvent e)
        {
        }

        public void stateChanged(ChangeEvent e) {
            Point pt = vp.getViewPosition();
            int offset = ep.viewToModel(pt);
            Element root = doc.getDefaultRootElement();
            int topl = root.getElementIndex(offset);
            System.err.println("Viewport stateChanged: top line " + topl);
        }

    }


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //
    // Visual Select
    //

    /**
     * Update the selection highlight.
     */
    public void updateVisualState()
    {
        // Following stuff standalone, only for debugging
        // (should really be a seperate method)
        if(ViManager.getViFactory().isStandalone()) {
            if (!G.VIsual_active) {
                try {
                    unhighlight(new int[] {
                        getBuffer().getMark('<').getOffset(),
                        getBuffer().getMark('>').getOffset(),
                        -1, -1});
                } catch(Exception e) {
                    unhighlight(new int[] {
                        0, editorPane.getText().length(),
                        -1, -1});
                }
            }
            int[] b = getBuffer()
                    .getVisualSelectBlocks(this, 0, Integer.MAX_VALUE);
            //dumpBlocks("blocks", b);
            highlight(b);
        }
    }


    //////////////////////////////////////////////////////////////////////
    //
    // Highlight Search
    //

    public void updateHighlightSearchState()
    {
        applyBackground(new int[] {0, getBuffer().getLength(), -1, -1},
                UNHIGHLIGHT);

        if(!Options.doHighlightSearch()) {
            return;
        }

        int[] b = getBuffer().getHighlightSearchBlocks(0, getBuffer().getLength());
        applyBackground(b, HIGHLIGHT);
    }


    //////////////////////////////////////////////////////////////////////
    //
    // StyledDocument highlight methods
    //

    private static MutableAttributeSet HIGHLIGHT = new SimpleAttributeSet();
    private static MutableAttributeSet UNHIGHLIGHT = new SimpleAttributeSet();
    static {
        StyleConstants.setBackground(HIGHLIGHT, Color.LIGHT_GRAY);
        StyleConstants.setBackground(UNHIGHLIGHT, Color.WHITE);
    }

    private void unhighlight( int[] blocks )
    {
        applyBackground(blocks, UNHIGHLIGHT);
    }

    protected int[] previousAppliedHighlight = null;

    private void highlight( int[] blocks )
    {
        if (previousAppliedHighlight != null
                && !Arrays.equals(previousAppliedHighlight, blocks)) {
            unhighlight(previousAppliedHighlight);
        }
        applyBackground(blocks, HIGHLIGHT);
        previousAppliedHighlight = blocks;
    }


    protected void applyBackground( int[] blocks, MutableAttributeSet mas )
    {
        StyledDocument document = (StyledDocument) editorPane.getDocument();
        for ( int i = 0; i < blocks.length; i+=2 ) {
            int start = blocks[i];
            int end = blocks[i+1];
            if ( start == -1 && end == -1 ) { // break
                return;
            }
            if ( start > end ) {
                int tmp = start;
                start = end;
                end = tmp;
            }
            document.setCharacterAttributes(start, end - start, mas, false);
            // update styled editor kit with new attributes
            // to overcome paint errors
            StyledEditorKit k = (StyledEditorKit)editorPane.getEditorKit();
            MutableAttributeSet inputAttrs = k.getInputAttributes();
            inputAttrs.addAttributes(mas);
        }
    }

} // end com.raelity.jvi.swing.TextView
