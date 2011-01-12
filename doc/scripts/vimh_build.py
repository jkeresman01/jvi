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

SET_PARA  = set(('header',
                 'ruler',
                 'graphic',
                 'section',
                 'title',
                 'example'))

SET_WORD  = set(('pipe',
                 'star',
                 'opt',
                 'ctrl',
                 'special',
                 'note',
                 'url',
                 'word',
                 'chars'))

SET_NL    = set(('newline',
                 'blankline'))

SET_OTHER = set(('eof'))

TY_PARA = 1
TY_WORD = 2
TY_NL   = 3
TY_EOF  = 4

MAP_TY = {}
MAP_TY.update(zip(SET_PARA, (TY_PARA,) * len(SET_PARA)))
MAP_TY.update(zip(SET_WORD, (TY_WORD,) * len(SET_WORD)))
MAP_TY.update(zip(SET_NL,   (TY_NL,)   * len(SET_NL)))
MAP_TY['eof'] = TY_EOF

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

    def start_line(self, lnum, input_line):
        """The next line to be parsed, generally for debug/diagnostics"""
        self.input_line = input_line
        self.lnum = lnum

    def markup(self, markup):
        pass

    def put_token(self, token_data):
        """token_data is (token, chars, col)."""
        #print token_data
        pass

    def get_output(self):
        return self.out

    def error(self, info):
        print "%s at %s:%d '%s'" \
                % (info, self.filename, self.lnum, self.input_line)

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
                vim_tag, filename = m.group(1, 2)
                self.do_add_tag(filename, vim_tag)

    def do_add_tag(self, filename, vim_tag):
        # determine style for a plain link
        style = 'hidden'
        m = RE_LINKWORD.match(vim_tag)
        if m:
            # style to one of: opt, ctrl, special
            style = m.lastgroup
        link = Link(filename)
        link.style = style
        self[vim_tag] = link
        return link

    def maplink(self, vim_tag, style = None):
        link = self[vim_tag]
        if link is not None:
            # this is a known link from the tags file
            if style and style != link.style and style != 'pipe':
                print 'LINK STYLE MISMATCH'
            pass
        elif style is not None:
            # not a known link, but a style was specified
            pass
        else:
            # not know link, no class specifed, just return it
            return vim_tag

#
# XML builder
#
# <vimhelp> is the root, the schema looks a bit like
#       <vimhelp> ::= [ <p> | <table> ]+
# and these are made up of leaf elements and character data
#       element   ::= <target> | <link> | <em>
#
# Every element can have a "t" attribute (type). For paragraphs they are
# one of SET_PARA. For the leaf elements they are from SET_WORD.
# But note that 'word' and 'chars' is typically just text.
#
# TODO: put this inside VimHelpBUildXml,
#       attempting that results in:
#               link = super(XmlLinks, self).do_add_tag(filename, vim_tag)
#           NameError: global name 'XmlLinks' is not defined

class XmlLinks(Links):

    def do_add_tag(self, filename, vim_tag):
        link = super(XmlLinks, self).do_add_tag(filename, vim_tag)
        print 'do_add_tag:', vim_tag, link.filename, link.style

    def maplink(self, vim_tag, style = None):
        link = self[vim_tag]
        if link is not None:
            # this is a known link from the tags file
            if style and style != link.style and style != 'pipe':
                print 'LINK STYLE MISMATCH'
            style = {'t':style, 'filename':link.filename}
            elem_tag = 'link'
        elif style is not None:
            # not a known link, but a style was specified
            elem_tag = 'em'
        else:
            # not known link, no class specifed
            return vim_tag
        print "maplink: '%s' '%s' '%s'" % (vim_tag, elem_tag, style)
        return VimHelpBuildXml.make_elem(elem_tag, style, vim_tag)

