/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;


import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.raelity.jvi.lib.MySegment;
import com.raelity.jvi.lib.StringSegment;

import static java.text.CharacterIterator.DONE;
import static org.junit.Assert.*;

/**
 *
 * @author err
 */
@RunWith(JUnitParamsRunner.class)
public class MiscTest
{

String testType;
String subseq;
private static final String  MY_SEG = "MySegment";
private static final String  STR_SEG = "StringSegment";
private static final String DEF = "0123456789";

// TODO: FIX PARAMETERIZED TESTS

// @Parameters(name = "{0}/{1}")
// public static Object[][] data() {
//     return new Object[][] {
//         {MY_SEG, "normal"}, {STR_SEG, "normal"},
//         {MY_SEG, "subseq"}, {STR_SEG, "subseq"},
//     };
// }

public MiscTest()
{
    testType = MY_SEG;
    //testType = STR_SEG;
    subseq = "normal";
}

@BeforeClass
public static void setUpClass()
{
}

@AfterClass
public static void tearDownClass()
{
}

@Before
public void setUp()
{
}

@After
public void tearDown()
{
}

String base;
int offset;
int count;
char doneChar;
MySegment getSegment() {
    return getSegment(DEF);
}

MySegment getSegment(String testChars)
{
    MySegment seg = null;
    switch(testType) {
    case MY_SEG:
        seg = new MySegment(testChars.toCharArray(), 0, testChars.length(), 3);
        doneChar = DONE;
        break;
    case STR_SEG:
        seg = new StringSegment(testChars);
        doneChar = 0;
        break;
    default:
        throw new RuntimeException();
    }

    assert seg != null;

    switch(subseq) {
    case "normal":
        base = testChars;
        offset = 0;
        count = testChars.length();
        break;
    // case "subseq":
    //     base = testChars.substring(1, 9);
    //     offset = 1;
    //     count = testChars.length() - 2;
    //     seg = seg.subSequence(1, 9);
    //     break;
    default:
        throw new RuntimeException();
    }

    return seg;
}

private Object[] stringNonWhite() {
    // the array is the position of non-white, last is the "terminator"
    return new Object[] {
        new Object[]{"a b"       , new int[]{0,2,3}},
        new Object[]{" a b "     , new int[]{1,3,5}},
        new Object[]{"  a   b   ", new int[]{2,6,10}},
        //            0123456789
    };
}

// position of non-white characters/end; offsets into stop
private static final int POS1 = 0;
private static final int POS2 = 1;
private static final int POS3 = 2;

/**
 * Test of skipwhite method, of class Misc.
 */
@Test
@Parameters(method = "stringNonWhite")
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testSkipwhite_CharacterIterator(String input, int[] stop)
{
    System.out.println("skipwhite_CharacterIterator");

    int expResult;
    int result;
    MySegment seg;

    seg = getSegment(input);
    Misc.skipwhite(seg);
    assertEquals('a', seg.current());
    expResult = stop[POS1] + offset;
    result = seg.getIndex();
    assertEquals(expResult, result);

    seg.next();
    Misc.skipwhite(seg);
    assertEquals('b', seg.current());
    expResult = stop[POS2] + offset;
    result = seg.getIndex();
    assertEquals(expResult, result);

    // shouldn't move
    Misc.skipwhite(seg);
    assertEquals('b', seg.current());
    expResult = stop[POS2] + offset;
    result = seg.getIndex();
    assertEquals(expResult, result);

    seg.next();

    Misc.skipwhite(seg);
    assertEquals(doneChar, seg.current());
    expResult = stop[POS3] + offset;
    result = seg.getIndex();
    assertEquals(expResult, result);
}

/**
 * Test of skipwhite method, of class Misc.
 */
@Test
@Parameters(method = "stringNonWhite")
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testSkipwhite_CharSequence_int(String input, int[] stop)
{
    System.out.println("skipwhite_CharSequence_int");

    int expResult;
    int result;
    int idx;
    MySegment seg;

    seg = getSegment(input);
    idx = 0;
    result = Misc.skipwhite(seg, idx);
    assertEquals('a', seg.charAt(result));
    expResult = stop[POS1];
    assertEquals(expResult, result);

    idx = result + 1;
    result = Misc.skipwhite(seg, idx);
    assertEquals('b', seg.charAt(result));
    expResult = stop[POS2];
    assertEquals(expResult, result);

    // shouldn't move
    result = Misc.skipwhite(seg, idx);
    assertEquals('b', seg.charAt(result));
    expResult = stop[POS2];
    assertEquals(expResult, result);

    idx = result + 1;

    result = Misc.skipwhite(seg, idx);
    //assertEquals(doneChar, seg.charAt(result));
    expResult = stop[POS3];
    assertEquals(expResult, result);
}

}

