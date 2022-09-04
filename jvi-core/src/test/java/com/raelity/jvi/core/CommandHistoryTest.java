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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.raelity.jvi.core.CommandHistory.HistEntry;
import com.raelity.jvi.core.CommandHistory.HistoryContext;
import com.raelity.jvi.core.CommandHistory.InitialHistoryItem;

import static org.junit.Assert.*;

/**
 *
 * @author err
 */
@RunWith(JUnitParamsRunner.class)
public class CommandHistoryTest
{

public CommandHistoryTest()
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
}

@After
public void tearDown()
{
}

HistoryContext createHistoryContext()
{
    G.p_hi = 20;
    return HistoryContext.create(null);
}

void clearHistory(HistoryContext ctx)
{
    G.p_hi = 0;
    ctx.push("foo");
    G.p_hi = 20;
}

HistoryContext getHistoryContext(List<String> inputList)
{
    HistoryContext hc = createHistoryContext();
    ListIterator<String> iter = inputList.listIterator(inputList.size());

    while(iter.hasPrevious())
        hc.push(iter.previous());
    return hc;
}

HistoryContext getHistoryContext(String[] l)
{
    return getHistoryContext(Arrays.asList(l));
}

String[] S0 = new String[] {
    ""
};

String[] S1 = new String[] {
    "one"
};

String[] S2 = new String[] {
    "one",
    "two",
    "three",
    "four",
};
Object[] initialHistory()
{
    return new Object[] {
        S0,
        S1,
        S2,
    };
}

@Test
@Parameters(method = "initialHistory")
public void testHistoryContext1(String[] l)
{
    if(l[0].isEmpty())
        l = new String[0];
    List<String> list = Arrays.asList(l);
    String expResult, result;

    HistoryContext ctx = getHistoryContext(l);

    InitialHistoryItem initState = ctx.init();
    boolean expAtBegin;
    if(l.length == 0) {
        expResult = "";
        expAtBegin = false;
    } else {
        expResult = l[0];
        expAtBegin = true;
    }
    assertEquals(expResult, initState.getInitialItem());
    assertEquals(expAtBegin, initState.isAtBeginning());

    // go in the next() direction
    assertEquals("", ctx.current());
    for(String s : l) {
        assertEquals(s, ctx.next());
    }
    assertEquals(null, ctx.next());
    assertEquals(null, ctx.next());

    // go in the previous() direction
    ListIterator<String> iter = list.listIterator(list.size());

    // Context doesn't go past the end in the next direction
    // so need to use current().
    if(l.length > 0) {
        expResult = iter.previous();
        result = ctx.current();
        assertEquals(expResult, result);
    }
    while(iter.hasPrevious()) {
        expResult = iter.previous();
        result = ctx.prev();
        assertEquals(expResult, result);
    }
    if(l.length > 0)
        assertEquals("", ctx.prev());
    assertEquals("", ctx.current());

    assertEquals(null, ctx.prev());
    assertEquals(null, ctx.prev());

    // go forwards again
    for(String s : l) {
        assertEquals(s, ctx.next());
    }
}

@Test
@Parameters(method = "initialHistory")
public void testHistoryContextIndex(String[] l)
{
    if(l[0].isEmpty())
        l = new String[0];
    List<String> list = Arrays.asList(l);
    String expResult, result;
    int idx;

    HistoryContext ctx = getHistoryContext(l);

    InitialHistoryItem initState = ctx.init();
    boolean expAtBegin;
    if(l.length == 0) {
        expResult = "";
        expAtBegin = false;
    } else {
        expResult = l[0];
        expAtBegin = true;
    }
    assertEquals(expResult, initState.getInitialItem());
    assertEquals(expAtBegin, initState.isAtBeginning());

    // go in the next() direction
    assertEquals("", ctx.current());
    idx = l.length;
    for(String s : l) {
        HistEntry he = ctx.test_next();
        assertEquals(s, he.hisstr);
        assertEquals(idx, he.hisnum);
        idx--;
    }
    assertEquals(null, ctx.next());
    assertEquals(null, ctx.next());

    // go in the previous() direction
    ListIterator<String> iter = list.listIterator(list.size());
    
    idx = 1;
    // Context doesn't go past the end in the next direction
    // so need to use current().
    if(l.length > 0) {
        HistEntry he = ctx.test_current();
        expResult = iter.previous();
        assertEquals(expResult, he.hisstr);
        assertEquals(idx, he.hisnum);
        idx++;
    }
    while(iter.hasPrevious()) {
        expResult = iter.previous();
        HistEntry he = ctx.test_prev();
        assertEquals(expResult, he.hisstr);
        assertEquals(idx, he.hisnum);
        idx++;
    }
    if(l.length > 0)
        assertEquals("", ctx.prev());
    assertEquals("", ctx.current());


    assertEquals(null, ctx.prev());
    assertEquals(null, ctx.prev());

    assertEquals("", ctx.current());
    if(l.length == 0) {
        expResult = null;
    } else {
        expResult = l[0];
    }

    // go forwards again
    idx = l.length;
    for(String s : l) {
        HistEntry he = ctx.test_next();
        assertEquals(s, he.hisstr);
        assertEquals(idx, he.hisnum);
        idx--;
    }
}

