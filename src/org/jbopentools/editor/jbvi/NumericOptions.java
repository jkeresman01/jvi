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
package org.jbopentools.editor.jbvi;

import java.awt.*;
import javax.swing.*;
import com.borland.jbcl.layout.*;
import javax.swing.border.*;
import java.awt.event.*;

import com.borland.primetime.properties.PropertyPage;
import com.borland.primetime.help.HelpTopic;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.G;
import com.raelity.text.RegExpFactory;

public class NumericOptions extends PropertyPage {

  public HelpTopic getHelpTopic() {
    return null;
  }

  /** The only validation is that the text fields must contain
   * only positive numbers. If there is an error, and the text
   * is disabled, then quietly make it 0.
   */
  public boolean isValid() {
    try {
      checkValidNumber(reportText, "Report value must be a positive number");
      checkValidNumber(scrolloffText,
                        "Scrolloff value must be a positive number");
      checkValidMetachars();
    } catch(Exception ex) {
      return false;
    }
    return true;
  }

  /** Load the current values into the UI */
  public void readProperties() {
    backspace1Check.setSelected(ViNumericPropertyGroup.BACKSPACE1.getBoolean());
    backspace2Check.setSelected(ViNumericPropertyGroup.BACKSPACE2.getBoolean());

    reportCheck.setSelected(ViNumericPropertyGroup.REPORT_ENBL.getBoolean());
    reportText.setText("" + ViNumericPropertyGroup.REPORT.getInteger());

    scrolloffCheck.setSelected(
                    ViNumericPropertyGroup.SCROLLOFF_ENBL.getBoolean());
    scrolloffText.setText("" + ViNumericPropertyGroup.SCROLLOFF.getInteger());

    syncState(reportCheck, reportLabel, reportText);
    syncState(scrolloffCheck, scrolloffLabel, scrolloffText);

    metaEqualsCheck.setSelected(
                          ViNumericPropertyGroup.META_EQUALS.getBoolean());
    metaText.setText(ViNumericPropertyGroup.META_ESCAPE.getValue());

    regExpText.setText("search using: " + RegExpFactory.getRegExpDisplayName());
  }

  /** Save the new values just entered */
  public void writeProperties() {
    ViNumericPropertyGroup.BACKSPACE1.setBoolean(backspace1Check.isSelected());
    ViNumericPropertyGroup.BACKSPACE2.setBoolean(backspace2Check.isSelected());

    ViNumericPropertyGroup.REPORT_ENBL.setBoolean(reportCheck.isSelected());
    ViNumericPropertyGroup.REPORT.setInteger(
                                      checkValidNumber(reportText, null));

    ViNumericPropertyGroup.SCROLLOFF_ENBL.setBoolean(
                                      scrolloffCheck.isSelected());
    ViNumericPropertyGroup.SCROLLOFF.setInteger(
                                      checkValidNumber(scrolloffText, null));

    ViNumericPropertyGroup.META_EQUALS.setBoolean(metaEqualsCheck.isSelected());
    ViNumericPropertyGroup.META_ESCAPE.setValue(metaText.getText().trim());

    // the ui values have been copied, now let vi know about them
    ViNumericPropertyGroup.getInstance().initializeProperties();
  }

  private void checkValidMetachars() throws Exception {
    String s = metaText.getText().trim();
    for(int i = 0; i < s.length(); i++) {
      if(G.metaEscapeAll.indexOf(s.charAt(i)) < 0) {
        reportValidationError(metaText, "Only metacharacters from '"
                          + G.metaEscapeAll + "' can be escaped");
        throw new Exception();
      }
    }
  }

  /**
   * Verify that the argument text field has a valid number.
   * If its only blanks, tread it as empty. Empty is 0 value.
   * If the value is less than zero that is an error.
   */
  private int checkValidNumber(JTextField f, String emsg)
                                throws NumberFormatException {
    int i = 0;
    String v = f.getText();
    boolean hasData = false;
    for(int j = 0; j < v.length(); j++) {
      if(v.charAt(j) != ' ') {
        hasData = true;
      }
    }
    if(! hasData) {
      return 0;
    }
    NumberFormatException e = null;
    try {
      i = Integer.parseInt(v);
    }
    catch (NumberFormatException ex) {
      e = ex;
    }
    if(i < 0) {
      e = new NumberFormatException();
    }
    if(e != null) {
      if(f.isEnabled()) {
        reportValidationError(f, emsg);
        throw e;
      }
      f.setText("");
    }
    return i;
  }

  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel1 = new JPanel();
  JPanel jPanel2 = new JPanel();
  JPanel jPanel3 = new JPanel();
  TitledBorder titledBorder1;
  TitledBorder titledBorder2;
  JPanel jPanel4 = new JPanel();
  TitledBorder titledBorder3;
  JCheckBox backspace2Check = new JCheckBox();
  JCheckBox backspace1Check = new JCheckBox();
  JCheckBox reportCheck = new JCheckBox();
  JPanel jPanel5 = new JPanel();
  JLabel reportLabel = new JLabel();
  JTextField reportText = new JTextField();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  GridBagLayout gridBagLayout3 = new GridBagLayout();
  GridBagLayout gridBagLayout4 = new GridBagLayout();
  GridBagLayout gridBagLayout5 = new GridBagLayout();
  JCheckBox scrolloffCheck = new JCheckBox();
  JPanel jPanel6 = new JPanel();
  JTextField scrolloffText = new JTextField();
  JLabel scrolloffLabel = new JLabel();
  GridBagLayout gridBagLayout6 = new GridBagLayout();
  GridBagLayout gridBagLayout7 = new GridBagLayout();
  Border border1;
  JPanel jPanel7 = new JPanel();
  JLabel releaseLabel = new JLabel();
  JPanel jPanel8 = new JPanel();
  TitledBorder titledBorder4;
  JCheckBox metaEqualsCheck = new JCheckBox();
  JPanel jPanel9 = new JPanel();
  JLabel metaLabel01 = new JLabel();
  JTextField metaText = new JTextField();
  GridBagLayout gridBagLayout8 = new GridBagLayout();
  GridBagLayout gridBagLayout9 = new GridBagLayout();
  JLabel metaLabel02 = new JLabel();
  JLabel regExpText = new JLabel();

