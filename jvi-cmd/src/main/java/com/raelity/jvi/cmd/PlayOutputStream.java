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

package com.raelity.jvi.cmd;

import java.io.StringWriter;
import java.util.EnumSet;

import com.raelity.jvi.*;
import com.raelity.jvi.lib.OutputStreamAdaptor;

import static com.raelity.jvi.lib.TextUtil.sf;

public class PlayOutputStream extends OutputStreamAdaptor {
  String type;
  String info;
  ViTextView tv;
  EnumSet<ViOutputStream.FLAGS> flags;

  private static class MyStringWriter extends StringWriter {
    public MyStringWriter()
    {
      super(60);
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void flush()
    {
      super.flush();
      String s = getBuffer().toString();
      getBuffer().setLength(0);
      System.err.print(s);
    }
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public PlayOutputStream(ViTextView tv, String type, String info, EnumSet<ViOutputStream.FLAGS> flags) {
    super(new MyStringWriter(), true);
    this.type = type;
    this.info = info;
    this.tv = tv;
    this.flags = flags;
    
    String fName = tv != null ? tv.getBuffer().getDebugFileName() : "no-file";
    System.err.println("vios: type: " + type
                       + ", file: " + fName
                       + ", flags: " + flags.toString()
                       + ", info: \n"
                       + "                " + info);
  }

  @Override
  public void println(int line, int offset, int length) {
    super.println("vios: " + type + ", " + info + ": "
                  + "line: " + line + ", "
                  + "offset: " + offset + ", "
                  + "length: " + length
		  );
  }

  @Override
  public void println(String s) {
    super.println("vios: " + s);
  }

  @Override
  public void println(String s, COLOR c) {
    super.println(sf("vios: %s (%s)", s, c.toString()));
  }

  @Override
  public void print(String s) {
    super.print("vios: " + s);
  }

  @Override
  public void print(String s, COLOR c) {
    super.print(sf("vios: %s (%s)", s, c.toString()));
  }

  @Override
  public void printlnLink(String text, String link) {
    super.println(sf("vios: %s, %s: link: %s, text: %s",
                      type, info, link, text));
  }

  @Override
  public void printLink(String text, String link) {
    super.print(sf("vios: %s, %s: link: %s, text: %s",
                      type, info, link, text));
  }
}

// vi:set sw=2 ts=8:
