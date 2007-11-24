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
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.awt.*;
import com.raelity.jvi.swing.*;
import com.raelity.jvi.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

public class Jvi {
  static boolean packFrame = true;
  static boolean make2 = false;
  static int nFrame = 0;
  static JviFrame frame1;
  static JviFrame frame2; // to test two jVi on same document

  //Construct the application
  public static JviFrame makeFrame() {
    JviFrame frame = new JviFrame();
    nFrame++;

    Font font = frame.editorPane.getFont();
    frame.editorPane.setFont(new Font("Monospaced",
				      font.getStyle(),
				      font.getSize()));
    font = frame.editorPane.getFont();
    FontMetrics fm = frame.editorPane.getFontMetrics(font);

    // Program the tabs, 8 chars per tab stop
    setTabs((JTextPane) frame.editorPane, 8);

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
    int offset = (nFrame -1) * 50;
    frame.setLocation((screenSize.width - frameSize.width) / 2 + offset,
                     (screenSize.height - frameSize.height) / 2 + offset);
    frame.setVisible(true);
    return frame;
  }

  private static void setTabs(JTextPane tp, int ts) {
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
      tp.getStyledDocument().setParagraphAttributes(0, length, attributes, false);  }
  
  private static void setupFrame(final JviFrame f) {

    // NEEDSWORK: editor is drawn, do rest in dispatch thread

    f.editorPane.setCaretColor(Color.black);

    ViManager.registerEditorPane(f.editorPane);
    ((BooleanOption)Options.getOption(Options.dbgKeyStrokes)).setBoolean(true);

    /*
    Font font = f.editorPane.getFont();
    f.editorPane.setFont(new Font("Monospaced",
				      font.getStyle(),
				      font.getSize()));
    font = f.editorPane.getFont();
    FontMetrics fm = f.editorPane.getFontMetrics(font);
    int width = fm.charWidth(' ') * 81;
    int height = fm.getHeight() * 40;
    f.editorPane.setSize(width, height);
    */

    TextView tv = (TextView)ViManager.getViTextView(f.editorPane);
    StatusDisplay sd = (StatusDisplay)tv.getStatusDisplay();
    sd.generalStatus = f.generalStatusBar;
    sd.strokeStatus = f.strokeStatusBar;
    sd.modeStatus = f.modeStatusBar;
    // G.setEditor(new TextView(f.editorPane, sd));
  }

  //Main method
  public static void main(String[] args) {
    try {
      //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      //System.err.println("SETTING LOOK AND FEEL");
      //UIManager.setLookAndFeel("org.jvnet.substance.SubstanceLookAndFeel");
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    
    if(args.length > 0) {
        make2 = true;
    }
    
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

    try {
      SwingUtilities.invokeAndWait(new Runnable() {
	public void run() {
            frame1 = makeFrame();
            setupFrame(frame1);
            if(make2) {
                frame2 = makeFrame();
                frame2.editorPane.setDocument(frame1.editorPane.getDocument());
                setupFrame(frame2);
            }
            frame1.editorPane.requestFocusInWindow();
	}   });
    } catch(Exception e) {}

    // invoke and wait to make sure widget is fully drawn.
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
	public void run() {
	  ViManager.installKeymap(frame1.editorPane);
          if(make2) {
              ViManager.installKeymap(frame2.editorPane);
          }
	}   });
    } catch(Exception e) {}
    
    // wait for frame to exit, so JUnitTest won't kill it
    
  }
}
