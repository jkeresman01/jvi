
import os, sys
import re
import xml.etree.ElementTree as ET
from io import StringIO
import vimh_build as VB

#
# This module works with xml representation of the vim help files
#

RE_CLEANSPACE = re.compile(r'\s+')

###################################################################
###################################################################
###################################################################

#
# Conversion from xml to original txt
#

def get_txt(e):
    sb = StringIO()
    sb_get_txt(e, sb)
    return sb.getvalue()

def sb_get_txt(e, sb):
    tag = e.tag
    style = e.get('t')
    s = e.text

    if 'table' == tag:
        s = sb_get_txt_table(e, sb)
        return
    elif tag in ('nl', 'br'):
        # these have a '\n' as a tail
        pass
    elif 'pipe' == style:
        s = '|' + s + '|'
    elif 'star' == style:
        s = '*' + s + '*'
    elif 'header' == style:
        s = fix_line_ending(s, '~')
    elif 'graphic' == style:
        s = fix_line_ending(s, ' `')
    elif 'example' == style:
        # TODO: This isn't quite right.
        # TODO: Use of embedded '<nl/>' really makes this not right.
        #       Need to do something like: (assume s_text, s_tail)
        #               s = s + get_children_text(e)
        #               s = ' >' + s + '<'
        #               f_skip_children = True
        #       or maybe on entry (hmm, the 'if' probably not needed)
        #       looses the append to single string buffer model.
        #               s_children = ''
        #               if 'pre' == tag:
        #                       s_children = get_children_text(e)
        #           then in this 'example' case
        #               s = ' >' + s + s_children + '<'
        #               s_children = ''
        #               
        # wrap s in ">" and "<", the ">" goes at end of previous line
        # wonder about that final "<"
        s = ' >' + s + '<'
        pass
    sb.write(s)
    for i in e.getchildren():
        sb_get_txt(i, sb)
    sb.write(e.tail)

def get_txt_table(table):
    sb = StringIO()
    sb_get_txt_table(table, sb)
    return sb.getvalue()

def sb_get_txt_table(table, sb):
    assert 'table' == table.tag
    m = table.get('markup')
    if m is not None:
        sb.write('#*#' + m + '#*#\n')
    for tr in table:
        sb_get_txt_table_row(tr, sb)

def get_txt_table_row(tr):
    sb = StringIO()
    sb_get_txt_table_row(tr, sb)
    return sb.getvalue()

def sb_get_txt_table_row(tr, sb):
    ### print '  tr:'
    col_text = []
    for td in tr:
        col_text.append(get_txt(td).split('\n'))
    t = list(zip(*col_text))
    # NEEDSWORK: possible to get rid of "list(...)"?
    # maybe use map
    for i in range(len(t)):
        t[i] = ''.join(t[i])
    # # get rid of the line. it is there because split('\n') added it
    # if len(t[-1]) == 0: t = t[:-1]
    sb.write('\n'.join(t))

##
# append "end" to each line in "s".
# but not to empty lines at end of line list
def fix_line_ending(s, end):
    l = s.split('\n')
    started = False
    for i in reversed(range(len(l))):
        if len(l[i]) > 0:
            started = True
        if started:
            l[i] += end
    return '\n'.join(l)

###################################################################
###################################################################
###################################################################

#
# general xml and vim help xml manipulations
#

def get_content(e):
    sb = StringIO()
    sb_get_content(e, sb)
    return sb.getvalue()

def sb_get_content(e, sb):
    sb.write(e.text)
    for i in e.getchildren():
        sb_get_content(i, sb)
    sb.write(e.tail)


##
# Find column that has word as part of its markup description.
# @return index of column, zero based, by word, else -1 if not found
def find_table_column(table, label):
    col_idx = -1
    i = 0
    for x in table.vh_cols:
        if label in x:
            col_idx = i
            break
        i += 1
    return col_idx

##
# Historical turned into fix_whitespace
def remove_nl(td):
    cur_tail = None
    for e in td:
        if 'nl' == e.tag:
            t = re.sub('\n', ' ', e.tail)
            if cur_tail is None:
                td.text += t
            else:
                cur_tail.tail += t
        else:
            cur_tail = e
    for i in reversed(range(len(td))):
        if 'nl' == td[i].tag:
            del td[i]

##
# Remove <nl/> nodes recursively, <br/> nodes remain.
# Multiple whitespace in content are converted to single space,
def fix_whitespace(e):
    cur_tail = None
    for e01 in e:
        fix_whitespace(e01)
        if 'nl' == e01.tag:
            t = re.sub('\n', ' ', e01.tail)
            if cur_tail is None:
                e.text += t
            else:
                cur_tail.tail += t
        else:
            cur_tail = e01
    for i in reversed(range(len(e))):
        if 'nl' == e[i].tag:
            del e[i]

    if e.text != '': e.text = RE_CLEANSPACE.sub(' ', e.text)
    if e.tail != '': e.tail = RE_CLEANSPACE.sub(' ', e.tail)

