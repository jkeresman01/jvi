package com.raelity.text;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;

public class RegExpResultJava extends RegExpResult
{
    public RegExpResultJava(Matcher m) {
	if(m == null) {
	    return;
	}
        this.result = m.toMatchResult();
    }
    @Override
    public boolean isMatch() {
        return (null != result);
    }
    @Override
    public int nGroup() {
        if(result == null) {
	    return 0;
	}
        return result.groupCount();
    }
    @Override
    public String group(int i) {
        if(result == null) {
	    return null;
	}
        return result.group(i);
    }
    @Override
    public int length(int i) {
        if(result == null || i > nGroup() || result.end(i) < 0) {
	    return -1;
	}
        return result.end(i) - result.start(i);
    }
    @Override
    public int start(int i) {
        if(result == null) {
	    return -1;
	}
        return result.start(i);
    }
    @Override
    public int stop(int i) {
        if(result == null) {
	    return -1;
	}
        return result.end(i);
    }
    private MatchResult result;
}
