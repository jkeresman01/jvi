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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

import org.openide.util.WeakListeners;

import com.raelity.jvi.ViCaret;
import com.raelity.jvi.ViCaretStyle;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.manager.Scheduler;
import com.raelity.jvi.manager.ViManager;

/**
 * This extension of {@link javax.swing.text.DefaultCaret} draws the
 * caret in different ways as defined by the cursor property.
 *
 * Turns out don't need NavigationFilter
 *
 * // NEEDSWORK: cache the current font metric, listen to font property changes
 */
public class SwingCaret extends DefaultCaret implements ViCaret
{
    SwingPaintCaret viDelegate;

    public SwingCaret()
    {
        super();
        viDelegate = new SwingPaintCaret(this);
        addChangeListener((ChangeEvent e) -> {
            Scheduler.cursorChange(SwingCaret.this);
        });

        Preferences prefs = ViManager.getFactory().getPreferences();
        prefs.addPreferenceChangeListener(WeakListeners.create(
                PreferenceChangeListener.class, prefsListener, prefs));
    }

    private final PreferenceChangeListener prefsListener
            = (PreferenceChangeEvent evt) -> {
                if(evt.getKey().equals(Options.caretBlinkRate)) {
                    int n = Options.getOption(Options.caretBlinkRate).getInteger();
                    setBlinkRate(n);
                }
    };

    @Override
    public void setCursor(ViCaretStyle cursor)
    {
        viDelegate.setCursor(cursor);
    }

    @Override
    public ViCaretStyle getCursor()
    {
        return viDelegate.getCursor();
    }

    @Override
    protected synchronized void damage(Rectangle r)
    {
        if (viDelegate.damage(this, r)) {
            repaint();
        }
    }

    @Override
    protected void adjustVisibility(Rectangle nloc)
    {
        Rectangle r = new Rectangle();
        viDelegate.damage(r, nloc); // broaden to encompass whole character
        super.adjustVisibility(r);
    }

    /** make sure the not-focused caret is drawn */
    @Override
    public void focusLost(FocusEvent e)
    {
        repaint();
    }

    /**
     * Render the caret as specified by the cursor.
     * <br/>
     * Note: might want to check editor manager, and if not vi then
     * call super(paint), same for super(damage)
     */
    @Override
    public void paint(Graphics g)
    {
        viDelegate.paint(g, isVisible(), null, getTextComponent());
    }

    @Override
    public JTextComponent getTextComponent()
    {
        return super.getComponent();
    }
}

