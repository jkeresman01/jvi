/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.raelity.jvi;

/**
 * This is called during jVi initialization. It should be registered using
 * @ServiceProvider. It is called very early in the startup process. The init
 * method should do local initialization only since general jVi facilities
 * are generally not available.
 *
 * {@link ViManager#addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)}
 * can be called from init.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public interface ViInitialization
{
    public void init();
}
