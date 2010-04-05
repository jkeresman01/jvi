package com.raelity.jvi.options;

import com.raelity.jvi.ViInitialization;
import com.raelity.jvi.manager.ViManager;
import com.raelity.jvi.ViOptionBag;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.ColonCommands.ColonEvent;
import com.raelity.jvi.core.G;
import com.raelity.jvi.core.Msg;
import com.raelity.jvi.core.Options;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implement ":se[t]".
 *
 * Options are either global or indirect, see the P_ XXX below An option
 * must be one or the other. Global options are static, an indirect option
 * is an instance variable in either G.curwin or G.curbuf. When a P_IND
 * variable is set, introspection is used to do the set.
 * <p>
 * In some cases, due to platform limitation, the same variable must be
 * set in all the instances, syncAllInstances(var) does that.
 */
public class SetColonCommand extends ColonCommands.ColonAction
{
  private static final
          Logger LOG = Logger.getLogger(SetColonCommand.class.getName());

  @ServiceProvider(service=ViInitialization.class, path="jVi/init")
  public static class Init implements ViInitialization
  {
    @Override
    public void init()
    {
      SetColonCommand.init();
    }
  }

    private static void init()
    {
        ColonCommands.register("se", "set", new SetColonCommand());
    }

  public static class SetCommandException extends Exception
  {

    public SetCommandException(String msg)
    {
      super(msg);
    }
  }

  private enum O {
    P_GBL, // a global option
    P_WIN, // a per window option
    P_BUF; // a per buffer option

    boolean isLocal()
    {
      return this == P_WIN || this == P_BUF;
    }

    boolean isGlobal()
    {
      return this == P_GBL;
    }

    boolean isWin()
    {
      return this == P_WIN;
    }

    boolean isBuf()
    {
      return this == P_BUF;
    }
  }

  private static class VimOption
  {

    String fullname; // option name
    String shortname; // option name
    O type;
    // name of field and/or option
    String varName; // java variable name in curbuf or curwin
    String optName; // the jVi Option name.

    VimOption(String fullname, String shortname, O type,
            String varName, String optName)
    {
      this.fullname = fullname;
      this.shortname = shortname;
      this.type = type;
      this.varName = varName;
      this.optName = optName;
    }
  }

  private static VimOption[] vopts = new VimOption[]{
    new VimOption("expandtab",   "et",  O.P_BUF, "b_p_et", null),
    new VimOption("ignorecase",  "ic",  O.P_GBL, null, Options.ignoreCase),
    new VimOption("incsearch",   "is",  O.P_GBL, null, Options.incrSearch),
    new VimOption("hlsearch",    "hls", O.P_GBL, null, Options.highlightSearch),
    new VimOption("wrapscan",    "ws",  O.P_GBL, null, Options.wrapScan),
    new VimOption("number",      "nu",  O.P_WIN, "w_p_nu", null),
    new VimOption("shiftwidth",  "sw",  O.P_BUF, "b_p_sw", Options.shiftWidth),
    new VimOption("tabstop",     "ts",  O.P_BUF, "b_p_ts", Options.tabStop),
    new VimOption("softtabstop", "sts", O.P_BUF, "b_p_sts", Options.softTabStop),
    new VimOption("textwidth",   "tw",  O.P_BUF, "b_p_tw", Options.textWidth),
    new VimOption("wrap",        "",    O.P_WIN, "w_p_wrap", null),
    new VimOption("linebreak",   "lbr", O.P_WIN, "w_p_lbr",  null),
    new VimOption("list",        "",    O.P_WIN, "w_p_list", null),
  };

  @Override
  public void actionPerformed(ActionEvent e)
  {
    ColonEvent evt = (ColonEvent)e;
    parseSetOptions(evt.getArgs());
  }

  public void parseSetOptions(List<String> eventArgs)
  {
    if (eventArgs.isEmpty() ||
            eventArgs.size() == 1 && "all".equals(eventArgs.get(0))) {
      displayAllOptions();
      return;
    }
    LinkedList<String> args = new LinkedList<String>();
    // copy eventArgs into args, with possible fixup
    // ":set sw =4" is allowed, so if something starts with "="
    // then append it to the previous element
    int j = 0;
    for (int i = 0; i < eventArgs.size(); i++) {
      String arg = eventArgs.get(i);
      if (arg.startsWith("=") && args.size() > 0) {
        arg = args.removeLast() + arg;
      }
      args.addLast(arg);
    }
    for (String arg : args) {
      try {
        parseSetOption(arg);
      } catch (SetCommandException ex) {
        // error message given
        return;
      } catch (IllegalAccessException ex) {
        LOG.log(Level.SEVERE, null, ex);
        return;
      } catch (IllegalArgumentException ex) {
        LOG.log(Level.SEVERE, null, ex);
        return;
      }
    }
  }

  /**
   * This holds the results of parsing a set command
   */
  private static class VimOptionState
  {

    Class type;
    Object value;
    // used if type.isLocal()
    Field f;
    ViOptionBag bag;
    // used if regular option is provided
    Option opt;
    // Following not really option state, they are the
    // parse results when settigns options
    boolean fInv;
    boolean fNo;
    boolean fShow;
    boolean fValue;
    String[] split;
  }

