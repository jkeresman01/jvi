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
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Timer;

import org.openide.util.lookup.ServiceProvider;

import com.raelity.jvi.*;
import com.raelity.jvi.core.Commands.AbstractColonAction;
import com.raelity.jvi.core.Commands.ColonAction;
import com.raelity.jvi.core.Commands.ColonEvent;
import com.raelity.jvi.core.lib.*;
import com.raelity.jvi.manager.*;
import com.raelity.jvi.options.*;

import static com.raelity.jvi.core.Misc01.*;
import static com.raelity.jvi.manager.ViManager.eatme;
import static com.raelity.jvi.lib.TextUtil.*;



/**
 * TODO: unit test. only win.buf.getsegment to deal with
 * <p>
 * NOTE: writing to the process can easily have an acceptable exception,
 * for example: ":.!date" writes a line to the date command, though the date
 * command does not read standard input, and if the date command finishes
 * before the write or close exceptions occur.
 * </p>
 * 
 * @author Ernie Rael <err at raelity.com>
 */
public class CcBang
{
private static final Logger LOG = Logger.getLogger(CcBang.class.getName());

    @ServiceProvider(service=ViInitialization.class, path="jVi/init", position=10)
    public static class Init implements ViInitialization
    {
    @Override
    public void init()
    {
        CcBang.init();
    }
    }

private static void init()
{
    Commands.register("!", "!", ACTION_bang,
                           EnumSet.of(CcFlag.XFILE, CcFlag.NO_PARSE));
}


static String lastBangCommand = null;

private static final ColonAction ACTION_bang = new BangAction();

private CcBang()
{
}

private static int last_exit;
public static int getLastExit() { return last_exit; }

public static class BangAction extends AbstractColonAction
{
    FilterThreadCoordinator coord = null;
    Future<Integer> exitValue;
    DebugOption dbg = Options.getDebugOption(Options.dbgBang);
        @Override
        public EnumSet<CcFlag> getFlags()
        {
            return EnumSet.of(CcFlag.RANGE);
        }

        @Override
    public void actionPerformed(ActionEvent ev)
    {
        if(coord != null) {
            Msg.emsg("Only one command at a time.");
            return;
        }
        ColonEvent evt = (ColonEvent)ev;
        int nArgs = evt.getNArg();
        dbg.println(() -> "!: cmdLine '" + evt.getArgString() + "'");

        if (nArgs >= 1) {
            //String cmd = arg.toString();
            String cmd = evt.getArgString();
            doBangCommand(evt, cmd);
            if(coord == null) {
                return;
            }
            lastBangCommand = cmd;
        } else {
            lastBangCommand = "";
        }
        String lastBangCommandFinal = lastBangCommand;
        dbg.println(() -> "!: Last command saved '" + lastBangCommandFinal + "'");
    }

    private void joinThread(FilterThread t)
    {
        dbg.println(() -> "!: joining thread " + t.getName());
        if(t.isAlive()) {
            t.interrupt("joinThread");
            try {
                t.join(50);
            } catch (InterruptedException ex) {
            }
            if(t.isAlive()) {
                LOG.warning(() -> sf("Thread %s won't die", t.getName()));
            }
        }
    }

