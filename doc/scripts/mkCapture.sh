#!/usr/bin/bash

CMD=${0##*/}
DIR=${0%/*}

srcdir=$1
dstdir=$2

FILES="$srcdir/updates.xml $srcdir/*.nbm $srcdir/licenses"
for i in $FILES
do
    if [[ ! -e $i ]]
    then
        echo not found: $i
        echo ABORT: $CMD
        exit 1
    fi
done

set -e

mkdir $dstdir

cp -pr $FILES $dstdir

