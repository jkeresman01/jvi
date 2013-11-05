#!/usr/bin/bash

. VARS.sh


OUT=$VIMHELP_OUT

# filter the documents, removing lines, before creating tags
#
$SCRIPTS/filter.sh $UNFILTERED $FILTERED

# vim -e -s << EOT
#     helptags $(cygpath -a -m ../vimhelp/)
#     q
# EOT
#
$VIM/vim -e -s -c "helptags $(cygpath -a -m $FILTERED)" -c q

mkdir -p $OUT
mv $FILTERED/tags $OUT/tags

python jvi.py $FILTERED $OUT
cp $VIM_CSS/*.css $OUT
