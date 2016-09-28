#!/usr/bin/env bash
#base="$(pwd)/BDEEventDetection"
base="$(pwd)"
P=""
P+=" $base/BDECLustering/res/clustering.properties"
P+=" $base/BDELocationExtraction/res/location_extraction.properties"
P+=" $base/BDERSSCrawler/res/newscrawler_configuration.properties"
P+=" $base/BDERSSCrawler/res/news_urls.txt"
P+=" $base/BDETwitterListener/res/twitter.properties"
P+=" $base/BDETwitterListener/res/twitter.queries"

for p in $P; 
do
	[ ! -f $p ] && echo 2>&1 "File $p does not exists"
	/home/nik/Software/Sublime/sublime_text_3/sublime_text $p
done
