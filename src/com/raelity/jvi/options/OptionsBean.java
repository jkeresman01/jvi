package com.raelity.jvi.options;

import com.raelity.jvi.Options;

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
    
    static public class General extends OptionsBeanBase {
	public General() {
	    super(General.class, "General Options",
		  Options.Category.GENERAL);
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

    static public class Debug extends OptionsBeanBase {
	
	public Debug() {
	    super(Debug.class, "Debug Output", Options.Category.DEBUG);
	}
    }
}
