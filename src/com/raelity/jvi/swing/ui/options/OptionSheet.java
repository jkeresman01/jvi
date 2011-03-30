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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.swing.ui.options;

import com.l2fprod.common.beans.ExtendedPropertyDescriptor;
import com.l2fprod.common.beans.editor.ComboBoxPropertyEditor;
import com.l2fprod.common.propertysheet.AbstractProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.swing.LookAndFeelTweaks;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.options.EnumOption;
import com.raelity.jvi.options.Option;
import com.raelity.text.XMLUtil;
import java.awt.Color;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
class OptionSheet extends JPanel {
    // NOTE: bean/beanInfo are same class
    final BeanInfo bean; // keep a reference,
    PropertySheetPanel sheet;
    final OptionsPanel optionPanel;

    OptionSheet(BeanInfo _bean, OptionsPanel _optionPanel)
    {
        this.optionPanel = _optionPanel;
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
        ((PropertySheetTableModel)sheet.getTable().getModel()).setCategorySortingComparator(OptionsPanel.reverseStringCompare);
        // compare properties by property name rather than display name
        ((PropertySheetTableModel)sheet.getTable().getModel()).setPropertySortingComparator(OptionsPanel.propertyNameCompare);
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

            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                Property prop = (Property)evt.getSource();
                boolean change = false;
                try {
                    prop.writeToObject(bean);
                    change = true;
                } catch(RuntimeException ex) {
                    if(!(ex.getCause() instanceof PropertyVetoException)) {
                        throw ex;
                    }
                    JOptionPane.showMessageDialog(null,
                                                  ex.getCause().getMessage(),
                                                  "jVi Option Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    prop.setValue(Options.getOption(prop.getName()).getString());
                }
                if(change && optionPanel.changeNotify != null) {
                    optionPanel.changeNotify.change();
                }
            }
        };
        sheet.addPropertySheetChangeListener(pcl);
    }

    void load()
    {
        sheet.readFromObject(bean);
    }

    private void setupSheetAndBeanProperties(PropertySheetPanel sheet,
                                             BeanInfo bean)
    {
        sheet.setEditorFactory(optionPanel.propertyEditors);
        PropertyDescriptor[] descriptors = bean.getPropertyDescriptors();
        // count the hidden properties
        int nHidden = 0;
        for(int i = 0, c = descriptors.length; i < c; i++) {
            if(descriptors[i].isHidden()) {
                nHidden++;
            }
        }
        // exclude the hidden properties
        Property[] properties = new Property[descriptors.length - nHidden];
        StringBuffer sb = new StringBuffer();
        XMLUtil xmlFix =
                new XMLUtil(OptionsPanel.IN_RANGE_INVALID_CR,
                            OptionsPanel.IN_RANGE_VALID_CR);
        for(int i = 0, i2 = 0, c = descriptors.length; i < c; i++) {
            if(!descriptors[i].isHidden()) {
                // xmlify the description
                PropertyDescriptor d = descriptors[i];
                Option opt = Options.getOption(d.getName());
                String s = d.getShortDescription();
                sb.setLength(0);
                xmlFix.utf2xml(s, sb);
                if(opt != null) {
                    sb.append("<br/><br/>Default: '<b>").
                            append(opt.getDefault()).append("</b>'");
                }
                s = sb.toString();
                d.setShortDescription(s);
                Property prop = new MyPropAdapt(d);
                // wish PropertyDescriptor.createPropertyEditor was used
                if(prop.getType().equals(Color.class)) {
                    ((PropertyEditorRegistry)optionPanel.propertyEditors).registerEditor(prop,
                                                                                         new OptionsPanel.ColorPropertyEditor(prop));
                }
                if(opt instanceof EnumOption) {
                    ComboBoxPropertyEditor pe = new ComboBoxPropertyEditor();
                    pe.setAvailableValues(((EnumOption)opt).getAvailableValues());
                    ((PropertyEditorRegistry)optionPanel.propertyEditors).registerEditor(prop,
                                                                                         pe);
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

        public MyPropAdapt(PropertyDescriptor descriptor)
        {
            super(descriptor);
        }

        @Override
        public String getCategory()
        {
            return descriptor.isExpert() ? "Expert" : "Properties";
        }
    }

    //
    // L2FProd's PropertyDescriptorAdapter is not public, so copy it here
    // and change descriptor field protected
    //
    class PropertyDescriptorAdapter extends AbstractProperty {

        protected PropertyDescriptor descriptor;

        public PropertyDescriptorAdapter(PropertyDescriptor descriptor)
        {
            setDescriptor(descriptor);
        }

        private void setDescriptor(PropertyDescriptor descriptor)
        {
            this.descriptor = descriptor;
        }

        public PropertyDescriptor getDescriptor()
        {
            return descriptor;
        }

        @Override
        public String getName()
        {
            return descriptor.getName();
        }

        @Override
        public String getDisplayName()
        {
            return descriptor.getDisplayName();
        }

        @Override
        public String getShortDescription()
        {
            return descriptor.getShortDescription();
        }

        @Override
        public Class getType()
        {
            return descriptor.getPropertyType();
        }

        @Override
        public Object clone()
        {
            PropertyDescriptorAdapter clone =
                    new PropertyDescriptorAdapter(descriptor);
            clone.setValue(getValue());
            return clone;
        }

        @Override
        public void readFromObject(Object object)
        {
            try {
                Method method = descriptor.getReadMethod();
                if(method != null) {
                    setValue(method.invoke(object, (Object[])null));
                }
            } catch(Exception e) {
                String message =
                        "Got exception when reading property " + getName();
                if(object == null) {
                    message += ", object was 'null'";
                } else {
                    message += ", object was " + String.valueOf(object);
                }
                throw new RuntimeException(message, e);
            }
        }

        @Override
        public void writeToObject(Object object)
        {
            try {
                Method method = descriptor.getWriteMethod();
                if(method != null) {
                    method.invoke(object, new Object[]{getValue()});
                }
            } catch(Exception e) {
                // let PropertyVetoException go to the upper level without logging
                if(e instanceof InvocationTargetException &&
                        ((InvocationTargetException)e).getTargetException() instanceof PropertyVetoException) {
                    throw new RuntimeException(((InvocationTargetException)e).getTargetException());
                }
                String message =
                        "Got exception when writing property " + getName();
                if(object == null) {
                    message += ", object was 'null'";
                } else {
                    message += ", object was " + String.valueOf(object);
                }
                throw new RuntimeException(message, e);
            }
        }

        @Override
        public boolean isEditable()
        {
            return descriptor.getWriteMethod() != null;
        }

        @Override
        public String getCategory()
        {
            if(descriptor instanceof ExtendedPropertyDescriptor) {
                return ((ExtendedPropertyDescriptor)descriptor).getCategory();
            } else {
                return null;
            }
        }
    }
    
}
