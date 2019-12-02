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

# vim -e -s << EOT
#     helptags $(cygpath -a -m ../vimhelp/)
#     q
# EOT
#

$VIM/vim --noplugin -e -s -V1 -c "helptags $(cygpath -a -m $FILTERED)" -c q

# echo vim helptags returns: $?


mkdir -p $OUT
mv $FILTERED/tags $OUT/tags

python jvi.py $FILTERED $OUT
cp $VIM_CSS/*.css $OUT

