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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.ViBadLocationException;
import com.raelity.jvi.ViBuffer.BIAS;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViTextView.Orientation;
import com.raelity.jvi.ViTextView.TAGOP;
import com.raelity.jvi.core.ColonCommands.AbstractColonAction;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.lib.CcFlag;
import com.raelity.jvi.core.lib.ColonCommandItem;
import com.raelity.jvi.core.lib.Messages;
import com.raelity.jvi.lib.MutableBoolean;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.manager.AppViews;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.*;

import static java.lang.Math.min;

import static com.raelity.jvi.core.Util.beep_flush;
import static com.raelity.jvi.core.lib.Constants.*;
import static com.raelity.jvi.core.VimPath.*;
import static com.raelity.jvi.manager.ViManager.eatme;
import static com.raelity.text.TextUtil.sf;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Cc01
{
    private static final Logger LOG = Logger.getLogger(Cc01.class.getName());

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
        @Override
        public void init()
        {
            Cc01.init();
        }
    }

    private Cc01() { }

    private static void init()
    {
        ColonCommands.register("n", "next", new Next(true), null);
        ActionListener alNext= new Next(false);
        ColonCommands.register("N", "Next", alNext, null);
        ColonCommands.register("prev", "previous", alNext, null);
        ColonCommands.register("clo", "close", new Close(), null);
        ColonCommands.register("on", "only", new Only(), null);

        ColonCommands.register("q", "quit", new Quit(), null);
        ColonCommands.register("w", "write", new Write(), null);
        ColonCommands.register("wq", "wq", new Wq(), null);
        ColonCommands.register("wa", "wall", new Wall(), null);

        ActionListener alBuffers = new Buffers();
        ColonCommands.register("files","files", alBuffers, null);
        ColonCommands.register("buffers","buffers", alBuffers, null);
        ColonCommands.register("ls","ls", alBuffers, null);

        ActionListener alBuffers2 = new Buffers2();
        ColonCommands.register("files2","files2", alBuffers2, null);
        ColonCommands.register("buffers2","buffers2", alBuffers2, null);
        ColonCommands.register("ls2","ls2", alBuffers2, null);

        ColonCommands.register("f", "file", new FileAction(), null);
        ColonCommands.register("e", "edit", new Edit(), null);
        ColonCommands.register("s", "substitute", ACTION_substitute, null);
        ColonCommands.register("&", "&", ACTION_substitute, null);
        ColonCommands.register("g", "global", ACTION_global, null);
        ColonCommands.register("v", "vglobal", new Vglobal(), null);
        ColonCommands.register("d", "delete", ACTION_delete, null);
        ColonCommands.register("p", "print", ACTION_print, null);

        ColonCommands.register("ju", "jumps", new Jumps(), null);

        ColonCommands.register("ta", "tag", new Tag(), null);
        ColonCommands.register("tags", "tags", new Tags(), null);
        ColonCommands.register("ts", "tselect", new Tselect(), null);
        ColonCommands.register("po", "pop", new Pop(), null);

        ColonCommands.register("noh", "nohlsearch", new Nohlsearch(), null);

        ColonCommands.register("testGlassKeys", "testGlassKeys",
                               new TestGlassKeys(),
                               EnumSet.of(CcFlag.DBG));
        ColonCommands.register("testModalKeys", "testModalKeys",
                               new TestModalKeys(),
                               EnumSet.of(CcFlag.DBG));

        ColonCommands.register("y", "yank", ACTION_yank, null);
        // not pretty, limit the number of shifts...
        ColonCommands.register(">", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>",
                               new ShiftAction(OP_RSHIFT),
                               null);
        ColonCommands.register("<", "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
                               new ShiftAction(OP_LSHIFT),
                               null);

        ColonCommands.register("m", "move", new moveCopy(true), null);
        ActionListener alCopy = new moveCopy(false);
        ColonCommands.register("co", "copy", alCopy, null);
        ColonCommands.register("t", "t", alCopy, null);
        ColonCommands.register("u", "undo", new Undo(), null);
        ColonCommands.register("red", "redo", new Redo(), null);

        // clone an editor
        ColonCommands.register("clon", "clone", new CloneAction(), null);
        ColonCommands.register("sp", "split",
                               new SplitAction(Orientation.UP_DOWN), null);
        ColonCommands.register("vs", "vsplit",
                               new SplitAction(Orientation.LEFT_RIGHT), null);

        addDebugColonCommands();
    }

    private static final ActionListener ACTION_substitute = new Substitute();
    private static final ActionListener ACTION_print      = new Print();
    private static final ActionListener ACTION_yank       = new Yank();
    private static final ActionListener ACTION_delete     = new Delete();
    private static final ActionListener ACTION_global     = new Global();


    static ActionListener getActionPrint()      { return ACTION_print; }
    static ActionListener getActionYank()       { return ACTION_yank; }
    static ActionListener getActionSubstitute() { return ACTION_substitute; }
    static ActionListener getActionDelete()     { return ACTION_delete; }
    static ActionListener getActionGlobal()     { return ACTION_global; }


    /** next/Next/previous through MRU list */
    static private class Next extends AbstractColonAction { // NEEDSWORK: count
        boolean goForward;

        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.NO_ARGS, CcFlag.RANGE);
        }

        Next(boolean goForward) {
            this.goForward = goForward;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            ColonEvent ce = (ColonEvent)e;
            int offset;
            if(ce.getAddrCount() == 0)
                offset = 1;
            else
                offset = ce.getLine1();
            if(!goForward)
                offset = -offset;
            ViAppView av = AppViews.relativeMruAppView(offset);
            if(av != null) {
                // don't want mru list to change
                AppViews.keepMruAfterActivation(av);
                ViManager.getFS().edit(av, false);
            }
        }
    }

    private static class Quit implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;
            cev.getViTextView().win_quit();
        }};

    private static class Close implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;
            // NEEDSWORK: win_close: hidden, need_hide....
            cev.getViTextView().win_close(false);
        }}

    private static class Only implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;
            // NEEDSWORK: forceit....

            List<ViAppView> avs = AppViews.getList(AppViews.ACTIVE);

            ViAppView avCur = ViManager.getFactory()
                    .getAppView(cev.getViTextView().getEditor());
            
            for(ViAppView av :avs)
                if(!av.equals(avCur))
                    av.close(false);
        }}

    private static class Wall implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            ViManager.getFS().writeAll(false);
        }};

    private static class FileAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            ColonEvent cev = (ColonEvent)ev;
            Msg.smsg(ViManager.getFS()
                    .getDisplayFileViewInfo(cev.getViTextView()));
        }}

    private static boolean do_write(ColonEvent cev)
    {
        boolean ok;
        boolean isBang = cev.isBang();
        String fName = cev.getNArg() == 0 ? null : cev.getArg(1);
        Integer[] range = cev.getAddrCount() > 0
                ? new Integer[] { cev.getLine1(), cev.getLine2() }
                : new Integer[0];

        if(cev.getNArg() < 2) {
            ok = ViManager.getFS().write(
                    cev.getViTextView(), isBang, fName, range);
        } else {
            Msg.emsg(":write only accepts none or one argument");
            ok = false;
        }
        return ok;
    }

    /**
        * Write command.
        */
    private static class Write extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.BANG);
        }
        
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            do_write((ColonEvent)ev);
        }
    };

    private static class Wq extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.BANG);
        }
        
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            if(do_write(cev))
                cev.getViTextView().win_quit();
        }
    };

    private static class SplitAction extends AbstractColonAction
    {
        Orientation orientation;

        public SplitAction(Orientation orientation)
        {
            this.orientation = orientation;
        }

        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.COMPL_FN, CcFlag.RANGE);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ColonEvent cev = (ColonEvent)e;
            int n = cev.getAddrCount() == 0 ? 0 : cev.getLine2();
            boolean reportUsage = false;
            List<String> args = new ArrayList<>(cev.getArgsRaw());
            MutableBoolean reportedError = new MutableBoolean();
            if(args.size() >= 1 && args.get(0).charAt(0) == '#') {
                ViAppView av = parseFileNumber(args, reportedError);
                if(!args.isEmpty() && !reportedError.getValue()) {
                    reportUsage = true;
                }
                if(av != null)
                    Misc01.win_split(orientation, n, av);
            } else if(args.isEmpty()) {
                Misc01.win_split(orientation, n, null);
            } else
                reportUsage = true;
            if(reportUsage)
                Msg.emsg(":" + cev.getArg(0)
                        + " accepts no arguments or only '# <WindowNum>'");
        }
    }

    /**
     * Edit command.
     */
    private static class Edit extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.BANG, CcFlag.COMPL_FN,
                              CcFlag.XFILE);
        }

        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            boolean reportUsage = false;
            List<String> argsraw = new ArrayList<>(cev.getArgsRaw());
            MutableBoolean reportedError = new MutableBoolean();
            if(!argsraw.isEmpty() && argsraw.get(0).charAt(0) == '#') {
                ViAppView av = parseFileNumber(argsraw, reportedError);
                if(!argsraw.isEmpty() && !reportedError.getValue()) {
                    reportUsage = true;
                }
                if(av != null)
                    ViManager.getFS().edit(av, cev.isBang());
            } else if (cev.getNArg() == 1) {
                String fName = cev.getArg(1);
                Path path = getPath(fName);
                ViManager.getFS().edit(path, cev.isBang(), null);
            } else
                reportUsage = true;
            if(reportUsage)
                Msg.emsg(":edit only accepts '#[-]<digits>' or '<fname>'");
        }
    };

    /**
     * args is typically from a colon event. Expect to see either
     * ("#[number]) or ("#", "[number]"). If a "#" is seen, then
     * either one or two elements are removed from the list.
     * <p/>
     * NEEDSWORK: in future, may only want to remove "<number>" if it in fact
     *            parsed as a number.
     * @param args
     * @param reportedError
     * @return
     */
    static ViAppView parseFileNumber(List<String> args,
                                     MutableBoolean reportedError)
    {
        ViAppView av = null;

        assert args.size() >= 1 && args.get(0).charAt(0) == '#';
        if(!args.isEmpty() && args.get(0).charAt(0) == '#') {
            String stringWindowID = args.get(0).substring(1);
            args.remove(0);
            if(stringWindowID.isEmpty() && !args.isEmpty()) {
                stringWindowID = args.get(0);
                args.remove(0);
            }
            av = convertFileNumber(stringWindowID, reportedError);
        }

        return av;
    }

    /**
     * Convert the string number into an AppView. Positive numbers are
     * window number; negative are index into mru list.
     * @param stringWindowID
     * @param reportedError set to true if an error was reported
     * @return av or null if bad parse
     */
    static ViAppView convertFileNumber(String stringWindowID,
                                       MutableBoolean reportedError)
    {
        ViAppView av = null;
        boolean error = false;

        int windowID = -1;
        if(!error && !stringWindowID.isEmpty()) {
            try {
                windowID = Integer.parseInt(stringWindowID);
            } catch(NumberFormatException ex) {
                error = true;
            }
            if(error) {
                reportedError.setValue(true);
                Msg.emsg("Only '#[<number>]' allowed");
            }
        }
        if( ! error) {
            // Look up the appView. If i >= 0 then find app view
            // with window that matches that number (standard vim).
            // If i < 0 then use it to index into the mru list.
            if(windowID >= 0) {
                for (ViAppView av1 : AppViews.getList(AppViews.ACTIVE)) {
                    if(windowID == av1.getWinID()) {
                        av = av1;
                        break;
                    }
                }
            } else {
                av = AppViews.getMruAppView(-windowID);
            }
            if(av == null) {
                reportedError.setValue(true);
                Msg.emsg("No alternate file name to substitute for '#"
                        + windowID + "'");
            }
        }
        return av;
    }

    private static class Substitute extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.NO_PARSE, CcFlag.RANGE);
        }
        
        @Override
        public void actionPerformed(final ActionEvent ev)
        {
            Misc.runUndoable(() -> {
                Search01.substitute((ColonEvent)ev);
            });
        }
    };

    private static class Global extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.NO_PARSE, CcFlag.RANGE, CcFlag.BANG);
        }
        
        @Override
        public void actionPerformed(final ActionEvent ev) {
            Misc.runUndoable(() -> {
                Search01.global((ColonEvent)ev);
                Options.newSearch();
            });
        }
    };

    private static class Vglobal extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.NO_PARSE, CcFlag.RANGE);
        }
        
        @Override
        public void actionPerformed(final ActionEvent ev) {
            Misc.runUndoable(() -> {
                Search01.global((ColonEvent)ev);
                Options.newSearch();
            });
        }
    };

    /** This is the default buffers2,files2,ls2 command.
     *  Name and two columns: MRU and normal.
     */
    private static class Buffers2 implements ActionListener
    {
    @Override
    public void actionPerformed(ActionEvent e)
    {
        try (ViOutputStream osa = ViManager.createOutputStream(
                "=== MRU (:n :N :e#-<digit>) ===                "
                        + "=== activation (:e#<digit> ===")) {
            int i = 0;
            ViAppView curav = AppViews.relativeMruAppView(0);
            ViAppView prev = AppViews.getMruAppView(1);
            
            List<String> outputData = new ArrayList<>();
            ViFactory factory = ViManager.getFactory();
            List<ViAppView> l1 = AppViews.getList(AppViews.MRU);
            List<ViAppView> l2 = AppViews.getList(AppViews.ACTIVE);
            assert l1.size() == l2.size();
            while(i < l1.size()) {
                //ViAppView o1 = AppViews.getMruAppView(i);
                //ViAppView o2 = AppViews.getAppView(i+1);
                ViAppView av1 = l1.get(i);
                ViAppView av2 = l2.get(i);
                int w2 = av2.getWinID();
                outputData.add(
                    sf(" %2d %c %-40s %3d %c %s",
                    i,
                    av1.equals(curav) ? '%' : av1.equals(prev) ? '#' : ' ',
                    factory.getFS().getDisplayFileName(av1, true),
                    w2,
                    av2.equals(curav) ? '%' : av2.equals(prev) ? '#' : ' ',
                    factory.getFS().getDisplayFileName(av2, true)));
                i++;
            }
            // print in reverse order, MRU visible if scrolling
            for(int i01 = outputData.size() -1; i01 >= 0; i01--) {
                osa.println(outputData.get(i01));
            }
        }
    }
    };

    /** This is the default buffers,files,ls command. The platform specific code
     * may chose to use this, or implement their own, possibly using
     * popup gui components.
     */
    private static class Buffers implements ActionListener
    {
    @Override
    public void actionPerformed(ActionEvent e)
    {
        try (ViOutputStream osa = ViManager.createOutputStream(null)) {
            int i = 0;
            ViAppView curav = AppViews.relativeMruAppView(0);
            ViAppView prev = AppViews.getMruAppView(1);
            
            ViFactory factory = ViManager.getFactory();
            List<ViAppView> l = AppViews.getList(AppViews.ACTIVE);
            while(i < l.size()) {
                ViAppView av = l.get(i);
                osa.println(sf("%3d %c %s", av.getWinID(),
                    av.equals(curav) ? '%' : av.equals(prev) ? '#' : ' ',
                    factory.getFS().getDisplayPath(av)));
                i++;
            }
        }
    }
    };

    /**
        * :print command. If not busy global then output to "print" stream.
        * For now, its just a no-op so the word "print" can be found.
        */
    private static class Print extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.HIDE, CcFlag.NO_ARGS);
        }
        
        @Override
        public void actionPerformed(ActionEvent ev) {
        }
    };

    /**
        * :tag command.
        */
    private static class Tag extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.RANGE);
        }
        
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent evt = (ColonEvent)ev;
            if(evt.getNArg() > 1) {
                Msg.emsg(Messages.e_trailing);
                return;
            }
            if(evt.getNArg() == 1) {
                ViManager.getFactory().tagDialog(evt);
            } else {
                int count = 1;
                if(evt.getAddrCount() == 1)
                    count = evt.getLine2();
                ViManager.getFactory().tagStack(TAGOP.NEWER, count);
            }
        }
    };

    /**
        * :pop command.
        */
    private static class Pop extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.NO_ARGS, CcFlag.RANGE);
        }
        
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent evt = (ColonEvent)ev;
            if(evt.getNArg() > 0) {
                Msg.emsg(Messages.e_trailing);
                return;
            }
            int count = 1;
            if(evt.getAddrCount() == 1)
                count = evt.getLine2();
            ViManager.getFactory().tagStack(TAGOP.OLDER, count);
        }
    };

    private static class Tselect extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.RANGE);
        }
        
        @Override
        public void actionPerformed(ActionEvent ev) {
            ColonEvent ce = (ColonEvent)ev;
            if(ce.getNArg() > 1) {
                Msg.emsg(Messages.e_trailing);
                return;
            }
            // Wonder what this is for?
            //int count = 1;
            //if(ce.getAddrCount() == 1)
            //    count = ce.getLine2();
            ViManager.getFactory().tagDialog(ce);
        }
    };

    private static class Jumps implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            MarkOps.do_jumps();
        }
    };

    private static class Tags implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            ViManager.getFactory().displayTags();
        }
    };

    private static class Nohlsearch implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            Options.nohCommand();
        }
    };

    /**
     * :delete command.
     */
    private static class Delete extends AbstractColonAction {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.RANGE);
        }

        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            final OPARG oa = ColonCommands.setupExop(cev, true);
            if(!oa.error) {
                oa.op_type = OP_DELETE;
                Misc.runUndoable(() -> {
                    Misc.op_delete(oa);
                });
            }
        }
    };

    /**
     * :yank command.
     */
    private static class Yank extends AbstractColonAction {

        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.RANGE);
        }
        
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            OPARG oa = ColonCommands.setupExop((ColonEvent)ev, true);
            if(!oa.error) {
                oa.op_type = OP_YANK;
                Misc.op_yank(oa, false, true);
            }
        }
    };

    private static class ShiftAction extends AbstractColonAction
    {
        final int op;

        public ShiftAction(int op)
        {
            this.op = op;
        }

        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.RANGE);
        }

        @Override
        public String getDisplayName(ColonCommandItem cci)
        {
            return cci.getName().substring(0, min(10, cci.getName().length()));
        }

        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            final OPARG oa = ColonCommands.setupExop(cev, true);
            final int amount = cev.getInputCommandName().length();
            if(!oa.error) {
                oa.op_type = op;
                Misc.runUndoable(() -> {
                    Misc.op_shift(oa, false, amount);
                });
            }
        }
    }

    private static class moveCopy extends AbstractColonAction
    {
    private final boolean doMove;
    moveCopy(boolean doMove)
    {
        this.doMove = doMove;
    }

    @Override
    public EnumSet<CcFlag> getFlags()
    {
        return EnumSet.of(CcFlag.RANGE);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        ColonEvent cev = (ColonEvent) e;
        ViTextView tv = cev.getViTextView();
        final Buffer buf = tv.getBuffer();
        if(cev.getLine1() > buf.getLineCount()
                || cev.getLine2() > buf.getLineCount()) {
            Msg.emsg(Messages.e_invrange);
            return; // BAIL
        }
        final int offset1 = buf.getLineStartOffset(cev.getLine1());
        final int offset2 = buf.getLineEndOffset(cev.getLine2());
        // get the destination line number
        MutableInt dst = new MutableInt();
        if(cev.getNArg() < 1
              || ColonCommands.get_address(cev.getArg(1), 0, false, dst) < 0
              || dst.getValue() > buf.getLineCount()) {
            Msg.emsg(Messages.e_invaddr);
            return; // BAIL
        }
        if(doMove && dst.getValue() == cev.getLine2())
            return; // 2,4 mo 4 does nothing

        final int dstOffset = dst.getValue() == 0
                ? 0
                : buf.getLineEndOffset(dst.getValue());
        if(doMove && dstOffset >= offset1 && dstOffset < offset2) {
            Msg.emsg("Move lines into themselves");
            return; // BAIL
        }

        // If at the end of the file, then can't delete the final
        // '\n' on a move, so back up the range by a character
        final int atEndAdjust = cev.getLine2() == buf.getLineCount() ? 1 : 0;

        // track postions for later delete
        Misc.runUndoable(() -> {
            try {
                ViMark pos1 = doMove
                        ? buf.createMark(offset1, BIAS.FORW) : null;
                ViMark pos2 = doMove
                        ? buf.createMark(offset2, BIAS.FORW) : null;
                String s = buf.getText(offset1, offset2 - offset1);
                buf.insertText(dstOffset, s);
                if(doMove) {
                    assert(pos1 != null && pos2 != null);
                    buf.deleteChar(pos1.getOffset() - atEndAdjust,
                            pos2.getOffset() - atEndAdjust);
                }
            } catch (ViBadLocationException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }
    }

    private static class TestGlassKeys implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            ViManager.getFactory().startGlassKeyCatch(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    e.consume();
                    if(e.getKeyCode() == KeyEvent.VK_Y) {
                        ViManager.getFactory().stopGlassKeyCatch();
                        Msg.clearMsg();
                    } else {
                        beep_flush();
                    }
                }
            });
            Msg.smsg("Enter 'y' to proceed");
        }
    };

    private static class Undo implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            G.curbuf.undo();
        }
    };

    private static class Redo implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            G.curbuf.redo();
        }
    };

    static private class CloneAction implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            Misc01.win_clone();
        }
    }

    private static class TestModalKeys implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            Msg.smsg("Enter 'y' to proceed");
            ViManager.getFactory().startModalKeyCatch(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    e.consume();
                    if(e.getKeyCode() == KeyEvent.VK_Y) {
                        ViManager.getFactory().stopModalKeyCatch();
                    } else {
                        beep_flush();
                    }
                }
            });
            Msg.clearMsg();
        }
    };

