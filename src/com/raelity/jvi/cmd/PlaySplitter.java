/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2019 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi.cmd;

import com.raelity.jvi.ViTextView.Direction;
import com.raelity.jvi.ViTextView.Orientation;
import java.awt.Component;
import javax.swing.JSplitPane;

/**
 * NOTE: anywhere an editorContainer is mentioned,
 * it could also be a splitter.
 * @author err
 */
public class PlaySplitter extends JSplitPane
{

public PlaySplitter(Orientation orientation)
{
    this(orientation, null, null);
}

@SuppressWarnings("OverridableMethodCallInConstructor")
public PlaySplitter(Orientation orientation,
        PlayEditorContainer edC1, PlayEditorContainer edC2)
{
    super(orientation == Orientation.LEFT_RIGHT
            ? HORIZONTAL_SPLIT : VERTICAL_SPLIT,
            edC1, edC2);
    setResizeWeight(0.5);
}

Direction getPosition(Component edC)
{
    int i = getLeftComponent() == edC ? 1
            : getRightComponent() == edC ? -1
            : 0;
    assert i != 0;
    return getOrientation() == HORIZONTAL_SPLIT
            ? (i > 0 ? Direction.LEFT : Direction.RIGHT)
            : (i > 0 ? Direction.UP : Direction.DOWN);
}

public Component getEditorContainer(Direction dir)
{
    return dir == Direction.LEFT || dir == Direction.UP
            ? getLeftComponent() : getRightComponent();
}

public Component getOther(Component edC)
{
    return leftComponent == edC ? rightComponent : leftComponent;
}
 
public void putEditorContainer(Component edC, Direction dir) {
    // adding either an editorContainer or a splitter
    addImpl(edC, dir.getSplitSide(), -1);
}

public void replaceEditorContainer(Component oldC, Component newC)
{
    if(leftComponent == oldC)
        setLeftComponent(newC);
    else if(rightComponent == oldC)
        setRightComponent(newC);
    else
        assert 0 == 1;
}
}
