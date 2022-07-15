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

package com.raelity.jvi.core;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.eventbus.Subscribe;

import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.lib.Constants.CBU;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.*;
import com.raelity.text.TextUtil;

import static java.util.logging.Level.*;

import static com.raelity.jvi.core.Ops.LOG;
import static com.raelity.jvi.core.Register.*;
import static com.raelity.jvi.lib.LibUtil.assignContents;
import static com.raelity.text.TextUtil.sf;

/** return clipboard for idx, else null if not valid idx
 * or clipboard is not available. */
// static JviClipboard idx2Cb(int idx)
// {
//   return JviClipboard.STAR.idx2cbCheck(idx) ? JviClipboard.STAR
//          : JviClipboard.PLUS.idx2cbCheck(idx) ? JviClipboard.PLUS
//          : JviClipboard.NO_CB;
// }
//private static final Permission PERM_CLIP = new AWTPermission("accessClipboard");

/** either systemClipboard or systemSelection */
enum JviClipboard implements ClipboardOwner
{
STAR(STAR_REGISTER, '*', CBU.UNNAMED, tk -> tk.getSystemSelection()),
PLUS(PLUS_REGISTER, '+', CBU.UNNAMED_PLUS, tk -> tk.getSystemClipboard()),
NO_CB(-1, '\uffff', null, tk -> null);

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
    @Override
    public void init()
    {
        JviClipboard.init();
    }
    }

private static void init()
{
    ViEvent.getBus().register(new Object() {
    @Subscribe public void boot(ViEvent.Boot ev) {
        initDataTransferFlavors();
        ViEvent.getBus().unregister(this);
    }
    });
    if(JviClipboard.ignoreOwner)
        Exceptions.printStackTrace(new Exception("ignoreOwner"));
}

static {
    OptionEvent.getEventBus().register(new Object() {
    @Subscribe public void parseClipboardOption(OptionEvent.Initialized ev) {
        SetView<CBU> new_unnamed = Sets.intersection(G.p_cb, unnamed_all);
        if(G.global_busy)
	    // clip_unnamed will be reset to clip_unnamed_saved
	    // at end_global_changes
            assignContents(clip_unnamed_saved, new_unnamed);
        else
            assignContents(clip_unnamed, new_unnamed);
        dbg.println(CONFIG, () -> sf("ClipOption: clip_unnamed: %s", clip_unnamed));
    }
    @Subscribe public void checkClipboardOption(OptionEvent.Global ev) {
        if(Options.clipboard.equals(ev.getName()))
            parseClipboardOption(null);
    }
    });
}

//
// Save and restore clip_unnamed before doing possibly many changes. This
// prevents accessing the clipboard very often which might slow down Vim
// considerably.
//
private static int global_change_count = 0; // if set, inside a start_global_changes
private static boolean clipboard_needs_update = false; // clipboard needs to be updated
private static boolean clip_did_set_selection = true;

// (start|end)_global_changes() from VIM9
/**
 * Save clip_unnamed and reset it.
 */
// v7.4.396
static void start_global_changes()
{
    if (++global_change_count > 1)
	return;
    assignContents(clip_unnamed_saved, clip_unnamed);
    clipboard_needs_update = false;

    if (clip_did_set_selection)
    {
	clip_unnamed.clear();
	clip_did_set_selection = false;
    }
}

static { if(Boolean.FALSE)is_clipboard_needs_update(); /*eatme*/ }
/**
 * Return TRUE if setting the clipboard was postponed, it already contains the
 * right text.
 */
private static boolean is_clipboard_needs_update()
{
    return clipboard_needs_update;
}

/**
 * Restore clip_unnamed and set the selection when needed.
 */
static void end_global_changes()
{
    if (--global_change_count > 0)
	// recursive
	return;
    if (!clip_did_set_selection)
    {
	clip_did_set_selection = true;
        assignContents(clip_unnamed, clip_unnamed_saved);
	clip_unnamed_saved.clear();
	if (clipboard_needs_update)
	{
            dbg.println(FINE,
                () -> TextUtil.sf("Clipb: end_global_changes: %s", clip_unnamed));
	    // only store something in the clipboard,
	    // if we have yanked anything to it
	    if (clip_unnamed.contains(CBU.UNNAMED))
		clip_star.clip_gen_set_selection();
	    if (clip_unnamed.contains(CBU.UNNAMED_PLUS))
		clip_plus.clip_gen_set_selection();
	}
    }
    clipboard_needs_update = false;
}

private static final DebugOption dbg = G.dbgOps;

private static final String UTF8 = "utf-8";

static final JviClipboard clip_star = JviClipboard.STAR;
static final JviClipboard clip_plus = JviClipboard.PLUS;

/** following just for similar looking code */
static final boolean clip_autoselect_plus = false;
static final boolean clip_autoselect_star = false;

static final EnumSet<CBU> unnamed_all = EnumSet.of(CBU.UNNAMED, CBU.UNNAMED_PLUS);
static final Set<CBU> clip_unnamed = EnumSet.noneOf(CBU.class);
static final Set<CBU> clip_unnamed_saved = EnumSet.noneOf(CBU.class);
static final Set<CBU> clip_unnamed_union
        = Sets.union(clip_unnamed, clip_unnamed_saved);

static public boolean cbOptHasUnnamed() {
    return clip_unnamed.contains(CBU.UNNAMED) || clip_unnamed.contains(CBU.UNNAMED_PLUS);
}

static public boolean isUnnamed(char name)
{
    return name == '*' && clip_unnamed.contains(CBU.UNNAMED)
            || name == '+' && clip_unnamed.contains(CBU.UNNAMED_PLUS);
}

static public boolean isUnnamedOrUnnamedSaved()
{
    return clip_unnamed_union.contains(CBU.UNNAMED)
            || clip_unnamed_union.contains(CBU.UNNAMED_PLUS);
}

/*
 * When "regname" is a clipboard register, obtain the selection.  If it's not
 * available return zero, otherwise return "regname".
 */
// return zero if specified yankreg name is not a usable clipboard
static char may_get_selection(char c)
{
    JviClipboard cb = name2Cb(c);
    if(!isValidCb(cb))
        return 0;
    cb.clip_get_selection();
    return c;
}

static void may_set_selection()
{
    if(isStarRegister(get_y_current()))
        JviClipboard.STAR.clip_gen_set_selection();
    else if(isPlusRegister(get_y_current()))
        JviClipboard.PLUS.clip_gen_set_selection();
}

/** is the the specified yankreg index a usable clipboard */
static boolean isCbIdx(int idx)
{
    return idx == JviClipboard.STAR.yank_reg_idx
            || idx == JviClipboard.PLUS.yank_reg_idx;
}

/** is the the specified yank reg name a clipboard */
static boolean isCbName(char c)
{
    return c == JviClipboard.STAR.regname
            || c == JviClipboard.PLUS.regname;
}

/** convert clipboard yankreg idx to yankreg name */
static char idx2CbName(int idx)
{
    return idx == JviClipboard.STAR.yank_reg_idx
          ? JviClipboard.STAR.regname
          : idx == JviClipboard.PLUS.yank_reg_idx
            ? JviClipboard.PLUS.regname
            : 0;
}

/** convert clipboard yankreg name to yankreg idx */
static int name2CbIdx(char c)
{
    return c == JviClipboard.STAR.regname
          ? JviClipboard.STAR.yank_reg_idx
          : c == JviClipboard.PLUS.regname
            ? JviClipboard.PLUS.yank_reg_idx
            : 0;
}

//////////////////////////////////////////////////////////////////////
//
// The above methods work on clipboard names and indexes. They do not
// check if the clipboard is actually available.
 
/** Return true, the clipboard exists and can be used. */
static boolean isValidCb(JviClipboard cb)
{
    //return cb != JviClipboard.NO_CB;
    return cb != null && cb.avail;
}

// The following methods return a clipboard. They return a dummy clipboard,
// NO_CB, if the argument is out of range or if the clipboard is not available.
 
/** return clipboard for regname, else null if not valid name
 * or clipboard is not available. */
static JviClipboard name2Cb(char regname)
{
    return JviClipboard.STAR.name2cbCheck(regname) ? JviClipboard.STAR
          : JviClipboard.PLUS.name2cbCheck(regname) ? JviClipboard.PLUS
            : JviClipboard.NO_CB;
}


private static boolean permOK()
{
    //SecurityManager sm = System.getSecurityManager();
    //if(sm != null) {
    //  //Object context = sm.getSecurityContext();
    //  try {
    //    //sm.checkPermission(PERM_CLIP, context);
    //    sm.checkPermission(PERM_CLIP);
    //    return true;
    //  } catch(SecurityException ex) {
    //    return false;
    //  }
    //}
    return true;
}

/////////////////////////////////////////////////////////////////////////////
//
// INSTANCE  JviClipboard

final int yank_reg_idx;
final char regname;
private final Function<Toolkit, Clipboard> toCb;
final CBU cbUnnamedFlag;
final boolean avail;
private boolean clipboard_owned;

private JviClipboard(int yank_reg_idx, char regname, CBU cbUnnamedFlag,
                     Function<Toolkit, Clipboard> toCb)
{
    this.yank_reg_idx = yank_reg_idx;
    this.regname = regname;
    this.cbUnnamedFlag = cbUnnamedFlag;
    this.toCb = toCb;
    this.avail = permOK() && getClipboard() != null;
}

private Clipboard getClipboard()
{
    return toCb.apply(Toolkit.getDefaultToolkit());
}

boolean name2cbCheck(char name)
{
    return name == regname && avail;
}

//private boolean idx2cbCheck(int idx)
//{
//    return idx == yank_reg_idx && avail;
//}

void clip_gen_set_selection()
{
    dbg.println(FINE,
               () -> TextUtil.sf("Clipb: clip_gen_set_selection entry: %s", this));
    if(!avail)
        return;

    // v7.4.396
    if (!clip_did_set_selection)
    {
        // Updating postponed, so that accessing the system clipboard won't
        // hang Vim when accessing it many times (e.g. on a :g comand).
        if(clip_unnamed_saved.contains(cbUnnamedFlag)) {
            clipboard_needs_update = true;
            return;
        }
    }

    Clipboard cb = getClipboard();
    if(cb == null)
        return;
    Yankreg y_reg = y_regs.get(yank_reg_idx);
    if(y_reg.isEmpty())
        return;
    Transferable trans = new StringAndVimSelection(y_reg.getType(), y_reg.getAll());
    synchronized(this) {
        dbg.println(CONFIG, () -> TextUtil.sf(
                "Clipb: clip_gen_set_selection setContents: %s", this));
        cb.setContents(trans, this);
        clipboard_owned = true;
    }
}

@SuppressWarnings(value = "FieldMayBeFinal")
static final boolean ignoreOwner = false;

/*
 * Get the selected text and put it in register '*' or '+'.
 */
void clip_get_selection()
{
    dbg.println(CONFIG,
               () -> TextUtil.sf("Clipb: clip_get_selection: %s", this));
    if(!avail)
        return;
    if(clipboard_owned && !ignoreOwner) {
        // whatever is in the clipboard, we put there. So just return.
        dbg.println(CONFIG,
                    () -> TextUtil.sf("Clipb: clip_get_selection: owner: %s", this));
        return;

        //      This method's comment talks about getting selected text to '*'
        // NEEDSWORK: clip_get_selection, code about clipboard.start/end...
        //            I think it's about copying the selection,
        //            and that's handled by platform for jVi. (I think)
        //            For example, visual select
        //              - visual select some stuff
        //              - ^G (converts visual select to platform select)
        //              - now "*p shows that the platform selected stuff is
        //                now in the STAR register

        //      if(!y_regs.get(yank_reg_idx).isEmpty())
        //          return;
	//      // Get the text between clip_star.start & clip_star.end
        //      // save stuff; do_pending_operator; restore things
    }
                // TODO: what's this about?
                //clip_free_selection(cb/this);
                //clip_gen_request_selection(cb/this);
    // Try to get selected text from another window
    Clipboard cb = getClipboard();
    if(cb == null)
        return;
    DataFlavor[] dfa = cb.getAvailableDataFlavors();
    if(dbg.getBoolean(FINER)) {
        if(hasVimFlavor(cb))
            dbg.println(FINER, "VimClip available");
        Arrays.sort(dfa,
                   (DataFlavor df1, DataFlavor df2) ->
                           df1.getMimeType().compareTo(df2.getMimeType()));
        int i = 0;
        dbg.printf(FINER, "===================== %d flavors\n", dfa.length);
        for(int j = 0; j < dfa.length && j < 5; j++) {
            DataFlavor df = dfa[j];
            dbg.printf(FINER, "%4d %s\n", i++, df.getMimeType());
        }
    }
    String stringData = null;
    Integer type = null;
    if(dfa == null || dfa.length == 0) {
        LOG.warning(() -> TextUtil.sf("Clipboard-%s: no data flavors", this));
        stringData = "";
    } else {
        if(hasVimFlavor(cb)) {
            Object[] data = getClipboardData(cb);
            if(data != null) {
                type = (Integer)data[0];
                stringData = (String)data[1];
            }
        }
        if(stringData == null) {
            try {
                // leave type null, let it get figured out
                stringData = (String)cb.getData(DataFlavor.stringFlavor);
            } catch(UnsupportedFlavorException | IOException ex) {
                LOG.warning(() ->
                        TextUtil.sf("Clipboard-%s: no stringFlavor", this));
            }
        }
        dbg.printf(CONFIG, "clipboard-%s: vimclip: %b, type %s, val='%s'\n",
                        this, hasVimFlavor(cb), type,
                        TextUtil.debugString(stringData)); }
    if(stringData != null) {
        byte byte_type = type == null ? 0 : type.byteValue();
        y_regs.get(yank_reg_idx).setData(stringData, byte_type);
    }
}

// return[0]:type, return[1]:string
@SuppressWarnings(value = "empty-statement")
private Object[] getClipboardData(Clipboard cb)
{
    if(!avail)
        return null;
    if(hasVimFlavor(cb)) {
        try {
            byte[] data = null;
            ByteBuffer buf = null;
            if(cb.isDataFlavorAvailable(VimClipboardByteBuffer))
                buf = (ByteBuffer)cb.getData(VimClipboardByteBuffer);
            else if(cb.isDataFlavorAvailable(VimClipboardByteArray))
                data = (byte[])cb.getData(VimClipboardByteArray);
            else
                return null;
            Integer type = null;
            String stringData = null;
            if(buf != null) {
                // use a byte buffer
                if(buf.limit() > 2) {
                    CharsetDecoder cd = Charset.forName(UTF8).newDecoder();
                    type = (int)buf.get();
                    while(buf.position() < buf.limit() && buf.get() != 0)
                        ;
                    // buf.slice() not public until jdk-13
                    // -1 not first char, -1 not the terminating 0,
                    ByteBuffer buf2 = sliceSim(buf, 1, buf.position() - 2);
                    String charsetName = cd.decode(buf2).toString();
                    // buf positioned at char after null
                    cd = Charset.forName(charsetName).newDecoder();
                    stringData = cd.decode(buf).toString();
                }
            } else if(data != null) {
                // use a byte array
                if(data.length > 2) {
                    type = (int)data[0];
                    int pos;
                    for(pos = 1; pos < data.length && data[pos] != 0; pos++)
                        ;
                    if(pos < data.length - 1) {
                        // at least one byte after mark
                        // use utf-8 in following by assumption/definition
                        String charsetName = new String(data, 1, pos - 1, "utf-8");
                        pos++; // first char of string data;
                        stringData = new String(data, pos, data.length - pos, charsetName);
                    }
                }
            }
            if(type != null && stringData != null)
                return new Object[]{type, stringData};
        } catch(UnsupportedFlavorException | IOException ex) {
            Misc01.beep_flush();
            LOG.warning(() ->
                    TextUtil.sf("Clipboard-%s: getClipboardData: %s",
                               ex.getMessage()));
        }
    }
    return null;
}

/**
 * Lost clipboard ownership, implementation of ClibboardOwner.
 */
@Override
public void lostOwnership(Clipboard cb, Transferable contents)
{
    synchronized(this) {
        dbg.println(CONFIG,
                () -> TextUtil.sf("Clipb: lostOwnership: %s", this));
        clipboard_owned = false;
        LOG.fine("clipboard: lostOwnership");
    }
}

