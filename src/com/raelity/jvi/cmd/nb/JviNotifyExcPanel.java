/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.cmd.nb;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.raelity.jvi.lib.*;


/**
 * In NB, this is a JPanel in the dialog; as an instance, it manages
 * the buttons. For now, we're using JoptionPane, but keep the instance
 * so the structure is similar hopefully.
 * 
 * @author err
 */
public class JviNotifyExcPanel extends JPanel
{
    /** the instance */
    private static JviNotifyExcPanel INSTANCE = null;

    /** preferred width of this component */
    private static final int SIZE_PREFERRED_WIDTH=550;
    /** preferred height of this component */
    private static final int SIZE_PREFERRED_HEIGHT=250;

    private static final int MAX_STORED_EXCEPTIONS = 5; // was 500

    /** enumeration of NbExceptionManager.Exc to notify */
    static ArrayListPos exceptions;


    /** current exception */
    private JviErrorManager.Exc current;
    /** details window */
    private JTextPane output;

    private static int extraH = 0, extraW = 0;

    private JviNotifyExcPanel () {

        output = new JTextPane() {
            public @Override boolean getScrollableTracksViewportWidth() {
                return false;
            }
        };
        output.setEditable(false);
        Font f = output.getFont();
        output.setFont(new Font("Monospaced", Font.PLAIN, null == f ? 12 : f.getSize() + 1)); // NOI18N
        output.setForeground(UIManager.getColor("Label.foreground")); // NOI18N
        output.setBackground(UIManager.getColor("Label.background")); // NOI18N

        setLayout( new BorderLayout() );
        add(new JScrollPane(output));
        setBorder( new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
            
        // Much stuff removed
        
        setPreferredSize(new Dimension(SIZE_PREFERRED_WIDTH + extraW, SIZE_PREFERRED_HEIGHT + extraH));

    }

    /** Adds new exception into the queue.
     */
    static void notify (
        final JviErrorManager.Exc t
    ) {
        if (!t.isUserQuestion() && !shallNotify(t.getSeverity(), false)) {
            return;
        }
        
        // #50018 Don't try to show any notify dialog when reporting headless exception
        if (/*"java.awt.HeadlessException".equals(t.getClassName()) &&*/ GraphicsEnvironment.isHeadless()) { // NOI18N
            t.printStackTrace(System.err);
            return;
        }

        SwingUtilities.invokeLater (new Runnable () {
            @Override
            public void run() {
                String glm = t.getLocalizedMessage();
                Level gs = t.getSeverity();
                boolean loc = t.isLocalized();
                
                if (t.isUserQuestion() && loc) {
                    // Object ret = DialogDisplayer.getDefault().notify(
                    //            new NotifyDescriptor.Confirmation(glm, NotifyDescriptor.OK_CANCEL_OPTION));
                    // if (ret == NotifyDescriptor.OK_OPTION) {
                    //     try {
                    //         t.confirm();
                    //     } catch (IOException ex) {
                    //         Exceptions.printStackTrace(ex);
                    //     }
                    // }
                    return;
                }

                if (loc) {
                    if (gs == Level.WARNING) {
                        // DialogDisplayer.getDefault().notify(
                        //     new NotifyDescriptor.Message(glm, NotifyDescriptor.WARNING_MESSAGE)
                        // );
                        return;
                    }

                    if (gs.intValue() == 1973) {
                        // DialogDisplayer.getDefault().notify(
                        //     new NotifyDescriptor.Message(glm, NotifyDescriptor.INFORMATION_MESSAGE)
                        // );
                        return;
                    }

                    if (gs == Level.SEVERE) {
                        // DialogDisplayer.getDefault().notify(
                        //     new NotifyDescriptor.Message(glm, NotifyDescriptor.ERROR_MESSAGE)
                        // );
                        return;
                    }
                }

                
                if( null == exceptions ) {
                    exceptions = new ArrayListPos();
                } else if (exceptions.size() >= MAX_STORED_EXCEPTIONS) {
                    // Ignore huge number of exceptions, prevents from OOME.
                    return ;
                }
                exceptions.add(t);
                exceptions.position = exceptions.size()-1;

                if(shallNotify(t.getSeverity(), true)) {
                    // Assertions are on, so show the exception window.
                    if( INSTANCE == null ) {
                        INSTANCE = new JviNotifyExcPanel();
                    }
                    INSTANCE.updateState(t);
                } else {
//                  // No assertions, use the flashing icon.
//                  if( null == INSTANCE ) {
//                      ImageIcon img1 = ImageUtilities.loadImageIcon("org/netbeans/core/resources/exception.gif", true);
//                      String summary = getExceptionSummary(t);
//                      ExceptionFlasher flash = ExceptionFlasher.notify(summary, img1);
//                      //exception window is not visible, start flashing the icon
//                  } else {
//                      //exception window is already visible (or the flashing icon is not available)
//                      //so we'll only update the exception window
//                      if( INSTANCE == null ) {
//                          INSTANCE = new NotifyExcPanel();
//                      }
//                      INSTANCE.updateState(t);
//                  }
                }
            }
        });
    }

    /**
     * updates the state of the dialog. called only in AWT thread.
     */
    private void updateState (JviErrorManager.Exc t) {
        if (!exceptions.existsNextElement()) {
            // it can be commented out while INSTANCE is not cached
            // (see the comment in actionPerformed)
            /*// be modal if some modal dialog is already opened, nonmodal otherwise
            boolean isModalDialogOpened = NbPresenter.currentModalDialog != null;
            if (descriptor.isModal() != isModalDialogOpened) {
                descriptor.setModal(isModalDialogOpened);
               // bugfix #27176, old dialog is disposed before recreating
               if (dialog != null) dialog.dispose ();
               // so we can safely send it to gc and recreate dialog
               // dialog = org.openide.DialogDisplayer.getDefault ().createDialog (descriptor);
            }*/
            // the dialog is not shown
            current = t;
            update ();
        } else {
            // add the exception to the queue
// ENABLE NEXT BUTTON
//          next.setVisible (true);
//          dialog.pack();
        }
//      try {
//          //Dialog.show() will pump events for the AWT thread.  If the 
//          //exception happened because of a paint, it will trigger opening
//          //another dialog, which will trigger another exception, endlessly.
//          //Catch any exceptions and append them to the list instead.
//          ensurePreferredSize();
//          if (!dialog.isVisible()) {
//              dialog.setVisible(true);
//          }
//          //throw new RuntimeException ("I am not so exceptional"); //uncomment to test
//      } catch (Exception e) {
//          exceptions.add(NbErrorManager.createExc(
//              e, Level.SEVERE, null));
//          next.setVisible(true);
//      }
    }

    // Take a look at NotifyExcPanel, there's a lot of action in there.
    // Just put up a static (no buttons) dialog
    private void update() {
        String msg = null;
        StringWriter wr = new StringWriter();
        current.printStackTrace(new PrintWriter(wr, true));
        output.setText(wr.toString());
        output.getCaret().setDot(0);
        int sev = current.getSeverity().intValue();
        int msgType = sev >= Level.SEVERE.intValue() ? JOptionPane.ERROR_MESSAGE
                  : sev >= Level.WARNING.intValue() ? JOptionPane.WARNING_MESSAGE
                  : JOptionPane.INFORMATION_MESSAGE;
        if(true) {
            JOptionPane optPane = new JOptionPane(INSTANCE, msgType);
            JDialog dialog = optPane.createDialog(null, "Exception");
            if(!dialog.isResizable())
                dialog.setResizable(true);
            UIUtil.translateToPrefScreen(dialog);
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(null, INSTANCE);
        }
        msg = null;
        // In NB the dialog can have a whole bunch of exceptions
        // and next/prev. When dismissed, clear the list.
        exceptions = null;
    }


    /** Method that checks whether the level is high enough to be notified
     * at all.
     * @param dialog shall we check for dialog or just a blinking icon (false)
     */
    private static boolean shallNotify(Level level, boolean dialog) {
        int minAlert = Integer.getInteger("netbeans.exception.alert.min.level", 900); // NOI18N
        boolean assertionsOn = false;
        assert assertionsOn = true;
        int defReport = assertionsOn ? 900 : 1001;
        int minReport = Integer.getInteger("netbeans.exception.report.min.level", defReport); // NOI18N

        if (dialog) {
            return level.intValue() >= minReport;
        } else {
            return level.intValue() >= minAlert || level.intValue() >= minReport;
        }
    }

    static class ArrayListPos extends ArrayList<JviErrorManager.Exc> {
        static final long serialVersionUID = 2L;
        
        static final int SOFT_MAX_SIZE = 20;
        static final int HARD_MAX_SIZE = 100;   // To prevent from OOME when too many exceptions are thrown

        protected int position;

        protected ArrayListPos () {
            super();
            position=0;
        }

        @Override
        public boolean add(JviErrorManager.Exc e) {
            if (size() >= SOFT_MAX_SIZE && position < size() - 5) {
                set(size() - 1, e);
                return true;
            } else {
                if (size() >= HARD_MAX_SIZE) {
                    remove(5);  // it's beneficient to see the initial exceptions
                }
                return super.add(e);
            }
        }

        protected boolean existsElement () {
            return size()>0;
        }

        protected boolean existsNextElement () {
            return position+1<size();
        }

        protected boolean existsPreviousElement () {
            return position>0&&size()>0;
        }

        protected boolean setNextElement () {
            if(!existsNextElement()) {
                return false;
            }
            position++;
            return true;
        }

        protected boolean setPreviousElement () {
            if(!existsPreviousElement()) {
                return false;
            }
            position--;
            return true;
        }

        protected JviErrorManager.Exc get () {
            return existsElement()?get(position):null;
        }

        protected void removeAll () {
            clear();
            position=0;
        }
    }
    
}
