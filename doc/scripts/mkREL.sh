#!/usr/bin/bash

set -e

#. VARS.sh
UC_MIRROR=/z/jvi/frs/jVi-for-NetBeans
SCRIPTS_DIR=$jd

JVI_RELDIR=/a/src/jvi-dev/rel

#JVI_VERSION=nbvi-1.4.1.x3
JVI_VERSION=$(basename $(pwd))

echo $JVI_VERSION

if [[ ! -d proj ]]
then
    echo not found: proj
    echo ABORT: $(basename $0)
    exit 1
fi

JVI_VERSIONDIR=$JVI_RELDIR/$JVI_VERSION

OUT=$JVI_VERSIONDIR/build-uc

GUTS=$OUT/cat-guts
CATALOG=$OUT/catalog.xml

NB_VERSION=NetBeans-7.0

JVI_MAIN=$JVI_VERSIONDIR/proj/updates.xml
JVI_UC=         # early access update center must be manually entered
UC_DIR=eaUC
JVI_ADD="
    $JVI_RELDIR/editor.pin-1.3.2/proj/updates.xml
    "

mkdir -p $OUT
# Turn several modules into update center. $GUTS is the catalog.
python $SCRIPTS_DIR/combine_uc.py $JVI_MAIN $JVI_UC $JVI_ADD $OUT

echo '<?xml version="1.0" encoding="UTF-8"?>' > $CATALOG
echo '<!DOCTYPE module_updates PUBLIC "-//NetBeans//DTD Autoupdate Catalog 2.6//EN" "http://www.netbeans.org/dtds/autoupdate-catalog-2_6.dtd">' >> $CATALOG

cat $GUTS >> $CATALOG
rm $GUTS

# keep both files around since there is an update center out there
# that references the non-gz version
gzip -9 -c $CATALOG > $CATALOG.gz


# don't abort on error since the cmp might fail
set +e

SAVE_DIR=$(pwd)
cd $OUT

UC=$UC_MIRROR/$NB_VERSION/$UC_DIR

echo ===== $UC =====

FILES=

for i in *
do
    echo ===== $i
    arg=""
    if [[ $i = ${CATALOG##*/}.gz ]]
    then
        # ignore the date 
        arg=-i8
    fi
    if ! cmp $arg $i $UC/$i > /dev/null
    then
        echo "      ^ DIFFERS ^"
        FILES="$FILES $i"
    fi
done

cd $SAVE_DIR

rm -rf new-files
mkdir new-files

for i in $FILES
do
    cp $OUT/$i new-files
done
