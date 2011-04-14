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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.cmd;

/**
 * Provides some sample text for the demo frame.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class SampleText {
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
+ "0123\n"
+ "abcd\n"
+ "wxyz\n"
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


}
