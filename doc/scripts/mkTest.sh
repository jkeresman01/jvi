#!/usr/bin/bash

DOC=../test
BUILD=../test/build

vim -e -s -c "helptags $(cygpath -a -m $DOC)" -c q
mv $DOC/tags $BUILD/tags

echo python vimh_xml.py $DOC $BUILD
python vimh_xml.py $DOC $BUILD
cp $DOC/../css/*.css $BUILD
