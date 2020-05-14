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

import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.GetChar;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.lib.TypeBufMultiCharMapping.TypeBufPeek;
import com.raelity.jvi.lib.MutableBoolean;
import com.raelity.jvi.lib.Wrap;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.OptUtil;
import com.raelity.text.TextUtil;

import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.lib.KeyDefs.*;
import static com.raelity.text.TextUtil.sf;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public final class Mappings {
    private static Mappings INSTANCE;

    private final Map<Character, List<Mapping>> mappings = new HashMap<>();
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
        Wrap<String> emsg = new Wrap<>();
        List<Mapping> mapCommands = Mappings.parseMapCommands(
                OptUtil.getOption(Options.mapCommands).getString(),
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
        ColonCommands.register("im",  "imap", cmd, null);

        ColonCommands.register("no",  "noremap",  cmd, null);
        ColonCommands.register("nn",  "nnoremap", cmd, null);
        ColonCommands.register("vn",  "vnoremap", cmd, null);
        ColonCommands.register("ono", "onoremap", cmd, null);
        ColonCommands.register("pn",  "pnoremap", cmd, null);
        ColonCommands.register("ino", "inoremap", cmd, null);

        ColonCommands.register("unm", "unmap",  cmd, null);
        ColonCommands.register("nun", "nunmap", cmd, null);
        ColonCommands.register("vu",  "vunmap", cmd, null);
        ColonCommands.register("ou",  "ounmap", cmd, null);
        ColonCommands.register("pun", "punmap", cmd, null);
        ColonCommands.register("iu",  "iunmap", cmd, null);

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
            if(m != null && m.isUnmap && !containsMappings(m)) {
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
     * Convert the next part of user map command input
     * to actual character. A single character is produced.
     *
     * the match to a char.
     * @param m regex matcher for the next input to translate
     * @param is_rhs translating the rhs of a map command
     * @param orig for debug, the string that is matched
     * @param emsg append error messages to here
     * @return null if problem else translated char
     */
    private static Character tranlateMapCommandChar(
            Matcher m, boolean is_rhs, String orig, Emsgs emsg)
    {
        // if(false) {
        //     ViManager.println("region: '"
        //             +  orig.substring(m.start())
        //             + "' match: '" + m.group() + "'");
        //     for(int i = 1; i <= m.groupCount(); i++) {
        //         ViManager.println("\t" + m.group(i));
        //     }
        // }

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
                switch (s) {
                case "C":
                case "c":
                    modifiers |= CTRL;
                    break;
                case "S":
                case "s":
                    modifiers |= SHFT;
                    break;
                default:
                    Logger.getLogger(GetChar.class.getName())
                            .log(Level.SEVERE, null,
                                    new Exception("unknown modifier: " + s));
                    break;
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

    /**
     * @return matcher to parse chars in lhs/rhs of map command.
     */
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

            // to fix bug #165 add p{L} and allow all unicode chars
            String pat = "([!-~\\p{L}&&[^\\\\<]])"
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
            refMapCharsMatcher = new WeakReference<>(mapCharsMatcher);
        }
        return mapCharsMatcher;
    }

    /**
     * Should already have verified that cmd is supported.
     *
     * vim's get_map_mode()
     *
     * @return mode(s) that the command matches
     */
    private static int parseMapMode(Wrap<String>cmdp, boolean forceit)
    {
        String cmd = cmdp.getValue();
        int p = 0;
        int mode;
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
                | "imap".equals(cmd)

                | "noremap".equals(cmd)
                | "nnoremap".equals(cmd)
                | "vnoremap".equals(cmd)
                | "onoremap".equals(cmd)
                | "pnoremap".equals(cmd)
                | "inoremap".equals(cmd)

                | "unmap".equals(cmd)
                | "nunmap".equals(cmd)
                | "vunmap".equals(cmd)
                | "ounmap".equals(cmd)
                | "punmap".equals(cmd)
                | "iunmap".equals(cmd)
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
        Matcher matcher = getMapCharsMatcher(); // just holds a reference
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
        Wrap<String> cmdp = new Wrap<>(cmd);
        int mode = parseMapMode(cmdp, false);
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

        String lhs = parseMappingChars(fields.get(1), emsg, "left-side");
        boolean ok = lhs != null;

        if(fields.size() == 2) {
            if(ok && maptype == 1) {
                // an unmap
                Mapping mapping = new Mapping(lhs, null, mode, false);
                mapping.isUnmap = true;
                return mapping;
            }
            if(maptype == 0 && printOk) {
                // print the global mappings (this is a static method)
                GetChar.printMappings(lhs, mode);
            }
            return null;
        }

        String rhs = parseMappingChars(fields.get(2), emsg, "right-side");

        if(emsg.length() == initialEmsgs) {
            Mapping mapping = new Mapping(lhs, rhs, mode, maptype == 2);
            Options.kd().println(() ->
                    "parseMapCommand: " + line + ": " + mapping);
            return mapping;
        } else {
            Options.kd().printf("parseMapCommand: %s: error\n", line);
            return null;
        }
    }

    /**
     * Parse the mappings, return any errors.
     * Return empty string if no error.
     * Discard the results of the parse.
     */
    public static String parseMapCommands(String input)
    {
        Wrap<String>emsg = new Wrap<>("");
        parseMapCommands(input, emsg);
        return emsg.getValue();
    }

    public static List<Mapping> parseMapCommands(String input,
                                                 Wrap<String>p_emsg)
    {
        Matcher matcher = getMapCharsMatcher(); // just holds a reference
        List<Mapping> mapCommands = new ArrayList<>();

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

    private static String parseMappingChars(String field,
                                            Emsgs emsg,
                                            String tag)
    {
        StringBuilder sb = new StringBuilder();

        Matcher matcher = getMapCharsMatcher();
        matcher.reset(field);
        boolean ok;

        int idx = 0;
        do {
            // ViManager.println("checking: '" + field.substring(idx) + "'");
            ok = false;
            if(matcher.find() && idx == matcher.start()) {
                Character c = tranlateMapCommandChar(matcher, true, field, emsg);
                if(c != null) {
                    sb.append(c.charValue());
                    ok = true;
                }
            } else {
                emsg.error().append("\"").append(field.substring(idx))
                        .append("\" ").append(tag).append(" not recognized\n");
            }
            if(!ok) {
                return null;
            }
            idx = matcher.end();
        } while(idx < matcher.regionEnd());

        return sb.toString();
    }

    public void printMappings(String lhs, int mode)
    {
        List<Mapping> lM = getMappings(lhs, mode);
        if(lM.isEmpty()) {
            Msg.smsg("No mapping found");
            return;
        }
        Collections.sort(lM,
                (Mapping o1, Mapping o2) -> o1.lhs.compareTo(o2.lhs));

        StringBuilder sb = new StringBuilder();
        try (ViOutputStream vios = ViManager.createOutputStream("mappings")) {
            for(Mapping m : lM) {
                vios.println(String.format("%-3s %-8s %c %s",
                                           Mapping.modeString(m.mode, false),
                                           mappingString(m.lhs),
                                           m.noremap ? Character.valueOf('*')
                                                     : Character.valueOf(' '),
                                           mappingString(m.rhs)));
            }
        }
    }

    /** for print mappings */
    private static List<Mapping> getMappings(List<Mapping> origM, int mode)
    {
        List<Mapping> lM = new ArrayList<>();
        if(origM != null) {
            for(Mapping m : origM) {
                if((m.mode & mode) != 0)
                    lM.add(m);
            }
        }
        return lM;
    }

    /** for print mappings */
    private List<Mapping> getMappings(String lhs, int mode)
    {
        List<Mapping> lM = new ArrayList<>();
        if(lhs != null) {
            for(Mapping m : getMappings(mappings.get(lhs.charAt(0)), mode)) {
                if(m.lhs.startsWith(lhs)) {
                    lM.add(m);
                }
            }

            return lM;
        }
        for(List<Mapping> l : mappings.values()) {
            lM.addAll(getMappings(l, mode));
        }
        return lM;
    }

    /**
     * convert lhs/rhs of a mapping to @{literal <C-x>, <Special>}
     * style for display.
     * @param s
     * @return
     */
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

    /**
     * Remove any mappings that overlap the param mapping
     * and add the new mapping unless m.isUnmap is set.
     * May change the mode of an existing mapping to reduce
     * the modes it operates on.
     * @param m
     */
    private void putMapping(Mapping newMapping)
    {
        boolean unmap = newMapping.isUnmap;
        Options.kd().printf(Level.FINER,
                      "putMapping(unmap %b): %s\n", unmap, newMapping);

        Character c = newMapping.lhs.charAt(0);
        List<Mapping> lM = mappings.get(c);
        if(lM == null) {
            if(unmap)
                return;
            mappings.put(c, (lM = new ArrayList<>()));
        }

        // remove any mappings that the new mapping replaces
        for(Iterator<Mapping> it = lM.iterator(); it.hasNext();) {
            Mapping oldMapping = it.next();
            boolean fixupOldMapping = false;

            if(newMapping.lhs.length() == oldMapping.lhs.length()) {
                if(newMapping.lhs.equals(oldMapping.lhs)) {
                    fixupOldMapping = true;
                }
            } else if(newMapping.lhs.length() > oldMapping.lhs.length()) {
                if(newMapping.lhs.startsWith(oldMapping.lhs)
                        && !unmap) {
                    fixupOldMapping = true;
                }
            } else { //if(newMapping.lhs.length() < oldMapping.lhs.length())
                if(oldMapping.lhs.startsWith(newMapping.lhs)) {
                    fixupOldMapping = true;
                }
            }

            if(fixupOldMapping) {
                int oldMode = oldMapping.mode;
                int newMode = oldMapping.mode & ~newMapping.mode;
                if(newMode == 0) {
                    Options.kd().printf("putMapping: remove %s\n", oldMapping);
                    it.remove();
                } else if(oldMode != newMode) {
                    oldMapping.mode = newMode;
                    Options.kd().printf("putMapping: mode change:"
                            + " %s (was %d)\n", oldMapping, oldMode);
                }
            }
        }
        if(!unmap)
            lM.add(newMapping);
    }

    Mapping getMapping(TypeBufPeek peek, int state, MutableBoolean maybe)
    {
        if(maybe != null)
            maybe.setValue(false);
        List<Mapping> lM = mappings.get(peek.firstChar());
        if( lM != null) {
            for(Mapping m : lM) {
                if((m.mode & state) != 0 && peek.isMatch(m.lhs, maybe))
                    return m;
            }
        }
        return null;
    }

    private boolean containsMappings(Mapping m)
    {
        return null != getMapping(new StringLookup(m.lhs), m.mode, null);
    }

    private static class StringLookup implements TypeBufPeek
    {
        String buf;

        public StringLookup(String buf)
        {
            this.buf = buf;
        }

        @Override
        public char firstChar()
        {
            return buf.charAt(0);
        }

        @Override
        public boolean isMatch(String lhs, MutableBoolean maybe)
        {
            // no partial match for lookup
            return buf.startsWith(lhs);
        }
    }

    /** reinitialize the mappings with these new mappings */
    public void saveMappings(List<Mapping> newMappings)
    {
        mappings.clear();
        //
        // NOTE: defaults must be put first
        //
        for(Mapping m : getDefaultMappings()) {
            putMapping(m);
        }
        for(Mapping m : newMappings) {
            putMapping(m);
        }
    }

    static private List<Mapping> getDefaultMappings()
    {
        List<Mapping> defaultMappings;

        if(refDefaultMappings == null
                || (defaultMappings = refDefaultMappings.get()) == null) {
            defaultMappings = new ArrayList<>();
            refDefaultMappings = new WeakReference<>(defaultMappings);

            // defaultMappings.put('x', new Mapping(lhs, rhs, mode, noremap));
        }
        return defaultMappings;
    }

    /** Used for display.
     * @return map of char to Special
     */
    private static Map<Character, String> getReverseMapCommandSpecial()
    {
        Map<Character, String> reverseMapCommandSpecial;

        if(refReverseMapCommandSpecial == null
                || (reverseMapCommandSpecial = refReverseMapCommandSpecial.get())
                == null) {
            Map<EqLower, Character> smap = getMapCommandSpecial();
            reverseMapCommandSpecial = new HashMap<>(smap.size());
            refReverseMapCommandSpecial
                    = new WeakReference<>(reverseMapCommandSpecial);

            for(Map.Entry<EqLower, Character> entry : smap.entrySet()) {
                reverseMapCommandSpecial.put(entry.getValue(),
                                             entry.getKey().toString());
            }
            reverseMapCommandSpecial.put('\n', "<NL>");
        }
        return reverseMapCommandSpecial;
    }

    /**
     *
     * @return map of &lt;special&gt; to actual character
     */
    private static Map<EqLower, Character> getMapCommandSpecial()
    {
        Map<EqLower, Character> mapCommandSpecial;

        if(refMapCommandSpecial == null
                || (mapCommandSpecial = refMapCommandSpecial.get()) == null) {
            mapCommandSpecial = new HashMap<>(30 + 30);
            refMapCommandSpecial = new WeakReference<> (mapCommandSpecial);

            mapCommandSpecial.put(new EqLower("Nul"),      '\n');
            mapCommandSpecial.put(new EqLower("BS"),       '\b');//////////////
            mapCommandSpecial.put(new EqLower("Tab"),      '\t');//////////////
            mapCommandSpecial.put(new EqLower("NL"),       '\n');
            mapCommandSpecial.put(new EqLower("FF"),       '\f');
            mapCommandSpecial.put(new EqLower("CR"),       '\n');
            mapCommandSpecial.put(new EqLower("Return"),   '\n');
            mapCommandSpecial.put(new EqLower("Enter"),    '\n');
            mapCommandSpecial.put(new EqLower("Esc"),      '\u001b');//////////
            mapCommandSpecial.put(new EqLower("lt"),       '<');
            mapCommandSpecial.put(new EqLower("Bslash"),   '\\');
            mapCommandSpecial.put(new EqLower("Bar"),      '|');
            mapCommandSpecial.put(new EqLower("Del"),      '\u007f');//////////

            mapCommandSpecial.put(new EqLower("Space"),    K_SPACE);

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
