import Foundation

@objc public class BleDeviceConnection: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
