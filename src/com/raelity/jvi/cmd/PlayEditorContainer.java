/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2019 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.cmd;

import com.raelity.jvi.manager.AppViews;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.CaretEvent;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

/**
 * An editor is packaged with it's scrollpane
 * @author err
 */
public class PlayEditorContainer extends JScrollPane
{
    private final PlayEditorPane editor;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public PlayEditorContainer(JviFrame f) {
        editor = new PlayEditorPane();
        getViewport().add(editor, null);
        editor.setCaretColor(Color.black);
        PlayAppView av = new PlayAppView(f, this);
        AppViews.open(av, "playEditorComponent");

        switch (av.getWinID()) {
        case 1:
            editor.setText(SampleText.txt01);
            break;
        case 2:
            // editor.setText(SampleText.txt02);
            // break;
        default:
            StringBuilder sb = new StringBuilder();
            String t = "aaaaaaaaa\n";
            for(int i = 0; i < 10; i++) {
                char c = (char) ('A' + ((av.getWinID() - 1 + i) % 26));
                sb.append(av.getWinID()).append(" - ")
                        .append(t.replace('a', c));
            }
            editor.setText(sb.toString());
            break;
        }

        final JLabel jl = f.getCursorStatusBar();
        editor.addCaretListener((CaretEvent e) -> {
            JTextComponent jtc = (JTextComponent)e.getSource();
            Document doc = jtc.getDocument();
            int dot = e.getDot();
            Element root = doc.getDefaultRootElement();
            int l = root.getElementIndex(dot);
            Element elem = root.getElement(l);
            int col = dot - elem.getStartOffset();
            jl.setText("" + (l+1) + "-" + col + " <" + dot +">");
        });

        Font font = editor.getFont();
        editor.setFont(new Font("Monospaced",
                       font.getStyle(),
                       font.getSize()));
        font = editor.getFont();
        FontMetrics fm = editor.getFontMetrics(font);

        // Program the tabs, 8 chars per tab stop
        Util.setTabs(editor, 8);

        int width = fm.charWidth(' ') * 40;
        int height = fm.getHeight() * 30;
        getViewport().setPreferredSize(new Dimension(width, height));

        PlayFactory.installKeymap(editor);

        editor.getCaret().setDot(0);
    }

    JEditorPane getEditor() {
        return (JEditorPane) getViewport().getComponent(0);
    }
}