    void finishBangCommand(boolean fOK)
    {
        assert(EventQueue.isDispatchThread());
        if(coord == null)
            return;
        coord.exitting = true;
        dbg.println(() -> sf("!: finishBangCommand: %s, alive: %s",
                             !fOK ? " ABORT" : "OK", coord.process.isAlive()));
        ViManager.getFactory().stopGlassKeyCatch();
        Msg.wmsg("");
        
        try {
            if(coord.process.isAlive()) {
                dbg.println("!: finishBangCommand: destroying process");
                coord.process.destroy();
            }
            last_exit = exitValue.get();
        } catch(InterruptedException|ExecutionException ex) { }

        if(!fOK) {
            if(coord.simpleExecuteThread != null) {
                coord.simpleExecuteThread.interrupt("finishBangCommand");
            } else {
                coord.documentThread.interrupt("finishBangCommand");
                coord.readFromProcessThread.interrupt("finishBangCommand");
                coord.writeToProcessThread.interrupt("finishBangCommand");
            }
        }

        if(coord.simpleExecuteThread != null) {
            joinThread(coord.simpleExecuteThread);
        } else {
            joinThread(coord.documentThread);
            joinThread(coord.readFromProcessThread);
            joinThread(coord.writeToProcessThread);
        }

        /* THESE CALLS WILL PROBABLY DEADLOCK.
            * IN ONE EXAMPLE, WE ARE IN A STUCK IN BUFFERED READER
            * READ, CALLING CANCEL ATTEMPTS TO CLOSE THE READER (FROM A
            * DIFFERENT THREAD NOLESS) THE CLOSE HANGS.
            *
            * WE'VE DONE LOTS TO TRY AND KILL THE THREAD CLEANLY,
            * IF IT WONT DIE, THEN TOO BAD.
            // These calls to cleanup don't make much sense,
            // we're in the wrong thread context.
            // But then again, they should be no-ops
            if(coord.simpleExecuteThread != null) {
            coord.simpleExecuteThread.cleanup();
            } else {
            coord.documentThread.cleanup();
            coord.readFromProcessThread.cleanup();
            coord.writeToProcessThread.cleanup();
            }
        */

        coord = null;
    }

    private boolean doBangCommand(ColonEvent evt, String commandLine)
    {
        boolean isFilter = (evt.getAddrCount() > 0);

        ArrayList<String> shellCommandLine = new ArrayList<>(3);
        String shellXQuote = G.p_sxq;

        shellCommandLine.add(G.p_sh);
        shellCommandLine.add(G.p_shcf);
        shellCommandLine.add(shellXQuote + commandLine + shellXQuote);

        dbg.println(() -> "!: ProcessBuilder: '" + shellCommandLine + "'");

        ProcessBuilder pb = new ProcessBuilder(shellCommandLine);
        pb.redirectErrorStream(true);
        pb.directory(VimPath.getCwd().toFile());

        Process p;
        try {
            p = pb.start();
            exitValue = p.onExit().thenApply(p1 -> {
                int ev = p1.exitValue();
                dbg.println(() -> sf("!: Bang process: exit 0x%x", ev));
                return ev;
            });
        } catch (IOException ex) {
            String s = ex.getMessage();
            Msg.emsg(s == null || s.equals("") ? "exec failed" : s);
            return false;
        }

        // Got a process, setup coord, start modal operation

        coord = new FilterThreadCoordinator(
                this, ExCommands.setupExop(evt, false), p);

        // NEEDSWORK: put this where it can be repeated
        coord.statusMessage = "Enter 'Ctrl-C' to ABORT";
        Msg.fmsg(coord.statusMessage);

        ViManager.getFactory().startGlassKeyCatch(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    e.consume();
                    if(e.getKeyChar() == (KeyEvent.VK_C & 0x1f)
                            && e.isControlDown()) {
                        dbg.println(() -> "!: Ctrl-C");
                        coord.process.destroy();
                    } else {
                        beep_flush();
                    }
                }
            });

        // start the thread(s)
        dbg.println("!: starting threads");

        String fullCommand = String.join(" ", shellCommandLine);
        if (isFilter) {
            outputFromProcessToFile(evt, p, fullCommand);
        } else {
            outputToViOutputStream(evt, p, fullCommand);
        }

        return true;
    }

    private void outputToViOutputStream(
            ColonEvent evt,
            Process p,
            String cmd )
    {
        eatme(evt);

        BufferedReader br;
        ViOutputStream vos;
        br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        vos = ViManager.createOutputStream(cmd);

        coord.simpleExecuteThread = new SimpleExecuteThread(coord, vos, br);

        coord.simpleExecuteThread.start();
    }

    /**
        * Set up the threads for a filter command, there are three threads.
        * <ul>
        *   <li>read/write from document</li>
        *   <li>write to process</li>
        *   <li>read from process</li>
        * </ul>
        * There are two BlockingQueue.
        * The document thread puts data from document to fromDoc BlockingQueue and
        * takes data from toDoc BlockingQueue and puts it into the document. Each
        * of the other threads passes data between a BlockingQueue and
        * the process. NOTE: r/w doc is not a thread, timer in EDT.
        * <p>
        * This could be done with a single thread if the process streams had a
        * timeout interface.
        *<p/>
        */
    private void outputFromProcessToFile(
            ColonEvent evt,
            Process p,
            String cmd )
    {
        eatme(cmd);
        // NEEDSWORK: set up a thread group???

        BufferedReader br;
        BufferedWriter bw;
        br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

        coord.writeToProcessThread = new ProcessWriterThread(coord, bw);
        coord.readFromProcessThread = new ProcessReaderThread(coord, br);
        coord.documentThread = new DocumentThread(coord, evt.getViTextView());

        //coord.documentThread.start();
        coord.writeToProcessThread.start();
        coord.readFromProcessThread.start();
        coord.documentThread.runUnderTimer();
    }

} // end inner class BangAction


