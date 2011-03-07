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

package com.raelity.jvi.swing;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
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
public class DocumentCharSequenceTest {
    private static final Logger LOG
            = Logger.getLogger(DocumentCharSequence.class.getName());

    public DocumentCharSequenceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        LOG.setLevel(Level.FINE);
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    PlainDocumentWithTestFeatures doc;
    String baseString;

    private void setUpDocument(boolean chunkit)
    {
        doc = new PlainDocumentWithTestFeatures();
        doc.setChunkit(chunkit);
        try {
            if(!chunkit) {
                // Insert something in the middle of the doc.
                // That should create a gap so we can test the
                // normal case.
                doc.insertString(doc.getLength() / 2, "#", null);
            }
            baseString = doc.getText(0, doc.getLength());
        } catch(BadLocationException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    @Before
    public void setUp() {
        setUpDocument(true);
    }

    @After
    public void tearDown() {
    }

    // @Test
    public void testLength()
    {
        System.out.println("length");
        DocumentCharSequence instance = null;
        int expResult = 0;
        int result = instance.length();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    private void testString(String str, int offset, int len)
    {
        String expRes = str.substring(offset, offset+len);
        DocumentCharSequence cs = new DocumentCharSequence(
                doc, offset, offset + len);
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        for(int i = 0; i < len; i++) {
            sb.append(cs.charAt(i));
        }
        assertEquals(String.format("(%d,%d)", offset, len),
                     expRes, sb.toString());
    }

    @Test
    public void testCharAt()
    {
        System.out.println("charAt");
        testString(baseString, 3, 4);
        testString(baseString, 8, 4);
        testString(baseString, 22, 2);
    }

    private CharSequence
    testSubSeq(String str, CharSequence cs, int start, int end)
    {
        int len = end - start;
        String expRes = str.substring(start, end);
        CharSequence sub = cs.subSequence(start, end);
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        for(int i = 0; i < len; i++) {
            sb.append(sub.charAt(i));
        }
        assertEquals(String.format("(%d,%d)", start, len),
                     expRes, sb.toString());
        assertEquals(String.format("(%d,%d)", start, len),
                     expRes, sub.toString());
        return sub;
    }

    @Test
    public void testSubSequence()
    {
        System.out.println("subSequence");
        int start = 0;
        int end = 0;
        CharSequence cs = new DocumentCharSequence(
                doc, 0, doc.getLength());
        assertEquals(String.format("(%d,%d)", 0, doc.getLength()),
                     baseString, cs.toString());
        cs = testSubSeq(cs.toString(), cs, 6, 24);
        cs = testSubSeq(cs.toString(), cs, 1, 17);
    }

    @Test
    public void testNormalChunks()
    {
        setUpDocument(false);
        CharSequence cs = new DocumentCharSequence(
                doc, 0, doc.getLength());
        cs = testSubSeq(cs.toString(), cs, 0, cs.length());
    }

    // @Test
    public void testToString()
    {
        System.out.println("toString");
        DocumentCharSequence instance = null;
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}