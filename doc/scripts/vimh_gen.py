import re
import xml.etree.ElementTree as ET
from StringIO import StringIO
import xml_sub as XS

#
# This module works with xml representation of the vim help files
#

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
    t = zip(*col_text)
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
    for i in reversed(xrange(len(l))):
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


##
# Find column that has word as part of its markup description.
# @return index of column, zero based, by word, else -1 if not found
def find_table_column(table, label):
    col_idx = -1
    i = 0
    for x in table.v_cols:
        if label in x:
            col_idx = i
            break
        i += 1
    return col_idx

##
# Remove direct 'nl' children nodes from param element.
# '\n' in content are converted to spaces,
# otherwise param content is unchanged.
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
    for i in reversed(xrange(len(td))):
        if 'nl' == td[i].tag:
            del td[i]

##
# Convert some <nl/> to <br/> in some table columns for text which
# acts like entry headers, like command name and pipe target.
# Two types of table fixups: 'ref' and 'index'.
# index: with 'tag' and 'command' columns
#       combine the tag and command columns. the command becomes a
#       <link> whose target is taken from tag
#       TODO: the tag column is deleted, so markup info doesn't match
#             columns, clean it up (delete and add flag to table?)
# ref:
#       - 'command' column (genererally first column)
#         one or more lines give the command text
#       - 'desc' column
#         may start with one or more lines of <target>s
def fix_vim_table_columns(table):
    if 'index' == table.get('form'):
        fix_table_index(table)
    elif 'ref' == table.get('form'):
        fix_table_ref(table)

def fix_table_index(table):
    tag_idx = find_table_column(table, 'tag')
    command_idx = find_table_column(table, 'command')
    ### print 'fix_table_index', (tag_idx, command_idx)
    if tag_idx < 0 or command_idx < 0:
        return
    new_col = None
    for tr in table:
        link = tr[tag_idx][0] if len(tr[tag_idx]) > 0 else None
        ### print 'fix_table_index 01:', (tr[tag_idx][:],)
        ### XS.dump_table_row_elements(tr)
        cmd = tr[command_idx]
        if link is None or link.tag != 'link' or link.get('t', '') != 'pipe':
            print 'fix_table_index 02: NOT LINK-PIPE: ', (get_txt(tr),)
            pass
        else:
            # modify the link
            ### print 'fix_table_index 03:', get_content(cmd)
            link.set('linkto', link.text)
            link.text = get_content(cmd).strip()
            link.tail = ''
            ### print 'fix_table_index 04:'
            # plug the link into the command column
            cmd[:] = (link,)

        if True:
            del tr[tag_idx]
        else:
            # clear tag column
            tr[tag_idx].clear()
            tr[tag_idx].text = ''
            tr[tag_idx].tail = ''

        for td in tr:
            remove_nl(td)
        ### print 'fix_table_index 09:'
        ### XS.dump_table_row_elements(tr)

def fix_table_ref(table):
    idx = find_table_column(table, 'command')
    print 'FIXUP: ref command col', idx
    if idx >= 0:
        for tr in table:
            fix_table_ref_command(tr[idx])
    idx = find_table_column(table, 'desc')
    print 'FIXUP: ref desc col', idx
    if idx >= 0:
        for tr in table:
            fix_table_ref_desc(tr[idx])

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
    saw_target = False
    for e in td:
        if 'target' == e.tag:
            saw_target = True
            t += e.tail
        elif 'nl' == e.tag:
            if saw_target:
                e.tag = 'br'
                saw_target = False
            t = e.tail
        else:
            t += get_txt(e)
        if len(t) > 0 and not t.isspace():
            break


def get_content(e):
    sb = StringIO()
    sb_get_content(e, sb)
    return sb.getvalue()

def sb_get_content(e, sb):
    # tag = e.tag
    # style = e.get('t')
    s = e.text

    # if 'table' == tag:
    #     # s = sb_get_content_table(e, sb)
    #     s = ' GET_CONTENT TABLE '
    #     return
    sb.write(s)
    for i in e.getchildren():
        sb_get_content(i, sb)
    sb.write(e.tail)


###################################################################
###################################################################
###################################################################

#
# debug assistance
#

def dump_table(table):
    print 'table:'
    for tr in table:
        dump_table_row(tr)

def dump_table_row(tr):
    dump_table_row_elements(tr)
    return
    print '  tr:'
    for td in tr:
        text = get_content(td)
        text2 = get_txt(td)
        l = [x.get('t') for x in td]
        s = set(l)
        print '    tda1: "%s"' % (re.sub('\n', r'\\n', text),)
        # print '    tda2: "%s"' % (re.sub('\n', r'\\n', text2),)
        # print '      :', text.split('\n')
        print '    td:', [ x.strip() for x in text.split('\n') ]
        print '      :', s, l

def dump_element(e):
    text = re.sub('\n', r'\\n', e.text)
    tail = re.sub('\n', r'\\n', e.tail)
    print '      <%s>"%s" : "%s" ' % (e.tag, text, tail)

def dump_table_row_elements(tr):
    print '  tre:'
    for td in tr:
        dump_table_row_data_elements(td)

def dump_table_row_data_elements(td):
    text = re.sub('\n', r'\\n', td.text)
    tail = re.sub('\n', r'\\n', td.tail)
    print '    td: "%s" : "%s"' % (text, tail)
    for d in td:
        dump_element(d)

def dump_table_ascii(table):
    print 'table: ====='
    for tr in table:
        dump_table_row_ascii(tr)
    print 'end-table: ====='

def dump_table_row_ascii(tr):
    ### print '  tr:'
    col_text = []
    for td in tr:
        # col_text.append(get_content(td).split('\n'))
        col_text.append(get_txt(td).split('\n'))
    t = zip(*col_text)
    for i in range(len(t)):
        t[i] = ''.join(t[i])
    # get rid of the line. it is there because split('\n') added it
    if len(t[-1]) == 0: t = t[:-1]
    for s in t:
        if len(s) == 0: s = '---'
        print s
