import re
import cgi
import xml.etree.ElementTree as ET
import urllib
import vimh_scan as vs

# accept tokens from vim help scanner
#
# A builder is created with a set of tags.
# It puts stuff together per file
#

# The following are the tokens recognized by the vim scanner.
# They are grouped by either paragraphs or words, where paragraphs
# are groups of lines.
#
# Note: there is also 'newline' token.
# Note: 'newline' ignores the column
#
# Note: there is a link type of 'hidden', much like a token
#

SET_PARA = set(('header',
                'ruler',
                'graphic',
                'section',
                'title',
                'example'))

SET_WORD = set(('pipe',
                'star',
                'opt',
                'ctrl',
                'special',
                'note',
                'url',
                'word',
                'chars'))

TY_PARA = 1
TY_WORD = 2
TY_NL   = 3

MAP_TY = {}
MAP_TY.update(zip(SET_PARA, (TY_PARA,) * len(SET_PARA)))
MAP_TY.update(zip(SET_WORD, (TY_WORD,) * len(SET_WORD)))
MAP_TY['newline'] = TY_NL

def build_link_re_from_pat():
    global RE_LINKWORD
    RE_LINKWORD = re.compile(
            vs.PAT_OPTWORD  + '|' + \
            vs.PAT_CTRL     + '|' + \
            vs.PAT_SPECIAL)

#
# This class is a base class for accepting tokens from VimHelpScanner.
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
        return self.out

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
            # style to one of: opt, ctrl, special
            style = m.lastgroup
        link = Link(filename)
        link.style = style
        self[tag] = link

    def maplink(self, tag, css_class = None):
        link = self[tag]
        if link is not None:
            # this is a known link from the tags file
            if css_class and css_class != self.style:
                print 'LINK STYLE MISMATCH'
            pass
        elif css_class is not None:
            # not a known link, but a class was specified
            pass
        else:
            # not know link, no class specifed, just return it
            return tag

#
# XML builder
#
# <vimhelp> is the root, the schema looks a bit like
#       <vimhelp> ::= [ <p> | <table> ]+
# and these are made up of leaf elements
#       element   ::= <target> | <link> | <em>
#
# Every element can have a "t" attribute (type). For paragraphs they are
# one of SET_PARA. For the leaf elements they are from SET_WORD.
# But note that 'word' and 'chars' is typically just text.
#

class XmlLinks(Links):
    def __init__(self, tags):
        super(XmlLinks, self).__init__(tags)

    def maplink(self, tag, style = None):
        link = self[tag]
        if link is not None:
            # this is a known link from the tags file
            if style and style != link.style: # and style != 'pipe':
                print 'LINK STYLE MISMATCH'
            elem_tag = 'link'
        elif style is not None:
            # not a known link, but a class was specified
            elem_tag = 'em'
        else:
            # not known link, no class specifed
            return tag
        return VimHelpBuildXml.make_elem(elem_tag, style, tag)

class VimHelpBuildXml(VimHelpBuildBase):

    def __init__(self, tags):
        build_link_re_from_pat()
        self.links = XmlLinks(tags)
        self.blank_lines = 0
        self.root = ET.Element('vimhelp')
        self.tree = ET.ElementTree(self.root)
        self.cur_elem = None

    def get_output(self):
        return self.tree

    def start_line(self, lnum, line):
        super(VimHelpBuildXml, self).start_line(lnum, line)
        print 'start_line:', self.lnum, self.input_line

    def markup(self, markup):
        print 'markup:', markup
        pass

    def put_token(self, token_data):
        """token_data is (token, chars, col)."""
        token, chars, col = token_data
        ty = MAP_TY[token]
        print 'token_data:', ty, token_data
        if ty == TY_NL:
            if len(self.input_line) == 0 or self.input_line.isspace():
                self.blank_lines += 1
                print 'BLANK_LINE'
            else:
                self.add_stuff('\n')

        elif ty == TY_PARA:
            self.add_para(token, chars)
        else:
            if token == 'chars':
                w = chars
            elif token == 'word':
                # may end up mapped to a 'link' or 'em'
                w = self.links.maplink(chars)
            elif token == 'pipe':
                w = self.links.maplink(chars, 'pipe')
            elif token == 'star':
                w = self.make_elem('target', token, chars)
            elif token in ('opt', 'ctrl', 'special'):
                w = self.links.maplink(chars, token)
            else:
                w = self.make_elem('em', token, chars)
            self.add_stuff(w)

    def add_stuff(self, stuff):
        self.fixup_blank_lines()
        self.do_add_stuff(stuff)

    def do_add_stuff(self, stuff):
        """Add plain text or an element to current paragraph."""
        e = self.get_cur_elem()
        if ET.iselement(stuff):
            e.append(stuff)
            return
        if len(e) == 0:
            e.text += stuff
        else:
            e[-1].tail += stuff

    def add_para(self, token, chars):
        self.fixup_blank_lines()
        e = self.cur_elem
        # if not the same kind of paragraph, "close" it
        if e is not None and (e.tag != 'p' or e.get('t', '') != token):
            # done with current paragraph
            self.cur_elem = None
        if self.cur_elem is None:
            e = self.get_cur_elem()
            e.set('t', token)
        self.add_stuff(chars)

    @staticmethod
    def make_elem(elem_tag, style, chars):
        e = ET.Element(elem_tag, {'t':style})
        e.text = chars
        e.tail = ''
        return e

    def get_cur_elem(self):
        if self.cur_elem is not None: return self.cur_elem
        p = ET.SubElement(self.root, 'p')
        self.cur_elem = p
        p.text = ''
        p.tail = ''
        return p

    def fixup_blank_lines(self, token = None):
        # may treat blank lines as continuation of current paragraph
        # such as blank lines in header/title
        closeit = False
        while self.blank_lines > 0:
            closeit = True
            self.do_add_stuff('\n')
            self.blank_lines -= 1
        if closeit:
            self.cur_elem = None

#
# Simple Html builder, should reproduce original work from Carlo
#

# Note that the lazy creation of link_plain,link_pipe 
# provides for reporting defined links that are not referenced
class HtmlLinks(Links):
    # styles map to the html style class for the link
    styles = dict(link='l', opt='o', ctrl='k',
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
            self.out.append(self.links.maplink(chars, 'opt'))
        elif 'ctrl' == token:
            self.out.append(self.links.maplink(chars, 'ctrl'))
        elif 'special' == token:
            self.out.append(self.links.maplink(chars, 'special'))
        elif 'title' == token:
            self.out.append('<span class="i">' +
                    cgi.escape(chars) + '</span>')
        elif 'note' == token:
            self.out.append('<span class="n">' +
                    cgi.escape(chars) + '</span>')
        elif 'ruler' == token:
            self.out.append('<span class="h">' + chars + '</span>')
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

