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

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class JviFrame extends JFrame
{
    /**
     *  If true, exit the application when the frame closes.
     *  For embedded applications this should be set false.
     */
    public static boolean SystemExitOnCloseFrame = true;

    protected JEditorPane editorPane;
    protected JScrollPane scrollPane;
    protected JButton optionsButton;
    protected JLabel generalStatusBar, strokeStatusBar, modeStatusBar;
    protected JLabel cursorStatusBar;
    protected PlayStatusDisplay statusDisplay;

    private Color m_color1 = new java.awt.Color(142,142,142),
                  m_color2 = new java.awt.Color(99,99,99);


    /**
     * Construct the frame.
     */
    public JviFrame()
    {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            jbInit();
            statusDisplay = new PlayStatusDisplay(generalStatusBar, strokeStatusBar,
                                              modeStatusBar);
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + " thrown by JviFrame():  " + e.getMessage() );
            e.printStackTrace();
        }
    }


    /**
     *  Return the JEditorPane used by this JviFrame.
     */
    public JEditorPane getEditor()
    {
        return editorPane;
    }


    /**
     *  Return the JScrollPane used by this JviFrame's editor.
     */
    public JScrollPane getScrollPane()
    {
        return scrollPane;
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
    private void jbInit() throws Exception
    {
        ImageIcon image1 = new ImageIcon(JviFrame.class.getResource("openFile.gif"));
        ImageIcon image2 = new ImageIcon(JviFrame.class.getResource("closeFile.gif"));
        ImageIcon image3 = new ImageIcon(JviFrame.class.getResource("help.gif"));
        JPanel contentPane = (JPanel)this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        this.setSize(new Dimension(400, 285));
        this.setTitle("Frame Title");
        JMenu menuFile = new JMenu();
        menuFile.setText("File");
        JMenuItem menuFileExit = new JMenuItem();
        menuFileExit.setText("Exit");
        menuFileExit.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fileExit_actionPerformed(e);
                }
            });
        JMenu menuHelp = new JMenu();
        menuHelp.setText("Help");
        JMenuItem menuHelpAbout = new JMenuItem();
        menuHelpAbout.setText("About");
        menuHelpAbout.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    helpAbout_actionPerformed(e);
                }
            });
        JButton jButton1 = new JButton(image1);
        jButton1.setToolTipText("Open File");

        JButton jButton2 = new JButton(image2);
        jButton2.setToolTipText("Close File");
        JButton helpButton = new JButton(image3);
        helpButton.setToolTipText("Help");
        optionsButton = new JButton("Options");
        optionsButton.setToolTipText("Options");
        JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
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
        cursorStatusBar.setMinimumSize(new Dimension(80,4));
        cursorStatusBar.setPreferredSize(new Dimension(80,4));

        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,m_color1,m_color2),
                BorderFactory.createEmptyBorder(2,0,2,0)));
        editorPane = new JTextPane();
        editorPane.setText(SampleText.txt02);
        JToolBar toolBar = new JToolBar();
        toolBar.add(jButton1);
        toolBar.add(jButton2);
        toolBar.add(optionsButton);
        toolBar.add(helpButton);
        menuFile.add(menuFileExit);
        menuHelp.add(menuHelpAbout);
        JMenuBar menuBar1 = new JMenuBar();
        menuBar1.add(menuFile);
        menuBar1.add(menuHelp);
        this.setJMenuBar(menuBar1);
        contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(jPanel1, BorderLayout.CENTER);
        scrollPane = new JScrollPane();
        jPanel1.add(scrollPane, BorderLayout.CENTER);
        scrollPane.getViewport().add(editorPane, null);
        jPanel1.add(statusPanel, BorderLayout.SOUTH);
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


/**
 * Provides some sample text for the demo frame.
 */
class SampleText
{
    static String txt01 =
              ""
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


