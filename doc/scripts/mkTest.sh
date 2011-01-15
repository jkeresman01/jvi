#!/usr/bin/bash

DOC=../test
BUILD=../test/build

# vim -e -s << EOT
#     helptags $(cygpath -a -m ../vimhelp/)
#     q
# EOT
vim -e -s -c "helptags $(cygpath -a -m $DOC)" -c q
mv $DOC/tags $BUILD/tags

#python jvi.py $DOC $BUILD
echo python vimh_xml.py $DOC $BUILD
python vimh_xml.py $DOC $BUILD
cp $DOC/../css/*.css $BUILD
