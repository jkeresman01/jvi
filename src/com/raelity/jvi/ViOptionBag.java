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
 * Interface for classes that have vim options, i.e. ViTextView and Buffer
 * aka {@link G#curwin} and {@link G#curbuf}; these are per document and per
 * window options.
 * The values are initialized from global options and change with the set
 * command. The options are referenced by name through curwin and curbuf
 * and notification is sent with {@link #viOptionSet} when one of
 * its options changes. These options may interact with paltform features,
 * and {@link #activateOptions} is used when a window becomes active.
 *
 * @author erra
 */
public interface ViOptionBag {
    
    /** The set command used to change an option */
    public void viOptionSet(ViTextView tv, String name);
    
    /** This is invoked when switchto bag's associated editor. */
    public void activateOptions(ViTextView tv);
}