##
# Convert some <nl/> to <br/> in some table columns for text which
# acts like entry headers, like command name and pipe target.
# Two types of table fixups: 'ref' and 'index'.
#
# index: with 'tag' and 'command' columns
#       combine the tag and command columns. the command becomes a
#       <link> whose target is taken from tag
#       TODO: the tag column is deleted, so markup info doesn't match
#             columns, clean it up (delete and add flag to table?)
#
# ref:
#       - 'command' column (genererally first column)
#         one or more lines give the command text
#       - 'desc' column
#         May start with one or more lines of <anchor>s.
#         NOTE: A new column is inserted, first column, from the anchors.
def fix_vim_table_columns(table):
    if 'index' == table.get('form'):
        fix_table_index(table)
        pass
    elif 'ref' == table.get('form'):
        fix_table_ref(table)

def fix_table_index(table):
    tag_idx = find_table_column(table, 'tag')
    command_idx = find_table_column(table, 'command')
    print('FIXUP TABLE: index', (table.get('label'), tag_idx, command_idx))
    if tag_idx < 0 or command_idx < 0:
        return
    new_col = None
    for tr in table:
        link = tr[tag_idx][0] if len(tr[tag_idx]) > 0 else None
        ### print 'fix_table_index 01:', (tr[tag_idx][:],)
        ### XS.dump_table_row_elements(tr)
        cmd = tr[command_idx]
        if link is None or link.tag != 'link' or link.get('t', '') != 'pipe':
            print('fix_table_index 02: NOT LINK-PIPE: ', (get_txt(tr),))
            pass
        else:
            # modify the link
            ### print 'fix_table_index 03:', get_content(cmd)
            link.set('linkto', link.text)
            link.text = get_content(cmd).strip()
            link.tail = ''
            ### print 'fix_table_index 04:'
            # plug the link into the command column, and remove old text
            cmd[:] = (link,)
            cmd.text = ''

        del tr[tag_idx]
        # TODO: fix markup info to reflect deleted table column

        ### print 'fix_table_index 09:'
        ### XS.dump_table_row_elements(tr)

def fix_table_ref(table):
    command_idx = find_table_column(table, 'command')
    desc_idx = find_table_column(table, 'desc')
    extra_or_idx = find_table_column(table, 'extra-or')
    print('FIXUP TABLE: ref', (table.get('label'), command_idx, desc_idx))
    if command_idx >= 0:
        for tr in table:
            fix_table_ref_command(tr[command_idx])
    if desc_idx >= 0:
        for tr in table:
            td_anchor = fix_table_ref_desc(tr[desc_idx])
            if extra_or_idx >= 0:
                del tr[extra_or_idx]
            tr.insert(0, td_anchor)

def fix_table_ref_command(td):
    t = td.text
    for e in td:
        if 'nl' == e.tag:
            if not t.isspace():
                e.tag = 'br'
            t = ''
        t += get_txt(e)

def fix_table_ref_desc(td):
    t = td.text
    saw_anchor = False
    any_anchor = False
    anchors = []
    for e in td:
        if 'anchor' == e.tag:
            saw_anchor = True
            any_anchor = True
            t += e.tail
        elif 'nl' == e.tag:
            if saw_anchor:
                e.tag = 'br'
                saw_anchor = False
            t = e.tail
        else:
            t += get_txt(e)
        if len(t) > 0 and not t.isspace():
            if 'br' == e.tag:
                anchors.append(e)
            break
        anchors.append(e)
    if not any_anchor and len(anchors) > 0:
        print('===== INPUT FILE PROBLEM =====')
        edump(td)

    td_anchor = VB.make_elem('td')
    if any_anchor and len(anchors) > 0:
        # split node; put the anchors into their own element
        for e in anchors:
            td.remove(e)
        td_anchor[:] += anchors
        td.text += anchors[-1].tail
        anchors[-1].tail = ''
    return td_anchor


###################################################################
###################################################################
###################################################################

#
# debug output
#

##
# prettyprint dump an element
def edump(x, l = 0):
    f_closed = _edump1(x, l)
    l1 = l + 1
    _idump(x.text,l1)
    for i in x.getchildren():
        edump(i,l1)
    if not f_closed:
        _edump1(x, l1, True)
    _idump(x.tail,l)

def _idump(s, l):
    if s: print(' ' * (l*2) + s)

def _edump1(e, l, closetag=False):
    if not ET.iselement(e):
        return
    f_closed = False
    if closetag:
        _idump('</'+e.tag + '>', l-1)
    else:
        f_closed = (e.text is None or e.text == '') and len(e) == 0
        end = '/>' if f_closed else '>'
        s1 = '<'+e.tag
        s2 = ' '.join([k + '="' + v + '"' for (k,v) in e.items()])
        if s2: s1 += ' ' + s2
        s1 += end
        _idump(s1, l)
    return f_closed

