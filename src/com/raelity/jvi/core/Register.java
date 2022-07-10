/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2022 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;

import java.awt.event.ActionEvent;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.google.common.eventbus.Subscribe;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.Ops.block_def;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.core.lib.Constants.CB;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.text.MySegment;
import com.raelity.text.TextUtil;

import static com.raelity.jvi.core.Edit.*;
import static com.raelity.jvi.core.ExCommands.getaltfname;
import static com.raelity.jvi.core.GetChar.*;
import static com.raelity.jvi.core.JviClipboard.*;
import static com.raelity.jvi.core.Misc.*;
import static com.raelity.jvi.core.Normal.find_ident_under_cursor;
import static com.raelity.jvi.core.Ops.*;
import static com.raelity.jvi.core.Ops.LOG;
import static com.raelity.jvi.core.Search.last_search_pat;
import static com.raelity.jvi.core.Util.*;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.CtrlChars.*;
import static com.raelity.jvi.manager.ViManager.eatme;
import static com.raelity.text.TextUtil.debugString;
import static com.raelity.text.TextUtil.sf;

/**
 *
 * @author err
 */
public class Register
{
private static final String PREF_REGISTERS = "registers";
private static PreferencesImportMonitor registersImportCheck;

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
        @Override
        public void init()
        {
            Register.init();
        }
    }

    private static void init() {
        Commands.register("reg", "registers", new DoRegisters(), null);
        ViEvent.getBus().register(new EventHandlers());
    }

    private static class EventHandlers
    {

    @Subscribe
    public void boot(ViEvent.Boot ev) {
      read_viminfo_registers();
      registersImportCheck = PreferencesImportMonitor.getMonitor(
              ViManager.getFactory().getPreferences(), PREF_REGISTERS);
    }

    @Subscribe
    public void shutdown(ViEvent.Shutdown ev) {
      if(!registersImportCheck.isChange()) {
        write_viminfo_registers();
      } else {
        LOG.info("jVi registers imported");
      }
    }
    }
    
private static final String DATA = "data";
private static final String TYPE = "type";

