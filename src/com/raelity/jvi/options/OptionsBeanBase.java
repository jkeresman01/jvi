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
/*
 * OptionsBeanBase.java
 *
 * Created on January 23, 2007, 11:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi.options;

import java.awt.Color;
import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.beans.SimpleBeanInfo;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openide.util.WeakListeners;

import com.raelity.jvi.core.Options;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.options.OptUtil.OptionChangeHandler;

import static com.raelity.jvi.manager.ViManager.getFactory;

/**
 * Base class for jVi options beans. This method contains the read/write methods
 * for all options. Which options are made visible is controlled by the
 * optionsList given to the constructor. Using this class, options are
 * grouped into different beans.
 *
 * @author erra
 */
public class OptionsBeanBase extends SimpleBeanInfo
implements Options.EditControl
{
    private static final
            Logger LOG = Logger.getLogger(OptionsBeanBase.class.getName());
    private final Class<?> clazz;
    private final List<String> optionsList;
    private final String displayName;
    
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final VetoableChangeSupport vcs = new VetoableChangeSupport(this);
    //private static String checkme = "Search Options";
    private final OptionChangeHandler optionChangeHandler;

    //private final Map<String,Object> changeMap = new HashMap<>();
    
    /** Creates a new instance of OptionsBeanBase */
    public OptionsBeanBase(Class<?> clazz, String displayName,
                           Options.Category category)
    {
        this.optionChangeHandler
                = new OptionChangeHandler(pcs, getFactory().getPreferences());
        this.clazz = clazz;
        this.displayName = displayName;
        this.optionsList = Options.getOptionList(category);

        optionsListener = new OptionsListener();
        //Options.addPropertyChangeListener(new OptionsListener());
        Options.addPropertyChangeListener(
                WeakListeners.propertyChange(optionsListener, Options.class));
        //if(checkme.equals(displayName))
        //    System.err.println("CONSTRUCT: " + displayName);
    }

    @Override
    public void start()
    {
        optionChangeHandler.clear();
    }

    @Override
    public void ok()
    {
        // Now's the time to persist the changes
        optionChangeHandler.applyChanges();
        optionChangeHandler.clear();
    }

    @Override
    public void cancel()
    {
        optionChangeHandler.clear();
    }

    private final OptionsListener optionsListener;
    private class OptionsListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if(optionsList.contains(evt.getPropertyName())) {
                //System.err.println("Fire: " + evt.getPropertyName());
                OptionsBeanBase.this.pcs.firePropertyChange(evt);
            }
        }
    }
    
    @Override
    public BeanDescriptor getBeanDescriptor() {
        return new ThisBeanDescriptor();
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
	PropertyDescriptor[] descriptors
                    = new PropertyDescriptor[optionsList.size()];
	int i = 0;

	for(String name : optionsList) {
            PropertyDescriptor d;
            if(name.equals("jViVersion")) {
                try {
                    d = new PropertyDescriptor(name, clazz,
                            "getJViVersion", null);
                } catch (IntrospectionException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    continue;
                }
                d.setDisplayName("jVi Version");
            } else {
                try {
                    d = getFactory()
                            .createPropertyDescriptor(name, name, clazz);
                } catch (IntrospectionException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    continue;
                }
            }
	    descriptors[i++] = d;
	}
	return descriptors;
    }

    public static  PropertyDescriptor createPropertyDescriptor(String optName,
                                                               String methodName,
                                                               Class<?> clazz)
    throws IntrospectionException {
        PropertyDescriptor d;
        Option<?> opt = Options.getOption(optName);
        // d = new MyPropertyDescriptor(methodName, clazz);
        d = new PropertyDescriptor(methodName, clazz);
        d.setDisplayName(opt.getDisplayName());
        d.setExpert(opt.isExpert());
        d.setHidden(opt.isHidden());
        d.setShortDescription(opt.getDesc());
        // NEEDSWORK: why aren't the others constrained?
        if(opt instanceof IntegerOption
           || opt instanceof StringOption) {
            d.setBound(true);
            d.setConstrained(true);
        }
        return d;
    }
    
    private static Image icon, icon32;
    @Override
    public Image getIcon (int type) {
        if (type == BeanInfo.ICON_COLOR_16x16
                || type == BeanInfo.ICON_MONO_16x16) {
            if (icon == null)
                icon = loadImage("/com/raelity/jvi/resources/jViLogo.png");
            return icon;
        } else {
            if (icon32 == null)
                icon = loadImage("/com/raelity/jvi/resources/jViLogo32.png");
            return icon32;
        }
    }
    
    private class ThisBeanDescriptor extends BeanDescriptor {
        ThisBeanDescriptor() {
            super(clazz);
        }
        
        @Override
        public String getDisplayName() {
	    return displayName;
        }
    }
    
    //
    // Look like a good bean
    //
    
    public void addPropertyChangeListener( PropertyChangeListener listener )
    {
        this.pcs.addPropertyChangeListener( listener );
    }

    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        this.pcs.removePropertyChangeListener( listener );
    }
    
    public void addVetoableChangeListener( VetoableChangeListener listener )
    {
        this.vcs.addVetoableChangeListener( listener );
    }

    public void removeVetoableChangeListener( VetoableChangeListener listener )
    {
        this.vcs.addVetoableChangeListener( listener );
    } 

    final protected void put(String name, String val) throws PropertyVetoException {
        String old = getString(name);
	Option<?> opt = Options.getOption(name);
        opt.validate(val);
        this.vcs.fireVetoableChange( name, old, val );
        optionChangeHandler.changeOption(name, "String", old, val);
    }

    final protected void put(String name, int val) throws PropertyVetoException {
        int old = getint(name);
	Option<?> opt = Options.getOption(name);
        opt.validate(val);
        this.vcs.fireVetoableChange( name, old, val );
        optionChangeHandler.changeOption(name, "Integer", old, (Integer)val);
    }

    final protected void put(String name, Color val) throws PropertyVetoException {
        Color old = getColor(name);
	ColorOption opt = (ColorOption)Options.getOption(name);
        opt.validate(val);
        this.vcs.fireVetoableChange( name, old, val );
        optionChangeHandler.changeOption(name, "Color", old, val);
    }

    final protected void put(String name, EnumSet<?> val) throws PropertyVetoException {
        @SuppressWarnings("unchecked")
        EnumSet<?> old = EnumSet.copyOf(getEnumSet(name));
	EnumSetOption<?> opt = (EnumSetOption)Options.getOption(name);
        opt.validate(val);
        this.vcs.fireVetoableChange( name, old, val );
        optionChangeHandler.changeOption(name, "EnumSet", old, val);
    }

    final protected void put(String name, boolean val) {
        boolean old = getboolean(name);
        optionChangeHandler.changeOption(name, "Boolean", old, val);
    }

    final protected String getString(String name) {
	Option<?> opt = Options.getOption(name);
	return opt.getString();
    }

    final protected int getint(String name) {
	Option<?> opt = Options.getOption(name);
	return opt.getInteger();
    }

    final protected Color getColor(String name) {
	Option<?> opt = (ColorOption) Options.getOption(name);
        return opt.getColor();
    }

    final protected EnumSet<?> getEnumSet(String name) {
	Option<?> opt = Options.getOption(name);
	return opt.getEnumSet();
    }

    final protected boolean getboolean(String name) {
	Option<?> opt = Options.getOption(name);
	return opt.getBoolean();
    }
    
    //
    // All the known options
    //      The bean getter/setter
    //
    
    /** this read-only option is special cased */
    public String getJViVersion() {
        return ViManager.getReleaseString();
    }

    public void setViCursorInView(boolean arg) {
        put(Options.cursorInView, arg);
    }

    public boolean getViCursorInView() {
	return getboolean(Options.cursorInView);
    }

    public void setViStartOfLine(boolean arg) {
        put("viStartOfLine", arg);
    }

    public boolean getViStartOfLine() {
	return getboolean("viStartOfLine");
    }

    public void setViPerProjectSupport(boolean arg)  throws PropertyVetoException {
        put(Options.perProjectSupport, arg);
    }

    public boolean getViPerProjectSupport() {
	return getboolean(Options.perProjectSupport);
    }

    public void setViHistory(int arg)
            throws PropertyVetoException {
        put(Options.history, arg);
    }

    public int getViHistory() {
        return getint(Options.history);
    }


    public void setViAaSearchMagic(String arg)
    throws PropertyVetoException {
        put(Options.magic, arg);
    }

    public String getViAaSearchMagic() {
	return getString(Options.magic);
    }

    public void setViVisualBellColor(Color arg)  throws PropertyVetoException {
        put(Options.visualBellColor, arg);
    }

    public Color getViVisualBellColor() {
        return getColor(Options.visualBellColor);
    }

    public void setViVisualBell(boolean arg)  throws PropertyVetoException {
        put(Options.visualBell, arg);
    }

    public boolean getViVisualBell() {
	return getboolean(Options.visualBell);
    }

    public void setViVisualBellTime(int arg)  throws PropertyVetoException {
        put(Options.visualBellTime, arg);
    }

    public int getViVisualBellTime() {
	    return getint(Options.visualBellTime);
    }

    public void setViFoldOpen(EnumSet<?> arg)  throws PropertyVetoException {
        put(Options.foldOpen, arg);
    }

    public EnumSet<?> getViFoldOpen() {
	return getEnumSet(Options.foldOpen);
    }

    public void setViCursorXorBug(boolean arg)  throws PropertyVetoException {
        put(Options.cursorXorBug, arg);
    }

    public boolean getViCursorXorBug() {
	return getboolean(Options.cursorXorBug);
    }

    public void setViDisableFontError(boolean arg)  throws PropertyVetoException {
        put(Options.disableFontError, arg);
    }

    public boolean getViDisableFontError() {
	return getboolean(Options.disableFontError);
    }

    public void setViDisableFontCheckSpecial(boolean arg)  throws PropertyVetoException {
        put(Options.disableFontCheckSpecial, arg);
    }

    public boolean getViDisableFontCheckSpecial() {
	return getboolean(Options.disableFontCheckSpecial);
    }

    public void setViEqualAlways(boolean arg)  throws PropertyVetoException {
        put(Options.equalAlways, arg);
    }

    public boolean getViEqualAlways() {
	return getboolean(Options.equalAlways);
    }

    public void setViSplitBelow(boolean arg)  throws PropertyVetoException {
        put(Options.splitBelow, arg);
    }

    public boolean getViSplitBelow() {
	return getboolean(Options.splitBelow);
    }

    public void setViSplitRight(boolean arg)  throws PropertyVetoException {
        put(Options.splitRight, arg);
    }

    public boolean getViSplitRight() {
	return getboolean(Options.splitRight);
    }

    public void setViTimeout(boolean arg)  throws PropertyVetoException {
        put(Options.timeout, arg);
    }

    public boolean getViTimeout() {
	return getboolean(Options.timeout);
    }

    public void setViTimeoutLen(int arg)  throws PropertyVetoException {
        put(Options.timeoutlen, arg);
    }

    public int getViTimeoutLen() {
	    return getint(Options.timeoutlen);
    }

    public void setViMapCommands(String arg)  throws PropertyVetoException {
        put(Options.mapCommands, arg);
    }

    public String getViMapCommands() {
	return getString(Options.mapCommands);
    }

    public void setViList(boolean arg)  throws PropertyVetoException {
        put(Options.list, arg);
    }

    public boolean getViList() {
	return getboolean(Options.list);
    }

    public void setViNumber(boolean arg)  throws PropertyVetoException {
        put(Options.number, arg);
    }

    public boolean getViNumber() {
	return getboolean(Options.number);
    }

    public void setViCaretBlinkRate(int arg)  throws PropertyVetoException {
        put(Options.caretBlinkRate, arg);
    }

    public int getViCaretBlinkRate() {
	    return getint(Options.caretBlinkRate);
    }

    public void setViIsKeyWord(String arg)  throws PropertyVetoException {
        put(Options.isKeyWord, arg);
    }

    public String getViIsKeyWord() {
	return getString(Options.isKeyWord);
    }

    public void setViMagicRedoAlgorithm(String arg)  throws PropertyVetoException {
        put(Options.magicRedoAlgorithm, arg);
    }

    public String getViMagicRedoAlgorithm() {
	return getString(Options.magicRedoAlgorithm);
    }

    public void setViWrap(boolean arg)  throws PropertyVetoException {
        put(Options.wrap, arg);
    }

    public boolean getViWrap() {
	return getboolean(Options.wrap);
    }

    public void setViLineBreak(boolean arg)  throws PropertyVetoException {
        put(Options.lineBreak, arg);
    }

    public boolean getViLineBreak() {
	return getboolean(Options.lineBreak);
    }

    public void setViPersistedBufMarks(int arg)  throws PropertyVetoException {
        put(Options.persistedBufMarks, arg);
    }

    public int getViPersistedBufMarks() {
	    return getint(Options.persistedBufMarks);
    }

    public void setViPlatformTab(boolean arg)  throws PropertyVetoException {
        put(Options.platformTab, arg);
    }

    public boolean getViPlatformTab() {
	return getboolean(Options.platformTab);
    }

    public void setViNrFormats(EnumSet<?> arg)  throws PropertyVetoException {
        put(Options.nrFormats, arg);
    }

    public EnumSet<?> getViNrFormats() {
	return getEnumSet(Options.nrFormats);
    }

    public void setViCoordSkip(boolean arg)  throws PropertyVetoException {
        put(Options.coordSkip, arg);
    }

    public boolean getViCoordSkip() {
	return getboolean(Options.coordSkip);
    }

    public void setViPlatformPreferences(boolean arg)  throws PropertyVetoException {
        put(Options.platformPreferences, arg);
    }

    public boolean getViPlatformPreferences() {
	return getboolean(Options.platformPreferences);
    }

    public void setViAutoPopupFN(boolean arg)  throws PropertyVetoException {
        put(Options.autoPopupFN, arg);
    }

    public boolean getViAutoPopupFN() {
	return getboolean(Options.autoPopupFN);
    }

    public void setViAutoPopupCcName(boolean arg)  throws PropertyVetoException {
        put(Options.autoPopupCcName, arg);
    }

    public boolean getViAutoPopupCcName() {
	return getboolean(Options.autoPopupCcName);
    }

    public void setViPlatformBraceMatch(boolean arg)  throws PropertyVetoException {
        put(Options.platformBraceMatch, arg);
    }

    public boolean getViPlatformBraceMatch() {
	return getboolean(Options.platformBraceMatch);
    }

    public void setViShell(String arg)  throws PropertyVetoException {
        put(Options.shell, arg);
    }

    public String getViShell() {
	    return getString(Options.shell);
    }

    public void setViShellCmdFlag(String arg)  throws PropertyVetoException {
        put(Options.shellCmdFlag, arg);
    }

    public String getViShellCmdFlag() {
	    return getString(Options.shellCmdFlag);
    }

    public void setViShellXQuote(String arg)  throws PropertyVetoException {
        put(Options.shellXQuote, arg);
    }

    public String getViShellXQuote() {
	    return getString(Options.shellXQuote);
    }

    public void setViShellSlash(boolean arg) throws PropertyVetoException {
        put(Options.shellSlash, arg);
    }

    public boolean getViShellSlash() {
        return getboolean(Options.shellSlash);
    }
    
    public void setViEqualProgram(String arg)  throws PropertyVetoException {
        put(Options.equalProgram, arg);
    }

    public String getViEqualProgram() {
	    return getString(Options.equalProgram);
    }

    public void setViFormatProgram(String arg)  throws PropertyVetoException {
        put(Options.formatProgram, arg);
    }

    public String getViFormatProgram() {
	    return getString(Options.formatProgram);
    }

    public void setViTextWidth(int arg)  throws PropertyVetoException {
        put(Options.textWidth, arg);
    }

    public int getViTextWidth() {
	    return getint(Options.textWidth);
    }

    public void setViModeline(boolean arg)  throws PropertyVetoException {
        put(Options.modeline, arg);
    }

    public boolean getViModeline() {
	return getboolean(Options.modeline);
    }

    public void setViModelines(int arg)  throws PropertyVetoException {
        put(Options.modelines, arg);
    }

    public int getViModelines() {
	return getint(Options.modelines);
    }

    public void setViRedoTrack(boolean arg)  throws PropertyVetoException {
        put(Options.redoTrack, arg);
    }

    public boolean getViRedoTrack() {
	return getboolean(Options.redoTrack);
    }

    public void setViPCMarkTrack(boolean arg)  throws PropertyVetoException {
        put(Options.pcmarkTrack, arg);
    }

    public boolean getViPCMarkTrack() {
	return getboolean(Options.pcmarkTrack);
    }

    public void setViInsertLeftWrapPrevious(boolean arg)
    throws PropertyVetoException {
        put(Options.insertLeftWrapPrevious, arg);
    }

    public boolean getViInsertLeftWrapPrevious() {
	return getboolean(Options.insertLeftWrapPrevious);
    }

    public void setViInsertRightWrapNext(boolean arg)
    throws PropertyVetoException {
        put(Options.insertRightWrapNext, arg);
    }

    public boolean getViInsertRightWrapNext() {
	return getboolean(Options.insertRightWrapNext);
    }

    public void setViSelectColor(Color arg)  throws PropertyVetoException {
        put(Options.selectColor, arg);
    }

    public Color getViSelectColor() {
        return getColor(Options.selectColor);
    }

    public void setViRoCursorColor(Color arg)  throws PropertyVetoException {
        put(Options.roCursorColor, arg);
    }

    public Color getViRoCursorColor() {
        return getColor(Options.roCursorColor);
    }

    public void setViSelectFgColor(Color arg)  throws PropertyVetoException {
        put(Options.selectFgColor, arg);
    }

    public Color getViSelectFgColor() {
        return getColor(Options.selectFgColor);
    }

    public void setViSearchColor(Color arg)  throws PropertyVetoException {
        put(Options.searchColor, arg);
    }

    public Color getViSearchColor() {
        return getColor(Options.searchColor);
    }

    public void setViSearchFgColor(Color arg)  throws PropertyVetoException {
        put(Options.searchFgColor, arg);
    }

    public Color getViSearchFgColor() {
        return getColor(Options.searchFgColor);
    }

    public void setViSelection(String arg)  throws PropertyVetoException {
        put(Options.selection, arg);
    }

    public String getViSelection() {
	return getString(Options.selection);
    }

    public void setViIncrSearch(boolean arg) {
        put(Options.incrSearch, arg);
    }

    public boolean getViIncrSearch() {
	return getboolean(Options.incrSearch);
    }

    public void setViSelectMode(boolean arg) {
        put(Options.selectMode, arg);
    }

    public boolean getViSelectMode() {
	return getboolean(Options.selectMode);
    }

    public void setViEndOfSentence(boolean arg) {
        put(Options.endOfSentence, arg);
    }

    public boolean getViEndOfSentence() {
	return getboolean(Options.endOfSentence);
    }

    public void setViHighlightSearch(boolean arg) {
        put(Options.highlightSearch, arg);
    }

    public boolean getViHighlightSearch() {
	return getboolean(Options.highlightSearch);
    }

    public void setViShowCommand(boolean arg) {
        put(Options.showCommand, arg);
    }

    public boolean getViShowCommand() {
	return getboolean(Options.showCommand);
    }

    public void setViShowMode(boolean arg) {
        put(Options.showMode, arg);
    }

    public boolean getViShowMode() {
	return getboolean(Options.showMode);
    }

    public void setViCommandEntryFrameOption(String arg)
    throws PropertyVetoException {
        put("viCommandEntryFrameOption", arg);
    }

    public String getViCommandEntryFrameOption() {
	return getString("viCommandEntryFrameOption");
    }

    public void setViBackspaceWrapPrevious(boolean arg) {
        put("viBackspaceWrapPrevious", arg);
    }

    public boolean getViBackspaceWrapPrevious() {
	return getboolean("viBackspaceWrapPrevious");
    }

    public void setViHWrapPrevious(boolean arg) {
        put("viHWrapPrevious", arg);
    }

    public boolean getViHWrapPrevious() {
	return getboolean("viHWrapPrevious");
    }

    public void setViLeftWrapPrevious(boolean arg) {
        put("viLeftWrapPrevious", arg);
    }

    public boolean getViLeftWrapPrevious() {
	return getboolean("viLeftWrapPrevious");
    }

    public void setViSpaceWrapNext(boolean arg) {
        put("viSpaceWrapNext", arg);
    }

    public boolean getViSpaceWrapNext() {
	return getboolean("viSpaceWrapNext");
    }

    public void setViLWrapNext(boolean arg) {
        put("viLWrapNext", arg);
    }

    public boolean getViLWrapNext() {
	return getboolean("viLWrapNext");
    }

    public void setViRightWrapNext(boolean arg) {
        put("viRightWrapNext", arg);
    }

    public boolean getViRightWrapNext() {
	return getboolean("viRightWrapNext");
    }

    public void setViTildeWrapNext(boolean arg) {
        put("viTildeWrapNext", arg);
    }

    public boolean getViTildeWrapNext() {
	return getboolean("viTildeWrapNext");
    }

    public void setViUnnamedClipboard(boolean arg) {
        put("viUnnamedClipboard", arg);
    }

    public boolean getViUnnamedClipboard() {
	return getboolean("viUnnamedClipboard");
    }

    public void setViJoinSpaces(boolean arg) {
        put("viJoinSpaces", arg);
    }

    public boolean getViJoinSpaces() {
	return getboolean("viJoinSpaces");
    }

    public void setViShiftRound(boolean arg) {
        put("viShiftRound", arg);
    }

    public boolean getViShiftRound() {
	return getboolean("viShiftRound");
    }

    public void setViChangeWordBlanks(boolean arg) {
        put("viChangeWordBlanks", arg);
    }

    public boolean getViChangeWordBlanks() {
	return getboolean("viChangeWordBlanks");
    }

    public void setViTildeOperator(boolean arg) {
        put("viTildeOperator", arg);
    }

    public boolean getViTildeOperator() {
	return getboolean("viTildeOperator");
    }

    public void setViSearchFromEnd(boolean arg) {
        put("viSearchFromEnd", arg);
    }

    public boolean getViSearchFromEnd() {
	return getboolean("viSearchFromEnd");
    }

    public void setViWrapScan(boolean arg) {
        put("viWrapScan", arg);
    }

    public boolean getViWrapScan() {
	return getboolean("viWrapScan");
    }

    public void setViIgnoreCase(boolean arg) {
        put("viIgnoreCase", arg);
    }

    public boolean getViIgnoreCase() {
	return getboolean("viIgnoreCase");
    }

    public void setViSmartCase(boolean arg) {
        put(Options.smartCase, arg);
    }

    public boolean getViSmartCase() {
	return getboolean(Options.smartCase);
    }

    public void setViExpandTabs(boolean arg) {
        put("viExpandTabs", arg);
    }

    public boolean getViExpandTabs() {
	return getboolean("viExpandTabs");
    }

    public void setViReport(int arg) throws PropertyVetoException {
        put("viReport", arg);
    }

    public int getViReport() {
	return getint("viReport");
    }

    public void setViBackspace(int arg) throws PropertyVetoException {
        put("viBackspace", arg);
    }

    public int getViBackspace() {
	return getint("viBackspace");
    }

    public void setViScrollOff(int arg) throws PropertyVetoException {
        put("viScrollOff", arg);
    }

    public int getViScrollOff() {
	return getint("viScrollOff");
    }

    public void setViSideScroll(int arg) throws PropertyVetoException {
        put(Options.sideScroll, arg);
    }

    public int getViSideScroll() {
	return getint(Options.sideScroll);
    }

    public void setViSideScrollOff(int arg) throws PropertyVetoException {
        put(Options.sideScrollOff, arg);
    }

    public int getViSideScrollOff() {
	return getint(Options.sideScrollOff);
    }

    public void setViShiftWidth(int arg) throws PropertyVetoException {
        put("viShiftWidth", arg);
    }

    public int getViShiftWidth() {
	return getint("viShiftWidth");
    }

    public void setViTabStop(int arg) throws PropertyVetoException {
        put("viTabStop", arg);
    }

    public int getViTabStop() {
	return getint("viTabStop");
    }

    public void setViSoftTabStop(int arg) throws PropertyVetoException {
        put("viSoftTabStop", arg);
    }

    public int getViSoftTabStop() {
	return getint("viSoftTabStop");
    }

    public void setViReadOnlyHack(boolean arg) {
        put("viReadOnlyHack", arg);
    }

    public boolean getViReadOnlyHack() {
	return getboolean("viReadOnlyHack");
    }

    public void setViClassicUndo(boolean arg) {
        put("viClassicUndo", arg);
    }

    public boolean getViClassicUndo() {
	return getboolean("viClassicUndo");
    }
    
    public void setViHideVersion(boolean arg) {
        put(Options.hideVersionOption, arg);
    }

    public boolean getViHideVersion() {
	return getboolean(Options.hideVersionOption);
    }

    public void setViDbgFonts(String arg)  throws PropertyVetoException {
        put(Options.dbgFonts, arg);
    }

    public String getViDbgFonts() {
	return getString(Options.dbgFonts);
    }

    public void setViDbgCoordSkip(String arg)  throws PropertyVetoException {
        put(Options.dbgCoordSkip, arg);
    }

    public String getViDbgCoordSkip() {
	return getString(Options.dbgCoordSkip);
    }

    public void setViDbgUndo(String arg)  throws PropertyVetoException {
        put(Options.dbgUndo, arg);
    }

    public String getViDbgUndo() {
	return getString(Options.dbgUndo);
    }

    public void setViDbgSearch(String arg)  throws PropertyVetoException {
        put(Options.dbgSearch, arg);
    }

    public String getViDbgSearch() {
	return getString(Options.dbgSearch);
    }

    public void setViDbgOptions(String arg)  throws PropertyVetoException {
        put(Options.dbgOptions, arg);
    }

    public String getViDbgOptions() {
	return getString(Options.dbgOptions);
    }

    public void setViDbgWindowTreeBuilder(String arg)  throws PropertyVetoException {
        put(Options.dbgWindowTreeBuilder, arg);
    }

    public String getViDbgWindowTreeBuilder() {
	return getString(Options.dbgWindowTreeBuilder);
    }

    public void setViDbgPrefChangeMonitor(String arg)  throws PropertyVetoException {
        put(Options.dbgPrefChangeMonitor, arg);
    }

    public String getViDbgPrefChangeMonitor() {
	return getString(Options.dbgPrefChangeMonitor);
    }

    public void setViDbgMouse(String arg) throws PropertyVetoException {
        put(Options.dbgMouse, arg);
    }

    public String getViDbgMouse() {
	return getString(Options.dbgMouse);
    }

    public void setViDbgKeyStrokes(String arg) throws PropertyVetoException {
        put(Options.dbgKeyStrokes, arg);
    }

    public String getViDbgKeyStrokes() {
	return getString(Options.dbgKeyStrokes);
    }

    public void setViDbgCache(String arg) throws PropertyVetoException {
        put(Options.dbgCache, arg);
    }

    public String getViDbgCache() {
	return getString(Options.dbgCache);
    }

    public void setViDbgBang(String arg) throws PropertyVetoException {
        put(Options.dbgBang, arg);
    }

    public String getViDbgBang() {
	return getString(Options.dbgBang);
    }

    public void setViDbgBangData(String arg) throws PropertyVetoException {
        put(Options.dbgBangData, arg);
    }

    public String getViDbgBangData() {
	return getString(Options.dbgBangData);
    }

    public void setViDbgEditorActivation(String arg) throws PropertyVetoException {
        put(Options.dbgEditorActivation, arg);
    }

    public String getViDbgEditorActivation() {
	return getString(Options.dbgEditorActivation);
    }

    public void setViDbgCompletion(String arg)  throws PropertyVetoException {
        put(Options.dbgCompletion, arg);
    }

    public String getViDbgCompletion() {
	return getString(Options.dbgCompletion);
    }

    public void setViDbgRedo(String arg)  throws PropertyVetoException {
        put(Options.dbgRedo, arg);
    }

    public String getViDbgRedo() {
	return getString(Options.dbgRedo);
    }

}

// vi: sw=4 et
