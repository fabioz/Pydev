#!/bin/bash

# This script uploads a cached .m2/repository directory to s3 to preload
# the repo. travism2_install does the download at the start of the build.

set -e
set -x

if [ "$PYDEV_TEST" == "false" ]; then
  # We only upload when doing tests because the test job has more
  # dependencies in .m2/repository
  exit 0
fi

cd ~/.m2

rm -f m2repo.tar.gz
tar --exclude=repository/.cache --exclude=repository/org/python/pydev -zcf m2repo.tar.gz repository/
s3cmd --no-progress put m2repo.tar.gz s3://$ARTIFACTS_S3_BUCKET/m2repo.tar.gz
