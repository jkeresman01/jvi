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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.raelity.jvi.swing;

import com.raelity.jvi.KeypadBindingBean;
import com.raelity.jvi.KeyBindingBean;
import com.l2fprod.common.beans.ExtendedPropertyDescriptor;
import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.ComboBoxPropertyEditor;
import com.l2fprod.common.propertysheet.AbstractProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorFactory;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.NaturalOrderStringComparator;
import com.l2fprod.common.swing.ComponentFactory;
import com.l2fprod.common.swing.LookAndFeelTweaks;
import com.l2fprod.common.swing.PercentLayout;
import com.l2fprod.common.swing.renderer.ColorCellRenderer;
import com.raelity.jvi.EnumOption;
import com.raelity.jvi.Option;
import com.raelity.jvi.Option.ColorOption;
import com.raelity.jvi.Options;
import com.raelity.jvi.Options.EditOptionsControl;
import com.raelity.jvi.OptionsBean;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

/**
 * The module requires com.l2fprod.common...
 * see http://common.l2fprod.com/ Only the jar l2fprod-common-sheet.jar is
 * needed.
 * 
 * NOTE: this file can simply be excluded from compilation and everything
 * will work fine since it is invoked through reflection.
 * 
 * @author erra
 */
public class OptionsPanel extends JPanel {
    private ChangeNotify changeNotify;
    private List<OptionSheet> optionSheets
            = new ArrayList<OptionSheet>();

    public OptionsPanel() {
        this(null);
    }

    public OptionsPanel(ChangeNotify changeNotify) {
        this.changeNotify = changeNotify;
        setLayout(new BorderLayout());
        add(getTabbedOptions(), BorderLayout.CENTER);
    }

    public void load()
    {
        // read property values from backing store
        // and prepare for a new property edit op
        for (OptionSheet optionSheet : optionSheets) {
            optionSheet.load();
            ((EditOptionsControl)optionSheet.bean).clear();
        }
    }

    public void cancel()
    {
        // back out the changes
        for (OptionSheet optionSheet : optionSheets) {
            ((EditOptionsControl)optionSheet.bean).cancel();
        }
    }

    public boolean valid() {
        return true;
    }

    public static interface ChangeNotify {
        public void change();
    }

    private PropertyEditorFactory propertyEditors;

    // NEEDSWORK: 3rd param "OptionsControl with init, cancel methods"
    private void addTab(JTabbedPane tabs, String name, BeanInfo bi) {
        OptionSheet optionSheet = new OptionSheet(bi);
        optionSheets.add(optionSheet);
        tabs.add(name, optionSheet);
    }

    public BeanInfo createDebugBean() {
        return new OptionsBean.Debug();
    }

