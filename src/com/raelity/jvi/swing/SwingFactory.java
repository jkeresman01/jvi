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

package com.raelity.jvi.swing;

import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.TextAction;

import org.openide.util.WeakSet;

import com.raelity.jvi.ViTextView.TAGOP;
import com.raelity.jvi.*;
import com.raelity.jvi.core.*;
import com.raelity.jvi.core.Commands.ColonEvent;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.core.lib.KeyDefs.KeyStrokeType;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.*;
import com.raelity.jvi.swing.simple.*;

import static com.raelity.jvi.core.G.dbgEditorActivation;
import static com.raelity.jvi.core.lib.KeyDefs.VIRT;

import static java.util.logging.Level.*;


/**
 * This provides the Vi items to interface with standard swing JTextComponent.
 * <b>NEEDSWORK:</b><ul>
 * <li> only one text view supported for now
 * </ul>
 */
abstract public class SwingFactory implements ViFactory
{
    public static final String PROP_TV = "ViTextView";
    public static final String PROP_BUF  = "ViBuffer";
    public static final String PROP_AV = "ViAppView";
    // the following can be null'd to force creation of a new action
    protected Action defaultAction;
    
    // all doc's that have been seen.
    protected Set<Document> docSet = new WeakSet<>();
    
    // This is used only when dbgEditorActivation is turned on
    protected WeakHashMap<JTextComponent, Object> editorSet = new WeakHashMap<>();
    
    JDialog dialog;
    protected static SwingFactory INSTANCE;
    
    private static final boolean IS_MAC = ViManager.getOsVersion().isMac();
    private MouseInputAdapter mouseAdapter;
    private KeyListener keyListener;
    
    // ............
    
    /**
     *  Default constructor.
     */
    public SwingFactory()
    {
        if ( INSTANCE != null ) {
            throw new IllegalStateException("ViFactory already exists");
        }
        captureINSTANCE();
        
        // Add VimClipboard DataFlavor if not already there
        // FlavorMap fm = SystemFlavorMap.getDefaultFlavorMap();
    }
    
    private void captureINSTANCE()
    {
        INSTANCE = this;

        Commands.register("dumpUIColors", "dumpUIColors", new DumpUIColors(),
                               EnumSet.of(CcFlag.DBG));
    }
    
    //////////////////////////////////////////////////////////////////////
    //
    // Some swing specific things that are "factory"ish,
    // that an implementation may want to override
    //

    /**
     * The default action is typically shared.
     * @return
     */
    public Action getDefaultAction()
    {
        if(defaultAction == null)
            defaultAction = createCharAction(
                    DefaultEditorKit.defaultKeyTypedAction);
        return defaultAction;
    }
    
    /**
     * @return action suitable for default key action
     */
    public Action createCharAction( String name )
    {
        return new EnqueCharAction(name);
    }
    
    
    /**
     * @return action for picking up specified key
     */
    public Action createKeyAction( String name, char key )
    {
        return new EnqueKeyAction(name, key);
    }
    
    @Override
    public ViWindowNavigator getWindowNavigator()
    {
        List<ViAppView> avs = Misc01.getVisibleAppViews(AppViews.ALL);
        if(avs == null)
            return null;
        return new SimpleWindowTreeBuilder(avs);
    }
    
    @Override
    public ViWindowNavigator getWindowNavigator(List<ViAppView> avs)
    {
        return new SimpleWindowTreeBuilder(avs);
    }
    
    //////////////////////////////////////////////////////////////////////
    //
    // ViFactory for swing
    //
    
    @Override
    public Class<?> loadClass( String name ) throws ClassNotFoundException
    {
        // NEEDSWORK: should this be systemclassloader or this's class loader???
        Class<?> c = ClassLoader.getSystemClassLoader().loadClass(name);
        return c;
    }
    
    @Override
    public final ViTextView getTextView(Component ed)
    {
        if(ed == null)
            return null;
        return (ViTextView)(((JTextComponent)ed).getClientProperty(PROP_TV));
    }


    
    @Override
    public final ViTextView createTextView(Component editor)
    {
        JTextComponent ed = (JTextComponent)editor;
        ViTextView tv01 = (ViTextView)ed.getClientProperty(PROP_TV);
        if ( tv01 == null ) {
            dbgEditorActivation().println(INFO, "Activation: getViTextView: create");
            tv01 = newTextView(ed);
            attachBuffer(tv01);
            
            tv01.startup();
            ed.putClientProperty(PROP_TV, tv01);
            editorSet.put(ed, null);
        }
        return tv01;
    }
    
