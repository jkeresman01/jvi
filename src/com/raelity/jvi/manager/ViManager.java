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
 * Copyright (C) 2011 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.manager;

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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeEvent;
import javax.swing.text.Keymap;

import org.openide.util.lookup.Lookups;

import com.raelity.jvi.ViFS;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.ViFeature;
import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Hook;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.Search;
import com.raelity.jvi.core.lib.CcFlag;

import java.util.Map.Entry;

import org.openide.util.Exceptions;

import com.raelity.jvi.ViOutputStream.COLOR;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.options.*;
import com.raelity.text.TextUtil;

import static com.raelity.jvi.ViOutputStream.FLAGS;
import static com.raelity.text.TextUtil.sf;

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
final public class ViManager
{
    private static final Logger LOG = Logger.getLogger(ViManager.class.getName());

    //
    // Note the use of "9" for the "final" release
    // conventionally: 1.2.beta3        module-rev 1.2.3  (think 03)
    // conventionally: 1.2              module-rev 1.2.9  (think 09)
    // conventionally: 1.2.3.beta4      module-rev 1.2.34    NOTE: THESE TWO
    // conventionally: 1.2.3.x4         module-rev 1.2.34          ARE SAME #
    // conventionally: 1.2.3            module-rev 1.2.39
    // conventionally: 1.2.11.x3         module-rev 1.2.113
    // conventionally: 1.2.11            module-rev 1.2.119
    // 1.4.0 is module rev 1.4.9
    // 1.4.1.x2 is module rev 1.4.12
    //
    public static final jViVersion version = new jViVersion("2.0.0");

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
    private static final EnumSet<ViFeature> features
            = EnumSet.allOf(ViFeature.class);

