/*
 * A command line entry widget that sits on the glass pane,
 * instead of a modal dialog; the modal dialog is preferred.
 * Can avoid problems with interpreter bugs, particularly
 * on early linux interpreters, around 2002.
 */

package com.raelity.jvi.swing;
import com.raelity.jvi.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

public class InlineCmdEntry extends CommandLine.CommandLineEntry {
    private MouseListener mouseListener;
    private boolean doneWithCommandLine;
    public InlineCmdEntry(){
        this(ViCmdEntry.COLON_ENTRY);
    }
    public InlineCmdEntry(int type){
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
            return SwingUtilities.getRootPane(tv.getEditorComponent());
        else
            return null;
    }
}
