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

package com.raelity.jvi.cmd;

import java.io.File;

import com.raelity.jvi.ViTextView;
import com.raelity.jvi.swing.simple.SimpleBuffer;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class PlayBuffer extends SimpleBuffer
{

    public PlayBuffer(ViTextView tv)
    {
        super(tv);
    }

    @Override
    public boolean isDirty()
    {
        return true;
    }

    @Override
    public File getFile()
    {
        return new File("/tmp/test.file");
    }
}
