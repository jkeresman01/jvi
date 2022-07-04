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
import com.raelity.jvi.core.lib.Constants;

/**
 * Arguments for operators.
 */
public class OPARG implements Cloneable
{
    int	    op_type;		/* current pending operator type */
    char    regname;		/* register to use for the operator */
    int	    motion_type;	/* type of the current cursor motion */
    boolean use_reg_one;	/* TRUE if delete uses reg 1 even when not
				   linewise */
    boolean inclusive;		/* TRUE if char motion is inclusive (only
				   valid when motion_type is MCHAR */
    boolean end_adjusted;	/* backuped b_op_end one char (only used by
				   do_format()) */
    ViFPOS  start;		/* start of the operator */
    ViFPOS  end;		/* end of the operator */
    int     line_count;		/* number of lines from op_start to op_end
				   (inclusive) */
    boolean empty;		/* op_start and op_end the same (only used by
				   do_change()) */
    boolean is_VIsual;		/* operator on Visual area */
    boolean block_mode;		/* current operator is Visual block mode */
    int	    start_vcol;		/* start col for block mode operator */
    int	    end_vcol;		/* end col for block mode operator */
    boolean error;              /* some type of error encounter (not in vim) */

    public void clearop() {
      op_type = Constants.OP_NOP;
      regname = 0;
      use_reg_one = false;
    }

    @Override
    public OPARG clone() throws CloneNotSupportedException
    {
        OPARG dest;
        dest = (OPARG) super.clone();
        dest.start = (start == null ? null : start.copy());
        dest.end = (end == null ? null : end.copy());
        return dest;
    }

    /**
     * Create a copy of OPARG.
     * @return a copy of this OPARG object
     */
    public OPARG copy() {
        try {
            return clone();
        } catch (CloneNotSupportedException ex) {
        }
        return null;
    }

    /**
     * Return a readable string representation of the OPARG object.
     * @return a readable string representation of the OPARG object.
     */
    @Override
    public String toString() {

      return                "op_type: " + op_type
			 + " regname: " + regname
			 + " motion_type: " + motion_type
			 + " inclusive: " + inclusive
			 + " end_adjusted: " + end_adjusted
			 + "\n start: {" + start + "}"
			 + " end: {" + end + "}"
			 + " line_count: " + line_count
			 + " empty: " + empty
			 + " is_VIsual: " + is_VIsual
			 + " block_mode: " + block_mode
			 + " start_vcol: " + start_vcol
			 + " end_vcol: " + end_vcol
			 ;
    }
}
