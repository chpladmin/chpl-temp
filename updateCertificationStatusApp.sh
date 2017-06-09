#!/bin/bash
(set -o igncr) 2>/dev/null && set -o igncr; # this comment is required to trick cygwin into dealing with windows vs. linux EOL characters

# to schedule this application for execution, add a line to a crontab on the machine hosting the application that looks something like:
# 15 5 * * * cd /tempApps && ./updateCertificationStatusApp.sh
# This will run it at 0515 UTC, which (depending on DST) is 0015 EST

java -jar certStatusApp-jar-with-dependencies.jar