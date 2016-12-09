#!/usr/bin/env bash
#base="$(pwd)/BDEEventDetection"
editor="$EDITOR"
#editor="nano"

if [ "$#" -gt 0 ]; then
	editor="$1"
fi

base="$(pwd)"
P=""
P+=" $base/BDEClustering/res/clustering.properties"
P+=" $base/BDELocationExtraction/res/location.properties"
P+=" $base/BDELocationExtraction/res/location.extras"
P+=" $base/BDERSSCrawler/res/news.properties"
P+=" $base/BDERSSCrawler/res/news.urls"
P+=" $base/BDETwitterListener/res/twitter.properties"
P+=" $base/BDETwitterListener/res/twitter.queries"
P+=" $base/BDETwitterListener/res/twitter.accounts"

for p in $P; 
do
	[ ! -f $p ] && echo 2>&1 "File $p does not exists"
	$editor $p
done
