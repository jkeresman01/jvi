/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.awt.*;
import com.raelity.jvi.swing.*;
import com.raelity.jvi.*;

public class Jvi {
  boolean packFrame = true;
  static JviFrame frame;

  //Construct the application
  public Jvi() {
    frame = new JviFrame();

    Font font = frame.editorPane.getFont();
    frame.editorPane.setFont(new Font("Monospaced",
				      font.getStyle(),
				      font.getSize()));
    font = frame.editorPane.getFont();
    FontMetrics fm = frame.editorPane.getFontMetrics(font);
    int width = fm.charWidth(' ') * 81;
    int height = fm.getHeight() * 30;
    //frame.editorPane.setSize(new Dimension(width, height));
    //frame.jScrollPane1.getViewport().setSize(width, height);
    frame.jScrollPane1.getViewport().setPreferredSize(new Dimension(width, height));
    //frame.jScrollPane1.setPreferredSize(new Dimension(width, height));
    //frame.jScrollPane1.invalidate();

    //Validate frames that have preset sizes
    //Pack frames that have useful preferred size info, e.g. from their layout
    if (packFrame) {
      frame.pack();
    }
    else {
      frame.validate();
    }
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);
  }

  //Main method
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    new Jvi();

    ViManager.setViFactory(new DefaultViFactory(null/*frame.commandLine1*/));
    ColonCommands.register("dumpOptions", "dumpOptions", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                ViManager.getViFactory().getPreferences().exportSubtree(System.out);
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
                for (String key : keys) {
                    ViManager.getViFactory().getPreferences().remove(key);
                }
            } catch (BackingStoreException ex) {
                ex.printStackTrace();
            }
        }
    });

    // NEEDSWORK: editor is drawn, do rest in dispatch thread

    frame.editorPane.setCaretColor(Color.black);

    ViManager.registerEditorPane(frame.editorPane);
    ((BooleanOption)Options.getOption(Options.dbgKeyStrokes)).setBoolean(true);

    /*
    Font font = frame.editorPane.getFont();
    frame.editorPane.setFont(new Font("Monospaced",
				      font.getStyle(),
				      font.getSize()));
    font = frame.editorPane.getFont();
    FontMetrics fm = frame.editorPane.getFontMetrics(font);
    int width = fm.charWidth(' ') * 81;
    int height = fm.getHeight() * 40;
    frame.editorPane.setSize(width, height);
    */

    TextView tv = (TextView)ViManager.getViTextView(frame.editorPane);
    StatusDisplay sd = (StatusDisplay)tv.getStatusDisplay();
    sd.generalStatus = frame.generalStatusBar;
    sd.strokeStatus = frame.strokeStatusBar;
    sd.modeStatus = frame.modeStatusBar;
    // G.setEditor(new TextView(frame.editorPane, sd));

    // invoke and wait to make sure widget is fully drawn.
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
	public void run() {
	  ViManager.installKeymap(frame.editorPane);
	}});
    } catch(Exception e) {}
    
    // wait for frame to exit, so JUnitTest won't kill it
    
  }
}
