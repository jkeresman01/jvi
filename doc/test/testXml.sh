#!/usr/bin/bash

tname=$1
test_type=xml

. test_common

python $jvi/jvi/doc/scripts/jvi/vimh/vimh_xml.py $FILTERED $OUT

# diff -r -q -l $tdir/golden $tdir/build
# rc=$?
# if ((rc)); then
#     echo FAIL: $tname
#     exit 1
# fi

