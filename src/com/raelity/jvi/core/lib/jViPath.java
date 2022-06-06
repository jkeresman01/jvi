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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

import static com.raelity.jvi.core.VimPath.getCwd;

/**
 * NOT USED, jVi should use methods in FilePath.
 * Though kind of interesting.
 * @author err
 */
public class jViPath implements Path
{
private final Path path;

jViPath(Path path)
{
    this.path = path;
}

private jViPath get(Path path)
{
    return new jViPath(path);
}

public static Path of(String first, String... more)
{
    return Path.of(first, more);
}

public static Path of(URI uri)
{
    return Path.of(uri);
}

@Override
public FileSystem getFileSystem()
{
    return path.getFileSystem();
}

@Override
public boolean isAbsolute()
{
    return path.isAbsolute();
}

@Override
public Path getRoot()
{
    return get(path.getRoot());
}

@Override
public Path getFileName()
{
    return get(path.getFileName());
}

@Override
public Path getParent()
{
    return get(path.getParent());
}

@Override
public int getNameCount()
{
    return path.getNameCount();
}

@Override
public Path getName(int index)
{
    return get(path.getName(index));
}

@Override
public Path subpath(int beginIndex, int endIndex)
{
    return get(path.subpath(beginIndex, endIndex));
}

@Override
public boolean startsWith(Path other)
{
    return path.startsWith(other);
}

@Override
public boolean startsWith(String other)
{
    return path.startsWith(other);
}

@Override
public boolean endsWith(Path other)
{
    return path.endsWith(other);
}

@Override
public boolean endsWith(String other)
{
    return path.endsWith(other);
}

@Override
public Path normalize()
{
    return get(path.normalize());
}

@Override
public Path resolve(Path other)
{
    return get(path.resolve(other));
}

@Override
public Path resolve(String other)
{
    return get(path.resolve(other));
}

@Override
public Path resolveSibling(Path other)
{
    return get(path.resolveSibling(other));
}

@Override
public Path resolveSibling(String other)
{
    return get(path.resolveSibling(other));
}

@Override
public Path relativize(Path other)
{
    return get(path.relativize(other));
}

@Override
public URI toUri()
{
    return path.toUri();
}

@Override
public Path toAbsolutePath()
{
    if(this.isAbsolute())
        return this;
    return getCwd().resolve(this);
}

@Override
public Path toRealPath(LinkOption... options) throws IOException
{
    return get(toAbsolutePath().toRealPath(options));
}

@Override
public File toFile()
{
    return path.toFile();
}

@Override
public WatchKey register(WatchService watcher,
                             Kind<?>[] events, Modifier... modifiers) throws
        IOException
{
    return path.register(watcher, events, modifiers);
}

@Override
public WatchKey register(WatchService watcher,
                             Kind<?>... events) throws IOException
{
    return path.register(watcher, events);
}

@Override
public Iterator<Path> iterator()
{
    return path.iterator();
}

@Override
public int compareTo(Path other)
{
    return path.compareTo(other);
}

@Override
public boolean equals(Object other)
{
    return path.equals(other);
}

@Override
public int hashCode()
{
    return path.hashCode();
}

@Override
public String toString()
{
    return path.toString();
}

@Override
public void forEach(
        Consumer<? super Path> action)
{
    path.forEach(action);
}

@Override
public Spliterator<Path> spliterator()
{
    return path.spliterator();
}

}
