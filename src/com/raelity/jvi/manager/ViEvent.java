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

package com.raelity.jvi.manager;

import java.beans.PropertyChangeEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

import org.openide.util.Exceptions;

import com.raelity.jvi.*;
import com.raelity.jvi.core.*;

import static com.raelity.jvi.manager.ViManager.*;

/**
 * jVi events handled through event bus. ViEvent is the superclass of all the
 * events. Typically subscribe to a subclass. Can subscribe to ViEvent to
 * get everything.
 * <p>
 * Boot is just after basic initialization. All initializations are
 * done and all colon commands should be registered.<br>
 * LateInit is just before the first SwithTo. PreShutdown is optional
 * depending on platform; may happen more than once; can check with
 * factory.hasPreShutdown. Shutdown as expected.
 * 
 * </p><p>
 * During a switch events are in the following order.
 * <ul>
 * <li>SwitchFromTv</li>
 * <li>OpenTv (if newly created)</li>
 * <li>OpenBuf (if newly created)</li>
 * <li>SwithToTv</li>
 * </ul>
 * </p><p>
 * During text view close (editor window close).
 * <ul>
 * <li>CloseWin</li>
 * <li>CloseBuf (if last close)</li>
 * </ul>
 * </p><p>
 * Extend propertyChangeEvent to ease transition from propertyChangeSupport.
 * </p>
 * @author err
 */
@SuppressWarnings("serial")
public class ViEvent extends PropertyChangeEvent
{
private static final EventBus bus = new EventBus(new ExHandler("ViEvent: handleException:"));
private static final String esource = "ViEventSource";

    public static class ExHandler implements SubscriberExceptionHandler
    {
    protected final String tag;
    public ExHandler(String tag)
    {
        this.tag = tag;
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void handleException(Throwable ex, SubscriberExceptionContext ctx)
    {
        System.err.println(tag);
        System.err.println("    " + ctx.getEventBus());
        System.err.println("    " + ctx.getEvent());
        System.err.println("    " + ctx.getSubscriber());
        System.err.println("    " + ctx.getSubscriberMethod());
        Exceptions.printStackTrace(ex);
    }
    } // END CLASS ExHandler

public static EventBus getBus()
{
    return bus;
}

public static void fire(ViEvent ev)
{
    bus.post(ev);
}

private ViEvent(String propertyName,
                Object oldValue, Object newValue)
{
    super(esource, propertyName, oldValue, newValue);
}

    /** this is transition aid */
    public static ViEvent get(String name, Object oldv, Object newv)
    {
        ViEvent ev;
        switch(name) {
        case P_BOOT:            ev = new Boot(name, oldv, newv); break;
        case P_LATE_INIT:       ev = new LateInit(name, oldv, newv); break;
        case P_PRE_SHUTDOWN:    ev = new PreShutdown(name, oldv, newv); break;
        case P_SHUTDOWN:        ev = new Shutdown(name, oldv, newv); break;
        case P_OPEN_BUF:        ev = new OpenBuf(name, oldv, newv); break;
        case P_CLOSE_BUF:       ev = new CloseBuf(name, oldv, newv); break;
        case P_OPEN_TV:         ev = new OpenTv(name, oldv, newv); break;
        case P_CLOSE_TV:        ev = new CloseTv(name, oldv, newv); break;
        case P_SWITCH_FROM_TV:  ev = new SwitchFromTv(name, oldv, newv); break;
        case P_SWITCH_TO_TV:    ev = new SwitchToTv(name, oldv, newv); break;
        default:
                throw new IllegalArgumentException("ViEvent.get(" + name + ")");
        }
        return ev;
    }

    public static class Boot extends ViEvent {
    public Boot(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    }

    public static class LateInit extends ViEvent {
    public LateInit(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    }

    public static class PreShutdown extends ViEvent {
    public PreShutdown(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    }

    public static class Shutdown extends ViEvent {
    public Shutdown(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    }

    /** First Open */
    public static class OpenBuf extends ViEvent {
    public OpenBuf(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    public Buffer getBuf()
    {
        return (Buffer)getNewValue();
    }
    }

    /** Last Close */
    public static class CloseBuf extends ViEvent {
    public CloseBuf(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    public Buffer getBuf()
    {
        return (Buffer)getOldValue();
    }
    }

    /** Event when transitions dirty state */
    public static class DirtyBuf extends ViEvent {
    public DirtyBuf(ViBuffer buf, boolean isDirty)
    {
        super(P_DIRTY_BUF, !isDirty, isDirty);
        source = buf;
    }
    public Buffer getBuf()
    {
        return (Buffer)source;
    }
    public Boolean isDirty()
    {
        return (Boolean)getNewValue();
    }
    
    @Override
    String customEventInfo()
    {
        return "; " + ViManager.getFactory().getFS().getDisplayPath(getBuf());
    }
                
    }

    public static class OpenTv extends ViEvent {
    public OpenTv(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    public TextView getTv()
    {
        return (TextView)getNewValue();
    }
    }

    public static class CloseTv extends ViEvent {
    public CloseTv(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    public TextView getTv()
    {
        return (TextView)getOldValue();
    }
    }

    public static class SwitchFromTv extends ViEvent {
    public SwitchFromTv(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    public TextView getTv()
    {
        return (TextView)getOldValue();
    }
    }

    public static class SwitchToTv extends ViEvent {
    public SwitchToTv(String propertyName, Object oldValue, Object newValue)
    {
        super(propertyName, oldValue, newValue);
    }
    public TextView getTv()
    {
        return (TextView)getNewValue();
    }
    }

    /** Event from Normal:processInputChar */
    public static class ProcessInput extends ViEvent {
    public ProcessInput()
    {
        super(P_PROCESS_INPUT, null, null);
    }
    }

String customEventInfo()
{
    return "";
}

@Override
public String toString()
{
    String ov = ViManager.getFactory().getFS().getDisplayPath(getOldValue());
    String nv = ViManager.getFactory().getFS().getDisplayPath(getNewValue());
    ov = ov != null ? ov : "" + getOldValue();
    nv = nv != null ? nv : "" + getNewValue();
    StringBuilder sb = new StringBuilder("ViEvent$")
            .append(getClass().getSimpleName());
    sb.append("; old=").append(ov);
    sb.append("; new=").append(nv);
    return sb.append(customEventInfo()).toString();
}
}