  // This is train of thought
  public static void parseSetOption(String arg) throws IllegalAccessException,
                                                       SetCommandException
  {
    VimOptionState voptState = new VimOptionState();
    voptState.split = arg.split("[:=]");
    String voptName = voptState.split[0];
    if (voptState.split.length > 1) {
      voptState.fValue = true;
    }
    if (voptName.startsWith("no")) {
      voptState.fNo = true;
      voptName = voptName.substring(2);
    } else if (voptName.startsWith("inv")) {
      voptState.fInv = true;
      voptName = voptName.substring(3);
    } else if (voptName.endsWith("!")) {
      voptState.fInv = true;
      voptName = voptName.substring(0, voptName.length() - 1);
    } else if (voptName.endsWith("?")) {
      voptState.fShow = true;
      voptName = voptName.substring(0, voptName.length() - 1);
    }
    VimOption vopt = null;
    for (VimOption v : vopts) {
      if (voptName.equals(v.fullname)
              || voptName.equals(v.shortname) && !v.shortname.isEmpty()) {
        vopt = v;
        break;
      }
    }
    if (vopt == null) {
      String msg = "Unknown option: " + voptName;
      Msg.emsg(msg);
      throw new SetCommandException(msg);
    }
    if (!determineOptionState(vopt, voptState)) {
      String msg = "Internal error: " + arg;
      Msg.emsg(msg);
      throw new SetCommandException(msg);
    }
    Object newValue = newOptionValue(arg, vopt, voptState);
    if (voptState.fShow) {
      Msg.smsg(formatDisplayValue(vopt, voptState.value));
    } else {
      if (voptState.opt != null) {
        try {
          voptState.opt.validate(newValue);
        } catch (PropertyVetoException ex) {
          Msg.emsg(ex.getMessage());
          throw new SetCommandException(ex.getMessage());
        }
      }

      if (vopt.type.isLocal()) {
        voptState.f.set(voptState.bag, newValue);
        voptState.bag.viOptionSet(G.curwin, vopt.varName);
      } else { // isGlobal()
        voptState.opt.setValue(newValue.toString());
      }
    }
  }

  /**
   * Set voptState with information about the argument vopt.
   */
  private static boolean determineOptionState(VimOption vopt,
                                              VimOptionState voptState)
  {
    if (vopt.optName != null) {
      voptState.opt = Options.getOption(vopt.optName);
    }
    if (vopt.type.isLocal()) {
      voptState.bag = vopt.type.isWin() ? G.curwin : G.curwin.getBuffer();
      try {
        voptState.f = voptState.bag.getClass().getField(vopt.varName);
      } catch (SecurityException ex) {
        LOG.log(Level.SEVERE, null, ex);
      } catch (NoSuchFieldException ex) {
        LOG.log(Level.SEVERE, null, ex);
      }
      if (voptState.f == null) {
        return false;
      }
      voptState.type = voptState.f.getType();
      // impossible to get exceptions
      try {
        voptState.value = voptState.f.get(voptState.bag);
      } catch (IllegalArgumentException ex) {
        LOG.log(Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex) {
        LOG.log(Level.SEVERE, null, ex);
      }
    } else if (vopt.type.isGlobal()) {
      if (voptState.opt instanceof BooleanOption) {
        voptState.type = boolean.class;
        voptState.value = voptState.opt.getBoolean();
      } else if (voptState.opt instanceof IntegerOption) {
        voptState.type = int.class;
        voptState.value = voptState.opt.getInteger();
      }
    }
    return true;
  }

  // Most of the argument are class members
  private static Object newOptionValue(String arg, VimOption vopt,
                                       VimOptionState voptState) throws NumberFormatException,
                                                                            SetCommandException
  {
    Object newValue = null;
    if (voptState.type == boolean.class) {
      if (voptState.fValue) {
        // like: ":set ic=val"
        String msg = "Unknown argument: " + arg;
        Msg.emsg(msg);
        throw new SetCommandException(msg);
      }
      if (!voptState.fShow) {
        newValue =
                voptState.fInv
                ? !((Boolean)voptState.value).booleanValue()
                : voptState.fNo ? false : true;
      }
    } else if (voptState.type == int.class) {
      if (!voptState.fValue) {
        voptState.fShow = true;
      }
      if (!voptState.fShow) {
        try {
          newValue = Integer.parseInt(voptState.split[1]);
        } catch (NumberFormatException ex) {
          String msg = "Number required after =: " + arg;
          Msg.emsg(msg);
          throw new SetCommandException(msg);
        }
      }
    } else {
      assert false : "Type " + voptState.type.getSimpleName() + " not handled";
    }
    return newValue;
  }

  private static String formatDisplayValue(VimOption vopt, Object value)
  {
    String v = "";
    if (value instanceof Boolean) {
      v = (((Boolean)value).booleanValue() ? "  " : "no") + vopt.fullname;
    } else if (value instanceof Integer) {
      v = vopt.fullname + "=" + value;
    } else {
      assert false : value.getClass().getSimpleName() + " not handled";
    }
    return v;
  }

  private static void displayAllOptions()
  {
    ViOutputStream osa =
            ViManager.createOutputStream(null, ViOutputStream.OUTPUT, null);
    for (VimOption vopt : vopts) {
      VimOptionState voptState = new VimOptionState();
      determineOptionState(vopt, voptState);
      osa.println(formatDisplayValue(vopt, voptState.value));
    }
    osa.close();
  }

  public static void syncAllInstances(String varName)
  {
    for (VimOption vopt : vopts) {
      if (vopt.type.isLocal()) {
        if (vopt.varName.equals(varName)) {
          VimOptionState voptState = new VimOptionState();
          determineOptionState(vopt, voptState);
          Set<? extends ViOptionBag> set =
                  vopt.type.isWin()
                  ? ViManager.getFactory().getViTextViewSet()
                  : ViManager.getFactory().getBufferSet();
          for (ViOptionBag bag : set) {
            try {
              if(G.dbgOptions)
                System.err.println("syncInstances: " + varName + " in " + bag);
              voptState.f.set(bag, voptState.value);
            } catch (IllegalArgumentException ex) {
              LOG.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
              LOG.log(Level.SEVERE, null, ex);
            }
          }
          break;
        }
      }
    }
  }
}

// vi:sw=2 et