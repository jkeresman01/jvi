package com.raelity.jvi.swing;

import com.raelity.jvi.manager.ViManager;

import java.awt.EventQueue;
import java.awt.Point;
import java.util.Objects;

import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import org.openide.util.Exceptions;

import com.raelity.jvi.lib.*;

import static com.raelity.text.TextUtil.sf;

/**
 * Stabilize (do not allow scrolling) the JViewport displaying
 * the indicated JTextComponent.
 * This is typically used when the underlying document may change while
 * being edited in another view. The {@link #stop} method is used to release
 * the listeners and so unfreeze the viewport.
 * <p>This is a one shot class. The editor is expected to be good to go.
 * Only document changes are listened to. The first char of the top line is
 * pinned to the upper left corner. If needed, this could be extended
 * to pin the horizontal position as well.
 */
public class FreezeViewport implements DocumentListener, ChangeListener
{
    private JTextComponent ep;
    private JViewport vp;
    private AbstractDocument doc;
    private Position frozenMark;
    private int topLine;
    private int nLine;
    private boolean doc_busy;

    private static void eatme(Object... o) { Objects.isNull(o); }

    public FreezeViewport(JTextComponent ep)
    {
        this.ep = ep;
        Object o = ep.getDocument();
        if (!(o instanceof AbstractDocument)) {
            return;
        }
        doc = (AbstractDocument)ep.getDocument();
        try {
            doc.readLock();
            vp = (JViewport)ViManager.getFactory().getViewport(ep);
            if(vp == null)
                return;
            Element root = doc.getDefaultRootElement();
            nLine = root.getElementCount();
            vpCapturePinLocation();
            setupListeners();
        } catch (BadLocationException ex) {
            // Note: did not start listener
        } finally {
            doc.readUnlock();
        }
    }

    /** Pin visible top line to top of the viewpoint.
     * Calculate values to freeze the top line.
     */
    private void vpCapturePinLocation() throws BadLocationException
    {
        topLine = vpCalcTopLine(null);
        int offset = doc.getDefaultRootElement().getElement(topLine).getStartOffset();
        // Get marker to offset in the document.
        // This is the pin.
        frozenMark = doc.createPosition(offset);
    }
    
    /** Return calculated/current top line of viewport.
     * Optionally return the offset of the top line's first visible char;
     * may not be first char of line when horizontal scroll.
     */
    private int vpCalcTopLine(MutableInt resultOffset)
    {
        Point pt = vp.getViewPosition();
        int offset = ep.viewToModel2D(pt);
        if(resultOffset != null)
            resultOffset.setValue(offset);
        return doc.getDefaultRootElement().getElementIndex(offset);
    }

    private int lastEvent;
    /**
     * Prevent frozen/idle viewport from visibly scrolling.
     * Brings viewport back to frozen mark.
     * Called from document event.
     * Gets done after returning from the listener.
     * @param offset 
     */
    private void vpReposition(int targetLine)
    {
        // https://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield
        // But, that article does the work on the first "later" action.
        // This does the work on the last "later" action,
        // to easily use targetLine and in case timing matters.
        final int myevent = ++lastEvent;
        EventQueue.invokeLater(() -> {
            if(myevent != lastEvent) {
                dbgOut(sf("vp repo skip %d", myevent));
                return;
            }

            try {
                int offset = doc.getDefaultRootElement()
                        .getElement(targetLine).getStartOffset();
                Point pt = ep.modelToView2D(offset).getBounds().getLocation();
                pt.translate(-pt.x, 0); // leaves a few pixels to left
                vp.setViewPosition(pt);

                dbgOut(sf("vp repo: %s", vpDebugPos()));
                doc_busy = false;
            } catch (BadLocationException ex) {
                stop();
            }
        });
    }

    /** Keep the top line pinned in the viewport.
     * If this doc event would cause a scroll, correct for it.
     */
    private void docChangeEvent(DocumentEvent e)
    {
        // Note while in listener document can't change, no read lock
        Element root = doc.getDefaultRootElement();
        int newNumLine = root.getElementCount();
        // return if line count unchanged or changed after our mark
        if (nLine == newNumLine || e.getOffset() > frozenMark.getOffset()) {
            dbgOut(sf("doc event: no effect"));
            return;
        }
        nLine = newNumLine;
        int newTopLine = root.getElementIndex(frozenMark.getOffset());
        if (topLine == newTopLine) {
            dbgOut(sf("doc event: same line position"));
            return;
        }
        topLine = newTopLine;
        // make a move
        doc_busy = true;
        dbgOut(sf("doc evt: %s", vpDebugPos()));
        vpReposition(topLine);
    }

    private void vpChangeEvent(ChangeEvent e)
    {
        eatme(e);
        dbgOut(sf("vp chng: %s", vpDebugPos()));
        if(doc_busy)
            return;
        int prevTopLine = topLine;
        try {
            vpCapturePinLocation();
            if(topLine != prevTopLine)
                dbgOut(sf("vp chng: adjusting topLine" ));
        } catch(BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private String vpDebugPos()
    {
        MutableInt mi = new MutableInt();
        int calc = vpCalcTopLine(mi);
        return sf("%d: %3s top=%d calc=%d off=%d mark=%d",
                  lastEvent, doc_busy ? "bsy" : "", topLine,
                  calc, mi.getValue(), frozenMark.getOffset());
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void dbgOut(String s)
    {
        if(Boolean.FALSE)
            System.err.println(s);
    }

    private void setupListeners()
    {
        doc.addDocumentListener(this);
        vp.addChangeListener(this);
    }

    public void stop()
    {
        if (doc != null) {
            doc.removeDocumentListener(this);
        }
        if (vp != null) {
            vp.removeChangeListener(this);
        }
    }

    // Document listener

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        docChangeEvent(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        docChangeEvent(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
    }

    // Viewport state change

    @Override
    public void stateChanged(ChangeEvent e)
    {
        vpChangeEvent(e);
    }
}