class VimHelpBuildXml(VimHelpBuildBase):

    def __init__(self, tags):
        build_link_re_from_pat()
        self.links = XmlLinks(tags)
        self.blank_lines = 0
        self.root = ET.Element('vimhelp')
        self.tree = ET.ElementTree(self.root)
        self.cur_elem = None
        self.in_table = False

    def start_file(self, filename):
        super(VimHelpBuildXml, self).start_file(filename)
        self.root.set('filename', filename)

    def get_output(self):
        return self.tree

    def start_line(self, lnum, line):
        super(VimHelpBuildXml, self).start_line(lnum, line)
        # print 'start_line:', self.lnum, self.input_line

    def markup(self, markup):
        markup = markup.strip()
        print 'markup:', markup
        cmd,rest = markup.split(None,1)
        started = False
        if cmd.find('table') >= 0:
            started = self.check_start_table(cmd, rest)

        if not started:
            self.error('UNKNOWN MARKUP COMMAND ' + cmd)
        pass

    def put_token(self, token_data):
        """token_data is (token, chars, col)."""
        token, chars, col = token_data
        ty = MAP_TY[token]
        # print 'token_data:', ty, token_data

        if self.in_table:
            ret = self.check_stop_table(ty, token_data)
            if ret:
                return

        if ty == TY_NL:
            if token == 'blankline':
                self.blank_lines += 1
                print 'BLANK_LINE'
            else:
                self.add_stuff('\n', token_data)

        elif ty == TY_PARA:
            self.add_para(token, chars)
        elif ty == TY_EOF:
            pass
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

            self.add_stuff(w, token_data)

    def add_para(self, token, chars):
        self.fixup_blank_lines()
        e = self.cur_elem
        if e is not None and (e.tag != 'p' or e.get('t', '') != token):
            # done with current paragraph
            self.cur_elem = None
        if self.cur_elem is None:
            e = self.get_cur_elem()
            e.set('t', token)
        self.do_add_stuff(chars)

    def add_stuff(self, stuff, token_data):
        if self.in_table:
            self.add_table(stuff, token_data)
            return
        self.fixup_blank_lines()
        self.do_add_stuff(stuff)

    def do_add_stuff(self, stuff, e = None):
        """Add plain text or an element to current paragraph."""
        if e is None:
            e = self.get_cur_elem()
        if ET.iselement(stuff):
            e.append(stuff)
            return
        if len(e) == 0:
            e.text += stuff
        else:
            e[-1].tail += stuff

    @staticmethod
    def make_elem(elem_tag, style = {}, chars = '', parent = None):
        if isinstance(style, str):
            style = {'t':style}
        e = ET.Element(elem_tag, style)
        e.text = chars
        e.tail = ''
        if parent is not None:
            parent.append(e)
        return e

    @staticmethod
    def make_sub_elem(parent, elem_tag, style = {}, chars = ''):
        return VimHelpBuildXml.make_elem(elem_tag, style, chars, parent)

    def get_cur_elem(self, elem_tag = None):
        if self.cur_elem is not None and elem_tag is not None \
                and self.cur_elem.tag != elem_tag:
            self.cur_elem = None
        if self.cur_elem is not None:
            return self.cur_elem
        if not elem_tag: elem_tag = 'p'
        e = self.make_sub_elem(self.root, elem_tag)
        # e = ET.SubElement(self.root, elem_tag)
        # e.text = ''
        # e.tail = ''
        self.cur_elem = e
        return e

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

    def check_start_table(self, cmd, column_info):
        t = cmd.split(':')
        if 'table' != t[0]:
            return False
        self.t_args = t
        self.t_data = []

        # convert info to list of list items: col# , 'arg2', 'arg3', ...
        t = [x.split(':') for x in  column_info.split()]
        self.t_cols = [ [int(x[0]),] + x[1:] for x in t ]

        self.fixup_blank_lines()
        self.cur_elem = None
        self.get_cur_elem('table')
        self.in_table = True
        return True

    def check_stop_table(self, ty, token_data):
        if ty == TY_PARA:
            self.build_table()
            return True
        if ty == TY_EOF:
            self.build_table()
            return True
        if token_data[0] == 'blankline':
            self.build_table()
            return True
        return False

    def add_table(self, w, token_data):
        self.t_data.append((token_data[0], w, token_data[2]))

    def build_table(self):
        cpos = [ x[0]-1 for x in self.t_cols]
        print 'XXX', cpos

        tr = None
        for token, stuff, pos in self.t_data:
            #print 'YYY', (token, stuff, pos)
            if pos == 0 and (not isinstance(stuff, str) or not stuff.isspace())\
                    or tr is None:
                if tr: self.cur_elem.append(tr)
                tr = self.make_elem('tr')
                td = [ self.make_sub_elem(tr, 'td') for x in xrange(len(cpos))]
            col = len(cpos) - 1 # assume words in last col
            if MAP_TY[token] == TY_NL:
                for x in td:
                    self.do_add_stuff('\n', x)
            else:
                for i in xrange(len(cpos) - 1):
                    if cpos[i] <= pos < cpos[i+1]:
                        col = i
                #print 'ZZZ', (col, stuff, td[col])
                self.do_add_stuff(stuff, td[col])
        if tr: self.cur_elem.append(tr)
        ET.dump(self.cur_elem)
        self.cur_elem = None
        self.in_table = False
        self.t_data = None

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

    def get_tag(self, vim_tag):
        """Lazily create link_plain and link_pipe."""
        link = self[vim_tag]
        if not link : return None
        if not hasattr(link, 'link_plain'):
            part1 = '<a href="' + link.filename + '.html#' + \
                    urllib.quote_plus(vim_tag) + '"'
            part2 = '>' + cgi.escape(vim_tag) + '</a>'
            link.link_pipe = part1 \
                    + ' class="' + self.styles['link'] + '"' + part2
            link.link_plain = part1 \
                    + ' class="' + self.styles[link.style] + '"' + part2
        return link

    def maplink(self, vim_tag, css_class = None):
        link = self.get_tag(vim_tag)
        if link is not None:
            # this is a known link from the tags file
            if css_class == 'link':
                # drop the anchor if foo.txt and foo.txt.html#foo.txt
                if vim_tag.endswith('.txt') \
                        and link.link_pipe.find(
                                '"' + vim_tag + '.html#' + vim_tag + '"') >= 0:
                    return link.link_pipe.replace('#' + vim_tag, '');
                return link.link_pipe
            else: return link.link_plain
        elif css_class is not None:
            # not a known link, but a class was specified
            return '<span class="' + self.styles[css_class] \
                    + '">' + cgi.escape(vim_tag) + '</span>'
        else:
            # not know link, no class specifed, just return it
            return cgi.escape(vim_tag)


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
            vim_tag = chars
            self.out.append('<a name="' + urllib.quote_plus(vim_tag) +
                    '" class="t">' + cgi.escape(vim_tag) + '</a>')
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
        elif token in ('newline', 'blankline'):
            self.out.append('\n')
        elif 'eof' == token:
            pass
        else: print 'ERROR: unknown token "' + token + '"'

