#!/usr/bin/bash

. VARS.sh

set -e

OUT=$HTDOCS_OUT

mkdir -p $OUT
cp -pr $HTDOCS/* $OUT

rm -rf $OUT/vis-block-help
mkdir $OUT/vis-block-help
cp $VIS_BLOCK_HELP/* $OUT/vis-block-help


mkdir -p $OUT/vimhelp
cp -p $VIMHELP_OUT/* $OUT/vimhelp

