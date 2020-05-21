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

package com.raelity.jvi.lib;

import java.util.function.Function;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author err
 */
public class ValueHashMapTest
{

public ValueHashMapTest()
{
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
    map = new ValueHashMap<>((val) -> val.theKey);
    map.put(v1);
    map.put(v2);
}

@After
public void tearDown()
{
    map = null;
}

ValueMap<Integer, Val> map;

Val v1 = new Val(2, 3);
Val v2 = new Val(4, 5);
Val v3 = new Val(6,7);
Val v4 = new Val(8,9);

class Val {
Integer theKey;
Integer someField;

public Val(Integer theKey, Integer someField)
{
    this.theKey = theKey;
    this.someField = someField;

}

@Override
public String toString()
{
    return "Val{" + "k=" + theKey + ", xxx=" + someField + '}';
}

}

/**
 * Test of put method, of class ValueHashMap.
 */
@Test
public void testPut_AnyType()
{
    System.out.println("put");
    assertTrue(map.size() == 2);
    assertEquals(v1, map.get(2));
    assertEquals(v2, map.get(4));
    map.put(6, v3);
    // v4's key is 8
    assertThrows(IllegalArgumentException.class, () -> map.put(10, v4));
}

/**
 * Test of getKeyFunction method, of class ValueHashMap.
 */
@Test
public void testGetKeyFunction()
{
    System.out.println("getKeyFunction");
    Function<Val, Integer> func = map.getKeyFunction();
    Integer expResult = 2;
    Integer result = func.apply(v1);
    assertEquals(expResult, result);

    expResult = 6;
    result = func.apply(v3);
    assertEquals(expResult, result);

    Val v = new Val(23,17);
    expResult = 23;
    result = func.apply(v);
    assertEquals(expResult, result);
}

/**
 * Test of replace method, of class ValueHashMap.
 */
@Test
public void testReplace_GenericType_GenericType()
{
    System.out.println("replace");

    // All the values have key '6'

    Val v = map.replace(6, v3);
    assertEquals(null, v);

    map.put(v3);
    Val v3_1 = new Val(6,36);
    v = map.replace(6, v3_1);
    assertEquals(v3, v);

    Val v3_2 = new Val(6,72);
    v = map.replace(6, v3_2);
    assertEquals(v3_1, v);

    assertThrows(IllegalArgumentException.class, () -> map.replace(7, v3));
}

/**
 * Test of replace method, of class ValueHashMap.
 */
@Test
public void testReplace_3args()
{
    System.out.println("replace");

    Val v3_1 = new Val(6,36);
    Val v3_2 = new Val(6,72);

    boolean result;

    map.put(v3);
    result = map.replace(6, v3, v3_1);
    assertEquals(true, result);

    result = map.replace(6, v3_1, v3_2);
    assertEquals(true, result);

    result = map.replace(6, v3, v3_1);
    assertEquals(false, result);

    assertThrows(IllegalArgumentException.class, () -> map.replace(7, v3, v3_1));
}

/**
 * Test of replaceAll method, of class ValueHashMap.
 */
@Test
public void testReplaceAll()
{
    assertThrows(UnsupportedOperationException.class,
                 () -> map.replaceAll(null));
}

/**
 * Test of merge method, of class ValueHashMap.
 */
@Test
public void testMerge()
{
    assertThrows(UnsupportedOperationException.class,
                 () -> map.merge(null, null, null));
}

/**
 * Test of compute method, of class ValueHashMap.
 */
@Test
public void testCompute()
{
    assertThrows(UnsupportedOperationException.class,
                 () -> map.compute(null, null));
}

/**
 * Test of computeIfPresent method, of class ValueHashMap.
 */
@Test
public void testComputeIfPresent()
{
    assertThrows(UnsupportedOperationException.class,
                 () -> map.computeIfPresent(null, null));
}

/**
 * Test of computeIfAbsent method, of class ValueHashMap.
 */
@Test
public void testComputeIfAbsent()
{
    assertThrows(UnsupportedOperationException.class,
                 () -> map.computeIfAbsent(null, null));
}

/**
 * Test of putIfAbsent method, of class ValueHashMap.
 */
@Test
public void testPutIfAbsent()
{
    assertThrows(UnsupportedOperationException.class,
                 () -> map.putIfAbsent(null, null));
}

/**
 * Test of getOrDefault method, of class ValueHashMap.
 */
@Test
public void testGetOrDefault()
{
    assertThrows(UnsupportedOperationException.class,
                 () -> map.getOrDefault(null, null));
}

/**
 * Test of putAll method, of class ValueHashMap.
 */
@Test
public void testPutAll()
{
    assertThrows(UnsupportedOperationException.class,
                 () -> map.putAll(null));
}

}
