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

import com.raelity.jvi.lib.TextUtil;

import static com.raelity.jvi.core.lib.Constants.*;

/**
 * This specifies a translation for the map command.
 * The translations occur during typebuf getChar processing.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Mapping {
    String lhs;
    String rhs;
    int mode;
    boolean noremap;
    boolean isUnmap;
    boolean isHidden;

    /**
     * Define a mapping.
     * @param lhs from
     * @param rhs to
     * @param mode which mode(s) the mapping applies to
     * @param noremap if true the rhs should not be scanned for mappings
     */
    public Mapping(String lhs, String rhs, int mode, boolean noremap)
    {
        this.lhs = lhs;
        this.rhs = rhs;
        this.mode = mode;
        this.noremap = noremap;
    }

    /**
     * @return the vim mode(s) for which the mapping is valid
     */
    public int getMode()
    {
        return mode;
    }

    public boolean isNoremap()
    {
        return noremap;
    }

    public String getLhs()
    {
        return lhs;
    }

    public String getRhs()
    {
        return rhs;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(100);
        sb.append("Mapping{'")
                .append(TextUtil.debugString(lhs))
                .append("' --> '").append(TextUtil.debugString(rhs))
                .append("' mode=");
        sb.append(modeString(mode, true));
        sb.append(noremap ? ", noremap" : "")
                .append(isUnmap ? ", isUnmap" : "")
                .append(isHidden ? ", isHidder" : "")
                .append('}');
        return sb.toString();
    }

    /**
     * verbose mean can return "nvo" other wise that is return as ""
     * @return the string
     */
    static String modeString(int mode, boolean verbose)
    {
        StringBuilder sb = new StringBuilder();

        if(verbose || mode != (NORMAL|VISUAL|OP_PENDING|PLATFORM)) {
            if((mode & NORMAL) != 0)     sb.append('n');
            if((mode & VISUAL) != 0)     sb.append('v');
            if((mode & OP_PENDING) != 0) sb.append('o');
            if((mode & PLATFORM) != 0)   sb.append('p');
            if((mode & INSERT) != 0)     sb.append('i');
        }
        return sb.toString();
    }

}
