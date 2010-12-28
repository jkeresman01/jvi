# parses vim documentation

import sys
import re
import cgi
import urllib

VIM_FAQ_LINE = '<a href="vim_faq.txt.html#vim_faq.txt" class="l">' \
               'vim_faq.txt</a>   Frequently Asked Questions\n'

RE_TAGLINE = re.compile(r'(\S+)\s+(\S+)')

PAT_HEADER   = r'(?P<header>^.*~$)'
PAT_GRAPHIC  = r'(?P<graphic>^.* `$)'
PAT_PIPEWORD = r'(?P<pipe>(?<!\\)\|[#-)!+-~]+\|)'
PAT_STARWORD = r'(?P<star>\*[#-)!+-~]+\*(?:(?=\s)|$))'
PAT_OPTWORD  = r"(?P<opt>'(?:[a-z]{2,}|t_..)')"
PAT_CTRL     = r'(?P<ctrl>CTRL-(?:W_)?(?:[\w\[\]^+-<>=@]|<[A-Za-z]+?>)?)'
PAT_SPECIAL  = r'(?P<special><.*?>|\{.*?}|' + \
               r'\[(?:range|line|count|offset|\+?cmd|[-+]?num|\+\+opt|' + \
               r'arg|arg(?:uments)|ident|addr|group)]|' + \
               r'\s\[[-a-z^A-Z0-9_]{2,}])'
PAT_TITLE    = r'(?P<title>Vim version [0-9.a-z]+|VIM REFERENCE.*)'
PAT_NOTE     = r'(?P<note>Notes?:?)'
PAT_URL      = r'(?P<url>(?:https?|ftp)://[^\'"<> \t]+[a-zA-Z0-9/])'
PAT_WORD     = r'(?P<word>[!#-)+-{}~]+)'
RE_LINKWORD = re.compile(
        PAT_OPTWORD  + '|' + \
        PAT_CTRL     + '|' + \
        PAT_SPECIAL)
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
RE_NEWLINE   = re.compile(r'[\r\n]')
RE_HRULE     = re.compile(r'[-=]{3,}.*[-=]{3,3}$')
RE_EG_START  = re.compile(r'(?:.* )?>$')
RE_EG_END    = re.compile(r'\S')
RE_SECTION   = re.compile(r'[-A-Z .][-A-Z0-9 .()]*(?=\s+\*)')
RE_STARTAG   = re.compile(r'\s\*([^ \t|]+)\*(?:\s|$)')
RE_LOCAL_ADD = re.compile(r'LOCAL ADDITIONS:\s+\*local-additions\*$')

STR_DEL       = 'DOC-DEL'
STR_START_DEL = 'START-DOC-DEL'
STR_STOP_DEL  = 'STOP-DOC-DEL'

class Link:
    def __init__(self, link_pipe, link_plain):
        self.link_pipe = link_pipe
        self.link_plain = link_plain

class VimHelpParser:
    urls = { }

    def __init__(self, tags):
        for line in tags:
            m = RE_TAGLINE.match(line)
            if m:
                tag, filename = m.group(1, 2)
                self.do_add_tag(filename, tag)

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
        self.urls[tag] = Link(link_pipe, link_plain)

    def maplink(self, tag, css_class = None):
        links = self.urls.get(tag)
        if links is not None:
            if css_class == 'l': return links.link_pipe
            else: return links.link_plain
        elif css_class is not None:
            return '<span class="' + css_class + '">' + cgi.escape(tag) + \
                    '</span>'
        else: return cgi.escape(tag)

    def parse(self, filename, contents, include_faq = True):

        out = [ ]

        inskip = 0
        inexample = 0
        faq_line = False
        for line in contents:
            if line.startswith(STR_DEL): continue
            if line.startswith(STR_STOP_DEL):
                inskip = 0
                continue
            if inskip or line.startswith(STR_START_DEL):
                inskip = 1
                continue
            line = line.rstrip('\r\n')
            line_tabs = line
            line = line.expandtabs()
            if RE_HRULE.match(line):
                out.append('<span class="h">' + line + '</span>\n')
                continue
            if inexample == 2:
                if RE_EG_END.match(line):
                    inexample = 0
                    if line[0] == '<': line = line[1:]
                else:
                    out.append('<span class="e">' + cgi.escape(line) +
                            '</span>\n')
                    continue
            if RE_EG_START.match(line_tabs):
                inexample = 1
                line = line[0:-1]
            if RE_SECTION.match(line_tabs):
                m = RE_SECTION.match(line)
                out.append(m.expand(r'<span class="c">\g<0></span>'))
                line = line[m.end():]
            if filename == 'help.txt' and RE_LOCAL_ADD.match(line_tabs):
                faq_line = True
            lastpos = 0
            for match in RE_TAGWORD.finditer(line):
                pos = match.start()
                if pos > lastpos:
                    out.append(cgi.escape(line[lastpos:pos]))
                lastpos = match.end()
                #print 'match:', match.lastgroup, match.group(match.lastgroup)
                pipeword, starword, opt, ctrl, special, title, note, \
                        header, graphic, url, word = \
                        match.group('pipe', 'star', 'opt', 'ctrl',
                        'special', 'title', 'note', 'header', 'graphic', 'url', 'word')
                if pipeword is not None:
                    out.append(self.maplink(pipeword[1:-1], 'l'))
                elif starword is not None:
                    tag = starword[1:-1]
                    out.append('<a name="' + urllib.quote_plus(tag) +
                            '" class="t">' + cgi.escape(tag) + '</a>')
                elif opt is not None:
                    out.append(self.maplink(opt, 'o'))
                elif ctrl is not None:
                    out.append(self.maplink(ctrl, 'k'))
                elif special is not None:
                    out.append(self.maplink(special, 's'))
                elif title is not None:
                    out.append('<span class="i">' +
                            cgi.escape(title) + '</span>')
                elif note is not None:
                    out.append('<span class="n">' +
                            cgi.escape(note) + '</span>')
                elif header is not None:
                    out.append('<span class="h">' +
                            cgi.escape(header[:-1]) + '</span>')
                elif graphic is not None:
                    out.append(cgi.escape(graphic[:-2]))
                elif url is not None:
                    out.append('<a class="u" href="' + url + '">' +
                            cgi.escape(url) + '</a>')
                elif word is not None:
                    out.append(self.maplink(word))
            if lastpos < len(line):
                out.append(cgi.escape(line[lastpos:]))
            out.append('\n')
            if inexample == 1: inexample = 2
            if faq_line:
                out.append(VIM_FAQ_LINE)
                faq_line = False

        return out
