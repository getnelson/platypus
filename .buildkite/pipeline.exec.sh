#!/usr/bin/env bash

set -o errexit
set -o errtrace
set -o nounset
set -o pipefail

git config --global user.email "team@getnelson.io"
git config --global user.name "Nelson Team"

# subvert the sbt-rig plugin
export TRAVIS="true" # way hacky
export TRAVIS_COMMIT="$BUILDKITE_COMMIT"
export TRAVIS_REPO_SLUG="getnelson/platypus"
export TRAVIS_JOB_NUMBER="1.1"
export TRAVIS_BUILD_NUMBER="$BUILDKITE_BUILD_NUMBER"

if [ "${BUILDKITE_PULL_REQUEST:-}" = 'false' ]; then
	git checkout -qf "$BUILDKITE_BRANCH";
fi

echo "--> running build for ${TRAVIS_SCALA_VERSION}..."
eval "sbt ++${TRAVIS_SCALA_VERSION} 'release with-defaults'"