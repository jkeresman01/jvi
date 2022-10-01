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
package com.raelity.jvi.options;

import java.beans.PropertyVetoException;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raelity.jvi.core.Options;

import org.netbeans.api.annotations.common.NonNull;

import com.raelity.jvi.lib.TextUtil;

import static com.raelity.jvi.manager.ViManager.eatme;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class EnumSetOption<S extends Enum<S>>
extends Option<EnumSet<S>>
{
    private final Class<S> enumType;
    static final Logger LOG = Logger.getLogger(EnumSetOption.class.getName());
    WeakReference<Map<String,S>> refNameEnumMap;

    public EnumSetOption(Class<EnumSet<S>> optionType, Class<S> enumType,
                         String key, EnumSet<S> defaultValue,
                         Validator<EnumSet<S>> valid)
    {
        super(optionType, key, defaultValue,
              valid != null ? valid : new DefaultEnumSetValidator<S>(key));
        this.enumType = enumType;
        super.initialize();
        clean();
    }

    private void clean()
    {
        if(Boolean.FALSE) {
            initialize();
            getValueFromString("");
            getValueAsString(null);
            eatme((Object)getAllStringValues());
        }
    }

    @Override
    void initialize()
    {
        // can't initialize until enumType is setup
    }



    public boolean contains(S e)
    {
        assert enumType == e.getDeclaringClass();
        return getValue().contains(e);
    }

    public EnumSet<S> getEmpty()
    {
        return EnumSet.noneOf(enumType);
    }

    @Override
    final EnumSet<S> getValueFromString(String sVal)
    {
        return decode(sVal);
    }

    @Override
    final String getValueAsString(EnumSet<S> val)
    {
        return encode(val);
    }

    public EnumSet<S> decode(String input)
    {
        Map<String, S> map = getNameEnumMap();
        EnumSet<S> set = EnumSet.noneOf(enumType);
        List<String>vals = TextUtil.tokens(input, "[,\\s]+");
        for(String s : vals) {
            S e = map.get(s);
            if(e != null)
                set.add(e);
            else
                LOG.log(Level.SEVERE, "EnumSet::decode {0} does not define {1}",
                        new Object[]{enumType.getName(), s});
        }

        return set;
    }

    public static String encode(EnumSet<?> set)
    {
        StringBuilder sb = new StringBuilder();

        for(Iterator<?> it = set.iterator(); it.hasNext();) {
            Object e = it.next();
            sb.append(e.toString());
            if(it.hasNext())
                sb.append(',');
        }

        return sb.toString();
    }

    private String[] getAllStringValues()
    {
        Enum<?>[] eVals = getAvailableValues();
        String[] sVals = new String[eVals.length];

        for(int i = 0; i < eVals.length; i++) {
            sVals[i] = eVals[i].toString();
        }
        return sVals;
    }

    public Enum<?>[] getAvailableValues()
    {
        EnumSet<S> set = EnumSet.allOf(enumType);
        Enum<?>[] vals = new Enum<?>[set.size()];
        int i = 0;
        for(Object e : set) {
            vals[i++] = (Enum)e;
        }
        return vals;
    }

    private Map<String, S> getNameEnumMap() {
        Map<String, S> map = refNameEnumMap == null
                                ? null : refNameEnumMap.get();
        if(map == null) {
            EnumSet<S> set = EnumSet.allOf(enumType);
            map = new HashMap<>(set.size());
            for(S e : set) {
                map.put(e.toString(), e);
            }
            refNameEnumMap = new WeakReference<>(map);
        }
        return map;
    }

    @Override
    public void validate(Object val) throws PropertyVetoException {
        if(validator instanceof DefaultEnumSetValidator<S> v)
            v.validateAnyType(val);
        else
            super.validate(val);
    }

    /**
     * The default EnumSetValidator gives no error if the argument
     * is an EnumSet (unless it is the wrong type of EnumSet) since
     * by definition it only contains valid enums.
     *
     * If the argument is a string, then it parses it, and checks
     * that each value is in the enum.
     */
    public static class DefaultEnumSetValidator<S extends Enum<S>>
    extends Validator<EnumSet<S>> {
        final String name;

        public DefaultEnumSetValidator(String name)
        {
            this.name = name;
        }
        @Override
        public void validate(EnumSet<S> val) throws PropertyVetoException
        {
            validateAnyType(val);
        }

        public void validateAnyType(@NonNull Object val) throws PropertyVetoException
        {
            @SuppressWarnings("unchecked")
            EnumSetOption<S> esOpt = (EnumSetOption<S>)Options.getOption(name);
            opt = esOpt;
            if(val instanceof EnumSet) {
                if(val.getClass() != opt.optionType)
                    reportPropertyVetoException(
                            "Invalid EnumSet class: "
                            + opt.getClass().getSimpleName(), val);
            } else if(val instanceof String string) {
                Map<String, S> map = esOpt.getNameEnumMap();
                List<String>vals = TextUtil.tokens(string, "[,\\s]+");
                for(String s : vals) {
                    Enum<?> e = map.get(s);
                    if(e == null) {
                        reportPropertyVetoException(
                                "Invalid value: " + s, val);
                    }
                }
            } else {
                reportPropertyVetoException(
                        "Must be either EnumSet or String not "
                        + val.getClass().getSimpleName(), val);
            }
        }

    }

}
