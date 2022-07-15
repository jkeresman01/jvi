
/*
* @(#)XmlUtil.java
*
* Copyright (C) 2001,,2003 2002 Matt Albrecht
* groboclown@users.sourceforge.net
* http://groboutils.sourceforge.net
*
*  Permission is hereby granted, free of charge, to any person obtaining a
*  copy of this software and associated documentation files (the "Software"),
*  to deal in the Software without restriction, including without limitation
*  the rights to use, copy, modify, merge, publish, distribute, sublicense,
*  and/or sell copies of the Software, and to permit persons to whom the 
*  Software is furnished to do so, subject to the following conditions:
*
*  The above copyright notice and this permission notice shall be included in 
*  all copies or substantial portions of the Software. 
*
*  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
*  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
*  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL 
*  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
*  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
*  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
*  DEALINGS IN THE SOFTWARE.
*/

package com.raelity.jvi.lib;
//package net.sourceforge.groboutils.util.xml.v1;

/**
 * A Utility to aid in various XML activities.
 * In particular convert a simple string into an xml string
 * by transforming characters. There is a default mapping available
 * which converts the invalid '&lt;', '&gt;', '&quot;', '&apos;', '&amp;'
 * to valid '&amp;lt', '&amp;gt', '&amp;quot', '&amp;apos', '&amp;amp'.
 * A custom invalid to valid mapping can be provided for special situation,
 * for example to convert '\n' to '&lt;br/&gt;'.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     May 21, 2001
 * @version   $Date$
 */
public class XMLUtil {

    private static XMLUtil s_instance;
    // *  [2]    Char    ::=    #x9 | #xA | #xD | [#x20-#xD7FF] |
    // *                        [#xE000-#xFFFD] | [#x10000-#x10FFFF]
    //private static final char LOWER_RANGE_1 = 0x20;
    //private static final char UPPER_RANGE_1 = 0xD7FF;
    //private static final char LOWER_RANGE_2 = 0xE000;
    //private static final char UPPER_RANGE_2 = 0xFFFD;
    private static final char LOWER_RANGE = 32;
    private static final char UPPER_RANGE = 127;
    // java doesn't support this range
    // private static final char LOWER_RANGE_3 = 0x10000;
    // private static final char UPPER_RANGE_3 = 0x10FFFF;
    private static final char VALID_CHAR_1 = 9;
    private static final char VALID_CHAR_2 = 10;
    private static final char VALID_CHAR_3 = 13;
    private static final char[] IN_RANGE_INVALID
            = {'<',    '>',    '"',      '\'',     '&'};
    private static final String[] IN_RANGE_VALID
            = {"&lt;", "&gt;", "&quot;", "&apos;", "&amp;"};
    //private static final String IN_RANGE_INVALID_STR =
    //    new String( IN_RANGE_INVALID );

    private final char[] invalid;
    private final String[] valid;

    protected XMLUtil()
    {
        this(IN_RANGE_INVALID, IN_RANGE_VALID);
    }

    public XMLUtil(char[] invalid, String[] valid)
    {
        this.invalid = invalid;
        this.valid = valid;
    }

    /**
     * So this can be used without needing to construct.
     * @return XMLUtil instance with default translation map.
     */
    public static XMLUtil get()
    {
        if(s_instance == null)
            s_instance = new XMLUtil();
        return s_instance;
    }
    //------------------------------------------
    //------------------------------------------

