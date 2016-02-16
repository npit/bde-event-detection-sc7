#!/bin/bash
# ============================================================================= #
# use this script to build from source and generate directory for BDE modules
# execute from project folder
# ============================================================================= #
cd "$CURDIR"
[ "$#" -ne 2 ] && echo "Usage: $(basename $0): <MASTER POM DIRECTORY> <TARGET_DIRECTORY>: Provide path to pom.xml, and path to target directory to generate the server snapshot.
e.g. $(basename $0) ~/bde_source/ /opt/BDE" && exit 1
cd $1
CURDIR=`pwd`
POM="$CURDIR/pom.xml"
[ ! -f "$POM" ] && echo "$(basename $0): Error: no pom found in $CURDIR" && exit 1
# build
mvn clean dependency:copy-dependencies package

# create directory to put files
DEP="$2"
echo "deploying modules at $DEP ..."
mkdir $DEP && cd $DEP
for each in `find $1 -iname 'BDE*' -type d`; do
	echo $each
	fname="${each##*/}"
	echo $fname
	mkdir $fname && cd $fname
	cp $CURDIR/$fname/target/*.jar .
	mkdir lib
	cp $CURDIR/$fname/target/dependency/*.jar lib/
	cp -r $CURDIR/$fname/target/res .
	cd $DEP
done
echo "Done"
