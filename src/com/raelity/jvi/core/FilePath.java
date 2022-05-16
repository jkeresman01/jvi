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
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.EnumSet;

import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.ColonCommands.AbstractColonAction;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.lib.*;
import com.raelity.text.StringSegment;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isExecutable;

/**
 * Vim has filepath.c::modify_fname
 * @author err
 */
public class FilePath
{

private FilePath() { }

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=2)
    public static class Init implements ViInitialization
    {
    @Override
    public void init()
    {
        FilePath.init();
    }
    }

private static void init()
{
    ColonCommands.register("pw", "pwd", new Pwd(), null);
    ColonCommands.register("cd", "cd", new Cd(), null);

    Path path = FileSystems.getDefault().getPath("");
    cwd = path.toAbsolutePath();
}

    private static class Pwd extends AbstractColonAction
    {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            // The vim cd cmd use bang in strange circumstances, allow it, ignore it
            return EnumSet.of(CcFlag.NO_ARGS);
        }
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ColonEvent cev = (ColonEvent) e;

            if(cev.getNArg() != 0) {
                Msg.emsg(Messages.e_trailing);
                return;
            }
            Msg.smsg(cwd.toString());
        }
    }

    private static class Cd extends AbstractColonAction
    {
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            // The vim cd cmd use bang in strange circumstances, allow it, ignore it
            return EnumSet.of(CcFlag.BANG, CcFlag.COMPL_FN, CcFlag.XFILE);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ColonEvent cev = (ColonEvent) e;
            
            if(cev.getNArg() > 1) {
                Msg.emsg("Too many arguments.");
                return;
            }
            if(cev.getNArg() == 0)
                doCwd(getHomeDir().toString(), true);
            else
                doCwd(cev.getArg(1), true);
        }
    }

/**
 * The "virtual" cwd used for running ":!" commands.
 * This should always be the result of toRealPath for consistency.
 */
private static Path cwd;
private static final Path tilde = FileSystems.getDefault().getPath("~");

public static Path getCwd() {
    if(cwd != null)
        return cwd;
    throw new IllegalStateException("cwd is not set");
}

private static Path getCheckPathFS(String s)
{
    Path path;
    try {
        path = FileSystems.getDefault().getPath(s);
    } catch (InvalidPathException ex) {
        Exceptions.printStackTrace(ex);
        path = FileSystems.getDefault().getPath("/tmp");
    }
    return path;
}

public static Path getPath(String s)
{
    Path path = getCheckPathFS(s);
    if(path.startsWith(tilde)) {
        Path in_home = getHomeDir();
        if(path.getNameCount() > 1)
            in_home = in_home.resolve(path.subpath(1, path.getNameCount()));
        path = in_home;
    }
    if(!path.isAbsolute()) {
        // do it again, make a path relative to curdir
        // TODO: this may be nonsense. A non absolute may be
        //       just the components, and what they're under
        //       is figured out at a later time.
        //       It does track the filesystem.

        path = cwd.resolve(path).toAbsolutePath().normalize();
        if(path.startsWith(cwd))
            path = cwd.relativize(path);
    }
    return path;
}

public static Path getRealPath(Path path, LinkOption... option)
throws IOException
{
    return getAbsolutePath(path).toRealPath(option);
}

public static Path getAbsolutePath(Path path)
{
    if(path.isAbsolute())
        return path;
    if(path.startsWith(tilde)) {
        Path tpath = getHomeDir();
        if(path.getNameCount() > 1)
            tpath = tpath.resolve(path.subpath(1, path.getNameCount()));
        return tpath;
    }
    return cwd.resolve(path);
}

public static boolean isAbsolutePath(Path path)
{
    return path.isAbsolute() || path.startsWith(tilde);
}

public static Path getHomeDir()
{
    Path path = FileSystems.getDefault().getPath(System.getProperty("user.home"));
    if(!path.isAbsolute()) {
        path = FileSystems.getDefault().getPath("/tmp");
        Msg.emsg("'user.home' must be absolute path, not '%s'",
                 System.getProperty("user.home"));
    }
                
    return path;
}

public static Path getTildePath(String s)
{
    return tilde.resolve(s);
}

/** The cd command */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
private static String doCwd(String dst_dir, boolean display, Object... junk)
{
    String exMsg = "";
    Path path;

    path = getPath(dst_dir);

    if(!path.isAbsolute())
        path = cwd.resolve(path);

    if(!path.isAbsolute())
        exMsg = ": not absolute path";
    else if (!isDirectory(path))
        exMsg = ": not directory";
    else if (!isExecutable(path))
        exMsg = ": not accessible";
    else {
        cwd = path;
        if (display)
            Msg.smsg(cwd.toString());
        return exMsg;
    }
    String msg = "";
    if (display || junk.length > 0) {
        msg = String.format("Can't cd to directory '%s'%s", dst_dir, exMsg);
        if(display)
            Msg.emsg(msg);
    }
    return msg;
}

static void setCwdTEST(String dir)
{
    Path path = FileSystems.getDefault().getPath(dir);
    if(!path.isAbsolute())
        throw new IllegalArgumentException("'" + dir + "' is not a directory");
    cwd = path;
}

static String setCwdTEST(String dir, boolean display)
{
    return doCwd(dir, display, Boolean.TRUE);
}

public static int VALID_PATH = 1;
public static int VALID_HEAD = 2;

