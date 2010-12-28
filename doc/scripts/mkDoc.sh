
DOC=../vimhelp
BUILD=../build

# vim -e -s << EOT
#     helptags $(cygpath -a -m ../vimhelp/)
#     q
# EOT
vim -e -s -c "helptags $(cygpath -a -m ../vimhelp/)" -c q

python jvi.py $DOC $BUILD
cp $DOC/../css/*.css $BUILD