/**
    * This is used for both filtered bang commands and to execute a command.
    * There should really be two coordinator, one for each, but I'm feeling
    * lazy right now. Any of the Thread variables, eg simpleExecuteThread, can
    * be used to test for how this is being used.
    */
private static class FilterThreadCoordinator
{
    public static final int WAIT = 50; // milliseconds to wait for q data.
    public static final int QLEN = 100; // size of to/from doc q's'
    int startLine;
    int lastLine;
    boolean exitting;

    BlockingQueue<String> fromDoc;
    BlockingQueue<String> toDoc;

    DocumentThread documentThread;
    ProcessWriterThread writeToProcessThread;
    ProcessReaderThread readFromProcessThread;
    SimpleExecuteThread simpleExecuteThread;

    OPARG oa;
    Process process;
    final BangAction ba;

    String statusMessage = "";

    public FilterThreadCoordinator(BangAction ba, OPARG oa, Process p)
    {
        this.oa = oa;
        this.process = p;
        this.ba = ba;

        startLine = oa.start.getLine();
        lastLine = oa.end.getLine();

        fromDoc = new LinkedBlockingQueue<>(QLEN);
        toDoc = new LinkedBlockingQueue<>(QLEN);
    }

    void finish(final Thread thread)
    {
    }
    void finish(final boolean fOK)
    {
        ViManager.runInDispatch(false, () -> ba.finishBangCommand(fOK) );
    }

    public void dumpState()
    {
        Options.getDebugOption(Options.dbgBang)
            .println(() -> "dump: startLine " + startLine + ", lastLine = " + lastLine);
    }

} // end inner class


private static class SimpleExecuteThread extends FilterThread
{
    ViOutputStream vos;
    BufferedReader reader;
    boolean didCleanup;

    SimpleExecuteThread(
            FilterThreadCoordinator coord,
            ViOutputStream vos,
            BufferedReader reader)
    {
        super(SIMPLE_EXECUTE, coord);
        this.vos = vos;
        this.reader = reader;
    }

    @Override
    @SuppressWarnings("CallToThreadRun")
    public void run()
    {
        super.run();
        // Is this needed here? 
        coord.finish(true);
    }

    @Override
    public void dumpState()
    {
        super.dumpState();
    }

    @Override
    void cleanup()
    {
        if(didCleanup) {
            return;
        }
        didCleanup = true;
        if (dbg.getBoolean()) {
            dumpState();
        }
        if(vos != null) {
            vos.close();
            vos = null;
        }
        if(reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            reader = null;
        }
    }

    @Override
    void doTask()
    {
        try {
            while(!isProblem()) {
                final String line = reader.readLine();
                if(line == null) {
                    break;
                }
                dbgData.println(() -> "!: Writing '" + line + "' to ViOutputStream");
                vos.println(line);
            }
            reader.close();
            reader = null;
            vos.close();
            vos = null;
        } catch (IOException ex) {
            exception = ex;
        }
    }

} // end inner class SimpleExecuteThread


private static class ProcessWriterThread extends FilterThread
{
    BufferedWriter writer;
    int currWriterLine;
    boolean wroteFirstLineToProcess;
    boolean reachedEndOfLines;
    boolean didCleanup;

    ProcessWriterThread(FilterThreadCoordinator coord,
                        BufferedWriter writer)
    {
        super(WRITE_PROCESS, coord);
        this.writer = writer;
    }

