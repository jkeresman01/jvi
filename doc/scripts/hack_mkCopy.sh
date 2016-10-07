#!/usr/bin/bash

. VARS.sh

set -e

OUT=$HTDOCS_OUT

#echo $OUT
#echo junction \
#    $(cygpath -w $OUT/javahelpset) \
#    $(cygpath -w $jvi/nbvi/jvi-help/src/org/netbeans/modules/jvi/help/docs)
#exit


mkdir -p $OUT
cp -pr $HTDOCS/* $OUT


rm -f $OUT/javahelpset
#ln -s $jvi/nbvi/jvi-help/src/org/netbeans/modules/jvi/help/docs \
#      $OUT/javahelpset
junction \
    $(cygpath -w $OUT/javahelpset) \
    $(cygpath -w $jvi/nbvi/jvi-help/src/org/netbeans/modules/jvi/help/docs)


mkdir -p $OUT/vimhelp
cp -p $VIMHELP_OUT/* $OUT/vimhelp

