import xml.etree.ElementTree as ET
from StringIO import StringIO

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

def elem_text(e):
    sb = StringIO()
    internal_elem_text(e, sb)
    return sb.getvalue()

def internal_elem_text(e, sb):
    sb.write(e.text)
    for i in e.getchildren():
        internal_elem_text(i, sb)
    sb.write(e.tail)

def dump_table(table):
    print 'table:'
    for tr in table:
        dump_table_row(tr)

def dump_table_row(tr):
    print '  tr:'
    for td in tr:
        text = elem_text(td)
        l = [x.get('t') for x in td]
        s = set(l)
        # print '    td:', re.sub('\n', r'\\n', text)
        # print '      :', text.split('\n')
        print '    td:', [ x.strip() for x in text.split('\n') ]
        # print '      :', s, l

def dump_table_ascii(table):
    print 'table: ====='
    for tr in table:
        dump_table_row_ascii(tr)
    print 'end-table: ====='

def dump_table_row_ascii(tr):
    ### print '  tr:'
    col_text = []
    for td in tr:
        col_text.append(elem_text(td).split('\n'))
    t = zip(*col_text)
    for i in range(len(t)):
        t[i] = ''.join(t[i])
    #print '\n'.join(t)
    if len(t[-1]) == 0: t = t[:-1]
    for s in t:
        if len(s) == 0: s = '---'
        print s

