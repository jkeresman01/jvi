package com.raelity.jvi.options;

import com.raelity.jvi.ViBuffer;
import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViOptionBag;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.TextView;
import com.raelity.jvi.core.Util;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implement ":se[t]".
 *
 * Options are either global or indirect, see the P_ XXX below An option
 * must be one or the other. Global options are static, an indirect option
 * is an instance variable in either G.curwin or G.curbuf. When a P_IND
 * variable is set, introspection is used to do the set.
 * <p>
 * In some cases, due to platform limitation, the same variable must be
 * set in all the instances, syncAllInstances(var) does that.
 */
public class SetColonCommand extends ColonCommands.AbstractColonAction
{
    private static final
            Logger LOG = Logger.getLogger(SetColonCommand.class.getName());
    
    @ServiceProvider(service=ViInitialization.class,
            path="jVi/init",
            position=10)
    public static class Init implements ViInitialization
    {
        @Override
        public void init()
        {
            SetColonCommand.init();
        }
    }
    
    private static void init()
    {
        ColonCommands.register("se", "set", new SetColonCommand(), null);
    }
    
    public static class SetCommandException extends Exception
    {
        
        public SetCommandException(String msg)
        {
            super(msg);
        }
    }
    
    // Scope of the option
    private enum S {
        P_GBL, // a global option
        P_WIN, // a per window option
        P_BUF; // a per buffer option
        
        boolean isLocal() {
            return this == P_WIN || this == P_BUF;
        }
        
        boolean isGlobal() {
            return this == P_GBL;
        }
        
        boolean isWin() {
            return this == P_WIN;
        }
        
        boolean isBuf() {
            return this == P_BUF;
        }
    }

    private enum OP {
        DFLT(""),       // DEFAULT if nothing specified, like ":set ic".
                        // Could be SHOW as in ":set isk" for non boolean.
        SHOW("?"),      // display option
        INV("inv"),     // ! invert boolean
        NO("no"),       // no<opt> set boolean false
        ASS("="),       // = assign
        ADD("+="),      // +=
        PRE("^="),      // ^=
        SUB("-="),      // -=
        ;

        private String token;

        OP(String token) {
            this.token = token;
        }

        public String getToken()
        {
            return token;
        }

        boolean isBooleanOp() {
            return this == INV || this == NO;
        }

        boolean isAnyAssign() {
            return this == ASS || this == ADD || this == PRE || this == SUB;
        }

        boolean isAssignOp() {
            return this == ADD || this == PRE || this == SUB;
        }

        boolean isShow() { return this == SHOW; }
        boolean isInv()  { return this == INV; }
        boolean isNo()   { return this == NO; }
        boolean isAss()  { return this == ASS; }
        boolean isAdd()  { return this == ADD; }
        boolean isPre()  { return this == PRE; }
        boolean isSub()  { return this == SUB; }
    }

    // option flags
    private enum F {
        COMMA,          // comma separated list
        NODUP,          // don't allow duplicate strings
        FLAGLIST,       // list of single-char flags
    }

    private static final Set<F> nullF = EnumSet.noneOf(F.class);
    
    private static class VimOption
    {
        
        String fullname; // option name
        String shortname; // option name
        S scope;
        // name of field and/or option
        String varName; // java variable name in curbuf or curwin
        String optName; // the jVi Option name.
        Set<F> flags;
        
        VimOption(String fullname, String shortname,
                               String varName, String optName,
                               S scope, Set<F> flags)
        {
            this.fullname = fullname;
            this.shortname = shortname;
            this.varName = varName;
            this.optName = optName;
            this.scope = scope;
            this.flags = flags;
        }

        boolean f(F f) {
            return flags.contains(f);
        }
    }
    
