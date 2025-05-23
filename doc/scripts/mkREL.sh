#!/usr/bin/bash

if [[ ! -d proj ]]
then
    echo not found: proj
    echo ABORT: $(basename $0)
    exit 1
fi

set -e

#JVI_VERSION=nbvi-1.4.1.x3
JVI_VERSION=$(basename $(pwd))
echo Createing update center $JVI_VERSION

JVI_RELDIR=$jvi/rel
UC_MIRROR=/z/jvi/frs/jVi-for-NetBeans

JVI_VERSIONDIR=$JVI_RELDIR/$JVI_VERSION

NB_VERSION=NetBeans-12_JDK-11

JVI_MAIN=$JVI_VERSIONDIR/proj/updates.xml
JVI_UC=         # pick up from plugin portal update center

#UC_DIR=eaUC
UC_DIR=UC

JVI_ADD="
    $JVI_RELDIR/editor.pin/editor.pin-2.0/proj/updates.xml
    $JVI_RELDIR/jvi-nb-windows/jvi-nb-windows-1.0.1/proj/updates.xml
    $JVI_RELDIR/jvi-nb-lib/jvi-nb-lib-1.0.3/proj/updates.xml
    $JVI_RELDIR/yenta/api-1.2-SNAPSHOT/proj/updates.xml
    "

OUT=$JVI_VERSIONDIR/build-uc

GUTS=$OUT/cat-guts
CATALOG=$OUT/catalog.xml

mkdir -p $OUT
# Turn several modules into update center. $GUTS is the catalog.
combine_uc $JVI_MAIN $JVI_UC $JVI_ADD $OUT

echo '<?xml version="1.0" encoding="UTF-8"?>' > $CATALOG
echo '<!DOCTYPE module_updates PUBLIC "-//NetBeans//DTD Autoupdate Catalog 2.8//EN" "https://netbeans.apache.org/dtds/autoupdate-catalog-2_8.dtd">' >> $CATALOG
cat $GUTS >> $CATALOG
rm $GUTS

# keep both files around since there is an update center out there
# that references the non-gz version
gzip -9 -c $CATALOG > $CATALOG.gz


SAVE_DIR=$(pwd)
cd $OUT

UC=$UC_MIRROR/$NB_VERSION/$UC_DIR

echo ========= $UC =========

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

CHANGED_FILES_DIR=changed-files

rm -rf $CHANGED_FILES_DIR
mkdir $CHANGED_FILES_DIR

for i in $FILES
do
    cp $OUT/$i $CHANGED_FILES_DIR
done

