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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.*;

import static com.raelity.jvi.core.G.dbgEditorActivation;
import static com.raelity.jvi.core.lib.Constants.*;

import static java.util.logging.Level.*;

import static com.raelity.jvi.lib.TextUtil.sf;

/**
 * This represents the core functionality of a vim window.
 * <p>
 * Vim references values in a structure, but we need to present a method
 * interface, so the vim code is not preserved as we'd like.
 * </p>
 */
public abstract class TextView implements ViTextView
{
    private static final int MAGIC_MOVE_LINE_LIMIT = 15;
    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
        @Override
        public void init()
        {
        }
    }


    protected Buffer w_buffer;

    //
    // Declare the variables that are a basic part of the window.
    //

    /**
     * This is a magic ViFPOS. It tracks the caret positon on the screen
     * and modifying w_cursor moves the caret positon.
     */
    final public ViFPOS w_cursor;

    private boolean didFirstInit;

    /**
     * The column we'd like to be at. Used for up/down cursor motions.
     * Should not be written to directly, except in special situations,
     * use updateCurswant().
     */
    protected int w_curswant;

    /**
     * flag that w_curswant should be set based on current cursor position
     */
    protected boolean w_set_curswant;

    //
    // Mark related stuff
    //
    protected ViMark w_pcmark;
    protected ViMark w_prev_pcmark;

    // need to be public until reflection code in setColonCommand is fixed

    public boolean w_p_nu;      // NOT USED
    public boolean w_p_list;    // NOT USED

    public boolean w_p_wrap;
    public boolean w_p_lbr;

    public int w_p_scr;

    //protected final int JUMPLISTSIZE = 50;
    protected List<ViMark> w_jumplist = new LinkedList<>();
    protected int w_jumplistidx;

    public TextView()
    {
        w_set_curswant = true;
        w_cursor = createWCursor();
        viewSizeChange();
    }

    /**
     * The default implementation simply updates w_curswant.
     * An implementation might save/compute a "w_view_curswant"
     * which takes wrapping into account.
     * @param fpos cursor on line to consider, if null then w_cursor
     */
    protected void updateCurswant(ViFPOS fpos, int curswant)
    {
        w_curswant = curswant;
    }

    @Override
    public void attachBuffer( ViBuffer buf )
    {
        if(this.w_buffer != null)
            ViManager.dumpStack();
        this.w_buffer = (Buffer)buf;

        w_pcmark = buf.createMark(null);
        w_prev_pcmark = buf.createMark(null);
    }

    public void detachBuffer()
    {
        w_pcmark = null;
        w_prev_pcmark = null;
        w_buffer = null;
        w_jumplist = new LinkedList<>();
        w_jumplistidx = 0;
    }

    @Override
    public void shutdown()
    {
        recordLastFPOS();
        if(w_buffer.getShare() == 1) {
            dbgEditorActivation.println(INFO, "TV.shutdown: LAST CLOSE");
        }
    }

    @Override
    public void recordLastFPOS()
    {
        if(w_cursor.isValid()) {
            w_buffer.saving_fpos = new ViFPOS.ReadOnlyFPOS(w_cursor);
            dbgEditorActivation.println(INFO, () ->
                    sf("LFP: recordLastFPOS: saving_fpos: %s %s",
                       w_buffer.saving_fpos.toString(), toString()));
            MarkOps.setMarkLastFPOS(getBuffer());
        }
    }

    @Override
    public void activateOptions(ViTextView tv) {
        if(getAppView().isNomad()) {
            G.dbgEditorActivation.println(CONFIG, "ACTIVATING OPTIONS FOR NOMAD");
        }
        if(!didFirstInit) {
            firstGo();
            didFirstInit = true;
        }
    }

    @Override
    public void viOptionSet(ViTextView tv, VimOption vopt)
    {
        if("w_p_scr".equals(vopt.getVarName())) {
            if(w_p_scr == 0)
                viewSizeChange(); // set to half window size
        }
    }


    /**
     * Put stuff here that should run once
     * after after construction and every things is setup (curbuf, curwin).
     * <br/>initOptions
     */
    protected void firstGo()
    {
        if(w_cursor.getOffset() == 0 && w_buffer.previous_close_fpos != null) {
            dbgEditorActivation.println(INFO, () -> sf("LFP: TextView: firstGo: %s %s",
                       w_buffer.previous_close_fpos.toString(),
                       w_buffer.getFile().toString()));
            int lino = w_buffer.previous_close_fpos.getLine();
            lino = lino <= w_buffer.getLineCount() ? lino : w_buffer.getLineCount();
            w_cursor.set(lino, w_buffer.previous_close_fpos.getColumn());
        }
        w_p_wrap = Options.getOption(Options.wrap).getBoolean();
        w_p_lbr = Options.getOption(Options.lineBreak).getBoolean();
        w_p_list = Options.getOption(Options.list).getBoolean();
        w_p_nu = Options.getOption(Options.number).getBoolean();
    }

    /**
     * This is invoked by a subclass to indicate that the size of the
     * view has changed.
     * Like win_new_height....
     */
    public final void viewSizeChange()
    {
        // from win_comp_scroll
        int i = (getVpLines() >> 1);
        if (i <= 0) {
            i = 1;
        }
        w_p_scr = i;
    }

    /**
     * A mouse click, or some other situation, has occured in this window.
     * Check the position so it is not on a newline (unless in input mode)
     */
    @Override
    public int validateCursorPosition(int offset)
    {
        w_set_curswant = true; // NEEDSWORK: keep this?
        if (Misc01.getCharAt(offset) == '\n' && (G.State & INSERT) == 0) { // DONE
            // Sitting on a newline and not in insert mode
            // back the cursor up (unless previous char is a newline)
            if (offset > 0 && Misc01.getCharAt(offset - 1) != '\n') { // DONE
                --offset;
            }
        }
        return offset;
    }

    /**
     * Notification that the caret has moved in the TextView.
     * Do some bookkeeping and also adjust pcmark
     * if the caret is moved by an 'external agent' (e.g. an IDE)
     * but unfortunately, the external agent detection is difficult
     * since NB updates the cursor with invokeLater so jViBusy is always false.
     *
     * @param lastDot previous dot position
     * @param dot new dot position
     * @param mark new mark position
     */
    protected void cursorMoveDetected(int lastDot, int dot, int mark)
    {
        if (G.VIsual_active && this == G.curwin)
            Normal.v_updateVisualState(this);

        // since difficult to detect magic, check dot-lastDot for quick exit
        if (!G.pcmarkTrack || Math.abs(dot - lastDot) < 3)
            return;
        if(G.keepingCursorInView) // external scrolling doesn't affect pcmark
            return;

        int currDot = dot;
        // With updates invokeLater, limit magic loosly
        // (around changeset 1218)
        int diff = Math.abs(w_buffer.getLineNumber(currDot)
                - w_buffer.getLineNumber(lastDot));
        boolean magicMove = !ViManager.jViBusy() && !Scheduler.isMouseDown()
                && diff > MAGIC_MOVE_LINE_LIMIT;

        if (magicMove && G.dbgMouse.getBoolean(Level.INFO)
                || G.dbgMouse.getBoolean(Level.FINE))
            G.dbgMouse.println(() -> "CaretMark: " + lastDot + " --> " + currDot
                    + " " + w_buffer.getDebugFileName());
        if (magicMove) {
            // The cursor was magcally moved and jVi had nothing to
            // do with it. (probably by an IDE or some such).
            // Record the previous location so that '' works (thanks Jose).
            G.dbgMouse.println(Level.INFO, "caretUpdate: setPCMark");
            ViFPOS fpos = w_buffer.createFPOS(lastDot);
            MarkOps.setpcmark(this, fpos);
        }
    }

    //
    // NEEDSWORK: the win_* should really be somewhere else?
    // Maybe some kind of platform and/or window-manager interface?
    //
    // FIXME: If not separated, probably should be in TextView.
    //

    /** Quit editing window. Can close last view.
     */
    @Override
    public void win_quit()
    {
        Msg.emsg("win_quit NIMP");
    }


    /** Split this window.
     * @param n the size of the new window.
     */
    @Override
    public void win_split( Direction dir, int n, ViAppView av)
    {
        Msg.emsg("win_split(%s, %d, %s) NIMP", dir, n, av);
    }

    @Override
    public void win_move(Direction dir, int n)
    {
        Msg.emsg("win_move(%s, %d) NIMP", dir, n);
    }

    @Override
    public void win_clone()
    {
        Msg.emsg("win_clone NIMP");
    }

    @Override
    public void win_size(SIZOP op, Orientation orientation, int n)
    {
        Msg.emsg("win_size(%s, %s, %d) NIMP", op, orientation, n);
    }

    /** Close this window
     * @param freeBuf true if the related buffer may be freed
     */
    @Override
    public void win_close( boolean freeBuf )
    {
        Msg.emsg("win_close(%s) NIMP", freeBuf);
    }

    @Override
    public String toString()
    {
        String fn = getBuffer() != null ? getBuffer().getDebugFileName() : null;
        return "TextView{" + fn + '}';
    }
}
