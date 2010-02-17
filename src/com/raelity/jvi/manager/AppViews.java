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

import com.raelity.jvi.ViAppView;
import com.raelity.jvi.ViFactory;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.G;
import java.awt.Component;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * These static methods are used by the platform to inform jVi of the state
 * of editors, for example activated or closed, and to query about opened
 * editors.
 *
 * jVi maintains two lists of opened editors: the order they opened and a
 * MRU (MostRecentlyUsed) list.
 *
 * Even when jVi is disabled these methods should be used so that new editors
 * are properly handled.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public enum AppViews
{
    ACTIVE,
    MRU,
    NOMAD;

    private static List<ViAppView> textBuffers =
            new ArrayList<ViAppView>();
    private static List<ViAppView> textMRU =
            new ArrayList<ViAppView>();
    private static Set<WeakAppView> textNomads =
            new LinkedHashSet<WeakAppView>();
    private static ViAppView currentlyActiveAppView;
    private static ViAppView keepMru;

    /**
     * Return a two element array of objects.
     * First element is list of AppView, if showingOnly is true
     * then only a list of user visible AppView.
     * Second element is index of currently active top component, or -1.
     * list of editors ordered as from getTextBuffer.
     */
    public static Object[] get(AppViews which, boolean showingOnly)
    {
        Iterator<ViAppView> iter = null;
        switch (which) {
            case ACTIVE:
                iter = getTextBufferIterator();
                break;
            case MRU:
                iter = getMruBufferIterator();
            case NOMAD:
                iter = getNomadBufferIterator();
        }
        List<ViAppView> l = new ArrayList();
        int idx = -1;
        while (iter.hasNext()) {
            ViAppView av = iter.next();
            if (!showingOnly || av.getEditor().isShowing()) {
                l.add(av);
                if (av.getEditor().equals(Scheduler.getCurrentEditor()))
                    idx = l.size() - 1; // index of the current/active window
            }
        }
        Object[] o = new Object[]{l, idx};
        return o;
    }

    private static void adjustMru(ViAppView av)
    {
        if (!av.isNomad()) {
            // this used to be the default case
            textMRU.remove(av);
            textMRU.add(0, av);
            if (!textBuffers.contains(av))
                textBuffers.add(av);
            textNomads.remove(new WeakAppView(av));
        }
        else {
            // insure nomads not in these lists
            textMRU.remove(av);
            textBuffers.remove(av);
            // and make sure it is in the nomad list
            // Don't need to check contained, since working with a Set.
            textNomads.add(new WeakAppView(av));
        }
    }

    /**
     * Quietly add the app view to the END of the right lists. Typically this
     * will be a nomad or perhaps something with editors in it that are
     * not considered an editor (for example a diff window).
     *
     * @param av
     * @param tag
     */
    public static void register(ViAppView av, String tag)
    {
        boolean nothingToDo = false;
        if (av.isNomad()) {
            if (textNomads.contains(new WeakAppView(av)))
                nothingToDo = true;
        } else
            if (textMRU.contains(av))
                nothingToDo = true;
        if (nothingToDo)
            return;
        Component ed = av.getEditor();
        if (fact() != null && G.dbgEditorActivation.getBoolean())
            System.err.println("Activation: AppViews.register: " + tag +
                    " " + ViManager.cid(ed) + " " + ViManager.cid(av) + " " +
                    fact().getFS().getDisplayFileName(av)); // the av is added to the end of the MRU
        if (!av.isNomad()) {
            if (!textMRU.contains(av))
                textMRU.add(av);
            if (!textBuffers.contains(av))
                textBuffers.add(av);
            textNomads.remove(new WeakAppView(av));
        }
        else {
            // insure nomads not in these lists
            textMRU.remove(av);
            textBuffers.remove(av);
            // and make sure it is in the nomad list
            // Don't need to check contained, since working with a Set.
            textNomads.add(new WeakAppView(av));
        }
    }

    /**
     * Fetch the text buffer indicated by the argument. The argument is
     * positive, fetch the Nth buffer, numbered 1 to N, according to
     * the order they were activated.
     * See {@link #getMruBuffer}.
     * @return the buffer or null if i does not specify an active buffer.
     */
    public static ViAppView getTextBuffer(int i)
    {
        i = i - 1; // put in range 0 - (N-1)
        if (i < 0 || i >= textBuffers.size())
            return null;
        return textBuffers.get(i);
    }

    public static Iterator<ViAppView> getTextBufferIterator()
    {
        return textBuffers.iterator();
    }

    public static Iterator<ViAppView> getMruBufferIterator()
    {
        return textMRU.iterator();
    }

    /**
     * Fetch the Nth buffer, 0 to N-1, from the Mru list.
     * @return the buffer, else null if i is out of bounds.
     */
    public static ViAppView getMruBuffer(int i)
    {
        if (i < 0 || i >= textMRU.size())
            return null;
        return textMRU.get(i);
    }

    public static Iterator<ViAppView> getNomadBufferIterator()
    {
        //return textNomads.iterator();
        // While this is iterated, null elements are tossed out.
        // remove is not implemented.
        final Iterator<WeakAppView> iter = textNomads.iterator();
        return new Iterator<ViAppView>() {

            ViAppView nextAppView;

            // Find the next non-null object, removing nulls
            private void findNextAppView()
            {
                if (nextAppView != null)
                    return;
                WeakAppView wo;
                while (iter.hasNext()) {
                    wo = iter.next();
                    nextAppView = wo.get();
                    if (nextAppView != null)
                        break;
                    iter.remove();
                }
            }

            public boolean hasNext()
            {
                if (nextAppView != null)
                    return true;
                findNextAppView();
                return nextAppView != null;
            }

            public ViAppView next()
            {
                findNextAppView();
                if (nextAppView == null)
                    throw new NoSuchElementException();
                ViAppView av = nextAppView;
                nextAppView = null;
                return av;
            }

            public void remove()
            {
                // iter.remove() here probably works fine
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Return the Ith next/previous AppView relative to the argument
     * AppView. If i &lt 0 then look in previously used direction.
     */
    private static ViAppView relativeMruBuffer(ViAppView av, int i)
    {
        if (fact() != null && G.dbgEditorActivation.getBoolean())
            System.err.println("Activation: AppViews.relativeMruBuffer: " +
                    fact().getFS().getDisplayFileName(av));
        if (textMRU.size() == 0)
            return null;
        int idx = textMRU.indexOf(av);
        if (idx < 0)
            return null;
        idx += -i;
        if (idx < 0)
            idx = 0;
        else if (idx >= textMRU.size())
            idx = textMRU.size() - 1;
        return textMRU.get(idx);
    }

    public static ViAppView relativeMruBuffer(int i)
    {
        return relativeMruBuffer(currentlyActiveAppView, i);
    }

    /**
     * Request that the next activation does not re-order the mru list if the
     * activated object is the argment.
     */
    public static void keepMruAfterActivation(ViAppView av)
    {
        if (!textBuffers.contains(av))
            return; // can't ignore if its not in the list
        keepMru = av;
    }

    /**
     * The application invokes this whenever a file becomes selected
     * in the specified container. This also serves as an open.
     * @param appView AppView that is getting focus for editing.
     * Editor may be null, may be a nomad.
     * @param tag String used in debug messages.
     */
    public static void activate(ViAppView av, String tag)
    {
        Component ed = av.getEditor();
        if (fact() != null && G.dbgEditorActivation.getBoolean())
            System.err.println("Activation: AppViews.activate: " + tag +
                    " " + ViManager.cid(ed) + " " + ViManager.cid(av) + " " +
                    fact().getFS().getDisplayFileName(av));
        if (ed != null && hasFact())
            fact().setupCaret(ed);
        if (G.curwin != null)
            G.curwin.getStatusDisplay().refresh();
        ViAppView keep = keepMru;
        keepMru = null;
        currentlyActiveAppView = av;
        if (textBuffers.contains(keep) && av.equals(keep))
            return; // return without adjusting mru
        adjustMru(av);
    }

    /**
     * The specified appView is loosing focus.
     * The associated jVi state is put into normal mode.
     * @param av appView loosing focus.
     */
    public static void deactivateCurrent(ViAppView av)
    {
        if (fact() != null && G.dbgEditorActivation.getBoolean())
            System.err.println("Activation: AppViews.deactivateCurent: " +
                    " " + ViManager.cid(av) + fact().getFS().getDisplayFileName(av));
        if (hasFact()) {
            ViManager.exitInputMode();
            G.curwin.getStatusDisplay().clearDisplay();
        }
        currentlyActiveAppView = null;
    }

    public static boolean isKnown(ViAppView av)
    {
        return textBuffers.contains(av);
    }

    /**
     * The applications invokes this method when a file is completely
     * removed from a container or should be forgotten by jVi.
     */
    public static void close(ViAppView av)
    {
        Component ed = av.getEditor();
        if (fact() != null && G.dbgEditorActivation.getBoolean()) {
            String fname = fact().getFS().getDisplayFileName(av);
            System.err.println("Activation: AppViews.close: " +
                    (ed == null ? "(no shutdown) " : "") + fname);
        }
        ViTextView tv = fact().getTextView(ed);
        if (tv != null) {
            ViManager.firePropertyChange(ViManager.P_CLOSE_WIN, tv, null);
            if (tv.getBuffer().singleShare())
                ViManager.firePropertyChange(ViManager.P_CLOSE_BUF, tv.getBuffer(), null);
        }
        assert (hasFact());
        if (hasFact() && ed != null)
            fact().shutdown(ed);
        if (av.equals(currentlyActiveAppView))
            currentlyActiveAppView = null;
        textMRU.remove(av);
        textBuffers.remove(av);
        textNomads.remove(new WeakAppView(av));
    }

    private static class BuffersList
    {

        List<WeakReference> l = new ArrayList();
    }

    private static class WeakAppView extends WeakReference<ViAppView>
    {

        public WeakAppView(ViAppView referent)
        {
            super(referent);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof WeakAppView))
                return false;
            ViAppView o = get();
            ViAppView other = ((WeakAppView)obj).get();
            return o == null ? o == obj : o.equals(other);
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            Object o = get();
            if (o != null)
                hash = o.hashCode();
            return hash;
        }
    }

    private static ViFactory fact()
    {
        return ViManager.getViFactory();
    }

    // Is the factory loaded
    private static boolean hasFact()
    {
        return ViManager.isFactoryLoaded();
    }

    public static void dump(PrintStream ps)
    {
        ps.println("-----------------------------------");
        ps.println("currentEditorPane = " +
                (G.curwin == null ? "null"
                : G.curwin.getBuffer().getDisplayFileName()));
        ps.println("factory = " + fact());
        ps.println("textBuffers: " + textBuffers.size());
        for (ViAppView av : textBuffers) {
            ps.println("\t" + fact().getFS().getDisplayFileName(av) + ", " +
                    av.getClass().getSimpleName());
        }
        ps.println("textMRU: " + textMRU.size());
        for (ViAppView av : textMRU) {
            ps.println("\t" + fact().getFS().getDisplayFileName(av) + ", " +
                    av.getClass().getSimpleName());
        }
        ps.println("currentlyActive: " +
                (currentlyActiveAppView == null ? "none"
                : "" + fact().getFS().
                getDisplayFileName(currentlyActiveAppView) + ", " +
                currentlyActiveAppView.getClass().getSimpleName()));
        ps.println("keepMru: " +
                (keepMru == null ? "none"
                : "" + fact().getFS().getDisplayFileName(keepMru) + ", " +
                keepMru.getClass().getSimpleName()));
        Set<ViTextView> tvSet = fact().getViTextViewSet();
        ps.println("TextViewSet: " + tvSet.size());
        for (ViTextView tv : tvSet) {
            ps.println("\t" + tv.getBuffer().getDisplayFileName());
        }
        Set<Buffer> bufSet = fact().getBufferSet();
        ps.println("BufferSet: " + bufSet.size());
        for (Buffer buf : bufSet) {
            if (buf == null)
                ps.println("null-buf");
            else
                ps.println("\t" + fact().getFS().getDisplayFileName(buf) +
                        ", share: " + buf.getShare());
        }
        ps.println("textNomads: " + textNomads.size());
        Iterator<ViAppView> iter = getNomadBufferIterator();
        while (iter.hasNext()) {
            ViAppView av = iter.next();
            ps.println("\t" + fact().getFS().getDisplayFileName(av) + ", " +
                    av.getClass().getSimpleName());
        }
    }

}
