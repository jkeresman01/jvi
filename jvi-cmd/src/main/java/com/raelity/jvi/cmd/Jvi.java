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

import com.raelity.lib.ui.Screens;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.nio.file.FileSystems;

import javax.swing.text.JTextComponent;

import com.l2fprod.common.propertysheet.PropertySheetDialog;

import com.raelity.jvi.ViAppView;

import com.raelity.jvi.manager.AppViews;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.swing.ui.options.OptionsPanel;

import java.util.List;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.text.Document;

import com.raelity.jvi.*;
import com.raelity.jvi.cmd.nb.*;
import com.raelity.jvi.core.*;
import com.raelity.jvi.options.*;

/**
 * The following are the hooks into jVi used in this class.
 * <br>The AppViews one is optional.
 * <br>The statusDisplay is for single line output.
 * <br>Note that bulk output via PlayOutputStream goes to System.err.
 * <br>Following are some key hookups.
 * <pre>
 *      ViManager.setViFactory(new PlayFactory());
 *      AppViews.open(new PlayAppView(f, editor), "Jvi.setupFrame");
 *      PlayFactory.installKeymap(m_frame1.getEditor());
 *      static private MyPropertySheetDialog dialog;
 * </pre>
 * 
 * @author Ernie Rael <err at raelity.com>
 */
public class Jvi
{
    // keep a reference to this so we can set it's level...
    // private static Logger exceptionsLogger
    //         = Logger.getLogger(org.openide.util.Exceptions.class.getName());

    static Frame mainWindow;

    /** If true, pack resulting UI. */
    public static boolean packFrame   = true;

    /** If true, make two frames. */
    public static boolean make2Frames = false;

    private static int nFrame = 0;              // total frame count

        // CAN'T USE THIS, IT GETS INCLUDED IN NORMAL JVI
        // SHOULD PUT jVi DEBUG INTO /test/ I GUESS
        // @ServiceProvider(service=ViInitialization.class,
        //         path="jVi/init",
        //         position=3)
        // so just call (new Init()).init() it directly
        public static class Init implements ViInitialization
        {
        @Override
        public void init()
        {
            OptUtil.setupPlatformDesc(Options.searchColor, "Testline1\nTestLine2");
            OptUtil.setupPlatformDesc(Options.searchFgColor, "Testline4\nTestLine3");
            OptUtil.setupPlatformDesc(Options.selectColor, "Testline1\nTestLine2");
            OptUtil.setupPlatformDesc(Options.selectFgColor, "Testline4\nTestLine3");
        }
        }

    /**
     *  Construct the frame-based application.
     * @return 
     */
    public static JviFrame makeFrame()
    {
        JviFrame frame = new JviFrame();
        nFrame++;
        //frame.setLocationByPlatform(true);

        if ( packFrame ) {
            frame.pack();
        } else {
            frame.validate();
        }

        Dimension screenSize = Screens.getPrefScreenBounds().getSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        int offset = (nFrame -1) * 50;
        // looks like this is centering more or less
        frame.setLocation((screenSize.width - frameSize.width) / 2 + offset,
                         (screenSize.height - frameSize.height) / 2 + offset);
        Screens.translateToPrefScreen(frame);
        frame.setVisible(true);
        return frame;
    }

    /**
     *  Main method.
     * @param args
     */
    @SuppressWarnings({"CallToThreadDumpStack", "CallToPrintStackTrace", "UseOfSystemOutOrSystemErr"})
    public static void main( String[] args )
    {
        for(LookAndFeelInfo lookAndFeel : UIManager.getInstalledLookAndFeels()) {
            System.err.println("L&F: " + lookAndFeel.getName());
        }

        if(null == Screens.setPrefGraphicsDev("foobar")
                && null == Screens.setPrefGraphicsDev("jvi")
                && null == Screens.setPrefGraphicsDev("NETBEANS_PREFERRED_SCREEN"))
            Screens.setPrefGraphicsDev("JAVA_PREFERRED_SCREEN");
        // java.awt.Window.locationByPlatform
        // "true"
        // Window.setLocationRelativeTo
        System.setProperty("java.awt.Window.locationByPlatform", "true");
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

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(new JviErrorManager());
        //exceptionsLogger.setLevel(Level.INFO);

        System.err.println("Java verion: " + System.getProperty("java.version"));

        ViManager.setViFactory(new PlayFactory());

        try {
            ViManager.runInDispatch(true, () -> {
                Toolkit.getDefaultToolkit().setDynamicLayout(true);
                JviFrame frame = makeFrame();
                mainWindow = frame;
                frame.optionsButton.addActionListener((ActionEvent e) -> {
                    try {
                        showOptionsDialog(frame);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                List<ViAppView> avs = AppViews.getList(AppViews.ACTIVE);
                PlayAppView av = (PlayAppView)avs.get(0);
                JTextComponent ed = av.getEditor();
                Document doc = ed.getDocument();
                doc.putProperty(Document.TitleProperty, FileSystems
                        .getDefault().getPath("/tmp/test.file"));
                ed.requestFocusInWindow();
                (new Init()).init();
            });
        } catch( Exception e ) {
            e.printStackTrace();
            //System.err.println( e.getClass().getName()
            //        + " thrown by main() [2]:  " + e.getMessage() );
        }

        // invoke and wait to make sure widget is fully drawn.
        // wait for frame to exit, so JUnitTest won't kill it
    }

    static private JDialog dialog;
    static private OptionsPanel optionsPanel;

    @SuppressWarnings("serial")
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
            optionsPanel.ok();
        }

    }

    @SuppressWarnings({"static-access", "UseOfSystemOutOrSystemErr"})
    static void showOptionsDialog(Frame owner) {
        // if(dialog == null) {
        //     dialog = new JDialog(owner, "jVi Options");
        //     dialog.add("Center", getTabbedOptions());
        //     dialog.pack();
        // }
        // dialog.setVisible(true);

        // set owner to null so options work
        // while document modal WindowCmdLine is active.
        Point target = owner != null ? owner.getLocation() : null;
        owner = null;

        if(dialog == null) {
            //dialog = getL2fDialog(owner);
            dialog = getOkApplyCancelDialog(owner);
        }
        Screens.translateToPrefScreen(dialog, target);
        // TODO: what is the following about?
        if(!dialog.isVisible()) {
            optionsPanel.load();
        }
        dialog.setVisible(true);
    }

    static JDialog getOkApplyCancelDialog(Frame owner)
    {
        OptionsDialog dlg = new OptionsDialog(owner);
        optionsPanel = dlg.optionsPanel;
        dlg.pack();
        return dlg;
    }

    static JDialog getL2fDialog(Frame owner)
    {
        optionsPanel = new OptionsPanel(() -> {
            System.err.println("Property Change");
        });
        MyPropertySheetDialog dlg
                = new MyPropertySheetDialog(optionsPanel, owner, "jVi Options");
        dlg.getBanner().setVisible(false);
        dlg.getContentPane().add(optionsPanel, BorderLayout.EAST);
        //dlg.getContentPane().add(optionsPanel, BorderLayout.CENTER);
        
        // dlg.getButtonPane().add(new JButton("Default ALL"));
        // dlg.getButtonPane().add(new JButton("Set Default"));
        
        //dlg.setDialogMode(dlg.CLOSE_DIALOG);
        dlg.setDialogMode(dlg.OK_CANCEL_DIALOG);
        dlg.pack();
        dlg.centerOnScreen();

        return dlg;
    }

} // end com.raelity.jvi.cmd.Jvi;
