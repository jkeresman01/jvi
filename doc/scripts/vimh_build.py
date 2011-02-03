import re
import cgi
import xml.etree.ElementTree as ET
import urllib
import vimh_scan as vs
import vimh_gen as VG
import xml_sub as XS

# accept tokens from vim help scanner
#
# A builder is created with a set of tags.
# It puts stuff together per file
#

# The following are the tokens recognized by the vim scanner.
# They are grouped by either paragraphs or words, where paragraphs
# are groups of lines.
#
# Note: there is a link type of 'hidden', much like a token
#

SET_PRE     = set(('header',
                   'ruler',
                   'graphic',
                   'section',
                   'title',
                   'example'))

SET_WORD    = set(('pipe',
                   'star',
                   'opt',
                   'ctrl',
                   'special',
                   'note',
                   'url',
                   'word',
                   'chars'))

SET_NL      = set(('newline',
                   'blankline'))

SET_CONTROL = set(('markup',
                   'start_line',
                   'start_file'))

SET_OTHER   = set(('eof'))

TY_PRE     = 1
TY_WORD    = 2
TY_EOL     = 3
TY_EOF     = 4
TY_CONTROL = 6

MAP_TY = {}
MAP_TY.update(zip(SET_PRE,     (TY_PRE,)     * len(SET_PRE)))
MAP_TY.update(zip(SET_WORD,    (TY_WORD,)    * len(SET_WORD)))
MAP_TY.update(zip(SET_NL,      (TY_EOL,)     * len(SET_NL)))
MAP_TY.update(zip(SET_CONTROL, (TY_CONTROL,) * len(SET_CONTROL)))
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

    def _start_file(self, filename):
        self.filename = filename
        self.out = [ ]

    def _start_line(self, input_line, lnum):
        """The next line to be parsed, generally for debug/diagnostics"""
        self.input_line = input_line
        self.lnum = lnum

    def _markup(self, markup):
        pass

    def _put_token(self, token_data):
        """token_data is (token, chars, col)."""
        token, chars, col = token_data
        if 'markup' == token: self._markup(chars)
        elif 'start_file' == token: self._start_file(chars)
        elif 'start_line' == token: self._start_line(chars, col)

    def process(self, data):
        print '      first entry:', data[0]
        print 'number of entries:', len(data)
        print '       last entry:', data[-1]

        while len(data) > 0:
            ### print data[0]
            self._put_token(data.popleft())

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
#       <vimhelp> ::= [ <p> | <pre> | <table> ]+
# and these are made up of leaf elements and character data
#       element   ::= <target> | <link> | <em>
#
# Every element can have a 't' attribute (type). For <pre> they are
# one of SET_PRE. For the leaf elements they are from SET_WORD.
# Note that 'word' and 'chars' is typically just text/cdata.
#
# All '\n' that are encountered are copied into <p> and <pre> elements. And for
# <table> elements, each column gets a '\n' copied into it. This is done so
# that the original text *and* intent can be recreated from the xml
# representation.  For example, when expressing a table, multiple words on
# the same line and in the same column, may be handled together.
#
# <p> elements
#       These elements group words and chars, including <em> elements from
#       SET_WORD.  There is typically no 't' attribute associated with a <p>.
#       A <p> is terminated by a blank line; but note that any number of
#       trailing \n are copied into the <p>'s text.
#
# <pre> elements
#       These elements are typically scanned as complete lines.
#       Multiple <pre> of the same 't' (type) are combined into a single <pre>
#       element.
#
# <table> elements are introduced by markup such as:
#       #*# table:form=index:id=xxx 1:tag 17:command 33:opt:note 36:desc #*#
#       #*# table 1 17 33 36 #*#
#   - where the first word must be table and any with it anything like
#     id=xxx becomes an attribute on the table. The form attribute affect
#     the parsing of a table
#          form_attr ::= index  // an index of commands, the first and second
#                               // columns may be combined into a single column
#                               // when an output table is generated. The
#                               // "command" column is displayed as a
#                               // link to "tag" column's target.
#                      | ref    // typically holds descriptions for a commands.
#                               // Each description is the target of a link.
#                      | simple // just a table
#   - the rest of the groups are the columns. The first item in the group
#     is the column where the table starts. The rest of the items usually
#     include a table column description.
#   - a form=ref table markup typically looks like:
#               #*#table:ref 1:command 2:extra-or 25:desc #*#
#     The column with 'extra-or' markup catches areas of a line that are
#     typically blank. Sometimes that area has the word 'or' which indicates
#     that the line after is a continuation. Sometimes it has text
#     which is example or special details of usage.
#
#
# TODO: - build in a more nested/recursive fashion. vim help files have
#         nested tables. See insert.txt for example i_CTRL-R or i_CTRL-X_CTRL-P.
# TODO: - Add stop-table and unkown markup as comments,
#         need a better concept of current know (building in that recursive...)
#


