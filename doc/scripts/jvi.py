import os, sys
import re
import vimh2h as vh
from vimh2h import VimH2H
import vimh_parse as vp

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

vh.TOP_TEXT = """
<h1>jVi help files</h1>
<p class="fix-width">This is an HTML version of the
<a href="http:http://jvi.sourceforge.net/" target="_blank">jVi</a> help pages.
They are almost entirely copied from the <a href="http://www.vim.org/"
target="_blank">Vim</a> help pages. Those familiar with the vim help pages 
will notice that there is much less information here; that is because jVi
has fewer features and is not as fully documented.
</p>
"""

vh.TOP_BEGIN = """
<div id="d1">
<pre id="sp">                                                                                </pre>
<div id="d2">
"""
vh.TOP_END =  ''

vh.MAIN_BEGIN = """
<div>
<pre>
"""
vh.MAIN_END = """
</pre>
"""
vh.FOOTER_END = """
</div>
</div>
</div>
"""

JVI_NAV_OVER  = '<a href="help.txt.html">help overview</a>'
JVI_NAV_INDEX = '<a href="index.txt.html">command index</a>'
vh.SITENAVI = [ JVI_NAV_OVER, vh.NAV_QUICK, vh.NAV_RTOC, JVI_NAV_INDEX ]

vh.OWNER = '<p id="footer">These help files are maintained with thanks' \
        +' to Carlo Teubner for his vimhelp to html work.</p>'

TAGS_FILE = INPUT_DIR + 'tags'

helpfiles = [ x for x in os.listdir(INPUT_DIR) if x.endswith('.txt') ]

print 'helpfiles:', helpfiles


PAT_TITLE    = r'(?P<title>jVi version [0-9.a-z]+|JVI REFERENCE.*)'
vp.PAT_TITLE = PAT_TITLE

class JviH2H(VimH2H):
    def __init__(self, tags):
        super(JviH2H, self).__init__(tags)

with open(TAGS_FILE) as f: h2h = JviH2H(f)

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
            html = h2h.to_html(helpfile, l, False, False)
            l = None
        else: html = h2h.to_html(helpfile, f, False, False)

    with open(OUTPUT_DIR + helpfile + '.html', 'w') as f: f.write(html)
