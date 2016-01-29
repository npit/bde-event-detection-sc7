#!/bin/bash
[ "$#" -ne 1 ] && echo "Usage: $(basename $0): <OPERATION: monitor | search | stream>" && exit 1
CP=""
for sFile in `find * -iname '*.jar'`; do CP="$CP:$sFile"; done
operation=$1
java -cp $CP gr.demokritos.iit.crawlers.twitter.CrawlScript --operation $operation --properties ./res/twitter.properties &> twitter_listener.log
