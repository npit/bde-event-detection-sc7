#!/usr/bin/env bash
BDEDIR="../"
cp ./newscrawler_configuration.properties "$BDEDIR/BDERSSCrawler/res/"
cp ./blogcrawler_configuration.properties "$BDEDIR/BDERSSCrawler/res/"
cp ./news_urls.txt "$BDEDIR/BDERSSCrawler/res/"
cp ./clustering.properties "$BDEDIR/BDECLustering/res/"
cp ./location_extraction.properties "$BDEDIR/BDELocationExtraction/res/"
cp ./twitter.properties "$BDEDIR/BDETwitterListener/res/"
cp ./twitter.queries "$BDEDIR/BDETwitterListener/res/"