// /**
//  * Test of get_indent method, of class Misc.
//  */
// @Test
// public void testGet_indent_0args()
// {
//     System.out.println("get_indent");
//     int expResult = 0;
//     int result = Misc.get_indent();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of get_indent method, of class Misc.
//  */
// @Test
// public void testGet_indent_ViFPOS()
// {
//     System.out.println("get_indent");
//     ViFPOS fpos = null;
//     int expResult = 0;
//     int result = Misc.get_indent(fpos);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of get_indent_lnum method, of class Misc.
//  */
// @Test
// public void testGet_indent_lnum()
// {
//     System.out.println("get_indent_lnum");
//     int lnum = 0;
//     int expResult = 0;
//     int result = Misc.get_indent_lnum(lnum);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of findParen method, of class Misc.
//  */
// @Test
// public void testFindParen_3args_1()
// {
//     System.out.println("findParen");
//     int lnum = 0;
//     int fromIndent = 0;
//     int dir = 0;
//     int expResult = 0;
//     int result = Misc.findParen(lnum, fromIndent, dir);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of findParen method, of class Misc.
//  */
// @Test
// public void testFindParen_3args_2()
// {
//     System.out.println("findParen");
//     MySegment seg = null;
//     int fromIndent = 0;
//     int dir = 0;
//     int expResult = 0;
//     int result = Misc.findParen(seg, fromIndent, dir);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of findFirstNonBlank method, of class Misc.
//  */
// @Test
// public void testFindFirstNonBlank_3args_1()
// {
//     System.out.println("findFirstNonBlank");
//     int lnum = 0;
//     int fromIndent = 0;
//     int dir = 0;
//     int expResult = 0;
//     int result = Misc.findFirstNonBlank(lnum, fromIndent, dir);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of findFirstNonBlank method, of class Misc.
//  */
// @Test
// public void testFindFirstNonBlank_3args_2()
// {
//     System.out.println("findFirstNonBlank");
//     MySegment seg = null;
//     int fromIndent = 0;
//     int dir = 0;
//     int expResult = 0;
//     int result = Misc.findFirstNonBlank(seg, fromIndent, dir);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of set_indent method, of class Misc.
//  */
// @Test
// public void testSet_indent_int_boolean()
// {
//     System.out.println("set_indent");
//     int size = 0;
//     boolean del_first = false;
//     Misc.set_indent(size, del_first);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of set_indent method, of class Misc.
//  */
// @Test
// public void testSet_indent_3args()
// {
//     System.out.println("set_indent");
//     int size = 0;
//     boolean del_first = false;
//     ViFPOS fpos = null;
//     Misc.set_indent(size, del_first, fpos);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of open_line method, of class Misc.
//  */
// @Test
// public void testOpen_line()
// {
//     System.out.println("open_line");
//     int dir = 0;
//     boolean redraw = false;
//     boolean del_spaces = false;
//     int old_indent = 0;
//     boolean expResult = false;
//     boolean result = Misc.open_line(dir, redraw, del_spaces, old_indent);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of del_lines method, of class Misc.
//  */
// @Test
// public void testDel_lines()
// {
//     System.out.println("del_lines");
//     int nlines = 0;
//     boolean dowindow = false;
//     boolean undo = false;
//     Misc.del_lines(nlines, dowindow, undo);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of gchar_pos method, of class Misc.
//  */
// @Test
// public void testGchar_pos()
// {
//     System.out.println("gchar_pos");
//     ViFPOS pos = null;
//     char expResult = ' ';
//     char result = Misc.gchar_pos(pos);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of gchar_cursor method, of class Misc.
//  */
// @Test
// public void testGchar_cursor()
// {
//     System.out.println("gchar_cursor");
//     char expResult = ' ';
//     char result = Misc.gchar_cursor();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of pchar_cursor method, of class Misc.
//  */
// @Test
// public void testPchar_cursor()
// {
//     System.out.println("pchar_cursor");
//     char c = ' ';
//     Misc.pchar_cursor(c);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of pchar method, of class Misc.
//  */
// @Test
// public void testPchar()
// {
//     System.out.println("pchar");
//     ViFPOS fpos = null;
//     char c = ' ';
//     Misc.pchar(fpos, c);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of change_warning method, of class Misc.
//  */
// @Test
// public void testChange_warning()
// {
//     System.out.println("change_warning");
//     int col = 0;
//     Misc.change_warning(col);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of inindent method, of class Misc.
//  */
// @Test
// public void testInindent()
// {
//     System.out.println("inindent");
//     int extra = 0;
//     boolean expResult = false;
//     boolean result = Misc.inindent(extra);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of plural method, of class Misc.
//  */
// @Test
// public void testPlural()
// {
//     System.out.println("plural");
//     int n = 0;
//     String expResult = "";
//     String result = Misc.plural(n);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of ins_char method, of class Misc.
//  */
// @Test
// public void testIns_char_char()
// {
//     System.out.println("ins_char");
//     char c = ' ';
//     Misc.ins_char(c);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of ins_char method, of class Misc.
//  */
// @Test
// public void testIns_char_char_boolean()
// {
//     System.out.println("ins_char");
//     char c = ' ';
//     boolean ctrlv = false;
//     Misc.ins_char(c, ctrlv);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of del_char method, of class Misc.
//  */
// @Test
// public void testDel_char()
// {
//     System.out.println("del_char");
//     boolean fixpos = false;
//     int expResult = 0;
//     int result = Misc.del_char(fixpos);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of del_chars method, of class Misc.
//  */
// @Test
// public void testDel_chars()
// {
//     System.out.println("del_chars");
//     int count = 0;
//     boolean fixpos = false;
//     int expResult = 0;
//     int result = Misc.del_chars(count, fixpos);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of msgmore method, of class Misc.
//  */
// @Test
// public void testMsgmore()
// {
//     System.out.println("msgmore");
//     int n = 0;
//     Misc.msgmore(n);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of line_breakcheck method, of class Misc.
//  */
// @Test
// public void testLine_breakcheck()
// {
//     System.out.println("line_breakcheck");
//     Misc.line_breakcheck();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of coladvanceColumnIndex method, of class Misc.
//  */
// @Test
// public void testColadvanceColumnIndex_3args()
// {
//     System.out.println("coladvanceColumnIndex");
//     int wcol = 0;
//     MySegment txt = null;
//     MutableBoolean reached = null;
//     int expResult = 0;
//     int result = Misc.coladvanceColumnIndex(wcol, txt, reached);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of coladvanceColumnIndex method, of class Misc.
//  */
// @Test
// public void testColadvanceColumnIndex_int_MySegment()
// {
//     System.out.println("coladvanceColumnIndex");
//     int wcol = 0;
//     MySegment txt = null;
//     int expResult = 0;
//     int result = Misc.coladvanceColumnIndex(wcol, txt);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of coladvanceColumnIndex method, of class Misc.
//  */
// @Test
// public void testColadvanceColumnIndex_MySegment()
// {
//     System.out.println("coladvanceColumnIndex");
//     MySegment txt = null;
//     int expResult = 0;
//     int result = Misc.coladvanceColumnIndex(txt);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of coladvance method, of class Misc.
//  */
// @Test
// public void testColadvance_int()
// {
//     System.out.println("coladvance");
//     int wcol = 0;
//     ViFPOS expResult = null;
//     ViFPOS result = Misc.coladvance(wcol);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of coladvance method, of class Misc.
//  */
// @Test
// public void testColadvance_ViFPOS_int()
// {
//     System.out.println("coladvance");
//     ViFPOS fpos = null;
//     int wcol = 0;
//     ViFPOS expResult = null;
//     ViFPOS result = Misc.coladvance(fpos, wcol);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of coladvance method, of class Misc.
//  */
// @Test
// public void testColadvance_3args()
// {
//     System.out.println("coladvance");
//     ViFPOS fpos = null;
//     int wcol = 0;
//     MutableBoolean hitTarget = null;
//     ViFPOS expResult = null;
//     ViFPOS result = Misc.coladvance(fpos, wcol, hitTarget);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of inc_cursor method, of class Misc.
//  */
// @Test
// public void testInc_cursor()
// {
//     System.out.println("inc_cursor");
//     int expResult = 0;
//     int result = Misc.inc_cursor();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of inc method, of class Misc.
//  */
// @Test
// public void testInc()
// {
//     System.out.println("inc");
//     ViFPOS lp = null;
//     int expResult = 0;
//     int result = Misc.inc(lp);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of incV7 method, of class Misc.
//  */
// @Test
// public void testIncV7()
// {
//     System.out.println("incV7");
//     ViFPOS lp = null;
//     int expResult = 0;
//     int result = Misc.incV7(lp);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of inc_cursorV7 method, of class Misc.
//  */
// @Test
// public void testInc_cursorV7()
// {
//     System.out.println("inc_cursorV7");
//     int expResult = 0;
//     int result = Misc.inc_cursorV7();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of inclV7 method, of class Misc.
//  */
// @Test
// public void testInclV7()
// {
//     System.out.println("inclV7");
//     ViFPOS fpos = null;
//     int expResult = 0;
//     int result = Misc.inclV7(fpos);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of dec_cursor method, of class Misc.
//  */
// @Test
// public void testDec_cursor()
// {
//     System.out.println("dec_cursor");
//     int expResult = 0;
//     int result = Misc.dec_cursor();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of dec method, of class Misc.
//  */
// @Test
// public void testDec()
// {
//     System.out.println("dec");
//     ViFPOS lp = null;
//     int expResult = 0;
//     int result = Misc.dec(lp);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of decl method, of class Misc.
//  */
// @Test
// public void testDecl()
// {
//     System.out.println("decl");
//     ViFPOS lp = null;
//     int expResult = 0;
//     int result = Misc.decl(lp);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of inclDeclV7 method, of class Misc.
//  */
// @Test
// public void testInclDeclV7()
// {
//     System.out.println("inclDeclV7");
//     ViFPOS lp = null;
//     int dir = 0;
//     int expResult = 0;
//     int result = Misc.inclDeclV7(lp, dir);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of check_cursor_lnumBeginline method, of class Misc.
//  */
// @Test
// public void testCheck_cursor_lnumBeginline()
// {
//     System.out.println("check_cursor_lnumBeginline");
//     int flags = 0;
//     Misc.check_cursor_lnumBeginline(flags);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of check_cursor_lnum method, of class Misc.
//  */
// @Test
// public void testCheck_cursor_lnum()
// {
//     System.out.println("check_cursor_lnum");
//     int lnum = 0;
//     int expResult = 0;
//     int result = Misc.check_cursor_lnum(lnum);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of check_cursor_col method, of class Misc.
//  */
// @Test
// public void testCheck_cursor_col_0args()
// {
//     System.out.println("check_cursor_col");
//     Misc.check_cursor_col();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of check_cursor_col method, of class Misc.
//  */
// @Test
// public void testCheck_cursor_col_int_int()
// {
//     System.out.println("check_cursor_col");
//     int lnum = 0;
//     int col = 0;
//     int expResult = 0;
//     int result = Misc.check_cursor_col(lnum, col);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of adjust_cursor method, of class Misc.
//  */
// @Test
// public void testAdjust_cursor()
// {
//     System.out.println("adjust_cursor");
//     Misc.adjust_cursor();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of op_shift method, of class Misc.
//  */
// @Test
// public void testOp_shift()
// {
//     System.out.println("op_shift");
//     OPARG oap = null;
//     boolean curs_top = false;
//     int amount = 0;
//     Misc.op_shift(oap, curs_top, amount);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of shift_line method, of class Misc.
//  */
// @Test
// public void testShift_line_3args()
// {
//     System.out.println("shift_line");
//     boolean left = false;
//     boolean round = false;
//     int amount = 0;
//     Misc.shift_line(left, round, amount);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of shift_line method, of class Misc.
//  */
// @Test
// public void testShift_line_4args()
// {
//     System.out.println("shift_line");
//     boolean left = false;
//     boolean round = false;
//     int amount = 0;
//     ViFPOS fpos = null;
//     Misc.shift_line(left, round, amount, fpos);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of valid_yank_reg method, of class Misc.
//  */
// @Test
// public void testValid_yank_reg()
// {
//     System.out.println("valid_yank_reg");
//     char regname = ' ';
//     boolean writing = false;
//     boolean expResult = false;
//     boolean result = Misc.valid_yank_reg(regname, writing);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of get_yank_register method, of class Misc.
//  */
// @Test
// public void testGet_yank_register()
// {
//     System.out.println("get_yank_register");
//     char regname = ' ';
//     boolean writing = false;
//     Misc.get_yank_register(regname, writing);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of get_register method, of class Misc.
//  */
// @Test
// public void testGet_register()
// {
//     System.out.println("get_register");
//     char name = ' ';
//     boolean copy = false;
//     Yankreg expResult = null;
//     Yankreg result = Misc.get_register(name, copy);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of put_register method, of class Misc.
//  */
// @Test
// public void testPut_register()
// {
//     System.out.println("put_register");
//     char name = ' ';
//     Yankreg reg = null;
//     Misc.put_register(name, reg);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of do_record method, of class Misc.
//  */
// @Test
// public void testDo_record()
// {
//     System.out.println("do_record");
//     char c = ' ';
//     int expResult = 0;
//     int result = Misc.do_record(c);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of do_execreg method, of class Misc.
//  */
// @Test
// public void testDo_execreg()
// {
//     System.out.println("do_execreg");
//     char regname = ' ';
//     boolean colon = false;
//     boolean addcr = false;
//     int expResult = 0;
//     int result = Misc.do_execreg(regname, colon, addcr);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of free_yank_all method, of class Misc.
//  */
// @Test
// public void testFree_yank_all()
// {
//     System.out.println("free_yank_all");
//     Misc.free_yank_all();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of shiftYank method, of class Misc.
//  */
// @Test
// public void testShiftYank()
// {
//     System.out.println("shiftYank");
//     Misc.shiftYank();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of adjust_clip_reg method, of class Misc.
//  */
// @Test
// public void testAdjust_clip_reg()
// {
//     System.out.println("adjust_clip_reg");
//     char rp = ' ';
//     char expResult = ' ';
//     char result = Misc.adjust_clip_reg(rp);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of insert_reg method, of class Misc.
//  */
// @Test
// public void testInsert_reg() throws Exception
// {
//     System.out.println("insert_reg");
//     char regname = ' ';
//     boolean literally = false;
//     int expResult = 0;
//     int result = Misc.insert_reg(regname, literally);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of check_fname method, of class Misc.
//  */
// @Test
// public void testCheck_fname()
// {
//     System.out.println("check_fname");
//     int expResult = 0;
//     int result = Misc.check_fname();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of get_spec_reg method, of class Misc.
//  */
// @Test
// public void testGet_spec_reg()
// {
//     System.out.println("get_spec_reg");
//     char regname = ' ';
//     Wrap<String> argp = null;
//     boolean errmsg = false;
//     boolean expResult = false;
//     boolean result = Misc.get_spec_reg(regname, argp, errmsg);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of op_delete method, of class Misc.
//  */
// @Test
// public void testOp_delete()
// {
//     System.out.println("op_delete");
//     OPARG oap = null;
//     boolean expResult = false;
//     boolean result = Misc.op_delete(oap);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of valid_op_range method, of class Misc.
//  */
// @Test
// public void testValid_op_range()
// {
//     System.out.println("valid_op_range");
//     OPARG oap = null;
//     boolean expResult = false;
//     boolean result = Misc.valid_op_range(oap);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of op_replace method, of class Misc.
//  */
// @Test
// public void testOp_replace()
// {
//     System.out.println("op_replace");
//     OPARG oap = null;
//     char c = ' ';
//     int expResult = 0;
//     int result = Misc.op_replace(oap, c);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of op_replace7 method, of class Misc.
//  */
// @Test
// public void testOp_replace7()
// {
//     System.out.println("op_replace7");
//     OPARG oap = null;
//     char c = ' ';
//     int expResult = 0;
//     int result = Misc.op_replace7(oap, c);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of op_tilde method, of class Misc.
//  */
// @Test
// public void testOp_tilde()
// {
//     System.out.println("op_tilde");
//     OPARG oap = null;
//     Misc.op_tilde(oap);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of swapchar method, of class Misc.
//  */
// @Test
// public void testSwapchar_int_ViFPOS()
// {
//     System.out.println("swapchar");
//     int op_type = 0;
//     ViFPOS fpos = null;
//     Misc.swapchar(op_type, fpos);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of swapchar method, of class Misc.
//  */
// @Test
// public void testSwapchar_int_char()
// {
//     System.out.println("swapchar");
//     int op_type = 0;
//     char c = ' ';
//     char expResult = ' ';
//     char result = Misc.swapchar(op_type, c);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of finishOpSplit method, of class Misc.
//  */
// @Test
// public void testFinishOpSplit()
// {
//     System.out.println("finishOpSplit");
//     Misc.finishOpSplit();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of op_change method, of class Misc.
//  */
// @Test
// public void testOp_change()
// {
//     System.out.println("op_change");
//     OPARG oap = null;
//     Misc.op_change(oap);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of finishOpChange method, of class Misc.
//  */
// @Test
// public void testFinishOpChange()
// {
//     System.out.println("finishOpChange");
//     Misc.finishOpChange();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of op_yank method, of class Misc.
//  */
// @Test
// public void testOp_yank()
// {
//     System.out.println("op_yank");
//     OPARG oap = null;
//     boolean deleting = false;
//     boolean mess = false;
//     int expResult = 0;
//     int result = Misc.op_yank(oap, deleting, mess);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of do_put method, of class Misc.
//  */
// @Test
// public void testDo_put()
// {
//     System.out.println("do_put");
//     int regname_ = 0;
//     int dir = 0;
//     int count = 0;
//     int flags = 0;
//     Misc.do_put(regname_, dir, count, flags);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of do_do_join method, of class Misc.
//  */
// @Test
// public void testDo_do_join()
// {
//     System.out.println("do_do_join");
//     int count = 0;
//     boolean insert_space = false;
//     boolean redraw = false;
//     Misc.do_do_join(count, insert_space, redraw);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of do_join method, of class Misc.
//  */
// @Test
// public void testDo_join()
// {
//     System.out.println("do_join");
//     boolean insert_space = false;
//     boolean redraw = false;
//     int expResult = 0;
//     int result = Misc.do_join(insert_space, redraw);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of ui_cursor_shape method, of class Misc.
//  */
// @Test
// public void testUi_cursor_shape()
// {
//     System.out.println("ui_cursor_shape");
//     Misc.ui_cursor_shape();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of clip_gen_set_selection method, of class Misc.
//  */
// @Test
// public void testClip_gen_set_selection()
// {
//     System.out.println("clip_gen_set_selection");
//     Misc.clip_gen_set_selection();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of clip_get_selection method, of class Misc.
//  */
// @Test
// public void testClip_get_selection()
// {
//     System.out.println("clip_get_selection");
//     Misc.clip_get_selection();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of lostOwnership method, of class Misc.
//  */
// @Test
// public void testLostOwnership()
// {
//     System.out.println("lostOwnership");
//     Clipboard clipboard = null;
//     Transferable contents = null;
//     Misc instance = null;
//     instance.lostOwnership(clipboard, contents);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of getvcol method, of class Misc.
//  */
// @Test
// public void testGetvcol_int()
// {
//     System.out.println("getvcol");
//     int endCol = 0;
//     int expResult = 0;
//     int result = Misc.getvcol(endCol);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of getvcol method, of class Misc.
//  */
// @Test
// public void testGetvcol_5args_1()
// {
//     System.out.println("getvcol");
//     ViTextView tv = null;
//     ViFPOS fpos = null;
//     MutableInt start = null;
//     MutableInt cursor = null;
//     MutableInt end = null;
//     Misc.getvcol(tv, fpos, start, cursor, end);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of getvcol method, of class Misc.
//  */
// @Test
// public void testGetvcol_5args_2()
// {
//     System.out.println("getvcol");
//     Buffer buf = null;
//     ViFPOS fpos = null;
//     MutableInt start = null;
//     MutableInt cursor = null;
//     MutableInt end = null;
//     Misc.getvcol(buf, fpos, start, cursor, end);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of getvcols method, of class Misc.
//  */
// @Test
// public void testGetvcols()
// {
//     System.out.println("getvcols");
//     ViFPOS pos1 = null;
//     ViFPOS pos2 = null;
//     MutableInt left = null;
//     MutableInt right = null;
//     Misc.getvcols(pos1, pos2, left, right);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of showmode method, of class Misc.
//  */
// @Test
// public void testShowmode()
// {
//     System.out.println("showmode");
//     int expResult = 0;
//     int result = Misc.showmode();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of update_screen method, of class Misc.
//  */
// @Test
// public void testUpdate_screen()
// {
//     System.out.println("update_screen");
//     int type = 0;
//     Misc.update_screen(type);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of check_for_delay method, of class Misc.
//  */
// @Test
// public void testCheck_for_delay()
// {
//     System.out.println("check_for_delay");
//     boolean check_msg_scroll = false;
//     Misc.check_for_delay(check_msg_scroll);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of u_undo method, of class Misc.
//  */
// @Test
// public void testU_undo()
// {
//     System.out.println("u_undo");
//     int count = 0;
//     Misc.u_undo(count);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of u_redo method, of class Misc.
//  */
// @Test
// public void testU_redo()
// {
//     System.out.println("u_redo");
//     int count = 0;
//     Misc.u_redo(count);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of vim_iswordc method, of class Misc.
//  */
// @Test
// public void testVim_iswordc()
// {
//     System.out.println("vim_iswordc");
//     char c = ' ';
//     boolean expResult = false;
//     boolean result = Misc.vim_iswordc(c);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of transchar method, of class Misc.
//  */
// @Test
// public void testTranschar()
// {
//     System.out.println("transchar");
//     char c = ' ';
//     String expResult = "";
//     String result = Misc.transchar(c);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of transchar_nonprint method, of class Misc.
//  */
// @Test
// public void testTranschar_nonprint()
// {
//     System.out.println("transchar_nonprint");
//     StringBuilder buf = null;
//     char c = ' ';
//     Misc.transchar_nonprint(buf, c);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of lbr_chartabsize method, of class Misc.
//  */
// @Test
// public void testLbr_chartabsize()
// {
//     System.out.println("lbr_chartabsize");
//     char c = ' ';
//     int col = 0;
//     int expResult = 0;
//     int result = Misc.lbr_chartabsize(c, col);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of linetabsize method, of class Misc.
//  */
// @Test
// public void testLinetabsize()
// {
//     System.out.println("linetabsize");
//     MySegment seg = null;
//     int expResult = 0;
//     int result = Misc.linetabsize(seg);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of vim_iswhite method, of class Misc.
//  */
// @Test
// public void testVim_iswhite()
// {
//     System.out.println("vim_iswhite");
//     char c = ' ';
//     boolean expResult = false;
//     boolean result = Misc.vim_iswhite(c);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of skiptowhite method, of class Misc.
//  */
// @Test
// public void testSkiptowhite()
// {
//     System.out.println("skiptowhite");
//     String str = "";
//     int idx = 0;
//     int expResult = 0;
//     int result = Misc.skiptowhite(str, idx);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of getdigits method, of class Misc.
//  */
// @Test
// public void testGetdigits()
// {
//     System.out.println("getdigits");
//     String s = "";
//     int sidx = 0;
//     MutableInt mi = null;
//     int expResult = 0;
//     int result = Misc.getdigits(s, sidx, mi);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of cmdline_at_end method, of class Misc.
//  */
// @Test
// public void testCmdline_at_end()
// {
//     System.out.println("cmdline_at_end");
//     boolean expResult = false;
//     boolean result = Misc.cmdline_at_end();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of cmdline_overstrike method, of class Misc.
//  */
// @Test
// public void testCmdline_overstrike()
// {
//     System.out.println("cmdline_overstrike");
//     boolean expResult = false;
//     boolean result = Misc.cmdline_overstrike();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of setCommandCharacters method, of class Misc.
//  */
// @Test
// public void testSetCommandCharacters()
// {
//     System.out.println("setCommandCharacters");
//     String s = "";
//     Misc.setCommandCharacters(s);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of out_flush method, of class Misc.
//  */
// @Test
// public void testOut_flush()
// {
//     System.out.println("out_flush");
//     Misc.out_flush();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of isInUndo method, of class Misc.
//  */
// @Test
// public void testIsInUndo()
// {
//     System.out.println("isInUndo");
//     boolean expResult = false;
//     boolean result = Misc.isInUndo();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of isInInsertUndo method, of class Misc.
//  */
// @Test
// public void testIsInInsertUndo()
// {
//     System.out.println("isInInsertUndo");
//     boolean expResult = false;
//     boolean result = Misc.isInInsertUndo();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of isInAnyUndo method, of class Misc.
//  */
// @Test
// public void testIsInAnyUndo()
// {
//     System.out.println("isInAnyUndo");
//     boolean expResult = false;
//     boolean result = Misc.isInAnyUndo();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of runUndoable method, of class Misc.
//  */
// @Test
// public void testRunUndoable()
// {
//     System.out.println("runUndoable");
//     Runnable r = null;
//     Misc.runUndoable(r);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of beginInsertUndo method, of class Misc.
//  */
// @Test
// public void testBeginInsertUndo()
// {
//     System.out.println("beginInsertUndo");
//     Misc.beginInsertUndo();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of endInsertUndo method, of class Misc.
//  */
// @Test
// public void testEndInsertUndo()
// {
//     System.out.println("endInsertUndo");
//     Misc.endInsertUndo();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of getKeyStroke method, of class Misc.
//  */
// @Test
// public void testGetKeyStroke()
// {
//     System.out.println("getKeyStroke");
//     int vikey = 0;
//     int modifiers = 0;
//     KeyStroke expResult = null;
//     KeyStroke result = Misc.getKeyStroke(vikey, modifiers);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of xlateViKey method, of class Misc.
//  */
// @Test
// public void testXlateViKey()
// {
//     System.out.println("xlateViKey");
//     Keymap map = null;
//     char vikey = ' ';
//     int modifiers = 0;
//     char expResult = ' ';
//     char result = Misc.xlateViKey(map, vikey, modifiers);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of block_prep method, of class Misc.
//  */
// @Test
// public void testBlock_prep()
// {
//     System.out.println("block_prep");
//     OPARG oap = null;
//     block_def bdp = null;
//     int lnum = 0;
//     boolean is_del = false;
//     Misc.block_prep(oap, bdp, lnum, is_del);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of do_addsub method, of class Misc.
//  */
// @Test
// public void testDo_addsub()
// {
//     System.out.println("do_addsub");
//     char command = ' ';
//     int Prenum1 = 0;
//     int expResult = 0;
//     int result = Misc.do_addsub(command, Prenum1);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of op_insert method, of class Misc.
//  */
// @Test
// public void testOp_insert()
// {
//     System.out.println("op_insert");
//     OPARG oap = null;
//     int count1 = 0;
//     Misc.op_insert(oap, count1);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of finishOpInsert method, of class Misc.
//  */
// @Test
// public void testFinishOpInsert()
// {
//     System.out.println("finishOpInsert");
//     Misc.finishOpInsert();
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of append_spaces method, of class Misc.
//  */
// @Test
// public void testAppend_spaces()
// {
//     System.out.println("append_spaces");
//     StringBuilder sb = null;
//     int len = 0;
//     Misc.append_spaces(sb, len);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of append_chars method, of class Misc.
//  */
// @Test
// public void testAppend_chars()
// {
//     System.out.println("append_chars");
//     StringBuilder dst = null;
//     int len = 0;
//     char c = ' ';
//     Misc.append_chars(dst, len, c);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of mch_memmove method, of class Misc.
//  */
// @Test
// public void testMch_memmove()
// {
//     System.out.println("mch_memmove");
//     StringBuilder dst = null;
//     int dstIndex = 0;
//     CharSequence src = null;
//     int srcIndex = 0;
//     int len = 0;
//     Misc.mch_memmove(dst, dstIndex, src, srcIndex, len);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of copy_spaces method, of class Misc.
//  */
// @Test
// public void testCopy_spaces()
// {
//     System.out.println("copy_spaces");
//     StringBuilder dst = null;
//     int index = 0;
//     int len = 0;
//     Misc.copy_spaces(dst, index, len);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of STRLEN method, of class Misc.
//  */
// @Test
// public void testSTRLEN()
// {
//     System.out.println("STRLEN");
//     StringBuilder sb = null;
//     int expResult = 0;
//     int result = Misc.STRLEN(sb);
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of copy_chars method, of class Misc.
//  */
// @Test
// public void testCopy_chars()
// {
//     System.out.println("copy_chars");
//     StringBuilder dst = null;
//     int index = 0;
//     int len = 0;
//     char c = ' ';
//     Misc.copy_chars(dst, index, len, c);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of block_insert method, of class Misc.
//  */
// @Test
// public void testBlock_insert()
// {
//     System.out.println("block_insert");
//     OPARG oap = null;
//     String s = "";
//     boolean b_insert = false;
//     block_def bdp = null;
//     Misc.block_insert(oap, s, b_insert, bdp);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }