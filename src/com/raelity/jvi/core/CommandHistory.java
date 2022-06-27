/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2020 Ernie Rael.  All Rights Reserved.
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
package com.raelity.jvi.core;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Level;

import com.google.common.eventbus.Subscribe;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.ViOutputStream.FLAGS;
import com.raelity.jvi.core.Commands.AbstractColonAction;
import com.raelity.jvi.core.Commands.ColonEvent;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.*;
import com.raelity.text.StringSegment;

import static com.raelity.jvi.core.Ops.get_register_value;
import static com.raelity.jvi.core.Misc.readPrefsList;
import static com.raelity.jvi.core.Msg.*;
import static com.raelity.jvi.core.Misc.skipwhite;
import static com.raelity.jvi.core.Misc.writePrefsList;
import static com.raelity.jvi.core.Util.beep_flush;
import static com.raelity.jvi.core.Util.isalpha;
import static com.raelity.jvi.core.Util.isdigit;
import static com.raelity.jvi.core.Util.strnicmp;
import static com.raelity.jvi.core.Util.vim_str2nr;
import static com.raelity.jvi.core.Util.vim_strchr;
import static com.raelity.jvi.core.lib.Constants.NUL;
import static com.raelity.jvi.manager.ViManager.eatme;
import static com.raelity.text.TextUtil.sf;
import static com.raelity.jvi.manager.ViManager.getFactory;

/**
 *
 * @author err
 */
public enum CommandHistory
{
COLON("cmd"),
SEARCH("search");

private static final String PREF_SEARCH = "search";
private static final String PREF_COMMANDS = "commands";
private static final Wrap<PreferencesImportMonitor> pSearchImportCheck = new Wrap<>();
private static final Wrap<PreferencesImportMonitor> pCommandsImportCheck = new Wrap<>();

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
    private static boolean didInit = false;
    @Override
    public void init()
    {
        if(didInit)
            return;
        didInit = true;
        CommandHistory.init();
    }
    }

private static void init() {
    Commands.register("his", "history", new History(),
                           EnumSet.of(CcFlag.NO_PARSE));
    Options.addPropertyChangeListenerSET(
            Options.history, (evt) -> historySizeChange(evt));
    ViEvent.getBus().register(new Object() {
        @Subscribe
        public void readHistories(ViEvent.Boot ev)
        {
            
            List<String> l = readPrefsList(PREF_SEARCH, pSearchImportCheck);
            // HACK
            if(!l.isEmpty())
                Search.startupInitializePattern(l.get(0));
            HistoryContext hc = SEARCH.initHistory(l);
            Search.getSearchCommandEntry().setHistory(hc);
            
            l = readPrefsList(PREF_COMMANDS, pCommandsImportCheck);
            hc = COLON.initHistory(l);
            ExCommands.getColonCommandEntry().setHistory(hc);
        }
        @Subscribe
        public void writeHistories(ViEvent.Shutdown ev)
        {
            List<String> l = SEARCH.getHistory();
            writePrefsList(PREF_SEARCH, l, pSearchImportCheck.getValue());

            l = COLON.getHistory();
            writePrefsList(PREF_COMMANDS, l, pCommandsImportCheck.getValue());
        }
    });
}

private HistoryContext history;
final String commonName;

private CommandHistory(String commonName)
{
    this.commonName = commonName;
}

static DebugOption dbgKeys;

HistoryContext initHistory(List<String> l)
{
    if(history != null)
        return null;
    history = HistoryContext.create(this);
    ListIterator<String> iter = l.listIterator(l.size());
    while(iter.hasPrevious())
        history.push(iter.previous());
    return history;
}

List<String> getHistory()
{
    List<String> l = new ArrayList<>(history.size());
    history.copyList(l);
    return l;
}

private static void historySizeChange(PropertyChangeEvent evt)
{
    eatme(evt);
    EventQueue.invokeLater(() -> historySizeChange());
}

private static void historySizeChange()
{
    COLON.history.trim();
    SEARCH.history.trim();
}


/**
 * Translate a history character to the associated history.
 */
private static CommandHistory char2CommandHistory(char c)
{
    switch(c) {
    case ':':
        return COLON;
    case '?':
    case '/':
        return SEARCH;
    default:
        return null;
    }
}

/**
 * Convert history name (from table above) to its HIST_ equivalent.
 * When "name" is empty, return "cmd" history.
 * Returns -1 for unknown history name.
 */
private static CommandHistory get_histtype(StringSegment name)
{
    int len = name.length();
    if(len == 0)
        return char2CommandHistory(':');
    for(CommandHistory which : CommandHistory.values()) {
        if(strnicmp(name, which.commonName, len) == 0)
            return which;
    }
    if(vim_strchr(":?/", name.charAt(0)) != null
            && name.charAt(1) == NUL)
        return char2CommandHistory(name.charAt(0));
    
    return null;
}

/**
 * Get indices "num1,num2" that specify a range within a list (not a range of
 * text lines in a buffer!) from a string.  Used for ":history" and ":clist".
 * Returns OK if parsed successfully, otherwise FAIL.
 * NOTE: str is used as a character iterator
 */
private static boolean get_list_range(StringSegment str,
                                      MutableInt p_num1, MutableInt p_num2)
{
    MutableInt p_len = new MutableInt();
    boolean first = false;
    MutableInt p_num = new MutableInt();
    
    skipwhite(str);
    if(str.current() == '-' || isdigit(str.current())) { // parse "from" part of range
        int tidx = str.getIndex();
        vim_str2nr(str, tidx, null, p_len, 0, 0, p_num, null);
        str.setIndex(tidx + p_len.getValue());
        p_num1.setValue(p_num.getValue());
        first = true;
    }
    skipwhite(str);
    if(str.current() == ',') { // parse "to" part of range
        str.next();
        skipwhite(str);
        int tidx;
        tidx = str.getIndex();
        vim_str2nr(str, tidx, null, p_len, 0, 0, p_num, null);
        if(p_len.getValue() > 0) {
            p_num2.setValue(p_num.getValue());
            str.setIndex(tidx + p_len.getValue());
            skipwhite(str);
        } else if(! first)
            return false;
    } else if(first)
        p_num2.setValue(p_num1.getValue());
    
    return true;
}

private List<HistEntry> getList()
{
    return history.getHistEntryList();
}

private static void processHistoryCommand(String command,
                                          Consumer<CommandHistory> consumeHistype,
                                          IntConsumer consumeCurrent,
                                          Consumer<HistEntry> consumeInfo)
{
    StringSegment arg = new StringSegment(command);
    EnumSet<CommandHistory> whichones = EnumSet.noneOf(CommandHistory.class);
    MutableInt p_hisidx1 = new MutableInt(1);
    MutableInt p_hisidx2 = new MutableInt(-1);
    skipwhite(arg);
    int argStartIdx = arg.getIndex();
    if(!(isdigit(arg.current())
            || arg.current() == '-' || arg.current() == ',')) {
        while(isalpha(arg.current())
                || vim_strchr(":/?", arg.current()) != null) {  // ":=@>/?"
            arg.next();
        }
        StringSegment targ = arg.subSequence(argStartIdx, arg.getIndex());
        CommandHistory which = get_histtype(targ);
        if(which != null)
            whichones.add(which);
        else {
            if(strnicmp(targ, "all", targ.length()) == 0) {
                whichones.addAll(EnumSet.allOf(CommandHistory.class));
            } else {
                emsg(Messages.e_trailing);
                return;
            }
        }
    }
    if(whichones.isEmpty())
        whichones.add(COLON);
    
    boolean ok = get_list_range(arg, p_hisidx1, p_hisidx2);
    
    dbgKeys.printf(Level.FINE, "history types: %s\n", whichones);
    dbgKeys.printf(Level.FINE, () ->
          sf("list_range: %b, %d,%d\n", ok, p_hisidx1.getValue(),
                                            p_hisidx2.getValue()));
    if(!ok || !arg.atEnd()) {
        emsg(Messages.e_trailing);
        return;
    }
    

    for(CommandHistory which : whichones) {
        List<HistEntry> hist = which.getList();
        consumeHistype.accept(which);
        int hislen = hist.size();
        if(hislen > 0) {
            
            // hist = history[histype1];
            int j = p_hisidx1.getValue();
            int k = p_hisidx2.getValue();
            dbgKeys.printf(Level.FINE, "before check: j = %d, k = %d\n", j, k);
            if (j < 0)
                j = (-j > hislen) ? 0 : hist.get(hislen + j).getIndex();
            if (k < 0)
                k = (-k > hislen) ? 0 : hist.get(hislen + k).getIndex();
            dbgKeys.printf(Level.FINE, "Hist range: j = %d, k = %d\n", j, k);
            consumeCurrent.accept(hist.get(hislen - 1).getIndex());
            if(j <= k)
                for(HistEntry he : hist) {
                    if(he.getIndex() >= j && he.getIndex() <= k) {
                        consumeInfo.accept(he);
                    }
                }
        }
    }
}

