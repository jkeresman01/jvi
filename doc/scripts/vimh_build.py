import re
import cgi
import urllib
import vimh_scan as vs

# accept tokens from vim help scanner
#
# A builder is created with a set of tags.
# It puts stuff together per file
#

# The following are the tokens recognized by the vim scanner
#       header
#       ruler           col ignored
#       graphic
#       pipe
#       star
#       opt
#       ctrl
#       special
#       title
#       note
#       url
#       word
#       example
#       section
#       chars
#       newline         col ignored

#
# This class is like a base class for accepting tokens
# from VimHelpScanner. It simply collects what it gets, and can dump it.
#
class VimHelpBuildDummy(object):

    def start_file(self, filename):
        pass

    def start_line(self, line):
        pass

    def put_token(self, token_data):
        """token_data is (token, chars, col)."""
        #print token_data
        pass

    def get_output(self):
        pass

RE_TAGLINE = re.compile(r'(\S+)\s+(\S+)')

#
# XML builder
#

#
# Simple Html builder, should reproduce original work from Carlo
#

def build_html_re_from_pat():
    global RE_LINKWORD
    RE_LINKWORD = re.compile(
            vs.PAT_OPTWORD  + '|' + \
            vs.PAT_CTRL     + '|' + \
            vs.PAT_SPECIAL)

class HtmlLink:
    def __init__(self, link_pipe, link_plain):
        self.link_pipe = link_pipe
        self.link_plain = link_plain

class VimHelpBuildHtml(object):
    urls = { }

    def __init__(self, tags):
        build_html_re_from_pat()
        for line in tags:
            m = RE_TAGLINE.match(line)
            if m:
                tag, filename = m.group(1, 2)
                self.do_add_tag(filename, tag)

    def start_file(self, filename):
        self.filename = filename
        self.out = [ ]

    def add_tags(self, filename, contents):
        for match in RE_STARTAG.finditer(contents):
            tag = match.group(1).replace('\\', '\\\\').replace('/', '\\/')
            self.do_add_tag(filename, tag)

    def do_add_tag(self, filename, tag):
        part1 = '<a href="' + filename + '.html#' + \
                urllib.quote_plus(tag) + '"'
        part2 = '>' + cgi.escape(tag) + '</a>'
        link_pipe = part1 + ' class="l"' + part2
        classattr = ' class="d"'
        m = RE_LINKWORD.match(tag)
        if m:
            opt, ctrl, special = m.group('opt', 'ctrl', 'special')
            if opt is not None: classattr = ' class="o"'
            elif ctrl is not None: classattr = ' class="k"'
            elif special is not None: classattr = ' class="s"'
        link_plain = part1 + classattr + part2
        self.urls[tag] = HtmlLink(link_pipe, link_plain)

    def maplink(self, tag, css_class = None):
        links = self.urls.get(tag)
        if links is not None:
            if css_class == 'l':
                # drop the anchor if foo.txt and foo.txt.html#foo.txt
                if tag.endswith('.txt') \
                        and links.link_pipe.find(
                                '"' + tag + '.html#' + tag + '"') >= 0:
                    return links.link_pipe.replace('#' + tag, '');
                return links.link_pipe
            else: return links.link_plain
        elif css_class is not None:
            return '<span class="' + css_class + '">' + cgi.escape(tag) + \
                    '</span>'
        else: return cgi.escape(tag)

    def start_line(self, line):
        """The next line to be parsed, generally for debug/diagnostics"""
        self.input_line = line
        #print line
        pass

    def put_token(self, token_data):
        """token_data is (type, chars, col)."""
        token, chars, col = token_data
        #print token_data
        if 'pipe' == token:
            self.out.append(self.maplink(chars, 'l'))
        elif 'star' == token:
            tag = chars
            self.out.append('<a name="' + urllib.quote_plus(tag) +
                    '" class="t">' + cgi.escape(tag) + '</a>')
        elif 'opt' == token:
            self.out.append(self.maplink(chars, 'o'))
        elif 'ctrl' == token:
            self.out.append(self.maplink(chars, 'k'))
        elif 'special' == token:
            self.out.append(self.maplink(chars, 's'))
        elif 'title' == token:
            self.out.append('<span class="i">' +
                    cgi.escape(chars) + '</span>')
        elif 'note' == token:
            self.out.append('<span class="n">' +
                    cgi.escape(chars) + '</span>')
        elif 'ruler' == token:
            self.out.append('<span class="h">' + chars + '</span>\n')
        elif 'header' == token:
            self.out.append('<span class="h">' +
                    cgi.escape(chars) + '</span>')
        elif 'graphic' == token:
            self.out.append(cgi.escape(chars))
        elif 'url' == token:
            self.out.append('<a class="u" href="' + chars + '">' +
                    cgi.escape(chars) + '</a>')
        elif 'word' == token:
            self.out.append(self.maplink(chars))
        elif 'example' == token:
            self.out.append('<span class="e">' + cgi.escape(chars) +
                    '</span>\n')
        elif 'section' == token:
            # NOTE: WHY NOT cgi.escape?????
            self.out.append(r'<span class="c">' + chars + '</span>')
        elif 'chars' == token:
            if not chars.isspace():
                #print '"',chars,'"', self.filename, self.input_line, 'NOT ISSPACE'
                #print '"%s" %s:"%s" NOT ISSPACE' \
                #        % (chars,self.filename, self.input_line)
                # the only non-space I've seen is blanks followed by a double-quote
                pass
            self.out.append(cgi.escape(chars))
        elif 'newline' == token:
            self.out.append('\n')
        else: print 'ERROR: unknown token "' + token + '"'

    def get_output(self):
        return self.out

