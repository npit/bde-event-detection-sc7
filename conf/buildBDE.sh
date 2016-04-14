#!/bin/bash
# ============================================================================= #
# use this script to build from source and generate directory for BDE modules
# execute from project folder
# ============================================================================= #
[ "$#" -ne 2 ] && echo "Usage: $(basename $0): <MASTER POM DIRECTORY> <TARGET_DIRECTORY>: Provide path to pom.xml, and path to target directory to generate the server snapshot.
e.g. $(basename $0) ~/bde_source/ /opt/BDE" && exit 1
CURDIR=`pwd`
cd "$CURDIR"
POM="$CURDIR/pom.xml"
[ ! -f "$POM" ] && echo "$(basename $0): Error: no pom found in $CURDIR" && exit 1
# build
mvn clean dependency:copy-dependencies package

# create directory to put files
DEP="$2"
echo "deploying modules at $DEP ..."
mkdir $DEP && cd $DEP
for each in `find $CURDIR -iname 'BDE*' -type d`; do
	echo $each
	fname="${each##*/}"
	echo $fname
	mkdir $fname && cd $fname
	# echo "currently in `pwd`"
	cp $CURDIR/$fname/target/*.jar .
	mkdir lib
	cp $CURDIR/$fname/target/dependency/*.jar lib/
	mkdir res
	cp -r $CURDIR/$fname/target/res/* res/
	if [ "$(ls -A res/*.sh)" ]; then
		mv res/*.sh .
		chmod +x *.sh
	fi
	cd $DEP
done
echo "Done"
