import Foundation
import Security

/**
 * Swift bridge for Kotlin Keychain Token Storage
 * Securely stores JWT tokens in iOS Keychain
 */

private let service = "com.youtuberatings.app"
private let account = "userToken"

@_silgen_name("YouTubeRatingApp_saveiOSKeychainToken")
public func saveiOSKeychainToken(token: String) {
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: account,
    ]
    
    // Try to delete existing token first
    SecItemDelete(query as CFDictionary)
    
    // Add new token
    let item: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: account,
        kSecValueData as String: token.data(using: .utf8) ?? Data(),
        kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
    ]
    
    let status = SecItemAdd(item as CFDictionary, nil)
    
    if status == errSecSuccess {
        print("Token saved to Keychain successfully")
    } else if status == errSecDuplicateItem {
        // Update existing item
        let updateQuery: [String: Any] = [
            kSecValueData as String: token.data(using: .utf8) ?? Data(),
        ]
        SecItemUpdate(query as CFDictionary, updateQuery as CFDictionary)
        print("Token updated in Keychain")
    } else {
        print("Error saving token to Keychain: \(status)")
    }
}

@_silgen_name("YouTubeRatingApp_getiOSKeychainToken")
public func getiOSKeychainToken() -> String? {
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: account,
        kSecReturnData as String: true,
    ]
    
    var result: AnyObject?
    let status = SecItemCopyMatching(query as CFDictionary, &result)
    
    if status == errSecSuccess,
       let data = result as? Data,
       let token = String(data: data, encoding: .utf8) {
        return token
    }
    
    return nil
}

@_silgen_name("YouTubeRatingApp_deleteiOSKeychainToken")
public func deleteiOSKeychainToken() {
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: account,
    ]
    
    let status = SecItemDelete(query as CFDictionary)
    
    if status == errSecSuccess {
        print("Token deleted from Keychain")
    } else if status == errSecItemNotFound {
        print("Token not found in Keychain")
    } else {
        print("Error deleting token from Keychain: \(status)")
    }
}
