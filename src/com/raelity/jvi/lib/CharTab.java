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

/**
 * CharTab implements the vim 'iskeyword' related behavior.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class CharTab implements Cloneable {

    private BitSet table = new BitSet(256);
    private String spec = "";

    /**
     * Parse the spec.
     * Do iskeyword processing only
     */
    public boolean init(String spec) {
        BitSet newTable = new BitSet(256);

        int p = 0;
        char c;
        int c2;
        boolean tilde;
        boolean do_isalpha;
        MutableInt mi = new MutableInt();

	while (p < spec.length())
	{
	    tilde = false;
	    do_isalpha = false;
	    if (spec.charAt(p) == '^' && p < (spec.length() - 1))
	    {
		tilde = true;
		++p;
	    }
            mi.setValue(p);
            c = getCharValue(spec, mi);
            p = mi.getValue();
	    c2 = -1;
	    if (p < spec.length()
                    && spec.charAt(p) == '-' && p < (spec.length() - 1))
	    {
		++p;
                mi.setValue(p);
                c2 = getCharValue(spec, mi);
                p = mi.getValue();
	    }
	    if (c <= 0 || (c2 < c && c2 != -1) || c2 >= 256
                     || !(p == spec.length() || spec.charAt(p) == ','))
		return false;

	    if (c2 == -1)	/* not a range */
	    {
		// A single '@' (not "@-@"):
		// Decide on letters being ID/printable/keyword chars with
		// standard function isalpha(). This takes care of locale for
		// single-byte characters).
		if (c == '@')
		{
		    do_isalpha = true;
		    c = 1;
		    c2 = 255;
		}
		else
		    c2 = c;
	    }
	    while (c <= c2)
	    {
		if (!do_isalpha || isalpha(c)
// #ifdef FEAT_FKMAP
// 			|| (p_altkeymap && (F_isalpha(c) || F_isdigit(c)))
// #endif
			    )
		{
		    /* (re)set keyword flag */
                    if (tilde)
                        newTable.clear(c);
                    else
                        newTable.set(c);
		}
		++c;
	    }
	    p = skip_to_option_part(spec, p);
	}
        this.spec = spec;
        table = newTable;
        return true;
    }

    public boolean iswordc(char c) {
        boolean isWord;
        if(c < 0x100) {
            isWord = table.get(c);
        } else {
            isWord = Character.isLetterOrDigit(c);
        }
        return isWord;
    }

    public String getSpec() {
        return spec;
    }

    private BitSet getTableClone() {
        return (BitSet)table.clone();
    }

    @Override
    public CharTab clone() throws CloneNotSupportedException {
        CharTab o = null;
        try {
            o = (CharTab)super.clone();
            o.table = (BitSet)table.clone();
        } catch(CloneNotSupportedException ex) {
        }
        return o;
    }

    private static boolean isdigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isalpha(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static int skip_to_option_part(String s, int p) {
        if(p >= s.length())
            return p;
        if(s.charAt(p) == ',')
            p++;
        while(p < s.length() && s.charAt(p) == ' ')
            p++;
        return p;
    }

    private static char getdigits(String s, MutableInt mi) {
        int p = mi.getValue();
        int q = p;

        while(q < s.length() && isdigit(s.charAt(q)))
            q++;
        mi.setValue(q);

        int i = 0;
        try {
            i = Integer.parseInt(s.substring(p, q));
        } catch(NumberFormatException ex) {
        }
        if(i >= 0x100)
            i = 0;
        
        return (char)i;
    }

    private char getCharValue(String s, MutableInt mi) {
        int p = mi.getValue();
        char c;

        if (isdigit(s.charAt(p))) {
            c = getdigits(s, mi);
        } else {
            c = s.charAt(p);
            p++;
            mi.setValue(p);
        }

        return c;
    }

}
