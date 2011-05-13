#!/usr/bin/bash

. VARS.sh

set -e

OUT=$HTDOCS_OUT

mkdir -p $OUT
cp -pr $HTDOCS/* $OUT

rm -f $OUT/javahelpset
ln -s $jvi/nbvi/jvi-help/src/org/netbeans/modules/jvi/help/docs \
      $OUT/javahelpset

mkdir -p $OUT/vimhelp
cp -p $VIMHELP_OUT/* $OUT/vimhelp

