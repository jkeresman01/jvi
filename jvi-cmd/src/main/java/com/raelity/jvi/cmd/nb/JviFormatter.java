/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.raelity.jvi.cmd.nb;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Modified formater for use in NetBeans.
 * See platform/core.startup. Sun May 24 18:37:08 PDT 2020
 * Only use stack trace printing
 *      REMOVE  format, print, addLoggerName, extractDelegates, specialProcessing
 */
public final class JviFormatter { //extends java.util.logging.Formatter {
    //private static String lineSeparator = System.getProperty("line.separator"); // NOI18N
    //public static final java.util.logging.Formatter FORMATTER = new NbFormatter();

    private static final Map<Throwable, Integer> catchIndex = Collections.synchronizedMap(new WeakHashMap<Throwable, Integer>()); // #190623

    /**
     * For use also from NbErrorManager.
     *
     * @param t throwable to print
     * @param pw the destination
     */
    public static void printStackTrace(Throwable t, PrintWriter pw) {
        doPrintStackTrace(pw, t, null, 10);
    }

    /**
     * #91541: show stack traces in a more natural order.
     */
    private static void doPrintStackTrace(PrintWriter pw, Throwable t, Throwable higher, int depth) {
        if (depth == 0) {
            pw.println("Truncating the output at ten nested exceptions..."); // NOI18N
            return;
        }
        //if (t != null) {t.printStackTrace(pw);return;}//XxX
        try {
            if (t.getClass().getMethod("printStackTrace", PrintWriter.class).getDeclaringClass() != Throwable.class) { // NOI18N
                // Hmm, overrides it, we should not try to bypass special logic here.
                //System.err.println("using stock printStackTrace from " + t.getClass());
                t.printStackTrace(pw);
                return;
            }
            //System.err.println("using custom printStackTrace from " + t.getClass());
        } catch (NoSuchMethodException e) {
            assert false : e;
        }
        Throwable lower = t.getCause();
        if (lower != null) {
            doPrintStackTrace(pw, lower, t, depth - 1);
            pw.print("Caused: "); // NOI18N
        }
        String summary = t.toString();
        if (lower != null) {
            String suffix = ": " + lower;
            if (summary.endsWith(suffix)) {
                summary = summary.substring(0, summary.length() - suffix.length());
            }
        }
        pw.println(summary);
        StackTraceElement[] trace = t.getStackTrace();
        int end = trace.length;
        if (higher != null) {
            StackTraceElement[] higherTrace = higher.getStackTrace();
            while (end > 0) {
                int higherEnd = end + higherTrace.length - trace.length;
                if (higherEnd <= 0 || !higherTrace[higherEnd - 1].equals(trace[end - 1])) {
                    break;
                }
                end--;
            }
        }
        Integer caughtIndex = catchIndex.get(t);
        for (int i = 0; i < end; i++) {
            if (caughtIndex != null && i == caughtIndex) {
                // Translate following tab -> space since formatting is bad in
                // Output Window (#8104) and some mail agents screw it up etc.
                pw.print("[catch] at "); // NOI18N
            } else {
                pw.print("\tat "); // NOI18N
            }
            pw.println(trace[i]);
        }
    }

    // See following comment for how this is used
    static void registerCatchIndex(Throwable t, int index) {
        catchIndex.put(t, index);
    }
/*
    PUT THIS IN JviErrorManager (we've only got one handler
package org.netbeans.core.startup.logging;
final class DispatchingHandler extends Handler implements Runnable {
    publish(...)
        ...
        Throwable t = record.getThrown();
        if (t != null) {
            StackTraceElement[] tStack = t.getStackTrace();
            StackTraceElement[] hereStack = new Throwable().getStackTrace();
            for (int i = 1; i <= Math.min(tStack.length, hereStack.length); i++) {
                if (!tStack[tStack.length - i].equals(hereStack[hereStack.length - i])) {
                    NbFormatter.registerCatchIndex(t, tStack.length - i);
                    break;
                }
            }
        }
*/
    
} // end of NbFormater
