#!/bin/bash
CP=""
for sFile in `find * -iname '*.jar'`; do CP="$CP:$sFile"; done
CP="/home/npittaras/Documents/project/BDE/BDEproject/bde-event-detection-sc7/BDERSSCrawler/target/classes:/home/npittaras/Documents/project/BDE/BDEproject/bde-event-detection-sc7/BDEBase/target/classes"
CP+=":/home/npittaras/.m2/repository/com/datastax/spark/spark-cassandra-connector_2.11/1.4.1/*"
CP+=":/home/npittaras/.m2/repository/com/datastax/cassandra/cassandra-driver-core/2.1.7.1/*"
CP+=":/home/npittaras/.m2/repository/org/apache/httpcomponents/httpclient/4.5.1/*"
CP+=":/home/npittaras/.m2/repository/org/apache/httpcomponents/httpcore/4.4.3/*"
CP+=":/home/npittaras/.m2/repository/com/mchange/mchange-commons-java/0.2.8/*"
CP+=":/home/npittaras/.m2/repository/com/mchange/c3p0/0.9.5-pre10/*"
CP+=":/home/npittaras/.m2/repository/org/slf4j/slf4j-api/1.7.7/*"
CP+=":/home/npittaras/.m2/repository/org/slf4j/slf4j-jdk14/1.5.6/*"
CP+=":/home/npittaras/.m2/repository/org/slf4j/slf4j-log4j12/1.6.4/*"
CP+=":/home/npittaras/.m2/repository/org/slf4j/jcl-over-slf4j/*"
path_to_config_file="newscrawler_configuration.properties"
folder='/home/npittaras/Documents/project/BDE/BDEproject/bde-event-detection-sc7/BDERSSCrawler/target/classes/gr/demokritos/iit/crawlers';
log="./blogcrawler.log"
java -cp $CP  gr.demokritos.iit.crawlers.rss.NewsCrawler  $path_to_config_file &> $log
cat $log
