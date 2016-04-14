#!/bin/bash
# [ "$#" -ne 1 ] && echo "Usage: $(basename $0): <PROPERTIES FILE PATH>" && exit 1
CP=""
for sFile in `find * -iname '*.jar'`; do CP="$CP:$sFile"; done
properties=$1
log="./bde_clustering_demo.log"
java -cp $CP gr.demokritos.iit.clustering.exec.DemoBDEEventDetection $properties &> $log
