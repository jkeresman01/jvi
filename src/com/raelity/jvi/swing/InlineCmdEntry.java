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
import com.raelity.jvi.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

/**
 * A command line entry widget that sits on the glass pane,
 * instead of a modal dialog; the modal dialog is preferred.
 * This widget can avoid problems with interpreter bugs,
 * particularly on early linux interpreters, around 2002.
 */
public class InlineCmdEntry extends CommandLine.CommandLineEntry {
    private MouseListener mouseListener;
    private boolean doneWithCommandLine;
    public InlineCmdEntry(){
        this(ViCmdEntry.Type.COLON);
    }
    public InlineCmdEntry(ViCmdEntry.Type type){
        super(type);

        // rather than screwing with the FocusTraversalPolicy,
        // simply prevent the command line from giving up focus
        // until it is ready to be dismissed.
        commandLine.getTextComponent().setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                return doneWithCommandLine;
            }
        });
        mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                commandLine.getToolkit().beep();
            }
        };
    }

    @Override
    public void finishActivate() {
        JPanel glass = (JPanel)getRootPane().getGlassPane();
        commandLine.setBounds(positionCommandEntry(glass, commandLine));
        if(glass.getLayout() != null) {
            glass.setLayout(null);
        }
        glass.setVisible(true);
        glass.addMouseListener(mouseListener);
        // by placing p.add(commandLine) after the p.setVisible
        // a blanking and redraw of the combo box is avoided.
        glass.add(commandLine);
        doneWithCommandLine = false;
        commandLine.takeFocus(true);
    };

    @Override
    protected void prepareShutdown(){
        doneWithCommandLine = true;
        //commandLine.removeActionListener(this);
        JPanel glass = (JPanel)getRootPane().getGlassPane();
        glass.removeMouseListener(mouseListener);
        glass.setVisible(false);
        glass.remove(commandLine);
        
        // repaint area around entry right now so it looks faster
        JComponent jc = (JComponent)getRootPane().getContentPane();
        Rectangle pos = commandLine.getBounds();
        Point p00 = SwingUtilities.convertPoint(glass,
                pos.x, pos.y, jc);
        pos.setLocation(p00);
        jc.paintImmediately(pos);
    }
    
    final JRootPane getRootPane(){
        if(tv!=null)
            return SwingUtilities.getRootPane(tv.getEditor());
        else
            return null;
    }
}
