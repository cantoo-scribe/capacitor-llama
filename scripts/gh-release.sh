#!/bin/bash -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# VERSION=$(node -p "require('$ROOT_DIR/package.json').version")
VERSION=0.0.2
# ANDROID_TAG="v${VERSION}-android"
# IOS_TAG="v${VERSION}-ios"
TAG="v${VERSION}"

# android release
echo "Building Android AAR..."
cd "$ROOT_DIR/android"
./gradlew assembleRelease
# gh release create "$ANDROID_TAG" ./build/outputs/aar/android-release.aar -t "CapLlama Android Archive ${VERSION}"

# ios release
echo "Building iOS XCFramework..."
cd "$ROOT_DIR/ios"
zip -r capllama.xcframework.zip ./Sources/CapacitorLlamaPlugin/capllama.xcframework
# gh release create "$IOS_TAG" capllama.xcframework.zip -t "CapLlama iOS Framework ${VERSION}"

cd "$ROOT_DIR"

mv android/build/outputs/aar/android-release.aar ./
mv ios/capllama.xcframework.zip ./

echo "Creating GitHub release $TAG..."
gh release create "$TAG" \
  android-release.aar \
  capllama.xcframework.zip \
  -t "CapLlama ${TAG}"

rm -f capllama.xcframework.zip
rm -f android-release.aar

echo "Release $TAG created successfully!"