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
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.manager;

import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.lib.CcFlag;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Hook;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.ViFeature;
import com.raelity.jvi.ViFS;
import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Keymap;

import org.openide.util.lookup.Lookups;

/**
 * <p>
 * This class coordinates things.
 * The platform (main application) notifies jVi about new editors that are
 * opened and switches between open editors. <b>NOTE:</b> unless otherwise
 * noted, methods in this class should only be invoked from the event dispatch
 * thread.
 * </p>
 * <b>NEEDSWORK:</b>
 * <ul>
 * <li>Get rid of requestSwitch. Do it automatically at end of
 * activateAppView</li>
 * </ul>
 */
public class ViManager
{
    private static final Logger LOG = Logger.getLogger(ViManager.class.getName());

    //
    // Note the use of "9" for the "final" release
    // conventionally: 1.2.beta3        module-rev 1.2.3  (think 03)
    // conventionally: 1.2              module-rev 1.2.9  (think 09)
    // conventionally: 1.2.3.beta4      module-rev 1.2.34    NOTE: THESE TWO
    // conventionally: 1.2.3.x4         module-rev 1.2.34          ARE SAME #
    // conventionally: 1.2.3            module-rev 1.2.39
    // 1.4.0 is module rev 1.4.9
    // 1.4.1.x2 is module rev 1.4.12
    public static final jViVersion version = new jViVersion("1.4.2.x4");

    private static com.raelity.jvi.core.Hook core;

    private static final String DEBUG_AT_HOME = "com.raelity.jvi.DEBUG";

    public static final String PREFS_ROOT = "com/raelity/jvi";
    public static final String PREFS_KEYS = "KeyBindings";

    public static final String VIM_CLIPBOARD = "VimClipboard";
    public static final String VIM_CLIPBOARD2 = "VimClipboard2";
    public static final String VIM_CLIPBOARD_RAW = "VimRawBytes";

    public enum OsVersion
    {
        UNIX,
        MAC,
        WINDOWS;

        public boolean isWindows()
        {
            return this.equals(WINDOWS);
        }

        public boolean isMac()
        {
            return this.equals(MAC);
        }
    }
    private static ViFactory factory;

    private static Keymap editModeKeymap;
    private static Keymap normalModeKeymap;

    /** The features which can be disabled by the platform. */
    private static EnumSet<ViFeature> features = EnumSet.allOf(ViFeature.class);

    private static Map<Object,Object> hackMap = new HashMap<Object,Object>();
    public static void putHackMap(Object key, Object val) {
        hackMap.put(key, val);
    }
    public static Object getHackMap(Object key) {
        return hackMap.get(key);
    }
    public static boolean getHackFlag(Object key) {
        Boolean b = (Boolean) hackMap.get(key);
        //return b == null || !b ? false : true;
        // null is treated as true !?
        return b == null || b;
    }

    private static Boolean isDebugAtHome;
    public static boolean isDebugAtHome() {
        if(isDebugAtHome == null)
            isDebugAtHome = Boolean.getBoolean(DEBUG_AT_HOME);
        return isDebugAtHome;
    }
    /**
     * ViManager is partially initialized; this event is fired from
     * {@link #setViFactory(com.raelity.jvi.ViFactory)} after the
     * {@link ViInitialization} methods are invoked. old/new are null */
    public static final String P_BOOT = "jViBoot";
    /**
     * This is invoked well after boot, just before edit operations. */
    public static final String P_LATE_INIT = "jViLateInit";
    /**
     * jVi is closing up shop for the day. old/new are null */
    public static final String P_SHUTDOWN = "jViShutdown";
    /**
     * A new Buffer to work with.
     * new/old are Buffer, old may be null (first window) */
    public static final String P_OPEN_BUF = "jViOpenBuf";
    /**
     * closing a Buffer. old is Buffer, new is null */
    public static final String P_CLOSE_BUF = "jViCloseBuf";
    /**
     * A new window/editor pane to work with.
     * new/old are ViTextView, old may be null (first window) */
    public static final String P_OPEN_WIN = "jViOpenWin";
    /**
     * closing a TextView. old is ViTextView, new is null */
    public static final String P_CLOSE_WIN = "jViCloseWin";
    /**
     * about to switch away from "old", new is null. */
    public static final String P_SWITCH_FROM_WIN = "jViSwitchingWin";
    /**
     * change the current TextView. new/old are TextView. This event happens
     * after the switch, so the old tv is not really usable.
     * This property is the last to change when related P_OPEN_WIN, P_CLOSE_WIN */
    public static final String P_SWITCH_TO_WIN = "jViSwitchWin";

