import Foundation

@objc public class CapacitorLlama: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
