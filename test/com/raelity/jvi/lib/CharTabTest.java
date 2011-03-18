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

package com.raelity.jvi.lib;

import java.util.BitSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class CharTabTest {

    public CharTabTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        // setup the default IsWordC
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    // This interface provides the 
    // pattern spec and the expected values
    interface IsWordC {
        String getSpec();
        boolean iswordc(char c);
    }

    static class DefaultIsWordC implements IsWordC
    {
        @Override public String getSpec()
        { return "@,48-57,_,192-255"; }

        @Override
        public boolean iswordc(char c)
        {
            boolean isWord =
                'a' <= c && c <= 'z'
                || 'A' <= c && c <= 'Z'
                || 48 <= c && c <= 57 // '0'...'9'
                || '_' == c
                || 192 <= c && c <= 255
                ;
            return isWord;
        }

    }

    static class NoAlphaIsWordC implements IsWordC
    {
        @Override
        public String getSpec()
        {
            return "48-57,_,192-255";
        }

        @Override
        public boolean iswordc(char c)
        {
            boolean isWord =
                48 <= c && c <= 57 // '0'...'9'
                || '_' == c
                || 192 <= c && c <= 255
                ;
            return isWord;
        }

    }

    static class ExcludeIsWordC implements IsWordC
    {
        @Override
        public String getSpec()
        {
            return "48-57,^50-52,_,192-255";
        }

        @Override
        public boolean iswordc(char c)
        {
            if(50 <= c && c <= 52)
                return false;
            boolean isWord =
                48 <= c && c <= 57 // '0'...'9'
                || '_' == c
                || 192 <= c && c <= 255
                ;
            return isWord;
        }

    }

    static class ExcludeComma implements IsWordC
    {
        @Override
        public String getSpec()
        {
            return "!-~,^,,9";
        }

        @Override
        public boolean iswordc(char c)
        {
            if(',' == c)
                return false;
            boolean isWord =
                '!' <= c && c <= '~'
                || 9 == c // tab
                ;
            return isWord;
        }

    }

    static class DigitsCommaUnderscore implements IsWordC
    {
        @Override
        public String getSpec()
        {
            return "48-57,,,_";
        }

        @Override
        public boolean iswordc(char c)
        {
            boolean isWord =
                48 <= c && c <= 57 // '0'...'9'
                || '_' == c
                || ',' == c
                ;
            return isWord;
        }

    }

    static class LettersAt implements IsWordC
    {
        @Override
        public String getSpec()
        {
            return "a-z,A-Z,@-@";
        }

        @Override
        public boolean iswordc(char c)
        {
            boolean isWord =
                'a' <= c && c <= 'z'
                || 'A' <= c && c <= 'Z'
                || '@' == c
                ;
            return isWord;
        }

    }

    static class PoundCaret implements IsWordC
    {
        @Override
        public String getSpec()
        {
            return "#,^";
        }

        @Override
        public boolean iswordc(char c)
        {
            boolean isWord =
                '#' == c
                || '^' == c
                ;
            return isWord;
        }

    }

    byte[] asArray(IsWordC isWordC) {
        byte[] b = new byte[256];
        for(char c = 0; c < 256; c++) {
            b[c] = isWordC.iswordc(c) ? (byte)1 : (byte)0;
        }
        return b;
    }

    byte[] asArray(CharTab ct) {
        byte[] b = new byte[256];
        for(char c = 0; c < 256; c++)
            b[c] = ct.iswordc(c) ? (byte)1 : (byte)0;
        return b;
    }

    void runTest(IsWordC t) {
        System.out.println("\t" + t.getSpec());
        byte[] expResult = asArray(t);
        CharTab ct = new CharTab();
        ct.init(t.getSpec());
        byte[] result = asArray(ct);
        assertArrayEquals(t.getSpec(), expResult, result);
    }
    
    @Test
    public void testIswordc()
    {
        System.out.println("iswordc");
        runTest(new DefaultIsWordC());
        runTest(new NoAlphaIsWordC());
        runTest(new ExcludeIsWordC());
        runTest(new ExcludeComma());
        runTest(new DigitsCommaUnderscore());
        runTest(new LettersAt());
        runTest(new PoundCaret());
    }
    
    // @Test
    public void testClone()
    {
        System.out.println("clone");
        CharTab instance = new CharTab();
        CharTab expResult = null;
        CharTab result = instance.clone();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}