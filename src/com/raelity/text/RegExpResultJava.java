package com.raelity.text;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;

public class RegExpResultJava extends RegExpResult
{
    public RegExpResultJava(Matcher m, boolean matches) {
	if(m == null) {
	    return;
	}
        this.result = m.toMatchResult();
        this.matches = matches;
    }
    @Override
    public boolean isMatch() {
        return result != null && matches;
    }
    @Override
    public int nGroup() {
        return result.groupCount();
    }
    @Override
    public String group(int i) {
        if(!matches)
	    return null;
        return result.group(i);
    }
    @Override
    public int length(int i) {
        if(!matches || i > nGroup() || result.end(i) < 0)
	    return -1;
        return result.end(i) - result.start(i);
    }
    @Override
    public int start(int i) {
        if(!matches)
            return -1;
        return result.start(i);
    }
    @Override
    public int stop(int i) {
        if(!matches)
            return -1;
        return result.end(i);
    }
    private MatchResult result;
    boolean matches;
}