def make_elem(elem_tag, style = None, chars = '', parent = None):
    if isinstance(style, str):
        style = {'t':style}
    elif style is None:
        style = {}
    ### print 'make_elem', elem_tag, style, chars, parent
    e = ET.Element(elem_tag, style)
    e.text = chars
    e.tail = ''
    if parent is not None:
        parent.append(e)
    return e

def make_sub_elem(parent, elem_tag, style = None, chars = ''):
    return make_elem(elem_tag, style, chars, parent)


class XmlLinks(Links):

    def do_add_tag(self, filename, vim_tag):
        link = super(XmlLinks, self).do_add_tag(filename, vim_tag)
        ### print 'do_add_tag:', vim_tag, link.filename, link.style

    def maplink(self, vim_tag, style = None):
        link = self[vim_tag]
        ### print "maplink-1: '%s' '%s' '%s'" % (vim_tag, link, style)
        if link is not None:
            ### print "maplink-1a: '%s' '%s' '%s'" % (vim_tag, link.__dict__, style)
            # this is a known link from the tags file
            if style and style != link.style and style != 'pipe':
                print 'LINK STYLE MISMATCH'
            # this is weird logic, since MISMATCH is never printed,
            # seems the idea is that, use link.style unless 'link' is
            # argument, in which case make it a 'pipe'
            style = 'pipe' if 'pipe' == style else link.style
            style = {'t':style, 'filename':link.filename}
            elem_tag = 'link'
        elif style is not None:
            # not a known link, but a style was specified
            elem_tag = 'em'
        else:
            # not known link, no class specifed
            return vim_tag
        ### print "maplink-2: '%s' '%s' '%s'" % (vim_tag, elem_tag, style)
        return make_elem(elem_tag, style, vim_tag)

