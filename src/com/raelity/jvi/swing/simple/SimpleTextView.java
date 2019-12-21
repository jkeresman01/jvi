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

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.AttributeSet;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.swing.SwingTextView;

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

        int[] b = getBuffer()
                .getHighlightSearchBlocks(0, getBuffer().getLength());
        highlight(b, Options.searchColor);
    }


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

        highlight(b, Options.selectColor);
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
    //       - to get things right in the face of changes to the document,
    //         can add doc listeners and recalc blocks when stuff changes.
    //         There is an example in the NB version.
    //
    // NEEDSWORK: doesn't highlight belong in Buffer, not TextView?
    //

    // Notice that the Document Styles are named like the jVi options.

    private static final String NO_HIGHLIGHT = "ViNoHighlight";

    boolean didInit;
    private void hlInit()
    {
        if(didInit)
            return;
        PropertyChangeListener pcl = (PropertyChangeEvent evt) -> {
            updateStyles();
            reApplyHighlight();
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

    Map<String, int[]> currentHighlights;

    private void highlight(int[] blocks, String str)
    {
        if(currentHighlights == null) {
            clearCurrentHighlights();
        }
        int[] prev = currentHighlights.get(str);
        // if data has not changed, then return
        for (int i = 0; i < prev.length; i++) {
            int j = prev[i];
            if(blocks[i] != j)
                break;
            if(j == -1)
                return; // they are equal
        }
        currentHighlights.put(str, blocks);

        applyCurentHighlights();

    }

    private void clearCurrentHighlights() {
        if(currentHighlights == null) {
            currentHighlights = new HashMap<>();
        }
        currentHighlights.put(Options.selectColor, new int[]{-1});
        currentHighlights.put(Options.searchColor, new int[]{-1});
    }

    private void clear() {
        StyledDocument sdoc = (StyledDocument)editorPane.getDocument();
        AttributeSet as = sdoc.getStyle(NO_HIGHLIGHT);
        sdoc.setCharacterAttributes(0, getBuffer().getLength(), as, true);

        clearCurrentHighlights();
    }

    private void reApplyHighlight()
    {
        applyCurentHighlights();
    }

    private void applyCurentHighlights()
    {
        StyledDocument sdoc = (StyledDocument)editorPane.getDocument();

        // simply redraw everything
        // first clear our any previous highlights
        sdoc.setCharacterAttributes(0, getBuffer().getLength(),
                                    sdoc.getStyle(NO_HIGHLIGHT), true);

        applyHighlight(currentHighlights.get(Options.searchColor),
                       sdoc.getStyle(Options.searchColor));
        applyHighlight(currentHighlights.get(Options.selectColor),
                       sdoc.getStyle(Options.selectColor));
    }

    private void applyHighlight(int[] blocks, AttributeSet as)
    {
        //System.err.println("HIGHLIGHT: " + dumpBlocks(blocks) + " " + as);
        StyledDocument document = (StyledDocument)editorPane.getDocument();
        for (int i = 0; i < blocks.length; i += 2) {
            int start = blocks[i];
            if (start == -1) {
                // break
                return;
            }
            int end = blocks[i + 1];
            if (start > end) {
                int tmp = start;
                start = end;
                end = tmp;
            }
            document.setCharacterAttributes(start, end - start, as, true);
        }
    }

    private String dumpBlocks(int[] b)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(b.length/2 -1).append("]{"); // nBlock
        for(int i = 0; i < b.length && i < 5; i += 2) { // only dump a few
            int start = b[i];
            if (start == -1)
                break;
            int end = b[i + 1];
            sb.append(start).append(",").append(end).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

}
