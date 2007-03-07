/*
 * ViOptionBag.java
 *
 * Created on March 5, 2007, 4:40 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi;

/**
 * Interface for a class that has vim options. The options are referenced
 * by name through its Class and notification is sent when one of
 * its options changes. Notification only because there may be platform
 * dependencies involved.
 *
 * @author erra
 */
public interface ViOptionBag {
    
    /** The set command used to change an option */
    public void viOptionChange(ViTextView tv, String name);
    
    /** This is invoked when switchto bag's associated editor. */
    public void activateOptions(ViTextView tv);
}
