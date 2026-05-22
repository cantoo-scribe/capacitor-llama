#!/bin/bash -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# VERSION=$(node -p "require('$ROOT_DIR/package.json').version")
VERSION=0.0.2
TAG="v${VERSION}"

# android release
echo "Compressing Android JNI libs..."
zip -r android/src/main/capllama-android-jni-libs.zip ./android/src/main/jniLibs

# ios release
echo "Compressing iOS XCFramework..."
cd "$ROOT_DIR/ios"
zip -r capllama.xcframework.zip ./Sources/CapacitorLlamaPlugin/capllama.xcframework

cd "$ROOT_DIR"

mv android/src/main/capllama-android-jni-libs.zip ./
mv ios/capllama.xcframework.zip ./

echo "Creating GitHub release $TAG..."
gh release create "$TAG" \
  capllama-android-jni-libs.zip \
  capllama.xcframework.zip \
  -t "CapLlama ${TAG}"

# rm -f capllama.xcframework.zip
# rm -f capllama-android-jni-libs.zip

echo "Release $TAG created successfully!"