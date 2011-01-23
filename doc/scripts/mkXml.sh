#!/usr/bin/bash

DOC=../vimhelp
BUILD=../build/xml

vim -e -s -c "helptags $(cygpath -a -m $DOC)" -c q
mv $DOC/tags $BUILD

echo python vimh_xml.py $DOC $BUILD
python vimh_xml.py $DOC $BUILD
#cp $DOC/../css/*.css $BUILD
