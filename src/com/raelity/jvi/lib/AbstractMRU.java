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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class AbstractMRU<E> extends AbstractCollection<E> implements MRU<E>
{
protected final Collection<E> delegate;
protected final Supplier<Integer> limit;

public AbstractMRU(Collection<E> c, Supplier<Integer> limit)
{
    this.delegate = c;
    this.limit = limit;
}

protected abstract void insert(E key);

@Override
public boolean addItem(E key)
{
    if(Objects.equals(key, itemMRU()))
        return false;
    delegate.remove(key);
    insert(key);
    trim();
    return true; // since insert always does
}

@Override
public boolean removeItem(E key)
{
    return delegate.remove(key);
}

@Override
public int size()
{
    return delegate.size();
}

@Override
public boolean addAll(Collection<? extends E> c)
{
    boolean modified = false;
    for(E e : c) {
        if(addItem(e))
            modified = true;
    }
    if(trim())
        modified = true;
    return modified;
}

protected abstract void doTrim(int iLimit);

@Override
public boolean trim()
{
    if(limit == null)
        return false;
    int iLimit = limit.get();
    if(iLimit < 0)
        iLimit = 0;
    int size = size();
    doTrim(iLimit);
    return size != size();
}

abstract protected E itemMRU();

//////////////////////////////////////////////////////////////////////
//
// Implementations
//

/** Simple implementation, if mostly accessing mru,
 * then this is probably the best.
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})
public static class LinkedListMRU<E> extends AbstractMRU<E>
        implements MRU<E>
{

public LinkedListMRU(Supplier<Integer> limit)
{
    super(new LinkedList<E>(), limit);
}

private LinkedList<E> getDelegate()
{
    return (LinkedList<E>)delegate;
}

@Override
protected E itemMRU()
{
    return getDelegate().peekFirst();
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
private E itemMRU;

public LinkedHashSetMRU(Supplier<Integer> limit)
{
    super(new LinkedHashSet<>(), limit);
}

private LinkedHashSet<E> getDelegate()
{
    return (LinkedHashSet<E>)delegate;
}

@Override
protected E itemMRU()
{
    return itemMRU;
}

@Override
protected void insert(E key)
{
    itemMRU = key;
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

} // END CLASS LinkedHashSetMRU ////////////////////////////////////////


@SuppressWarnings("serial")
static class SynchronizedMRU<E>
        extends SynchronizedCollection<E>
        implements MRU<E>, Serializable
{

SynchronizedMRU(MRU<E> c) {
    super(c);
}

private MRU<E>getMRU()
{
    return (MRU<E>)c;
}

@Override
public boolean addItem(E key) {
    synchronized (mutex) {return getMRU().addItem(key);}
}

@Override
public boolean removeItem(E key) {
    synchronized (mutex) {return getMRU().removeItem(key);}
}

@Override
public boolean trim() {
    synchronized (mutex) {return getMRU().trim();}
}
}

/**
 * @serial include
 */
@SuppressWarnings("serial")
static class SynchronizedCollection<E> implements Collection<E>, Serializable {
//@java.io.Serial
//private static final long serialVersionUID = 3053995032091335093L;

@SuppressWarnings("serial") // Conditionally serializable
final Collection<E> c;  // Backing Collection
@SuppressWarnings("serial") // Conditionally serializable
final Object mutex;     // Object on which to synchronize

SynchronizedCollection(Collection<E> c) {
    this.c = Objects.requireNonNull(c);
    mutex = this;
}

SynchronizedCollection(Collection<E> c, Object mutex) {
    this.c = Objects.requireNonNull(c);
    this.mutex = Objects.requireNonNull(mutex);
}

// collection synchronized

public int size() {
    synchronized (mutex) {return c.size();}
}
public boolean isEmpty() {
    synchronized (mutex) {return c.isEmpty();}
}
public boolean contains(Object o) {
    synchronized (mutex) {return c.contains(o);}
}
public Object[] toArray() {
    synchronized (mutex) {return c.toArray();}
}
public <T> T[] toArray(T[] a) {
    synchronized (mutex) {return c.toArray(a);}
}
public <T> T[] toArray(IntFunction<T[]> f) {
    synchronized (mutex) {return c.toArray(f);}
}

public Iterator<E> iterator() {
    return c.iterator(); // Must be manually synched by user!
}

public boolean add(E e) {
    synchronized (mutex) {return c.add(e);}
}
public boolean remove(Object o) {
    synchronized (mutex) {return c.remove(o);}
}

public boolean containsAll(Collection<?> coll) {
    synchronized (mutex) {return c.containsAll(coll);}
}
public boolean addAll(Collection<? extends E> coll) {
    synchronized (mutex) {return c.addAll(coll);}
}
public boolean removeAll(Collection<?> coll) {
    synchronized (mutex) {return c.removeAll(coll);}
}
public boolean retainAll(Collection<?> coll) {
    synchronized (mutex) {return c.retainAll(coll);}
}
public void clear() {
    synchronized (mutex) {c.clear();}
}
public String toString() {
    synchronized (mutex) {return c.toString();}
}
// Override default methods in Collection
@Override
public void forEach(Consumer<? super E> consumer) {
    synchronized (mutex) {c.forEach(consumer);}
}
@Override
public boolean removeIf(Predicate<? super E> filter) {
    synchronized (mutex) {return c.removeIf(filter);}
}
@Override
public Spliterator<E> spliterator() {
    return c.spliterator(); // Must be manually synched by user!
}
@Override
public Stream<E> stream() {
    return c.stream(); // Must be manually synched by user!
}
@Override
public Stream<E> parallelStream() {
    return c.parallelStream(); // Must be manually synched by user!
}
@java.io.Serial
private void writeObject(ObjectOutputStream s) throws IOException {
    synchronized (mutex) {s.defaultWriteObject();}
}
    }

} // END CLASS AbstractMRU ////////////////////////////////////////
