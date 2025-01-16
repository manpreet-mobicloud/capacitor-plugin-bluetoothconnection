// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPluginBledeviceconnection",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "CapacitorPluginBledeviceconnection",
            targets: ["BleDeviceConnectionPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "BleDeviceConnectionPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BleDeviceConnectionPlugin"),
        .testTarget(
            name: "BleDeviceConnectionPluginTests",
            dependencies: ["BleDeviceConnectionPlugin"],
            path: "ios/Tests/BleDeviceConnectionPluginTests")
    ]
)