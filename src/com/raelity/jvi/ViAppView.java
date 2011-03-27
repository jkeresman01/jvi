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

import java.awt.Component;

/**
 * This is a handle for a platform edit window.
 * It is used to abstract a platform container that holds the actual editable
 * component. There is no requirement for a platform to use this, but without
 * it certain jVi features are not useful, such as ":ls".
 * When used, the platform may,but is not required to,
 * include the editable component in the
 * implementation, and return it from getEditor.
 * <p/>
 * Note that a ViAppView may exist before the contained editor is
 * actually instantiated by the platform.
 * This interface is often used when an application allows multiple
 * open editors.
 * <p/>
 * NEEDSWORK: jVi generally keeps weak references to these.
 * <p/>
 * A platform may have editors that do not have a special container,
 * or for some reason jVi should not track the editor,
 * for example an editor in a dialog. Usually there is no user accessible
 * disk file associated with these editors.
 * jVi considers them  "nomadic" edtiors and
 * an AppView is not needed for these.
 * <p/>
 * The equality of two of these is based on equals. But in some situations
 * a platform may start working with the container before an editor component
 * is assigned to the container. ViManager has a method to cleanup "stale"
 * AppViews in this situation so that any storage that depends on equals,
 * for example a set, is fixed up. Note that if there is always a 1-1,
 * or 1-0, * relationship between Container and Editor then
 * equality can be based on Container.
 * <p/>
 *
 * @author Ernie Rael <err at raelity.com>
 */
public interface ViAppView
{
    /**
     * The editable component which is associated with
     * a {@link ViTextView}.
     *
     * @return the editor component, may be null.
     */
    public Component getEditor();

    /**
     * Like Component.isShowing().
     */
    public boolean isShowing();

    /**
     * Some editors may not have a platform container, for example if a JEP
     * is in a dialog, and certain jVi features ignore it.
     *
     * @return true to exclude this editor from some commands and consideration.
     */
    public boolean isNomad();

    /** Get the unique/invariant integer identifier of the editor/window
     * associated with the av.
     * When the app first opens an editor it should derive this number.
     * This number is used in jVi commands such as ":e#!number!"
     *
     * Nomads probably don't have a window number.
     *
     * @return the invariant number of the editor or less than 0 for unknown
     */
    public int getWNum();

}
