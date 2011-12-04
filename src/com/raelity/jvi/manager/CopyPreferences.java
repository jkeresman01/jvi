package com.raelity.jvi.manager;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.options.Option;
import com.raelity.jvi.swing.KeyBinding;

class CopyPreferences
{
    private static final
            Logger LOG = Logger.getLogger(CopyPreferences.class.getName());

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
                String subTree = dir.isEmpty() ? child : (dir + child);
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
                    if (!(val = srcNode.get(optionName, "-DEFAULT-VALUE-"))
                                    .equals("-DEFAULT-VALUE-")) {
                        LOG.log(Level.CONFIG, "ADD: {0}:{1}", new Object[]{optionName, val});
                        dstNode.put(optionName, val);
                    }
                    else
                        LOG.log(Level.CONFIG, "DEF: {0}:{1}", new Object[]{optionName, val});
                }
                else {
                    LOG.log(Level.CONFIG, "OPTION NOT FOUND: {0}", optionName);
                    dstNode.put(optionName, srcNode.get(optionName, ""));
                }
            }
        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        LOG.config("copy out");
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
                        LOG.log(Level.CONFIG, "ADD: {0}:{1}", new Object[]{optionName, val});
                        dstNode.putBoolean(optionName, val);
                    }
                    else
                        LOG.log(Level.CONFIG, "DEF: {0}:{1}", new Object[]{optionName, val});
                }
                else {
                    LOG.log(Level.CONFIG, "OPTION NOT FOUND: {0}", optionName);
                    dstNode.put(optionName, srcNode.get(optionName, ""));
                }
            }
        } catch (BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        LOG.config("copy out");
    }
}