    abstract protected ViTextView newTextView( JTextComponent ed );
    
    @Override
    public final Set<ViTextView> getViTextViewSet()
    {
        Set<ViTextView> s = new HashSet<>();
        for (JTextComponent ed : editorSet.keySet()) {
            ViTextView tv = (ViTextView) ed.getClientProperty(PROP_TV);
            if(tv != null)
                s.add(tv);
        }
        
        return s;
    }
    
    
    abstract protected Buffer createBuffer( ViTextView tv );
    
    
    @Override
    public Set<Buffer> getBufferSet() // NEEDSWORK: collection, list MRU?
    {
        Set<Buffer> s = new HashSet<>();
        for (Document doc : docSet) {
            Buffer buf = (Buffer) doc.getProperty(PROP_BUF);
            if ( buf != null ) {
                s.add(buf);
            }
        }
        
        return s;
    }
    
    
    @Override
    public void changeBuffer(ViTextView tv, Object _oldDoc)
    {
        // don't think this ever happens
        Document oldDoc = (Document) _oldDoc;
        dbgEditorActivation().println(SEVERE, "Activation: changeBuffer");
        attachBuffer(tv);
        releaseBuffer((Buffer)oldDoc.getProperty(PROP_BUF));
    }
    

    /** for either JTextComponent or Document */
    @Override
    public ViBuffer getBuffer(Object obj) {
        if(obj == null)
            return null;
        Document doc;
        if(obj instanceof JTextComponent)
            doc = ((JTextComponent)obj).getDocument();
        else if(obj instanceof Document)
            doc = (Document)obj;
        else
            return null;
        return (Buffer)doc.getProperty(PROP_BUF);
    }
    
    private void attachBuffer(ViTextView tv)
    {
        Document doc = ((JTextComponent)tv.getEditor()).getDocument();
        Buffer buf = null;
        if ( doc != null )
        {
            buf = (Buffer)doc.getProperty(PROP_BUF);
            if ( buf == null )
            {
                buf = createBuffer(tv);
                doc.putProperty(PROP_BUF, buf);
                docSet.add(doc);
            }
            buf.addShare();
        }
        tv.attachBuffer(buf);
    }
    
    private void releaseBuffer(Buffer buf)
    {
        if ( buf != null ) {
            Document doc = (Document)buf.getDocument();
            buf.removeShare();
            if(buf.getShare() == 0) {
                if ( doc != null) {
                    doc.putProperty(PROP_BUF, null);
                    docSet.remove(doc);
                } else {
                    ViManager.dumpStack("SHUTDOWN NULL DOC");
                }
            }
        }
    }
    
    
    @Override
    public String getPlatformSelectionDisplayName()
    {
        return "PLATFORM-SELECTION";
    }
    
    
    @Override
    public ViAppView getAppView(Component e) {
        return (ViAppView)((JTextComponent)e).getClientProperty(PROP_AV);
    }
    
    @Override
    public void shutdown( Component editor )
    {
        JTextComponent ed = (JTextComponent)editor;
        ViTextView tv = (ViTextView)ed.getClientProperty(PROP_TV);
        if ( tv == null ) {
            ed.putClientProperty(PROP_AV, null);
            return;
        }
        
        dbgEditorActivation().println(CONFIG, "Activation: shutdown TV");
        Buffer buf = (Buffer)tv.getBuffer();
        tv.shutdown();
        ed.putClientProperty(PROP_TV, null);
        ed.putClientProperty(PROP_AV, null);
        releaseBuffer(buf);
    }


