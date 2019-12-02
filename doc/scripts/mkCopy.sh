#!/usr/bin/bash

. VARS.sh

set -e

OUT=$HTDOCS_OUT

mkdir -p $OUT
cp -pr $HTDOCS/* $OUT

rm -f $OUT/vis-block-help
symlink_dir $VIS_BLOCK_HELP \
            $OUT/vis-block-help

mkdir -p $OUT/vimhelp
cp -p $VIMHELP_OUT/* $OUT/vimhelp

