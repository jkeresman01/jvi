/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 * @author err
 */
public class ValueHashMap<K, V>
extends HashMap<K, V>
implements ValueMap<K, V>
{
    private static final long serialVersionUID = 1L;
    private final Function<V,K> keyFunction;

    public ValueHashMap(Function<V, K> keyFunction, int initialCapacity)
    {
        super(initialCapacity);
        this.keyFunction = keyFunction;
    }

    public ValueHashMap(Function<V, K> keyFunction)
    {
        this.keyFunction = keyFunction;
    }

    // FIX
    // private ValueHashMap(Function<V, K> keyFunction,
    //                     Map<? extends K, ? extends V> map)
    // {
    //     super(map);
    //     this.keyFunction = keyFunction;
    // }

    @Override
    public V put(V value)
    {
        return super.put(keyFunction.apply(value), value);
    }

    @Override
    public Function<V, K> getKeyFunction()
    {
        return keyFunction;
    }



    @Override
    public V put(K key, V value)
    {
        if(!key.equals(keyFunction.apply(value)))
            throw new IllegalArgumentException("key/value miss-match");
        return super.put(key, value);
    }

    @Override
    public V replace(K key, V value)
    {
        if(!key.equals(keyFunction.apply(value)))
            throw new IllegalArgumentException("key/value miss-match");
        return super.replace(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue)
    {
        if(!key.equals(keyFunction.apply(newValue)))
            throw new IllegalArgumentException("key/newValue miss-match");
        return super.replace(key, oldValue, newValue);
    }

    // === LATER...

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
    {
        if(true) throw new UnsupportedOperationException("yet");
        super.replaceAll(function);
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.merge(key, value, remappingFunction);
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.compute(key, remappingFunction);
    }

    @Override
    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V putIfAbsent(K key, V value)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.putIfAbsent(key, value);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    public void putAll(
                       Map<? extends K, ? extends V> m)
    {
        if(true) throw new UnsupportedOperationException("yet");
        super.putAll(m);
    }

}
