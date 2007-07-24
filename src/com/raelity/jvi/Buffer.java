/*
 * Buffer.java
 *
 * Created on March 5, 2007, 11:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi;

import javax.swing.event.DocumentEvent;

import static com.raelity.jvi.Constants.*;

/**
 * Buffer: structure that holds information about one file, primarily
 * per file options.
 *
 * Several windows can share a single Buffer.
 *
 * @author erra
 */
public abstract class Buffer implements ViBuffer, ViOptionBag {
    private boolean didCheckModelines;
    
    private int share; // the number of text views sharing this buffer
    public int getShare() { return share; }
    public void addShare() { share++; }
    public void removeShare() {
        share--;
    }
    
    /** Creates a new instance of Buffer, initialize values from Options.
     * NOTE: tv is not completely "constructed".
     */
    public Buffer(ViTextView tv) {
        b_visual_start = createMark();
        b_visual_end = createMark();
        b_op_start = createMark();
        b_op_end = createMark();

        initOptions();
    }
    
    protected void initOptions() {
        b_p_ts = Options.getOption(Options.tabStop).getInteger();
        b_p_sw = Options.getOption(Options.shiftWidth).getInteger();
        b_p_et = Options.getOption(Options.expandTabs).getBoolean();
        b_p_tw = Options.getOption(Options.textWidth).getInteger();
    }

    public void viOptionSet(ViTextView tv, String name) {
    }
    
    /** from switchto */
    public void activateOptions(ViTextView tv) {
    }
    
    /** from switchto, everything else has been setup */
    public void checkModeline() {
        if(didCheckModelines)
            return;
        didCheckModelines = true;
        Options.processModelines();
    }
    
    //////////////////////////////////////////////////////////////////////
    //
    // Declare the variables referenced as part of a ViOptionBag
    //
    
    public int b_p_ts;     // tab stop
    public int b_p_sw;     // shiftw width
    public boolean b_p_et;     // expand tabs
    public int b_p_tw;     // text width
    
    //////////////////////////////////////////////////////////////////////
    //
    // Other per buffer variables
    //

    public ViMark getMark(int i) {
        if(marks[i] == null)
            marks[i] = createMark();
        return marks[i];
    }
    // The lower case marks
    private ViMark marks[] = new ViMark[26];
    
    // Save the current VIsual area for '< and '> marks, and "gv"
    public final ViMark b_visual_start;
    public final ViMark b_visual_end;
    public char b_visual_mode;
    public String b_p_mps; // used in nv_object

    // start and end of an operator, also used for '[ and ']
    public final ViMark b_op_start;
    public final ViMark b_op_end;

    //////////////////////////////////////////////////////////////////////
    //
    //

    protected String getRemovedText(DocumentEvent e) {
        return null;
    }

    protected boolean isInsertMode() {
        return (G.State & BASE_STATE_MASK) == INSERT;
    }

    protected void docInsert(int offset, String s) {
        GetChar.docInsert(offset, s);
    }

    protected void docRemove(int offset, int length, String s) {
        GetChar.docRemove(offset, length, s);

    }

}

// vi: set sw=4 ts=8:
