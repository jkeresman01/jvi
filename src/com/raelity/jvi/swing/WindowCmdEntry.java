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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.raelity.jvi.ViCmdEntry;

/**
 * This class provides a floating CommandLine entry widget.
 * A separate window must be allocated for each browser.
 * The window close button is disabled. <ESC> or <CR> must be
 * used to dismiss it.
 */
public class WindowCmdEntry extends CommandLine.CommandLineEntry {
    /** valid while active taking input */
    private CommandLineWindow commandLineWindow;
    // NEEDSWORK: WindowCmdEntry: on frame dispose, dispose of window
    
    String title;
    
    public WindowCmdEntry(ViCmdEntry.Type type) {
        super(type);
        title = type == ViCmdEntry.Type.SEARCH ? "Search Pattern" : "Command";
    }
    
    /**
     * Position the dialog and start taking input.
     */
    @Override
    public void finishActivate() {
        Window root = SwingUtilities.getWindowAncestor(tv.getEditor());
        if(commandLineWindow == null || commandLineWindow.getOwner() != root) {
            if(commandLineWindow != null) {
                commandLineWindow.remove(commandLine);
                commandLineWindow.dispose();
                commandLineWindow = null;
            }
            //JDialog.setDefaultLookAndFeelDecorated(false);
            commandLineWindow = CommandLineWindow.get(commandLine, root, title);
            //JDialog.setDefaultLookAndFeelDecorated(true);
        }

        Rectangle bounds = positionCommandEntry(root, commandLineWindow);

        // NEEDSWORK: avoid the validate by checking if anything changed?
        commandLineWindow.setSize(bounds.width, bounds.height);
        commandLineWindow.validate();

        Point p = bounds.getLocation();
        SwingUtilities.convertPointToScreen(p, root);
        commandLineWindow.setLocation(p.x, p.y);

        commandLine.takeFocus(true);
        commandLineWindow.setVisible(true);
    }
    
    @Override
    protected void prepareShutdown() {
        commandLineWindow.setVisible(false);
    }
    
    /**
     * A Dialog that holds a CommandLine, no decoration.
     */
    private static class CommandLineWindow extends JDialog {

        static private CommandLineWindow get(CommandLine commandLine,
                                   Window owner,
                                   String title)
        {
            if(!(owner instanceof Frame || owner instanceof Dialog))
                owner = null; // e.g. owner is a JWindow, only Frame,Dialog ok

            CommandLineWindow d = new CommandLineWindow(owner);

            d.setUndecorated(true);
            d.add(commandLine, BorderLayout.NORTH);
            d.pack();
            d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            return d;
        }

        private CommandLineWindow(Window owner) {
            super(owner, ModalityType.DOCUMENT_MODAL);
        }
    }
}