    static String txt02 =
              ""
            + "package com.raelity.tools.vi;\n"
            + "\n"
            + "import java.util.Map;\n"
            + "import java.util.HashMap;\n"
            + "import java.util.Set;\n"
            + "import java.util.Collections;\n"
            + "\n"
            + "999\n"
            + "XXX999XXX\n"
            + "0x799\n"
            + "0X799\n"
            + "0x7ff\n"
            + "0xfff\n"
            + "XXX0xfffXXX\n"
            + "0777\n"
            + "\n"
            + "dwlllllldwllllllldwllllllldwj0\n"
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
            + "/*01*/  public static final String backspaceWrapPreviousOption =\n"
            + "/*02*/                                                \"viBackspaceWrapPrevious\";\n"
            + "/*03*/  public static final String hWrapPreviousOption = \"viHWrapPrevious\";\n"
            + "/*04*/  public static final String leftWrapPreviousOption = \"viLeftWrapPrevious\";\n"
            + "/*05*/  public static final String spaceWrapNextOption = \"viSpaceWrapNext\";\n"
            + "/*06*/  public static final String lWrapNextOption = \"viLWrapNext\";\n"
            + "/*07*/  public static final String rightWrapNextOption = \"viRightWrapNext\";\n"
            + "/*08*/  public static final String tildeWrapNextOption = \"viTildeWrapNext\";\n"
            + "/*09*/\n"
            + "/*10*/  public static final String unnamedClipboardOption = \"viUnnamedClipboard\";\n"
            + "/*11*/  public static final String joinSpacesOption = \"viJoinSpaces\";\n"
            + "/*12*/  public static final String shiftRoundOption = \"viShiftRound\";\n"
            + "/*13*/  public static final String notStartOfLineOption = \"viNotStartOfLine\";\n"
            + "/*14*/  public static final String changeWordBlanksOption = \"viChangeWordBlanks\";\n"
            + "/*15*/  public static final String tildeOperator = \"viTildeOperator\";\n"
            + "/*16*/  public static final String searchFromEnd = \"viSearchFromEnd\";\n"
            + "/*17*/  public static final String wrapScan = \"viWrapScan\";\n"
            + "/*18*/\n"
            + "/*19*/  private static Map options = new HashMap();\n"
            + "/*20*/  private static Set optionNames;\n"
            + "/*21*/\n"
            + "/*22*/  private static boolean didInit = false;\n"
            + "/*23*/  public static void init() {\n"
            + "/*24*/    if(didInit) {\n"
            + "/*25*/      return;\n"
            + "/*26*/    }\n"
            + "/*27*/    G.p_ww_bs = setupBooleanOption(backspaceWrapPreviousOption, true);\n"
            + "/*28*/    G.p_ww_h = setupBooleanOption(hWrapPreviousOption, false);\n"
            + "/*29*/    G.p_ww_larrow = setupBooleanOption(leftWrapPreviousOption, false);\n"
            + "/*30*/    G.p_ww_sp = setupBooleanOption(spaceWrapNextOption, true);\n"
            + "/*31*/    G.p_ww_l = setupBooleanOption(lWrapNextOption, false);\n"
            + "/*32*/    G.p_ww_rarrow = setupBooleanOption(rightWrapNextOption, false);\n"
            + "/*33*/    G.p_ww_tilde = setupBooleanOption(tildeWrapNextOption, false);\n"
            + "/*34*/\n"
            + "/*35*/    G.p_cb = setupBooleanOption(unnamedClipboardOption, false);\n"
            + "/*36*/    G.p_js = setupBooleanOption(joinSpacesOption, true);\n"
            + "/*37*/    G.p_sr = setupBooleanOption(shiftRoundOption, false);\n"
            + "/*38*/    G.p_notsol = setupBooleanOption(notStartOfLineOption, false);\n"
            + "/*39*/    G.p_cpo_w = setupBooleanOption(changeWordBlanksOption, true);\n"
            + "/*40*/    G.p_to = setupBooleanOption(tildeOperator, false);\n"
            + "/*41*/    G.p_cpo_search = setupBooleanOption(searchFromEnd, true);\n"
            + "/*42*/    G.p_ws = setupBooleanOption(wrapScan, true);\n"
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

    private SampleText()
    {
    }

} // end class SampleText