    @Override
    public void dumpState()
    {
        dbg.println(() -> "dump: currWriterLine " + currWriterLine
                + ", wroteFirstLineToProcess " + wroteFirstLineToProcess
                + ", reachedEndOfLines " + reachedEndOfLines);
        super.dumpState();
    }

    @Override
    void cleanup()
    {
        if(didCleanup) {
            return;
        }
        didCleanup = true;
        if (dbg.getBoolean()) {
            dumpState();
        }
        if(writer != null) {
            try {
                writer.close();
            } catch (IOException ex) {
            }
            writer = null;
        }
    }

    @Override
    void doTask()
    {
        writeToProcess();
    }

    public void writeToProcess()
    {
        currWriterLine = coord.startLine;
        try {
            while(!isProblem() && coord.process.isAlive()) {
                String data = coord.fromDoc.take();
                if(DONE.equals(data)) {
                    break;
                }
                writer.write(data);
                // NEEDSWORK: why trim(), use Text.formDebugDString
                dbgData.println(() -> "!: Writer #" + currWriterLine
                        + ": '" + data.trim() + "'");
                currWriterLine++;
                wroteFirstLineToProcess = true;
            }
            writer.close();
            writer = null;
        } catch (InterruptedException | IOException ex) {
            exception = ex;
        }
        dbg.println("!: Wrote all lines needed to process");
        if(!isProblem()) {
            reachedEndOfLines = true;
        }
    }

} // end inner class ProcessWriterThread


private static class ProcessReaderThread extends FilterThread
{
    BufferedReader reader;
    int currReaderLine;
    boolean wroteFirstLineToFile;
    boolean reachedEndOfProcessOutput;
    boolean didCleanup;

    ProcessReaderThread(
            FilterThreadCoordinator coord,
            BufferedReader reader )
    {
        super(READ_PROCESS, coord);
        this.reader = reader;
    }

    @Override
    public void dumpState()
    {
        dbg.println(() -> "dump: currReaderLine " + currReaderLine
                + ", wroteFirstLineToFile " + wroteFirstLineToFile
                + ", reachedEndOfProcessOutput " + reachedEndOfProcessOutput);
        super.dumpState();
    }

    @Override
    void cleanup()
    {
        if(didCleanup) {
            return;
        }
        didCleanup = true;
        if (dbg.getBoolean()) {
            dumpState();
        }
        if(reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                // LOG.log(Level.SEVERE, null, ex);
            }
            reader = null;
        }
    }

    @Override
    void doTask()
    {
        readFromProcess();
    }

    private void readFromProcess()
    {
        currReaderLine = coord.startLine;
        try {
            while(!isProblem()) {
                String data = reader.readLine();
                if (data == null) {
                    if (wroteFirstLineToFile && !isInterrupted()) {
                        reachedEndOfProcessOutput = true;
                    }
                    dbg.println("!: end of process read data");
                    break;
                }
                // NEEDSWORK: why trim(), use Text.formDebugString
                dbgData.println(() -> "!: Reader #" + currReaderLine
                        + ": '" + data.trim() + "'");
                coord.toDoc.put(data);
                currReaderLine++;
            }
            coord.toDoc.put(DONE);
            reader.close();
            reader = null;
        } catch (InterruptedException | IOException ex) {
            exception = ex;
        }
    }

} // end inner class ProcessReaderThread


/**
 * This both reads and writes to the document; it works to keep the
 * fromDoc blocking Q full and the toDoc blocking Q empty.
 * IT IS NOT A THREAD. It extends FilterThread
 * because it is interfaced to like one for historical reasons.
 * But now it runs under a timer as
 * part of the event dispatch thread, this avoid locking issues
 * between read/write the document and display.
 * <p>
 * Read one line at a time from the document and write it to a Queue for the
 * writeToProcess q. Read a line at a time from the readFromProcess q and
 * build up a large string with the process output. When the process completes
 * and all the data is gathered do a single op to the document.
 * </p>
 */
private static class DocumentThread extends FilterThread
{
    int docReadLine;
    boolean docReadDone;

    int docWriteLine;
    boolean docWriteDone;
    StringBuilder sb = new StringBuilder();

    TextView win;

    int debugCounter;

    // timer is used as a flag, when null work is done
    Timer timer;
    boolean isThread;
    boolean interruptFlag;

