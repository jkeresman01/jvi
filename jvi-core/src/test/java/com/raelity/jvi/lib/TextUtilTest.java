/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2022 Ernie Rael.  All Rights Reserved.
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

package com.raelity.jvi.lib;

import java.text.StringCharacterIterator;
import java.util.List;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author err
 */
public class TextUtilTest
{

public TextUtilTest()
{
}

@BeforeAll
public static void setUpClass()
{
}

@AfterAll
public static void tearDownClass()
{
}

@BeforeEach
public void setUp()
{
}

@AfterEach
public void tearDown()
{
}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testtoString_1arg()
{
    System.out.println("toString_1arg");
    String base = "one";

    assertEquals("", TextUtil.toString(new StringCharacterIterator("")));
    assertEquals("", TextUtil.toString(new StringCharacterIterator("" + "\n")));

    assertEquals(base, TextUtil.toString(new StringCharacterIterator(base)));
    assertEquals(base, TextUtil.toString(new StringCharacterIterator(base + "\n")));

    assertEquals("", TextUtil.toString(new MySegment("")));
    assertEquals("", TextUtil.toString(new MySegment("" + "\n")));

    assertEquals(base, TextUtil.toString(new MySegment(base)));
    assertEquals(base, TextUtil.toString(new MySegment(base + "\n")));

    assertEquals(base, TextUtil.toString(new StringSegment(base)));
    assertEquals(base, TextUtil.toString(new StringSegment(base + "\n")));
}


/**
 * Test of tokens method, of class TextUtil.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testTokens()
{
    System.out.println("tokens");
    List<String> expResult;
    List<String> result;
    expResult = List.of("one", "two", "three");
    result = TextUtil.tokens("one two three");
    assertEquals(expResult, result);

    result = TextUtil.tokens("");
    assertEquals(List.of(), result);

    result = TextUtil.tokens("one\ntwo\nthree");
    assertEquals(expResult, result);

    result = TextUtil.tokens("\none\ntwo\nthree\n");
    assertEquals(expResult, result);

    result = TextUtil.tokens("one");
    assertEquals(List.of("one"), result);

    result = TextUtil.tokens("\none");
    assertEquals(List.of("one"), result);

    result = TextUtil.tokens("one ");
    assertEquals(List.of("one"), result);

    result = TextUtil.tokens(" \n one  \n  two  \n  three  \n  ");
    assertEquals(expResult, result);

    System.err.println("done: " + result);
    // TODO review the generated test code and remove the default call to fail.
}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testTokens_2args()
{
    System.out.println("tokens_2args");
    List<String> expResult;
    List<String> result;
    expResult = List.of("one", "two", "three");
    result = TextUtil.tokens("one, two three", "[,\\s]+");
    assertEquals(expResult, result);

    result = TextUtil.tokens(", \n", "[,\\s]+");
    assertEquals(List.of(), result);

    result = TextUtil.tokens("one,\ntwo,\nthree", "[,\\s]+");
    assertEquals(expResult, result);

    result = TextUtil.tokens("\none\ntwo\nthree\n", "[,\\s]+");
    assertEquals(expResult, result);

    result = TextUtil.tokens("one", "[,\\s]+");
    assertEquals(List.of("one"), result);

    result = TextUtil.tokens(",one", "[,\\s]+");
    assertEquals(List.of("one"), result);

    result = TextUtil.tokens("one,", "[,\\s]+");
    assertEquals(List.of("one"), result);

    result = TextUtil.tokens(" ,\n one  \n,  two  \n  three , \n  ", "[,\\s]+");
    assertEquals(expResult, result);

    System.err.println("done: " + result);
    // TODO review the generated test code and remove the default call to fail.
}
}
