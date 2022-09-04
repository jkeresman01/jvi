/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.raelity.jvi;

/**
 * This is called during jVi initialization. It should be registered using
 * @ServiceProvider. It is called very early in the jVi's startup process,
 * in particular from {@link ViManager#setViFactory(com.raelity.jvi.ViFactory)}
 * and from the init methods {@link ViManager#getViFactory()} behaves as expected.
 * The init
 * method should do local initialization only since general jVi facilities
 * are generally not available.
 *
 * When used,
 * {@link ViManager#addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)}
 * is typically called from init.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public interface ViInitialization
{
    public void init();
}
