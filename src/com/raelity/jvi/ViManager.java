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
package com.raelity.jvi;

import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;

import java.awt.datatransfer.SystemFlavorMap;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import com.raelity.jvi.swing.KeyBinding;
import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * <p>
 * This class coordinates things.
 * The platform (main application) notifies jVi about new editors that are
 * opened and switches between open editors. <b>NOTE:</b> unless otherwise
 * noted, methods in this class should only be invoked from the event dispatch
 * thread.
 * </p>
 * Here are several static methods used to inform jVi of major changes.
 * The <i>appHandle</i> is opaque to jVi; it means something to the platform.
 * It may be a logical container which tracks the editor. There is one of these
 * for each open editor, if the same document is editted in two windows, then
 * there are two of these. appHandle may be nulll; this signifies that the
 * editor is a "nomad" and is not associated directly with top level file data,
 * for example if the editor is in a dialog and is used to edit some app
 * configuration data. When appHandle is null, the editor is not added to the
 * MRU list, and so it does not show up in the ":ls" command.
 * <ul>
 * <li>{@link #activateAppEditor}(ep, appHandle)<br/>
 * The application invokes this whenever an editor becomes selected.
 * This also serves as an open.
 * </li>
 * <li>{@link #deactivateCurrentAppEditor}(appHandle)<br/>
 * Inform jVi that the currently active editor is going quiet. Primary function
 * is to take it out of input mode.
 * </li>
 * <li>{@link #closeAppEditor}(ep, appHandle)<br/>
 * The applications invokes this method when a file is completely
 * removed from a container or should be forgotten by jVi.
 * </li>
 * <li>requestSwitch(ep)<br/>
 * This rarely needs to be invoked directly. Use activateAppEditor.
 * </li>
 * </ul>
 * <b>NEEDSWORK:</b>
 * <ul>
 * <li>Get rid of requestSwitch. Do it automaticaly at end of activateAppEd</li>
 * </ul>
 */
public class ViManager
{
  private static Logger LOG = Logger.getLogger(ViManager.class.getName());
    
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

    private static JEditorPane currentEditorPane;
    private static ViFactory factory;

    private static Keymap editModeKeymap;
    private static Keymap normalModeKeymap;

    private static Map hackMap = new HashMap();
    public static void putHackMap(Object key, Object val) {
        hackMap.put(key, val);
    }
    public static Object getHackMap(Object key) {
        return hackMap.get(key);
    }
    public static boolean getHackFlag(Object key) {
        Boolean b = (Boolean) hackMap.get(key);
        return b == null || !b ? false : true;
    }

    // HACK: to workaround JDK bug dealing with focus and JWindows
    private static ViCmdEntry activeCommandEntry;

    //
    // 1.0.0.beta2 is NB vers 0.9.6.4
    // 1.0.0.beta3 is NB vers 0.9.7.5
    //
    public static final jViVersion version = new jViVersion("1.2.7.x10");

    private static boolean enabled;

    public static void setViFactory(ViFactory factory)
    {
        if(ViManager.factory != null)
        {
            throw new RuntimeException("ViFactory already set");
        }


        enabled = true;
        ViManager.factory = factory;

        Options.init();
        KeyBinding.init();
        Misc.read_viminfo_registers();

        // Add the vim clipboards

        // Spawn to get current release info
        new GetMotd().start();

        getViFactory().setShutdownHook(new Runnable() {
            public void run() {
                Misc.write_viminfo_registers();
            }
        });
    }

    /**
     * Disable the feature.
     * @param f feature to diable
     */
    public static void removeFeature(ViFeature f)
    {
        G.f.remove(f);
    }

    /**
     * Disable the set of features.
     * @param f features to disable
     */
    public static void removeFeature(EnumSet<ViFeature> f)
    {
        G.f.removeAll(f);
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

    private static boolean jdk15;
    private static boolean jdk16;

    static
    {
        try {
            jdk15 = true;
            jdk16 = false;
            Class.forName("java.util.ArrayDeque");
            jdk15 = false;
            jdk16 = true;
        } catch (ClassNotFoundException ex) { }
    }

    public static boolean isJdk15()
    {
        return jdk15;
    }
    public static boolean isJdk16()
    {
        return jdk16;
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
            if(s.startsWith("Windows"))
                osVersion = OsVersion.WINDOWS;
            else if(s.startsWith("Mac"))
                osVersion = OsVersion.MAC;
            else
                osVersion = OsVersion.UNIX;
        }
        return osVersion;
    }

    public static String getReleaseString()
    {
        return "jVi " + version;
    }

    public static ViFactory getViFactory()
    {
        return factory;
    }

    public static ViFS getFS()
    {
        return factory.getFS();
    }

    public static ViTextView getViTextView(JEditorPane editorPane)
    {
        return factory.getViTextView(editorPane);
    }

    /** get any text view, other than tv, which has buf KLUDGE HACK */
    public static ViTextView getAlternateTextView(ViTextView tv, Buffer buf)
    {
        ViTextView tv01 = null;
        Set<ViTextView> tvSet = factory.getViTextViewSet();
        for (ViTextView tv02 : tvSet) {
            if(tv == tv02)
                continue;
            JEditorPane ep = tv02.getEditorComponent();
            if(ep != null) {
                if(factory.getBuffer(ep) == buf) {
                    tv01 = tv02;
                    break;
                }
            }
        }
        return tv01;
    }

    public static Buffer getBuffer(JEditorPane editorPane)
    {
        return factory.getBuffer(editorPane);
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

    static public void installKeymap(JEditorPane editorPane)
    {
        editorPane.setKeymap(KeyBinding.getKeymap());
    }

    /**
     * Pass control to indicated ViCmdEntry widget. If there are
     * readahead or typeahead characters available, then collect
     * them up to a &lt;CR&gt; and append them to initialString.
     * If there was a CR, then signal the widget to immeadiately
     * fire its actionPerformed without displaying any UI element.
     */
    public static void startCommandEntry(
            ViCmdEntry commandEntry,
            String mode,
            ViTextView tv,
            StringBuffer initialString)
    {
        Msg.clearMsg();
        if(initialString == null) {
            initialString = new StringBuffer();
        }
        if(activeCommandEntry != null) {
            throw new RuntimeException("activeCommandEntry not null");
        }

        activeCommandEntry = commandEntry;
        boolean passThru;
        if (initialString.indexOf("\n") >= 0) {
            passThru = true;
        } else {
            passThru = GetChar.getRecordedLine(initialString);
        }
        try {
            commandEntry.activate(mode, tv, new String(initialString), passThru);
        } catch(Throwable ex) {
            // NOTE: do not set the flag until the activate completes.
            // There have been cases of NPE.
            // Particularly in relationship to nomands.
            //
            // If modal, and everything went well, then activeCommandEntry is
            // already NULL. But not modal, then it isn't null.
            Util.vim_beep();
            LOG.log(Level.SEVERE, null, ex);
            activeCommandEntry = null;
            Normal.resetCommand();
        }
    }

    public static void stopCommandEntry()
    {
        activeCommandEntry = null;
    }

    /** update visible textviews */
    public static void updateHighlightSearchState()
    {
        Set<ViTextView> s = factory.getViTextViewSet();
        for (ViTextView tv : s) {
            if(factory.isVisible(tv)) {
                tv.updateHighlightSearchState();
            }
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

    //
    // jVi maintains two lists of opened files: the order they opened, and a
    // MostRecentlyUsed list.
    //
    // Even if jVi is disabled, these methods can be used. They only maintain
    // the lists.
    //

    // NEEDSWORK: textMRU: use a weak reference to appHandle?
    static class WeakObject extends WeakReference<Object> {

        public WeakObject(Object referent) {
            super(referent);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof WeakObject))
                return false;
            Object o = get();
            Object other = ((WeakObject)obj).get();
            return o == null ? o == obj : o.equals(other);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            Object o = get();
            if(o != null)
                hash = o.hashCode();
            return hash;
        }
    }
    private static class BuffersList {
        List<WeakReference> l = new ArrayList();
    }
    private static List<Object> textBuffers = new ArrayList<Object>();
    private static List<Object> textMRU = new ArrayList<Object>();
    private static Set<WeakObject> textNomads = new LinkedHashSet<WeakObject>();
    private static Object currentlyActive;
    private static Object ignoreActivation;

    private static void setCurrentAppHandle(Object appHandle, boolean isNomad)
    {
        if(!isNomad) {
            // this used to be the default case
            textMRU.remove(appHandle);
            textMRU.add(0, appHandle);
            if( ! textBuffers.contains(appHandle)) {
                textBuffers.add(appHandle);
            }
            // Following for the bizare case where it was ...?
            textNomads.remove(new WeakObject(appHandle));
        } else {
            // insure nomads not in these lists
            textMRU.remove(appHandle);
            textBuffers.remove(appHandle);
            // and make sure it is in the nomad list
            // Don't need to check contained, since working with a Set.
            textNomads.add(new WeakObject(appHandle));
        }
    }

    /**
     * Fetch the text buffer indicated by the argument. The argument is
     * positive, fetch the Nth buffer, numbered 1 to N, according to
     * the order they were activated.
     * See {@link #getMruBuffer}.
     * @return the buffer or null if i does not specify an active buffer.
     */
    public static Object getTextBuffer(int i)
    {
        i = i - 1; // put in range 0 - (N-1)
        if(i < 0 || i >= textBuffers.size())
            return null;

        return textBuffers.get(i);
    }
    public static Iterator getTextBufferIterator()
    {
        return textBuffers.iterator();
    }

    /**
     * Fetch the Nth buffer, 0 to N-1, from the Mru list.
     * @return the buffer, else null if i is out of bounds.
     */
    public static Object getMruBuffer(int i)
    {
        if(i < 0 || i >= textMRU.size())
            return null;
        return textMRU.get(i);
    }

    public static Iterator getNomadBufferIterator()
    {
        //return textNomads.iterator();
        // While this is iterated, null elements are tossed out.
        // remove is not implemented.
        final Iterator<WeakObject> iter = textNomads.iterator();
        return new Iterator() {
            Object nextObject;

            // Find the next non-null object, removing nulls
            private void findNextObject() {
                if(nextObject != null)
                    return;
                WeakObject wo;
                while(iter.hasNext()) {
                    wo = iter.next();
                    nextObject = wo.get();
                    if(nextObject != null)
                        break;
                    iter.remove();
                }
            }

            public boolean hasNext() {
                if(nextObject != null)
                    return true;
                findNextObject();
                return nextObject != null;
            }

            public Object next() {
                findNextObject();
                if(nextObject == null)
                    throw new NoSuchElementException();
                Object o = nextObject;
                nextObject = null;
                return o;
            }

            public void remove() {
                // iter.remove() here probably works fine
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Return the Ith next/previous appHandle relative to the argument
     * appHandle. If i &lt 0 then look in previously used direction.
     */
    public static Object relativeMruBuffer(Object appHandle, int i)
    {
        if(factory != null && G.dbgEditorActivation.getBoolean()) {
            System.err.println("Activation: ViManager.relativeMruBuffer: "
                    + factory.getDisplayFilename(appHandle));
        }
        if(textMRU.size() == 0)
            return null;
        int idx = textMRU.indexOf(appHandle);
        if(idx < 0)
            return null;
        // the most recent is at index 0, so bigger numbers are backwwards in time
        idx += -i;
        if(idx < 0)
            idx = 0;
        else if(idx >= textMRU.size())
            idx = textMRU.size() -1;
        return textMRU.get(idx);
    }

    public static Object relativeMruBuffer(int i)
    {
        return relativeMruBuffer(currentlyActive, i);
    }
  
    /**
     * Request that the next activation does not re-order the mru list if the
     * activated object is the argment.
     */
    public static void ignoreActivation(Object appHandle)
    {
        if(!textBuffers.contains(appHandle)) {
            return; // can't ignore if its not in the list
        }
        ignoreActivation = appHandle;
    }

    /**
     * The application invokes this whenever a file becomes selected
     * in the specified container. This also serves as an open.
     * @param ep May be null, otherwise the associated editor pane
     * @param appHandle Usually, but not necessarily, a container that hold the
     *               editor. May be null if nomadic editor.
     * @param tag String used in debug messages.
     */
    public static void activateAppEditor(
            JEditorPane ep, Object appHandle, String tag)
    {
        if(factory != null && G.dbgEditorActivation.getBoolean()) {
            System.err.println("Activation: ViManager.activateAppEditor: "
                    + tag + " " + cid(ep) + " " + cid(appHandle)
                    + " " + factory.getDisplayFilename(appHandle)
                    );
        }
        if(ep != null && enabled)
            factory.registerEditorPane(ep);
        if(appHandle == null)
            return;

        Object ign = ignoreActivation;
        ignoreActivation = null;
        currentlyActive = appHandle;
        if(textBuffers.contains(ign) && appHandle == ign) {
            return;
        }

        setCurrentAppHandle(appHandle, factory.isNomadic(ep, appHandle));
    }

    public static void deactivateCurrentAppEditor(Object appHandle)
    {
        if(factory != null && G.dbgEditorActivation.getBoolean()) {
            System.err.println(
                    "Activation: ViManager.deactivateCurentAppEditor: "
                    + " " + cid(appHandle)
                    + factory.getDisplayFilename(appHandle));
        }
        // For several reasons, eg. don't want to hold begin/endUndo
        if(enabled)
            exitInputMode();

        currentlyActive = null;
    }

    public static boolean isKnownAppHandle(Object o)
    {
        return textBuffers.contains(o);
    }

    /**
     * The applications invokes this method when a file is completely
     * removed from a container or should be forgotten by jVi.
     */
    public static void closeAppEditor(JEditorPane ep, Object appHandle)
    {
        if(factory != null && G.dbgEditorActivation.getBoolean()) {
            String fname = factory.getDisplayFilename(appHandle);
            System.err.println("Activation: ViManager.closeAppEditor: "
                    + (ep == null ? "(no shutdown) " : "") + fname);
        }

        assert(factory != null);
        if(factory != null && ep != null && enabled) {
            factory.shutdown(ep);
        }
        if(appHandle != null) { // null indicates nomad
            if(appHandle == currentlyActive)
                currentlyActive = null;
            textMRU.remove(appHandle);
            textBuffers.remove(appHandle);
        }
    }

    //
    // END of OpenEditors list handling
    //

    public static void log(Object... a)
    {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            s.append(a[i]);
        }
        System.err.println(s);
    }

    /**
     * A key was typed. Handle the event.
     * <br>NEEDSWORK: catch all exceptions comming out of here?
     */
    static public void keyStroke(JEditorPane target, char key, int modifier)
    {
        if(jViBusy != 0) {
            ViManager.dumpStack();
        }
        try {
            setJViBusy(true);

            switchTo(target);
            if(rerouteChar(key, modifier)) {
                return;
            }
            factory.finishTagPush(G.curwin);
            GetChar.gotc(key, modifier);

            if(G.curwin != null)
                G.curwin.getStatusDisplay().refresh();
        } finally {
            setJViBusy(false);
        }
    }

    /** If chars came in between the time a dialog was initiated and
     * the time the dialog starts taking the characters, we feed the
     * chars to the dialog.
     * <p>Special characters are discarded.
     * </p>
     */
    static boolean rerouteChar(char c, int modifiers)
    {
        if(activeCommandEntry == null) {
            return false;
        }
        if((c & 0xF000) != KeyDefs.VIRT
                && modifiers == 0) {
            if(c >= 0x20 && c != 0x7f) {
                if(KeyBinding.isKeyDebug()) {
                    System.err.println("rerouteChar");
                }
                activeCommandEntry.append(c);
            }
        }
        // System.err.println("rerouteChar " + (char)c);
        return true;
    }

    /** requestSwitch can be used from platform code for situation where an
     * editor is activated. It allows things to be initialized,
     * with some visual implications, before a key is entered.
     * It should typically only be used after activateAppEditor.
     */
    public static void requestSwitch(JEditorPane ep)
    {
        switchTo(ep);
    }

    private static boolean started = false;
    static void switchTo(JEditorPane editorPane)
    {
        if(editorPane == currentEditorPane) {
            return;
        }
        if( ! started) {
            started = true;
            startup();
        }
        motd.outputOnce();

        exitInputMode(); // if switching, make sure prev out of input mode
        draggingBlockMode = false;

        ViTextView textView = getViTextView(editorPane);
        Buffer buf = getBuffer(editorPane);
        factory.registerEditorPane(editorPane); // make sure has the right caret
        textView.attach();
        if(G.dbgEditorActivation.getBoolean()) {
            System.err.println("Activation: ViManager.SWITCHTO: "
                    + cid(editorPane) + " " + buf.getDisplayFileName());
        }

        if(currentEditorPane != null) {
            Normal.abortVisualMode();
            ViTextView currentTv = getViTextView(currentEditorPane);
            // Freeze and/or detach listeners from previous active view
            currentTv.detach();
        }

        currentEditorPane = editorPane;
        G.switchTo(textView, buf);
        Normal.resetCommand(); // Means something first time window switched to
        buf.activateOptions(textView);
        textView.activateOptions(textView);
    }

    public static ViTextView getCurrentTextView()
    {
        return factory.getExistingViTextView(currentEditorPane);
    }

    private static boolean inStartup;
    /** invoked once when vi is first used */
    private static void startup()
    {
        setupStartupList();
        inStartup = true;
        Iterator iter = startupList.iterator();
        while(iter.hasNext()) {
            ((ActionListener)iter.next()).actionPerformed(null);
        }
        Misc.javaKeyMap = KeyBinding.initJavaKeyMap();
        inStartup = false;
        startupList = null;
    }

    static List<ActionListener> startupList;
    static void setupStartupList()
    {
        if(startupList == null) {
            startupList = new ArrayList<ActionListener>();
        }
    }

    /**
     * Add listener to invoke when editor is starting up.
     * A null argument can be used to test if startup has
     * already occured.
     * @return true if listener add, otherwise false indicates
     * that startup has already occured.
     */
    public static boolean addStartupListener(ActionListener l)
    {
        if(started) {
            return false;
        }
        if(l != null) {
            setupStartupList();
            startupList.add(l);
        }
        return true;
    }

    public static void removeStartupListener(ActionListener l)
    {
        if(inStartup) {
            return;
        }
        startupList.remove(l);
    }

    /**
     * The arg JEditorPane is detached from its text view,
     * forget about it.
     */
    public static void detached(JEditorPane ep)
    {
        if(currentEditorPane == ep) {
            if(G.dbgEditorActivation.getBoolean()) {
                System.err.println("Activation: ViManager.detached " + cid(ep));
            }
            currentEditorPane = null;
        }
    }

    public static void exitInputMode() {
        if(currentEditorPane != null) {
            Normal.resetCommand();
        }
    }

    private static boolean draggingBlockMode;
    private static boolean mouseDown;

    public static boolean isMouseDown()
    {
        return mouseDown;
    }

    /**
     * A mouse click; switch to the activated editor.
     * Pass the click on to the window and give it
     * a chance to adjust the position and whatever.
     */
    public static int mouseSetDot(int pos, JTextComponent c, MouseEvent mev)
    {
        try {
            setJViBusy(true);
            mouseDown = true;

            if(!(c instanceof JEditorPane)) {
                return pos;
            }

            GetChar.flush_buffers(true);
            exitInputMode();
            if(currentEditorPane != null)
                Normal.abortVisualMode();

            JEditorPane editorPane = (JEditorPane)c;

            ViTextView tv = factory.getExistingViTextView(editorPane);
            if(tv == null)
                return pos;

            switchTo(editorPane);
            /*int lookFor = mev.ALT_DOWN_MASK | mev.BUTTON1_DOWN_MASK;
            int mods = mev.getModifiersEx();
            if((mev.getModifiersEx() & lookFor) == lookFor) {
                draggingBlockMode = true;
                System.err.println("START_DRAG");
            }*/
            pos = tv.validateCursorPosition(pos);

            if(G.dbgMouse.getBoolean()) {
                System.err.println("mouseSetDot(" + pos + ") "
                        + MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(
                //                      mev.getModifiers()));
            }
            return pos;
        } finally {
            setJViBusy(false);
        }
    }

    public static void mouseRelease(MouseEvent mev)
    {
        try {
            setJViBusy(true);

            int mask = MouseEvent.BUTTON1_DOWN_MASK
                    | MouseEvent.BUTTON2_DOWN_MASK
                    | MouseEvent.BUTTON3_DOWN_MASK;

            if ((mev.getModifiersEx() & mask) == 0)
                mouseDown = false;

            /*if(draggingBlockMode
                && (mev.getModifiersEx() & mev.BUTTON1_DOWN_MASK) == 0) {
                draggingBlockMode = false;
                System.err.println("END_DRAG");
            }*/

            if(G.dbgMouse.getBoolean()) {
                System.err.println("mouseRelease: "
                        + MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(
                //                      mev.getModifiers()));
            }
        } finally {
            setJViBusy(false);
        }
    }

    public static int mouseMoveDot(int pos, JTextComponent c, MouseEvent mev)
    {
        if(true) // NEEDSWORK: this method doesn't do anything anymore
            return pos;

        try {
            setJViBusy(true);

            if(G.curwin == null || c != G.curwin.getEditorComponent()) {
                return pos;
            }

            //
            // Don't automatically go into visual mode on a drag,
            // vim does "SELECT" mode.
            // But when in select mode would like to extend selection on arrow keys,
            // which is also like vim.
            //
            // if(pos != G.curwin.getCaretPosition() && !G.VIsual_active) {
            //   G.VIsual_mode ='v';
            //   G.VIsual_active = true;
            //   G.VIsual = (FPOS) G.curwin.getWCursor().copy();
            //   Misc.showmode();
            // }

            if(G.dbgMouse.getBoolean()) {
                System.err.println("mouseMoveDot(" + pos + ") "
                        + MouseEvent.getModifiersExText(mev.getModifiersEx()));
                //System.err.println(mev.getMouseModifiersText(mev.getModifiers()));
            }

            return pos;
        } finally {
            setJViBusy(false);
        }
    }

    /** The viewport has changed or scrolled, clear messages*/
    public static void viewMoveChange(ViTextView textView)
    {
        if(G.curwin == null) {
            // this case is because switchto, does attach, does viewport init
            // but G.curwin is not set yet. See switchTo(JEditorPane editorPane)
            return;
        }
        Msg.clearMsg();
    }

    static public void dumpStack(String msg, boolean supressIfNotBusy)
    {
        if(supressIfNotBusy && !jViBusy())
            return;
        LOG.log(Level.SEVERE, msg, new IllegalStateException());
    }

    static public void dumpStack(String msg)
    {
        dumpStack(null, false);
    }

    static public void dumpStack()
    {
        dumpStack(null);
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
  
    // ":jviDump"
    static public void dump(PrintStream ps)
    {
        ps.println("-----------------------------------");
        ps.println("currentEditorPane = "
                + (G.curwin == null ? "null" : G.curbuf.getDisplayFileName()));

        ps.println("factory = " + factory );

        ps.println("textBuffers: " + textBuffers.size());
        for (Object appHandle : textBuffers) {
            ps.println("\t" + factory.getDisplayFilename(appHandle)
                    + ", " + appHandle.getClass().getSimpleName());
        }
        ps.println("textMRU: " + textMRU.size());
        for (Object appHandle : textMRU) {
            ps.println("\t" + factory.getDisplayFilename(appHandle)
                    + ", " + appHandle.getClass().getSimpleName());
        }
        ps.println("currentlyActive: " + (currentlyActive == null ? "none"
                : "" + factory.getDisplayFilename(currentlyActive)
                + ", " + currentlyActive.getClass().getSimpleName()));
        ps.println("ignoreActivation: " + (ignoreActivation == null ? "none"
                : "" + factory.getDisplayFilename(ignoreActivation)
                + ", " + ignoreActivation.getClass().getSimpleName()));

        Set<ViTextView> tvSet = factory.getViTextViewSet();
        ps.println("TextViewSet: " + tvSet.size());
        for (ViTextView tv : tvSet) {
            ps.println("\t" + tv.getBuffer().getDisplayFileName());
        }

        Set<Buffer> bufSet = factory.getBufferSet();
        ps.println("BufferSet: " + bufSet.size());
        for (Buffer buf : bufSet) {
            if(buf == null)
                ps.println("null-buf");
            else
                ps.println("\t" + factory.getDisplayFilename(buf.getDocument())
                           + ", share: " + buf.getShare());
        }

        ps.println("textNomads: " + textNomads.size());
        Iterator iter = getNomadBufferIterator();
        while(iter.hasNext()) {
            Object appHandle = iter.next();
            ps.println("\t" + factory.getDisplayFilename(appHandle)
                    + ", " + appHandle.getClass().getSimpleName());
        }
    }

    /** version is of the form #.#.# or #.#.#.[x|alpha|beta|rc]#,
    * examples 0.9.1, 0.9.1.beta1
    * also, 0.9.1.beta1.3 for tweaking between exposed releases
    */
    public static final class jViVersion implements Comparable<jViVersion>
    {
        // in order
        public static final String X = "x";
        public static final String ALPHA = "alpha";
        public static final String BETA = "beta";
        public static final String RC = "rc";
        // following is map in order of suspected quality, these map to values
        // 0, 1, 2, 3
        // a release has none of these tags and compares greater than any of
        // them since it is set to value qualityMap.length, value == 4
        String[] qualityMap = new String[] { X, ALPHA, BETA, RC };

        // Each component of the version is an element of the array.
        // major.minor.micro
        // major.minor.micro.<quality>which
        // If no a|alpha.... then this is the
        private int[] version = new int[6];

        private boolean valid;

        public jViVersion(String s)
        {
            String rev[] = s.split("\\.");
            if(rev.length < 3 || rev.length > 5) {
                init(0, 0, 0, 0, 0);
                return;
            }
            for(int i = 0; i < 3; i++)  {
                try {
                    version[i] = Integer.parseInt(rev[i]);
                } catch (NumberFormatException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    init(0, 0, 0, 0, 0);
                    return;
                }
            }
            valid = true;
            if(rev.length == 3) {
                // A release version, no quality tag or tweak,
                // it is "better" than those
                version[3] = qualityMap.length;
            } else {
                // so this is something between releases
                // version[0:2] has 1.2.3 stored in it
                // rev[3] has string like beta3, rev[4] may have a tweak; beta3.7
                // into rev[0:2] put strings for beta,3,7
                Pattern p = Pattern.compile("(x|alpha|beta|rc)(\\d+)");
                Matcher m = p.matcher(rev[3]);
                // Note, if doesn't match, then version number looks like 1.2.3.x0
                // since version[3:5] left at zero
                if(m.matches()) {
                    String q = m.group(1);
                    for(int i = 0; i < qualityMap.length; i++) {
                        if(q.equals(qualityMap[i])) {
                            rev[0] = "" + i;
                            break;
                        }
                    }
                    rev[1] = m.group(2);
                    // if there's a tweak on beta1, then copy it, else set to zero
                    rev[2] = rev.length == 5 ? rev[4] : "0";
                    try {
                        for(int i = 0; i <= 2; i++)
                            version[i+3] = Integer.parseInt(rev[i]);
                    } catch (NumberFormatException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            }
            //System.err.println("input: " + s + ", version: " + this);
        }

        private void init(int major, int minor, int micro, int qTag, int qVer)
        {
            version[0] = major;
            version[1] = minor;
            version[2] = micro;
            version[3] = qTag;
            version[4] = qVer;
        }

        public boolean isValid()
        {
            return valid;
        }

        public boolean isRelease()
        {
            return version[3] == qualityMap.length;
        }

        public boolean isDevelopment()
        {
            // development releases are x, alpha or any tweaks
            // beta and rc are not (unless tweaked)
            int qTag = version[3];
            return qTag == 0 || qTag == 1 || getTweak() != 0;
        }

        @Override
        public String toString()
        {
            String s =   "" + version[0]
                    + "." + version[1]
                    + "." + version[2];
            if(version[3] != qualityMap.length)
                s += "." + qualityMap[version[3]] + version[4];
            if(version[5] != 0)
                s += "." + version[5];
            return s;
        }

        public int getMajor()
        {
            return version[0];
        }

        public int getMinor()
        {
            return version[1];
        }

        public int getMicro()
        {
            return version[2];
        }

        public int getTweak()
        {
            return version[5];
        }

        public String getTag()
        {
            if(isRelease())
                return "";
            return qualityMap[version[3]] + version[4]
                    + (getTweak() == 0 ? "" : getTweak());
        }

        public int compareTo(ViManager.jViVersion v2)
        {
            for(int i = 0; i < version.length; i++) {
                if(version[i] != v2.version[i])
                    return version[i] - v2.version[i];
            }
            return 0;
        }
    }
  
    static Motd motd = new Motd();

    /**
     * Parse and output the jVi motd data.
     * The fields are:
     * <br/>    motd-version: version-number
     * <br/>    jVi-release: release-number
     * <br/>    jVi-beta: release-number
     * <br/>    jVi-download-target: where to download new jvi stuff
     * <br/>    motd-link: link-with-no-spaces
     * <br/>    display text for link
     * <br/>    &lt;EOT&gt;
     * <br/>    motd-message:
     *          The message, followed by a line starting with &lt;EOT&gt;
     * <p/>
     * Note:    there can be any number of motd-link and motd-message and
     *          they are output in the order encounter
     *
     */
    static class Motd
    {
        private interface  OutputHandler {
            public void output(ViOutputStream vios);
        }
        private class OutputString implements OutputHandler {
            String msg;

            public OutputString(String msg) {
                this.msg = msg;
            }

            public void output(ViOutputStream vios) {
                vios.println(msg);
            }

            @Override
            public String toString() {
                return msg;
            }
        }
        private class OutputLink implements OutputHandler {
            String link, text;

            public OutputLink(String link, String text) {
                this.link = link;
                this.text = text;
            }

            public void output(ViOutputStream vios) {
                vios.printlnLink(link, text);
            }

            @Override
            public String toString() {
                return link + " : " + text;
            }
        }
        private jViVersion latestRelease;
        private jViVersion latestBeta;
        private String motdVersion;
        private String downloadTarget;

        private int messageNumber;
        private String message;
        private boolean valid;
        private boolean outputNetworkInfo;
        private boolean outputBasicInfo;
        // following could be a list of pairs of link:text
        List<OutputHandler> outputList = new ArrayList<OutputHandler>(5);

        Motd()
        {
            // not valid
        }

        boolean getValid()
        {
            return valid;
        }

        Motd(String s)
        {
            //String lines[] = motd.split("\n");
            Pattern p;
            Matcher m;

            p = Pattern.compile("^motd-version:\\s*(\\S+)", Pattern.MULTILINE);
            m = p.matcher(s);
            if(m.find()) {
                motdVersion = m.group(1);
            }

            p = Pattern.compile("^jVi-release:\\s*(\\S+)", Pattern.MULTILINE);
            m = p.matcher(s);
            if(m.find()) {
                latestRelease = new jViVersion(m.group(1));
            }

            p = Pattern.compile("^jVi-beta:\\s*(\\S+)", Pattern.MULTILINE);
            m = p.matcher(s);
            if(m.find()) {
                latestBeta = new jViVersion(m.group(1));
            }

            p = Pattern.compile("^jVi-download-target:\\s*(\\S+)",
                                Pattern.MULTILINE);
            m = p.matcher(s);
            if(m.find()) {
                downloadTarget = m.group(1);
            }

            p = Pattern.compile(  "^(?:motd-link:\\s*(\\S+)"
                                +   "|motd-message:)"
                                +                       "\\s*\\n"
                                + "(.*?)\\s*\\n<EOT>",
                                Pattern.MULTILINE|Pattern.DOTALL);
            m = p.matcher(s);
            while(m.find()) {
                if(m.start(1) >= 0) {
                    outputList.add(new OutputLink(m.group(1), m.group(2)));
                } else {
                    outputList.add(new OutputString(m.group(2)));
                }
            }

            valid = true;
        }

        void outputOnce()
        {
            if(outputNetworkInfo)
                return;
            if(outputBasicInfo && !valid)
                return;
            output(G.isHideVersion.value
                    ? ViOutputStream.PRI_LOW
                    : ViOutputStream.PRI_NORMAL);
        }

        void output()
        {
            output(ViOutputStream.PRI_NORMAL);
        }

        void output(int priority)
        {
            if(!valid) {
                ViOutputStream vios = ViManager.createOutputStream(
                    null, ViOutputStream.OUTPUT, getReleaseString(), priority);
                vios.close();
                outputBasicInfo = true;
                return;
            }
            outputNetworkInfo = true;

            ViOutputStream vios = ViManager.createOutputStream(
                null, ViOutputStream.OUTPUT, "jVi Version Information", priority);

            String tagCurrent = "";
            String hasNewer = null;
            if(latestRelease != null && latestRelease.isValid()) {
                if(latestRelease.compareTo(version) > 0) {
                    hasNewer = "Newer release available: " + latestRelease;
                } else if(latestRelease.compareTo(version) == 0)
                    tagCurrent = " (This is the latest release)";
                else {
                    // In this else, should be able to assert that !isRelease()
                    if(version.isDevelopment())
                        tagCurrent = " (development release)";
                }
            }
            vios.println("Running: " + getReleaseString() + tagCurrent);
            if(hasNewer != null)
                vios.printlnLink(downloadTarget, hasNewer);
            if(latestBeta != null && latestBeta.isValid()) {
                if(latestBeta.compareTo(version) > 0) {
                    vios.printlnLink(downloadTarget,
                                   "Beta or release candidate available: "
                                   + latestBeta);
                }
            }
            for (int i = 0; i < outputList.size(); i++) {
                OutputHandler outputHandler = outputList.get(i);
                outputHandler.output(vios);
            }
            //if(message != null)
            //    vios.println(message);
            vios.close();
        }
    }

    static void debugMotd() {
        new GetMotd(true).start();
    }

    private static class GetMotd extends Thread
    {
        private static final int BUF_LEN = 1024;
        private static final int MAX_MSG = 8 * 1024;
        private boolean outputOnly;

        public GetMotd() {
        }

        public GetMotd(boolean outputOnly) {
            this.outputOnly = outputOnly;
        }

        @Override
        public void run()
        {
            URL url = null;
            try {
                String s = System.getProperty("com.raelity.jvi.motd");
                if(s != null)
                    System.err.println("DEBUG MOTD: " + s);
                if(s == null)
                    s = "http://jvi.sourceforge.net/motd";
                URI uri = new URI(s);
                url = uri.toURL();
            } catch (MalformedURLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            } catch (URISyntaxException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            if(url == null)
                return;

            // Read the remote file into a string
            // We *know* that the decoder will never have unprocessed
            // bytes in it, US-ASCII ==> 1 byte per char.
            // So use a simple algorithm.
            try {
                URLConnection c = url.openConnection();
                InputStream in = c.getInputStream();
                byte b[] = new byte[BUF_LEN];
                ByteBuffer bb = ByteBuffer.wrap(b);
                StringBuilder sb = new StringBuilder();
                Charset cset = Charset.forName("US-ASCII");
                int n;
                int total = 0;
                while((n = in.read(b)) > 0 && total < MAX_MSG) {
                    bb.position(0);
                    bb.limit(n);
                    CharBuffer cb = cset.decode(bb);
                    sb.append(cb.toString());
                    total += n;
                }
                in.close();

                if(!outputOnly)
                    motd = new Motd(sb.toString());
                else
                    new Motd(sb.toString()).output();
            } catch (IOException ex) {
                //ex.printStackTrace();
            }
        }
    }

    /**
    * Copy preferences tree.
    */
    public static void copyPreferences(Preferences dst,
                                Preferences src,
                                boolean clearDst)
    {
      new CopyPreferences(dst, src, clearDst);
    }

    static class CopyPreferences
    {
        Preferences srcRoot;
        Preferences dstRoot;

        CopyPreferences(Preferences dst, Preferences src, boolean clear)
        {
            dstRoot = dst;
            srcRoot = src;
            //copyJVi("");
            //copyKeys(PREFS_KEYS);
            copyTree("");
        }

        private void copyTree(String dir)
        {
            try {
                Preferences srcNode = srcRoot.node(dir);
                Preferences dstNode = dstRoot.node(dir);
                String[] children = srcRoot.node(dir).childrenNames();
                String[] keys = srcRoot.node(dir).keys();
                for(String key : keys) {
                    dstNode.put(key, srcNode.get(key, ""));
                }
                for(String child : children) {
                    String subTree = dir.equals("") ? child : (dir + child);
                    copyTree(subTree);
                }
            } catch (BackingStoreException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }

        //
        // The following copyXXX get rid of stuff set to the default
        // For it to work, a lot of stuff has to be set up
        //

        private void copyJVi(String dir)
        {
            String[] children;
            String[] options;
            try {
                Preferences srcNode = srcRoot.node(dir);
                Preferences dstNode = dstRoot.node(dir);
                children = srcRoot.node(dir).childrenNames();
                options = srcRoot.node(dir).keys();
                for(String optionName : options) {
                    Option opt = Options.getOption(optionName);
                    if(opt != null) {
                        String val;
                        if(!(val = srcNode.get(optionName, opt.getDefault()))
                                .equals(opt.getDefault())) {
                            System.err.println(
                                    "ADD: " + optionName + ":" + val);
                            dstNode.put(optionName, val);
                        } else {
                            System.err.println(
                                    "DEF: " + optionName + ":" + val);
                        }
                    } else {
                        System.err.println("OPTION NOT FOUND: " + optionName);
                        dstNode.put(optionName, srcNode.get(optionName, ""));
                    }
                }
            } catch (BackingStoreException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            System.err.println("copy out");
        }

        private void copyKeys(String dir)
        {
            String[] children;
            String[] options;
            try {
                Preferences srcNode = srcRoot.node(dir);
                Preferences dstNode = dstRoot.node(dir);
                children = srcRoot.node(dir).childrenNames();
                options = srcRoot.node(dir).keys();
                for(String optionName : options) {
                    if(KeyBinding.isKnownKey(optionName)) {
                        boolean val;
                        boolean sDefault
                                = KeyBinding.getCatchKeyDefault(optionName);
                        val = srcNode.getBoolean(optionName, sDefault);
                        if(val != sDefault) {
                            System.err.println("ADD: " + optionName + ":" +val);
                            dstNode.putBoolean(optionName, val);
                        } else
                            System.err.println("DEF: " + optionName + ":" +val);
                    } else {
                        System.err.println("OPTION NOT FOUND: " + optionName);
                        dstNode.put(optionName, srcNode.get(optionName, ""));
                    }
                }
            } catch (BackingStoreException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            System.err.println("copy out");
        }
    }
}

// vi:set sw=4 ts=8:
