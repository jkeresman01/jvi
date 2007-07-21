/*
 * A command line entry widget that sits on the glass pane.
 * Doing this, instead of a modal dialog, avoids problems
 * with interpreter bugs, particularly on linux.
 */

package com.raelity.jvi.swing;
import com.raelity.jvi.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

public class InlineCmdEntry extends CommandLine.CommandLineEntry {
    //protected int entryType;
    //protected ActionListener listener;
    //protected CommandLine commandLine;
    //protected String lastCommand;
    //protected ViTextView parentTV;
    //private String initialText;
    private MouseListener mouseListener;
    public InlineCmdEntry(){
        this(ViCmdEntry.COLON_ENTRY);
    }
    public InlineCmdEntry(int type){
        super(type);

        // NEEDSWORK: FOCUS: use FocusTraversalPolicy
        commandLine.setNextFocusableComponent(commandLine);
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
        commandLine.takeFocus(true);
    };

    protected void prepareShutdown(){
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
