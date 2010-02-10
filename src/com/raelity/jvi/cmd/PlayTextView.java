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

package com.raelity.jvi.cmd;

import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.swing.SwingTextView;
import java.awt.Color;
import java.util.Arrays;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class PlayTextView extends SwingTextView
{
    public PlayTextView(final JTextComponent editor, ViStatusDisplay sd)
    {
        super(editor);
        this.statusDisplay = sd;
    }

    //////////////////////////////////////////////////////////////////////
    //
    // Visual Select
    //

    /**
     * Update the selection highlight.
     */
    @Override
    public void updateVisualState()
    {
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

    //////////////////////////////////////////////////////////////////////
    //
    // Highlight Search
    //

    @Override
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
            StyledEditorKit k = (StyledEditorKit)getEditorKit();
            MutableAttributeSet inputAttrs = k.getInputAttributes();
            inputAttrs.addAttributes(mas);
        }
    }

}
