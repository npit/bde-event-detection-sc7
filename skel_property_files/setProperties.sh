#!/usr/bin/env bash
#base="$(pwd)/BDEEventDetection"
base="."
editor="nano"
P=""
P+="./clustering.properties"
P+=" ./location_extraction.properties"
P+=" ./newscrawler_configuration.properties"
P+=" ./news_urls.txt"
P+=" ./twitter.properties"
P+=" ./twitter.queries"

for p in $P; 
do
	[ ! -f $p ] && echo 2>&1 "File $p does not exists"
	$editor $p
done
