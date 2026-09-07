package com.meneses.budgethunter.commons.platform

/**
 * Synchronous secure key-value store backed by a platform-native implementation.
 *
 * On iOS this is implemented natively in Swift (see `IOSKeychainStore.swift`) and injected
 * through [com.meneses.budgethunter.di.IOSBridge], avoiding the fragile Kotlin/Native cinterop
 * casts that the Security framework requires from Kotlin.
 */
interface KeychainStore {
    fun save(key: String, value: String)
    fun read(key: String): String?
    fun delete(key: String)
}
