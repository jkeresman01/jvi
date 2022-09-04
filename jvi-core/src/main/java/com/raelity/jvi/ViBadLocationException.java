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

package com.raelity.jvi;

/**
 * Patterned after swing's BadLocationException.
 * 
 * @author Ernie Rael <err at raelity.com>
 */
public class ViBadLocationException extends Exception {
    final private int offs;
    /**
     * Creates a new ViBadLocationException object.
     * 
     * @param s		a string indicating what was wrong with the arguments
     * @param offs      offset within the document that was requested >= 0
     */
    public ViBadLocationException(String s, int offs) {
	super(s);
	this.offs = offs;
    }

    public ViBadLocationException(String s, int offs, Throwable cause)
    {
        super(s, cause);
	this.offs = offs;
    }

    /**
     * Returns the offset into the document that was not legal.
     *
     * @return the offset >= 0
     */
    public int offsetRequested() {
	return offs;
    }
}
