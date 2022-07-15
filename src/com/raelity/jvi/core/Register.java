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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.google.common.eventbus.Subscribe;

import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.Ops.block_def;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.core.lib.Constants.CBU;
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
import static com.raelity.jvi.core.Misc01.*;
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
    /** How to make a copy of a yank register */
    enum Access {
    CLEAR_ORIG,
    COPY,
    READ_ONLY
    }

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
                    byte type = (byte)prefs.getInt(TYPE, MCHAR);
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
static final List<Yankreg> y_regs = Collections.unmodifiableList(Arrays.asList(initialYankRegs()));

/** allocate all the named registers. Some simplification */
private static Yankreg[] initialYankRegs()
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
     * A Yankreg is essentially an array/list of text extracted
     * from the editor document. There are methods for manipulating
     * the array and adding/removing text.
     */
    final static class Yankreg
    {
    final char y_name;

    // NOTE: if a field is added, make sure to fixup this.set(Yankreg)

    /** This field is cast as ArrayList */
    private List<String> y_array;
    private byte y_type;
    private int y_width;
    
    public Yankreg()
    {
        this('\uffff');
    }
    
    public Yankreg(char y_name)
    {
        this.y_name = y_name;
    }
    
    // Default is list with single empty element.
    private void clear() {
        y_type = 0;
        y_width = 0;
        y_array = null;
    }

    /** Setup empty List with capacity for nLines */
    private void alloc(int nLines)
    {
        init_y_array(nLines);
    }

    /** Leave the list elements intact, but make sure there's room for more */
    private void realloc(int nLines)
    {
        if(y_array == null)
            y_array = new ArrayList<>(nLines + 5);
        else
            ((ArrayList<String>)y_array).ensureCapacity(nLines + 5);
    }

    /** add an empty storage list with capacity for specified nLines. */
    private void init_y_array(int nLines)
    {
        // If the list is "big", start with a new one
        if(y_array == null || y_array.size() > nLines + 50)
            y_array = new ArrayList<>(nLines + 5);
        else {
            y_array.clear();
            realloc(nLines);
        }
    }

    private List<String> getArray()
    {
        if(y_array == null)
            init_y_array(0);
        return y_array;
    }

    int getSize()
    {
        return y_array == null ? 0 : y_array.size();
    }

    byte getType()
    {
        return y_type;
    }
    
    boolean isEmpty() {
        return y_array == null || y_array.isEmpty();
    }
    
    /**
     * Append '\n' to each line, except the last; if not MCHAR
     * then also append '\n' at the end.
     * @return all the contents as a single string, empty string if empty.
     */
    String getAll()
    {
        if(isEmpty())
            return "";

        ListIterator<String> it = y_array.listIterator();
        StringBuilder sb = new StringBuilder();

        while(it.hasNext()) {
            sb.append(it.next());
            if(it.hasNext())
                sb.append('\n'); // DONE
        }
        if(y_type != MCHAR)
                sb.append('\n'); // DONE
        return sb.toString();
    }

    /** append to last element */
    private void append(String s)
    {
        getArray();
        if(y_array.isEmpty())
            y_array.add(s);
        else {
            int idx = y_array.size() - 1;   // last line in yreg
            y_array.set(idx, y_array.get(idx) + s);
        }
    }
    
    /**
     * Return a yankreg with the same contents as this yankreg. If fCopy is
     * set then create a copy of the string data, otherwise copy the data
     * and clear the data in the original.
     * <p>
     * readOnly has meaning only if fCopy is true.
     */
    private Yankreg copy(Access access)
    {
        Yankreg reg = new Yankreg();
        reg.set(this, access);
        return reg;
    }

    /**
     * Move the contents of the argument Yankreg into this and clear/copy the
     * argument reg's data as specified by Access.
     */
    private void set(Yankreg reg, Access access)
    {
        if(reg.isEmpty()) {
            clear();
            return;
        }
        y_type = reg.y_type;
        y_width = reg.y_width;
        switch(access) {
        case CLEAR_ORIG:
            y_array = reg.y_array;
            reg.clear();
            break;
        case READ_ONLY:
            y_array = Collections.<String>unmodifiableList(reg.y_array);
            break;
        case COPY:
            @SuppressWarnings("unchecked")
            List<String> t
                    = (List<String>)((ArrayList<String>)reg.y_array).clone();
            y_array = t;
            break;
        }
    }

    // Split the text into an array of lines/String.
    // Also used by Clipboard.
    void setData(CharSequence seg, int _type) {
        byte type = (byte)_type;
        List<String> l = getArray();
        l.clear();
        int idx = 0; 
        int startIndex;
        int twidth = 0;
        //int lines = 0;
        while(idx < seg.length()) {
            startIndex = idx;
            while(idx < seg.length() && seg.charAt(idx) != '\n')
                idx++;
            l.add(seg.subSequence(startIndex, idx).toString());
            //lines++;
            if(idx - startIndex > twidth)
                twidth = idx - startIndex;
            idx++;
        }

        y_array = l;
        y_type = type;
            
        twidth--;  // WISH I NEW WHY THIS IS NEEDED, see vim's str_to_reg

        if(type == MBLOCK)
            y_width = twidth;
        else
            y_width = 0;
    }

    @Override
    public String toString()
    {
        final int limit = 30;
        String s = getAll();
        return sf("Yankreg{'%c' %d (%d,@%s) \"%s\"}",
                  y_name, y_type, y_width,
                  y_array == null ? y_array : y_array.size(),
                  debugString(s.length() > limit
                              ? (s.substring(0, limit) + "...") : s));
    }
    
    } // END CLASS Yankreg ==================================================

