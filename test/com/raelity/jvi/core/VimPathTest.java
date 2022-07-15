/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2022 Ernie Rael.  All Rights Reserved.
 *
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
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package com.raelity.jvi.core;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//import org.junit.jupiter.api.*;
import org.openide.util.Exceptions;

import com.raelity.jvi.lib.*;
import com.raelity.jvi.lib.StringSegment;

import static org.junit.Assert.*;
//import static org.junit.jupiter.api.Assertions.*;

import static com.raelity.jvi.core.VimPath.*;

/**
 *
 * @author err
 */
public class VimPathTest
{

public VimPathTest()
{
    //System.err.println("THIS IS A TEST IN FILEPATHTEST");
}

//@BeforeAll
@BeforeClass
public static void setUpClass()
{
    //System.err.println("THIS IS A TEST IN SETUPCLASS");
}

//@AfterAll
@AfterClass
public static void tearDownClass()
{
}

static Path getPath(String s)
{
    return VimPath.getVimPathTesting(s);
}

static Path getRegularPath(String s)
{
    return FileSystems.getDefault().getPath(s).normalize();
}

String initCurDir = "/tmp/xxx/randomdirTEST";
String initHomeDir = "/tmp/roothome/homedirTEST";

String saved_home_dir;
//@BeforeEach
@Before
public void setUp()
{
    try {
        saved_home_dir = System.getProperty("user.home");
        setCwdTEST(FileSystems.getDefault().getPath("").toAbsolutePath().toString());
        Path path = FileSystems.getDefault().getPath("/");

        Files.createDirectories(path.resolve(initCurDir + "/cd1/cd2/cd3"));
        setCwdTEST(initCurDir, false);

        Files.createDirectories(path.resolve(initHomeDir + "/hd1/hd2/hd3"));
        System.setProperty("user.home", initHomeDir);

        //System.err.println("THIS IS A TEST IN SETUP");
    } catch(IOException ex) {
        Exceptions.printStackTrace(ex);
    }
}

//@AfterEach
@After
public void tearDown()
{
    System.setProperty("user.home", saved_home_dir);
    Path path = FileSystems.getDefault().getPath("").toAbsolutePath();
    setCwdTEST(path.toString());
}

void eatme(Object... o) {}

/**
 * Test of setCwd method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testSetCwd()
{
    System.out.println("setCwd");
    String result;
    result = setCwdTEST("a/b/c/d/e", false);
    System.err.println(result);
    assertNotEquals("", result);

    result = setCwdTEST("/etc/passwd", false);
    System.err.println(result);
    assertNotEquals("", result);

    result = setCwdTEST("/etc/lvm/backup", false);
    System.err.println(result);
    assertNotEquals("", result);

    result = setCwdTEST("/tmp", false);
    assertEquals("", result);
    assertEquals("/tmp", getCwd().toString());

    result = setCwdTEST("~", false);
    assertEquals("", result);
    assertEquals(initHomeDir, getCwd().toString());

    result = setCwdTEST("~/", false);
    assertEquals("", result);
    assertEquals(initHomeDir, getCwd().toString());

    result = setCwdTEST("hd1", false);
    assertEquals("", result);
    assertEquals(initHomeDir + "/hd1", getCwd().toString());

    // Note starting at ~/hd1
    result = setCwdTEST("hd1/./hd2/../../..", false);
    assertEquals("", result);
    assertEquals(initHomeDir, getCwd().toString());

    setCwdTEST("~/", false);
    result = setCwdTEST("hd1/./hd2/../../..", false);
    assertEquals("", result);
    assertEquals(getRegularPath(initHomeDir + "/..").toString(),
                 getCwd().toString());
    //assertEquals("/tmp", getCwd().toString());
}

/**
 * Test of getVimPathOriginal method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetVimPathOriginal()
{
    System.out.println("getVimPath");

    assertEquals("~", getVimPathTesting(getRegularPath(initHomeDir)).toString());
    assertEquals("~/foo", getVimPathTesting(getRegularPath(initHomeDir + "/foo")).toString());
    assertEquals(getRegularPath(initHomeDir + "/..").toString(),
                 getVimPathTesting(getRegularPath(initHomeDir + "/..")).toString());

    assertEquals(initCurDir, getAbsolutePath(getVimPathTesting("")).toString());
    assertEquals(initCurDir + "/foo/bar", getAbsolutePath(getVimPathTesting("foo/bar")).toString());
    assertEquals("",
                 getVimPathTesting(getRegularPath(initCurDir)).toString());
    assertEquals("foo/bar",
                 getVimPathTesting(getRegularPath(initCurDir + "/foo/bar")).toString());
    setCwdTEST(getRegularPath("/").toString(), false);
    assertEquals(initCurDir + "/foo/bar",
                 getVimPathTesting(getRegularPath(initCurDir + "/foo/bar")).toString());
    
    // homedir/cd1/foo displays differently depending on where you're sitting
    setCwdTEST(initHomeDir + "cd1/cd2", false);
    assertEquals("~/cd1/foo",
                 getVimPathTesting(getRegularPath(initHomeDir + "/cd1/foo")).toString());
    setCwdTEST(initHomeDir, false);
    assertEquals("cd1/foo",
                 getVimPathTesting(getRegularPath(initHomeDir + "/cd1/foo")).toString());
    setCwdTEST(getRegularPath("/").toString(), false);
    assertEquals("~/cd1/foo",
                 getVimPathTesting(getRegularPath(initHomeDir + "/cd1/foo")).toString());
}

// /**
//  * Test of getPath method, of class FilePath.
//  */
// @Test
// @SuppressWarnings("UseOfSystemOutOrSystemErr")
// public void testGetPath()
// {
//     System.out.println("getPath");
//     assertEquals(initCurDir, getAbsolutePath(FilePath.getPath("")).toString());
//     assertEquals(initCurDir + "/foo/bar", getAbsolutePath(FilePath.getPath("foo/bar")).toString());
// 
//     assertEquals(initHomeDir, FilePath.getPath("~").toString());
//     assertEquals(initHomeDir + "/foo", FilePath.getPath("~/foo").toString());
//     assertEquals(initHomeDir + "/foo/bar", FilePath.getPath("~/foo/bar").toString());
// 
//     assertEquals("", FilePath.getPath("foo/..").toString());
//     assertEquals(getRegularPath(initCurDir + "/..").toString(),
//                  FilePath.getPath("foo/../..").toString());
//     assertEquals(initCurDir, getAbsolutePath(FilePath.getPath("x/y/../..")).toString());
// }

/**
 * Test of getCwd method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetCwd()
{
    System.out.println("getCwd");
    assertEquals(initCurDir, getCwd().toString());
}

/**
 * Test of getHomeDir method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetHomeDir()
{
    System.out.println("getHomeDir");
    assertEquals(initHomeDir, getHomeDir().toString());
    assertEquals(initHomeDir, getHomeDir().toString());
    System.setProperty("user.home", "not/absolute");
    assertEquals("/tmp", getHomeDir().toString());
}

/**
 * Test of getTildeDir method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetTildeDir()
{
    System.out.println("getTildeDir");
    assertEquals("~", getTildePath("").toString());
    assertEquals("~/x/y", getTildePath("x/y").toString());
    assertEquals(initHomeDir, getAbsolutePath(getTildePath("")).toString());
    assertEquals(initHomeDir + "/x/y",
                 getAbsolutePath(getTildePath("x/y")).toString());
    assertEquals(initHomeDir, getHomeDir().toString());
}

/**
 * Test of getAbsolutePath method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetAbsolutePath()
{
    System.out.println("getAbsolutePath");
    assertEquals(initCurDir, getAbsolutePath(getPath("")).toString());
    assertEquals(initHomeDir, getAbsolutePath(getPath("~")).toString());
}

/**
 * Test of isAbsolutePath method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testIsAbsolutePath()
{
    System.out.println("isAbsolutePath");
    Path path = getTildePath("a/b");
    assertEquals(true, isAbsolutePath(path));
    path = getPath("a/b");
    assertEquals(false, isAbsolutePath(path));
    assertEquals(true, isAbsolutePath(getAbsolutePath(path)));
}


/**
 * Test of modify_fname method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModify_fname()
{
    System.out.println("modify_fname");
    Path path;
    Path newp;
    Wrap<Path> fnamep = new Wrap<>();
    
    int start_idx;
    Path curdir = getCwd();

    Path homedir = getHomeDir();

    if (curdir.equals(homedir)) {
        System.err.println("CWD equals HOME. Some things not tested");
    }
    //assertNotEquals(curdir, homedir, "CWD equals HOME. Some things not tested");
    assertNotEquals("CWD equals HOME. Some things not tested", curdir, homedir);
    
    path = getPath("a/b/c/d");

    StringSegment modifs;

    modifs = new StringSegment("anything but :");
    start_idx = 3;
    modifs.setIndex(start_idx);
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals(path, newp);
    assertEquals(0, modifs.getIndex() - start_idx);

   
    // TEST: ":z" does nothing

    modifs = new StringSegment("xxx:zyyy");
    start_idx = 3;
    modifs.setIndex(start_idx);
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals(path, newp);
    assertEquals(0, modifs.getIndex() - start_idx);

    // TEST: ":p" and ., .., and such vim does a force expansion

}
/**
 * Test of modifyFilename method, of class FilePath.
 */
//
// NOTE: if the test is run and CWD == HOME then some things
//       are not tested.
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModifyFilename_p()
{
    System.out.println("modifyFilename_p");
    Path path;
    Path newp;
    Wrap<Path> fnamep = new Wrap<>();
    
    int start_idx;
    Path curdir = getCwd();

    Path homedir = getHomeDir();

    if (curdir.equals(homedir)) {
        System.err.println("CWD equals HOME. Some things not tested");
    }
    //assertNotEquals(curdir, homedir, "CWD equals HOME. Some things not tested");
    assertNotEquals("CWD equals HOME. Some things not tested", curdir, homedir);
    
    path = getPath("a/b/c/d");

    StringSegment modifs;


    // verify that seg can be used to calculate count
    modifs = new StringSegment("xxx:pyyy");
    start_idx = 3; // normally: start_idx = seg.getIndex() or somesuch
    modifs.setIndex(start_idx); // ":"
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals(curdir + "/a/b/c/d", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    //
    // ":p"
    //
    start_idx = 0;
    modifs = new StringSegment(":p");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals(curdir + "/a/b/c/d", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);
    
    modifs = new StringSegment(":p:pyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals(curdir + "/a/b/c/d", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    //
    // ":p" of tilde path
    //

    path = getPath("~/a/b");

    modify_fname(new StringSegment(":p"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals(getHomeDir() + "/a/b", newp.toString());
}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModifyFilename_tilde_dot()
{
    System.out.println("modifyFilename_tilde_dot");
    Path path;
    Path newp;
    StringSegment modifs;
    Wrap<Path> fnamep = new Wrap<>();
    int start_idx = 0;
    
    //
    // ":~"
    //
    // set path to /home/xxx/a/b/c
    //path = homedir.resolve("a/b/c");
    path = getHomeDir().resolve("a/b/c");
    modifs = new StringSegment(":~");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("~/a/b/c", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    // make sure not under homde.dir, then ":~" should do nothing
    path = getPath("/a/b/c");
    modifs = new StringSegment(":~yyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/a/b/c", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    //
    // ":."
    //
    // set path to curdir/a/b/c
    //path = FileSystems.getDefault().getPath(
    //        curdir.toAbsolutePath().toString() + "/a/b/c");
    /////path = curdir.resolve("a/b/c");
    path = getPath("a/b/c");
    modifs = new StringSegment(":.yyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("a/b/c", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    // make sure not under homde.dir, then ":~" should do nothing
    path = getPath("/a/b/c");
    modifs = new StringSegment(":.");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/a/b/c", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);
}


@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModifyFilename_h()
{
    System.out.println("modifyFilename_h");
    Path path;
    Path newp;
    StringSegment modifs;
    Wrap<Path> fnamep = new Wrap<>();
    int start_idx = 0;

    //
    // ":h"
    //
    path = getPath("a/b/c/d");

    modifs = new StringSegment(":hyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("a/b/c", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":h:h");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("a/b", newp.toString());
    assertEquals(4, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":h:h:hyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("a", newp.toString());
    assertEquals(6, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":h:h:h:h");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("", newp.toString());
    assertEquals(8, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":h:h:h:h:hyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("", newp.toString());
    assertEquals(10, modifs.getIndex() - start_idx);

    path = getPath("/a/b");

    modifs = new StringSegment(":h");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/a", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":h:hyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/", newp.toString());
    assertEquals(4, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":h:h:h");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/", newp.toString());
    assertEquals(6, modifs.getIndex() - start_idx);

    //
    // ":h" of tilde path
    //

    path = getPath("~/a/b");

    modify_fname(new StringSegment(":~:h"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("~/a", newp.toString());

    modify_fname(new StringSegment(":~:h:h"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("~", newp.toString());
}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModifyFilename_t()
{
    System.out.println("modifyFilename_t");
    Path path;
    Path newp;
    StringSegment modifs;
    Wrap<Path> fnamep = new Wrap<>();
    int start_idx = 0;

    //
    // ":t"
    //

    path = getPath("a/b/c/d");

    modifs = new StringSegment(":tyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("d", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    path = getPath("/a/b/c/d");

    modifs = new StringSegment(":t");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("d", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);
}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModifyFilename_r()
{
    System.out.println("modifyFilename_r");
    Path path;
    Path newp;
    StringSegment modifs;
    Wrap<Path> fnamep = new Wrap<>();
    int start_idx = 0;

    //
    // ":r"
    //

    path = getPath("/x/a.b.c");

    modifs = new StringSegment(":ryyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/x/a.b", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":r:r");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/x/a", newp.toString());
    assertEquals(4, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":r:r:ryyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/x/a", newp.toString());
    assertEquals(6, modifs.getIndex() - start_idx);


    path = getPath("/x/a");
    modifs = new StringSegment(":r");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/x/a", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    path = getPath("/x/.a");
    modifs = new StringSegment(":ryyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/x/.a", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    path = getPath("/x/.a");
    modifs = new StringSegment(":r:r");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/x/.a", newp.toString());
    assertEquals(4, modifs.getIndex() - start_idx);
}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModifyFilename_e()
{
    System.out.println("modifyFilename_e");
    Path path;
    Path newp;
    StringSegment modifs;
    Wrap<Path> fnamep = new Wrap<>();
    int start_idx = 0;

    //
    // ":e"
    //

    path = getPath("a.b.c.d");

    modifs = new StringSegment(":eyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("d", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":e:e");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("c.d", newp.toString());
    assertEquals(4, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":e:e:eyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("b.c.d", newp.toString());
    assertEquals(6, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":e:e:e:e");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("b.c.d", newp.toString());
    assertEquals(8, modifs.getIndex() - start_idx);

    modifs = new StringSegment(":e:e:e:e:eyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("b.c.d", newp.toString());
    assertEquals(10, modifs.getIndex() - start_idx);

    // ":e" special cases

    path = getPath("a");
    modifs = new StringSegment(":e");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    path = getPath(".a");
    modifs = new StringSegment(":eyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    path = getPath(".a.b.c");
    modifs = new StringSegment(":e");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("c", newp.toString());
    assertEquals(2, modifs.getIndex() - start_idx);

    path = getPath(".a.b.c");
    modifs = new StringSegment(":e:eyyy");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("b.c", newp.toString());
    assertEquals(4, modifs.getIndex() - start_idx);

    path = getPath(".a.b.c");
    modifs = new StringSegment(":e:e:e");
    modify_fname(modifs, path, fnamep);
    newp = fnamep.getValue();
    assertEquals("b.c", newp.toString());
    assertEquals(6, modifs.getIndex() - start_idx);
}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModifyFilename_helpexamples_1()
{
    System.out.println("modifyFilename_helpexamples_1");
    Path path;
    Path newp;
    Wrap<Path> fnamep = new Wrap<>();

    setCwdTEST("/home/mool/vim");
    System.setProperty("user.home", "/home/mool");

    path = getPath("src/version.c");

    modify_fname(new StringSegment(":p"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/home/mool/vim/src/version.c", newp.toString());

    modify_fname(new StringSegment(":p:."), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("src/version.c", newp.toString());

    modify_fname(new StringSegment(":p:~"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("~/vim/src/version.c", newp.toString());

    modify_fname(new StringSegment(":p:h"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/home/mool/vim/src", newp.toString());

    modify_fname(new StringSegment(":p:h:h"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/home/mool/vim", newp.toString());

    modify_fname(new StringSegment(":t"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("version.c", newp.toString());

    modify_fname(new StringSegment(":p:t"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("version.c", newp.toString());

    modify_fname(new StringSegment(":r"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("src/version", newp.toString());

    modify_fname(new StringSegment(":p:r"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/home/mool/vim/src/version", newp.toString());

    modify_fname(new StringSegment(":t:r"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("version", newp.toString());

    modify_fname(new StringSegment(":e"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("c", newp.toString());

}

@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testModifyFilename_helpexamples_2()
{
    System.out.println("modifyFilename_helpexamples_2");
    Path path;
    Path newp;
    Wrap<Path> fnamep = new Wrap<>();

    setCwdTEST("/home/mool/vim");
    System.setProperty("user.home", "/home/mool");

    path = getPath("src/version.c.gz");

    modify_fname(new StringSegment(":p"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("/home/mool/vim/src/version.c.gz", newp.toString());

    modify_fname(new StringSegment(":e"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("gz", newp.toString());

    modify_fname(new StringSegment(":e:e"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("c.gz", newp.toString());

    modify_fname(new StringSegment(":e:e:e"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("c.gz", newp.toString());

    modify_fname(new StringSegment(":e:e:r"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("c", newp.toString());

    modify_fname(new StringSegment(":r"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("src/version.c", newp.toString());

    modify_fname(new StringSegment(":r:e"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("c", newp.toString());

    modify_fname(new StringSegment(":r:r"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("src/version", newp.toString());

    modify_fname(new StringSegment(":r:r:r"), path, fnamep);
    newp = fnamep.getValue();
    assertEquals("src/version", newp.toString());
}

/**
 * Test of getRealPath method, of class FilePath.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetRealPath()
{
    System.out.println("getRealPath");
    Path expResult = null;
    Path path;
    Path result;
    try {
        path = getPath("a");
        result = getRealPath(path);
        eatme(result);
        //assertEquals(expResult, result);
        path = getPath("/test/a");
        result = getRealPath(path, LinkOption.NOFOLLOW_LINKS);
        //assertEquals(expResult, result);
        eatme(expResult, result);
    } catch(IOException ex) {
        Exceptions.printStackTrace(ex);
    }
}

/**
 * Test of getBaseName method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetBaseName()
{
    System.out.println("getBaseName");
    assertEquals("b.c", getBaseNameString("/a/b.c.d"));
}

/**
 * Test of indexOfLastSeparator method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testIndexOfLastSeparator()
{
    System.out.println("indexOfLastSeparator");
    assertEquals(2, indexOfLastSeparator("/a/b"));
    assertEquals(4, indexOfLastSeparator("/a/b/"));
}

/**
 * Test of getName method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetName()
{
    System.out.println("getName");
    assertEquals("a.b", getFileNameString("/a.b"));
    assertEquals("", getFileNameString("/a.b/c/"));
    assertEquals(".", getFileNameString("/a.b/."));
}

/**
 * Test of removeExtension method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testRemoveExtension()
{
    System.out.println("removeExtension");
    assertEquals("/a", removeExtensionString("/a.b"));
    assertEquals("/a.b/foo", removeExtensionString("/a.b/foo"));
}

/**
 * Test of indexOfExtension method, of class FilePath.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testIndexOfExtension()
{
    System.out.println("indexOfExtension");

    assertEquals(2, indexOfExtension("/a.b"));
    assertEquals(-1, indexOfExtension("/a.b/foo"));
    assertEquals(0, indexOfExtension(".foo"));
}


}
