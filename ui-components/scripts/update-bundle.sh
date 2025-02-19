#!/bin/bash

# Check if version parameter is provided
if [ -z "$1" ]; then
    echo "Error: Version parameter is required"
    echo "Usage: $0 <version>"
    exit 1
fi

VERSION=$1
echo "Using version: $VERSION"

# Check if we are in the correct directory (scripts folder of ui-components)
CURRENT_DIR=${PWD##*/}
PARENT_DIR=${PWD%/*}
PARENT_DIR=${PARENT_DIR##*/}

if [ "$CURRENT_DIR" != "scripts" ]; then
    echo "Error: This script must be executed from the 'scripts' folder of ui-components"
    echo "Current directory: $PWD"
    exit 1
fi

if [ "$PARENT_DIR" != "ui-components" ]; then
    echo "Error: Parent directory must be 'ui-components'"
    echo "Parent directory: $PARENT_DIR"
    exit 1
fi

if [ ! -f "../package.json" ]; then
    echo "Error: package.json not found in parent directory"
    exit 1
fi

# Go to parent directory (ui-components)
cd ..

# Update version in package.json
if ! npm version "$VERSION" --git-tag-version false --allow-same-version true; then
    echo "Error updating version in package.json"
    exit 1
fi

echo "Successfully updated version to $VERSION in package.json"

# Build the bundle
if ! npm run build; then
    echo "Error during build process"
    exit 1
fi

# Check if generated files exist
if [ ! -f "dist/elaastic-vue-components-v$VERSION.umd.min.js" ]; then
    echo "JS file not found: elaastic-vue-components-v$VERSION.umd.min.js"
    exit 1
fi

if [ ! -f "dist/style-v$VERSION.css" ]; then
    echo "CSS file not found: style-v$VERSION.css"
    exit 1
fi

# Check/Create destination directory and clean its content
cd ..
TARGET_DIR="server/src/main/resources/static/vue-components"

if [ ! -d "$TARGET_DIR" ]; then
    mkdir -p "$TARGET_DIR"
else
    rm -f "$TARGET_DIR"/*
fi

# Copy the new files
cp "ui-components/dist/elaastic-vue-components-v$VERSION.umd.min.js" "$TARGET_DIR/"
cp "ui-components/dist/style-v$VERSION.css" "$TARGET_DIR/"

echo "Update completed successfully!"
echo "New files copied to $TARGET_DIR"