private static void processHistoryOutputCommand(String command)
{
    MutableInt pCurrentIndex = new MutableInt();
    Wrap<CommandHistory> pWhich = new Wrap<>();

    try (ViOutputStream os = ViManager.createOutputStream(null, ViOutputStream.LINES, "History",
            EnumSet.of(FLAGS.NEW_NO, FLAGS.RAISE_YES, FLAGS.CLEAR_YES))) {
        processHistoryCommand(command,
            (which) -> {
                os.println(sf("\n      #  %s history", which.commonName));
                pWhich.setValue(which);
            },
            (current) -> pCurrentIndex.setValue(current),
            (he) -> {
                int current = pCurrentIndex.getValue();
                String s = he.getCmd();
                int idx = he.getIndex();
                eatme(idx);
                os.print(sf("%c%6d  ",
                              he.getIndex() == current ? '>' : ' ',
                              he.getIndex()));
                CommandHistory which = pWhich.getValue();
                os.println(s, () -> invokeHistoryAction(
                        he.getIndex(), which, he.getCmd()));
            }
        );
    }
}

private static void invokeHistoryAction(int idx, CommandHistory which, String command)
{
    //System.err.println(sf("CLICK %s: %d", which, idx));
    if(ViManager.jViBusy()) {
        ViManager.dumpStack();
        return;
    }
    HistoryActionArgs actionArgs = new HistoryActionArgs(which, idx, command);
    getFactory().platformRequestsCommand((ViTextView)null, () ->
            GetChar.externalCommand(which == COLON ? ":" : "/", actionArgs));
}

    private static class HistoryActionArgs
    {
    final CommandHistory which;
    final int index;
    final String command;

    public HistoryActionArgs(CommandHistory which, int idx, String command)
    {
        this.which = which;
        this.index = idx;
        this.command = command;
    }
    }

