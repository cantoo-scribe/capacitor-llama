// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorLlama",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorLlama",
            targets: ["CapacitorLlamaPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "CapacitorLlamaPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CapacitorLlamaPlugin"),
        .testTarget(
            name: "CapacitorLlamaPluginTests",
            dependencies: ["CapacitorLlamaPlugin"],
            path: "ios/Tests/CapacitorLlamaPluginTests")
    ]
)