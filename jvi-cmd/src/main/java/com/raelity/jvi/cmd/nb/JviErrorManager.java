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

package com.raelity.jvi.cmd.nb;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


// - platform/openide.util/Exceptions annotates
//     then finally LOG.log(level, null, t)
// - platform/core.startup/TopLogging$LookupDel handler which passes off
//     to all registered (via serviceprovider) handlers. In particular
// - platform/o.n.core/NbErrorManager isa handler. It diddles level
//     and creates an exception record for
// - platform/o.n.core/NotifyExcPanel.notify. If shallNotify it does
//     invokeLater which builds the dialog acc'd to exception record.
// - and don't forget 1973
//
// Note that in shallNotify there are a couple of system properties used
// to set the cutoff for what get's displayed.

/**
 * Patterned from NbErrorManager, greatly simplified; much removed.
 * @author err
 */
public class JviErrorManager extends Handler
{
    
    static Exc createExc(Throwable t, Level severity, LogRecord add) {
        LogRecord[] ann = findAnnotations(t, add);
        return new Exc(t, severity, ann, findAnnotations0(t, add, true, new HashSet<Throwable>()));
    }

    @Override
    public void publish(LogRecord record)
    {
        Throwable t = record.getThrown();
        if(t == null)
            return;

        //
        // from the root logger's first handler, publish from DispatchingHandler
        // DispatchingHandler RP Qs record for RP and gives it to delegate
        // for normal logging.
        //
        // figure out 'catch at ...'
        StackTraceElement[] tStack = t.getStackTrace();
        StackTraceElement[] hereStack = new Throwable().getStackTrace();
        for (int i = 1; i <= Math.min(tStack.length, hereStack.length); i++) {
            if (!tStack[tStack.length - i].equals(hereStack[hereStack.length - i])) {
                JviFormatter.registerCatchIndex(t, tStack.length - i);
                break;
            }
        }
        // So the catch index is available for later handlers

        //
        // NbErrorManager is that later handler that
        // takes care of exception display.
        //
        
        Level level = record.getLevel();
        if (level.intValue() == Level.WARNING.intValue() + 1) {
            // unknown level
            level = null;
        }
        if (level != null && level.intValue() == Level.SEVERE.intValue() + 1) {
            // unknown level
            level = null;
        }
        Exc ex = createExc(record.getThrown(), level, record.getLevel().intValue() == 1973 ? record : null);
        JviNotifyExcPanel.notify(ex);

    }

    // Following verbatim from NbErrorManager
    @Override
    public void flush()
    {
        //logWriter.flush();
    }

    @Override
    public void close() throws SecurityException
    {
        // nothing needed
    }

    /** Extracts localized message from a LogRecord */
    private static String getLocalizedMessage(LogRecord rec) {
        ResourceBundle rb = rec.getResourceBundle();
        if (rb == null) {
            return null;
        }
        
        String msg = rec.getMessage();
        if (msg == null) {
            return null;
        }
        
        String format = rb.getString(msg);

        Object[] arr = rec.getParameters();
        if (arr == null) {
            return format;
        }

        return MessageFormat.format(format, arr);
    }

    /** Finds annotations associated with given exception.
     * @param t the exception
     * @return array of annotations or null
     */
    private static LogRecord[] findAnnotations(Throwable t, LogRecord add) {
        return findAnnotations0(t, add, false, new HashSet<Throwable>());
    }
    
