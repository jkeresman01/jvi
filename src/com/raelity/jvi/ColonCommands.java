/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */
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
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TooManyListenersException;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.raelity.jvi.ViManager;
import com.raelity.jvi.swing.*;

/**
 * This class handles registration, command input, parsing, dispatching
 * and in some instances execution of ":" commands. Some internal vi
 * commands, e.g. set, are executed here. Colon commands are added to
 * the list of avaialable commands through the
 * {@link #register} method.
 * <p>
 * A command is represented by an {@link java.awt.event.ActionListener}.
 * A {@link javax.swing.Action} could be used. Only commands that subclass
 * {@link ColonCommands.ColonAction} can have arguments. These commands
 * are passed a {@link ColonCommands.ColonEvent}. Actions that subclass
 * {@link javax.swing.Action} are not invoked if they are disabled.
 */
public class ColonCommands implements ColonCommandFlags, Constants {
  private static AbbrevLookup commands = new AbbrevLookup();
  
  private static String lastCommand;

  public ColonCommands() {
  }

  private static ViCmdEntry colonCommandEntry;
  private static ViCmdEntry getColonCommandEntry() {
    if(colonCommandEntry == null) {
      try {
        colonCommandEntry = ViManager.getViFactory()
                              .createCmdEntry(ViCmdEntry.COLON_ENTRY);
        colonCommandEntry.addActionListener(
          new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              colonEntryComplete(ev);
            }});
      }
      catch (TooManyListenersException ex) {
        ex.printStackTrace();
        throw new RuntimeException();
      }
    }
    return colonCommandEntry;
  }

  static private void colonEntryComplete(ActionEvent ev) {
    ViManager.stopCommandEntry();
    String commandLine = colonCommandEntry.getCommand();
    String cmd = ev.getActionCommand();
    // if not <CR> must be an escape, just ignore it
    if(cmd.charAt(0) == '\n') {
      if( ! commandLine.equals("")) {
	lastCommand = commandLine;
      }
      executeCommand(parseCommand(commandLine));
      closePrint();
    }
  }

  static void doColonCommand(CMDARG cap) {
    StringBuffer range = new StringBuffer();
    if(cap.count0 != 0) {
      range.append(".");
      if(cap.count0 > 1) {
        range.append(",.+");
        range.append(cap.count0-1);
      }
    }

    ViManager.startCommandEntry(getColonCommandEntry(),
                                ":", G.curwin,
                                range);
  }
  
  static void executeCommand(ColonEvent cev) {
    if(cev != null) {
      ((ActionListener)cev.commandElement.getValue()).actionPerformed(cev);
    }
  }

  /**
   * Parse (partially) the command.
   * Return null if parse failure or otherwise can't execute,
   * otherwise return the colonEvent for the command.
   *
   * Following is the stock vim comment
   * <pre>
   * Execute one Ex command.
   *
   * If 'sourcing' is TRUE, the command will be included in the error message.
   *
   * 2. skip comment lines and leading space
   * 3. parse range
   * 4. parse command
   * 5. parse arguments
   * 6. switch on command name
   *
   * Note: "getline" can be NULL.
   *
   * This function may be called recursively!
   * </pre>
   */
  static ColonEvent parseCommand(String commandLine) {
    int sidx = 0; // index into string
    int sidx01;
    int sidx02;
    // Use the TextView as source
    ColonEvent cev = new ColonEvent(G.curwin);
    boolean bang = false;
    MutableInt lnum = new MutableInt(0);
    boolean skip = false; // NEEDSWORK: executCommmand how else is this set

    //
    // 3. parse a range specifier of the form: addr [,addr] [;addr] ..
    //
    // where 'addr' is:
    //
    // %	      (entire file)
    // $  [+-NUM]
    // 'x [+-NUM] (where x denotes a currently defined mark)
    // .  [+-NUM]
    // [+-NUM]..
    // NUM
    //
    // The ea.cmd pointer is updated to point to the first character following
    // the range spec. If an initial address is found, but no second, the upper
    // bound is equal to the lower.
    //
    // cev.line1, cev.line2 are set to the range values

    // NEEDSWORK: parse a range!
    // while(parsing line number) {
    //   if(range number) {
    //     cev.line2 = range number
    //     ++cev.addr_count;
    //   }
    // }
    
    // repeat for all ',' or ';' separated addresses
    for (;;)
    {
	cev.line1 = cev.line2;
        // default is current line number
	cev.line2 = G.curwin.getWCursor().getLine();
        sidx = Misc.skipwhite(commandLine, sidx);
	sidx = get_address(commandLine, sidx, skip, lnum);
	if (sidx < 0)		    // error detected
	    return null; // NEEDSWORK: goto doend;
	if (lnum.getValue() == MAXLNUM)
	{
	    if (commandLine.charAt(sidx) == '%')   // '%' - all lines
	    {
                ++sidx;
		cev.line1 = 1;
                cev.line2 = G.curwin.getLineCount();
		++cev.addr_count;
	    }
            /* ************************************************************
            NOT SURE WHAT THIS ADDRESS IS
					    // '*' - visual area
	    else if (*ea.cmd == '*' && vim_strchr(p_cpo, CPO_STAR) == NULL)
	    {
		FPOS	    *fp;

		++ea.cmd;
		if (!ea.skip)
		{
		    fp = getmark('<', FALSE);
		    if (check_mark(fp) == FAIL)
			goto doend;
		    ea.line1 = fp->lnum;
		    fp = getmark('>', FALSE);
		    if (check_mark(fp) == FAIL)
			goto doend;
		    ea.line2 = fp->lnum;
		    ++ea.addr_count;
		}
	    }
            ************************************************************/
	}
	else
	    cev.line2 = lnum.getValue();
	cev.addr_count++;

        /* *****************************************************************
	if (*ea.cmd == ';')
	{
	    if (!ea.skip)
		curwin->w_cursor.lnum = ea.line2;
	} else
        ********************************************************************/
        if (sidx >= commandLine.length() || commandLine.charAt(sidx) != ',')
	    break;
	++sidx;
    }
    
    // If one address given: set start and end lines
    if (cev.addr_count == 1) {
	cev.line1 = cev.line2;
	    // ... but only implicit: really no address given
	if (lnum.getValue() == MAXLNUM)
	    cev.addr_count = 0;
    }

    // Find the command

    sidx = Misc.skipwhite(commandLine, sidx);
    if(sidx >= commandLine.length()) {
      // no command, but if a number was entered then goto that line
      if(cev.getAddrCount() > 0) {
        Misc.gotoLine(cev.getLine1(), BL_SOL | BL_FIX);
        return null;
      }
    }
    
    // if(sidx >= commandLine.length()) { return null; }
    sidx01 = sidx;
    // skip alpha characters
    for(; sidx < commandLine.length(); sidx++) {
      if( ! Util.isalpha(commandLine.charAt(sidx))) {
        break;
      }
    }
    sidx02 = sidx;
    if(sidx < commandLine.length() && commandLine.charAt(sidx) == '!') {
      bang = true;
      ++sidx;
    }
    sidx = Misc.skipwhite(commandLine, sidx);

    String command = commandLine.substring(sidx01, sidx02);
    AbbrevLookup.CommandElement ce = commands.lookupCommand(command);
    if(ce == null) {
      Msg.emsg("Not an editor command: " + command);
      return null;
    }

    //
    // Invoke the command
    //

    if(ce.getValue() instanceof Action) {
      if( ! ((Action)ce.getValue()).isEnabled()) {
        Msg.emsg(ce.getName() + " is not enabled");
        return null;
      }
    }
    if( ! (ce.getValue() instanceof ColonAction)) {
      // no arguments allowed
      if(sidx < commandLine.length()) {
        Msg.emsg(Messages.e_trailing);
        return null;
      }
    }
    if(bang
        && ( ! (ce.getValue() instanceof ColonAction)
             || (((ColonAction)ce.getValue()).getFlags() & BANG) == 0)) {
      Msg.emsg("No ! allowed");
      return null;
    }
    cev.command = ce.getName();
    cev.bang = bang;
             
    if(sidx < commandLine.length()) {
      cev.args = new ArrayList();
      if((((ColonAction)ce.getValue()).getFlags() & NOPARSE) != 0) {
        // put the line (without command name) as the argument
        cev.args.add(commandLine.substring(sidx));
      } else {
        // parse the command into tokens
        StringTokenizer toks = new StringTokenizer(commandLine.substring(sidx));
        while(toks.hasMoreTokens()) {
          cev.args.add(toks.nextToken());
        }
      }
    }

    cev.commandElement = ce;
    return cev;
    // ((ActionListener)ce.getValue()).actionPerformed(cev);
  }
  
  /**
   * get a single EX address
   *
   * Set ptr to the next character after the part that was interpreted.
   * Set ptr to NULL when an error is encountered.
   *
   * Return MAXLNUM when no Ex address was found.
   * 
   * @param skip only skip the address, don't use it
   * @return the index of next character after the part that was interpreted
   * else minus one if there was an error;
   */
  private static int get_address(String s, int sidx, boolean skip,
                                 MutableInt lnum) {
    // note that the return sidx is set to minus one if there is an error
    
    sidx = Misc.skipwhite(s, sidx);
    lnum.setValue(MAXLNUM);
    do {
      char c = s.charAt(sidx);
      switch(c) {
        case '.':         // '.' - Cursor position
          ++sidx;
          lnum.setValue(G.curwin.getWCursor().getLine());
          break;
        case '$':         // '$' - last line
          ++sidx;
          lnum.setValue(G.curwin.getLineCount());
          break;
        case '\'':        // ''' - mark
          ++sidx;
          c = s.charAt(sidx);
          if(sidx >= s.length()) {
            return -1;
          }
          if(skip) {
            ++sidx;
          } else {
            ViMark fp = (ViMark)MarkOps.getmark(c, false);
            ++sidx;
            if(MarkOps.check_mark(fp) == FAIL) {
              return -1;
            }
            lnum.setValue(fp.getLine());
          }
          break;
        
        /* ******************************
        // NEEDSWORK: get_address: These are big cases....
        case '/':
        case '?':         // '/' or '?' - search
        
        case '\\':	  // "\?", "\/" or "\&", repeat search
        *******************************/
        default:
          if(Util.isdigit(c)) {
            sidx = Misc.getdigits(s, sidx, lnum);
          }
      }

      int i = 0;
      int n = 0;
      for (;;)
      {
        sidx = Misc.skipwhite(s, sidx);
        if(sidx >= s.length()) {
          break;
        }
        c = s.charAt(sidx);
        if (c != '-' && c != '+' && !Util.isdigit(c))
          break;

        if (lnum.getValue() == MAXLNUM) {
          // "+1" is same as ".+1"
          lnum.setValue(G.curwin.getWCursor().getLine());
        }
        if (Util.isdigit(c)) {
          i = '+';		// "number" is same as "+number"
        } else {
          i = c;
          ++sidx;
          c = s.charAt(sidx);
        }
        
        if (!Util.isdigit(c))	// '+' is '+1', but '+0' is not '+1'
          n = 1;
        else {
          MutableInt mi = new MutableInt(0);
          sidx = Misc.getdigits(s, sidx, mi);
          n = mi.getValue();
        }
          
        if (i == '-')
          lnum.setValue(lnum.getValue() - n);
        else
          lnum.setValue(lnum.getValue() + n);
      }
    } while(false); //NEEDSWORK:  while (*cmd == '/' || *cmd == '?');
    
    return sidx;
  }

  /**
   * This method returns a read-only list of the registered commands.
   * The elements of the list are {@link AbbrevLookup.CommandElement}.
   */
  static public List getCommandList() {
    return commands.getList();
  }

  /**
   * Register a command; the abbrev must be unique.  The abbrev is the "key"
   * for the command, for example since "s" is the abbreviation for "substitue"
   * and "se" is the abbreviation for "set" then "substitute" sorts earlier
   * than "set" because "s" sorts earlier than "se". Consider this when
   * adding commands, since unique prefix has nothing to do with how commands
   * are recognized.
   * @exception IllegalArgumentException this is thrown if the abbreviation
   * and/or the name already exist in the list or there's a null argument.
   */
  static public void register(String abbrev, String name, ActionListener l) {
    commands.add(abbrev, name, l);
  }

  /**
   * Commands that take arguments subclass this. Some treatment of the
   * command arguments can be controled through the {@link ColonAction#getFlags}
   * method.
   */

  static abstract public class ColonAction extends AbstractAction
                                           implements ColonCommandFlags
  {
    /**
     * Specify some of the commands argument handling. This default
     * implementation returns zero.
     * @see ColonCommandFlags
     */
    public int getFlags() {
      return 0;
    }
  }

  /**
   * The event passed to {@link ColonCommands.ColonAction}. It is used
   * to pass argument information. The arguments to the command are
   * white space separated. The command word finishes with the first
   * non-alpha character. So "e#" and "e #" are parsed the same.
   */
  static public class ColonEvent extends ActionEvent {
    // NEEDSWORK: ColonEvent Add stuff for getting at command arguments
    /** The expanded command word */
    String command;
    /** The command associated with this event */
    AbbrevLookup.CommandElement commandElement;
    /** indicates that the command word has a trailing "!" */
    boolean bang;
    /** command line as entered */
    String commandLine;
    /** the command arguments */
    List args;
    /** the text view for this event */
    ViTextView viTextView;
    /** the first line number */
    int line1;
    /** the second line number or count */
    int line2;
    /** the number of addresses given */
    int addr_count;

    ColonEvent(ViTextView c) {
      super(c.getEditorComponent(), ActionEvent.ACTION_PERFORMED, "");
      viTextView = c;
    }

    /**
     * @return the first line number
     */
    public int getLine1() {
      return line1;
    }

    /**
     * @return the second line number or count
     */
    public int getLine2() {
      return line2;
    }
    
    /**
     * @return the number of addresses given
     */
    public int getAddrCount() {
      return addr_count;
    }
    
    public ActionListener getAction() {
      return (ActionListener)commandElement.getValue();
    }

    /**
     * @return true if the command has a "!" appended.
     */
    public boolean isBang() {
      return bang;
    }

    /**
     * @return the textView for this command
     */
    public ViTextView getViTextView() {
      return viTextView;
    }

    /**
     * @return the number of arguments, not including command name.
     */
    public int getNArg() {
      if(args == null) {
        return 0;
      }
      return args.size();
    }

    /**
     * Fetch an argument.
     * @return the nth argument, n == 0 is the expanded command name.
     */
    public String getArg(int n) {
      if(n == 0) {
        return command;
      }
      return (String)args.get(n-1);
    }

    /**
     * Fetch the command line, including commmand name
     */
    public String XXXgetCommandLine() {
      return commandLine;
    }
  }

  static ActionListener ACTION_quit = new ActionListener() {
    public void actionPerformed(ActionEvent ev) {
      ColonEvent cev = (ColonEvent)ev;
      cev.getViTextView().win_quit();
    }};

  static ActionListener ACTION_close = new ActionListener() {
    public void actionPerformed(ActionEvent ev) {
      ColonEvent cev = (ColonEvent)ev;
      // NEEDSWORK: win_close: hidden, need_hide....
      cev.getViTextView().win_close(false);
    }};

  static ActionListener ACTION_only = new ActionListener() {
    public void actionPerformed(ActionEvent ev) {
      ColonEvent cev = (ColonEvent)ev;
      // NEEDSWORK: win_close_others: forceit....
      cev.getViTextView().win_close_others(false);
    }};

  static ActionListener ACTION_wall = new ActionListener() {
    public void actionPerformed(ActionEvent ev) {
      ViManager.getFS().writeAll(false);
    }};

  static ActionListener ACTION_file = new ActionListener() {
    public void actionPerformed(ActionEvent ev) {
      ColonEvent cev = (ColonEvent)ev;
      cev.getViTextView().displayFileInfo();
    }};

  /**
   * Write command.
   * <br>
   * If there is an argument, it must be '*'. ':w *' should invoke
   * a save as dialog.
   */
  static ColonAction ACTION_write = new ColonAction() {
    public int getFlags() {
      return BANG;
    }

    public void actionPerformed(ActionEvent ev) {
      ColonEvent cev = (ColonEvent)ev;
      if(cev.getNArg() == 0) {
        ViManager.getFS().write(cev.getViTextView(), cev.isBang());
      } else if(cev.getNArg() == 1 && cev.getArg(1).equals("*")) {
        ViManager.getFS().write(cev.getViTextView(), null, cev.isBang());
      } else {
        Msg.emsg("Only single argument '*' allowed");
      }
    }
  };

  /**
   * Edit command. Only ':e#[number]' is supported.
   */
  static ColonAction ACTION_edit = new ColonAction() {
    public int getFlags() {
      return BANG;
    }

    public void actionPerformed(ActionEvent ev) {
      ColonEvent cev = (ColonEvent)ev;
      String arg;
      boolean error = false;
      /*
      if(cev.getNArg() == 0) {
        ViManager.getFS().write(cev.getViTextView(), cev.isBang());
      } else
      */
      if(cev.getNArg() == 1 && cev.getArg(1).charAt(0) == '#') {
        int i = -1;
        arg = cev.getArg(1);
        if(arg.length() > 1) {
          arg = arg.substring(1);
          try {
            i = Integer.parseInt(arg);
          } catch(NumberFormatException ex) {
            error = true;
          }
        }
        if( ! error) {
          ViManager.getFS().edit(cev.getViTextView(), i, cev.isBang());
        }
      } else {
        error = true;
      }

      if(error) {
        Msg.emsg("Only 'e#[<number>]' allowed");
      }
    }
  };
  
  static ColonAction ACTION_substitute = new ColonAction() {
    public int getFlags() {
      return NOPARSE;
    }
    
    public void actionPerformed(ActionEvent ev) {
      Misc.beginUndo();
      try {
        Search.substitute((ColonEvent)ev);
      }
      finally {
        Misc.endUndo();
      }
      
    }
  };
  
  static ColonAction ACTION_global = new ColonAction() {
    public int getFlags() {
      return NOPARSE;
    }
    
    public void actionPerformed(ActionEvent ev) {
      Misc.beginUndo();
      try {
        Search.global((ColonEvent)ev);
      }
      finally {
        Misc.endUndo();
      }
    }
  };
  
  /**
   * :print command. If not busy global then output to "print" stream.
   * For now, its just a no-op so the word "print" can be found.
   */
  static ColonAction ACTION_print = new ColonAction() {
    public void actionPerformed(ActionEvent ev) {
    }
  };
  
  /**
   * This is used for several of the colon commands to translate arguments
   * into OPARG.
   */
   
  static OPARG setupExop(ColonEvent cev) {
    OPARG oa = new OPARG();
    // oa.regname = ; NEEDSWORK:
    oa.start = new FPOS();
    oa.start.setPosition(cev.getLine1(), 0);
    oa.end = new FPOS();
    oa.end.setPosition(cev.getLine2(), 0);
    oa.line_count = cev.getLine2() - cev.getLine1() + 1;
    oa.motion_type = MLINE;
    if(cev.getAction() != ACTION_yank) {
      MarkOps.setpcmark();
      G.curwin.getWindow().setWCursor(oa.start);
      Edit.beginline(BL_SOL|BL_FIX);
    }
    return oa;
  }
  
  /**
   * :delete command.
   */
  static ColonAction ACTION_delete = new ColonAction() {
    public void actionPerformed(ActionEvent ev) {
      ColonEvent cev = (ColonEvent)ev;
      OPARG oa = setupExop(cev);
      oa.op_type = OP_DELETE;
      if(cev.getNArg() == 1) {
	try {
	  oa.line_count = Integer.parseInt(cev.getArg(1));
	}
	catch (Exception ex) {
	  Msg.emsg(Messages.e_trailing);
	  return;
	}
      } else if(cev.getNArg() > 1) {
	Msg.emsg(Messages.e_trailing);
	return;
      }
      
      Misc.op_delete(oa);
    }
  };
  
  /**
   * :yank command.
   */
  static ColonAction ACTION_yank = new ColonAction() {
    public void actionPerformed(ActionEvent ev) {
      OPARG oa = setupExop((ColonEvent)ev);
      oa.op_type = OP_YANK;
    }
  };

  static void registerBuiltinCommands() {
    register("clo", "close", ACTION_close);
    register("on", "only", ACTION_only);
    register("q", "quit", ACTION_quit);
    register("w", "write", ACTION_write);
    register("wa", "wall", ACTION_wall);
    register("f", "file", ACTION_file);
    register("e", "edit", ACTION_edit);
    register("s", "substitute", ACTION_substitute);
    register("g", "global", ACTION_global);
    register("d", "delete", ACTION_delete);
    register("p", "print", ACTION_print);
    // register("y", "yank", ACTION_yank);
    
    // register("n", "next", ACTION_next);
    // register("N", "Next", ACTION_Next);
  }

  static {
    registerBuiltinCommands();
  }
  
  //
  // handle the print stream
  //
  
  /**
   * The current print stream (text window) is automatically created if there
   * is no current one.
   *
   * It is expected that it will be closed at the end of
   * the current command.
   */
  private static ViOutputStream printStream;
  
  private static void makePrintStream() {
    if(printStream != null) {
      return;
    }
    printStream = G.curwin.createOutputStream(ViOutputStream.TEXT,
                                              lastCommand);
  }
   
  /**
   * Output a string to the current text window.
   */
  static void outputPrint(String s) {
    makePrintStream();
    printStream.println(s);
  }
  
  /**
   * Output the specified document line to the current text window.
   */
  static void outputPrint(int line, int offset, int length) {
    makePrintStream();
    printStream.println(line, offset, length);
  }
  
  /**
   * Close the current print output stream.
   */
  static void closePrint() {
    if(printStream == null) {
      return;
    }
    printStream.close();
    printStream = null;
  }
}