@Test
public void testHistoryContextPush()
{
    String[] l = S2;
    assertEquals("Wrong Size Initialization", S2.length >= 4, true);
    HistoryContext ctx = getHistoryContext(l);
    ctx.init();
    int lastPushCount = ctx.test_next().getIndex();
    ctx.init();

    String s = ctx.next();
    s = ctx.next();
    s = ctx.next();

    HistEntry he = ctx.test_current();
    assertEquals(l[2], he.hisstr);
    ctx.push(he.hisstr);
    ctx.init();
    he = ctx.test_next();
    assertEquals(lastPushCount+1, he.hisnum);

    ctx.init();
    assertEquals(l[2], ctx.next());
    assertEquals(l[0], ctx.next());
    assertEquals(l[1], ctx.next());
    assertEquals(l[3], ctx.next());
}

@Test
public void testHistoryContextPushNew()
{
    String[] l = S2;
    assertEquals("Wrong Size Initialization", S2.length >= 4, true);
    HistoryContext ctx = getHistoryContext(l);
    ctx.init();
    int lastPushCount = ctx.test_next().getIndex();
    ctx.init();

    String s = ctx.next();
    s = ctx.next();
    s = ctx.next();

    HistEntry he = ctx.test_current();
    assertEquals(l[2], he.hisstr);
    ctx.push("NEW");
    ctx.init();
    he = ctx.test_next();
    lastPushCount++;
    assertEquals(lastPushCount, he.hisnum);

    ctx.init();
    HistEntry expResult, result;
    expResult = new HistEntry("NEW", lastPushCount); result = ctx.test_next();
    assertEquals(expResult, result);
    expResult = new HistEntry(l[0], 4); result = ctx.test_next();
    assertEquals(expResult, result);
    expResult = new HistEntry(l[1], 3); result = ctx.test_next();
    assertEquals(expResult, result);
    expResult = new HistEntry(l[2], 2); result = ctx.test_next();
    assertEquals(expResult, result);
    expResult = new HistEntry(l[3], 1); result = ctx.test_next();
    assertEquals(expResult, result);
}

List<String> F1_INIT_HIST = Arrays.asList(new String[] {
    "one",
    "two",
    "pat1",
    "three",
    "pat2",
    "four",
});

String F1_FILTER = "pat";

List<String> F1_FILT_HIST = Arrays.asList(new String[] {
    "pat1",
    "pat2",
});

List<String> F2_INIT_HIST = Arrays.asList(new String[] {
    "pat0",
    "one",
    "two",
    "pat1",
    "three",
    "threex",
    "pat2",
    "four",
    "pat3",
});

String F2_FILTER = "pa";

List<String> F2_FILT_HIST = Arrays.asList(new String[] {
    "pat0",
    "pat1",
    "pat2",
    "pat3",
});

// _INIT_HIST is the full history
// _FILT_HIST is the filtered list
// _FILTER is the filter,

Object[] filterHistory()
{
    return new Object[] {
        new Object[] {F1_INIT_HIST, F1_FILT_HIST, F1_FILTER},
        new Object[] {F2_INIT_HIST, F2_FILT_HIST, F2_FILTER},
    };
}

