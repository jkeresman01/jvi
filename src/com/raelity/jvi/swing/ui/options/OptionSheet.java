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

import java.awt.Color;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.util.Comparator;
import java.util.EnumSet;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.l2fprod.common.beans.editor.ComboBoxPropertyEditor;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.NaturalOrderStringComparator;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.ColorOption;
import com.raelity.jvi.options.EnumOption;
import com.raelity.jvi.options.EnumSetOption;
import com.raelity.jvi.options.Option;
import com.raelity.jvi.options.VimOption;
import com.raelity.jvi.options.*;

/**
 * @author Ernie Rael <err at raelity.com>
 */
@SuppressWarnings("serial")
class OptionSheet extends JPanel implements Options.EditControl {
    // NOTE: bean/beanInfo are same class
    final BeanInfo bean; // keep a reference,
    PropertySheetPanel sheet;
    final OptionsPanel optionsPanel;

    OptionSheet(BeanInfo _bean, OptionsPanel _optionsPanel)
    {
        createFactories();

        this.optionsPanel = _optionsPanel;
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
        pcl = (PropertyChangeEvent evt) -> {
            Property prop = (Property)evt.getSource();
            boolean change = false;
            try {
                prop.writeToObject(bean);
                change = true;
            } catch(RuntimeException ex) {
                PropertyVetoException veto = OptUtil.getVeto(ex);
                if(veto == null) {
                    throw ex;
                }

                Option<?> option = Options.getOption(prop.getName());
                JOptionPane.showMessageDialog(
                        ViManager.getFactory().getMainWindow(),
                        option.getCategory() + "\n"
                                + option.getDisplayName() + ":\n"
                                + "    " + veto.getMessage(),
                        "jVi Option Error",
                        JOptionPane.ERROR_MESSAGE);
                prop.setValue(getCurrentValue(prop.getName()));
            }
            if(change && optionsPanel.changeNotify != null) {
                optionsPanel.changeNotify.change();
            }
        };
        sheet.addPropertySheetChangeListener(pcl);
    }

    // read property values from backing store
    // and prepare for a new property edit op
    @Override
    public void start()
    {
        sheet.readFromObject(bean);
        ((Options.EditControl)bean).start();
    }

    @Override
    public void ok()
    {
        // Now's the time to persist the changes
        ((Options.EditControl)bean).ok();
    }

    // back out the changes
    @Override
    public void cancel()
    {
        ((Options.EditControl)bean).cancel();
    }

    @Override
    public Object getCurrentValue(String name)
    {
        return ((Options.EditControl)bean).getCurrentValue(name);
    }

    //
    // Character mapping for xlate to xml.
    // NOTE: using "<br>" instead of "<br/>"
    // to avoid the "\n>" from the html rendering engine
    //
    static final char[] IN_RANGE_INVALID_CR =
        { '<',    '>',    '"',      /*'\'',*/     '&',     '\n' };
    static final String IN_RANGE_VALID_CR[] =
        { "&lt;", "&gt;", "&quot;", /*"&apos;",*/ "&amp;", "<br>" };

