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

/*
 * MapCommands.java
 * <p/>
 * NEEDSWORK: getPrefs was package scope.
 * <br/> Should be using a bean here.
 * <br/> With a bean, could more easily make this
 *       a generic text area independent of mappings.
 */
package com.raelity.jvi.swing.ui.options;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.core.lib.Mappings;
import com.raelity.jvi.options.OptUtil;
import com.raelity.jvi.options.Option;
import com.raelity.jvi.options.OptionsBean;
import com.raelity.text.XMLUtil;
import java.awt.Color;
import java.beans.PropertyVetoException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.util.WeakListeners;

/**
 * NEEDSWORK:
 * This should use the property descriptors to read/write Options.mapCommands.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class MapCommands extends javax.swing.JPanel
implements Options.EditControl {
    private final OptionsPanel optionsPanel;
    private final Option opt;
    private OptionsBean.General bean;
    private String statusCurrent = "Displaying saved mappings";
    private String statusModified = "Displaying modified mappings";
    private String statusError = "Displaying failed mappings ";

    private static String lastSetMappings;

    /** Creates new form MapCommands */
    public MapCommands(String optName, OptionsPanel optionsPanel)
    {
        assert optName.equals(Options.mapCommands);
        initComponents();
        this.optionsPanel = optionsPanel;
        opt = OptUtil.getOption(optName);
        bean = new OptionsBean.General();
        mappings.getDocument().addDocumentListener(
                WeakListeners.document(listen, mappings.getDocument()));
    }

    DocumentListener listen = new DocumentListener() {

        @Override public void insertUpdate(DocumentEvent e) {
            setStatus(false);
        }

        @Override public void removeUpdate(DocumentEvent e) {
            setStatus(false);
        }

        @Override public void changedUpdate(DocumentEvent e) { }
    };

    @Override
    public void start()
    {
        // read property values from backing store
        // and prepare for a new property edit op

        if(lastSetMappings == null
                || opt.getValue().equals(lastSetMappings)) {
            reset();
        } else {
            mappings.setText(lastSetMappings);
            mappings.setCaretPosition(0);
            status.setText(statusError);
            setStatus(true);
        }

        XMLUtil xmlFix = new XMLUtil(OptionSheet.IN_RANGE_INVALID_CR,
                                     OptionSheet.IN_RANGE_VALID_CR);

        description.setText("<html>"
            + "<b>"
            + opt.getDisplayName()
            + "</b><br>"
            + xmlFix.utf2xml(opt.getDesc()));
        description.setCaretPosition(0);
    }

    @Override
    public void ok()
    {
        lastSetMappings = mappings.getText();
        if(opt.getValue().equals(lastSetMappings))
            return;

        boolean change = false;
        try {
            bean.setViMapCommands(lastSetMappings);
            change = true;
        } catch(PropertyVetoException ex) {
            showError(ex.getCause() != null
                        ? ex.getCause().getMessage()
                        : ex.getMessage());
        }
        if(change && optionsPanel.changeNotify != null) {
            optionsPanel.changeNotify.change();
        }
    }

    @Override
    public void cancel()
    {
        // nothing to undo
    }

    private void setStatus(boolean defaultError)
    {
        if(opt.getValue().equals(mappings.getText())) {
            status.setForeground(Color.black);
            status.setText(statusCurrent);
        } else {
            if(defaultError) {
                status.setForeground(Color.red);
                status.setText(statusError);
            } else {
                status.setForeground(Color.black);
                status.setText(statusModified);
            }
        }
    }

    private void showError(String msg)
    {
        JOptionPane.showMessageDialog(
                null,
                msg,
                "jVi Mappings Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private void reset()
    {
        mappings.setText(opt.getValue());
        mappings.setCaretPosition(0);
        setStatus(false);
    }

    private void check()
    {
        String emsg = Mappings.parseMapCommands(mappings.getText());
        if(!emsg.isEmpty()) {
            showError(emsg);
            setStatus(true);
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "Parse succeeded",
                    "jVi Mappings Error",
                    JOptionPane.INFORMATION_MESSAGE);
            setStatus(false);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jSplitPane1 = new javax.swing.JSplitPane();
                jPanel1 = new javax.swing.JPanel();
                jScrollPane1 = new javax.swing.JScrollPane();
                mappings = new javax.swing.JTextArea();
                jPanel2 = new javax.swing.JPanel();
                reset = new javax.swing.JButton();
                check = new javax.swing.JButton();
                status = new javax.swing.JLabel();
                jScrollPane2 = new javax.swing.JScrollPane();
                description = new javax.swing.JEditorPane();

                jSplitPane1.setDividerSize(15);
                jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
                jSplitPane1.setResizeWeight(0.65);

                mappings.setColumns(20);
                mappings.setRows(15);
                jScrollPane1.setViewportView(mappings);

                reset.setText("Reset"); // NOI18N
                reset.setToolTipText("Display saved mappings"); // NOI18N
                reset.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                resetActionPerformed(evt);
                        }
                });

                check.setText("Check"); // NOI18N
                check.setToolTipText("Check for errors"); // NOI18N
                check.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                checkActionPerformed(evt);
                        }
                });

                status.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                status.setText("X"); // NOI18N

                javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(check)
                                .addGap(18, 18, 18)
                                .addComponent(reset)
                                .addContainerGap())
                );

                jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {check, reset});

                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(reset)
                                        .addComponent(check)
                                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, 18, Short.MAX_VALUE)))
                );

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                                        .addContainerGap()))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(193, Short.MAX_VALUE)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
                                        .addGap(49, 49, 49)))
                );

                jSplitPane1.setLeftComponent(jPanel1);

                description.setBackground(UIManager.getColor("Panel.background"));
                description.setContentType("text/html"); // NOI18N
                description.setEditable(false);
                jScrollPane2.setViewportView(description);

                jSplitPane1.setRightComponent(jScrollPane2);

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE)
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents

    private void resetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_resetActionPerformed
    {//GEN-HEADEREND:event_resetActionPerformed
        reset();
    }//GEN-LAST:event_resetActionPerformed

    private void checkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_checkActionPerformed
    {//GEN-HEADEREND:event_checkActionPerformed
        check();
    }//GEN-LAST:event_checkActionPerformed

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton check;
        private javax.swing.JEditorPane description;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JSplitPane jSplitPane1;
        private javax.swing.JTextArea mappings;
        private javax.swing.JButton reset;
        private javax.swing.JLabel status;
        // End of variables declaration//GEN-END:variables
}
