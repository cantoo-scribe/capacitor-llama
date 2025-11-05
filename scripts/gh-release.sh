#!/bin/bash -e

# android release
cd ./android
./gradlew assembleRelease
# TODO: dynamically get version
gh release create v0.0.1-android ./build/outputs/aar/android-release.aar -t "CapLlama Android Archive v0.0.1"

# ios release
cd ../ios
zip -r capllama.xcframework.zip ./Sources/CapacitorLlamaPlugin/capllama.xcframework
gh release create v0.0.1-ios capllama.xcframework.zip -t "CapLlama iOS Framework v0.0.1"
rm -f capllama.xcframework.zip

