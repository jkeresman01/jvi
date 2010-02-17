package com.raelity.jvi.manager;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.options.Option;
import com.raelity.jvi.swing.KeyBinding;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class CopyPreferences
{
    private static Logger LOG = Logger.getLogger(CopyPreferences.class.getName());

    Preferences srcRoot;
    Preferences dstRoot;

    CopyPreferences(Preferences dst, Preferences src, boolean clear)
    {
        dstRoot = dst;
        srcRoot = src;
        //copyJVi("");
        //copyKeys(PREFS_KEYS);
        copyTree("");
    }

    private void copyTree(String dir)
    {
        try {
            Preferences srcNode = srcRoot.node(dir);
            Preferences dstNode = dstRoot.node(dir);
            String[] children = srcRoot.node(dir).childrenNames();
            String[] keys = srcRoot.node(dir).keys();
            for (String key : keys) {
                dstNode.put(key, srcNode.get(key, ""));
            }
            for (String child : children) {
                String subTree = dir.equals("") ? child : (dir + child);
                copyTree(subTree);
            }
        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    //
    // The following copyXXX get rid of stuff set to the default
    // For it to work, a lot of stuff has to be set up
    //
    private void copyJVi(String dir)
    {
        String[] children;
        String[] options;
        try {
            Preferences srcNode = srcRoot.node(dir);
            Preferences dstNode = dstRoot.node(dir);
            children = srcRoot.node(dir).childrenNames();
            options = srcRoot.node(dir).keys();
            for (String optionName : options) {
                Option opt = Options.getOption(optionName);
                if (opt != null) {
                    String val;
                    if (!(val = srcNode.get(optionName, opt.getDefault())).equals(opt.getDefault())) {
                        System.err.println("ADD: " + optionName + ":" + val);
                        dstNode.put(optionName, val);
                    }
                    else
                        System.err.println("DEF: " + optionName + ":" + val);
                }
                else {
                    System.err.println("OPTION NOT FOUND: " + optionName);
                    dstNode.put(optionName, srcNode.get(optionName, ""));
                }
            }
        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        System.err.println("copy out");
    }

    private void copyKeys(String dir)
    {
        String[] children;
        String[] options;
        try {
            Preferences srcNode = srcRoot.node(dir);
            Preferences dstNode = dstRoot.node(dir);
            children = srcRoot.node(dir).childrenNames();
            options = srcRoot.node(dir).keys();
            for (String optionName : options) {
                if (KeyBinding.isKnownKey(optionName)) {
                    boolean val;
                    boolean sDefault = KeyBinding.getCatchKeyDefault(optionName);
                    val = srcNode.getBoolean(optionName, sDefault);
                    if (val != sDefault) {
                        System.err.println("ADD: " + optionName + ":" + val);
                        dstNode.putBoolean(optionName, val);
                    }
                    else
                        System.err.println("DEF: " + optionName + ":" + val);
                }
                else {
                    System.err.println("OPTION NOT FOUND: " + optionName);
                    dstNode.put(optionName, srcNode.get(optionName, ""));
                }
            }
        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        System.err.println("copy out");
    }
}
