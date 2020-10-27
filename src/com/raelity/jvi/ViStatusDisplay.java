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
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 * 
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi;

/**
 * Display state of the editor is sent through this interface.
 */
public interface ViStatusDisplay {
  /** Output the current mode: INSERT/REPLACE */
  public void displayMode(String mode);

  /** Output the command characters accumulated so far */
  public void displayCommand(String cmd);

  /** Output chitchat, VV_STATUSMSG */
  public void displayStatusMessage(String msg);

  /** visual mode bounds */
  default public void displayVisualBounds(String msg) {};

  /** Output warning chitchat */
  public void displayErrorMessage(String msg);

  /** Output error chitchat, VV_ERRMSG; HLF_E highlight */
  public void displayWarningMessage(String msg);

  /** Output a message that can not be cleared with clearMessge.
   * Outputing any message thaws the message display.
   */
  public void displayFrozenMessage(String msg);
  
  /** Clear current status and/or error message */
  public void clearMessage();

  /** Clear display */
  public void clearDisplay();

  /** This method is called when the textview scrolls.
   * Vim clears the message when there's a scroll,
   * I think it should be ignored...
   */
  public void scrolling();
  
  /** Redisplay last message. May be noop depending on implementation.
   * This is invoked when exiting a keystroke because some platforms
   * clear the display at inoportune times.
   */
  public void refresh();
}
