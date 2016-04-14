#!/bin/bash
cd ~/infoasset/crawlers/twitter
LogFile=./twitterCrawl.log
# delete previous log file, if there
if [ -f $LogFile ];
then
   rm $LogFile
fi
# configure classpath
CP=""
for sFile in `find * -iname '*.jar'`; do CP="$CP:$sFile"; done
# run
#java -jar twittercrawler.jar >& $LogFile
java -cp $CP org.scify.crawlers.twitter.CrawlScript &> $LogFile