    // NEEDSWRK: is a property needed for switch_from, to pick up active tv?
    // NEEDSWORK: property for AppWindow open/close?

    private static PropertyChangeSupport pcs
            = new PropertyChangeSupport(getViMan());

    ViManager() {} // PRIVATE
    private static ViManager viMan;
    private static ViManager getViMan() {
        if(viMan == null)
            viMan = new ViManager();
        return viMan;
    }


    public static void setViFactory(ViFactory factory)
    {
        if(isFactoryLoaded())
        {
            throw new RuntimeException("ViFactory already set");
        }


        ViManager.factory = factory;

        for (ViInitialization i : Lookups.forPath("jVi/init")
                                        .lookupAll(ViInitialization.class)) {
            if(isDebugAtHome())
                System.err.println("INIT: " + i.getClass().getName());
            i.init();
        }

        if(core == null)
            throw new RuntimeException("NetBeans Bug 192496");
        assert core != null;

        ColonCommands.register("ve", "version",
                               new VersionCommand(), null);
        ColonCommands.register("debugMotd", "debugMotd",
                               new DebugMotdCommand(), EnumSet.of(CcFlag.DBG));
        ColonCommands.register("debugVersion", "debugVersion",
                               new DebugVersionCommand(), null);


        firePropertyChange(P_BOOT, null, null);

        // Add the vim clipboards

        // Spawn to get current release info
        // Note this is async
        Motd.get(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                motd = (Motd)e.getSource();
            }
        });

        getFactory().setShutdownHook(new Runnable() {
            @Override
            public void run() {
                firePropertyChange(P_SHUTDOWN, null, null);
            }
        });
    }

    public static boolean isFactoryLoaded()
    {
        return factory != null;
    }

    public static ViManHook setCoreHook(Hook hook)
    {
        assert core == null;
        core = hook;
        return new ViManHook();
    }

    /*package*/ static Hook getCore()
    {
        return core;
    }

    public static class ViManHook
    {
        private ViManHook(){}

        public void setJViBusy(boolean f) {
            ViManager.setJViBusy(f);
        }
    }

    static class VersionCommand implements ActionListener //ACTION_version = new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ViManager.motd.output();
        }
    };
    static class DebugMotdCommand implements ActionListener //ACTION_debugMotd = new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            ViManager.debugMotd();
        }
    };
    static class DebugVersionCommand extends ColonCommands.AbstractColonAction
    {

        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.DBG);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ColonCommands.ColonEvent cev = (ColonCommands.ColonEvent)e;
            if(cev.getNArg() == 1) {
                String vs = cev.getArg(1);
                jViVersion v = new jViVersion(vs);
                System.err.println("input=" + vs
                                   +", version=" + v.toString()
                                   + ", vToCur=" + v.compareTo(version));
            }
        }

    }

    /**
     * Disable the feature.
     * @param f feature to diable
     */
    public static void removeFeature(ViFeature f)
    {
        // NEEDSWORK: lock features after boot
        features.remove(f);
    }

    /**
     * Disable the set of features.
     * @param f features to disable
     */
    public static void removeFeature(EnumSet<ViFeature> f)
    {
        // NEEDSWORK: lock features after boot
        features.removeAll(f);
    }

    public static boolean hasFeature(ViFeature f)
    {
        return features.contains(f);
    }

    public static String cid(Object o)
    {
        if (o == null)
            return "(null)";
        return o.getClass().getSimpleName() + "@" + id(o);
    }

    public static String id(Object o)
    {
        if (o == null)
            return "(null)";
        return Integer.toHexString(System.identityHashCode(o));
    }

    // public static final DataFlavor VimClipboard
    //         = addVimClipboard(VIM_CLIPBOARD);
    public static final DataFlavor VimClipboard2
            = addVimClipboard(VIM_CLIPBOARD2);
    // public static final DataFlavor VimRawBytes
    //         = addVimClipboard(VIM_CLIPBOARD_RAW);

    private static DataFlavor addVimClipboard(String cbName)
    {
        DataFlavor df = null;
        SystemFlavorMap sfm
                = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();
        try {
            df = new DataFlavor("application/"
                    + cbName
                    + "; class=java.io.InputStream");
        } catch (ClassNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        sfm.addFlavorForUnencodedNative(cbName, df);
        sfm.addUnencodedNativeForFlavor(df, cbName);
        return df;
    }

    private static int jViBusy;
    private static boolean jViSettling;
    public static boolean jViBusy()
    {
        return jViBusy != 0 || jViSettling;
    }

    static void verifyNotBusy()
    {
        if (jViBusy != 0)
            ViManager.dumpStack();
    }

    static void setJViBusy(boolean f)
    {
        if(f)
            jViBusy++;
        else {
            jViBusy--;
            // if(jViBusy == 0) {
            //     // Some events may have been queued up due to jVi processing.
            //     // Let the events get handled before considering jVi idle.
            //     // In particular, caret updates on multi-core CPUs seem to
            //     // be async.
            //     // Could "peek" at the event Q and wait for it to be empty,
            //     // but that seems extreme.
            //     jViSettling = true;
            //     EventQueue.invokeLater(new Runnable() {
            //         public void run() {
            //             jViSettling = false;
            //         }
            //     });
            // }
        }
    }

    private static OsVersion osVersion;
    public static OsVersion getOsVersion()
    {
        String s = System.getProperty("os.name");
        if(osVersion == null) {
            osVersion = s.startsWith("Windows")
                            ? OsVersion.WINDOWS
                            : s.startsWith("Mac")
                                ? OsVersion.MAC
                                : OsVersion.UNIX;
        }
        return osVersion;
    }

    public static String getReleaseString()
    {
        return "jVi " + version;
    }

    public static ViFactory getFactory()
    {
        return factory;
    }

    public static ViFS getFS()
    {
        return factory.getFS();
    }

    public static ViTextView mayCreateTextView(Component editorPane)
    {
        return factory.createTextView(editorPane);
    }

    public static void changeBuffer(ViTextView tv, Object oldDoc)
    {
        factory.changeBuffer(tv, oldDoc);
        Scheduler.changeBuffer(tv);
    }

    /** get any text view, other than tv, which has buf KLUDGE HACK */
    public static ViTextView getAlternateTextView(ViTextView tv, Buffer buf)
    {
        ViTextView tv01 = null;
        Set<ViTextView> tvSet = factory.getViTextViewSet();
        for (ViTextView tv02 : tvSet) {
            if(tv == tv02)
                continue;
            Component ed = tv02.getEditor();
            if(ed != null) {
                if(tv02.getBuffer() == buf) {
                    tv01 = tv02;
                    break;
                }
            }
        }
        return tv01;
    }

    public static ViOutputStream createOutputStream(
            ViTextView tv,
            Object type,
            Object info)
    {
        return factory.createOutputStream(tv, type, info,
                ViOutputStream.PRI_NORMAL);
    }

    public static ViOutputStream createOutputStream(
            ViTextView tv,
            Object type,
            Object info,
            int priority)
    {
        return factory.createOutputStream(tv, type, info, priority);
    }

    /** update visible textviews */
    public static void updateHighlightSearchState()
    {
        Set<ViTextView> s = factory.getViTextViewSet();
        for (ViTextView tv : s) {
            if(tv.getAppView().isShowing())
                tv.updateHighlightSearchState();
        }
    }

    private static  boolean platformFindMatch;
    /**
     * Specify if the platform can be used for brace matching.
     * @param f true if platform can perform all find match functions.
     */
    public static  void setPlatformFindMatch(boolean f)
    {
        platformFindMatch = f;
    }
    public static  boolean getPlatformFindMatch() {
        return platformFindMatch;
    }

    public static void exitInputMode() // NEEDSWORK: take editor as arg
    {
        if(Scheduler.getCurrentEditor() != null) {
            core.resetCommand(false);
        }
    }

    /** The viewport has changed or scrolled, clear messages*/
    public static void viewMoveChange(ViTextView textView)
    {
        if(G.curwin == null) {
            // this case is because switchto, does attach, does viewport init
            // but G.curwin is not set yet. See switchTo(Component editor)
            return;
        }
        G.curwin.getStatusDisplay().scrolling();
    }

    //////////////////////////////////////////////////////////////////////
    //
    //

    static public void dumpStack(String msg, boolean supressIfNotBusy)
    {
        if(supressIfNotBusy && !jViBusy())
            return;
        System.err.println("" + msg);
        LOG.log(Level.SEVERE, msg, new IllegalStateException());
    }

    static public void dumpStack(String msg)
    {
        dumpStack(msg, false);
    }

    static public void dumpStack()
    {
        dumpStack(null);
    }

    static public void printStack()
    {
        for(StackTraceElement e : Thread.currentThread().getStackTrace()) {
            System.err.println(" " + e.toString());
        }
    }

    static public void setInsertModeKeymap(Keymap newInsertModeKeymap)
    {
        editModeKeymap = newInsertModeKeymap;
    }

    static public Keymap getInsertModeKeymap()
    {
        return editModeKeymap;
    }

    static public void setNormalModeKeymap(Keymap newNormalModeKeymap)
    {
        normalModeKeymap = newNormalModeKeymap;
    }

    static public Keymap getNormalModeKeymap()
    {
        return normalModeKeymap;
    }

    static public ActionListener xlateKeymapAction(ActionListener act)
    {
        return factory.xlateKeymapAction(act);
    }
  
    private static Motd motd = new Motd();

    static void motdOutputOnce()
    {
        motd.outputOnce();
    }

    static void debugMotd() {
        Motd.get(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                ((Motd)e.getSource()).output();
            }
        });
    }

      //
      // Look like a good bean
      // But they're static!
      //

    /** This should typically be used from a {@link ViInitialization} */
      public static void addPropertyChangeListener(
              PropertyChangeListener listener )
      {
        pcs.addPropertyChangeListener( listener );
      }

    /** This should typically be used from a {@link ViInitialization} */
      public static void addPropertyChangeListener(String p,
                                                   PropertyChangeListener l)
      {
        pcs.addPropertyChangeListener(p, l);
      }

      public static void removePropertyChangeListener(
              PropertyChangeListener listener )
      {
        pcs.removePropertyChangeListener( listener );
      }

      public static void removePropertyChangeListener(String p,
                                                      PropertyChangeListener l)
      {
        pcs.removePropertyChangeListener(p, l);
      }

      /*package*/ static void firePropertyChange(
              String name, Object oldValue, Object newValue) {
        pcs.firePropertyChange(name, oldValue, newValue);
      }

    /**
    * Copy preferences tree.
    */
    public static void copyPreferences(Preferences dst,
                                Preferences src,
                                boolean clearDst)
    {
        CopyPreferences p = new CopyPreferences(dst, src, clearDst);
    }

    private static class RunLatched implements Runnable
    {
        Runnable r;
        CountDownLatch latch;
        Throwable ex;

        public RunLatched(Runnable r, CountDownLatch latch)
        {
            this.r = r;
            this.latch = latch;
        }

        @Override
        public void run()
        {
            try {
                r.run();
            }
            catch (Throwable ex1) {
                ex = ex1;
            }
            finally {
                latch.countDown();
            }
        }

        Throwable getThrowable()
        {
            return ex;
        }
    }

    /**
     * Request that after the current character is processed,
     * let the eventQ run before processing more characters.
     * An example is in NB after doing TC.requestActive().
     *
     * This will be ignored if certain conditions are not met,
     * see GetChar.requestRunEventQueue for details.
     */
    public static void requestRunEventQueue(int nLoop)
    {
        core.requestRunEventQueue(nLoop);
    }
    
    // NEEDSWORK: add another parameter flag about how to handle throw?
    public static void runInDispatch(boolean wait, Runnable runnable) {
        if(EventQueue.isDispatchThread()) {
            runnable.run();
        } else if(!wait) {
            EventQueue.invokeLater(runnable);
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            RunLatched rl = new RunLatched(runnable, latch);
            EventQueue.invokeLater(rl);
            try {
                latch.await();
            } catch(InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            if(rl.getThrowable() != null) {
                throw new RuntimeException(
                        "After wait after invokeLater", rl.getThrowable());
            }
        }
    }
}

// vi:set sw=4 ts=8:
