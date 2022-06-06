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

import java.awt.event.ActionEvent;
import java.util.EnumSet;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Iterables;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.ViOutputStream.COLOR;
import com.raelity.jvi.ViOutputStream.FLAGS;
import com.raelity.jvi.core.ColonCommands.AbstractColonAction;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.manager.*;

import static com.raelity.jvi.core.lib.Messages.*;

/**
 * A utility class for displaying messages and status
 * within the current window's status bar.
 */
public enum Msg
{
    STAT(null),
    WARN(COLOR.WARNING),
    ERR(COLOR.FAILURE);

    COLOR color;
    private Msg(COLOR c) {
        color = c;
    }

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=2)
    public static class Init implements ViInitialization
    {
    @Override
    public void init()
    {
        Msg.init();
    }
    }

    private static void init()
    {
        ColonCommands.register("mes", "messages", new Messages(), null);
    }

    /**
     *  Display a status message, but do not put it into the msg Q.
     * @param msg
     * @param args
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void nmsg( String msg, Object ... args )
    {
        // VV_STATUSMSG
        String s = args.length == 0 ? msg : String.format(msg, args);
        if(ok())
            G.curwin.getStatusDisplay().displayStatusMessage(s);
        else
            System.err.println("jVi: STATUS: " + s);
    }

    /**
     *  Display a status message.
     * @param msg
     * @param args
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void smsg( String msg, Object ... args )
    {
        // VV_STATUSMSG
        String s = args.length == 0 ? msg : String.format(msg, args);
        add(STAT, s);
        if(ok())
            G.curwin.getStatusDisplay().displayStatusMessage(s);
        else
            System.err.println("jVi: STATUS: " + s);
    }


    /**
     *  Display a warning message.
     * @param msg
     * @param args
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void wmsg( String msg, Object ... args )
    {
        // VV_STATUSMSG
        String s = args.length == 0 ? msg : String.format(msg, args);
        add(WARN, s);
        if(ok())
            G.curwin.getStatusDisplay().displayWarningMessage(s);
        else
            System.err.println("jVi: WARN: " + s);
    }


    /**
     *  Display an error message.
     * @param msg
     * @param args
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void emsg( String msg, Object... args )
    {
        // VV_ERRMSG; HLF_E highlight
        String s = args.length == 0 ? msg : String.format(msg, args);
        add(ERR, s);
        if(ok()) {
            G.curwin.getStatusDisplay().displayErrorMessage(s);
            GetChar.flush_buffers(false);
        }
        else
            System.err.println("jVi: ERROR: " + s);
    }


    /**
     *  Display a frozen message.
     * @param msg
     * @param args
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void fmsg( String msg, Object ... args )
    {
        String s = args.length == 0 ? msg : String.format(msg, args);
        if(ok())
            G.curwin.getStatusDisplay().displayFrozenMessage(s);
        else
            System.err.println("jVi MSG: " + s);
    }


    /**
     *  Clear the message display.
     */
    public static void clearMsg()
    {
        if(ok())
            G.curwin.getStatusDisplay().clearMessage();
    }

    private static boolean ok()
    {
        return G.curwin != null && G.curwin.getStatusDisplay() != null;
    }

    ////////////////////////////////////////////////////////////////////////
    //
    // The message Q and its commands

    private static void add(Msg t, String m)
    {
        msgq.add(new MsgItem(t, m));
    }

    static class MsgItem {
    final Msg type;
    final String msg;
    
    MsgItem(Msg type, String msg)
    {
        this.type = type;
        this.msg = msg;
    }
    }
    private static final EvictingQueue<MsgItem> msgq = EvictingQueue.create(200);

    /**
     * Goal is to show tailCount most recent entries in msgQ.
     * Calculate how many to skip.
     * 
     * @param tailCount target to show
     * @return how many to skip
     */
    private static int skipCount(int nDisplay)
    {
        int nQ = msgq.size();
        return nDisplay >= nQ ? 0 : nQ - nDisplay;
    }

    /** Messages to output window */
    private static class Messages extends AbstractColonAction
    {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.RANGE);
        }

    @Override
    public void actionPerformed(ActionEvent ev) {
        ColonEvent cev = (ColonEvent)ev;

        int nDisplay;
        if(cev.getAddrCount() == 0)
            nDisplay = msgq.size();
        else
            nDisplay = cev.getLine1();

        if(cev.getNArg() > 1) {
            emsg(e_trailing);
            return;
        }
        if(cev.getNArg() == 1) {
            if(!cev.getArg(1).equals("clear")) {
                emsg(e_invarg);
                return;
            }
            // take messages out of the Q
            int sk = skipCount(nDisplay);
            if(sk == msgq.size())
                msgq.clear();
            else
                for(int i = sk; i > 0; i--) {
                    msgq.poll();
                }
            return;
        }

        try (ViOutputStream vios = ViManager.createOutputStream(
                null, ViOutputStream.LINES, "Messages",
                EnumSet.of(FLAGS.NEW_NO, FLAGS.RAISE_YES, FLAGS.CLEAR_YES))) {
            for(MsgItem mi : Iterables.skip(msgq, skipCount(nDisplay))) {
                if(mi.type.color == null)
                    vios.println(mi.msg);
                else
                    vios.println(mi.msg, mi.type.color);
            }
        }
    }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static void dump(int n)
    {
        for(MsgItem mi : Iterables.skip(msgq, skipCount(n))) {
            System.out.println(mi.type + "   " + mi.msg);
        }
    }

} // end