    protected boolean didCleanup;

    DocumentThread(FilterThreadCoordinator coord, ViTextView tv)
    {
        super(RW_DOC, coord);
        win = (TextView)tv;
    }

    @Override
    public void run()
    {
        assert(false);
    }

    @Override
    public void interrupt()
    {
        interruptFlag = true;
    }

    @Override
    public boolean isInterrupted()
    {
        return interruptFlag;
    }

    public void runUnderTimer()
    {
        assert(!isThread);
        timer = new Timer(FilterThreadCoordinator.WAIT, (ActionEvent e) -> {
            assert(EventQueue.isDispatchThread());
            if(timer != null) {
                doServiceUnderTimer();
            }
        });
        timer.setRepeats(false);

        docReadLine = coord.startLine;
        docWriteLine = coord.startLine;

        doServiceUnderTimer();
    }

    private void finishUnderTimer()
    {
        if(!docReadDone) {
            // cleanup writes to the document
            cleanup(); // like "10000!!date"
        }

        doTaskCleanup();

        coord.finish(true);
    }

    private void doServiceUnderTimer()
    {
        boolean didSomething;

        try {
            do {
                didSomething = readDocument();
                didSomething |= writeDocument();
            } while(didSomething && !isProblem() && !docWriteDone);
        } catch(Throwable t) {
            exception = t;
        }
        if(!coord.process.isAlive()) {
            if(!docReadDone) {
                docReadDone = true;
                dbg.println(() -> "readDocument: terminated");
            }
            if(coord.toDoc.isEmpty() && !coord.readFromProcessThread.isAlive()) {
                docWriteDone = true;
                dbg.println(() -> "writeDocument: empty+terminated");
            }
        }

        if(!isProblem() && !docWriteDone) {
            timer.start();
        } else {
            finishUnderTimer();
            cleanup(); // write to the file
        }
    }

    @Override
    void doTask()
    {
    }

    public boolean readDocument()
    {
        boolean didSomething = false;
        if(docReadLine <= coord.lastLine) {
            dbg.println("!: rwDoc: try read doc");
            while(!isProblem() && docReadLine <= coord.lastLine) {
                int docLine = docReadLine;
                String data = win.w_buffer.getLineSegment(docLine).toString();
                if(!coord.fromDoc.offer(data)) {
                    break;
                }
                dbgData.println(() -> "!: fromDoc #" + docReadLine + "," + docLine
                        + ": '" + data.trim() + "'");
                docReadLine++;
                didSomething = true;
            }
        } else {
            if(!docReadDone && coord.fromDoc.offer(DONE)) {
                docReadDone = true;
                dbg.println("!: rwDoc: docReadDONE");
            }
        }
        return didSomething;
    }

    public boolean writeDocument()
    {
        boolean didSomething = false;
        if(!docWriteDone) {
            dbg.println(() -> "!: rwDoc: try write doc " + debugCounter++);
            while(!isProblem()) {
                String data = coord.toDoc.poll();
                if(data == null) {
                    break;
                }
                if(DONE.equals(data)) {
                    docWriteDone = true;
                    dbg.println("!: rwDoc: docWriteDONE");
                    break;
                }
                sb.append(data);
                sb.append('\n');
                dbgData.println(() -> "!: toDoc #" + docWriteLine + ": '"
                        + data.trim() + "'");
                didSomething = true;
            }
        }
        return didSomething;
    }

    @Override
    public void dumpState()
    {
        dbg.println(() -> "dump: docReadDone " + docReadDone
                + ", docReadLine " + docReadLine
                + ", docWriteDone " + docWriteDone
                + ", docWriteLine " + docWriteLine);
        super.dumpState();
    }

