/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2022 Ernie Rael.  All Rights Reserved.
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

package com.raelity.jvi.swing;

import java.awt.Point;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author err
 */
public class SwingUtil
{
private SwingUtil() {}

public static Point point(double x, double y) {
    Point pt = new Point();
    pt.setLocation(x, y);
    return pt;
}

public static Point location(Rectangle2D r2d) {
    return point(r2d.getX(), r2d.getY());
}
}