private static HistoryActionArgs getHistoryActionArgs()
{
    Object o = GetChar.getExternalArgs();
    return o instanceof HistoryActionArgs ? (HistoryActionArgs)o : null;
}

    public static class InitialHistoryItem
    {
    private boolean atBeginning;
    private String initialItem;
    
    private InitialHistoryItem(boolean atBeginning, String initialItem)
    {
        this.atBeginning = atBeginning;
        this.initialItem = initialItem;
    }
    
    /** 
     * If atBeginning then typically the initiaItem is last push,
     * and will be highlighted in the first history text field display.
     * 
     * @return false if external action initialized pointer into history.
     */
    public boolean isAtBeginning()
    {
        return atBeginning;
    }
    
    /** Usually lastPush, but current() if !isAtBeginning */
    public String getInitialItem()
    {
        return initialItem;
    }
    
    }

    /**
     * :history command - print a history
     * See vim/src/cmdhist.c.
     */
    private static class History extends AbstractColonAction
    {
    @Override
    public void actionPerformed(ActionEvent e)
    {
        dbgKeys = Options.getDebugOption(Options.dbgKeyStrokes);
        ColonEvent cev = (ColonEvent)e;
        
        if(G.p_hi == 0) {
            smsg("'history' option is zero");
            return;
        }
        // work with the whole string to handle unusual syntax
        processHistoryOutputCommand(cev.getArgString());
    }
    }

    final public static class HistEntry
    {
    String hisstr;
    int hisnum;
    
    public HistEntry(String hisstr, int hisnum)
    {
        this.hisstr = hisstr;
        this.hisnum = hisnum;
    }
    
    public HistEntry(String hisstr)
    {
        this(hisstr, -1);
    }

    public String getCmd()
    {
        return hisstr;
    }

    public int getIndex()
    {
        return hisnum;
    }
    
    @Override
    public String toString()
    {
        return "" + hisnum + ":" + hisstr;
    }
    
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.hisstr);
        return hash;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        final HistEntry other = (HistEntry)obj;
        return Objects.equals(this.hisstr, other.hisstr);
    }
    
    }

    abstract static public class HistoryContext
    {
    final CommandHistory which;

    static HistoryContext create(CommandHistory which)
    {
        return new HistoryContextArrayList(which);
    }
    
    public HistoryContext(CommandHistory which)
    {
        this.which = which;
    }
    
    final static HistEntry dummy = new HistEntry("");
    
    /**
     * Usually return true and set up such that current() is empty string;
     * and next() returns the last  push. But in the case
     * of external initialization through HistoryActionArgs
     * return false.
     */
    abstract public InitialHistoryItem init();
    
    /**
     * Set the most recent item.
     * @param cmd put this at the top of stack
     */
    abstract public void push(String cmd);
    
    abstract public boolean atTop();
    
    abstract public String current();
    
    /**
     * @return the next item that startsWith filter
     */
    abstract public String next();
    
    /**
     * @return the previous item that startsWith filter
     */
    abstract public String prev();
    
    abstract public String getFilter();
    
    abstract public void setFilter(String filter);
    
    // non public
    
    abstract void trim();
    
    abstract int size();
    
    abstract List<HistEntry> getHistEntryList();
    
    abstract void copyList(List<String> l);
    
    // for unit test
    abstract HistEntry test_next();
    abstract HistEntry test_prev();
    abstract HistEntry test_current();

    public String get_register(char regname) {
        return get_register_value(regname);
    }
    }

    /**
     * Access to command line history. A filter can be specified and any
     * subsequent returns from next/prev startsWith the filter. The filter
     * is the input as it is being typed, 
     * 
     * next() returns old entries, prev() returns newer entries.
     * lastPush() is the most recent push(); it is the last command accepted. 
     * 
     * Typical usage:
     *      init()
     *      travers up/down the history list
     *      push() // selected or new entry
     */
    public final static class HistoryContextArrayList extends HistoryContext
    {
    private final ArrayList<HistEntry> history;
    private int histPushCount;
    private String filter;
    private int lastIndex;
    private final boolean checkState = true;
    private boolean ready = false;
    private final int DIR_NEXT = -1;
    private final int DIR_PREV = 1;


    //
    // ArrayList backwards.
    // next/prev go in opposite direction from what might be expected.
    // next presents successively old items, prev newer items.
    // It's more like a stack with the requirement of accessing stuff
    // in the middle bi-directionally. Use the list,
    // instead of just an array, because remove(index) is convenient.
    //
    // There's some state checking; all it verifies is thats
    // a push() must be followed by init(). There are many other times
    // init must be called.
    //
    // This does a lot of array moving; deletes in the middle,
    // push, trim() at the other end. A deque is almost
    // perfect; except that there's no bi-directional traversal.
    // Linked list is probably better, but dealing with that
    // ListIterator is cumbersome.
    //

    HistoryContextArrayList(CommandHistory which)
    {
        super(which);
        this.history = new ArrayList<>(G.p_hi + 4);
    }

    /**
     * Set up such that current() is empty string.
     * And next returns the last  push.
     * 
     * If there is a HistoryAction, then that gets set up
     * if a matching item is found.
     */
    @Override
    public InitialHistoryItem init()
    {
                                                if(checkState) ready = true;
        trim();
        lastIndex = history.size();
        setFilter("");

        return setupInitialState();
    }

    /** lastIndex already setup, adjust if HistoryAction match */
    private InitialHistoryItem setupInitialState()
    {
        if(history.isEmpty())
            return new InitialHistoryItem(false, "");

        boolean atBeginning = true;
        HistEntry he = history.get(history.size() - 1); // last push
        //String initialItem = history.get(history.size() - 1).hisstr;
        //int idx = history.size() - 1; // last push
        if(which != null) {
            // Kind of a cheat, which is only null during JUnitTests.
            // Problem is that JUnit cannot initialize GetChar class.
            HistoryActionArgs args = getHistoryActionArgs();
            
            if(args != null) {
                int idx;
                for(idx = 0; idx < history.size(); idx++) {
                    he = history.get(idx);
                    if(he.getIndex() == args.index
                            && he.getCmd().equals(args.command))
                        break;
                }
                if(idx != history.size()) {
                    lastIndex = idx;
                    atBeginning = false;
                } else {
                    EventQueue.invokeLater(() -> beep_flush());
                }
            }
        }
        return new InitialHistoryItem(atBeginning, he.hisstr);
    }

    /**
     * Set the most recent item.
     * @param cmd put this at the top of stack
     */
    @Override
    public void push(String cmd)
    {
        HistEntry he = new HistEntry(cmd, ++histPushCount);
        remove(he);
        history.add(he);
        trim();
                                                if(checkState) ready = false;
    }

    @Override
    public boolean atTop()
    {
        return lastIndex == history.size();
    }

    @Override
    public String current()
    {
                                                if(checkState) someAction();
        assert !(lastIndex > history.size() || lastIndex < 0);
        if(lastIndex > history.size() || lastIndex < 0)
            return null; // impossible
        if(lastIndex == history.size())
            return filter;
        return history.get(lastIndex).hisstr;
    }

    /**
     * @return the next item that startsWith filter
     */
    @Override
    public String next()
    {
                                                if(checkState) someAction();
        return fetch(DIR_NEXT);
    }
    
    /**
     * @return the previous item that startsWith filter
     */
    @Override
    public String prev()
    {
                                                if(checkState) someAction();
        return fetch(DIR_PREV);
    }

    @Override
    public void setFilter(String filter)
    {
                                                if(checkState) someAction();
        this.filter = filter;
    }

    @Override
    public String getFilter()
    {
        return filter;
    }

    @Override
    void trim()
    {
        trim(G.p_hi);
    }

    @Override
    int size()
    {
        return history.size();
    }

    // remove oldest items until length
    private void trim(int len)
    {
        while(history.size() > len) {
            history.remove(0);
        }
    }

    @Override
    List<HistEntry> getHistEntryList()
    {
        return Collections.unmodifiableList(history);
    }
    
    @Override
    void copyList(List<String> l)
    {
        ListIterator<HistEntry> iter = history.listIterator(history.size());
        l.clear();
        while(iter.hasPrevious())
            l.add(iter.previous().getCmd());
    }

    // side affect set's lastIndex
    private String fetch(int direction)
    {
        if(history.isEmpty())
            return null;
        HistEntry val = dummy;
        boolean found = false;
        int newIndex = lastIndex + direction;
        while(newIndex >= 0 && newIndex < history.size()) {
            val = history.get(newIndex);
            if(val.hisstr.startsWith(filter)) {
                found = true;
                break;
            }
            newIndex += direction;
        }
        if(found) {
            lastIndex = newIndex;
            return val.hisstr;
        }
        if(direction == DIR_PREV) {
            // gone off the end in the previous direction
            // lastIndex is allowed to be size, indiating that filter
            // should be shown.
            assert newIndex >= history.size();
            if(newIndex == history.size()) {
                lastIndex = newIndex;
                return filter;
            }
        }
        return null;
    }

    private void someAction()
    {
        if(ready == false)
            throw new IllegalStateException("command history not ready");
    }

    /**
     * Typically if an element is found it is recent.
     * So start at the end. Doesn't help with cache pre-fetch,
     * hope for long lines.
     */
    private void remove(HistEntry he)
    {
        String s = he.hisstr;
        for(int i = history.size() - 1; i >= 0; i--) {
            if(s.equals(history.get(i).hisstr)) {
                history.remove(i);
                return;
            }
        }
    }

    @Override
    HistEntry test_next()
    {
        String next = next();
        if(next == null)
            return null;
        return history.get(lastIndex);
    }

    @Override
    HistEntry test_prev()
    {
        String prev = prev();
        if(prev == null)
            return null;
        if(lastIndex == history.size())
            return new HistEntry(prev);
        return history.get(lastIndex);
    }

    @Override
    HistEntry test_current()
    {
        int idx = lastIndex;
        if(idx >= 0 && idx < history.size())
            return history.get(idx);
        if(lastIndex == history.size())
            return new HistEntry(filter);
        return null;
    }
    }

    // /** Use a listIterator to traverse the list */
    // public final static class HistoryContextArrayListIterator extends HistoryContext
    // {
    // private final ArrayList<HistEntry> history;
    // /** main ListIterator, for implementation of next/prev */
    // enum dir { ITER_PREV, ITER_NEXT }
    // private ListIterator<HistEntry> mainIter;
    // private dir lastDir;
    // private int lastIndex;
    // private HistEntry lastEntry;

    // private int histPushCount;
    // private String filter;
    // private final boolean checkState = true;
    // private boolean ready = false;


    // // THIS SEEMS TO BE A LOST CAUSE...
    // // THE FILTER FUNCTION COULD WORK IF THE ITERATOR COULD BE CLONED
    // //
    // // List backwards.
    // // next/prev go in opposite direction from what might be expected.
    // // next presents successively old items, prev newer items.
    // // It's more like a stack with the requirement of accessing stuff
    // // in the middle bi-directionally.
    // //
    // // There's some state checking; all it verifies is thats
    // // a push() must be followed by init(); multiple push ok.
    // // There are many other times init must be called.
    // //
    // // Track
    // //
    // HistoryContextArrayListIterator()
    // {
    //     this.history = new ArrayList<>(G.p_hi + 4);
    // }

    // /**
    //  * Set up such that current() is empty string.
    //  * And next returns the last  push.
    //  */
    // @Override
    // public void init()
    // {
    //                                             if(checkState) ready = true;
    //     trim();
    //     lastIndex = history.size();
    //     setFilter("");
    // }

    // /**
    //  * Set the most recent item.
    //  * @param cmd put this at the top of stack
    //  */
    // @Override
    // public void push(String cmd)
    // {
    //     HistEntry he = new HistEntry(cmd, ++histPushCount);
    //     remove(he);
    //     history.add(he);
    //     trim();
    //     initIterator();
    //                                             if(checkState) ready = false;
    // }

    // @Override
    // public String lastPush()
    // {
    //     if(history.isEmpty())
    //         return null;
    //     return history.get(history.size() -1).hisstr;
    // }

    // @Override
    // public boolean atTop()
    // {
    //     return lastIndex == history.size();
    // }

    // @Override
    // public String current()
    // {
    //                                             if(checkState) someAction();
    //     assert !(lastIndex > history.size() || lastIndex < 0);
    //     if(lastIndex > history.size() || lastIndex < 0)
    //         return null; // impossible
    //     if(lastIndex == history.size())
    //         return filter;
    //     return lastEntry.hisstr;
    // }

    // /**
    //  * @return the next item that startsWith filter
    //  */
    // @Override
    // public String next()
    // {
    //                                             if(checkState) someAction();
    //     return iterPrev();
    // }
    // 
    // /**
    //  * @return the previous item that startsWith filter
    //  */
    // @Override
    // public String prev()
    // {
    //                                             if(checkState) someAction();
    //     return iterNext();
    // }

    // @Override
    // public void setFilter(String filter)
    // {
    //                                             if(checkState) someAction();
    //     this.filter = filter;
    // }

    // @Override
    // public String getFilter()
    // {
    //     return filter;
    // }

    // @Override
    // void trim()
    // {
    //     trim(G.p_hi);
    // }

    // @Override
    // int size()
    // {
    //     return history.size();
    // }

    // // remove oldest items until length
    // private void trim(int len)
    // {
    //     while(history.size() > len) {
    //         history.remove(0);
    //         mainIter = null;
    //     }
    // }

    // @Override
    // List<HistEntry> getHistEntryList()
    // {
    //     return Collections.unmodifiableList(history);
    // }
    // 
    // @Override
    // void copyList(List<String> l)
    // {
    //     ListIterator<HistEntry> iter = history.listIterator(history.size());
    //     l.clear();
    //     while(iter.hasPrevious())
    //         l.add(iter.previous().getCmd());
    // }

    // String iterPrev()
    // {
    //     if(mainIter == null)
    //         initIterator();
    //     if(lastDir != ITER_PREV) {
    //         //mainIter.previous();
    //         lastDir = ITER_PREV;
    //     }
    //     int idx = mainIter.previousIndex();
    //     if(idx < 0) {
    //         assert lastIndex == 0;
    //         ////////
    //     }
    //     lastEntry = mainIter.previous();
    //     return null;
    // }

    // String iterNext()
    // {
    //     if(mainIter == null)
    //         initIterator();
    //     if(lastDir != ITER_NEXT) {
    //         mainIter.next();
    //         lastDir = ITER_NEXT;
    //     }
    //     return null;
    // }

    // void initIterator()
    // {
    //     mainIter = history.listIterator(history.size());
    //     lastDir = ITER_NEXT;
    //     lastIndex = history.size();
    //     lastEntry = null;
    // }

    // private void someAction()
    // {
    //     if(ready == false)
    //         throw new IllegalStateException("command history not ready");
    // }

    // /**
    //  * Typically if an element is found it is recent.
    //  * So start at the end. Doesn't help with cache pre-fetch,
    //  * hope for long lines.
    //  */
    // private void remove(HistEntry he)
    // {
    //     // Collection.remove(he); Could optim by find nearest to iter or begin or end
    //     // use fromEnd 
    //     history.remove(he);
    //     mainIter = null;
    //     // String s = he.hisstr;
    //     // for(int i = history.size() - 1; i > 0; i--) {
    //     //     if(s.equals(history.get(i).hisstr)) {
    //     //         history.remove(i);
    //     //         return;
    //     //     }
    //     // }
    // }

    // @Override
    // HistEntry test_next()
    // {
    //     String next = next();
    //     if(next == null)
    //         return null;
    //     return history.get(lastIndex);
    // }

    // @Override
    // HistEntry test_prev()
    // {
    //     String prev = prev();
    //     if(prev == null)
    //         return null;
    //     if(lastIndex == history.size())
    //         return new HistEntry(prev);
    //     return history.get(lastIndex);
    // }

    // @Override
    // HistEntry test_current()
    // {
    //     int idx = lastIndex;
    //     if(idx >= 0 && idx < history.size())
    //         return history.get(idx);
    //     if(lastIndex == history.size())
    //         return new HistEntry(filter);
    //     return null;
    // }
    // }

}
