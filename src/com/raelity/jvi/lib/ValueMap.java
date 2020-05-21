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

import java.util.Map;
import java.util.function.Function;

/**
 * Map that takes it key from the value, see getKeyFunction.
 * The KeyFunction is typically set in a constructor.
 * <p/>
 * Could have a setFunction method which makes sure things are consistent.
 * @author err
 */
public interface ValueMap<K, V> extends Map<K, V>
{

/**
 * This is equivalent to put(getKeyFunction().apply(value), value).
 * @param value
 * @return 
 */
V put(V value);

Function<V,K> getKeyFunction();
}
