#!/bin/bash
CP=""
for sFile in `find * -iname '*.jar'`; do CP="$CP:$sFile"; done

path_to_config_file="newscrawler_configuration.properties"
folder='/home/npittaras/Documents/project/BDE/BDEproject/bde-event-detection-sc7/BDERSSCrawler/target/classes/gr/demokritos/iit/crawlers';
log="./blogcrawler.log"
java -cp $CP  gr.demokritos.iit.crawlers.rss.NewsCrawler  $path_to_config_file &> $log
cat $log
