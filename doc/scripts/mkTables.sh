#!/usr/bin/bash

. VARS.sh


# in IN_XML files are like foo.txt.xml

IN_XML=$BASE/build/xml
OUT_XML=$BASE/build/tables/xml
HTML=$BASE/build/tables/html

XSL=$BASE/css/tables.xsl

python vimh_gen.py tables $IN_XML $OUT_XML

for f in $OUT_XML/*.txt.xml.tables
do
    bn=${f##*/}
    bn=${bn%.xml.tables}
    echo xslt $f $HTML/$bn.html
    xslt $f $XSL $HTML/$bn.html
done

cp $VIM_CSS/*.css $HTML
