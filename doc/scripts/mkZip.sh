#!/usr/bin/bash

UC=build-uc

if [[ ! -d $UC ]]
then
    echo not found: $UC
    echo ABORT: $(basename $0)
    exit 1
fi

set -e

name=$(basename $(pwd))

echo $name

mkdir $name
cp $UC/* $name

jar -Mcf $name.zip $name

rm $name/*.nbm
rm $name/*
rmdir $name

### /pf/WinZip/WINZIP32.EXE -min -a -r -p \
###     $(cygpath -m $(pwd)/$name.zip) $name

# /pf/WinZip/WINZIP32.EXE -min -a \
#     $(cygpath -m /a/src/jvi-dev/rel/nbvi-1.4.1.x2/foobar.zip) *