class VimHelpBuildXml(VimHelpBuildBase):

    def __init__(self, tags):
        build_link_re_from_pat()
        self.links = XmlLinks(tags)
        self._init_table_ops()


    def get_output(self):
        return self.tree

    def _start_file(self, filename):
        super(VimHelpBuildXml, self)._start_file(filename)

        self.blank_lines = 0
        self.root = ET.Element('vimhelp')
        self.tree = ET.ElementTree(self.root)
        self.cur_elem = None
        self.cur_table = None
        self.after_blank_line = False

        self.root.set('filename', filename)

    def _start_line(self, line, lnum):
        super(VimHelpBuildXml, self)._start_line(line, lnum)
        ### print 'start_line:', self.lnum, self.input_line

    def _markup(self, markup):
        handled = self.check_table_markup(markup)

        if not handled:
            self.error('UNKNOWN MARKUP COMMAND ' + cmd)
        pass

    def _put_token(self, token_data):
        """token_data is (token, chars, col)."""
        token, chars, col = token_data
        if token in ('markup', 'start_file', 'start_line'):
            super(VimHelpBuildXml, self)._put_token(token_data)
            return
        ty = MAP_TY[token]
        ### print 'token_data:', ty, token_data

        if self.cur_table is not None:
            ret = self.check_stop_table(ty, token_data)
            if ret:
                return

        if ty == TY_EOL:
            if token == 'blankline':
                self.after_blank_line = True
            self.add_stuff('\n', token_data)
        elif ty == TY_PRE:
            self.add_para(token, chars)
        elif ty == TY_EOF:
            pass
        else:
            if self.after_blank_line:
                self.cur_elem = None
                self.after_blank_line = False
            if token == 'chars':
                w = chars
            elif token == 'word':
                # may end up mapped to a 'link' or 'em'
                w = self.links.maplink(chars)
            elif token == 'pipe':
                w = self.links.maplink(chars, 'pipe')
            elif token == 'star':
                w = make_elem('target', token, chars)
            elif token in ('opt', 'ctrl', 'special'):
                w = self.links.maplink(chars, token)
            else:
                w = make_elem('em', token, chars)

            self.add_stuff(w, token_data)

    ##
    # Add paragraphs and contents of a particular type.
    # Consecutive stuff of the same token type are put into the
    # same paragraph.
    def add_para(self, token, chars):
        e = self.get_cur_elem('pre', token)
        self.do_add_stuff(chars, e)

    def add_stuff(self, stuff, token_data):
        if self.cur_table is not None:
            self.add_to_table(stuff, token_data)
            return

        # newlines can be added to any type of element
        e = self.cur_elem if TY_EOL == MAP_TY[token_data[0]] else None

        self.do_add_stuff(stuff, e)

    def do_add_stuff(self, stuff, e = None):
        """Add plain text or an element to current paragraph."""
        if '\n' == stuff:
            stuff = make_elem('nl')
            stuff.tail = '\n'
        if e is None:
            e = self.get_cur_elem()
        if ET.iselement(stuff):
            e.append(stuff)
            return
        if len(e) == 0:
            e.text += stuff
        else:
            e[-1].tail += stuff

    ##
    # Get the current element of the specified tag-style.
    # If the current element doesn't match then create a new one.
    # 
    # @return the current elemenent
    def get_cur_elem(self, elem_tag = 'p', style = None):
        if self.cur_elem is not None:
            if self.cur_elem.tag == elem_tag \
                    and self.cur_elem.get('t') == style:
                return self.cur_elem
            self.cur_elem = None
        e = make_sub_elem(self.root, elem_tag, style)
        self.cur_elem = e
        return e

    #
    # Table Handling
    #

    def _init_table_ops(self):
        self.DEFAULT_TABLE_OPS = (self.check_stop_table_simple,
                                  self.check_start_table_row_simple)
        self.TABLE_OPS = { 'simple' : self.DEFAULT_TABLE_OPS,
                           'index'  : self.DEFAULT_TABLE_OPS,
                           'ref'    : (self.check_stop_table_ref,
                                       self.check_start_table_row_ref)
                         }

    def check_table_markup(self, markup):
        t = parse_table_markup(markup)
        t01 = t[0]
        # t02 has a list for each column
        t02 = t[1:]

        if 'table' != t01[0]:
            return False
        if 'stop-table' in t01:
            self.check_stop_table(TY_CONTROL, ('markup', 'stop-table', 0))
            return True

        self.cur_elem = None
        table = self.get_cur_elem('table')
        self.cur_elem = None
        self.cur_table = table

        table.set('markup', markup)
        table.v_markup = t01
        table.v_cols = t02
        for k,v in [ x.split('=') for x in t01[1:] if x.find('=') >= 0 ]:
            table.set(k, v)
        form = table.get('form', '')
        self.t_ops = self.TABLE_OPS.get(form, self.DEFAULT_TABLE_OPS)

        self.t_data = []
        self.t_ref_table_checked_idx = -1
        self.t_ref_table_extra_or_col = VG.find_table_column(table, 'extra-or')
        return True

    def check_stop_table(self, ty, token_data):
        if self.cur_table is None:
            return False

        finish_table, consume_token = self.t_ops[0](ty, token_data)

        if finish_table:
            self.build_table()
            VG.fix_vim_table_columns(self.cur_table)
            XS.dump_table(self.cur_table)
            XS.dump_table_ascii(self.cur_table)
            #print VG.get_txt(self.cur_table),
            self.cur_table = None
            self.t_data = None
        return consume_token

    def add_to_table(self, w, token_data):
        self.t_data.append((token_data[0], w, token_data[2]))

    # used for both form in {index, simple}
    def check_stop_table_simple(self, ty, token_data):
        finish_table = False
        if ty in (TY_PRE, TY_EOF):
            finish_table = True
        elif token_data[0] == 'blankline':
            finish_table = True
        return (finish_table, False)

    def check_start_table_row_simple(self, idx):
        # token starts in column zero
        # and either token is not a string (i.e. it is an element)
        #            or token is not all blanks
        token, stuff, pos = self.t_data[idx]
        return (pos == 0
                and (not isinstance(stuff, str) or not stuff.isspace()))

    def check_stop_table_ref(self, ty, token_data):
        finish_table = False
        consume_token = False
        if ty == TY_EOF:
            finish_table = True
        elif 'ruler' == token_data[0]:
            finish_table = True
        elif 'markup' == token_data[0] and 'stop-table' == token_data[1]:
            finish_table, consume_token = (True, True)
        return (finish_table, consume_token)

    ##
    # reference table entry starts with <target t="star"> somewhere in the line.
    # Check till EOL or EOF. If there's an extra-or column, then check
    # previous input line for 'or', if present then this line is continuation.
    def check_start_table_row_ref(self, idx):
        if idx <= self.t_ref_table_checked_idx:
            return False
        new_entry_ok = True
        if self.t_ref_table_extra_or_col >= 0 and self.cur_table_row is not None:
            tr = self.cur_table_row
            l = VG.get_content(tr[self.t_ref_table_extra_or_col]).split('\n')
            if len(l) > 1 and 'or' == l[-2].strip():
                # advance past this line, will never return true
                new_entry_ok = False
        new_entry = False
        while True:
            token, stuff, pos = self.t_data[idx]
            ty = MAP_TY[token]
            if ty in (TY_EOL, TY_EOF):
                break;
            if (new_entry_ok
                    and ET.iselement(stuff) and stuff.tag == 'target'
                    and stuff.get('t') == 'star'):
                new_entry = True
            idx += 1
        self.t_ref_table_checked_idx = idx
        if not new_entry:
            return False
        return True

    def build_table(self):
        cpos = [ x[0]-1 for x in self.cur_table.v_cols]
        print 'XXX', cpos

        tr = None
        self.cur_table_row = tr
        # use an index into t_data, since may need to do lookahead
        for idx in xrange(len(self.t_data)):
            token, stuff, pos = self.t_data[idx]
            if self.t_ops[1](idx) or tr is None:
                if tr is not None:
                    self.cur_table.append(tr)
                tr = make_elem('tr')
                td = [ make_sub_elem(tr, 'td') for x in xrange(len(cpos))]
                self.cur_table_row = tr
            if MAP_TY[token] == TY_EOL:
                for x in td:
                    self.do_add_stuff('\n', x)
            else:
                col = len(cpos) - 1 # assume words in last col
                for i in xrange(len(cpos) - 1):
                    if cpos[i] <= pos < cpos[i+1]:
                        col = i
                        break
                self.do_add_stuff(stuff, td[col])
        if tr is not None: self.cur_table.append(tr)


