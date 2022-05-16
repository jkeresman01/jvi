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

package com.raelity.jvi.core.lib;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author err
 */
public enum Cmd
{
BANG("!");

private static Map<String, Cmd> cmds;

@SuppressWarnings("LeakingThisInConstructor")
private Cmd(String abbrev)
{
    stash(abbrev, this);
}

private static void stash(String abbrev, Cmd cmd)
{
    if(cmds == null)
        cmds = new HashMap<>(100);
    cmds.put(abbrev, cmd);
}

static Cmd findCmd(String abbrev)
{
    return cmds.get(abbrev);
}

}
