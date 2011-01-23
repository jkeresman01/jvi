# scan vim documentation, output tokens

import re
from vimh_build import VimHelpBuildBase

VIM_FAQ_LINE = '<a href="vim_faq.txt.html#vim_faq.txt" class="l">' \
               'vim_faq.txt</a>   Frequently Asked Questions\n'

PAT_HEADER   = r'(?P<header>^.*~$)'
PAT_GRAPHIC  = r'(?P<graphic>^.* `$)'
PAT_PIPEWORD = r'(?P<pipe>(?<!\\)\|[#-)!+-~]+\|)'
PAT_STARWORD = r'(?P<star>\*[#-)!+-~]+\*(?:(?=\s)|$))'
PAT_OPTWORD  = r"(?P<opt>'(?:[a-z]{2,}|t_..)')"
PAT_CTRL     = r'(?P<ctrl>CTRL-(?:W_)?(?:[\w\[\]^+-<>=@]|<[A-Za-z]+?>)?)'
PAT_SPECIAL  = r'(?P<special><.*?>|\{.*?}|' + \
               r'\[(?:range|line|count|offset|\+?cmd|[-+]?num|\+\+opt|' + \
               r'arg|arg(?:uments)|ident|addr|group)]|' + \
               r'(?<=\s)\[[-a-z^A-Z0-9_]{2,}])'
PAT_TITLE    = r'(?P<title>Vim version [0-9.a-z]+|VIM REFERENCE.*)'
PAT_NOTE     = r'(?P<note>N(ote|OTE)[sS]?:?)'
PAT_URL      = r'(?P<url>(?:https?|ftp)://[^\'"<> \t]+[a-zA-Z0-9/])'
PAT_WORD     = r'(?P<word>[!#-)+-{}~]+)'

def build_re_from_pat():
    global RE_TAGWORD
    RE_TAGWORD = re.compile(
            PAT_HEADER   + '|' + \
            PAT_GRAPHIC  + '|' + \
            PAT_PIPEWORD + '|' + \
            PAT_STARWORD + '|' + \
            PAT_OPTWORD  + '|' + \
            PAT_CTRL     + '|' + \
            PAT_SPECIAL  + '|' + \
            PAT_TITLE    + '|' + \
            PAT_NOTE     + '|' + \
            PAT_URL      + '|' + \
            PAT_WORD)

RE_NEWLINE    = re.compile(r'[\r\n]')
RE_HRULE      = re.compile(r'[-=]{3,}.*[-=]{3,3}$')
RE_EG_START   = re.compile(r'(?:.* )?>$')
RE_EG_END     = re.compile(r'\S')
RE_SECTION    = re.compile(r'[-A-Z .][-A-Z0-9 .()]*(?=\s+\*)')
RE_STARTAG    = re.compile(r'\s\*([^ \t|]+)\*(?:\s|$)')
RE_LOCAL_ADD  = re.compile(r'LOCAL ADDITIONS:\s+\*local-additions\*$')

# special markup directions in capture group
# Either '#+#some markup#+#' or '#*#some other markup #*#'
# if the #*# form is used then the line on which it appears is deleted
RE_MARKUP     = re.compile(r'#(\+|\*)#(.*?)#\1#')
# delete chars within a line
RE_DEL_CHARS  = re.compile(r'#-#.*?#-#')

# following match at beginning of line
STR_SKIP       = 'DOC-DEL'
STR_START_SKIP = 'START-DOC-DEL'
STR_STOP_SKIP  = 'STOP-DOC-DEL'

def _trim_token(token_data):
    """Take out extra characters from the token."""
    token, chars, col = token_data
    if 'pipe' == token or 'star' == token:
        chars = chars[1:-1]
    elif 'header' == token:
        chars = chars[:-1]
    elif 'graphic' == token:
        chars = chars[:-2]
    else:
        return token_data
    return (token, chars, col)

class DataWrapper(object):
    def __init__(self, data):
        self.data = data

    def append(self, d):
        print d
        self.data.append(d)

class VimHelpScanner:

    def __init__(self):
        build_re_from_pat()

    def parse(self, filename, contents, _data, include_faq = True):

        #data = DataWrapper(_data)
        data = _data
        data.append(('start_file', filename, 0))

        lnum = 0
        inskip = 0
        inexample = 0
        faq_line = False
        for line in contents:
            lnum += 1

            # Skip lines as directed
            if line.startswith(STR_SKIP): continue
            if line.startswith(STR_STOP_SKIP):
                inskip = 0
                continue
            if inskip or line.startswith(STR_START_SKIP):
                inskip = 1
                continue

            line = line.rstrip('\r\n')
            line_tabs = line
            line = line.expandtabs()

            data.append(('start_line', line, lnum))

            # handle custom markup
            delete_line = False
            pos = 0
            while True:
                m = RE_MARKUP.search(line, pos)
                if m:
                    data.append(('markup', m.group(2), 0))
                    if m.group(1) == '*':
                        delete_line = True
                    pos = m.start()
                    line = line[:pos] + line[m.end():]
                else: break;
            if delete_line:
                # print 'markup delete line'
                continue
            # handle line fragment deletion
            line = RE_DEL_CHARS.sub('', line)

            if len(line) == 0 or line.isspace():
                data.append(('blankline', '', -1));
                continue

            if RE_HRULE.match(line):
                data.append(('ruler', line, 0))
                data.append(('newline', '', -1));
                continue

            col_offset = 0
            if inexample == 2:
                if RE_EG_END.match(line):
                    inexample = 0
                    if line[0] == '<':
                        line = line[1:]
                        col_offset = 1
                else:
                    data.append(('example', line, 0))
                    data.append(('newline', '', -1));
                    continue
            if RE_EG_START.match(line_tabs):
                inexample = 1
                line = line[0:-1]

            if RE_SECTION.match(line_tabs):
                m = RE_SECTION.match(line)
                data.append(
                        ('section', m.group(), m.start() + col_offset))
                line = line[m.end():]
                col_offset += m.end()

            if filename == 'help.txt' and RE_LOCAL_ADD.match(line_tabs):
                faq_line = True

            lastpos = 0
            for match in RE_TAGWORD.finditer(line):
                pos = match.start()
                if pos > lastpos:
                    data.append(
                            ('chars', line[lastpos:pos], lastpos + col_offset))
                lastpos = match.end()
                #print 'match:', (match.lastgroup, match.group(match.lastgroup))
                data.append(_trim_token((match.lastgroup,
                                        match.group(), pos + col_offset)))
            if lastpos < len(line):
                data.append(
                        ('chars', line[lastpos:], lastpos + col_offset))
            data.append(('newline', '', -1));
            if inexample == 1: inexample = 2
            if faq_line and include_faq:
                out.append(VIM_FAQ_LINE)
                faq_line = False
        data.append(('eof', '', -1))

