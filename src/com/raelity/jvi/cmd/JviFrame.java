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

import com.raelity.jvi.ViTextView.Orientation;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

@SuppressWarnings("serial")
public class JviFrame extends JFrame
{
    /**
     *  If true, exit the application when the frame closes.
     *  For embedded applications this should be set false.
     */
    public static boolean SystemExitOnCloseFrame = true;

    protected JButton optionsButton;
    AbstractButton jviButton;
    protected JLabel generalStatusBar, strokeStatusBar, modeStatusBar;
    protected JLabel cursorStatusBar;
    protected PlayStatusDisplay statusDisplay;

    private Color m_color1 = new java.awt.Color(142,142,142),
                  m_color2 = new java.awt.Color(99,99,99);


    /**
     * Construct the frame.
     */
    @SuppressWarnings({"CallToThreadDumpStack", "CallToPrintStackTrace", "UseOfSystemOutOrSystemErr"})
    public JviFrame()
    {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            jbInit();
            statusDisplay = new PlayStatusDisplay(generalStatusBar, strokeStatusBar,
                                              modeStatusBar);
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName()
                    + " thrown by JviFrame():  " + e.getMessage() );
            e.printStackTrace();
        }
    }

    public PlayStatusDisplay getStatusDisplay() {
        return statusDisplay;
    }

    public JLabel getCursorStatusBar() {
        return cursorStatusBar;
    }


    /**
     *  Component initialization.
     */
    @SuppressWarnings("DeadBranch")
    private void jbInit() throws Exception
    {
        ImageIcon image1 = new ImageIcon(JviFrame.class.getResource("openFile.gif"));
        ImageIcon image2 = new ImageIcon(JviFrame.class.getResource("closeFile.gif"));
        ImageIcon image3 = new ImageIcon(JviFrame.class.getResource("help.gif"));
        final ImageIcon jvi_on = new ImageIcon(
                JviFrame.class.getResource("jViLogoToggle24_selected.png"));
        final ImageIcon jvi_off = new ImageIcon(
                JviFrame.class.getResource("jViLogoToggle24.png"));
        JPanel contentPane = (JPanel)this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        this.setSize(new Dimension(400, 285));
        this.setTitle("Frame Title");
        JMenu menuFile = new JMenu();
        menuFile.setText("File");
        JMenuItem menuFileExit = new JMenuItem();
        menuFileExit.setText("Exit");
        menuFileExit.addActionListener(this::fileExit_actionPerformed);
        JMenu menuHelp = new JMenu();
        menuHelp.setText("Help");
        JMenuItem menuHelpAbout = new JMenuItem();
        menuHelpAbout.setText("About");
        menuHelpAbout.addActionListener(this::helpAbout_actionPerformed);
        JButton jButton1 = new JButton(image1);
        jButton1.setToolTipText("Open File");

        JButton jButton2 = new JButton(image2);
        jButton2.setToolTipText("Close File");
        JButton helpButton = new JButton(image3);
        helpButton.setToolTipText("Help");
        optionsButton = new JButton("Options");
        optionsButton.setToolTipText("Options");

        if(true) {
            jviButton = new JToggleButton(jvi_off);
            jviButton.setSelectedIcon(jvi_on);
            jviButton.setSelected(true);
            //jviButton.setContentAreaFilled(false);
            jviButton.setFocusPainted(false);
            jviButton.setPreferredSize(new Dimension(24, 24));
            // was: ()->editorPane.setEditable(jviButton.isSelected());
            jviButton.addActionListener(Util::setCurrentEditable);
        } else {
            jviButton = new JButton(jvi_off);
            jviButton.setSelectedIcon(jvi_on);
            //jviButton.setContentAreaFilled(false);
            jviButton.setFocusPainted(false);
            jviButton.setPreferredSize(new Dimension(24, 24));
            jviButton.addActionListener((ActionEvent e) -> {
                jviButton.setSelected(!jviButton.isSelected());
            });
        }
        System.err.println("jviButton UI: "
                + jviButton.getUI().getClass().getName() );
        System.err.println("jviButton isContentAreaFilled: "
                + jviButton.isContentAreaFilled());

        JPanel mainPanel = new MainPanel();
        mainPanel.setLayout(new BorderLayout());
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new GridBagLayout());

        generalStatusBar = new JLabel();
        generalStatusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,m_color1,m_color2),
                BorderFactory.createEmptyBorder(0,2,0,0)));
        generalStatusBar.setText("commandInputAndGeneralStatus");

        strokeStatusBar = new JLabel();
        strokeStatusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,m_color1,m_color2),
                BorderFactory.createEmptyBorder(0,2,0,0)));
        strokeStatusBar.setMinimumSize(new Dimension(60,21));
        strokeStatusBar.setPreferredSize(new Dimension(60,0));
        strokeStatusBar.setText("strokes");

        modeStatusBar = new JLabel();
        modeStatusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,m_color1,m_color2),
                BorderFactory.createEmptyBorder(0,2,0,0)));
        modeStatusBar.setMinimumSize(new Dimension(80,4));
        modeStatusBar.setPreferredSize(new Dimension(80,4));

        cursorStatusBar = new JLabel();
        cursorStatusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,m_color1,m_color2),
                BorderFactory.createEmptyBorder(0,2,0,0)));
        cursorStatusBar.setMinimumSize(new Dimension(100,4));
        cursorStatusBar.setPreferredSize(new Dimension(100,4));

        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,m_color1,m_color2),
                BorderFactory.createEmptyBorder(2,0,2,0)));
        JToolBar toolBar = new JToolBar();
        toolBar.add(jButton1);
        toolBar.add(jButton2);
        toolBar.add(optionsButton);
        toolBar.add(jviButton);
        toolBar.add(helpButton);
        menuFile.add(menuFileExit);
        menuHelp.add(menuHelpAbout);
        JMenuBar menuBar1 = new JMenuBar();
        menuBar1.add(menuFile);
        menuBar1.add(menuHelp);
        this.setJMenuBar(menuBar1);
        contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(mainPanel, BorderLayout.CENTER);
        PlayEditorContainer editorC1 = new PlayEditorContainer(this);
        if(false) {
            PlayEditorContainer editorC2 = new PlayEditorContainer(this);
            PlaySplitter sp = new PlaySplitter(Orientation.LEFT_RIGHT,
                    editorC1, editorC2);
            mainPanel.add(sp, BorderLayout.CENTER);
        } else {
            mainPanel.add(editorC1, BorderLayout.CENTER);
        }
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        statusPanel.add(generalStatusBar, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 111, 0));
        statusPanel.add(strokeStatusBar, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));
        statusPanel.add(modeStatusBar, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));
        statusPanel.add(cursorStatusBar, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));
    }


    /**
     *  File | Exit action performed.
     */
    public void fileExit_actionPerformed( ActionEvent e )
    {
        if ( SystemExitOnCloseFrame ) {
            System.exit(0);
        }
    }


    /**
     *  Help | About action performed.
     */
    public void helpAbout_actionPerformed( ActionEvent e )
    {
        System.err.println("unimplemented.");
    }


    /**
     * Overridden so we can exit when window is closed.
     * @override
     */
    @Override
    protected void processWindowEvent( WindowEvent e )
    {
        super.processWindowEvent(e);
        if ( e.getID() == WindowEvent.WINDOW_CLOSING ) {
            fileExit_actionPerformed(null);
        }
    }

} // end com.raelity.jvi.cmd.JviFrame