/**
 * Apply modifiers to the argument path.
 * The StringSegment "points" to the first possible modifier ":" and the pointer
 * is advanced to point to the character after the last processed modifier.
 * @see VIM: ":help filename-modifiers"
 * @see VIM: filepath.c::modify_fname
 * @param modifiers modifier commands embedded in a StringSegment
 * @param path starting path
 * @param fnamep result put back here
 * @param mod_count return number of characters processed
 * @return some kind of vim status info: VALID_PATH and/or VALID_HEAD
 */
static int modify_fname(StringSegment modifiers, Path path, Wrap<Path> fnamep)
{
    Path new_path = path.resolve("");
    boolean used_modifier = true;
    int valid = 0;
    boolean do_tilde = false;
    
    // multiple modifies can be used, only in order
    char option;
    boolean try_stuff = modifiers.current() == ':';

    main: if(try_stuff) do {
        option = modifiers.next(); used_modifier = false;
        
        if('p' == option) {
            used_modifier = true;
            valid |= VALID_PATH;
            new_path = getAbsolutePath(new_path);
            if(modifiers.next() != ':') break main;
            option = modifiers.next(); used_modifier = false;
        }
        
        while('~' == option || '.' == option) {
            used_modifier = true;
            Path relroot;
            if('~' == option) {
                relroot = getHomeDir();
                do_tilde = true;
            } else {
                relroot = getCwd();
            }
            Path new_abs =  getAbsolutePath(new_path);
            if(new_abs.startsWith(relroot)) {
                new_path = relroot.relativize(new_abs);
                if(do_tilde) {
                    new_path = tilde.resolve(new_path);
                }
            }
            if(modifiers.next() != ':') break main;
            option = modifiers.next(); used_modifier = false;
        }
        
        while('h' == option) {
            used_modifier = true;
            valid |= VALID_HEAD;
            Path parent = new_path.getParent();
            if(parent != null)
                new_path = parent;
            else {
                if(!new_path.isAbsolute())
                    new_path = FileSystems.getDefault().getPath("");
            }
            if(modifiers.next() != ':') break main;
            option = modifiers.next(); used_modifier = false;
        }
        
        if('t' == option) {
            used_modifier = true;
            Path tail = new_path.getFileName();
            if(tail != null)
                new_path = tail;
            if(modifiers.next() != ':') break main;
            option = modifiers.next(); used_modifier = false;
        }

        // Multiple ':e' is weird because it is the only case where a 2nd
        // can not be derived from the first; need to keep special state.
        // Ex: "a.b.c.d":e --> "d", "a.b.c.d":e:e --> "c.d".
        //
        // inner_e_r accumulates ':e' changes, if flip to ':r'
        // then exit inner, fix things up and have another go with outer
        //
        // NOTE: the labels are actually never referenced.
        outer_e_r: while('e' == option || 'r' == option) {
            int extension_idx = 10_000;
            boolean did_e = false;
            boolean did_r = false;
            String str_name = null;
            inner_e_r: while('e' == option || 'r' == option) {
                used_modifier = true;
                if(str_name == null)
                    str_name = new_path.getFileName().toString();
                if('e' == option) {
                    did_e = true;
                    // recall str_path has no path separators
                    int t = str_name.lastIndexOf('.', extension_idx - 1);
                    if(t > 0)
                        // collect another extension
                        extension_idx = t;
                    else if (extension_idx == 10_000)
                        // no '.' or first char '.' and no other '.' in string
                        // result is empty string
                        extension_idx = -1;
                } else {    // 'r'
                    did_r = true;
                    String root = removeExtension(str_name);
                    if(!root.isEmpty())
                        str_name = root;
                }
                // Handled one 'e' or 'r' modifier.
                // inner loop for another if same modifier.
                option = 0; // set it to NUL in case looping.
                if(modifiers.next() != ':') break;
                option = modifiers.next(); used_modifier = false;
                // If flipping between 'e' and 'r', exit inner and fixup,
                // then do the outer loop.
                if(did_e && option != 'e')
                    break;
                if(did_r && option != 'r')
                    break;
            }
            // re-incorporate str_name into path being built
            if(str_name != null) {
                // saw 'e' or 'r'
                if(did_e) {
                    if(extension_idx != 10_000) {
                        // did something with ':e'
                        if(extension_idx < 0)
                            str_name = "";
                        else {
                            // extension_idx pointing at '.', get past it
                            extension_idx++;
                            if(extension_idx < str_name.length())
                                str_name = str_name.substring(extension_idx);
                        }
                    }
                    new_path = FileSystems.getDefault().getPath(str_name);
                } else
                    new_path = new_path.resolveSibling(str_name);
            }
            // do outer loop after re-syncing new_path
        }
    } while(false);
    if(!used_modifier)
        modifiers.previous();
    fnamep.setValue(new_path);
    return valid;
}

public static String getBaseName(String filename)
{
    return removeExtension(getName(filename));
}

public static int indexOfLastSeparator(String filename)
{
    if(filename == null) {
        return -1;
    } else {
        int lastUnixPos = filename.lastIndexOf('/');
        int lastWindowsPos = filename.lastIndexOf('\\');
        return Math.max(lastUnixPos, lastWindowsPos);
    }
}

public static String getName(String filename)
{
    if(filename == null) {
        return null;
    } else {
        int index = indexOfLastSeparator(filename);
        return filename.substring(index + 1);
    }
}

public static String removeExtension(String filename)
{
    if(filename == null) {
        return null;
    } else {
        int index = indexOfExtension(filename);
        return index == -1 ? filename : filename.substring(0, index);
    }
}

public static int indexOfExtension(String filename)
{
    if(filename == null) {
        return -1;
    } else {
        int extensionPos = filename.lastIndexOf('.');
        int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? -1 : extensionPos;
    }
}
}
