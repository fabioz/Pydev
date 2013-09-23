#!/bin/sh

if test -e ~/.s3cfg;
then
  echo "~/.s3cfg already exists, please remove it first"
  exit 1
fi

echo "[default]" > ~/.s3cfg
echo "access_key = $ARTIFACTS_AWS_ACCESS_KEY_ID" >> ~/.s3cfg
echo "secret_key = $ARTIFACTS_AWS_SECRET_ACCESS_KEY" >> ~/.s3cfg
