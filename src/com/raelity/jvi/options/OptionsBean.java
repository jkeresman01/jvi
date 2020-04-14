package com.raelity.jvi.options;

import com.raelity.jvi.core.Options;

/**
 * Simple class so there is some way to change vi options in the IDE.
 * The bean is its own beanInfo.
 */
public class OptionsBean {
    
    static public class Platform extends OptionsBeanBase {
	public Platform() {
	    super(Platform.class, "jVi Options and Configuration",
		  Options.Category.PLATFORM);
        }
    }
    
    static public class Colors extends OptionsBeanBase {
	public Colors() {
	    super(Colors.class, "Colors Options", Options.Category.COLORS);
        }
    }
    
    static public class General extends OptionsBeanBase {
	public General() {
	    super(General.class, "General Options", Options.Category.GENERAL);
        }
    }
    
    static public class Windows extends OptionsBeanBase {
	public Windows() {
	    super(Windows.class, "Window Options and Display",
                                 Options.Category.WINDOW);
        }
    }
    
    static public class Search extends OptionsBeanBase {
	
	public Search() {
	    super(Search.class, "Search Options", Options.Category.SEARCH);
	}
    }
    
    static public class Modify extends OptionsBeanBase {
	
	public Modify() {
	    super(Modify.class, "File Modifications Options",
                  Options.Category.MODIFY);
	}
    }
    
    static public class CursorWrap extends OptionsBeanBase {
	
	public CursorWrap() {
	    super(CursorWrap.class, "Cursor Wrap Options",
                  Options.Category.CURSOR_WRAP);
	}
    }
    
    static public class ExternalProcess extends OptionsBeanBase {
	
	public ExternalProcess() {
	  super(ExternalProcess.class, "External Process Options",
                Options.Category.PROCESS);
	}
    }
    
    static public class None extends OptionsBeanBase {
	public None() {
	    super(None.class, "None Options", Options.Category.NONE);
        }
    }

    static public class Debug extends OptionsBeanBase {
	
	public Debug() {
	    super(Debug.class, "Debug Output", Options.Category.DEBUG);
	}
    }
}
