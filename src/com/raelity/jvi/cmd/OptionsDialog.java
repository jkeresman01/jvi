/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.raelity.jvi.cmd;

import com.l2fprod.common.beans.ExtendedPropertyDescriptor;
import com.l2fprod.common.propertysheet.AbstractProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.swing.LookAndFeelTweaks;
import com.raelity.jvi.OptionsBean;
import com.raelity.jvi.swing.KeyBindingBean;
import com.raelity.jvi.swing.KeypadBindingBean;
import java.awt.Component;
import java.awt.Frame;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import javax.swing.JDialog;
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
public class OptionsDialog {
    static private JDialog dialog;

    public static void show(Frame owner) {
        if(dialog == null) {
            dialog = new JDialog(owner, "jVi Options");
            JTabbedPane tabs = new JTabbedPane();
            tabs.add("Platform", new OptionSheet(new OptionsBean.Platform()));
            tabs.add("General", new OptionSheet(new OptionsBean.General()));
            tabs.add("Search", new OptionSheet(new OptionsBean.Search()));
            tabs.add("Modify", new OptionSheet(new OptionsBean.Modify()));
            tabs.add("CursorWrap", new OptionSheet(new OptionsBean.CursorWrap()));
            tabs.add("External Process",
                     new OptionSheet(new OptionsBean.ExternalProcess()));
            tabs.add("Ctrl-Key", new OptionSheet(new KeyBindingBean()));
            tabs.add("KeyPad", new OptionSheet(new KeypadBindingBean()));
            tabs.add("Debug", new OptionSheet(new OptionsBean.Debug()));
            dialog.add("Center", tabs);
            dialog.pack();

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
                    int h = sp.getHeight();
                    //int loc = sp.getDividerLocation();
                    int newLoc = h - 100;
                    sp.setDividerLocation(newLoc);
                    double d = sp.getResizeWeight();
                    //sp.setResizeWeight(.5);
                    //sp.invalidate();
                }
                // Need to set false, so the setDividerLocation is remembered
                sheet.setDescriptionVisible(false);
                sheet.setDescriptionVisible(true);
            }
            //dialog.pack();
        }
        dialog.setVisible(true);
    }

    // NEEDSWORK:   convert string to xml for property descriptions
    //              net.sourceforge.groboutils.util.xml.v1.XMLUtil
    private static class OptionSheet extends JPanel {
        BeanInfo bean; // keep a reference, NOTE: bean/beanInfo are same class
        PropertySheetPanel sheet;
        OptionSheet(final BeanInfo bean) {
            this.bean = bean;

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
            setupSheetAndBeanProperties(sheet, bean); //sheet.setBeanInfo(bean);
            sheet.readFromObject(bean);

            sheet.setMode(PropertySheet.VIEW_AS_CATEGORIES);
            sheet.setDescriptionVisible(true);
            sheet.setSortingCategories(true);
            sheet.setSortingProperties(false);
            sheet.setRestoreToggleStates(true);
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
            
            // everytime a property change, update the button with it
            PropertyChangeListener listener = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    Property prop = (Property)evt.getSource();
                    prop.writeToObject(bean);
                    //bean.repaint();
                }
            };
            sheet.addPropertySheetChangeListener(listener);
        }

        private void setupSheetAndBeanProperties(PropertySheetPanel sheet,
                                                 BeanInfo bean) {
            PropertyDescriptor[] descriptors = bean.getPropertyDescriptors();

            // count the hidden properties
            int nHidden = 0;
            for (int i = 0, c = descriptors.length; i < c; i++) {
                if(descriptors[i].isHidden())
                    nHidden++;
            }

            // exclude the hidden properties
            Property[] properties = new Property[descriptors.length - nHidden];
            for (int i = 0, i2 = 0, c = descriptors.length; i < c; i++) {
                if(!descriptors[i].isHidden())
                    properties[i2++] = new MyPropAdapt(descriptors[i]);
            }
            sheet.setProperties(properties);

            // compare reverse order so that Prop is before Expert
            // compare method from PropSheetTableModel::NaturalOrderStringCo...
            ((PropertySheetTableModel)sheet.getTable().getModel())
                    .setCategorySortingComparator(new Comparator() {
                public int compare(Object o1, Object o2) {
                    String s1 = (String) o1;
                    String s2 = (String) o2;
                    if (s1 == null) {
                        return s2==null?0:-1;
                    } else {
                        if (s2 == null) {
                            return 1;
                        } else {
                            return -s1.compareTo(s2);
                        }
                    }
                }
            });
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
}
