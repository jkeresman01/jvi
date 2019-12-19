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
package com.raelity.jvi;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * NOTE: the first tab has index 1
 * 
 * @author err
 */
public class ViTabInfo
{
private final List<ViAppView> appViews;
private final int curIdx; // NOTE: this is 1 based

public static final int LAST_TAB = 9999;
public ViTabInfo(List<ViAppView> avs, int idx) {
    
    appViews = avs;
    curIdx = idx;
}

public int getLastNr() {
    return appViews.size();
}

public int getCurNr() {
    return curIdx;
}

// public ViAppView getCur() {
//     return appViews[curIdx];
// }

public ViAppView getAppView(int tab_number) {
    if(tab_number < 1 || tab_number > appViews.size())
        return null;
    return appViews.get(tab_number - 1);
}

////////////////////////////////////////////////////////////////

static ViTabInfo getDebugTabInfo() {
    List<ViAppView> l = new ArrayList<>();
    // make it a 6 element list
    l.add(null); l.add(null); l.add(null);
    l.add(null); l.add(null); l.add(null);
    return new ViTabInfo(l, 3);
}

private static class DebugAppView implements ViAppView {

        @Override
        public Component getEditor() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isShowing() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isNomad() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getWNum() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
