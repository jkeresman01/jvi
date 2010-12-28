import os, sys
import re
import vimh2h as vh
from vimh2h import VimH2H

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

print 'input dir:', INPUT_DIR, 'output dir:', OUTPUT_DIR

JVI_NAV_OVER  = '<a href="help.txt.html">help overview</a>'
vh.SITENAVI = '<p> Quick links: ' \
        + JVI_NAV_OVER + ' &middot; \n' \
        + vh.NAV_QUICK + ' &middot; \n' \
        + vh.NAV_RTOC + ' &middot; \n' \
        + '</p>'

TAGS_FILE = INPUT_DIR + 'tags'

helpfiles = [ x for x in os.listdir(INPUT_DIR) if x.endswith('.txt') ]

print 'helpfiles:', helpfiles

with open(TAGS_FILE) as f: h2h = VimH2H(f)

for helpfile in helpfiles:
    with open(INPUT_DIR + helpfile) as f:
        if helpfile == 'index.txt':
            # initial columns might be like ' |w|', 'x|w|', ...
            re_fix = re.compile(r'^(?:([ e])|([x.]))\|')
            l = []
            skipping = 0
            for line in f:
                m = re_fix.match(line)
                if m:
                    line = line[1:]
                # if line start with whitespace and has characters on it
                if skipping and len(line) > 0 \
                        and line[0].isspace()  and not line.isspace():
                    continue
                skipping = 0
                if m and m.lastindex == 2:
                    skipping = 1
                    continue
                l.append(line)
            html = h2h.to_html(helpfile, l, False)
            l = None
        else: html = h2h.to_html(helpfile, f, False)

    with open(OUTPUT_DIR + helpfile + '.html', 'w') as f: f.write(html)
