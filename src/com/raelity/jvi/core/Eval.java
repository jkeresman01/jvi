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
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.core;

import com.raelity.jvi.ViFPOS;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.MarkOps.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Util.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Eval {

    /** NEEDSWORK: make class SaveCpo part of G */
    public static class SaveCpo {
        boolean w;
        boolean search;
        boolean j;

        public SaveCpo() {
            w      = G.p_cpo_w();
            search = G.p_cpo_search();
            j      = G.p_cpo_j();
        }

        public void restore() {
            G.p_cpo_wOption_setBoolean(w);
            G.p_cpo_searchOption_setBoolean(search);
            G.p_cpo_jOption_setBoolean(j);
        }

        static public void clearCpo() {
            G.p_cpo_wOption_setBoolean(false);
            G.p_cpo_searchOption_setBoolean(false);
            G.p_cpo_jOption_setBoolean(false);
        }
    }

/*
 * Search for a start/middle/end thing.
 * Used by searchpair(), see its documentation for the details.
 * Returns 0 or -1 for no match,
 *
 * NOTE: use of searchit in V7 is different!
 */
public static int do_searchpair(
            //spat, mpat, epat, dir, skip, flags, match_pos, lnum_stop)
    String	spat,	    /* start pattern */
    String	mpat,	    /* middle pattern */
    String	epat,	    /* end pattern */
    int		dir,	    /* BACKWARD or FORWARD */
    String	skip,	    /* skip expression */
    int		flags,	    /* SP_SETPCMARK and other SP_ values */
    ViFPOS	match_pos,  // NOTE: was *match_pos to RETURN a value
    int         lnum_stop   /* stop at this line if not zero */
)
{
    SaveCpo	save_cpo;
    String	pat, pat2 = null, pat3 = null;
    int		retval = 0;
    ViFPOS	pos;
    ViFPOS	firstpos;
    ViFPOS	foundpos;
    ViFPOS	save_cursor;
    ViFPOS	save_pos;
    int		n;
    boolean	r;
    int		nest = 1;
    boolean	err;
    int		options = SEARCH_KEEP;

    /* Make 'cpoptions' empty, the 'l' flag should not be used here. */
    save_cpo = new SaveCpo();
    SaveCpo.clearCpo(); // p_cpo = (char_u *)"";

    /* Make two search patterns: start/end (pat2, for in nested pairs) and
     * start/middle/end (pat3, for the top pair). */

    //sprintf((char *)pat2, "\\(%s\\m\\)\\|\\(%s\\m\\)", spat, epat);
    pat2 = String.format("(%s)|(%s)", spat, epat);
    if (mpat.length() == 0)
        pat3 = pat2; //STRCPY(pat3, pat2);
    else {
        //sprintf((char *)pat3, "\\(%s\\m\\)\\|\\(%s\\m\\)\\|\\(%s\\m\\)",
        //                                                  spat, epat, mpat);
        pat3 = String.format("(%s)|(%s)|(%s)", spat, epat, mpat);
    }
    if ((flags & SP_START) != 0)
        options |= SEARCH_START;

    save_cursor = G.curwin.w_cursor.copy();
    pos = G.curwin.w_cursor.copy();
    firstpos = null; //clearpos(&firstpos);
    foundpos = null; //clearpos(&foundpos);
    pat = pat3;
    for (;;)
    {
        n = Search.searchit(G.curwin,
                            //curbuf,
                            pos, dir, pat, 1,
                            options,
                            RE_SEARCH, lnum_stop);
        if (n == FAIL || equalpos(pos, firstpos))
            /* didn't find it or found the first match again: FAIL */
            break;

        if (firstpos == null)
            firstpos = pos.copy();
        if (equalpos(pos, foundpos))
        {
            /* Found the same position again.  Can happen with a pattern that
             * has "\zs" at the end and searching backwards.  Advance one
             * character and try again. */
            if (dir == BACKWARD)
                decl(pos);
            else
                inclV7(pos);
        }
        foundpos = pos.copy();

        /* If the skip pattern matches, ignore this match. */
        if (skip.length() != 0)
        {
            save_pos = G.curwin.w_cursor.copy();
            G.curwin.w_cursor.set(pos);
            if(true)
                throw new RuntimeException("eval_to_bool not implemented");
            //r = eval_to_bool(skip, &err, null, false);
            G.curwin.w_cursor.set(save_pos);
            if (err)
            {
                /* Evaluating {skip} caused an error, break here. */
                G.curwin.w_cursor.set(save_cursor);
                retval = -1;
                break;
            }
            if (r)
                continue;
        }

        if ((dir == BACKWARD && n == 3) || (dir == FORWARD && n == 2))
        {
            /* Found end when searching backwards or start when searching
             * forward: nested pair. */
            ++nest;
            pat = pat2;		/* nested, don't search for middle */
        }
        else
        {
            /* Found end when searching forward or start when searching
             * backward: end of (nested) pair; or found middle in outer pair. */
            if (--nest == 1)
                pat = pat3;	/* outer level, search for middle */
        }

        if (nest == 0)
        {
            /* Found the match: return matchcount or line number. */
            if ((flags & SP_RETCOUNT) != 0)
                ++retval;
            else
                retval = pos.getLine();
            if ((flags & SP_SETPCMARK) != 0)
                setpcmark();
            G.curwin.w_cursor.set(pos);
            if ((flags & SP_REPEAT) == 0)
                break;
            nest = 1;	    /* search for next unmatched */
        }
    }

    if (match_pos != null)
    {
        /* Store the match cursor position */
        match_pos.set(G.curwin.w_cursor.getLine(),
                      G.curwin.w_cursor.getColumn() + 1);
    }

    /* If 'n' flag is used or search failed: restore cursor position. */
    if ((flags & SP_NOMOVE) != 0 || retval == 0)
        G.curwin.w_cursor.set(save_cursor);

    save_cpo.restore(); // p_cpo = save_cpo;

    return retval;
}

    private Eval()
    {
    }

}
