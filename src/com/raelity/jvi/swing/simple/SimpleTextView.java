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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.swing.simple;

import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.swing.SwingTextView;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import javax.swing.text.AttributeSet;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Provides visual mode and search highlighting
 * using a JTextPane Style techniques.
 *
 * @author Ernie Rael <err at raelity.com>
 */
abstract public class SimpleTextView extends SwingTextView
{

    public SimpleTextView(JTextComponent editorPane)
    {
        super(editorPane);
    }

    /**
     * Update the search match highlight.
     */
    @Override
    public void updateHighlightSearchState()
    {
        if(!(editorPane.getDocument() instanceof StyledDocument))
                return;
        setupStyles();

        clear();
        int[] b = getBuffer()
                .getHighlightSearchBlocks(0, getBuffer().getLength());
        highlight(b, Options.searchColor);
    }


    boolean visualDisplayed;
    /**
     * Update the visual mode selection highlight.
     */
    @Override
    public void updateVisualState()
    {
        if(!(editorPane.getDocument() instanceof StyledDocument))
                return;
        setupStyles();

        int[] b = getBuffer().getVisualSelectBlocks(this, 0, Integer.MAX_VALUE);
        if (!G.VIsual_active || isEmpty(b)) {
            if(visualDisplayed)
                clear();
            visualDisplayed = false;
        }

        //dumpBlocks("blocks", b);
        highlight(b, Options.selectColor);
        visualDisplayed = true;
    }

    //////////////////////////////////////////////////////////////////////
    //
    // StyledDocument highlight "layer" methods
    //
    // When style doc highlights used,
    // should filter out doc CHANGE events from undo/redo
    //
    // CAN DO: PERFORMANCE ENHANCEMENTS
    //       - This implementation adds stuff to each document as needed.
    //         There are ways to improve and share stuff. For example, do:
    //             sc = new StyleContext();
    //             new JTextPane(new DefaultStyleDocument(sc))
    //         Then setupStyles and updateStyles could be done directly to the
    //         single shared "sc", rather than to each document individually.
    //
    //       - This implmentation only has one of visual or search highlights
    //         at a time. Both sets of current blocks could be kept and
    //         displayed as appropriate.
    //

    // Notice that the Document Styles are named like the jVi options.

    private static final String NO_HIGHLIGHT = "ViNoHighlight";

    boolean didInit;
    private void hlInit()
    {
        if(didInit)
            return;
        PropertyChangeListener pcl = new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent evt)
            {
                updateStyles();
                reApplyHighlight();
            }
        };
        Options.addPropertyChangeListener(Options.selectColor, pcl);
        Options.addPropertyChangeListener(Options.selectFgColor, pcl);
        Options.addPropertyChangeListener(Options.searchColor, pcl);
        Options.addPropertyChangeListener(Options.searchFgColor, pcl);
    }

    private void setupStyles()
    {
        hlInit();
        StyledDocument sdoc = (StyledDocument)editorPane.getDocument();
        Style s = sdoc.getStyle(Options.selectColor);
        if(s == null) {
            sdoc.addStyle(Options.selectColor, null);
            sdoc.addStyle(Options.searchColor, null);
            sdoc.addStyle(NO_HIGHLIGHT, null);
            updateStyles();
        }
    }

    private void updateStyles()
    {
        StyledDocument sdoc = (StyledDocument)editorPane.getDocument();
        Style s;

        s = sdoc.getStyle(Options.selectColor);
        Color color;
        color = Options.getOption(Options.selectColor).getColor();
        StyleConstants.setBackground(s, color);
        color = Options.getOption(Options.selectFgColor).getColor();
        if(color != null)
            StyleConstants.setForeground(s, color);
        else
            s.removeAttribute(StyleConstants.Foreground);

        s = sdoc.getStyle(Options.searchColor);
        color = Options.getOption(Options.searchColor).getColor();
        StyleConstants.setBackground(s, color);
        color = Options.getOption(Options.searchFgColor).getColor();
        if(color != null)
            StyleConstants.setForeground(s, color);
        else
            s.removeAttribute(StyleConstants.Foreground);
    }

    private boolean isEmpty(int[] a)
    {
        return a == null || a[0] == -1;
    }

    private int[] previousHighlightBlocks;
    AttributeSet previousHighlightAs;

    private void highlight(int[] blocks, String str)
    {
        StyledDocument sdoc = (StyledDocument)getEditorComponent().getDocument();
        AttributeSet as = sdoc.getStyle(str);
        if(as == null)
            return;
        if (previousHighlightBlocks != null &&
                !Arrays.equals(previousHighlightBlocks, blocks)) {
            clear();
        }
        applyHighlight(blocks, as);
        previousHighlightBlocks = blocks;
        previousHighlightAs = as;
    }

    private void clear() {
        if(isEmpty(previousHighlightBlocks))
            return;
        System.err.println("CLEAR");
        StyledDocument sdoc = (StyledDocument)editorPane.getDocument();
        AttributeSet as = sdoc.getStyle(NO_HIGHLIGHT);
        sdoc.setCharacterAttributes(0, getBuffer().getLength(), as, true);
        previousHighlightBlocks = null;
        previousHighlightAs = null;
    }

    private void reApplyHighlight()
    {
        if(!isEmpty(previousHighlightBlocks))
            applyHighlight(previousHighlightBlocks, previousHighlightAs);
    }

    private void applyHighlight(int[] blocks, AttributeSet as)
    {
        StyledDocument document = (StyledDocument)editorPane.getDocument();
        for (int i = 0; i < blocks.length; i += 2) {
            int start = blocks[i];
            int end = blocks[i + 1];
            if (start == -1 && end == -1) {
                // break
                return;
            }
            if (start > end) {
                int tmp = start;
                start = end;
                end = tmp;
            }
            document.setCharacterAttributes(start, end - start, as, false);
        }
    }

}
