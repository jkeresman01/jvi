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

import java.util.LinkedList;
import java.util.List;
import static com.raelity.jvi.Constants.*;

/**
 * This represents the core functionality of a vim window.
 * <p>
 * Vim references values in a structure, but we need to present a method
 * interface, so the vim code is not preserved as we'd like.
 * </p>
 */
public abstract class Window implements ViTextView
{
    protected Buffer w_buffer;

    //
    // Declare the variables that are a basic part of the window.
    //

    /**
     * This is a magic ViFPOS. It tracks the caret positon on the screen
     * and modifying w_cursor moves the caret positon.
     */
    final protected ViFPOS w_cursor;

    /**
     * The column we'd like to be at. Used for up/down cursor motions.
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

    protected boolean w_p_list;

    // NEEDSWORK: this should be comming from the cache (WHAT?)
    protected int w_p_scroll;

    //protected final int JUMPLISTSIZE = 50;
    protected List<ViMark> w_jumplist = new LinkedList();
    protected int w_jumplistidx;

    public Window()
    {
        w_set_curswant = true;
        w_cursor = createWCursor();
        viewSizeChange();
    }


    public void attachBuffer( Buffer buf )
    {
        if(this.w_buffer != null)
            ViManager.dumpStack();
        this.w_buffer = buf;

        w_pcmark = buf.createMark();
        w_prev_pcmark = buf.createMark();
    }

    public void detachBuffer()
    {
        w_pcmark = null;
        w_prev_pcmark = null;
        w_buffer = null;
        w_jumplist = new LinkedList();
        w_jumplistidx = 0;
    }

    public void shutdown()
    {
        if ( G.dbgEditorActivation.getBoolean() ) {
            assert getBuffer() == ViManager.getBuffer(getEditorComponent());
            if(getBuffer().getShare() == 1) {
                System.err.println("TV.shutdown: LAST CLOSE");
            }
        }
    }

    /**
     * This is invoked by a subclass to indicate that the size of the
     * view has changed.
     * Like win_new_height....
     */
    public void viewSizeChange()
    {
        // from win_comp_scroll
        int i = (getViewLines() >> 1);
        if (i <= 0) {
            i = 1;
        }
        w_p_scroll = i;
    }

    /**
     * A mouse click has just occured in this window. Check the
     * position so it is not on a newline (unless in input mode)
     * <br>
     * NEEDSWORK: "signal" a change in cursor position
     */
    public int validateCursorPosition(int offset)
    {
        w_set_curswant = true;
        if (Util.getCharAt(offset) == '\n' && (G.State & INSERT) == 0) {
            // Sitting on a newline and not in insert mode
            // back the cursor up (unless previous char is a newline)
            if (offset > 0 && Util.getCharAt(offset - 1) != '\n') {
                --offset;
            }
        }
        return offset;
    }
}
