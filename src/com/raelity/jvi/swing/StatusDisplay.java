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

import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.G;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 *  A basic implementation of the vi status bar.
 */
public class StatusDisplay implements ViStatusDisplay
{
    private JLabel generalStatus;
    private JLabel strokeStatus;
    private JLabel modeStatus;
    private boolean fFrozen;

    public StatusDisplay(JLabel generalStatus, JLabel strokeStatus,
                         JLabel modeStatus)
    {
        this.generalStatus = generalStatus;
        this.strokeStatus = strokeStatus;
        this.modeStatus = modeStatus;
    }

    // ............


    public void displayMode( String mode )
    {
        String s = mode + ( G.Recording ? "recording" : "" );
        if ( s.equals("") ) {
            s = " ";
        }
        setText(modeStatus, s);
    }


    public void displayCommand( String cmd )
    {
        setText(strokeStatus, cmd);
    }


    public void displayStatusMessage( String msg )
    {
        fFrozen = false;
        setText(generalStatus, msg);
    }

    public void displayErrorMessage( String msg )
    {
        fFrozen = false;
        // NEEDSWORK: make error message red or something
        setText(generalStatus, msg);
    }


    public void displayWarningMessage( String msg )
    {
        fFrozen = false;
        // NEEDSWORK: make error message red or something
        setText(generalStatus, msg);
    }


    public void displayFrozenMessage(String msg)
    {
        fFrozen = true;
        setText(generalStatus, msg);
    }


    public void clearMessage()
    {
        if ( !fFrozen ) {
            setText(generalStatus, "");
        }
    }


    synchronized void setText( JLabel l00, String s00 )
    {
        if ( l00 == generalStatus && s00.equals("") ) {
            s00 = " "; // need this to keep the status bar from collapsing
        }
        if ( SwingUtilities.isEventDispatchThread() ) {
            l00.setText(s00);
        } else {
            final JLabel l01 = l00;
            final String s01 = s00;
            SwingUtilities.invokeLater(
            new Runnable() {
                    public void run() {
                        l01.setText(s01);
                    } });
        }
    }


    /**
     *  Don't need anything special here.
     */
    public void refresh()
    {
    }


} // end com.raelity.jvi.swing.StatusDisplay

// vi: ts=8 sw=4
