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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Most recently used collection.
 * With bulk operations the most recent is first in the Collection.
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
public interface MRU<T>
{

public static <T>MRU<T> getSetMRU(Supplier<Integer> limit)
{
    return new LinkedHashSetMRU<>(limit);
}

public static <T>MRU<T> getListMRU(Supplier<Integer> limit)
{
    return new LinkedListMRU<>(limit);
}

/** Must be the first call; otherwise errors. */
void initialize(Collection<T> l);

void addItem(T key);

void removeItem(T key);

void trim();

void forEach(Consumer<T> action);

/** This iterator does not allow modification; may reference the collection. */
Iterator<T> closingIterator();

/** return copy, most recently added is first in iteration */
Collection<T> copy();

//////////////////////////////////////////////////////////////////////
//
// Implementations
//

/** Simpler implementation, if mostly accessing mru,
 * then this is probably the best.
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})
public static class LinkedListMRU<T>
        implements MRU<T>
{
private LinkedList<T> delegate;
private final Supplier<Integer> limit;
private boolean closing;

public LinkedListMRU(Supplier<Integer> limit)
{
    this.limit = limit;
}

@Override
synchronized public void initialize(Collection<T> l)
{
    if(delegate != null)
        throw new IllegalStateException("Allready initialized");
    delegate = new LinkedList<>(l);
}

/** copy */
@Override
synchronized public List<T> copy()
{
    if(closing)
        throw new IllegalStateException("Closing");
    return new ArrayList<>(delegate);
}

@Override
synchronized public void forEach(Consumer<T> action)
{
    if(closing)
        throw new IllegalStateException("Closing");
    delegate.iterator().forEachRemaining(action);
}

@Override
synchronized public Iterator<T> closingIterator()
{
    closing = true;
    ListIterator<T> it = delegate.listIterator(delegate.size());
    return new Iterator<T>()
    {
        @Override
        public boolean hasNext()
        {
            return it.hasNext();
        }
        
        @Override
        public T next()
        {
            return it.next();
        }
    };
}

@Override
synchronized public void addItem(T key)
{
    if(closing)
        throw new IllegalStateException("Closing");
    delegate.remove(key);
    delegate.addFirst(key);
    trim();
}

@Override
synchronized public void removeItem(T key)
{
    if(closing)
        throw new IllegalStateException("Closing");
    delegate.remove(key);
}

@Override
synchronized public void trim()
{
    if(closing)
        throw new IllegalStateException("Closing");
    if(limit == null)
        return;
    int iLimit = limit.get();
    if(iLimit < 0)
        iLimit = 0;
    while(delegate.size() > iLimit)
        delegate.removeLast();
}
} // END CLASS ClosedfilesListMRU ////////////////////////////////////////


/**
 * This is best if random lookup is most important.
 * But probably most lookup/remove is recent.
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})
public static class LinkedHashSetMRU<T>
        implements MRU<T>
{
private LinkedHashSet<T> delegate;
private final Supplier<Integer> limit;
private boolean closing;

public LinkedHashSetMRU(Supplier<Integer> limit)
{
    this.limit = limit;
}

@Override
synchronized public void initialize(Collection<T> l)
{
    if(delegate != null)
        throw new IllegalStateException("Allready initialized");
    List<T> t;
    if(l instanceof List)
        t = (List<T>)l;
    else
        t = new ArrayList<>(l);
    
    Collections.reverse(t);
    delegate = new LinkedHashSet<>(t);
}

@Override
synchronized public List<T> copy()
{
    if(closing)
        throw new IllegalStateException("Closing");
    ArrayList<T> l = new ArrayList<>(delegate);
    Collections.reverse(l);
    return l;
}

@Override
synchronized public void forEach(Consumer<T> action)
{
    if(closing)
        throw new IllegalStateException("Closing");
    ArrayList<T> l = new ArrayList<>(delegate);
    ListIterator<T> it = l.listIterator(l.size());
    while(it.hasPrevious())
        action.accept(it.previous());
}

@Override
synchronized public Iterator<T> closingIterator()
{
    closing = true;
    ArrayList<T> l = new ArrayList<>(delegate);
    ListIterator<T> it = l.listIterator(l.size());
    return new Iterator<T>()
    {
        @Override
        public boolean hasNext()
        {
            return it.hasPrevious();
        }
        
        @Override
        public T next()
        {
            return it.previous();
        }
    };
}

@Override
synchronized public void addItem(T key)
{
    if(closing)
        throw new IllegalStateException("Closing");
    // need to remove first, otherwise won't change list position
    removeItem(key);
    delegate.add(key);
    trim();
}

@Override
synchronized public void removeItem(T key)
{
    if(closing)
        throw new IllegalStateException("Closing");
    delegate.remove(key);
}

@Override
synchronized public void trim()
{
    if(closing)
        throw new IllegalStateException("Closing");
    if(limit == null || delegate.size() <= limit.get())
        return;
    
    Iterator<T> it = delegate.iterator();
    int iLimit = limit.get();
    if(iLimit < 0)
        iLimit = 0;
    while(delegate.size() > iLimit) {
        //System.err.println("CLOSED: trim: curlen " + size());
        it.next();
        it.remove();
    }
}

} // END CLASS ClosedfilesLinkedSetMRU ////////////////////////////////////////

} // END INTERFACE MRU ////////////////////////////////////////
