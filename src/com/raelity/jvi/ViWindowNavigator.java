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
package com.raelity.jvi;

import com.raelity.jvi.ViTextView.Direction;
import com.raelity.jvi.ViTextView.Orientation;
import java.awt.Component;
import java.util.List;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public interface ViWindowNavigator
{
    /**
     * The windows are sorted top-to-bottom then left-to-right.
     *
     * Notice that with multiple screens this sorting finishes a screen,
     * top-to-bottom, before moving to the next screen on the right.
     *
     * @return app views in order for sequential Ctrl-W traversal
     */
    public List<ViAppView> getList();

    /**
     * Select an AppView as the target for Ctrl-W window motion commands
     * in the specified direction. When there are multiple choices, select
     * the one closest to the cursor in the fromAv window.
     *
     * @param dir
     * @param fromAv
     * @param n to go multiple windows in the direction
     * @param mustTouch true means the target must be touching,
     *        false means target could be off to the side
     * @return target, null if no target satisfies contraints
     */
    public ViAppView getTarget(Direction dir, ViAppView fromAv,
                               int n, boolean mustTouch);

    public ViAppView getTarget(Direction dir, ViAppView fromAv, int n);

    public SplitterNode getParentSplitter(ViAppView av);

    /**
     * If the parent splitter is not the expected orientation or there is
     * no parent splitter, then return a dummy splitter that is suitable
     * for calculating sizes in the specified orientation.
     */
    public SplitterNode getParentSplitter(ViAppView av, Orientation orientation);

    /**
     * The root splitter should only be used to calculate
     * target weight.
     * <p/>
     * The children should not be used (at least not yet)
     */
    public SplitterNode getRootSplitter(ViAppView av, Orientation orientation);

    public interface SplitterNode {
        int getTargetIndex();
        int getChildCount();
        /** children are either splitters or editors */
        Component[] getChildren();
        Component getComponent();
        Orientation getOrientation();
    }

}
