#!/bin/bash
CP=""
for sFile in `find * -iname '*.jar'`; do CP="$CP:$sFile"; done
log="./location_extraction.log"
java -cp $CP gr.demokritos.iit.location.schedule.LocationExtractionSchedule /location_extraction.properties &> $log
