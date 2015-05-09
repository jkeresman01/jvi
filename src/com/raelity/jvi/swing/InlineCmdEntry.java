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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.core.Options;

/**
 * A command line entry widget that sits on the glass pane,
 * instead of a modal dialog; the modal dialog is preferred.
 * This widget can avoid problems with interpreter bugs,
 * particularly on early linux interpreters, around 2002.
 *
 * There is some funny business with the rootPane and its defaultButton.
 * This is because with the glass pane usage, when InlineCmdEntry.java
 * is used doing something like ":ls[RETURN]" or searching
 * it ends up activating the default button.
 * The dialog goes away before the command line fires completion, otherwise
 * the completion event would be consumed and prevent the default button
 * (I think) and so this means its a glass pane timning/race issue.
 * Consider "Glasspane not catching keyboard events properly"
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4137681 .
 * NEEDSWORK: make this handling NB specific???
 * BTW, the problem might also be fixable using
 * KeyboardFocusManager/KeyEventDispatch.
 */
public class InlineCmdEntry extends CommandLine.CommandLineEntry {
    private MouseListener mouseListener;
    private boolean doneWithCommandLine;
    WeakReference<JRootPane> refRootPane;
    JButton button;
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
        Options.kd().println("InlineCommandEntry: finishActivate"); //REROUTE
        JRootPane rootPane = getRootPane();
        refRootPane = new WeakReference<JRootPane>(rootPane);
        JPanel glass = (JPanel)rootPane.getGlassPane();

        button = rootPane.getDefaultButton();
        rootPane.setDefaultButton(null);

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
        if(refRootPane == null)
            return; // wasn't activate, happens if generated passthru cmd line
        JRootPane rootPane = refRootPane.get();
        refRootPane.clear();
        if(rootPane == null)
            return; // we're in trouble
        rootPane.setDefaultButton(button);
        button = null;
        JPanel glass = (JPanel)rootPane.getGlassPane();
        glass.removeMouseListener(mouseListener);
        glass.setVisible(false);
        glass.remove(commandLine);
        
        // repaint area around entry right now so it looks faster
        JComponent jc = (JComponent)rootPane.getContentPane();
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
