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

package com.raelity.jvi.core;

import com.raelity.jvi.ViAppView;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The vim algorithm for traversing windows with the ^W_^W style commands is
 * based on the way that the windows are split. Here we build a tree that
 * reflects how the windows are split. A GUI component tree is traversed to
 * build a simple tree that only has splitter and editor nodes.
 *
 * A complication is that there may be multiple independent trees, where each
 * tree is rooted in its own java Window/Frame.
 *
 * The resulting tree's must be binary trees. For example a JPanel with
 * a grid of 4 editors would be a node with four children, the platform
 * specific code will have to break this down in to some binary tree.
 *
 * @author Ernie Rael <err at raelity.com>
 */
public abstract class WindowTreeBuilder {
    private Set<ViAppView> toDo = new HashSet<ViAppView>();
    private List<ViAppView> sorted = new ArrayList<ViAppView>();
    private List<Node> roots = new ArrayList<Node>();

    public WindowTreeBuilder(List<ViAppView> avs)
    {
        toDo.addAll(avs);
    }

    /**
     * This only does something the first timeit is called. Subsequent
     * calls return the same stuff.
     * @return app views in order for Ctrl-W traversal
     */
    public List<ViAppView> processAppViews()
    {
        while(!toDo.isEmpty()) {
            Component c = windowForAppView(toDo.iterator().next());
            //allComps(c);
            Node root = buildTree(c);
            //dumpTree(root, 0);
            assert root != null;
            if(root == null)
                break;
            roots.add(root);
        }

        assert toDo.isEmpty();

        // NEEDSWORK: sort roots by screen location

        for (Node root : roots) {
            addToSorted(root);
        }

        return Collections.unmodifiableList(sorted);
    }

    private void dumpTree(Node n, int depth)
    {
        //String formatString = String.format("%%"+((depth+1)*4)+"s%%s\\n", "");
        //System.err.format(formatString, "", n);
        String shift = String.format("%"+((depth+1)*4)+"s", "");
        System.err.println(shift + n);
        for (Node child : n.getChildren()) {
            dumpTree(child, depth+1);
        }
    }

    private void addToSorted(Node n)
    {
        if(n.isEditor()) {
            sorted.add(getAppView(n.getPeer()));
        } else {
            for (Node n01 : n.children) {
                addToSorted(n01);
            }
        }
    }

    private Node buildTree(Component c)
    {
        if(isEditor(c)) {
            removeFromToDo(c);
            return createEditorNode(c);
        }

        Node child = null;
        List<Node> ns = null; // only create if more than one child
        if(c instanceof Container) {
            Component components[] = ((Container)c).getComponents();
            for (int i = 0; i < components.length; i++) {
                Node n01 = buildTree(components[i]);
                if(n01 != null) {
                    // avoid creating the array if possible
                    if(child == null && ns == null)
                        child = n01;
                    else {
                        // There's more than one child.
                        // Note order in component is maintained
                        if(ns == null)
                            ns = new ArrayList<Node>(2);
                        if(child != null) {
                            ns.add(child);
                            child = null;
                        }
                        ns.add(n01);
                    }
                }
            }
        }

        assert child == null && ns == null || child == null || ns == null;
        return child != null ? child
                : ns != null ? createSplitNode(c, ns) : null;
    }

    /**
     * sort the roots top-to-bottom then left-to-right.
     *
     * Notice that with multiple screens this sorting finishes a screen,
     * top-to-bottom, before moving to the next screen on the right.
     */
    private void sortRoots()
    {
        Collections.sort(roots, new CompareNodeLocations());
    }

    protected Point getLocation(Node n)
    {
        return n.getPeer().getLocationOnScreen();
    }

    protected class CompareNodeLocations implements Comparator<Node>
    {
        public int compare(Node n1, Node n2)
        {
            Point w1 = getLocation(n1);
            Point w2 = getLocation(n2);

            int rv;
            if(w1.x != w2.x)
                rv = w1.x - w2.x;
            else
                rv = w1.y - w2.y;
            // System.err.format("Comp rv %d\n    %s%s\n    %s%s\n",
            //         rv, this, w1, o, w2);
            return rv;
        }
    }

    private Component allComps(Component c)
    {
        System.err.println("findNode check: " + c.getClass().getSimpleName());
        if(c.getClass().getSimpleName().equals("MultiSplitPane"))
            System.err.println("multisplitpane");
        if(isEditor(c))
            System.err.println("FOUND ONE");//return c;
        if(c instanceof Container) {
            Component components[] = ((Container)c).getComponents();
            for (int i = 0; i < components.length; i++) {
                Component c01 = allComps(components[i]);
                if(c01 != null)
                    return c01;
            }
        }
        return null;
    }

    protected abstract Component windowForAppView(ViAppView av);

    protected boolean removeFromToDo(ViAppView av)
    {
        boolean f = toDo.remove(av);
        assert f;
        return f;
    }

    protected boolean isToDo(ViAppView av)
    {
        return toDo.contains(av);
    }

    /**
     * Take something out of the todo list
     * @param c
     */
    protected abstract boolean removeFromToDo(Component c);

    /**
     * Used on the input component tree elements.
     * @param c the component to test
     * @return true if the node is an editor and in the toDo list
     */
    protected abstract boolean isEditor(Component c);

    /**
     * Determine the AppView associated with the node. If the node is not
     * an editor then this will return null.
     * @param n the node of interest
     * @return the app view associated with the node
     */
    protected abstract ViAppView getAppView(Component c);

    protected Node createEditorNode(Component peer)
    {
        return new Node(peer);
    }

    protected Node createSplitNode(Component peer, List<Node> children)
    {
        return new Node(peer, children);
    }

    protected static class Node {
        private boolean isEditor;
        private boolean isLeftRight;
        private Component peer;
        private List<Node> children;

        protected Node(Component peer)
        {
            this.peer = peer;
            isEditor = true;
        }

        protected Node(Component peer, List<Node> children)
        {
            this.peer = peer;
            this.children = children;
            adjustNode();
        }

        protected void adjustNode()
        {
            if(children != null && children.size() >= 2)
                isLeftRight = calcIsLeftRightChildren(
                        children.get(0), children.get(1));
        }

        public boolean isEditor()
        {
            return isEditor;
        }

        public boolean isSplitter()
        {
            return !isEditor;
        }

        public boolean isLeftRight()
        {
            return isLeftRight;
        }

        public Component getPeer()
        {
            return peer;
        }

        public List<Node> getChildren()
        {
            return children == null ? Collections.EMPTY_LIST
                : Collections.unmodifiableList(children);
        }

        @Override
        public String toString()
        {
            String s = peer.getClass().getSimpleName();
            return isEditor() ? "editor: " + s
                              : "split:  " + s
                                + (isLeftRight() ? " LeftRight" : " TopBottom");
        }

        public static boolean calcIsLeftRightChildren(
                Node n01, Node n02)
        {
            Point p1 = n01.getPeer().getLocationOnScreen();
            Point p2 = n02.getPeer().getLocationOnScreen();
            int dX = Math.abs(p1.x - p2.x);
            int dY = Math.abs(p1.y - p2.y);
            return dX > dY;
        }

    }
}
