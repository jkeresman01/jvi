#!/usr/bin/bash

BASE=..

UNFILTERED=$BASE/vimhelp

FILTERED=$BASE/build/filtered
OUT=$BASE/build/xml

# filter the documents, removing lines, before creating tags
#
./filter.sh $UNFILTERED $FILTERED

vim -e -s -c "helptags $(cygpath -a -m $FILTERED)" -c q

mkdir -p $OUT
mv $FILTERED/tags $OUT/tags

echo python vimh_xml.py $FILTERED $OUT
python vimh_xml.py $FILTERED $OUT
cp $BASE/css/*.css $OUT
