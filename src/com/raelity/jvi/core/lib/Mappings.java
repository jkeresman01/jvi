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

import com.raelity.jvi.options.OptUtil;
import java.util.ListIterator;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.GetChar;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.lib.Wrap;
import com.raelity.jvi.manager.ViManager;
import com.raelity.text.TextUtil;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public final class Mappings {
    private static Mappings INSTANCE;

    private Map<Character, List<Mapping>> mappings
            = new HashMap<Character, List<Mapping>>();
    private static WeakReference<List<Mapping>> refDefaultMappings;
    private static WeakReference<Map<EqLower, Character>> refMapCommandSpecial;
    private static WeakReference<Map<Character, String>> refReverseMapCommandSpecial;

    /**
     * Only the first created Mappings will register itself as the
     * colon commands. If there is some reason to create another
     * Mappings then it will not be initialized with either the
     * default mappings or the user specified options; see reinitMappings.
     */
    public Mappings()
    {
        if(INSTANCE == null) {
            registerCommands();
            initMappings();
        }
    }

    public void reinitMappings()
    {
        initMappings();
    }

    private void initMappings()
    {
        Wrap<String> emsg = new Wrap<String>();
        List<Mapping> mapCommands = Mappings.parseMapCommands(
                OptUtil.getOption(Options.mapCommands).getValue(),
                emsg);
        if(mapCommands != null)
            saveMappings(mapCommands);
        else
            Logger.getLogger(GetChar.class.getName())
                    .log(Level.SEVERE, null, new Exception(emsg.getValue()));
    }

    private void registerCommands()
    {
        ColonCommands.AbstractColonAction cmd = new MapColonCommand();

        // EnumSet.of(CcFlag.NO_PARSE)

        ColonCommands.register("map", "map",  cmd, null);
        ColonCommands.register("nm",  "nmap", cmd, null);
        ColonCommands.register("vm",  "vmap", cmd, null);
        ColonCommands.register("om",  "omap", cmd, null);
        ColonCommands.register("pm",  "pmap", cmd, null);

        ColonCommands.register("no",  "noremap",  cmd, null);
        ColonCommands.register("nn",  "nnoremap", cmd, null);
        ColonCommands.register("vn",  "vnoremap", cmd, null);
        ColonCommands.register("ono", "onoremap", cmd, null);
        ColonCommands.register("pn",  "pnoremap", cmd, null);

        ColonCommands.register("unm", "unmap",  cmd, null);
        ColonCommands.register("nun", "nunmap", cmd, null);
        ColonCommands.register("vu",  "vunmap", cmd, null);
        ColonCommands.register("ou",  "ounmap", cmd, null);
        ColonCommands.register("pun", "punmap", cmd, null);

    }

    private class MapColonCommand
    extends ColonCommands.AbstractColonAction
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ColonEvent cev = (ColonEvent) e;
            Emsgs emsg = new Emsgs();
            Mapping m = parseMapCommand(cev.getCommandLine(), emsg, true);
            if(m != null && m.isUnmap && !containsMappings(m.lhs.charAt(0))) {
                emsg.error().append("No such mapping");
                m = null;
            }
            if(m != null)
                putMapping(m);
            else if(emsg.length() != 0)
                Msg.emsg(emsg.toString());     // also calls flushbuff
        }

    }

    /**
     * Convert the match to a char.
     * @return null if problem else translated char
     */
    private static Character tranlateMapCommandChar(
            Matcher m, boolean is_rhs, String orig, Emsgs emsg)
    {
        if(false) {
            System.err.println("region: '"
                    +  orig.substring(m.start())
                    + "' match: '" + m.group() + "'");
            for(int i = 1; i <= m.groupCount(); i++) {
                System.err.println("\t" + m.group(i));
            }
        }

        // the groups in mapSeqPattern
        final int g_char = 1;
        final int g_ctrl = 2;
        final int g_modif = 3; // possible only if g_spec match
        final int g_spec = 4;

        char c = 0;
        String s;
        if((s = m.group(g_char)) != null)
            c = s.charAt(0);
        else if((s = m.group(g_ctrl)) != null)
            c = (char)(s.charAt(0) & ~0x60); // convert both upper/lower case
        else if((s = m.group(g_spec)) != null) {
            int modifiers = 0;
            c = getMapCommandSpecial().get(new EqLower(s));
            s = m.group(g_modif);

            if(s != null) {
                if(s.equals("C") || s.equals("c")) {
                    modifiers |= CTRL;
                } else if(s.equals("S") || s.equals("s")) {
                    modifiers |= SHFT;
                } else {
                    Logger.getLogger(GetChar.class.getName())
                            .log(Level.SEVERE, null,
                            new Exception("unknown modifier: " + s));
                }
                if(isVIRT(c)) {
                    c |= (modifiers << MODIFIER_POSITION_SHIFT);
                } else {
                    emsg.error().append("Ctrl/Shft not supported with '<")
                            .append(m.group(g_spec)).append(">'\n");
                    return null;
                }
            }
        }

        return c;
    }

    private static WeakReference<Matcher> refMapCharsMatcher;

    private static Matcher getMapCharsMatcher()
    {
        Matcher mapCharsMatcher;

        if(refMapCharsMatcher == null
                || (mapCharsMatcher = refMapCharsMatcher.get()) == null) {
            //
            // a char is matched like:
            //          [!-~&&[^\\<]]      all printables, except \, < and space
            //          <C-[@-_&&[^\\\\]]> all real control chars, except Ctrl-\
            //          <special>          list of special chars
            //          <C-special>        ctrl of special chars
            //          <S-special>        shft of special chars
            // NOTE: special look like: key1|key2|key3
            // char: [!-~&&[^\\<]] | <C-[@-_]> | <([SC]-)?(special)>
            // a line is like: noremap char char+
            //
            String pat = "([!-~&&[^\\\\<]])"
                    + "|<C-([@-_&&[^\\\\]])>"
                    + "|<(?:([SC])-)?(special)>";
            // Make a string of all the special words we match.
            StringBuilder sb = new StringBuilder();
            for(EqLower k : getMapCommandSpecial().keySet()) {
                sb.append(k.toString()).append("|");
            }
            sb.deleteCharAt(sb.length() - 1);
            // plug all the special chars into the pattern
            pat = pat.replace("special", sb.toString());

            Pattern mapCharsPattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
            mapCharsMatcher = mapCharsPattern.matcher("");
            refMapCharsMatcher = new WeakReference<Matcher>(mapCharsMatcher);
        }
        return mapCharsMatcher;
    }

    /** should already have verified that cmd is supported */
    private static int get_map_mode(Wrap<String>cmdp, boolean forceit)
    {
        String cmd = cmdp.getValue();
        int p = 0;
        int mode = 0;
        char modec = cmd.charAt(p++);
        if (modec == 'i')
            mode = INSERT;				   /* :imap */
        else if (modec == 'c')
            mode = CMDLINE;				   /* :cmap */
        else if (modec == 'n' && cmd.charAt(p) != 'o') /* avoid :noremap */
            mode = NORMAL;				   /* :nmap */
        else if (modec == 'v')
            mode = VISUAL;				   /* :vmap */
        else if (modec == 'o')
            mode = OP_PENDING;			   /* :omap */
        else if (modec == 'p')
            mode = PLATFORM;			   /* :omap */
        else
        {
            --p;
            if (forceit)
                mode = INSERT + CMDLINE;		/* :map ! */
            else
                mode = VISUAL + NORMAL
                        + OP_PENDING + PLATFORM;/* :map */
        }

        cmdp.setValue(cmd.substring(p));
        return mode;
    }

    //
    // Commands are looked up, but that only resolves abbreviations.
    // Still need to know command is a valid map-command.
    //
    private static boolean supportedMapCommand(String cmd)
    {
        return   cmd != null
                && (
                false
                | "map".equals(cmd)
                | "nmap".equals(cmd)
                | "vmap".equals(cmd)
                | "omap".equals(cmd)
                | "pmap".equals(cmd)
                | "noremap".equals(cmd)
                | "nnoremap".equals(cmd)
                | "vnoremap".equals(cmd)
                | "onoremap".equals(cmd)
                | "pnoremap".equals(cmd)
                | "unmap".equals(cmd)
                | "nunmap".equals(cmd)
                | "vunmap".equals(cmd)
                | "ounmap".equals(cmd)
                | "punmap".equals(cmd)
                )
                ;
    }

    /**
     * Parse the mapping command.
     * If emsgs is not changed, then no error occurred.
     * <p/>
     * NEEDSWORK: this only prints to the global mappings, see printMappings
     * @param line
     * @param emsgs
     * @return null if no mapping on line or error
     */
    static Mapping parseMapCommand(String line, Emsgs emsg, boolean printOk)
    {
        Matcher matcher = getMapCharsMatcher();
        StringBuilder rhs = new StringBuilder();
        int initialEmsgs = emsg.length();

        List<String> fields = TextUtil.tokens(line);

        if(fields.isEmpty() || fields.get(0).startsWith("\""))
            return null;
        if(fields.size() > 3) {
            emsg.error().append("too many fields\n");
            return null;
        }

        // NEEDSWORK: forceit/'!' handling
        String cmd = fields.get(0);
        if(!supportedMapCommand(ColonCommands.getFullCommandName(cmd))) {
            emsg.error().append("\"").append(cmd).append("\" command not supported\n");
            return null;
        }

        String originalCmd = cmd;
        Wrap<String> cmdp = new Wrap<String>(cmd);
        int mode = get_map_mode(cmdp, false);
        cmd = cmdp.getValue();
        int maptype = (cmd.charAt(0) == 'n') ? 2 : cmd.charAt(0) == 'u' ? 1 : 0;

        if(maptype != 0) {
            // check number of arguments
            if(fields.size() == 1                             // unmap or noremap
                    || fields.size() == 2  && maptype == 2    // noremap
                    ) {
                emsg.error().append("\"").append(originalCmd).append("\" missing arguments\n");
                return null;
            }
        }

        if(fields.size() == 1) {
            if(maptype == 0 && printOk) {
                // print the global mappings (this is a static method)
                GetChar.printMappings(null, mode);
            }
            return null;
        }

        Character lhs = null;
        rhs.setLength(0);
        boolean ok;

        String field = fields.get(1);
        matcher.reset(field);
        ok = false;
        if(matcher.lookingAt()) {
            if(matcher.end() < field.length()) {
                emsg.error().append("for lhs only one char allowed; trailing: '")
                        .append(field.substring(matcher.end()))
                        .append("'\n");
            } else {
                lhs = tranlateMapCommandChar(matcher, false, field, emsg);
                ok = lhs != null;
            }
        } else {
            emsg.error().append("\"").append(field)
                    .append("\" left-side not recognized\n");
        }

        if(fields.size() == 2) {
            if(ok && maptype == 1) {
                // an unmap
                Mapping mapping = new Mapping(String.valueOf(lhs), null,
                        mode, false);
                mapping.isUnmap = true;
                return mapping;
            }
            if(maptype == 0 && printOk) {
                // print the global mappings (this is a static method)
                GetChar.printMappings(lhs, mode);
            }
            return null;
        }


        field = fields.get(2);
        matcher.reset(field);
        int idx = 0;
        do {
            if(false) {
                System.err.println("checking: '" + field.substring(idx) + "'");
            }
            ok = false;
            if(matcher.find() && idx == matcher.start()) {
                Character c = tranlateMapCommandChar(matcher, true, field, emsg);
                if(c != null) {
                    rhs.append(c.charValue());
                    ok = true;
                }
            } else {
                emsg.error().append("\"").append(field.substring(idx))
                        .append("\" right-side not recognized\n");
            }
            if(!ok) {
                break;
            }
            idx = matcher.end();
        } while(idx < matcher.regionEnd());

        if(emsg.length() == initialEmsgs) {
            Mapping mapping = new Mapping(String.valueOf(lhs), rhs.toString(),
                    mode, maptype == 2);
            if(Options.isKeyDebug()) {
                System.err.println("parseMapCommand: " + line
                        + ": " + mapping);
            }
            return mapping;
        } else {
            Options.kd().printf("parseMapCommand: %s: error\n", line);
            return null;
        }
    }

    /**
     * Parse the mappings, return empty string if no error.
     */
    public static String parseMapCommands(String input)
    {
        Wrap<String>emsg = new Wrap<String>("");
        parseMapCommands(input, emsg);
        return emsg.getValue();
    }

    public static List<Mapping> parseMapCommands(String input,
                                                 Wrap<String>p_emsg)
    {
        List<Mapping> mapCommands = new ArrayList<Mapping>();

        Emsgs emsg = new Emsgs();

        String[] lines = input.split("\n");
        for(int lnum = 0; lnum < lines.length; lnum++) {
            String line = lines[lnum];
            emsg.setLnum(lnum + 1);
            Mapping m = parseMapCommand(line, emsg, false);
            if(m != null)
                mapCommands.add(m);
        }

        if(emsg.length() > 0) {
            p_emsg.setValue(emsg.toString());
            return null;
        }

        return mapCommands;
    }

    public void printMappings(Character lhs, int mode)
    {
        List<Mapping> lM = getMappings(lhs, mode);
        Collections.sort(lM, new Comparator<Mapping>() {
            @Override
            public int compare(Mapping o1, Mapping o2)
            {
                return o1.lhs.compareTo(o2.lhs);
            }
        });

        StringBuilder sb = new StringBuilder();
        ViOutputStream vios = ViManager.createOutputStream(
                null, ViOutputStream.OUTPUT, "mappings");
        for(Mapping m : lM) {
            vios.println(String.format("%-3s %-8s %c %s",
                                       Mapping.modeString(m.mode, false),
                                       mappingString(m.lhs),
                                       m.noremap ? Character.valueOf('*')
                                                 : Character.valueOf(' '),
                                       mappingString(m.rhs)));
        }
        vios.close();
    }

    private static String mappingString(String s)
    {
        Map<Character, String> cmap = getReverseMapCommandSpecial();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int mods = 0;
            if(isVIRT(c)) {
                mods = (c >> MODIFIER_POSITION_SHIFT) & MOD_MASK;
                c &= ~(MOD_MASK << MODIFIER_POSITION_SHIFT);
            }
            String mapC = cmap.get(c);
            if(mapC != null)  {
                String modifier = "";
                if(mods != 0) {
                    modifier = (mods & SHFT) != 0 ? "S-"
                            : (mods & CTRL) != 0 ? "C-" : "?-";
                }
                mapC = "<" + modifier + mapC + ">";
            } else {
                if(c < 0x20) {
                    mapC = "<C-" + (char)('@' + c) + ">";
                } else {
                    mapC = String.valueOf(c);
                }
            }
            sb.append(mapC);
        }
        return sb.toString();
    }

    private static List<Mapping> getMappings(List<Mapping> origM, int mode)
    {
        List<Mapping> lM = new ArrayList<Mapping>();
        if(origM != null) {
            for(Mapping m : origM) {
                if((m.mode & mode) != 0)
                    lM.add(m);
            }
        }
        return lM;
    }

    // NOTE: if mapping has "isUnmap",
    // then overlapping mappings are removed.
    private boolean putMapping(Mapping m)
    {
        boolean unmap = m.isUnmap;
        Options.kd().printf(Level.FINER,
                      "putMapping(unmap %b): %s\n", unmap, m);

        Character c = m.lhs.charAt(0);
        List<Mapping> lM = mappings.get(c);
        if(lM == null) {
            if(unmap)
                return false;
            mappings.put(c, (lM = new ArrayList<Mapping>()));
        }

        // remove any mappings that the new mapping replaces
        for(ListIterator<Mapping> it = lM.listIterator(); it.hasNext();) {
            Mapping oldMapping = it.next();
            int oldMode = oldMapping.mode;
            int newMode = oldMapping.mode & ~m.mode;
            if(newMode == 0) {
                Options.kd().printf("putMapping: remove %s\n", oldMapping);
                it.remove();
            } else if(oldMode != newMode) {
                oldMapping.mode = newMode;
                Options.kd().printf("putMapping: mode change: %s (was %d)\n",
                          oldMapping, oldMode);
            }
        }
        if(!unmap)
            lM.add(m);
        return true;
    }

    private boolean containsMappings(Character lhs)
    {
        return mappings.containsKey(lhs);
    }

    private List<Mapping> getMappings(Character lhs, int mode)
    {
        if(lhs != null) {
            return getMappings(mappings.get(lhs), mode);
        }
        List<Mapping> lM = new ArrayList<Mapping>();
        for(List<Mapping> l : mappings.values()) {
            lM.addAll(getMappings(l, mode));
        }
        return lM;
    }

    public Mapping getMapping(Character c, int state)
    {
        List<Mapping> lM = mappings.get(c);
        if(lM == null)
            return null;

        for(ListIterator<Mapping> it = lM.listIterator(); it.hasNext();) {
            Mapping m = it.next();
            if((m.mode & state) != 0)
                return m;
        }
        return null;
    }

    private void removeMapping(Character c, int mode)
    {
        List<Mapping> lM = mappings.get(c);
        if(lM == null)
            return;

        // remove any mappings that overlap with the mode
        for(ListIterator<Mapping> it = lM.listIterator(); it.hasNext();) {
            Mapping oldM = it.next();
            if((mode & oldM.mode) != 0) {
                Options.kd().printf("removeMapping: remove %s\n", oldM);
                it.remove();
            }
        }
    }

    public void saveMappings(List<Mapping> newMappings)
    {
        mappings.clear();
        //
        // NOTE: these must be "put" in order
        //
        for(Mapping m : getDefaultMappings()) {
            putMapping(m);
        }
        for(Mapping m : newMappings) {
            // if(m.isUnmap && !containsMappings(m.lhs.charAt(0))) {
            //   //
            // }
            putMapping(m);
        }
    }

    static private List<Mapping>
            getDefaultMappings()
    {
        List<Mapping> defaultMappings;

        if(refDefaultMappings == null
                || (defaultMappings = refDefaultMappings.get()) == null) {
            defaultMappings = new ArrayList<Mapping>();
            refDefaultMappings
                    = new WeakReference<List<Mapping>>(defaultMappings);

            // defaultMappings.put('x', new Mapping(lhs, rhs, mode, noremap));
        }
        return defaultMappings;
    }

    private static Map<Character, String> getReverseMapCommandSpecial()
    {
        Map<Character, String> reverseMapCommandSpecial;

        if(refReverseMapCommandSpecial == null
                || (reverseMapCommandSpecial = refReverseMapCommandSpecial.get())
                == null) {
            Map<EqLower, Character> smap = getMapCommandSpecial();
            reverseMapCommandSpecial = new HashMap<Character, String>(smap.size());
            refReverseMapCommandSpecial = new WeakReference<
                    Map<Character, String>>(reverseMapCommandSpecial);

            for(Map.Entry<EqLower, Character> entry : smap.entrySet()) {
                reverseMapCommandSpecial.put(entry.getValue(),
                                             entry.getKey().toString());
            }
            reverseMapCommandSpecial.put('\n', "<NL>");
        }
        return reverseMapCommandSpecial;
    }

    private static Map<EqLower, Character> getMapCommandSpecial()
    {
        Map<EqLower, Character> mapCommandSpecial;

        if(refMapCommandSpecial == null
                || (mapCommandSpecial = refMapCommandSpecial.get()) == null) {
            mapCommandSpecial = new HashMap<EqLower, Character>(30 + 30);
            refMapCommandSpecial = new WeakReference<Map<EqLower, Character>>
                    (mapCommandSpecial);

            mapCommandSpecial.put(new EqLower("Nul"),      '\n');
            mapCommandSpecial.put(new EqLower("BS"),       '\b');//////////////
            mapCommandSpecial.put(new EqLower("Tab"),      '\t');//////////////
            mapCommandSpecial.put(new EqLower("NL"),       '\n');
            mapCommandSpecial.put(new EqLower("FF"),       '\f');
            mapCommandSpecial.put(new EqLower("CR"),       '\n');
            mapCommandSpecial.put(new EqLower("Return"),   '\n');
            mapCommandSpecial.put(new EqLower("Enter"),    '\n');
            mapCommandSpecial.put(new EqLower("Esc"),      '\u001b');//////////
            mapCommandSpecial.put(new EqLower("Space"),    ' ');
            mapCommandSpecial.put(new EqLower("lt"),       '<');
            mapCommandSpecial.put(new EqLower("Bslash"),   '\\');
            mapCommandSpecial.put(new EqLower("Bar"),      '|');
            mapCommandSpecial.put(new EqLower("Del"),      '\u007f');//////////

            mapCommandSpecial.put(new EqLower("EOL"),      '\n');

            mapCommandSpecial.put(new EqLower("Up"),       K_UP);
            mapCommandSpecial.put(new EqLower("Down"),     K_DOWN);
            mapCommandSpecial.put(new EqLower("Left"),     K_LEFT);
            mapCommandSpecial.put(new EqLower("Right"),    K_RIGHT);

            mapCommandSpecial.put(new EqLower("Help"),     K_HELP);
            mapCommandSpecial.put(new EqLower("Undo"),     K_UNDO);
            mapCommandSpecial.put(new EqLower("Insert"),   K_INS);
            mapCommandSpecial.put(new EqLower("Home"),     K_HOME);
            mapCommandSpecial.put(new EqLower("End"),      K_END);
            mapCommandSpecial.put(new EqLower("PageUp"),   K_PAGEUP);
            mapCommandSpecial.put(new EqLower("PageDown"), K_PAGEDOWN);
        }

        return mapCommandSpecial;

        /*
         * <kHome>
         * <kEnd>
         * <kPageUp>
         * <kPageDown>
         * <kPlus>
         * <kMinus>
         * <kMultiply>
         * <kDivide>
         * <kEnter>
         * <kPoint>
         * <k0> - <k9>
         *
         * <S-Up>
         * <S-Down>
         * <S-Left>
         * <S-Right>
         * <C-Left>
         * <C-Right>
         * <F1> - <F12>
         * <S-F1> - <S-F12>
         * <CSI>
         * <xCSI>
         */
    }

}
