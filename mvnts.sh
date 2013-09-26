#!/bin/bash
# From https://gist.github.com/mmichaelis/993139
# Prepends a date string in front of the Maven output.
# A workaround for http://jira.codehaus.org/browse/MNG-519 "Timestamps on messages"
 
[ -z "${AWK}" ] && AWK="$(which gawk 2>/dev/null)" || AWK="$(which awk 2>/dev/null)" || { echo "Error: Could not find AWK tool."; exit 1; }
"${AWK}" 'BEGIN { print strftime("%Y-%m-%d %H:%M:%S"); }' 2>&1 > /dev/null || { echo "Error: your AWK version does not support strftime() function." && exit 1; }
 
mvn "${@}" 2>&1 |"${AWK}" '{ print strftime("%Y-%m-%d %H:%M:%S"), $0; fflush(); }'
# see comp.unix.shell FAQ: (http://cfajohnson.com/shell/cus-faq-2.html)
# "How do I get the exit code of cmd1 in cmd1|cmd2"
exit ${PIPESTATUS[0]}