@Test
@Parameters(method = "filterHistory")
public void testFilter(List<String> initHist, List<String> filtHist, String filter)
{
    String expResult, result;
    HistoryContext ctx = getHistoryContext(initHist);

    ctx.init();
    result = ctx.current(); expResult = "";
    assertEquals(expResult, result);

    result = ctx.next(); expResult = initHist.get(0);
    assertEquals(expResult, result);
    result = ctx.prev(); expResult = "";
    assertEquals(expResult, result);

    ctx.setFilter(filter);
    // now the top is the filter
    result = ctx.current(); expResult = filter; 
    assertEquals(expResult, result);

    // check filtered list in next direction
    ListIterator<String> iter = filtHist.listIterator();
    while(iter.hasNext()) {
        result = ctx.next(); expResult = iter.next(); 
        assertEquals(expResult, result);
    }
    assertEquals(null, ctx.next());
    assertEquals(null, ctx.next());

    // check filtered list in prev direction
    iter = filtHist.listIterator(filtHist.size());
    result = ctx.current();
    expResult = iter.previous();
    assertEquals(expResult, result);
    while(iter.hasPrevious()) {
        result = ctx.prev();
        expResult = iter.previous();
        assertEquals(expResult, result);
    }
    result = ctx.prev(); expResult = filter; 
    assertEquals(expResult, result);

    assertEquals(null, ctx.prev());
    assertEquals(null, ctx.prev());
    result = ctx.current();
    expResult = filter;
    assertEquals(expResult, result);

    // ctx is most recent input,
    // go to next filter item, clear filter, verify next() is correct
    result = ctx.next();
    expResult = filtHist.get(0);
    assertEquals(expResult, result);

    ctx.setFilter("");
    // current should not have changed
    result = ctx.current();
    assertEquals(expResult, result);
    // now next should be according to original list
    // find current, now in result, in the original list
    iter = initHist.listIterator();
    while(iter.hasNext())
        if(iter.next().equals(result))
            break;
    expResult = iter.next();
    result = ctx.next();
    assertEquals(expResult, result);
}

List<String> TRIM = Arrays.asList(new String[] {
    "a",
    "b",
    "c",
    "d",
    "e",
    "f",
    "g",
});

/**
 * verify that size is limited.
 */
@Test
public void testTrim1()
{
    HistoryContext ctx = getHistoryContext(Collections.emptyList());
    G.p_hi = 4;

    ctx.init();
    assertEquals(0, ctx.size());
    ListIterator<String> iter = TRIM.listIterator(TRIM.size());
    while(iter.hasPrevious())
        ctx.push(iter.previous());
    assertEquals(G.p_hi, ctx.size());

    ctx.init();
    iter = TRIM.listIterator();
    String result, expResult;
    int count = 0;
    while((result = ctx.next()) != null) {
        expResult = iter.next();
        assertEquals(expResult, result);
        count++;
    }
    assertEquals(G.p_hi, count);
}

/**
 * verify that size gets truncated
 */
@Test
public void testTrim2()
{
    HistoryContext ctx = getHistoryContext(Collections.emptyList());

    ctx.init();
    assertEquals(0, ctx.size());
    ListIterator<String> iter = TRIM.listIterator(TRIM.size());
    while(iter.hasPrevious())
        ctx.push(iter.previous());
    assertEquals(TRIM.size(), ctx.size());

    ctx.init();
    G.p_hi = 4;
    ctx.trim();
    assertEquals(G.p_hi, ctx.size());


    ctx.init();
    iter = TRIM.listIterator();
    String result, expResult;
    int count = 0;
    while((result = ctx.next()) != null) {
        expResult = iter.next();
        assertEquals(expResult, result);
        count++;
    }
    assertEquals(G.p_hi, count);
}

/**
 * Bug. If something is pushed and it is item zero, last item in history,
 *      then if identical is pushed, it was missed.
 */
@Test
public void testPushOfItemZeroRemoved()
{
    HistoryContext ctx = getHistoryContext(Collections.emptyList());
    G.p_hi = 4;

    ctx.init();
    assertEquals(0, ctx.size());
    ListIterator<String> iter = TRIM.listIterator(TRIM.size());
    while(iter.hasPrevious())
        ctx.push(iter.previous());
    assertEquals(G.p_hi, ctx.size());

    ctx.init();
    iter = TRIM.listIterator();
    String result, expResult;
    int count = 0;
    while((result = ctx.next()) != null) {
        expResult = iter.next();
        assertEquals(expResult, result);
        count++;
    }
    assertEquals(G.p_hi, count);

    G.p_hi = 0;
    ctx.push("foo");
    assertEquals(0, ctx.size());

    // Bug. If something is pushed and it is item zero (last item in history)
    //      then if identical is pushed.
    clearHistory(ctx);
    ctx.push("foo");
    ctx.push("bar");
    ctx.push("baz");
    ctx.push("foo");
    assertEquals(3, ctx.size());

    clearHistory(ctx);
    ctx.push("foo");
    ctx.push("foo");
    assertEquals(1, ctx.size());

}
}
