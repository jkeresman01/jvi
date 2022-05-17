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


/**
 * When jVi wants to output multi-line information, for example lines
 * matching a search or result of some command execution, the output
 * is sent to a ViOutputStream.
 */
public interface ViOutputStream extends AutoCloseable
{
/** Indicates that the output stream is for search results */
public static final String SEARCH = "Search";
/** Indicates that the output stream is random lines from a file */
public static final String LINES = "Text";
/**
 * Indicates that the output stream is command state information.
 * NEW_NO, CLEAR_NO are forced.
 */
public static final String MAIN = "Output";

/** default is xxx_NO, xxx_YES overrides; effectively the xxx_NO are ignored */
public enum FLAGS {
    NEW_YES, NEW_NO,
    CLEAR_YES, CLEAR_NO,
    RAISE_YES, RAISE_NO
}

public enum COLOR { SUCCESS, WARNING, FAILURE, DEBUG }

default public void println(String s, Runnable doit)
{
    println(s);
}

default public void println(String s, COLOR c)
{
    println(s);
}

default public void println(String s, Runnable doit, COLOR c)
{
    println(s);
}

default public void print(String s, Runnable doit)
{
    print(s);
}

default public void print(String s, COLOR c)
{
    print(s);
}

default public void print(String s, Runnable doit, COLOR c)
{
    print(s);
}

/**
 * Add a message to this output stream. This argument information
 * could be used
 * to identify a match within a line.
 * @param line line number corresponding to this message
 * @param col column within line
 * @param length length, from col, in the line; 0 means to end of line
 */
public void println(int line, int col, int length);

/**
 *  a "link" to the output stream
 */
public void printlnLink(String text, String link);

public void printLink(String text, String link);

@Override
void close();

void print(String s);

void println(String x);

}
