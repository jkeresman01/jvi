#!/usr/bin/bash

. VARS.sh

reportError() {
    rc=$?
    echo
    echo Error $rc from $BASH_COMMAND
}
trap reportError ERR


OUT=$VIMHELP_OUT

# filter the documents, removing lines, before creating tags
#
$SCRIPTS/filter.sh $UNFILTERED $FILTERED

$XVIM/vim --noplugin -e -s -V1 -c "helptags $(cygpath -a -m $FILTERED)" -c q

mkdir -p $OUT
mv $FILTERED/tags $OUT/tags

vimh $FILTERED $OUT
cp $VIM_CSS/*.css $OUT