    @Override
    public void setShutdownHook(Runnable hook) {
        Runtime.getRuntime().addShutdownHook(new Thread(hook));
    }
    
    
    /**
     *  Get the glass pane for the given component, if it doesn't
     *  have an associated mouseAdapter create one and add it.
     */
    private Container getModalGlassPane( final Component c )
    {
        Container glass = null;
        if ( c != null ) {
            JRootPane rp = SwingUtilities.getRootPane(c);
            if ( rp != null ) {
                glass = (Container) rp.getGlassPane();
                if ( mouseAdapter == null ) {
                    mouseAdapter = new MouseInputAdapter() {
                        @Override
                        public void mousePressed(MouseEvent evt) {
                            c.getToolkit().beep();
                        }
                    };
                    glass.addMouseListener(mouseAdapter);
                    glass.addMouseMotionListener(mouseAdapter);
                }
            }
        }
        return glass;
    }
    
    
    /**
     *  Method to establish a glass pane with the param key listener and all
     *  mouse events are blocked.
     *  This is not modal, in particular the event thread is still running, but
     *  it blocks the current window.
     */
    @Override
    public void startGlassKeyCatch(KeyListener kl)
    {
        if ( mouseAdapter != null ) {
            throw new IllegalStateException("Already in modal state");
        }
        
        Container glass = getModalGlassPane(G.curwin().getEditor());
        keyListener = kl;
        glass.addKeyListener(kl);
        glass.setVisible(true);
        
        // disable all focus traversal
        Set<AWTKeyStroke>noKeyStroke = Collections.emptySet();
        glass.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                noKeyStroke);
        glass.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                noKeyStroke);
        glass.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS,
                noKeyStroke);
        glass.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
                noKeyStroke);
        glass.setFocusCycleRoot(true);
        
        glass.requestFocusInWindow();
    }
    
    
    @Override
    public void stopGlassKeyCatch()
    {
        if ( mouseAdapter == null ) {
            throw new IllegalStateException("Not in modal state");
        }
        Container glass = getModalGlassPane(G.curwin().getEditor());
        glass.setVisible(false);
        glass.removeMouseListener(mouseAdapter);
        glass.removeMouseMotionListener(mouseAdapter);
        glass.removeKeyListener(keyListener);
        mouseAdapter = null;
        keyListener = null;
        
        // Back to default bahavior
        glass.setFocusCycleRoot(false);
        glass.setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        glass.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        glass.setFocusTraversalKeys(
                KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS, null);
        glass.setFocusTraversalKeys(
                KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, null);
        G.curwin().getEditor().requestFocusInWindow();
    }
    
    
    @Override
    public void startModalKeyCatch( KeyListener kl )
    {
        JTextComponent ed = (JTextComponent)G.curwin().getEditor();
        java.awt.Window wep = SwingUtilities.getWindowAncestor(ed);
        dialog = new JDialog((Frame)wep, "jVi", true);
        dialog.setUndecorated(true); // on windows see nothing, perfect
        dialog.pack();
        // place dialog in lower left of editor
        Container jc = SwingUtilities.getAncestorOfClass(
                javax.swing.JScrollPane.class,
                G.curwin().getEditor());
        if ( jc == null ) {
            jc = (JTextComponent)G.curwin().getEditor();
        }
        // put the dialog just below the editor pane, on the right
        Dimension d00 = dialog.getSize();
        Point p00 = jc.getLocation();
        p00.translate(jc.getWidth() - (int)d00.getWidth(), jc.getHeight());
        SwingUtilities.convertPointToScreen(p00, jc.getParent());
        dialog.setLocation(p00);
        
        Component glass = dialog.getGlassPane();
        glass.addKeyListener(kl);
        glass.setVisible(true);
        glass.requestFocusInWindow();
        
        dialog.setVisible(true);
    }
    
    
    @Override
    public void stopModalKeyCatch()
    {
        if ( dialog == null ) {
            throw new IllegalStateException("Not in modal state");
        }
        dialog.setVisible(false);
        dialog.dispose();
        dialog = null;
    }
    
    private Timer timer;
    
    @Override
    public void startTimeout(int timeoutlen, ActionListener l)
    {
        if(timer == null)
            timer = new Timer(0, null);
        timer.setInitialDelay(timeoutlen);
        timer.addActionListener(l);
        timer.setRepeats(false);
        timer.start();
    }
    
    @Override
    public void stopTimeout(ActionListener l)
    {
        if(timer == null)
            return;
        timer.stop();
        timer.removeActionListener(l);
    }
    
    
    /**
     * Register editor pane for use with vi. Install a
     * vi cursor. This is a nop
     * if already registered.
     */
    @Override
    @SuppressWarnings("null")
    public void setupCaret( Component editor)
    {
        JTextComponent ed = (JTextComponent)editor;
        // install cursor if neeeded
        Caret c = ed.getCaret();
        if ( !(c instanceof ViCaret) ) {
            SwingCaret caret = new SwingCaret();
            ed.setCaret(caret);
            caret.setDot(c.getDot());
            int n = Options.getOption(Options.caretBlinkRate).getInteger();
            caret.setBlinkRate(n);
            caret.setVisible(c.isVisible());
        }
    }
    
    
    @Override
    public PropertyDescriptor createPropertyDescriptor(
            String optName,
            String methodName,
            Class<?> clazz )
            throws IntrospectionException
    {
        return OptionsBeanBase.createPropertyDescriptor(
                optName, methodName, clazz);
    }
    
    
    @Override
    public ViCmdEntry createCmdEntry( ViCmdEntry.Type type )
    {
        Object opt = ViManager.getOptionAtStartup(Options.commandEntryFrame);
        G.dbgOptions().println("createCmdEntry: modality: %s", opt);
        if (Options.CEF_GLASS_PANE.equals(opt)) {
            return new GlassCmdEntry(type);
        } else {
            return new WindowCmdEntry(type);
        }
    }

    // TODO: get rid of this, incorporate into createCmdEntry, builder pattern DOC
    public Document createCmdEntryDoc()
    {
        return new PlainDocument();
    }
    
    
    /*
    public Keymap getInsertModeKeymap()
    {
    return KeyBinding.insertModeKeymap;
    }
    
    public Keymap getNormalModeKeymap()
    {
    return KeyBinding.normalModeKeymap;
    }
    */
    
    
    @Override
    public Action createInsertModeKeyAction(String name, char vkey, String desc)
    {
        return new InsertModeAction(name, vkey, desc);
    }
    
    @Override
    public Action createNormalModeKeyAction(String name, int vkey, String desc)
    {
        return null;
    }
    
    @Override
    public ActionListener xlateKeymapAction( ActionListener act )
    {
        return act;
    }
    
    @Override
    public Preferences getPreferences()
    {
        return Preferences.userRoot().node(ViManager.PREFS_ROOT);
        //return Preferences.userNodeForPackage(Options.class);
    }
    
    
    /**
     * This is the default key action.
     * Ignore all Ctrl characters (which includes that troublesome Ctrl-space).
     * Control characters of interest are picked up as key-press events
     * in {link #EnqKeyAction}.
     */
    @SuppressWarnings({"serial", "CloneableImplementsClone"})
    public static class EnqueCharAction extends TextAction
    {
        public EnqueCharAction(String name) {
            super(name);
        }
        
        
        @Override
        public void actionPerformed( ActionEvent e )
        {
            JTextComponent target = getTextComponent(e);
            if ( target != null && e != null ) {
                String content = e.getActionCommand();
                if ( content != null && content.length() > 0 ) {
                    char c = content.charAt(0);
                    // just get out if a ' ', now KeyAction, not CharAction
                    if(c == ' ')
                        return;
                    // Check whether the modifiers are OK
                    int mod = e.getModifiers();
                    boolean ctrl = ((mod & ActionEvent.CTRL_MASK) != 0);
                    // On the mac, norwegian and french keyboards use Alt to do
                    // bracket characters. This replicates Apple's modification
                    // DefaultEditorKit.DefaultKeyTypedAction
                    boolean alt = IS_MAC
                            ? ((mod & ActionEvent.META_MASK) != 0)
                            : ((mod & ActionEvent.ALT_MASK) != 0);
                    
                    boolean keep = true;
                    if ( alt || ctrl
                            || content.length() != 1
                            || c <= 0x20
                            || c == 0x7f ) {
                        // the delete key comes in as a virtual key.
                        // the space key comes in as a virtual key. (2014)
                        // Wouldn't have thought that the 'c<0x20' was needed,
                        // <RETURN>,<BS> come in < 0x20 without the Control key
                        keep = false;
                    }
                    
                    if (c >= 0x20 ) {
                        boolean fkeep = keep;
                        Options.kd().println(() -> "CharAction: "
                                + (fkeep ? "" : "REJECT: ")
                                + "'" + content + "' "
                                + String.format("%x", (int)c)
                                + "(" + (int)c + ") " + mod);
                    }
                    if ( keep ) {
                        Scheduler.keyStroke(target, c, mod, KeyStrokeType.CHAR);
                    }
                } else {
                    Options.kd().println(() -> "CharAction: " + e);
                }
            }
        }
        
        @Override
        public String toString() {
            return "jVi[DKTA]";
        }
        
    } // end inner class EnqueCharAction
    
    
    /**
     * Catch non-printing keys with this class. The constructor
     * specifies which key. The modifiers are handled by examining
     * the event and added to the key. Recieved characters are placed
     * on the vi input Q.
     */
    @SuppressWarnings({"serial", "CloneableImplementsClone"})
    public static class EnqueKeyAction extends TextAction
    {
        char basekey;
        
        public EnqueKeyAction(String name, char key) {
            super(name);
            this.basekey = key;
        }
        
        @Override
        public void actionPerformed( ActionEvent e )
        {
            JTextComponent target = getTextComponent(e);
            int mod = e.getModifiers();
            char key = basekey;
            if ( Options.kd().getBoolean() ) {
                String virt = ((key & 0xF000) == VIRT) ? "virt" : "";
                Options.kd().println("KeyAction: "
                        + getValue(Action.NAME).toString()
                        + ": " + String.format("%x", (int)key)
                        + "(" + ((int)key&~VIRT) + ") " + mod + " " + virt);
            }
            Scheduler.keyStroke(target, key, mod, KeyStrokeType.KEY);
        }
        
        @Override
        public String toString() {
            return getValue(Action.NAME).toString();
        }
    }
    
    
    @SuppressWarnings({"serial", "CloneableImplementsClone"})
    private static class InsertModeAction extends TextAction
            implements ViXlateKey
    {
        char basekey;
        
        public InsertModeAction(String name, char vkey, String desc) {
            super(name); // ??????????????????????
            this.basekey = vkey;
            
            // if name starts with Vi and ends with Key, then put out a message
            // with the name of the key in it
            //this.putValue(Action.LONG_DESCRIPTION, desc);
            //this.putValue("ActionGroup", GROUP_VI_EDIT);
            //EditorActions.addBindableEditorAction(this,
            //                                      JBViKeymap.VI_EDIT_KEYMAP);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // NOT USED for the translation keymap
        }
        
        @Override
        public char getXlateKey() {
            return basekey;
        }
        
    } // end inner class InsertModeAction
    
    
    @Override
    public void startTagPush( ViTextView tv, String ident ) {}
    
    @Override
    public void tagStack( TAGOP op, int count ) {}
    
    @Override
    public void displayTags() {}
    
    @Override
    public void tagDialog( ColonEvent e ) {}
    
    @Override
    public void commandEntryAssist(ViCmdEntry cmdEntry, boolean enable ) {}
    
    @Override
    public Component getViewport(Component c) {
        if(c instanceof JViewport)
            return c;
        Component t = SwingUtilities.getAncestorOfClass(JViewport.class, c);
        return t != null ? t : null;
    }

    static List<Entry<String,Color>> getUIColors()
    {
        List<Entry<String,Color>> rval = new ArrayList<>();
        
        Set<Entry<Object, Object>> entries = UIManager.getDefaults().entrySet();
        for (Entry<Object, Object> entry : entries)
        {
            if (entry.getValue() instanceof Color)
            {
                rval.add(new AbstractMap.SimpleEntry<>(
                         (String)entry.getKey(), (Color)entry.getValue()));
            }
        }
        return rval;
    }
    
    private static class DumpUIColors implements ActionListener {

        @Override
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        public void actionPerformed(ActionEvent e)
        {
            List<Entry<String, Color>> uiColors = getUIColors();
            
            // sort the color keys
            uiColors.sort((e1, e2) -> e1.getKey().compareTo(e2.getKey()));

            // print the "name color" for all UI
            System.err.println("LookAndFeelName: "
                    + UIManager.getLookAndFeel().getName());
            System.err.println("LookAndFeelID: "
                    + UIManager.getLookAndFeel().getID());
            for (Entry<String, Color> entry : uiColors)
            {
                System.err.printf("%-50s %08x\n",
                                  entry.getKey(), entry.getValue().getRGB());
            }
        }
    }
    
} // end com.raelity.jvi.swing.DefaultViFactory