private static void addDebugColonCommands()
{
    //
    // Some debug commands
    //
    ColonCommands.register("dumpPreferences", "dumpPreferences",
            (ActionEvent e) -> {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ViManager.getFactory().getPreferences().exportSubtree(os);
            try (ViOutputStream vios = ViManager.createOutputStream("Preferences")) {
                vios.println(os.toString());
            }

        } catch (BackingStoreException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    },  EnumSet.of(CcFlag.DBG, CcFlag.NO_ARGS));
    ColonCommands.register("optionsDelete", "optionsDelete",
            (ActionEvent e) -> {
        try {
            Preferences prefs = ViManager.getFactory().getPreferences();
            String keys[] = prefs.keys();
            for (String key : keys) {
                prefs.remove(key);
            }
            prefs = prefs.node(ViManager.PREFS_KEYS);
            keys = prefs.keys();
            for (String key : keys) {
                prefs.remove(key);
            }
        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    },  EnumSet.of(CcFlag.DBG));
    ColonCommands.register("optionDelete", "optionDelete",
        new ColonCommands.AbstractColonAction() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent) ev;
                
                if(cev.getNArg() != 1) {
                    Msg.emsg("optionDelete takes exactly one argument");
                    return;
                }
                String key = cev.getArg(1);
                Option<?> opt = Options.getOption(key);
                eatme(opt);
                if(Options.getOption(key) == null) {
                    Msg.emsg("No such option: " + key);
                    return;
                }
                Preferences prefs = ViManager.getFactory().getPreferences();
                List<String> keys;
                try {
                    keys = Arrays.asList(prefs.keys());
                    if(keys.contains(key)) {
                        prefs.remove(key);
                    } else {
                        Msg.smsg("option is not set: " + key);
                    }
                } catch(BackingStoreException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }, EnumSet.of(CcFlag.DBG));
    ColonCommands.register("disabledCommand", "disabledCommand",
        new ColonCommands.AbstractColonAction() {

        @Override
        public boolean isEnabled()
        {
            return false;
        }
        
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.NO_ARGS);
        }
        
        @Override
        public void actionPerformed(ActionEvent ev) {
            Msg.emsg("***** executing !isEnabled command *****");
        }
    }, EnumSet.of(CcFlag.DBG));
    ColonCommands.register("dumpCommands", "dumpCommands",
            (ActionEvent e) -> {
        try (ViOutputStream vios = ViManager.createOutputStream("Dump Commands")) {
            for(ColonCommandItem cci : ColonCommands.getList()) {
                vios.println(String.format("\t%s%s%s",
                        cci.getDisplayName(),
                        cci.getFlags().contains(CcFlag.DBG) ? " debug" : "",
                        cci.getFlags().contains(CcFlag.DEPRECATED) ? " deprec" : "",
                        cci.getFlags().contains(CcFlag.NO_ARGS) ? "" : " ..."
                ));
            }
        }
    },  EnumSet.of(CcFlag.DBG, CcFlag.NO_ARGS));
    ColonCommands.register("dumpOptions", "dumpOptions",
            (ActionEvent e) -> {
        try (ViOutputStream vios = ViManager.createOutputStream("Dump Options")) {
            // sort by display name
            Comparator<Option<?>> comp = (Option<?> o1, Option<?> o2)
                    -> o1.getDisplayName().compareTo(o2.getDisplayName());
            List<Option<?>> opts = new ArrayList<>(OptUtil.getOptions());
            Collections.sort(opts, comp);
            for(Option<?> opt : opts) {
                vios.println(String.format("\t%s (%s) [%s]",
                        opt.getDisplayName(),
                        opt.getCategory(),
                        opt.getName()));
            }
        }
    },  EnumSet.of(CcFlag.DBG, CcFlag.NO_ARGS));

    ColonCommands.register("echolog", "echolog",
        new ColonCommands.AbstractColonAction() {
            @Override
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;
                                              
                System.err.println(cev.getCommandLine());
            }
    }, EnumSet.of(CcFlag.DBG, CcFlag.XFILE));

    // all the msg Q to the log
    ColonCommands.register("echologm", "echologmsg",
        new ColonCommands.AbstractColonAction() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;
                eatme(cev);
                Msg.nmsg("'echologmsg' Not Implemented");
        }
    }, EnumSet.of(CcFlag.DBG, CcFlag.XFILE));

    // to status line, not msg Q
    ColonCommands.register("ec", "echo",
        new ColonCommands.AbstractColonAction() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;

                Msg.nmsg(cev.getArgString());
        }
    }, EnumSet.of(CcFlag.DBG, CcFlag.XFILE));

    // to output window
    ColonCommands.register("echoo", "echoout",
        new ColonCommands.AbstractColonAction() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;

                try (ViOutputStream osa = ViManager.createOutputStream(null)) {
                    osa.println(cev.getCommandLine());
                }
        }
    }, EnumSet.of(CcFlag.DBG, CcFlag.XFILE));

    // to status line and msg Q
    ColonCommands.register("echom", "echomsg",
        new ColonCommands.AbstractColonAction() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;

                Msg.smsg(cev.getArgString());
        }
    }, EnumSet.of(CcFlag.DBG, CcFlag.XFILE));

} // end
}
