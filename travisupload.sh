#!/bin/bash

# This script publishes all build results from Travis to S3

set -e
set -x

S3PUT="s3cmd --no-progress put --acl-public --guess-mime-type"
S3DEL="s3cmd --no-progress del"

S3BUILD=s3://$ARTIFACTS_S3_BUCKET/artifacts/build/$TRAVIS_BUILD_ID/$TRAVIS_JOB_ID/
S3BRANCH=s3://$ARTIFACTS_S3_BUCKET/artifacts/branch/$TRAVIS_BRANCH/

# Generate combined junit html and XML using ant
ant

# Make the runnable version in to a ZIP
# Exclude MyLyn from this version
(cd features/org.python.pydev.p2-repo/target/runnable && zip -r runnable.zip features plugins -x \*/\*mylyn\* && mv runnable.zip ..)

# Make the repo version in to a ZIP
(cd features/org.python.pydev.p2-repo/target/repository && zip -r repository.zip . && mv repository.zip ..)

# Remove last output on the branch
$S3DEL --recursive $S3BRANCH

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