  public NumericOptions() {
    try {
      jbInit();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    syncState(reportCheck, reportLabel, reportText);
    syncState(scrolloffCheck, scrolloffLabel, scrolloffText);
  }

  void jbInit() throws Exception {
    titledBorder1 = new TitledBorder("");
    titledBorder2 = new TitledBorder("");
    titledBorder3 = new TitledBorder("");
    border1 = BorderFactory.createEmptyBorder(0,3,0,3);
    titledBorder4 = new TitledBorder("");
    titledBorder4.setTitle("regular expression metacharacters");
    this.setLayout(borderLayout1);
    jPanel1.setLayout(gridBagLayout7);
    jPanel2.setBorder(titledBorder1);
    jPanel2.setLayout(gridBagLayout3);
    titledBorder1.setTitle("<backspace> and <del> during insert");
    jPanel3.setBorder(titledBorder2);
    jPanel3.setLayout(gridBagLayout1);
    titledBorder2.setTitle("reporting lines changed (report)");
    jPanel4.setBorder(titledBorder3);
    jPanel4.setLayout(gridBagLayout6);
    titledBorder3.setTitle("visible context around cursor (scrolloff)");
    backspace2Check.setText("allow backspace over start of insert");
    backspace2Check.addItemListener(new java.awt.event.ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        backspace2Check_itemStateChanged(e);
      }
    });
    backspace1Check.setText("allow backspace over <EOL>");
    backspace1Check.addItemListener(new java.awt.event.ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        backspace1Check_itemStateChanged(e);
      }
    });
    reportCheck.setText("enable");
    reportCheck.addItemListener(new java.awt.event.ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        reportCheck_itemStateChanged(e);
      }
    });
    reportLabel.setText("minimum to report:");
    reportText.setText("");
    jPanel5.setLayout(gridBagLayout2);
    scrolloffCheck.setText("enable");
    scrolloffCheck.addItemListener(new java.awt.event.ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        scrolloffCheck_itemStateChanged(e);
      }
    });
    jPanel6.setLayout(gridBagLayout4);
    scrolloffText.setText("");
    scrolloffLabel.setText("line count:");
    jPanel1.setBorder(border1);
    releaseLabel.setEnabled(false);
    releaseLabel.setText(ViManager.getReleaseString());
    jPanel8.setBorder(titledBorder4);
    jPanel8.setLayout(gridBagLayout8);
    metaEqualsCheck.setText("use \'=\', not \'?\', to indicate optional atom");
    metaLabel01.setText("metacharacters requiring escape:");
    metaLabel02.setText("any of: '(', ')', '|', '+', '?', '{'");
    metaText.setText("");
    jPanel9.setLayout(gridBagLayout9);
    regExpText.setText("search using: ");
    this.add(jPanel1, BorderLayout.NORTH);
    jPanel1.add(jPanel2, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), -10, 0));
    jPanel2.add(backspace1Check, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 146, 0));
    jPanel2.add(backspace2Check, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 106, 0));
    jPanel1.add(jPanel3, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 111, 0));
    jPanel3.add(reportCheck, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 4, 2, 0), 0, 0));
    jPanel3.add(jPanel5, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 4), 0, 0));
    jPanel5.add(reportLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 23, 0, 0), 0, 0));
    jPanel5.add(reportText, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    jPanel1.add(jPanel4, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 129, 0));
    jPanel4.add(scrolloffCheck, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 4, 2, 0), 0, 0));
    jPanel4.add(jPanel6, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 4), 0, 0));
    jPanel6.add(scrolloffLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 23, 0, 0), 0, 0));
    jPanel6.add(scrolloffText, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    jPanel1.add(jPanel8, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));
    jPanel8.add(metaEqualsCheck, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 0, 0), 0, 0));
    jPanel8.add(jPanel9, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 8), 0, 0));
    jPanel9.add(metaLabel01, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0));
    jPanel9.add(metaText, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    jPanel9.add(metaLabel02, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel9.add(regExpText, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0));
    this.add(jPanel7, BorderLayout.SOUTH);
    jPanel7.add(releaseLabel, null);
  }

  void backspace1Check_itemStateChanged(ItemEvent e) {
    // if disabling, then disable 2
    if(e.getStateChange() == ItemEvent.DESELECTED) {
      backspace2Check.setSelected(false);
    }
  }

  void backspace2Check_itemStateChanged(ItemEvent e) {
    // if enabling, then enable 1
    if(e.getStateChange() == ItemEvent.SELECTED) {
      backspace1Check.setSelected(true);
    }
  }

  void reportCheck_itemStateChanged(ItemEvent e) {
    // enable/disable label/text accordingly
    syncState(reportCheck, reportLabel, reportText);
  }

  void scrolloffCheck_itemStateChanged(ItemEvent e) {
    // enable/disable label/text accordingly
    syncState(scrolloffCheck, scrolloffLabel, scrolloffText);
  }

  private void syncState(JCheckBox check,
                         JLabel label,
                         JTextField text)
  {
    boolean canUse;
    if(check.isSelected()) {
      canUse = true;
    } else {
      canUse = false;
    }
    label.setEnabled(canUse);
    text.setEnabled(canUse);
  }
}
