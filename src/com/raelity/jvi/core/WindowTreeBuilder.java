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
import java.util.EnumSet;
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

    private boolean dbg = false;

    public enum Direction { LEFT, RIGHT, UP, DOWN }
    public enum Orientation { LEFT_RIGHT, UP_DOWN }

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
            if(dbg)dumpTree(root);
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

    public ViAppView jump(Direction dir, final ViAppView fromAv, int n)
    {
        if(n <= 0)
            n = 1;

        // get the node corresponding to the current app view
        Visitor v = new Visitor()
        {
            @Override
            void visit(Node node)
            {
                if(node.isEditor() && fromAv == getAppView(node.getPeer())) {
                    foundNode = node;
                    finished = true;
                }
            }
        };

        for (Node root : roots) {
            traverse(root, v);
            if(v.finished())
                break;
        }
        Node currentNode = v.foundNode;
        if(currentNode == null)
            return null;

        Node targetNode = null;
        do
        {
            currentNode = jump(dir, currentNode);
            if(currentNode != null)
                targetNode = currentNode;
            if(dbg)System.err.println("windowJump: " + dbgName(currentNode));
        } while(currentNode != null && --n > 0);

        if(targetNode == null)
            return null;
        return getAppView(targetNode.getPeer());
    }

    private void dumpTree(Node n)
    {
        traverse(n, new Visitor()
        {
            @Override
            void visit(Node node)
            {
                String shift = String.format("%"+((depth+1)*4)+"s", "");
                System.err.println(shift + node);
            }
        });
    }

    private void addToSorted(Node n)
    {
        traverse(n, new Visitor()
        {
            @Override
            void visit(Node node)
            {
                if(node.isEditor())
                    sorted.add(getAppView(node.getPeer()));
            }
        });
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

    /** basically a preorder traversal */
    private void traverse(Node n, Visitor v)
    {
        v.visit(n);
        if(v.finished())
            return;
        ++v.depth;
        for (Node child : n.getChildren()) {
            traverse(child, v);
            if(v.finished())
                return;
        }
        --v.depth;
    }

    private static abstract class Visitor
    {
        int depth;
        boolean finished;
        Node foundNode; // hack

        abstract void visit(Node node);

        boolean finished() // to signal early termination
        {
            return finished;
        }
    }

    protected static class Node
    {
        private boolean isEditor;
        private Orientation orientation;
        private Component peer;
        Node parent;
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
            for (Node child : children) {
                child.parent = this;
            }
        }

        protected void adjustNode()
        {
            if(children != null && children.size() >= 2)
                orientation = calcOrientation(
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

        public Orientation getOrientation()
        {
            return orientation;
        }

        public Component getPeer()
        {
            return peer;
        }

        public Node getParent()
        {
            return parent;
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
                                + (getOrientation() == Orientation.LEFT_RIGHT
                                    ? " LeftRight" : " TopBottom");
        }

        public static Orientation calcOrientation(
                Node n01, Node n02)
        {
            Point p1 = n01.getPeer().getLocationOnScreen();
            Point p2 = n02.getPeer().getLocationOnScreen();
            int dX = Math.abs(p1.x - p2.x);
            int dY = Math.abs(p1.y - p2.y);
            return dX > dY ? Orientation.LEFT_RIGHT : Orientation.UP_DOWN;
        }

    }

    private Node jump(Direction dir, Node from)
    {
        Node to = treeUpFindSiblingNodeForJump(dir, from);
        if(to == null)
            return null;
        to = treeDownFindJumpTarget(dir, to);
        assert to != null;
        return to;
    }

    /**
     * Move up the tree, following parent link, looking for a targetNode where
     * we can start going down the tree to get the target window in the
     * direction.
     * @param dir
     * @param targetNode
     * @return
     */
    private Node treeUpFindSiblingNodeForJump(Direction dir, Node node)
    {
        Node found = null;
        while(true) {
            Node parent = node.getParent();
            if(parent == null)
                break;
            if(dbg)System.err.println("treeUp: " + dbgName(node));
            if((found = pickSiblingForJump(dir, node)) != null)
                break;
            node = parent;
        }
        if(dbg)System.err.println("treeUp found: " + dbgName(found));
        return found;
    }
    private String dbgName(Node n)
    {
        String s;
        if(n == null)
            s = null;
        else if(n.isEditor()) {
            s = getAppView(n.getPeer()).toString();
        } else {
            s = n.getPeer().getClass().getSimpleName();
        }
        return s;
    }

    /**
     * Used while moving up the tree to get a sibling that can start the
     * way down the tree to the target window. The parent targetNode must be
     * oriented in the goal direction and there must be a sibling in the
     * goal direction.
     * @param dir
     * @param targetNode
     * @return sibling that satisfies the directional requirement or null
     */
    private Node pickSiblingForJump(Direction dir, Node child)
    {
        Node parent = child.getParent();
        if(parent == null)
            return null;
        if(parent.getOrientation() != orientation(dir))
            return null;
        List<Node> children = parent.getChildren();
        int idx = children.indexOf(child);
        idx += (towardsFirst(dir) ? -1 : 1);
        if(idx < 0 || idx >= children.size())
            return null;
        return children.get(idx);
    }

    /**
     * Traverse the tree down to find the target window. Notice that the
     * target window is kind of the "opposite" direction of the jump within
     * the window targetNode we are jumping into. For example, if jump UP then
     * in the group above pick the DOWN window.
     * @param dir
     * @param targetNode
     * @return
     */
    private Node treeDownFindJumpTarget(Direction dir, Node node)
    {
        EnumSet<Direction> dirs = null;
        switch(dir) {
            case UP:
                dirs = EnumSet.of(Direction.LEFT, Direction.DOWN);
                break;
            case DOWN:
                dirs = EnumSet.of(Direction.LEFT, Direction.UP);
                break;
            case LEFT:
                dirs = EnumSet.of(Direction.RIGHT, Direction.UP);
                break;
            case RIGHT:
                dirs = EnumSet.of(Direction.LEFT, Direction.UP);
                break;
        }

        while(!node.isEditor()) {
            node = pickNodeForJumpDirection(dirs, node);
            if(dbg)System.err.println("treeDown: " + dbgName(node));
        }
        return node;
    }

    private Node pickNodeForJumpDirection(EnumSet<Direction> dirs, Node node)
    {
        for (Direction dir : dirs) {
            Orientation orientation = orientation(dir);
            if(node.getOrientation() != orientation)
                continue;
            List<Node> children = node.getChildren();
            return towardsFirst(dir) ? children.get(0)
                                     : children.get(children.size()-1);
        }
        return null;
    }

    private Orientation orientation(Direction dir)
    {
        switch (dir) {
            case LEFT:
            case RIGHT:
                return Orientation.LEFT_RIGHT;
            case UP:
            case DOWN:
            default: // to keep compiler happy
                return Orientation.UP_DOWN;
        }
    }

    /**
     * Return if target is towards the front or back of an ordered array.
     * @param dir
     * @return true if dir is LEFT or UP, false if RIGHT, DOWN
     */
    private boolean towardsFirst(Direction dir)
    {
        return dir == Direction.LEFT || dir == Direction.UP ? true : false;
    }
}
