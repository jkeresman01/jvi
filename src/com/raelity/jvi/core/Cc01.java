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

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViTextView.TAGOP;
import com.raelity.jvi.core.ColonCommands.ColonAction;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.lib.MutableInt;
import com.raelity.jvi.manager.AppViews;
import com.raelity.jvi.manager.ViManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import org.openide.util.lookup.ServiceProvider;

import static com.raelity.jvi.core.Constants.*;
import static com.raelity.jvi.core.ColonCommandFlags.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Cc01
{
    private static Logger LOG = Logger.getLogger(Cc01.class.getName());

    @ServiceProvider(service=ViInitialization.class, path="jVi/init")
    public static class Init implements ViInitialization
    {
      public void init()
      {
        Cc01.init();
      }
    }

    private static void init()
    {
        ColonCommands.register("n", "next", ACTION_next);
        ColonCommands.register("N", "Next", ACTION_Next);
        ColonCommands.register("prev", "previous", ACTION_Next);
        ColonCommands.register("clo", "close", ACTION_close);
        ColonCommands.register("on", "only", ACTION_only);

        ColonCommands.register("q", "quit", ACTION_quit);
        ColonCommands.register("w", "write", ACTION_write);
        ColonCommands.register("wq", "wq", ACTION_wq);
        ColonCommands.register("wa", "wall", ACTION_wall);

        ColonCommands.register("files","files", ACTION_BUFFERS);
        ColonCommands.register("buffers","buffers", ACTION_BUFFERS);
        ColonCommands.register("ls","ls", ACTION_BUFFERS);

        ColonCommands.register("f", "file", ACTION_file);
        ColonCommands.register("e", "edit", ACTION_edit);
        ColonCommands.register("s", "substitute", ACTION_substitute);
        ColonCommands.register("g", "global", ACTION_global);
        ColonCommands.register("d", "delete", ACTION_delete);
        ColonCommands.register("p", "print", ACTION_print);

        ColonCommands.register("ju", "jumps", ACTION_jumps);

        ColonCommands.register("ta", "tag", ACTION_tag);
        ColonCommands.register("tags", "tags", ACTION_tags);
        ColonCommands.register("ts", "tselect", ACTION_tselect);
        ColonCommands.register("po", "pop", ACTION_pop);

        ColonCommands.register("noh", "nohlsearch", ACTION_nohlsearch);

        ColonCommands.register("testGlassKeys", "testGlassKeys",
                               ACTION_testGlassKeys);
        ColonCommands.register("testModalKeys", "testModalKeys",
                               ACTION_testModalKeys);

        ColonCommands.register("y", "yank", ACTION_yank);
        // not pretty, limit the number of shifts...
        ColonCommands.register(">", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>",
                               ACTION_rshift);
        ColonCommands.register("<", "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
                               ACTION_lshift);

        ColonCommands.register("m", "move", ACTION_move);
        ColonCommands.register("co", "copy", ACTION_copy);
        ColonCommands.register("t", "t", ACTION_copy);

        addDebugColonCommands();
    }

    static ActionListener getActionPrint() { return ACTION_print; }
    static ActionListener getActionYank() { return ACTION_yank; }
    static ActionListener getActionSubstitute() { return ACTION_substitute; }
    static ActionListener getActionDelete() { return ACTION_delete; }
    static ActionListener getActionGlobal() { return ACTION_global; }

    private static ColonAction ACTION_next = new Next(true);
    private static ColonAction ACTION_Next = new Next(false);

    /** next/Next/previous through MRU list */
    static private class Next extends ColonAction { // NEEDSWORK: count
        boolean goForward;

        Next(boolean goForward) {
            this.goForward = goForward;
        }
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

    private static ActionListener ACTION_quit = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;
                cev.getViTextView().win_quit();
            }};

    private static ActionListener ACTION_close = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;
                // NEEDSWORK: win_close: hidden, need_hide....
                cev.getViTextView().win_close(false);
            }};

    private static ActionListener ACTION_only = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;
                // NEEDSWORK: win_close_others: forceit....
                cev.getViTextView().win_close_others(false);
            }};

    private static ActionListener ACTION_wall = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                ViManager.getFS().writeAll(false);
            }};

    private static ActionListener ACTION_file = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                ColonEvent cev = (ColonEvent)ev;
                Msg.smsg(ViManager.getFS()
                        .getDisplayFileViewInfo(cev.getViTextView()));
            }};

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
    private static ColonAction ACTION_write = new ColonAction() {
            @Override
            public int getFlags()
            {
                return BANG;
            }

            public void actionPerformed(ActionEvent ev)
            {
                do_write((ColonEvent)ev);
            }
        };

    private static ColonAction ACTION_wq = new ColonAction() {
            @Override
            public int getFlags()
            {
                return BANG;
            }

            public void actionPerformed(ActionEvent ev)
            {
                ColonEvent cev = (ColonEvent)ev;
                if(do_write(cev))
                    cev.getViTextView().win_quit();
            }
        };

    /**
        * Edit command. Only ':e#[number]' is supported.
        */
    private static ColonAction ACTION_edit = new ColonAction() {
            @Override
            public int getFlags()
            {
                return BANG;
            }

            public void actionPerformed(ActionEvent ev)
            {
                ColonEvent cev = (ColonEvent)ev;
                /*
                if(cev.getNArg() == 0) {
                    ViManager.getFS().write(cev.mayCreateTextView(), cev.isBang());
                } else
                */
                if(cev.getNArg() == 1 && cev.getArg(1).charAt(0) == '#') {
                    boolean error = false;
                    String arg;
                    int i = -1;
                    arg = cev.getArg(1);
                    if(arg.length() > 1) {
                        arg = arg.substring(1);
                        try {
                            i = Integer.parseInt(arg);
                        } catch(NumberFormatException ex) {
                            error = true;
                        }
                        if(error) {
                            Msg.emsg("Only 'e#[<number>]' allowed");
                        }
                    }
                    if( ! error) {
                        ViAppView av = null;
                        // Look up the appView. If i >= 0 then find app view
                        // with window that matches that number (standard vim).
                        // If i < 0 then use it to index into the mru list.
                        if(i >= 0) {
                            for (ViAppView av1 : AppViews.getList(AppViews.ACTIVE)) {
                                if(i == av1.getWNum()) {
                                    av = av1;
                                    break;
                                }
                            }
                        } else {
                            av = AppViews.getMruAppView(-i);
                        }
                        if(av != null)
                            ViManager.getFS().edit(av, cev.isBang());
                        else
                            Msg.emsg("No alternate file name to substitute for '#"
                                    + i + "'");
                    }
                } else if ( cev.getNArg() < 2) {
                    String fName = cev.getNArg() == 0 ? null : cev.getArg(1);
                    ViManager.getFS().edit(new File(fName), cev.isBang(), null);
                } else {
                    Msg.emsg(":edit only accepts none or one argument");
                }
            }
        };

    private static ColonAction ACTION_substitute = new ColonAction() {
            @Override
            public int getFlags()
            {
                return NOPARSE;
            }

            public void actionPerformed(final ActionEvent ev)
            {
                Misc.runUndoable(new Runnable() {
                    public void run() {
                        Search.substitute((ColonEvent)ev);
                    }
                });
            }
        };

    private static ColonAction ACTION_global = new ColonAction() {
            @Override
            public int getFlags()
            {
                return NOPARSE;
            }

            public void actionPerformed(final ActionEvent ev) {
                Misc.runUndoable(new Runnable() {
                    public void run() {
                        Search.global((ColonEvent)ev);
                        Options.newSearch();
                    }
                });
            }
        };

    /** This is the default buffers,files,ls command. The platform specific code
        * may chose to open this, or implement their own, possibly using
        * popup gui components.
        */
    private static ActionListener ACTION_BUFFERS = new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            ViOutputStream osa = ViManager.createOutputStream(
                    null, ViOutputStream.OUTPUT,
                    "=== MRU (:n :N :e#-<digit>) ===                "
                    + "=== activation (:e#<digit> ===");
            int i = 0;
            ViAppView cur = AppViews.relativeMruAppView(0);
            ViAppView prev = AppViews.getMruAppView(1);

            List<String> outputData = new ArrayList<String>();
            ViFactory factory = ViManager.getFactory();
            List<ViAppView> l1 = AppViews.getList(AppViews.MRU);
            List<ViAppView> l2 = AppViews.getList(AppViews.ACTIVE);
            assert l1.size() == l2.size();
            while(i < l1.size()) {
                //ViAppView o1 = AppViews.getMruAppView(i);
                //ViAppView o2 = AppViews.getAppView(i+1);
                ViAppView o1 = l1.get(i);
                ViAppView o2 = l2.get(i);
                int w2 = o2.getWNum();
                outputData.add(String.format(
                        " %2d %c %-40s %3d %c %s",
                        i,
                        cur != null && cur.equals(o1) ? '%'
                        : prev != null && prev.equals(o1) ? '#' : ' ',
                        o1 != null ? factory.getFS().getDisplayFileName(o1) : "",
                        w2,
                        cur != null && cur.equals(o2) ? '%'
                        : prev != null && prev.equals(o2) ? '#' : ' ',
                        o2 != null ? factory.getFS().getDisplayFileName(o2) : ""));
                i++;
            }
            // print in reverse order, MRU visible if scrolling
            for(int i01 = outputData.size() -1; i01 >= 0; i01--) {
                osa.println(outputData.get(i01));
            }
            osa.close();
        }
    };

    /**
        * :print command. If not busy global then output to "print" stream.
        * For now, its just a no-op so the word "print" can be found.
        */
    private static ColonAction ACTION_print = new ColonAction() {
            public void actionPerformed(ActionEvent ev) {

            }
        };

    /**
        * :tag command.
        */
    private static ColonAction ACTION_tag = new ColonAction() {
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
    private static ColonAction ACTION_pop = new ColonAction() {
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

    private static ColonAction ACTION_tselect = new ColonAction() {
            public void actionPerformed(ActionEvent ev) {
                ColonEvent ce = (ColonEvent)ev;
                if(ce.getNArg() > 1) {
                    Msg.emsg(Messages.e_trailing);
                    return;
                }
                int count = 1;
                if(ce.getAddrCount() == 1)
                    count = ce.getLine2();
                ViManager.getFactory().tagDialog(ce);
            }
        };

    private static ActionListener ACTION_jumps = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                MarkOps.do_jumps();
            }
        };

    private static ActionListener ACTION_tags = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                ViManager.getFactory().displayTags();
            }
        };

    private static ActionListener ACTION_nohlsearch = new ActionListener() {
        public void actionPerformed(ActionEvent ev)
        {
            Options.nohCommand();
        }
    };

    /**
     * :delete command.
     */
    private static ColonAction ACTION_delete = new ColonAction() {
        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            OPARG oa = ColonCommands.setupExop(cev, true);
            if(!oa.error) {
                oa.op_type = OP_DELETE;
                Misc.op_delete(oa);
            }
        }
    };

    /**
     * :yank command.
     */
    private static ColonAction ACTION_yank = new ColonAction() {
        public void actionPerformed(ActionEvent ev)
        {
            OPARG oa = ColonCommands.setupExop((ColonEvent)ev, true);
            if(!oa.error) {
                oa.op_type = OP_YANK;
                Misc.op_yank(oa, false, true);
            }
        }
    };

    private static ColonAction ACTION_lshift = new ShiftAction(OP_LSHIFT);
    private static ColonAction ACTION_rshift = new ShiftAction(OP_RSHIFT);

    private static class ShiftAction extends ColonAction
    {
        final int op;

        public ShiftAction(int op)
        {
            this.op = op;
        }

        public void actionPerformed(ActionEvent ev)
        {
            ColonEvent cev = (ColonEvent)ev;
            final OPARG oa = ColonCommands.setupExop(cev, true);
            final int amount = cev.getInputCommandName().length();
            if(!oa.error) {
                oa.op_type = op;
                Misc.runUndoable(new Runnable() {
                        public void run()
                        {
                            Misc.op_shift(oa, false, amount);
                        }
                    });
            }
        }
    }

    private static class moveCopy extends ColonAction
    {
        private boolean doMove;
        moveCopy(boolean doMove)
        {
            this.doMove = doMove;
        }

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

            final int dstOffset = buf.getLineEndOffset(dst.getValue());
            if(doMove && dstOffset >= offset1 && dstOffset < offset2) {
                Msg.emsg("Move lines into themselves");
                return; // BAIL
            }

            // If at the end of the file, then can't delete the final
            // '\n' on a move, so back up the range by a character
            final int atEndAdjust = cev.getLine2() == buf.getLineCount() ? 1 : 0;

            // track postions for later delete
            Misc.runUndoable(new Runnable() {
                public void run() {
                    try {
                        Position pos1 = doMove
                                ? ((Document)buf.getDocument())
                                            .createPosition(offset1)
                                : null;
                        Position pos2 = doMove
                                ? ((Document)buf.getDocument())
                                            .createPosition(offset2)
                                : null;
                        String s = buf.getText(offset1, offset2 - offset1);
                        buf.insertText(dstOffset, s);
                        if(doMove) {
                            buf.deleteChar(pos1.getOffset() - atEndAdjust,
                                            pos2.getOffset() - atEndAdjust);
                        }
                    } catch (BadLocationException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            });
        }

    }

    private static ColonAction ACTION_move = new moveCopy(true);
    private static ColonAction ACTION_copy = new moveCopy(false);

    private static ActionListener ACTION_testGlassKeys = new ActionListener() {
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
                                Util.vim_beep();
                            }
                        }
                });
                Msg.smsg("Enter 'y' to proceed");
                }
            };

    private static ActionListener ACTION_testModalKeys = new ActionListener() {
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
                                Util.vim_beep();
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
    ColonCommands.register("optionsDump", "optionsDump", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ViManager.getFactory().getPreferences().exportSubtree(os);
            ViOutputStream vios = ViManager.createOutputStream(
                    null, ViOutputStream.OUTPUT, "Preferences");
            vios.println(os.toString());
            vios.close();

        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        }
    });
    ColonCommands.register("optionsDelete", "optionsDelete", new ActionListener() {
        public void actionPerformed(ActionEvent e) {
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
        }
    });
    ColonCommands.register("optionDelete", "optionDelete",
            new ColonCommands.ColonAction() {
                public void actionPerformed(ActionEvent ev) {
                    ColonEvent cev = (ColonEvent) ev;

                    if(cev.getNArg() == 1) {
                    String key = cev.getArg(1);
                    Preferences prefs = ViManager.getFactory().getPreferences();
                    prefs.remove(key);
                    } else
                    Msg.emsg("optionDelete takes exactly one argument");
                }
            });

} // end
}
