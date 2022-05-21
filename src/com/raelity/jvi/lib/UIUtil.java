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

package com.raelity.jvi.lib;

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
 * <p>
 * The default screen
 * can be specified by the environment variable: "JAVA_PREFERRED_SCREEN".
 * If the environment variable is not present or out of bound, the
 * default is used.
 * </p>
 * See {@link #setPrefGraphicsDev(java.lang.String) }
 * or {@link #setPrefGraphicsDev(int) } to set the preferred graphics device
 * used by these methods.
 * @author err
 */
public class UIUtil
{
private UIUtil() {}

private static boolean isVirtual;
private static Rectangle virtualBounds;

static
{
	initVirtualParams();
}

public static boolean isVirtualEnvironment()
{
	return isVirtual;
}

/**
 * @return default graphics configuration of the preferred device
 */
public static GraphicsConfiguration getPrefGraphicsConfiguration()
{
    return getPrefGraphicsDev().getDefaultConfiguration();
}

/** Using the location of the Window, if it is not on the preferred screen,
 * then translate it to the preferred screen.
 * @param window 
 */
public static void translateToPrefScreen(Window window)
{
    window.setLocation(translateToPrefScreen(window.getLocation()));
}

/** If Window is not on the same screen as target,
 * then translate Window to target's screen.
 * @param window 
 * @param target 
 */
public static void translateToPrefScreen(Window window, Point target)
{
    if(target == null)
        return;
    window.setLocation(translateToScreen(window.getLocation(), target));
}

/** If the location is not on the preferred screen,
 * then translate it (the same object) to the preferred screen.
 * @param location a point on the scre
 * @return return for convenience, same object as argument
 */
public static Point translateToPrefScreen(Point location)
{
    Rectangle screenBounds = getPrefScreenBounds();
    if(!screenBounds.contains(location)) {
        Rectangle locScreenBounds = getGraphicsConfiguration(location).getBounds();
        location.translate(screenBounds.x - locScreenBounds.x,
                           screenBounds.y - locScreenBounds.y);
    }
    return location;
}

/** If location is not on the same screen as
 * the target then translate it (the same object) to the target's screen.
 * @param location move this point to the target screen
 * @param target used to find destination screen
 * @return return for convenience, same object as argument
 */
public static Point translateToScreen(Point location, Point target)
{
    if(target == null)
        return location;
    Rectangle screenBounds = getGraphicsConfiguration(target).getBounds();
    if(!screenBounds.contains(location)) {
        Rectangle locScreenBounds = getGraphicsConfiguration(location).getBounds();
        location.translate(screenBounds.x - locScreenBounds.x,
                           screenBounds.y - locScreenBounds.y);
    }
    return location;
}

/**
 * @return screen bounds of the preferred device
 */
public static Rectangle getPrefScreenBounds()
{
    return getPrefGraphicsConfiguration().getBounds();
}

/**
 * Center the window on the preferred screen.
 * @param window 
 */
public static void centerOnPrefScreen(Window window) {
    //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    //Rectangle screenBounds = Jvi.getScreenBounds();
    Dimension screenSize = getPrefScreenBounds().getSize();
    Dimension size = window.getSize();
    window.setLocation((screenSize.width - size.width) / 2,
                (screenSize.height - size.height) / 2);
    translateToPrefScreen(window);
}

/** the preferred graphics device */
private static GraphicsDevice preferredGraphicsDevice = null;
/**
 * If there's an environment variable "JAVA_PREFERRED_SCREEN",
 * then use that as an index into
 * {@link GraphicsEnvironment#getLocalGraphicsEnvironment()#getScreenDevices()}
 * and return that device; otherwise return the default screen device.
 * @return
 */
public static GraphicsDevice getPrefGraphicsDev()
{
    if(preferredGraphicsDevice == null) {
        GraphicsDevice gdev = setPrefGraphicsDev("JAVA_PREFERRED_SCREEN");
        if(gdev == null)
            preferredGraphicsDevice = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }
    return preferredGraphicsDevice;
}

/**
 * This sets the preferred graphics device, use by the methods in this class,
 * according the value of the specified environment variable.
 * If null is returned, then the existing preferred device is not changed.
 * @param envVar check this environment variable's value
 * @return updated preferred graphics dev or null if bad argument
 */
public static GraphicsDevice setPrefGraphicsDev(String envVar)
{
    GraphicsDevice gdev = null;
    
    int screenIdx;
    String sint = System.getenv(envVar);
    if(sint != null) {
        try {
            screenIdx = Integer.parseInt(sint);
            gdev = setPrefGraphicsDev(screenIdx);
        } catch(NumberFormatException ex) {
        }
    }

    return gdev;
}

/**
 * This returns null if the screen is not found.
 * @param screenIdx look for this screen
 * @return graphics dev or null if bad argument
 */
/**
 * This sets the preferred graphics device, use by the methods in this class,
 * according the specified value.
 * If null is returned, then the existing preferred device is not changed.
 * @param screenIdx look for this screen device index.
 * @return updated preferred graphics dev or null if bad argument
 */
public static GraphicsDevice setPrefGraphicsDev(int screenIdx)
{
    GraphicsDevice gdev = null;
    
    GraphicsDevice[] devs = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getScreenDevices();
    if(screenIdx >= 0 && screenIdx < devs.length)
        gdev = devs[screenIdx];
    if(gdev != null)
        preferredGraphicsDevice = gdev;

    return gdev;
}

/**
 * Check each screen Device finding the defaultConfiguration that
 * contains the given point.
 * @param location
 * @return 
 */
public static GraphicsConfiguration getGraphicsConfiguration(Point location)
{
	GraphicsDevice[] gs = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getScreenDevices();
	for (GraphicsDevice gd : gs) {
		GraphicsConfiguration config = gd.getDefaultConfiguration();
		if(config.getBounds().contains(location))
			return config;
	}
	return GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getDefaultScreenDevice().getDefaultConfiguration();
}

private static void initVirtualParams()
{
	Point zeroPoint = new Point();
	virtualBounds = new Rectangle();
	GraphicsEnvironment ge = GraphicsEnvironment.
			getLocalGraphicsEnvironment();
	GraphicsDevice[] gs = ge.getScreenDevices();
	for (GraphicsDevice gd : gs) {
		for (GraphicsConfiguration gc : gd.getConfigurations()) {
			if(!gc.getBounds().getLocation().equals(zeroPoint))
				isVirtual = true;
			virtualBounds = virtualBounds.union(gc.getBounds());
		}
	}
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
