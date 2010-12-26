import os, sys
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

TAGS_FILE = INPUT_DIR + 'tags'

helpfiles = [ x for x in os.listdir(INPUT_DIR) if x.endswith('.txt') ]

print 'helpfiles:', helpfiles

with open(TAGS_FILE) as f: h2h = VimH2H(f)

for helpfile in helpfiles:
    with open(INPUT_DIR + helpfile) as f: html = h2h.to_html(helpfile, f)
    with open(OUTPUT_DIR + helpfile + '.html', 'w') as f: f.write(html)
