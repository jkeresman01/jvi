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

package com.raelity.jvi.manager;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.openide.util.WeakSet;
import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.lib.CcFlag;

/**
 * These static methods are used by the platform to inform jVi of the state
 * of editors, for example activated or closed, and to query about opened
 * editors.
 *
 * jVi maintains three lists of opened editors: ACTIVE is the order
 * they opened, MRU is MostRecentlyUsed list and NOMAD are editors that
 * are not associated with a top level platform editor.
 *
 * Even when jVi is disabled these methods should be used so that new editors
 * are properly handled.
 *
 * Here are several static methods used to inform jVi of major changes.
 * A {@link ViAppView} is the primary handle for an application editor.
 * There is one of these for each open editor.
 * If the same document is editted in two windows, then
 * there are two of these.
 * <ul>
 * <li>{@link #open(AppView, String)}<br/>
 * Add the appView to jVi's lists of known editors.
 * </li>
 * <li>{@link #deactivate}(appView)<br/>
 * Inform jVi that the currently active editor is going quiet. Typically some
 * other part fo the application gets focus.
 * </li>
 * <li>{@link #close}(appView)<br/>
 * The applications invokes this method when a file is completely
 * removed from a container or should be forgotten by jVi.
 * </li>
 * </ul>
 *
 * @author Ernie Rael <err at raelity.com>
 */
public enum AppViews
{
    ACTIVE,
    MRU,
    NOMAD,
    ALL; // ACTIVE + NOMAD

