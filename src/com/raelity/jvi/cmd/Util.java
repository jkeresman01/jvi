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

import com.raelity.jvi.ViTextView.Direction;
import com.raelity.jvi.ViWindowNavigator;
import com.raelity.jvi.ViWindowNavigator.SplitterNode;
import com.raelity.jvi.manager.AppViews;
import com.raelity.jvi.manager.ViManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

/**
 *
 * @author err
 */
public class Util
{
    static void setCurrentEditable(ActionEvent e)
    {
        PlayAppView av = (PlayAppView) AppViews.getList(AppViews.MRU).get(0);
        JviFrame frame = (JviFrame) av.getFrame();
        ((JTextPane)av.getEditor()).setEditable(frame.jviButton.isSelected());
    }

    static void setTabs( JTextPane tp, int ts )
    {
        FontMetrics fm = tp.getFontMetrics( tp.getFont() );
        int charWidth = fm.charWidth( 'w' );
        int tabWidth = charWidth * ts;

        TabStop[] tabs = new TabStop[10];

        for (int j = 0; j < tabs.length; j++) {
            int tab = j + 1;
            tabs[j] = new TabStop( tab * tabWidth );
        }

        TabSet tabSet = new TabSet(tabs);
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setTabSet(attributes, tabSet);
        int length = tp.getDocument().getLength();
        tp.getStyledDocument()
                .setParagraphAttributes(0, length, attributes, false);
    }

    // just drop the closed editor, and it's splitter on the floor
    static void winClose(PlayAppView av) {
        ViWindowNavigator nav = ViManager.getFactory().getWindowNavigator();
        SplitterNode sn = nav.getParentSplitter(av);
        // take the other editor from splitter
        // and use it to replace splitter in parentSplitter
        if(sn != null) {
            SplitterNode snParent = nav.getParentSplitter(sn);
            PlaySplitter splitter = (PlaySplitter) sn.getComponent();
            // edC's getting discarded
            Component edC = av.getContainer();
            edC = splitter.getOther(edC);
            if(snParent != null) {
                ((PlaySplitter) snParent.getComponent())
                        .replaceEditorContainer(splitter, edC);
            } else {
                Container c = SwingUtilities.getAncestorOfClass(
                        MainPanel.class, edC);
                c.remove(splitter);
                c.add(edC);
                c.revalidate();
            }
            AppViews.close(av);

        } else {
            // don't let the last editor close
            com.raelity.jvi.core.Util.beep_flush();
        }
        av = (PlayAppView) AppViews.getMruAppView(0);
        if(av != null)
            ViManager.getFS().edit(av, true);
    }

    static void winSplit(PlayAppView av, Direction dir,
                         int n, PlayAppView avNew)
    {
        // ignore n for now
        ViWindowNavigator nav = ViManager.getFactory().getWindowNavigator();
        SplitterNode sn = nav.getParentSplitter(av);

        PlaySplitter parentSplitter = null;
        Direction parentPosition = null;
        Container c = null;
        if(sn != null) {
            parentSplitter = (PlaySplitter) sn.getComponent();
            // The new splitter replaces the av, remember where it was
            parentPosition = parentSplitter.getPosition(av.getContainer());
        } else {
            c = SwingUtilities.getAncestorOfClass(
                    MainPanel.class, av.getContainer());
        }

        // create new splitter as directed
        PlaySplitter splitter = new PlaySplitter(dir.getOrientation());
        splitter.putEditorContainer(avNew.getContainer(), dir);
        splitter.putEditorContainer(av.getContainer(), dir.getOpposite());

        if(parentSplitter != null) {
            parentSplitter.putEditorContainer(splitter, parentPosition);
        } else {
            // probably just the parent
            c.add(splitter);
        }

        ViManager.getFS().edit(avNew, true);
    }
}