    /** If recursively is true it is not adviced to print all annotations
     * because a lot of warnings will be printed. But while searching for
     * localized message we should scan all the annotations (even recursively).
     */
    private static LogRecord[] findAnnotations0(Throwable t, LogRecord add, boolean recursively, Set<Throwable> alreadyVisited) {
        List<LogRecord> l = new ArrayList<LogRecord>();
        Throwable collect = t;
        while (collect != null) {
            if (collect instanceof Callable) {
                Object res = null;
                try {
                    res = ((Callable) collect).call();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (res instanceof LogRecord[]) {
                    LogRecord[] arr = (LogRecord[])res;
                    l.addAll(Arrays.asList(arr));
                }
            }
            collect = collect.getCause();
        }

        if (add != null) {
            l.add(add);
        }

        
        if (recursively) {
            ArrayList<LogRecord> al = new ArrayList<LogRecord>();
            for (LogRecord ano : l) {
                Throwable t1 = ano.getThrown();
                if ((t1 != null) && (! alreadyVisited.contains(t1))) {
                    alreadyVisited.add(t1);
                    LogRecord[] tmpAnnoArray = findAnnotations0(t1, null, true, alreadyVisited);
                    if ((tmpAnnoArray != null) && (tmpAnnoArray.length > 0)) {
                        al.addAll(Arrays.asList(tmpAnnoArray));
                    }
                }
            }
            l.addAll(al);
        }

        Throwable cause = t.getCause();
        if (cause != null) {
            LogRecord[] extras = findAnnotations0(cause, null, true, alreadyVisited);
            if (extras != null && extras.length > 0) {
                l.addAll(Arrays.asList(extras));
            }
        }
        
        LogRecord[] arr;
        arr = new LogRecord[l.size()];
        l.toArray(arr);
        
        return arr;
    }
    
    ///////// from NbErrorManager.Exc

    static final class Exc {
        Throwable t;
        final LogRecord[] arr;      // Accessed from tests
        final LogRecord[] arrAll;   // all - recursively, accessed from tests
        private Level severity;

        Exc(Throwable t, Level severity, LogRecord[] arr, LogRecord[] arrAll) {
            this.t = t;
            //this.severity = severity;
            this.arr = arr == null ? new LogRecord[0] : arr;
            this.arrAll = arrAll == null ? new LogRecord[0] : arrAll;
        }
        
        /** @return localized message */
        String getLocalizedMessage() {
            String m = t.getLocalizedMessage();
            if (m != null && !m.equals(t.getMessage())) {
                return m;
            }
            if (arrAll == null) {
                // arrAll not filled --> use the old non recursive variant
                return (String)find(2);
            }
            for (int i = 0; i < arrAll.length; i++) {
                String s = JviErrorManager.getLocalizedMessage(arrAll[i]);
                if (s != null) {
                    return s;
                }
            }
            return m;
        }
        
        boolean isLocalized() {
            String m = t.getLocalizedMessage();
            if (m != null && !m.equals(t.getMessage())) {
                return true;
            }
            if (arrAll == null) {
                // arrAll not filled --> use the old non recursive variant
                return (String)find(2) != null;
            }
            for (int i = 0; i < arrAll.length; i++) {
                String s = JviErrorManager.getLocalizedMessage(arrAll[i]);
                if (s != null) {
                    return true;
                }
            }
            return false;
        }
        
        final boolean isUserQuestion() {
            // return t instanceof UserQuestionException;
            return false;
        }
        
        final void  confirm() throws IOException {
            //((UserQuestionException)t).confirmed();
        }
        
        /** @return the severity of the exception */
        Level getSeverity() {
            if (severity != null) {
                return severity;
            }
            
            LogRecord[] anns = (arrAll != null) ? arrAll : arr;
            for (int i = 0; i < anns.length; i++) {
                Level s = anns[i].getLevel();
                if (severity == null || s.intValue() > severity.intValue()) {
                    severity = s;
                }
            }
            
            if (severity == null || severity == Level.ALL) {
                // no severity specified, assume this is an error
                severity = t instanceof Error ? Level.SEVERE : Level.WARNING;
            }
            
            return severity;
        }
        
        void printStackTrace(PrintStream ps) {
            printStackTrace(new PrintWriter(new OutputStreamWriter(ps)));
        }
        /** Prints stack trace of all annotations and if
         * there is no annotation trace then of the exception
         */
        void printStackTrace(PrintWriter pw) {
            // #19487: don't go into an endless loop here
            printStackTrace(pw, new HashSet<Throwable>(10));
        }
        
        private void printStackTrace(PrintWriter pw, Set<Throwable> nestingCheck) {
            if (t != null && !nestingCheck.add(t)) {
                // Unlocalized log message - this is for developers of NB, not users
                Logger l = Logger.getAnonymousLogger();
                l.warning("WARNING - ErrorManager detected cyclic exception nesting:"); // NOI18N
                for (Throwable thrw : nestingCheck) {
                    l.warning("\t" + thrw); // NOI18N
                    LogRecord[] anns = findAnnotations(thrw, null);
                    if (anns != null) {
                        for (int i = 0; i < anns.length; i++) {
                            Throwable t2 = anns[i].getThrown();
                            if (t2 != null) {
                                l.warning("\t=> " + t2); // NOI18N
                            }
                        }
                    }
                }
                l.warning("Be sure not to annotate an exception with itself, directly or indirectly."); // NOI18N
                return;
            }
            /*Heaeder
            pw.print (getDate ());
            pw.print (": "); // NOI18N
            pw.print (getClassName ());
            pw.print (": "); // NOI18N
            String theMessage = getMessage();
            if (theMessage != null) {
                pw.print(theMessage);
            } else {
                pw.print("<no message>"); // NOI18N
            }
            pw.println ();
             */
            /*Annotations */
            for (LogRecord rec : arr) {
                if (rec == null) {
                    continue;
                }
                
                Throwable thr = rec.getThrown();
                String annotation = JviErrorManager.getLocalizedMessage(rec);
                
                if (annotation == null) {
                    annotation = rec.getMessage();
                }
                /*
                if (annotation == null && thr != null) annotation = thr.getLocalizedMessage();
                if (annotation == null && thr != null) annotation = thr.getMessage();
                 */
                
                if (annotation != null) {
                    if (thr == null) {
                        pw.println("Annotation: "+annotation);// NOI18N
                    }
                    //else pw.println ("Nested annotation: "+annotation);// NOI18N
                }
            }
            
            // ok, print trace of the original exception too
            // Attempt to show an annotation indicating where the exception
            // was caught. Not 100% reliable but often helpful.
            if (t instanceof VirtualMachineError) {
                // Decomposition may not work here, e.g. for StackOverflowError.
                // Play it safe.
                t.printStackTrace(pw);
            } else {
                // TopLogging just trampolines to NbFormatter
                //TopLogging.printStackTrace(t, pw);
                JviFormatter.printStackTrace(t, pw);
            }
            /*Nested annotations */
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) {
                    continue;
                }
                
                Throwable thr = arr[i].getThrown();
                if (thr != null) {
                    LogRecord[] ans = findAnnotations(thr, null);
                    Exc ex = new Exc(thr, null, ans, null);
                    pw.println("==>"); // NOI18N
                    ex.printStackTrace(pw, nestingCheck);
                }
            }
        }