// END INSTANCE  JviClipboard
////////////////////////////////////////////////////////////////////////////


    class StringAndVimSelection extends StringSelection
    {
    private final byte type;
    
    public StringAndVimSelection(byte type, String data)
    {
        super(data);
        this.type = type;
    }
    
    private ByteBuffer allocBuf(CharsetEncoder ce, String stringData, boolean max)
    {
        int nData
                = max
                 ? (int)Math.ceil((ce.averageBytesPerChar() + .1) *
                         stringData.length())
                 : (int)Math.ceil(ce.maxBytesPerChar() * stringData.length()) + 10;
        return ByteBuffer.allocate(1 + UTF8.length() + 1 + nData);
    }
    
    @Override
    public Object getTransferData(DataFlavor flavor) throws
            UnsupportedFlavorException,
           IOException
    {
        dbg.println(CONFIG, () ->
                TextUtil.sf("Clipb: getTransferData: %s: isVim %b: name %s: %s",
                           JviClipboard.this,
                           VIM_CLIPBOARD.equals(flavor.getHumanPresentableName()),
                           flavor.getHumanPresentableName(), flavor));
        if(VIM_CLIPBOARD.equals(flavor.getHumanPresentableName())) {
            String stringData = (String)super.getTransferData(DataFlavor.stringFlavor);
            CharsetEncoder ce = Charset.forName(UTF8).newEncoder();
            ByteBuffer buf = allocBuf(ce, stringData, false);
            for(int i = 0; i < 2; i++) {
                buf.put(type);
                buf.put(UTF8.getBytes(UTF8));
                buf.put((byte)0);
                CoderResult rc = ce.encode(CharBuffer.wrap(stringData), buf, true);
                if(!rc.isError()) {
                    if(ByteBuffer.class.equals(flavor.getRepresentationClass())) {
                        // return this as read only
                        buf.limit(buf.position());
                        buf.rewind();
                        return buf.asReadOnlyBuffer();
                    } else if(byte[].class.equals(flavor.getRepresentationClass())) {
                        // create and return a byte array
                        if(buf.hasArray()) {
                            byte[] b = new byte[buf.position()];
                            buf.rewind();
                            buf.get(b);
                            return b;
                        } else
                            break; // impossible
                    }
                }
                dbg.println(INFO,
                           () -> TextUtil.sf("Clipb: getTransferData: RETRY bigger buf "));
                if(i == 0 && rc.isOverflow()) {
                    // over first time through, try again.
                    if(ByteBuffer.class.equals(flavor.getRepresentationClass())) {
                    }
                    buf = allocBuf(ce, stringData, true);
                }
            }
            throw new IOException("Can not encode data");
        }
        return super.getTransferData(flavor);
    }
    
    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        if(VIM_CLIPBOARD.equals(flavor.getHumanPresentableName()))
            return true;
        return super.isDataFlavorSupported(flavor);
    }
    
    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        DataFlavor[] fs = super.getTransferDataFlavors();
        final int nVim = 2;
        DataFlavor[] result = new DataFlavor[fs.length + nVim];
        result[0] = VimClipboardByteArray;
        result[1] = VimClipboardByteBuffer;
        System.arraycopy(fs, 0, result, nVim, fs.length);
        return result;
    }
    } // END CLASS StringAndVimSelection ==================================

// not public until jdk-13
static ByteBuffer sliceSim(ByteBuffer buf, int index, int length)
{
    return buf.duplicate().position(index).limit(index + length);
}

//public static final String JVI_CLIPBOARD = "JviClipboard";
public static final String VIM_CLIPBOARD = "VimClipboard";
public static final String VIM_CLIPBOARD_ATOM = "_VIMENC_TEXT";
public static final DataFlavor VimClipboardByteArray
        = new DataFlavor(byte[].class, VIM_CLIPBOARD);
public static final DataFlavor VimClipboardByteBuffer
        = new DataFlavor(ByteBuffer.class, VIM_CLIPBOARD);

static boolean hasVimFlavor(Clipboard cb)
{
    return hasVimFlavor(cb.getAvailableDataFlavors());
}

static boolean hasVimFlavor(DataFlavor[] fs)
{
    for(DataFlavor f : fs) {
        if(VIM_CLIPBOARD.equals(f.getHumanPresentableName()))
            return true;
    }
    return false;
}

private static void initDataTransferFlavors()
{
    SystemFlavorMap sfm = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();
    //((SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap())
    sfm.addFlavorForUnencodedNative(VIM_CLIPBOARD_ATOM, VimClipboardByteArray);
    sfm.addUnencodedNativeForFlavor(VimClipboardByteArray, VIM_CLIPBOARD_ATOM);
    sfm.addFlavorForUnencodedNative(VIM_CLIPBOARD_ATOM, VimClipboardByteBuffer);
    sfm.addUnencodedNativeForFlavor(VimClipboardByteBuffer, VIM_CLIPBOARD_ATOM);
}

/**
 * Adjust the register name pointed to with "rp" for the clipboard being
 * used always and the clipboard being available.
 */
// v7.4.396
static char adjust_clip_reg(char rp)
{
    // If no reg. specified, and "unnamed" or "unnamedplus" is in 'clipboard',
    // use '*' or '+' reg, respectively. "unnamedplus" prevails.
    if(rp == 0 && !clip_unnamed_union.isEmpty())
    {
        if(!clip_unnamed.isEmpty())
            rp = clip_unnamed.contains(CBU.UNNAMED_PLUS) && clip_plus.avail
                 ? '+' : '*';
        else
            rp = clip_unnamed_saved.contains(CBU.UNNAMED_PLUS) && clip_plus.avail
                 ? '+' : '*';
    }
    if(isCbName(rp) && !isValidCb(name2Cb(rp)))
        rp = 0;
    return rp;
}

} // END ENUM JviClipboard
