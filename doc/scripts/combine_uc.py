#!/usr/bin/python

import os, sys
import shutil
import collections
#import StringIO
import xml.etree.ElementTree as ET
import vimh_gen as VG

def usage():
    print 'xxx source+ dest_dir'
    exit(1)

UPD = 'updates.xml'
GUTS = 'cat-guts'
CAT = """
<module_updates timestamp="07/05/09/5/2/2011">
</module_updates>
"""

args = sys.argv[1:]

if len(args) < 2:
    usage()

uc_out_dir = args[-1]
args = args[:-1]

#print (uc_out_dir, args)

def dumpStruct(x, l = 0):
    l1 = l + 1
    _idump(x.tag, l)
    for e in x.getchildren():
        dumpStruct(e, l1)

def _idump(s, l):
    if s: print ' ' * (l*2) + s

catalog_out = ET.XML(CAT)
licenses = []

def copy_uc(src):
    (input_dir, fname) = os.path.split(src)
    xml = ET.parse(src)
    modules = [ x for x in xml.getroot().getiterator("module") ]

    # add modules to the catalog; collect nbm names
    nbms = []
    for m in modules:
        catalog_out.append(m)
        nbm = m.get("distribution")
        vers = m.find("manifest").get("OpenIDE-Module-Specification-Version")
        print "%12s %s" % (vers, nbm)
        nbms.append(nbm)

    # copy modules
    # print "NBMs", nbms
    for nbm in nbms:
        shutil.copy(os.path.join(input_dir, nbm), uc_out_dir)

    # stash list of (name, url, base_dir, license_element)
    for l in xml.getroot().getiterator("license"):
        name = l.get("name")
        url = l.get("url")
        # saving the element, 'l', in following is "just in case"
        licenses.append((name, url, input_dir, l))

def add_licenses():
    # assume name is unique across all input dir's
    # assume all have url (not inline)
    names = set()
    for lic in licenses:
        (name, url, idir, e) = lic
        if name in names:
            continue
        print 'Adding license ' + name
        l = ET.Element("license")
        with open(os.path.join(idir, url)) as f:
            l.text = f.read()
        l.tail = '\n\n'
        l.set("name", name)
        catalog_out.append(l)
        names.add(name)


for arg in args:
    copy_uc(arg)

add_licenses()

ET.ElementTree(catalog_out).write(uc_out_dir + '/' + GUTS)

exit(0)







for arg in args:
    (input_dir, fname) = os.path.split(arg)

    # if UPD == fname

    updates = arg
    print "===== " + arg

    xml = ET.parse(updates)

    modules = [ x for x in xml.getroot().getiterator("module") ]

    print "===== modules"
    for m in modules:
        dumpStruct(m)


    #ET.dump(xml)

    #print xml.getroot().tag

    #for e in xml.getroot():
    #    print "    " + e.tag

    print "===== struct"

    dumpStruct(xml.getroot())

