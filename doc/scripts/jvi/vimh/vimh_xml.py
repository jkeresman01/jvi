import os, sys
import re
import vimh_scan as VS
from vimh_scan import VimHelpScanner
from vimh_build import VimHelpBuildXml
import vimh_gen as VG
from collections import deque
import xml.etree.ElementTree as ET

if len(sys.argv) < 2:   exit(1)

# usage: cmd input_dir [output_dir]
# if output_dir not present, use input_dir
# tags and any .txt files in input_dir are processed

INPUT_DIR = sys.argv[1]
if INPUT_DIR[-1] != '/':
    INPUT_DIR = INPUT_DIR + '/'
if len(sys.argv) > 2:
    OUTPUT_DIR = sys.argv[2]
    if OUTPUT_DIR[-1] != '/':
        OUTPUT_DIR = OUTPUT_DIR + '/'
    try: os.mkdir(OUTPUT_DIR)
    except: pass
else:
    OUTPUT_DIR = INPUT_DIR

print('input dir:', INPUT_DIR, 'output dir:', OUTPUT_DIR)


TAGS_FILE = OUTPUT_DIR + 'tags'

helpfiles = [ x for x in os.listdir(INPUT_DIR) if x.endswith('.txt') ]

#print 'helpfiles:', helpfiles


PAT_TITLE    = r'(?P<title>jVi version [0-9.a-z]+|JVI REFERENCE.*)'
VS.PAT_TITLE = PAT_TITLE

class my_deque(deque):
    pass

class VimHelp2Xml(object):

    def __init__(self, tags):
        self.parser = VimHelpScanner()
        self.builder = VimHelpBuildXml(tags)

    def to_xml(self, filename, contents, include_sitesearch = True,
            include_faq = True):

        tokens = my_deque()
        self.parser.parse(filename, contents, tokens, include_faq)
        self.builder.process(tokens)

        return self.builder.get_output()

def sanitize(root):
    """ turn the lists into strings """
    print("===== TABLE ROOTS =====")
    print(root.findall(".//table"))
    for t in root.findall(".//table"):
        #ET.dump(t)
        # vh_markup, vh_cols
        for (k,v) in t.items():
            if not isinstance(v, str):
                #print('FIX: ', k, '=', str(v))
                t.set(k, str(v))
        print(' '.join([k + '="' + v + '"\n' for (k,v) in t.items()]))


with open(TAGS_FILE) as f: h2x = VimHelp2Xml(f)

for helpfile in helpfiles:
    with open(INPUT_DIR + helpfile) as f:
        xml = h2x.to_xml(helpfile, f, False, False)
        #VG.edump(xml.dump())
        #ET.dump(xml)
        #sanitize(xml)

    #encoding='utf-8', errors='backslashreplace',
    with open(OUTPUT_DIR + helpfile + '.xml', mode='w', newline='') as f:
        xml.write(f, encoding='unicode')