    private JComponent getTabbedOptions() {
        createPropertyEditors();
        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabPlacement(JTabbedPane.LEFT);
        addTab(tabs, "Platform", new OptionsBean.Platform());
        addTab(tabs, "General", new OptionsBean.General());
        addTab(tabs, "Search", new OptionsBean.Search());
        addTab(tabs, "Buffer Modifications", new OptionsBean.Modify());
        addTab(tabs, "Cursor Line Wrap", new OptionsBean.CursorWrap());
        addTab(tabs, "External Process", new OptionsBean.ExternalProcess());
        addTab(tabs, "Ctrl-Key Bindings", new KeyBindingBean());
        addTab(tabs, "KeyPad Bindings", new KeypadBindingBean());
        addTab(tabs, "Debug", createDebugBean());

        // lay things out to get sizes so we can adjust splitter
        //tabs.validate();

        //
        // Adjust the splitter to give more room
        // to the property description
        //
        for(int i = tabs.getComponentCount() -1; i >= 0; i--) {
            Component c = tabs.getComponentAt(i);
            if(!(c instanceof OptionSheet))
                continue;
            PropertySheetPanel sheet = ((OptionSheet) c).sheet;
            JSplitPane sp = null;
            for(int j = sheet.getComponentCount() -1; j > 0; j--) {
                if(sheet.getComponent(j) instanceof JSplitPane) {
                    sp = (JSplitPane) sheet.getComponent(j);
                    break;
                }
            }
            if(sp != null) {
                //int h = sp.getHeight();
                int h = sp.getPreferredSize().height;
                //int loc = sp.getDividerLocation();
                int newLoc = h - 120;
                sp.setDividerLocation(newLoc);
                double d = sp.getResizeWeight();
                sp.setResizeWeight(.65);
                //sp.invalidate();

                // There is layout problem with long lines in the description.
                // it tries to widen the property sheet to contain the
                // description in a single line. Give it a small prefered size
                // and the other sizing algorithms work better.
                Component comp = sp.getBottomComponent();
                if(comp != null) {
                    comp.setPreferredSize(new Dimension(100,100));
                }
                if(false && comp instanceof JScrollPane) {
                    comp = ((JScrollPane)comp).getViewport().getView();
                    if(comp instanceof JEditorPane) 
                        LookAndFeelTweaks.makeMultilineLabel((JEditorPane)comp);
                }
                if(comp != null)
                    comp.invalidate();
            }
            // Need to set false, so the setDividerLocation is remembered
            sheet.setDescriptionVisible(false);
            sheet.setDescriptionVisible(true);
        }
        //propertyEditors = null;
        return tabs;
    }

