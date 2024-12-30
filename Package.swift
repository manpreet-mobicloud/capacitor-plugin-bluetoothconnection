// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Bluetooth",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "Bluetooth",
            targets: ["BluetoothPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "BluetoothPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BluetoothPlugin"),
        .testTarget(
            name: "BluetoothPluginTests",
            dependencies: ["BluetoothPlugin"],
            path: "ios/Tests/BluetoothPluginTests")
    ]
)