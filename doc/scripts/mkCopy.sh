#!/usr/bin/bash

. VARS.sh

set -e

OUT=$HTDOCS_OUT

mkdir -p $OUT
cp -pr $HTDOCS/* $OUT

rm $OUT/javahelpset
ln -s $jvi/nbvi/javahelp/org/netbeans/modules/jvi/docs \
      $OUT/javahelpset

mkdir -p $OUT/vimhelp
cp -p $VIMHELP_OUT/* $OUT/vimhelp