##
# @return list of lists, first list for table, remaining are list per column
def parse_table_markup(markup):
    markup = markup.strip()
    l = markup.split(None,1)
    cmd = l[0]
    t01 = [ cmd.split(':') ]
    if len(l) > 1:
        column_info = l[1]
        # convert info to list of list items: int-col , 'arg2', 'arg3', ...
        t02 = [x.split(':') for x in  column_info.split()]
        t02 = [ [int(x[0]),] + x[1:] for x in t02 ]
        t01 += t02
    print 'markup:', (markup, t01)
    return t01


###################################################################
###################################################################
###################################################################

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

    def _put_token(self, token_data):
        """token_data is (type, chars, col)."""
        token, chars, col = token_data
        if token in ('markup', 'start_file', 'start_line'):
            super(VimHelpBuildHtml, self)._put_token(token_data)
            return
        ###print token_data
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
            ### print self.filename + ': section: "' + chars +'"'
        elif 'chars' == token:
            if not chars.isspace():
                ###print '"%s" %s:"%s" NOT ISSPACE' \
                ###        % (chars,self.filename, self.input_line)
                ### the only non-space I've seen is blanks followed by a double-quote
                pass
            self.out.append(cgi.escape(chars))
        elif token in ('newline', 'blankline'):
            self.out.append('\n')
        elif 'eof' == token:
            pass
        else: print 'ERROR: unknown token "' + token + '"'

