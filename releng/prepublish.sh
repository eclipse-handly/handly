#!/bin/bash

###############################################################################
# Copyright (c) 2014, 2026 1C-Soft LLC and others.
#
# This program and the accompanying materials are made available under
# the terms of the Eclipse Public License 2.0 which is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     George Suaridze (1C) - initial API and implementation
#     Vladimir Piskarev (1C) - ongoing maintenance
###############################################################################

# This script prepares the project's build artifacts for distribution
# from Eclipse download facility. In particular, it adds p2 properties such as
# p2.mirrorsURL and p2.statsURI. The result can be found in the $BUILD_LABEL
# subdirectory of the $BUILD_DIR directory. This subdirectory can then be
# uploaded to Handly downloads area on download.eclipse.org.
#
# To add p2.mirrorsURL and p2.statsURI properties to the artifact repository
# this script uses xsltproc and the p2.xsl file co-located with this file.
#
# Requirements:
# * xsltproc, xz, zip and unzip utilities
#
# Optional:
# * BUILD_DIR may specify a root directory for build artifacts.
#   If this variable is not set, "../repository/target" is assumed.
# * BUILD_LABEL may specify a label under which the build will be published,
#   e.g. "1.0", "1.0RC1", "1.0.v20241002-1000". If this variable is not set,
#   the value from the $BUILD_DIR/VERSION file will be used.
# * DOWNLOADS_AREA may specify a directory in the project's download area
#   where the p2 repository will be published. If this variable is not set,
#   "releases" is assumed. Use "drops" for non-release builds.
#
# Usage: ./prepublish.sh

BUILD_DIR=${BUILD_DIR:-"../repository/target"}
[ ! -f "$BUILD_DIR/VERSION" ] && echo "Error: VERSION file does not exist" && exit 1
BUILD_VERSION=$(head -n 1 $BUILD_DIR/VERSION)
BUILD_LABEL=${BUILD_LABEL:-$BUILD_VERSION}
OUTPUT_DIR="$BUILD_DIR/$BUILD_LABEL"

#
# Populating $OUTPUT_DIR with the build artifacts to be published
#

mkdir $OUTPUT_DIR || exit 1

cp -r $BUILD_DIR/repository $OUTPUT_DIR || exit 1

cp $BUILD_DIR/handly-repository-${BUILD_VERSION}.zip $OUTPUT_DIR/handly-repository-${BUILD_LABEL}.zip || exit 1

(cd $BUILD_DIR && mkdir -p javadoc && cp -r reference/api/. javadoc/ && zip -rq handly-javadoc-${BUILD_LABEL}.zip javadoc/ && rm -rf javadoc) || exit 1
mv $BUILD_DIR/handly-javadoc-${BUILD_LABEL}.zip $OUTPUT_DIR || exit 1

#
# Updating p2 repository with additional properties
#

REPO_PATH="$OUTPUT_DIR/repository"
DOWNLOADS_AREA=${DOWNLOADS_AREA:-"releases"}
REMOTE_REPO_PATH="/handly/$DOWNLOADS_AREA/$BUILD_LABEL/repository"
MIRRORS_URL="http://www.eclipse.org/downloads/download.php?file=$REMOTE_REPO_PATH&format=xml"

unzip -p $REPO_PATH/artifacts.jar | xsltproc -stringparam mirrorsURL "$MIRRORS_URL" -stringparam statsId "$BUILD_LABEL" p2.xsl - > artifacts.xml
if [[ "$?" != "0" ]]; then
    rm -f artifacts.xml
    exit 1
fi

zip -q $REPO_PATH/artifacts.jar artifacts.xml
if [[ "$?" != "0" ]]; then
    rm -f artifacts.xml
    exit 1
fi

XZ_EXE=$(which xz)
if [[ "$?" != "0" || -z "${XZ_EXE}" ]]; then
    echo "Error: cannot locate xz executable"
    rm -f artifacts.xml
    exit 1
fi

$XZ_EXE -e --force artifacts.xml
if [[ "$?" != "0" ]]; then
    rm -f artifacts.xml
    exit 1
fi

mv -f artifacts.xml.xz $REPO_PATH
if [[ "$?" != "0" ]]; then
    rm -f artifacts.xml
    exit 1
fi

rm -f artifacts.xml

echo "$OUTPUT_DIR directory is ready for publishing"
