package com.meneses.budgethunter.commons.util

import androidx.navigation.NavType
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T : Any> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {

    override fun put(bundle: androidx.savedstate.SavedState, key: String, value: T) {
        bundle.write {
            putString(key, json.encodeToString(value))
        }
    }

    override fun get(bundle: androidx.savedstate.SavedState, key: String): T? {
        bundle.read {
            return if (contains(key)) {
                getString(key).let<String, T>(json::decodeFromString)
            } else {
                null
            }
        }
    }

    override fun parseValue(value: String): T {
        val jsonString = decodeURL(value, "UTF_8")
        return json.decodeFromString(jsonString)
    }

    override fun serializeAsValue(value: T): String {
        val jsonString = json.encodeToString(value)
        return encodeURL(jsonString, "UTF_8")
    }
}
