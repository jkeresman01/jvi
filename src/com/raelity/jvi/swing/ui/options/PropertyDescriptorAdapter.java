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
package com.raelity.jvi.swing.ui.options;

import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.l2fprod.common.beans.ExtendedPropertyDescriptor;
import com.l2fprod.common.propertysheet.AbstractProperty;

import com.raelity.jvi.options.*;

/**
 * L2FProd's PropertyDescriptorAdapter is not public, so copy it here
 * and change descriptor field to protected
 *
 * @author Ernie Rael <err at raelity.com>
 */
@SuppressWarnings("serial")
class PropertyDescriptorAdapter extends AbstractProperty {
    protected PropertyDescriptor descriptor;

    public PropertyDescriptorAdapter(PropertyDescriptor descriptor)
    {
        setDescriptor(descriptor);
    }

    private void setDescriptor(PropertyDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    public PropertyDescriptor getDescriptor()
    {
        return descriptor;
    }

    @Override
    public String getName()
    {
        return descriptor.getName();
    }

    @Override
    public String getDisplayName()
    {
        return descriptor.getDisplayName();
    }

    @Override
    public String getShortDescription()
    {
        return descriptor.getShortDescription();
    }

    @Override
    public Class<?> getType()
    {
        return descriptor.getPropertyType();
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Object clone()
    {
        PropertyDescriptorAdapter clone =
                new PropertyDescriptorAdapter(descriptor);
        clone.setValue(getValue());
        return clone;
    }

    @Override
    public void readFromObject(Object object)
    {
        try {
            Method method = descriptor.getReadMethod();
            if(method != null) {
                setValue(method.invoke(object, (Object[])null));
            }
        } catch(IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            String message = "Got exception when reading property " + getName();
            if(object == null) {
                message += ", object was 'null'";
            } else {
                message += ", object was " + String.valueOf(object);
            }
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public void writeToObject(Object object)
    {
        try {
            Method method = descriptor.getWriteMethod();
            if(method != null) {
                method.invoke(object, new Object[]{getValue()});
            }
        } catch(IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            // let PropertyVetoException go to the upper level without logging
            PropertyVetoException veto = OptUtil.getVeto(e);
            if(veto != null)
                throw new RuntimeException(e);
            String message = "Got exception when writing property " + getName();
            if(object == null) {
                message += ", object was 'null'";
            } else {
                message += ", object was " + String.valueOf(object);
            }
            // TODO: not Runtime?
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public boolean isEditable()
    {
        return descriptor.getWriteMethod() != null;
    }

    @Override
    public String getCategory()
    {
        if(descriptor instanceof ExtendedPropertyDescriptor) {
            return ((ExtendedPropertyDescriptor)descriptor).getCategory();
        } else {
            return null;
        }
    }
    
}