    private static final Map<Object,Object> hackMap = new HashMap<>();
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
        return b == null ? false : b;
    }

    private static Boolean isDebugAtHome;
    public static boolean isDebugAtHome() {
        if(isDebugAtHome == null) {
            isDebugAtHome = Boolean.getBoolean(DEBUG_AT_HOME);
            if(isDebugAtHome) {
                if(System.getProperty("com.raelity.jvi.motd") == null) {
                    System.setProperty("com.raelity.jvi.motd",
                                       "file:///src/jvi-dev/work/motd");
                }

            }
        }
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
     * jVi might shutdown. old/new are null.
     * This can happen more than once. */
    public static final String P_PRE_SHUTDOWN = "jViPreShutdown";
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

    private static final PropertyChangeSupport pcs
            = new PropertyChangeSupport(getViMan());

    private ViManager() {}
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
        fixupPreferences();

        Options.informAfterInit((ChangeEvent e) -> {
            setupOptionAtStartup();
        });
        // As of Mon Mar 16 10:10:19 PDT 2020
        //
        // 1 - 
        //      Hook
        // 2 - 
        //      Options
        // 3 - 
        //      nb/NbOptions
        // 4 - 
        // 5 - 
        //      nb/HighlightsExecutor - initialize executor
        // 6 - 
        //      MarkOps - colon commands, pcl
        // 7 - 
        //      KeyBinding - inits tables...
        //      Filemarks
        // 8 - 
        //      nb/KeyBindings - initializes a pcl on KeyBinding
        // 9 - 
        // 10 - COLON COMMANDS, PCL
        //      nb/Module - add listener  NEEDSWORK: move options stuff around
        //      Cc01 - register lots of colon commands
        //      CcBang - register lots of colon commands
        //      CommandHistory - register a colon command
        //      WindowTreeBuilder - colon command
        //      Misc - colon command, pcl
        //      TabPages - colon commands
        //      TextView - pcl
        //      AppViews - colon commands
        //      SetColonCommand - colon command
        //      SwingTextView - colon command
        //100 - 
        //      GetChar - mappings
        //
        for (ViInitialization i : Lookups.forPath("jVi/init")
                                        .lookupAll(ViInitialization.class)) {
            if(isDebugAtHome())
                G.dbg.println("INIT: " + i.getClass().getName());
            i.init();
        }
        if(isDebugAtHome()) {
            for(Entry<Object,Object> e: hackMap.entrySet()) {
                G.dbg.println("HACKMAP: %s %s", e.getKey(), e.getValue());
            }
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
        ColonCommands.register("debugOutputStyles", "debugOutputStyles",
                               new DebugOutputStyles(), EnumSet.of(CcFlag.DBG));
        ColonCommands.register("isDebugAtHome", "isDebugAtHome",
                               new IsDebugAtHome(), null);
        ColonCommands.register("debugDebug", "debugDebug",
                               new DebugDebug(), null);


        firePropertyChange(P_BOOT, null, null);

        // Add the vim clipboards

        // Spawn to get current release info
        // Note this is async
        Motd.get((ChangeEvent e) -> {
            motd = (Motd)e.getSource();
        });

        getFactory().setShutdownHook(() -> {
            firePropertyChange(P_SHUTDOWN, null, null);
        });
    }

    public static void preShutdownCheck()
    {
            firePropertyChange(P_PRE_SHUTDOWN, null, null);
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
                G.dbg.println("input=" + vs
                                   +", version=" + v.toString()
                                   + ", vToCur=" + v.compareTo(version));
            }
        }

    }

    static class DebugOutputStyles implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            Runnable r = () -> System.err.println("click");
            try (ViOutputStream os = ViManager.createOutputStream(null)) {
                os.println("plain ALL CAPS And More");
                os.println("plain ALL CAPS And More success", COLOR.SUCCESS);
                os.println("plain ALL CAPS And More warning", COLOR.WARNING);
                os.println("plain ALL CAPS And More failure", COLOR.FAILURE);
                os.println("plain ALL CAPS And More debug", COLOR.DEBUG);
                os.println("link link ALL CAPS And More", r);
                os.println("link ALL CAPS And More success", r, COLOR.SUCCESS);
                os.println("link ALL CAPS And More warning", r, COLOR.WARNING);
                os.println("link ALL CAPS And More failure", r, COLOR.FAILURE);
                os.println("link ALL CAPS And More debug", r, COLOR.DEBUG);
            }
        }
    };

    /**
     * Some random things to play with the debugging infrastructure.
     * The map can be modified through the debugger,
     * or by providing parm=value on the command line.
     * Typically value is an integer.
     */
    private static final Map<String, Object> debugDebugDiddling = new HashMap<>();

    /** Typically done from the debugger.  */
    static void debugSetParam(String s, Object o)
    {
        debugDebugDiddling.put(s, o);
    }

    static class DebugDebug extends ColonCommands.AbstractColonAction
    {

        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.DBG);
        }

        @Override
        @SuppressWarnings("ThrowableResultIgnored")
        public void actionPerformed(ActionEvent e)
        {
            if(debugDebugDiddling.isEmpty()) {
                debugDebugDiddling.put("which", 0); // which case to run
                debugDebugDiddling.put("level", Level.WARNING.intValue());
                debugDebugDiddling.put("local", "false"); // use localalized message
            }
            ColonCommands.ColonEvent cev = (ColonCommands.ColonEvent)e;
            if(cev.getNArg() > 0) {
                // parse out parm=value on the command line,
                // do map.put(parm, converted-value)
                // which = Integer.parseInt(value)
            }
            int which = (Integer)debugDebugDiddling.get("which");
            Level level = Level.parse(""+(Integer)debugDebugDiddling.get("level"));
            boolean local = Boolean.parseBoolean((String)debugDebugDiddling.get("local"));

            switch(which) {
            case 0: 
                Throwable t = new Throwable("some throwable");
                Throwable t1 = Exceptions.attachSeverity(t, level);
                if(local) {
                    // using attach localized message, only the attached message is displayed
                    Throwable t2 = Exceptions.attachLocalizedMessage(t, "some attached message");
                } else {
                    Throwable t2 = Exceptions.attachMessage(t, "some attached message");
                }
                String s = Exceptions.findLocalizedMessage(t);
                Exceptions.printStackTrace(t);
                // Dialog d = new Dialog((Frame)null);
                // d.show();
                break;
            case 1:
                throw new RuntimeException("runtime exception");
            case 2:
                DebugOption x = Options.getDebugOption(Options.dbgBang);
                x.println("one");
                x.println("%s %s %s", "one", "two", "three");
                x.printf("oneNL\n");
                x.printf("%s %s %sNL\n", "one", "two", "three");
                x.println(Level.INFO, "one");
                x.println(Level.INFO, "%s %s %s", "one", "two", "three");
                x.printf(Level.INFO, "oneNL\n");
                x.printf(Level.INFO, "%s %s %sNL\n", "one", "two", "three");
                LOG.log(Level.SEVERE, "don't leave me here");
            }
        }

    }

    static class IsDebugAtHome extends ColonCommands.AbstractColonAction
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
                String s = cev.getArg(1);
                isDebugAtHome = s.toLowerCase().startsWith("y");
            }
            try (ViOutputStream os = ViManager.createOutputStream(null)) {
                os.println(TextUtil.sf("isDebugAtHome=%s", isDebugAtHome()));
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
            Object info)
    {
        return factory.createOutputStream(null, ViOutputStream.MAIN, info,
                EnumSet.of(FLAGS.NEW_NO, FLAGS.RAISE_YES, FLAGS.CLEAR_NO));
    }

    //EnumSet.of(FLAGS.NEW_YES, FLAGS.RAISE_YES, FLAGS.CLEAR_NO));

    public static ViOutputStream createOutputStream(
            ViTextView tv, Object type, Object info, EnumSet<FLAGS> flags)
    {
        return factory.createOutputStream(tv, type, info, flags);
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

    /**
     * The viewport has changed or scrolled, clear messages
     * and make sure cursor stays in view.
     */
    public static void viewMoveChange(ViTextView textView)
    {
        if(G.curwin() == null) {
            // this case is because switchto, does attach, does viewport init
            // but G.curwin is not set yet. See switchTo(Component editor)
            return;
        }
        if(factory.isEnabled()) {
            // Nov-2020 add isEnabled check; needed because keepCursorInView,
            // which was added earlier this year, touches too much stuff.
            // This seems like the right point to cut off responding,
            // the stuff before we get here records state info.
            if(textView.isReady())
                core.keepCursorInView(textView);
        }
        G.curwin().getStatusDisplay().scrolling();
    }

    //////////////////////////////////////////////////////////////////////
    //
    //

    /** this is assumed to be some output protected by debug consideration */
    static public void println(String s)
    {
        G.dbg.println(s);
    }
    static public void printf(String s, Object... args)
    {
        G.dbg.printf(s, args);
    }
    static public void warning(String s)
    {
        LOG.warning(s);
    }

    static public void dumpStack(String msg, boolean supressIfNotBusy)
    {
        if(supressIfNotBusy && !jViBusy())
            return;
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
            G.dbg.println(" " + e.toString());
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
        Motd.get((ChangeEvent e) -> {
            ((Motd)e.getSource()).output();
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

      // some changes (poorly thought out) require fixups in preferences
      private static void fixupPreferences() {
        Preferences prefs = getFactory().getPreferences();

        String t = prefs.get(Options.commandEntryFrame, "xyzzy");
        // if the pref hasn't been set, then nothing to do
        if(!t.equals("xyzzy")) {
            // something is set.
            // If it is a boolean, old school, then force it back to default.
            // Could map false to "Glass Pane", but time try modal again
            if(t.equalsIgnoreCase("true")
                    || t.equalsIgnoreCase("false"))
                prefs.remove(Options.commandEntryFrame);
        }

        // If metaEscape was never set, then nothing to do.
        // The default for Options.magic will be used
        t = prefs.get(Options.metaEscape, "xyzzy");
        if(!t.equals("xyzzy")) {
            // boolean equalsMetaEscape = true;
            // metaEscape was set by the user

            // If the string is shorter than default,
            // then assume very magic. Otherwise let
            // the default kick in
            if(t.length() < Search.MAGIC.length()) {
                // assume very magic
                prefs.put(Options.magic, Options.MESC_VERY_MAGIC);
            }
            // else if(!t.equals(Search.MAGIC)) {
            //     for(int i = 0; i < t.length(); i++) {
            //         if(!Search.MAGIC.contains(t.substring(i, i+1)))
            //             equalsMetaEscape = false;
            //     }
            // }
            prefs.remove(Options.metaEscape); // should never get here again
        }
        t = prefs.get(Options.notStartOfLine, "xyzzy");
        if(!t.equals("xyzzy")) {
            // The backwards option is set.
            // The correct option is the opposite state, so...
            boolean notsol = prefs.getBoolean(Options.notStartOfLine, true);
            prefs.putBoolean(Options.startOfLine, !notsol);
            prefs.remove(Options.notStartOfLine);
        }
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
    public static void requestCharBreakPauseRunEventQueue(int nLoop)
    {
        core.requestCharBreakPauseRunEventQueue(nLoop);
    }

    public static void nInvokeLater(int nPause, final Runnable runnable)
    {
        nInvokeLater("", nPause, runnable);
    }

    public static void nInvokeLater(String tag, int nPause, final Runnable runnable)
    {
        G.dbgEditorActivation().println(() -> sf("nInvokeLater %s: nPause %d",
                nPause <= 0 ? "run" : "later", nPause));
        if(nPause <= 0) {
            runnable.run();
        } else {
            EventQueue.invokeLater(() -> {
                nInvokeLater(nPause-1, runnable);
            });
        }
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

    private static String useFrame_ValueAtBoot;
    private static boolean comboCommandLine_ValueAtBoot;

    // this is called after the options are setup
    private static void setupOptionAtStartup() {
        if(useFrame_ValueAtBoot != null) {
            throw new RuntimeException("setupOptionAtBoot already set");
        }
        // Get the boot value of certain Options
        useFrame_ValueAtBoot = Options.getOption(Options.commandEntryFrame)
                .getString();
        comboCommandLine_ValueAtBoot = Options.getOption(
                Options.comboCommandLine).getBoolean();
    }

    // Might be better to save the option value in
    // WindowCmdEentry, then use it from there when
    // creating the command line window. But this
    // is extensible and guarenteed to preserve semantics.
    public static Object getOptionAtStartup(String optName) {
        switch(optName) {
        case Options.commandEntryFrame:
            return useFrame_ValueAtBoot;
        case Options.comboCommandLine:
            return comboCommandLine_ValueAtBoot;
        default:
            return null;
        }
    }
        
}

// vi:set sw=4 ts=8:
