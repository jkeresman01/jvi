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
package com.raelity.jvi.core.lib;

/**
 * This collects error messages. It is useful when a process does
 * not stop on first error. It automatically included a line number.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Emsg {
    private StringBuilder sb;
    private int lnum;

    public Emsg()
    {
        this.sb = new StringBuilder();
    }

    /**
     * Add "line number:" to collected messages in a StringBuilder.
     * The StringBuilder is returned so the error specific message
     * can be added; the caller should add a '\n' to the message.
     * @return StringBuilder to which the error message should be added.
     */
    public StringBuilder error()
    {
        if(lnum > 0) {
            sb.append("line ").append(lnum).append(": ");
        }
        return sb;
    }

    public int getLnum()
    {
        return lnum;
    }

    /**
     * Line number associated with error.
     * When lnum is zero, no line number message is output.
     * @param lnum
     */
    public void setLnum(int lnum)
    {
        this.lnum = lnum;
    }

    public int length()
    {
        return sb.length();
    }

    public void clear()
    {
        sb.setLength(0);
        lnum = 0;
    }

    @Override
    public String toString()
    {
        return sb.toString();
    }

}
