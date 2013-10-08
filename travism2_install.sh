#!/bin/bash

# This script downloads a cached .m2/repository directory from s3 to preload
# the repo. travism2_upload does the upload at the end of the build.

set -e
set -x

mkdir -p ~/.m2/repository
cd ~/.m2

rm -f m2repo.tar.gz
s3cmd --no-progress get s3://$ARTIFACTS_S3_BUCKET/m2repo.tar.gz || exit 0
tar --keep-old-files -zxf m2repo.tar.gz

# Just to be sure, remove pydev so that we build it and don't pick up
# an old version
rm -rf repository/org.python.pydev