    private static VimOption[] vopts = new VimOption[]{
    new VimOption("expandtab",   "et",  "b_p_et",   null,
                  S.P_BUF, nullF),
    new VimOption("ignorecase",  "ic",  null,       Options.ignoreCase,
                  S.P_GBL, nullF),
    new VimOption("incsearch",   "is",  null,       Options.incrSearch,
                  S.P_GBL, nullF),
    new VimOption("hlsearch",    "hls", null,       Options.highlightSearch,
                  S.P_GBL, nullF),
    new VimOption("wrapscan",    "ws",  null,       Options.wrapScan,
                  S.P_GBL, nullF),
    new VimOption("number",      "nu",  "w_p_nu",   null,
                  S.P_WIN, nullF),
    new VimOption("shiftwidth",  "sw",  "b_p_sw",   Options.shiftWidth,
                  S.P_BUF, nullF),
    new VimOption("tabstop",     "ts",  "b_p_ts",   Options.tabStop,
                  S.P_BUF, nullF),
    new VimOption("softtabstop", "sts", "b_p_sts",  Options.softTabStop,
                  S.P_BUF, nullF),
    new VimOption("textwidth",   "tw",  "b_p_tw",   Options.textWidth,
                  S.P_BUF, nullF),
    new VimOption("wrap",        "",    "w_p_wrap", null,
                  S.P_WIN, nullF),
    new VimOption("linebreak",   "lbr", "w_p_lbr",  null,
                  S.P_WIN, nullF),
    new VimOption("list",        "",    "w_p_list", null,
                  S.P_WIN, nullF),
    new VimOption("scroll",      "scr", "w_p_scroll", null,
                  S.P_WIN, nullF),
    new VimOption("iskeyword",   "isk", "b_p_isk",  Options.isKeyWord,
                  S.P_BUF, EnumSet.of(F.COMMA, F.NODUP)),
    };
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        ColonEvent evt = (ColonEvent)e;
        parseSetOptions(evt.getArgs());
    }

    private static void error(String msg)
    {
        Msg.emsg(msg);
        Util.vim_beep();
    }
    
    public static void parseSetOptions(List<String> eventArgs)
    {
        if (eventArgs.isEmpty() ||
                eventArgs.size() == 1 && "all".equals(eventArgs.get(0))) {
            displayAllOptions();
            return;
        }
        LinkedList<String> args = new LinkedList<String>();
        // copy eventArgs into args, with possible fixup
        // ":set sw =4" is allowed, so if something starts with "="
        // then append it to the previous element
        int j = 0;
        for (int i = 0; i < eventArgs.size(); i++) {
            String arg = eventArgs.get(i);
            if (arg.startsWith("=") && args.size() > 0) {
                arg = args.removeLast() + arg;
            }
            args.addLast(arg);
        }
        for (String arg : args) {
            try {
                parseSetOption(arg);
            } catch (SetCommandException ex) {
                // error message given
                return;
            } catch (IllegalAccessException ex) {
                LOG.log(Level.SEVERE, null, ex);
                return;
            } catch (IllegalArgumentException ex) {
                LOG.log(Level.SEVERE, null, ex);
                return;
            }
        }
    }
    
    /**
     * This holds the results of parsing a set command
     */
    private static class VimOptionState
    {
        Class type;
        Object value;
        // used if type.isLocal()
        Field f;
        ViOptionBag bag;
        // used if regular option is provided
        Option opt;
        // Following not really option state, they are the
        // parse results when settigns options
        OP op;
        String stringValue;
    }

    // This is train of thought
    public static void parseSetOption(String arg)
    throws IllegalAccessException, SetCommandException
    {
        VimOptionState voptState = new VimOptionState();
        String voptName = arg; // assume split does nothing
        Object[] split = splitAssignment(arg);
        if(split != null) {
            voptName = (String)split[0];
            voptState.op = (OP)split[1];
            voptState.stringValue = (String)split[2];
        }
        if (voptName.startsWith("no")) {
            voptState.op = OP.NO;
            voptName = voptName.substring(2);
        } else if (voptName.startsWith("inv")) {
            voptState.op = OP.INV;
            voptName = voptName.substring(3);
        } else if (voptName.endsWith("!")) {
            voptState.op = OP.INV;
            voptName = voptName.substring(0, voptName.length() - 1);
        } else if (voptName.endsWith("?")) {
            voptState.op = OP.SHOW;
            voptName = voptName.substring(0, voptName.length() - 1);
        }
        if(voptState.op == null)
            voptState.op = OP.DFLT;
        VimOption vopt = null;
        for (VimOption v : vopts) {
            if (voptName.equals(v.fullname)
                    || voptName.equals(v.shortname) && !v.shortname.isEmpty()) {
                vopt = v;
                break;
            }
        }
        if (vopt == null) {
            String msg = "Unknown option: " + voptName;
            error(msg);
            throw new SetCommandException(msg);
        }
        if (determineOptionState(vopt, voptState) == null) {
            String msg = "Internal error: " + arg;
            error(msg);
            throw new SetCommandException(msg);
        }
        Object newValue = newOptionValue(arg, vopt, voptState);
        if (voptState.op.isShow()) {
            Msg.smsg(formatDisplayValue(vopt, voptState.value));
        } else {
            if (voptState.opt != null) {
                try {
                    voptState.opt.validate(newValue);
                } catch (PropertyVetoException ex) {
                    error(ex.getMessage());
                    throw new SetCommandException(ex.getMessage());
                }
            }
            
            if (vopt.scope.isLocal()) {
                voptState.f.set(voptState.bag, newValue);
                voptState.bag.viOptionSet(G.curwin, vopt.varName);
            } else { // isGlobal()
                voptState.opt.setValue(newValue.toString());
            }
        }
    }

    // Split on '=', '+=', '-=', '^='
    private static Object[] splitAssignment(String arg)
    {
        Pattern p = Pattern.compile("^([a-z]+)(=|\\+=|\\^=|-=)(.*)$");
        Matcher m = p.matcher(arg);
        if(m.matches()) {
            Object[] split = new Object[3];
            split[0] = m.group(1);
            split[2] = m.group(3);
            OP op = null;
            String sop = m.group(2);
            if("=".equals(sop)) {
                op = OP.ASS;
            } else if("+=".equals(sop)) {
                op = OP.ADD;
            } else if("-=".equals(sop)) {
                op = OP.SUB;
            } else if("^=".equals(sop)) {
                op = OP.PRE;
            }
            split[1] = op;

            return split;
        }

        return null;
    }
    
    /**
     * Set voptState with information about the argument vopt.
     * The info about the option is taken from curwin/curbuf.
     */
    private static VimOptionState determineOptionState(VimOption vopt,
                                                     VimOptionState voptState)
    {
        if(voptState == null)
            voptState = new VimOptionState();
        if (vopt.optName != null) {
            voptState.opt = Options.getOption(vopt.optName);
        }
        if (vopt.scope.isLocal()) {
            voptState.bag = vopt.scope.isWin() ? G.curwin : G.curwin.getBuffer();
            try {
                voptState.f = voptState.bag.getClass().getField(vopt.varName);
            } catch (SecurityException ex) {
                LOG.log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            if (voptState.f == null) {
                return null;
            }
            voptState.type = voptState.f.getType();
            // impossible to get exceptions
            try {
                voptState.value = voptState.f.get(voptState.bag);
            } catch (IllegalArgumentException ex) {
                LOG.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        } else if (vopt.scope.isGlobal()) {
            if (voptState.opt instanceof BooleanOption) {
                voptState.type = boolean.class;
                voptState.value = voptState.opt.getBoolean();
            } else if (voptState.opt instanceof IntegerOption) {
                voptState.type = int.class;
                voptState.value = voptState.opt.getInteger();
            } else if (voptState.opt instanceof StringOption) {
                voptState.type = String.class;
                voptState.value = voptState.opt.getString();
            }
        }
        return voptState;
    }
    
    // Most of the argument are class members
    private static Object newOptionValue(String arg, VimOption vopt,
                                         VimOptionState voptState)
    throws SetCommandException
    {
        Object newValue = null;
        if (voptState.type == boolean.class) {
            if (voptState.stringValue != null) {
                // like: ":set ic=val"
                String msg = "Unknown argument: " + arg;
                error(msg);
                throw new SetCommandException(msg);
            }
            if (!voptState.op.isShow()) {
                newValue =
                        voptState.op.isInv()
                        ? !((Boolean)voptState.value).booleanValue()
                        : voptState.op.isNo() ? false : true;
            }

        } else {
            // handle a number or a string
            if(voptState.op.isBooleanOp()) {
                String msg = "boolean op '" + voptState.op
                                + "' invalid for "
                                + voptState.type.getSimpleName()
                                + " option";
                error(msg);
                throw new SetCommandException(msg);
            }
            if (voptState.stringValue == null) {
                voptState.op = OP.SHOW;
            }
            if (!voptState.op.isShow()) {
                if(!voptState.op.isAnyAssign()) {
                    String msg = "Operation '" + voptState.op
                                    + "' invalid in this context";
                    error(msg);
                    throw new SetCommandException(msg);
                }
                if (voptState.type == int.class) {
                    try {
                        int oldValue = (Integer)voptState.value;
                        int val = Integer.parseInt(voptState.stringValue);
                        switch(voptState.op) {
                            case ASS: break; // val = val
                            case ADD: val = oldValue + val; break;
                            case SUB: val = oldValue - val; break;
                            case PRE: val = oldValue * val; break;
                        }
                        newValue = (Integer)val;
                    } catch (NumberFormatException ex) {
                        String msg = "Number required after '=': " + arg;
                        error(msg);
                        throw new SetCommandException(msg);
                    }
                } else if (voptState.type == String.class) {
                    if(!voptState.op.isAssignOp())
                        newValue = voptState.stringValue;
                    else
                        newValue = doStringAssignOp(arg, vopt, voptState);
                } else {
                    assert false : "Type " + voptState.type.getSimpleName()
                                    + " not handled";
                }
            }
        }
        return newValue;
    }

    private static String doStringAssignOp(String arg, VimOption vopt,
                                           VimOptionState voptState)
            throws SetCommandException
    {
        // See vim's option.c...
        String origval = (String)voptState.value;
        String newval = voptState.stringValue;
        boolean adding = voptState.op.isAdd();
        boolean prepending = voptState.op.isPre();
        boolean removing = voptState.op.isSub();

        // System.err.printf("doStringAssignOp '%s'\n\t%s\n\t%s\n",
        //                   voptState.op.getToken(),origval, newval);

        // locate newval[] in origval[] when removing it
        // and when adding to avoid duplicates
        int i = 0;
        int s = 0;
        if(removing || vopt.f(F.NODUP)) {
            i = newval.length();
            int bs = 0;
            for(s = 0; s < origval.length(); s++)
            {
                if((!vopt.f(F.COMMA)
                            || s == 0
                            || origval.charAt(s - 1) == ',' && (bs & 1) == 0)
                        && origval.substring(s).startsWith(newval)
                        && (!vopt.f(F.COMMA)
                            || origval.length() == s + i
                            || origval.charAt(s + i) == ',')
                ) {
                    break;
                }
                // Count backslashes.  Only a comma with an
                // even number of backslashes before it is
                // recognized as a separator
                if(s > 0 && origval.charAt(s - 1) == '\\')
                    ++bs;
                else
                    bs = 0;
            }
            
            /* do not add if already there */
            if ((adding || prepending) && s < origval.length())
            {
                prepending = false;
                adding = false;
                newval = origval;
            }
        }
            
        // concatenate the two strings; add a ',' if
        // needed
        if (adding || prepending)
        {
            boolean comma = vopt.f(F.COMMA) && !origval.isEmpty()
                                             && !newval.isEmpty();
            if(adding) {
                newval = origval + (comma ? "," : "") + newval;
            } else {
                newval += (comma ? "," : "") + origval;
            }
        }

        // Remove newval[] from origval[]. (Note: "i" has
        // been set above and is used here).
        if (removing)
        {
            newval = origval;
            if(s < origval.length())
            {
                // may need to remove a comma
                if(vopt.f(F.COMMA)) {
                    if(s == 0) {
                        // include comma after string
                        if(s + i < origval.length()
                                && origval.charAt(s + i) == ',')
                            ++i;
                    } else {
                        // include comma before string
                        --s;
                        ++i;
                    }
                }
                newval = origval.substring(0, s)
                            + origval.substring(s + i);
            }
        }

        if(vopt.f(F.FLAGLIST))
        {
            // remove flags that appear twice
            StringBuilder sb = new StringBuilder(newval);
            for(s = 0; s < sb.length(); s++) {
                if((!vopt.f(F.COMMA) || sb.charAt(s) != ',')
                    && Util.vim_strchr_cs(sb, s + 1, sb.charAt(s)) >= 0)
                {
                    sb.deleteCharAt(s);
                    --s; // since it is about to be incremented
                }
            }
            if(sb.length() != newval.length())
                newval = sb.toString();
        }

        ///// STRANGE, may need some day...
        ///// if (save_arg != NULL)   /* number for 'whichwrap'
        /////     arg = save_arg;

        // System.err.println("\t"+newval);

        return newval;
    }
    
    private static String formatDisplayValue(VimOption vopt, Object value)
    {
        String v = "";
        if (value instanceof Boolean) {
            v = (((Boolean)value).booleanValue() ? "  " : "no") + vopt.fullname;
        } else if (value instanceof Integer
                || value instanceof String) {
            v = vopt.fullname + "=" + value;
        } else {
            assert false : value.getClass().getSimpleName() + " not handled";
        }
        return v;
    }
    
    private static void displayAllOptions()
    {
        ViOutputStream osa =
                ViManager.createOutputStream(null, ViOutputStream.OUTPUT, null);
        for (VimOption vopt : vopts) {
            VimOptionState voptState = determineOptionState(vopt, null);
            osa.println(formatDisplayValue(vopt, voptState.value));
        }
        osa.close();
    }
    
    private static VimOption getVopt(String varName)
    {
        VimOption v = null;
        for(VimOption vopt : vopts) {
            if(vopt.varName != null && vopt.varName.equals(varName)) {
                v = vopt;
                break;
            }
        }
        return v;
    }
    
    /**
     * Some options (for example w_p_wrap) are per window; however the platform
     * (NB) may support it only per buffer. So if the user changes it with set,
     * then the variable in any other window that shares the buffer must be
     * updated. The value to sync is taken from curwin.
     * @param varName the variable to sync
     * @param buf the buffer to check for
     */
    public static void syncTextViewInstances(String varName, TextView tv)
    {
        assert tv == G.curwin; // NEEDSWORK: since determine option state assumes
        VimOption vopt = getVopt(varName);
        // if var is not window then nothing to do
        if(!vopt.scope.isWin())
            return;
        
        ViBuffer buf = tv.getBuffer();
        
        for(ViTextView tv01 : ViManager.getFactory().getViTextViewSet()) {
            if(tv01.getBuffer() != buf || tv01 == tv)
                continue;
            if(G.dbgOptions)
                System.err.println("syncInstances: " + varName + " in " + tv01);
            setLocalOption(tv01, vopt);
        }
    }
    
    public static void syncAllInstances(String varName)
    {
        VimOption vopt = getVopt(varName);
        if(vopt.scope.isLocal()) {
            Set<? extends ViOptionBag> set =
                    vopt.scope.isWin()
                    ? ViManager.getFactory().getViTextViewSet()
                    : ViManager.getFactory().getBufferSet();
            for(ViOptionBag bag : set) {
                if(G.dbgOptions) {
                    System.err.printf("syncInstances: %s in %s\n",
                                      varName, bag);
                }
                setLocalOption(bag, vopt);
            }
        }
    }
    
    private static void setLocalOption(ViOptionBag bag, VimOption vopt)
    {
        VimOptionState voptState = determineOptionState(vopt, null);
        try {
            voptState.f.set(bag, voptState.value);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
