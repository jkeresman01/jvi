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

package com.raelity.jvi.options;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.eventbus.EventBus;

import com.raelity.jvi.core.*;
import com.raelity.jvi.manager.*;

import static java.util.logging.Level.*;

import static com.raelity.jvi.lib.TextUtil.sf;

/**
 *
 * @author err
 */
public class OptionEvent
{
    //private static class ReportPostEventBus extends EventBus
    //{
    //private final Consumer<Object> reportPost;
    //
    //public ReportPostEventBus(SubscriberExceptionHandler exceptionHandler,
    //                          Consumer<Object> reportPost)
    //{
    //  super(exceptionHandler);
    //  this.reportPost = reportPost;
    //}
    //
    //@Override
    //public void post(Object ev)
    //{
    //  if(!OptUtil.isStarted())
    //    return;
    //  reportPost.accept(ev);
    //  runInDispatch(false, () -> super.post(ev));
    //}
    //} // END CLASS ReportPostEventBus

public static void firePropertyChange(Change ev)
{
    OptionEvent.getEventBus().post(ev);
}

private static final Queue<Runnable> toRun = new ConcurrentLinkedQueue<>();

static void qPut(Runnable r)
{
    if(OptUtil.isStarted() && r != null)
        toRun.offer(r);
}

static void qPut(Change ev)
{
    if(OptUtil.isStarted() && ev != null)
        qPut(() -> firePropertyChange(ev));
}

static void qRun()
{
    Runnable r;
    while(toRun.peek() != null) {
        synchronized(toRun) {
            r = toRun.poll();
            if(r != null)
                r.run();
        }
    }
}

public static EventBus bus;
/** All events on this bus are dispatched on the EDT. */
public static EventBus getEventBus()
{
    if(bus == null)
        bus = new ViEvent.ReportPostEventBus(
                "OptionEvent: handleException:",
                (Object ev) -> {
                    if(!OptUtil.isStarted())
                        return false;
                    G.dbgOptions().printf(CONFIG, () ->
                            sf("FIRE:Option: %s\n", ev.toString()));
                    return true;
                });
    return bus;
}
// TODO: event for buf/win local? See SetColonCommand

    /** not used 
     * Maybe should call it apply/Change/prefs or something; see applyChanges. */
    static public class Dialog extends AbstractChange
    {
    public Dialog(String name, Object oldValue, Object newValue)
    {
      super(name, oldValue, newValue);
    }
    } // END CLASS OptionChangeDialogEvent

    /** option is changed in memory, G.xxx.
     * <p>
     * ?????????????????????
     * What about non global options, win/buf, see SetColonCommand.
     * 
     */
    static public class Global extends AbstractChange
    {
    public Global(String name, Object oldValue, Object newValue)
    {
      super(name, oldValue, newValue);
    }
    } // END CLASS OptionChangeSetEvent

    /** {@literal Option<>} changed value. */
    static public class Option extends AbstractChange
    {
    public Option(String name, Object oldValue, Object newValue)
    {
      super(name, oldValue, newValue);
    }
    } // END CLASS OptionChangeSetEvent

    static public class Initialized implements Change
    {
    @Override
    public String toString()
    {
      return sf("Option{%s:}", this.getClass().getSimpleName());
    }
    } // END CLASS OptionsInitializedEvent 

    /** Tag for option related events. */
    static public interface Change
    {
    }

    /** base class for option change events */
    public static class AbstractChange implements Change
    {
    private final String name;
    private final Object oldValue;
    private final Object newValue;
    
    public AbstractChange(String name, Object oldValue, Object newValue)
    {
      this.name = name;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }
    
    public String getName()
    {
      return name;
    }
    
    public Object getOldValue()
    {
      return oldValue;
    }
    
    public Object getNewValue()
    {
      return newValue;
    }
    
    @Override
    public String toString()
    {
      //return sf("Option{%s: %s: new=%s}",
      //          this.getClass().getSimpleName(),
      //          name, newValue);
      return sf("Option{%s: %s: old=%s, new=%s}",
                this.getClass().getSimpleName(),
                name, oldValue, newValue);
    }
    
    } // END CLASS AbstractOptionChangeEvent

    private OptionEvent()
    {
    }
  
}
