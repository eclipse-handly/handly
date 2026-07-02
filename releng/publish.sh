#!/bin/bash

###############################################################################
# Copyright (c) 2026 1C-Soft LLC and others.
#
# This program and the accompanying materials are made available under
# the terms of the Eclipse Public License 2.0 which is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
###############################################################################

# This script uploads the project's build artifacts to Eclipse download facility.
# The artifacts need to be prepared by the prepublish.sh script (co-located with
# this file), and are expected to be located in the $BUILD_LABEL subdirectory
# of the $BUILD_DIR directory.
#
# Optional:
# * BUILD_DIR may specify a root directory for build artifacts.
#   If this variable is not set, "../repository/target" is assumed.
# * BUILD_LABEL may specify a label under which the build will be published,
#   e.g. "1.0", "1.0RC1", "1.0.v20241002-1000". If this variable is not set,
#   the value from the $BUILD_DIR/VERSION file will be used.
# * DOWNLOADS_AREA may specify a directory in the project's download area
#   where the artifacts will be published. If this variable is not set,
#   "releases" is assumed. Use "drops" for non-release builds.
#
# Usage: ./publish.sh

BUILD_DIR=${BUILD_DIR:-"../repository/target"}
if [[ "$BUILD_LABEL" == "" ]]; then
    [ ! -f "$BUILD_DIR/VERSION" ] && echo "Error: BUILD_LABEL is not specified and VERSION file does not exist" >&2 && exit 1
    BUILD_LABEL=$(head -n 1 $BUILD_DIR/VERSION)
fi
INPUT_DIR="$BUILD_DIR/$BUILD_LABEL"
[ ! -d $INPUT_DIR ] && echo "Error: $INPUT_DIR directory does not exist" >&2 && exit 1

DOWNLOADS_AREA=${DOWNLOADS_AREA:-"releases"}
REMOTE_HOST="genie.handly@projects-storage.eclipse.org"
REMOTE_PATH="/home/data/httpd/download.eclipse.org/handly/$DOWNLOADS_AREA"

scp -o BatchMode=yes -r $INPUT_DIR $REMOTE_HOST:$REMOTE_PATH || exit 1
ssh -o BatchMode=yes $REMOTE_HOST "chmod -R g+w $REMOTE_PATH/$BUILD_LABEL"

echo "$BUILD_LABEL has been published from $INPUT_DIR to download.eclipse.org/handly/$DOWNLOADS_AREA"
