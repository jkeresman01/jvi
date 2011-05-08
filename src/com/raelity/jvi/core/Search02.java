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

package com.raelity.jvi.core;

import com.raelity.jvi.ViFPOS;
import com.raelity.text.TextUtil.MySegment;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Search03.*;
import static com.raelity.jvi.core.Util.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Search02 {

    private Search02()
    {
    }

    //
    // "Other" Searches
    //
    // ****************************************************
    // NOTE: The findmatch and related are taken from vim7
    // ****************************************************
    ///

    /**
     * findmatch - find the matching paren or brace
     *
     * Improvement over vi: Braces inside quotes are ignored.
     */
    static ViFPOS findmatch(OPARG oap, char initc)
    {
        return findmatchlimit(oap, initc, 0, 0);
    }

    /**
     * findmatchlimit -- find the matching paren or brace, if it exists within
     * maxtravel lines of here.  A maxtravel of 0 means search until falling off
     * the edge of the file.
     *
     * "initc" is the character to find a match for.  NUL means to find the
     * character at or after the cursor.
     *
     * flags: FM_BACKWARD	search backwards (when initc is '/', '*' or '#')
     *	  FM_FORWARD	search forwards (when initc is '/', '*' or '#')
     *	  FM_BLOCKSTOP	stop at start/end of block ({ or } in column 0)
     *	  FM_SKIPCOMM	skip comments (not implemented yet!)
     *
     * "oap" is only used to set oap->motion_type for a linewise motion, it be
     * NULL
     */

    static ViFPOS findmatchlimit(OPARG oap, char initc, int flags, int maxtravel)
    {
        return findmatchlimit(G.curwin.w_cursor.copy(), oap,
                              initc, flags, maxtravel);
    }

    static ViFPOS findmatchlimit(ViFPOS cursor,
            OPARG oap, char initc, int flags, int maxtravel)
    {
        int         pos_lnum;
        int         pos_col;
        char	findc = 0;		// matching brace
        char 	c;
        int		count = 0;	// cumulative number of braces
        boolean	backwards = false;	// init for gcc
        boolean	inquote = false;	// TRUE when inside quotes
        MySegment 	linep;		// pointer to current line
        int 	ptr;
        int		do_quotes;	// check for quotes in current line
        int		at_start;	// do_quotes value at start position
        int		hash_dir = 0;	// Direction searched for # things
        int		comment_dir = 0;// Direction searched for comments
        //ViFPOS	match_pos;	// Where last slash-star was found
        int 	match_pos_lnum;
        int 	match_pos_col;
        int		start_in_quotes;// start position is in quotes
        int		traveled = 0;	// how far we've searched so far
        boolean	ignore_cend = false;    // ignore comment end
        boolean	cpo_match;		// vi compatible matching
        boolean	cpo_bsl;		// don't recognize backslashes
        int		match_escaped = 0;	// search for escaped match
        int		dir;			// Direction to search
        int		comment_col = MAXCOL;   // start of / / comment
    //#ifdef FEAT_LISP...

        // pos_lnum = G.curwin.w_cursor.getLine();
        // pos_col = G.curwin.w_cursor.getColumn();
        pos_lnum = cursor.getLine();
        pos_col = cursor.getColumn();
        linep = ml_get(pos_lnum);

        cpo_match = (vim_strchr(G.p_cpo, 0, CPO_MATCH) >= 0);
        cpo_bsl = (vim_strchr(G.p_cpo, 0, CPO_MATCHBSL) >= 0);

        /* Direction to search when initc is '/', '*' or '#' */
        if ((flags & FM_BACKWARD) != 0)
            dir = BACKWARD;
        else if ((flags & FM_FORWARD) != 0)
            dir = FORWARD;
        else
            dir = 0;

        //
        // if initc given, look in the table for the matching character
        // '/' and '*' are special cases: look for start or end of comment.
        // When '/' is used, we ignore running backwards into an star-slash, for
        // "[*" command, we just want to find any comment.
        //
        if (initc == '/' || initc == '*')
        {
            comment_dir = dir;
            if (initc == '/')
                ignore_cend = true;
            backwards = (dir == FORWARD) ? false : true;
            initc = NUL;
        }
        else if (initc != '#' && initc != NUL)
        {
            /* 'matchpairs' is "x:y,x:y" */
            //for (ptr = curbuf->b_p_mps; *ptr; ptr += 2)
            String s = G.curbuf.b_p_mps;
            for (ptr = 0; ptr < s.length(); ptr += 2)
            {
                if (s.charAt(ptr) == initc)
                {
                    findc = initc;
                    initc = s.charAt(ptr+2);
                    backwards = true;
                    break;
                }
                ptr += 2;
                if (s.charAt(ptr) == initc)
                {
                    findc = initc;
                    initc = s.charAt(ptr-2);
                    backwards = false;
                    break;
                }
            }
            if (findc == 0)		// invalid initc!
                return null;
        }
        /*
         * Either initc is '#', or no initc was given and we need to look under the
         * cursor.
         */
        else
        {
            if (initc == '#')
            {
                hash_dir = dir;
            }
            else
            {
                //
                // initc was not given, must look for something to match under
                // or near the cursor.
                // Only check for special things when 'cpo' doesn't have '%'.
                //
                if (!cpo_match)
                {
                    // Are we before or at #if, #else etc.?
                    ptr = skipwhite(linep, 0);
                    if (ptr < linep.count -1
                            && linep.charAt(ptr) == '#' && pos_col <= ptr)
                    {
                        ptr = skipwhite(linep, ptr + 1);
                        if (   strncmp(linep, ptr, "if", 2) == 0
                            || strncmp(linep, ptr, "endif", 5) == 0
                            || strncmp(linep, ptr, "el", 2) == 0)
                            hash_dir = 1;
                    }

                    // Are we on a comment?
                    else if (linep.charAt(pos_col) == '/')
                    {
                        if (linep.charAt(pos_col + 1) == '*')
                        {
                            comment_dir = FORWARD;
                            backwards = false;
                            pos_col++;
                        }
                        else if (pos_col > 0 && linep.charAt(pos_col - 1) == '*')
                        {
                            comment_dir = BACKWARD;
                            backwards = true;
                            pos_col--;
                        }
                    }
                    else if (linep.charAt(pos_col) == '*')
                    {
                        if (linep.charAt(pos_col + 1) == '/')
                        {
                            comment_dir = BACKWARD;
                            backwards = true;
                        }
                        else if (pos_col > 0 && linep.charAt(pos_col - 1) == '/')
                        {
                            comment_dir = FORWARD;
                            backwards = false;
                        }
                    }
                }

                //
                // If we are not on a comment or the # at the start of a line,
                // then look for brace anywhere on this line after the cursor.
                //
                if (hash_dir == 0 && comment_dir == 0)
                {
                    //
                    // Find the brace under or after the cursor.
                    // If beyond the end of the line, use the last character in
                    // the line.
                    //
                    if(pos_col >= linep.count - 1 && pos_col != 0)
                        --pos_col;
                    for (;;)
                    {
                        initc = linep.charAt(pos_col);
                        if (initc == NUL)
                            break;

                        ptr = 0;
                        String s = G.curbuf.b_p_mps;
                        for (; ptr < s.length(); ptr += 2)
                        {
                            if (s.charAt(ptr) == initc)
                            {
                                findc = s.charAt(ptr+2);
                                backwards = false;
                                break;
                            }
                            ptr += 2;
                            if (s.charAt(ptr) == initc)
                            {
                                findc = s.charAt(ptr-2);
                                backwards = true;
                                break;
                            }
                        }
                        if (findc != NUL)
                            break;
    //#ifdef FEAT_MBYTE...
                        ++pos_col;
                    }
                    if (findc == 0)
                    {
                        // no brace in the line, maybe use "  #if" then
                        int i00 = skipwhite(linep, 0);
                        if (!cpo_match && linep.charAt(i00) == '#')
                            hash_dir = 1;
                        else
                            return null;
                    }
                    else if (!cpo_bsl)
                    {
                        int col, bslcnt = 0;

                        // Set "match_escaped" if there are an odd number of
                        // backslashes.
                        col = pos_col;
                        while(true) {
                            boolean match = check_prevcol(linep, col, '\\',null);
                            if(!match)
                              break;
                            bslcnt++;
                        }
                        match_escaped = (bslcnt & 1);
                    }
                }
            }
            if (hash_dir != 0)
            {
                //
                // Look for matching #if, #else, #elif, or #endif
                //
                if (oap != null)
                    oap.motion_type = MLINE;   // Linewise for this case only
                if (initc != '#')
                {
                    ptr = skipwhite(linep, skipwhite(linep, 0) + 1);
                    if (strncmp(linep, ptr, "if", 2) == 0
                            || strncmp(linep, ptr, "el", 2) == 0)
                        hash_dir = 1;
                    else if (strncmp(linep, ptr, "endif", 5) == 0)
                        hash_dir = -1;
                    else
                        return null;
                }
                pos_col = 0;
                while(true) // while (!got_int)
                {
                    if (hash_dir > 0)
                    {
                        if (pos_lnum == G.curbuf.getLineCount())
                            break;
                    }
                    else if (pos_lnum == 1)
                        break;
                    pos_lnum += hash_dir;
                    linep = ml_get(pos_lnum);
                    //line_breakcheck();	/* check for CTRL-C typed */
                    ptr = skipwhite(linep, 0);
                    if (linep.charAt(ptr) != '#')
                        continue;
                    pos_col = ptr;
                    ptr = skipwhite(linep, ptr + 1);
                    if (hash_dir > 0)
                    {
                        if (strncmp(linep, ptr, "if", 2) == 0)
                            count++;
                        else if (strncmp(linep, ptr, "el", 2) == 0)
                        {
                            if (count == 0)
                                return new FPOS(pos_lnum, pos_col);
                        }
                        else if (strncmp(linep, ptr, "endif", 5) == 0)
                        {
                            if (count == 0)
                                return new FPOS(pos_lnum, pos_col);
                            count--;
                        }
                    }
                    else
                    {
                        if (strncmp(linep, ptr, "if", 2) == 0)
                        {
                            if (count == 0)
                                return new FPOS(pos_lnum, pos_col);
                            count--;
                        }
                        else if (initc == '#'
                                && strncmp(linep, ptr, "el", 2) == 0)
                        {
                            if (count == 0)
                                return new FPOS(pos_lnum, pos_col);
                        }
                        else if (strncmp(linep, ptr, "endif", 5) == 0)
                            count++;
                    }
                }
                return null;
            }
        }

    //#ifdef FEAT_RIGHTLEFT
    //    /* This is just guessing: when 'rightleft' is set, search for a maching
    //     * paren/brace in the other direction. */
    //    if (curwin->w_p_rl && vim_strchr((char_u *)"()[]{}<>", initc) != NULL)
    //	backwards = !backwards;
    //#endif
    //
        do_quotes = -1;
        start_in_quotes = MAYBE;
        match_pos_lnum = 0; //clearpos(&match_pos); ????????????????
        match_pos_col = 0;  //clearpos(&match_pos); ????????????????

        // backward search: Check if this line contains a single-line comment
        if ((backwards && comment_dir != 0)
    //#ifdef FEAT_LISP...
                )
            comment_col = check_linecomment(linep);
    //#ifdef FEAT_LISP...
        while(true) // while (!got_int)
        {
            //
            // Go to the next position, forward or backward. We could use
            // inc() and dec() here, but that is much slower
            //
            if (backwards)
            {
    //#ifdef FEAT_LISP...
                if (pos_col == 0)	// at start of line, go to prev. one
                {
                    if (pos_lnum == 1)	// start of file
                        break;
                    --pos_lnum;

                    if (maxtravel > 0 && ++traveled > maxtravel)
                        break;

                    linep = ml_get(pos_lnum);
                    pos_col = linep.length() - 1; // pos.col on trailing '\n'
                    do_quotes = -1;
                    //line_breakcheck();

                    // Check if this line contains a single-line comment
                    if (comment_dir != 0
    //#ifdef FEAT_LISP...
                            )
                        comment_col = check_linecomment(linep);
    //#ifdef FEAT_LISP...
                }
                else
                {
                    --pos_col;
    //#ifdef FEAT_MBYTE...
                }
            }
            else				// forward search
            {
                if (pos_col + 1 >= linep.length()
                        // at end of line, go to next one
    //#ifdef FEAT_LISP...
                        )
                {
                    if (pos_lnum == G.curbuf.getLineCount()  // end of file
    //#ifdef FEAT_LISP...
                             )
                        break;
                    ++pos_lnum;

                    if (maxtravel != 0 && traveled++ > maxtravel)
                        break;

                    linep = ml_get(pos_lnum);
                    pos_col = 0;
                    do_quotes = -1;
                    //line_breakcheck();
    //#ifdef FEAT_LISP...
                }
                else
                {
    //#ifdef FEAT_MBYTE...
                        ++pos_col;
                }
            }

            //
            // If FM_BLOCKSTOP given, stop at a '{' or '}' in column 0.
            //
            if (pos_col == 0 && (flags & FM_BLOCKSTOP) != 0
                     && ((c = linep.charAt(0)) == '{' || c == '}'))
            {
                if (c == findc && count == 0) {	// match!
                    return new FPOS(pos_lnum, pos_col);
                }
                break;					/* out of scope */
            }

            if (comment_dir != 0)
            {
                /* Note: comments do not nest, and we ignore quotes in them */
                /* TODO: ignore comment brackets inside strings */
                if (comment_dir == FORWARD)
                {
                    if (linep.charAt(pos_col) == '*'
                            && linep.charAt(pos_col + 1) == '/')
                    {
                        pos_col++;
                        return new FPOS(pos_lnum, pos_col);
                    }
                }
                else    /* Searching backwards */
                {
                    /*
                     * A comment may contain / * or / /, it may also start or end
                     * with / * /.	Ignore a / * after / /.
                     */
                    if (pos_col == 0)
                        continue;
                    else if (  linep.charAt(pos_col - 1) == '/'
                            && linep.charAt(pos_col) == '*'
                            && pos_col < comment_col)
                    {
                        count++;
                        match_pos_lnum = pos_lnum;
                        match_pos_col = pos_col;
                        match_pos_col--;
                    }
                    else if (linep.charAt(pos_col - 1) == '*'
                            && linep.charAt(pos_col) == '/')
                    {
                        if (count > 0) {
                            pos_lnum = match_pos_lnum;
                            pos_col = match_pos_col;
                        }else if (pos_col > 1 && linep.charAt(pos_col - 2) == '/'
                                                   && pos_col <= comment_col)
                            pos_col -= 2;
                        else if (ignore_cend)
                            continue;
                        else
                            return null;
                        return new FPOS(pos_lnum, pos_col);
                    }
                }
                continue;
            }

            //
            // If smart matching ('cpoptions' does not contain '%'), braces inside
            // of quotes are ignored, but only if there is an even number of
            // quotes in the line.
            //
            if (cpo_match)
                do_quotes = 0;
            else if (do_quotes == -1)
            {
                //
                // Count the number of quotes in the line, skipping \" and '"'.
                // Watch out for "\\".
                //
                at_start = do_quotes;
                // '-1' in following 'cause of newline
                for (ptr = 0; ptr < linep.count - 1; ++ptr)
                {
                    if (ptr == pos_col + (backwards ? 1 : 0))
                        at_start = (do_quotes & 1);
                    if (linep.charAt(ptr) == '"'
                            && (ptr == 0
                                || linep.charAt(ptr-1) != '\''
                                || linep.charAt(ptr + 1) != '\''))
                        ++do_quotes;
                    if (linep.charAt(ptr) == '\\' && ptr + 1 < linep.count)
                        ++ptr;
                }
                do_quotes &= 1;	// result is 1 with even number of quotes

                //
                // If we find an uneven count, check current line and previous
                // one for a '\' at the end.
                //
                if (do_quotes == 0)
                {
                    inquote = false;
                    if (linep.charAt(ptr-1) == '\\')
                    {
                        do_quotes = 1;
                        if (start_in_quotes == MAYBE)
                        {
                            /* Do we need to use at_start here? */
                            inquote = true;
                            start_in_quotes = TRUE;
                        }
                        else if (backwards)
                            inquote = true;
                    }
                    if (pos_lnum > 1)
                    {
                        MySegment seg = ml_get(pos_lnum - 1);
                        // checking last char of previous line
                        if (seg.charAt(0) != '\n'
                                && seg.charAt(seg.count - 2) == '\\')
                        {
                            do_quotes = 1;
                            if (start_in_quotes == MAYBE)
                            {
                                inquote = at_start != 0;
                                if (inquote)
                                    start_in_quotes = TRUE;
                            }
                            else if (!backwards)
                                inquote = true;
                        }
                    }
                }
            }
            if (start_in_quotes == MAYBE)
                start_in_quotes = FALSE;

            //
            // If 'smartmatch' is set:
            //   Things inside quotes are ignored by setting 'inquote'.  If we
            //   find a quote without a preceding '\' invert 'inquote'.  At the
            //   end of a line not ending in '\' we reset 'inquote'.
            //
            //   In lines with an uneven number of quotes (without preceding '\')
            //   we do not know which part to ignore. Therefore we only set
            //   inquote if the number of quotes in a line is even, unless this
            //   line or the previous one ends in a '\'.  Complicated, isn't it?
            //

            switch (c = linep.charAt(pos_col))
            {
            case NUL:
                /* at end of line without trailing backslash, reset inquote */
                if (pos_col == 0 || linep.array[linep.offset + pos_col - 1] != '\\')
                {
                    inquote = false;
                    start_in_quotes = FALSE;
                }
                break;

            case '"':
                // a quote that is preceded with an odd number of backslashes is
                // ignored
                if (do_quotes != 0)
                {
                    int col;

                    for (col = pos_col - 1; col >= 0; --col)
                        if (linep.charAt(col) != '\\')
                            break;
                    if (((pos_col - 1 - col) & 1) == 0)
                    {
                        inquote = !inquote;
                        start_in_quotes = FALSE;
                    }
                }
                break;

            //
            // If smart matching ('cpoptions' does not contain '%'):
            //   Skip things in single quotes: 'x' or '\x'.  Be careful for single
            //   single quotes, eg jon's.  Things like '\233' or '\x3f' are not
            //   skipped, there is never a brace in them.
            //   Ignore this when finding matches for `'.
            //
            case '\'':
                if (!cpo_match && initc != '\'' && findc != '\'')
                {
                    if (backwards)
                    {
                        if (pos_col > 1)
                        {
                            if (linep.charAt(pos_col - 2) == '\'')
                            {
                                pos_col -= 2;
                                break;
                            }
                            else if (linep.charAt(pos_col - 2) == '\\'
                                     && pos_col > 2
                                     && linep.charAt(pos_col - 3) == '\'')
                            {
                                pos_col -= 3;
                                break;
                            }
                        }
                    }
                    // jVi: FOLLOWING was +1
                    else if (pos_col + 2 < linep.length())	// forward search
                    {
                        if (linep.charAt(pos_col + 1) == '\\'
                                // ORIGINAL: && pos_col + 2 < linep.length()
                                && pos_col + 3 < linep.length()
                                && linep.charAt(pos_col + 3) == '\'')
                        {
                            pos_col += 3;
                            break;
                        }
                        else if (linep.charAt(pos_col + 2) == '\'')
                        {
                            pos_col += 2;
                            break;
                        }
                    }
                }
                /* FALLTHROUGH */

            default:
    //#ifdef FEAT_LISP...

                // Check for match outside of quotes, and inside of
                // quotes when the start is also inside of quotes.
                if ((!inquote || start_in_quotes == TRUE)
                        && (c == initc || c == findc))
                {
                    int	col, bslcnt = 0;

                    if (!cpo_bsl)
                    {
                        col = pos_col;
                        while(true) {
                            boolean match = check_prevcol(linep, col, '\\', null);
                            if(!match)
                              break;
                            bslcnt++;
                            col--;
                        }
                    }
                    // Only accept a match when 'M' is in 'cpo' or when ecaping is
                    // what we expect.
                    if (cpo_bsl || (bslcnt & 1) == match_escaped)
                    {
                        if (c == initc)
                            count++;
                        else
                        {
                            if (count == 0) {
                                return new FPOS(pos_lnum, pos_col);
                            }
                            count--;
                        }
                    }
                }
            }
        }

        if (comment_dir == BACKWARD && count > 0)
        {
            return new FPOS(match_pos_lnum, match_pos_col);
        }
        return null;	// never found it
    }
}
