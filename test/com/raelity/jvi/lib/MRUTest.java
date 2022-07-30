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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.raelity.jvi.lib.MRU.IndexedEntry;

import static org.junit.Assert.*;

import static com.raelity.jvi.lib.TextUtil.sf;

/**
 *
 * @author err
 */
@RunWith(Parameterized.class)
public class MRUTest
{
final String which;

final MRU<String> mru;
private static final int DEFAULT_LIMIT = 5;
int limit = DEFAULT_LIMIT;

@Parameters(name = "{0}")
public static Object[][] data() {
    return new Object[][] {
        {"list"}
            ,
        {"set"}
    };
}
public MRUTest(String which)
{
    this.which = which;
    MRU<String> tmru;
    switch(which) {
    case "list": tmru = MRU.getListMRU(() -> limit); break;
    case "set": tmru = MRU.getSetMRU(() -> limit); break;
    default: throw new RuntimeException("WHAT?");
    }
    this.mru = tmru;
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
    initMRU();
}

@After
public void tearDown()
{
}

void initMRU()
{
    limit = DEFAULT_LIMIT;
    mru.clear();
    mru.addAll(l1);
    initL2();
    initLRev();
}
void initL2()
{
    l2.clear();
    l2.addAll(l1);
}
void initLRev()
{
    lrev.clear();
    lrev.addAll(l1);
    Collections.reverse(lrev);
}

List<String> l1 = List.of("one", "two", "three", "four", "five");
List<String> l2 = new ArrayList<>();
List<String> lrev = new ArrayList<>();

/**
 * Test of addItem method, of class MRU.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testAddItem()
{
    System.out.println("addItem " + which);

    ArrayList<String> l;

    // initial state
    l = new ArrayList<>(mru);
    assertEquals(l1, l);

    // adding the MRU should not change anything, return false
    boolean result = mru.addItem("one");
    assertEquals(false, result);

    l = new ArrayList<>(mru);
    assertEquals(l1, l);

    result = mru.addItem("two"); // true, changed the list
    assertEquals(true, result);

    l = new ArrayList<>(mru);
    l2.remove("two");
    l2.add(0, "two");
    assertEquals(l2, l);

    initL2();
    for(String key : l1) {
        mru.addItem(key);
    }
    l = new ArrayList<>(mru);
    assertEquals(lrev, l);
}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void TestIndexedIterator()
{
    System.out.println("indexedIterator " + which);

    if(!(mru instanceof MRU.IndexedIterable)) {
        assertEquals(null, mru.indexedIterable());
        System.out.println(sf("    %s does NOT support IndexedIterable", which));
        return;
    }
    String[] tArr = new String[mru.size()];
    for(IndexedEntry<String> ie : mru.indexedIterable()) {
        tArr[ie.getIndex()] = ie.getItem();
    }
    List<String> l = Arrays.asList(tArr);
    assertEquals(l1, l);
}

/**
 * Test of removeItem method, of class MRU.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testRemoveItem()
{
    System.out.println("removeItem " + which);

    ArrayList<String> l;
    boolean result;

    result = mru.removeItem("foobar");
    assertEquals(false, result);

    result = mru.removeItem("four");
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    l2.remove("four");
    assertEquals(l2, l);

    result = mru.removeItem("three");
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    l2.remove("three");
    assertEquals(l2, l);

}

/**
 * Test of trim method, of class MRU.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testTrim()
{
    System.out.println("trim " + which);
    ArrayList<String> l;
    boolean result;

    // trim does nothing, returns false if within bounds
    result = mru.trim();
    assertEquals(false, result);
    l = new ArrayList<>(mru);
    assertEquals(l1, l);

    limit = 4;
    result = mru.trim();
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    l2.remove("five");
    assertEquals(l2, l);

    // make sure initMRU creates full list
    initMRU();
    l = new ArrayList<>(mru);
    assertEquals(l1, l);

    limit = 3;
    result = mru.trim();
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    l2.removeAll(List.of("four", "five"));
    assertEquals(l2, l);

    initMRU();
    limit = 0;
    result = mru.trim();
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    assertEquals(List.of(), l);

    // check trim operates during addAll
    // Note that l2 was not modified since previous initMRU
    limit = 3;
    mru.clear();
    mru.addAll(l1);
    l = new ArrayList<>(mru);
    l2.removeAll(List.of("four", "five"));
    assertEquals(l2, l);
}
}
