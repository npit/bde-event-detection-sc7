#!/bin/bash
CP=""
for sFile in `find /home/npittaras/.m2 -iname '*.jar'`; do CP="$CP:$sFile"; done
for sFile in $(find $(pwd) -iname '*.jar'); do CP="$CP:$sFile"; done

path_to_config_file="newscrawler_configuration.properties"

log="./newscrawler.log"
java -cp $CP  gr.demokritos.iit.crawlers.rss.NewsCrawler  $path_to_config_file &> $log

cat $log

