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

package com.raelity.jvi.cmd;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

/**
 * Support methods for multiscreen environments that operate on the
 * preferred screen.
 * The default screen
 * can be specified by the environment variable: "JAVA_PREFERRED_SCREEN".
 * If the environment variable is not present or out of bound, the
 * default is used.
 * <br>
 * Single screen should work fine.
 * @author err
 */
public class UIUtil
{

/**
 * @return default graphics configuration of the preferred device
 */
public static GraphicsConfiguration getGraphicsConfiguration()
{
    return getGraphicsDev().getDefaultConfiguration();
}

/** Using the location of the Window, if it is not on the preferred screen,
 * then translate it to the preferred screen.
 * @param window 
 */
public static void translateToScreen(Window window)
{
    Rectangle screenBounds = getScreenBounds();
    Point loc = window.getLocation();
    if(!screenBounds.contains(loc)) {
        loc.translate(screenBounds.x, screenBounds.y);
        window.setLocation(loc);
    }
}

/** If the location is not on the preferred screen,
 * then translate it to the preferred screen.
 * @param location a point on the screen
 */
public static void translateToScreen(Point location)
{
    Rectangle screenBounds = getScreenBounds();
    if(!screenBounds.contains(location)) {
        location.translate(screenBounds.x, screenBounds.y);
    }
}

/**
 * @return screen bounds of the preferred device
 */
public static Rectangle getScreenBounds()
{
    return getGraphicsConfiguration().getBounds();
}

/**
 * Center the window on the preferred screen.
 * @param window 
 */
public static void centerOnScreen(Window window) {
    //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    //Rectangle screenBounds = Jvi.getScreenBounds();
    Dimension screenSize = getScreenBounds().getSize();
    Dimension size = window.getSize();
    window.setLocation((screenSize.width - size.width) / 2,
                (screenSize.height - size.height) / 2);
    translateToScreen(window);
}

/** the preferred graphics device */
private static GraphicsDevice preferredGraphicsDevice = null;
/**
 * If there's an environment variable ,
 * then use that as an index into
 * {@link GraphicsEnvironment#getLocalGraphicsEnvironment()#getScreenDevices()}
 * and return that device; otherwise return the default screen device.
 * @return
 */
public static GraphicsDevice getGraphicsDev()
{
    if(preferredGraphicsDevice == null) {
        int screenIdx;
        String sint = System.getenv("JAVA_PREFERRED_SCREEN");
        if(sint != null) {
            try {
                screenIdx = Integer.parseInt(sint);
                GraphicsDevice[] devs = GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getScreenDevices();
                if(screenIdx >= 0 && screenIdx < devs.length)
                    preferredGraphicsDevice = devs[screenIdx];
            } catch(NumberFormatException ex) {
            }
        }
        if(preferredGraphicsDevice == null)
            preferredGraphicsDevice = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }
    return preferredGraphicsDevice;
}

/* ************************************

public static GraphicsDevice getGraphicsDev()
{
//checkGaphicsDev();
GraphicsDevice dev = null;
GraphicsDevice[] devs
= GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
dev = devs[1];
GraphicsConfiguration[] gc = dev.getConfigurations();
GraphicsConfiguration gcDef
= dev.getDefaultConfiguration();
return dev != null ? dev
: GraphicsEnvironment.getLocalGraphicsEnvironment()
.getDefaultScreenDevice();
}

public static Rectangle checkGaphicsDev()
{
Rectangle virtualBounds = new Rectangle();
GraphicsEnvironment ge = GraphicsEnvironment.
getLocalGraphicsEnvironment();
GraphicsDevice[] gs =
ge.getScreenDevices();
for (int j = 0; j < gs.length; j++) {
GraphicsDevice gd = gs[j];
GraphicsConfiguration[] gc =
gd.getConfigurations();
for (int i=0; i < gc.length; i++) {
Rectangle bounds = gc[i].getBounds();
virtualBounds =
virtualBounds.union(gc[i].getBounds());
}
}
return virtualBounds;
}
****************************************/
}
