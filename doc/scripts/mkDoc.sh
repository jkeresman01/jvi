#!/usr/bin/bash

BASE=..

UNFILTERED=$BASE/vimhelp

FILTERED=$BASE/build/filtered
OUT=$BASE/build/txt_html

# filter the documents, removing lines, before creating tags
#
./filter.sh $UNFILTERED $FILTERED

# vim -e -s << EOT
#     helptags $(cygpath -a -m ../vimhelp/)
#     q
# EOT
#
vim -e -s -c "helptags $(cygpath -a -m $FILTERED)" -c q

mkdir -p $OUT
mv $FILTERED/tags $OUT/tags

python jvi.py $FILTERED $OUT
cp $BASE/css/*.css $OUT