    @Override
    void cleanup()
    {
        if(didCleanup) {
            return;
        }
        didCleanup = true;
        if (dbg.getBoolean()) {
            dumpState();
        }

        // Check that all the lines from doc were read.
        if(docReadDone && docReadLine <= coord.lastLine) {
            dbg.println(() -> sf("!: %s: lines not read: %d, should be %d",
                                 docReadLine, coord.lastLine));
        }
        dbg.println("!: checking doc CLEANUP");
        // don't run the document cleanup if there are issues
        if(isProblem()) {
            return;
        }

        Misc.runUndoable(() -> {
            Buffer buf = win.w_buffer;
            int startOffset = buf.getLineStartOffset(coord.startLine);
            int endOffset = buf.getLineEndOffset(coord.lastLine);
            if(endOffset > buf.getLength()) {
                // replacing last line, but the '\n' is not part of file
                endOffset = buf.getLength();
                // if replacement text end in '\n', then strip it.
                if(sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n')
                    sb.setLength(sb.length()-1);
            }
            int endOffsetFinal = endOffset;
            dbg.println(() -> sf("!: UpdateDoc: replace [%d,%d) with %d chars",
                                 startOffset, endOffsetFinal, sb.length()));
            buf.replaceString(startOffset, endOffset, sb.toString());
        });

    }

} // end


/** Base class for Bang command threads.
    * <p>
    * The thread that signals completion
    * of the task should override run() and signal success if there was no
    * problem. If an problem occurs, failure will be signaled automatically,
    * see coord.finish()
    */
private static abstract class FilterThread extends Thread
{
    static final String READ_PROCESS = "FilterReadProcess";
    static final String WRITE_PROCESS = "FilterWriteProcess";
    static final String RW_DOC = "FilterDocument";
    static final String SIMPLE_EXECUTE = "SimpleCommandExecute";

    static final String DONE = String.valueOf(CharacterIterator.DONE);

    protected FilterThreadCoordinator coord;

    protected Throwable uncaughtException;
    protected Throwable exception;
    protected boolean error;
    protected boolean interrupted;

    protected DebugOption dbg = Options.getDebugOption(Options.dbgBang);
    protected DebugOption dbgData = Options.getDebugOption(Options.dbgBangData);

    public FilterThread(String filterType, FilterThreadCoordinator coord)
    {
        super(filterType);
        this.coord = coord;

        // if(false) {
        //     // low priority a bit
        //     int pri = getPriority() -1;
        //     if(pri >= MIN_PRIORITY) {
        //         setPriority(pri);
        //     }
        // }
    }

    protected void dumpState()
    {
        if(exception != null) {
            dbg.println("dump: exception: " + exception.getMessage());
        }
        if(uncaughtException != null) {
            dbg.println("dump: uncaughtException: " + uncaughtException.getMessage());
        }
        if(error) {
            dbg.println("dump: error: " + error);
        }
        if(interrupted) {
            dbg.println("dump: interrupted: " + interrupted);
        }
        coord.dumpState();
    }

    /** Its possible that cleanup is called multiple times */
    abstract void cleanup();

    abstract void doTask();

    boolean isProblem()
    {
        return error || exception != null || uncaughtException != null;
    }

    @Override
    public void run()
    {
        setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            FilterThread thisThread = (FilterThread) t;
            if(thisThread.uncaughtException != null) {
                return;
            }
            thisThread.uncaughtException = e;
            LOG.log(Level.SEVERE, null, e);
            cleanup();
        });

        doTask();
        doTaskCleanup();
    }

    // TODO:
    //      cleanup/coord.finish(): shouldn't always be called?
    public void doTaskCleanup()
    {
        if(exception != null) {
            dbg.println(() -> sf("!: task cleanup: %s: exception: %s",
                            getName() , exception.toString()));
            cleanup();
        }

        if(error) {
            dbg.println(() -> sf("!: task cleanup: %s: error", getName()));
            cleanup();
        }

        if(isInterrupted()) {
            interrupted = true;
            dbg.println(() -> sf("!: task cleanup: %s: isInterrupted", getName()));
            cleanup();
        }

        if(isProblem()) {
            dbg.println(() -> sf("!: task cleanup: %s: isProblem(): finish", getName()));

            coord.finish(this);

            // TODO: get rid of this, this thread terminating is detected
            coord.finish(false);
        }
    }

    public void interrupt(String tag)
    {
        dbg.println(() -> sf("!: task cleanup: interrupt %s by %s", getName(), tag));
        super.interrupt();
    }
    
    void debugSleep(int ms)
    {
        try {
            sleep(ms);
        } catch(InterruptedException ex) {
            dbg.println(() -> "!: debugSleep interrupted");
        }
    }
    
} // end inner class FilterThread

}
