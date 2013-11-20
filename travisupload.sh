#!/bin/bash

# This script publishes all build results from Travis to S3

set -e
set -x

# Don't do anything if we don't have access to S3
s3cmd ls  s3://$ARTIFACTS_S3_BUCKET > /dev/null || exit 0

S3PUT="s3cmd --no-progress put --acl-public --guess-mime-type"
S3DEL="s3cmd --no-progress del"

S3BUILD=s3://$ARTIFACTS_S3_BUCKET/artifacts/build/$TRAVIS_BUILD_ID/$TRAVIS_JOB_ID/
if [ "$PYDEV_TEST" == "true" ]; then
  S3BRANCH=s3://$ARTIFACTS_S3_BUCKET/artifacts/branch/$TRAVIS_BRANCH/test/
else
  S3BRANCH=s3://$ARTIFACTS_S3_BUCKET/artifacts/branch/$TRAVIS_BRANCH/release/
fi

# Generate combined junit html and XML using ant
ant

# Create some meta info to upload
echo "Build Info" > build.info
echo "PYDEV_TEST: $PYDEV_TEST" >> build.info
echo "S3BUILD: $S3BUILD" >> build.info
echo "S3BRANCH: $S3BRANCH" >> build.info
echo "TRAVIS_BRANCH: $TRAVIS_BRANCH" >> build.info
echo "TRAVIS_BUILD_DIR: $TRAVIS_BUILD_DIR" >> build.info
echo "TRAVIS_BUILD_ID: $TRAVIS_BUILD_ID" >> build.info
echo "TRAVIS_BUILD_NUMBER: $TRAVIS_BUILD_NUMBER" >> build.info
echo "TRAVIS_COMMIT: $TRAVIS_COMMIT" >> build.info
echo "TRAVIS_COMMIT_RANGE: $TRAVIS_COMMIT_RANGE" >> build.info
echo "TRAVIS_JOB_ID: $TRAVIS_JOB_ID" >> build.info
echo "TRAVIS_JOB_NUMBER: $TRAVIS_JOB_NUMBER" >> build.info
echo "TRAVIS_PULL_REQUEST: $TRAVIS_PULL_REQUEST" >> build.info
echo "TRAVIS_SECURE_ENV_VARS: $TRAVIS_SECURE_ENV_VARS" >> build.info
echo "TRAVIS_REPO_SLUG: $TRAVIS_REPO_SLUG" >> build.info
# TODO it would be great to get the build log, or perhaps just the maven build output
echo "Log URL: https://api.travis-ci.org/jobs/$TRAVIS_JOB_ID/log.txt" >> build.info

# Make the runnable version in to a ZIP
# Exclude MyLyn from this version
(cd features/org.python.pydev.p2-repo/target/runnable && zip -q -r runnable.zip features plugins -x \*/\*mylyn\* && mv runnable.zip ..)

# Make the repo version in to a ZIP
(cd features/org.python.pydev.p2-repo/target/repository && zip -q -r repository.zip . && mv repository.zip ..)

# Remove last output on the branch
$S3DEL --recursive $S3BRANCH

# Upload build info
$S3PUT build.info $S3BUILD
$S3PUT build.info $S3BRANCH

# Upload junit results
$S3PUT report/junit-noframes.html $S3BUILD
$S3PUT report/junit-noframes.html $S3BRANCH
$S3PUT report/TESTS-TestSuites.xml $S3BUILD
$S3PUT report/TESTS-TestSuites.xml $S3BRANCH

## Upload P2 repo
$S3PUT --recursive features/org.python.pydev.p2-repo/target/repository $S3BUILD
$S3PUT features/org.python.pydev.p2-repo/target/repository.zip $S3BUILD
$S3PUT --recursive features/org.python.pydev.p2-repo/target/repository $S3BRANCH
$S3PUT features/org.python.pydev.p2-repo/target/repository.zip $S3BRANCH

## Upload runnable version
$S3PUT features/org.python.pydev.p2-repo/target/runnable.zip $S3BUILD
$S3PUT features/org.python.pydev.p2-repo/target/runnable.zip $S3BRANCH
