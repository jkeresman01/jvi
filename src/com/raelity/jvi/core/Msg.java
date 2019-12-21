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

/**
 * A utility class for displaying messages and status
 * within the current window's status bar.
 */
public class Msg
{

    /**
     *  Display a status message.
     */
    public static void smsg( String msg, Object ... args )
    {
        // VV_STATUSMSG
        String s = args.length == 0 ? msg : String.format(msg, args);
        G.curwin.getStatusDisplay().displayStatusMessage(s);
    }


    /**
     *  Display a warning message.
     */
    public static void wmsg( String msg, Object ... args )
    {
        // VV_STATUSMSG
        String s = args.length == 0 ? msg : String.format(msg, args);
        G.curwin.getStatusDisplay().displayWarningMessage(s);
    }


    /**
     *  Display an error message.
     */
    public static void emsg( String msg, Object... args )
    {
        // VV_ERRMSG; HLF_E highlight
        String s = args.length == 0 ? msg : String.format(msg, args);
        G.curwin.getStatusDisplay().displayErrorMessage(s);
        GetChar.flush_buffers(false);
    }


    /**
     *  Display a frozen message.
     */
    public static void fmsg( String msg, Object ... args )
    {
        String s = args.length == 0 ? msg : String.format(msg, args);
        G.curwin.getStatusDisplay().displayFrozenMessage(s);
    }


    /**
     *  Clear the message display.
     */
    public static void clearMsg()
    {
        G.curwin.getStatusDisplay().clearMessage();
    }

    private Msg()
    {
    }


} // end