        /**
         * Method that iterates over annotations to find out
         * the first annotation that brings the requested value.
         *
         * @param kind what to look for (1, 2, 3, 4, ...);
         * @return the found object
         */
        private Object find(int kind) {
            return find(kind, true);
        }
        
        /**
         * Method that iterates over annotations to find out
         * the first annotation that brings the requested value.
         *
         * @param kind what to look for (1, 2, 3, 4, ...);
         * @return the found object
         */
        private Object find(int kind, boolean def) {
            for (int i = 0; i < arr.length; i++) {
                LogRecord a = arr[i];
                
                Object o = null;
                switch (kind) {
                    case 1: // message
                        o = a.getMessage(); break;
                    case 2: // localized
                        o = JviErrorManager.getLocalizedMessage(a); break;
                    case 3: // class name
                    {
                        Throwable t = a.getThrown();
                        o = t == null ? null : t.getClass().getName();
                        break;
                    }
                    case 4: // date
                        o = new Date(a.getMillis()); break;
                }
                
                if (o != null) {
                    return o;
                }
            }
            
            if (!def) {
                return null;
            }
            switch (kind) {
                case 1: // message
                    return t.getMessage();
                case 2: // loc.msg.
                    return t.getLocalizedMessage();
                case 3: // class name
                    return t.getClass().getName();
                case 4: // date
                    return new Date();
                default:
                    throw new IllegalArgumentException(
                        "Unknown " + Integer.valueOf(kind) // NOI18N
                        );
            }
        }
    }
    
}
