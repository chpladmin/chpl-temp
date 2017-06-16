#!/bin/bash
(set -o igncr) 2>/dev/null && set -o igncr; # this comment is required to trick cygwin into dealing with windows vs. linux EOL characters

# to schedule this application for execution, add a line to a crontab on the machine hosting the application that looks something like:
# 15 5 * * * cd /tempApps && ./updateCertificationStatusApp.sh
# This will run it at 0515 UTC, which (depending on DST) is 0015 EST

# create timestamp and filename
TIMESTAMP=$(date "+%Y.%m.%d-%H.%M.%S")
log=logs/log.certStatusApp.$TIMESTAMP.txt

# put header info into log, then output application info into log file
echo "Update Certification Status for CCHIT Retired in 2014: " $TIMESTAMP >> $log
echo "####################################" >> $log
java -jar certStatusApp-jar-with-dependencies.jar 2>&1 | tee $log
echo "####################################" >> $log
