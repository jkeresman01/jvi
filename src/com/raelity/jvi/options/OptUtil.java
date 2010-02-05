/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.raelity.jvi.options;

import com.raelity.jvi.Options;
import com.raelity.jvi.ViManager;
import java.beans.PropertyChangeSupport;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class OptUtil {

    private static boolean didInit = false;
    private static Preferences prefs;
    private static PropertyChangeSupport pcs;

    public static void init(PropertyChangeSupport pcs)
    {
        if (didInit) {
            return;
        }
        didInit = true;

        OptUtil.pcs = pcs;

        prefs = ViManager.getViFactory().getPreferences();

        prefs.addPreferenceChangeListener(new PreferenceChangeListener() {

            public void preferenceChange(PreferenceChangeEvent evt)
            {
                Option opt = Options.getOption(evt.getKey());
                if (opt != null) {
                    if (evt.getNewValue() != null) {
                        opt.preferenceChange(evt.getNewValue());
                    }
                }
            }
        });
    }

    static Preferences getPrefs()
    {
        return prefs;
    }

  /** This should only be used from Option and its subclasses */
  static void firePropertyChange(String name, Object oldValue, Object newValue) {
    pcs.firePropertyChange(name, oldValue, newValue);
  }
}
