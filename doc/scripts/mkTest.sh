#!/usr/bin/bash

DOC=../test
BUILD=../test/build

vim -e -s -c "helptags $(cygpath -a -m $DOC)" -c q
mv $DOC/tags $BUILD/tags

##### for original html testing
echo python jvi.py $DOC $BUILD
python jvi.py $DOC $BUILD

##### for xml texting
#echo python vimh_xml.py $DOC $BUILD
#python vimh_xml.py $DOC $BUILD

cp $DOC/../css/*.css $BUILD