    /**
     * Convert a standard Java String into an XML string.  It transforms
     * out-of-range characters (&lt;, &gt;, &amp;, ", ', and non-standard
     * character values) into XML formatted values.  Since it does correctly
     * escape the quote characters, this may be used for both attribute values
     * as well as standard text.
     *
     * @param javaStr the Java string to be transformed into XML text.  If
     *      the string is <tt>null</tt>, then <tt>null</tt> is returned.
     * @return the XML version of <tt>javaStr</tt>.
     * @see #utf2xml( String, StringBuilder )
     */
    public String utf2xml(String javaStr)
    {
        if(javaStr == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        utf2xml(javaStr, sb);
        return sb.toString();
    }

    /**
     * Convert a standard Java String into an XML string.  It transforms
     * out-of-range characters (&lt;, &gt;, &amp;, ", ', and non-standard
     * character values) into XML formatted values.  Since it does correctly
     * escape the quote characters, this may be used for both attribute values
     * as well as standard text.
     * <P>
     * From <a href="http://www.w3c.org/TR/2000/REC-xml-20001006">
     * the XML recommendation</a>:
     * <PRE>
     * [Definition: A parsed entity contains text, a sequence of characters,
     * which may represent markup or character data.]
     * [Definition: A character is an atomic unit of text as specified by
     * ISO/IEC 10646 [ISO/IEC 10646] (see also [ISO/IEC 10646-2000]).
     * Legal characters are tab, carriage return, line feed, and the legal
     * characters of Unicode and ISO/IEC 10646. The versions of these standards
     * cited in A.1 Normative References were current at the time this document
     * was prepared. New characters may be added to these standards by
     * amendments or new editions. Consequently, XML processors must accept
     * any character in the range specified for Char. The use of
     * "compatibility characters", as defined in section 6.8 of
     * [Unicode] (see also D21 in section 3.6 of [Unicode3]), is discouraged.]
     *
     * Character Range
     *  [2]    Char    ::=    #x9 | #xA | #xD | [#x20-#xD7FF] |
     *                        [#xE000-#xFFFD] | [#x10000-#x10FFFF]
     *         // any Unicode character, excluding the surrogate blocks,
     *            FFFE, and FFFF. //
     *
     * The mechanism for encoding character code points into bit patterns may
     * vary from entity to entity. All XML processors must accept the UTF-8
     * and UTF-16 encodings of 10646; the mechanisms for signaling which of
     * the two is in use, or for bringing other encodings into play, are
     * discussed later, in 4.3.3 Character Encoding in Entities.
     *
     * ...
     *
     * The ampersand character (&amp;) and the left angle bracket (&lt;)
     * may appear in their literal form only when used as markup delimiters, or
     * within a comment, a processing instruction, or a CDATA section. If they
     * are needed elsewhere, they must be escaped using either numeric
     * character references or the strings "&amp;amp;" and "&amp;lt;"
     * respectively. The right angle bracket (>) may be represented using the
     * string "&amp;gt;", and must, for compatibility, be escaped using
     * "&amp;gt;" or a character reference when it appears in the string
     * "]]>" in content, when that string is not marking the end of a CDATA
     * section.
     * To allow attribute values to contain both single and double quotes, the
     * apostrophe or single-quote character (&apos;) may be represented as
     * "&amp;apos;", and the double-quote character (&quot;) as "&amp;quot;".
     * </PRE>
     *
     * @param javaStr the Java string to be transformed into XML text. If
     *      it is <tt>null</tt>, then the text "null" is appended to the
     * @param output the StringBuilder to send the transformed XML into.
     */
    public void utf2xml(String javaStr, StringBuilder output)
    {
        if(output == null) {
            throw new IllegalArgumentException("No null StringBuilder");
        }
        if(javaStr == null) {
            // original:
            // javaStr = "null";
            // the string "null" does not have any out-of-range characters,
            // so to optimize...
            output.append("null");
            return;
        }
        int len = javaStr.length();
        // Ensure that the output string buffer has enough space.
        // The given huristic seems to work well.
        output.ensureCapacity(output.length() + (len * 2));
        // for efficiency, directly access the array.
        char[] buf = javaStr.toCharArray();
        for(int pos = 0; pos < len; ++pos) {
            char c = buf[pos];
            // test for out-of-range for escaping using &#
            if((c < LOWER_RANGE && c != VALID_CHAR_1 && c != VALID_CHAR_2 &&
                    c != VALID_CHAR_3) || (c > UPPER_RANGE)) {
                output.append("&#");
                output.append(Integer.toString(c));
                output.append(';');
            } else {
                // should we escape the character with an &XXX; ?
                boolean notfound = true;
                for(int p2 = invalid.length; --p2 >= 0;) {
                    if(invalid[p2] == c) {
                        notfound = false;
                        output.append(valid[p2]);
                        break;
                    }
                }
                if(notfound) {
                    // append the character as-is
                    output.append(c);
                }
            }
        }
    }
}
