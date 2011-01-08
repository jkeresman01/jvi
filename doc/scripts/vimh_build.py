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

def build_link_re_from_pat():
    global RE_LINKWORD
    RE_LINKWORD = re.compile(
            vs.PAT_OPTWORD  + '|' + \
            vs.PAT_CTRL     + '|' + \
            vs.PAT_SPECIAL)

#
# This class is like a base class for accepting tokens
# from VimHelpScanner. It simply collects what it gets, and can dump it.
#
class VimHelpBuildBase(object):

    def start_file(self, filename):
        self.filename = filename
        self.out = [ ]

    def start_line(self, lnum, line):
        """The next line to be parsed, generally for debug/diagnostics"""
        self.input_line = line
        self.lnum = lnum

    def markup(self, markup):
        pass

    def put_token(self, token_data):
        """token_data is (token, chars, col)."""
        #print token_data
        pass

    def get_output(self):
        pass

RE_TAGLINE = re.compile(r'(\S+)\s+(\S+)')

class Link:
    def __init__(self, filename):
        self.filename = filename

class Links(dict):
    def __missing__(self, key):
        return None

    def __init__(self, tags):
        for line in tags:
            m = RE_TAGLINE.match(line)
            if m:
                tag, filename = m.group(1, 2)
                self.do_add_tag(filename, tag)

    def do_add_tag(self, filename, tag):
        # determine style for a plain link
        style = 'hidden'
        m = RE_LINKWORD.match(tag)
        if m:
            opt, ctrl, special = m.group('opt', 'ctrl', 'special')
            if opt is not None: style = 'option'
            elif ctrl is not None: style = 'keystroke'
            elif special is not None: style = 'special'
        link = Link(filename)
        link.style = style
        self[tag] = link


#
# XML builder
#

#
# Simple Html builder, should reproduce original work from Carlo
#

# Note that the lazy creation of link_plain,link_pipe 
# provides for reporting defined links that are not referenced
class HtmlLinks(Links):
    # styles map to the html style class for the link
    styles = dict(link='l', option='o', keystroke='k',
                  special='s', hidden='d')

    def __init__(self, tags):
        super(HtmlLinks, self).__init__(tags)

    def get_tag(self, tag):
        """Lazily create link_plain and link_pipe."""
        link = self[tag]
        if not link : return None
        if not hasattr(link, 'link_plain'):
            part1 = '<a href="' + link.filename + '.html#' + \
                    urllib.quote_plus(tag) + '"'
            part2 = '>' + cgi.escape(tag) + '</a>'
            link.link_pipe = part1 \
                    + ' class="' + self.styles['link'] + '"' + part2
            link.link_plain = part1 \
                    + ' class="' + self.styles[link.style] + '"' + part2
        return link

    def maplink(self, tag, css_class = None):
        link = self.get_tag(tag)
        if link is not None:
            # this is a known link from the tags file
            if css_class == 'link':
                # drop the anchor if foo.txt and foo.txt.html#foo.txt
                if tag.endswith('.txt') \
                        and link.link_pipe.find(
                                '"' + tag + '.html#' + tag + '"') >= 0:
                    return link.link_pipe.replace('#' + tag, '');
                return link.link_pipe
            else: return link.link_plain
        elif css_class is not None:
            # not a known link, but a class was specified
            return '<span class="' + self.styles[css_class] \
                    + '">' + cgi.escape(tag) + '</span>'
        else:
            # not know link, no class specifed, just return it
            return cgi.escape(tag)


class VimHelpBuildHtml(VimHelpBuildBase):

    def __init__(self, tags):
        build_link_re_from_pat()
        self.links = HtmlLinks(tags)


    def markup(self, markup):
        markup = markup.strip()
        # print 'markup: %s:%s "%s"' \
        #         % (self.filename, self.lnum, markup)
        pass

    def put_token(self, token_data):
        """token_data is (type, chars, col)."""
        token, chars, col = token_data
        #print token_data
        if 'pipe' == token:
            self.out.append(self.links.maplink(chars, 'link'))
        elif 'star' == token:
            tag = chars
            self.out.append('<a name="' + urllib.quote_plus(tag) +
                    '" class="t">' + cgi.escape(tag) + '</a>')
        elif 'opt' == token:
            self.out.append(self.links.maplink(chars, 'option'))
        elif 'ctrl' == token:
            self.out.append(self.links.maplink(chars, 'keystroke'))
        elif 'special' == token:
            self.out.append(self.links.maplink(chars, 'special'))
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
            self.out.append(self.links.maplink(chars))
        elif 'example' == token:
            self.out.append('<span class="e">' + cgi.escape(chars) +
                    '</span>\n')
        elif 'section' == token:
            # NOTE: WHY NOT cgi.escape?????
            self.out.append(r'<span class="c">' + chars + '</span>')
            # print self.filename + ': section: "' + chars +'"'
        elif 'chars' == token:
            if not chars.isspace():
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

