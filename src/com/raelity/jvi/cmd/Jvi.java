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
/**
 *  Title:        jVi
 *  <p>
 *  Description:  A VI-VIM clone. Use VIM as a model where applicable.
 *  <p>
 *  Copyright:    Copyright &copy; Ernie Rael <br>
 *  Company:      Raelity Engineering
 *
 * @author Ernie Rael
 * @version 1.0
 */

package com.raelity.jvi.cmd;

import com.l2fprod.common.propertysheet.PropertySheetDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

import com.raelity.jvi.ColonCommands;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.swing.DefaultViFactory;
import com.raelity.jvi.swing.OptionsPanel;
import com.raelity.jvi.swing.StatusDisplay;
import com.raelity.jvi.swing.TextView;

public class Jvi
{
    /** If true, pack resulting UI. */
    public static boolean packFrame   = true;

    /** If true, make two frames. */
    public static boolean make2Frames = false;

    private static int nFrame = 0;              // total frame count
    private static JviFrame m_frame1 = null;
    private static JviFrame m_frame2 = null;    // test two jVi on same document


    /**
     *  Construct the frame-based application.
     */
    public static JviFrame makeFrame()
    {
        JviFrame frame = new JviFrame();
        nFrame++;

        JEditorPane editor = frame.getEditor();
        JScrollPane scrollPane = frame.getScrollPane();

        Font font = editor.getFont();
        editor.setFont(new Font("Monospaced",
                       font.getStyle(),
                       font.getSize()));
        font = editor.getFont();
        FontMetrics fm = editor.getFontMetrics(font);

        // Program the tabs, 8 chars per tab stop
        setTabs((JTextPane)editor, 8);

        int width = fm.charWidth(' ') * 81;
        int height = fm.getHeight() * 30;
        scrollPane.getViewport().setPreferredSize(new Dimension(width, height));

        // Validate frames that have preset sizes
        // Pack frames with useful preferred size info, e.g. from their layout
        if ( packFrame ) {
            frame.pack();
        } else {
            frame.validate();
        }
        // Center the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        int offset = (nFrame -1) * 50;
        frame.setLocation((screenSize.width - frameSize.width) / 2 + offset,
                         (screenSize.height - frameSize.height) / 2 + offset);
        frame.setVisible(true);
        return frame;
    }


    private static void setTabs( JTextPane tp, int ts )
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


    private static void setupFrame( final JviFrame f )
    {
        JEditorPane editor = f.getEditor();

        editor.setCaretColor(Color.black);

        TextView tv = (TextView)ViManager.getViTextView(editor);
        StatusDisplay sd = (StatusDisplay)tv.getStatusDisplay();
        sd.generalStatus = f.generalStatusBar;
        sd.strokeStatus  = f.strokeStatusBar;
        sd.modeStatus    = f.modeStatusBar;

        //((BooleanOption)Options.getOption(Options.dbgKeyStrokes)).setBoolean(true);
        ViManager.activateAppEditor(editor, null, "Jvi.setupFrame");
        ViManager.requestSwitch(editor);
    }


    /**
     *  Return frame 1.
     */
    public JFrame getFrame1()
    {
        return m_frame1;
    }


    /**
     *  Return frame 2, null if not created.
     */
    public JFrame getFrame2()
    {
        return m_frame2;
    }


    /**
     *  Main method.
     */
    public static void main( String[] args )
    {
        try {
          // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          // System.err.println("SETTING LOOK AND FEEL");
          // UIManager.setLookAndFeel("org.jvnet.substance.SubstanceLookAndFeel");
        } catch( Exception e ) {
            e.printStackTrace();
        }

        if ( args.length > 0 ) {
            make2Frames = true;
        }

        ViManager.setViFactory(new DefaultViFactory());

        ColonCommands.register("dumpOptions", "dumpOptions", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ViManager.getViFactory()
                            .getPreferences().exportSubtree(System.out);
                } catch (BackingStoreException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        ColonCommands.register("deleteOptions", "deleteOptions", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String keys[] = ViManager.getViFactory().getPreferences().keys();
                    for ( String key : keys ) {
                        ViManager.getViFactory().getPreferences().remove(key);
                    }
                } catch ( BackingStoreException ex ) {
                    ex.printStackTrace();
                }
            }
        });

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        Toolkit.getDefaultToolkit().setDynamicLayout(true);
                        m_frame1 = makeFrame();
                        m_frame1.optionsButton.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    showOptionsDialog(m_frame1);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                        setupFrame(m_frame1);
                        JEditorPane editor1 = m_frame1.getEditor();
                        if ( make2Frames ) {
                            m_frame2 = makeFrame();
                            JEditorPane editor2 = m_frame2.getEditor();
                            editor2.setDocument(editor1.getDocument());
                            setupFrame(m_frame2);
                        }
                        editor1.requestFocusInWindow();
                    }
                });
        } catch( Exception e ) {
            e.printStackTrace();
            //System.err.println( e.getClass().getName()
            //        + " thrown by main() [2]:  " + e.getMessage() );
        }

        // invoke and wait to make sure widget is fully drawn.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        ViManager.installKeymap(m_frame1.getEditor());
                        if ( make2Frames ) {
                            ViManager.installKeymap(m_frame2.getEditor());
                        }
                    }
                });
        } catch( Exception e ) {
            e.printStackTrace();
            //System.err.println( e.getClass().getName()
            //        + " thrown by main() [3]:  " + e.getMessage() );
        }
        // wait for frame to exit, so JUnitTest won't kill it
    }

    static private MyPropertySheetDialog dialog;
    static private OptionsPanel optionsPanel;

    static class MyPropertySheetDialog extends PropertySheetDialog {
        OptionsPanel op;

        // NEEDSWORK: move this to Jvi command.
        public MyPropertySheetDialog(OptionsPanel op,
                                     Frame owner,
                                     String title) throws HeadlessException
        {
            super(owner, title);
            this.op = op;
        }

        @Override
        public void cancel()
        {
            super.cancel();
            optionsPanel.cancel();
        }

        @Override
        public void ok()
        {
            super.ok();
        }

    }

    @SuppressWarnings("static-access")
    static void showOptionsDialog(Frame owner) {
        // if(dialog == null) {
        //     dialog = new JDialog(owner, "jVi Options");
        //     dialog.add("Center", getTabbedOptions());
        //     dialog.pack();
        // }
        // dialog.setVisible(true);
        if(dialog == null) {
            optionsPanel = new OptionsPanel(new OptionsPanel.ChangeNotify() {
                public void change()
                {
                    System.err.println("Property Change");
                }
            });
            dialog = new MyPropertySheetDialog(optionsPanel, owner, "jVi Options");
            dialog.getBanner().setVisible(false);
            dialog.getContentPane().add(optionsPanel, BorderLayout.EAST);
            //dialog.getContentPane().add(optionsPanel, BorderLayout.CENTER);

            // dialog.getButtonPane().add(new JButton("Default ALL"));
            // dialog.getButtonPane().add(new JButton("Set Default"));

            //dialog.setDialogMode(dialog.CLOSE_DIALOG);
            dialog.setDialogMode(dialog.OK_CANCEL_DIALOG);
            dialog.pack();
            dialog.centerOnScreen();
        }
        if(!dialog.isVisible()) {
            optionsPanel.load();
        }
        dialog.setVisible(true);
    }

} // end com.raelity.jvi.cmd.Jvi;
