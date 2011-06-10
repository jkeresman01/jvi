#!/usr/bin/python

import os, sys
import shutil
import collections
#import StringIO
import xml.etree.ElementTree as ET
import vimh_gen as VG

def usage():
    print 'xxx catalog.xml*'
    exit(1)

args = sys.argv[1:]

if len(args) < 1:
    usage()

def dumpStruct(x, l = 0):
    l1 = l + 1
    _idump(x.tag, l)
    for e in x.getchildren():
        dumpStruct(e, l1)

def _idump(s, l):
    if s: print ' ' * (l*2) + s

def print_catalog(src):
    (input_dir, fname) = os.path.split(src)
    xml = ET.parse(src)
    modules = [ x for x in xml.getroot().getiterator("module") ]
    for m in modules:
        nbm = m.get("distribution")
        vers = m.find("manifest").get("OpenIDE-Module-Specification-Version")
        print "%12s %s" % (vers, nbm)

for catalog in args:
    print
    print (catalog)
    print_catalog(catalog)

print

