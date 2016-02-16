#!/bin/bash
CP=""
for sFile in `find * -iname '*.jar'`; do CP="$CP:$sFile"; done
operation=$1
log="./news_crawler.log"
java -cp $CP gr.demokritos.iit.crawlers.rss.NewsCrawler &> $log
