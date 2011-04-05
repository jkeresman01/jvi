#!/usr/bin/bash

if [[ $# -ne 2 ]]
then
    echo filter.sh needs exactly two args
    exit 1
fi

src=$1
dst=$2
echo $src $dst

mkdir -p $dst

for i in $src/*txt
do
    base=${i##*/}
    echo $i $dst/$base
    awk '
        /^START-DOC-DEL/,/^STOP-DOC-DEL/ {next}
        /^DOC-DEL/ {next}
        {print}
    ' < $i > $dst/$base
done

