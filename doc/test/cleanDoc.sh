#!/usr/bin/bash

set -e

testdir=$jvi/jvi/doc/test

if [[ ! -d $testdir ]]; then
    echo "'$testdir' does not exist"
    exit 1
fi

cd $testdir

rm -rf links/build index/build full/build