private static void read_viminfo_registers() {
    Preferences prefsRegs = ViManager.getFactory()
            .getPreferences().node(PREF_REGISTERS);
    for(int i = 0; i < y_regs.size(); i++) {
        try {
            String regname = String.valueOf(get_register_name(i));
            if(regname.equals("-") || isCbName(regname.charAt(0)))
                continue;
            if (prefsRegs.nodeExists(regname)) {
                Preferences prefs = prefsRegs.node(regname);
                String regval = prefs.get(DATA, null);
                if (regval != null) {
                    int type = prefs.getInt(TYPE, MCHAR);
                    get_yank_register(regname.charAt(0), false);
                    Yankreg reg = y_current;
                    reg.setData(regval, type);
                    //System.err.println("\t" + type);
                }
            }
        } catch (BackingStoreException ex) {
            //Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

private static void write_viminfo_registers() {
    Preferences prefsRegs = ViManager.getFactory()
            .getPreferences().node(PREF_REGISTERS);
    for(int i = 0; i < y_regs.size(); i++) {
        String regname = String.valueOf(get_register_name(i));
        if(regname.equals("-") || isCbName(regname.charAt(0)))
            continue;
        Yankreg reg = y_regs.get(i);
        String regval = "";
        if(reg != null) {
            regval = reg.getAll();
        }
        try {
            if(regval.isEmpty() || regval.length() > 1024) {
                if (prefsRegs.nodeExists(regname)) {
                    prefsRegs.node(regname).removeNode();
                }
            } else {
                assert reg != null;  // since regval != null
                Preferences prefs = prefsRegs.node(regname);
                prefs.put(DATA, regval);
                prefs.putInt(TYPE, reg.y_type);
            }
            prefsRegs.flush();
        } catch (BackingStoreException ex) {
            //Logger.getLogger(Misc.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
    
//
// Number of registers.
//	0 = unnamed register, for normal yanks and puts
//   1..9 = number registers, for deletes
// 10..35 = named registers
//     36 = delete register (-)
//     37 = Selection register (*).
//     38 = Clipboard register (+).
//
static final int DELETION_REGISTER = 36;   // array index to yankreg
static final int STAR_REGISTER = 37;  // array index to yankreg
static final int PLUS_REGISTER = 38;  // array index to yankreg
static final int LAST_REGISTER = PLUS_REGISTER;
private static Yankreg y_current = null;
private static Yankreg y_previous = null;
private static boolean y_append;
// y_regs_as_arry should *never* be modified, here only for debug
//static final Yankreg[] y_regs_as_array = initialYankRegs();
static final List<Yankreg> y_regs = Collections.unmodifiableList(Arrays.asList(initialYankRegs()));

/** allocate all the named registers. Some simplification */
static Yankreg[] initialYankRegs()
{
    Yankreg[] regs = new Yankreg[LAST_REGISTER + 1];
    
    for(int i = 0; i < regs.length; i++) {
        regs[i] = new Yankreg(get_register_name(i));
    }
    return regs;
}

static Yankreg get_y_current()
{
    return y_current;
}

final static boolean isStarRegister(Yankreg yreg)
{
    return yreg == y_regs.get(STAR_REGISTER);
}

final static boolean isPlusRegister(Yankreg yreg)
{
    return yreg == y_regs.get(PLUS_REGISTER);
}
    
    /**
     * Sadly the yankbuf's are not code compatible
     * except for MBLOCK.
     * <br/>For MLINE and MCHAR there is a single
     * string, even if multiple lines, and the string
     * has embedded '\n'. The y_size is the number of
     * lines, but y_array is only one element. The
     * difference between MLINE and MCHAR is the
     * terminating '\n'.
     * <br/> Although not code compatible, the code for
     * put operations is pretty simple.
     */
    // TODO: clean up since "know" that y_array is never null,
    //       and has at least one StringBuilder i.e. y_array[0].method is OK.
    final static class Yankreg {
    final char y_name;
    // NEEDSWORK: init to null when private
    StringBuilder[] y_array = new StringBuilder[] { new StringBuilder() };
    
    // NOTE: if a field is added, make sure to fixup this.set(Yankreg)
    int y_size;
    int y_type;
    int y_width;
    
    public Yankreg()
    {
        this((char)0);
    }
    
    public Yankreg(char y_name)
    {
        this.y_name = y_name;
    }
    
    /** @return the indexed line, index starts at 0 */
    String get(int i) {
        return y_array[i].toString();
    }
    
    boolean isEmpty() {
        return y_type != MBLOCK
                && (y_size == 0 || y_array == null || y_array.length == 0
                || y_array[0] == null || y_array[0].isEmpty());
    }
    
    /**
     * @return all the contents as a single string
     */
    String getAll() {
        if(y_type == MBLOCK) {
            StringBuilder sb = new StringBuilder();
            for(StringBuilder y_array1 : y_array) {
                sb.append(y_array1);
                sb.append('\n'); // DONE
            }
            return sb.toString();
        }
        else {
            if (y_size == 0 || y_array == null || y_array[0] == null)
                return "";
            return y_array[0].toString();
        }
    }
    
    void clear() {
        y_size = 0;
        y_type = 0;
        // NEEDSWORK: init to null when private
        y_array = new StringBuilder[] { new StringBuilder() };
    }
    
    /**
     * Return a yankreg with the same contents as this yankreg. If fCopy is
     * set then create a copy of the string data, otherwise copy the data
     * and clear the data in the original.
     */
    Yankreg copy(boolean fCopy) {
        Yankreg reg = new Yankreg();
        reg.set(this, fCopy);
        return reg;
    }
    
    /**
     * Move the contents of the argument Yankreg into this and clear the
     * argument reg's data.
     */
    void set(Yankreg reg, boolean keepSourceData) {
        y_size = reg.y_size;
        y_type = reg.y_type;
        y_width = reg.y_width;
        y_array = reg.y_array;
        if(reg.y_array == null)
            y_array = null;
        else {
            if(keepSourceData) {
                // copy the stuff
                y_array = new StringBuilder[reg.y_array.length];
                for(int i = 0; i < reg.y_array.length; i++)
                    y_array[i] = reg.y_array[i] == null
                         ? null : new StringBuilder(reg.y_array[i]);
            } else {
                y_array = reg.y_array;
                //for(int i = 0; i < reg.y_array.length; i++)
                //  y_array[i] = reg.y_array[i] == null
                //             ? null : new StringBuilder(reg.y_array[i]);
            }
        }
        if(!keepSourceData) {
            reg.clear();
        }
    }
    
    void setData(String s, Integer type) {
        if(type != null && type == MBLOCK) {
            y_width = 0;
            int startOffset = 0;
            int endOffset;
            int lines = 0;
            List<StringBuilder> l = new ArrayList<>();
            while((endOffset = s.indexOf('\n', startOffset)) >= 0) { // DONE
                StringBuilder sb
                        = new StringBuilder(s.subSequence(startOffset, endOffset));
                l.add(sb);
                if(sb.length() > y_width)
                    y_width = sb.length();
                startOffset = endOffset + 1;
                lines++;
            }
            y_array = l.toArray(StringBuilder[]::new);
            y_type = MBLOCK;
            y_size = lines;
            
            y_width--;  // WISH I NEW WHY THIS IS NEEDED, see vim's str_to_reg
            
            //y_array[0] = new StringBuilder(s);
        } else {
            y_width = 0;
            int offset = 0;
            int lines = 0;
            while((offset = s.indexOf('\n', offset)) >= 0) { // DONE
                offset++;	// for next time through the loop
                lines++;
            }
            if(lines > 0) {
                y_type = MLINE;
                y_size = lines;
            } else {
                y_type = MCHAR;
                y_size = 1;
            }
            y_array[0] = new StringBuilder(s);
        }
    }
    
    @Override
    public String toString()
    {
        String s = getAll();
        return sf("Yankreg{'%c' %d \"%s\"}", y_name, y_type,
                                                   debugString(s.length() > 25 ? (s.substring(0, 25) + "...") : s));
    }
    
    }

/**
 * Map yankreg index to regname string, this is like an inverse mapping
 * @param i yankreg index
 * @return the regname as a string
 */
static char get_register_name(int i) {
    if(i == -1)
        return '\"';
    else if(i <= 9)
        return (char)('0'+i);
    else if(i <= 35)
        return (char)('a' + i - 10);
    else if(i == DELETION_REGISTER)
        return '-';
    else if(isCbIdx(i))
        return idx2CbName(i);
    else
        return 0; // this should never happen
}

    /**
     * print the registers
     */
    private static class DoRegisters extends Commands.AbstractColonAction
    {
    @Override
    public void actionPerformed(ActionEvent ev) {
        Commands.ColonEvent cev = (Commands.ColonEvent)ev;
        
        String arg = null;
        if(cev.getNArg() > 0)
            arg = cev.getArgString();
        Yankreg yb;
        char name;
        
        try (ViOutputStream vios = ViManager.createOutputStream("\nType Name Content")) {
            StringBuilder sb = new StringBuilder();
            for (int i = -1; i < y_regs.size(); ++i) {
                if(i == -1) {
                    name = get_register_name(i);
                    if (y_previous != null)
                        yb = y_previous;
                    else
                        yb = y_regs.get(0);
                } else {
                    yb = y_regs.get(i);
                    name = yb.y_name;
                }
                if (arg != null && vim_strchr(arg, name) == null)
                    continue;	    /* did not ask for this register */
                name = adjust_clip_reg(name);
                if(name == 0)
                    continue;
                may_get_selection(name);
                String regval = "";
                if(yb != null)
                    regval = yb.getAll();
                if (!regval.isEmpty()) {
                    assert yb != null;
                    char type = yb.y_type == MBLOCK ? 'b'
                        : yb.y_type == MLINE ? 'l'
                          : 'c';
                    printOneRegister(sb, type, name, regval, false);
                    vios.println(sb.toString());
                }
            }
            
            // display last inserted text
            do_dis(vios, sb, '.', arg, true);
            
            // display last command line
            do_dis(vios, sb, ':', arg, false);
            
            // display current file name
            do_dis(vios, sb, '%', arg, false);
            
            // display alternate file name
            do_dis(vios, sb, '#', arg, false);
            
            // display last search pattern
            do_dis(vios, sb, '/', arg, false);
            
            // #ifdef FEAT_EVAL..
            // display last used expression
            // #endif
        }
    }
    } // END CLASS DoRegisters ===============================================
    
private final static int regCmdFormatColumns = 120;
private static void printOneRegister(StringBuilder sb,
                                     char type, char regname,
                                     String value, boolean skip_esc)
{
    eatme(skip_esc);
    sb.setLength(0);
    sb.append("  ").append(type).append("  ")
            .append("\"").append(regname).append("   ");
    
    int n = regCmdFormatColumns - 6;
    String escaped = debugString(value);
    if(escaped.length() > n)
        escaped = escaped.substring(0, n-1);
    sb.append(escaped);
}
    
private static void do_dis(ViOutputStream vios, StringBuilder sb,
                           char regname, String arg, boolean skip_esc) {
    if (arg != null && vim_strchr(arg, regname) == null)
        return;	    /* did not ask for this register */
    Wrap<String> argp = new Wrap<>();
    boolean displayit = get_spec_reg(regname, argp, false);
    if(displayit && argp.getValue() != null) {
        printOneRegister(sb, 'c', regname, argp.getValue(), skip_esc);
        vios.println(sb.toString());
    }
}

/**
 * Check if 'regname' is a valid name of a yank register.
 * Note: There is no check for 0 (default register), caller should do this
 * @param writing if true check for writable registers
 */
public static boolean valid_yank_reg(char regname, boolean writing) {
    if (regname > '~')
        return false;
    return Util.isalnum(regname)
            || (!writing && vim_strchr("/.%#:", 0, regname) >= 0)
            || regname == '"'
            || regname == '-'
            || regname == '_'
            || isCbName(regname);
}

/**
 * Set y_current and y_append, according to the value of "regname".
 * Cannot handle the '_' register.
 *
 * If regname is 0 and writing, use register 0
 * If regname is 0 and reading, use previous register
 */
// IN VIM9 THIS RETURNS A BOOLEAN
static void get_yank_register(char regname, boolean writing) {
    char	    i;
    
    y_append = false;
    if (((regname == 0 && !writing) || regname == '"') && y_previous != null) {
        y_current = y_previous;
        return;
    }
    i = regname;
    if (Util.isdigit(i))
        i -= '0';
    else if (Util.islower(i))
        i -= 'a' - 10;
    else if (Util.isupper(i)) {
        i -= 'A' - 10;
        y_append = true;
    } else if (regname == '-')
        i = DELETION_REGISTER;
    // When clipboard is not available, use register 0 instead of '*'/'+'
    else if(isValidCb(name2Cb(regname)))
        i = (char)name2CbIdx(regname);
    else		/* not 0-9, a-z, A-Z or '-': use register 0 */
        i = 0;
    y_current = y_regs.get(i);
    if (writing)	/* remember the register we write into for do_put() */
        y_previous = y_current;
}

/**
 * Obtain the contents of a "normal" register. The register is made empty.
 * The returned pointer has allocated memory, use put_register() later.
 * <p>
 * Note that this creates a floating Yankreg;
 * it is not referenced by y_regs.
 * 
 * @param name the register of which to make a copy
 * @param copy make a copy, if FALSE make register empty.
 * @return the register copy
 */
static Yankreg get_register(char name, boolean copy) {
// When Visual area changed, may have to update selection.  Obtain the
//selection too.
if (name == '*' && clip_star.avail)
{
    //if (clip_autoselect_star)
    //  clip_star.clip_update_selection();
    may_get_selection(name);
}
if (name == '+' && clip_plus.avail)
{
    //if (clip_autoselect_plus)
    //  clip_plus.clip_update_selection();
    may_get_selection(name);
}

get_yank_register(name, false);
Yankreg reg = y_current.copy(copy);
return reg;
}

/**
* Return the value, as a string, of the named register;
* these are for i_^R, wonder if they are grouped/used like this elsewhere
* @param name register name
* @return the string value
*/
static String get_register_value(char regname) {
    String val = null;
    if(isCbName(regname))
        regname = may_get_selection(regname);
    
    if(regname >= 'a' && regname <= 'z' || "\"-+*\000".indexOf(regname) >= 0) {
        Yankreg reg = get_register(regname, true);
        if (reg.y_size != 0 && reg.y_array != null)
            val = reg.getAll();
    } else {
        //else if ("%/:.".indexOf(regname) >= 0) {
        Wrap<String> pArg = new Wrap<>();
        if(get_spec_reg(regname, pArg, true))
            val = pArg.getValue();
    }
    return val;
}

/**
 * Put "reg" into register "name".  Free any previous contents and "reg".
 */
static void put_register(char name, Yankreg reg) {
    get_yank_register(name, false);
    y_current.set(reg, false); // reg's data/pointers is cleared.
    
    /* Send text written to clipboard register to the clipboard. */
    may_set_selection();
}

static char	do_record_regname;
/**
 * start or stop recording into a yank register
 *
 * return FAIL for failure, OK otherwise
 */
static int do_record(char c) {
    Yankreg old_y_previous, old_y_current;
    int		retval;
    
    if ( ! G.Recording) {	    // start recording
        // registers 0-9, a-z and " are allowed
        if (c > '~' || (!Util.isalnum(c) && c != '"'))
            retval = FAIL;
        else {
            G.Recording = true;
            showmode();
            do_record_regname = c;
            retval = OK;
        }
    } else {			    /* stop recording */
        String s;
        G.Recording = false;
        showmode(); // Msg.smsg("");
        s = GetChar.get_recorded();
        if (s.length() == 0)
            retval = FAIL;
        else {
            //
            // We don't want to change the default register here, so save and
            // restore the current register name.
            //
            old_y_previous = y_previous;
            old_y_current = y_current;
            
            retval = stuff_yank(do_record_regname, s);
            // System.err.println("Recorded: '" + s + "'");
            
            y_previous = old_y_previous;
            y_current = old_y_current;
        }
    }
    return retval;
}

/**
 * Stuff string 'p' into yank register 'regname' as a single line (append if
 * uppercase).	'p' must have been alloced.
 *
 * @return FAIL for failure, OK otherwise
 */
static private int stuff_yank(char regname, String s) {
    // check for read-only register
    if (regname != 0 && !valid_yank_reg(regname, true)) {
        return FAIL;
    }
    if (regname == '_') {		    // black hole: don't do anything
        return OK;
    }
    get_yank_register(regname, true);
    if (y_append) {
        y_current.y_array[0].append(s);
    } else {
        free_yank_all();
        y_current.y_array[0].append(s);
        y_current.y_size = 1;
        y_current.y_type = MCHAR;  // (orig comment)used to be MLINE, why?
    }
    return OK;
}

static private char	lastc_do_execreg = NUL;
/**
 * execute a yank register: copy it into the stuff buffer
 * @return FAIL for failure, OK otherwise
 * @param regname     get commands from this register
 * @param colon	insert ':' before each line
 * @param addcr	always add '\n' to end of line
 */
static int do_execreg(char regname, boolean colon, boolean addcr) {
    int		retval = OK;
    
    // NEEDSWORK: do_execreg: colon always false
    if (regname == '@')			// repeat previous one
        regname = lastc_do_execreg;
    // check for valid regname
    if (regname == '%' || regname == '#' || !valid_yank_reg(regname, false))
        return FAIL;
    lastc_do_execreg = regname;
    
    if (regname == '_')			// black hole: don't stuff anything
        return OK;
    
    switch (regname) {
    // use last command line
    case ':':
        return FAIL; // NEEDSWORK: do_execreg ':'
        /* ****************************************************************
        !!! s = ColonCommands.lastCommand; // s = last_cmdline;
        if (last_cmdline == NULL)
        {
        EMSG(e_nolastcmd);
        return FAIL;
        }
        vim_free(new_last_cmdline); // don't keep the cmdline containing @:
        new_last_cmdline = NULL;
        retval = put_in_typebuf(last_cmdline, TRUE);
        ****************************************************************/
    case '.':
        // use last inserted text
        String s = get_last_insert_save();
        if (s == null || s.length() == 0)
        {
            Msg.emsg(Messages.e_noinstext);
            return FAIL;
        }
        retval = put_in_typebuf(s, colon);
        break;
    default:
        int remap;
        get_yank_register(regname, false);
        if(y_current.isEmpty())
            return FAIL;
        
        /* Disallow remaping for ":@r". */
        remap = colon ? -1 : 0;
        
        //
        // Insert lines into typeahead buffer, from last one to first one.
        //
        /* ****************************************************************
        for (i = y_current.y_size; --i >= 0; )
        {
        // insert newline between lines and after last line if type is MLINE
        if (y_current.y_type == MLINE || i < y_current.y_size - 1
        || addcr)
        {
        if (ins_typebuf("\n", remap, 0, true) == FAIL)
        return FAIL;
        }
        if (ins_typebuf(y_current.y_array[i], remap, 0, true) == FAIL)
        return FAIL;
        if (colon && ins_typebuf(":", remap, 0, true) == FAIL)
        return FAIL;
        }
        ****************************************************************/
        // Just roll our own for jvi
        StringBuilder sb = new StringBuilder(y_current.y_array[0]);
        if(y_current.y_type == MLINE || addcr) {
            if(sb.length() == 0 || sb.charAt(sb.length()-1) != '\n') { // DONE
                sb.append('\n'); // DONE
            }
        }
        //
        // NEEDSWORK: if(colon) put ":" begin of each line
        //
        if(ins_typebuf_redo(sb, remap, 0, true) == FAIL) {
            return FAIL;
        }
        G.Exec_reg = true;	// disable the 'q' command
        break;
    }
    return retval;
}

private static int put_in_typebuf(String s, boolean colon)
{
    int		retval = OK;
    
    if (colon)
        retval = ins_typebuf_redo("\n", FALSE, 0, true); // DONE
    if (retval == OK)
        retval = ins_typebuf_redo(s, FALSE, 0, true);
    if (colon && retval == OK)
        retval = ins_typebuf_redo(":", FALSE, 0, true);
    return retval;
}

/**
 * Insert a yank register: copy it into the Read buffer.
 * Used by CTRL-R command and middle mouse button in insert mode.
 *
 * return FAIL for failure, OK otherwise
 */
static int insert_reg(char regname, boolean literally)
        throws NotSupportedException
{
    int         i;
    int		retval = OK;
    
/////    /*
/////     * It is possible to get into an endless loop by having CTRL-R a in
/////     * register a and then, in insert mode, doing CTRL-R a.
/////     * If you hit CTRL-C, the loop will be broken here.
/////     */
/////    ui_breakcheck();
/////    if (got_int)
/////	return FAIL;

/* check for valid regname */
if (regname != NUL && !valid_yank_reg(regname, false))
    return FAIL;

regname = may_get_selection(regname);

Wrap<String> pArg = new Wrap<>();
if (regname == '.')                 // insert last inserted text
    retval = stuff_inserted(NUL, 1, true);
else if (get_spec_reg(regname, pArg, true))
{
    String arg = pArg.getValue();
    if (arg == null)
        return FAIL;
    if (literally)
        stuffescaped(arg);
    else {
        Edit.checkForStuffReadbuf(arg, "i_CTRL-R");
        stuffReadbuff(arg);
    }
}
else				/* name or number register */
{
    get_yank_register(regname, false);
    if(y_current.isEmpty())
        retval = FAIL;
    else
    {
        // Sadly the yankbuf's are not code compatible
        // except for MBLOCK
        if(y_current.y_type == MBLOCK) {
            // THIS LOOP IS THE ORIGINAL CODE
            for (i = 0; i < y_current.y_size; ++i)
            {
                String s = y_current.get(i);
                if (literally)
                    stuffescaped(s);
                else {
                    Edit.checkForStuffReadbuf(s, "i_CTRL-R");
                    stuffReadbuff(s);
                }
                //
                // Insert a newline between lines and after last line if
                // y_type is MLINE.
                //
                if (y_current.y_type == MLINE || i < y_current.y_size - 1)
                    stuffcharReadbuff('\n'); // DONE
            }
        } else {
            String s = y_current.get(0);
            if (literally)
                stuffescaped(s);
            else {
                Edit.checkForStuffReadbuf(s, "i_CTRL-R");
                stuffReadbuff(s);
            }
        }
    }
}

return retval;
}

/*
* Stuff a string into the typeahead buffer, such that edit() will insert it
* literally. Note that IM_LITERAL is used instead of Ctrl-V. That is because
* Ctrl-V only allows a few different characters.
*/
private static void stuffescaped(String arg)
{
    int offset = 0;
    while (offset < arg.length())
    {
        char c = arg.charAt(offset);
        stuffcharReadbuff(IM_LITERAL);
        stuffcharReadbuff(c);
        offset++;
    }
}

/*
* If "regname" is a special register, return a pointer to its value.
*/
static boolean get_spec_reg(char regname, Wrap<String> argp, boolean errmsg)
{
    int		cnt;
    String            s;
    
    argp.setValue(null);
    switch (regname)
    {
    case '%':		// file name
        if (errmsg)
            check_fname();	// will give emsg if not set
        argp.setValue(G.curbuf.getFile().getPath());
        return true;
        
    case '#':		/* alternate file name */
        argp.setValue(getaltfname(errmsg));	/* may give emsg if not set */
        return true;
        
//  #ifdef WANT_EVAL...
        
    case ':':		/* last command line */
        s = ExCommands.lastCommand; // s = last_cmdline;
        if (s == null && errmsg)
            Msg.emsg(Messages.e_nolastcmd);
        argp.setValue(s);
        return true;
        
    case '/':		/* last search-pattern */
        s = last_search_pat();
        if (s == null && errmsg)
            Msg.emsg(Messages.e_noprevre);
        argp.setValue(s);
        return true;
        
    case '.':		/* last inserted text */
        argp.setValue(get_last_insert_save());
        if (argp.getValue() == null && errmsg)
            Msg.emsg(Messages.e_noinstext);
        return true;
        
//  #ifdef FEAT_SEARCHPATH...
        
    case CTRL_W:  // word under cursor
    case CTRL_A:  // WORD (mnemonic All) under cursor
        if (!errmsg)
            return false;
        MutableInt mi = new MutableInt();
        CharacterIterator ci
                = find_ident_under_cursor(mi, regname == CTRL_W
                                                    ?  (FIND_IDENT|FIND_STRING) : FIND_STRING);
        cnt = mi.getValue();
        argp.setValue(cnt > 0 ? TextUtil.toString(ci, cnt) : null);
        return true;
        
    case CTRL_L:		// Line under cursor
        if (!errmsg)
            return false;
        MySegment seg = ml_get_buf(G.curwin.w_buffer,
                                         G.curwin.w_cursor.getLine());
        argp.setValue(TextUtil.toString(seg));
        return true;
        
    case '_':		// black hole: always empty
        argp.setValue("");
        return true;
    }
    
    return false;
}

/*
 * Shift the delete registers: "9 is cleared, "8 becomes "9, etc.
 */
static void shift_delete_registers()
{
    for (int n = 9; n > 1; --n)
        y_regs.get(n).set(y_regs.get(n - 1), false);

    // TODO: vim9 /////////////////////////////
    //if(!y_append)
    //    y_previous = y_current;
    ///////////////////////////////////////////

    y_previous = y_current = y_regs.get(1);
    y_regs.get(1).clear();
    
}

/**
 * Free up storage associated with current yank buffer.
 */
static void free_yank_all() {
    y_current.clear();
}

/**
 * Yank the text between oap->start and oap->end into a yank register.
 * If we are to append (uppercase register), we first yank into a new yank
 * register and then concatenate the old and the new one (so we keep the old
 * one in case of out-of-memory).
 * <br><b>NEEDSWORK:</b><ul>
 * <li>An unfaithful and lazy port of yanking off.
 * </ul>
 *
 * @return FAIL for failure, OK otherwise
 */
public static int op_yank(OPARG oap, boolean deleting, boolean mess)
{
    int		y_idx = 0;		// index in y_array[]
    Yankreg		curr;		// copy of y_current
    // char_u		**new_ptr;
    int			lnum;		// current line number
    // int			j;
    // int			len;
    int			yanktype = oap.motion_type;
    int			yanklines = oap.line_count;
    int			yankendlnum = oap.end.getLine();
    // char_u		*p;
    StringBuilder        pnew;
    block_def		bd;
    
    // check for read-only register
    if (oap.regname != 0 && !valid_yank_reg(oap.regname, true))
    {
        Util.beep_flush();
        return FAIL;
    }
    if (oap.regname == '_')	    // black hole: nothing to do
        return OK;
    
    if(isCbName(oap.regname) && !isValidCb(name2Cb(oap.regname)))
        oap.regname = 0;
    
    if (!deleting)		    // op_delete() already set y_current
        get_yank_register(oap.regname, true);
    
    curr = y_current;
    // append to existing contents
    if (y_append && !y_current.isEmpty()) {
        y_current = new Yankreg();
        // y_current = new Yankreg(); // NEEDSWORK: just append to y_current
    } else {
        free_yank_all();	    // free previously yanked lines
    }
    
    //
    // If the cursor was in column 1 before and after the movement, and the
    // operator is not inclusive, the yank is always linewise.
    //
    if (       oap.motion_type == MCHAR
            && oap.start.getColumn() == 0
            && !oap.inclusive
            && (!oap.is_VIsual || G.p_sel.charAt(0) == 'o')
            && !oap.block_mode  // from vim7
            && oap.end.getColumn() == 0
            && yanklines > 1)
    {
        yanktype = MLINE;
        --yankendlnum;
        --yanklines;
    }
    
    y_current.y_size = yanklines;
    y_current.y_type = yanktype;   // set the yank register type
    y_current.y_width = 0;
    // y_current.y_array = (char_u **)lalloc_clear.......
    // if (y_current.y_array == NULL) { y_current = curr; return FAIL; }
    
    lnum = oap.start.getLine();
    
    //
    // Visual block mode
    //
    if (oap.block_mode) {
        // block mode deleted
        y_current.y_type = MBLOCK;        /* set the yank register type */
        y_current.y_width = oap.end_vcol - oap.start_vcol;
        
        if (  G.curwin.w_curswant == MAXCOL && y_current.y_width > 0)
            y_current.y_width--;
        y_current.y_array = new StringBuilder[yanklines];
        bd = new block_def();
        for ( ; lnum <= yankendlnum; ++lnum)
        {
            block_prep(oap, bd, lnum, false);
            
            pnew = new StringBuilder();
            pnew.setLength(bd.startspaces + bd.endspaces + bd.textlen);
            int pnew_idx = 0;
            y_current.y_array[y_idx++] = pnew;
            
            copy_spaces(pnew, pnew_idx, bd.startspaces);
            pnew_idx += bd.startspaces;
            
            mch_memmove(pnew, pnew_idx, Util.ml_get(lnum), bd.textstart,
                                                         bd.textlen);
            pnew_idx += bd.textlen;
            
            copy_spaces(pnew, pnew_idx, bd.endspaces);
        }
    } else {
        int start;
        int end;
        if(yanktype == MLINE) {
            start = G.curbuf.getLineStartOffset(lnum);
            end = G.curbuf.getLineEndOffset(yankendlnum);
        } else {
            start = oap.start.getOffset();
            end = oap.end.getOffset() + (oap.inclusive ? 1 : 0);
        }
        int length = end - start;
        StringBuilder reg = y_current.y_array[0];
        MySegment seg = G.curbuf.getSegment(start, length, null);
        reg.append(seg.array, seg.offset, seg.count); // DONE
        // bug #1724053 visual mode not capture \n after '$'
        // I guess the oap.inclusive should be trusted.
        // if(yanktype == MCHAR && length > 0
        //    	&& reg.charAt(reg.length()-1) == '\n') {
        //   reg.deleteCharAt(reg.length()-1);
        // }
    }
    
    // NEEDSWORK: if lines are made an array in the yank buffer
    //            then in some cases must append the current
    //            yank to the previous contents of yank buffer
    
    if (mess)			// Display message about yank?
    {
        if (yanktype == MCHAR && !oap.block_mode && yanklines == 1)
            yanklines = 0;
        // Some versions of Vi use ">=" here, some don't...
        if (yanklines >= G.p_report)
        {
            // redisplay now, so message is not deleted
            // NEEDSWORK: update_topline_redraw();
            Msg.smsg("" + yanklines + " line" + plural(yanklines) + " yanked");
        }
    }
    
    //
    //  set "'[" and "']" marks.
    //
    ViFPOS op_start = oap.start.copy();
    ViFPOS op_end = oap.end.copy();
    if(yanktype == MLINE && !oap.block_mode) {
        op_start.setColumn(0);
        // op_end.setColumn(MAXCOL); NEEDSWORK: need way to set ViMark to MAXCOL
        // put it on the newline
        op_end.setColumn(Util.lineLength(op_end.getLine()));
    }
    G.curbuf.b_op_start.setMark(op_start);
    G.curbuf.b_op_end.setMark(op_end);
    
    
    // If we were yanking to the '*' register, send result to clipboard.
    // If no register was specified, and "unnamed" in 'clipboard', make a copy
    // to the '*' register.
    
    boolean did_star = false;
    if(clip_star.avail
            && (isStarRegister(curr)
            || (!deleting && oap.regname == 0
            && clip_unnamed_union.contains(CB.UNNAMED))))
    {
        if(!isStarRegister(curr))
            // Copy the text from register 0 to the clipboard register.
            copy_yank_reg(y_regs.get(STAR_REGISTER));
        clip_star.clip_gen_set_selection();
        did_star = true;
    }
    
    // If we were yanking to the '+' register, send result to selection.
    // Also copy to the '*' register, in case auto-select is off.  But not when
    // 'clipboard' has "unnamedplus" and not "unnamed"; and not when
    // deleting and both "unnamedplus" and "unnamed".
    if(clip_plus.avail
            && (isPlusRegister(curr)
            || (!deleting && oap.regname == 0
            && clip_unnamed_union.contains(CB.UNNAMED_PLUS))))
    {
        if(!isPlusRegister(curr))
            copy_yank_reg(y_regs.get(PLUS_REGISTER));
        clip_plus.clip_gen_set_selection();
        
        if(!clip_autoselect_star
                && !clip_autoselect_plus
                && !(clip_unnamed_union.contains(CB.UNNAMED_PLUS)
                && clip_unnamed_union.size() == 1)
                && !(deleting && clip_unnamed_union.equals(unnamed_all))
                && !did_star
                && isPlusRegister(curr)) {
            copy_yank_reg(y_regs.get(STAR_REGISTER));
            clip_star.clip_gen_set_selection();
        }
    }
    return OK;
}

// copy_yank_reg is #ifdef FEAT_CLIPBOARD
/*
* Make a copy of the y_current register to register "reg".
*/
static void copy_yank_reg(Yankreg reg)
{
    reg.set(y_current, true);
}


/**
 * put contents of register "regname" into the text.
 * For ":put" command count == -1.
 * flags: PUT_FIXINDENT	make indent look nice
 *	  PUT_CURSEND	leave cursor after end of new text
 *
 * PUT_LINE   not supported (i'm just putting this comment in)
 * FIX_INDENT not supported, used by mouse and bracket print, [p
 */
public static void do_put(int regname_, int dir, int count, int flags)
{
    //StringBuilder        ptr;
    int                 ptr_idx;
    MySegment           oldp;
    StringBuilder        newp;
    int                 yanklen;
    int                 totlen = 0;
    int                 lnum;
    int                 col;
    int                 y_type;
    int                 y_size;
    
    int                 oldlen;
    int                 y_width;
    int                 vcol;
    int                 delcount;
    int                 incr = 0;
    int                 j;
    block_def           bd;
    
    char regname = (char)regname_;
    
    StringBuilder[] y_array;
    
    final ViFPOS cursor = G.curwin.w_cursor;
    
    // extra ?
    int			old_lcount = G.curbuf.getLineCount();
    
    // Adjust register name for "unnamed" in 'clipboard'.
    regname = adjust_clip_reg(regname);
    //if (regname == '*')
    //clip_get_selection();
    may_get_selection(regname);
    
    // NEEDSWORK: do_put: there are special registers like: '%', '#', ':',...
    // if (get_spec_reg(regname, &insert_string, &allocated, TRUE))
    
    if(G.False) {
        // This is the case where insert_string from get_spec_reg is non null
        // TODO: SPECIAL REGS NOT SUPPORTED
    } else {
        get_yank_register(regname, false);
        
        y_type = y_current.y_type;
        y_width = y_current.y_width;
        y_size = y_current.y_size;
        y_array = y_current.y_array;
    }
    
    if (y_type == MLINE) {
        if ((flags & PUT_LINE_SPLIT) != 0) {
            // "p" or "P" in Visual mode: split the lines to put the text in
            // between.
            // Lots of code was replaced by the following.
            // G.curwin.insertChar('\n');
            int currOffset = cursor.getOffset();
            G.curwin.insertNewLine(); // DONE
            //G.curwin.insertText(cursor.getOffset(), "\n");
            // back up the cursor so it is on the newline
            //cursor.set(tpos);
            G.curwin.w_cursor.set(currOffset);
            dir = FORWARD;
        }
        if ((flags & PUT_LINE_FORWARD) != 0) {
            /* Must be "p" for a Visual block, put lines below the block. */
            cursor.set(G.curbuf.b_visual_end);
            dir = FORWARD;
        }
        // b_op_start/end handle later
        //curbuf->b_op_start = curwin->w_cursor;  /* default for '[ mark */
        //curbuf->b_op_end = curwin->w_cursor;	/* default for '] mark */
    }
    
    if ((flags & PUT_LINE) != 0) // :put command or "p" in Visual line mode.
        y_type = MLINE;
    
    if (y_size == 0 || y_array == null) {
        Msg.emsg("Nothing in register "
                + (regname == 0 ? "\"" : transchar(regname)));
        return;
    }
    
    
    ViFPOS fpos = cursor.copy();
    int offset = fpos.getOffset();
    int length;
    //int new_offset;
    if(y_type == MCHAR) {
        if(dir == FORWARD) {
            // increment position, unless pointing at new line
            if(Util.getCharAt(offset) != '\n') { // DONE
                offset++;
            }
        }
    } else if(y_type == MLINE) {
        // adjust for folding
        lnum = fpos.getLine();
        MutableInt mi = new MutableInt(lnum);
        if(dir == BACKWARD)
            G.curwin.hasFolding(lnum, mi, null);
        else
            G.curwin.hasFolding(lnum, null, mi);
        lnum = mi.getValue();
        
        // vim: if(dir == FORWARD)
        // vim:    ++lnum;
        
        // vim for folding does: if(FORW) cursor.setLine(lnum-1) else setLine(lnum)
        fpos.setLine(lnum);
        
        // jVi uses offsets for the put, offset may have changed if folding
        offset = fpos.getOffset();
        if(dir == FORWARD) {
            offset = G.curbuf.getLineEndOffsetFromOffset(offset);
        } else {
            offset = G.curbuf.getLineStartOffsetFromOffset(offset);
        }
    }
    
    lnum = fpos.getLine();
    
    // block mode
    if (y_type == MBLOCK) {
        cursor.set(fpos); // NEEDSWORK: do the blockmode with shadowCursor
        int finishPositionColumn = cursor.getColumn();
        
        char c = gchar_pos(cursor);
        int endcol2 = 0;
        eatme(endcol2);
        
        MutableInt mi1 = new MutableInt();
        MutableInt mi2 = new MutableInt();
        if (dir == FORWARD && c != '\n') { // DONE
            getvcol(G.curwin, cursor, null, null, mi1);
            col = mi1.getValue();
            
            cursor.incColumn();
            ++col;
        } else {
            getvcol(G.curwin, cursor, mi1, null, mi2);
            col = mi1.getValue();
            endcol2 = mi2.getValue();
            eatme(endcol2);
        }
        
        bd = new block_def();
        bd.textcol = 0;
        int line = cursor.getLine();
        for (int i = 0; i < y_size; ++i, ++line) {
            int spaces;
            boolean shortline;
            
            bd.startspaces = 0;
            bd.endspaces = 0;
            vcol = 0;
            delcount = 0;
            
            // add a new line
            if(line > G.curbuf.getLineCount()) {
                G.curwin.insertText(
                        G.curbuf.getLineEndOffset(G.curbuf.getLineCount()), "\n"); // DONE
            }
            // get the old line and advance to the position to insert at
            oldp = Util.ml_get(line);
            oldlen = oldp.length() - 1; // -1 to ignore '\n' // DONE
            
            for (ptr_idx = 0; vcol < col && (c = oldp.charAt(ptr_idx)) != '\n'; ) { // DONE
                // Count a tab for what it's worth (if list mode not on)
                incr = lbr_chartabsize(c, vcol);
                vcol += incr;
                ptr_idx++;
            }
            bd.textcol = ptr_idx;
            
            shortline = vcol < col || (vcol == col && c == '\n'); // DONE
            
            if (vcol < col) // line too short, padd with spaces
            {
                bd.startspaces = col - vcol;
            } else if (vcol > col) {
                bd.endspaces = vcol - col;
                bd.startspaces = incr - bd.endspaces;
                --bd.textcol;
                delcount = 1;
                if (oldp.charAt(bd.textcol) != TAB) {
                    // Only a Tab can be split into spaces.  Other
                    // characters will have to be moved to after the
                    // block, causing misalignment.
                    delcount = 0;
                    bd.endspaces = 0;
                }
            }
            
            yanklen = y_array[i].length();//STRLEN(y_array[i]);
            
            // calculate number of spaces required to fill right side of block
            spaces = y_width + 1;
            for (j = 0; j < yanklen; j++)
                spaces -= lbr_chartabsize(y_array[i].charAt(j), 0);
            if (spaces < 0)
                spaces = 0;
            
            // insert the new text
            totlen = count * (yanklen + spaces) + bd.startspaces + bd.endspaces;
            newp = new StringBuilder(); //newp = alloc_check(totlen + oldlen + 1);
            newp.setLength(totlen + oldlen + 1);
            
            // copy part up to cursor to new line
            ptr_idx = 0;
            
            mch_memmove(newp, ptr_idx, oldp, 0, bd.textcol);
            ptr_idx += bd.textcol;
            ///may insert some spaces before the new text
            copy_spaces(newp, ptr_idx, bd.startspaces);
            ptr_idx += bd.startspaces;
            // insert the new text
            for (j = 0; j < count; ++j) {
                mch_memmove(newp, ptr_idx, y_array[i], 0, yanklen);
                ptr_idx += yanklen;
                
                // insert block's trailing spaces only if there's text behind
                if ((j < count - 1 || !shortline) && spaces != 0) {
                    copy_spaces(newp, ptr_idx, spaces);
                    ptr_idx += spaces;
                }
            }
            // may insert some spaces after the new text
            copy_spaces(newp, ptr_idx, bd.endspaces);
            ptr_idx += bd.endspaces;
            // move the text after the cursor to the end of the line.
            // '- 1' in following because don't want newline
            mch_memmove(newp, ptr_idx, oldp, bd.textcol + delcount,
                                         (oldlen - bd.textcol - delcount + 1 - 1));
            newp.setLength(STRLEN(newp));
            Util.ml_replace(line, newp);
            
            //++curwin.w_cursor.lnum;
            //if (i == 0)
            //  curwin.w_cursor.col += bd.startspaces;
            if(i == 0)
                finishPositionColumn += bd.startspaces;
        }
        
        // Set '[ mark.
        //curbuf->b_op_start = curwin->w_cursor;
        //curbuf->b_op_start.lnum = lnum;
        ViFPOS op_start = cursor.copy(); // to get something to work with
        op_start.set(lnum, finishPositionColumn);
        G.curbuf.b_op_start.setMark(op_start);
        
        // adjust '] mark
        //curbuf->b_op_end.lnum = curwin->w_cursor.lnum - 1;
        //curbuf->b_op_end.col = bd.textcol + totlen - 1;
        ViFPOS op_end = cursor.copy(); // to get something to work with
        {
        int len;
        int li = line - 1;
        int co = bd.textcol + totlen - 1;
        
        /* in Insert mode we might be after the NUL, correct for that */
        //len = (colnr_T)STRLEN(ml_get_curline());
        //if (curwin.w_cursor.col > len)
        //  curwin.w_cursor.col = len;
        len = Util.ml_get(li).length() - 1;
        if(co > len)
            co = len;
        op_end.set(li, co);
    }
        G.curbuf.b_op_end.setMark(op_end);
        
        if ((flags & PUT_CURSEND) != 0) {
            cursor.set(G.curbuf.b_op_end.getLine(),
                   G.curbuf.b_op_end.getColumn() + 1);
        } else {
            //curwin.w_cursor.lnum = lnum;
            cursor.set(lnum, finishPositionColumn);
        }
        
        
//        update_topline();
//        if (flags & PUT_CURSEND)
//            update_screen(NOT_VALID);
//        else
//            update_screen(VALID_TO_CURSCHAR);

    } else { // not block mode, fpos still in efect
        String s = y_array[0].toString();
        // NEEDSWORK: HACK for PUT_LINE flag, NOTE: should not need to do
        // (flags&PUT_LINE)!=0 since all MLINE should be terminated by \n
        if(y_type == MLINE // && (flags & PUT_LINE) != 0
                && s.length() != 0 && s.charAt(s.length()-1) != '\n') // DONE
            s += '\n'; // DONE
        if(count > 1) {
            StringBuilder sb = new StringBuilder(s);
            do {
                sb.append(s);
            } while(--count > 1);
            s = sb.toString();
        }
        length = s.length();
        G.curwin.insertText(offset, s);
        
        ViFPOS startFpos = G.curbuf.createFPOS(offset);
        ViFPOS endFpos = G.curbuf.createFPOS(offset + length - 1);
        G.curbuf.b_op_start.setMark(startFpos);
        G.curbuf.b_op_end.setMark(endFpos);
        
        // now figure out cursor position
        if(y_type == MCHAR && y_size == 1) {
            G.curwin.w_cursor.set(endFpos);
        } else {
            if((flags & PUT_CURSEND) != 0) {
                endFpos.set(endFpos.getOffset()+1);
                G.curwin.w_cursor.set(endFpos);
                if(y_type == MLINE) {
                } else {
                }
            } else if (y_type == MLINE) {
                beginline(startFpos, BL_WHITE | BL_FIX).copyTo(G.curwin.w_cursor);
            } else {
                G.curwin.w_cursor.set(startFpos);
            }
        }
    }
    
    msgmore(G.curbuf.getLineCount() - old_lcount);
    G.curwin.w_set_curswant = true;
    
    /* NEEDSWORK: if ((flags & PUT_CURSEND)
    && gchar_cursor() == NUL
    && curwin->w_cursor.col
    && !(restart_edit || (State & INSERT))) {
    --curwin->w_cursor.col;
    }
    */
}

private Register()
{
}
}
