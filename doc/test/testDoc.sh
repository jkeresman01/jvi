#!/usr/bin/bash

tname=$1
test_type=html

. test_common

python $jvi/jvi/doc/scripts/jvi/vimh/jvi.py $FILTERED $OUT

mv $OUT/tags $OUT/golden-tags

diff -r -q -l $tdir/golden $tdir/build
rc=$?
if ((rc)); then
    echo FAIL: $tname
    exit 1
fi

