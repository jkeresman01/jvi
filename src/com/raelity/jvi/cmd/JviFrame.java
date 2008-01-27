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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.DefaultStyledDocument;

public class JviFrame extends JFrame {
  JPanel contentPane;
  JMenuBar menuBar1 = new JMenuBar();
  JMenu menuFile = new JMenu();
  JMenuItem menuFileExit = new JMenuItem();
  JMenu menuHelp = new JMenu();
  JMenuItem menuHelpAbout = new JMenuItem();
  JToolBar toolBar = new JToolBar();
  JButton jButton1 = new JButton();
  JButton jButton2 = new JButton();
  JButton helpButton = new JButton();
  JButton optionsButton = new JButton();
  ImageIcon image1;
  ImageIcon image2;
  ImageIcon image3;
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel1 = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JScrollPane jScrollPane1 = new JScrollPane();
  JPanel statusPanel = new JPanel();
  JLabel generalStatusBar = new JLabel();
  JLabel strokeStatusBar = new JLabel();
  JLabel modeStatusBar = new JLabel();
  Border border1;
  Border border2;
  Border border3;
  Border border4;
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JEditorPane editorPane = new JTextPane();

  //Construct the frame
  public JviFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  //Component initialization
  private void jbInit() throws Exception  {
    image1 = new ImageIcon(JviFrame.class.getResource("openFile.gif"));
    image2 = new ImageIcon(JviFrame.class.getResource("closeFile.gif"));
    image3 = new ImageIcon(JviFrame.class.getResource("help.gif"));
    contentPane = (JPanel) this.getContentPane();
    border1 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,new java.awt.Color(142, 142, 142),new java.awt.Color(99, 99, 99)),BorderFactory.createEmptyBorder(0,2,0,0));
    border2 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,new java.awt.Color(142, 142, 142),new java.awt.Color(99, 99, 99)),BorderFactory.createEmptyBorder(0,2,0,0));
    border3 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,new java.awt.Color(142, 142, 142),new java.awt.Color(99, 99, 99)),BorderFactory.createEmptyBorder(0,2,0,0));
    border4 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new java.awt.Color(142, 142, 142),new java.awt.Color(99, 99, 99)),BorderFactory.createEmptyBorder(2,0,2,0));
    contentPane.setLayout(borderLayout1);
    this.setSize(new Dimension(400, 285));
    this.setTitle("Frame Title");
    menuFile.setText("File");
    menuFileExit.setText("Exit");
    menuFileExit.addActionListener(new ActionListener()  {

      public void actionPerformed(ActionEvent e) {
        fileExit_actionPerformed(e);
      }
    });
    menuHelp.setText("Help");
    menuHelpAbout.setText("About");
    menuHelpAbout.addActionListener(new ActionListener()  {

      public void actionPerformed(ActionEvent e) {
        helpAbout_actionPerformed(e);
      }
    });
    jButton1.setIcon(image1);
    jButton1.setToolTipText("Open File");
    jButton2.setIcon(image2);
    jButton2.setToolTipText("Close File");
    helpButton.setIcon(image3);
    helpButton.setToolTipText("Help");
    optionsButton.setText("Options");
    optionsButton.setToolTipText("Options");
    jPanel1.setLayout(borderLayout2);
    statusPanel.setLayout(gridBagLayout1);
    generalStatusBar.setBorder(border1);
    generalStatusBar.setText("commandInputAndGeneralStatus");
    strokeStatusBar.setBorder(border2);
    strokeStatusBar.setMinimumSize(new Dimension(60, 21));
    strokeStatusBar.setPreferredSize(new Dimension(60, 0));
    strokeStatusBar.setText("strokes");
    modeStatusBar.setBorder(border3);
    modeStatusBar.setMinimumSize(new Dimension(80, 4));
    modeStatusBar.setPreferredSize(new Dimension(80, 4));
    statusPanel.setBorder(border4);
    editorPane.setText(SampleText.txt02);
    toolBar.add(jButton1);
    toolBar.add(jButton2);
    toolBar.add(optionsButton);
    toolBar.add(helpButton);
    menuFile.add(menuFileExit);
    menuHelp.add(menuHelpAbout);
    menuBar1.add(menuFile);
    menuBar1.add(menuHelp);
    this.setJMenuBar(menuBar1);
    contentPane.add(toolBar, BorderLayout.NORTH);
    contentPane.add(jPanel1, BorderLayout.CENTER);
    jPanel1.add(jScrollPane1, BorderLayout.CENTER);
    jScrollPane1.getViewport().add(editorPane, null);
    jPanel1.add(statusPanel, BorderLayout.SOUTH);
    statusPanel.add(generalStatusBar, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 111, 0));
    statusPanel.add(strokeStatusBar, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));
    statusPanel.add(modeStatusBar, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));
  }

  //File | Exit action performed
  public void fileExit_actionPerformed(ActionEvent e) {
    System.exit(0);
  }

  //Help | About action performed
  public void helpAbout_actionPerformed(ActionEvent e) {
  }

  //Overridden so we can exit when window is closed
    @Override
protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      fileExit_actionPerformed(null);
    }
  }
}

class SampleText {

  static String txt01 = ""
               + "one\n"
               + "two\n"
               + "three\n"
               + "four\n"
               + "five\n"
               + "six\n"
               + "seven\n"
               + "eight\n"
               + "nine\n"
               + "ten\n"
               + "1111111111 11\n"
               + "  OOOOOOOOOO 12\n"
               + "        iiiiiiiiii 13\n"
               + "MMMMMMMMMM 14\n"
               + "jEditorPane1 15\n"
               + "second line 16\n"
               + "123456789 17";



  static String txt02 = ""
    + "package com.raelity.tools.vi;\n"
    + "\n"
    + "import java.util.Map;\n"
    + "import java.util.HashMap;\n"
    + "import java.util.Set;\n"
    + "import java.util.Collections;\n"
    + "\n"
    + "/**\n"
    + " * Option handling from external sources.\n"
    + " * <br>\n"
    + " * Should there be a vi command to set the options to persistent storage,\n"
    + " * this is useful if want to save after several set commands.\n"
    + " * <br>\n"
    + " *\n"
    + " */\n"
    + "\n"
    + "public class Options {\n"
    + "  public static final String backspaceWrapPreviousOption =\n"
    + "                                                \"viBackspaceWrapPrevious\";\n"
    + "  public static final String hWrapPreviousOption = \"viHWrapPrevious\";\n"
    + "  public static final String leftWrapPreviousOption = \"viLeftWrapPrevious\";\n"
    + "  public static final String spaceWrapNextOption = \"viSpaceWrapNext\";\n"
    + "  public static final String lWrapNextOption = \"viLWrapNext\";\n"
    + "  public static final String rightWrapNextOption = \"viRightWrapNext\";\n"
    + "  public static final String tildeWrapNextOption = \"viTildeWrapNext\";\n"
    + "\n"
    + "  public static final String unnamedClipboardOption = \"viUnnamedClipboard\";\n"
    + "  public static final String joinSpacesOption = \"viJoinSpaces\";\n"
    + "  public static final String shiftRoundOption = \"viShiftRound\";\n"
    + "  public static final String notStartOfLineOption = \"viNotStartOfLine\";\n"
    + "  public static final String changeWordBlanksOption = \"viChangeWordBlanks\";\n"
    + "  public static final String tildeOperator = \"viTildeOperator\";\n"
    + "  public static final String searchFromEnd = \"viSearchFromEnd\";\n"
    + "  public static final String wrapScan = \"viWrapScan\";\n"
    + "\n"
    + "  private static Map options = new HashMap();\n"
    + "  private static Set optionNames;\n"
    + "\n"
    + "  private static boolean didInit = false;\n"
    + "  public static void init() {\n"
    + "    if(didInit) {\n"
    + "      return;\n"
    + "    }\n"
    + "    G.p_ww_bs = setupBooleanOption(backspaceWrapPreviousOption, true);\n"
    + "    G.p_ww_h = setupBooleanOption(hWrapPreviousOption, false);\n"
    + "    G.p_ww_larrow = setupBooleanOption(leftWrapPreviousOption, false);\n"
    + "    G.p_ww_sp = setupBooleanOption(spaceWrapNextOption, true);\n"
    + "    G.p_ww_l = setupBooleanOption(lWrapNextOption, false);\n"
    + "    G.p_ww_rarrow = setupBooleanOption(rightWrapNextOption, false);\n"
    + "    G.p_ww_tilde = setupBooleanOption(tildeWrapNextOption, false);\n"
    + "\n"
    + "    G.p_cb = setupBooleanOption(unnamedClipboardOption, false);\n"
    + "    G.p_js = setupBooleanOption(joinSpacesOption, true);\n"
    + "    G.p_sr = setupBooleanOption(shiftRoundOption, false);\n"
    + "    G.p_notsol = setupBooleanOption(notStartOfLineOption, false);\n"
    + "    G.p_cpo_w = setupBooleanOption(changeWordBlanksOption, true);\n"
    + "    G.p_to = setupBooleanOption(tildeOperator, false);\n"
    + "    G.p_cpo_search = setupBooleanOption(searchFromEnd, true);\n"
    + "    G.p_ws = setupBooleanOption(wrapScan, true);\n"
    + "\n"
    + "    dbgInit();\n"
    + "    didInit = true;\n"
    + "  }\n"
    + "\n"
    + "  static {\n"
    + "    init();\n"
    + "  }\n"
    + "\n"
    + "  static private BooleanOption setupBooleanOption(String name,\n"
    + "                                                  boolean initValue)\n"
    + "  {\n"
    + "    BooleanOption opt = new BooleanOption(name, initValue);\n"
    + "    options.put(name, opt);\n"
    + "    return opt;\n"
    + "  }\n"
    + "\n"
    + "  public static void setOptionValue(Option option, Object value) {\n"
    + "    if(option instanceof BooleanOption) {\n"
    + "      setOptionValue((BooleanOption)option, ((Boolean)value).booleanValue());\n"
    + "    } else {\n"
    + "    }\n"
    + "  }\n"
    + "\n"
    + "  public static void setOptionValue(BooleanOption option, boolean value) {\n"
    + "    option.setBoolean(value);\n"
    + "  }\n"
    + "\n"
    + "  public static void XXXsetOptionValue(String name, boolean value) {\n"
    + "    BooleanOption bo = (BooleanOption)options.get(name);\n"
    + "    bo.setBoolean(value);\n"
    + "  }\n"
    + "\n"
    + "  public static Option getOption(String name) {\n"
    + "    return (Option)options.get(name);\n"
    + "  }\n"
    + "\n"
    + "  /** @return the String key names of the options. */\n"
    + "  public static Set getOptionNamesSet() {\n"
    + "    if(optionNames == null) {\n"
    + "      optionNames = Collections.unmodifiableSet(options.keySet());\n"
    + "    }\n"
    + "    return optionNames;\n"
    + "  }\n"
    + "\n"
    + "  public static final String dbgKeyStrokes = \"viDbgKeyStroks\";\n"
    + "  public static final String dbgCache = \"viDbgCache\";\n"
    + "  static void dbgInit() {\n"
    + "    setupBooleanOption(dbgKeyStrokes, false);\n"
    + "    setupBooleanOption(dbgCache, false);\n"
    + "  }\n"
    + "}\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\t\t\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\t\t\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\t\t\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\t\t\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\t\t\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\tc123\td123\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\tc123\td123\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\tc123\td123\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\t\t\t\t\t\t=\n"
    + "a123456|b123456|c123456|d123456|e123456|f123456|=\n"
    + "\n";

}
