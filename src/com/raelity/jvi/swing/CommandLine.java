/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import com.raelity.jvi.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * This class presents a editable combo box UI for picking up command entry
 * data. A mode, usually a single character like "?" or ":", can be
 * set for display. This UI supports the maintenance of a history of commands.
 * This component builds a keymap through which the action events are
 * delivered. When an event is delivered, the history list is updated.
 * The command line has a label that can be set with {@link #setMode}.
 * <p>Take steps to prevent this component from taking focus
 * throught the focus manager. But ultimately need to subclass the
 * editor component to handle it.
 */
public class CommandLine extends JPanel {
    static public final int DEFAULT_HISTORY_SIZE = 50;
    JLabel modeLabel = new JLabel();
    JComboBox combo = new JComboBox();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    private String mode;
    private java.util.List<String> list;
    private int historySize;
    Border border1;
    boolean setKeymapActive;

    // This is not intended to match an actual keystroke, it is used
    // to register an action that can be used externally.
    public static final KeyStroke EXECUTE_KEY
            = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 
                                     InputEvent.SHIFT_DOWN_MASK
                                     | InputEvent.ALT_DOWN_MASK
                                     | InputEvent.META_DOWN_MASK
                                     | InputEvent.CTRL_DOWN_MASK
                                     | InputEvent.BUTTON1_DOWN_MASK
                                     | InputEvent.BUTTON2_DOWN_MASK
                                     | InputEvent.BUTTON3_DOWN_MASK);
    
    /** This is used to initialize the text of the combo box,
     * needed so that characters entered before the combo box gets focus
     * are not lost.
     */
    public void init(String s) {
        dot = mark = 0;
        if(s.length() == 0) {
            JTextComponent tc = getTextComponent();
            int len = tc.getText().length();
            if(len > 0) {
                tc.setCaretPosition(0);
                tc.moveCaretPosition(len);
                mark = 0;
                dot = len;
                //System.err.println("Selection length = " + len);
            }
        } else {
            try {
                Document doc = getTextComponent().getDocument();
                mark = dot = s.length();
                doc.remove(0,doc.getLength());
                doc.insertString(0, s, null);
            } catch (BadLocationException ex) { }
        }
    }

    int dot;
    int mark;

    /** This is used to append characters to the the combo box. It is
     * needed so that characters entered before the combo box gets focus
     * are not lost.
     * 
     * If there is a selection, then clear the selection. Do this because
     * typically typed chararacters replace the selection.
     */
    public void append(char c) {
        String s = String.valueOf(c);
        JTextComponent tc = getTextComponent();
        if(tc.getSelectionStart() != tc.getSelectionEnd()) {
            // replace the selection
            tc.setText(s);
        } else {
            int offset = tc.getDocument().getLength();
            try {
                getTextComponent().getDocument().insertString(offset, s, null);
            } catch (BadLocationException ex) { }
        }
    }

    public CommandLine() {
        // see https://substance.dev.java.net/issues/show_bug.cgi?id=285
        //combo.putClientProperty(LafWidget.COMBO_BOX_NO_AUTOCOMPLETION, true);
        combo.putClientProperty("lafwidgets.comboboxNoAutoCompletion", true);
        combo.setEditor(new BasicComboBoxEditor());
        combo.setEditable(true);
        JTextComponent text = (JTextComponent) combo.getEditor().getEditorComponent();
        text.addPropertyChangeListener("keymap", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if(setKeymapActive)
                    return;
                Object newO = evt.getNewValue();
                Object oldO = evt.getOldValue();
                if(newO != null) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            setKeymap();
                        }
                    });
                }
            }
        });

        try {
            jbInit();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        Font font = modeLabel.getFont();
        modeLabel.setFont(new Font("Monospaced",
                Font.BOLD,
                font.getSize()));
        font = combo.getFont();
        combo.setFont(new Font("Monospaced",
                font.getStyle(),
                font.getSize()));
        setHistorySize(DEFAULT_HISTORY_SIZE);
        setMode(" ");
        setComboDoneListener();
        setKeymap();
        // allow tabs to be entered into text field
        Component c = getTextComponent();
        Set<AWTKeyStroke> set = Collections.emptySet();
        c.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set);
        c.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);
        
        //getTextField().setNextFocusableComponent(null);
        //takeFocus(false);
    }
    
    void jbInit() throws Exception {
        modeLabel.setText("");
        this.setLayout(gridBagLayout1);
        this.add(modeLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        this.add(combo, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }
    
    public void setupBorder() {
        if(border1 == null) {
            border1 = BorderFactory.createLineBorder(Color.black, 2);
        }
        this.setBorder(border1);
    }
    
    /** This is used when the combo box is going to be displayed.
     * A blank line is put at the head of the list. This blank is
     * automatically removed when the user completes the action.
     */
    public void clear() {
        if(historySize == 0 || list == null) {
            getTextComponent().setText("");
            return;
        }
        list.add(0, "");
        combo.insertItemAt("", 0);
        combo.setSelectedIndex(0);
        getTextComponent().setText(""); // ??? prevent re-execute last command on <CR>
    }
    
    public void takeFocus(boolean flag) {
        if(flag) {
            combo.setEnabled(true);
            // NEEDSWORK: FOCUS: use requestFocusInWindow()
            getTextComponent().requestFocus();
        } else {
            combo.setEnabled(false);
        }
    }
    
    /** Retrieve the contents of the command line. This is typically
     * done in response to a action event.
     */
    public String getCommand() {
        return getTextComponent().getText();
    }
    
    /** This sets the mode of the command line, e.g. ":" or "?". */
    public void setMode(String newMode) {
        mode = newMode;
        modeLabel.setText(" " + mode + " ");
    }
    
    public String getMode() {
        return mode;
    }
    
    JTextComponent getTextComponent() {
        Component c = combo.getEditor().getEditorComponent();
        return (JTextComponent)c;
    }
    
    /** This installs the history list. This allows multiple history
     * lists to share the same component.
     */
    public void setList(java.util.List<String> newList) {
        list = newList;
        combo.removeAllItems();
        if(list == null) {
            return;
        }
        Iterator iter = newList.iterator();
        while(iter.hasNext()) {
            combo.addItem(iter.next());
        }
    }
    
    /** retrieve the history list. */
    public java.util.List getList() {
        return list;
    }
    
    /**
     * Make the argument command the top of the list.
     * If is already in the list then first remove it.
     */
    void makeTop(String command) {
        if(historySize == 0 || list == null) {
            return;
        }
        // remove the empty-blank element
        int i = list.indexOf("");
        if(i >= 0) {  // it really must be in the list
            list.remove(i);
            combo.removeItemAt(i);
        }
        // if the empty-blank string was selected we're done
        if(command.equals("")) {
            return;
        }
        // now move the selected string to the top of the list
        i = list.indexOf(command);
        if(i == 0) {
            return;  // already on the top
        }
        if(i > 0) {  // if its already in the list, remove it.
            list.remove(i);
            combo.removeItemAt(i);
        }
        list.add(0, command);
        combo.insertItemAt(command, 0);
        combo.setSelectedIndex(0);
        trimList();
    }
    
    /** Use this to limit the size of the history list */
    public void setHistorySize(int newHistorySize) {
        if(newHistorySize < 0) {
            throw new IllegalArgumentException();
        }
        historySize = newHistorySize;
    }
    
    /** the max size of the history list */
    public int getHistorySize() {
        return historySize;
    }
    
    private void trimList() {
        if(list == null) {
            return;
        }
        while(list.size() > historySize) {
            combo.removeItemAt(list.size() - 1);
            list.remove(list.size() - 1);
        }
    }
    
    /** Bounce the event, modified, to this class's user. */
    private class SimpleEvent extends TextAction {
        SimpleEvent(String name) {
            super(name);
        }
        public void actionPerformed(ActionEvent e) {
            ActionEvent e01 = new ActionEvent(CommandLine.this, e.getID(),
                    e.getActionCommand(), e.getModifiers());
            fireActionPerformed(e01);
        }
    };
    
    protected Action createSimpleEvent(String name) {
        return new SimpleEvent(name);
    }

    private void setComboDoneListener() {
        combo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(e.getActionCommand().equals("comboBoxEdited")) {
                    ActionEvent e01 = new ActionEvent(CommandLine.this,
                            e.getID(), "\n", e.getModifiers());
                    fireActionPerformed(e01);
                }
            }
        });

    }
    
    /**
     * Return and <ESC> fire events and update the history list.
     */
    private void setKeymap() {
        if(setKeymapActive)
            return;
        setKeymapActive = true;
        Keymap keymap = JTextComponent.addKeymap("CommandLine",
                getTextComponent().getKeymap());
        JTextComponent.loadKeymap(keymap, getBindings(), getActions());
        getTextComponent().setKeymap(keymap);
        setKeymapActive = false;
    }
    
    protected JTextComponent.KeyBinding[] getBindings() {
        JTextComponent.KeyBinding[] bindings = {
            new JTextComponent.KeyBinding(EXECUTE_KEY,
                    "vi-command-execute"),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    KeyEvent.VK_ESCAPE, 0),
                    "vi-command-escape"),
            new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                    '\t'),
                    "vi-command-tab"),
        };
        return bindings;
    }
    
    protected Action[] getActions() {
        Action[] localActions = null;
        try {
            localActions = new Action[] {
                createSimpleEvent("vi-command-execute"),
                createSimpleEvent("vi-command-escape"),
                new TextAction("vi-command-tab") {
                    public void actionPerformed(ActionEvent e) {
                        ((JTextField)e.getSource()).replaceSelection("\t");
                    }
                }
            };
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return localActions;
    }
    
    /**
     * Take the argument event and create an action event copy with
     * this as its source. Then deliver it as needed.
     * Do some maintenance on the LRU history.
     */
    protected void fireActionPerformed(ActionEvent e) {
        String command = getTextComponent().getText();
        
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
        
        // Maintain the LRU history, do this after the notifying completion
        // to avoid document events relating to the following actions
        makeTop(command);
        combo.hidePopup();
    }
    
    /**
     * Adds the specified action listener to receive
     * action events from this textfield.
     *
     * @param l the action listener
     */
    public synchronized void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }
    
    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this textfield.
     *
     * @param l the action listener
     */
    public synchronized void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * The CommandLine JPanel may be embedded in a Dialog or put on a glass
     * pane; CommandLineEntry is some common handling particularly for
     * the ViCmdEntry interface.
     */
    public static abstract class CommandLineEntry 
    implements ViCmdEntry, ActionListener {
        /** result of last entry */
        protected String lastCommand;
        /** reference back to the user of this entry widget */
        protected ActionListener listener;

        protected int entryType;
        protected CommandLine commandLine;
        protected ViTextView tv;

        protected String initialText;
        
        CommandLineEntry(int type) {
            entryType=type;
            commandLine = new CommandLine();
            commandLine.setupBorder();
            commandLine.setList(new LinkedList<String>());
            commandLine.addActionListener(this);

            commandLine.setMode(entryType == ViCmdEntry.COLON_ENTRY
                    ? ":" : "/");

            if(ViManager.getOsVersion().isMac()) {
                // MAC needs a little TLC since the selection is changed
                // in JTextComponent after adding text and before focusGained
                //    init set: dot: 5 mark:5
                //    init: dot: 5 mark:5
                //    focusGained: dot: 5 mark:0
                //    focusGained set: dot: 5 mark:5
                // This class sets selection back as needed
                focusSetSelection = new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        JTextComponent tc = (JTextComponent) e.getSource();
                        tc.removeFocusListener(this);
                        Caret c = tc.getCaret();
                        tc.setCaretPosition(commandLine.mark);
                        tc.moveCaretPosition(commandLine.dot);
                    }
                };
            }
        }
        
        /** ViCmdEntry interface */
        final public void activate(String mode, ViTextView parent) {
            activate(mode, parent, "", false);
        }
        
        /**
         * Start taking input. As a side effect of invoking this,
         * the variable {@line #commandLineWindow} is set.
         */
        final public void activate(String mode, ViTextView tv,
                String initialText, boolean passThru) {
            this.tv = tv;
            this.initialText = initialText;
            commandLine.getTextComponent().removeFocusListener(focusSetSelection);
            lastCommand = "";
            if(passThru) {
                lastCommand = initialText;
                fireEvent(new ActionEvent(tv.getEditorComponent(),
                        ActionEvent.ACTION_PERFORMED,
                        "\n"));
                commandLine.makeTop(initialText);
                return;
            }
            commandLine.setMode(mode);
            commandLine.init(initialText);

            commandLine.getTextComponent().addFocusListener(focusSetSelection);

            finishActivate();
        }
        
        /**
         * When the command line gets focus, there's some work to do
         * to handle MAC bug.
         */
        private FocusListener focusSetSelection;

        public void append(char c){
            if (c == '\n') {
                fireEvent(new ActionEvent(tv.getEditorComponent(),
                        ActionEvent.ACTION_PERFORMED,
                        "\n"));
            } else
                commandLine.append(c);
        };

        protected abstract void finishActivate();

        private void shutdownEntry() {
            commandLine.getTextComponent().removeFocusListener(focusSetSelection);
            prepareShutdown();

            tv.getEditorComponent().requestFocus();
            tv = null;
        }

        protected abstract void prepareShutdown();

        /**
         * Calculate the bounds for the entry component relative to the root
         * component when placed immediately under the tv's editor.
         * @param root the root, typically a JFrame
         * @param entry the command entry widget
         * @return bounds of the widget within the root
         */
        protected Rectangle positionCommandEntry(Component root,
                                                 Component entry) {
            Container jc = SwingUtilities.getAncestorOfClass(
                    javax.swing.JScrollPane.class,
                    tv.getEditorComponent());
            if(jc == null) {
                jc = tv.getEditorComponent();
            }
            
            Dimension d00 = entry.getPreferredSize();
            Rectangle pos = jc.getBounds(); // become bounds for commmand entry
            pos.translate(0, jc.getHeight());  // just beneath component
            pos.height = d00.height;
            // trim the width
            pos.width = Math.min(500, jc.getWidth());
            // now translate bounds so relative to root pane
            Point p00 = SwingUtilities.convertPoint(jc.getParent(),
                    pos.x, pos.y, root);
            pos.setLocation(p00);
            // If past bottom of root pane, shift it up
            int offset = root.getHeight() - (pos.y + pos.height);
            if(offset < 0) {
                pos.translate(0, offset);
            }
            return pos;
        }
        
        public String getCommand(){
            return lastCommand;
        };

        public void cancel(){
            lastCommand = "";
            shutdownEntry();
        };
        
        public JTextComponent getTextComponent() {
            return commandLine.getTextComponent();
        }

        public void actionPerformed(ActionEvent e) {
            // VISUAL REPAINT HACK
            // Repaint before executing commands..
            // so that I can be sure the visual area didn't change yet
            // and all has been repainted
            if(G.drawSavedVisualBounds) {
                G.drawSavedVisualBounds = false;
                Normal.v_updateVisualState(tv);
            }
            // END VISUAL REPAINT HACK
            lastCommand = commandLine.getCommand();
            if(Options.getOption(Options.dbgKeyStrokes).getBoolean())
                System.err.println("CommandAction: '" + lastCommand + "'");
            shutdownEntry();
            fireEvent(e);
            commandLine.clear();
        }

        /** Send the event If it is a successful entry, a CR, then
         * record the input. Note that initial Text is not part of
         * the recorded input.
         */
        protected void fireEvent(ActionEvent e){
            if(e.getActionCommand().charAt(0) == '\n') {
                StringBuffer sb = new StringBuffer();
                if( ! initialText.equals("")
                        && lastCommand.startsWith(initialText)) {
                    sb.append(lastCommand.substring(initialText.length()));
                } else {
                    sb.append(lastCommand);
                }
                sb.append('\n');
                GetChar.userInput(new String(sb));
            }
            listener.actionPerformed(e);
        }
        
        public void addActionListener(ActionListener l)
                throws TooManyListenersException {
            if(listener != null) {
                throw new TooManyListenersException();
            }
            listener = l;
        }
        
        public void removeActionListener(ActionListener l) {
            if(listener == l) {
                listener = null;
            }
        }
    }




    //////////////////////////////////////////////////////////////////////
    //                                                                  //
    //                    NOT USED BELOW THIS LINE                      //
    //                                                                  //
    //////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////
    //
    // This is for a combo box that contains a JEP.

    // Taken from following where I found a bug, issue 97130 in NB.
    // debuggerjpda/ui/src/org/netbeans/modules/debugger/jpda/ui/Evaluator.java
    // NEEDSWORK: It looks like this could go into jvi.swing, don't think
    // there's anything NB specific.
    private static final class ViCommandEditor implements ComboBoxEditor {
        
        private JEditorPane editor;
        //private Component component;
        private Object oldValue;
        
        public ViCommandEditor() {
            editor = new JEditorPane("text/x-vicommand", ""); //NOI18N
            editor.setBorder(null);
            new KeymapUpdater().run();
            /*component = new JScrollPane(editor,
                                        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            editor.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    WatchPanel.setupContext(editor);
                }
                public void focusLost(FocusEvent e) {
                }
            });*/
            editor.addPropertyChangeListener("keymap",
                                             new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (!(evt.getNewValue() instanceof FilteredKeymap)) {
                        // We have to do this lazily, because the property change
                        // is fired *before* the keymap is actually changed!
                        EventQueue.invokeLater(new KeymapUpdater());
                    }
                }
            });
        }
        
        public void addActionListener(ActionListener l) {
            // NB that setEditor calls addActionListener
            //editor.addActionListener(l);
        }

        public void removeActionListener(ActionListener l) {
            //assert false;
            //editor.removeActionListener(l);
        }

        public Component getEditorComponent() {
            return editor;
        }

        public Object getItem() {
            Object newValue = editor.getText();
            
            if (oldValue != null && !(oldValue instanceof String))  {
                // The original value is not a string. Should return the value in it's
                // original type.
                if (newValue.equals(oldValue.toString()))  {
                    return oldValue;
                } else {
                    // Must take the value from the editor and get the value and cast it to the new type.
                    Class cls = oldValue.getClass();
                    try {
                        Method method = cls.getMethod("valueOf", new Class[]{String.class});
                        newValue = method.invoke(oldValue, new Object[] { editor.getText()});
                    } catch (Exception ex) {
                        // Fail silently and return the newValue (a String object)
                    }
                }
            }
            return newValue;
        }

        public void setItem(Object obj) {
            if (obj != null)  {
                editor.setText(obj.toString());
                
                oldValue = obj;
            } else {
                editor.setText("");
            }
        }
        
        public void selectAll() {
            editor.selectAll();
            editor.requestFocus();
        }
        
        private class KeymapUpdater implements Runnable {
            
            public void run() {
                editor.setKeymap(new FilteredKeymap(editor.getKeymap()));
            }
            
        }

    }

    // taken from same package as the above combo box editor
    public static class FilteredKeymap implements Keymap {
        
        private final KeyStroke enter
                = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        private final KeyStroke esc
                = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        private final KeyStroke tab
                = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
        private final Keymap keyMap; // The original keymap
        
        /** Creates a new instance of FilteredKeymap */
        public FilteredKeymap(Keymap keyMap) {
            this.keyMap = keyMap;
        }
        
        public void addActionForKeyStroke(KeyStroke key, Action a) {
            keyMap.addActionForKeyStroke(key, a);
        }
        public Action getAction(KeyStroke key) {
            if (enter.equals(key) ||
                    esc.equals(key) ||
                    tab.equals(key)) {
                
                return null;
            } else {
                return keyMap.getAction(key);
            }
        }
        public Action[] getBoundActions() {
            return keyMap.getBoundActions();
        }
        public KeyStroke[] getBoundKeyStrokes() {
            return keyMap.getBoundKeyStrokes();
        }
        public Action getDefaultAction() {
            return keyMap.getDefaultAction();
        }
        public KeyStroke[] getKeyStrokesForAction(Action a) {
            return keyMap.getKeyStrokesForAction(a);
        }
        public String getName() {
            return keyMap.getName()+"_Filtered"; //NOI18N
        }
        public javax.swing.text.Keymap getResolveParent() {
            return keyMap.getResolveParent();
        }
        public boolean isLocallyDefined(KeyStroke key) {
            if (enter.equals(key) ||
                    esc.equals(key) ||
                    tab.equals(key)) {
                
                return false;
            } else {
                return keyMap.isLocallyDefined(key);
            }
        }
        public void removeBindings() {
            keyMap.removeBindings();
        }
        public void removeKeyStrokeBinding(KeyStroke keys) {
            keyMap.removeKeyStrokeBinding(keys);
        }
        public void setDefaultAction(Action a) {
            keyMap.setDefaultAction(a);
        }
        public void setResolveParent(Keymap parent) {
            keyMap.setResolveParent(parent);
        }
    }
}
