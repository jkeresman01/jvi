# converts vim documentation to html

import sys
import re
import cgi
import urllib
from vimh_parse import VimHelpParser


HEADER1 = """
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>Vim: {filename}</title>
<!--[if IE]>
<link rel="stylesheet" href="vimhelp-ie.css" type="text/css">
<![endif]-->
<!--[if !IE]>-->
<link rel="stylesheet" href="vimhelp.css" type="text/css">
<!--<![endif]-->
</head>
<body>
"""

START_HEADER = """
<h1>Vim help files</h1>
<p>This is an HTML version of the <a href="http://www.vim.org/"
target="_blank">Vim</a> help pages. They are kept up-to-date automatically from
the <a href="http://code.google.com/p/vim/source/browse/runtime/doc"
target="_blank" class="d">Vim source repository</a>. Also included is the <a
href="vim_faq.txt.html">Vim FAQ</a>, kept up to date from its <a
href="http://github.com/chrisbra/vim_faq" target="_blank" class="d">github
repository</a>.</p>
"""
NAV_OVER  = '<a href="/">help overview</a>'
NAV_QUICK = '<a href="quickref.txt.html">quick reference</a>'
NAV_UTOC  = '<a href="usr_toc.txt.html">user manual toc</a>'
NAV_RTOC  = '<a href="help.txt.html#reference_toc">reference manual toc</a>'
NAV_FAQ   = '<a href="vim_faq.txt.html">faq</a>'

SITENAVI = '<p> Quick links: ' \
        + NAV_OVER + ' &middot; \n' \
        + NAV_QUICK + ' &middot; \n' \
        + NAV_UTOC + ' &middot; \n' \
        + NAV_RTOC + ' &middot; \n' \
        + NAV_FAQ + ' \n' \
        + '</p>'


SITESEARCH = """
<div id="cse" style="width: 100%;">Loading Google custom search</div>
<script src="http://www.google.com/jsapi" type="text/javascript"></script>
<script type="text/javascript">
  google.load('search', '1', {language : 'en'});
  google.setOnLoadCallback(function() {
    var customSearchControl = new google.search.CustomSearchControl('007529716539815883269:a71bug8rd0k');
    customSearchControl.setResultSetSize(google.search.Search.FILTERED_CSE_RESULTSET);
    customSearchControl.draw('cse');
  }, true);
</script>
"""

HEADER2 = """
<div id="d1">
<pre id="sp">                                                                                </pre>
<div id="d2">
<pre>
"""

OWNER = '<p id="footer">This site is maintained by Carlo Teubner (<i>(my first name) dot (my last name) at gmail dot com</i>).</p>'

FOOTER = '</pre>'

FOOTER2 = """
</div>
</div>
</body>
</html>
"""

class VimH2H:
    def __init__(self, tags):
        self.parser = VimHelpParser(tags)

    def to_html(self, filename, contents, include_sitesearch = True,
            include_faq = True):

        out = self.parser.parse(filename, contents, include_faq)

        return HEADER1.replace('{filename}', filename) + \
                (START_HEADER if filename == 'help.txt' else '') + \
                SITENAVI + \
                (SITESEARCH if include_sitesearch else '') + \
                HEADER2 + \
                ''.join(out) + \
                FOOTER + \
                SITENAVI + \
                OWNER + \
                FOOTER2