    private class OptionSheet extends JPanel {
        // NOTE: bean/beanInfo are same class
        final BeanInfo bean; // keep a reference,
        PropertySheetPanel sheet;
        OptionSheet(BeanInfo _bean) {
            this.bean = _bean;

            BeanDescriptor bdesc = bean.getBeanDescriptor();
            //bdesc.setShortDescription("A desc xxx");
            String descr = bdesc.getShortDescription();
            if(descr != null) {
                JTextArea message = new JTextArea();
                message.setText(descr);
                LookAndFeelTweaks.makeMultilineLabel(message);
                add(message);
            }

            setLayout(LookAndFeelTweaks.createVerticalPercentLayout());
            
            sheet = new PropertySheetPanel();

            // Convert Properties to L2F properties so categories display
            setupSheetAndBeanProperties(sheet, bean);
            sheet.readFromObject(bean);

            // compare reverse order so that Prop is before Expert
            ((PropertySheetTableModel)sheet.getTable().getModel())
                    .setCategorySortingComparator(reverseStringCompare);

            // compare properties by property name rather than display name
            ((PropertySheetTableModel)sheet.getTable().getModel())
                    .setPropertySortingComparator(propertyNameCompare);

            sheet.setMode(PropertySheet.VIEW_AS_CATEGORIES);
            sheet.setDescriptionVisible(true);
            sheet.setSortingCategories(true);
            sheet.setSortingProperties(true);
            sheet.setRestoreToggleStates(false);
            sheet.setToolBarVisible(false);
            add(sheet, "*");
            
            // everytime a property change, update the sheet with it
            //new BeanBinder(data, sheet);
            
            // initialize the properties with the value from the object
            // one can use sheet.readFromObject(button)
            // but I encountered some issues with Java Web Start. The method
            // getLocationOnScreen on the button is throwing an exception, it
            // does not happen when not using Web Start. Load properties one
            // by one as follow will do the trick
            /*
            Property[] properties = sheet.getProperties();
            for (int i = 0, c = properties.length; i < c; i++) {
                try {
                    properties[i].readFromObject(bean);
                } catch (Exception e) {
                }
            }
            */
            
            // everytime a property change, update the bean
            // (which will update the Preference which updates the option)
            PropertyChangeListener pcl;
            pcl = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    Property prop = (Property)evt.getSource();
                    boolean change = false;
                    try {
                        prop.writeToObject(bean);
                        change = true;
                    } catch(RuntimeException ex) {
                        if(!(ex.getCause() instanceof PropertyVetoException))
                            throw ex;
                        JOptionPane.showMessageDialog(null /*dialog*/,
                                ex.getCause().getMessage(),
                                "jVi Option Error",
                                JOptionPane.ERROR_MESSAGE);
                        prop.setValue(Options.getOption(prop.getName())
                                                            .getString());
                    }
                    if(change && changeNotify != null) {
                        changeNotify.change();
                    }
                }
            };
            sheet.addPropertySheetChangeListener(pcl);
        }

        private void load() {
            sheet.readFromObject(bean);
        }

        private void setupSheetAndBeanProperties(PropertySheetPanel sheet,
                                                 BeanInfo bean) {
            sheet.setEditorFactory(propertyEditors);

            PropertyDescriptor[] descriptors = bean.getPropertyDescriptors();

            // count the hidden properties
            int nHidden = 0;
            for (int i = 0, c = descriptors.length; i < c; i++) {
                if(descriptors[i].isHidden())
                    nHidden++;
            }

            // exclude the hidden properties
            Property[] properties = new Property[descriptors.length - nHidden];
            StringBuffer sb = new StringBuffer();
            for (int i = 0, i2 = 0, c = descriptors.length; i < c; i++) {
                if(!descriptors[i].isHidden()) {
                    // xmlify the description
                    PropertyDescriptor d = descriptors[i];
                    String s = d.getShortDescription();
                    sb.setLength(0);
                    XMLUtil.getInstance().utf2xml(s,
                                                  sb,
                                                  IN_RANGE_INVALID_CR,
                                                  IN_RANGE_VALID_CR);
                    s = sb.toString();
                    d.setShortDescription(s);
                    Property prop = new MyPropAdapt(d);
                    // wish PropertyDescriptor.createPropertyEditor was used
                    if(prop.getType().equals(Color.class)) {
                        ((PropertyEditorRegistry)propertyEditors)
                                .registerEditor(prop,
                                                new ColorPropertyEditor(prop));
                    }
                    Option opt = Options.getOption(prop.getName());
                    if(opt instanceof EnumOption) {
                        ComboBoxPropertyEditor pe = new ComboBoxPropertyEditor();
                        pe.setAvailableValues(
                                ((EnumOption)opt).getAvailableValues());
                        ((PropertyEditorRegistry)propertyEditors)
                                .registerEditor(prop, pe);
                    }
                    properties[i2++] = prop;
                }
            }
            sheet.setProperties(properties);
        }

        /**
         * Use our own copy of the L2F property
         * so we can control the property's category.
         */
        class MyPropAdapt extends PropertyDescriptorAdapter {
            public MyPropAdapt(PropertyDescriptor descriptor) {
                super(descriptor);
            }

            @Override
            public String getCategory() {
                return descriptor.isExpert() ? "Expert" : "Properties";
            }
        }

        //
        // L2FProd's PropertyDescriptorAdapter is not public, so copy it here
        // and change descriptor field protected
        //
        class PropertyDescriptorAdapter extends AbstractProperty {
            
            protected PropertyDescriptor descriptor;
            
            public PropertyDescriptorAdapter() {
                super();
            }
            
            public PropertyDescriptorAdapter(PropertyDescriptor descriptor) {
                this();
                setDescriptor(descriptor);
            }
            
            public void setDescriptor(PropertyDescriptor descriptor) {
                this.descriptor = descriptor;
            }
            
            public PropertyDescriptor getDescriptor() {
                return descriptor;
            }
            
            public String getName() {
                return descriptor.getName();
            }
            
            public String getDisplayName() {
                return descriptor.getDisplayName();
            }
            
            public String getShortDescription() {
                return descriptor.getShortDescription();
            }
            
            public Class getType() {
                return descriptor.getPropertyType();
            }
            
            @Override
            public Object clone() {
                PropertyDescriptorAdapter clone = new PropertyDescriptorAdapter(descriptor);
                clone.setValue(getValue());
                return clone;
            }
            
            public void readFromObject(Object object) {
                try {
                    Method method = descriptor.getReadMethod();
                    if (method != null) {
                        setValue(method.invoke(object, (Object[])null));
                    }
                } catch (Exception e) {
                    String message = "Got exception when reading property " + getName();
                    if (object == null) {
                        message += ", object was 'null'";
                    } else {
                        message += ", object was " + String.valueOf(object);
                    }
                    throw new RuntimeException(message, e);
                }
            }
            
            public void writeToObject(Object object) {
                try {
                    Method method = descriptor.getWriteMethod();
                    if (method != null) {
                        method.invoke(object, new Object[]{getValue()});
                    }
                } catch (Exception e) {
                    // let PropertyVetoException go to the upper level without logging
                    if (e instanceof InvocationTargetException &&
                            ((InvocationTargetException)e).getTargetException() instanceof PropertyVetoException) {
                        throw new RuntimeException(((InvocationTargetException)e).getTargetException());
                    }
                    
                    String message = "Got exception when writing property " + getName();
                    if (object == null) {
                        message += ", object was 'null'";
                    } else {
                        message += ", object was " + String.valueOf(object);
                    }
                    throw new RuntimeException(message, e);
                }
            }
            
            public boolean isEditable() {
                return descriptor.getWriteMethod() != null;
            }
            
            public String getCategory() {
                if (descriptor instanceof ExtendedPropertyDescriptor) {
                    return ((ExtendedPropertyDescriptor)descriptor).getCategory();
                } else {
                    return null;
                }
            }
            
        }
    }
    
    private void createPropertyEditors() {
        if(propertyEditors == null) {
            PropertyEditorRegistry pe = new PropertyEditorRegistry();
            // Add our custom editors
            pe.registerEditor(boolean.class,
                              MyBooleanAsCheckBoxPropertyEditor.class);
            pe.registerEditor(Color.class,
                              ColorPropertyEditor.class);
            propertyEditors = pe;
        }
    }

    public static class ColorPropertyEditor
    extends AbstractPropertyEditor {
        public static final int SHOW_NULL = 0x01;
        public static final int SHOW_DFLT = 0x02;
        
        private ColorCellRenderer label;
        private JButton button;
        private Color color;
        private Property property;
        
        public ColorPropertyEditor() {
            this(SHOW_NULL, null);
        }
        
        public ColorPropertyEditor(Property property) {
            this(SHOW_DFLT | 
                 (((ColorOption)Options.getOption(property.getName()))
                        .isPermitNull() ? SHOW_NULL : 0),
                 property);
        }

        /**
         * @param flags of SHOW_NULL, SHOW_DFLT controls extra editing buttons
         * @param property used to determine default value
         */
        public ColorPropertyEditor(int flags, Property property) {
            this.property = property;

            editor = new JPanel(new PercentLayout(PercentLayout.HORIZONTAL, 0));
            ((JPanel)editor).add("*", label = new ColorCellRenderer());
            label.setOpaque(false);

            ((JPanel)editor).add(button = ComponentFactory.Helper.getFactory()
                    .createMiniButton());
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectColor();
                }
            });

            if((flags & SHOW_NULL) != 0) {
                ((JPanel)editor).add(button = ComponentFactory.Helper
                        .getFactory().createMiniButton());
                button.setText("X");
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        selectNull();
                    }
                });
            }

            if((flags & SHOW_DFLT) != 0 && property != null) {
                ((JPanel)editor).add(button = ComponentFactory.Helper.getFactory()
                        .createMiniButton());
                button.setText("DFLT");
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        selectDefault();
                    }
                });
            }

            ((JPanel)editor).setOpaque(false);
        }
        
        @Override
        public Object getValue() {
            return color;
        }
        
        @Override
        public void setValue(Object value) {
            color = (Color)value;
            label.setValue(color);
        }
        
        protected void selectColor() {
            // ResourceManager rm = ResourceManager.all(FilePropertyEditor.class);
            // String title = rm.getString("ColorPropertyEditor.title");
            String title = "Color section";
            Color selectedColor = JColorChooser.showDialog(editor, title, color);
            
            if (selectedColor != null) {
                Color oldColor = color;
                Color newColor = selectedColor;
                label.setValue(newColor);
                color = newColor;
                firePropertyChange(oldColor, newColor);
            }
        }

        protected void selectDefault() {
            Color oldColor = color;
            ColorOption opt = (ColorOption)Options.getOption(
                    property.getName());
            Color newColor = opt.decode(opt.getDefault());
            label.setValue(newColor);
            color = newColor;
            firePropertyChange(oldColor, newColor);
        }
        
        protected void selectNull() {
            Color oldColor = color;
            Color newColor = null;
            if(property != null) {
                ColorOption opt = (ColorOption)Options.getOption(
                        property.getName());
                newColor = opt.decode("");
            }
            label.setValue(newColor);
            color = newColor;
            firePropertyChange(oldColor, newColor);
        }
    }

    public static class MyBooleanAsCheckBoxPropertyEditor
    extends AbstractPropertyEditor {
        
        public MyBooleanAsCheckBoxPropertyEditor() {
            editor = new JCheckBox();
            ((JCheckBox)editor).setOpaque(false);
            ((JCheckBox)editor).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    firePropertyChange(
                            ((JCheckBox)editor).isSelected()
                                ? Boolean.FALSE : Boolean.TRUE,
                            ((JCheckBox)editor).isSelected()
                                ? Boolean.TRUE : Boolean.FALSE);
                    ((JCheckBox)editor).transferFocus();
                }
            });
        }
        
        @Override
        public Object getValue() {
            return ((JCheckBox)editor).isSelected() ? Boolean.TRUE : Boolean.FALSE;
        }
        
        @Override
        public void setValue(Object value) {
            ((JCheckBox)editor).setSelected(Boolean.TRUE.equals(value));
        }
        
    }

    //
    // Character mapping for xlate to xml.
    // NOTE: using "<br>" instead of "<br/>"
    // to avoid the "\n>" from the html rendering engine
    //
    private static final char[] IN_RANGE_INVALID_CR =
        { '<', '>', '"', /*'\'',*/ '&', '\n' };
    private static final String IN_RANGE_VALID_CR[] =
        { "&lt;", "&gt;", "&quot;", /*"&apos;",*/ "&amp;", "<br>" };
    private static final Comparator STRING_COMPARATOR =
            new NaturalOrderStringComparator();

    static Comparator reverseStringCompare = new Comparator() {
        public int compare(Object o1, Object o2) {
            return - STRING_COMPARATOR.compare(o1, o2);
        }
    };

    static Comparator propertyNameCompare = new Comparator() {
        public int compare(Object o1, Object o2) {
            if (o1 instanceof Property && o2 instanceof Property) {
                Property prop1 = (Property) o1;
                Property prop2 = (Property) o2;
                if (prop1 == null) {
                    return prop2==null?0:-1;
                } else {
                    return STRING_COMPARATOR.compare(
                            prop1.getName(), prop2.getName());
                            // prop1.getDisplayName()==null
                            //     ? null
                            //     : prop1.getDisplayName().toLowerCase(),
                            // prop2.getDisplayName() == null
                            //     ? null
                            //     : prop2.getDisplayName().toLowerCase());
                }
            } else {
                return 0;
            }
        }
    };

    /*
    * @(#)XmlUtil.java
    *
    * Copyright (C) 2001,,2003 2002 Matt Albrecht
    * groboclown@users.sourceforge.net
    * http://groboutils.sourceforge.net
    *
    *  Permission is hereby granted, free of charge, to any person obtaining a
    *  copy of this software and associated documentation files (the "Software"),
    *  to deal in the Software without restriction, including without limitation
    *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
    *  and/or sell copies of the Software, and to permit persons to whom the 
    *  Software is furnished to do so, subject to the following conditions:
    *
    *  The above copyright notice and this permission notice shall be included in 
    *  all copies or substantial portions of the Software. 
    *
    *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
    *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
    *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL 
    *  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
    *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
    *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
    *  DEALINGS IN THE SOFTWARE.
    */

    //package net.sourceforge.groboutils.util.xml.v1;


    /**
    * A Utility to aid in various XML activities.
    *
    * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
    * @since     May 21, 2001
    * @version   $Date$
    */
    static public class XMLUtil
    {
        protected static XMLUtil s_instance = new XMLUtil();
        
        // *  [2]    Char    ::=    #x9 | #xA | #xD | [#x20-#xD7FF] |
        // *                        [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        //private static final char LOWER_RANGE_1 = 0x20;
        //private static final char UPPER_RANGE_1 = 0xD7FF;
        //private static final char LOWER_RANGE_2 = 0xE000;
        //private static final char UPPER_RANGE_2 = 0xFFFD;
        private static final char LOWER_RANGE = 0x20;
        private static final char UPPER_RANGE = 0x7f;
        
        // java doesn't support this range
        // private static final char LOWER_RANGE_3 = 0x10000;
        // private static final char UPPER_RANGE_3 = 0x10FFFF;
        private static final char VALID_CHAR_1 = 0x9;
        private static final char VALID_CHAR_2 = 0xA;
        private static final char VALID_CHAR_3 = 0xD;
        
        
        private static final char[] IN_RANGE_INVALID =
            { '<', '>', '"', '\'', '&' };
        //private static final String IN_RANGE_INVALID_STR =
        //    new String( IN_RANGE_INVALID );
        private static final String IN_RANGE_VALID[] =
            { "&lt;", "&gt;", "&quot;", "&apos;", "&amp;" };
        
        protected XMLUtil()
        {
            // do nothing
        }
        
        
        public static XMLUtil getInstance()
        {
            return s_instance;
        }
        
        //------------------------------------------
        
        
        
        /**
        * Convert a standard Java String into an XML string.  It transforms
        * out-of-range characters (&lt;, &gt;, &amp;, ", ', and non-standard
        * character values) into XML formatted values.  Since it does correctly
        * escape the quote characters, this may be used for both attribute values
        * as well as standard text.
        *
        * @param javaStr the Java string to be transformed into XML text.  If
        *      the string is <tt>null</tt>, then <tt>null</tt> is returned.
        * @return the XML version of <tt>javaStr</tt>.
        * @see #utf2xml( String, StringBuffer )
        */
        public String utf2xml( String javaStr )
        {
            if (javaStr == null)
            {
                return null;
            }
            StringBuffer sb = new StringBuffer();
            utf2xml( javaStr, sb );
            return sb.toString();
        }
        
        
        /**
        * Convert a standard Java String into an XML string.  It transforms
        * out-of-range characters (&lt;, &gt;, &amp;, ", ', and non-standard
        * character values) into XML formatted values.  Since it does correctly
        * escape the quote characters, this may be used for both attribute values
        * as well as standard text.
        * <P>
        * From <a href="http://www.w3c.org/TR/2000/REC-xml-20001006">
        * the XML recommendation</a>:
        * <PRE>
        * [Definition: A parsed entity contains text, a sequence of characters,
        * which may represent markup or character data.]
        * [Definition: A character is an atomic unit of text as specified by
        * ISO/IEC 10646 [ISO/IEC 10646] (see also [ISO/IEC 10646-2000]).
        * Legal characters are tab, carriage return, line feed, and the legal
        * characters of Unicode and ISO/IEC 10646. The versions of these standards
        * cited in A.1 Normative References were current at the time this document
        * was prepared. New characters may be added to these standards by
        * amendments or new editions. Consequently, XML processors must accept
        * any character in the range specified for Char. The use of
        * "compatibility characters", as defined in section 6.8 of
        * [Unicode] (see also D21 in section 3.6 of [Unicode3]), is discouraged.]
        *
        * Character Range
        *  [2]    Char    ::=    #x9 | #xA | #xD | [#x20-#xD7FF] |
        *                        [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        *         // any Unicode character, excluding the surrogate blocks,
        *            FFFE, and FFFF. //
        *
        * The mechanism for encoding character code points into bit patterns may
        * vary from entity to entity. All XML processors must accept the UTF-8
        * and UTF-16 encodings of 10646; the mechanisms for signaling which of
        * the two is in use, or for bringing other encodings into play, are
        * discussed later, in 4.3.3 Character Encoding in Entities.
        *
        * ...
        *
        * The ampersand character (&amp;) and the left angle bracket (&lt;)
        * may appear in their literal form only when used as markup delimiters, or
        * within a comment, a processing instruction, or a CDATA section. If they
        * are needed elsewhere, they must be escaped using either numeric
        * character references or the strings "&amp;amp;" and "&amp;lt;"
        * respectively. The right angle bracket (>) may be represented using the
        * string "&amp;gt;", and must, for compatibility, be escaped using
        * "&amp;gt;" or a character reference when it appears in the string
        * "]]>" in content, when that string is not marking the end of a CDATA
        * section.
        * To allow attribute values to contain both single and double quotes, the
        * apostrophe or single-quote character (&apos;) may be represented as
        * "&amp;apos;", and the double-quote character (&quot;) as "&amp;quot;".
        * </PRE>
        *
        * @param javaStr the Java string to be transformed into XML text. If
        *      it is <tt>null</tt>, then the text "null" is appended to the
        * @param output the StringBuffer to send the transformed XML into.
        */
        public void utf2xml(String javaStr,
                            StringBuffer output) {
            utf2xml(javaStr, output, IN_RANGE_INVALID, IN_RANGE_VALID);
        }
        public void utf2xml( String javaStr,
                            StringBuffer output,
                            char[] invalid,
                            String[] valid
                            )
        {
            if (output == null)
            {
                throw new IllegalArgumentException("No null StringBuffer");
            }
            if (javaStr == null)
            {
                // original:
                // javaStr = "null";
                
                // the string "null" does not have any out-of-range characters,
                // so to optimize...
                output.append("null");
                return;
            }
            int len = javaStr.length();
            // Ensure that the output string buffer has enough space.
            // The given huristic seems to work well.
            output.ensureCapacity( output.length() + (len * 2) );
            
            // for efficiency, directly access the array.
            char buf[] = javaStr.toCharArray();
            for ( int pos = 0; pos < len; ++pos)
            {
                char c = buf[pos];
                // test for out-of-range for escaping using &#
                if (
                    // *  [2]    Char    ::=    #x9 | #xA | #xD | [#x20-#xD7FF] |
                    // *                        [#xE000-#xFFFD] | [#x10000-#x10FFFF]
                    (c < LOWER_RANGE &&
                    c != VALID_CHAR_1 && c != VALID_CHAR_2 && c != VALID_CHAR_3)
                    ||
                    (c > UPPER_RANGE)
                    )
                {
                    output.append( "&#" );
                    output.append( Integer.toString( c ) );
                    output.append( ';' );
                }
                else
                {
                    // should we escape the character with an &XXX; ?
                    boolean notfound = true;
                    for (int p2 = invalid.length; --p2 >= 0;)
                    {
                        if (invalid[p2] == c)
                        {
                            notfound = false;
                            output.append( valid[ p2 ] );
                            break;
                        }
                    }
                    if (notfound)
                    {
                        // append the character as-is
                        output.append( c );
                    }
                }
            }
        }
    }
}
