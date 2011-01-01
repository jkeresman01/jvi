# converts vim documentation to html

from vimh_parse import VimHelpParser


HTML_HEAD = """
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
"""

TOP_TEXT = """
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

BEG_NAVI = '<p> Quick links: '
END_NAVI = '</p>'
SITENAVI = [ NAV_OVER, NAV_QUICK, NAV_UTOC, NAV_RTOC, NAV_FAQ ]

def _site_navi():
    return BEG_NAVI + ' &middot; '.join(SITENAVI) + END_NAVI


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

BODY_BEGIN = '<body>'
BODY_END = '</body></html>'

TOP_BEGIN = ''
TOP_END = ''

MAIN_BEGIN = """
<div id="d1">
<pre id="sp">                                                                                </pre>
<div id="d2">
<pre>
"""
MAIN_END = '</pre>'

OWNER = '<p id="footer">This site is maintained by Carlo Teubner (<i>(my first name) dot (my last name) at gmail dot com</i>).</p>'

FOOTER_BEGIN = ''
FOOTER_END = """
</div>
</div>
"""

class VimH2H(object):
    def __init__(self, tags):
        self.parser = VimHelpParser(tags)

    def to_html(self, filename, contents, include_sitesearch = True,
            include_faq = True):

        out = self.parser.parse(filename, contents, include_faq)

        return HTML_HEAD.replace('{filename}', filename) \
                + BODY_BEGIN \
                + TOP_BEGIN \
                + (TOP_TEXT if filename == 'help.txt' else '') \
                + _site_navi() \
                + (SITESEARCH if include_sitesearch else '') \
                + TOP_END \
                + MAIN_BEGIN \
                + ''.join(out) \
                + MAIN_END \
                + FOOTER_BEGIN \
                + _site_navi() \
                + OWNER \
                + FOOTER_END \
                + BODY_END

