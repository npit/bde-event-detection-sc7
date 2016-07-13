#!/usr/bin/env bash
host="172.17.0.2"
port="9042"
buildFile="/home/npittaras/Documents/project/BDE/BDEproject/bde-event-detection-sc7/BDERSSCrawler/res/db/rss_db.cql"

commands=$(cat "$buildFile")
cqlsh --cqlversion="3.3.1"  -k "bde" -e "$(cat $buildFile)" $host $port