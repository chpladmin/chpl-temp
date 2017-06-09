#!/bin/bash
(set -o igncr) 2>/dev/null && set -o igncr; # this comment is required to trick cygwin into dealing with windows vs. linux EOL characters

# to schedule this script for execution, add a line to a crontab on the machine hosting the application that looks something like:
# 0 08 * * * cd /some/directory/chpl-api/chpl/chpl-service && ./closeUpdateCertificationStatusApp.sh
# # This will run it once daily at 8am UTC (because the Jenkins machine is on UTC based on the date command) or 4am EST

# close certStatusApp-jar-with-dependencies.jar:
# `ps` lists processes
# `grep` finds things using regular expressions (in this case it'll probably be something like `java.*NAME OF JAR`)
# `cut` breaks a line on spaces
# `-fN` picks which chunk to keep
# We pipe `|` that pid to `kill` using the `-9` to *really mean it*
ps -aef | grep certStatusApp | grep -v grep | awk '{print $2}' | xargs kill