    private void setupSheetAndBeanProperties(PropertySheetPanel sheet,
                                             BeanInfo bean)
    {
        sheet.setEditorFactory(propertyEditors);
        sheet.setRendererFactory(propertyRenderers);
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
        StringBuilder sb = new StringBuilder();
        XMLUtil xmlFix =
                new XMLUtil(IN_RANGE_INVALID_CR, IN_RANGE_VALID_CR);
        for(int i = 0, i2 = 0, c = descriptors.length; i < c; i++) {
            if(!descriptors[i].isHidden()) {
                // xmlify the description
                PropertyDescriptor d = descriptors[i];
                Option<?> opt = Options.getOption(d.getName());
                VimOption vopt = VimOption.get(d.getName());
                sb.setLength(0);
                if(opt != null) {
                    sb.append("Default: '<b>");
                    if(opt instanceof ColorOption) {
                        Color color = (Color)opt.getDefault();
                        sb.append(color == null
                                  ? "X"
                                  : String.format("#%06x", color.getRGB() & 0xffffff));
                    }
                    else
                        sb.append(opt.getDefault());
                    sb.append("</b>'");

                    sb.append("; ").append(d.getPropertyType().getSimpleName());
                    if(vopt != null) {
                        sb.append("; ");
                        if(vopt.isGlobal())
                            sb.append("global");
                        else
                            sb.append("local to ")
                              .append(vopt.isBuf() ? "buffer" : "window");
                        if(!vopt.isHidden())
                            sb.append("; can use '<b>:set</b>'.");
                    }
                    if(opt instanceof ColorOption) {
                        ColorOption copt = (ColorOption)opt;
                        sb.append("<br>Buttons:");
                        sb.append(" '<b>...</b>' color chooser");
                        if(copt.isPermitNull())
                            sb.append(", '<b>X</b>' use platform/null value");
                        sb.append(", '<b>DFLT</b>' use default value.");
                        if(vopt != null && !vopt.isHidden()) {
                            sb.append("<br>'<b>:set</b>' accepts 'default'");
                            if(copt.isPermitNull())
                                sb.append(", 'null'");
                            sb.append(", numeric values and java color names.");
                        }
                    }
                    sb.append("<br><br>");
                }
                String s = d.getShortDescription();
                xmlFix.utf2xml(s, sb);

                s = sb.toString();
                d.setShortDescription(s);
                Property prop = new MyPropAdapt(d);
                // wish PropertyDescriptor.createPropertyEditor was used
                if(prop.getType().equals(Color.class)) {
                    propertyEditors.registerEditor(
                            prop, new ColorPropertyEditor(prop));
                } else if(opt instanceof EnumOption) {
                    ComboBoxPropertyEditor pe = new ComboBoxPropertyEditor();
                    ((JComboBox)pe.getCustomEditor()).setMaximumRowCount(10);
                    pe.setAvailableValues(((EnumOption)opt).getAvailableValues());
                    propertyEditors.registerEditor(prop, pe);
                } else if(opt instanceof EnumSetOption) {
                    propertyEditors.registerEditor(prop,
                        new EnumSetPropertyEditor(prop));
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

    private static PropertyEditorRegistry propertyEditors;
    private static PropertyRendererRegistry propertyRenderers;
    
    private void createFactories() {
        if(propertyEditors == null) {
            PropertyEditorRegistry pe = new PropertyEditorRegistry();
            // Add our custom editors
            pe.registerEditor(boolean.class,
                              BooleanAsCheckBoxPropertyEditor.class);
            pe.registerEditor(Color.class,
                              ColorPropertyEditor.class);
            pe.registerEditor(EnumSet.class,
                              EnumSetPropertyEditor.class);
            propertyEditors = pe;
        }
        if(propertyRenderers == null) {
            propertyRenderers = new PropertyRendererRegistry();
            DefaultEnumSetCellRenderer renderer = new DefaultEnumSetCellRenderer();
            renderer.setShowOddAndEvenRows(false);
            propertyRenderers.registerRenderer(EnumSet.class, renderer);
        }
    }

    private static final Comparator STRING_COMPARATOR =
            new NaturalOrderStringComparator();

    // STRING_COMPARATOR
    static Comparator reverseStringCompare = (Comparator)
            (Object o1, Object o2) -> - STRING_COMPARATOR.compare(o1, o2);

    // STRING_COMPARATOR
    static Comparator propertyNameCompare = (Comparator)
            (Object o1, Object o2) -> {
        if (o1 instanceof Property && o2 instanceof Property) {
            Property prop1 = (Property) o1;
            Property prop2 = (Property) o2;
            if (prop1 == null) {
                return prop2==null ? 0 : -1;
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
    };
    
}
