#!/usr/bin/bash

CMD=${0##*/}
DIR=${0%/*}

# srcdir is a nb project basedir with
# either a projdir/build or projdir/target directory

# like $jvi/nbvi/jvi-lib
srcdir=$1
# proj (in a dir under $jvi/rel)
dstdir=$2

if [[ $# -ne 2 || ( -z "$srcdir" || -z "$dstdir" ) ]]; then
    echo "Must specify a source and destination"
    exit 1
fi

Main() {

    local -i is_mvn=0

    cp_mvn() {
        echo "do mvn stuff"
        cd $dstdir
        cp -pr $FILES .
        jar -xf $FILES Info/info.xml
        mv Info/info.xml updates.xml
        rmdir Info
    }

    cp_ant() {
        echo "do ant stuff"
        cp -pr $FILES $dstdir
    }

    # detect a maven project if $srcdir/target is a directory
    if [ -d $srcdir/target ]; then
        FILES="$srcdir/target/*.nbm"
        is_mvn=1
    else
        # must be ant based, find a directory with both updates.xml
        # and licenses
        updx=$(find $srcdir/build -name updates.xml)
        updx=${updx%/*}
        FILES="$updx/updates.xml $updx/*.nbm $updx/licenses"
        is_mvn=0
    fi

    #FILES="$srcdir/updates.xml $srcdir/*.nbm $srcdir/licenses"
    for i in $FILES
    do
        if [[ ! -e $i ]]
        then
            echo not found: $i
            echo ABORT: $CMD
            exit 1
        fi
    done

    ls -ld $FILES

    set -e

    mkdir $dstdir

    if ((is_mvn)); then
        cp_mvn
    else
        cp_ant
    fi
}

Main
