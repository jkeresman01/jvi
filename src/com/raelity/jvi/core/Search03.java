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
import com.raelity.jvi.lib.MutableInt;
import com.raelity.text.TextUtil.MySegment;

import static com.raelity.jvi.core.Eval.do_searchpair;
import static com.raelity.jvi.core.MarkOps.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Search.*;
import static com.raelity.jvi.core.Search01.*;
import static com.raelity.jvi.core.Search02.*;
import static com.raelity.jvi.core.Search03.*;
import static com.raelity.jvi.core.Util.*;
import static com.raelity.jvi.core.lib.Constants.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Search03
{

    private Search03() { }

    /**
     * Check if line[] contains a / / comment.
     * Return MAXCOL if not, otherwise return the column.
     * TODO: skip strings.
     */
    static int check_linecomment(MySegment seg)
    {
        int p = 0;
    //#ifdef FEAT_LISP...
        while ((p = vim_strchr(seg, p, '/')) >= 0)
        {
            if (seg.charAt(p + 1) == '/')
                break;
            ++p;
        }

        if (p < 0)
            return MAXCOL;
        return p;
    }

    //===========================================================================


    /**
     * Go back to the start of the word or the start of white space
     */
    static void back_in_line()
    {
        int		sclass;		    /* starting class */

        sclass = cls();
        for (;;)
        {
            if (G.curwin.w_cursor.getColumn() == 0)	    // stop at start of line
                break;
            dec_cursor();
            if (cls() != sclass)		    /* stop at start of word */
            {
                inc_cursorV7();
                break;
            }
        }
    }

    static void
    find_first_blank(ViFPOS posp)
    {
        char    c;

        while (decl(posp) != -1)
        {
            c = gchar_pos(posp);
            if (!vim_iswhite(c))
            {
                inclV7(posp);
                break;
            }
        }
    }

    /**
     * Skip count/2 sentences and count/2 separating white spaces.
     */
    static void
    findsent_forward(int count, boolean at_start_sent)
        //int	    at_start_sent;	/* cursor is at start of sentence */
    {
        while (count-- != 0)
        {
            findsent(FORWARD, 1);
            if (at_start_sent)
                find_first_blank(G.curwin.w_cursor);
            if (count == 0 || at_start_sent)
                decl(G.curwin.w_cursor);
            at_start_sent = !at_start_sent;
        }
    }

    /**
     * Find word under cursor, cursor at end.
     * Used while an operator is pending, and in Visual mode.
     */
    static int current_word(OPARG oap, int count, boolean include, boolean bigword)
    //    oparg_T	*oap;
    //    long	count;
    //    int		include;	/* TRUE: include word and white space */
    //    int		bigword;	/* FALSE == word, TRUE == WORD */
    {
        ViFPOS	start_pos;
        ViFPOS	pos;
        boolean	inclusive = true;
        boolean	include_white = false;

        funnyCharsAsWord = bigword; // cls_bigword = bigword;
        start_pos = null; // clearpos(&start_pos);

        /* Correct cursor when 'selection' is exclusive */
        if (G.VIsual_active && G.p_sel.charAt(0) == 'e'
                && G.VIsual.compareTo(G.curwin.w_cursor) < 0)
            Misc.dec_cursor();

        //
        // When Visual mode is not active, or when the VIsual area is only one
        // character, select the word and/or white space under the cursor.
        //
        if (!G.VIsual_active || G.curwin.w_cursor.equals(G.VIsual))
        {
            /*
             * Go to start of current word or white space.
             */
            back_in_line();
            start_pos = G.curwin.w_cursor.copy();

            //
            // If the start is on white space, and white space should be included
            // ("	word"), or start is not on white space, and white space should
            // not be included ("word"), find end of word.
            //
            if ((cls() == 0) == include)
            {
                if (end_word(1, bigword, true, true) == FAIL)
                    return FAIL;
            }
            else
            {
                //
                // If the start is not on white space, and white space should be
                // included ("word	 "), or start is on white space and white
                // space should not be included ("	 "), find start of word.
                // If we end up in the first column of the next line (single char
                // word) back up to end of the line.
                //
                fwd_word(1, bigword, true);
                if (G.curwin.w_cursor.getColumn() == 0)
                    Misc.decl(G.curwin.w_cursor);
                else
                    Edit.oneleft();

                if (include)
                    include_white = true;
            }

            if (G.VIsual_active)
            {
                /* should do something when inclusive == FALSE ! */
                G.VIsual = start_pos;
                // redraw_curbuf_later(INVERTED);	/* update the inversion */
                Normal.v_updateVisualState();
            }
            else
            {
                oap.start = start_pos;
                oap.motion_type = MCHAR;
            }
            --count;
        }

        //
        // When count is still > 0, extend with more objects.
        //
        while (count > 0)
        {
            inclusive = true;
            if (G.VIsual_active && G.curwin.w_cursor.compareTo(G.VIsual) < 0)
            {
                //
                // In Visual mode, with cursor at start: move cursor back.
                //
                if (Misc.decl(G.curwin.w_cursor) == -1)
                    return FAIL;
                if (include != (cls() != 0))
                {
                    if (bck_word(1, bigword, true) == FAIL)
                        return FAIL;
                }
                else
                {
                    if (bckend_word(1, bigword, true) == FAIL)
                        return FAIL;
                    Misc.inclV7(G.curwin.w_cursor);
                }
            }
            else
            {
                //
                // Move cursor forward one word and/or white area.
                //
                if (Misc.inclV7(G.curwin.w_cursor) == -1)
                    return FAIL;
                if (include != (cls() == 0))
                {
                    if (fwd_word(1, bigword, true) == FAIL && count > 1)
                        return FAIL;
                    //
                    // If end is just past a new-line, we don't want to include
                    // the first character on the line.
                    // Put cursor on last char of white.
                    //
                    if (Edit.oneleft() == FAIL)
                        inclusive = false;
                }
                else
                {
                    if (end_word(1, bigword, true, true) == FAIL)
                        return FAIL;
                }
            }
            --count;
        }

        if (include_white && (cls() != 0
                     || (G.curwin.w_cursor.getColumn() == 0 && !inclusive)))
        {
            //
            // If we don't include white space at the end, move the start
            // to include some white space there. This makes "daw" work
            // better on the last word in a sentence (and "2daw" on last-but-one
            // word).  Also when "2daw" deletes "word." at the end of the line
            // (cursor is at start of next line).
            // But don't delete white space at start of line (indent).
            //
            pos = G.curwin.w_cursor.copy();	// save cursor position
            G.curwin.w_cursor.set(start_pos);
            if (Edit.oneleft() == OK)
            {
                back_in_line();
                if (cls() == 0 && G.curwin.w_cursor.getColumn() > 0)
                {
                    if (G.VIsual_active)
                        G.VIsual = G.curwin.w_cursor.copy();
                    else
                        oap.start = G.curwin.w_cursor.copy();
                }
            }
            G.curwin.w_cursor.set(pos);	// put cursor back at end
        }

        if (G.VIsual_active)
        {
            if (G.p_sel.charAt(0) == 'e'
                    && inclusive
                    && G.VIsual.compareTo(G.curwin.w_cursor) <= 0)
                inc_cursorV7();
            if (G.VIsual_mode == 'V')
            {
                G.VIsual_mode = 'v';
                //redraw_cmdline = TRUE;		/* show mode later */
            }
            Normal.v_updateVisualState();
        }
        else
            oap.inclusive = inclusive;

        return OK;
    }

    /**
     * Find sentence(s) under the cursor, cursor at end.
     * When Visual active, extend it by one or more sentences.
     */
    static int
    current_sent(OPARG oap, int count, boolean include)
    {
        ViFPOS	start_pos;
        ViFPOS	pos;
        boolean	start_blank;
        char	c;
        boolean	at_start_sent;
        int		ncount;
        boolean	extending = false;

        start_pos = G.curwin.w_cursor.copy();
        pos = start_pos.copy();
        findsent(FORWARD, 1);	// Find start of next sentence.

        //
        // When visual area is bigger than one character: Extend it.
        //

extend:
while(true) {
        if (extending || G.VIsual_active && !start_pos.equals(G.VIsual))
        {
//extend: ORIGINAL TARGET OF GOTO
            if (start_pos.compareTo(G.VIsual) < 0)
            {
                /*
                 * Cursor at start of Visual area.
                 * Find out where we are:
                 * - in the white space before a sentence
                 * - in a sentence or just after it
                 * - at the start of a sentence
                 */
                at_start_sent = true;
                decl(pos);
                while (pos.compareTo(G.curwin.w_cursor) < 0)
                {
                    c = gchar_pos(pos);
                    if (!vim_iswhite(c))
                    {
                        at_start_sent = false;
                        break;
                    }
                    inclV7(pos);
                }
                if (!at_start_sent)
                {
                    findsent(BACKWARD, 1);
                    if (equalpos(G.curwin.w_cursor, start_pos))
                        at_start_sent = true;  /* exactly at start of sentence */
                    else
                        /* inside a sentence, go to its end (start of next) */
                        findsent(FORWARD, 1);
                }
                if (include)	/* "as" gets twice as much as "is" */
                    count *= 2;
                while (count-- != 0)
                {
                    if (at_start_sent)
                        find_first_blank(G.curwin.w_cursor);
                    c = gchar_cursor();
                    if (!at_start_sent || (!include && !vim_iswhite(c)))
                        findsent(BACKWARD, 1);
                    at_start_sent = !at_start_sent;
                }
            }
            else
            {
                /*
                 * Cursor at end of Visual area.
                 * Find out where we are:
                 * - just before a sentence
                 * - just before or in the white space before a sentence
                 * - in a sentence
                 */
                inclV7(pos);
                at_start_sent = true;
                if (!equalpos(pos, G.curwin.w_cursor)) /* not just before a sentence */
                {
                    at_start_sent = false;
                    while (lt(pos, G.curwin.w_cursor))
                    {
                        c = gchar_pos(pos);
                        if (!vim_iswhite(c))
                        {
                            at_start_sent = true;
                            break;
                        }
                        inclV7(pos);
                    }
                    if (at_start_sent)	/* in the sentence */
                        findsent(BACKWARD, 1);
                    else		/* in/before white before a sentence */
                        G.curwin.w_cursor.set(start_pos);
                }

                if (include)	/* "as" gets twice as much as "is" */
                    count *= 2;
                findsent_forward(count, at_start_sent);
                if (G.p_sel.charAt(0) == 'e')
                    G.curwin.w_cursor.incColumn();
            }
            return OK;
        }

        /*
         * If cursor started on blank, check if it is just before the start of the
         * next sentence.
         */
        while (vim_iswhite(gchar_pos(pos)))	/* vim_iswhite() is a macro */
            inclV7(pos);
        if (equalpos(pos, G.curwin.w_cursor))
        {
            start_blank = true;
            find_first_blank(start_pos);	/* go back to first blank */
        }
        else
        {
            start_blank = false;
            findsent(BACKWARD, 1);
            start_pos = G.curwin.w_cursor.copy();
        }
        if (include)
            ncount = count * 2;
        else
        {
            ncount = count;
            if (start_blank)
                --ncount;
        }
        if (ncount > 0)
            findsent_forward(ncount, true);
        else
            decl(G.curwin.w_cursor);

        if (include)
        {
            /*
             * If the blank in front of the sentence is included, exclude the
             * blanks at the end of the sentence, go back to the first blank.
             * If there are no trailing blanks, try to include leading blanks.
             */
            if (start_blank)
            {
                find_first_blank(G.curwin.w_cursor);
                c = gchar_pos(G.curwin.w_cursor);	/* vim_iswhite() is a macro */
                if (vim_iswhite(c))
                    decl(G.curwin.w_cursor);
            }
            else if (!vim_iswhite(gchar_cursor()))
                find_first_blank(start_pos);
        }

        if (G.VIsual_active)
        {
            /* avoid getting stuck with "is" on a single space before a sent. */
            if (equalpos(start_pos, G.curwin.w_cursor)) {
                extending = true; // part of goto extend workaround
                continue extend;  // was goto extend
            }
            if (G.p_sel.charAt(0) == 'e')
                G.curwin.w_cursor.incColumn();
            G.VIsual = start_pos;
            G.VIsual_mode = 'v';
            //redraw_curbuf_later(INVERTED);	/* update the inversion */
            Normal.v_updateVisualState();
        }
        else
        {
            /* include a newline after the sentence, if there is one */
            if (inclV7(G.curwin.w_cursor) == -1)
                oap.inclusive = true;
            else
                oap.inclusive = false;
            oap.start = start_pos;
            oap.motion_type = MCHAR;
        }

break; // part of implementing goto extend
}
        return OK;
    }

    /*
     * Find block under the cursor, cursor at end.
     * "what" and "other" are two matching parenthesis/paren/etc.
     */
    static int
    current_block(OPARG oap, int count, boolean include, char what, char other)
        // int		include;	/* TRUE == include white space */
        // int		what;		/* '(', '{', etc. */
        // int		other;		/* ')', '}', etc. */
    {
        ViFPOS	old_pos;
        ViFPOS	pos = null; //*pos = NULL;
        ViFPOS	start_pos = null;
        ViFPOS	end_pos; //*end_pos;
        ViFPOS	old_start, old_end;
        String	save_cpo;
        boolean	sol = false;		// '{' at start of line

        old_pos = G.curwin.w_cursor.copy();
        old_end = G.curwin.w_cursor.copy();		/* remember where we started */
        old_start = old_end;

        //
        // If we start on '(', '{', ')', '}', etc., use the whole block inclusive.
        //
        if (!G.VIsual_active || G.VIsual.equals(G.curwin.w_cursor))
        {
            MarkOps.setpcmark();
            if (what == '{')		/* ignore indent */
                while (Misc.inindent(1))
                    if (inc_cursorV7() != 0)
                        break;
            if (Misc.gchar_cursor() == what)
                /* cursor on '(' or '{', move cursor just after it */
                G.curwin.w_cursor.incColumn();
        }
        else if (G.VIsual.compareTo(G.curwin.w_cursor) < 0)
        {
            old_start = G.VIsual.copy();
            G.curwin.w_cursor.set(G.VIsual);  // cursor at low end of Visual
        }
        else
            old_end = G.VIsual.copy();

        /*
         * Search backwards for unclosed '(', '{', etc..
         * Put this position in start_pos.
         * Ignore quotes here.
         */
        save_cpo = G.p_cpo;
        G.p_cpo = "%";
        while (count-- > 0)
        {
            if ((pos = findmatch(null, what)) == null)
                break;
            G.curwin.w_cursor.set(pos);
            start_pos = pos.copy(); // the findmatch for end_pos will overwrite *pos
        }
        G.p_cpo = save_cpo;

        //
        // Search for matching ')', '}', etc.
        // Put this position in curwin->w_cursor.
        //
        if (pos == null || (end_pos = findmatch(null, other)) == null)
        {
            G.curwin.w_cursor.set(old_pos);
            return FAIL;
        }
        G.curwin.w_cursor.set(end_pos);

        //
        // Try to exclude the '(', '{', ')', '}', etc. when "include" is FALSE.
        // If the ending '}' is only preceded by indent, skip that indent.
        // But only if the resulting area is not smaller than what we started with.
        //
        while (!include)
        {
            Misc.inclV7(start_pos);
            sol = (G.curwin.w_cursor.getColumn() == 0);
            Misc.decl(G.curwin.w_cursor);
            if (what == '{')
                while (Misc.inindent(1))
                {
                    sol = true;
                    if (Misc.decl(G.curwin.w_cursor) != 0)
                        break;
                }
            //
            // In Visual mode, when the resulting area is not bigger than what we
            // started with, extend it to the next block, and then exclude again.
            //
            if (!(start_pos.compareTo(old_start) < 0)                 // !lt
                    && !(old_end.compareTo(G.curwin.w_cursor) < 0)    // !lt
                    && G.VIsual_active)
            {
                G.curwin.w_cursor.set(old_start);
                Misc.decl(G.curwin.w_cursor);
                if ((pos = findmatch(null, what)) == null)
                {
                    G.curwin.w_cursor.set(old_pos);
                    return FAIL;
                }
                start_pos.set(pos);
                G.curwin.w_cursor.set(pos);
                if ((end_pos = findmatch(null, other)) == null)
                {
                    G.curwin.w_cursor.set(old_pos);
                    return FAIL;
                }
                G.curwin.w_cursor.set(end_pos);
            }
            else
                break;
        }

        if (G.VIsual_active)
        {
            if (G.p_sel.charAt(0) == 'e')
                G.curwin.w_cursor.incColumn();
            if (sol && Misc.gchar_cursor() != NUL)
                Misc.inc(G.curwin.w_cursor);	// include the line break
            G.VIsual.set(start_pos);
            G.VIsual_mode = 'v';
            // redraw_curbuf_later(INVERTED);	/* update the inversion */
            // showmode();
            Normal.v_updateVisualState();
        }
        else
        {
            oap.start = start_pos;
            oap.motion_type = MCHAR;
            if (sol)
            {
                Misc.inclV7(G.curwin.w_cursor);
                oap.inclusive = false;
            }
            else
                oap.inclusive = true;
        }

        return OK;
    }

    /**
     * Return true if the cursor is on a "<aaa>" tag.  Ignore "<aaa/>".
     * When "end_tag" is true return true if the cursor is on "</aaa>".
     */
    static boolean
    in_html_tag(boolean end_tag)
    {
        MySegment	line = ml_get_curline();
        int		p;
        char	c;
        int		lc = NUL;
        ViFPOS	pos;

    // #ifdef FEAT_MBYTE
    //     if (enc_dbcs)
    //     {
    // 	char_u	*lp = null;
    //
    // 	/* We search forward until the cursor, because searching backwards is
    // 	 * very slow for DBCS encodings. */
    // 	for (p = line; p < line + G.curwin.w_cursor.getColumn(); mb_ptr_adv(p))
    // 	    if (line.charAt(p) == '>' || line.charAt(p) == '<')
    // 	    {
    // 		lc = line.charAt(p);
    // 		lp = p;
    // 	    }
    // 	if (line.charAt(p) != '<')	    /* check for '<' under cursor */
    // 	{
    // 	    if (lc != '<')
    // 		return false;
    // 	    p = lp;
    // 	}
    //     }
    //     else
    // #endif
        {
            //for (p = line + G.curwin.w_cursor.getColumn(); p > line; )
            for (p =  G.curwin.w_cursor.getColumn(); p > 0; )
            {
                if (line.charAt(p) == '<')	/* find '<' under/before cursor */
                    break;
                --p; //mb_ptr_back(line, p);
                if (line.charAt(p) == '>')	/* find '>' before cursor */
                    break;
            }
            if (line.charAt(p) != '<')
                return false;
        }

        pos = G.curwin.w_cursor.copy();
        pos.set(G.curwin.w_cursor.getLine(), p); // (..., p - line)

        p++; //mb_ptr_adv(p);
        if (end_tag)
            /* check that there is a '/' after the '<' */
            return line.charAt(p) == '/';

        /* check that there is no '/' after the '<' */
        if (line.charAt(p) == '/')
            return false;

        /* check that the matching '>' is not preceded by '/' */
        for (;;)
        {
            if (incV7(pos) < 0)
                return false;
            c = ml_get_pos(pos).current();
            if (c == '>')
                break;
            lc = c;
        }
        return lc != '/';
    }

    /**
     * Find tag block under the cursor, cursor at end.
     * @param include true == include white space
     */
    static int
    current_tagblock(OPARG oap, int count_arg, boolean include)
        /* true == include white space */
    {
        int		count = count_arg;
        int		n;
        ViFPOS	old_pos;
        ViFPOS	start_pos;
        ViFPOS	end_pos;
        ViFPOS	old_start, old_end;
        String	spat, epat;
        MySegment   line;
        int		p;
        int		cp;
        int		len;
        int		r;
        boolean	do_include = include;
        boolean	save_p_ws = G.p_ws;
        int		retval = FAIL;

        G.p_wsOption_set(false);

        old_pos = G.curwin.w_cursor.copy();
        old_end = G.curwin.w_cursor.copy();		    /* remember where we started */
        old_start = old_end.copy();
        //System.err.println("html: init cursor " + old_pos);

        /*
         * If we start on "<aaa>" select that block.
         */
        if (!G.VIsual_active || equalpos(G.VIsual, G.curwin.w_cursor))
        {
            setpcmark();

            /* ignore indent */
            while (inindent(1))
                if (inc_cursorV7() != 0)
                    break;

            if (in_html_tag(false))
            {
                /* cursor on start tag, move to just after it */
                while (ml_get_cursor().current() != '>')
                    if (inc_cursorV7() < 0)
                        break;
                //System.err.println("html: in start tag " + G.curwin.w_cursor);
            }
            else if (in_html_tag(true))
            {
                /* cursor on end tag, move to just before it */
                while (ml_get_cursor().current() != '<')
                    if (dec_cursor() < 0)
                        break;
                dec_cursor();
                old_end = G.curwin.w_cursor.copy();
                //System.err.println("html: in end tag " + G.curwin.w_cursor);
            }
        }
        else if (lt(G.VIsual, G.curwin.w_cursor))
        {
            old_start = G.VIsual.copy();
            G.curwin.w_cursor.set(G.VIsual);	    /* cursor at low end of Visual */
        }
        else
            old_end = G.VIsual.copy();

        /*
         * Search backwards for unclosed "<aaa>".
         * Put this position in start_pos.
         */
again:
while(true) {
        for (n = 0; n < count; ++n)
        {
                      //"<[^ \t>/!]\\+\\%(\\_s\\_[^>]\\{-}[^/]>\\|$\\|\\_s\\=>\\)",
                      //"<[^ \t>/!]+(?:[\\n\\s][\\n[^>]]*?[^/]>|$|[\\n\\s]?>)",
                      //"<[^ \t>/!]+(?:\\s[^>]*?[^/]>|$|\\s?>)",
            if (do_searchpair(
                        "<[^ \t>/!]+(?:[\\n\\s][\\n[^>]]*?[^/]>|$|[\\n\\s]?>)",
                        "",
                        "</[^>]*>", BACKWARD, "", SEARCH_ISCLEAN, null, 0) <= 0)
            {
                G.curwin.w_cursor.set(old_pos);
                break again; //goto theend;
            }
        }
        start_pos = G.curwin.w_cursor.copy();
        //System.err.println("html: find count " + G.curwin.w_cursor);

        /*
         * Search for matching "</aaa>".  First isolate the "aaa".
         */
        inc_cursorV7();
        line = (MySegment)ml_get_cursor(); // p = ml_get_cursor();
        p = line.getIndex() - line.getBeginIndex();
        //for (cp = p; *cp != NUL && *cp != '>' && !vim_iswhite(*cp); mb_ptr_adv(cp))
        for (cp = p;
             line.charAt(cp) != '\n'
                  && cp < line.length() // this is defensive, not needed
                  && line.charAt(cp) != '>'
                  && !vim_iswhite(line.charAt(cp));
             //mb_ptr_adv(cp)
        )
            ++cp;
        len = (cp - p);
        if (len == 0)
        {
            G.curwin.w_cursor.set(old_pos);
            break again; //goto theend;
        }
        // NOTE: IGNORE CASE
        //sprintf((char *)spat, "<%.*s\\%%(\\_[^>]\\{-}[^/]>\\|>\\)\\c", len, p);
        //                "<%s(?:[\\n[^>]]*?[^/]>|>)\\c",
        //                   "<%s(?:[^>]*?[^/]>|>)\\c",
        spat = String.format(
                             "<%s(?:[^>]*?[^/]>|>)\\c",
                             line.subSequence(p, cp).toString());
        //sprintf((char *)epat, "</%.*s>\\c", len, p);
        epat = String.format("</%s>\\c",
                             line.subSequence(p, cp).toString());

        r = do_searchpair(spat, "", epat, FORWARD, "", SEARCH_ISCLEAN, null, 0);
        //System.err.format("html: after searchpair r: %d\n    spat '%s' epat '%s'\n", r, spat, epat);

        if (r < 1 || lt(G.curwin.w_cursor, old_end))
        {
            /* Can't find other end or it's before the previous end.  Could be a
             * HTML tag that doesn't have a matching end.  Search backwards for
             * another starting tag. */
            count = 1;
            G.curwin.w_cursor.set(start_pos);
            continue again;
        }

        if (do_include || r < 1)
        {
            /* Include up to the '>'. */
            while (ml_get_cursor().current() != '>')
                if (inc_cursorV7() < 0)
                    break;
        }
        else
        {
            /* Exclude the '<' of the end tag. */
            if (ml_get_cursor().current() == '<')
                dec_cursor();
        }
        end_pos = G.curwin.w_cursor.copy();

        if (!do_include)
        {
            /* Exclude the start tag. */
            G.curwin.w_cursor.set(start_pos);
            while (inc_cursorV7() >= 0)
                if (ml_get_cursor().current() == '>'
                      && lt(G.curwin.w_cursor, end_pos))
                {
                    inc_cursorV7();
                    start_pos = G.curwin.w_cursor.copy();
                    break;
                }
            G.curwin.w_cursor.set(end_pos);

            /* If we now have the same text as before reset "do_include" and try
             * again. */
            if (equalpos(start_pos, old_start) && equalpos(end_pos, old_end))
            {
                do_include = true;
                G.curwin.w_cursor.set(old_start);
                count = count_arg;
                continue again;
            }
        }

        if (G.VIsual_active)
        {
            if (G.p_sel.charAt(0) == 'e')
                G.curwin.w_cursor.incColumn();
            G.VIsual = start_pos.copy();
            G.VIsual_mode = 'v';
            // redraw_curbuf_later(INVERTED);	/* update the inversion */
            Normal.v_updateVisualState();
            // showmode();
        }
        else
        {
            oap.start = start_pos;
            oap.motion_type = MCHAR;
            oap.inclusive = true;
        }
        retval = OK;
break;
}  // theend: LABEL

        G.p_wsOption_set(save_p_ws);
        return retval;
    }

    static int
    current_par(OPARG oap, int count, boolean include, char type)
        // int		include;	/* TRUE == include white space */
        // int		type;		/* 'p' for paragraph, 'S' for section */
    {
        int		start_lnum;
        int		end_lnum;
        boolean	white_in_front;
        int		dir;
        int		start_is_white;
        int		prev_start_is_white;
        int		retval = OK;
        boolean	do_white = false;
        int		t;
        int		i;
        boolean	extending = false;

        if (type == 'S')	    /* not implemented yet */
            return FAIL;

        start_lnum = G.curwin.w_cursor.getLine();

        /*
         * When visual area is more than one line: extend it.
         */
    extend: do {
        if (extending || G.VIsual_active && start_lnum != G.VIsual.getLine())
        {
    //extend:
            if (start_lnum < G.VIsual.getLine())
                dir = BACKWARD;
            else
                dir = FORWARD;
            for (i = count; --i >= 0; )
            {
                if (start_lnum ==
                               (dir == BACKWARD ? 1 : G.curbuf.getLineCount()))
                {
                    retval = FAIL;
                    break;
                }

                prev_start_is_white = -1;
                for (t = 0; t < 2; ++t)
                {
                    start_lnum += dir;
                    start_is_white = linewhite(start_lnum) ? 1 : 0;
                    if (prev_start_is_white == start_is_white)
                    {
                        start_lnum -= dir;
                        break;
                    }
                    for (;;)
                    {
                        if (start_lnum == (dir == BACKWARD
                                                ? 1 : G.curbuf.getLineCount()))
                            break;
                        if (start_is_white != (linewhite(start_lnum + dir) ? 1 : 0)
                                || (start_is_white == 0
                                        && startPS(start_lnum
                                                   + (dir > 0 ? 1 : 0), 0, false)))
                            break;
                        start_lnum += dir;
                    }
                    if (!include)
                        break;
                    if (start_lnum == (dir == BACKWARD
                                                ? 1 : G.curbuf.getLineCount()))
                        break;
                    prev_start_is_white = start_is_white;
                }
            }
            G.curwin.w_cursor.setLine(start_lnum);
            G.curwin.w_cursor.setColumn(0);
            return retval;
        }

        /*
         * First move back to the start_lnum of the paragraph or white lines
         */
        white_in_front = linewhite(start_lnum);
        while (start_lnum > 1)
        {
            if (white_in_front)	    /* stop at first white line */
            {
                if (!linewhite(start_lnum - 1))
                    break;
            }
            else		/* stop at first non-white line of start of paragraph */
            {
                if (linewhite(start_lnum - 1) || startPS(start_lnum, 0, false))
                    break;
            }
            --start_lnum;
        }

        /*
         * Move past the end of any white lines.
         */
        end_lnum = start_lnum;
        while (end_lnum <= G.curbuf.getLineCount() && linewhite(end_lnum))
            ++end_lnum;

        --end_lnum;
        i = count;
        if (!include && white_in_front)
            --i;
        while (i-- != 0)
        {
            if (end_lnum == G.curbuf.getLineCount())
                return FAIL;

            if (!include)
                do_white = linewhite(end_lnum + 1);

            if (include || !do_white)
            {
                ++end_lnum;
                /*
                 * skip to end of paragraph
                 */
                while (end_lnum < G.curbuf.getLineCount()
                        && !linewhite(end_lnum + 1)
                        && !startPS(end_lnum + 1, 0, false))
                    ++end_lnum;
            }

            if (i == 0 && white_in_front && include)
                break;

            /*
             * skip to end of white lines after paragraph
             */
            if (include || do_white)
                while (end_lnum < G.curbuf.getLineCount()
                                                       && linewhite(end_lnum + 1))
                    ++end_lnum;
        }

        /*
         * If there are no empty lines at the end, try to find some empty lines at
         * the start (unless that has been done already).
         */
        if (!white_in_front && !linewhite(end_lnum) && include)
            while (start_lnum > 1 && linewhite(start_lnum - 1))
                --start_lnum;

        if (G.VIsual_active)
        {
            /* Problem: when doing "Vipipip" nothing happens in a single white
             * line, we get stuck there.  Trap this here. */
            if (G.VIsual_mode == 'V' && start_lnum == G.curwin.w_cursor.getLine())
                continue extend;
            G.VIsual.setLine(start_lnum);
            G.VIsual_mode = 'V';
            //redraw_curbuf_later(INVERTED);	/* update the inversion */
            Normal.v_updateVisualState();
            //showmode();
        }
        else
        {
            oap.start.setLine(start_lnum);
            oap.start.setColumn(0);
            oap.motion_type = MLINE;
        }
        G.curwin.w_cursor.setLine(end_lnum);
        G.curwin.w_cursor.setColumn(0);
    } while(false);
        return OK;
    }


    /**
     * Search quote char from string line[col].
     * Quote character escaped by one of the characters in "escape" is not counted
     * as a quote.
     * Returns column number of "quotechar" or -1 when not found.
     */
    static int
    find_next_quote(MySegment line, int col, char quotechar, String escape)
    //    char_u	*escape;	/* escape characters, can be NULL */
    {
        char	c;

        for (;;)
        {
            if (col >= line.count)
                return -1;
            c = line.charAt(col);
            //if (escape != null && vim_strchr(escape, c) != null)
            if(escape != null && vim_strchr(escape, 0, c) >= 0)
                ++col;
            else if (c == quotechar)
                break;
    //#ifdef FEAT_MBYTE...
            ++col;
        }
        return col;
    }

    /**
     * Search backwards in "line" from column "col_start" to find "quotechar".
     * Quote character escaped by one of the characters in "escape" is not counted
     * as a quote.
     * Return the found column or zero.
     */
    static int
    find_prev_quote(MySegment line, int col_start, char quotechar, String escape)
    //    char_u	*escape;	/* escape characters, can be NULL */
    {
        int		n;

        while (col_start > 0)
        {
            --col_start;
    //#ifdef FEAT_MBYTE...
            n = 0;
            if (escape != null)
                while (col_start - n > 0
                       && vim_strchr(escape, 0, line.charAt(col_start - n - 1)) >= 0)
                    ++n;
            if ((n & 1) != 0)
                col_start -= n;	// uneven number of escape chars, skip it
            else if (line.charAt(col_start) == quotechar)
                break;
        }
        return col_start;
    }

    /**
     * Find quote under the cursor, cursor at end.
     * Returns TRUE if found, else FALSE.
     */
    static int current_quote(OPARG oap, int count, boolean include, char quotechar)
    //  int		include;	/* TRUE == include quote char */
    //  int		quotechar;	/* Quote character */
    {
        MySegment	line = ml_get_curline();
        int		col_end;
        int		col_start = G.curwin.w_cursor.getColumn();
        boolean	inclusive = false;
        boolean	vis_empty = true;	// Visual selection <= 1 char
        boolean	vis_bef_curs = false;	// Visual starts before cursor
        boolean	inside_quotes = false;	// Looks like "i'" done before
        boolean	selected_quote = false;	// Has quote inside selection
        int		i;

        // Correct cursor when 'selection' is exclusive
        if (G.VIsual_active)
        {
            vis_bef_curs = G.VIsual.compareTo(G.curwin.w_cursor) < 0;
            if (G.p_sel.charAt(0) == 'e' && vis_bef_curs)
                dec_cursor();
            vis_empty = G.VIsual.equals(G.curwin.w_cursor);
        }

        if (!vis_empty)
        {
            // Check if the existing selection exactly spans the text inside
            // quotes.
            if (vis_bef_curs)
            {
                inside_quotes = G.VIsual.getColumn() > 0
                            && line.charAt(G.VIsual.getColumn() - 1) == quotechar
                            && line.charAt(G.curwin.w_cursor.getColumn()) != 0
                            && line.charAt(G.curwin.w_cursor.getColumn() + 1) == quotechar;
                i = G.VIsual.getColumn();
                col_end = G.curwin.w_cursor.getColumn();
            }
            else
            {
                inside_quotes = G.curwin.w_cursor.getColumn() > 0
                            && line.charAt(G.curwin.w_cursor.getColumn() - 1) == quotechar
                            && line.charAt(G.VIsual.getColumn()) != 0
                            && line.charAt(G.VIsual.getColumn() + 1) == quotechar;
                i = G.curwin.w_cursor.getColumn();
                col_end = G.VIsual.getColumn();
            }

            // Find out if we have a quote in the selection.
            while (i <= col_end)
                if (line.charAt(i++) == quotechar)
                {
                    selected_quote = true;
                    break;
                }
        }

        if (!vis_empty && line.charAt(col_start) == quotechar)
        {
            // Already selecting something and on a quote character.  Find the
            // next quoted string.
            if (vis_bef_curs)
            {
                // Assume we are on a closing quote: move to after the next
                // opening quote.
                col_start = find_next_quote(line, col_start + 1, quotechar, null);
                if (col_start < 0)
                    return FALSE;
                col_end = find_next_quote(line, col_start + 1, quotechar,
                                                                  G.curbuf.b_p_qe);
                if (col_end < 0)
                {
                    // We were on a starting quote perhaps?
                    col_end = col_start;
                    col_start = G.curwin.w_cursor.getColumn();
                }
            }
            else
            {
                col_end = find_prev_quote(line, col_start, quotechar, null);
                if (line.charAt(col_end) != quotechar)
                    return FALSE;
                col_start = find_prev_quote(line, col_end, quotechar,
                                                                  G.curbuf.b_p_qe);
                if (line.charAt(col_start) != quotechar)
                {
                    // We were on an ending quote perhaps?
                    col_start = col_end;
                    col_end = G.curwin.w_cursor.getColumn();
                }
            }
        }
        else

        if (line.charAt(col_start) == quotechar
                || !vis_empty
                )
        {
            int	first_col = col_start;

            if (!vis_empty)
            {
                if (vis_bef_curs)
                    first_col = find_next_quote(line, col_start, quotechar, null);
                else
                    first_col = find_prev_quote(line, col_start, quotechar, null);
            }
            // The cursor is on a quote, we don't know if it's the opening or
            // closing quote.  Search from the start of the line to find out.
            // Also do this when there is a Visual area, a' may leave the cursor
            // in between two strings.
            col_start = 0;
            for (;;)
            {
                // Find open quote character.
                col_start = find_next_quote(line, col_start, quotechar, null);
                if (col_start < 0 || col_start > first_col)
                    return FALSE;
                // Find close quote character.
                col_end = find_next_quote(line, col_start + 1, quotechar,
                                                                  G.curbuf.b_p_qe);
                if (col_end < 0)
                    return FALSE;
                // If is cursor between start and end quote character, it is
                // target text object.
                if (col_start <= first_col && first_col <= col_end)
                    break;
                col_start = col_end + 1;
            }
        }
        else
        {
            // Search backward for a starting quote.
            col_start = find_prev_quote(line, col_start, quotechar, G.curbuf.b_p_qe);
            if (line.charAt(col_start) != quotechar)
            {
                // No quote before the cursor, look after the cursor.
                col_start = find_next_quote(line, col_start, quotechar, null);
                if (col_start < 0)
                    return FALSE;
            }

            // Find close quote character.
            col_end = find_next_quote(line, col_start + 1, quotechar,
                                                                  G.curbuf.b_p_qe);
            if (col_end < 0)
                return FALSE;
        }

        // When "include" is TRUE, include spaces after closing quote or before
        // the starting quote.
        if (include)
        {
            if (vim_iswhite(line.charAt(col_end + 1)))
                while (vim_iswhite(line.charAt(col_end + 1)))
                    ++col_end;
            else
                while (col_start > 0 && vim_iswhite(line.charAt(col_start - 1)))
                    --col_start;
        }

        /* Set start position.  After vi" another i" must include the ".
         * For v2i" include the quotes. */
        if (!include && count < 2
                && (vis_empty || !inside_quotes)
                )
            ++col_start;
        G.curwin.w_cursor.setColumn(col_start);
        if (G.VIsual_active)
        {
            // Set the start of the Visual area when the Visual area was empty, we
            // were just inside quotes or the Visual area didn't start at a quote
            // and didn't include a quote.
            //
            if (vis_empty
                    || (vis_bef_curs
                        && !selected_quote
                        && (inside_quotes
                            || (line.charAt(G.VIsual.getColumn()) != quotechar
                                && (G.VIsual.getColumn() == 0
                                    || line.charAt(G.VIsual.getColumn() - 1)
                                        != quotechar)))))
            {
                G.VIsual = G.curwin.w_cursor.copy();
                // redraw_curbuf_later(INVERTED);
                Normal.v_updateVisualState();
            }
        }
        else
        {
            oap.start = G.curwin.w_cursor.copy();
            oap.motion_type = MCHAR;
        }

        // Set end position.
        G.curwin.w_cursor.setColumn(col_end);
        if ((include || count > 1
                    // After vi" another i" must include the ".
                    || (!vis_empty && inside_quotes)
            ) && inc_cursorV7() == 2)
            inclusive = true;
        if (G.VIsual_active)
        {
            if (vis_empty || vis_bef_curs)
            {
                // decrement cursor when 'selection' is not exclusive
                if (G.p_sel.charAt(0) != 'e')
                    dec_cursor();
            }
            else
            {
                // Cursor is at start of Visual area.  Set the end of the Visual
                // area when it was just inside quotes or it didn't end at a
                // quote.
                if (inside_quotes
                        || (!selected_quote
                            && line.charAt(G.VIsual.getColumn()) != quotechar
                            && (line.charAt(G.VIsual.getColumn()) == NUL
                                || line.charAt(G.VIsual.getColumn() + 1) != quotechar)))
                {
                    dec_cursor();
                    G.VIsual = G.curwin.w_cursor.copy();
                }
                G.curwin.w_cursor.setColumn(col_start);
            }
            if (G.VIsual_mode == 'V')
            {
                G.VIsual_mode = 'v';
                //redraw_cmdline = TRUE;		// show mode later
                Normal.v_updateVisualState();
            }
        }
        else
        {
            // Set inclusive and other oap's flags.
            oap.inclusive = inclusive;
        }

        return OK;
    }

    /**
     * Return TRUE if the character before "linep[col]" equals "ch".
     * Return FALSE if "col" is zero.
     * Update "*prevcol" to the column of the previous character, unless "prevcol"
     * is NULL.
     * Handles multibyte string correctly.
     */
    static boolean check_prevcol(MySegment linep, int col, char ch,
                                 MutableInt pPrevcol)
    {
        --col;
    //#ifdef FEAT_MBYTE...
        if (pPrevcol != null)
            pPrevcol.setValue(col);
        return col >= 0 && linep.charAt(col) == ch;
    }

    static boolean linewhite(int /*linenr_t*/ lnum) {
        MySegment seg = ml_get(lnum);
        int idx = Misc.skipwhite(seg, 0);
        return seg.array[seg.offset + idx] == '\n';
    }
}