    private static final List<ViAppView> avs = new ArrayList<>();
    private static final List<ViAppView> avsMRU = new ArrayList<>();
    private static final Set<ViAppView> avsNomads = new WeakSet<>();
    private static ViAppView avCurrentlyActive;
    private static ViAppView keepMru;

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
        private static boolean didInit;
        @Override
        public void init()
        {
            if(didInit)
                return;
            AppViews.init();
            didInit = true;
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void init()
    {
        ColonCommands.register("dumpJvi", "dumpJvi", (ActionEvent e) -> {
            System.err.println(AppViews.dump(null).toString());
        }, EnumSet.of(CcFlag.DBG));
        getLocation(null); // shut up the not-used warning
    }

    /**
     * The application invokes this whenever a file becomes active
     * in the specified container. Typically, by this time activate
     * has been called from the scheduler, and this a no-op.
     * It sets current av active if the scheduler agrees.
     * <p>
     * This is needed because the application can invoke deactivate on current,
     * and the scheduler does not change it's concept of current.
     * </p>
     * 
     * FROM: NB-module
     * 
     * @param appView AppView that is getting focus for editing.
     * Editor may be null, may be a nomad.
     */
    public static void activateCurrent(ViAppView av)
    {
        // Only do anything if this av is already first in MRU list
        if (hasFact()
                && Scheduler.getCurrentEditor() == av.getEditor()
                && !avsMRU.isEmpty() && avsMRU.get(0).equals(av)
                && avCurrentlyActive == null)
            avCurrentlyActive = av;
    }

    /**
     * The scheduler invokes this whenever a file becomes selected
     * in the specified container. This also serves as an open.
     * 
     * FROM: Scheduler
     * 
     * @param appView AppView that is getting focus for editing.
     * Editor may be null, may be a nomad.
     */
    /*package*/ static void activate(ViAppView av)
    {
        activate(av, "");
    }

    /**
     * @param appView AppView
     * @param tag String used in debug messages.
     */
    private static void activate(ViAppView av, String tag)
    {
        if(av.equals(avCurrentlyActive))
            return;
        if(avCurrentlyActive != null)
            deactivateCurrent();
        Scheduler.cancelCommandEntry();

        Component ed = av.getEditor();
        if (fact() != null) {
            G.dbgEditorActivation().println(() -> "Activation: AppViews.activate: "
                    + tag + " " + ViManager.cid(ed) + " " + ViManager.cid(av)
                    + " " + fact().getFS().getDisplayPath(av));
        }
        ViAppView keep = keepMru;
        keepMru = null;
        avCurrentlyActive = av;
        if (!(avs.contains(keep) && av.equals(keep)))
            adjustMru(av); // adjust since keepMru not in effect

        // NEEDSWORK: not sure this is quite right.
        //          what if curwin and tv have different displayers
        if (G.curwin() != null)
            G.curwin().getStatusDisplay().refresh();
        ViTextView tv = fact().getTextView(av);
        if(tv != null) {
            // NEEDSWORK: which window's status displayer is used by Msg...
            Msg.smsg(fact().getFS().getDisplayFileViewInfo(tv));
        }
    }

    /** FROM: NB-Module */
    public static void deactivate(ViAppView av)
    {
        if(av.equals(avCurrentlyActive))
            deactivateCurrent();
    }

    /**
     * Something that is not an editor is getting focus.
     * The associated jVi state is put into normal mode.
     * @param av appView loosing focus.
     */
    private static void deactivateCurrent()
    {
        deactivateCurrent(false);
    }

    /** FROM: Scheduler */
    /*package*/ static void deactivateCurrent(boolean force)
    {
        if(!force && avCurrentlyActive == null)
            return;
        Scheduler.cancelCommandEntry();
        if (fact() != null) {
            G.dbgEditorActivation().println("Activation: AppViews.deactivateCurrent: ");
        }
            //System.err.println("Activation: avs.deactivateCurent: " +
            //        " " + ViManager.cid(av) + fact().getFS().getDisplayFileName(av));
        if (hasFact() && Scheduler.getCurrentEditor() != null) {
            ViManager.exitInputMode(); // NEEDSWORK: relates AppView to Editor
            G.curwin().getStatusDisplay().clearDisplay();
        }
        avCurrentlyActive = null;
    }

    /**
     * The applications invokes this method when a file is completely
     * removed from a container or should be forgotten by jVi.
     * 
     * FROM: jVi.Util, NB-Module
     */
    public static void close(ViAppView av)
    {
        Component ed = av.getEditor();
        if (fact() != null) {
            G.dbgEditorActivation().println(() -> "Activation: AppViews.close: "
                    + (ed == null ? "(no shutdown) " : "")
                    + fact().getFS().getDisplayPath(av) + " " + ViManager.cid(ed));
        }
        ViTextView tv = fact().getTextView(ed);
        if (tv != null) {
            ViManager.firePropertyChange(ViManager.P_CLOSE_TV, tv, null);
            if (tv.getBuffer().singleShare())
                ViManager.firePropertyChange(
                        ViManager.P_CLOSE_BUF, tv.getBuffer(), null);
        }
        assert (hasFact());
        if (hasFact() && ed != null)
            fact().shutdown(ed);
        if (av.equals(avCurrentlyActive))
            avCurrentlyActive = null;
        avsMRU.remove(av);
        avs.remove(av);
        avsNomads.remove(av);
    }

    /**
     * Quietly add the app view to the END of the right lists.
     * This makes the
     * editor available to certain commands. If the app view is already in the
     * lists then this is a nop.
     * 
     * FROM: NbAppView, PlayEditorContainer
     *
     * @param av app view to add.
     * @param tag used in debug output, may be null
     */
    public static void open(ViAppView av, String tag)
    {
        if(tag == null)
            tag = "";
        Scheduler.register(av.getEditor());
        boolean nothingToDo = false;
        if (av.isNomad()) {
            if (avsNomads.contains(av))
                nothingToDo = true;
        } else
            if (avsMRU.contains(av))
                nothingToDo = true;
        if (nothingToDo)
            return;
        Component ed = av.getEditor();
        if (fact() != null) {
            // the av is added to the end of the MRU
            String tagF = tag;
            G.dbgEditorActivation().println(() -> "Activation: AppViews.open: "
                    + tagF + " " + ViManager.cid(ed) + " " + ViManager.cid(av)
                    + " " + fact().getFS().getDisplayPath(av));
        }
        if (!av.isNomad()) {
            if (!avsMRU.contains(av))
                avsMRU.add(av);
            if (!avs.contains(av))
                avs.add(av);
            avsNomads.remove(av);
        }
        else {
            // insure nomads not in these lists
            avsMRU.remove(av);
            avs.remove(av);
            // and make sure it is in the nomad list
            // Don't need to check contained, since working with a Set.
            avsNomads.add(av);
        }
    }

    private static void adjustMru(ViAppView av)
    {
        if (!av.isNomad()) {
            // this used to be the default case
            avsMRU.remove(av);
            avsMRU.add(0, av);
            if (!avs.contains(av))
                avs.add(av);
            avsNomads.remove(av);
        }
        else {
            // insure nomads not in these lists
            avsMRU.remove(av);
            avs.remove(av);
            // and make sure it is in the nomad list
            // Don't need to check contained, since working with a Set.
            avsNomads.add(av);
        }
    }

    /**
     * A shallow copy of the specified list.
     * 
     * FROM: jVi all over
     * 
     * @param which the list to copy
     * @return
     */
    public static List<ViAppView> getList(AppViews which)
    {
        List<ViAppView> l = null;
        switch (which) {
            case ACTIVE:
                l = new ArrayList<>(avs);
                break;
            case MRU:
                l = new ArrayList<>(avsMRU);
                break;
            case NOMAD:
                l = new ArrayList<>(avsNomads);
                break;
            case ALL:
                l = new ArrayList<>();
                l.addAll(getList(ACTIVE));
                l.addAll(getList(NOMAD));
                break;
        }
        return l;
    }

    /**
     * determine the index in the list of the current app view.
     * 
     * FROM: Misc01
     * 
     * @param avs
     * @return -1 if current not in list, else the index
     */
    public static int indexOfCurrentAppView(List<ViAppView> avs)
    {
        int idx = -1;
        int i = 0;
        for (ViAppView av : avs) {
            if(av.getEditor() != null
                    && av.getEditor().equals(Scheduler.getCurrentEditor())
                || av.getEditor() == null
                    && av.equals(avCurrentlyActive)) {
                idx = i;
                break;
            }
            i++;
        }
        return idx;
    }

    /** FROM: NOT USED */
    public static ViAppView currentAppView(List<ViAppView> avs)
    {
        for (ViAppView av : avs) {
            if(av.getEditor() != null
                    && av.getEditor().equals(Scheduler.getCurrentEditor())
                || av.getEditor() == null
                    && av.equals(avCurrentlyActive)) {
                return av;
            }
        }
        return null;
    }

    /**
     * A convenience method to easily support '%', '#',
     * '#&lt;digits&gt;', '#-&lt;digits&gt;'; 0 is the current av,
     * greater than 0 is buffer number, less than 0 is MRU.
     * @param id
     * @return null if not found, else the indicated appview
     */
    public static ViAppView getAppView(int id)
    {
        ViAppView av = null;
        if(id == 0)
            av = relativeMruAppView(0);
        else if(id > 0) {
            for (ViAppView av1 : avs) {
                if(id == av1.getWinID()) {
                    av = av1;
                    break;
                }
            }
        } else {
            av = getMruAppView(-id);
        }
        
        return av;
    }

    /**
     * Fetch the Nth buffer, 0 to N-1, from the Mru list.
     * 
     * FROM: Misc01, Cc01, Util, NbTextView
     * 
     * @return the buffer, else null if i is out of bounds.
     */
    public static ViAppView getMruAppView(int i)
    {
        if (i < 0 || i >= avsMRU.size())
            return null;
        return avsMRU.get(i);
    }

    /**
     * Return the Ith next/previous AppView relative to the argument
     * AppView. If i &lt 0 then look in previously used direction.
     */
    private static ViAppView relativeMruAppView(ViAppView av, int i)
    {
        if (fact() != null) {
            G.dbgEditorActivation().println(() ->
                    "Activation: AppViews.relativeMruAppView: "
                            + fact().getFS().getDisplayPath(av));
        }
        if (avsMRU.isEmpty())
            return null;
        int idx = avsMRU.indexOf(av);
        if (idx < 0)
            return null;
        idx += -i;
        if (idx < 0)
            idx = 0;
        else if (idx >= avsMRU.size())
            idx = avsMRU.size() - 1;
        return avsMRU.get(idx);
    }

    /** FROM: Cc01 */
    // WHY IS relativeMruAppView(0) used for '%'?
    public static ViAppView relativeMruAppView(int i)
    {
        return relativeMruAppView(avCurrentlyActive, i);
    }

    /**
     * Request that the next activation does not re-order the mru list if the
     * activated object is the argument.
     * 
     * FROM: Cc01
     * Sounds like a kludge
     */
    public static void keepMruAfterActivation(ViAppView av)
    {
        if (!avs.contains(av))
            return; // can't ignore if its not in the list
        keepMru = av;
    }

    /** FROM: NOT USED */
    public static boolean isKnown(ViAppView av)
    {
        return avs.contains(av);
    }

    /**
     * The window upper-left to lower-right sort. If the ViApView
     * implement comparable, then that is used for the sort order.
     * 
     * FROM: Misc01
     * 
     * @param avs
     */
    public static void sortAppView(List<ViAppView> avs)
    {
        List<ViAppView> avs01 = ViManager.getFactory()
                .getWindowNavigator(avs).getList();
        avs.clear();
        avs.addAll(avs01);
    }

    private static Point getLocation(ViAppView av)
    {
        if(av == null)
            return null;
        Point p;
        if(av.getEditor() != null) {
            p = av.getEditor().getLocationOnScreen();
        } else
            p = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        return p;
    }

    private static ViFactory fact()
    {
        return ViManager.getFactory();
    }

    // Is the factory loaded
    private static boolean hasFact()
    {
        return ViManager.isFactoryLoaded();
    }

    /** FROM: NB-Module, :dumpJvi */
    public static StringBuilder dump(StringBuilder ps)
    {
        if(ps == null)
            ps = new StringBuilder(200);
        ps.append("-----------------------------------").append('\n');
        ps.append("currentEditorPane = ")
          .append(G.curwin() == null
                  ? "null"
                  : G.curwin().getBuffer().getDebugFileName()).append('\n');
        ps.append("factory = ").append(fact()).append('\n');
        ps.append("AppViews: ").append(avs.size()).append('\n');
        for (ViAppView av : avs) {
            ps.append("\t").append(fact().getFS().getDisplayPath(av))
              .append(", ").append(av.getClass().getSimpleName()).append('\n');
        }
        ps.append("AppViewsMRU: ").append(avsMRU.size()).append('\n');
        for (ViAppView av : avsMRU) {
            ps.append("\t").append(fact().getFS().getDisplayPath(av))
              .append(", ").append(av.getClass().getSimpleName()).append('\n');
        }
        ps.append("currentlyActive: ");
        if(avCurrentlyActive == null)
            ps.append("none");
        else
            ps.append("")
              .append(fact().getFS().getDisplayPath(avCurrentlyActive))
              .append(", ").append(avCurrentlyActive.getClass().getSimpleName());
        ps.append('\n');
        ps.append("keepMru: ");
        if(keepMru == null)
            ps.append("none");
        else
            ps.append("").append(fact().getFS().getDisplayPath(keepMru))
              .append(", ").append(keepMru.getClass().getSimpleName());
        ps.append('\n');
        Set<ViTextView> tvSet = fact().getViTextViewSet();
        ps.append("TextViewSet: ").append(tvSet.size()).append('\n');
        for (ViTextView tv : tvSet) {
            ps.append("\t").append(tv.getBuffer().getDebugFileName()).append('\n');
        }
        Set<Buffer> bufSet = fact().getBufferSet();
        ps.append("BufferSet: ").append(bufSet.size()).append('\n');
        for (Buffer buf : bufSet) {
            if (buf == null)
                ps.append("null-buf").append('\n');
            else
                ps.append("\t").append(buf.getDebugFileName())
                  .append(", share: ").append(buf.getShare()).append('\n');
        }
        ps.append("AppViewNomads: ").append(avsNomads.size()).append('\n');
        for(ViAppView av : avsNomads) {
            ps.append("\t").append(fact().getFS().getDisplayPath(av))
              .append(", ").append(av.getClass().getSimpleName()).append('\n');
        }
        return ps;
    }

}