def dump_table(table):
    print('table:')
    for tr in table:
        dump_table_row(tr)

def dump_table_row(tr):
    dump_table_row_elements(tr)

    return

    print('  tr:')
    for td in tr:
        text = get_content(td)
        text2 = get_txt(td)
        l = [x.get('t') for x in td]
        s = set(l)
        print('    tda1: "%s"' % (re.sub('\n', r'\\n', text),))
        # print '    tda2: "%s"' % (re.sub('\n', r'\\n', text2),)
        # print '      :', text.split('\n')
        print('    td:', [ x.strip() for x in text.split('\n') ])
        print('      :', s, l)

def dump_element(e):
    text = re.sub('\n', r'\\n', e.text)
    tail = re.sub('\n', r'\\n', e.tail)
    print('      <%s>"%s" : "%s" ' % (e.tag, text, tail))

def dump_table_row_elements(tr):
    print('  tre:')
    for td in tr:
        dump_table_row_data_elements(td)

def dump_table_row_data_elements(td):
    text = re.sub('\n', r'\\n', td.text)
    tail = re.sub('\n', r'\\n', td.tail)
    print('    td: "%s" : "%s"' % (text, tail))
    for d in td:
        dump_element(d)

def dump_table_ascii(table):
    print('table: =====')
    for tr in table:
        dump_table_row_ascii(tr)
    print('end-table: =====')

def dump_table_row_ascii(tr):
    ### print '  tr:'
    col_text = []
    for td in tr:
        # col_text.append(get_content(td).split('\n'))
        col_text.append(get_txt(td).split('\n'))
    t = list(zip(*col_text))
    # NEEDSWORK: ditto. possible to get rid of "list(...)"?
    # maybe use map
    for i in range(len(t)):
        t[i] = ''.join(t[i])
    # get rid of the line. it is there because split('\n') added it
    if len(t[-1]) == 0: t = t[:-1]
    for s in t:
        if len(s) == 0: s = '---'
        print(s)


###################################################################
###################################################################
###################################################################

#
# commands to generate output from the xml files

OUTPUT_FORMATS = ('txt', 'html', 'flex', 'tables')

def usage():
    print(( sys.argv[0]
            + ' (' + '|'.join(OUTPUT_FORMATS) + ') input_dir [output_dir]'))
    exit(1)

def gen_tables(xml):
    out = VB.make_vimhelp_tree(xml.getroot().get('filename'))
    root = out.getroot()
    tables = [ table for table in xml.findall('table') ]
    for table in tables:
        table.tail = '\n'
        ### edump(table)
        root.append(table)
    return out

def fix_tables(xml):
    for table in xml.findall('table'):
        fix_vim_table_columns(table)
        fix_whitespace(table)

def runit():
    global INPUT_DIR, OUTPUT_DIR

    if len(sys.argv) < 3:
        print('must be at least two arguments')
        usage()

    # if output_dir not present, use input_dir
    # tags and any .txt files in input_dir are processed

    output_format = sys.argv[1]
    if output_format not in OUTPUT_FORMATS:
        print('unknown output format:', output_format)
        usage()

    INPUT_DIR = sys.argv[2]
    if INPUT_DIR[-1] != '/':
        INPUT_DIR = INPUT_DIR + '/'
    if len(sys.argv) > 3:
        OUTPUT_DIR = sys.argv[3]
        if OUTPUT_DIR[-1] != '/':
            OUTPUT_DIR = OUTPUT_DIR + '/'
        try: os.mkdir(OUTPUT_DIR)
        except: pass
    else:
        OUTPUT_DIR = INPUT_DIR

    print('input dir:', INPUT_DIR, 'output dir:', OUTPUT_DIR)

    xmlfiles = [ x for x in os.listdir(INPUT_DIR) if x.endswith('.txt.xml') ]

    print('xmlfiles:', xmlfiles)


    for xmlfile in xmlfiles:
        print('PROCESSING:', xmlfile)
        xml = VB.read_xml_file(INPUT_DIR + xmlfile)

        if 'txt' == output_format:
            txt = VG.get_txt(xml.getroot())

            with open(OUTPUT_DIR + xmlfile + '.' + output_format, 'w') as f:
                f.write(txt)
        elif 'tables' == output_format:
            fix_tables(xml)
            out = gen_tables(xml)
            with open(OUTPUT_DIR + xmlfile + '.' + output_format, 'w') as f:
                out.write(f)
        else:
            print('not handled: "%s"' % (output_format,))
            exit(1)

#####
##### FOR PRETTY PRINTING
##### from xml.dom.minidom import parse, parseString
#####


if __name__ == "__main__":
    runit()

