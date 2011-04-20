#!/usr/bin/bash

BASE=..

HTDOCS=$BASE/htdocs

VIMHELP_OUT=$BASE/build/txt_html
HTDOCS_OUT=$BASE/build/htdocs


OUT=$HTDOCS_OUT

mkdir -p $OUT
cp -pr $HTDOCS/* $OUT
mkdir -p $OUT/vimhelp
cp -p $VIMHELP_OUT/* $OUT/vimhelp

