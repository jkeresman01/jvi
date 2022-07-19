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

import java.util.AbstractCollection;
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
import java.util.stream.Stream;

/**
 * Most recently used collection, the only method that modifies this
 * collecion is {@code addItem}; other methods throw.
 * The iteration order is most recent first.
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
// TODO: could make this a collection with lots of stuff throw exception
public interface MRU<E> extends Collection<E>
{

public static <T>MRU<T> getSetMRU(Supplier<Integer> limit)
{
    return new LinkedHashSetMRU<>(limit);
}

public static <T>MRU<T> getListMRU(Supplier<Integer> limit)
{
    return new LinkedListMRU<>(limit);
}

boolean isReady();

/** Must be the first call; otherwise errors. */
void initialize(Collection<E> l);

void addItem(E key);

void removeItem(E key);

void trim();

/** This iterator does not allow modification; may reference the collection
 * directly. Once it is called, no other operations are allowed.
 */
Iterable<E> closingIterable();

Stream<E> closingStream();


//////////////////////////////////////////////////////////////////////
//
// Implementations
//

public abstract static class AbstractMRU<E> extends AbstractCollection<E>
        implements MRU<E>
{
protected Collection<E> delegate;
protected final Supplier<Integer> limit;
protected boolean closing;

public AbstractMRU(Supplier<Integer> limit)
{
    this.limit = limit;
}

@Override
public boolean isReady()
{
    return delegate != null;
}

@Override
abstract public void initialize(Collection<E> l);

@Override
synchronized public void forEach(Consumer<? super E> action)
{
    if(closing)
        throw new IllegalStateException("Closing");
    super.forEach(action);
}

//////////////////////////////////////////////////
// TODO: get rid of the closingXxx
@Override
synchronized public Stream<E> closingStream()
{
    closing = true;
    return stream();
}

@Override
public Iterable<E> closingIterable()
{
    return this::closingIterator;
    //return () -> closingIteratorXXX();
}

synchronized private Iterator<E> closingIterator()
{
    closing = true;
    return iterator();
}
// TODO: get rid of the closingXxx
//////////////////////////////////////////////////

abstract protected void insert(E key);

@Override
synchronized public void addItem(E key)
{
    if(closing)
        throw new IllegalStateException("Closing");
    delegate.remove(key);
    insert(key);
    trim();
}

@Override
synchronized public void removeItem(E key)
{
    if(closing)
        throw new IllegalStateException("Closing");
    delegate.remove(key);
}

@Override
public int size()
{
    return delegate.size();
}

protected abstract void doTrim(int iLimit);

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
    doTrim(iLimit);
}
} // END CLASS AbstractMRU ////////////////////////////////////////

/** Simpler implementation, if mostly accessing mru,
 * then this is probably the best.
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})
public static class LinkedListMRU<E> extends AbstractMRU<E>
        implements MRU<E>
{

public LinkedListMRU(Supplier<Integer> limit)
{
    super(limit);
    //this.limit = limit;
}

@Override
synchronized public void initialize(Collection<E> l)
{
    if(delegate != null)
        throw new IllegalStateException("Allready initialized");
    delegate = new LinkedList<>(l);
}

private LinkedList<E> getDelegate()
{
    return (LinkedList<E>)delegate;
}

@Override
protected void insert(E key)
{
    getDelegate().addFirst(key);
}

@Override
public Iterator<E> iterator()
{
    ListIterator<E> it = getDelegate().listIterator(delegate.size());
    return new Iterator<E>()
    {
        @Override
        public boolean hasNext()
        {
            return it.hasPrevious();
        }
        
        @Override
        public E next()
        {
            return it.previous();
        }
    };
}

@Override
protected void doTrim(int iLimit)
{
    while(delegate.size() > iLimit)
        getDelegate().removeLast();
}
} // END CLASS ClosedfilesListMRU ////////////////////////////////////////


/**
 * This is best if random lookup is most important
 * or large datasets.
 * But probably most lookup/remove is recent.
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})
public static class LinkedHashSetMRU<E> extends AbstractMRU<E>
        implements MRU<E>
{

public LinkedHashSetMRU(Supplier<Integer> limit)
{
    super(limit);
    //this.limit = limit;
}

@Override
synchronized public void initialize(Collection<E> l)
{
    if(delegate != null)
        throw new IllegalStateException("Allready initialized");
    List<E> t;
    if(l instanceof List)
        t = (List<E>)l;
    else
        t = new ArrayList<>(l);
    
    Collections.reverse(t);
    delegate = new LinkedHashSet<>(t);
}

private LinkedHashSet<E> getDelegate()
{
    return (LinkedHashSet<E>)delegate;
}

@Override
protected void insert(E key)
{
    getDelegate().add(key);
}

@Override
public Iterator<E> iterator()
{
    ArrayList<E> l = new ArrayList<>(delegate);
    ListIterator<E> it = l.listIterator(l.size());
    return new Iterator<E>()
    {
        @Override
        public boolean hasNext()
        {
            return it.hasPrevious();
        }
        
        @Override
        public E next()
        {
            return it.previous();
        }
    };
}

@Override
protected void doTrim(int iLimit)
{
    Iterator<E> it = delegate.iterator();
    while(delegate.size() > iLimit) {
        //System.err.println("CLOSED: trim: curlen " + size());
        it.next();
        it.remove();
    }
}

} // END CLASS ClosedfilesLinkedSetMRU ////////////////////////////////////////

} // END INTERFACE MRU ////////////////////////////////////////
