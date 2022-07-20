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

package com.raelity.jvi.lib;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Most recently used collection, the only methods that modify this
 * collection is {@code addItem}, {@code removeItem},
 * or {@code addAll}; other attempts to modify throw.
 * Implementations are expected to be
 * unmodifieable except as noted above.
 * Only one of a given element is allowed, like a set.
 * The iteration order is most recent first.
 * <p>
 * Implementations in {@link AbstractMRU}
 * There is synchronizedMRU.
 * <pre>
 * performance for available implementations
 * 
 *               LinkedList                 LinkedHashSet
 * 
 * removeItem    loopfind                   treefind-remove
 *
 * 
 * addItem      loopfind                    treefind-remove
 *              list insert                 treefind-insert
 *
 * forEach      direct                      create array list
 *
 * trim         removeLast()                it.remove()
 *
 * iterator     direct                      create array list
 * </pre>
 */
public interface MRU<E> extends Collection<E>
{

/**
 * Implementation based on LinkedHashSet. Arg limit is used
 * for trim(); may be null which means no limit.
 */
public static <E>MRU<E> getSetMRU(Supplier<Integer> limit)
{
    return new AbstractMRU.LinkedHashSetMRU<>(limit);
}

/**
 * Implementation based on LinkedList. Arg limit is used
 * for trim(); may be null which means no limit.
 */
public static <E>MRU<E> getListMRU(Supplier<Integer> limit)
{
    return new AbstractMRU.LinkedListMRU<>(limit);
}

/** Returns true if the collection changed.
 * Typically only returns false if the current mru item is the argument.
 */
boolean addItem(E key);

/** contract like remove(E) */
boolean removeItem(E key);

/** Remove LRU items until within limit. return true if modified */
boolean trim();

/**
 * The limitations/restrictions specified in 
 * {@code Collections.synchronizedCollection}
 * apply.
 */
public static <E> MRU<E> synchronizedMRU(MRU<E> mru)
{
    return new AbstractMRU.SynchronizedMRU<>(mru);
}

} // END INTERFACE MRU ////////////////////////////////////////
