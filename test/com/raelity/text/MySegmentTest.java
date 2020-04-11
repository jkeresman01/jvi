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

package com.raelity.text;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static java.text.CharacterIterator.DONE;
import static org.junit.Assert.*;

/**
 *
 * @author err
 */
@RunWith(Parameterized.class)
public class MySegmentTest
{

String testType;
String subseq;
private static final String  MY_SEG = "MySegment";
private static final String  STR_SEG = "StringSegment";
private static final String DEF = "0123456789";

@Parameters(name = "{0}/{1}")
public static Object[][] data() {
    return new Object[][] {
        {MY_SEG, "normal"}, {STR_SEG, "normal"},
        {MY_SEG, "subseq"}, {STR_SEG, "subseq"},
    };
}
public MySegmentTest(String testType, String subseq)
{
    this.testType = testType;
    this.subseq = subseq;
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
MySegment getSegment()
{
    MySegment seg = null;
    switch(testType) {
    case MY_SEG:
        seg = new MySegment(DEF.toCharArray(), 0, DEF.length(), 3);
        doneChar = DONE;
        break;
    case STR_SEG:
        seg = new StringSegment(DEF);
        doneChar = 0;
        break;
    }

    assert seg != null;

    switch(subseq) {
    case "normal":
        base = DEF;
        offset = 0;
        count = DEF.length();
        break;
    case "subseq":
        base = DEF.substring(1, 9);
        offset = 1;
        count = DEF.length() - 2;
        seg = seg.subSequence(1, 9);
        break;
    default:
        throw new RuntimeException();
    }

    return seg;
}

/**
 * Test of toString method, of class MySegment.
 */
@Test
public void testToString()
{
    System.out.println("toString");
    MySegment instance = getSegment();
    String expResult = base;
    String result = instance.toString();
    assertEquals(expResult, result);
}

/**
 * Test of substring method, of class MySegment.
 */
@Test
public void testSubstring()
{
    System.out.println("substring");
    int start = 2;
    int end = 4;
    MySegment instance = getSegment();
    String expResult = base.substring(start, end);
    String result = instance.substring(start, end);
    assertEquals(expResult, result);
}

/**
 * Test of first method, of class MySegment.
 */
@Test
public void testFirst()
{
    System.out.println("first");
    MySegment instance = getSegment();
    char expResult = base.charAt(0);
    char result = instance.first();
    assertEquals(expResult, result);
    assertEquals(doneChar, instance.previous());
    assertEquals(0 + offset, instance.getBeginIndex());
    assertNotEquals(doneChar, instance.next());
}

/**
 * Test of last method, of class MySegment.
 */
@Test
public void testLast()
{
    System.out.println("last");
    MySegment instance = getSegment();
    char expResult = base.charAt(base.length() - 1);
    char result = instance.last();
    assertEquals(expResult, result);

    result = instance.current();
    assertEquals(expResult, result);

    result = instance.next();
    assertEquals(doneChar, result);

    result = instance.current();
    assertEquals(doneChar, result);

    assertNotEquals(doneChar, instance.previous());
}

/**
 * Test of current method, of class MySegment.
 */
@Test
public void testCurrent()
{
    System.out.println("current");
    MySegment instance = getSegment();
    char expResult = base.charAt(0);
    char result = instance.current();
    assertEquals(expResult, result);

    expResult = base.charAt(3);
    instance.setIndex(instance.getBeginIndex()+3);
    result = instance.current();
    assertEquals(expResult, result);

    expResult = doneChar;
    instance.setIndex(instance.getEndIndex());
    result = instance.current();
    assertEquals(expResult, result);
}

/**
 * Test of next method, of class MySegment.
 */
@Test
public void testNext()
{
    System.out.println("next");
    MySegment instance = getSegment();
    char expResult = base.charAt(1);
    char result = instance.next();
    assertEquals(expResult, result);

    expResult = base.charAt(count - 1);
    result = instance.last();
    assertEquals(expResult, result);

    expResult = doneChar;
    result = instance.next();
    assertEquals(expResult, result);
}

/**
 * Test of previous method, of class MySegment.
 */
@Test
public void testPrevious()
{
    System.out.println("previous");
    MySegment instance = getSegment();
    char expResult = doneChar;
    char result = instance.previous();
    assertEquals(expResult, result);

    expResult = base.charAt(1);
    instance.setIndex(instance.getBeginIndex() + 2);
    result = instance.previous();
    assertEquals(expResult, result);

    expResult = base.charAt(0);
    result = instance.previous();
    assertEquals(expResult, result);

    expResult = doneChar;
    result = instance.previous();
    assertEquals(expResult, result);
}

/**
 * Test of setIndex method, of class MySegment.
 */
@Test
public void testSetIndex()
{
    System.out.println("setIndex");
    MySegment instance = getSegment();

    char expResult = doneChar;
    instance.setIndex(instance.getBeginIndex() + count);
    char result = instance.current();
    assertEquals(expResult, result);

    expResult = base.charAt(4);
    instance.setIndex(instance.getBeginIndex() + 4);
    result = instance.current();
    assertEquals(expResult, result);
}
/**
 * Test of getEndIndex method, of class MySegment.
 */
@Test
public void testGetEndIndex()
{
    System.out.println("getEndIndex");
    MySegment instance = getSegment();
    int expResult =  offset + count;
    int result = instance.getEndIndex();
    assertEquals(expResult, result);
    // assertEquals(count - 1 + offset, instance.getEndIndex());
}


/**
 * Test of charAt method, of class MySegment.
 */
@Test
public void testCharAt()
{
    System.out.println("charAt");
    int index = 0;
    MySegment instance = getSegment();
    char expResult = base.charAt(0);
    char result = instance.charAt(0);
    assertEquals(expResult, result);

    expResult = base.charAt(3);
    result = instance.charAt(3);
    assertEquals(expResult, result);

    expResult = base.charAt(count-1);
    result = instance.charAt(count-1);
    assertEquals(expResult, result);

    switch(testType) {
    case MY_SEG: 
    @SuppressWarnings("ThrowableResultIgnored")
        StringIndexOutOfBoundsException thrown
                = assertThrows(StringIndexOutOfBoundsException.class,
                               ()->instance.charAt(count));
        break;

    case STR_SEG:
        expResult = doneChar;
        result = instance.charAt(count);
        assertEquals(expResult, result);
        break;
    default:
        throw new RuntimeException();
    }
}

/**
 * Test of length method, of class MySegment.
 */
@Test
public void testLength()
{
    System.out.println("length");
    MySegment instance = getSegment();
    int expResult = base.length();
    int result = instance.length();
    assertEquals(expResult, result);
}

/**
 * Test of atEnd method, of class MySegment.
 */
@Test
public void testAtEnd()
{
    System.out.println("atEnd");
    MySegment instance = getSegment();
    boolean expResult = false;
    boolean result = instance.atEnd();
    assertEquals(expResult, result);

    instance.setIndex(instance.getBeginIndex() + count - 1);
    assertEquals(base.charAt(count-1), instance.current());

    expResult = false;
    result = instance.atEnd();
    assertEquals(expResult, result);

    instance.next();
    expResult = true;
    result = instance.atEnd();
    assertEquals(expResult, result);
}

/**
 * Test of getIndex method, of class MySegment.
 */
@Test
public void testGetIndex()
{
    System.out.println("getIndex");
    MySegment instance = getSegment();
    int expResult = offset;
    int result = instance.getIndex();
    assertEquals(expResult, result);

    instance.previous();
    expResult = offset;
    result = instance.getIndex();
    assertEquals(expResult, result);

    instance.setIndex(instance.getBeginIndex() + count - 1);
    expResult = offset + count -1;
    result = instance.getIndex();
    assertEquals(expResult, result);

    instance.next();
    expResult = offset + count;
    result = instance.getIndex();
    assertEquals(expResult, result);

    instance.next();
    expResult = offset + count;
    result = instance.getIndex();
    assertEquals(expResult, result);
}

// getBeginIndex, gets used a lot
// clone, tested as part of subsequence testing;

}

// 
// /**
//  * Test of getBeginIndex method, of class MySegment.
//  */
// @Test
// public void testGetBeginIndex()
// {
//     System.out.println("getBeginIndex");
//     MySegment instance = getSegment();
//     int expResult = 0;
//     int result = instance.getBeginIndex();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }
// 
// /**
//  * Test of clone method, of class MySegment.
//  */
// @Test
// public void testClone()
// {
//     System.out.println("clone");
//     MySegment instance = getSegment();
//     Object expResult = null;
//     Object result = instance.clone();
//     assertEquals(expResult, result);
//     // TODO review the generated test code and remove the default call to fail.
//     fail("The test case is a prototype.");
// }