
DOC=../test
BUILD=../test

# vim -e -s << EOT
#     helptags $(cygpath -a -m ../vimhelp/)
#     q
# EOT
vim -e -s -c "helptags $(cygpath -a -m $DOC)" -c q

python jvi.py $DOC $BUILD
cp $DOC/../css/*.css $BUILD
