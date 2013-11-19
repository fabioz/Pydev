#!/bin/bash

# Attempt to get the keystore, but do not fail even if we can't get it, that
# just means we won't sign the results

set -x

if [ -z "$SIGN_KEYSTORE" ]
then
  echo "$SIGN_KEYSTORE is not set, Maven will not sign jars, see pom.xml"
  exit 0
fi

if test -e "$SIGN_KEYSTORE";
then
  echo "$SIGN_KEYSTORE already exists"
  exit 0
fi

s3cmd --no-progress get s3://$ARTIFACTS_S3_BUCKET/pydev.keystore "$SIGN_KEYSTORE" || rm "$SIGN_KEYSTORE"

exit 0
