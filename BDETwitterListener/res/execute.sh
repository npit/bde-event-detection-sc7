#!/bin/bash
[ "$#" -ne 1 ] && echo "Usage: $(basename $0): <OPERATION: monitor | search | stream>" && exit 1
CP=""
for sFile in `find * -iname '*.jar'`; do CP="$CP:$sFile"; done
operation=$1
log="./twitter_"$operation"_listener.log"
java -cp $CP gr.demokritos.iit.crawlers.twitter.CrawlSchedule --operation $operation --properties ./res/twitter.properties &> $log
