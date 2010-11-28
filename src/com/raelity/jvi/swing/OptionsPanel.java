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

import com.raelity.text.XMLUtil;
import com.raelity.jvi.options.KeypadBindingBean;
import com.raelity.jvi.options.KeyBindingBean;
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
import com.raelity.jvi.options.EnumOption;
import com.raelity.jvi.options.Option;
import com.raelity.jvi.options.ColorOption;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.Options.EditOptionsControl;
import com.raelity.jvi.options.OptionsBean;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
            XMLUtil xmlFix = new XMLUtil(IN_RANGE_INVALID_CR, IN_RANGE_VALID_CR);
            for (int i = 0, i2 = 0, c = descriptors.length; i < c; i++) {
                if(!descriptors[i].isHidden()) {
                    // xmlify the description
                    PropertyDescriptor d = descriptors[i];
                    String s = d.getShortDescription();
                    sb.setLength(0);
                    xmlFix.utf2xml(s, sb);
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
            
            private void setDescriptor(PropertyDescriptor descriptor) {
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
        { '<',    '>',    '"',      /*'\'',*/     '&',     '\n' };
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
}