/**
 * Map yankreg index to regname string, this is like an inverse mapping
 * @param i yankreg index
 * @return the regname as a string
 */
static char get_register_name(int i)
{
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
                           char regname, String arg, boolean skip_esc)
{
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
public static boolean valid_yank_reg(char regname, boolean writing)
{
    if (regname > '~')
        return false;
    return Misc01.isalnum(regname)
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
static boolean get_yank_register(char regname, boolean writing)
{
    char    i;
    boolean ret = false;
    
    y_append = false;
    if (((regname == 0 && !writing) || regname == '"') && y_previous != null) {
        y_current = y_previous;
        return ret;
    }
    i = regname;
    if (isdigit(i))
        i -= '0';
    else if (islower(i))
        i -= 'a' - 10;
    else if (isupper(i)) {
        i -= 'A' - 10;
        y_append = true;
    } else if (regname == '-')
        i = DELETION_REGISTER;
    // When clipboard is not available, use register 0 instead of '*'/'+'
    else if(isValidCb(name2Cb(regname))) {
        i = (char)name2CbIdx(regname);
        ret = true;
    } else		/* not 0-9, a-z, A-Z or '-': use register 0 */
        i = 0;
    y_current = y_regs.get(i);
    if (writing)	/* remember the register we write into for do_put() */
        y_previous = y_current;
    return ret;
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
static Yankreg get_register(char name, Access access)
{
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
    // readonly for following?
    Yankreg reg = y_current.copy(access);
    return reg;
}

/**
* Return the value, as a string, of the named register;
* these are for i_^R, wonder if they are grouped/used like this elsewhere
* @param name register name
* @return the string value
*/
static String get_register_value(char regname)
{
    String val = null;
    regname = may_get_selection(regname);
    
    if(regname >= 'a' && regname <= 'z' || "\"-+*\000".indexOf(regname) >= 0) {
        Yankreg reg = get_register(regname, Access.READ_ONLY);
        if (!reg.isEmpty())
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
static void put_register(char name, Yankreg reg)
{
    get_yank_register(name, false);
    y_current.set(reg, Access.CLEAR_ORIG); // reg's data/pointers is cleared.
    
    /* Send text written to clipboard register to the clipboard. */
    may_set_selection();
}

static char	do_record_regname;
/**
 * start or stop recording into a yank register
 *
 * return FAIL for failure, OK otherwise
 */
static int do_record(char c)
{
    Yankreg old_y_previous, old_y_current;
    int		retval;
    
    if ( ! G.Recording) {	    // start recording
        // registers 0-9, a-z and " are allowed
        if (c > '~' || (!Misc01.isalnum(c) && c != '"'))
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
 * <p>
 * Used for do_record
 * </p>
 *
 * @return FAIL for failure, OK otherwise
 */
static private int stuff_yank(char regname, String s)
{
    // check for read-only register
    if (regname != 0 && !valid_yank_reg(regname, true)) {
        return FAIL;
    }
    if (regname == '_') {		    // black hole: don't do anything
        return OK;
    }
    get_yank_register(regname, true);
    if (y_append && !y_current.isEmpty()) {
        y_current.append(s);
    } else {
        free_yank_all();
        y_current.setData(s, MCHAR); // used to be MLINE, why? (vim comment)
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
static int do_execreg(char regname, boolean colon, boolean addcr)
{
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
        if (ins_typebuf(y_current.y_arrayCCC[i], remap, 0, true) == FAIL)
        return FAIL;
        if (colon && ins_typebuf(":", remap, 0, true) == FAIL)
        return FAIL;
        }
        ****************************************************************/
        // Just roll our own for jvi
        String s1 = y_current.getAll();
        if(addcr) {
            if(s1.length() == 0 || s1.charAt(s1.length()-1) != '\n') { // DONE
                s1 += '\n'; // DONE
            }
        }
        //
        // NEEDSWORK: if(colon) put ":" begin of each line
        //
        if(ins_typebuf_redo(s1, remap, 0, true) == FAIL) {
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
    int		retval = OK;
    
    //    /*
    //     * It is possible to get into an endless loop by having CTRL-R a in
    //     * register a and then, in insert mode, doing CTRL-R a.
    //     * If you hit CTRL-C, the loop will be broken here.
    //     */
    //    ui_breakcheck();
    //    if (got_int)
    //	return FAIL;
    
    // check for valid regname
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
    else				// name or number register
    {
        if(get_yank_register(regname, false))
            literally = true;
        if(y_current.isEmpty())
            retval = FAIL;
        else
        {
            for(Iterator<String> it = y_current.getArray().iterator();
                    it.hasNext();) {
                String s = it.next();
                if (literally)
                    stuffescaped(s);
                else {
                    Edit.checkForStuffReadbuf(s, "i_CTRL-R");
                    stuffReadbuff(s);
                }
                // Insert a newline between lines and after last line if
                // y_type is MLINE.
                if (y_current.y_type == MLINE || it.hasNext())
                    stuffcharReadbuff('\n');
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
        y_regs.get(n).set(y_regs.get(n - 1), Access.CLEAR_ORIG);

    y_current = y_regs.get(1);
    // y_append check in VIM9, earlier just did the assign to y_previous
    if(!y_append)
        y_previous = y_current;
    y_regs.get(1).clear();
    
}

/**
 * Free up storage associated with current yank buffer.
 */
static void free_yank_all()
{
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
    //int			y_idx = 0;	// index in y_arrayCCC[]
    Yankreg		curr;		// copy of y_current
    // char_u		**new_ptr;
    int			lnum;		// current line number
    // int			j;
    // int			len;
    byte		yanktype = (byte)oap.motion_type;
    int			yanklines = oap.line_count;
    int			yankendlnum = oap.end.getLine();
    // char_u		*p;
    StringBuilder        pnew;
    block_def		bd;
    
    // check for read-only register
    if (oap.regname != 0 && !valid_yank_reg(oap.regname, true))
    {
        beep_flush();
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
    
    y_current.alloc(yanklines);
    y_current.y_type = yanktype;   // set the yank register type
    y_current.y_width = 0;
    
    lnum = oap.start.getLine();
    
    if (oap.block_mode) {
        // Visual block mode
        y_current.y_type = MBLOCK;        /* set the yank register type */
        y_current.y_width = oap.end_vcol - oap.start_vcol;
        
        if (  G.curwin.w_curswant == MAXCOL && y_current.y_width > 0)
            y_current.y_width--;
        bd = new block_def();
        for ( ; lnum <= yankendlnum; ++lnum)
        {
            block_prep(oap, bd, lnum, false);
            
            pnew = new StringBuilder();
            pnew.setLength(bd.startspaces + bd.endspaces + bd.textlen);
            int pnew_idx = 0;
            
            copy_spaces(pnew, pnew_idx, bd.startspaces);
            pnew_idx += bd.startspaces;
            
            mch_memmove(pnew, pnew_idx, ml_get(lnum), bd.textstart,
                                                         bd.textlen);
            pnew_idx += bd.textlen;
            
            copy_spaces(pnew, pnew_idx, bd.endspaces);
            y_current.y_array.add(pnew.toString());
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
        MySegment seg = G.curbuf.getSegment(start, length, null);

        if(!y_current.isEmpty())
            Exceptions.printStackTrace(new Exception("Yankreg not size() 1"));
        // MCHAR/MLINE save the yank, may be an append
        y_current.setData(seg, yanktype);

        // bug #1724053 visual mode not capture \n after '$'
        // I guess the oap.inclusive should be trusted.
        // if(yanktype == MCHAR && length > 0
        //    	&& reg.charAt(reg.length()-1) == '\n') {
        //   reg.deleteCharAt(reg.length()-1);
        // }
    }

    if(curr != y_current) {
        if (yanktype == MLINE)	// MLINE overrides MCHAR and MBLOCK
            curr.y_type = MLINE;

        int y_idx;      // index into new block
	// Concatenate the last line of the old block with the first line of
	// the new block, unless being Vi compatible.
        // jVi: don't consider vi compat
	//if (curr->y_type == MCHAR && vim_strchr(p_cpo, CPO_REGAPPEND) == NULL)
	if (curr.y_type == MCHAR)
	{
            curr.append(y_current.y_array.get(0));
            y_idx = 1;
        } else
            y_idx = 0;
        // add any remaining lines from y_current to curr
        curr.y_array.addAll(y_current.y_array.subList(y_idx, y_current.getSize()));

        y_current = curr;
    }
    
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
        op_end.setColumn(lineLength(op_end.getLine()));
    }
    G.curbuf.b_op_start.setMark(op_start);
    G.curbuf.b_op_end.setMark(op_end);
    
    // If we were yanking to the '*' register, send result to clipboard.
    // If no register was specified, and "unnamed" in 'clipboard', make a copy
    // to the '*' register.
    // v7.4.396 added unnamed|unnamed_saved
    boolean did_star = false;
    if(clip_star.avail && (isStarRegister(curr)
            || (!deleting && oap.regname == 0
                && clip_unnamed_union.contains(CBU.UNNAMED))))
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
    if(clip_plus.avail && (isPlusRegister(curr)
            || (!deleting && oap.regname == 0
                && clip_unnamed_union.contains(CBU.UNNAMED_PLUS))))
    {
        if(!isPlusRegister(curr))
            copy_yank_reg(y_regs.get(PLUS_REGISTER));
        clip_plus.clip_gen_set_selection();
        
        if(!clip_autoselect_star
                && !clip_autoselect_plus
                && !(clip_unnamed_union.contains(CBU.UNNAMED_PLUS)
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
    reg.set(y_current, Access.COPY);
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
    
    int                 oldlen;
    int                 vcol;
    int                 delcount;
    int                 incr = 0;
    int                 j;
    block_def           bd;
    
    char regname = (char)regname_;
    
    final ViFPOS cursor = G.curwin.w_cursor;
    
    int			old_lcount = G.curbuf.getLineCount();
    
    // Adjust register name for "unnamed" in 'clipboard'.
    regname = adjust_clip_reg(regname);
    may_get_selection(regname);
    
    // NEEDSWORK: do_put: there are special registers like: '%', '#', ':',...
    // if (get_spec_reg(regname, &insert_string, &allocated, TRUE))
    
    Yankreg yreg;
    if(G.False) {
        // This is the case where insert_string from get_spec_reg is non null
        // TODO: SPECIAL REGS NOT SUPPORTED

        // IF want to support this, need to deal with yreg.
        yreg = null;
    } else {
        get_yank_register(regname, false);

        yreg = y_current.copy(Access.READ_ONLY);
    }
    
    assert yreg != null;
    if (yreg.y_type == MLINE) {
        if ((flags & PUT_LINE_SPLIT) != 0) {
            // "p" or "P" in Visual mode: split the lines to put the text in
            // between.
            // Lots of code was replaced by the following.
            int currOffset = cursor.getOffset();
            G.curwin.insertNewLine(); // DONE
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
        yreg.y_type = MLINE;
    
    if (yreg.isEmpty()) {
        Msg.emsg("Nothing in register "
                + (regname == 0 ? "\"" : transchar(regname)));
        return;
    }
    
    ViFPOS fpos = cursor.copy();
    int offset = fpos.getOffset();
    int length;
    //int new_offset;
    if(yreg.y_type == MCHAR) {
        if(dir == FORWARD) {
            // increment position, unless pointing at new line
            if(getCharAt(offset) != '\n') { // DONE
                offset++;
            }
        }
    } else if(yreg.y_type == MLINE) {
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
    if (yreg.y_type == MBLOCK) {
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
        for (int i = 0; i < yreg.getSize(); ++i, ++line) {
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
            oldp = ml_get(line);
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
            
            yanklen = yreg.y_array.get(i).length();//STRLEN(y_array[i]);
            
            // calculate number of spaces required to fill right side of block
            spaces = yreg.y_width + 1;
            for (j = 0; j < yanklen; j++)
                spaces -= lbr_chartabsize(yreg.y_array.get(i).charAt(j), 0);
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
                mch_memmove(newp, ptr_idx, yreg.y_array.get(i), 0, yanklen);
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
            ml_replace(line, newp);
            
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
            len = ml_get(li).length() - 1;
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
        // Use getAll
        //String s = yreg.y_array.get(0);
        String s = yreg.getAll();
        // NEEDSWORK: HACK for PUT_LINE flag, NOTE: should not need to do
        // (flags&PUT_LINE)!=0 since all MLINE should be terminated by \n
        if(yreg.y_type == MLINE // && (flags & PUT_LINE) != 0
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
        // TODO: IS THIS CORRECT? DOESN't LOOK LIKE IT
        if(yreg.y_type == MCHAR && yreg.getSize() == 1) {
            G.curwin.w_cursor.set(endFpos);
        } else {
            if((flags & PUT_CURSEND) != 0) {
                endFpos.set(endFpos.getOffset()+1);
                G.curwin.w_cursor.set(endFpos);
                if(yreg.y_type == MLINE) {
                } else {
                }
            } else if (yreg.y_type == MLINE) {
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
