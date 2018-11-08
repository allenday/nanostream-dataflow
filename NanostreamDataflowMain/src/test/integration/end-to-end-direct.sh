#!/usr/bin/env bash
# fail on error
set -e

# resistant genes config
#ALIGNMENT_SERVER="${ALIGNMENT_SERVER:-http://130.211.33.64/cgi-bin/bwa.cgi}"
#ALIGNMENT_DB="${ALIGNMENT_DB:-DB.fast}"

# species config
 ALIGNMENT_SERVER="${ALIGNMENT_SERVER:-http://35.241.15.140/cgi-bin/bwa.cgi}"
 ALIGNMENT_DB="${ALIGNMENT_DB:-genomeDB.fast}"

## run test
# start dataflow application
 mvn clean install exec:java \
-Dexec.mainClass=com.theappsolutions.nanostream.NanostreamDirectApp \
-Dexec.cleanupDaemonThreads=false \
-Dexec.args=" \
 --runner=DirectRunner \
 --resistanceGenesAlignmentDatabase=${ALIGNMENT_DB} \
 --resistanceGenesAlignmentServer=${ALIGNMENT_SERVER}"