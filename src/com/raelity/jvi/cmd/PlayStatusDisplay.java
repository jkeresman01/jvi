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

package com.raelity.jvi.cmd;

import javax.swing.JLabel;

import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.core.G;
import com.raelity.jvi.manager.ViManager;

/**
 *  A basic implementation of the vi status bar.
 * It uses three JLabels. See NbStatus bar for an
 * example that displays everything in a single label.
 */
public class PlayStatusDisplay implements ViStatusDisplay
{
    private JLabel generalStatus;
    private JLabel strokeStatus;
    private JLabel modeStatus;
    private boolean fFrozen;
    private int scrollCount;

    public PlayStatusDisplay(JLabel generalStatus, JLabel strokeStatus,
                         JLabel modeStatus)
    {
        this.generalStatus = generalStatus;
        this.strokeStatus = strokeStatus;
        this.modeStatus = modeStatus;
    }

    // ............


    @Override
    public void displayMode( String mode )
    {
        String s = mode + ( G.Recording() ? "recording" : "" );
        if ( s.isEmpty() ) {
            s = " ";
        }
        setText(modeStatus, s);
    }


    @Override
    public void displayCommand( String cmd )
    {
        setText(strokeStatus, cmd);
    }


    @Override
    public void displayStatusMessage( String msg )
    {
        fFrozen = false;
        setText(generalStatus, msg);
    }

    @Override
    public void displayErrorMessage( String msg )
    {
        fFrozen = false;
        // NEEDSWORK: make error message red or something
        setText(generalStatus, msg);
    }


    @Override
    public void displayWarningMessage( String msg )
    {
        fFrozen = false;
        // NEEDSWORK: make error message red or something
        setText(generalStatus, msg);
    }


    @Override
    public void displayFrozenMessage(String msg)
    {
        fFrozen = true;
        setText(generalStatus, msg);
    }


    @Override
    public void clearMessage()
    {
        if ( !fFrozen ) {
            setText(generalStatus, "");
        }
    }


    @Override
    public void clearDisplay()
    {
        setText(generalStatus, "");
        setText(modeStatus, "");
        setText(strokeStatus, "");
    }

    /**
     *  Don't need anything special here.
     */
    @Override
    public void refresh()
    {
    }

    @Override
    public void scrolling()
    {
        // let messages hang around a little more,
        // there is more scrolling that happens in swing...
        if(++scrollCount > 3) {
            clearMessage();
        }
    }


    synchronized void setText( final JLabel l00, String s00 )
    {
        scrollCount = 0;
        if ( l00 == generalStatus && s00.isEmpty() ) {
            s00 = " "; // need this to keep the status bar from collapsing
        }
        final String s01 = s00;
        ViManager.runInDispatch(false,
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        l00.setText(s01);
                                    } });
    }



} // end com.raelity.jvi.swing.PlayStatusDisplay

// vi: ts=8 sw=4
